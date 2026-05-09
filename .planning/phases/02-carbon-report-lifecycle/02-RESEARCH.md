# Phase 2: Carbon Report Lifecycle - Research

**Researched:** 2026-05-09
**Domain:** Carbon report CRUD, review lifecycle, cascading side effects, cross-role access control
**Confidence:** HIGH

## Summary

Phase 2 implements the central carbon report business flow: enterprise users create reports with emission data, submit them for review, reviewers approve or reject, and cascading side effects (credit score update, emission rating calculation, blockchain mock record) fire after approval. The backend code already contains complete CRUD operations and the three cascading services exist as `@Service` beans, but they are not wired into the review flow. The primary work is (a) wiring cascading side effects into `CarbonService.reviewReport()`, (b) fixing the frontend-backend field mapping bug that prevents reviews from working, and (c) creating API test scripts to verify the full lifecycle.

**Critical finding:** CONTEXT.md decision D-04 specifies simplified method signatures (`addBonusPoints(enterpriseId, 5)`, `rateEnterprise(enterpriseId)`) that do not match the actual service method signatures. The planner must use the real signatures documented below.

**Primary recommendation:** Wire three existing services into `reviewReport()`, fix the frontend field mapping, and validate via API test scripts following the established `login-test.sh` pattern.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Wire CreditScoreService, EmissionRatingService, and BlockchainService into `CarbonService.reviewReport()` after approval.
- **D-02:** Synchronous sequential execution -- call credit score, then emission rating, then blockchain in order within reviewReport().
- **D-03:** Same transaction -- all side effects run inside the existing @Transactional. If any fails, the entire approval rolls back.
- **D-04:** Side effect details (see corrected signatures in Code Examples section below):
  - CreditScoreService.addBonusPoints(enterpriseId, 5, description, triggeredBy)
  - EmissionRatingService.rateEnterprise(enterpriseId, year, totalEmission, revenue, ratedBy)
  - BlockchainService.commitReportToChain(reportId, reportData)
- **D-05:** Add ON_CHAIN(5) transition -- after approval + successful blockchain mock, transition from APPROVED(3) to ON_CHAIN(5). Set blockchainTxHash and onChainAt fields.
- **D-06:** UNDER_REVIEW(2) stays unreachable. Reviewer sees SUBMITTED reports. CARB-05 adjusted.
- **D-07:** Valid state transitions: DRAFT(0)->SUBMITTED(1), SUBMITTED(1)->APPROVED(3)|REJECTED(4), APPROVED(3)->ON_CHAIN(5), REJECTED(4)->SUBMITTED(1).
- **D-08:** API scripts + browser verify pattern.
- **D-09:** Use enterprise001 and enterprise002 for testing.
- **D-10:** Real MinIO upload for attachments.
- **D-11:** Authenticator gets read-only access.
- **D-12:** Fix frontend-backend review field mapping (approved -> reviewResult).

### Claude's Discretion
- Exact emissionData JSON structure for test reports
- Test report titles and accounting periods
- Whether to create a single combined script or separate scripts
- How many reports to create for testing

