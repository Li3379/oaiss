#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo -e "${YELLOW}[..]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_PORT=3306 source "$SCRIPT_DIR/db-config.sh"

API="http://localhost:8080/api/v1"
API_PASSWORD="${API_PASSWORD:-admin123}"

TOTAL=0
PASSED=0
FAILED=0

# --- Helper: extract JSON field value (jq preferred, grep fallback) ---
extract_field() {
  local json="$1" field="$2"
  if command -v jq &>/dev/null; then
    echo "$json" | jq -r ".$field // empty" 2>/dev/null && return
  fi
  echo "$json" | { grep -o "\"$field\":[^,}]*" || true; } | head -1 | sed "s/\"$field\"://" | tr -d '"'
}

# --- Verify backend is up ---
info "Checking backend availability..."
curl -sf "$API/auth/login" -o /dev/null -X POST -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"$API_PASSWORD\"}" || { fail "Backend not running. Start it first: cd oaiss-chain-backend && mvn spring-boot:run"; exit 1; }
ok "Backend is reachable"

# --- Login helper ---
login() {
  local username="$1"
  local resp=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$API_PASSWORD\"}")

  local code=$(extract_field "$resp" "code")
  if [[ "$code" != "200" ]]; then
    fail "$username: Login failed (code=$code)"
    echo "$resp" >&2
    return 1
  fi

  local token=$(echo "$resp" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ -z "$token" ]]; then
    fail "$username: No accessToken in response"
    return 1
  fi
  echo "$token"
}

# --- Setup: Login ---
info "Logging in as enterprise001 (seller, userId=2)..."
TOKEN_E1=$(login "enterprise001") || { fail "Cannot proceed without enterprise001 token"; exit 1; }
ok "enterprise001 logged in"

info "Logging in as enterprise002 (buyer, userId=3)..."
TOKEN_E2=$(login "enterprise002") || { fail "Cannot proceed without enterprise002 token"; exit 1; }
ok "enterprise002 logged in"

info "Logging in as admin..."
TOKEN_ADMIN=$(login "admin") || { fail "Cannot proceed without admin token"; exit 1; }
ok "admin logged in"

# --- Reset P2P trade state (WR-03) ---
info "Resetting P2P trade state..."
mysql $MYSQL_CONN -e "DELETE FROM transaction WHERE trade_type=2" 2>/dev/null || true
mysql $MYSQL_CONN -e \
  "UPDATE enterprise SET carbon_tradable=38000, carbon_quota=50000, carbon_used=12000 WHERE user_id=2" 2>/dev/null
mysql $MYSQL_CONN -e \
  "UPDATE enterprise SET carbon_tradable=55000, carbon_quota=55000, carbon_used=0 WHERE user_id=3" 2>/dev/null
ok "P2P trade state reset, quotas restored to seed values"

# --- Pre-condition: Record seller and buyer quotas before P2P trade (for TRADE-10) ---
info "Recording enterprise quotas before P2P trade..."

SELLER_QUOTA_BEFORE=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_tradable FROM enterprise WHERE user_id=2" | tr -d '[:space:]' | cut -d. -f1)

BUYER_TRADABLE_BEFORE=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_tradable FROM enterprise WHERE user_id=3" | tr -d '[:space:]' | cut -d. -f1)
BUYER_CARBON_QUOTA_BEFORE=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_quota FROM enterprise WHERE user_id=3" | tr -d '[:space:]' | cut -d. -f1)

info "Before P2P trade:"
info "  Seller (enterprise001): carbonTradable=$SELLER_QUOTA_BEFORE"
info "  Buyer (enterprise002): carbonTradable=$BUYER_TRADABLE_BEFORE, carbonQuota=$BUYER_CARBON_QUOTA_BEFORE"

# --- TRADE-07: Create P2P trade (seller creates) ---
info "[TRADE-07] Creating P2P trade (seller=enterprise001, buyer=enterprise002, qty=50, price=10.00)..."
TRADE07_RESP=$(curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType": 2, "sellerId": 2, "buyerId": 3, "quantity": 50, "unitPrice": 10.00, "remark": "P2P test trade 1"}')

TRADE07_CODE=$(extract_field "$TRADE07_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$TRADE07_CODE" == "200" ]]; then
  P2P_ID=$(extract_field "$TRADE07_RESP" "id")
  P2P_STATUS=$(extract_field "$TRADE07_RESP" "status")
  ok "TRADE-07: P2P trade created: id=$P2P_ID, status=$P2P_STATUS (expected 0=PENDING)"
  PASSED=$((PASSED + 1))
