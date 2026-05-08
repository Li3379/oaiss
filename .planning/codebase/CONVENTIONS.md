# Codebase Conventions

> Last updated: 2026-05-08

## 1. Project Layout

```
oaiss-chain-backend/
  src/main/java/com/oaiss/chain/
    controller/     # REST controllers (16 files)
    service/        # Business logic (19 files)
    repository/     # Spring Data JPA interfaces
    entity/         # JPA entities extending BaseEntity
    dto/            # Request/response DTOs
    config/         # Spring configuration classes
    annotation/     # Custom annotations (AuditLog, RateLimit, etc.)
    aop/            # AOP aspects for cross-cutting concerns
    security/       # JWT filter, entry point, UserDetails
    enums/          # Enumerations
    constant/       # ErrorCode, ErrorMessage
    exception/      # Custom exceptions + GlobalExceptionHandler
    util/           # HashUtil, RsaKeyUtil, CommonUtils

oaiss-chain-frontend/
  src/
    api/            # Axios API modules (16 files, one per domain)
    types/          # TypeScript type definitions (barrel index.ts)
    views/          # Page components organized by role
    store/          # Pinia store (single index.ts)
    router/         # Vue Router with role-based guards
    i18n/           # Internationalization (zh-CN, en-US)
    components/     # Shared components
    layout/         # App layout shell
    config/         # Menu and image config
    utils/          # Auth utilities, ECharts helpers
```

## 2. Backend Conventions

### 2.1 Java Style

- **Language level**: Java 17 (records, sealed classes, text blocks available but not observed in use)
- **Lombok everywhere**: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Slf4j`
- **No manual getters/setters/constructors** -- rely on Lombok
- **Javadoc**: Bilingual (Chinese + English) on classes and public methods; `@author OAISS Team` tag standard

### 2.2 Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `UserService`, `BusinessException` |
| Methods | camelCase | `getCurrentUserInfo()`, `findByIdAndDeletedFalse()` |
| Constants | UPPER_SNAKE_CASE | `SYSTEM_ERROR`, `INSUFFICIENT_BALANCE` |
| DTOs | PascalCase + suffix | `LoginRequest`, `UserInfoResponse`, `PageResponse` |
| Entities | PascalCase (singular) | `User`, `CarbonReport`, `AuctionOrder` |
| Repositories | Entity + `Repository` | `UserRepository`, `EnterpriseRepository` |
| Controllers | Domain + `Controller` | `UserController`, `TradeController` |
| Services | Domain + `Service` | `UserService`, `BlockchainService` |
| Enums | PascalCase + `Enum` | `UserTypeEnum`, `TradeStatusEnum` |
| Tables | snake_case | `created_at`, `is_deleted`, `user_type` |
| DB columns | snake_case via `@Column(name = ...)` | `@Column(name = "created_at")` |

### 2.3 Error Handling

**Exception hierarchy:**
```
RuntimeException
  BusinessException          -- base (code + message)
    AuthenticationException  -- auth failures (2xxx)
    AuthorizationException   -- permission denied (2xxx)
    CarbonException          -- carbon module (3xxx)
    TradeException           -- trade module (4xxx)
    BlockchainException      -- blockchain module (5xxx)
```

**Error codes** (`ErrorCode.java`): Integer constants with module prefixes:
- 1xxx: Common errors
- 2xxx: Auth/authorization
- 3xxx: Carbon accounting
- 4xxx: Carbon trading
- 5xxx: Blockchain

**Error messages** (`ErrorMessage.java`): Matching `String` constants for each error code.

**Global handler** (`GlobalExceptionHandler.java`): `@RestControllerAdvice` catches all exceptions, returns `ApiResponse<Void>` with appropriate HTTP status. Maps error code ranges to HTTP status codes.

**Throwing pattern:**
```java
throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
// or static factory:
throw BusinessException.notFound("User not found");
```

### 2.4 API Response Envelope

All endpoints return `ApiResponse<T>`:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "meta": {
    "requestId": "req_...",
    "timestamp": "2026-05-08T...",
    "pagination": { "page": 0, "size": 10, "total": 100, "totalPages": 10 }
  }
}
```

