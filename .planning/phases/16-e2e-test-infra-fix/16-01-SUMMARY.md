---
plan: 16-01
phase: 16-e2e-test-infra-fix
status: complete
started: 2026-05-22T09:15:00+08:00
completed: 2026-05-22T09:15:30+08:00
---

# Plan 16-01: Auth Fixture Fix — Summary

## Objective
Fix `loginViaApi()` sending form-encoded data instead of JSON, causing 30s timeout on Spring Boot `@RequestBody` endpoints.

## What Was Done

### Task 1: Add Content-Type header
- **File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts`
- **Change:** Added `headers: { 'Content-Type': 'application/json' }` to `page.request.post()` call at line 46
- **Root cause:** Playwright's `page.request.post(url, { data: obj })` defaults to `application/x-www-form-urlencoded`. Spring Boot's `AuthController.login()` expects `@RequestBody` JSON.
- **Commit:** `073386a fix(16-01): add Content-Type header to loginViaApi()`

## Key Files
- `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts` — 1 line added (Content-Type header)

## Deviations
None — single-line fix executed exactly as planned.

## Self-Check: PASSED
- [x] Change matches plan specification
- [x] Only auth.ts modified, no other files touched
- [x] loginViaToken() and loginWithMonitor() unchanged
- [x] No token storage changes (D-02 honored)

---
*Plan: 16-01 | Phase: 16-e2e-test-infra-fix | E2E-01*
