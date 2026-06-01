---
phase: 11-frontend-coverage
review: 11-REVIEW.md
fix_commit: e64726a
fix_date: 2026-05-16
findings_total: 16
findings_fixed: 10
findings_skipped: 6
findings_skipped_list:
  - WR-03: CompanyDashboard credit score for carbon coins (requires backend changes)
  - WR-05: Promise<unknown> return types (broad refactor, not Phase 11 scope)
  - WR-07: EnterpriseInfo contact form stale data (cosmetic, low impact)
  - IN-01: getReportList 403 for REVIEWER (FALSE POSITIVE — backend already has hasAnyRole including REVIEWER)
  - IN-02: projectTypeOptions raw i18n keys (established pattern, works correctly)
  - IN-04: Backend @Tag sequential numbering validation (already verified in Plan 11-04)
---

# Phase 11: Code Review Fix Report

**Review:** 11-REVIEW.md (deep)
**Fix Commit:** e64726a
**Date:** 2026-05-16

## Summary

Fixed 10 of 16 findings (5 Critical, 5 Warning/Info). Remaining 6 are either false positives, require backend changes beyond scope, or are established patterns.

## Fixes Applied

### Critical (5/5 fixed)

| ID | Finding | Fix | Files |
|----|---------|-----|-------|
| CR-01 | enterprise.ts updateContact sends JSON body, backend expects @RequestParam | Changed to `request.put('/enterprise/contact', null, { params: data })` | enterprise.ts |
| CR-03 | thirdParty.ts updateContact same issue as CR-01 | Changed to `request.put('/third-party/contact', null, { params: data })` | thirdParty.ts |
| CR-02 | carbonCoin.ts recharge sends wrong fields in wrong location | Changed to `request.post(/carbon-coin/recharge?userId=${userId}`, { amount })` | carbonCoin.ts |
| CR-04 | CarbonNeutral.vue compares status as strings but backend returns Integer | Changed to numeric: `row.status === 0` (DRAFT), `=== 2` (APPROVED), `=== 3` (IN_PROGRESS) | CarbonNeutral.vue |
| CR-05 | carbonNeutral.ts useCredits sends `creditAmount` but backend reads `amount` | Changed to `{ amount: number }` | carbonNeutral.ts |

### Warning (4/7 fixed)

| ID | Finding | Fix | Files |
|----|---------|-----|-------|
| WR-01 | Missing i18n key `carbonUpload.colReportType` | Added to both zh-CN.ts and en-US.ts | zh-CN.ts, en-US.ts |
| WR-02 | getMyEnterpriseAdmission/getMyReviewerQualification in wrong module | Moved to enterprise.ts and reviewer.ts; updated imports | admin.ts, enterprise.ts, reviewer.ts, CompanyDashboard.vue, AuditList.vue |
| WR-04 | ProjectReview.vue deduct button no status guard | Added `v-if="row.status === 'PENDING_REVIEW' \|\| row.status === 'PENDING_VERIFICATION'"` | ProjectReview.vue |
| WR-06 | CarbonUpload.vue create button uses carbonNeutral.createProject | Changed to `common.create` | CarbonUpload.vue |

### Info (1/4 fixed)

| ID | Finding | Fix | Files |
|----|---------|-----|-------|
| IN-03 | AuditList.vue status 3='danger' inconsistent with CarbonUpload 3='success' | Aligned mapping: `{ 0:'info', 1:'warning', 2:'primary', 3:'success', 4:'danger' }` | AuditList.vue |

## Skipped Findings

| ID | Reason |
|----|--------|
| WR-03 | CompanyDashboard fetches carbonCoins/carbonQuota from credit score API — requires backend endpoint changes |
| WR-05 | `Promise<unknown>` return types — broad pattern across 8+ files, not Phase 11 scope |
| WR-07 | EnterpriseInfo stale contact form data — cosmetic edge case with low user impact |
| IN-01 | **FALSE POSITIVE** — `listReports` already has `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")` |
| IN-02 | `projectTypeOptions` raw i18n keys — established codebase pattern, works correctly with `t()` |
| IN-04 | Backend @Tag numbering — already verified unique 01-20 in Plan 11-04 |

## Verification

- Frontend TypeScript: passes (no new errors, all pre-existing)
- Backend compile: passes (`mvn compile -q` clean)
- Commit: e64726a (13 files, +34/-26 lines)
