#!/bin/bash
# 04-02: Credit Score + Level Evaluation + Trade Restrictions
# Requirements: CRED-01~05

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

extract_json_int() {
    local json="$1" key="$2"
    echo "$json" | grep -o "\"$key\":[0-9-]*" | head -1 | cut -d: -f2
}

# Compute expected level from score
expected_level() {
    local score=$1
    if [ "$score" -ge 80 ]; then echo "EXCELLENT"
    elif [ "$score" -ge 60 ]; then echo "GOOD"
    elif [ "$score" -ge 40 ]; then echo "WARNING"
    elif [ "$score" -ge 20 ]; then echo "DANGER"
    else echo "FROZEN"
    fi
}

echo "=== 04-02: Credit Score + Level Evaluation + Trade Restrictions ==="
echo ""

# ─── Authentication ───
echo "[1/5] Authenticating..."

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

# ─── CRED-01: View credit score and history ───
echo "[2/5] CRED-01: View credit score + history..."

RESP_SCORE=$(curl -s "$BASE_URL/credit/my-score" \
    -H "Authorization: Bearer $TOKEN_E1")
INITIAL_SCORE=$(extract_json_int "$RESP_SCORE" "score")
INITIAL_LEVEL=$(echo "$RESP_SCORE" | grep -o '"level":"[A-Z]*"' | head -1 | cut -d'"' -f4)
assert_contains "View my credit score" "$RESP_SCORE" '"score":'
assert_contains "Credit level present" "$RESP_SCORE" '"level":'
echo "  Initial score: $INITIAL_SCORE, level: $INITIAL_LEVEL"

RESP_HISTORY=$(curl -s "$BASE_URL/credit/history?page=1&size=20" \
    -H "Authorization: Bearer $TOKEN_E1")
assert_contains "View credit history (paginated)" "$RESP_HISTORY" '"content":'

E1_ENTERPRISE_ID=$(echo "$RESP_SCORE" | grep -o '"enterpriseId":[0-9]*' | head -1 | cut -d: -f2)
echo "  Enterprise ID: $E1_ENTERPRISE_ID"
echo ""

# ─── CRED-02: Level evaluation — use large deductions to cross thresholds ───
echo "[3/5] CRED-02: Credit level transitions..."

# Helper: deduct and verify level + restrictions
deduct_verify() {
    local event_type=$1 desc=$2
    RESP=$(curl -s -X POST "$BASE_URL/credit/deduct" \
        -H "Authorization: Bearer $TOKEN_ADMIN" \
        -H "Content-Type: application/json" \
        -d "{\"enterpriseId\": $E1_ENTERPRISE_ID, \"eventType\": $event_type, \"description\": \"$desc\"}")

    local score=$(extract_json_int "$RESP" "score")
    local level=$(echo "$RESP" | grep -o '"level":"[A-Z]*"' | head -1 | cut -d'"' -f4)
    local restricted=$(echo "$RESP" | grep -o '"tradeRestricted":[a-z]*' | head -1 | cut -d: -f2)
    local frozen=$(echo "$RESP" | grep -o '"accountFrozen":[a-z]*' | head -1 | cut -d: -f2)
    local exp_level=$(expected_level "$score")
    local exp_restricted="false"
    local exp_frozen="false"
    [ "$score" -lt 40 ] && exp_restricted="true"
    [ "$score" -lt 20 ] && exp_frozen="true"

    echo "  -> score=$score level=$level restricted=$restricted frozen=$frozen"

    # Verify level
    if [ "$level" = "$exp_level" ]; then
        TEST_ID=$((TEST_ID + 1)); echo "  [PASS] Test $TEST_ID: Level=$level correct for score=$score"; PASS=$((PASS + 1))
    else
        TEST_ID=$((TEST_ID + 1)); echo "  [FAIL] Test $TEST_ID: Expected level=$exp_level got=$level at score=$score"; FAIL=$((FAIL + 1))
    fi

    # Verify tradeRestricted
    if [ "$restricted" = "$exp_restricted" ]; then
        TEST_ID=$((TEST_ID + 1)); echo "  [PASS] Test $TEST_ID: tradeRestricted=$restricted correct"; PASS=$((PASS + 1))
    else
        TEST_ID=$((TEST_ID + 1)); echo "  [FAIL] Test $TEST_ID: Expected tradeRestricted=$exp_restricted got=$restricted"; FAIL=$((FAIL + 1))
    fi

    # Verify accountFrozen
    if [ "$frozen" = "$exp_frozen" ]; then
        TEST_ID=$((TEST_ID + 1)); echo "  [PASS] Test $TEST_ID: accountFrozen=$frozen correct"; PASS=$((PASS + 1))
    else
        TEST_ID=$((TEST_ID + 1)); echo "  [FAIL] Test $TEST_ID: Expected accountFrozen=$exp_frozen got=$frozen"; FAIL=$((FAIL + 1))
    fi

    CURRENT_SCORE=$score
}

