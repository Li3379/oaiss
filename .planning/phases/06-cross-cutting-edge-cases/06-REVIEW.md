---
phase: 06-cross-cutting-edge-cases
reviewed: 2026-05-16T16:02:00+08:00
depth: deep
files_reviewed: 5
files_reviewed_list:
  - scripts/bugfix-test.sh
  - scripts/aop-test.sh
  - scripts/edge-test.sh
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java
findings:
  critical: 3
  warning: 5
  info: 4
  total: 12
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-05-16T16:02:00+08:00
**Depth:** deep
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Reviewed 3 bash E2E test scripts (bugfix-test.sh, aop-test.sh, edge-test.sh) and 2 Java source files (DigitalSignatureService.java, SecurityConfig.java). The Java fixes for BUG-01 (NonUniqueResultException via `findLatestByUserId`) and BUG-02/BUG-03 (Swagger auth + CORS) are correctly implemented. However, the test scripts contain several issues that undermine test reliability: pagination tests use wrong parameter names and silently pass, the AOP test's `sed` injection pattern is too broad and may target wrong methods, and a race condition in `generateKeyPair` could reintroduce the NonUniqueResultException bug under concurrent load. All three scripts hardcode database credentials inline instead of using the existing `test-helpers.sh` utility.

## Critical Issues

### CR-01: Pagination tests silently pass due to wrong parameter names

**File:** `scripts/edge-test.sh:315-350`
**Issue:** The pagination boundary tests for carbon reports and auction orders send `page` and `size` query parameters, but the admin users test at line 348 sends `pageNum` and `pageSize`. The backend `AdminController.listUsers` (line 71-73) accepts `page` and `size` as `@RequestParam` names. When the test sends `pageNum=1&pageSize=1`, Spring ignores these unrecognized parameters and applies the defaults (`page=1, size=10`). The test then asserts `"code":200` which always passes, giving false confidence that pagination with size=1 works when it actually returns 10 records.

The same issue affects the carbon reports tests at lines 315-327 -- they correctly use `page`/`size`, but the inconsistency with the admin test shows parameter naming was not verified against the actual controller signatures.

**Fix:**
```bash
# edge-test.sh line 348: change pageNum/pageSize to page/size
RESP=$(curl -s "$BASE_URL/admin/users?page=1&size=1" \
    -H "Authorization: Bearer $TOKEN_ADMIN")
```

### CR-02: AOP test `sed` pattern matches too broadly, may inject annotation on wrong method

**File:** `scripts/aop-test.sh:215,309,416`
**Issue:** The `sed` commands use broad patterns to inject annotations before controller methods. For example, line 215:
```bash
sed -i '/public ApiResponse<CarbonReportResponse> createReport/i\    @AuditLog(...)' "$CARBON_FILE"
```
This matches ANY line containing `public ApiResponse<CarbonReportResponse> createReport`. If the controller has overloaded methods, helper methods, or methods with names like `createReportBatch`, the annotation would be injected before the first match, which may not be the intended target. Similarly, line 309 matches `public ApiResponse<LoginResponse> login(` and line 416 matches `public ApiResponse<List<MatchingResultResponse>> executeMatching`.

While the current controller files happen to have unique method signatures, this is fragile -- any future refactor adding a similarly-named method would silently break the test by annotating the wrong endpoint.

**Fix:**
```bash
# Use a more precise pattern that anchors to the @PostMapping/@GetMapping line above
# or use line-number-based injection after grepping for the exact method
METHOD_LINE=$(grep -n "public ApiResponse<CarbonReportResponse> createReport" "$CARBON_FILE" | head -1 | cut -d: -f1)
sed -i "${METHOD_LINE}i\\    @AuditLog(module = \"test\", action = \"createReport\")" "$CARBON_FILE"
```

