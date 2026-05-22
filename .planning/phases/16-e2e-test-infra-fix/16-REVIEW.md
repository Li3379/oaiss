---
phase: 16-e2e-test-infra-fix
reviewed: 2026-05-22T14:52:00+08:00
depth: deep
files_reviewed: 6
files_reviewed_list:
  - oaiss-chain-frontend/tests/e2e/fixtures/auth.ts
  - oaiss-chain-frontend/tests/e2e/fixtures/test-env.ts
  - oaiss-chain-frontend/tests/e2e/v1.1/d9-blockchain-browser.spec.js
  - oaiss-chain-frontend/tests/e2e/v1.1/d10-carbon-report.spec.js
  - oaiss-chain-frontend/tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts
  - oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts
findings:
  critical: 3
  warning: 4
  info: 2
  total: 9
status: issues_found
---

# Phase 16: Deep Code Review Report

**Reviewed:** 2026-05-22T14:52:00+08:00
**Depth:** deep
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Deep cross-file analysis of the E2E test infrastructure, tracing the full token lifecycle from fixture through Playwright storageState into the Vue app's auth utility and Pinia store. Previous standard review found 8 issues (all fixed). This deep pass found 3 new critical issues and 4 warnings.

The most significant finding is a **storage mechanism mismatch**: `loginViaToken` and `loginViaApi` both write `access_token` to `localStorage`, but `AuthMonitor.hasValidToken()` (auth-monitor.ts line 366) checks `sessionStorage`. Since `loginWithMonitor` in the same `auth.ts` file creates an AuthMonitor and then calls `loginViaApi`, the monitor's `hasValidToken` helper will always return `false`. Additionally, `buildStorageState` hardcodes the origin as `http://localhost:5173`, which breaks when `BASE_URL` is overridden in CI or Docker environments. Finally, `getToken(role)` accepts lowercase role strings but `MOCK_TOKENS` keys are uppercase, causing silent fallback to the ENTERPRISE token.

Cross-file call chain traced:
- `loginViaToken` -> `getToken` -> `MOCK_TOKENS` (case mismatch at boundary)
- `loginViaToken` -> `page.addInitScript` -> `localStorage.setItem` (used by d9, d10)
- `buildStorageState` -> `getToken` -> Playwright `storageState` (hardcoded origin)
- `loginViaApi` -> `page.evaluate` -> `localStorage.setItem` (used by blockchain-formula-flow)
- `loginWithMonitor` -> `new AuthMonitor` + `loginViaApi` (monitor checks sessionStorage, login writes localStorage)
- Vue app: `getAccessToken` checks both `localStorage` and `sessionStorage` (forgiving)
- Vue app: `resolveInitialState` -> `parseJwtPayload` extracts role from JWT, never reads `user_role` from storage

## Critical Issues

### CR-01: Storage mismatch between login fixtures and AuthMonitor.hasValidToken

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:35` (loginViaToken writes localStorage) vs `oaiss-chain-frontend/tests/e2e/fixtures/auth-monitor.ts:366` (hasValidToken checks sessionStorage)

**Issue:** `loginViaToken` (auth.ts line 35) writes the access token to `localStorage`:
```ts
localStorage.setItem('access_token', args.token)
```
`loginViaApi` (auth.ts line 62) also writes to `localStorage`. However, `AuthMonitor.hasValidToken()` (auth-monitor.ts line 366) checks only `sessionStorage`:
```ts
const token = await page.evaluate(() => sessionStorage.getItem('access_token'))
```
The `loginWithMonitor` function (auth.ts lines 109-119) creates an `AuthMonitor` and then calls `loginViaApi`. Any test using `loginWithMonitor` or calling `hasValidToken` after `loginViaToken`/`loginViaApi` will get a false negative -- the monitor reports "no valid token" even though the token exists in localStorage. This defeats the purpose of auth health monitoring in E2E tests.

The app's own `getAccessToken()` (src/utils/auth.ts lines 88-99) correctly checks both storages, so the app itself works. The test infrastructure is inconsistent with the app's behavior.

**Fix:** Update `hasValidToken` in auth-monitor.ts to check both storages, matching the app's `getAccessToken()` behavior:
```ts
export async function hasValidToken(page: Page): Promise<boolean> {
  const token = await page.evaluate(() => {
    return localStorage.getItem('access_token') || sessionStorage.getItem('access_token')
  })
  return !!token && token.length > 20
}
```

### CR-02: buildStorageState hardcodes origin URL -- breaks when BASE_URL is overridden

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:89`