### Deferred Ideas (OUT OF SCOPE)
- UNDER_REVIEW(2) status implementation
- VERIFIER/CERTIFIER role gap investigation
- Full authenticator verify/certify flow
- JWT refresh testing (ENV-09)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CARB-01 | Enterprise creates carbon report (form + file upload to MinIO) | Backend: `POST /carbon/reports` with `CarbonReportRequest` DTO. File upload via `POST /file/upload` returns `UploadResult` with `objectName` and `url`. Attachments stored as JSON array in `attachments` column. |
| CARB-02 | Enterprise views report list (paginated, sorted) | Backend: `GET /carbon/my-reports` scoped to enterprise via JWT. Frontend: `CarbonUpload.vue` calls `getMyReports()`. Request interceptor converts pageNum/pageSize to page/size. |
| CARB-03 | Enterprise views report detail | Backend: `GET /carbon/reports/{reportId}`. No role restriction in `@PreAuthorize` -- any authenticated user can view detail. Returns full `CarbonReportResponse` with resolved enterpriseName and reviewerName. |
| CARB-04 | Enterprise submits report (DRAFT -> SUBMITTED) | Backend: `POST /carbon/reports/{reportId}/submit`. Validates ownership + `isSubmittable()`. Calculates emissions from emissionData JSON before setting status=1. |
| CARB-05 | Reviewer views pending review list (status filter: SUBMITTED) | Backend: `GET /carbon/reports?status=1`. `@PreAuthorize` includes REVIEWER. Frontend: `AuditList.vue` calls `getReportList()`. Per D-06, filter on SUBMITTED(1) not UNDER_REVIEW(2). |
| CARB-06 | Reviewer approves report | Backend: `POST /carbon/review` with `reviewResult=3`. Triggers cascading side effects per D-01/D-02/D-03. After blockchain mock, transitions to ON_CHAIN(5) per D-05. |
| CARB-07 | Reviewer rejects report | Backend: `POST /carbon/review` with `reviewResult=4`. Sets `reviewComment`. No side effects. |
| CARB-08 | Approved report triggers credit score update | Wire `CreditScoreService.addBonusPoints(enterpriseId, 5, ...)` into reviewReport() after setting APPROVED status. |
| CARB-09 | Approved report triggers emission rating calculation | Wire `EmissionRatingService.rateEnterprise(enterpriseId, year, totalEmission, revenue, ratedBy)` into reviewReport(). Requires totalEmission from the report entity. |
| CARB-10 | Approved report triggers blockchain mock record | Wire `BlockchainService.commitReportToChain(reportId, reportData)` into reviewReport(). Store returned txHash in `blockchainTxHash`, set `onChainAt`. Transition status to ON_CHAIN(5). |
| CARB-11 | Illegal state transitions rejected | `ReportStatusEnum.isReviewable()` only returns true for SUBMITTED(1) and UNDER_REVIEW(2). Trying to review a DRAFT report throws "report status not reviewable". Trying to submit a SUBMITTED report fails via `isSubmittable()`. Test: attempt DRAFT->APPROVED (via review), SUBMITTED->SUBMITTED (via submit). |
| CARB-12 | Authenticator views report list (read-only) | Backend: `GET /carbon/reports` has AUTHENTICATOR in `@PreAuthorize`. Authenticator can list and view but has no review/verify endpoints. |
| CARB-13 | Cross-role access control: enterprise cannot access reviewer endpoints | `POST /carbon/review` has `@PreAuthorize("hasRole('REVIEWER')")`. Enterprise token gets 403. `GET /carbon/reports` (admin list) has `@PreAuthorize` excluding ENTERPRISE. Enterprise can only use `/carbon/my-reports`. |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Report CRUD + submission | API / Backend | -- | `CarbonController` + `CarbonService` handle create, list, detail, submit, delete. Business logic and state transitions live here. |
| Report review + side effects | API / Backend | -- | `reviewReport()` in `CarbonService` orchestrates approval/rejection and cascading calls to CreditScoreService, EmissionRatingService, BlockchainService. |
| File upload | API / Backend | -- | `FileController` + `MinioService` handle MinIO upload. Returns URL for report attachments. |
| Report list display | Browser / Client | -- | `CarbonUpload.vue` and `AuditList.vue` render paginated tables with status tags. |
| Review form submission | Browser / Client | API / Backend | `AuditList.vue` collects approved/comment, `carbon.ts` must map to reviewResult/reviewComment before sending to backend. |
| Cross-role access control | API / Backend | Browser / Client | `@PreAuthorize` annotations enforce role restrictions. Frontend routing guards prevent UI navigation to unauthorized pages. |

## Standard Stack

### Core
All dependencies are already in the project. No new installations required.

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.5 | Backend framework | Project-wide standard [VERIFIED: pom.xml] |
| Spring Data JPA | 3.2.5 | Data access | Project-wide standard [VERIFIED: codebase] |
| Spring Security | 6.x | Role-based access control | `@PreAuthorize` annotations [VERIFIED: SecurityConfig.java] |
| Jackson (ObjectMapper) | bundled | JSON parsing of emissionData | Used in `calculateEmissions()` [VERIFIED: CarbonService.java] |
| MinIO Java SDK | bundled | File upload to object storage | `MinioService` with `UploadResult` record [VERIFIED: MinioService.java] |

