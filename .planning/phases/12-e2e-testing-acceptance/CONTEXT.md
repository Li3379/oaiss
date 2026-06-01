# Phase 12: E2E Testing & Acceptance — Context

## Phase Goal
全量 E2E 自动化测试通过，覆盖率 90%+，通过率 90%+，v1.1.0 验收达标

## Success Criteria (from ROADMAP)
1. E2E 自动化测试覆盖 v1.1.0 全部 12 个需求项的验证场景
2. E2E 测试覆盖率 >= 90%（覆盖核心业务流程 endpoint）
3. E2E 测试通过率 >= 90%（允许少量非阻塞 flaky 失败）
4. v1.0 已验证功能未被 v1.1.0 修改破坏（回归测试通过）
5. v1.1.0 milestone 验收报告签署通过

## Requirements Coverage
| REQ | Name | Phase | What to Test |
|-----|------|-------|-------------|
| REQ-01 | 市场趋势预测 API | Phase 7 | MarketPredictionController returns price forecast, supply/demand prediction |
| REQ-02 | 企业推断 API | Phase 7 | EnterpriseInferenceController returns emission trend, compliance risk |
| REQ-03 | 碳排放 ML 预测 | Phase 7 | CarbonPredictionService uses Prophet regression (not stub) |
| REQ-04 | AI 前端页面 | Phase 8 | MarketPrediction.vue + EnterpriseInference.vue render ECharts |
| REQ-05 | 区块链真实对接 | Phase 9 | Fabric Gateway SDK writes/reads real chain data |
| REQ-06 | 碳核算公式 | Phase 8 | PowerGeneration (25-param) + PowerGrid (9-param) formulas |
| REQ-07 | 企业准入证书 | Phase 10 | Admin issues/revoke EnterpriseAdmission, enterprise views status |
| REQ-08 | 审核员资格证 | Phase 10 | Admin issues/revoke ReviewerQualification, reviewer views status |
| REQ-09 | 前端 API 覆盖 | Phase 11 | All backend endpoints have corresponding frontend API functions |
| REQ-10 | Enterprise 视图 | Phase 11 | CRUD operations in enterprise views functional |
| REQ-11 | Reviewer 视图 | Phase 11 | Audit operations in reviewer views functional |
| REQ-12 | Fabric CA | Phase 9 | (optional, may be mock) |

## Existing E2E Infrastructure

### Playwright Configuration
- File: `oaiss-chain-frontend/playwright.config.ts`
- Two modes: `smoke` (default, 15s timeout) and `flow` (`TEST_MODE=flow`, 30s timeout)
- baseURL: `http://localhost:5173`
- Single worker, Chromium only
- HTML + list reporters

### Test Structure (18 spec files total)
```
tests/e2e/
├── AGENTS.md
├── d1-login.spec.js           # legacy (v1.0 demo)
├── d2-carbon-upload.spec.js
├── d3-auction-market.spec.js
├── d4-p2p-trade.spec.js
├── d5-admin-users.spec.js
├── d6-third-party-monitor.spec.js
├── d7-carbon-neutral.spec.js
├── d8-credit-score.spec.js
├── fixtures/
│   ├── AGENTS.md
│   ├── auth.ts                 # loginViaApi + loginWithMonitor + MOCK_TOKENS
│   ├── auth-monitor.ts         # monitors 401/session expiry
│   ├── auth-resilient.ts       # retry on auth failures
│   ├── api-mock.ts             # mock API responses
│   ├── test-data.ts            # factory functions (createCarbonReport, createTrade, etc.)
│   └── page-objects/
│       ├── AGENTS.md
│       ├── Layout.ts
│       ├── LoginPage.ts
│       ├── CarbonUploadPage.ts
│       ├── AuditListPage.ts
│       ├── MonitorPage.ts
│       ├── AuctionMarketPage.ts
│       ├── P2PTradePage.ts
│       ├── CreditScorePage.ts
│       ├── CarbonCoinPage.ts
│       ├── CarbonNeutralPage.ts
│       ├── AdminUsersPage.ts
│       └── VerifyListPage.ts
├── smoke/                      # 5 smoke tests (enterprise, reviewer, third-party, admin, admin-verify)
│   ├── enterprise.smoke.spec.ts
│   ├── reviewer.smoke.spec.ts
│   ├── third-party.smoke.spec.ts
│   ├── admin.smoke.spec.ts
│   └── admin.verify.smoke.spec.ts
└── flows/                      # 13 flow tests (v1.0 features)
    ├── auth-flow.spec.ts
    ├── auth-resilience-flow.spec.ts
    ├── carbon-report-flow.spec.ts
    ├── carbon-coin-flow.spec.ts
    ├── auction-flow.spec.ts
    ├── p2p-trade-flow.spec.ts
    ├── credit-score-flow.spec.ts
    ├── carbon-neutral-flow.spec.ts
    ├── blockchain-flow.spec.ts
    ├── digital-signature-flow.spec.ts
    ├── file-management-flow.spec.ts
    ├── admin-manage-flow.spec.ts
    └── third-party-monitor-flow.spec.ts
```

