#!/bin/bash
# 05-07: Cross-Entity Search - Reports/Trades/MarketOverview
# Requirements: SRCH-01

set -euo pipefail

BASE_URL="http://localhost:8080/api/v1"
PASS=0
FAIL=0
TEST_ID=0

assert_contains() {
    local test_name="$1" response="$2" expected="$3"
    TEST_ID=$((TEST_ID + 1))
    if echo "$response" | grep -q "$expected"; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name — expected '$expected' in response"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== 05-07: Cross-Entity Search (SRCH-01) ==="
echo ""

# --- Authentication ---
echo "[1/7] Authenticating..."

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"admin123\"}"
}

RESP_E1=$(login_user "enterprise001")
TOKEN_E1=$(echo "$RESP_E1" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

echo "  enterprise001 token: ${TOKEN_E1:0:20}..."
echo "  admin token: ${TOKEN_ADMIN:0:20}..."
echo ""

# --- SRCH-01: Search reports ---
echo "[2/7] SRCH-01: Search carbon reports..."

RESP_REPORTS=$(curl -s "$BASE_URL/search/reports?page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Reports search response: $(echo "$RESP_REPORTS" | head -c 400)"

assert_contains "Search reports returns 200" "$RESP_REPORTS" '"code":200'
echo ""

# --- Filter by keyword ---
echo "[3/7] SRCH-01: Search reports by keyword..."

RESP_KW=$(curl -s "$BASE_URL/search/reports?keyword=%E6%8A%A5%E5%91%8A&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Keyword search response: $(echo "$RESP_KW" | head -c 300)"

assert_contains "Keyword search returns 200" "$RESP_KW" '"code":200'
echo ""

# --- Filter by status ---
echo "[4/7] SRCH-01: Search reports by status..."

RESP_STATUS=$(curl -s "$BASE_URL/search/reports?status=2&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Status filter response: $(echo "$RESP_STATUS" | head -c 300)"

assert_contains "Status filter returns 200" "$RESP_STATUS" '"code":200'
echo ""

# --- Search trades ---
echo "[5/7] SRCH-01: Search trades..."

RESP_TRADES=$(curl -s "$BASE_URL/search/trades?page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Trades search response: $(echo "$RESP_TRADES" | head -c 400)"

assert_contains "Search trades returns 200" "$RESP_TRADES" '"code":200'
echo ""

# --- Filter trades by type ---
echo "[6/7] SRCH-01: Search trades by type..."

RESP_TYPE=$(curl -s "$BASE_URL/search/trades?tradeType=1&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Type filter response: $(echo "$RESP_TYPE" | head -c 300)"

assert_contains "Trade type filter returns 200" "$RESP_TYPE" '"code":200'
echo ""

# --- Market overview ---
echo "[7/7] SRCH-01: Market overview..."

RESP_MARKET=$(curl -s "$BASE_URL/search/market-overview" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Market overview response: $(echo "$RESP_MARKET" | head -c 400)"

assert_contains "Market overview returns 200" "$RESP_MARKET" '"code":200'
assert_contains "Market overview has data" "$RESP_MARKET" '"data":'
echo ""

# --- Cross-entity: admin can see all data ---
echo "[extra] Cross-entity: Admin search (all enterprises)..."

RESP_ADMIN_SEARCH=$(curl -s "$BASE_URL/search/reports?page=1&size=20" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Admin search returns 200" "$RESP_ADMIN_SEARCH" '"code":200'
echo ""

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
