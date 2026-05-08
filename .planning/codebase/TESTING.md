# Testing Conventions

> Last updated: 2026-05-08

## 1. Overview

| Layer | Framework | Runner | Coverage Tool |
|-------|----------|--------|--------------|
| Backend unit | JUnit 5 + Mockito | Maven Surefire | JaCoCo (90% line) |
| Backend integration | JUnit 5 + Testcontainers | Maven Failsafe | JaCoCo |
| Frontend unit | Vitest + happy-dom + Vue Test Utils | Vitest CLI | Vitest coverage |
| Frontend E2E | Playwright (Chromium) | Playwright CLI | Built-in traces |

## 2. Backend Unit Tests

### 2.1 File Convention

- Location: `src/test/java/com/oaiss/chain/`
- Mirror main source package structure
- Naming: `*Test.java` (e.g., `UserControllerTest.java`)
- Framework: `@WebMvcTest` for controllers, plain JUnit for services/utilities

### 2.2 Controller Test Pattern

```java
@WebMvcTest(value = UserController.class,
        excludeAutoConfiguration = {
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private JwtUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUserDetails = JwtUserDetails.builder()
                .userId(1L).username("testuser").userType(1)
                .roles(List.of("ENTERPRISE")).enterpriseId(1L)
                .enabled(true).accountNonExpired(true)
                .accountNonLocked(true).credentialsNonExpired(true)
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("获取当前用户信息成功测试")
    void testGetProfileSuccess() throws Exception {
        // Given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(testUserDetails, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ENTERPRISE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userService.getCurrentUserInfo(any())).thenReturn(userInfoResponse);

        // When & Then
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(userService, times(1)).getCurrentUserInfo(any());
    }
}
```

Key patterns:
- `@WebMvcTest` slices to controller layer only
- `excludeAutoConfiguration` removes JPA/Hibernate from context
- `@AutoConfigureMockMvc(addFilters = false)` disables security filters
- `@MockBean` for all service dependencies
- Manual `SecurityContextHolder` setup for authenticated tests
- Given/When/Then structure with `@DisplayName` in Chinese
- `jsonPath()` assertions on the `ApiResponse` envelope
- `verify()` to confirm service method invocation

### 2.3 Test Structure

Tests use `// ==================== Section ====================` separators for grouping related test methods. Each test method:
1. Sets up authentication context (if needed)
2. Configures mock behavior
3. Performs the action
4. Asserts response status, JSON structure, and service invocation

### 2.4 Mocking Strategy

- **Services**: `@MockBean` (Spring's Mockito integration)
- **Security**: Manual `SecurityContextHolder` setup with `UsernamePasswordAuthenticationToken`
- **Request bodies**: Serialized via `ObjectMapper`
- **Verification**: `verify(service, times(N))` and `verify(service, never())`
- **Exception testing**: `doThrow(new BusinessException(...)).when(service).method()`

### 2.5 Validation Tests

Controller tests include validation failure scenarios:
```java
@Test
@DisplayName("参数验证失败-真实姓名太长")
void testUpdateProfileValidationFail() throws Exception {
    String longName = "a".repeat(51);
    UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
            .realName(longName).build();

    mockMvc.perform(put("/user/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

    verify(userService, never()).updateProfile(any(), any());
}
```

## 3. Backend Integration Tests

### 3.1 Base Class

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    protected static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("oaiss_chain_test")
                    .withUsername("test").withPassword("test");

    @Container
    protected static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
```

### 3.2 Integration Test Pattern

```java
@Disabled("Requires Docker for Testcontainers. Enable when Docker is available.")
class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("用户注册集成测试")
    class RegisterIntegrationTests {
        @Test
        @DisplayName("企业用户注册成功")
        void registerEnterpriseUser_Success() {
            // Arrange
            RegisterRequest request = RegisterRequest.builder()...build();
            // Act
            var response = authService.register(request);
            // Assert
            assertNotNull(response);
            User saved = userRepository.findByUsername("test_enterprise").orElse(null);
            assertNotNull(saved);
        }
    }
}
```

Key patterns:
- Extend `BaseIntegrationTest` for Testcontainers setup
- `@Disabled` annotation (Docker dependency)
- `@Nested` inner classes for grouping related tests
- Arrange/Act/Assert structure
- Real database assertions via repositories
- `@BeforeEach` cleanup with `deleteAll()`

## 4. Frontend Unit Tests

### 4.1 File Convention

- Location: `src/views/__tests__/`, `src/components/__tests__/`, `src/utils/__tests__/`
- Naming: `ComponentName.test.ts`
- Framework: Vitest + `@vue/test-utils` + happy-dom

### 4.2 Component Test Pattern

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

// Mock API modules
vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

// Mock vue-router
const mockRouterReplace = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ query: {} })),
  useRouter: vi.fn(() => ({ replace: mockRouterReplace })),
}))

// Mock Element Plus
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  }
})

// Define stubs for Element Plus components
const stubGlobal = {
  stubs: {
    'el-card': { template: '<div class="el-card"><slot /></div>' },
    'el-form': {
      template: '<form @submit.prevent><slot /></form>',
      methods: { validate() { return Promise.resolve(true) } },
    },
    'el-input': {
      template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
      props: ['modelValue', 'type', 'placeholder'],
      emits: ['update:modelValue'],
    },
    'el-button': {
      template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
      props: ['type', 'size', 'loading'],
      emits: ['click'],
    },
    // ... more stubs
  },
}

function mountComponent() {
  return mount(Login, { global: stubGlobal })
}

async function flush() {
  await nextTick()
  await nextTick()
  await nextTick()
}

describe('Login.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('calls login API with form data', async () => {
    login.mockResolvedValue({ accessToken: 'token', refreshToken: 'refresh' })
    const wrapper = mountComponent()
    await flush()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('testuser')
    await inputs[1].setValue('password123')
    await inputs[2].setValue('ABC1')

    await wrapper.find('button').trigger('click')
    await flush()

    expect(login).toHaveBeenCalledWith({
      username: 'testuser',
      password: 'password123',
      captchaKey: 'test-key-123',
      captcha: 'ABC1',
    })
  })
})
```

