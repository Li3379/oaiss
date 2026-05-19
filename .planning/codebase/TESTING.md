# Testing Patterns

**Analysis Date:** 2026-05-18

## Test Framework

### Backend

**Runner:**
- JUnit 5 (via `spring-boot-starter-test`)
- Surefire 3.2.5 for unit tests (`*Test.java`)
- Failsafe 3.2.5 for integration tests (`*IntegrationTest.java`)

**Assertion Library:**
- JUnit 5 assertions (`org.junit.jupiter.api.Assertions.*`)
- MockMvc assertions for controller tests (`MockMvcResultMatchers.*`)

**Mocking:**
- Mockito via `@ExtendWith(MockitoExtension.class)` for service tests
- `@MockBean` for controller tests with Spring context
- OkHttp `mockwebserver` 4.12.0 for ML service contract tests

**Run Commands:**
```bash
# Unit tests only (Surefire)
cd oaiss-chain-backend && mvn test

# Integration tests only (Failsafe, requires Docker)
cd oaiss-chain-backend && mvn verify

# All tests (unit + integration)
cd oaiss-chain-backend && mvn verify

# Single test class
mvn test -Dtest=CarbonServiceTest

# Skip tests
mvn package -DskipTests
```

### Frontend

**Runner:**
- Vitest 4.1.5 with `happy-dom` 20.9.0 environment
- Config in `oaiss-chain-frontend/vite.config.js` under `test` key

**Assertion Library:**
- Vitest built-in (`expect`, `vi`)
- `@vue/test-utils` 2.4.10 for component mounting

**E2E:**
- Playwright 1.59.1, Chromium only
- Config in `oaiss-chain-frontend/playwright.config.ts`

**Run Commands:**
```bash
# Unit tests
cd oaiss-chain-frontend && npm run test              # vitest run (single run)
cd oaiss-chain-frontend && npm run test:watch         # vitest watch mode

# E2E tests
cd oaiss-chain-frontend && npm run test:e2e           # smoke tests
cd oaiss-chain-frontend && npm run test:e2e:flow      # flow tests (TEST_MODE=flow)
cd oaiss-chain-frontend && npm run test:e2e:headed    # headed mode
cd oaiss-chain-frontend && npm run test:e2e:debug     # debug mode

# v1.1 E2E tests
cd oaiss-chain-frontend && TEST_MODE=v1.1 npx playwright test
```

## Test File Organization

### Backend

**Location:**
- `src/test/java/com/oaiss/chain/` -- mirrors main source structure
- Test classes co-located by package: `service/`, `controller/`, `repository/`, `aop/`, `config/`, `exception/`, `dto/`, `security/`, `util/`, `entity/`

**Naming:**
- Unit tests: `*Test.java` -- `CarbonServiceTest.java`, `CarbonControllerTest.java`
- Integration tests: `*IntegrationTest.java` -- `UserIntegrationTest.java`
- Surefire includes `**/*Test.java` and `**/*Tests.java`, excludes `**/*IntegrationTest.java`
- Failsafe includes `**/*IntegrationTest.java`

**Structure:**
```
src/test/java/com/oaiss/chain/
├── BaseIntegrationTest.java          # Base class for integration tests
├── controller/
│   ├── CarbonControllerTest.java
│   ├── AuthControllerTest.java
│   ├── TradeControllerTest.java
│   └── ... (21 controller tests)
├── service/
│   ├── CarbonServiceTest.java
│   ├── AuthServiceTest.java
│   ├── TradeServiceTest.java
│   ├── ml/
│   │   ├── MlServiceClientContractTest.java
│   │   ├── EnterpriseInferenceServiceTest.java
│   │   └── MarketPredictionServiceTest.java
│   └── ... (29 service tests)
├── repository/
│   ├── CarbonReportRepositoryTest.java
│   └── ... (9 repository tests)
├── aop/
│   ├── AuditLogAspectTest.java
│   ├── DistributedLockAspectTest.java
│   └── ... (5 AOP tests)
├── exception/
│   ├── GlobalExceptionHandlerTest.java
│   └── ... (5 exception tests)
├── dto/
│   ├── PageResponseTest.java
│   └── ...
├── security/
│   ├── JwtTokenProviderTest.java
│   └── JwtAuthenticationFilterTest.java
├── config/
│   └── ...
├── entity/
│   └── AuthenticatorTest.java
└── util/
    ├── RsaKeyUtilTest.java
    └── CommonUtilsTest.java
```