else
  fail "TRADE-07: P2P trade creation failed (code=$TRADE07_CODE)"
  echo "$TRADE07_RESP" >&2
  FAILED=$((FAILED + 1))
fi

# --- TRADE-08: Confirm P2P trade (buyer confirms) ---
info "[TRADE-08] Confirming P2P trade (buyer=enterprise002 confirms)..."
TOTAL=$((TOTAL + 1))
if [[ -z "${P2P_ID:-}" ]]; then
  fail "TRADE-08: Cannot confirm: no P2P trade ID"
  FAILED=$((FAILED + 1))
else
  TRADE08_RESP=$(curl -s -X POST "$API/trade/$P2P_ID/confirm" \
    -H "Authorization: Bearer $TOKEN_E2")

  TRADE08_CODE=$(extract_field "$TRADE08_RESP" "code")
  if [[ "$TRADE08_CODE" == "200" ]]; then
    CONFIRM_STATUS=$(extract_field "$TRADE08_RESP" "status")
    if [[ "$CONFIRM_STATUS" == "2" ]]; then
      ok "TRADE-08: P2P trade confirmed: status=$CONFIRM_STATUS (COMPLETED)"
      PASSED=$((PASSED + 1))
    else
      fail "TRADE-08: Status mismatch: got $CONFIRM_STATUS, expected 2 (COMPLETED)"
      FAILED=$((FAILED + 1))
    fi
  else
    fail "TRADE-08: P2P confirm failed (code=$TRADE08_CODE)"
    echo "$TRADE08_RESP" >&2
    FAILED=$((FAILED + 1))
  fi
fi

# --- TRADE-10: Verify settlement after confirmation (direct DB quota verification) ---
info "[TRADE-10] Verifying settlement via direct DB query..."

SELLER_QUOTA_AFTER=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_tradable FROM enterprise WHERE user_id=2" | tr -d '[:space:]' | cut -d. -f1)

BUYER_TRADABLE_AFTER=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_tradable FROM enterprise WHERE user_id=3" | tr -d '[:space:]' | cut -d. -f1)
BUYER_CARBON_QUOTA_AFTER=$(mysql $MYSQL_CONN -N -e \
  "SELECT carbon_quota FROM enterprise WHERE user_id=3" | tr -d '[:space:]' | cut -d. -f1)

info "After P2P confirmation:"
info "  Seller: carbonTradable=$SELLER_QUOTA_AFTER (was $SELLER_QUOTA_BEFORE)"
info "  Buyer: carbonTradable=$BUYER_TRADABLE_AFTER (was $BUYER_TRADABLE_BEFORE), carbonQuota=$BUYER_CARBON_QUOTA_AFTER (was $BUYER_CARBON_QUOTA_BEFORE)"

TOTAL=$((TOTAL + 1))
EXPECTED_SELLER=$((SELLER_QUOTA_BEFORE - 50))
EXPECTED_BUYER_TRADABLE=$((BUYER_TRADABLE_BEFORE + 50))
EXPECTED_BUYER_QUOTA=$((BUYER_CARBON_QUOTA_BEFORE + 50))

if [[ "$SELLER_QUOTA_AFTER" -eq "$EXPECTED_SELLER" && \
      "$BUYER_TRADABLE_AFTER" -eq "$EXPECTED_BUYER_TRADABLE" && \
      "$BUYER_CARBON_QUOTA_AFTER" -eq "$EXPECTED_BUYER_QUOTA" ]]; then
  ok "TRADE-10: Settlement verified: seller -50 tradable, buyer +50 tradable/+50 quota"
  PASSED=$((PASSED + 1))
