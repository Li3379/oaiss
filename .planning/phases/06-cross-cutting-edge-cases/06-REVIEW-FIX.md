---
phase: 06-cross-cutting-edge-cases
fixed_at: 2026-05-16T16:26:00+08:00
review_path: .planning/phases/06-cross-cutting-edge-cases/06-REVIEW.md
iteration: 1
findings_in_scope: 12
fixed: 12
skipped: 0
status: all_fixed
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-05-16T16:26:00+08:00
**Source review:** .planning/phases/06-cross-cutting-edge-cases/06-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 12
- Fixed: 12
- Skipped: 0

## Fixed Issues

### CR-01: Pagination tests silently pass due to wrong parameter names

**Files modified:** `scripts/edge-test.sh`
**Commit:** d5fafc1
**Applied fix:** Changed `pageNum=1&pageSize=1` to `page=1&size=1` in admin users pagination test to match actual `AdminController.listUsers` `@RequestParam` names.

### CR-02: AOP test `sed` pattern matches too broadly

**Files modified:** `scripts/aop-test.sh`
**Commit:** ab6d61b
**Applied fix:** Replaced broad pattern-matching `sed -i '/pattern/i\...'` with line-number-based injection using `grep -n` + `sed -i "${LINE}i\..."` for all three annotation injection points (CarbonController.createReport, AuthController.login, DoubleAuctionController.executeMatching).

### CR-03: Race condition in `generateKeyPair`

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java`
**Commit:** 16ee0d8
**Applied fix:** Added `@DistributedLock(key = "'keypair:generate:' + #userId", expireTime = 10)` annotation to `generateKeyPair` method. This serializes concurrent key pair generation for the same user via Redis-based distributed lock, preventing the check-then-act race condition that could create duplicate active keys.

### WR-01: All three test scripts hardcode database credentials inline

**Files modified:** `scripts/edge-test.sh`, `scripts/aop-test.sh`, `scripts/bugfix-test.sh`
**Commit:** bfbcbf3
**Applied fix:** All three scripts now source `scripts/test-helpers.sh` and use `run_mysql()` which uses `--defaults-extra-file` to hide credentials from the process list. Local `db_query()` functions removed.

### WR-02: Duplicate helper functions across all test scripts

**Files modified:** `scripts/edge-test.sh`, `scripts/aop-test.sh`, `scripts/bugfix-test.sh`
**Commit:** bfbcbf3
**Applied fix:** Removed local copies of `assert_contains`, `assert_not_contains`, `login_user`, and `db_query` from all three scripts. They now inherit these from `test-helpers.sh` which uses `grep -qF` (fixed-string matching) instead of `grep -q` (regex matching).

### WR-03: `aop-test.sh` does not verify `mvn compile` success properly

**Files modified:** `scripts/aop-test.sh`
**Commit:** 3f9f297
**Applied fix:** Changed all 3 instances of `(cd ... && mvn compile -q) / if [ $? -eq 0 ]` to `if (cd ... && mvn compile -q); then`. With `set -e` active, the separate command + `$?` check was dead code because the script would exit on non-zero exit before reaching the `if`.

### WR-04: `edge-test.sh` test numbering gap

**Files modified:** `scripts/edge-test.sh`
**Commit:** 32f1c1e
**Applied fix:** Renumbered test 10 to test 9 in EDGE-01 section comments to close the gap where test 9 was missing from the enumeration.

### WR-05: `bugfix-test.sh` BUG-02 test uses temp files without cleanup

**Files modified:** `scripts/bugfix-test.sh`
**Commit:** ae2c3cc
**Applied fix:** Added `trap cleanup EXIT` handler that removes `/tmp/swagger_resp_body.json` and `/tmp/swagger_auth_resp_body.json` on script exit.

### IN-01: `assert_contains` uses regex instead of fixed-string matching

**Files modified:** `scripts/edge-test.sh`, `scripts/aop-test.sh`, `scripts/bugfix-test.sh`
**Commit:** bfbcbf3
**Applied fix:** Resolved as part of WR-02. The shared `test-helpers.sh` uses `grep -qF` (fixed-string matching) for all assertions.

### IN-02: XSS and SQL injection tests only check for server crash

**Files modified:** `scripts/edge-test.sh`
**Commit:** 0ac0fa2
**Applied fix:** XSS test now verifies the payload is either rejected (non-200 code) or sanitized (not reflected verbatim in response). SQL injection test now checks that the response does not contain multiple record IDs indicating a data leak.

### IN-03: AOP-04 DistributedLock test has a weak fallback

**Files modified:** `scripts/aop-test.sh`
**Commit:** 34722b0
**Applied fix:** Added a `[WARN]` log message when the fallback path is taken where both concurrent requests succeeded without observing lock contention.

### IN-04: `SecurityConfig.java` CORS `allowedOrigins` has no fallback default

**Files modified:** `oaiss-chain-backend/src/main/resources/application-docker.yml`
**Commit:** ee89bb4
**Applied fix:** Added `http://localhost:5173` as fallback default for `CORS_ALLOWED_ORIGINS` in the Docker profile YAML to prevent Spring injection failure when the environment variable is not set.

---

_Fixed: 2026-05-16T16:26:00+08:00_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
