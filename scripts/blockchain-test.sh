#!/bin/bash
# 05-04: Blockchain Explorer - Status/Blocks/Transactions mock data + format validation
# Requirements: BLOCK-01, BLOCK-02, BLOCK-03

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

echo "=== 05-04: Blockchain Explorer (BLOCK-01~03) ==="
echo ""

# --- Authentication ---
echo "[1/7] Authenticating..."

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"admin123\"}"
}

RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  admin token: ${TOKEN_ADMIN:0:20}..."
echo ""

# --- BLOCK-01: Connection status ---
echo "[2/7] BLOCK-01: Blockchain connection status..."

RESP_STATUS=$(curl -s "$BASE_URL/blockchain/status" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Status response: $(echo "$RESP_STATUS" | head -c 400)"

assert_contains "Status returns 200" "$RESP_STATUS" '"code":200'
assert_contains "Status has connected field" "$RESP_STATUS" '"connected"'
assert_contains "Status has channel field" "$RESP_STATUS" '"channel"'
assert_contains "Status has mode field" "$RESP_STATUS" '"mode"'
assert_contains "Mode is MOCK" "$RESP_STATUS" 'MOCK'
echo ""

# --- BLOCK-02: Latest blocks ---
echo "[3/7] BLOCK-02: Latest blocks..."

RESP_BLOCKS=$(curl -s "$BASE_URL/blockchain/blocks/latest?page=1&size=5" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Blocks response: $(echo "$RESP_BLOCKS" | head -c 500)"

assert_contains "Blocks returns 200" "$RESP_BLOCKS" '"code":200'

# Extract first blockNumber from content
FIRST_BLOCK=$(echo "$RESP_BLOCKS" | grep -o '"blockNumber":[0-9]*' | head -1 | cut -d: -f2)
FIRST_HASH=$(echo "$RESP_BLOCKS" | grep -o '"blockHash":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  First block: number=$FIRST_BLOCK hash=$FIRST_HASH"

# Validate blockHash format (starts with 0x)
TEST_ID=$((TEST_ID + 1))
if [ -n "$FIRST_HASH" ] && echo "$FIRST_HASH" | grep -q '^0x'; then
    echo "  [PASS] Test $TEST_ID: blockHash starts with '0x'"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] Test $TEST_ID: blockHash does not start with '0x' (got: $FIRST_HASH)"
    FAIL=$((FAIL + 1))
fi

# Validate blockNumber is positive
TEST_ID=$((TEST_ID + 1))
if [ -n "$FIRST_BLOCK" ] && [ "$FIRST_BLOCK" -gt 0 ] 2>/dev/null; then
    echo "  [PASS] Test $TEST_ID: blockNumber is positive integer ($FIRST_BLOCK)"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] Test $TEST_ID: blockNumber is not a positive integer (got: $FIRST_BLOCK)"
    FAIL=$((FAIL + 1))
fi
echo ""

# --- Block detail ---
echo "[4/7] BLOCK-02: Block detail..."

if [ -n "$FIRST_BLOCK" ]; then
    RESP_BLOCK_DETAIL=$(curl -s "$BASE_URL/blockchain/block/$FIRST_BLOCK" \
        -H "Authorization: Bearer $TOKEN_ADMIN")
    echo "  Block detail response: $(echo "$RESP_BLOCK_DETAIL" | head -c 400)"

    assert_contains "Block detail returns 200" "$RESP_BLOCK_DETAIL" '"code":200'
    assert_contains "Block detail has data" "$RESP_BLOCK_DETAIL" '"data":'
else
    TEST_ID=$((TEST_ID + 1))
    echo "  [SKIP] Test $TEST_ID: No block number available for detail query"
fi
echo ""

# --- BLOCK-03: Transactions ---
echo "[5/7] BLOCK-03: Transaction list..."

RESP_TXS=$(curl -s "$BASE_URL/blockchain/transactions?page=1&size=5" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Transactions response: $(echo "$RESP_TXS" | head -c 500)"

assert_contains "Transactions returns 200" "$RESP_TXS" '"code":200'

# Extract first txHash
FIRST_TX=$(echo "$RESP_TXS" | grep -o '"txHash":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  First txHash: $FIRST_TX"

# Validate txHash format (starts with tx_mock_)
TEST_ID=$((TEST_ID + 1))
if [ -n "$FIRST_TX" ] && echo "$FIRST_TX" | grep -q '^tx_mock_'; then
    echo "  [PASS] Test $TEST_ID: txHash starts with 'tx_mock_'"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] Test $TEST_ID: txHash does not start with 'tx_mock_' (got: $FIRST_TX)"
    FAIL=$((FAIL + 1))
fi
echo ""

# --- Transaction detail ---
echo "[6/7] BLOCK-03: Transaction detail..."

if [ -n "$FIRST_TX" ]; then
    RESP_TX_DETAIL=$(curl -s "$BASE_URL/blockchain/transaction/$FIRST_TX" \
        -H "Authorization: Bearer $TOKEN_ADMIN")
    echo "  Transaction detail response: $(echo "$RESP_TX_DETAIL" | head -c 400)"

    assert_contains "Transaction detail returns 200" "$RESP_TX_DETAIL" '"code":200'

    # Check status field (mock transactions should have VALID status)
    assert_contains "Transaction has status VALID" "$RESP_TX_DETAIL" 'VALID'
else
    TEST_ID=$((TEST_ID + 1))
    echo "  [SKIP] Test $TEST_ID: No txHash available for detail query"
fi
echo ""

# --- Pagination check ---
echo "[7/7] Pagination: page=2&size=2..."

RESP_PAGE2=$(curl -s "$BASE_URL/blockchain/transactions?page=2&size=2" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Pagination returns 200" "$RESP_PAGE2" '"code":200'
echo ""

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
