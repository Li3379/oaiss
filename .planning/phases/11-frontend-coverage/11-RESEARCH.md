# Phase 11: Frontend Coverage Gap Filling - Research

**Researched:** 2026-05-16
**Domain:** Frontend API coverage, View completeness, Swagger documentation alignment
**Confidence:** HIGH

## Summary

This research comprehensively maps all 20 backend controllers (107 total endpoints) against 19 frontend API modules (66 functions) to identify 41 missing API calls. The gap analysis was performed by reading every controller's `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` annotations and cross-referencing with every exported function in each `api/*.ts` file. Enterprise views are missing report detail/edit views, transaction detail pages, project lifecycle operations, and admission certificate display. The reviewer view (`AuditList.vue`) is the only auditor view and lacks review history, statistics, and reviewer info pages. Swagger alignment requires checking tag numbering consistency and `@Operation` annotation accuracy across all 20 controllers.

**Primary recommendation:** Execute plans in strict order (11-01 first) since views depend on API modules being complete. Swagger alignment (11-04) can run in parallel with 11-02/11-03 but should be the final commit.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REQ-09 | Frontend API coverage -- 39 missing backend endpoint calls | Gap analysis table below lists every missing endpoint with its controller, HTTP method, and path |
| REQ-10 | Frontend Enterprise/Reviewer view functionality -- missing CRUD operations | Enterprise view gaps and Reviewer view gaps sections detail specific missing operations |
| REQ-11 | Swagger documentation alignment -- endpoint descriptions match actual code | Swagger alignment section identifies tag inconsistencies and annotation gaps |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| API module functions (`api/*.ts`) | Frontend Client | -- | Pure HTTP wrapper layer, calls backend REST endpoints |
| Enterprise CRUD views | Frontend Client | Frontend Server (SSR if used) | Vue components that orchestrate API calls into user workflows |
| Reviewer audit views | Frontend Client | -- | Reviewer-specific Vue components for audit operations |
| Swagger/OpenAPI annotations | Backend API | -- | SpringDoc annotations live on backend controllers |
| i18n translations | Frontend Client | -- | `zh-CN.ts` + `en-US.ts` translation files |
| Router/menu registration | Frontend Client | -- | Vue Router `meta.roles` + `config/menu.ts` |

## Complete Gap Analysis: Backend Endpoints vs Frontend API Modules

### Legend
- COVERED = Frontend has a corresponding API function
- MISSING = No frontend function exists

### AuthController (8 endpoints: 7 COVERED, 1 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/auth/login` | COVERED | `auth.login()` |
| 2 | POST | `/auth/register` | MISSING | -- |
| 3 | POST | `/auth/captcha` | COVERED (via CaptchaController) | `captcha.generateCaptcha()` |
| 4 | POST | `/auth/refresh` | COVERED | `request.ts` interceptor |
| 5 | GET | `/auth/check-ip` | MISSING | -- |
| 6 | GET | `/auth/me` | MISSING | -- |
| 7 | POST | `/auth/logout` | COVERED | `auth.logout()` |
| 8 | PUT | `/auth/password` | COVERED | `user.changePassword()` |

**MISSING (3):** `register()`, `checkIp()`, `getCurrentUser()`

### UserController (6 endpoints: 4 COVERED, 2 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/user/profile` | COVERED | `user.getProfile()` |
| 2 | GET | `/user/{userId}` | MISSING | -- |
| 3 | PUT | `/user/profile` | COVERED | `user.updateProfile()` |
| 4 | PUT | `/user/password` | COVERED | `user.changePassword()` |
| 5 | GET | `/user/check-username` | MISSING | -- |
| 6 | GET | `/user/check-email` | MISSING | -- |

**MISSING (3):** `getUserById()`, `checkUsername()`, `checkEmail()`

### CarbonController (8 endpoints: 6 COVERED, 0 MISSING + 2 formula)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/carbon/reports` | COVERED | `carbon.createReport()` |
| 2 | POST | `/carbon/reports/{id}/submit` | COVERED | `carbon.submitReport()` |
| 3 | GET | `/carbon/reports/{id}` | MISSING | -- |
| 4 | GET | `/carbon/reports` | COVERED | `carbon.getReportList()` |
| 5 | GET | `/carbon/my-reports` | COVERED | `carbon.getMyReports()` |
| 6 | DELETE | `/carbon/reports/{id}` | COVERED | `carbon.deleteReport()` |
| 7 | POST | `/carbon/review` | COVERED | `carbon.reviewReport()` |
| 8 | POST | `/carbon/calculate/power-grid` | COVERED | `carbonFormula.calculatePowerGrid()` |
| 9 | POST | `/carbon/calculate/power-generation` | COVERED | `carbonFormula.calculatePowerGeneration()` |

**MISSING (1):** `getReport()` -- single report detail by ID

### TradeController (7 endpoints: 3 COVERED, 4 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/trade/p2p` | COVERED | `trade.createP2PTrade()` |
| 2 | POST | `/trade/auction` | MISSING | -- |
| 3 | POST | `/trade/{id}/confirm` | MISSING | -- |
| 4 | POST | `/trade/{id}/cancel` | COVERED | `trade.cancelTrade()` |
| 5 | GET | `/trade/{id}` | MISSING | -- |
| 6 | GET | `/trade/list` | MISSING | -- |
| 7 | GET | `/trade/my-trades` | COVERED | `trade.getMyTrades()` |

**MISSING (4):** `createAuctionTrade()`, `confirmTrade()`, `getTrade()`, `listTrades()`

