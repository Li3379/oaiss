---
phase: 17-acceptance-gap-i18n
plan: 17-02
subsystem: frontend-i18n
tags: [i18n, cleanup, vue-i18n]
dependency_graph:
  requires: [17-01]
  provides: [I18N-01-complete]
  affects: [VerifyList.vue, Monitor.vue, auth.ts, emission.ts, zh-CN.ts, en-US.ts]
tech_stack:
  added: [vue-i18n global.t() pattern for API files]
  patterns: [i18n.global.t() in non-component files, t() in Vue components]
key_files:
  created: []
  modified:
    - oaiss-chain-frontend/src/views/admin/VerifyList.vue
    - oaiss-chain-frontend/src/views/third-party/Monitor.vue
    - oaiss-chain-frontend/src/api/auth.ts
    - oaiss-chain-frontend/src/api/emission.ts
    - oaiss-chain-frontend/src/i18n/locales/zh-CN.ts
    - oaiss-chain-frontend/src/i18n/locales/en-US.ts
decisions:
  - Used t() in Vue components (already have useI18n()), i18n.global.t() in API files (no component context)
  - Added auth section as new top-level section in locale files, placed after common
  - Added verifyList.statusOnChain and emissionData.enterpriseIdRequired to existing sections
metrics:
  duration: 5m
  completed: 2026-05-22
  tasks_total: 4
  tasks_completed: 4
  files_modified: 6
  commits: 4
---

# Phase 17 Plan 02: i18n Cleanup Summary

Extracted 5 hardcoded Chinese strings from 4 frontend files into vue-i18n translation keys, with corresponding zh-CN and en-US translations.

## Tasks Completed

| Task | File | Change | Commit |
|------|------|--------|--------|
| 1 | VerifyList.vue | Replace `'已上链'` with `t('verifyList.statusOnChain')`; add key to both locales | 3718773 |
| 2 | Monitor.vue | Remove `\|\| '审核通过率'` fallback from statApprovalRate display | 4e25ec6 |
| 3 | auth.ts | Import i18n, replace `'用户名不能为空'` and `'密码不能为空'` with `i18n.global.t()`; add auth section to both locales | 5d62c27 |
| 4 | emission.ts | Import i18n, replace `'企业ID不能为空'` with `i18n.global.t()`; add enterpriseIdRequired to emissionData section in both locales | 7f7cfc2 |

## Key Changes

### Pattern Used

- **Vue components**: `t('key')` via existing `useI18n()` setup
- **API files**: `import i18n from '@/i18n'` then `i18n.global.t('key')` per D-04

### i18n Keys Added

| Key | zh-CN | en-US |
|-----|-------|-------|
| verifyList.statusOnChain | 已上链 | On Chain |
| auth.usernameRequired | 用户名不能为空 | Username is required |
| auth.passwordRequired | 密码不能为空 | Password is required |
| emissionData.enterpriseIdRequired | 企业ID不能为空 | Enterprise ID is required |

### Fallback Removed

- `Monitor.vue`: `t('monitor.statApprovalRate') || '审核通过率'` simplified to `t('monitor.statApprovalRate')` since both locales already have the key

## Deviations from Plan

None - plan executed exactly as written.

## Verification

- VerifyList.vue: no literal `'已上链'` in code (remains in comment only)
- Monitor.vue: no `'审核通过率'` fallback
- auth.ts: no hardcoded Chinese strings, imports i18n
- emission.ts: no hardcoded Chinese strings, imports i18n
- Both locale files contain all 4 new translation keys

## Self-Check: PASSED

All 7 files exist on disk. All 4 commit hashes verified in git log.
