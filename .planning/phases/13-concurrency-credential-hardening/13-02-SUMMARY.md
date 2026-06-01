---
phase: 13-concurrency-credential-hardening
plan: 02
subsystem: infrastructure-security
tags: [SEC-07, SEC-08, SEC-09, credential-hardening, docker-compose, spring-config]
dependency_graph:
  requires: []
  provides: [externalized-credentials, no-insecure-defaults, security-startup-validator-minio]
  affects: [docker-compose.yml, docker-compose.infra.yml, application.yml, application-local.yml, SecurityStartupValidator]
tech_stack:
  added: []
  patterns: [env-var-substitution, fail-fast-on-missing-credentials, weak-credential-blocklist]
key_files:
  created: []
  modified:
    - docker-compose.yml
    - docker-compose.infra.yml
    - .env.example
    - oaiss-chain-backend/src/main/resources/application.yml
    - oaiss-chain-backend/src/main/resources/application-local.yml
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityStartupValidator.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/config/SecurityStartupValidatorTest.java
decisions:
  - Use empty-default syntax ${VAR:} in application YAML instead of no-default ${VAR} to avoid Spring startup errors for non-credential properties; credentials with empty defaults cause connection failures that surface the missing variable
  - Add MinIO credential validation to existing SecurityStartupValidator rather than creating a separate validator class
metrics:
  duration: 737s
  completed: 2026-05-19
  tasks_completed: 2
  files_modified: 7
---

# Phase 13 Plan 02: Credential Hardening Summary

Externalized all hardcoded credentials from docker-compose and Spring YAML files; removed insecure default fallbacks; added MinIO weak-credential detection to SecurityStartupValidator.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Externalize docker-compose credentials (SEC-07, SEC-09) | 2c56836 | docker-compose.yml, docker-compose.infra.yml, .env.example |
| 2 | Remove insecure defaults from application YAML files (SEC-08, SEC-09) | 40cb102 | application.yml, application-local.yml, SecurityStartupValidator.java, SecurityStartupValidatorTest.java |

## Changes Summary

### Task 1: docker-compose credential externalization

**docker-compose.yml:**
- `MYSQL_ROOT_PASSWORD: Oa1ssDb2026Pr0dSecureP4ss` -> `${DB_PASSWORD}`
- `DB_PASSWORD: Oa1ssDb2026Pr0dSecureP4ss` -> `${DB_PASSWORD}`
- `JWT_SECRET: Oa1ss2026Pr0dS3cur3JwtK3yF0rHmacSha256S1gn1ngD0ck3r` -> `${JWT_SECRET}`
- Redis `--requirepass ${REDIS_PASSWORD:-oaiss_redis_dev_2026}` -> `${REDIS_PASSWORD}` (removed fallback)
- Redis healthcheck `-a ${REDIS_PASSWORD:-oaiss_redis_dev_2026}` -> `${REDIS_PASSWORD}` (removed fallback)
- `MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}` -> `${MINIO_ACCESS_KEY}` (removed fallback)
- `MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin}` -> `${MINIO_SECRET_KEY}` (removed fallback)
- Backend `MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY:-minioadmin}` -> `${MINIO_ACCESS_KEY}` (removed fallback)
- Backend `MINIO_SECRET_KEY: ${MINIO_SECRET_KEY:-minioadmin}` -> `${MINIO_SECRET_KEY}` (removed fallback)

**docker-compose.infra.yml:** Identical changes for MySQL, Redis, and MinIO services.

**.env.example:** Replaced `MINIO_ACCESS_KEY=minioadmin` and `MINIO_SECRET_KEY=minioadmin` with `change_me_minio_access_key` and `change_me_minio_secret_key`.

### Task 2: Application YAML insecure default removal

**application.yml:**
- `password: ${DB_PASSWORD:123456}` -> `password: ${DB_PASSWORD:}` (removed insecure default)

**application-local.yml:**
- `password: ${DB_PASSWORD:Oa1ssDb2026Pr0dSecureP4ss}` -> `password: ${DB_PASSWORD:}`
- `password: ${REDIS_PASSWORD:oaiss_redis_dev_2026}` -> `password: ${REDIS_PASSWORD:}`
- `access-key: ${MINIO_ACCESS_KEY:minioadmin}` -> `access-key: ${MINIO_ACCESS_KEY:}`
- `secret-key: ${MINIO_SECRET_KEY:minioadmin}` -> `secret-key: ${MINIO_SECRET_KEY:}`
- `secret: ${JWT_SECRET:dGVzdC1qd3Qtc2VjcmV0LWZvci1sb2NhbC1kZXYtZW52}` -> `secret: ${JWT_SECRET:}`

**SecurityStartupValidator.java:**
- Added `WEAK_MINIO_CREDENTIALS` set: `minioadmin`, `minio`, `admin`, `accesskey`, `secretkey`
- Added `@Value("${minio.access-key:}")` and `@Value("${minio.secret-key:}")` fields
- Added `validateMinioCredentials()` method that blocks startup in production with weak MinIO credentials, warns in non-production

**SecurityStartupValidatorTest.java:**
- Updated all 6 existing tests to set `minioAccessKey`/`minioSecretKey` fields
- Added 2 new tests: production with weak MinIO credentials (throws), dev with weak MinIO credentials (warns only)
- All 8 tests pass

## Verification Results

- `grep -rn "123456" docker-compose*.yml application*.yml` -> 0 matches
- `grep -rn "minioadmin" docker-compose*.yml application*.yml .env.example` -> 0 matches
- `grep -rn "Oa1ssDb2026Pr0dSecureP4ss" docker-compose*.yml application*.yml` -> 0 matches
- `grep -rn "oaiss_redis_dev_2026" docker-compose*.yml application*.yml` -> 0 matches
- `grep -rn "Oa1ss2026Pr0dS3cur3JwtK3yF0rHmacSha256S1gn1ngD0ck3r" docker-compose*.yml application*.yml` -> 0 matches
- SecurityStartupValidatorTest: 8/8 tests pass

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Threat Flags

No new threat surface introduced. All changes reduce attack surface by removing hardcoded credentials and insecure defaults.

## Self-Check: PASSED

All 8 modified files verified present. Both commit hashes (2c56836, 40cb102) confirmed in git log.
