---
status: complete
phase: 16-e2e-test-infra-fix
source: 16-01-SUMMARY.md, 16-02-SUMMARY.md, 16-REVIEW-FIX.md
started: 2026-05-22T14:15:00+08:00
updated: 2026-05-22T14:20:00+08:00
---

## Current Test

[testing complete]

## Tests

### 1. loginViaApi sends JSON Content-Type
expected: loginViaApi() POST includes Content-Type: application/json header.
result: pass
evidence: auth.ts:48 — `headers: { 'Content-Type': 'application/json' }` present

### 2. loginViaApi checks response status before destructuring
expected: If login fails, clear error message thrown instead of cryptic TypeError.
result: pass
evidence: auth.ts:52 — `if (!response.ok() || body.code !== 200 || !body.data?.accessToken)` guard exists before destructuring

### 3. buildStorageState returns populated auth state
expected: buildStorageState('ENTERPRISE') returns origins with localStorage containing access_token and user_role.
result: pass
evidence: auth.ts:84-97 — returns origins with localStorage array containing token and role entries

### 4. loginViaToken uses localStorage (unified with loginViaApi)
expected: loginViaToken stores token in localStorage, not sessionStorage.
result: pass
evidence: auth.ts:35 — `localStorage.setItem('access_token', args.token)` in addInitScript

### 5. isFabricAvailable authenticates before checking endpoint
expected: isFabricAvailable() logs in first, then calls /blockchain/status with Bearer header.
result: pass
evidence: test-env.ts:28-48 — loginCtx.post('/auth/login') then loginCtx.get('/blockchain/status', { headers: { Authorization: `Bearer ${token}` } })

### 6. d9/d10 specs discoverable in v1.1/ testDir
expected: Files exist at tests/e2e/v1.1/d9-blockchain-browser.spec.js and d10-carbon-report.spec.js.
result: pass
evidence: Both files confirmed present in v1.1/ directory via Glob

### 7. BlockchainExplorerPage tab count assertion
expected: switchToTransactionsTab() asserts tabs count >= 2 before clicking nth(1).
result: pass
evidence: BlockchainExplorerPage.ts:22 — `await expect(tabs).toHaveCount(2, { timeout: 5000 })` before nth(1).click()

### 8. Blockchain API tests include auth headers
expected: Blockchain API test requests include Authorization: Bearer header.
result: pass
evidence: blockchain-formula-flow.spec.ts:178-184,192-198 — login + `headers: { Authorization: \`Bearer ${accessToken}\` }` on all blockchain API requests

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]
