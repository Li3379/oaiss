#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo -e "${YELLOW}[..]${NC} $1"; }

API="http://localhost:8080/api/v1"

TOTAL=0
PASSED=0
FAILED=0

# --- Helper: extract JSON field value via grep ---
extract_field() {
  local json="$1" field="$2"
  echo "$json" | { grep -o "\"$field\":[^,}]*" || true; } | head -1 | sed "s/\"$field\"://" | tr -d '"'
}

# --- Verify backend is up ---
info "Checking backend availability..."
curl -sf "$API/auth/login" -o /dev/null -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' || { fail "Backend not running. Start it first: cd oaiss-chain-backend && mvn spring-boot:run"; exit 1; }
ok "Backend is reachable"

# --- Login helper ---
login() {
  local username="$1"
  local resp=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"admin123\"}")

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
info "Logging in as enterprise001..."
TOKEN_E1=$(login "enterprise001") || { fail "Cannot proceed without enterprise001 token"; exit 1; }
ok "enterprise001 logged in"

info "Logging in as enterprise002..."
TOKEN_E2=$(login "enterprise002") || { fail "Cannot proceed without enterprise002 token"; exit 1; }
ok "enterprise002 logged in"

info "Logging in as admin..."
TOKEN_ADMIN=$(login "admin") || { fail "Cannot proceed without admin token"; exit 1; }
ok "admin logged in"

# --- COIN-01: View carbon coin balance ---
info "[COIN-01] Viewing carbon coin balance for enterprise001..."
COIN01_RESP=$(curl -s "$API/carbon-coin/account" \
  -H "Authorization: Bearer $TOKEN_E1")

COIN01_CODE=$(extract_field "$COIN01_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$COIN01_CODE" != "200" ]]; then
  fail "COIN-01: Get balance failed (code=$COIN01_CODE)"
  echo "$COIN01_RESP" >&2
  FAILED=$((FAILED + 1))
else
  BALANCE=$(extract_field "$COIN01_RESP" "balance")
  BALANCE_INT=$(echo "$BALANCE" | cut -d. -f1)
  if [[ -n "$BALANCE_INT" && "$BALANCE_INT" =~ ^[0-9]+$ ]]; then
    ok "COIN-01: Balance retrieved: balance=$BALANCE"
    PASSED=$((PASSED + 1))
  else
    fail "COIN-01: Balance field missing or not numeric: '$BALANCE'"
    FAILED=$((FAILED + 1))
  fi
fi

# --- COIN-02: View transaction history (paginated) ---
info "[COIN-02] Viewing transaction history for enterprise001..."
COIN02_RESP=$(curl -s "$API/carbon-coin/transactions?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN_E1")

COIN02_CODE=$(extract_field "$COIN02_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$COIN02_CODE" != "200" ]]; then
  fail "COIN-02: Get transactions failed (code=$COIN02_CODE)"
  echo "$COIN02_RESP" >&2
  FAILED=$((FAILED + 1))
else
  HAS_CONTENT=$(echo "$COIN02_RESP" | grep -o '"content"' | head -1)
  HAS_TOTAL=$(echo "$COIN02_RESP" | grep -o '"totalElements"' | head -1)
  if [[ -n "$HAS_CONTENT" && -n "$HAS_TOTAL" ]]; then
    TOTAL_ELEMENTS=$(extract_field "$COIN02_RESP" "totalElements")
    ok "COIN-02: Transaction history retrieved: content=present totalElements=$TOTAL_ELEMENTS"
    PASSED=$((PASSED + 1))
  else
    fail "COIN-02: Missing pagination fields (content=$HAS_CONTENT totalElements=$HAS_TOTAL)"
    FAILED=$((FAILED + 1))
  fi
fi

# --- COIN-03: Transfer coins from enterprise001 to enterprise002 ---
info "[COIN-03] Recording balances before transfer..."

# Get E1 balance before
BALANCE_E1_BEFORE_RESP=$(curl -s "$API/carbon-coin/account" \
  -H "Authorization: Bearer $TOKEN_E1")
