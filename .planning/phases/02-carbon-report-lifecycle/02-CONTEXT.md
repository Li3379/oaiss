# Phase 2: Carbon Report Lifecycle - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

The complete carbon report lifecycle: enterprise creates/edits/submits reports (with file upload to MinIO), reviewer approves or rejects (with cascading side effects: credit score update, emission rating calculation, blockchain mock record), state machine validation, and cross-role access control. This phase delivers a verified core business flow that all downstream phases depend on.

</domain>

<decisions>
## Implementation Decisions

### Cascading Side Effects (CARB-08/09/10)
- **D-01:** Wire CreditScoreService, EmissionRatingService, and BlockchainService into `CarbonService.reviewReport()` after approval. These services exist as @Service beans but are not called from the review flow.
- **D-02:** Synchronous sequential execution — call credit score → emission rating → blockchain in order within reviewReport(). No async or event-driven complexity.
- **D-03:** Same transaction — all side effects run inside the existing @Transactional. If any side effect fails, the entire approval rolls back. Data consistency over partial success.
- **D-04:** Side effect details:
  - CreditScoreService.addBonusPoints(enterpriseId, 5) — approved report earns +5 credit score
  - EmissionRatingService.rateEnterprise(enterpriseId) — recalculates A-E emission rating based on total emissions
  - BlockchainService.commitReportToChain(reportId, reportData) — mock blockchain returns txHash, stored in report.blockchainTxHash

### Status State Machine (CARB-05/11)
- **D-05:** Add ON_CHAIN(5) transition — after approval + successful blockchain mock, transition report status from APPROVED(3) to ON_CHAIN(5). Set blockchainTxHash and onChainAt fields. This makes CARB-10 fully testable with actual status change.
- **D-06:** UNDER_REVIEW(2) stays unreachable — no "start review" endpoint. Test reviewer listing with SUBMITTED(1) status filter instead of UNDER_REVIEW(2). The reviewer sees SUBMITTED reports and reviews them directly. CARB-05 adjusted accordingly.
- **D-07:** Valid state transitions after fixes:
  - DRAFT(0) → SUBMITTED(1) via submitReport
  - SUBMITTED(1) → APPROVED(3) or REJECTED(4) via reviewReport
  - APPROVED(3) → ON_CHAIN(5) after blockchain mock (automatic within same transaction)
  - REJECTED(4) → SUBMITTED(1) via resubmit

### Test Data Approach
- **D-08:** API scripts + browser verify — create reports via API calls (like login-test.sh pattern from Phase 1), then verify UI displays them correctly in browser. Browser testing only for UI-specific checks.
- **D-09:** Use enterprise001 and enterprise002 for report testing. enterprise003 stays clean (no pre-existing data) for trading scenarios in Phase 3. This also enables data isolation testing (CARB-13) between 001 and 002.
- **D-10:** Real MinIO upload for attachments — upload a small test file via FileController API (`POST /file/upload`), include the returned URL in report `attachments` JSON field. Tests the actual file upload path.

### Authenticator Scope (CARB-12)
- **D-11:** Authenticator gets read-only access — authenticator001 can view the report list (`GET /carbon/reports`, AUTHENTICATOR already in @PreAuthorize) and view report details. No verify/certify operations (no endpoints exist). CARB-12 adjusted to verify read-only access only.

### Frontend-Backend Review Field Mapping
- **D-12:** Fix the field mismatch between frontend AuditList.vue and backend ReviewRequest. Frontend sends `{ reportId, approved (boolean), comment }` but backend expects `{ reportId, reviewResult (Integer), reviewComment }`. The API layer in `carbon.ts` must map `approved: true → reviewResult: 3`, `approved: false → reviewResult: 4`, `comment → reviewComment`. Without this fix, the review flow fails at API level.

