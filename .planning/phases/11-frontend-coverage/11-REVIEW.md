---
phase: 11-frontend-coverage
reviewed: 2026-05-16T20:24:00+08:00
depth: deep
files_reviewed: 32
files_reviewed_list:
  - oaiss-chain-frontend/src/api/enterprise.ts
  - oaiss-chain-frontend/src/api/reviewer.ts
  - oaiss-chain-frontend/src/api/auth.ts
  - oaiss-chain-frontend/src/api/user.ts
  - oaiss-chain-frontend/src/api/carbon.ts
  - oaiss-chain-frontend/src/api/trade.ts
  - oaiss-chain-frontend/src/api/auction.ts
  - oaiss-chain-frontend/src/api/carbonCoin.ts
  - oaiss-chain-frontend/src/api/credit.ts
  - oaiss-chain-frontend/src/api/carbonNeutral.ts
  - oaiss-chain-frontend/src/api/blockchain.ts
  - oaiss-chain-frontend/src/api/emission.ts
  - oaiss-chain-frontend/src/api/admin.ts
  - oaiss-chain-frontend/src/api/thirdParty.ts
  - oaiss-chain-frontend/src/api/captcha.ts
  - oaiss-chain-frontend/src/api/signature.ts
  - oaiss-chain-frontend/src/views/enterprise/EnterpriseInfo.vue
  - oaiss-chain-frontend/src/views/enterprise/CarbonUpload.vue
  - oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue
  - oaiss-chain-frontend/src/views/enterprise/CompanyDashboard.vue
  - oaiss-chain-frontend/src/views/auditor/AuditList.vue
  - oaiss-chain-frontend/src/views/auditor/ReviewHistory.vue
  - oaiss-chain-frontend/src/views/auditor/ProjectReview.vue
  - oaiss-chain-frontend/src/router/index.ts
  - oaiss-chain-frontend/src/config/menu.ts
  - oaiss-chain-frontend/src/i18n/locales/zh-CN.ts
  - oaiss-chain-frontend/src/i18n/locales/en-US.ts
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EnterpriseController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonCoinController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/MarketPredictionController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EnterpriseInferenceController.java
findings:
  critical: 5
  warning: 7
  info: 4
  total: 16
status: issues_found
---

# Phase 11: Code Review Report

**Reviewed:** 2026-05-16T20:24:00+08:00
**Depth:** deep
**Files Reviewed:** 32
**Status:** issues_found

## Summary

Deep review of 32 files across 4 sub-plans: frontend API modules (16), enterprise/auditor views (7), config/i18n (4), and backend controller Swagger annotations (5). Cross-file API contract analysis traced every frontend API call to its backend endpoint handler.

**5 critical API contract mismatches** were found where the frontend sends data in a format the backend cannot accept (wrong parameter passing method, wrong field names, or wrong data types). These will cause silent failures at runtime -- the API calls will return 400/500 errors, and in the CarbonNeutral case, action buttons will never render because the status comparison is always false.

## Critical Issues

### CR-01: Enterprise updateContact sends JSON body but backend expects @RequestParam

**File:** `oaiss-chain-frontend/src/api/enterprise.ts:14`
**Issue:** The frontend calls `request.put('/enterprise/contact', data)` which sends `{ contactPerson, contactPhone }` as a JSON request body. The backend `EnterpriseController.updateContact()` at line 97-100 uses `@RequestParam(required = false) String contactPerson` and `@RequestParam(required = false) String contactPhone`, meaning Spring Boot expects query parameters, not a request body. The backend will receive null values for both fields, silently succeeding without actually updating contact info.
**Fix:**
```typescript
// enterprise.ts line 14 -- send as query params instead of body
export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('联系人不能为空'))
  if (!data?.contactPhone) return Promise.reject(new Error('联系电话不能为空'))
  return request.put('/enterprise/contact', null, { params: data })
}
```

### CR-02: CarbonCoin recharge sends wrong field names and wrong parameter location

