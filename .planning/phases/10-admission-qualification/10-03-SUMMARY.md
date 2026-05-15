---
phase: 10-admission-qualification
plan: 03
subsystem: frontend
tags: [certificate, vue, element-plus, i18n, admin, enterprise, reviewer]
dependency_graph:
  requires:
    - phase: 10-01
      provides: [EnterpriseAdmissionService, AdminController admission endpoints]
    - phase: 10-02
      provides: [ReviewerQualificationService, AdminController qualification endpoints]
  provides: [CertificateManage.vue, certificate API functions, certificate i18n, cert status in UserProfile/AuditList]
  affects: []
tech_stack:
  added: []
  patterns: [el-tabs dual-tab admin page, /my endpoint cert status display, ElMessageBox.confirm for destructive actions]
key_files:
  created:
    - oaiss-chain-frontend/src/views/admin/CertificateManage.vue
  modified:
    - oaiss-chain-frontend/src/api/admin.ts
    - oaiss-chain-frontend/src/i18n/locales/zh-CN.ts
    - oaiss-chain-frontend/src/i18n/locales/en-US.ts
    - oaiss-chain-frontend/src/router/index.ts
    - oaiss-chain-frontend/src/config/menu.ts
    - oaiss-chain-frontend/src/views/enterprise/UserProfile.vue
    - oaiss-chain-frontend/src/views/auditor/AuditList.vue
decisions:
  - "Followed SystemUsers.vue pattern for CertificateManage.vue (el-table, el-pagination, el-tag status)"
  - "Used Record<string, unknown> for API response typing since API functions return Promise<unknown>"
  - "Certificate status uses computed refs for reactive status type/text derived from fetched data"
patterns-established:
  - "Admin certificate management follows same dual-tab + pagination pattern as SystemUsers.vue"
  - "Role-specific /my endpoint cert status display pattern for UserProfile and AuditList"
requirements-completed:
  - REQ-07
  - REQ-08
metrics:
  duration: 8min
  completed: "2026-05-15T12:37:44Z"
  tasks_completed: 3
  tasks_total: 3
  files_created: 1
  files_modified: 7
  tests_added: 0
---

# Phase 10 Plan 03: Frontend Certificate Management Summary

Admin certificate management page with dual-tab layout (admission + qualification), 8 API client functions, i18n translations, router/menu config, and certificate status display in enterprise UserProfile and auditor AuditList views.

## Performance

- **Duration:** 8 min
- **Started:** 2026-05-15T12:29:17Z
- **Completed:** 2026-05-15T12:37:44Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments
- CertificateManage.vue with dual-tab layout for admission certificates and reviewer qualifications
- 8 API functions in admin.ts for enterprise admission and reviewer qualification CRUD
- i18n certificateManage section in both zh-CN and en-US locales
- Route /admin/certificates with ROLE.ADMIN guard + menu item under ADMIN role
- Enterprise UserProfile displays admission certificate status (el-descriptions card)
- Auditor AuditList displays qualification status banner at page top

## Task Commits

Each task was committed atomically:

1. **Task 1: API functions + i18n + router/menu** - `3faeafd` (feat)
2. **Task 2: CertificateManage.vue admin page** - `4696a28` (feat)
3. **Task 3: Enterprise/auditor cert status display** - `5cf70ac` (feat)

## Files Created/Modified
- `oaiss-chain-frontend/src/views/admin/CertificateManage.vue` - Admin certificate management page with dual-tab layout, issue/revoke dialogs, pagination
- `oaiss-chain-frontend/src/api/admin.ts` - 8 API functions: getEnterpriseAdmissionList, issueEnterpriseAdmission, revokeEnterpriseAdmission, getMyEnterpriseAdmission, getReviewerQualificationList, issueReviewerQualification, revokeReviewerQualification, getMyReviewerQualification
- `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts` - Added certificateManage section + menu.certificateManage/certificateList keys
- `oaiss-chain-frontend/src/i18n/locales/en-US.ts` - Added certificateManage section + menu.certificateManage/certificateList keys
- `oaiss-chain-frontend/src/router/index.ts` - Added /admin/certificates route with ROLE.ADMIN guard
- `oaiss-chain-frontend/src/config/menu.ts` - Added certificateManage menu group under ADMIN role
- `oaiss-chain-frontend/src/views/enterprise/UserProfile.vue` - Added admission certificate status card with el-descriptions
- `oaiss-chain-frontend/src/views/auditor/AuditList.vue` - Added qualification status banner with el-tag

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Followed SystemUsers.vue pattern | Consistent codebase style, same el-table + el-pagination + el-tag pattern |
| Used Record<string, unknown> for API responses | API functions return Promise<unknown>, need explicit cast for type safety |
| Certificate status via computed refs | Reactive status type/text derived from fetched data, clean template binding |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed PageRequest field names**
- **Found during:** Task 2
- **Issue:** CertificateManage.vue used `{ page, size }` but PageRequest type uses `{ pageNum, pageSize }`
- **Fix:** Changed to `{ pageNum, pageSize }` which the request interceptor converts to `{ page, size }`
- **Files modified:** CertificateManage.vue
- **Verification:** vue-tsc --noEmit shows no CertificateManage errors
- **Committed in:** `4696a28`

**2. [Rule 1 - Bug] Fixed TypeScript type errors in CertificateManage.vue**
- **Found during:** Task 2
- **Issue:** `ref([])` creates `Ref<never[]>`, response typed as `unknown` caused assignment errors
- **Fix:** Added `ref<Record<string, unknown>[]>([])` and explicit `as number` casts for revoke calls
- **Files modified:** CertificateManage.vue
- **Verification:** vue-tsc --noEmit shows no CertificateManage errors
- **Committed in:** `4696a28`

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for TypeScript compilation. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Known Stubs
None. All API functions are wired to real backend endpoints.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: T-10-14 mitigated | CertificateManage.vue | Route restricted to ROLE.ADMIN via router meta.roles |
| threat_flag: T-10-15 mitigated | UserProfile.vue | getMyEnterpriseAdmission() calls backend /my requiring ENTERPRISE role JWT |
| threat_flag: T-10-16 mitigated | AuditList.vue | getMyReviewerQualification() calls backend /my requiring REVIEWER role JWT |

## Self-Check

- [x] CertificateManage.vue exists with el-tabs, el-table, el-pagination, el-dialog
- [x] api/admin.ts contains all 8 certificate functions
- [x] router/index.ts contains path: 'admin/certificates' with AdminCertificates name
- [x] config/menu.ts contains certificateManage menu item
- [x] zh-CN.ts contains certificateManage section + menu keys
- [x] en-US.ts contains certificateManage section + menu keys
- [x] UserProfile.vue contains getMyEnterpriseAdmission import and admissionStatus display
- [x] AuditList.vue contains getMyReviewerQualification import and qualificationStatus display
- [x] No new TypeScript errors from my changes (pre-existing errors only)
- [x] Commit `3faeafd` exists (Task 1)
- [x] Commit `4696a28` exists (Task 2)
- [x] Commit `5cf70ac` exists (Task 3)

## Self-Check: PASSED

## Next Phase Readiness
- Phase 10 complete: all 3 plans executed (10-01, 10-02, 10-03)
- Enterprise admission + reviewer qualification full-stack feature complete
- Frontend builds without new errors

---
*Phase: 10-admission-qualification*
*Completed: 2026-05-15*
