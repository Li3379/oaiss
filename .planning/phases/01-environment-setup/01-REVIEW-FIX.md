---
status: all_fixed
phase: 01
phase_name: environment-setup
findings_in_scope: 4
fixed: 4
skipped: 0
iteration: 1
---

# Code Review Fix Report: Phase 01 — Environment Setup

**Fixed:** 2026-05-15
**Scope:** critical + warning findings from 01-REVIEW.md

## Fixes Applied

### WR-01: Hardcoded MinIO credentials → env vars ✓

**File:** docker-compose.infra.yml:39-41
**Fix:** Replaced hardcoded `minioadmin:minioadmin` with `${MINIO_ACCESS_KEY:-minioadmin}` / `${MINIO_SECRET_KEY:-minioadmin}` environment variable references. Defaults preserved for local dev.

### WR-02: MySQL port mismatch → 3307 ✓

**Files:** docker-compose.infra.yml:12, scripts/db-config.sh:14
**Fix:** Changed Docker MySQL host port from `3306` to `3307` in docker-compose. Updated `DB_PORT_DEFAULT` in db-config.sh to `3307` to match. This prevents the host MySQL (on 3306) from being confused with the Docker container.

### WR-03: Credential leak in print_db_config() → redacted ✓

**File:** scripts/db-config.sh:52-58
**Fix:** Changed `print_db_config()` to display `Password: ******` instead of the actual password value.

### WR-04: Missing authenticator001 → added ✓

**File:** scripts/login-test.sh:32
**Fix:** Added `"authenticator001:admin123:5:AUTHENTICATOR"` to the ACCOUNTS array. Script now tests all 7 seed accounts as documented in the SUMMARY.

## Skipped Findings

None — all 4 warning findings were fixable.

## Info Findings (not in scope)

The 4 info findings (IN-01 through IN-04) were not in the default fix scope. These are quality improvements that can be addressed separately with `/gsd-code-review-fix 1 --all`.