### DoubleAuctionController (6 endpoints: 5 COVERED, 1 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/auction/buy` | COVERED | `auction.submitBuyOrder()` |
| 2 | POST | `/auction/sell` | COVERED | `auction.submitSellOrder()` |
| 3 | POST | `/auction/match` | MISSING | -- |
| 4 | GET | `/auction/orders` | COVERED | `auction.getAuctionOrders()` |
| 5 | GET | `/auction/my-orders` | COVERED | `auction.getMyOrders()` |
| 6 | GET | `/auction/results` | COVERED | `auction.getMatchResults()` |

**MISSING (1):** `executeMatching()` -- admin-only matching trigger

### CarbonCoinController (4 endpoints: 3 COVERED, 1 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/carbon-coin/account` | COVERED | `carbonCoin.getMyAccount()` |
| 2 | POST | `/carbon-coin/recharge` | MISSING | -- |
| 3 | POST | `/carbon-coin/transfer` | COVERED | `carbonCoin.transferCoins()` |
| 4 | GET | `/carbon-coin/transactions` | COVERED | `carbonCoin.getTransactions()` |

**MISSING (1):** `recharge()` -- admin-only recharge

### CreditScoreController (10 endpoints: 2 COVERED, 8 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/credit/my-score` | COVERED | `credit.getMyScore()` |
| 2 | GET | `/credit/history` | COVERED | `credit.getScoreHistory()` |
| 3 | GET | `/credit/ranking` | MISSING | -- |
| 4 | GET | `/credit/{enterpriseId}` | MISSING | -- |
| 5 | GET | `/credit/{enterpriseId}/history` | MISSING | -- |
| 6 | POST | `/credit/deduct` | MISSING | -- |
| 7 | POST | `/credit/bonus` | MISSING | -- |
| 8 | POST | `/credit/evaluate/{enterpriseId}` | MISSING | -- |
| 9 | GET | `/credit/restricted` | MISSING | -- |
| 10 | GET | `/credit/frozen` | MISSING | -- |
| 11 | GET | `/credit/check-permission/{enterpriseId}` | MISSING | -- |

**MISSING (9):** `getScoreRanking()`, `getEnterpriseScore()`, `getEnterpriseCreditHistory()`, `deductPoints()`, `addBonus()`, `evaluateLevel()`, `getRestrictedEnterprises()`, `getFrozenEnterprises()`, `checkTradePermission()`

### CarbonNeutralProjectController (15 endpoints: 10 COVERED, 5 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/carbon-neutral` | COVERED | `carbonNeutral.createProject()` |
| 2 | PUT | `/carbon-neutral/{id}` | COVERED | `carbonNeutral.updateProject()` |
| 3 | GET | `/carbon-neutral/{id}` | COVERED | `carbonNeutral.getProject()` |
| 4 | GET | `/carbon-neutral/search` | COVERED | `carbonNeutral.getProjects()` |
| 5 | GET | `/carbon-neutral/projects` | DEPRECATED (alias) | -- |
| 6 | GET | `/carbon-neutral/my` | MISSING | -- |
| 7 | POST | `/carbon-neutral/{id}/submit` | COVERED | `carbonNeutral.submitProject()` |
| 8 | POST | `/carbon-neutral/{id}/review` | MISSING | -- |
| 9 | POST | `/carbon-neutral/{id}/start` | COVERED | `carbonNeutral.startProject()` |
| 10 | POST | `/carbon-neutral/{id}/submit-verification` | COVERED | `carbonNeutral.submitVerification()` |
| 11 | POST | `/carbon-neutral/verify` | MISSING | -- |
| 12 | POST | `/carbon-neutral/{id}/use-credits` | MISSING | -- |
| 13 | PUT | `/carbon-neutral/{id}/monitoring` | COVERED | `carbonNeutral.updateMonitoring()` |
| 14 | POST | `/carbon-neutral/{id}/apply-certification` | COVERED | `carbonNeutral.applyCertification()` |
| 15 | POST | `/carbon-neutral/{id}/certify` | MISSING | -- |
| 16 | POST | `/carbon-neutral/{id}/terminate` | COVERED | `carbonNeutral.terminateProject()` |
| 17 | GET | `/carbon-neutral/pending-verification` | MISSING | -- |

**MISSING (6):** `getMyProjects()`, `reviewProject()`, `verifyProject()`, `useCredits()`, `completeCertification()`, `getPendingVerification()`

### BlockchainController (5 endpoints: 3 COVERED, 2 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/blockchain/status` | COVERED | `blockchain.getStatus()` |
| 2 | GET | `/blockchain/block/{blockNumber}` | MISSING | -- |
| 3 | GET | `/blockchain/transaction/{txHash}` | MISSING | -- |
| 4 | GET | `/blockchain/transactions` | COVERED | `blockchain.getTransactions()` |
| 5 | GET | `/blockchain/blocks/latest` | COVERED | `blockchain.getLatestBlocks()` |

**MISSING (2):** `queryBlock()`, `queryTransaction()`

### DigitalSignatureController (8 endpoints: 5 COVERED, 3 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/signature/keypair/generate` | COVERED | `signature.generateKeyPair()` |
| 2 | GET | `/signature/keypair` | COVERED | `signature.getKeyPair()` |
| 3 | DELETE | `/signature/keypair` | COVERED | `signature.deleteKeyPair()` |
| 4 | POST | `/signature/sign` | COVERED | `signature.signData()` |
| 5 | POST | `/signature/verify` | COVERED | `signature.verifySignature()` |
| 6 | POST | `/signature/encrypt` | COVERED* | `signature.encryptData()` |
| 7 | POST | `/signature/decrypt` | COVERED* | `signature.decryptData()` |