**File:** `oaiss-chain-frontend/src/api/carbonCoin.ts:16-19`
**Issue:** The frontend sends `request.post('/carbon-coin/recharge', data)` with `{ enterpriseId, amount }` in the request body. The backend `CarbonCoinController.recharge()` at lines 68-71 expects: (1) `@RequestParam Long userId` as a query parameter, and (2) `@RequestBody CarbonCoinRechargeRequest` containing `{ amount, paymentMethod, remark }`. The field name `enterpriseId` does not exist in the backend DTO, and the `userId` is expected as a query parameter, not in the body. The recharge call will fail with a 400 error.
**Fix:**
```typescript
// carbonCoin.ts line 16-19
export function recharge(data: { userId: number; amount: number }): Promise<unknown> {
  if (!data?.userId) return Promise.reject(new Error('用户ID不能为空'))
  if (!data?.amount || data.amount <= 0) return Promise.reject(new Error('充值金额必须大于0'))
  return request.post(`/carbon-coin/recharge?userId=${data.userId}`, { amount: data.amount })
}
```

### CR-03: ThirdParty updateContact sends JSON body but backend expects @RequestParam

**File:** `oaiss-chain-frontend/src/api/thirdParty.ts:17-18`
**Issue:** Same pattern as CR-01. Frontend sends `request.put('/third-party/contact', data)` with a JSON body. Backend `ThirdPartyController.updateContact()` at lines 91-94 expects `@RequestParam` (query parameters). The update will silently succeed without applying changes.
**Fix:**
```typescript
// thirdParty.ts line 17-18
export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('联系人不能为空'))
  return request.put('/third-party/contact', null, { params: data })
}
```

### CR-04: CarbonNeutral.vue status comparison uses strings but backend returns Integer

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue:225-228`
**Issue:** The template conditionally renders action buttons using `v-if="row.status === 'DRAFT'"`, `v-if="row.status === 'APPROVED'"`, `v-if="row.status === 'IN_PROGRESS'"`. The backend `CarbonNeutralProject.status` field is `Integer` (0, 1, 2, 3...) per the entity definition. After the request interceptor unwraps the API response, `row.status` will be a number (e.g., `0`), never matching the string `'DRAFT'`. All conditional action buttons (Submit, Start, Apply Certification, Terminate) will never render. The `getProjectStatusTag` and `getProjectStatusText` helper functions (lines 80-103) correctly use numeric keys, but the template `v-if` conditions use strings.
**Fix:**
```html
<!-- CarbonNeutral.vue lines 225-228: use numeric comparisons -->
<el-button v-if="row.status === 0" link type="primary" @click="onSubmitProject(row)">{{ t('carbonNeutral.submit') }}</el-button>
<el-button v-if="row.status === 2" link type="primary" @click="onStartProject(row)">{{ t('carbonNeutral.start') }}</el-button>
<el-button v-if="row.status === 3" link type="success" @click="onApplyCertification(row)">{{ t('carbonNeutral.applyCert') }}</el-button>
<el-button v-if="[0, 2, 3].includes(row.status)" link type="danger" @click="onTerminateProject(row)">{{ t('carbonNeutral.terminate') }}</el-button>
```

### CR-05: carbonNeutral.ts useCredits sends `creditAmount` but backend expects `amount`

**File:** `oaiss-chain-frontend/src/api/carbonNeutral.ts:58`
**Issue:** The frontend function sends `{ creditAmount: number }` in the request body. The backend `CarbonNeutralProjectController.useCredits()` at line 234 reads `body.get("amount")`. The key name mismatch means the backend receives null for `amount`, which will cause a NullPointerException or incorrect behavior when processing the credit usage.
**Fix:**
```typescript
// carbonNeutral.ts line 58
export function useCredits(projectId: number, data: { amount: number }): Promise<void> {
  if (!projectId) return Promise.reject(new Error('项目ID不能为空'))
  return request.post(`/carbon-neutral/${projectId}/use-credits`, data)
}
```

## Warnings

### WR-01: Missing i18n key `carbonUpload.colReportType` used in CarbonUpload detail dialog

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonUpload.vue:236`
**Issue:** The detail dialog renders `<el-descriptions-item :label="t('carbonUpload.colReportType')">` but neither `zh-CN.ts` nor `en-US.ts` defines the key `carbonUpload.colReportType`. The label will render as the raw key string "carbonUpload.colReportType" instead of a human-readable label. The key exists as `carbonUpload.labelReportType` but not as `colReportType`.
**Fix:** Add to both `zh-CN.ts` and `en-US.ts` under the `carbonUpload` section:
```typescript
// zh-CN.ts
colReportType: '报告类型',
// en-US.ts
colReportType: 'Report Type',
```

