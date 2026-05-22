---
phase: 16-e2e-test-infra-fix
reviewed: 2026-05-22
iteration: 1
fix_scope: all
findings_in_scope: 8
fixed: 8
skipped: 0
status: all_fixed
---

# Phase 16: Code Review Fix Report

**Iteration:** 1
**Fix Scope:** all (Critical + Warning + Info)
**Status:** all_fixed

## Summary

All 8 findings from code review have been fixed and committed atomically.

## Fixes Applied

| ID | Severity | Description | Commit | Files |
|----|----------|-------------|--------|-------|
| CR-01 | Critical | `buildStorageState` now returns populated localStorage with token and role | `86321d6` | `auth.ts` |
| CR-02 | Critical | `isFabricAvailable()` now authenticates before checking `/blockchain/status` | `b991a49` | `test-env.ts` |
| WR-01 | Warning | Unified `loginViaToken` to use localStorage (was sessionStorage) | `fd59910` | `auth.ts` |
| WR-02 | Warning | `loginViaApi` checks `response.ok()` before destructuring | `9bb2fdb` | `auth.ts` |
| WR-03 | Warning | Blockchain API tests include auth headers | `a688962` | `blockchain-formula-flow.spec.ts` |
| WR-04 | Warning | `BlockchainExplorerPage.switchToTransactionsTab` asserts tab count before click | `7ca141d` | `BlockchainExplorerPage.ts` |
| IN-01 | Info | Removed `console.error` from JWT parse catch block | `6e8aced` | `auth.ts` |
| IN-02 | Info | Test password reads from `process.env.TEST_USER_PASSWORD` with fallback | `954f5c6` | `auth.ts` |

## Notes

- **CR-02** changes the authentication flow in the Fabric skip guard — it now performs a login before checking blockchain status. This changes runtime behavior and should be verified with a running backend.
- **WR-01** aligns `loginViaToken` with the frontend's localStorage-based token strategy (previously used sessionStorage, which the app doesn't read from).

---

_Reviewed: 2026-05-22_
_Fixer: gsd-code-fixer (auto)_