**Issue:** `buildStorageState` returns a Playwright `storageState` object with a hardcoded origin:
```ts
origin: 'http://localhost:5173',
```
The Playwright config (playwright.config.ts line 28) reads `baseURL` from `process.env.BASE_URL || 'http://localhost:5173'`. When `BASE_URL` is set to a different value (e.g., `http://frontend:5173` in Docker Compose, or a staging URL in CI), Playwright opens pages at the configured `baseURL` but the `storageState` writes tokens to `http://localhost:5173`'s localStorage. Since browser localStorage is origin-scoped, the token will not be available to the page under test. All d9 and d10 tests would fail with login redirects.

The `loginViaToken` function does NOT have this problem because `addInitScript` runs in the page context after navigation, writing to whatever origin the page loaded from. But `test.use({ storageState })` applies the state before the page is created using the hardcoded origin.

**Fix:** Derive the origin from the same environment variable:
```ts
export function buildStorageState(role = 'ENTERPRISE') {
  const token = getToken(role)
  const baseUrl = process.env.BASE_URL || 'http://localhost:5173'
  return {
    origins: [
      {
        origin: baseUrl,
        localStorage: [
          { name: 'access_token', value: token },
          { name: 'user_role', value: role },
        ],
      },
    ],
  }
}
```

### CR-03: getToken silently falls back to ENTERPRISE for case-mismatched role strings

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:27-29`

**Issue:** `getToken` looks up `MOCK_TOKENS[role]` with no case normalization:
```ts
export function getToken(role: string): string {
  return MOCK_TOKENS[role] || MOCK_TOKENS.ENTERPRISE
}
```
The `MOCK_TOKENS` keys are uppercase (`ENTERPRISE`, `ADMIN`, `REVIEWER`, `THIRD_PARTY`). All callers pass lowercase strings: `loginViaToken(page, 'enterprise')` and `buildStorageState('enterprise')` in d9/d10 specs. This means `MOCK_TOKENS['enterprise']` is `undefined`, and the function silently returns the ENTERPRISE token as a fallback.

Currently this is harmless because the d9/d10 tests do intend to use ENTERPRISE. However, if a test is written for a different role -- e.g., `buildStorageState('admin')` -- it would silently use the ENTERPRISE token. The test would pass with the wrong auth context, masking real authorization bugs.

Additionally, the `user_role` localStorage value is set to the lowercase string `'enterprise'`, which does not match any valid `RoleType` in the app. The app ignores this value (it extracts roles from JWT), but it indicates the role parameter is not being validated.

**Fix:** Normalize the role to uppercase or validate it:
```ts
export function getToken(role: string): string {
  const normalized = role.toUpperCase()
  if (!MOCK_TOKENS[normalized]) {
    throw new Error(`Unknown role: ${role}. Valid: ${Object.keys(MOCK_TOKENS).join(', ')}`)
  }
  return MOCK_TOKENS[normalized]
}
```
Alternatively, update all callers to pass uppercase strings (`'ENTERPRISE'` instead of `'enterprise'`).

## Warnings

### WR-01: loginViaToken writes phantom user_role key that the app never reads

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:36`

**Issue:** `loginViaToken` sets `localStorage.setItem('user_role', args.roleLabel)` and `buildStorageState` includes `{ name: 'user_role', value: role }`. However, no part of the application reads `user_role` from storage. The app resolves user role exclusively from the JWT payload via `parseJwtPayload` in the Pinia store (src/store/index.ts line 19). This dead data can mislead developers debugging test failures into thinking the app uses a `user_role` storage key.

**Fix:** Remove the `user_role` writes from both `loginViaToken` (line 36) and `buildStorageState` (line 92). They serve no functional purpose.

### WR-02: Redundant dual auth setup in d9 and d10 specs

**File:** `oaiss-chain-frontend/tests/e2e/v1.1/d9-blockchain-browser.spec.js:75,79` and `oaiss-chain-frontend/tests/e2e/v1.1/d10-carbon-report.spec.js:60,64`

