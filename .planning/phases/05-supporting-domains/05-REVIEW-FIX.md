---
phase: 05-supporting-domains
fixed_at: 2026-05-16T14:00:00Z
review_path: .planning/phases/05-supporting-domains/05-REVIEW.md
iteration: 1
findings_in_scope: 16
fixed: 16
skipped: 0
status: all_fixed
---

# Phase 5: Code Review Fix Report

**Fixed at:** 2026-05-16T14:00:00Z
**Source review:** .planning/phases/05-supporting-domains/05-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 16
- Fixed: 16
- Skipped: 0

## Fixed Issues

### IN-01: Extract shared test-helpers.sh library

**Files modified:** `scripts/test-helpers.sh` (new)
**Commit:** 7dec805
**Applied fix:** Created `scripts/test-helpers.sh` with shared functions: `assert_contains`, `assert_not_contains`, `assert_code_200`, `login_user`, `extract_token`, `extract_field`, `run_mysql`, `validate_integer`, `check_dependencies`, `print_summary`. All 7 scripts now source this library via `source "$(dirname "$0")/test-helpers.sh"`.

### CR-01: Hardcoded MySQL password replaced with run_mysql helper

**Files modified:** `scripts/sign-test.sh`, `scripts/emission-test.sh`
**Commit:** b03043e, 80eaeb6
**Applied fix:** Replaced inline `mysql -h 127.0.0.1 -P 3306 -u root -p123456` calls with `run_mysql` helper that uses `--defaults-extra-file` with a temporary config file. Credentials are configurable via `DB_USER`, `DB_PASS`, `DB_HOST`, `DB_PORT`, `DB_NAME` environment variables. Password never appears in process listings.

### CR-02: SQL injection via unvalidated variable interpolation

**Files modified:** `scripts/sign-test.sh`, `scripts/emission-test.sh`
**Commit:** b03043e, 80eaeb6
**Applied fix:** Added `validate_integer` calls before any SQL query using extracted IDs (`E1_USER_ID`, `E1_ENTERPRISE_ID`). Script exits with clear error if the extracted value is not a positive integer.

### CR-03: assert_contains false positives on substring match

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** Replaced `grep -q` with `grep -qF` (fixed-string match) in `assert_contains` and `assert_not_contains`. Added `assert_code_200` function that uses `grep -qE '"code"\s*:\s*200[^0-9]'` for precise code-200 matching that rejects codes like 2000, 2001, etc. All `'"code":200'` assertions across all scripts now use `assert_code_200`.

### WR-01: No token validation after login

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** Added `extract_token` function in test-helpers.sh that checks if the extracted token is empty and exits with `[FATAL]` diagnostic including the raw response body. All scripts now use `extract_token` instead of raw grep/cut pipelines.

### WR-02: No dependency checks

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** Added `check_dependencies` function in test-helpers.sh that verifies `curl`, `grep`, `cut`, `head` are available. Scripts with additional dependencies call `check_dependencies mysql` (sign-test.sh, emission-test.sh) for the mysql client.

### WR-03: Fragile JSON parsing via grep/cut

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** Added `extract_field` function in test-helpers.sh that uses `jq -r ".field // empty"` when jq is available, falling back to grep/cut. Token extraction now uses `extract_field` via `extract_token`. ID extraction in sign-test.sh and emission-test.sh uses `extract_field`.

### WR-04: No trap-based cleanup

**Files modified:** `scripts/sign-test.sh`, `scripts/file-test.sh`
**Commit:** b03043e, 6866b3a
**Applied fix:** In file-test.sh, added `trap 'rm -f "$TEST_FILE" /tmp/oaiss-downloaded.txt 2>/dev/null' EXIT` after creating the temp file. In sign-test.sh, the pre-cleanup step now uses `run_mysql` which handles its own temp file cleanup internally.

### WR-05: Hardcoded login password

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** `login_user` function in test-helpers.sh now uses `TEST_PASSWORD` environment variable with `admin123` as default. All 7 scripts inherit this via `source test-helpers.sh`.

### WR-06: Cleanup step counts as test in sign-test.sh

**Files modified:** `scripts/sign-test.sh`
**Commit:** b03043e
**Applied fix:** The revoke cleanup step no longer calls `assert_contains`. Instead it uses a simple if/else that prints `[OK]` or `[WARN]` without incrementing `TEST_ID` or `PASS`/`FAIL` counters.

### WR-07: emission-test.sh masks MySQL errors with || echo "0"

**Files modified:** `scripts/emission-test.sh`
**Commit:** 80eaeb6
**Applied fix:** Replaced `mysql ... 2>/dev/null || echo "0"` with `run_mysql` which returns `MYSQL_ERROR:...` on failure. The script now checks for this prefix, prints the actual error message, validates the result is a number, and only then uses it for comparison.

### IN-02: file-test.sh step numbering inconsistency

**Files modified:** `scripts/file-test.sh`
**Commit:** 6866b3a
**Applied fix:** Renumbered all steps from `[1/9]`...`[9/9]` to `[1/10]`...`[10/10]`. The MinIO console check is now `[9/10]` and delete is `[10/10]`, eliminating the non-standard `[8.5/9]` label.

### IN-03: admin-test.sh inline Python fragility

**Files modified:** `scripts/admin-test.sh`
**Commit:** 352a17f
**Applied fix:** Replaced the 8-line inline Python snippet with a jq-based approach: `jq -r '.data.content[] | select(.userType == 1) | .id'` when jq is available, with a grep fallback for environments without jq. The fragile `replace('"code":', '"_code":')` hack is eliminated.

### IN-04: No test isolation documentation

**Files modified:** All 7 scripts
**Commit:** b03043e, 80eaeb6, 352a17f, f533b08, 6866b3a, d247a72, 80a224a
**Applied fix:** Added "Required seed data" documentation block at the top of each script documenting the users, database, and backend dependencies. Also documented in test-helpers.sh header.

### IN-05: blockchain-test.sh skip miscount

**Files modified:** `scripts/blockchain-test.sh`
**Commit:** f533b08
**Applied fix:** Added `SKIP=0` counter in test-helpers.sh (initialized alongside `PASS` and `FAIL`). Skip branches in blockchain-test.sh now increment `SKIP=$((SKIP + 1))` instead of only incrementing `TEST_ID`. Summary uses `print_summary` which reports `$PASS passed, $FAIL failed, $SKIP skipped`.

## Skipped Issues

None -- all findings were successfully fixed.

---

_Fixed: 2026-05-16T14:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