Factory methods: `ApiResponse.success()`, `ApiResponse.success(data)`, `ApiResponse.error(code, message)`.

### 2.5 Entity Pattern

All entities extend `BaseEntity`:
- `id` (Long, `@GeneratedValue(IDENTITY)`)
- `createdAt` (`@CreatedDate`)
- `updatedAt` (`@LastModifiedDate`)
- `deleted` (Boolean, soft delete flag)

Soft delete is used throughout -- repositories filter with `findByXxxAndDeletedFalse()`.

### 2.6 Controller Pattern

```java
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "02. 用户中心", description = "...")
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "...", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ApiResponse<UserInfoResponse> getProfile(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        return ApiResponse.success(userService.getCurrentUserInfo(currentUser));
    }
}
```

Key patterns:
- Constructor injection via `@RequiredArgsConstructor`
- `@AuthenticationPrincipal JwtUserDetails` for current user
- `@Valid @RequestBody` for request validation
- OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponses`)
- Return `ApiResponse.success(data)` or `ApiResponse.success()` (no data)

### 2.7 Service Pattern

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserInfoResponse updateProfile(JwtUserDetails currentUser, UserProfileUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
        // ... business logic
        user = userRepository.save(user);
        log.info("User profile updated: {}", user.getUsername());
        return toUserInfoResponse(user);
    }

    private UserInfoResponse toUserInfoResponse(User user) { ... }
}
```

Key patterns:
- Constructor injection
- `@Transactional` on write operations
- Private `toXxxResponse()` mapper methods (no MapStruct observed in services despite dependency)
- `log.info()` for success, `log.warn()` for business errors
- Orchestrate repositories directly (no intermediate service layers)