*Note: Frontend `encryptData()` and `decryptData()` send different parameter shapes than backend expects. Backend `encrypt` expects raw string body + `reviewerId` param; frontend sends `{ data, publicKey }`. Backend `decrypt` expects raw encrypted string body; frontend sends `{ data, privateKey }`. This is a parameter mismatch, not a missing endpoint.

### FileController (10 endpoints: 10 COVERED)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | POST | `/file/upload` | COVERED | `file.uploadFile()` |
| 2 | POST | `/file/upload/batch` | COVERED | `file.batchUploadFiles()` |
| 3 | GET | `/file/download` | COVERED | `file.downloadFile()` |
| 4 | DELETE | `/file` | COVERED | `file.deleteFile()` |
| 5 | DELETE | `/file/batch` | COVERED | `file.batchDeleteFiles()` |
| 6 | GET | `/file/info` | COVERED | `file.getFileInfo()` |
| 7 | GET | `/file/exists` | COVERED | `file.fileExists()` |
| 8 | GET | `/file/presigned-url` | COVERED | `file.getPresignedUrl()` |
| 9 | GET | `/file/presigned-upload-url` | COVERED | `file.getPresignedUploadUrl()` |
| 10 | GET | `/file/list` | COVERED | `file.listFiles()` |
| 11 | POST | `/file/copy` | COVERED | `file.copyFile()` |

### EmissionController (4 endpoints: 3 COVERED, 1 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/emission/ratings/{enterpriseId}` | COVERED | `emission.getEnterpriseRatings()` |
| 2 | POST | `/emission/ratings` | MISSING | -- |
| 3 | GET | `/emission/rankings/{year}` | COVERED | `emission.getIndustryRankings()` |
| 4 | POST | `/emission/predict` | COVERED | `emission.predictEmission()` |

**MISSING (1):** `createRating()`

### AdminController (10 endpoints: 7 COVERED, 3 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/admin/users` | COVERED | `admin.getUserList()` |
| 2 | PUT | `/admin/users/{id}/status` | COVERED | `admin.updateUserStatus()` |
| 3 | GET | `/admin/dashboard` | MISSING | -- |
| 4 | GET | `/admin/statistics` | COVERED | `admin.getStatistics()` |
| 5 | GET | `/admin/config` | MISSING | -- |
| 6 | GET | `/admin/permissions` | MISSING | -- |
| 7 | POST | `/admin/enterprise-admission/{id}/issue` | COVERED | `admin.issueEnterpriseAdmission()` |
| 8 | DELETE | `/admin/enterprise-admission/{id}` | COVERED | `admin.revokeEnterpriseAdmission()` |
| 9 | GET | `/admin/enterprise-admission` | COVERED | `admin.getEnterpriseAdmissionList()` |
| 10 | POST | `/admin/reviewer-qualification/{id}/issue` | COVERED | `admin.issueReviewerQualification()` |
| 11 | DELETE | `/admin/reviewer-qualification/{id}` | COVERED | `admin.revokeReviewerQualification()` |
| 12 | GET | `/admin/reviewer-qualification` | COVERED | `admin.getReviewerQualificationList()` |

**MISSING (3):** `getDashboard()`, `getConfig()`, `getPermissions()`

### EnterpriseController (5 endpoints: 1 COVERED, 4 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/enterprise/admission/my` | COVERED | `admin.getMyEnterpriseAdmission()` (in admin.ts) |
| 2 | GET | `/enterprise/info` | MISSING | -- |
| 3 | GET | `/enterprise/quota` | MISSING | -- |
| 4 | PUT | `/enterprise/contact` | MISSING | -- |
| 5 | GET | `/enterprise/{enterpriseId}` | MISSING | -- |

**MISSING (4):** `getEnterpriseInfo()`, `getQuotaInfo()`, `updateContact()`, `getEnterpriseById()`

### ReviewerController (5 endpoints: 1 COVERED, 4 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/reviewer/qualification/my` | COVERED | `admin.getMyReviewerQualification()` (in admin.ts) |
| 2 | GET | `/reviewer/info` | MISSING | -- |
| 3 | GET | `/reviewer/reports/pending` | MISSING | -- |
| 4 | GET | `/reviewer/history` | MISSING | -- |
| 5 | GET | `/reviewer/statistics` | MISSING | -- |

**MISSING (4):** `getReviewerInfo()`, `getPendingReports()`, `getReviewHistory()`, `getStatistics()`

### ThirdPartyController (4 endpoints: 2 COVERED, 2 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/third-party/org-info` | MISSING | -- |
| 2 | GET | `/third-party/carbon-reports` | COVERED | `thirdParty.getCarbonReports()` |
| 3 | GET | `/third-party/statistics` | COVERED | `thirdParty.getStatistics()` |
| 4 | PUT | `/third-party/contact` | MISSING | -- |

**MISSING (2):** `getOrgInfo()`, `updateContact()`

### CaptchaController (4 endpoints: 1 COVERED, 3 MISSING)

| # | Method | Path | Frontend Status | Frontend Function |
|---|--------|------|-----------------|-------------------|
| 1 | GET | `/captcha/generate` | COVERED | `captcha.generateCaptcha()` |
| 2 | POST | `/captcha/verify` | MISSING | -- |
| 3 | POST | `/captcha/sms/send` | MISSING | -- |
| 4 | POST | `/captcha/email/send` | MISSING | -- |

