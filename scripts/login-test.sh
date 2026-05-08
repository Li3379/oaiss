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

# Verify backend is up first
info "Checking backend availability..."
curl -sf "$API/swagger-ui.html" -o /dev/null || { fail "Backend not running. Start it first: cd oaiss-chain-backend && mvn spring-boot:run"; exit 1; }
ok "Backend is reachable"

TOTAL=0
PASSED=0
FAILED=0

# Define test accounts: username password expected_userType expected_roleName
ACCOUNTS=(
  "admin:admin123:4:ADMIN"
  "enterprise001:admin123:1:ENTERPRISE"
  "enterprise002:admin123:1:ENTERPRISE"
  "enterprise003:admin123:1:ENTERPRISE"
  "reviewer001:admin123:2:REVIEWER"
  "thirdparty001:admin123:3:THIRD_PARTY"
  "authenticator001:admin123:5:AUTHENTICATOR"
)

for entry in "${ACCOUNTS[@]}"; do
  IFS=':' read -r username password expected_type expected_role <<< "$entry"
  TOTAL=$((TOTAL + 1))

  echo ""
  info "Testing account: $username (expected userType=$expected_type, role=$expected_role)"

  # Step 1: Login
  LOGIN_RESP=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}")

  LOGIN_CODE=$(echo "$LOGIN_RESP" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [[ "$LOGIN_CODE" != "200" ]]; then
    fail "$username: Login failed (code=$LOGIN_CODE, response=$LOGIN_RESP)"
    FAILED=$((FAILED + 1))
    continue
  fi

  # Extract token and userType
  TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  ACTUAL_TYPE=$(echo "$LOGIN_RESP" | grep -o '"userType":[0-9]*' | head -1 | cut -d: -f2)

  if [[ -z "$TOKEN" ]]; then
    fail "$username: No accessToken in response"
    FAILED=$((FAILED + 1))
    continue
  fi
  ok "$username: Login successful (token received, userType=$ACTUAL_TYPE)"

  # Step 2: Verify userType matches expected
  if [[ "$ACTUAL_TYPE" != "$expected_type" ]]; then
    fail "$username: userType mismatch (expected=$expected_type, got=$ACTUAL_TYPE)"
    FAILED=$((FAILED + 1))
    continue
  fi
  ok "$username: userType matches expected ($expected_type)"

  # Step 3: Access protected endpoint with token
  ME_RESP=$(curl -s "$API/auth/me" -H "Authorization: Bearer $TOKEN")
  ME_CODE=$(echo "$ME_RESP" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [[ "$ME_CODE" != "200" ]]; then
    fail "$username: /auth/me failed with Bearer token (code=$ME_CODE)"
    FAILED=$((FAILED + 1))
    continue
  fi
  ok "$username: Bearer token works on /auth/me"

  # Step 4: Logout
  LOGOUT_RESP=$(curl -s -X POST "$API/auth/logout" -H "Authorization: Bearer $TOKEN")
  LOGOUT_CODE=$(echo "$LOGOUT_RESP" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [[ "$LOGOUT_CODE" != "200" ]]; then
    fail "$username: Logout failed (code=$LOGOUT_CODE)"
    FAILED=$((FAILED + 1))
    continue
  fi
  ok "$username: Logout successful"

  # Step 5: Verify token is blacklisted
  BLACKLIST_RESP=$(curl -s "$API/auth/me" -H "Authorization: Bearer $TOKEN")
  BLACKLIST_CODE=$(echo "$BLACKLIST_RESP" | grep -o '"code":[0-9]*' | head -1 | cut -d: -f2)
  if [[ "$BLACKLIST_CODE" == "200" ]]; then
    fail "$username: Token still valid after logout (blacklist not working)"
    FAILED=$((FAILED + 1))
    continue
  fi
  ok "$username: Token blacklisted after logout (subsequent request returns $BLACKLIST_CODE)"

  PASSED=$((PASSED + 1))
done

echo ""
echo -e "========================================"
echo -e " Login Test Results"
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
