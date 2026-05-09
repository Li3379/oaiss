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
TIMESTAMP=$(date +%s)

TOTAL=0
PASSED=0
FAILED=0

# --- Helper: extract JSON field value via grep ---
extract_field() {
  local json="$1" field="$2"
  echo "$json" | grep -o "\"$field\":[^,}]*" | head -1 | sed "s/\"$field\"://" | tr -d '"'
}

# --- Verify backend is up ---
info "Checking backend availability..."
curl -sf "$API/swagger-ui.html" -o /dev/null || { fail "Backend not running. Start it first: cd oaiss-chain-backend && mvn spring-boot:run"; exit 1; }
ok "Backend is reachable"

# --- Login helpers ---
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

# --- Step 0: Login as enterprise001 and enterprise002 ---
info "Logging in as enterprise001..."
TOKEN_E1=$(login "enterprise001") || { fail "Cannot proceed without enterprise001 token"; exit 1; }
ok "enterprise001 logged in"

info "Logging in as enterprise002..."
TOKEN_E2=$(login "enterprise002") || { fail "Cannot proceed without enterprise002 token"; exit 1; }
ok "enterprise002 logged in"

# --- Step 1: [CARB-01] File upload (best-effort) ---
info "[CARB-01] Uploading test file..."
UPLOAD_FILE=$(mktemp)
echo "Carbon report test evidence - $TIMESTAMP" > "$UPLOAD_FILE"

UPLOAD_RESP=$(curl -s -X POST "$API/file/upload" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -F "file=@$UPLOAD_FILE;filename=test-evidence.txt;type=text/plain" || true)

UPLOAD_CODE=$(extract_field "$UPLOAD_RESP" "code")
OBJECT_NAME=""

if [[ "$UPLOAD_CODE" == "200" ]]; then
  OBJECT_NAME=$(extract_field "$UPLOAD_RESP" "objectName")
  ok "File uploaded, objectName=$OBJECT_NAME"
else
  info "File upload failed or MinIO unavailable (code=$UPLOAD_CODE), proceeding without attachment"
fi
rm -f "$UPLOAD_FILE"

# --- Step 2: [CARB-01] Create reports ---
info "[CARB-01] Creating carbon reports..."

EMISSION_DATA='{"scope1":[{"name":"gas","activity_data":1000,"emission_factor":2.0}],"scope2":[{"name":"electricity","activity_data":5000,"emission_factor":0.6}],"scope3":[{"name":"travel","activity_data":200,"emission_factor":0.15}]}'

ATTACHMENTS="null"
if [[ -n "$OBJECT_NAME" ]]; then
  ATTACHMENTS="[\"$OBJECT_NAME\"]"
fi

# Create report1 (enterprise001, will be approved in Plan 02-02)
REPORT1_RESP=$(curl -s -X POST "$API/carbon/reports" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"CARB-TEST-APPROVE-$TIMESTAMP\",\"accountingPeriod\":\"2024-Q1\",\"reportType\":1,\"emissionData\":\"${EMISSION_DATA}\",\"calculationMethod\":\"manual\",\"attachments\":$ATTACHMENTS}")

REPORT1_CODE=$(extract_field "$REPORT1_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$REPORT1_CODE" == "200" ]]; then
  REPORT1_ID=$(extract_field "$REPORT1_RESP" "id")
  ok "Report1 created (id=$REPORT1_ID, title=CARB-TEST-APPROVE-$TIMESTAMP)"
  PASSED=$((PASSED + 1))
else
  fail "Report1 creation failed (code=$REPORT1_CODE)"
  echo "$REPORT1_RESP" >&2
  FAILED=$((FAILED + 1))
fi

# Create report2 (enterprise001, will be rejected in Plan 02-02)
REPORT2_RESP=$(curl -s -X POST "$API/carbon/reports" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"CARB-TEST-REJECT-$TIMESTAMP\",\"accountingPeriod\":\"2024-Q2\",\"reportType\":1,\"emissionData\":\"${EMISSION_DATA}\",\"calculationMethod\":\"manual\",\"attachments\":null}")

REPORT2_CODE=$(extract_field "$REPORT2_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$REPORT2_CODE" == "200" ]]; then
  REPORT2_ID=$(extract_field "$REPORT2_RESP" "id")
  ok "Report2 created (id=$REPORT2_ID, title=CARB-TEST-REJECT-$TIMESTAMP)"
  PASSED=$((PASSED + 1))
else
  fail "Report2 creation failed (code=$REPORT2_CODE)"
  echo "$REPORT2_RESP" >&2
  FAILED=$((FAILED + 1))
fi

# Create report3 (enterprise002, stays DRAFT -- tests data isolation)
REPORT3_RESP=$(curl -s -X POST "$API/carbon/reports" \
  -H "Authorization: Bearer $TOKEN_E2" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"CARB-TEST-ISOLATION-$TIMESTAMP\",\"accountingPeriod\":\"2024-Q3\",\"reportType\":2,\"emissionData\":\"${EMISSION_DATA}\",\"calculationMethod\":\"manual\",\"attachments\":null}")

REPORT3_CODE=$(extract_field "$REPORT3_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$REPORT3_CODE" == "200" ]]; then
  REPORT3_ID=$(extract_field "$REPORT3_RESP" "id")
  ok "Report3 created (id=$REPORT3_ID, title=CARB-TEST-ISOLATION-$TIMESTAMP)"
  PASSED=$((PASSED + 1))
