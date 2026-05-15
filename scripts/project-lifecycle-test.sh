#!/bin/bash
# 04-01: Carbon Neutral Project Lifecycle Test
# Requirements: PROJ-01~05

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

echo "=== 04-01: Carbon Neutral Project Lifecycle ==="
echo ""

# ─── Authentication ───
echo "[1/6] Authenticating..."

login_user() {
    local username="$1"
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"admin123\"}"
}

RESP_E1=$(login_user "enterprise001")
TOKEN_E1=$(echo "$RESP_E1" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
E1_USER_ID=$(echo "$RESP_E1" | grep -o '"userId":[0-9]*' | head -1 | cut -d: -f2)

RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

echo "  enterprise001 token: ${TOKEN_E1:0:20}..."
echo "  admin token: ${TOKEN_ADMIN:0:20}..."
echo ""

# ─── PROJ-01: Create project + view list + view detail ───
echo "[2/6] PROJ-01: Project CRUD..."

# Create project (DRAFT)
RESP_CREATE=$(curl -s -X POST "$BASE_URL/carbon-neutral" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d '{
        "projectName": "Test Forest Carbon Sink Project",
        "projectType": 1,
        "description": "Test project for lifecycle verification",
        "location": "Beijing",
        "expectedReduction": 5000
    }')

PROJECT_ID=$(echo "$RESP_CREATE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
assert_contains "Create project (DRAFT)" "$RESP_CREATE" '"status":0'
echo "  Project ID: $PROJECT_ID"

# View my projects
RESP_MY=$(curl -s "$BASE_URL/carbon-neutral/my?page=1&size=20" \
    -H "Authorization: Bearer $TOKEN_E1")
assert_contains "View my projects list" "$RESP_MY" "$PROJECT_ID"

# View project detail
RESP_DETAIL=$(curl -s "$BASE_URL/carbon-neutral/$PROJECT_ID" \
    -H "Authorization: Bearer $TOKEN_E1")
assert_contains "View project detail" "$RESP_DETAIL" '"projectName":"Test Forest Carbon Sink Project"'
assert_contains "Project detail status=DRAFT" "$RESP_DETAIL" '"status":0'
echo ""

# ─── PROJ-02: Submit for review (DRAFT→PENDING) ───
echo "[3/6] PROJ-02: Submit for review..."

RESP_SUBMIT=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_ID/submit" \
    -H "Authorization: Bearer $TOKEN_E1")
assert_contains "Submit for review (DRAFT→PENDING)" "$RESP_SUBMIT" '"status":1'
echo ""

# ─── PROJ-03: Review — approve project A, reject project B ───
echo "[4/6] PROJ-03: Review (approve + reject)..."

# Create project B for rejection
RESP_CREATE_B=$(curl -s -X POST "$BASE_URL/carbon-neutral" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d '{
        "projectName": "Test Rejected Project",
        "projectType": 2,
        "description": "Will be rejected",
        "expectedReduction": 1000
    }')

PROJECT_B_ID=$(echo "$RESP_CREATE_B" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Project B ID: $PROJECT_B_ID"

# Submit project B for review
curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_B_ID/submit" \
    -H "Authorization: Bearer $TOKEN_E1" > /dev/null

# Approve project A
RESP_APPROVE=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_ID/review" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d '{"approved": true, "comment": "Approved for testing"}')
assert_contains "Approve project A (PENDING→APPROVED)" "$RESP_APPROVE" '"status":2'

# Reject project B
RESP_REJECT=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_B_ID/review" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d '{"approved": false, "comment": "Insufficient documentation"}')
assert_contains "Reject project B (PENDING→REJECTED)" "$RESP_REJECT" '"status":6'
echo ""

# ─── PROJ-04: Start implementation + terminate ───
echo "[5/6] PROJ-04: Start + Terminate..."

# Start implementation (APPROVED→IMPLEMENTING)
RESP_START=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_ID/start" \
    -H "Authorization: Bearer $TOKEN_E1")
assert_contains "Start implementation (APPROVED→IMPLEMENTING)" "$RESP_START" '"status":3'

# Terminate project (IMPLEMENTING→TERMINATED)
RESP_TERM=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_ID/terminate" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d '{"reason": "Testing termination flow"}')
assert_contains "Terminate project (IMPLEMENTING→TERMINATED)" "$RESP_TERM" '"status":5'
echo ""

# ─── PROJ-05: Verification + Certification (ADMIN role) ───
echo "[6/6] PROJ-05: Verification + Certification..."

# Create project C for full verification/certification lifecycle
RESP_CREATE_C=$(curl -s -X POST "$BASE_URL/carbon-neutral" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d '{
        "projectName": "Test Full Lifecycle Project",
        "projectType": 3,
        "description": "Full lifecycle including verification and certification",
        "expectedReduction": 3000
    }')

PROJECT_C_ID=$(echo "$RESP_CREATE_C" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Project C ID: $PROJECT_C_ID"

# Submit → Approve → Start
curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/submit" \
    -H "Authorization: Bearer $TOKEN_E1" > /dev/null

curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/review" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d '{"approved": true, "comment": "Approved"}' > /dev/null

curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/start" \
    -H "Authorization: Bearer $TOKEN_E1" > /dev/null

echo "  Project C status: IMPLEMENTING"

# Submit for verification (assign verifier)
RESP_SUB_VER=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/submit-verification" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d "{\"verifierId\": $E1_USER_ID}")
assert_contains "Submit for verification" "$RESP_SUB_VER" '"verificationStatus":1'

# Verify as ADMIN
RESP_VERIFY_ADMIN=$(curl -s -X POST "$BASE_URL/carbon-neutral/verify" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d "{\"projectId\": $PROJECT_C_ID, \"verifiedReduction\": 3000, \"verificationReport\": \"Verified by admin\", \"monitoringData\": \"{}\"}")
assert_contains "Verify as ADMIN" "$RESP_VERIFY_ADMIN" '"verificationStatus":2'
assert_contains "Verify issues credits" "$RESP_VERIFY_ADMIN" '"issuedCredits":3000'

# Apply for certification
RESP_CERT_APPLY=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/apply-certification" \
    -H "Authorization: Bearer $TOKEN_E1" \
    -H "Content-Type: application/json" \
    -d '{"certOrg": "TestCertOrg"}')
assert_contains "Apply for certification" "$RESP_CERT_APPLY" '"certStatus":1'

# Certify as ADMIN
RESP_CERT_ADMIN=$(curl -s -X POST "$BASE_URL/carbon-neutral/$PROJECT_C_ID/certify" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H "Content-Type: application/json" \
    -d '{"certNo": "CERT-TEST-001"}')
assert_contains "Certify as ADMIN" "$RESP_CERT_ADMIN" '"certStatus":2'
assert_contains "Certification number set" "$RESP_CERT_ADMIN" '"certNo":"CERT-TEST-001"'
echo ""

# ─── Summary ───
echo "========================================"
echo "Results: $PASS passed, $FAIL failed (total: $TEST_ID tests)"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
