#!/bin/bash
# 05-01: Digital Signatures - RSA Keypair + Sign + Verify + Revoke
# Requirements: SIGN-01, SIGN-02, SIGN-03

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

assert_not_contains() {
    local test_name="$1" response="$2" expected="$3"
    TEST_ID=$((TEST_ID + 1))
    if ! echo "$response" | grep -q "$expected"; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name — did NOT expect '$expected' in response"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== 05-01: Digital Signatures (SIGN-01~03) ==="
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

# Get enterprise001's userId
E1_USER_ID=$(echo "$RESP_E1" | grep -o '"userId":[0-9]*' | head -1 | cut -d: -f2)
echo "  enterprise001 userId: $E1_USER_ID"
echo ""

# --- Cleanup any pre-existing keypairs (backend has NonUniqueResultException bug with multiple keypairs) ---
echo "[pre] Cleanup: Removing old keypairs for enterprise001..."
mysql -h 127.0.0.1 -P 3306 -u root -p123456 oaiss_chain -e "DELETE FROM rsa_key_pair WHERE user_id=$E1_USER_ID" 2>/dev/null || true
echo ""

# --- SIGN-01: RSA Keypair Generation ---
echo "[2/7] SIGN-01: Generate RSA keypair..."

RESP_GEN=$(curl -s -X POST "$BASE_URL/signature/keypair/generate" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Generate response: $(echo "$RESP_GEN" | head -c 300)"

assert_contains "Keypair generation returns 200" "$RESP_GEN" '"code":200'
assert_contains "Keypair has publicKey" "$RESP_GEN" '"publicKey":'
assert_contains "Keypair has id" "$RESP_GEN" '"id":'

KEY_ID=$(echo "$RESP_GEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Generated keypair id: $KEY_ID"
echo ""

# --- Get keypair info ---
echo "[3/7] SIGN-01: Get keypair info..."

RESP_GET=$(curl -s "$BASE_URL/signature/keypair" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Get keypair response: $(echo "$RESP_GET" | head -c 300)"

assert_contains "Get keypair returns 200" "$RESP_GET" '"code":200'
assert_contains "Get keypair has publicKey" "$RESP_GET" '"publicKey":'
assert_not_contains "Get keypair does NOT expose privateKey" "$RESP_GET" '"privateKey":'
echo ""

# --- DB check: rsa_key_pairs table ---
echo "[4/7] DB check: rsa_key_pairs for enterprise001..."

DB_CHECK=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 oaiss_chain -sNe \
    "SELECT COUNT(*) FROM rsa_key_pair WHERE user_id=$E1_USER_ID" 2>/dev/null || echo "0")
if [ "$DB_CHECK" -gt 0 ]; then
    TEST_ID=$((TEST_ID + 1))
    echo "  [PASS] Test $TEST_ID: rsa_key_pairs has record for enterprise001 (count=$DB_CHECK)"
    PASS=$((PASS + 1))
else
    TEST_ID=$((TEST_ID + 1))
    echo "  [FAIL] Test $TEST_ID: rsa_key_pairs has NO record for enterprise001"
    FAIL=$((FAIL + 1))
fi
echo ""

# --- SIGN-02: Sign report data ---
echo "[5/7] SIGN-02: Sign report data..."

REPORT_DATA='{"reportName":"test-report-2025","totalEmission":1500.50,"period":"2025-Q1"}'
RESP_SIGN=$(curl -s -X POST "$BASE_URL/signature/sign" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d "$REPORT_DATA")
echo "  Sign response: $(echo "$RESP_SIGN" | head -c 300)"

assert_contains "Sign returns 200" "$RESP_SIGN" '"code":200'
assert_contains "Sign has signature data" "$RESP_SIGN" '"signature":'

SIGNATURE=$(echo "$RESP_SIGN" | grep -o '"signature":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  Signature length: ${#SIGNATURE}"
echo ""

# --- SIGN-03: Verify signature (valid) ---
echo "[6/7] SIGN-03: Verify signature (valid + tampered)..."

# Verify with correct data — reportData is a String field, must be JSON-encoded string
REPORT_DATA_STR=$(echo "$REPORT_DATA" | sed 's/"/\\"/g')
VERIFY_BODY="{\"reportId\":1,\"signatureData\":\"$SIGNATURE\",\"reportData\":\"$REPORT_DATA_STR\",\"signerId\":$E1_USER_ID}"
RESP_VERIFY=$(curl -s -X POST "$BASE_URL/signature/verify" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d "$VERIFY_BODY")
echo "  Verify (valid) response: $(echo "$RESP_VERIFY" | head -c 300)"

assert_contains "Verify valid signature returns 200" "$RESP_VERIFY" '"code":200'
assert_contains "Valid signature returns true" "$RESP_VERIFY" '"valid":true'

# Verify with tampered data — different report data, same signature
TAMPERED_BODY="{\"reportId\":1,\"signatureData\":\"$SIGNATURE\",\"reportData\":\"{\\\"reportName\\\":\\\"TAMPERED\\\",\\\"totalEmission\\\":99999}\",\"signerId\":$E1_USER_ID}"
RESP_TAMPERED=$(curl -s -X POST "$BASE_URL/signature/verify" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d "$TAMPERED_BODY")
echo "  Verify (tampered) response: $(echo "$RESP_TAMPERED" | head -c 300)"

assert_contains "Tampered data verification returns 200" "$RESP_TAMPERED" '"code":200'
assert_contains "Tampered data returns false" "$RESP_TAMPERED" '"valid":false'
echo ""

# --- Cleanup: Revoke keypair ---
echo "[7/7] Cleanup: Revoke keypair..."

RESP_REVOKE=$(curl -s -X DELETE "$BASE_URL/signature/keypair" \
    -H "Authorization: Bearer $TOKEN_E1")
echo "  Revoke response: $(echo "$RESP_REVOKE" | head -c 300)"

assert_contains "Revoke keypair returns 200" "$RESP_REVOKE" '"code":200'
echo ""

# --- Summary ---
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
