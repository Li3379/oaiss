---
phase: 16-e2e-test-infra-fix
reviewed: 2026-05-22T12:00:00Z
depth: standard
files_reviewed: 5
files_reviewed_list:
  - oaiss-chain-frontend/tests/e2e/fixtures/auth.ts
  - oaiss-chain-frontend/tests/e2e/v1.1/d9-blockchain-browser.spec.js
  - oaiss-chain-frontend/tests/e2e/v1.1/d10-carbon-report.spec.js
  - oaiss-chain-frontend/tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts
  - oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts
findings:
  critical: 2
  warning: 4
  info: 2
  total: 8
status: issues_found
---

# Phase 16: Code Review Report

**Reviewed:** 2026-05-22T12:00:00Z
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

Reviewed 5 E2E test infrastructure files: auth fixture, two migrated spec files (d9, d10), a page object (BlockchainExplorerPage), and the blockchain-formula-flow spec. Two critical issues found: `buildStorageState` returns empty auth state that all d9/d10 describe blocks pass to `test.use({ storageState })`, making the declared storage state meaningless; and `isFabricAvailable()` calls a likely-auth-protected endpoint without credentials, causing all Fabric-dependent tests to be silently skipped. Four warnings cover inconsistent storage strategy between `loginViaToken`/`loginViaApi`, missing response status check before destructuring login response, unauthenticated blockchain API requests, and an unsafe tab index access in the page object.

## Critical Issues

### CR-01: buildStorageState returns empty auth state -- all test.use({ storageState }) is no-op

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:79-88`
**Issue:** `buildStorageState()` returns an object with an empty `localStorage` array and never injects a token. Every `test.describe` block in d9 and d10 passes `test.use({ storageState: buildStorageState('enterprise') })`, which tells Playwright to load zero authentication state into the browser context. The tests only pass because `loginViaToken` in `beforeEach` injects a mock token via `addInitScript` afterward. This is misleading and fragile -- if any test navigates before `loginViaToken` runs (e.g., via a redirect), it will hit the login wall with no stored credentials. The `role` parameter is also completely ignored.

**Fix:**
```typescript
export function buildStorageState(role = 'ENTERPRISE') {
  const token = getToken(role)
  return {
    origins: [
      {
        origin: 'http://localhost:5173',
        localStorage: [
          { name: 'access_token', value: token },
          { name: 'user_role', value: role },
        ],
      },
    ],
  }
}
```

### CR-02: isFabricAvailable() calls /blockchain/status without authentication -- skip guard always skips

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/test-env.ts:38-49`
**Issue:** `isFabricAvailable()` makes an unauthenticated GET to `/blockchain/status`. Given the backend's `SecurityConfig` and JWT filter architecture, this endpoint almost certainly requires a Bearer token. Without auth, the response will be 401, `response.ok()` returns false, and the function returns false. This means all Fabric-dependent tests in `blockchain-formula-flow.spec.ts` (lines 173-215) are silently skipped even when Fabric is actually running. The skip guard is broken by missing authentication.

**Fix:**
```typescript
export async function isFabricAvailable(): Promise<boolean> {
  try {
    // Login first to get a valid token
    const loginCtx = await request.newContext({ baseURL: API_BASE, timeout: 5000 })
    const loginResp = await loginCtx.post('/auth/login', {
      data: { username: 'admin', password: process.env.ADMIN_PASSWORD || 'admin123' },
    })
    if (!loginResp.ok()) { await loginCtx.dispose(); return false }
    const { data } = await loginResp.json()
    const token = data.accessToken

    const response = await loginCtx.get('/blockchain/status', {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 5000,
    })
    await loginCtx.dispose()
    if (!response.ok()) return false
    const body = await response.json()
    return body?.data?.connected === true
  } catch {
    return false
  }
}
```

## Warnings