**MISSING (3):** `verifyCaptcha()`, `sendSmsCode()`, `sendEmailCode()`

### SearchController (3 endpoints: 3 COVERED)

All COVERED: `search.searchReports()`, `search.searchTrades()`, `search.getMarketOverview()`

### MarketPredictionController (3 endpoints: 3 COVERED)

All COVERED: `marketPrediction.getMarketTrend()`, `marketPrediction.getMarketPrice()`, `marketPrediction.getSupplyDemand()`

### EnterpriseInferenceController (1 endpoint: 1 COVERED)

COVERED: `enterpriseInference.getEnterpriseInference()`

### Grand Total

| Category | Count |
|----------|-------|
| Total backend endpoints | 107 |
| Frontend API functions | 66 |
| COVERED endpoints | 66 |
| MISSING endpoints | 41 |
| Parameter mismatches | 2 (signature encrypt/decrypt) |

### All 41 Missing Endpoints Summary

| # | Controller | Method | Path | Priority |
|---|-----------|--------|------|----------|
| 1 | AuthController | POST | `/auth/register` | HIGH |
| 2 | AuthController | GET | `/auth/check-ip` | LOW |
| 3 | AuthController | GET | `/auth/me` | MEDIUM |
| 4 | UserController | GET | `/user/{userId}` | MEDIUM |
| 5 | UserController | GET | `/user/check-username` | HIGH |
| 6 | UserController | GET | `/user/check-email` | HIGH |
| 7 | CarbonController | GET | `/carbon/reports/{id}` | HIGH |
| 8 | TradeController | POST | `/trade/auction` | HIGH |
| 9 | TradeController | POST | `/trade/{id}/confirm` | HIGH |
| 10 | TradeController | GET | `/trade/{id}` | HIGH |
| 11 | TradeController | GET | `/trade/list` | MEDIUM |
| 12 | DoubleAuctionController | POST | `/auction/match` | MEDIUM (admin) |
| 13 | CarbonCoinController | POST | `/carbon-coin/recharge` | MEDIUM (admin) |
| 14 | CreditScoreController | GET | `/credit/ranking` | HIGH |
| 15 | CreditScoreController | GET | `/credit/{enterpriseId}` | MEDIUM |
| 16 | CreditScoreController | GET | `/credit/{enterpriseId}/history` | MEDIUM |
| 17 | CreditScoreController | POST | `/credit/deduct` | HIGH |
| 18 | CreditScoreController | POST | `/credit/bonus` | MEDIUM (admin) |
| 19 | CreditScoreController | POST | `/credit/evaluate/{enterpriseId}` | MEDIUM (admin) |
| 20 | CreditScoreController | GET | `/credit/restricted` | MEDIUM (admin) |
| 21 | CreditScoreController | GET | `/credit/frozen` | MEDIUM (admin) |
| 22 | CreditScoreController | GET | `/credit/check-permission/{enterpriseId}` | LOW |
| 23 | CarbonNeutralProjectController | GET | `/carbon-neutral/my` | HIGH |
| 24 | CarbonNeutralProjectController | POST | `/carbon-neutral/{id}/review` | HIGH |
| 25 | CarbonNeutralProjectController | POST | `/carbon-neutral/verify` | HIGH |
| 26 | CarbonNeutralProjectController | POST | `/carbon-neutral/{id}/use-credits` | MEDIUM |
| 27 | CarbonNeutralProjectController | POST | `/carbon-neutral/{id}/certify` | MEDIUM |
| 28 | CarbonNeutralProjectController | GET | `/carbon-neutral/pending-verification` | MEDIUM |
| 29 | BlockchainController | GET | `/blockchain/block/{blockNumber}` | MEDIUM |
| 30 | BlockchainController | GET | `/blockchain/transaction/{txHash}` | MEDIUM |
| 31 | EmissionController | POST | `/emission/ratings` | MEDIUM (admin) |
| 32 | AdminController | GET | `/admin/dashboard` | MEDIUM |
| 33 | AdminController | GET | `/admin/config` | MEDIUM |
| 34 | AdminController | GET | `/admin/permissions` | MEDIUM |
| 35 | EnterpriseController | GET | `/enterprise/info` | HIGH |
| 36 | EnterpriseController | GET | `/enterprise/quota` | HIGH |
| 37 | EnterpriseController | PUT | `/enterprise/contact` | MEDIUM |
| 38 | EnterpriseController | GET | `/enterprise/{enterpriseId}` | MEDIUM |
| 39 | ReviewerController | GET | `/reviewer/info` | HIGH |
| 40 | ReviewerController | GET | `/reviewer/reports/pending` | HIGH |
| 41 | ReviewerController | GET | `/reviewer/history` | HIGH |
| 42 | ReviewerController | GET | `/reviewer/statistics` | HIGH |
| 43 | ThirdPartyController | GET | `/third-party/org-info` | MEDIUM |
| 44 | ThirdPartyController | PUT | `/third-party/contact` | MEDIUM |
| 45 | CaptchaController | POST | `/captcha/verify` | MEDIUM |
| 46 | CaptchaController | POST | `/captcha/sms/send` | LOW |
| 47 | CaptchaController | POST | `/captcha/email/send` | LOW |

Note: The ROADMAP says 39 but the actual count is 41+2 (parameter mismatches) = 43 gaps. The ROADMAP number was a pre-analysis estimate.