### WR-02: `admin.ts` places `getMyEnterpriseAdmission` and `getMyReviewerQualification` in wrong module

**File:** `oaiss-chain-frontend/src/api/admin.ts:30-49`
**Issue:** Two functions (`getMyEnterpriseAdmission` at line 30, `getMyReviewerQualification` at line 48) are personal "my-status" queries placed in the `admin.ts` API module. `getMyEnterpriseAdmission` calls `/enterprise/admission/my` (an ENTERPRISE-role endpoint) and `getMyReviewerQualification` calls `/reviewer/qualification/my` (a REVIEWER-role endpoint). Non-admin users importing these get functions they cannot authorize. `CompanyDashboard.vue` (line 9) imports `getMyEnterpriseAdmission` from `admin.ts`, and `AuditList.vue` (line 6) imports `getMyReviewerQualification` from `admin.ts`. These should live in their respective role modules (`enterprise.ts` and `reviewer.ts`).
**Fix:** Move `getMyEnterpriseAdmission` to `enterprise.ts` and `getMyReviewerQualification` to `reviewer.ts`. Update all imports accordingly.

### WR-03: CompanyDashboard.vue fetches credit score as user profile data

**File:** `oaiss-chain-frontend/src/views/enterprise/CompanyDashboard.vue:246-253`
**Issue:** `fetchUserProfile` calls `getMyScore()` from the credit module to populate `userProfile`, then accesses `userProfile.value?.carbonCoins`, `userProfile.value?.carbonQuota`, `userProfile.value?.score`. The `CreditScoreResponse` type (defined in `types/credit.ts`) has fields `score`, `level`, `tradeRestricted`, `accountFrozen` but does not have `carbonCoins` or `carbonQuota` fields. The dashboard overview cards will always display 0 for "Carbon Coins" and "Carbon Quota" because these fields do not exist on the response object.
**Fix:** Either fetch from the actual carbon-coin account and quota endpoints in parallel, or extend the credit score response to include these fields on the backend.

### WR-04: ProjectReview.vue deduct dialog always allows credit deduction regardless of project status

**File:** `oaiss-chain-frontend/src/views/auditor/ProjectReview.vue:166`
**Issue:** The "Deduct Credit" button (`<el-button link type="warning" @click="openDeductDialog(row)">`) has no `v-if` status guard. Any reviewer can open the deduct dialog on any project regardless of its status, including already-terminated or draft projects. The backend may reject the request, but the UI should not present the action on inappropriate statuses.
**Fix:** Add a conditional guard, for example:
```html
<el-button v-if="row.status === 'PENDING_REVIEW' || row.status === 'PENDING_VERIFICATION'"
  link type="warning" @click="openDeductDialog(row)">
  {{ t('projectReview.deductCredit') }}
</el-button>
```

### WR-05: Multiple API functions use `Promise<unknown>` return type defeating TypeScript safety

**Files:** `oaiss-chain-frontend/src/api/enterprise.ts`, `reviewer.ts`, `blockchain.ts`, `admin.ts`, `thirdParty.ts`, `trade.ts`, `credit.ts`, `carbonNeutral.ts`
**Issue:** Functions like `getEnterpriseInfo()`, `getQuotaInfo()`, `getReviewerInfo()`, `getPendingReports()`, `getReviewHistory()`, `getStatistics()` (both reviewer and admin), `getUserList()`, `getDashboard()`, etc. all return `Promise<unknown>`. This forces consuming components to cast with `as Record<string, unknown>`, losing all type safety. Every consumer must manually know and assert the shape of the response.
**Fix:** Define proper response interfaces (e.g., `EnterpriseInfoResponse`, `QuotaInfoResponse`, `ReviewerStatisticsResponse`) and use them as return types. This is a broad pattern issue but should be addressed incrementally.