### CR-03: Race condition in `generateKeyPair` can reintroduce NonUniqueResultException

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java:66-108`
**Issue:** The `generateKeyPair` method performs a check-then-act sequence without synchronization:
1. Line 70: `existsByUserIdAndDeletedFalse(userId)` -- checks if key exists
2. Line 73: `revokeKeyPair(userId)` -- marks old key as revoked
3. Line 83: `findLatestByUserId(userId)` -- gets latest version
4. Line 100: `save(rsaKeyPairEntity)` -- creates new key

If two concurrent requests arrive for the same user, both can pass the `existsByUserIdAndDeletedFalse` check before either calls `revokeKeyPair`. Both will then create new key pairs, resulting in multiple non-deleted rows -- the exact BUG-01 condition this fix was meant to prevent. The `@Transactional` annotation alone does not prevent this because the isolation level (default READ_COMMITTED) does not serialize the check-then-act pattern.

The test script `bugfix-test.sh` generates keypairs sequentially (lines 81-92), so it never exercises this concurrent path.

**Fix:**
```java
// Option A: Add @Transactional with isolation level SERIALIZABLE (performance cost)
@Transactional(isolation = Isolation.SERIALIZABLE)

// Option B: Use a unique constraint on (user_id, key_status=1, deleted=false) 
// in the database to prevent duplicate active keys, and handle the constraint 
// violation in the service layer

// Option C: Use a distributed lock (consistent with AOP-04 pattern)
@DistributedLock(key = "keypair:generate:#userId", expireTime = 10)
```

## Warnings

### WR-01: All three test scripts hardcode database credentials inline

**File:** `scripts/bugfix-test.sh:39`, `scripts/aop-test.sh:50`, `scripts/edge-test.sh:49`
**Issue:** All three scripts define `db_query()` with hardcoded credentials `mysql -h 127.0.0.1 -P 3306 -u root -p123456`. This exposes the password in the process list (`ps aux`), shell history, and audit logs. The existing `test-helpers.sh` (line 65-84) provides `run_mysql()` which uses `--defaults-extra-file` to hide the password from the process list. None of the three scripts source `test-helpers.sh`.

**Fix:**
```bash
# Source test-helpers.sh and use run_mysql() instead of db_query()
source "$(dirname "$0")/test-helpers.sh"
# Replace db_query "..." calls with run_mysql "..."
```

### WR-02: Duplicate helper functions across all test scripts

**File:** `scripts/bugfix-test.sh:12-36`, `scripts/aop-test.sh:17-41`, `scripts/edge-test.sh:16-40`
**Issue:** All three scripts redefine `assert_contains()`, `assert_not_contains()`, and `login_user()` locally instead of sourcing `scripts/test-helpers.sh`. The shared helper provides additional utilities (`extract_token`, `validate_integer`, `assert_code_200`, `print_summary`) and uses `grep -qF` (fixed-string matching) instead of `grep -q` (regex matching). The local copies use regex matching, which means patterns like `"code":200` could theoretically match `"code":2004` if the assertion string were less carefully chosen. While the current assertion strings happen to work correctly, the duplication creates maintenance burden and divergent behavior.

**Fix:** Source `test-helpers.sh` at the top of each script and remove the local copies.

### WR-03: `aop-test.sh` does not verify `mvn compile` success before restarting backend

**File:** `scripts/aop-test.sh:227-233`
**Issue:** The script runs `mvn compile -q` and checks `$?`, but because `set -euo pipefail` is active, if `mvn compile` fails, the script will exit immediately at the `if [ $? -eq 0 ]` line (which always evaluates to 0 because the previous command succeeded or the script already exited). The `$?` check on line 228 is dead code -- it will always be 0 because `set -e` already caught any non-zero exit. The same pattern appears at lines 320-326 and 427-433.

**Fix:**
```bash
# Use a subshell to capture exit code without triggering set -e
if (cd "$WORK_DIR/oaiss-chain-backend" && mvn compile -q); then
    echo "  Compile successful"
else
    echo "  ERROR: Compilation failed"
    FAIL=$((FAIL + 1))
    # Optionally skip remaining tests in this section