### WR-01: Inconsistent token storage -- loginViaToken uses sessionStorage, loginViaApi uses localStorage

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:29-77`
**Issue:** `loginViaToken` stores the token in `sessionStorage` (line 33), while `loginViaApi` stores it in `localStorage` (line 57). If the frontend application reads the token from only one storage location, one of these login methods will silently fail to authenticate the page. The inconsistency means tests using `loginViaToken` (d9, d10) may not be exercising the same auth path as tests using `loginViaApi` (blockchain-formula-flow), making cross-test comparisons unreliable.

**Fix:** Pick one storage strategy and use it consistently. If the frontend reads from `localStorage` (as suggested by `rememberMe=true` logic in `loginViaApi`), update `loginViaToken` to use `localStorage` as well:
```typescript
// In loginViaToken, change sessionStorage to localStorage:
await page.addInitScript(
  (args) => {
    localStorage.setItem('access_token', args.token)
    localStorage.setItem('user_role', args.roleLabel)
  },
  { token, roleLabel: role },
)
```

### WR-02: loginViaApi destructures body.data without checking response status

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:49-50`
**Issue:** `loginViaApi` calls `response.json()` and immediately destructures `body.data.accessToken` without first checking `response.ok()` or `body.code`. If the login endpoint returns a non-200 status or an error envelope (e.g., `{ code: 1001, message: "invalid credentials" }`), `body.data` is undefined and destructuring throws a cryptic `TypeError: Cannot destructure property 'accessToken' of undefined`. The error message will not indicate that authentication failed.

**Fix:**
```typescript
const body = await response.json()
if (!response.ok() || body.code !== 200 || !body.data?.accessToken) {
  throw new Error(`Login failed: status=${response.status()}, body=${JSON.stringify(body)}`)
}
const { accessToken, refreshToken } = body.data
```

### WR-03: Blockchain API tests make requests without auth headers

**File:** `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts:177-192`
**Issue:** The "Blockchain API (REQ-05)" tests call `/blockchain/blocks/latest` and `/blockchain/transactions/latest` via `request.get()` with no `Authorization` header. If the backend requires JWT authentication for these endpoints (which the SecurityConfig and JWT filter architecture suggest), these requests will receive 401 instead of the expected 200/503, and the `expect([200, 503]).toContain(response.status())` assertion will fail with an unhelpful message.

**Fix:** Acquire a token before the test block and pass it as a Bearer header:
```typescript
test('should query latest blocks', async ({ request }) => {
  const loginResp = await request.post(`${API_BASE}/auth/login`, {
    data: { username: TEST_USERS.admin.username, password: TEST_USERS.admin.password },
  })
  const { accessToken } = (await loginResp.json()).data
  const response = await request.get(`${API_BASE}/blockchain/blocks/latest`, {
    params: { limit: 5 },
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  expect([200, 503]).toContain(response.status())
})
```

### WR-04: BlockchainExplorerPage.switchToTransactionsTab uses nth(1) without verifying tab count

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/BlockchainExplorerPage.ts:20-23`
**Issue:** `switchToTransactionsTab` clicks `tabs.nth(1)` without first asserting that at least 2 tabs exist. If the tab bar has not fully rendered or the component loads tabs asynchronously, `nth(1)` targets an empty locator and the click times out with an uninformative error. The method should either wait for tab visibility or assert the tab count before clicking.

**Fix:**
```typescript
async switchToTransactionsTab(): Promise<void> {
  const tabs = this.page.locator('.el-tabs__item')
  await expect(tabs).toHaveCount(2, { timeout: 5000 })
  await tabs.nth(1).click()
}
```

## Info

### IN-01: console.error in auth fixture

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:71`
**Issue:** `console.error('Failed to parse JWT:', e)` in the JWT parsing catch block. Test code should avoid console output that pollutes test runner logs. Consider removing or gating behind a debug flag.

**Fix:** Remove or replace with a silent no-op: `catch { /* JWT parse failed, skip expiry extraction */ }`

### IN-02: Hardcoded test password admin123 in TEST_USERS

**File:** `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts:19-22`
**Issue:** All four test users share the password `admin123` hardcoded in the fixture. While acceptable for test fixtures, if these credentials are also used in CI environments with real database seeds, they represent a known weak credential. Consider reading from `process.env.TEST_USER_PASSWORD` with a fallback.

**Fix:**
```typescript
const TEST_PASSWORD = process.env.TEST_USER_PASSWORD || 'admin123'
export const TEST_USERS = {
  admin: { username: 'admin', password: TEST_PASSWORD, role: 'ADMIN' },
  // ...
}
```

---

_Reviewed: 2026-05-22T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
