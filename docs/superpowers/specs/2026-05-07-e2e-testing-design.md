# E2E Testing Design: Full-Role, Full-Module Coverage

> Date: 2026-05-07
> Status: Approved

## 1. Overview

Comprehensive end-to-end testing strategy for OAISS CHAIN covering all 5 roles and all 16 business modules. Combines Playwright frontend E2E tests with backend Testcontainers integration tests.

### Requirements

- **Scope**: Full-chain business flows + page-level smoke tests
- **API Strategy**: Hybrid (Mock for smoke, real API for flows)
- **Loop**: Per-scenario iterative (write one → run → fix → next)
- **Coverage**: Frontend Playwright E2E + backend Testcontainers integration tests

### Test Accounts

| Username | Role | Password |
|----------|------|----------|
| admin | ADMIN | admin123 |
| enterprise001 | ENTERPRISE | admin123 |
| reviewer001 | REVIEWER | admin123 |
| thirdparty001 | THIRD_PARTY | admin123 |
| authenticator001 | AUTHENTICATOR | admin123 |

## 2. Architecture

### Approach: Role-Layered (Approach A)

```
oaiss-chain-frontend/tests/e2e/
├── fixtures/
│   ├── auth.ts                 # Auth helpers (token injection + real API login)
│   ├── api-mock.ts             # Mock route factories
│   ├── test-data.ts            # Test data factories (ApiResponse<T> format)
│   └── page-objects/
│       ├── LoginPage.ts
│       ├── CarbonUploadPage.ts
│       ├── AuctionMarketPage.ts
│       ├── P2PTradePage.ts
│       ├── CarbonNeutralPage.ts
│       ├── CreditScorePage.ts
│       ├── CarbonCoinPage.ts
│       ├── AdminUsersPage.ts
│       ├── MonitorPage.ts
│       ├── VerifyListPage.ts
│       ├── AuditListPage.ts
│       └── Layout.ts
├── smoke/                      # Smoke tests (Mock mode)
│   ├── enterprise.smoke.spec.ts
│   ├── admin.smoke.spec.ts
│   ├── reviewer.smoke.spec.ts
│   ├── authenticator.smoke.spec.ts
│   └── third-party.smoke.spec.ts
└── flows/                      # Business flow tests (real API mode)
    ├── auth-flow.spec.ts
    ├── carbon-report-flow.spec.ts
    ├── auction-flow.spec.ts
    ├── p2p-trade-flow.spec.ts
    ├── carbon-neutral-flow.spec.ts
    ├── credit-score-flow.spec.ts
    ├── carbon-coin-flow.spec.ts
    ├── admin-manage-flow.spec.ts
    ├── third-party-monitor-flow.spec.ts
    ├── blockchain-flow.spec.ts
    ├── digital-signature-flow.spec.ts
    └── file-management-flow.spec.ts

oaiss-chain-backend/src/test/java/com/oaiss/chain/
├── BaseIntegrationTest.java
├── integration/
│   ├── AuthIntegrationTest.java
│   ├── CarbonReportIntegrationTest.java
│   ├── AuctionIntegrationTest.java
│   ├── TradeIntegrationTest.java
│   ├── CarbonNeutralIntegrationTest.java
│   ├── CreditScoreIntegrationTest.java
│   └── CarbonCoinIntegrationTest.java
└── data/
    └── seed-test-data.sql
```

## 3. Fixtures Layer

### auth.ts (upgrade from auth.js)

- `loginViaApi(page, username, password)` — real API login, returns JWT
- `loginViaToken(page, role)` — token injection (mock mode)
- `getToken(role)` — get mock token by role
- 5 role token constants: ENTERPRISE / REVIEWER / AUTHENTICATOR / THIRD_PARTY / ADMIN

### api-mock.ts

- `setupSmokeMock(page, role)` — mock all APIs for a role's pages
- `mockApiResponse(page, method, path, data)` — single route mock
- `mockPaginatedList(page, path, items, total)` — paginated list mock
- Standard response templates per module