fi
```

### WR-04: `edge-test.sh` test numbering gap (test 9 missing)

**File:** `scripts/edge-test.sh:125-132`
**Issue:** The EDGE-01 section comments enumerate tests 1-8, then jumps to test 10 (line 130: `# 10. Unauthenticated -> admin users list`). Test 9 is missing from the comments. While this does not affect execution (TEST_ID auto-increments), it suggests a test case was dropped without updating the numbering, which could indicate an incomplete test plan.

**Fix:** Add the missing test 9 or renumber test 10 to test 9.

### WR-05: `bugfix-test.sh` BUG-02 test uses temp files without cleanup

**File:** `scripts/bugfix-test.sh:146-149`
**Issue:** The Swagger test writes response bodies to `/tmp/swagger_resp_body.json` and `/tmp/swagger_auth_resp_body.json` but never cleans them up. On shared systems, these files could leak response data. The script also lacks a `trap cleanup EXIT` handler (unlike `aop-test.sh` which has one at line 125).

**Fix:**
```bash
# Add cleanup trap
cleanup() { rm -f /tmp/swagger_resp_body.json /tmp/swagger_auth_resp_body.json; }
trap cleanup EXIT
```

## Info

### IN-01: `assert_contains` in test scripts uses regex instead of fixed-string matching

**File:** `scripts/bugfix-test.sh:15`, `scripts/aop-test.sh:20`, `scripts/edge-test.sh:19`
**Issue:** The local `assert_contains` uses `grep -q "$expected"` (regex mode) while `test-helpers.sh` line 113 uses `grep -qF "$expected"` (fixed-string mode). Regex mode is riskier for matching JSON fragments containing characters like `.` or `*`. Current assertion strings do not contain regex metacharacters, so this is not a bug today, but it is a latent risk.

**Fix:** Use `grep -qF` for fixed-string matching, consistent with `test-helpers.sh`.

### IN-02: `edge-test.sh` XSS and SQL injection tests only check for server crash

**File:** `scripts/edge-test.sh:419-462`
**Issue:** The XSS test (line 419) sends `<script>alert(1)</script>` as a title and only checks that the response contains `"code"` (i.e., the server did not crash). It does not verify that the XSS payload was rejected or sanitized. Similarly, the SQL injection test (line 435) sends `test' OR 1=1 --` and only checks for a valid response. These tests document that the server handles the input, but do not verify that the security concern is actually mitigated.

**Fix:** Add assertions to verify the XSS payload is either rejected (non-200 code) or sanitized (not reflected in response). For SQL injection, verify the response does not return unexpected data (e.g., all records).

### IN-03: `aop-test.sh` AOP-04 DistributedLock test has a weak fallback

**File:** `scripts/aop-test.sh:480-488`
**Issue:** If neither concurrent response contains code 1009 (OPERATION_IN_PROGRESS), the test falls back to checking that both responses are non-empty and declares PASS. This means the test passes even if the distributed lock did not actually block the concurrent request, as long as both requests returned something. The comment acknowledges this: "Both might have succeeded if matching completed very fast."

**Fix:** Log a warning when the fallback path is taken, or increase the concurrent request count to improve the probability of observing lock contention.

### IN-04: `SecurityConfig.java` CORS `allowedOrigins` has no fallback default in Docker profile

**File:** `oaiss-chain-backend/src/main/resources/application-docker.yml:28-29`
**Issue:** The main `application.yml` (line 134) provides a default: `${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:5174}`. The Docker profile sets `allowed-origins: ${CORS_ALLOWED_ORIGINS}` without a default. If the environment variable is not set, Spring will fail to inject the `List<String>` at startup. This is a pre-existing issue, not introduced by Phase 6, but relevant to the CORS fix context.

**Fix:** Add a fallback in `application-docker.yml`:
```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

---

_Reviewed: 2026-05-16T16:02:00+08:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