### Page Objects (12 existing)
Layout, LoginPage, CarbonUploadPage, AuditListPage, MonitorPage, AuctionMarketPage, P2PTradePage, CreditScorePage, CarbonCoinPage, CarbonNeutralPage, AdminUsersPage, VerifyListPage

### Auth Helpers
- `loginViaApi(page, username, password)` — real API login with JWT
- `loginWithMonitor(page, role)` — login + AuthMonitor for session tracking
- `loginViaToken(page, role)` — mock token injection (for smoke tests)
- TEST_USERS: admin, enterprise001, reviewer001, thirdparty001 (password: admin123)

### Test Data Factories
- `createCarbonReport()`, `createAuctionOrder()`, `createTrade()`, `createCarbonNeutralProject()`, `createUser()`, `createCreditScore()`, `createCarbonCoinAccount()`

## What's Missing for v1.1.0 (Phase 12 Scope)

### New Page Objects Needed
- MarketPredictionPage (REQ-04)
- EnterpriseInferencePage (REQ-04)
- CarbonFormulaCalculatorPage (REQ-06)
- CertificateManagePage (REQ-07/08)
- BlockchainExplorerPage (REQ-05, enhanced from existing blockchain-flow)

### New Flow Tests Needed
1. **AI Prediction flows** (REQ-01~04): Market prediction API → frontend chart rendering, Enterprise inference API → frontend chart rendering
2. **Blockchain real integration** (REQ-05): Fabric write → read verification, chain explorer
3. **Carbon formula** (REQ-06): Power generation 25-param + Power grid 9-param calculation
4. **Certificate flows** (REQ-07/08): Admin issue/revoke admission, admin issue/revoke qualification, enterprise/reviewer view status
5. **Frontend coverage** (REQ-09~11): API module calls work, enterprise CRUD, reviewer audit operations

### Regression Tests
- Run existing v1.0 smoke + flow tests to verify no breakage
- Update any tests broken by Phase 11 fixes (e.g., status type changes from string to number)

## Test Roles & Routes
| Role | Home Route | Key Pages |
|------|-----------|-----------|
| ADMIN | /admin/system/users | UserManage, CertificateManage, Dashboard |
| ENTERPRISE | /enterprise/carbon/upload | CarbonUpload, CarbonNeutral, CompanyDashboard, MarketPrediction, CarbonFormula |
| REVIEWER | /auditor/audit/list | AuditList, ProjectReview, EnterpriseInference |
| THIRD_PARTY | /third-party/monitor | Monitor, BlockchainExplorer |

## Key Files (v1.1.0 additions)
- Frontend AI pages: `src/views/enterprise/MarketPrediction.vue`, `src/views/enterprise/EnterpriseInference.vue`
- Carbon formula: `src/views/enterprise/CarbonFormulaCalculator.vue`
- Certificate management: `src/views/admin/CertificateManage.vue`
- API modules (new): `src/api/enterprise.ts`, `src/api/reviewer.ts`
- AI API: `src/api/marketPrediction.ts`, `src/api/enterpriseInference.ts`, `src/api/carbonFormula.ts`
- Backend AI: `MarketPredictionController`, `EnterpriseInferenceController`, `EmissionController`
- Backend formulas: `PowerGenerationFormulaService`, `PowerGridFormulaService`
- Backend certificates: `EnterpriseAdmissionService`, `ReviewerQualificationService`
- Backend blockchain: `FabricBlockchainService` (replaces mock `BlockchainService`)
- ML service: `oaiss-chain-ml-service/` (Python FastAPI on port 8001)