### test-data.ts

- `createCarbonReport(overrides)` — carbon report data
- `createAuctionOrder(overrides)` — auction order data
- `createTrade(overrides)` — P2P trade data
- `createCarbonNeutralProject(overrides)` — carbon neutral project
- `createUser(overrides)` — user data
- All factories return `ApiResponse<T>` format

### Page Object Model

Each page object encapsulates:
- `goto()` — navigate to page
- `expectLoaded()` — assert page loaded (breadcrumb, key elements)
- `expectSidebar(role)` — assert sidebar menu
- Business action methods (e.g. `submitReport()`, `createOrder()`)

## 4. Smoke Tests (Mock Mode)

### Enterprise (12 pages)

| Page | Route | Verification |
|------|-------|-------------|
| 碳核算-上传审核 | /enterprise/carbon/upload | Form, table headers, create button |
| P2P订单管理 | /enterprise/orders/manage | Order list, status filter |
| 双向拍卖 | /enterprise/trading/market | Tab switching, order form |
| P2P交易 | /enterprise/trading/p2p | Trade list, initiate button |
| 数据可视化 | /enterprise/company/dashboard | ECharts containers, data cards |
| 信誉评分 | /enterprise/credit/score | Score display, history table |
| 碳币账户 | /enterprise/carbon-coin/account | Balance, transactions, buttons |
| 区块链浏览器 | /enterprise/blockchain/browser | Block list, transaction list |
| 碳中和项目 | /enterprise/carbon-neutral/projects | Project list, search, create |
| 碳中和详情 | /enterprise/carbon-neutral/projects/:id | Project info, status buttons |
| 排放数据 | /enterprise/emission/data | Ratings, rankings |
| 个人中心 | /enterprise/user/profile | Profile form, password change |

### Admin (4 pages)

| Page | Route | Verification |
|------|-------|-------------|
| 用户管理 | /admin/system/users | User table, status filter, actions |
| 碳核算管理 | /admin/system/carbon | Report management list |
| 系统配置 | /admin/system/config | Config form |
| 统计数据 | /admin/data/statistics | Statistics charts |

### Reviewer (1 page)

- 碳排放数据 /auditor/audit/list — Audit list, review action buttons

### Authenticator (1 page)

- 认证列表 /authenticator/verify/list — Certification list, certify actions

### Third Party (1 page)

- 监管面板 /third-party/monitor — Org info, carbon reports, statistics

### Smoke spec structure

```js
test.describe('Role: Enterprise - Smoke', () => {
  // shared beforeEach: setupSmokeMock → loginViaToken → navigate

  test('sidebar menu complete', ...)
  test('user info displayed (role label, username)', ...)

  test.describe('Carbon Upload', () => {
    test('page route correct', ...)
    test('breadcrumb correct', ...)
    test('search form exists', ...)
    test('table headers correct', ...)
    test('empty state shown', ...)
    test('key action buttons exist', ...)
  })
})
```

## 5. Business Flow Tests (Real API Mode)

### 5.1 auth-flow.spec.ts

```
Register enterprise → Login get JWT → Access protected page
→ Token refresh → Change password → Logout
```

### 5.2 carbon-report-flow.spec.ts

```
Enterprise: Create report (DRAFT) → Upload attachment → Submit (SUBMITTED)
Reviewer: View pending list → Approve (APPROVED) → On-chain (ON_CHAIN)
Enterprise: View on-chain report → Delete draft (should fail, non-DRAFT)
```

### 5.3 auction-flow.spec.ts

```
Enterprise A: Create buy order
Enterprise B: Create sell order
Admin: Execute match → Generate matching results
Enterprise A: View my orders → View matching results
Enterprise B: View my orders → Confirm settlement
```

### 5.4 p2p-trade-flow.spec.ts

```
Enterprise A: Initiate P2P trade
Enterprise B: View trade → Confirm trade
Enterprise A: View completed trade
Enterprise C: Try confirm others' trade (should fail)
Enterprise A: Initiate another trade → Cancel
```