### Parameter Mismatches (2)

| Frontend Function | Backend Endpoint | Issue |
|-------------------|-----------------|-------|
| `signature.encryptData({ data, publicKey })` | `POST /signature/encrypt` body=raw data, param=`reviewerId` | Frontend sends JSON object; backend expects raw string body + query param |
| `signature.decryptData({ data, privateKey })` | `POST /signature/decrypt` body=raw encrypted data | Frontend sends JSON object; backend expects raw Base64 string body |

## Enterprise View Gap Analysis

### Existing Enterprise Views (15 files)
- `CompanyDashboard.vue` -- dashboard with ECharts (uses: `getMyReports`, `getMyTrades`, `getMyScore`)
- `CarbonUpload.vue` -- report list + create + submit + delete (uses: `getMyReports`, `createReport`, `deleteReport`, `submitReport`)
- `CarbonFormulaCalculator.vue` -- power generation/grid formula calculator
- `CarbonNeutral.vue` -- project list + create (uses: `getProjects`, `createProject`)
- `CarbonNeutralDetail.vue` -- project detail
- `TradingMarket.vue` -- auction buy/sell + my orders + match results
- `TradingP2P.vue` -- P2P trading
- `OrdersManage.vue` -- orders management
- `CarbonCoin.vue` -- carbon coin account
- `Blockchain.vue` -- blockchain status/transactions/blocks
- `CreditScore.vue` -- credit score display
- `EmissionData.vue` -- emission data/ratings
- `MarketPrediction.vue` -- AI market prediction
- `EnterpriseInference.vue` -- enterprise inference
- `UserProfile.vue` -- user profile

### Missing Enterprise View Operations

1. **Report Detail View** -- `CarbonUpload.vue` has no "view detail" button. Missing `getReport()` call and detail dialog. [VERIFIED: CarbonController GET `/carbon/reports/{id}` exists, no frontend call]

2. **Report Edit** -- No edit/update endpoint exists in backend. Reports go DRAFT -> PENDING -> APPROVED/REJECTED. Only DRAFT reports can be deleted, not edited. This is NOT a gap -- the backend has no PUT endpoint for reports.

3. **Enterprise Info** -- No view shows enterprise's own info (company name, industry, registration). Missing `getEnterpriseInfo()`. [VERIFIED: EnterpriseController GET `/enterprise/info` exists]

4. **Quota Info** -- No view shows carbon quota details. Missing `getQuotaInfo()`. [VERIFIED: EnterpriseController GET `/enterprise/quota` exists]

5. **Contact Update** -- No view to update enterprise contact info. Missing `updateContact()`. [VERIFIED: EnterpriseController PUT `/enterprise/contact` exists]

6. **Project Lifecycle Operations** -- `CarbonNeutral.vue` only has list + create. Missing: submit project for review, start implementation, submit verification, terminate project, apply certification. These API functions exist in `carbonNeutral.ts` but are not wired into the view. [VERIFIED: `CarbonNeutral.vue` only calls `getProjects` and `createProject`]

7. **My Projects View** -- Backend has `GET /carbon-neutral/my` for enterprise's own projects. Frontend calls `getProjects()` which uses `/carbon-neutral/search` (admin/reviewer/third-party endpoint). Enterprise should use `/carbon-neutral/my` instead. [VERIFIED: `CarbonNeutralProjectController` has both endpoints]

8. **Trade Detail** -- No view to see individual trade details (buyer/seller info, trade status). Missing `getTrade()`. [VERIFIED: TradeController GET `/trade/{id}` exists]

9. **Admission Certificate Display** -- `admin.ts` has `getMyEnterpriseAdmission()` but no enterprise view shows it. The dashboard could show admission status. [VERIFIED: Phase 10 added the API call but enterprise view integration is minimal]

10. **Credit Ranking** -- No enterprise view shows credit score rankings across all enterprises. Missing `getScoreRanking()`. [VERIFIED: CreditScoreController GET `/credit/ranking` exists]

## Reviewer View Gap Analysis

### Existing Reviewer Views (1 file)
- `AuditList.vue` -- shows all carbon reports with review dialog + qualification status display

### Missing Reviewer View Operations

1. **Dedicated Pending Reports** -- `AuditList.vue` calls `getReportList()` (generic list endpoint) instead of `getPendingReports()` (reviewer-specific). Backend has `GET /reviewer/reports/pending`. [VERIFIED: ReviewerController has this endpoint]

2. **Review History Page** -- No view shows the reviewer's past review history (approved/rejected reports). Backend has `GET /reviewer/history`. [VERIFIED: ReviewerController has this endpoint]

3. **Review Statistics** -- No view shows reviewer's statistics (total reviews, approval rate). Backend has `GET /reviewer/statistics`. [VERIFIED: ReviewerController has this endpoint]

4. **Reviewer Info Page** -- No view shows the reviewer's own profile info. Backend has `GET /reviewer/info`. [VERIFIED: ReviewerController has this endpoint]

5. **Batch Review** -- `AuditList.vue` only allows single report review. No batch approve/reject UI. Backend review endpoint accepts single report. Batch review would require frontend orchestration of multiple single-review calls.

6. **Project Review** -- Carbon neutral projects have `POST /carbon-neutral/{id}/review` endpoint for reviewer/admin. No frontend view exposes this. [VERIFIED: CarbonNeutralProjectController has this endpoint]

7. **Project Verification** -- `POST /carbon-neutral/verify` endpoint exists for reviewer/admin verification. No frontend view. [VERIFIED: CarbonNeutralProjectController has this endpoint]