else
  fail "Report3 creation failed (code=$REPORT3_CODE)"
  echo "$REPORT3_RESP" >&2
  FAILED=$((FAILED + 1))
fi

# --- Step 3: [CARB-02] List my-reports for enterprise001 ---
info "[CARB-02] Listing enterprise001 reports..."
LIST_RESP=$(curl -s "$API/carbon/my-reports?pageNum=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN_E1")

LIST_CODE=$(extract_field "$LIST_RESP" "code")
TOTAL=$((TOTAL + 1))
if [[ "$LIST_CODE" != "200" ]]; then
  fail "List my-reports failed (code=$LIST_CODE)"
  FAILED=$((FAILED + 1))
else
  # Verify report1 and report2 appear, report3 does NOT
  FOUND1=0; FOUND2=0; FOUND3=0
  [[ -n "${REPORT1_ID:-}" ]] && echo "$LIST_RESP" | grep -q "\"id\":$REPORT1_ID" && FOUND1=1
  [[ -n "${REPORT2_ID:-}" ]] && echo "$LIST_RESP" | grep -q "\"id\":$REPORT2_ID" && FOUND2=1
  [[ -n "${REPORT3_ID:-}" ]] && echo "$LIST_RESP" | grep -q "\"id\":$REPORT3_ID" && FOUND3=1

  if [[ "$FOUND1" -eq 1 && "$FOUND2" -eq 1 && "$FOUND3" -eq 0 ]]; then
    ok "List contains report1 ($REPORT1_ID) and report2 ($REPORT2_ID), but NOT report3 (data isolation OK)"
    PASSED=$((PASSED + 1))
  else
    fail "List check failed: found1=$FOUND1 found2=$FOUND2 found3=$FOUND3 (expected 1,1,0)"
    FAILED=$((FAILED + 1))
  fi
fi

# --- Step 4: [CARB-03] Get report1 detail ---
info "[CARB-03] Fetching report1 detail..."
TOTAL=$((TOTAL + 1))
if [[ -z "${REPORT1_ID:-}" ]]; then
  fail "Cannot fetch detail: report1 ID not available"
  FAILED=$((FAILED + 1))
else
  DETAIL_RESP=$(curl -s "$API/carbon/reports/$REPORT1_ID" \
    -H "Authorization: Bearer $TOKEN_E1")

  DETAIL_CODE=$(extract_field "$DETAIL_RESP" "code")
  if [[ "$DETAIL_CODE" != "200" ]]; then
    fail "Get report detail failed (code=$DETAIL_CODE)"
    FAILED=$((FAILED + 1))
  else
    DETAIL_ID=$(extract_field "$DETAIL_RESP" "id")
    DETAIL_STATUS=$(extract_field "$DETAIL_RESP" "status")
    HAS_EMISSION=$(echo "$DETAIL_RESP" | grep -o '"emissionData"' | head -1)

    if [[ "$DETAIL_ID" == "$REPORT1_ID" && "$DETAIL_STATUS" == "0" && -n "$HAS_EMISSION" ]]; then
      ok "Report1 detail: id=$DETAIL_ID status=$DETAIL_STATUS emissionData=present"
      PASSED=$((PASSED + 1))
    else
      fail "Report1 detail mismatch: id=$DETAIL_ID (expected $REPORT1_ID) status=$DETAIL_STATUS (expected 0) emissionData=$HAS_EMISSION"
      FAILED=$((FAILED + 1))
    fi
  fi
fi

# --- Step 5: [CARB-04] Submit report1 and report2 ---
info "[CARB-04] Submitting report1..."

submit_report() {
  local rid="$1" label="$2"
  local resp=$(curl -s -X POST "$API/carbon/reports/$rid/submit" \
    -H "Authorization: Bearer $TOKEN_E1")

  local code=$(extract_field "$resp" "code")
  TOTAL=$((TOTAL + 1))
  if [[ "$code" != "200" ]]; then
    fail "$label submission failed (code=$code)"
    echo "$resp" >&2
    FAILED=$((FAILED + 1))
    return
  fi

  local new_status=$(extract_field "$resp" "status")
  local total_emission=$(extract_field "$resp" "totalEmission")

  if [[ "$new_status" == "1" && -n "$total_emission" && "$total_emission" != "0" ]]; then
    ok "$label submitted: status=$new_status totalEmission=$total_emission"
    PASSED=$((PASSED + 1))
  else
    fail "$label submit check failed: status=$new_status (expected 1) totalEmission=$total_emission"
    FAILED=$((FAILED + 1))
  fi
}

if [[ -n "${REPORT1_ID:-}" ]]; then
  submit_report "$REPORT1_ID" "Report1"
else
  TOTAL=$((TOTAL + 1))
  fail "Report1 submit skipped: no ID"
  FAILED=$((FAILED + 1))
fi

info "[CARB-04] Submitting report2..."
if [[ -n "${REPORT2_ID:-}" ]]; then
  submit_report "$REPORT2_ID" "Report2"
else
  TOTAL=$((TOTAL + 1))
  fail "Report2 submit skipped: no ID"
  FAILED=$((FAILED + 1))
fi

# --- Summary ---
echo ""
echo -e "========================================"
echo -e " Carbon Report Test Results (CARB-01..04)"
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
