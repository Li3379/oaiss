#!/bin/bash
# 05-06: Third-Party Monitoring - OrgInfo/Statistics/CarbonReports/Contact
# Requirements: TP-01, TP-02 (partial)

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

echo "=== 05-06: Third-Party Monitoring (TP-01~02) ==="
echo ""

# --- Authentication ---
echo "[1/6] Authenticating..."

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"admin123\"}"
}

RESP_TP=$(login_user "thirdparty001")
TOKEN_TP=$(echo "$RESP_TP" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  thirdparty001 token: ${TOKEN_TP:0:20}..."
echo ""

# --- TP-01: Org info ---
echo "[2/6] TP-01: Get org info..."

RESP_ORG=$(curl -s "$BASE_URL/third-party/org-info" \
    -H "Authorization: Bearer $TOKEN_TP")
echo "  Org info response: $(echo "$RESP_ORG" | head -c 400)"

assert_contains "Org info returns 200" "$RESP_ORG" '"code":200'
assert_contains "Org info has data" "$RESP_ORG" '"data":'
echo ""

# --- TP-01: Statistics ---
echo "[3/6] TP-01: Get monitoring statistics..."

RESP_STATS=$(curl -s "$BASE_URL/third-party/statistics" \
    -H "Authorization: Bearer $TOKEN_TP")
echo "  Statistics response: $(echo "$RESP_STATS" | head -c 400)"

assert_contains "Statistics returns 200" "$RESP_STATS" '"code":200'
assert_contains "Statistics has data" "$RESP_STATS" '"data":'
echo ""

# --- TP-01: Carbon reports (monitoring perspective) ---
echo "[4/6] TP-01/TP-02: Query carbon reports..."

RESP_REPORTS=$(curl -s "$BASE_URL/third-party/carbon-reports?page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_TP")
echo "  Reports response: $(echo "$RESP_REPORTS" | head -c 400)"

assert_contains "Carbon reports returns 200" "$RESP_REPORTS" '"code":200'
echo ""

# --- Filter by status ---
echo "[5/6] TP-01: Filter carbon reports by status=2..."

RESP_FILTERED=$(curl -s "$BASE_URL/third-party/carbon-reports?status=2&page=1&size=10" \
    -H "Authorization: Bearer $TOKEN_TP")
echo "  Filtered response: $(echo "$RESP_FILTERED" | head -c 400)"

assert_contains "Filtered reports returns 200" "$RESP_FILTERED" '"code":200'
echo ""

# --- Update contact ---
echo "[6/6] TP-01: Update contact info..."

RESP_CONTACT=$(curl -s -X PUT "$BASE_URL/third-party/contact?contactPerson=test_auditor&contactPhone=13800138000" \
    -H "Authorization: Bearer $TOKEN_TP")
echo "  Contact update response: $(echo "$RESP_CONTACT" | head -c 300)"

assert_contains "Update contact returns 200" "$RESP_CONTACT" '"code":200'
echo ""

# --- Gap notice ---
echo "========================================"
echo "  TP-02 (Trade Audit): PARTIAL COVERAGE"
echo "  - /carbon-reports used as proxy for monitoring"
echo "  - No dedicated trade audit endpoint exists"
echo "========================================"

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
