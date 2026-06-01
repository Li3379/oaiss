---
status: complete
phase: 17-acceptance-gap-i18n
source: 17-02-SUMMARY.md, 17-REVIEW.md, 17-REVIEW-FIX.md
started: 2026-05-22T15:00:00+08:00
updated: 2026-05-22T15:05:00+08:00
---

## Current Test

[testing complete]

## Tests

### 1. Monitor.vue statusMap is reactive (computed)
expected: statusMap uses computed() so labels update when locale changes.
result: pass
evidence: Monitor.vue:77 — `const statusMap = computed(() => ({` wraps all t() calls; :87/91 use `statusMap.value[status]`

### 2. Monitor.vue getStatusTag and getStatusText use .value
expected: Template functions read from computed .value, not plain object.
result: pass
evidence: Monitor.vue:87 — `return statusMap.value[status]?.tag || 'info'`; :91 — `return statusMap.value[status]?.text || status`

### 3. VerifyList.vue has status code 4 in getStatusLabel
expected: getStatusLabel(4) returns localized certRejected string, not raw number.
result: pass
evidence: VerifyList.vue:123 — map includes `4: t('verifyList.statusCertRejected')`

### 4. VerifyList.vue has status code 4 in getStatusType
expected: getStatusType(4) returns 'danger', not default 'info'.
result: pass
evidence: VerifyList.vue:129 — map includes `4: 'danger'`

### 5. zh-CN locale has statusCertRejected key
expected: verifyList.statusCertRejected = '认证驳回' in zh-CN.ts
result: pass
evidence: zh-CN.ts:372 — `statusCertRejected: '认证驳回'`

### 6. en-US locale has statusCertRejected key
expected: verifyList.statusCertRejected = 'Certification Rejected' in en-US.ts
result: pass
evidence: en-US.ts:372 — `statusCertRejected: 'Certification Rejected'`

### 7. Monitor.vue loadStatistics has error handling
expected: catch block logs error and shows user-facing message, no silent swallow.
result: pass
evidence: Monitor.vue:42-44 — `console.error('Failed to load statistics:', error)` then `ElMessage.error(t('monitor.loadFailed'))`

### 8. monitor.loadFailed key exists in both locales
expected: loadFailed key present in zh-CN and en-US monitor section.
result: pass
evidence: zh-CN.ts:448 — `loadFailed: '获取统计数据失败'`; en-US.ts:448 — `loadFailed: 'Failed to load statistics'`

### 9. i18n/index.ts has dual-pattern JSDoc documentation
expected: JSDoc comment explains when to use useI18n() vs i18n.global.t().
result: pass
evidence: i18n/index.ts:3-8 — JSDoc block: "Vue components: use useI18n().t(key)... API / non-component files: use i18n.global.t(key)"

### 10. i18n/index.ts exports defensive t() accessor
expected: Named export `t` that returns key as fallback if i18n.global is not initialized.
result: pass
evidence: i18n/index.ts:24-27 — `export const t = (key, params?) => { if (!i18n.global) return key; return i18n.global.t(key, params) }`

### 11. auth.ts uses defensive t() from i18n/index.ts
expected: auth.ts imports `{ t }` from '@/i18n' instead of `i18n` singleton, calls t() not i18n.global.t().
result: pass
evidence: auth.ts:3 — `import { t } from '@/i18n'`; auth.ts:14-15 — `t('auth.usernameRequired')`, `t('auth.passwordRequired')`

### 12. emission.ts uses defensive t() from i18n/index.ts
expected: emission.ts imports `{ t }` from '@/i18n' instead of `i18n` singleton, calls t() not i18n.global.t().
result: pass
evidence: emission.ts:3 — `import { t } from '@/i18n'`; emission.ts:22 — `t('emissionData.enterpriseIdRequired')`

## Summary

total: 12
passed: 12
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]
