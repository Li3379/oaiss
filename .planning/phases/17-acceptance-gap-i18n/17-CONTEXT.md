---
phase: 17-acceptance-gap-i18n
status: decided
decided: 2026-05-22
---

# Phase 17 CONTEXT: 验收缺口补齐与 i18n 清理

## Decisions

### D-01: GAP-01/GAP-02 Already Covered — Verification Only

**Decision:** No new test code needed. Tests and CORE_ENDPOINTS entries already exist.

**Evidence:**
- `blockchain-formula-flow.spec.ts` covers `/carbon/calculate/power-generation` (25-param) and `/carbon/calculate/power-grid` (9-param) — lines 10-146
- `ai-prediction-flow.spec.ts` covers `/emission/predict` with 5 test cases — lines 220-280
- `CORE_ENDPOINTS` lists all three endpoints — coverage-report.ts lines 69-73

**Why:** The original "gap" was that these tests couldn't RUN due to the auth fixture timeout. Phase 16 fixed the root cause (Content-Type header in loginViaApi). The tests exist, the entries exist — Phase 17 confirms they work.

**How to apply:** Plan 17-01 is verification-only. Run existing tests, document results.

### D-02: CarbonFormulaCalculatorPage.ts — Skip

**Decision:** Do NOT create CarbonFormulaCalculatorPage.ts.

**Why:** Two reasons:
1. The existing formula calculator frontend test (blockchain-formula-flow.spec.ts:151-168) uses direct page interactions — `page.goto()`, `page.getByRole('tab')`. No page object pattern needed.
2. d10-carbon-report.spec.js also uses inline selectors.
3. Creating a page object without a consumer is speculative (violates simplicity principle from Phase 16 D-03).

**How to apply:** If a future phase needs a formula calculator page object, create it then with a real consumer.

### D-03: I18N Scope — 4 Files, ~5-7 Instances

**Decision:** Extract hardcoded Chinese from these 4 files:

| # | File | Instances | Strings |
|---|------|-----------|---------|
| 1 | `views/admin/VerifyList.vue:123` | 1 | `'已上链'` in status map |
| 2 | `views/third-party/Monitor.vue:138` | 1 | `'审核通过率'` as t() fallback |
| 3 | `api/auth.ts:13-14` | 2 | `'用户名不能为空'`, `'密码不能为空'` |
| 4 | `api/emission.ts:21` | 1 | `'企业ID不能为空'` |

**Why these files:**
- Views first (user-visible labels/status)
- API validation messages that surface in Element Plus message boxes on high-traffic pages (login, emission prediction)
- Scope bounded to 4 files matching ROADMAP "4 files 7 places" (actual count ~5, close enough)

**How to apply:**
1. Add new i18n keys to `zh-CN.ts` and `en-US.ts` under existing sections
2. Replace hardcoded strings with `$t()` / `t()` calls in views
3. Import `i18n` instance in API files and use `i18n.global.t()` for validation messages
4. Verify no visual regression

### D-04: API Validation i18n Pattern

**Decision:** Use `i18n.global.t()` for API validation messages, importing the i18n instance.

**Why:** API files don't have Vue component context (no `useI18n()`). The i18n instance is a singleton, so importing and calling `i18n.global.t('key')` works. This is the standard pattern for non-component files.

**How to apply:**
```typescript
import i18n from '@/i18n'
// ...
return Promise.reject(new Error(i18n.global.t('auth.usernameRequired')))
```

## Pre-Resolved Items (No Discussion Needed)

| Item | Status | Reason |
|------|--------|--------|
| Carbon formula E2E tests | Already written | Phase 8/11 wrote them |
| Emission predict E2E tests | Already written | Phase 8/11 wrote them |
| CORE_ENDPOINTS entries | Already present | Added in Phase 12 |
| Auth fixture fix | Done in Phase 16 | Content-Type header |

## Scope Fences

- **IN SCOPE:** Verify existing GAP tests work, extract i18n from 4 files
- **OUT OF SCOPE:** New test code for GAP-01/GAP-02, CarbonFormulaCalculatorPage.ts, bulk API i18n migration