### 5.5 carbon-neutral-flow.spec.ts

```
Enterprise: Create project → Submit for review
Reviewer: Approve → Start execution
Enterprise: Submit verification materials
Authenticator: Verify → Apply certification
Admin: Issue certification
Enterprise: Use credits → Update monitoring → Terminate project
```

### 5.6 credit-score-flow.spec.ts

```
Admin: View enterprise score → Deduct (data falsification -20) → View downgrade
Enterprise: View own score → View deduction history → Verify trade restricted
Admin: Bonus (good behavior +5) → Evaluate level
Enterprise: Verify restriction lifted
```

### 5.7 carbon-coin-flow.spec.ts

```
Enterprise A: View balance → Recharge
Enterprise A: Transfer to Enterprise B
Enterprise B: View balance change → View transaction history
Admin: View all carbon coin transactions
```

### 5.8 admin-manage-flow.spec.ts

```
Admin: View user list → Filter → Disable user
Disabled user: Try login (should fail)
Admin: Enable user → View dashboard → View statistics
```

### 5.9 third-party-monitor-flow.spec.ts

```
Third Party: View org info → View carbon reports → View statistics → Update contact
Third Party: Try access enterprise page (should be denied)
```

### 5.10 blockchain-flow.spec.ts

```
Enterprise: View blockchain status → Browse latest blocks → View transaction details
Authenticator: View block details
Third Party: View transaction details
Unauthorized role: Try access block details (should be denied)
```

### 5.11 digital-signature-flow.spec.ts

```
Enterprise: Generate keypair → View public key → Sign data → Verify signature → Encrypt → Decrypt
Reviewer: Verify enterprise signature
Enterprise: Delete keypair
```

### 5.12 file-management-flow.spec.ts

```
Enterprise: Upload single → Upload batch → View file info → Download → Delete
Enterprise: Get presigned URL → Upload via presigned URL
```

### Flow spec structure

```js
test.describe('Flow: Carbon Report Lifecycle', () => {
  // No mock, real backend API

  test.describe('Enterprise Actions', () => {
    test('create carbon report', async ({ page }) => { ... })
    test('upload attachment', async ({ page }) => { ... })
    test('submit report', async ({ page }) => { ... })
  })

  test.describe('Reviewer Actions', () => {
    test('view pending list', async ({ page }) => { ... })
    test('approve report', async ({ page }) => { ... })
  })

  test.describe('Permission Checks', () => {
    test('enterprise cannot review', async ({ page }) => { ... })
    test('reviewer cannot create', async ({ page }) => { ... })
  })
})
```

## 6. Backend Integration Tests

### BaseIntegrationTest

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("oaiss_chain_test")
        .withInitScript("db/migration/V1__init_schema.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // Helper methods: loginAs(), json(), get(), post(), put(), delete()
}
```

### 7 Integration Test Classes

**AuthIntegrationTest**: register, login, refresh, logout, password change, disabled user login

**CarbonReportIntegrationTest**: create draft, submit, review approve/reject, delete draft vs non-draft, on-chain, pagination, search

**AuctionIntegrationTest**: buy/sell orders, match, cancel, partial/full match, settlement

**TradeIntegrationTest**: P2P create, confirm, cancel, permission checks, status transitions

**CarbonNeutralIntegrationTest**: full lifecycle (DRAFT→SUBMITTED→APPROVED→IN_PROGRESS→VERIFIED→CERTIFIED→TERMINATED), search, my projects

**CreditScoreIntegrationTest**: score query, deduct, bonus, level transitions, restricted/frozen lists

**CarbonCoinIntegrationTest**: account, recharge, transfer, insufficient balance, transactions

### Seed Data

Each test class uses `@Transactional` for automatic rollback. Seed data includes:
- 5 test users (admin, enterprise001, reviewer001, thirdparty001, authenticator001)
- Enterprise records
- Pre-existing carbon reports in various states
- Carbon coin accounts

## 7. Playwright Configuration

### Dual-mode config

```js
// playwright.config.js
const isFlowMode = process.env.TEST_MODE === 'flow'