### Supporting (existing, no install needed)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Vue 3 | 3.5 | Frontend framework | UI rendering for report tables and forms |
| Element Plus | 2.13 | UI components | Tables, dialogs, forms, pagination, tags |
| Axios | bundled | HTTP client | API calls from `carbon.ts` |
| Pinia | 3 | State management | Auth store for token management |

### Alternatives Considered
None required. All dependencies are already in the project.

## Architecture Patterns

### System Architecture Diagram

```
Enterprise Browser                Reviewer Browser               Authenticator Browser
      |                                |                               |
      | createReport()                 |                               |
      |------------------------------> |                               |
      | POST /carbon/reports           |                               |
      |                                |                               |
      | POST /file/upload              |                               |
      | (attachment upload)            |                               |
      |                                |                               |
      | submitReport()                 | listReports(status=1)          | listReports()
      |------------------------------> |------------------------------>|-------------------------->
      | POST /carbon/reports/{id}/submit  GET /carbon/reports?status=1   GET /carbon/reports
      |                                |                               |
      |                                | reviewReport()                |
      |                                |----------------------------->|
      |                                | POST /carbon/review           |
      |                                |   {reviewResult: 3 or 4}     |
      |                                |                               |
      v                                v                               v
  +---------------------------------------------------------------------+
  |                      CarbonController                                |
  |  @PreAuthorize roles: ENTERPRISE (CRUD), REVIEWER (review),          |
  |  ADMIN/REVIEWER/AUTHENTICATOR/THIRD_PARTY (list)                     |
  +---------------------------------------------------------------------+
                |                              |
                v                              v
  +---------------------------+   +---------------------------+
  | CarbonService             |   | CarbonReportRepository    |
  | - createReport()          |   | - search()                |
  | - submitReport()          |   | - findByEnterpriseId...   |
  | - reviewReport() <-- +    |   | - findByStatus...         |
  | - getReport()        |    |   +---------------------------+
  | - listReports()      |    |
  | - listMyReports()    |    |
  +---------------------------+
                    |
                    | AFTER APPROVAL (reviewResult=3)
                    v
        +-----------+-----------+-----------+
        |           |           |           |
        v           v           v           v
  +-----------+ +-----------+ +-----------+ +-----------+
  | Credit    | | Emission  | | Blockchain| | Status    |
  | Score     | | Rating    | | Service   | | Update    |
  | Service   | | Service   | | (mock)    | | APPROVED  |
  |           | |           | |           | | ->ON_CHAIN|
  | +5 points | | A-E level | | txHash    | | set       |
  |           | |           | |           | | txHash,   |
  |           | |           | |           | | onChainAt |
  +-----------+ +-----------+ +-----------+ +-----------+
        |           |           |
        v           v           v
  credit_score   emission_   carbon_report
  table          rating table blockchainTxHash
```

### Recommended Project Structure

No new files or directories needed. All modifications are to existing files:

```
Backend (modifications only):
  oaiss-chain-backend/src/main/java/com/oaiss/chain/
  ├── service/CarbonService.java          -- Wire side effects into reviewReport()
  └── (no new files needed)

Frontend (modifications only):
  oaiss-chain-frontend/src/
  ├── api/carbon.ts                       -- Fix field mapping in reviewReport()
  └── (no new files needed)

Test scripts (new files):
  scripts/
  ├── carbon-report-test.sh               -- API test script for full lifecycle
  └── (single combined script per D-08 pattern)
```

### Pattern 1: Synchronous Cascading Side Effects in Review
**What:** After setting APPROVED status, call three services sequentially within the same @Transactional method.
**When to use:** In `CarbonService.reviewReport()` when `reviewResult == 3` (APPROVED).
**Example:**
```java
// In CarbonService.reviewReport(), after setting status to APPROVED(3):
if (request.getReviewResult() == ReportStatusEnum.APPROVED.getCode()) {
    Long enterpriseId = report.getEnterpriseId();

    // 1. Credit score bonus
    creditScoreService.addBonusPoints(enterpriseId, 5,
        "碳报告审核通过奖励", currentUser.getUserId());

    // 2. Emission rating
    emissionRatingService.rateEnterprise(enterpriseId,
        report.getAccountingPeriod(),
        report.getTotalEmission(),
        null,  // revenue not available from report
        currentUser.getUserId());

    // 3. Blockchain mock
    String txHash = blockchainService.commitReportToChain(
        report.getId(), report.getEmissionData());
    report.setBlockchainTxHash(txHash);
    report.setOnChainAt(LocalDateTime.now());

    // 4. Transition to ON_CHAIN(5)
    report.setStatus(ReportStatusEnum.ON_CHAIN.getCode());
}
```
[Source: VERIFIED from CarbonService.java, CreditScoreService.java, EmissionRatingService.java, BlockchainService.java code]

