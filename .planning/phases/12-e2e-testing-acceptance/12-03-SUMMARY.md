---
phase: 12-e2e-testing-acceptance
plan: 03
subsystem: testing
tags: [playwright, e2e, blockchain, carbon-accounting]

# Dependency graph
requires:
  - phase: 11-frontend-coverage
    provides: "Frontend API coverage and view completeness"
provides:
  - "D9: Blockchain browser E2E tests (11 tests: blocks tab, transactions tab, empty state, API integration)"
  - "D10: Carbon report lifecycle E2E tests (14 tests: report list, create dialog, empty state, API integration)"
affects: [12-e2e-testing-acceptance]

# Tech tracking
tech-stack:
  added: []
  patterns: [regex-route-matching, mock-api-by-url-path, el-table-body-wrapper-selector]

key-files:
  created:
    - oaiss-chain-frontend/tests/e2e/d9-blockchain-browser.spec.js
    - oaiss-chain-frontend/tests/e2e/d10-carbon-report.spec.js
  modified: []

key-decisions:
  - "Used regex-based route matching (page.route(/pattern/)) instead of glob patterns (page.route('**/path')) because Playwright on Windows fails to match deep glob paths like **/api/v1/blockchain/blocks/latest"
  - "Used .el-table__body-wrapper selector for Element Plus table assertions because el-table renders two separate <table> elements (header and body) causing strict mode violations with getByRole('table')"
  - "Carbon formula calculator E2E tests deferred -- no CarbonFormulaController or CarbonFormulaCalculator.vue exists in codebase (Phase 8 plans 08-02/08-03 not yet implemented)"
  - "Used el-tab-pane visibility selector for transaction tab assertions because both tab panes render simultaneously in DOM"

patterns-established:
  - "Regex route matching for API mocking on Windows Playwright"
  - "el-table__body-wrapper selector pattern for Element Plus table assertions"
  - "Per-URL-path mock routing with route.request().url() inspection"

requirements-completed: [REQ-05, REQ-06]

# Metrics
duration: 42min
completed: 2026-05-17
---

# Phase 12 Plan 03: Blockchain + Carbon Accounting E2E Tests Summary

**25 E2E tests covering blockchain browser (blocks/transactions tabs, empty state, API) and carbon report lifecycle (list, create dialog, submit/delete actions, empty state, API)**

## Performance

- **Duration:** 42 min
- **Started:** 2026-05-16T16:35:30Z
- **Completed:** 2026-05-17T00:17:31Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- 11 blockchain browser E2E tests: page rendering, blocks table with mock data, tab switching, transaction data display, empty state, pagination, API integration verification
- 14 carbon report lifecycle E2E tests: page rendering with breadcrumb, report table mock data, status tags, draft/pending button visibility, pagination, search form, create dialog, empty state, API integration
- Discovered and documented Playwright Windows glob pattern matching limitation requiring regex-based route patterns
- Discovered Element Plus dual-table rendering requiring .el-table__body-wrapper selector

## Task Commits

Each task was committed atomically:

1. **Task 1: Blockchain browser E2E tests** - `6971ce1` (feat)
2. **Task 2: Carbon report lifecycle E2E tests** - `693f730` (feat)

## Files Created
- `oaiss-chain-frontend/tests/e2e/d9-blockchain-browser.spec.js` - 11 tests for blockchain browser page (blocks tab, transactions tab, empty state, API integration)
- `oaiss-chain-frontend/tests/e2e/d10-carbon-report.spec.js` - 14 tests for carbon report lifecycle (report list, create dialog, empty state, API integration)

## Decisions Made
- Used regex-based `page.route(/pattern/)` instead of glob `page.route('**/path')` because Playwright on Windows cannot match deep glob paths (e.g., `**/api/v1/blockchain/blocks/latest` fails to intercept, while `**/api/v1/**` works)
- Used `.el-table__body-wrapper` CSS selector instead of `getByRole('table')` because Element Plus renders two `<table>` elements per el-table component (header table + body table), causing strict mode violations
- Carbon formula calculator E2E tests deferred to a future plan since Phase 8 (08-02/08-03) for power generation and grid industry formulas has not been implemented yet

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replaced glob-based API route mocking with regex-based route mocking**
- **Found during:** Task 1 (blockchain browser tests)
- **Issue:** Playwright on Windows fails to intercept requests when using deep glob patterns like `**/api/v1/blockchain/blocks/latest`. The `setupApiMock` helper uses glob patterns which silently fail, causing API requests to hit the real backend (returning 401 and triggering auth redirects)
- **Fix:** Replaced glob pattern approach with regex-based `page.route(/\/api\/v1\/blockchain\//, handler)` that correctly intercepts matching requests on Windows
- **Files modified:** d9-blockchain-browser.spec.js
- **Commit:** 6971ce1

**2. [Rule 1 - Bug] Replaced getByRole('table') with .el-table__body-wrapper selector**
- **Found during:** Task 2 (carbon report tests)
- **Issue:** Element Plus el-table renders two separate `<table>` elements (one for header, one for body). Playwright's `getByRole('table')` finds both, causing strict mode violation errors
- **Fix:** Used `.el-table__body-wrapper` CSS selector to target only the body table, avoiding strict mode violations
- **Files modified:** d10-carbon-report.spec.js, d9-blockchain-browser.spec.js
- **Commit:** 6971ce1, 693f730

### Deferred Items

**Carbon formula calculator E2E tests** -- No CarbonFormulaController (backend) or CarbonFormulaCalculator.vue (frontend) exists in the codebase. These are planned under Phase 8 (plans 08-02 and 08-03) which have not been implemented yet. E2E tests for this feature should be written when the feature is implemented.

---

**Total deviations:** 2 auto-fixed (2 bugs discovered during test development)
**Impact on plan:** Minor -- worked around Playwright/Windows and Element Plus quirks. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Blockchain browser E2E tests provide coverage for REQ-05 (blockchain on-chain query)
- Carbon report lifecycle tests provide coverage for REQ-06 (carbon accounting -- report creation/submission flow)
- Carbon formula calculator E2E tests deferred pending Phase 8 implementation
- Plans 12-04 through 12-06 can proceed with their respective E2E test coverage

## Self-Check: PASSED

- [x] d9-blockchain-browser.spec.js exists at oaiss-chain-frontend/tests/e2e/d9-blockchain-browser.spec.js
- [x] d10-carbon-report.spec.js exists at oaiss-chain-frontend/tests/e2e/d10-carbon-report.spec.js
- [x] Commit 6971ce1 found in git log
- [x] Commit 693f730 found in git log
- [x] All 25 new tests pass (verified with npx playwright test)
- [x] No regressions in existing passing tests (d1-d8 pre-existing failures are out of scope)

---
*Phase: 12-e2e-testing-acceptance*
*Completed: 2026-05-17*
