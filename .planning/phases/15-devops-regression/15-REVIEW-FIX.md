---
phase: 15-devops-regression
fix_date: 2026-05-21
findings_addressed: 9
findings_skipped: 2
skipped_reasons:
  IN-03: Placeholder deploy steps are intentional — no actual deployment infrastructure exists yet
  IN-04: Documentation-only item — V3 migration gap is informational, Flyway handles non-sequential versions
status: fixed
---

# Phase 15: Code Review Fix Report

**Fix Date:** 2026-05-21
**Findings Addressed:** 9 of 11 (2 skipped as intentional/documentation-only)

## Fixes Applied

| Finding | Fix | File |
|---------|-----|------|
| CR-01: E2E health check URL wrong (context-path) | Changed `/actuator/health` → `/api/v1/actuator/health` | e2e-tests.yml:121 |
| CR-02: Slack notification `if` cannot compare secrets | Changed to `if: env.SLACK_WEBHOOK != ''` with env mapping; status now uses `needs.build.result` | ci-cd.yml:227-240 |
| WR-01: Deploy conditions don't check docker-build result | Added `needs.docker-build.result == 'success'` to both deploy conditions | ci-cd.yml:190,207 |
| WR-02: Notify reports own status instead of build's | Changed `job.status` → `needs.build.result` | ci-cd.yml:233 |
| WR-03: SpotBugs/PMD continue-on-error bypasses quality gate | Removed `continue-on-error: true` from both steps | ci-cd.yml:88-94 |
| WR-04: MinIO health check uses curl (may not exist) | Changed to `wget -q -O /dev/null` | e2e-tests.yml:54 |
| WR-05: v1.1 E2E tests lack frontend dev server | Added `npx vite --port 5173 &` step before v1.1 tests | e2e-tests.yml:134-137 |
| IN-01: Duplicate Maven caching in ci-cd.yml | Removed redundant `actions/cache` step (setup-java `cache: maven` is sufficient) | ci-cd.yml |
| IN-02: Unused MAVEN_VERSION env var | Removed `MAVEN_VERSION: '3.9.5'` from env block | ci-cd.yml:27 |

## Skipped

| Finding | Reason |
|---------|--------|
| IN-03: Deploy steps are placeholder | Intentional — no deployment infrastructure exists yet. Comment already documents this. |
| IN-04: V3 migration gap | Flyway handles non-sequential version numbers. Developers may need `flyway:clean flyway:migrate` on first use — documented in REVIEW.md. |

---
_Fixed: 2026-05-21_
