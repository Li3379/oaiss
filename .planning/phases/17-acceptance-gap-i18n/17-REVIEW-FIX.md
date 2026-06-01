---
phase: 17
fixed_at: "2026-05-22T12:00:00.000Z"
review_path: .planning/phases/17-acceptance-gap-i18n/17-REVIEW.md
iteration: 1
findings_in_scope: 8
fixed: 5
skipped: 3
status: partial
---

# Phase 17: Code Review Fix Report

**Fixed at:** 2026-05-22T12:00:00.000Z
**Source review:** .planning/phases/17-acceptance-gap-i18n/17-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 8
- Fixed: 5
- Skipped: 3

## Fixed Issues

### CR-01: Monitor.vue statusMap not reactive to locale changes

**Files modified:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue`
**Commit:** 09a2395
**Applied fix:** Wrapped `statusMap` in `computed(() => ({ ... }))` and updated `getStatusTag`/`getStatusText` to reference `statusMap.value[status]` instead of `statusMap[status]`. This ensures the status labels and tags reactively update when the user switches locale.

### WR-01: VerifyList.vue missing status code 4 mapping

**Files modified:** `oaiss-chain-frontend/src/views/admin/VerifyList.vue`, `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts`, `oaiss-chain-frontend/src/i18n/locales/en-US.ts`
**Commit:** b153174
**Applied fix:** Added status code 4 (`certRejected`) to both `getStatusLabel` and `getStatusType` maps in VerifyList.vue, and added `verifyList.statusCertRejected` i18n key to both locale files (zh-CN: '认证驳回', en-US: 'Certification Rejected').

### WR-02: Monitor.vue silently swallows errors in loadStatistics

**Files modified:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue`
**Commit:** 09a2395
**Applied fix:** Replaced `// silently handle` comment in the `loadStatistics` catch block with `console.error('Failed to load statistics:', error)` and `ElMessage.error(t('monitor.loadFailed'))`. The `monitor.loadFailed` key already existed in both locale files.

### WR-03: Dual i18n pattern undocumented

**Files modified:** `oaiss-chain-frontend/src/i18n/index.ts`
**Commit:** 59acef5
**Applied fix:** Added a JSDoc comment block after the `vue-i18n` import in `i18n/index.ts` documenting the dual access pattern: components use `useI18n().t()` for reactivity, API/non-component files use `i18n.global.t()` for synchronous access.

### WR-04: i18n.global.t() sync init fragility

**Files modified:** `oaiss-chain-frontend/src/i18n/index.ts`, `oaiss-chain-frontend/src/api/auth.ts`, `oaiss-chain-frontend/src/api/emission.ts`
**Commit:** 59acef5
**Applied fix:** Added a defensive `t()` accessor function in `i18n/index.ts` that returns the key as fallback if `i18n.global` is not yet initialized. Updated `auth.ts` and `emission.ts` to import and use `{ t }` from `@/i18n` instead of `i18n.global.t()` directly.

## Skipped Issues

### WR-05: ~14 remaining hardcoded Chinese strings in other API files

**File:** Various API files outside Phase 17 scope
**Reason:** Out of scope for this phase. The finding references API files that were not part of the Phase 17 changes. Modifying files outside the phase scope would introduce unrelated changes and risk regressions. Should be addressed in a dedicated i18n-hardening phase.
**Original issue:** Approximately 14 hardcoded Chinese strings remain in API files not touched by Phase 17.

### IN-01: Monitor.vue fallback removal

**File:** `oaiss-chain-frontend/src/views/third-party/Monitor.vue`
**Reason:** Informational only. The REVIEW.md classifies this as informational with no fix required.
**Original issue:** Fallback values in Monitor.vue could be removed now that i18n keys exist.

### IN-02: No unit tests

**File:** Phase 17 modified files
**Reason:** Test creation is a separate effort. Adding unit tests for the i18n changes is out of scope for a fix pass and should be planned as a dedicated testing phase.
**Original issue:** No unit tests exist for the i18n changes made in Phase 17.

---

_Fixed: 2026-05-22T12:00:00.000Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