### Frontend

**Location:**
- Unit tests: co-located `__tests__/` directories next to source files
- E2E tests: `tests/e2e/` at project root

**Naming:**
- Unit tests: `*.test.ts` -- `CarbonUpload.test.ts`, `Login.test.ts`
- E2E tests: `*.spec.ts` -- `enterprise.smoke.spec.ts`, `carbon-report-flow.spec.ts`

**Structure:**
```
src/
├── views/
│   └── __tests__/
│       ├── CarbonUpload.test.ts
│       ├── Login.test.ts
│       └── ... (24 view tests)
├── components/
│   └── __tests__/
│       └── ErrorBoundary.test.ts
└── utils/
    └── __tests__/
        └── auth.test.ts

tests/e2e/
├── smoke/                           # Smoke tests per role
│   ├── enterprise.smoke.spec.ts
│   ├── reviewer.smoke.spec.ts
│   ├── third-party.smoke.spec.ts
│   └── admin.smoke.spec.ts
├── flows/                           # End-to-end flow tests
│   ├── carbon-report-flow.spec.ts
│   ├── p2p-trade-flow.spec.ts
│   ├── auction-flow.spec.ts
│   ├── auth-flow.spec.ts
│   └── ... (15 flow tests)
├── v1.1/                            # Version-specific E2E tests
│   ├── certificate-flow.spec.ts
│   ├── regression-flow.spec.ts
│   └── ... (5 v1.1 tests)
├── fixtures/
│   ├── auth.ts                      # Login helpers (via token, via API)
│   ├── api-mock.ts                  # Mock API setup for E2E
│   ├── test-data.ts                 # Shared test data
│   ├── auth-monitor.ts              # Auth event monitoring
│   ├── cleanup.ts                   # Test cleanup utilities
│   ├── test-env.ts                  # Test environment config
│   ├── api-coverage.ts              # API coverage tracking
│   └── page-objects/                # Page Object Model classes
│       ├── Layout.ts
│       ├── LoginPage.ts
│       ├── CarbonUploadPage.ts
│       └── ... (12 page objects)
└── utils/
    └── coverage-report.ts
```

## Test Structure

### Backend Unit Tests (Service Layer)

**Suite Organization:**
```java
@ExtendWith(MockitoExtension.class)
class CarbonServiceTest {

    @Mock
    private CarbonReportRepository carbonReportRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @InjectMocks
    private CarbonService carbonService;

    private JwtUserDetails currentUser;
    private CarbonReport testReport;

    @BeforeEach
    void setUp() {
        // Build test data using builders
        currentUser = new JwtUserDetails();
        currentUser.setUserId(1L);

        testReport = CarbonReport.builder()
                .reportNo("CR20240101001")
                .enterpriseId(1L)
                .status(ReportStatusEnum.DRAFT.getCode())
                .build();
        testReport.setId(1L);
    }

    @Test
    @DisplayName("创建碳报告成功")
    void testCreateReportSuccess() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        // When
        CarbonReportResponse response = carbonService.createReport(currentUser, reportRequest);

        // Then
        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }
}
```

**Patterns:**
- Given/When/Then comments in test body
- `@DisplayName` with Chinese description on every test
- `@BeforeEach` for shared test data setup
- Mockito `@Mock` + `@InjectMocks` for dependency injection
- Builder pattern for entity and DTO construction in setup
- `verify()` to check interaction counts

### Backend Controller Tests

**Suite Organization:**
```java
@WebMvcTest(value = CarbonController.class,
        excludeAutoConfiguration = {
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CarbonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarbonService carbonService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Build test users
        enterpriseUser = JwtUserDetails.builder()
                .userId(1L)
                .username("enterprise_user")
                .roles(List.of("ENTERPRISE"))
                .build();

        // Set authentication context manually
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(enterpriseUser, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ENTERPRISE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityContextHolder.clearContext(); // or set as needed per test
    }

    @Test
    @DisplayName("创建碳报告成功测试")
    void testCreateReportSuccess() throws Exception {
        when(carbonService.createReport(any(), any())).thenReturn(reportResponse);

        mockMvc.perform(post("/carbon/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

**Patterns:**
- `@WebMvcTest` with JPA auto-configuration excluded
- `@AutoConfigureMockMvc(addFilters = false)` -- security filters disabled
- `@ActiveProfiles("test")` for H2 test database
- Manual `SecurityContextHolder` setup per test for auth context
- `@MockBean` for service dependencies
- JSON path assertions: `jsonPath("$.code").value(200)`, `jsonPath("$.data.id").value(1)`
- Validation failure tests send invalid DTOs and assert `status().isBadRequest()`

### Frontend Unit Tests

**Suite Organization:**
```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock API at module level before import
vi.mock('../../api/carbon', () => ({
  getMyReports: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  createReport: vi.fn(() => Promise.resolve()),
}))

