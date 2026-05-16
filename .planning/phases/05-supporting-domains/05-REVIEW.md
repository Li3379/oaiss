---
phase: 05-supporting-domains
reviewed: 2026-05-16T12:00:00Z
depth: deep
files_reviewed: 7
files_reviewed_list:
  - scripts/admin-test.sh
  - scripts/blockchain-test.sh
  - scripts/emission-test.sh
  - scripts/file-test.sh
  - scripts/search-test.sh
  - scripts/sign-test.sh
  - scripts/thirdparty-test.sh
findings:
  critical: 3
  warning: 8
  info: 5
  total: 16
status: issues_found
---

# Phase 5: Code Review Report

**Reviewed:** 2026-05-16T12:00:00Z
**Depth:** deep
**Files Reviewed:** 7
**Status:** issues_found

## Summary

Seven bash test scripts were reviewed at deep depth, covering digital signatures, file management, emission ratings, blockchain explorer, admin management, third-party monitoring, and cross-entity search. The scripts follow a curl-based pattern with login, API calls, and substring-match assertions.

The most critical finding is **hardcoded database root credentials** in two scripts (sign-test.sh, emission-test.sh), which directly writes the password `123456` into source code and into shell process listings. Beyond security, several scripts have **no validation that login succeeded** before using the extracted token, meaning a failed login silently cascades into misleading test failures. The `assert_contains` function is fundamentally too loose -- `grep -q '"code":200'` matches `"code":2000` and `"code":2001` as well, producing false positives. Cross-script analysis reveals heavy code duplication (the same `assert_contains`, `login_user`, and counter boilerplate is copy-pasted 7 times), no shared helper library, and inconsistent cleanup behavior.

## Critical Issues

### CR-01: Hardcoded Database Root Password in Source Code

**File:** `scripts/sign-test.sh:66,99` and `scripts/emission-test.sh:75`
**Issue:** Both scripts embed the MySQL root password `123456` directly in `mysql` command invocations. This password is committed to version control, visible in `ps` output to all users on the system, and stored in shell history. The same pattern appears in sign-test.sh lines 66 and 99, and emission-test.sh line 75.

**Fix:**
```bash
# At top of script, read from environment variable with a fallback
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:?Database password required. Set DB_PASS environment variable.}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-oaiss_chain}"

# Use --defaults-extra-file to avoid password in process listing
MYSQL_CNF=$(mktemp)
trap "rm -f $MYSQL_CNF" EXIT
cat > "$MYSQL_CNF" <<EOF
[client]
user=$DB_USER
password=$DB_PASS
host=$DB_HOST
port=$DB_PORT
EOF

mysql --defaults-extra-file="$MYSQL_CNF" "$DB_NAME" -sNe "SELECT COUNT(*) FROM rsa_key_pair WHERE user_id=$E1_USER_ID"
```

### CR-02: SQL Injection via Unvalidated Variable Interpolation in DB Queries

**File:** `scripts/sign-test.sh:66,99-100` and `scripts/emission-test.sh:75-76`
**Issue:** Variables `$E1_USER_ID` and `$E1_ENTERPRISE_ID` are interpolated directly into raw SQL strings without any validation. These variables are extracted from JSON API responses using fragile `grep | cut` pipelines. If the extraction fails or returns unexpected data (e.g., empty string, or a value containing SQL metacharacters), the SQL query will either break silently or execute unintended commands. With `set -e` active, the `|| echo "0"` fallback on emission-test.sh line 76 may mask the real error.

**Fix:**
```bash
# Validate the extracted ID is a positive integer before using in SQL
if ! [[ "$E1_USER_ID" =~ ^[0-9]+$ ]]; then
    echo "  [ERROR] Invalid userId extracted: '$E1_USER_ID'"
    exit 1
fi

# Then use in query (integer-only is safe for interpolation)
mysql ... -sNe "SELECT COUNT(*) FROM rsa_key_pair WHERE user_id=$E1_USER_ID"
```

### CR-03: assert_contains False Positives -- Substring Match on `"code":200`

**File:** All 7 scripts (every `assert_contains` call checking for `'"code":200'`)
**Issue:** `grep -q '"code":200'` uses substring matching, which also matches `"code":2000`, `"code":2001`, `"code":20099`, or any string containing `200` as a substring of the code value. This means tests can pass when the API actually returns a non-200 status code, producing false positives that silently mask real failures. For example, an API returning `{"code":2001,"message":"Unauthorized"}` would still pass the `"code":200` assertion.

**Fix:**
```bash
assert_contains() {
    local test_name="$1" response="$2" expected="$3"
    TEST_ID=$((TEST_ID + 1))
    # Use -F for fixed-string match, or use a more precise pattern
    if echo "$response" | grep -qF "$expected"; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name -- expected '$expected' in response"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}

# For code checks specifically, use a precise pattern:
assert_code_200() {
    local test_name="$1" response="$2"
    TEST_ID=$((TEST_ID + 1))
    if echo "$response" | grep -qE '"code"\s*:\s*200[^0-9]'; then
        echo "  [PASS] Test $TEST_ID: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] Test $TEST_ID: $test_name -- expected code 200"
        echo "    Response: $(echo "$response" | head -c 500)"
        FAIL=$((FAIL + 1))
    fi
}
```

