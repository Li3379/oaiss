---
plan: 16-02
phase: 16-e2e-test-infra-fix
status: complete
started: 2026-05-22T09:16:00+08:00
completed: 2026-05-22T09:18:00+08:00
---

# Plan 16-02: Test Discovery Fix — Summary

## Objective
Migrate d9/d10 orphaned specs into v1.1/ testDir, create missing page objects, and wire isFabricAvailable() into blockchain tests.

## What Was Done

### Task 1: Migrate d9/d10 specs to v1.1/
- Moved `d9-blockchain-browser.spec.js` and `d10-carbon-report.spec.js` from `tests/e2e/` to `tests/e2e/v1.1/`
- Files now discoverable by Playwright when `TEST_MODE=v1.1`

### Task 2: Fix import paths
- Changed `'./fixtures/auth'` → `'../fixtures/auth'` in both migrated files
- Only import in both files is `buildStorageState` and `loginViaToken`

### Task 3: Create BlockchainExplorerPage.ts
- Created `tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts`
- Methods: `goto()`, `expectLoaded()`, `expectBlocksTable()`, `switchToTransactionsTab()`, `expectTransactionsTable()`, `expectEmptyState()`
- Follows existing page object pattern (constructor + goto + expectLoaded + domain methods)

### Task 4: Wire isFabricAvailable()
- Added `isFabricAvailable` import to `blockchain-formula-flow.spec.ts` (alongside existing `isMlServiceAvailable`)
- Added `test.beforeAll` with `test.skip(!(await isFabricAvailable()), 'Fabric network not available')` to Blockchain API (REQ-05) test.describe block
- Non-blockchain tests (carbon formula) unaffected — only Fabric-dependent tests skip

## Key Files
- `oaiss-chain-frontend/tests/e2e/v1.1/d9-blockchain-browser.spec.js` — migrated + import fixed
- `oaiss-chain-frontend/tests/e2e/v1.1/d10-carbon-report.spec.js` — migrated + import fixed
- `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts` — new page object
- `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts` — isFabricAvailable wired
- **Commit:** `37a8a84 fix(16-02): migrate d9/d10 specs to v1.1, create BlockchainExplorerPage, wire isFabricAvailable`

## Deviations
- CarbonFormulaCalculatorPage.ts **deferred** to Phase 17 — d10 uses inline selectors, not page objects. Creating it now would be speculative (violates simplicity principle).
- d9 blockchain test kept without isFabricAvailable skip — it uses mocked API responses, doesn't need real Fabric.

## Self-Check: PASSED
- [x] d9/d10 no longer at tests/e2e/ root, now in v1.1/
- [x] Import paths corrected to ../fixtures/auth
- [x] BlockchainExplorerPage.ts follows existing pattern
- [x] isFabricAvailable imported and used in blockchain-formula-flow.spec.ts
- [x] No TypeScript conversion (D-03 honored)

---
*Plan: 16-02 | Phase: 16-e2e-test-infra-fix | E2E-02, E2E-03*