// Mock Element Plus messaging
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import CarbonUpload from '../enterprise/CarbonUpload.vue'
import { getMyReports } from '../../api/carbon'

// Element Plus component stubs
const stubs = {
  'el-card': { template: '<div class="el-card"><slot /></div>' },
  'el-button': {
    template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
    props: ['type', 'loading'],
    emits: ['click'],
  },
  'el-table': { template: '<table><slot /></table>', props: ['data'] },
  // ... more stubs
}

function mountComponent() {
  return mount(CarbonUpload, {
    global: { plugins: [createPinia()], stubs },
  })
}

describe('CarbonUpload.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('页面加载时获取碳核算报告列表', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(getMyReports).toHaveBeenCalled()
    wrapper.unmount()
  })
})
```

**Patterns:**
- API mocks at module level with `vi.mock()` before component import
- Element Plus components stubbed with minimal templates
- `createPinia()` as global plugin for each mount
- `vi.clearAllMocks()` + `setActivePinia(createPinia())` in `beforeEach`
- `flushPromises()` to resolve async operations before assertions
- Manual `wrapper.unmount()` in each test
- Mock rejection: `getMyReports.mockRejectedValueOnce(new Error('network error'))`
- Mock resolution: `getMyReports.mockResolvedValueOnce({ items: [...], total: 1 })`

## Mocking

### Backend

**Framework:** Mockito via `MockitoExtension`

**Service Tests:**
- `@Mock` for repository and service dependencies
- `@InjectMocks` for the service under test
- `when(...).thenReturn(...)` for stubbing
- `when(...).thenThrow(...)` for error scenarios
- `doNothing().when(...)` for void methods
- `verify(mock, times(N)).method(...)` for interaction assertions
- `verify(mock, never()).method(...)` for negative assertions

**Controller Tests:**
- `@MockBean` for service layer
- `@MockBean` for `JwtTokenProvider` (required by security filter chain)
- Manual `SecurityContextHolder` setup for authentication
- No `@WithMockUser` -- manual auth setup preferred

**What to Mock:**
- All repository interfaces in service tests
- All service classes in controller tests
- `ObjectMapper` when JSON parsing is needed (or use real instance)

**What NOT to Mock:**
- The class under test (always real instance via `@InjectMocks`)
- DTOs and entities (use builders to create real instances)

### Frontend

**Framework:** Vitest built-in `vi`

**Patterns:**
- `vi.mock('module-path', () => ({ ... }))` for API module mocking
- `vi.fn(() => Promise.resolve(...))` for mock implementations
- `vi.clearAllMocks()` in `beforeEach`
- Element Plus stubs: lightweight template-only components with relevant props
- `mockRejectedValueOnce()` / `mockResolvedValueOnce()` for per-test behavior

**What to Mock:**
- All API calls (`src/api/*` modules)
- Element Plus messaging (`ElMessage`, `ElMessageBox`)
- Router (if needed)

**What NOT to Mock:**
- Pinia stores (use real `createPinia()` instances)
- Vue composition API functions

## Fixtures and Factories

### Backend

**Test Data:**
- Built inline using Lombok builders in `@BeforeEach`:
```java
testReport = CarbonReport.builder()
        .reportNo("CR20240101001")
        .enterpriseId(1L)
        .submitterId(1L)
        .accountingPeriod("2024-Q1")
        .status(ReportStatusEnum.DRAFT.getCode())
        .build();
testReport.setId(1L);
```

**Location:** No separate fixture files; test data constructed inline per test class

**Patterns:**
- Use builder pattern for entities and DTOs
- Call `.setId(1L)` after builder (id is in BaseEntity, not always on builder)
- Create multiple user types (enterprise, reviewer, admin) for role-based tests
- `JwtUserDetails.builder()` for authentication contexts

### Frontend

**Test Data:**
- Mock return values inline: `Promise.resolve({ items: [], total: 0 })`
- Element Plus stubs defined as constants at top of test file
- No separate fixture factory files for unit tests

**E2E Fixtures:**
- `tests/e2e/fixtures/auth.ts` -- login helpers, mock JWT tokens, test user credentials
- `tests/e2e/fixtures/api-mock.ts` -- route mock setup for API interception
- `tests/e2e/fixtures/test-data.ts` -- shared test data constants

## Coverage

### Backend

**Requirements:** 90% line coverage at package level (JaCoCo)

**JaCoCo Configuration** (in `pom.xml`):
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.90</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**View Coverage:**
```bash
cd oaiss-chain-backend && mvn test        # generates target/site/jacoco/index.html
```

**Coverage Scope:**
- 80+ unit test files across controller, service, repository, aop, config, exception, dto, security, entity, and util layers
- Tests exercise all major modules: carbon, trade, auction, auth, blockchain, credit score, digital signature, search, emission rating

### Frontend

**Requirements:** No enforced coverage target

**Coverage Tool:** No coverage plugin configured in Vitest

**Unit Test Scope:**
- 24 view component tests in `src/views/__tests__/`
- 1 shared component test: `src/components/__tests__/ErrorBoundary.test.ts`
- 1 utility test: `src/utils/__tests__/auth.test.ts`

## Test Types

### Unit Tests

**Backend:**
- Scope: Individual service methods, controller endpoints, repository queries, utility functions
- Approach: Mockito for isolation, MockMvc for HTTP layer
- Naming: `*Test.java` (Surefire)
- Test count: 80+ test classes

**Frontend:**
- Scope: Vue component rendering, API call behavior, utility functions
- Approach: `@vue/test-utils` mount with Element Plus stubs, API mocks via `vi.mock()`
- Naming: `*.test.ts` (Vitest)
- Test count: 26 test files

### Integration Tests

**Backend:**
- Scope: Full Spring context with real database and cache
- Approach: Testcontainers (MySQL 8.0 + Redis 7), `@SpringBootTest` with RANDOM_PORT
- Naming: `*IntegrationTest.java` (Failsafe)
- Base class: `BaseIntegrationTest.java` -- configures Testcontainers, dynamic properties, disables Flyway
- Currently `@Disabled` with note "Requires Docker for Testcontainers"
- Uses H2 in-memory for regular unit tests (`application-test.yml`)

**Frontend:**
- No integration tests; covered by E2E tests

### E2E Tests

**Framework:** Playwright 1.59.1

**Structure:**
- **Smoke tests** (`tests/e2e/smoke/`): Quick validation per role (4 files)
  - Each role: sidebar menu, user info, page loads, key components visible
- **Flow tests** (`tests/e2e/flows/`): Full user workflows (15 files)
  - Examples: carbon report submission, P2P trade, auction, auth, blockchain
- **v1.1 tests** (`tests/e2e/v1.1/`): Version-specific regression (5 files)

**Page Object Model** (`tests/e2e/fixtures/page-objects/`):
```typescript
import { type Page, expect } from '@playwright/test'

export class CarbonUploadPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/enterprise/carbon/upload')
    await this.page.waitForLoadState('networkidle')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page).toHaveURL(/\/enterprise\/carbon\/upload/)
    await expect(this.page.getByRole('button', { name: '创建项目' })).toBeVisible()
  }

  async expectTableHeaders(): Promise<void> {
    const headers = ['报告编号', '报告标题', '核算周期', '总排放量(tCO2e)', '状态', '审核人', '创建时间', '操作']
    for (const h of headers) {
      await expect(this.page.getByRole('columnheader', { name: h })).toBeVisible()
    }
  }
}
```

**12 page objects:** Layout, LoginPage, CarbonUploadPage, AuditListPage, MonitorPage, AuctionMarketPage, P2PTradePage, CreditScorePage, CarbonCoinPage, CarbonNeutralPage, AdminUsersPage, VerifyListPage, EnterpriseInferencePage, MarketPredictionPage, CertificateManagePage

**Auth fixtures** (`tests/e2e/fixtures/auth.ts`):
- `loginViaToken(page, role)` -- inject mock JWT via `page.addInitScript`
- `loginViaApi(page, username, password)` -- real API login with token storage
- `loginWithMonitor(page, role)` -- login + auth event monitoring
- Mock JWT tokens for all 4 roles (ENTERPRISE, ADMIN, REVIEWER, THIRD_PARTY)
- Test user credentials: `{username, password, role}` per role

## Common Patterns

### Async Testing (Backend)

```java
// Services use @Transactional -- tests don't need special async handling
// MockMvc is synchronous
@Test
void testAsyncOperation() throws Exception {
    mockMvc.perform(post("/endpoint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

### Async Testing (Frontend)

```typescript
it('handles async data loading', async () => {
  getMyReports.mockResolvedValueOnce({ items: [...], total: 1 })
  const wrapper = mountComponent()
  await flushPromises()                    // wait for promises to resolve
  expect(getMyReports).toHaveBeenCalled()
  wrapper.unmount()
})
```

### Error Testing (Backend)

```java
@Test
@DisplayName("创建碳报告失败-企业不存在")
void testCreateReportFailEnterpriseNotFound() {
    // Given
    when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(CarbonException.class,
        () -> carbonService.createReport(currentUser, reportRequest));
    verify(carbonReportRepository, never()).save(any());
}
```

### Error Testing (Frontend)

```typescript
it('获取报告失败显示错误消息', async () => {
  getMyReports.mockRejectedValueOnce(new Error('network error'))
  const wrapper = mountComponent()
  await flushPromises()
  expect(ElMessage.error).toHaveBeenCalled()
  wrapper.unmount()
})
```

### Validation Testing (Backend Controller)

```java
@Test
@DisplayName("创建碳报告失败-参数验证失败-标题为空")
void testCreateReportValidationFailTitleEmpty() throws Exception {
    CarbonReportRequest invalidRequest = CarbonReportRequest.builder()
            .title("")     // blank title triggers @NotBlank
            .build();

    mockMvc.perform(post("/carbon/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

    verify(carbonService, never()).createReport(any(), any());
}
```

### Role-Based Testing (Backend Controller)

```java
@Test
@DisplayName("创建碳报告失败-无权限")
void testCreateReportForbidden() throws Exception {
    // Set up reviewer context (wrong role for this endpoint)
    UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(reviewerUser, null,
                    List.of(new SimpleGrantedAuthority("ROLE_REVIEWER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(carbonService.createReport(any(), any()))
            .thenThrow(new BusinessException(403, "无权限创建报告"));

    mockMvc.perform(post("/carbon/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reportRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(403));
}
```

## Test Profiles and Configuration

### Backend Test Profile

`src/main/resources/application-test.yml`:
- H2 in-memory database: `jdbc:h2:mem:testdb`
- `spring.jpa.hibernate.ddl-auto: create-drop` (schema auto-created, dropped after)
- `spring.flyway.enabled: false` (not in test yml; disabled by BaseIntegrationTest)
- `logging.level.com.oaiss.chain: DEBUG`
- Hardcoded JWT secret for testing

### Backend Surefire Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>   <!-- fresh JVM per test class -->
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Frontend Vitest Configuration

In `oaiss-chain-frontend/vite.config.js`:
```javascript
test: {
    environment: 'happy-dom',
    globals: true,
    exclude: ['tests/e2e/**', 'node_modules/**'],
}
```

### Playwright Configuration

In `oaiss-chain-frontend/playwright.config.ts`:
- Three test directories via `TEST_MODE` env var:
  - Default: `./tests/e2e/smoke` (15s timeout)
  - `TEST_MODE=flow`: `./tests/e2e/flows` (30s timeout)
  - `TEST_MODE=v1.1`: `./tests/e2e/v1.1` (45s timeout)
- Chromium only, single worker (`workers: 1`)
- Auto-starts Vite dev server for smoke tests
- `trace: 'on-first-retry'`, `screenshot: 'only-on-failure'`

## CI/CD Testing

### Pipeline

**CI Workflow:** `.github/workflows/e2e-tests.yml`

**Triggers:** Pull requests touching backend, frontend, or workflow files; manual dispatch

**Steps:**
1. MySQL 8.0 + Redis 7 services (GitHub Actions services)
2. Node.js 20 + Java 17 setup
3. Maven dependency cache + npm dependency cache
4. `npm ci` for frontend dependencies
5. `mvn clean package -DskipTests` for backend build
6. Backend started as background process
7. Health check wait (120s timeout via `actuator/health`)
8. Playwright chromium install
9. `npm run test:e2e` (smoke tests)
10. `TEST_MODE=v1.1 npx playwright test` (v1.1 tests)
11. Upload Playwright report artifact (7-day retention)

**No backend unit test CI step detected** -- only E2E tests run in CI.

---

*Testing analysis: 2026-05-18*