### Pattern 2: Frontend Field Mapping
**What:** Map frontend boolean `approved` + string `comment` to backend integer `reviewResult` + string `reviewComment`.
**When to use:** In `carbon.ts` `reviewReport()` function.
**Example:**
```typescript
// In carbon.ts reviewReport():
export function reviewReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('报告ID不能为空'))
  return request.post('/carbon/review', {
    reportId: data.reportId,
    reviewResult: data.approved ? 3 : 4,
    reviewComment: data.comment,
  })
}
```
[Source: VERIFIED from AuditList.vue (sends approved/comment), ReviewRequest.java (expects reviewResult/reviewComment)]

### Anti-Patterns to Avoid
- **Calling cascading services on REJECTION:** Side effects only fire for APPROVED(3). Rejected reports just set status to REJECTED(4) with a comment. No credit score change, no emission rating, no blockchain.
- **Sending frontend field names directly to backend:** The frontend `approved` boolean and `comment` string do NOT match backend `reviewResult` integer and `reviewComment` string. The mapping MUST happen in the API layer.
- **Using `data.status` to validate in carbon.ts:** The current code checks `data.status` but the form sends `data.approved`. This check always fails because `status` is undefined. Must be fixed.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Credit score update | Custom score increment logic | `CreditScoreService.addBonusPoints()` | Handles auto-init if no score exists, enforces 0-100 bounds, creates CreditEvent record, checks WARNING/FROZEN thresholds |
| Emission rating | Custom A-E level calculation | `EmissionRatingService.rateEnterprise()` | Has correct thresholds (A:<1000, B:1000-5000, C:5000-20000, D:20000-50000, E:>50000), prevents duplicates per year, calculates intensity |
| Blockchain record | Custom txHash generation | `BlockchainService.commitReportToChain()` | Mock implementation generates realistic txHash format `tx_mock_{timestamp}_{uuid}`, will be swapped for real SDK in production |
| File upload | Direct MinIO SDK calls | `MinioService.uploadFile()` via `FileController` | Handles bucket init, content type detection, presigned URL generation, size validation |
| Emission calculation | Custom emission math | `CarbonService.calculateEmissions()` (already exists) | Parses scope1/2/3 JSON, multiplies activity_data * emission_factor, sets all emission fields |

**Key insight:** All three cascading services already exist and have comprehensive unit tests (verified in test directory). The work is purely integration -- wiring them into the review flow.

## Common Pitfalls

### Pitfall 1: Method Signature Mismatch (CRITICAL)
**What goes wrong:** CONTEXT.md D-04 specifies simplified signatures like `addBonusPoints(enterpriseId, 5)` and `rateEnterprise(enterpriseId)`, but the actual methods require more parameters.
**Why it happens:** Design decisions were made without verifying actual service interfaces.
**How to avoid:** Use the verified signatures documented in the Code Examples section below.
**Actual signatures needed:**
- `addBonusPoints(Long enterpriseId, Integer points, String description, Long triggeredBy)`
- `rateEnterprise(Long enterpriseId, String year, BigDecimal totalEmission, BigDecimal revenue, Long ratedBy)`
- `commitReportToChain(Long reportId, String reportData)` -- this one matches D-04