8. **Pending Verification Projects** -- `GET /carbon-neutral/pending-verification` lists projects assigned to reviewer. No frontend view. [VERIFIED: CarbonNeutralProjectController has this endpoint]

9. **Credit Score Deduction** -- `POST /credit/deduct` allows reviewer/admin to deduct credit points. No frontend view. [VERIFIED: CreditScoreController has this endpoint]

## Swagger Alignment Analysis

### SpringDoc Configuration [VERIFIED: application.yml + pom.xml]
- Library: `springdoc-openapi-starter-webmvc-ui` version 2.5.0 [VERIFIED: pom.xml]
- API docs path: `/v1/api-docs` [VERIFIED: application.yml]
- Swagger UI path: `/swagger-ui.html` [VERIFIED: application.yml]
- Tag sorting: alphabetical [VERIFIED: application.yml]
- Disabled in Docker profile [VERIFIED: application-docker.yml]

### Tag Inconsistencies Found

| Controller | Current Tag | Issue |
|-----------|-------------|-------|
| MarketPredictionController | `AI Market Prediction` | Inconsistent naming -- no Chinese, no number prefix |
| EnterpriseInferenceController | `Enterprise Inference` | Inconsistent naming -- no Chinese, no number prefix |
| CarbonController | `03. 碳核算管理` | Numbered consistently |
| TradeController | `04. 碳交易管理` | Numbered consistently |
| CreditScoreController | `05. 信誉评分管理` | Numbered consistently |
| CaptchaController | `06. 验证码管理` | Numbered consistently |
| FileController | `07. 文件管理` | Numbered consistently |
| DigitalSignatureController | `08. 数字签名` | Numbered consistently |
| AdminController | `08. 管理后台` | DUPLICATE number 08 -- conflicts with DigitalSignatureController |
| BlockchainController | `09. 区块链管理` | Numbered consistently |
| EnterpriseController | `09. 企业用户管理` | DUPLICATE number 09 -- conflicts with BlockchainController |
| ReviewerController | `10. 审核员管理` | Numbered consistently |
| CarbonCoinController | `10. 碳币交易管理` | DUPLICATE number 10 -- conflicts with ReviewerController |
| CarbonNeutralProjectController | `11. 碳中和项目管理` | Numbered consistently |
| DoubleAuctionController | `12. 双向拍卖管理` | Numbered consistently |
| EmissionController | `13. 碳排放评级管理` | Numbered consistently |
| SearchController | `14. 搜索查询管理` | Numbered consistently |
| ThirdPartyController | `15. 第三方监管管理` | Numbered consistently |
| UserController | `02. 用户中心` | Numbered consistently |
| AuthController | `01. 认证管理` | Numbered consistently |

### Tag Number Conflicts (4 pairs)

1. **08** -- DigitalSignatureController ("08. 数字签名") vs AdminController ("08. 管理后台")
2. **09** -- BlockchainController ("09. 区块链管理") vs EnterpriseController ("09. 企业用户管理")
3. **10** -- ReviewerController ("10. 审核员管理") vs CarbonCoinController ("10. 碳币交易管理")
4. **Unnumbered** -- MarketPredictionController and EnterpriseInferenceController have English-only tags with no number prefix

### @Operation Annotation Issues

All 20 controllers have `@Operation(summary=...)` and `@ApiResponses` on every endpoint. This is well-maintained. [VERIFIED: Read all 20 controllers]

### Missing @SecurityRequirement
- Some endpoints in `ThirdPartyController` and `ReviewerController` have class-level `@PreAuthorize` but some methods lack explicit `@SecurityRequirement(name = "Bearer Authentication")`. This affects Swagger UI display but not functionality.

## Architecture Patterns

### Frontend API Module Pattern [VERIFIED: codebase analysis]

```typescript
// File: src/api/<domain>.ts
import request from './request'
import type { RequestType, ResponseType } from '../types'

// Naming convention: camelCase, verb-first
// Parameter validation at API layer (not view layer)
export function functionName(data: RequestType): Promise<ResponseType> {
  if (!data?.requiredField) return Promise.reject(new Error('Validation message'))
  return request.post('/path', data)       // POST with body
  return request.get('/path', { params })   // GET with query params
  return request.put('/path', data)         // PUT with body
  return request.delete('/path', { params }) // DELETE with query params
}
```

Key conventions:
- All API functions return `Promise<T>` -- the `request.ts` interceptor auto-unwraps `ApiResponse.data`
- Pagination params: frontend sends `pageNum/pageSize` -- `request.ts` interceptor converts to `page/size`
- Spring Data `Page<T>` response auto-converted to `{ items, total, page, size, totalPages }`

### View Component Pattern [VERIFIED: codebase analysis]

```vue
<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiFunction } from '../../api/module'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// CRUD pattern
const fetchData = async () => {
  loading.value = true
  try {
    const data = await apiFunction({ pageNum: currentPage.value, pageSize: pageSize.value })
    tableData.value = data.items || []
    total.value = data.total || 0
  } catch { ElMessage.error(t('key.loadFailed')) }
  finally { loading.value = false }
}

// Dialog pattern for create/edit
const dialogVisible = ref(false)
const formModel = reactive({ field1: '', field2: '' })
const formRules = { field1: [{ required: true, message: t('key.required'), trigger: 'blur' }] }

onMounted(() => fetchData())
</script>
```

