#!/bin/bash
# 05-03: Emission Ratings - View/Create/Recalculate + Rankings + AI Predict
# Requirements: EMIT-01, EMIT-02, EMIT-03

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

echo "=== 05-03: Emission Ratings (EMIT-01~03) ==="
echo ""

# --- Authentication ---
echo "[1/6] Authenticating..."

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"admin123\"}"
}

RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  admin token: ${TOKEN_ADMIN:0:20}..."

# Get enterprise001's enterprise ID from carbon reports
E1_ENTERPRISE_ID=$(curl -s "$BASE_URL/credit/my-score" \
    -H "Authorization: Bearer $(echo $(login_user 'enterprise001') | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)" \
    | grep -o '"enterpriseId":[0-9]*' | head -1 | cut -d: -f2)
echo "  enterprise001 enterpriseId: $E1_ENTERPRISE_ID"
echo ""

# --- EMIT-01: View emission ratings ---
echo "[2/6] EMIT-01: View emission ratings for enterprise001..."

RESP_RATINGS=$(curl -s "$BASE_URL/emission/ratings/$E1_ENTERPRISE_ID" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Ratings response: $(echo "$RESP_RATINGS" | head -c 400)"

assert_contains "Get ratings returns 200" "$RESP_RATINGS" '"code":200'
echo ""

# --- EMIT-01: Create/recalculate rating ---
echo "[3/6] EMIT-01: Create emission rating..."

RESP_CREATE=$(curl -s -X POST "$BASE_URL/emission/ratings" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d "{\"enterpriseId\":$E1_ENTERPRISE_ID,\"year\":\"2025\",\"totalEmission\":1500.50,\"revenue\":50000,\"ratedBy\":1}")
echo "  Create rating response: $(echo "$RESP_CREATE" | head -c 400)"

assert_contains "Create rating returns 200" "$RESP_CREATE" '"code":200'
assert_contains "Rating has ratingLevel" "$RESP_CREATE" '"ratingLevel":'
assert_contains "Rating has totalEmission" "$RESP_CREATE" '"totalEmission":'
echo ""

# --- DB verification ---
echo "[4/6] DB check: emission_rating table..."

DB_COUNT=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 oaiss_chain -sNe \
    "SELECT COUNT(*) FROM emission_rating WHERE enterprise_id=$E1_ENTERPRISE_ID" 2>/dev/null || echo "0")
TEST_ID=$((TEST_ID + 1))
if [ "$DB_COUNT" -gt 0 ]; then
    echo "  [PASS] Test $TEST_ID: emission_rating has records for enterprise001 (count=$DB_COUNT)"
    PASS=$((PASS + 1))
else
    echo "  [FAIL] Test $TEST_ID: emission_rating has NO records for enterprise001"
    FAIL=$((FAIL + 1))
fi
echo ""

# --- EMIT-02: Industry rankings ---
echo "[5/6] EMIT-02: Industry rankings for 2025..."

RESP_RANKINGS=$(curl -s "$BASE_URL/emission/rankings/2025" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Rankings response: $(echo "$RESP_RANKINGS" | head -c 400)"

assert_contains "Rankings returns 200" "$RESP_RANKINGS" '"code":200'
assert_contains "Rankings has list data" "$RESP_RANKINGS" '"data":'
echo ""

# --- EMIT-03: AI Prediction ---
echo "[6/6] EMIT-03: AI emission prediction..."

RESP_PREDICT=$(curl -s -X POST "$BASE_URL/emission/predict" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d "{\"enterpriseId\":$E1_ENTERPRISE_ID,\"predictMonths\":6}")
echo "  Predict response: $(echo "$RESP_PREDICT" | head -c 400)"

assert_contains "Prediction returns 200" "$RESP_PREDICT" '"code":200'
assert_contains "Prediction has data" "$RESP_PREDICT" '"data":'
echo ""

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