BALANCE_E1_BEFORE=$(extract_field "$BALANCE_E1_BEFORE_RESP" "balance" | cut -d. -f1)
info "enterprise001 balance before: $BALANCE_E1_BEFORE"

# Get E2 balance before (admin can query by userId)
BALANCE_E2_BEFORE_RESP=$(curl -s "$API/carbon-coin/account?userId=3" \
  -H "Authorization: Bearer $TOKEN_ADMIN")
BALANCE_E2_BEFORE=$(extract_field "$BALANCE_E2_BEFORE_RESP" "balance" | cut -d. -f1)
info "enterprise002 balance before: $BALANCE_E2_BEFORE"

info "[COIN-03] Transferring 500 coins from enterprise001 to enterprise002..."
TRANSFER_RESP=$(curl -s -X POST "$API/carbon-coin/transfer" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"counterpartId": 3, "amount": 500, "remark": "Phase3 test transfer"}')

TRANSFER_CODE=$(extract_field "$TRANSFER_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$TRANSFER_CODE" == "200" ]]; then
  TRANSFER_BALANCE=$(extract_field "$TRANSFER_RESP" "balance")
  ok "COIN-03: Transfer succeeded: new sender balance=$TRANSFER_BALANCE"
  PASSED=$((PASSED + 1))
else
  fail "COIN-03: Transfer failed (code=$TRANSFER_CODE)"
  echo "$TRANSFER_RESP" >&2
  FAILED=$((FAILED + 1))
fi

# --- COIN-04: Verify atomic balance update after transfer ---
info "[COIN-04] Verifying atomic balance updates..."

BALANCE_E1_AFTER_RESP=$(curl -s "$API/carbon-coin/account" \
  -H "Authorization: Bearer $TOKEN_E1")
BALANCE_E1_AFTER=$(extract_field "$BALANCE_E1_AFTER_RESP" "balance" | cut -d. -f1)

BALANCE_E2_AFTER_RESP=$(curl -s "$API/carbon-coin/account?userId=3" \
  -H "Authorization: Bearer $TOKEN_ADMIN")
BALANCE_E2_AFTER=$(extract_field "$BALANCE_E2_AFTER_RESP" "balance" | cut -d. -f1)

info "enterprise001: before=$BALANCE_E1_BEFORE after=$BALANCE_E1_AFTER"
info "enterprise002: before=$BALANCE_E2_BEFORE after=$BALANCE_E2_AFTER"

TOTAL=$((TOTAL + 1))
EXPECTED_E1=$((BALANCE_E1_BEFORE - 500))
EXPECTED_E2=$((BALANCE_E2_BEFORE + 500))

if [[ "$BALANCE_E1_AFTER" -eq "$EXPECTED_E1" && "$BALANCE_E2_AFTER" -eq "$EXPECTED_E2" ]]; then
  ok "COIN-04: Atomic balance update verified: E1=$BALANCE_E1_AFTER (expected $EXPECTED_E1), E2=$BALANCE_E2_AFTER (expected $EXPECTED_E2)"
  PASSED=$((PASSED + 1))
else
  fail "COIN-04: Balance mismatch: E1=$BALANCE_E1_AFTER (expected $EXPECTED_E1), E2=$BALANCE_E2_AFTER (expected $EXPECTED_E2)"
  FAILED=$((FAILED + 1))
fi

# --- COIN-05: Insufficient balance rejected ---
info "[COIN-05] Testing insufficient balance transfer..."
INSUFF_RESP=$(curl -s -X POST "$API/carbon-coin/transfer" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"counterpartId": 3, "amount": 999999, "remark": "should fail"}')

INSUFF_CODE=$(extract_field "$INSUFF_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$INSUFF_CODE" != "200" ]]; then
  ok "COIN-05: Insufficient balance correctly rejected (code=$INSUFF_CODE)"
  PASSED=$((PASSED + 1))
else
  fail "COIN-05: Insufficient balance transfer should have failed but returned 200"
  FAILED=$((FAILED + 1))
fi

# --- Summary ---
echo ""
echo -e "========================================"
echo -e " Carbon Coin Test Results (COIN-01..05)"
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
