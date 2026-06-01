# SUMMARY: Phase 15, Plan 02 — Fix CI/CD Pipeline Issues

**Plan:** 15-02-fix-ci-cd-pipeline
**Status:** COMPLETE
**Requirements:** OPS-01
**Commits:** 2

## What Changed

### Commit 1: `9c9a3b4` — Fix backend CI/CD pipeline (ci-cd.yml)

| Change | Before | After |
|--------|--------|-------|
| Security scan | OWASP dependency-check-maven (not in pom.xml, would fail) | Trivy filesystem scan on compiled JAR |
| docker-build trigger | `refs/heads/main` only | `refs/heads/main` or `refs/heads/develop` |
| notify dependency | `[build, docker-build]` | `[build]` only |
| Secrets documentation | None | Header comments listing DOCKER_USERNAME, DOCKER_PASSWORD, SLACK_WEBHOOK |

**Rationale:**
- OWASP plugin is not configured in pom.xml, so `mvn org.owasp:dependency-check-maven:check` would fail at runtime
- Trivy provides equivalent vulnerability scanning without requiring Maven plugin changes
- deploy-staging depends on docker-build but checked `refs/heads/develop` while docker-build only ran on `main` — now both main and develop trigger docker-build
- notify job depended on docker-build, which is skipped on PRs — now notify always runs after build

### Commit 2: `5008292` — Fix E2E workflow (e2e-tests.yml)

| Change | Before | After |
|--------|--------|-------|
| MinIO service | Missing (file upload tests fail) | Added with health check |
| MINIO_ACCESS_KEY | `minioadmin` (hardcoded weak default) | `minio-test-access` (test-specific) |
| MINIO_SECRET_KEY | `minioadmin` (hardcoded weak default) | `minio-test-secret-key` (test-specific) |
| SPRING_PROFILES_ACTIVE | Missing | `docker` |
| MINIO_ENDPOINT | Missing | `http://localhost:9000` |
| REDIS_PASSWORD | Missing | `""` (empty, matching docker profile default) |
| Push trigger | None (PR only) | `push` on `main` branch added |

**Rationale:**
- MinIO is required by FileController for file upload/download; without it, E2E tests involving file operations would fail
- Hardcoded `minioadmin` is the MinIO default — using in CI is a security risk and would fail if MinIO defaults are changed in production
- `SPRING_PROFILES_ACTIVE=docker` ensures the backend uses the correct configuration profile (database, Redis, MinIO connection settings)
- Push trigger on main ensures E2E tests run after merges, catching regressions early

## Verification

- [x] `grep "dependency-check-maven" ci-cd.yml` returns 0 matches
- [x] `grep "minio:" e2e-tests.yml` finds MinIO service definition
- [x] `grep "minioadmin" e2e-tests.yml` returns 0 matches
- [x] `grep "SPRING_PROFILES_ACTIVE" e2e-tests.yml` finds `docker`
- [x] `grep "push:" e2e-tests.yml` finds push trigger on main
- [x] `grep "Required GitHub" ci-cd.yml` finds secrets documentation
- [x] Both files are syntactically valid YAML

## Files Modified

| File | Action |
|------|--------|
| `oaiss-chain-backend/.github/workflows/ci-cd.yml` | Replaced OWASP with Trivy, fixed deploy-staging logic, fixed notify dependency, added secrets docs |
| `.github/workflows/e2e-tests.yml` | Added MinIO service, replaced credentials, added env vars, added push trigger |

## Threat Mitigations

| Threat | Mitigation |
|--------|------------|
| T-15-02-01: Hardcoded CI credentials | Replaced minioadmin with test-specific credentials |
| T-15-02-03: Missing MinIO causing silent test failures | Added MinIO service container with health check |
