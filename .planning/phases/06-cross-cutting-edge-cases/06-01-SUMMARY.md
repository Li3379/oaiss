---
phase: 06-cross-cutting-edge-cases
plan: 01
subsystem: security-bugfix
tags: [bugfix, security, swagger, cors, jpa]
dependency_graph:
  requires: [phase-1-complete]
  provides: [BUG-01-fix, BUG-02-fix, BUG-03-fix, bugfix-test-script]
  affects: [DigitalSignatureService, SecurityConfig]
tech_stack:
  added: []
  patterns: [findLatestByUserId with ORDER BY + LIMIT 1]
key_files:
  created:
    - scripts/bugfix-test.sh
  modified:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java
decisions:
  - 7 call sites (not 6) replaced in DigitalSignatureService -- plan underestimated count
  - DB column is is_deleted (not deleted) -- fixed in test script query
  - JWT filter whitelist contains stale swagger paths but SecurityConfig still enforces auth
metrics:
  duration: 7m
  completed: 2026-05-10
  tasks: 3
  tests: 12
  files_changed: 3
---

# Phase 6 Plan 01: Bug Fix Verification Summary

Fixed 3 bugs and created automated test script verifying all fixes -- 12/12 tests passing.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Fix BUG-01/02/03 code changes | c21aab4 | DigitalSignatureService.java, SecurityConfig.java |
| 2 | Create bugfix-test.sh | 6bac523 | scripts/bugfix-test.sh |
| 3 | Security review of SecurityConfig | (no code changes) | -- |

## Bug Fix Details

### BUG-01: NonUniqueResultException in DigitalSignatureService
- **Root cause:** `findByUserIdAndDeletedFalse()` returns multiple rows when user has revoked + active keypairs
- **Fix:** Replaced all 7 call sites with `findLatestByUserId()` which uses `ORDER BY keyVersion DESC LIMIT 1`
- **Verification:** Generated 2 keypairs for same user, GET /signature/keypair returns 200 (not 500)

### BUG-02: Swagger UI endpoints publicly accessible (SEC-03)
- **Root cause:** SecurityConfig had `.permitAll()` on swagger-ui/**, /v1/api-docs/**, /v3/api-docs/**
- **Fix:** Changed to `.authenticated()` -- production already disables springdoc via application-docker.yml
- **Verification:** Unauthenticated access returns 401 with code:2000; authenticated access returns 302 redirect

### BUG-03: CORS default allows localhost:5173 at Java level (SEC-04)
- **Root cause:** `@Value("${app.cors.allowed-origins:http://localhost:5173}")` had hardcoded fallback
- **Fix:** Removed `:http://localhost:5173` from @Value annotation; YAML-level default in application.yml still provides dev fallback
- **Verification:** evil.example.com blocked (403), localhost:5173 allowed via YAML default

## Security Review Findings (Task 3)

| Severity | Finding | Status |
|----------|---------|--------|
| LOW | Stale swagger paths in JwtAuthenticationFilter whitelist (dead code, no security impact) | Documented as tech debt |
| INFO | CSRF disabled for JWT stateless auth -- intentional design | Accepted |
| INFO | SecurityStartupValidator validates weak JWT/DB passwords -- good practice | Noted |

No CRITICAL or HIGH severity findings.

## Test Results

```
=== 06-01: Bug Fix Verification (BUG-01~03) ===
[PASS] Test 1: Generate keypair #1 returns 200
[PASS] Test 2: Generate keypair #2 returns 200
[PASS] Test 3: Multiple keypairs exist (count=2)
[PASS] Test 4: Get keypair returns 200 (no NonUniqueResultException)
[PASS] Test 5: Get keypair has publicKey
[PASS] Test 6: Get keypair does not return error 500
[PASS] Test 7: Sign returns 200 (no NonUniqueResultException)
[PASS] Test 8: Sign has signature data
[PASS] Test 9: Unauthenticated swagger returns auth error (code 2000)
[PASS] Test 10: Authenticated swagger access succeeds (HTTP 302)
[PASS] Test 11: CORS blocks unauthorized origin evil.example.com
[PASS] Test 12: CORS allows localhost:5173
Results: 12 passed, 0 failed (total: 12 tests)
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] DB column name mismatch in test script**
- **Found during:** Task 2 execution
- **Issue:** Test script used `deleted=0` but actual column name is `is_deleted`
- **Fix:** Changed query to use `is_deleted=0`
- **Files modified:** scripts/bugfix-test.sh
- **Commit:** 6bac523

**2. [Plan adjustment] 7 call sites not 6**
- **Found during:** Task 1 execution
- **Issue:** Plan estimated 6 call sites for findByUserIdAndDeletedFalse but there were 7 (plus 1 existing findLatestByUserId)
- **Fix:** Replaced all occurrences correctly (7 + reviewerId variants + enterpriseUserId variant)
- **Files modified:** DigitalSignatureService.java
- **Commit:** c21aab4

## Threat Model Compliance

| Threat ID | Category | Disposition | Status |
|-----------|----------|-------------|--------|
| T-06-01 | Information Disclosure | mitigate | FIXED -- Swagger requires auth |
| T-06-02 | Tampering | mitigate | FIXED -- CORS no Java-level default |
| T-06-03 | Denial of Service | mitigate | FIXED -- findLatestByUserId with LIMIT 1 |

## Self-Check: PASSED

All files exist, all commits found in git log.