**Issue:** Both spec files configure `test.use({ storageState: buildStorageState('enterprise') })` at the describe level AND call `await loginViaToken(page, 'enterprise')` inside `beforeEach`. These are two independent auth mechanisms that both write `access_token` to localStorage. The `addInitScript` from `loginViaToken` overrides the `storageState` value on every navigation. This is not a functional bug (the tests pass), but it creates maintenance confusion: removing either one individually would appear safe but changes the auth mechanism. If CR-02 is fixed and `buildStorageState` starts using the correct origin, the `storageState` approach alone would be sufficient and `loginViaToken` in `beforeEach` becomes redundant.

**Fix:** Pick one mechanism and use it consistently. Since these tests mock API responses (no real backend), `loginViaToken` alone is sufficient. Remove `test.use({ storageState: ... })` from the describe blocks.

### WR-03: BlockchainExplorerPage page object is never imported by any spec

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts`

**Issue:** The `BlockchainExplorerPage` class is defined in the page-objects directory but is never imported by any spec file in the test suite. Grep across all `**/*.spec.*` files confirms zero imports. The d9-blockchain-browser spec uses inline locator patterns (e.g., `page.locator('.el-table__body-wrapper').first()`) instead of this page object. This is dead code that adds maintenance burden without providing test value. The page object's methods (`switchToTransactionsTab`, `expectBlocksTable`) are well-structured and would improve the spec's readability if used.

**Fix:** Either refactor d9-blockchain-browser.spec.js to use the page object (preferred -- centralizes selectors), or remove the unused file to avoid confusion.

### WR-04: blockchain-formula-flow frontend tests navigate twice via loginViaApi

**File:** `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts:152-153`

**Issue:** The frontend tests (lines 151-168) call `loginViaApi` which internally calls `page.goto(BASE_URL)` (auth.ts line 58), navigating to the app root. The test then immediately calls `page.goto('/enterprise/carbon-formula-calculator')` (line 153). This double-navigation adds latency. More importantly, when `loginViaApi` navigates to the root, the router guard (src/router/index.ts line 193) checks `appStore.loggedIn`. The Pinia store's `resolveInitialState()` reads from localStorage during store creation, so this typically works. However, the timing between `page.evaluate` writing to storage and the Vue app initializing is fragile -- if the app's JS bundle loads before the evaluate completes, the store would see no token and redirect to `/login`. Using `loginViaToken` (which uses `addInitScript` to inject before any JS runs) would be safer for these frontend-only tests.

**Fix:** For frontend tests that mock API responses, use `loginViaToken` instead of `loginViaApi`. Reserve `loginViaApi` for tests that need a real backend session.

## Info

### IN-01: test-env.ts skipIfServiceUnavailable is a trivial wrapper that misleads callers

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/test-env.ts:59-64`

**Issue:** `skipIfServiceUnavailable(condition, reason)` simply returns `{ condition, reason }` -- an identity function. The doc comment (line 53) shows example usage `test.skip(await !isMlServiceAvailable(), 'ML service not available')`, which is incorrect because `test.skip` accepts `(condition, description)` not an object. No current file actually calls `skipIfServiceUnavailable`; `blockchain-formula-flow.spec.ts` calls `isFabricAvailable()` directly with `test.skip`.

**Fix:** Remove the unused function, or fix its doc comment and make it call `test.skip` directly.

### IN-02: MOCK_TOKENS have .mock signature suffix -- structurally valid but not cryptographically valid JWTs

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:7-16`

**Issue:** The mock tokens end with `.mock` (e.g., `...OTk5OTk5OTk5fQ.mock`). The app's `parseJwtPayload` (src/utils/auth.ts) splits on `.` and `atob`s each part -- the header and payload decode correctly, but the signature part `mock` is not valid base64 (it happens to decode but is meaningless). This is fine for tests that mock API responses, but if the app ever adds client-side signature validation, these tokens would be rejected. Worth documenting.

**Fix:** Add a comment on the `MOCK_TOKENS` constant noting these are structurally-valid-but-cryptographically-invalid JWTs intended for mocked-API tests only.

---

_Reviewed: 2026-05-22T14:52:00+08:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
