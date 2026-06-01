---
phase: 17-acceptance-gap-i18n
depth: deep
reviewed: 2026-05-22T12:00:00Z
status: issues_found
findings_total: 8
critical: 1
warning: 5
info: 2
files_reviewed_list:
  - oaiss-chain-frontend/src/views/admin/VerifyList.vue
  - oaiss-chain-frontend/src/views/third-party/Monitor.vue
  - oaiss-chain-frontend/src/api/auth.ts
  - oaiss-chain-frontend/src/api/emission.ts
  - oaiss-chain-frontend/src/i18n/locales/zh-CN.ts
  - oaiss-chain-frontend/src/i18n/locales/en-US.ts
---

# Phase 17: Code Review Report

**Reviewed:** 2026-05-22T12:00:00Z
**Depth:** deep
**Files Reviewed:** 6
**Status:** issues_found

## Summary

This review covers Phase 17-02 i18n cleanup: extracting 5 hardcoded Chinese strings from 4 frontend files into vue-i18n translation keys. While the mechanical key extraction is correct (keys exist in both locale files, imports resolve), deep cross-file analysis reveals a **critical i18n reactivity bug** in Monitor.vue where translation calls in a static `statusMap` object will never update on locale change, a **missing status code mapping** in VerifyList.vue that will render raw numbers for status 4, and several quality issues including a silent error swallow in Monitor.vue, inconsistent i18n access patterns across API vs component files, and ~14 remaining hardcoded Chinese strings in other API files that were not addressed by this phase.

## Critical Issues

### CR-01: Monitor.vue statusMap is non-reactive -- i18n locale changes silently ignored

**File:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue:76-83`
**Issue:** The `statusMap` object is declared as a plain `const` at `<script setup>` scope, outside any reactive wrapper. It calls `t('monitor.statusPending')`, `t('monitor.statusApproved')`, and `t('monitor.statusRejected')` during initialization. Because `statusMap` is a plain object (not `ref`, `reactive`, or `computed`), the `t()` return values are captured once at component setup time and frozen. When the user switches locale, Vue re-renders the template, but `statusMap` is never recomputed -- the old locale strings persist until the component is destroyed and remounted. This means the status labels displayed in the table will be stale/wrong after a locale switch.
**Cross-file impact:** Any component that consumes `statusMap` (template lines 13-14 via `statusMap[row.status]`) will display incorrect text. The zh-CN and en-US locale files correctly define the keys, but the reactivity disconnect means they will not be used after the first render.
**Fix:**
```typescript
// BEFORE (non-reactive, values frozen at init):
const statusMap: Record<number, string> = {
  1: t('monitor.statusPending'),
  2: t('monitor.statusApproved'),
  3: t('monitor.statusRejected')
}

// AFTER (computed, re-evaluates when locale changes):
const statusMap = computed(() => ({
  1: t('monitor.statusPending'),
  2: t('monitor.statusApproved'),
  3: t('monitor.statusRejected')
}))
// Template must change: statusMap[row.status] -> statusMap.value[row.status]
```

## Warnings

### WR-01: VerifyList.vue missing status code 4 in getStatusLabel and getStatusType

**File:** `oaiss-chain-frontend/src/views/admin/VerifyList.vue:122-130`
**Issue:** The status comment at line 121 documents `4=认证驳回` (certification rejected), but neither `getStatusLabel` (line 122) nor `getStatusType` (line 128) include a case for `status === 4`. When `getStatusLabel(4)` is called, it falls through to the default branch and returns the raw number `4`, which renders as "4" in the UI. Similarly `getStatusType(4)` returns `'info'` instead of a semantically correct type like `'danger'` or `'warning'`. The new i18n key `verifyList.statusOnChain` was added for status 3, but status 4 remains unmapped.
**Cross-file impact:** Backend `TradeController` and `TradeService` can produce status 4 records. Any such records will display incorrectly in VerifyList.vue with a raw number instead of a localized label.
**Fix:**
```typescript
// In getStatusLabel, add:
4: t('verifyList.statusCertRejected'),  // need to add key to locale files

// In getStatusType, add:
4: 'danger',  // or 'warning' per design
```

### WR-02: Monitor.vue loadStatistics silently swallows errors

**File:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue:42-44`
**Issue:** The `loadStatistics` function catches all errors and does nothing -- `catch (error) { /* silently handle */ }`. This violates the project's coding standard (CLAUDE.md: "Never silently swallow errors"). When the API call fails, the user sees no feedback: no error message, no loading indicator reset, no fallback data. The `statistics` ref retains its initial empty object value, and the component renders with blank/zero statistics with no indication of failure.
**Cross-file impact:** The `request.ts` interceptor already handles 401 errors (token refresh), but network errors, 500s, and business logic errors from the statistics endpoint are silently dropped. Users will see a broken dashboard with no explanation.
**Fix:**
```typescript
} catch (error) {
  console.error('Failed to load statistics:', error)  // at minimum, log it
  ElMessage.error(t('monitor.loadStatisticsFailed'))  // add i18n key
  statistics.value = { /* safe fallback defaults */ }
}
```

### WR-03: Inconsistent i18n access pattern -- API files use i18n.global.t() while components use useI18n() t()