CURRENT_SCORE=$INITIAL_SCORE

# Step 1: Deduct until score < 60 (should pass through GOOD, then reach WARNING)
# eventType=3 is MINOR_VIOLATION (-10), eventType=4 is MAJOR_VIOLATION (-30)
echo "  Deducting to WARNING zone (score < 60)..."
while [ "$CURRENT_SCORE" -ge 60 ]; do
    deduct_verify 3 "Deduction toward WARNING"
done

# We're now in WARNING(40-59) or below. Keep going to below 40 for trade restriction.
echo "  Deducting to DANGER zone (score < 40, trade restriction should activate)..."
while [ "$CURRENT_SCORE" -ge 40 ]; do
    deduct_verify 3 "Deduction toward DANGER"
done

# We're now in DANGER(20-39). Keep going to below 20 for frozen.
echo "  Deducting to FROZEN zone (score < 20, account should freeze)..."
while [ "$CURRENT_SCORE" -ge 20 ]; do
    deduct_verify 3 "Deduction toward FROZEN"
done

# Final verification at FROZEN
echo "  Final state at FROZEN level:"
RESP_FINAL=$(curl -s "$BASE_URL/credit/$E1_ENTERPRISE_ID" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
FINAL_SCORE=$(extract_json_int "$RESP_FINAL" "score")
assert_contains "Final score is below 20" "$RESP_FINAL" '"accountFrozen":true'
echo ""

# ─── CRED-03: Trade restriction check ───
echo "[4/5] CRED-03/04: Trade permission + frozen verification..."

RESP_PERM=$(curl -s "$BASE_URL/credit/check-permission/$E1_ENTERPRISE_ID" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Trade permission=false when score<40" "$RESP_PERM" '"data":false'

# Verify via /frozen endpoint (admin only)
RESP_FROZEN=$(curl -s "$BASE_URL/credit/frozen" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Enterprise in frozen list" "$RESP_FROZEN" "$E1_ENTERPRISE_ID"

# Verify via /restricted endpoint (admin only)
RESP_RESTRICTED=$(curl -s "$BASE_URL/credit/restricted" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Enterprise in restricted list" "$RESP_RESTRICTED" "$E1_ENTERPRISE_ID"
echo ""

# ─── CRED-05: Admin bonus + re-evaluation ───
echo "[5/5] CRED-05: Admin bonus recovery + re-evaluate..."

# Use eventType=5 (BONUS_GOOD_BEHAVIOR, +5) via /bonus endpoint to restore
RESP_BONUS=$(curl -s -X POST "$BASE_URL/credit/bonus?enterpriseId=$E1_ENTERPRISE_ID&points=100&description=Score%20recovery%20test" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
BONUS_SCORE=$(extract_json_int "$RESP_BONUS" "score")
echo "  After bonus: score=$BONUS_SCORE"
assert_contains "Bonus restores score above 80" "$RESP_BONUS" '"level":"EXCELLENT"'
assert_contains "Trade restriction cleared" "$RESP_BONUS" '"tradeRestricted":false'
assert_contains "Account unfrozen" "$RESP_BONUS" '"accountFrozen":false'

# Verify trade permission restored
RESP_PERM_AFTER=$(curl -s "$BASE_URL/credit/check-permission/$E1_ENTERPRISE_ID" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Trade permission restored=true" "$RESP_PERM_AFTER" '"data":true'

# Re-evaluate
RESP_EVAL=$(curl -s -X POST "$BASE_URL/credit/evaluate/$E1_ENTERPRISE_ID" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Re-evaluation confirms EXCELLENT" "$RESP_EVAL" '"level":"EXCELLENT"'
echo ""

# ─── Summary ───
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