### Claude's Discretion
- Exact emissionData JSON structure for test reports (scope1/2/3 arrays with activity_data and emission_factor)
- Test report titles and accounting periods (use timestamped unique names to avoid collisions)
- Whether to create a single combined script or separate scripts for report CRUD and review flows
- How many reports to create for testing (minimum 3: one approved, one rejected, one remaining draft)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend - Carbon Report Core
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonController.java` — REST endpoints, @PreAuthorize roles, request/response types
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonService.java` — Business logic, status transitions, emission calculation (THE file to modify for side effects)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CarbonReport.java` — Entity fields, attachments JSON, blockchainTxHash
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/ReportStatusEnum.java` — Status codes 0-5, isEditable/isSubmittable/isReviewable flags
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CarbonReportRequest.java` — Create request fields
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CarbonReportResponse.java` — Response with resolved names
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/ReviewRequest.java` — Review DTO (reportId, reviewResult, reviewComment)

### Backend - Cascading Services
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java` — addBonusPoints(), deductPoints(), evaluateLevel(), score thresholds
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EmissionRatingService.java` — rateEnterprise(), A-E level thresholds
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/BlockchainService.java` — commitReportToChain(), mock implementation

### Backend - File Upload
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java` — Upload/download/delete endpoints
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MinioService.java` — uploadFile(), presigned URLs
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/MinioConfig.java` — Bucket name, max file size

### Frontend
- `oaiss-chain-frontend/src/views/enterprise/CarbonUpload.vue` — Enterprise report table + create dialog
- `oaiss-chain-frontend/src/views/auditor/AuditList.vue` — Reviewer report list + approve/reject dialog
- `oaiss-chain-frontend/src/api/carbon.ts` — API client (field mapping fix needed here)
- `oaiss-chain-frontend/src/types/carbon.ts` — TypeScript types

### Requirements & Roadmap
- `.planning/REQUIREMENTS.md` — CARB-01 through CARB-13
- `.planning/ROADMAP.md` — Phase 2 success criteria, entry/exit criteria

### Database
- `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql` — carbon_report table definition
- `oaiss-chain-backend/src/main/resources/db/migration/V2__seed_data.sql` — Existing seed data

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `scripts/login-test.sh` — Pattern for API test scripts (login → verify → report). Reuse the login-as-user helper pattern for Phase 2 scripts.
- `scripts/health-check.sh` — Environment verification. Can be run before Phase 2 tests to ensure environment is ready.
- Phase 1 Playwright browser test pattern — addInitScript for token injection, used for browser role routing verification.

### Established Patterns
- API test script pattern: bash script with curl calls, ok/fail/info output helpers, exit on first failure
- JWT token storage: sessionStorage key "access_token" for browser tests
- API base path: `/api/v1`, response envelope `ApiResponse<T>` with `{ code, message, data }`
- Pagination: frontend sends pageNum/pageSize, backend returns Spring Data Page

### Integration Points
- `CarbonService.reviewReport()` — THE integration point. Must call CreditScoreService, EmissionRatingService, BlockchainService after setting status=APPROVED
- `carbon.ts reviewReport()` — Frontend API call that needs field mapping fix (approved → reviewResult)
- `FileController.uploadFile()` — Used before report creation to get attachment URLs
- Report status transitions must respect `isSubmittable()` and `isReviewable()` guards on ReportStatusEnum

### Critical Code Gaps to Fix
1. `CarbonService.reviewReport()` — wire cascading side effects (D-01)
2. `carbon.ts` — fix field mapping for review request (D-12)
3. `CarbonService.reviewReport()` — add ON_CHAIN transition after blockchain mock (D-05)
4. Consider: frontend AuditList.vue may need status filter adjustment (SUBMITTED instead of UNDER_REVIEW per D-06)

</code_context>

<specifics>
## Specific Ideas

- Test reports should use timestamped unique titles like "测试报告-2026Q1-{timestamp}" to avoid name collisions across test runs
- emissionData JSON structure follows this pattern: `{"scope1":[{"name":"...","activity_data":100,"emission_factor":0.5}],"scope2":[...],"scope3":[...]}`
- The three enterprises have equal starting conditions from V3: quota=50000, carbon_tradable=50000, credit_score=100, coin_balance=10000
- After approval + side effects: credit score increases by 5 (100→105), emission rating is calculated based on total emissions, blockchain mock returns a fake txHash

</specifics>

<deferred>
## Deferred Ideas

- UNDER_REVIEW(2) status implementation (requires new "start review" endpoint) — if needed in future, add a POST /carbon/reports/{id}/start-review endpoint
- VERIFIER/CERTIFIER role gap investigation — Phase 4
- Full authenticator verify/certify flow — Phase 5
- JWT refresh testing (ENV-09) — deferred indefinitely

</deferred>

---

*Phase: 02-carbon-report-lifecycle*
*Context gathered: 2026-05-09*
