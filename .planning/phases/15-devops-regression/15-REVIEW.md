---
phase: 15-devops-regression
reviewed: 2026-05-21T12:00:00Z
depth: deep
files_reviewed: 3
files_reviewed_list:
  - oaiss-chain-backend/src/main/resources/application-dev.yml
  - oaiss-chain-backend/.github/workflows/ci-cd.yml
  - .github/workflows/e2e-tests.yml
findings:
  critical: 2
  warning: 5
  info: 4
  total: 11
status: issues_found
---

# Phase 15: Code Review Report

**Reviewed:** 2026-05-21T12:00:00Z
**Depth:** deep
**Files Reviewed:** 3
**Status:** issues_found

## Summary

Phase 15 covers three sub-plans: (1) dev profile fix enabling Flyway + ddl-auto validate, (2) CI/CD pipeline fix replacing OWASP with Trivy, adding MinIO to E2E, fixing deploy logic, (3) E2E regression test execution (no code changes).

Two critical issues found: (1) E2E health check URL is wrong — `server.servlet.context-path: /api/v1` shifts actuator endpoints to `/api/v1/actuator/health`, but the workflow curls `/actuator/health`, causing a 2-minute timeout and blocking all E2E tests. (2) Slack notification step uses `if: ${{ secrets.SLACK_WEBHOOK != '' }}` which GitHub Actions never evaluates to true — secrets cannot be compared in `if` expressions, so notifications are permanently silently skipped.

## Critical Issues

### CR-01: E2E health check URL wrong — context-path shifts actuator endpoints

**File:** `.github/workflows/e2e-tests.yml:121`
**Issue:** The "Wait for backend" step curls `http://localhost:8080/actuator/health`, but `application.yml` sets `server.servlet.context-path: /api/v1`. In Spring Boot, when a context-path is configured, actuator endpoints resolve relative to it. The actual health endpoint is at `http://localhost:8080/api/v1/actuator/health`. The E2E workflow uses `SPRING_PROFILES_ACTIVE: docker`, and `application-docker.yml` does not override `server.servlet.context-path`, so the context-path from `application.yml` remains active.

The `timeout 120` command spins for 2 minutes hitting a 404 and then fails, causing every downstream E2E test step to be skipped. The entire E2E pipeline is effectively dead.

**Fix:**
```yaml
      - name: Wait for backend
        run: |
          timeout 120 bash -c 'until curl -sf http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1; do sleep 3; done'
          echo "Backend is healthy"
```

### CR-02: Slack notification `if` condition cannot compare secrets — notification never sent

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:249`
**Issue:** The step-level condition `if: ${{ secrets.SLACK_WEBHOOK != '' }}` is never true in GitHub Actions. GitHub explicitly forbids secret access in `if` expressions — secrets are only accessible within `run` step bodies via environment variable injection. The expression always evaluates to false, so Slack notifications are never sent regardless of whether `SLACK_WEBHOOK` is configured. The pipeline's build-failure notification mechanism is entirely non-functional.

**Fix:**
```yaml
      - name: Send Slack notification
        uses: 8398a7/action-slack@v3
        if: env.SLACK_WEBHOOK != ''
        env:
          SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
        with:
          status: ${{ needs.build.result }}
          text: |
            OAISS Chain Backend CI/CD Pipeline
            Repository: ${{ github.repository }}
            Branch: ${{ github.ref_name }}
            Commit: ${{ github.sha }}
            Author: ${{ github.actor }}
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## Warnings

### WR-01: Deploy job conditions should check `needs.docker-build.result`

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:200-201` and `:217-218`
**Issue:** Both `deploy-staging` and `deploy-production` have `needs: docker-build` and an `if` condition on branch ref. When `docker-build` is skipped (e.g., on a PR), the deploy jobs' `needs` evaluates to "skipped dependency" and deploy jobs are also skipped — correct behavior. However, the deploy conditions should explicitly check that `docker-build` succeeded, not just check the branch ref. This makes the intent explicit and protects against future `always()` additions.

**Fix:**
```yaml
  deploy-staging:
    needs: docker-build
    if: needs.docker-build.result == 'success' && github.ref == 'refs/heads/develop'
```

### WR-02: `notify` job reports its own status instead of the build's status

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:241`
**Issue:** The `notify` job uses `status: ${{ job.status }}`. The notify job itself is lightweight and almost always succeeds. Since `needs: [build]` with `if: always()` means notify runs regardless of build outcome, `job.status` reflects the notify job's own status, not the pipeline outcome. The Slack message will report "success" even when the build job failed.

**Fix:** Use `status: ${{ needs.build.result }}` to report the actual build outcome.

### WR-03: SpotBugs and PMD are `continue-on-error: true` — quality gates are cosmetic

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:100,105`
**Issue:** Both code quality steps have `continue-on-error: true`, meaning SpotBugs and PMD violations never fail the pipeline. Code with serious static analysis violations will be promoted to Docker build and deployment without any friction.

**Fix:** Remove `continue-on-error: true` or set to `false` to enforce quality gates.

### WR-04: MinIO health check uses `curl` which may not exist in the container

**File:** `.github/workflows/e2e-tests.yml:54`
**Issue:** The MinIO health check runs `curl -f http://localhost:9000/minio/health/live`. The `minio/minio:RELEASE.2025-04-22T22-12-26Z` image is a minimal Alpine-based image that may not include `curl`. If `curl` is absent, the health check will always fail, and GitHub Actions will report the MinIO service as unhealthy after retries, potentially blocking the entire E2E job.

**Fix:** Use `wget` instead of `curl`:
```yaml
        options: >-
          --health-cmd="wget -q -O /dev/null http://localhost:9000/minio/health/live || exit 1"
```

### WR-05: v1.1 E2E tests lack frontend dev server

**File:** `.github/workflows/e2e-tests.yml:136-138`
**Issue:** The v1.1 tests (`TEST_MODE=v1.1`) have `webServer: undefined` in their Playwright config, meaning they expect a pre-running frontend dev server. The workflow only starts the backend. No frontend dev server is started before the v1.1 test step, so these tests will fail.

**Fix:** Add a frontend dev server step before v1.1 tests, or ensure the Playwright config handles CI mode correctly for all test suites.

## Info

### IN-01: Duplicate Maven caching — `setup-java` cache and `actions/cache` overlap

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:46-54`
**Issue:** `setup-java` with `cache: maven` already handles Maven dependency caching. The separate `actions/cache@v4` step caches the same `~/.m2/repository` path, resulting in redundant cache operations. Same pattern in `e2e-tests.yml`.

### IN-02: Unused `MAVEN_VERSION` environment variable

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:28`
**Issue:** `MAVEN_VERSION: '3.9.5'` is declared in the workflow-level `env` block but never referenced. `setup-java` does not accept a Maven version parameter.

### IN-03: Deploy steps are placeholder echo commands

**File:** `oaiss-chain-backend/.github/workflows/ci-cd.yml:207-211,224-228`
**Issue:** Both deploy steps contain only `echo "Deploying..."`. These jobs succeed without actually deploying anything, giving false confidence in the pipeline dashboard.

### IN-04: V3 migration gap may require local schema rebuild for developers

**File:** `oaiss-chain-backend/src/main/resources/db/migration/` (cross-file analysis)
**Issue:** Flyway migrations jump from V2 to V4 (no V3). This is valid for Flyway but with `ddl-auto: validate` now active in dev, developers upgrading from the old `ddl-auto: update` dev profile may need to run `mvn flyway:clean flyway:migrate` to rebuild their local schema. Document this migration step.

---

_Reviewed: 2026-05-21T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