export default defineConfig({
  testDir: isFlowMode ? './tests/e2e/flows' : './tests/e2e/smoke',
  timeout: isFlowMode ? 30000 : 15000,
  use: {
    video: isFlowMode ? 'on-first-retry' : 'off',
    // ... other settings
  },
})
```

### Run Commands

```bash
# Smoke tests (Mock, fast, no backend needed)
npx playwright test

# Flow tests (real API, needs docker-compose up)
TEST_MODE=flow npx playwright test

# Single scenario debug
npx playwright test tests/e2e/flows/carbon-report-flow.spec.ts --headed

# Backend integration tests
cd oaiss-chain-backend && mvn verify
```

### Development Loop

```
1. Write one flow spec (e.g. carbon-report-flow)
2. TEST_MODE=flow npx playwright test ...
3. Fail? → Fix spec or frontend code
4. Pass? → Write corresponding backend integration test
5. mvn verify → Fail? → Fix backend code
6. Both pass → Next scenario
```

## 8. Implementation Phases

### Phase 1: Infrastructure (Round 1)

- Upgrade fixtures/auth.ts
- Create fixtures/api-mock.ts
- Create fixtures/test-data.ts
- Create Layout Page Object
- Upgrade playwright.config.js
- Upgrade BaseIntegrationTest
- Seed data SQL

### Phase 2: Smoke Tests (Round 2)

- enterprise.smoke.spec.ts (12 pages)
- admin.smoke.spec.ts (4 pages)
- reviewer.smoke.spec.ts (1 page)
- authenticator.smoke.spec.ts (1 page)
- third-party.smoke.spec.ts (1 page)

### Phase 3: Core Business Flows (Rounds 3-8)

| Round | Frontend Flow | Backend Integration |
|-------|--------------|-------------------|
| 3 | auth-flow | AuthIntegrationTest |
| 4 | carbon-report-flow | CarbonReportIntegrationTest |
| 5 | auction-flow | AuctionIntegrationTest |
| 6 | p2p-trade-flow | TradeIntegrationTest |
| 7 | carbon-neutral-flow | CarbonNeutralIntegrationTest |
| 8 | credit-score-flow | CreditScoreIntegrationTest |

### Phase 4: Extended Business Flows (Rounds 9-12)

| Round | Frontend Flow | Backend Integration |
|-------|--------------|-------------------|
| 9 | carbon-coin-flow | CarbonCoinIntegrationTest |
| 10 | admin-manage-flow | (reuse AuthIntegrationTest) |
| 11 | third-party-monitor-flow | — |
| 12 | blockchain-flow / digital-signature-flow / file-management-flow | — |

## 9. Coverage Matrix

| Module | ENTERPRISE | REVIEWER | AUTHENTICATOR | THIRD_PARTY | ADMIN |
|--------|:---:|:---:|:---:|:---:|:---:|
| Auth | smoke+flow | smoke | smoke | smoke | smoke+flow |
| Carbon Report | smoke+flow | smoke+flow | smoke | smoke | smoke |
| Auction | smoke+flow | smoke | — | smoke | smoke+flow |
| P2P Trade | smoke+flow | — | — | — | smoke |
| Carbon Neutral | smoke+flow | smoke+flow | smoke+flow | — | smoke+flow |
| Credit Score | smoke+flow | smoke+flow | — | — | smoke+flow |
| Carbon Coin | smoke+flow | — | — | — | smoke |
| Blockchain | smoke+flow | — | smoke | smoke | — |
| Digital Signature | smoke+flow | smoke | — | — | — |
| File Management | smoke | — | — | — | — |
| Emission Data | smoke | — | — | — | — |
| Third Party | — | — | — | smoke+flow | — |
| Admin | — | — | — | — | smoke+flow |

## 10. Per-Round Completion Criteria

```
✓ All frontend flow tests pass
✓ All backend integration tests pass
✓ No console.error / no flaky tests
✓ Commit submitted
```