### Pitfall 2: Frontend Double-Bug in Review Flow (CRITICAL)
**What goes wrong:** Two bugs prevent the review flow from working:
1. `carbon.ts:29` checks `data.status` but the form sends `data.approved` -- validation always rejects
2. Even if validation passed, the raw `{approved, comment}` object would be sent to backend which expects `{reviewResult, reviewComment}`
**Why it happens:** The frontend `ReviewRequest` type was updated to match backend (has `reviewResult`, `reviewComment`) but `AuditList.vue` still sends `{approved, comment}`, and `carbon.ts` validates against `status` (a third name).
**How to avoid:** Rewrite `carbon.ts reviewReport()` to accept the Vue form shape `{reportId, approved, comment}` and map to backend shape `{reportId, reviewResult, reviewComment}`.

### Pitfall 3: EmissionRatingService Duplicate Prevention
**What goes wrong:** `rateEnterprise()` throws `BusinessException(3001)` if a rating already exists for that enterprise+year combination.
**Why it happens:** The method checks `findByEnterpriseIdAndRatingYear()` and throws if found.
**How to avoid:** The `accountingPeriod` from the report (e.g., "2024-Q1") is used as the `year` parameter. Since each report has a unique accounting period, duplicates should not occur. However, if two reports share the same accounting period for the same enterprise, the second approval will fail.
**Warning signs:** Approval returns error code 3001 ("该企业...年评级已存在").

### Pitfall 4: Frontend Pagination Parameter Mismatch
**What goes wrong:** `CarbonUpload.vue` sends `title` and `accountingPeriod` as query params to `getMyReports()`, but the backend `/carbon/my-reports` endpoint only accepts `status`, `page`, and `size`.
**Why it happens:** The frontend search form has fields that the backend endpoint does not support filtering on.
**How to avoid:** The extra params are silently ignored by Spring (not declared as `@RequestParam`). The search will return all reports for the enterprise regardless of title/period filter. This is acceptable for Phase 2 testing -- the frontend just won't filter by title/period.

### Pitfall 5: Credit Score Cap at 100
**What goes wrong:** After approval, `addBonusPoints` with +5 points is capped at 100 (Math.min(100, scoreBefore + points)). Since seed data starts at 100, the first approval will NOT change the score (100 + 5 capped to 100).
**Why it happens:** The credit score system enforces a 0-100 range.
**How to avoid:** For testing, verify that the CreditEvent record is created (showing +5 attempt) even if the score stays at 100. The score update IS working correctly -- it just cannot exceed 100. To see actual score change, the enterprise would need to first have points deducted below 100.

### Pitfall 6: State Machine Enforcement Gap
**What goes wrong:** The current `reviewReport()` blindly sets `report.setStatus(request.getReviewResult())` without validating the requested status is a valid transition target.
**Why it happens:** The code checks `isReviewable()` (valid: SUBMITTED or UNDER_REVIEW) but does not validate that the `reviewResult` value is one of the valid target states (APPROVED=3 or REJECTED=4).
**How to avoid:** For Phase 2, this is acceptable since the test will only send reviewResult=3 or 4. However, CARB-11 requires testing that illegal transitions are rejected. The `isReviewable()` check already prevents reviewing a DRAFT report.

## Code Examples

### Verified Method Signatures for Cascading Side Effects

These are the ACTUAL signatures found in the codebase, not the simplified versions in CONTEXT.md D-04.

#### CreditScoreService.addBonusPoints() [VERIFIED: CreditScoreService.java:179-180]
```java
@Transactional
public CreditScoreResponse addBonusPoints(Long enterpriseId, Integer points,
                                            String description, Long triggeredBy)
```
- `enterpriseId`: from `report.getEnterpriseId()`
- `points`: 5 (per D-04)
- `description`: descriptive string, e.g., "碳报告审核通过奖励"
- `triggeredBy`: from `currentUser.getUserId()` (the reviewer)
- Returns: `CreditScoreResponse` (can be logged but not required for report update)

#### EmissionRatingService.rateEnterprise() [VERIFIED: EmissionRatingService.java:52-54]
```java
@Transactional
public EmissionRating rateEnterprise(Long enterpriseId, String year,
                                      BigDecimal totalEmission, BigDecimal revenue,
                                      Long ratedBy)
```
- `enterpriseId`: from `report.getEnterpriseId()`
- `year`: from `report.getAccountingPeriod()` (e.g., "2024-Q1")
- `totalEmission`: from `report.getTotalEmission()` (calculated during submit)
- `revenue`: null (not available in carbon report context)
- `ratedBy`: from `currentUser.getUserId()` (the reviewer)
- Returns: `EmissionRating` entity
- THROWS: `BusinessException(3001)` if rating already exists for enterprise+year

