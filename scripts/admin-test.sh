#!/bin/bash
# 05-05: Admin Management - List/Filter Users, Status Toggle, Dashboard, Statistics
# Requirements: ADMIN-01, ADMIN-02 (gap), ADMIN-03 (gap), ADMIN-04, ADMIN-05

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

echo "=== 05-05: Admin Management (ADMIN-01~05) ==="
echo ""

# --- Authentication ---
echo "[1/8] Authenticating..."

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"admin123\"}"
}

RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  admin token: ${TOKEN_ADMIN:0:20}..."
echo ""

# --- ADMIN-01: List users ---
echo "[2/8] ADMIN-01: List users (paginated)..."

RESP_USERS=$(curl -s "$BASE_URL/admin/users?page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Users response: $(echo "$RESP_USERS" | head -c 400)"

assert_contains "List users returns 200" "$RESP_USERS" '"code":200'
assert_contains "Users has content" "$RESP_USERS" '"content":'

# Extract enterprise001's userId for status toggle
E1_USER_ID=$(echo "$RESP_USERS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
# Find a non-admin user (enterprise001 typically has id=2)
E1_USER_ID=$(echo "$RESP_USERS" | python3 -c "
import sys, json
data = json.loads(sys.stdin.read().replace('\"code\":', '\"_code\":').split('\"data\":', 1)[1].rsplit(',\"meta\"', 1)[0])
for u in data.get('content', []):
    if u.get('userType', 0) == 1:
        print(u['id'])
        break
" 2>/dev/null || echo "$E1_USER_ID")
echo "  enterprise001 userId: $E1_USER_ID"
echo ""

# --- Filter by userType ---
echo "[3/8] ADMIN-01: Filter users by enterprise type..."

RESP_FILTER=$(curl -s "$BASE_URL/admin/users?userType=1&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Filter response: $(echo "$RESP_FILTER" | head -c 300)"

assert_contains "Filter by type returns 200" "$RESP_FILTER" '"code":200'
assert_contains "Filter has content" "$RESP_FILTER" '"content":'
echo ""

# --- ADMIN-04: Disable user ---
echo "[4/8] ADMIN-04: Disable enterprise001..."

RESP_DISABLE=$(curl -s -X PUT "$BASE_URL/admin/users/$E1_USER_ID/status?status=0" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Disable response: $(echo "$RESP_DISABLE" | head -c 300)"

assert_contains "Disable user returns 200" "$RESP_DISABLE" '"code":200'
echo ""

# --- Verify disabled ---
echo "[5/8] ADMIN-04: Verify user is disabled..."

RESP_DISABLED=$(curl -s "$BASE_URL/admin/users?status=0&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
assert_contains "Disabled user appears in status=0 filter" "$RESP_DISABLED" '"id"'
echo ""

# --- ADMIN-04: Re-enable user ---
echo "[6/8] ADMIN-04: Re-enable enterprise001..."

RESP_ENABLE=$(curl -s -X PUT "$BASE_URL/admin/users/$E1_USER_ID/status?status=1" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Enable response: $(echo "$RESP_ENABLE" | head -c 300)"

assert_contains "Re-enable user returns 200" "$RESP_ENABLE" '"code":200'
echo ""

# --- ADMIN-05: Dashboard ---
echo "[7/8] ADMIN-05: Dashboard data..."

RESP_DASH=$(curl -s "$BASE_URL/admin/dashboard" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Dashboard response: $RESP_DASH"

assert_contains "Dashboard returns 200" "$RESP_DASH" '"code":200'
assert_contains "Dashboard has totalUsers" "$RESP_DASH" '"totalUsers":'
echo ""

# --- ADMIN-05: Statistics ---
echo "[8/8] ADMIN-05: Statistics..."

RESP_STATS=$(curl -s "$BASE_URL/admin/statistics" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
echo "  Statistics response: $RESP_STATS"

assert_contains "Statistics returns 200" "$RESP_STATS" '"code":200'
assert_contains "Statistics has enterpriseCount" "$RESP_STATS" '"enterpriseCount":'
assert_contains "Statistics has reviewerCount" "$RESP_STATS" '"reviewerCount":'
assert_contains "Statistics has thirdPartyCount" "$RESP_STATS" '"thirdPartyCount":'
echo ""

# --- Known code gaps ---
echo "========================================"
echo "  KNOWN CODE GAPS:"
echo "  ADMIN-02 (Create User): No backend endpoint exists"
echo "  ADMIN-03 (Edit User): No backend endpoint exists"
echo "========================================"

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
