# RESEARCH: Phase 15 — DevOps 与回归验证

## Scope

Phase 15 covers the final 2 active requirements plus full E2E regression:
- **OPS-01**: GitHub Actions CI/CD pipeline
- **OPS-02**: Dev profile Flyway fix (disable ddl-auto:update, enable Flyway)
- **Full E2E regression** after v2.0 changes (Phases 13-14)

## OPS-01: CI/CD Pipeline

### Current State

Two workflow files exist:

1. **`oaiss-chain-backend/.github/workflows/ci-cd.yml`** — Backend CI/CD
   - Stages: Build & Test → Code Quality (SpotBugs, PMD) → Security (OWASP) → Docker Build → Deploy
   - Issues found:
     - `deploy-staging` has condition `github.ref == 'refs/heads/develop'` but depends on `docker-build` which only runs on `main` — **dead code / logic bug**
     - Deploy steps are placeholder (`echo` only)
     - SpotBugs/PMD plugins need verification in pom.xml (may not be configured)
     - OWASP `continue-on-error: true` hides vulnerabilities
     - Docker Hub push requires secrets (`DOCKER_USERNAME`, `DOCKER_PASSWORD`) not documented
     - Slack notification requires `SLACK_WEBHOOK` secret

2. **`.github/workflows/e2e-tests.yml`** — Root-level E2E
   - Triggers on PR only (not push)
   - Missing `SPRING_PROFILES_ACTIVE` env var for backend startup
   - Hardcoded `MINIO_ACCESS_KEY: minioadmin` in workflow (weak credentials)
   - No MinIO service container — file upload tests will fail
   - No ML service container — AI prediction tests will fail or need mocking
   - Runs smoke tests + v1.1 tests, but not flow tests in CI
   - Backend health check uses `http://localhost:8080/actuator/health`

### What Needs Fixing

- Fix deploy-staging dependency (either remove or fix docker-build trigger condition)
- Add MinIO service container to E2E workflow
- Set proper `SPRING_PROFILES_ACTIVE` for E2E backend
- Replace hardcoded MinIO credentials with secrets
- Add flow tests to CI E2E step
- Document required GitHub secrets
- Verify SpotBugs/PMD plugins exist in pom.xml

### pom.xml Plugin Check

Need to verify if `spotbugs-maven-plugin` and `pmd-maven-plugin` are configured. If not, the code-quality job will fail.

## OPS-02: Dev Profile Flyway Fix

### Current State

| Profile | ddl-auto | flyway.enabled | Status |
|---------|----------|---------------|--------|
| default (application.yml) | validate | true | CORRECT |
| dev (application-dev.yml) | **update** | **false** | WRONG |
| docker (application-docker.yml) | validate | true (repair) | CORRECT |
| local (application-local.yml) | inherits validate | inherits true | CORRECT |

### The Problem

`application-dev.yml` disables Flyway and uses Hibernate `ddl-auto: update`. This means:
- Schema changes bypass Flyway migrations
- Schema drift between dev and other environments
- Flyway migrations are not tested during development
- If a developer runs with dev profile, they get a different schema than what production uses

### Fix

Change `application-dev.yml`:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # was: update
  flyway:
    enabled: true            # was: false (was not present, defaults false since overridden)
```

This is a 2-line change. Low risk, high impact.

## E2E Regression

### Test Inventory

| Category | Files | Tests |
|----------|-------|-------|
| smoke/ | 4 (admin, enterprise, reviewer, third-party + admin.verify) | ~20 |
| flows/ | 12 (auth, carbon-report, auction, p2p-trade, carbon-coin, etc.) | ~60 |
| v1.1/ | 5 (ai-prediction, blockchain-formula, certificate, frontend-coverage, regression) | ~25 |
| **Total** | **21 spec files** | **~105 tests** |

### v2.0 Risk Areas

Changes in Phases 13-14 that could affect E2E:
- Phase 13: Distributed locks, optimistic locking, credential externalization, @PreAuthorize additions
  - Risk: Auth failures if tokens invalid, permission changes block previously working flows
- Phase 14: Redis SCAN, async cache preload, FK indexes, RSA key encryption, @Transactional(readOnly)
  - Risk: RSA key encryption may break signature tests, cache preload timing changes

### Regression Strategy

1. Run all smoke tests — verify basic functionality
2. Run all flow tests — verify end-to-end business flows
3. Run all v1.1 tests — verify AI/blockchain/certificate features
4. Document any failures and root-cause analysis

## Plan Structure Recommendation

| Plan | Scope | Requirements | Complexity |
|------|-------|-------------|------------|
| 15-01 | Dev Profile Fix | OPS-02 | Simple (2 lines) |
| 15-02 | CI/CD Pipeline Fix | OPS-01 | Medium |
| 15-03 | E2E Regression | 全量回归 | Medium |

All plans are Wave 1 (no inter-dependencies) and can be executed in parallel, though 15-03 should ideally run after 15-01 and 15-02 to test against the fixed configuration.

## Dependencies

- Phase 15 depends on Phase 14 (complete)
- No external tool dependencies beyond existing GitHub Actions, Maven, npm, Playwright
