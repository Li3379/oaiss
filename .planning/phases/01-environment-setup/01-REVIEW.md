---
status: issues_found
phase: 01
phase_name: environment-setup
depth: standard
files_reviewed: 4
findings:
  critical: 0
  warning: 4
  info: 4
  total: 8
---

# Code Review: Phase 01 — Environment Setup

**Reviewed:** 2026-05-15
**Depth:** standard
**Files:** docker-compose.infra.yml, scripts/health-check.sh, scripts/login-test.sh, scripts/db-config.sh

## Findings

### WR-01: Hardcoded MinIO default credentials in docker-compose

**Severity:** warning
**File:** docker-compose.infra.yml:39-40
**Category:** security

MinIO uses well-known default credentials `minioadmin:minioadmin` directly in the compose file rather than referencing environment variables.

```yaml
environment:
  MINIO_ROOT_USER: minioadmin
  MINIO_ROOT_PASSWORD: minioadmin
```

**Recommendation:** Use `${MINIO_ACCESS_KEY}` / `${MINIO_SECRET_KEY}` environment variable references (matching the project's `.env.example` pattern), with defaults only for local dev:

```yaml
environment:
  MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
  MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin}
```

---

### WR-02: MySQL port mismatch between docker-compose and documented environment

**Severity:** warning
**File:** docker-compose.infra.yml:12
**Category:** bug

The compose file maps MySQL to host port `3306`, but the SUMMARY documents that host MySQL occupies 3306 and Docker MySQL should run on port 3307. The health-check.sh and login-test.sh also connect via `localhost:3306`, which may hit the host MySQL instead of the container.

```yaml
ports:
  - "3306:3306"   # SUMMARY says this should be 3307
```

**Recommendation:** Change to `"3307:3306"` if the intent is to run alongside host MySQL, or document the assumption that host MySQL is stopped when using this compose file.

---

### WR-03: health-check.sh prints database credentials to stdout

**Severity:** warning
**File:** scripts/health-check.sh:73
**Category:** security

The script prints the MySQL connection details including table counts, and the sourced `db-config.sh` exposes `DB_PASSWORD` in its `print_db_config()` function. While this is a local dev script, credentials in terminal output can leak into CI logs or terminal scrollback.

**Recommendation:** Suppress or redact password values in output. Consider removing `print_db_config()` or masking the password field.

---

### WR-04: login-test.sh missing authenticator001 account

**Severity:** warning
**File:** scripts/login-test.sh:25-32
**Category:** bug

The SUMMARY states the script tests "all 7 seed accounts" but the ACCOUNTS array only contains 6 entries — `authenticator001` (userType=5, AUTHENTICATOR) is missing.

```bash
ACCOUNTS=(
  "admin:admin123:4:ADMIN"
  "enterprise001:admin123:1:ENTERPRISE"
  # ... 6 total, missing authenticator001
)
```

**Recommendation:** Add `"authenticator001:admin123:5:AUTHENTICATOR"` to the ACCOUNTS array.

---

### IN-01: health-check.sh fail() exits immediately — partial results only

**Severity:** info
**File:** scripts/health-check.sh:14
**Category:** quality

The `fail()` function calls `exit 1` immediately, so if an early check fails, subsequent checks are never run. This prevents seeing the full health status in a single run.

**Recommendation:** Consider collecting failures and reporting all at the end (set a FAILED flag, continue checks, exit non-zero at end if any failed).

---

### IN-02: Backend health check accepts HTTP 405 as "reachable"

**Severity:** info
**File:** scripts/health-check.sh:57
**Category:** quality

The regex `^(200|400|405)$` accepts 405 (Method Not Allowed) as a passing condition. A 405 on `/auth/login` POST would indicate a routing or method mapping problem, not a healthy backend.

**Recommendation:** Remove 405 from the accepted codes: `^(200|400)$`.

---

### IN-03: login-test.sh has no curl timeout

**Severity:** info
**File:** scripts/login-test.sh:43
**Category:** quality

Curl calls use no `--connect-timeout` or `--max-time` flags. If the backend hangs, the script blocks indefinitely.

**Recommendation:** Add `--connect-timeout 5 --max-time 15` to curl calls.

---

### IN-04: MinIO image uses :latest tag

**Severity:** info
**File:** docker-compose.infra.yml:37
**Category:** quality

The MinIO service uses `minio/minio:latest` which is non-deterministic. The MySQL and Redis services pin to specific versions (`mysql:8.0`, `redis:7-alpine`).

**Recommendation:** Pin to a specific MinIO version tag for reproducibility.

---

## Observations (non-findings)

| Observation | Detail |
|------------|--------|
| V3__test_seed_data.sql missing | SUMMARY references this file but it no longer exists on disk. Either deleted after migration was applied to host MySQL, or never committed. |
| db-config.sh is a shared utility | Sourced by health-check.sh; reviewed as part of Phase 1 scope. Contains a fallback to local `mysql` CLI which may not be installed. |
| login-test.sh uses grep for JSON parsing | Fragile but acceptable for a local dev script with known response shapes. |