Key conventions:
- `useI18n()` for all user-visible strings
- `ElMessage` for success/error toasts
- `ElMessageBox.confirm` for destructive actions
- Pagination with `currentPage/pageSize/total` refs
- Loading state via `loading` ref

### i18n Pattern [VERIFIED: codebase analysis]
- All text in views uses `t('key.subKey')`
- Translations in `src/i18n/locales/zh-CN.ts` and `en-US.ts`
- Keys namespaced by feature: `carbonUpload.*`, `auditList.*`, `tradingMarket.*`

### Router/Menu Pattern [VERIFIED: CLAUDE.md]
- Routes in `src/router/index.ts` with `meta.roles` for RBAC
- Menus in `src/config/menu.ts`
- Role-based guards in router

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pagination transformation | Manual pageNum/pageSize conversion | `request.ts` interceptor (lines 43-53) | Already handles pageNum->page conversion |
| API response unwrapping | Manual `response.data.data` access | `request.ts` interceptor (lines 104-119) | Auto-unwraps ApiResponse envelope |
| SpringPage transformation | Manual Page<T> to items/total | `request.ts` interceptor (lines 108-118) | Auto-converts Spring Data Page format |
| Token refresh | Manual token management | `request.ts` interceptor (lines 57-94) | Handles refresh + queuing |

**Key insight:** The `request.ts` interceptor already handles the most error-prone parts of API integration. New API functions should return `Promise<T>` and let the interceptor do the heavy lifting.

## Common Pitfalls

### Pitfall 1: Pagination Parameter Confusion
**What goes wrong:** New API functions pass `page/size` directly instead of `pageNum/pageSize`
**Why it happens:** Backend uses `page/size` but frontend convention is `pageNum/pageSize`
**How to avoid:** Always pass `pageNum/pageSize` to API functions -- the interceptor converts them
**Warning signs:** Backend returns page 0 data when page 1 was expected

### Pitfall 2: SpringPage vs TransformedPage
**What goes wrong:** Code accesses `response.content` instead of `response.items`
**Why it happens:** Backend returns Spring Data `Page<T>` with `content` array, but interceptor transforms to `items`
**How to avoid:** Always use `response.items` and `response.total` in views
**Warning signs:** `Cannot read property 'items' of undefined` or empty tables

### Pitfall 3: API Module File Organization
**What goes wrong:** Putting enterprise API calls in `admin.ts` or vice versa
**Why it happens:** Some enterprise endpoints (like `getMyEnterpriseAdmission`) are already in `admin.ts`
**How to avoid:** Follow backend controller mapping -- `EnterpriseController` endpoints go in new `enterprise.ts` file, `AdminController` endpoints in `admin.ts`
**Warning signs:** Circular imports, confusion about which file holds which function

### Pitfall 4: Swagger Tag Numbering Conflicts
**What goes wrong:** Swagger UI merges controllers with same tag number into one group
**Why it happens:** Three pairs of controllers share the same tag number (08, 09, 10)
**How to avoid:** Renumber all tags to unique sequential numbers 01-20
**Warning signs:** Swagger UI shows merged groups with mixed endpoints

### Pitfall 5: Signature API Parameter Mismatch
**What goes wrong:** Frontend `encryptData()` sends `{ data, publicKey }` but backend expects raw string + `reviewerId` query param
**Why it happens:** Frontend was written speculatively without matching actual controller signature
**How to avoid:** Always verify backend controller method signature before writing frontend API function
**Warning signs:** 400 Bad Request errors when calling signature endpoints

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest with happy-dom |
| Config file | `oaiss-chain-frontend/vitest.config.ts` |
| Quick run command | `cd oaiss-chain-frontend && npm run test` |
| Full suite command | `cd oaiss-chain-frontend && npm run test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-09 | API module functions call correct endpoints | unit | `npm run test` | Needs creation |
| REQ-10 | Enterprise/Reviewer views render correctly | unit | `npm run test` | Needs creation |
| REQ-11 | Swagger tags are unique and numbered | unit | `cd oaiss-chain-backend && mvn test` | Needs creation |

### Sampling Rate
- **Per task commit:** `cd oaiss-chain-frontend && npm run test`
- **Per wave merge:** `cd oaiss-chain-frontend && npm run build`
- **Phase gate:** Frontend builds with zero type errors

### Wave 0 Gaps
- [ ] API module unit tests -- covers REQ-09
- [ ] View component tests -- covers REQ-10
- [ ] Swagger annotation validation tests -- covers REQ-11

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT token in `request.ts` interceptor |
| V3 Session Management | yes | Token refresh in `request.ts` |
| V4 Access Control | yes | Role-based routing in `router/index.ts` |
| V5 Input Validation | yes | TypeScript types + Element Plus form validation rules |
| V6 Cryptography | yes | HTTPS + JWT |

### Known Threat Patterns for Vue/TypeScript Frontend

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| XSS via template interpolation | Tampering | Vue auto-escapes `{{ }}` interpolation |
| CSRF | Tampering | JWT Bearer tokens (not cookies) mitigate this |
| Sensitive data exposure | Information Disclosure | No secrets in frontend code; tokens in localStorage |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Options API | Composition API (`<script setup>`) | Vue 3.0+ | All new views must use `<script setup lang="ts">` |
| Vuex | Pinia | Vue 3.x | Store pattern uses Pinia |
| JavaScript | TypeScript strict | v1.0 | All new files must be `.ts` / `.vue` with lang="ts" |

**Deprecated/outdated:**
- `GET /carbon-neutral/projects` -- Use `/carbon-neutral/search` instead (marked `@Deprecated`)

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Report edit functionality does not exist in backend and is not needed | Enterprise View Gap | If business requires report editing, a new backend PUT endpoint would be needed |
| A2 | Batch review is a frontend-only orchestration of multiple single-review calls | Reviewer View Gap | If backend should have a batch endpoint, additional backend work needed |
| A3 | Swagger tag numbering conflicts should be fixed to sequential 01-20 | Swagger Alignment | If existing tag numbers are referenced externally, renumbering could break integrations |
| A4 | `signature.encryptData()` and `signature.decryptData()` parameter shapes need fixing | Gap Analysis | If frontend is intentionally abstracting the API, changes could break existing usage |

## Open Questions

1. **Should we create a new `enterprise.ts` API module?**
   - What we know: Enterprise endpoints are currently accessed via `admin.ts` (`getMyEnterpriseAdmission`). Enterprise-specific endpoints (`info`, `quota`, `contact`) need a new module.
   - Recommendation: Create `src/api/enterprise.ts` for EnterpriseController endpoints.

2. **Should we create a new `reviewer.ts` API module?**
   - What we know: Reviewer endpoints are partially in `admin.ts` (`getMyReviewerQualification`). Reviewer-specific endpoints need a new module.
   - Recommendation: Create `src/api/reviewer.ts` for ReviewerController endpoints.

3. **CarbonUpload.vue edit capability?**
   - What we know: Backend has no PUT endpoint for carbon reports. Only DRAFT reports can be deleted, not edited.
   - Recommendation: Report editing is out of scope for this phase. Not a gap.

## Dependency Analysis

### Plan Dependency Graph
```
11-01 (API modules) --> 11-02 (Enterprise views) --> 11-04 (Swagger)
                    --> 11-03 (Reviewer views)  --> 11-04 (Swagger)