**File:** `oaiss-chain-frontend/src/api/auth.ts:2`, `oaiss-chain-frontend/src/api/emission.ts:3`
**Issue:** `auth.ts` and `emission.ts` import the i18n singleton and call `i18n.global.t()`, while Vue components use the `useI18n()` composable with `t()`. These are two distinct access paths to vue-i18n. The `i18n.global.t()` approach works outside Vue component context but bypasses the reactive locale tracking that `useI18n()` provides. This dual pattern is undocumented in the project -- there is no convention documented in CLAUDE.md or the i18n module about when to use which approach.
**Cross-file impact:** Future developers working on API files may copy either pattern without understanding the distinction. If a developer uses `useI18n()` inside an API file (which runs outside Vue component setup context), it will throw a runtime error. If they use `i18n.global.t()` inside a component, they lose reactivity. The project needs a documented convention.
**Fix:** Add a comment convention to `@/i18n/index.ts`:
```typescript
// Usage:
// - In Vue components: import { useI18n } from 'vue-i18n'; const { t } = useI18n();
// - Outside components (API files, utils): import i18n from '@/i18n'; i18n.global.t('key');
```

### WR-04: i18n.global.t() initialization timing -- API validation called before app.use(i18n)

**File:** `oaiss-chain-frontend/src/api/auth.ts:2,12,13`, `oaiss-chain-frontend/src/api/emission.ts:3,18`
**Issue:** `auth.ts` and `emission.ts` import `i18n` from `@/i18n` and call `i18n.global.t()` inside validation functions. These functions are only called when the user submits a form, so in practice `app.use(i18n)` has already run. However, the import creates a module-level dependency on the i18n singleton being initialized. In `i18n/index.ts`, the i18n instance is created by `createI18n()` at module evaluation time (synchronous), so the import always resolves to a valid object. The `i18n.global.t()` function works in legacy:false mode even before `app.use(i18n)` because it reads from the internally stored message pool. This is safe in the current architecture but fragile -- if the i18n initialization is ever made async (e.g., lazy-loading locale messages), these calls would return raw key names without warning.
**Cross-file impact:** Both `auth.ts` and `emission.ts` depend on `@/i18n` synchronous initialization. If the i18n module is refactored to load messages asynchronously, all API file `i18n.global.t()` calls will silently return key paths instead of translated strings.
**Fix:** This is acceptable for now but should be guarded. Add a startup check in `i18n/index.ts`:
```typescript
// Ensure messages are loaded before any i18n.global.t() calls
if (!i18n.global.availableLocales.includes('zh-CN')) {
  console.warn('i18n: locale messages not loaded at initialization time')
}
```

### WR-05: Remaining hardcoded Chinese strings in other API files -- incomplete scope

**File:** `oaiss-chain-frontend/src/api/` (multiple files)
**Issue:** Phase 17-02 extracted hardcoded Chinese from `auth.ts` and `emission.ts`, but approximately 14 additional hardcoded Chinese strings remain in other API files: `credit.ts` (6 strings), `user.ts` (5 strings), `enterprise.ts` (3 strings), `request.ts` (8 strings including error messages like "Token刷新失败", "请求超时", "网络错误"), `blockchain.ts`, `captcha.ts`, `carbon.ts`, `carbonCoin.ts`, `carbonNeutral.ts`, and `file.ts`. This creates an inconsistent state where some API validation errors are i18n-aware and others are not. If a user with English locale sees a mix of translated and untranslated Chinese error messages, the UX is worse than if all were consistently in Chinese.
**Cross-file impact:** The locale files added `auth.*` and `emissionData.*` sections, but no corresponding sections exist for credit, user, enterprise, or the request interceptor. The i18n infrastructure is partially adopted, creating an inconsistent experience.
**Fix:** Either complete the i18n extraction for all API files in a follow-up phase, or document the remaining hardcoded strings as accepted technical debt with a tracking issue.

## Info

### IN-01: Monitor.vue removed `|| '审核通过率'` fallback -- relies on vue-i18n fallback mechanism

**File:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue:28`
**Issue:** The original code had `t('monitor.approvalRate') || '审核通过率'` as a fallback. The phase removed the `|| '审核通过率'` part, relying entirely on vue-i18n's built-in fallback. In vue-i18n with `legacy: false`, if a key is missing from the current locale, it falls back to the key path string (e.g., `"monitor.approvalRate"`). The previous fallback ensured a user-friendly Chinese string even if the key was missing. The new behavior returns an ugly key path on missing keys. Since both locale files do contain `monitor.approvalRate`, this is functionally correct, but the safety net is removed.
**Cross-file impact:** No direct impact since the key exists in both locales. This is a minor robustness note.
**Fix:** No action needed. If robustness is desired, configure vue-i18n's `fallbackLocale: 'zh-CN'` in `i18n/index.ts` to ensure Chinese fallback instead of key paths.

### IN-02: No test files exist for VerifyList.vue or Monitor.vue

**File:** `oaiss-chain-frontend/src/views/__tests__/` (missing)
**Issue:** There are no unit test files for `VerifyList.vue` or `Monitor.vue`. The i18n extraction changed runtime behavior (translation function calls, status map reactivity), but there are no tests to verify the changes work correctly. Other view components in the same directory (TradingP2P, CreditScore, Blockchain, UserProfile) do have test files.
**Cross-file impact:** The missing status code 4 (WR-01) and the non-reactive statusMap (CR-01) could have been caught by tests if they existed. The i18n key mapping could also be validated by tests.
**Fix:** Add basic unit tests for VerifyList.vue and Monitor.vue that verify status label rendering and i18n key resolution.

---

_Reviewed: 2026-05-22T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