#### BlockchainService.commitReportToChain() [VERIFIED: BlockchainService.java:73]
```java
public String commitReportToChain(Long reportId, String reportData)
```
- `reportId`: from `report.getId()`
- `reportData`: from `report.getEmissionData()` (JSON string)
- Returns: mock txHash string like `tx_mock_1715234567890_a1b2c3d4`
- No exception thrown in mock mode

### Frontend Field Mapping Fix [VERIFIED: AuditList.vue + carbon.ts + ReviewRequest.java]

Current broken code in `carbon.ts`:
```typescript
// BROKEN: checks data.status (doesn't exist), sends raw object
export function reviewReport(data: ReviewRequest): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('报告ID不能为空'))
  if (!data?.status) return Promise.reject(new Error('审核状态不能为空'))  // BUG: status undefined
  return request.post('/carbon/review', data)  // BUG: sends {reportId, reviewResult, reviewComment}
}
```

AuditList.vue sends:
```typescript
await reviewReport({
  reportId: reviewForm.value.reportId,
  approved: reviewForm.value.approved,   // boolean
  comment: reviewForm.value.comment,     // string
})
```

Fix required in `carbon.ts`:
```typescript
export function reviewReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('报告ID不能为空'))
  return request.post('/carbon/review', {
    reportId: data.reportId,
    reviewResult: data.approved ? 3 : 4,
    reviewComment: data.comment,
  })
}
```

### Test Report JSON Structures

#### emissionData JSON (for create report API call)
```json
{
  "scope1": [
    {"name": "天然气燃烧", "activity_data": 1000, "emission_factor": 2.0},
    {"name": "柴油消耗", "activity_data": 500, "emission_factor": 2.7}
  ],
  "scope2": [
    {"name": "电力消耗", "activity_data": 5000, "emission_factor": 0.6}
  ],
  "scope3": [
    {"name": "商务出差", "activity_data": 200, "emission_factor": 0.15}
  ]
}
```
Total emissions: scope1=3350 + scope2=3000 + scope3=30 = 6380 tCO2e (Level C: 5000-20000)

#### attachments JSON (after file upload)
```json
["reports/evidence-file.pdf"]
```
The `objectName` from `MinioService.UploadResult` goes into this array.

### API Test Flow Sequence