## Warnings

### WR-01: No Token Validation After Login -- Empty Token Cascades Silently

**File:** All 7 scripts (every `login_user` / token extraction block)
**Issue:** After extracting the token via `grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4`, no script checks whether the result is empty. If login fails (server down, wrong password, user doesn't exist), `TOKEN_*` will be an empty string. Subsequent `curl` calls will send `Authorization: Bearer ` (empty), which may either hit unauthenticated endpoints and return misleading 200s, or produce confusing 401/403 errors that don't point to the real cause (login failure).

**Fix:**
```bash
RESP_ADMIN=$(login_user "admin")
TOKEN_ADMIN=$(echo "$RESP_ADMIN" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$TOKEN_ADMIN" ]; then
    echo "  [FATAL] Login failed for admin. Response: $RESP_ADMIN"
    exit 1
fi
echo "  admin token: ${TOKEN_ADMIN:0:20}..."
```

### WR-02: No Dependency Checks -- Silent Failures When Tools Missing

**File:** All 7 scripts
**Issue:** None of the scripts check for required external tools (`curl`, `grep`, `cut`, `head`, `date`). Additionally, `admin-test.sh` depends on `python3` (line 55-62) without checking if it's available. The `emission-test.sh` and `sign-test.sh` depend on the `mysql` client. If any tool is missing, the failure mode is an unhelpful "command not found" error from `set -e` rather than a clear diagnostic.

**Fix:**
```bash
# Add at top of each script after set -euo pipefail
for cmd in curl grep cut head; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "[FATAL] Required command '$cmd' not found"; exit 1; }
done

# In admin-test.sh, additionally:
command -v python3 >/dev/null 2>&1 || { echo "[FATAL] python3 required for JSON parsing"; exit 1; }

# In sign-test.sh and emission-test.sh, additionally:
command -v mysql >/dev/null 2>&1 || { echo "[FATAL] mysql client required for DB checks"; exit 1; }
```

### WR-03: Fragile JSON Parsing via grep/cut -- Breaks on Minor API Changes

**File:** All 7 scripts
**Issue:** Every token extraction, ID extraction, and field check uses `grep -o '"fieldName":"[^"]*"' | cut -d'"' -f4` or similar. This is extremely brittle: if the API adds whitespace (`"fieldName" : "value"`), changes field order, or wraps values in nested objects, all extractions silently return empty strings. The admin-test.sh script (lines 55-62) already acknowledges this by falling back to python3 for one case -- but then uses the same fragile grep/cut everywhere else.

**Fix:**
```bash
# Prefer jq if available, with grep/cut fallback
extract_field() {
    local json="$1" field="$2"
    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r ".$field // empty" 2>/dev/null
    else
        echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
    fi
}

TOKEN_E1=$(extract_field "$RESP_E1" "accessToken")
```

### WR-04: No Trap-Based Cleanup -- Temp Files and DB Records Left on Failure

**File:** `scripts/sign-test.sh` (DB cleanup), `scripts/file-test.sh` (temp files)
**Issue:** sign-test.sh inserts data into `rsa_key_pair` and revokes at the end, but if the script exits early (due to `set -e`), the DB records are never cleaned up. Similarly, file-test.sh creates `/tmp/oaiss-test-file-*.txt` and cleans up at line 166, but an early exit leaves orphan files. Neither script uses `trap` for cleanup.

**Fix:**
```bash
# In file-test.sh, after creating the temp file:
TEST_FILE="/tmp/oaiss-test-file-$(date +%s).txt"
echo "OAISS CHAIN file management test - $(date)" > "$TEST_FILE"
trap 'rm -f "$TEST_FILE" /tmp/oaiss-downloaded.txt 2>/dev/null' EXIT

# In sign-test.sh, after the cleanup SQL:
trap 'mysql ... -e "DELETE FROM rsa_key_pair WHERE user_id=$E1_USER_ID" 2>/dev/null' EXIT
```

### WR-05: Hardcoded Login Password for All Users

**File:** All 7 scripts (in `login_user` function)
**Issue:** The `login_user` function hardcodes `"password":"admin123"` for every user (admin, enterprise001, thirdparty001). This couples all test scripts to a specific seed data password. If the test environment changes the password, all 7 scripts break simultaneously with no clear error message (see WR-01 about missing token validation).

**Fix:**
```bash
TEST_PASSWORD="${TEST_PASSWORD:-admin123}"

login_user() {
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$1\",\"password\":\"$TEST_PASSWORD\"}"
}
```

### WR-06: sign-test.sh Cleanup Step Mutates Test Counters But Is Not a Test

**File:** `scripts/sign-test.sh:157-163`
**Issue:** The "Cleanup: Revoke keypair" step at line 157 calls `assert_contains`, which increments `TEST_ID` and `PASS`/`FAIL`. This means the revoke operation is counted as a test in the summary. If revoke fails, it counts as a test failure, but the actual signature tests (SIGN-01 through SIGN-03) may have all passed. This conflates cleanup success with test success.

**Fix:**
```bash
# Don't count cleanup as a test -- use a separate function or skip assertion counting
echo "[7/7] Cleanup: Revoke keypair..."
RESP_REVOKE=$(curl -s -X DELETE "$BASE_URL/signature/keypair" \
    -H "Authorization: Bearer $TOKEN_E1")
if echo "$RESP_REVOKE" | grep -q '"code":200'; then
    echo "  [OK] Keypair revoked"
else
    echo "  [WARN] Keypair revoke failed (may need manual cleanup): $RESP_REVOKE"
fi
```

### WR-07: emission-test.sh DB Check Uses || echo "0" Masking Real Errors

**File:** `scripts/emission-test.sh:75-76`
**Issue:** The line `DB_COUNT=$(mysql ... 2>/dev/null || echo "0")` suppresses all mysql errors (including connection refused, auth failure, wrong table name) and silently substitutes `0`. Combined with `set -e`, this means a database connectivity issue will be reported as "emission_rating has NO records" -- a misleading test failure that sends the developer debugging in the wrong direction.

**Fix:**
```bash
DB_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -sNe \
    "SELECT COUNT(*) FROM emission_rating WHERE enterprise_id=$E1_ENTERPRISE_ID" 2>&1) || {
    echo "  [ERROR] MySQL query failed: $DB_COUNT"
    DB_COUNT="0"
}

# Validate it's actually a number
if ! [[ "$DB_COUNT" =~ ^[0-9]+$ ]]; then
    echo "  [ERROR] Unexpected DB response: $DB_COUNT"
    DB_COUNT="0"
fi
```

## Info

### IN-01: Massive Code Duplication Across All 7 Scripts

**File:** All 7 scripts
**Issue:** The `assert_contains` function (12 lines), `login_user` function (5 lines), counter variables (`PASS`, `FAIL`, `TEST_ID`), and summary block are identically copy-pasted in all 7 scripts. sign-test.sh also adds `assert_not_contains`. This violates DRY and means any fix (like CR-03) must be applied 7 times.

**Fix:** Extract a shared helper library:
```bash
# scripts/test-helpers.sh
#!/bin/bash
PASS=0; FAIL=0; TEST_ID=0
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"

assert_contains() { ... }
assert_not_contains() { ... }
login_user() { ... }
print_summary() { ... }

# Then source in each script:
source "$(dirname "$0")/test-helpers.sh"
```

### IN-02: file-test.sh Step Numbering Is Inconsistent

**File:** `scripts/file-test.sh:137`
**Issue:** The MinIO console check is labeled `[8.5/9]` (line 137), which is non-standard and may confuse CI parsers that expect integer step numbers. The download step is labeled `[8/9]` but contains two sub-tests, making the actual count 12 tests in a "9-step" script.

**Fix:** Renumber steps to `[8/10]` and `[9/10]` for MinIO and delete respectively, or combine MinIO check into an existing step.

### IN-03: admin-test.sh Fragile JSON Parsing with Inline Python

**File:** `scripts/admin-test.sh:55-62`
**Issue:** The script embeds an inline Python snippet to parse JSON, with a fragile string replacement (`replace('"code":', '"_code":')`) to work around the ApiResponse envelope. This 8-line inline script is hard to read, hard to test, and will break if the API response structure changes even slightly.

**Fix:** Use `jq` if available, or at minimum extract to a named function with clear documentation of the expected input format.

### IN-04: No Test Isolation -- Tests Depend on Specific Seed Data

**File:** All 7 scripts
**Issue:** Every script assumes specific users exist (`admin`, `enterprise001`, `thirdparty001`) with password `admin123`, and that the database has specific state. There is no setup phase to ensure the expected state, and no teardown to restore it. Running tests out of order or after a failed run can produce inconsistent results.

**Fix:** Document required seed data at the top of each script, or add a setup phase that creates required test data.

### IN-05: blockchain-test.sh Skips Tests But Doesn't Adjust Test Count

**File:** `scripts/blockchain-test.sh:101-104,143-146`
**Issue:** When `FIRST_BLOCK` or `FIRST_TX` is empty, the script prints `[SKIP]` and increments `TEST_ID` but does not increment `PASS` or `FAIL`. This means the summary shows `(total: N tests)` where N includes skipped tests, making the pass/fail ratio misleading. A run with 7 passes and 2 skips reports "7 passed, 0 failed (total: 9 tests)" which looks like 2 tests were silently lost.

**Fix:** Track skipped tests separately:
```bash
SKIP=0
# In skip branches:
SKIP=$((SKIP + 1))
# In summary:
echo "Results: $PASS passed, $FAIL failed, $SKIP skipped (total: $TEST_ID tests)"
```

---

_Reviewed: 2026-05-16T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