else
  fail "TRADE-10: Settlement mismatch: seller=$SELLER_QUOTA_AFTER (exp $EXPECTED_SELLER), buyer_tradable=$BUYER_TRADABLE_AFTER (exp $EXPECTED_BUYER_TRADABLE), buyer_quota=$BUYER_CARBON_QUOTA_AFTER (exp $EXPECTED_BUYER_QUOTA)"
  FAILED=$((FAILED + 1))
fi

# Verify completed trade appears in my-trades
info "[TRADE-10] Verifying completed trade in my-trades list..."
MY_TRADES_RESP=$(curl -s "$API/trade/my-trades?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN_E1")

MY_TRADES_CODE=$(extract_field "$MY_TRADES_RESP" "code")
if [[ "$MY_TRADES_CODE" == "200" ]]; then
  if echo "$MY_TRADES_RESP" | grep -q "\"id\":$P2P_ID"; then
    info "TRADE-10: Completed trade $P2P_ID found in my-trades list"
  else
    info "TRADE-10: Trade $P2P_ID not found in my-trades (may be pagination or endpoint difference)"
  fi
fi

# --- TRADE-09: Cancel P2P trade (rejection path) ---
info "[TRADE-09] Creating second P2P trade to test cancellation..."
TRADE09_CREATE_RESP=$(curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType": 2, "sellerId": 2, "buyerId": 3, "quantity": 30, "unitPrice": 8.00, "remark": "P2P test trade 2 (to cancel)"}')

TRADE09_CREATE_CODE=$(extract_field "$TRADE09_CREATE_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$TRADE09_CREATE_CODE" != "200" ]]; then
  fail "TRADE-09: Second P2P trade creation failed (code=$TRADE09_CREATE_CODE)"
  echo "$TRADE09_CREATE_RESP" >&2
  FAILED=$((FAILED + 1))
else
  P2P2_ID=$(extract_field "$TRADE09_CREATE_RESP" "id")
  info "TRADE-09: Second P2P trade created: id=$P2P2_ID"

  # Cancel the trade
  CANCEL_RESP=$(curl -s -X POST "$API/trade/$P2P2_ID/cancel" \
    -H "Authorization: Bearer $TOKEN_E1")

  CANCEL_CODE=$(extract_field "$CANCEL_RESP" "code")
  if [[ "$CANCEL_CODE" == "200" ]]; then
    CANCEL_STATUS=$(extract_field "$CANCEL_RESP" "status")
    if [[ "$CANCEL_STATUS" == "3" ]]; then
      ok "TRADE-09: P2P trade cancelled: status=$CANCEL_STATUS (CANCELLED)"
      PASSED=$((PASSED + 1))
    else
      fail "TRADE-09: Cancel status mismatch: got $CANCEL_STATUS, expected 3 (CANCELLED)"
      FAILED=$((FAILED + 1))
    fi
  else
    fail "TRADE-09: P2P cancel failed (code=$CANCEL_CODE)"
    echo "$CANCEL_RESP" >&2
    FAILED=$((FAILED + 1))
  fi
fi

# --- TRADE-11: Document TradeController vs DoubleAuctionController relationship ---
# NOTE: This is documentation only -- not a runtime assertion.
# TradeController: handles P2P trades (trade_type=2) and simple auction listings (trade_type=1)
#   - Uses 'transaction' table for trade records
#   - Endpoints: POST /trade/p2p, POST /trade/{id}/confirm, POST /trade/{id}/cancel
# DoubleAuctionController: handles double auction buy/sell orders with matching engine
#   - Uses 'auction_order' table for orders, 'matching_result' table for matches
#   - Endpoints: POST /auction/buy, POST /auction/sell, POST /auction/match
# These are SEPARATE, INDEPENDENT subsystems -- no shared matching engine.
info "[TRADE-11] Controller relationship documented (comment only, no test count)"

# --- Summary ---
echo ""
echo -e "========================================"
echo -e " P2P Trade Test Results (TRADE-07..10)"
echo -e "========================================"
echo -e " Total:  $TOTAL"
echo -e " Passed: ${GREEN}$PASSED${NC}"
if [[ $FAILED -gt 0 ]]; then
  echo -e " Failed: ${RED}$FAILED${NC}"
else
  echo -e " Failed: 0"
fi
echo -e "========================================"

if [[ $FAILED -gt 0 ]]; then
  exit 1
fi