```
1. Login as enterprise001 -> TOKEN_E1
2. Login as enterprise002 -> TOKEN_E2
3. Login as reviewer001   -> TOKEN_R
4. Login as authenticator001 -> TOKEN_A

5. [enterprise001] POST /file/upload (test file) -> attachment URL
6. [enterprise001] POST /carbon/reports (with emissionData, attachment) -> report1 (DRAFT)
7. [enterprise001] POST /carbon/reports (second report) -> report2 (DRAFT)
8. [enterprise002] POST /carbon/reports -> report3 (DRAFT)

9. [enterprise001] GET /carbon/my-reports -> verify report1, report2 in list
10. [enterprise001] GET /carbon/reports/{report1.id} -> verify detail fields
11. [enterprise001] POST /carbon/reports/{report1.id}/submit -> report1 SUBMITTED
12. [enterprise001] POST /carbon/reports/{report2.id}/submit -> report2 SUBMITTED

13. [reviewer001] GET /carbon/reports?status=1 -> verify report1, report2 in review queue
14. [reviewer001] POST /carbon/review {reportId: report1.id, reviewResult: 3, reviewComment: "Approved"} -> APPROVED
15. Verify report1 status=5 (ON_CHAIN), blockchainTxHash is set, onChainAt is set
16. Verify credit_score for enterprise001: CreditEvent created (+5 points)
17. Verify emission_rating for enterprise001: Rating record created

18. [reviewer001] POST /carbon/review {reportId: report2.id, reviewResult: 4, reviewComment: "Data incomplete"} -> REJECTED

19. [enterprise001] GET /carbon/my-reports -> verify report1=ON_CHAIN, report2=REJECTED

20. [authenticator001] GET /carbon/reports -> verify authenticator can list (read-only)
21. [enterprise001] POST /carbon/review -> verify 403 (cross-role denied)

22. [enterprise001] POST /carbon/reports/{report1.id}/submit -> verify error (cannot resubmit ON_CHAIN)
23. [enterprise001] POST /carbon/review -> verify 403 (enterprise cannot review)

24. Data isolation: [enterprise001] GET /carbon/my-reports -> should NOT contain report3
25. [enterprise002] GET /carbon/my-reports -> should contain only report3
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom auth filter chains | Spring Security 6.x lambda DSL | Spring Boot 3.x | SecurityConfig uses lambda style, CSRF disabled for JWT |
| Controller-based pagination conversion | Axios interceptor pagination mapping | Project convention | `request.ts` line 44-52 converts pageNum/pageSize to page/size |

**No deprecated patterns in this phase's scope.** All code uses current project conventions.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `accountingPeriod` format "2024-Q1" works as `year` parameter for `rateEnterprise()` | Code Examples | If the method expects a 4-digit year like "2024", ratings will be created per-quarter instead of per-year. Low risk -- the method stores whatever string is passed. |
| A2 | No Flyway migration needed for ON_CHAIN(5) status | Architecture | The `ReportStatusEnum` already includes ON_CHAIN(5) and the `carbon_report.status` column is Integer. No schema change needed. |
| A3 | The `@Transactional` on `reviewReport()` will correctly roll back all cascading service calls if any fails | Architecture | If `CreditScoreService` or `EmissionRatingService` use `REQUIRES_NEW` propagation, their changes would NOT roll back. Verified: they use default `@Transactional` (REQUIRED), so they join the existing transaction. |
| A4 | `enterprise002` has no pre-existing credit score events, so its first approval will be a clean test | Test Data | V2 seed data inserts credit_score with score=100 for enterprise 2 (id=2). No credit events exist. First approval creates the first event. |

**Note on assumptions:** A2 and A3 are LOW risk -- verified through code reading but not runtime-tested.

## Open Questions

1. **Should `revenue` be null for emission rating?**
   - What we know: `rateEnterprise()` takes a `revenue` parameter used to calculate `emissionIntensity`. Carbon reports do not contain revenue data.
   - What's unclear: Whether a null revenue causes any issues downstream.
   - Recommendation: Pass null. When revenue is null or zero, the method sets intensity to null. This is documented behavior in EmissionRatingService.java line 64-65.

2. **Should the test script be one combined script or split into separate scripts?**
   - What we know: CONTEXT.md leaves this to Claude's discretion. Phase 1 used a single `login-test.sh`.
   - What's unclear: Whether a single script would be too long or hard to debug.
   - Recommendation: Single combined script `carbon-report-test.sh` following the `login-test.sh` pattern. All 13 CARB requirements can be tested in one sequential flow. Use section comments to delineate CARB-01 through CARB-13 tests.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL (Docker on :3307) | Report data storage | Verified in Phase 1 | 8.x | -- |
| Redis (Docker) | Session/token blacklist | Verified in Phase 1 | 7.x | -- |
| MinIO (Docker) | File upload for attachments | Verified in Phase 1 | -- | -- |
| Backend (port 8080) | All API calls | Start manually | Spring Boot 3.2.5 | -- |
| Frontend (port 5173) | Browser verification | Start manually | Vite | -- |
| curl | API test scripts | Standard on Git Bash | -- | -- |
| bash | Script execution | Git Bash on Windows | -- | -- |

**Missing dependencies with no fallback:** None -- all infrastructure verified in Phase 1.

**Missing dependencies with fallback:** None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Bash API test scripts (curl) |
| Config file | None -- scripts are self-contained |
| Quick run command | `bash scripts/carbon-report-test.sh` |
| Full suite command | `bash scripts/carbon-report-test.sh` (same -- single script) |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CARB-01 | Create report with file upload | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-02 | List reports (paginated) | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-03 | View report detail | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-04 | Submit report DRAFT->SUBMITTED | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-05 | Reviewer list SUBMITTED reports | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-06 | Approve report | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-07 | Reject report | API test | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-08 | Credit score update after approval | API test (verify via GET credit score) | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-09 | Emission rating after approval | API test (verify via GET emission rating) | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-10 | Blockchain record after approval | API test (verify txHash in response) | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-11 | Illegal state transitions rejected | API test (negative test) | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-12 | Authenticator read-only access | API test (list success, review denied) | `bash scripts/carbon-report-test.sh` | Wave 0 |
| CARB-13 | Cross-role access control | API test (enterprise denied reviewer endpoint) | `bash scripts/carbon-report-test.sh` | Wave 0 |

### Sampling Rate
- **Per task commit:** `bash scripts/carbon-report-test.sh`
- **Per wave merge:** Full script run
- **Phase gate:** All 13 CARB tests green

### Wave 0 Gaps
- [ ] `scripts/carbon-report-test.sh` -- covers CARB-01 through CARB-13

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT Bearer tokens, Spring Security filter chain |
| V3 Session Management | yes | Stateless JWT, token blacklist on logout via Redis |
| V4 Access Control | yes | `@PreAuthorize` role-based annotations on controller methods |
| V5 Input Validation | yes | `@Valid` + Jakarta validation annotations on DTOs (`@NotBlank`, `@NotNull`, `@Size`) |
| V6 Cryptography | no | No encryption operations in this phase |

### Known Threat Patterns for Spring Boot + Vue Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Broken access control | Elevation of privilege | `@PreAuthorize` annotations with role checks on every endpoint |
| IDOR (Insecure Direct Object Reference) | Information disclosure | Ownership validation in `submitReport()` and `deleteReport()` checks enterpriseId match |
| Mass assignment | Tampering | DTOs with explicit field declarations, `@Valid` validation |
| SQL injection | Tampering | Spring Data JPA parameterized queries, `@Query` with `@Param` |
| XSS | Tampering | Vue 3 auto-escapes template interpolation |

## Sources

### Primary (HIGH confidence)
- `CarbonService.java` -- Full source read, all methods and signatures verified
- `CarbonController.java` -- All endpoints, @PreAuthorize roles, request/response types verified
- `ReportStatusEnum.java` -- Status codes 0-5, isEditable/isSubmittable/isReviewable verified
- `CreditScoreService.java` -- addBonusPoints() signature, score logic (0-100 cap, threshold checks) verified
- `EmissionRatingService.java` -- rateEnterprise() 5-parameter signature, A-E thresholds verified
- `BlockchainService.java` -- commitReportToChain() signature, mock txHash format verified
- `AuditList.vue` -- Review form sends {approved, comment}, not {reviewResult, reviewComment}
- `carbon.ts` -- Field mapping bug confirmed (checks data.status instead of mapping approved->reviewResult)
- `ReviewRequest.java` -- Backend expects {reportId, reviewResult, reviewComment}
- `CarbonReport.java` -- Entity has blockchainTxHash, onChainAt, attachments fields
- `FileController.java` -- Upload endpoint returns UploadResult(objectName, url, size, contentType)
- `V1__init_schema.sql` -- carbon_report table schema verified
- `V2__seed_data.sql` -- Enterprise 1 (user_id=2), Enterprise 2 (user_id=3), Reviewer (user_id=4), Authenticator (user_id=6)
- `V3__test_seed_data.sql` -- Enterprise 3 (user_id=7) added
- `login-test.sh` -- Pattern for API test scripts verified

### Secondary (MEDIUM confidence)
- `SecurityConfig.java` -- CORS, CSRF disabled, stateless session, JWT filter chain verified
- `request.ts` -- Axios interceptor converts pageNum/pageSize to page/size, transforms Spring Data Page

### Tertiary (LOW confidence)
None -- all findings verified by source code reading.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all dependencies already in project, verified by code reading
- Architecture: HIGH -- all method signatures verified by reading actual Java source
- Pitfalls: HIGH -- all pitfalls discovered by cross-referencing CONTEXT.md decisions with actual code
- Field mapping bug: HIGH -- confirmed by reading three related files (AuditList.vue, carbon.ts, ReviewRequest.java)

**Research date:** 2026-05-09
**Valid until:** 2026-06-09 (stable codebase, no framework changes expected)