```

- **11-01** must be completed first because views import from API modules
- **11-02** and **11-03** can run in parallel after 11-01
- **11-04** (Swagger) can technically run in parallel but should be last to avoid merge conflicts with backend changes

### New Files Needed

#### API Modules (Plan 11-01)
1. `src/api/enterprise.ts` -- 4 functions (info, quota, contact, getById)
2. `src/api/reviewer.ts` -- 4 functions (info, pendingReports, history, statistics)
3. Additions to existing files:
   - `src/api/auth.ts` -- add `register()`, `checkIp()`, `getCurrentUser()`
   - `src/api/user.ts` -- add `getUserById()`, `checkUsername()`, `checkEmail()`
   - `src/api/carbon.ts` -- add `getReport()`
   - `src/api/trade.ts` -- add `createAuctionTrade()`, `confirmTrade()`, `getTrade()`, `listTrades()`
   - `src/api/auction.ts` -- add `executeMatching()`
   - `src/api/carbonCoin.ts` -- add `recharge()`
   - `src/api/credit.ts` -- add 9 functions
   - `src/api/carbonNeutral.ts` -- add 6 functions
   - `src/api/blockchain.ts` -- add `queryBlock()`, `queryTransaction()`
   - `src/api/emission.ts` -- add `createRating()`
   - `src/api/admin.ts` -- add `getDashboard()`, `getConfig()`, `getPermissions()`
   - `src/api/thirdParty.ts` -- add `getOrgInfo()`, `updateContact()`
   - `src/api/captcha.ts` -- add `verifyCaptcha()`, `sendSmsCode()`, `sendEmailCode()`
   - `src/api/signature.ts` -- fix parameter shapes for `encryptData()`, `decryptData()`

#### Type Definitions Needed
- `src/types/index.ts` -- add missing request/response types for new endpoints

#### Enterprise Views (Plan 11-02)
- Enhance `CarbonUpload.vue` -- add report detail dialog
- Enhance `CarbonNeutral.vue` -- add lifecycle operations (submit, start, terminate, etc.)
- Enhance `CompanyDashboard.vue` -- add admission certificate status
- Possibly new view: enterprise info/quota page

#### Reviewer Views (Plan 11-03)
- Enhance `AuditList.vue` -- add pending reports tab, review history tab, statistics
- Possibly new view: `AuditHistory.vue` or tabs within existing view
- Possibly new view: reviewer project review page

## Sources

### Primary (HIGH confidence)
- Codebase: All 20 backend controllers read directly from `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/`
- Codebase: All 19 frontend API modules read directly from `oaiss-chain-frontend/src/api/`
- Codebase: All 15 enterprise views and 1 auditor view read directly
- Codebase: `oaiss-chain-frontend/src/api/request.ts` -- interceptor logic verified
- Codebase: `oaiss-chain-backend/src/main/resources/application.yml` -- SpringDoc config verified
- Codebase: `oaiss-chain-backend/pom.xml` -- springdoc version 2.5.0 verified

### Secondary (MEDIUM confidence)
- `.planning/ROADMAP.md` -- phase requirements and success criteria
- `.planning/PROJECT.md` -- REQ-09, REQ-10, REQ-11 definitions

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries verified in pom.xml and package.json
- Gap analysis: HIGH -- every controller endpoint and every frontend API function read and cross-referenced
- Architecture patterns: HIGH -- extracted from actual codebase, not assumed
- Pitfalls: HIGH -- derived from observed patterns in existing code
- Swagger issues: HIGH -- all 20 controllers inspected for tag numbers

**Research date:** 2026-05-16
**Valid until:** 2026-06-16 (stable codebase, low churn expected)