### 2.8 Repository Pattern

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByIdAndDeletedFalse(Long id);
    boolean existsByPhone(String phone);
    Page<User> findByDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.username LIKE %:keyword%")
    Page<User> searchByUsername(@Param("keyword") String keyword, Pageable pageable);
}
```

- Spring Data JPA query derivation for simple queries
- `@Query` JPQL for complex or custom queries
- Soft-delete aware methods (`AndDeletedFalse` suffix)

### 2.9 Cross-Cutting Concerns (AOP)

Five custom annotations with matching aspects:

| Annotation | Aspect | Purpose |
|-----------|--------|---------|
| `@AuditLog` | `AuditLogAspect` | Operation logging to DB |
| `@RateLimit` | `RateLimitAspect` | Request throttling |
| `@RequirePermission` | `PermissionAspect` | Permission checks |
| `@DataIsolation` | `DataIsolationAspect` | Tenant data isolation |
| `@DistributedLock` | `DistributedLockAspect` | Redis-based locking |

All aspects use `@Around` advice. `AuditLogAspect` records request params with sensitive field masking.

### 2.10 Security

- **Stateless JWT** -- no server sessions
- **JWT tokens**: Access token (short-lived) + Refresh token (long-lived) via `JwtTokenProvider`
- **BCrypt** password encoding
- **CSRF disabled** (stateless API, tokens in sessionStorage not cookies)
- **Role-based access**: `@PreAuthorize` on controllers; roles: ENTERPRISE, REVIEWER, AUTHENTICATOR, THIRD_PARTY, ADMIN
- **CORS**: Configurable allowed origins, supports credentials

### 2.11 Database

- **MySQL 8** as primary database
- **Flyway** for migrations (disabled in tests)
- **Hibernate auto-DDL** (`create-drop`) in test profile
- **Redis 7** for caching and distributed locks
- **MinIO** for object storage (files)

### 2.12 Configuration

- `application.yml` with profile-based overrides (`test`, `dev`, `prod`)
- `@Value` injection for simple properties
- `@Configuration` classes for complex bean setup
- Health indicators for DB and Redis (`DatabaseHealthIndicator`, `RedisHealthIndicator`)

## 3. Frontend Conventions

### 3.1 TypeScript Style

- **Strict TypeScript** with explicit type annotations
- **Interface-first** for type definitions (no `type` aliases observed for objects)
- **Barrel exports**: `src/types/index.ts` re-exports all type modules
- **No `any`** in well-written code; `unknown` used for untyped payloads

### 3.2 Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Components | PascalCase `.vue` | `CarbonUpload.vue`, `SystemUsers.vue` |
| API modules | camelCase `.ts` | `auth.ts`, `carbon.ts`, `trade.ts` |
| Types/Interfaces | PascalCase | `ApiResponse<T>`, `LoginRequest` |
| Functions | camelCase | `getAccessToken()`, `parseJwtPayload()` |
| Constants | UPPER_SNAKE_CASE | `TOKEN_KEY`, `SUCCESS_CODES` |
| Stores | camelCase `use...Store` | `useAppStore` |
| CSS classes | kebab-case | `captcha-image`, `el-form-item` |

### 3.3 API Layer

Each domain has its own API module (`src/api/*.ts`):
```typescript
import request from './request'
import type { LoginRequest, LoginResponse } from '../types'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}
```

**Axios interceptors** (`request.ts`):
- Request: Auto-attaches Bearer token, auto-refreshes expired tokens, transforms pagination params (`pageNum/pageSize` -> `page/size`)
- Response: Unwraps `ApiResponse<T>` envelope, transforms Spring Data `Page` to `{ items, total, page, size, totalPages }`
- Error: Maps HTTP status codes to user-facing `ElMessage` notifications
- Retry: Up to 2 retries on network errors with exponential backoff

### 3.4 State Management

Single Pinia store (`useAppStore`):
- Derives auth state from JWT token on init
- Actions: `login()`, `logout()`, `toggleSidebar()`
- Getters: `roleLabel`, `homePath`
- Token storage: Access token in `sessionStorage`, refresh token in `localStorage`, in-memory cache

### 3.5 Routing

- Role-based guards in `router.beforeEach()`
- Route `meta.roles` array restricts access per role
- Public routes marked with `meta.public: true`
- Lazy-loaded components via `() => import(...)`
- Role constants from `config/menu.ts`: `ROLE.ENTERPRISE`, `ROLE.REVIEWER`, etc.

### 3.6 Component Pattern

Views are organized by role:
```
views/
  enterprise/     # CarbonUpload, TradingMarket, P2P, CreditScore, etc.
  auditor/        # AuditList
  authenticator/  # VerifyList
  admin/          # SystemUsers, SystemConfig, DataStatistics
  third-party/    # Monitor
```

Shared components in `components/`. Layout shell in `layout/`.

### 3.7 Validation

- Backend: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, etc.) on DTOs
- Frontend: Element Plus form validation rules; `@Valid @RequestBody` on controllers

### 3.8 i18n

- `vue-i18n` with `zh-CN` and `en-US` locales
- Translation keys used in templates (e.g., `t('layout.title')`)

## 4. Build & Tooling

### 4.1 Backend

- **Maven** build with Spring Boot parent
- **PMD** and **SpotBugs** for static analysis (non-blocking: `failOnViolation=false`)
- **JaCoCo** for coverage enforcement (90% line coverage per package)
- **Surefire** for unit tests (`**/*Test.java`)
- **Failsafe** for integration tests (`**/*IntegrationTest.java`)

### 4.2 Frontend

- **Vite 8** for dev server and builds
- **TypeScript 6** with `vue-tsc` for type checking
- **Vitest 4** with happy-dom for unit tests
- **Playwright** for E2E tests (Chromium only)
- **Element Plus** as UI component library

### 4.3 Docker

- `docker-compose.yml` for full-stack: MySQL, Redis, MinIO, backend, frontend
- Environment via `.env` file (copy from `.env.example`)

## 5. Code Quality Rules

1. **No mutation of existing objects** -- use builders or new instances
2. **Constructor injection only** -- `@RequiredArgsConstructor` (no `@Autowired` on fields)
3. **Soft delete** -- never hard-delete entities; use `isDeleted` flag
4. **Bilingual comments** -- Chinese primary, English secondary in Javadoc
5. **`@Transactional`** on all write-path service methods
6. **Validation at boundaries** -- `@Valid` on controller params, business validation in services
7. **Centralized error handling** -- throw exceptions, let `GlobalExceptionHandler` convert to responses
8. **No secrets in code** -- all sensitive values via `@Value` from config/env