### 4.3 Mocking Strategy

- **API modules**: `vi.mock('../../api/module', () => ({ fn: vi.fn() }))`
- **Vue Router**: `vi.mock('vue-router', ...)` with mock `useRoute`/`useRouter`
- **Element Plus**: Partial mock preserving original exports, replacing `ElMessage` with spies
- **Component stubs**: Manual HTML stubs for Element Plus components (not using `shallowMount`)
- **localStorage/sessionStorage**: Direct manipulation in `beforeEach` cleanup

### 4.4 Test Helpers

- `flush()` -- multiple `nextTick()` calls to settle async Vue updates
- `mountComponent()` -- factory function with pre-configured stubs
- `beforeEach` resets Pinia, clears localStorage, clears all mocks

### 4.5 Test Categories

Each component test file covers:
1. **Rendering** -- verifies form fields, buttons, and labels exist
2. **Success flows** -- API calls, success messages, navigation
3. **Failure flows** -- error handling, captcha refresh, input clearing
4. **Edge cases** -- validation failures, empty inputs, localStorage persistence

## 5. Frontend E2E Tests

### 5.1 Configuration

```typescript
// playwright.config.ts
export default defineConfig({
  testDir: isFlowMode ? './tests/e2e/flows' : './tests/e2e/smoke',
  timeout: isFlowMode ? 30000 : 15000,
  workers: 1,
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
})
```

### 5.2 Test Structure

```
tests/e2e/
  fixtures/       # Shared fixtures (auth, page objects, test data, API mocks)
    auth.ts       # Authentication helpers
    auth-monitor.ts
    auth-resilient.ts
    api-mock.ts   # API response mocking
    test-data.ts  # Test data constants
    page-objects/ # Page object models
  smoke/          # Smoke tests (fast, essential flows)
  flows/          # Full flow tests (comprehensive, slower)
```

### 5.3 Test Files

Eight E2E spec files covering key user journeys:
- `d1-login.spec.js` -- Login flow
- `d2-carbon-upload.spec.js` -- Carbon data upload
- `d3-auction-market.spec.js` -- Double auction market
- `d4-p2p-trade.spec.js` -- P2P trading
- `d5-admin-users.spec.js` -- Admin user management
- `d6-third-party-monitor.spec.js` -- Third-party monitoring
- `d7-carbon-neutral.spec.js` -- Carbon neutral projects
- `d8-credit-score.spec.js` -- Credit scoring

### 5.4 Run Modes

- `npm run test:e2e` -- Smoke tests only (fast, 15s timeout)
- `npm run test:e2e:flow` -- Full flow tests (comprehensive, 30s timeout)
- `npm run test:e2e:headed` -- Visible browser
- `npm run test:e2e:debug` -- Debug mode

## 6. Coverage Requirements

### 6.1 Backend

- **Target**: 90% line coverage per package (JaCoCo enforcement)
- **Scope**: All packages under `com.oaiss.chain`
- **Enforcement**: `jacoco-check` goal in Maven build
- **Report**: `mvn test` generates HTML report at `target/site/jacoco/index.html`

### 6.2 Frontend

- **Unit tests**: No explicit coverage threshold configured (Vitest default)
- **E2E**: Coverage via Playwright traces and screenshots on failure

## 7. Test Commands

```bash
# Backend
cd oaiss-chain-backend
mvn test                    # Unit tests (Surefire)
mvn verify                  # Unit + integration tests (Failsafe)
mvn jacoco:report           # Generate coverage report

# Frontend
cd oaiss-chain-frontend
npm run test                # Unit tests (Vitest)
npm run test:watch          # Watch mode
npm run test:e2e            # E2E smoke tests
npm run test:e2e:flow       # E2E flow tests
```

## 8. Test Data Patterns

### 8.1 Backend

- Builder pattern for test fixtures: `User.builder().username("testuser")...build()`
- `@BeforeEach` cleanup: `userRepository.deleteAll()`
- Static test constants defined in test class fields

### 8.2 Frontend

- Mock return values set per test: `login.mockResolvedValue({...})`
- `localStorage.clear()` in `beforeEach`
- Test data in `tests/e2e/fixtures/test-data.ts`
- Page objects in `tests/e2e/fixtures/page-objects/`