### WR-06: CarbonUpload.vue "Create" button uses wrong i18n key

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonUpload.vue:158`
**Issue:** The "Add" button text uses `t('carbonNeutral.createProject')` which renders "Create Project" -- the carbon neutral project label -- instead of a carbon-report-specific label. This is a copy-paste error from the CarbonNeutral view.
**Fix:**
```html
<!-- Line 158: use carbonUpload-specific key or common.create -->
<el-button type="success" plain @click="openAddDialog">{{ t('common.create') }}</el-button>
```

### WR-07: EnterpriseInfo.vue does not reset contact form on re-fetch

**File:** `oaiss-chain-frontend/src/views/enterprise/EnterpriseInfo.vue:52`
**Issue:** After a successful `updateContact`, the component calls `fetchInfo()` to refresh data (line 52), but the response interceptor unwraps the `ApiResponse` envelope, so `getEnterpriseInfo()` returns the raw enterprise data. The component then casts this as `Record<string, unknown>` and accesses nested properties with repeated type assertions (lines 26-31). If the backend returns null for any reason, the optional chaining will skip the assignment, leaving stale data in the contact form.

## Info

### IN-01: `getReportList` in carbon.ts calls `/carbon/reports` which requires ENTERPRISE role on backend

**File:** `oaiss-chain-frontend/src/api/carbon.ts:14`
**Issue:** The `getReportList` function calls `GET /carbon/reports`. The backend `CarbonController.listReports()` at line 127 has `@PreAuthorize("hasRole('ENTERPRISE')")`. However, this function is imported by `AuditList.vue` (reviewer role), which calls it when the "All Reports" tab is active. The reviewer will get a 403 Forbidden error when switching to the "All Reports" tab. This may be intentional (the endpoint should be opened to reviewers), or the reviewer should use a different endpoint.
**Fix:** Either (a) add `REVIEWER` to the backend `@PreAuthorize` for the `/carbon/reports` endpoint, or (b) use a reviewer-specific endpoint in `AuditList.vue` for the "all reports" tab.

### IN-02: CarbonNeutral.vue `projectTypeOptions` uses raw i18n keys as labels without `t()` at definition time

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue:28-33`
**Issue:** The `projectTypeOptions` array stores i18n key paths like `'carbonNeutral.typeCarbonSink'` as `label` values. The template must call `t(item.label)` when rendering (line 257), and `getProjectTypeLabel` does this correctly (line 77). However, this pattern is fragile -- if someone uses `projectTypeOptions` directly without `t()`, raw keys appear. This is an established pattern in this codebase, so it works, but it relies on consistent usage.

### IN-03: AuditList.vue getStatusType maps status 3 to 'danger' while CarbonUpload maps it to 'success'

**File:** `oaiss-chain-frontend/src/views/auditor/AuditList.vue:92-101`
**Issue:** `AuditList.getStatusType` maps `{ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info' }` while `CarbonUpload.statusTagType` maps `{ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }`. Status 3 (approved) is `'danger'` in AuditList but `'success'` in CarbonUpload. Status 2 is `'success'` in AuditList but `'primary'` in CarbonUpload. These inconsistencies suggest either the status code semantics differ between contexts, or there is a mapping error. The CarbonUpload mapping appears correct (3=APPROVED=success), while AuditList appears incorrect.
**Fix:** Align `AuditList.vue` status mapping with `CarbonUpload.vue`:
```typescript
const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}
```

### IN-04: Backend controllers use sequential @Tag numbers that may conflict

**Files:** Backend controllers (AdminController=16, EnterpriseController=17, CarbonCoinController=18, MarketPredictionController=19, EnterpriseInferenceController=20)
**Issue:** The `@Tag` annotations use sequential numbering (16-20). If any other controller was previously assigned these numbers, Swagger UI will merge them under the same group heading. The renumbering in Plan 11-04 appears consistent, but there is no validation that no other controller uses these numbers.

---

_Reviewed: 2026-05-16T20:24:00+08:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
