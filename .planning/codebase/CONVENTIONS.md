# Coding Conventions

**Analysis Date:** 2026-05-18

## Naming Patterns

### Files

**Backend (Java):**
- Controllers: PascalCase with `Controller` suffix -- `CarbonController.java`
- Services: PascalCase with `Service` suffix -- `CarbonService.java`
- Repositories: PascalCase with `Repository` suffix -- `CarbonReportRepository.java`
- Entities: PascalCase, singular noun -- `CarbonReport.java`, `User.java`
- DTOs: PascalCase with `Request`/`Response` suffix -- `CarbonReportRequest.java`, `CarbonReportResponse.java`
- Enums: PascalCase with `Enum` suffix -- `ReportStatusEnum.java`, `TradeStatusEnum.java`
- Exceptions: PascalCase with `Exception` suffix -- `CarbonException.java`, `TradeException.java`
- Custom annotations: PascalCase, no suffix -- `AuditLog.java`, `RateLimit.java`
- AOP aspects: PascalCase with `Aspect` suffix -- `AuditLogAspect.java`
- Constants: PascalCase -- `ErrorCode.java`, `ErrorMessage.java`

**Frontend (TypeScript/Vue):**
- Vue components: PascalCase `.vue` files -- `CarbonUpload.vue`, `Login.vue`
- TypeScript files: camelCase -- `auth.ts`, `request.ts`
- Test files: PascalCase with `.test.ts` suffix -- `CarbonUpload.test.ts`, `Login.test.ts`
- Directories: kebab-case -- `third-party/`, `page-objects/`
- API modules: kebab-case -- `carbon.ts`, `trade.ts` (in `src/api/`)
- Type definition files: kebab-case -- `api.ts`, `carbon.ts` (in `src/types/`)
- i18n locale files: locale code -- `zh-CN.ts`, `en-US.ts`

### Functions and Methods

**Backend (Java):**
- Service methods: camelCase, verb-first -- `createReport()`, `submitReport()`, `listMyReports()`
- Repository query methods: Spring Data derived query naming -- `findByUserId()`, `findByEnterpriseIdAndDeletedFalse()`
- Controller endpoints: verb-first or noun-based -- `createReport()`, `listReports()`, `getReport()`
- Static factory methods on exceptions: camelCase, domain-qualifier -- `CarbonException.submitFailed()`, `BusinessException.of()`

**Frontend (TypeScript):**
- Functions: camelCase -- `getAccessToken()`, `isTokenExpired()`, `parseJwtPayload()`
- Event handlers: camelCase with `on` prefix or `handle` prefix

### Variables

**Backend:**
- Constants: `UPPER_SNAKE_CASE` -- `SYSTEM_ERROR`, `TOKEN_EXPIRED`
- Class fields: camelCase -- `carbonReportRepository`, `testEnterprise`
- Builder fields: camelCase -- `enterpriseId`, `totalEmission`

**Frontend:**
- Constants: `UPPER_SNAKE_CASE` -- `TOKEN_KEY`, `RETRY_MAX_ATTEMPTS`
- Variables: camelCase -- `accessToken`, `responsePayload`
- Type parameters: PascalCase -- `T`, `ApiResponse<T>`

### Types

**Backend Enums:**
- Enum names: PascalCase with `Enum` suffix -- `ReportStatusEnum`, `TradeStatusEnum`
- Enum values: `UPPER_SNAKE_CASE` -- `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`
- Each enum has `code` (Integer) and `description` (String) fields
- Standard methods: `fromCode(Integer)`, domain-specific boolean methods (e.g., `isEditable()`, `isSubmittable()`)
- Example at `src/main/java/com/oaiss/chain/enums/ReportStatusEnum.java`:
```java
@Getter
public enum ReportStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    UNDER_REVIEW(2, "审核中"),
    APPROVED(3, "审核通过"),
    REJECTED(4, "审核拒绝"),
    ON_CHAIN(5, "已上链");

    private final Integer code;
    private final String description;

    ReportStatusEnum(Integer code, String description) { ... }

    public static ReportStatusEnum fromCode(Integer code) { ... }
    public boolean isEditable() { return this == DRAFT || this == REJECTED; }
}
```

**Frontend Types:**
- Interfaces: PascalCase -- `ApiResponse<T>`, `JwtPayload`
- Type aliases: PascalCase -- `SpringPage<T>`, `TransformedPage<T>`
- Barrel export at `src/types/index.ts` re-exports from domain-specific type files

## Code Style

### Formatting

**Backend (Java):**
- Lombok annotations: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on DTOs and entities
- Lombok `@Slf4j` for logging (not manual logger creation)
- Lombok `@RequiredArgsConstructor` for constructor injection (no `@Autowired`)
- Lombok `@Getter` on enums instead of `@Data`
- Indentation: 4 spaces
- File header: `package` statement, followed by grouped imports, then Javadoc class comment with `@author OAISS Team`
- Section separators: `// ==================== Section Name ====================`

**Frontend (TypeScript/Vue):**
- Indentation: 2 spaces
- Single quotes for strings
- Semicolons required
- `export default` for Vue components and i18n modules
- Named exports for utility functions and types

### Linting

**Backend:**
- PMD: `maven-pmd-plugin` 3.21.0 with `quickstart.xml` ruleset, `failOnViolation=false`
- SpotBugs: `spotbugs-maven-plugin` 4.8.6.2, effort Max, threshold Medium, `failOnError=false`
- Neither blocks the build currently

**Frontend:**
- No ESLint or Prettier config detected in the project
- Relies on Vite and TypeScript compiler for type checking

## Import Organization

**Backend:**
1. `com.oaiss.chain.*` (project imports)
2. Third-party libraries (`lombok`, `jakarta.*`, `org.springframework.*`)
3. Standard Java (`java.*`)

No explicit ordering enforced by tooling. Imports are typically organized by package grouping.

**Frontend:**
1. Vue framework imports -- `import { ... } from 'vue'`
2. Third-party libraries -- `import axios from 'axios'`
3. Project modules -- `import { ... } from '../../api/carbon'`
4. Types -- `import type { ... } from '../types'`

**Path Aliases:**
- `@` mapped to `src/` via Vite config in `oaiss-chain-frontend/vite.config.js`

## Error Handling

### Backend Error Architecture

**Three-tier error system:**

1. **ErrorCode constants** (`constant/ErrorCode.java`): Integer error codes organized by module prefix
   - `1xxx` -- Common errors (SYSTEM_ERROR=1000, PARAM_ERROR=1001, RESOURCE_NOT_FOUND=1002)
   - `2xxx` -- Auth errors (USER_NOT_LOGIN=2000, LOGIN_FAILED=2001, TOKEN_INVALID=2002)
   - `3xxx` -- Carbon module errors (CARBON_REPORT_NOT_FOUND=3000)
   - `4xxx` -- Trade module errors (TRADE_NOT_FOUND=4000)
   - `5xxx` -- Blockchain errors (BLOCKCHAIN_CONNECTION_FAILED=5000)
   - `6xxx` -- ML/AI errors (ML_SERVICE_UNAVAILABLE=6000)

2. **ErrorMessage constants** (`constant/ErrorMessage.java`): i18n message keys mapped to `messages_*.properties`
   - Key pattern: `error.{domain}.{detail}` -- `error.carbon.report.not.found`, `error.login.failed`
   - Support parameterized messages with `{0}` placeholders

3. **Exception hierarchy** (`exception/`):
   - `BusinessException` -- base, supports i18n via message keys
   - `CarbonException extends BusinessException` -- carbon module
   - `TradeException extends BusinessException` -- trade module
   - `AuthenticationException extends BusinessException` -- auth failures
   - `AuthorizationException extends BusinessException` -- permission failures
   - `BlockchainException extends BusinessException` -- blockchain errors

**Usage pattern:**
```java
// Using static factory methods on domain exceptions
throw CarbonException.submitFailed("未找到关联企业信息");

// Using BusinessException.of() with ErrorCode + i18n message key
throw BusinessException.of(ErrorCode.CARBON_REPORT_NOT_FOUND, ErrorMessage.CARBON_REPORT_NOT_FOUND, reportId);

// Using BusinessException with raw message (backward compatible)
throw new BusinessException(403, "无权限创建报告");
```

**GlobalExceptionHandler** (`exception/GlobalExceptionHandler.java`):
- Catches all exceptions and returns `ApiResponse<Void>` with appropriate HTTP status
- Maps error code ranges to HTTP statuses: 2xxx -> 401/403, 1xxx -> 400/404
- Handles validation errors (`@Valid`), Spring Security errors, and unknown exceptions

### Frontend Error Handling

**Axios interceptor** (`src/api/request.ts`):
- Success codes: `[200, 0]` treated as success
- Automatic token refresh on expiry with request queuing
- Retry up to 2 times on network errors with exponential backoff (base 1000ms)
- Automatic Spring Data Page format transformation to `{ items, total, page, size, totalPages }`
- Error messages displayed via `ElMessage.error()`
- 401 -> redirect to `/login` and clear tokens
- 403 -> "没有权限执行此操作"
- 404 -> "请求的资源不存在"

## Logging

**Backend:**
- Framework: SLF4J via Lombok `@Slf4j`
- JSON logging via `logstash-logback-encoder` for ELK integration
- Logback config: `src/main/resources/logback-spring.xml`
- Test profile: `com.oaiss.chain` level set to `DEBUG`

**Frontend:**
- No structured logging framework detected
- Uses `ElMessage` for user-facing error notifications
- `console.error` used in catch blocks

## Comments

**When to Comment:**
- Javadoc on all public classes, methods, and fields
- Chinese + English bilingual comments on class-level Javadoc
- Section separators using `// ==================== Section Name ====================`

**Javadoc Pattern:**
```java
/**
 * 碳核算控制器
 * Carbon Accounting Controller
 *
 * @author OAISS Team
 */
```

**Frontend Comments:**
- Function-level JSDoc on utility functions
- Inline comments for complex logic in both Chinese and English

## Function Design

**Size:**
- Controller methods: thin delegators (3-10 lines), call service and wrap in `ApiResponse`
- Service methods: business logic containers, typically 20-60 lines
- Target: functions under 50 lines, files under 800 lines

**Parameters:**
- Controllers: `@AuthenticationPrincipal JwtUserDetails` for current user, `@Valid @RequestBody` for request DTOs
- Services: domain objects (Entity, DTO), `JwtUserDetails` for auth context
- Use `@RequestParam(defaultValue = "1")` for pagination defaults

**Return Values:**
- Controllers: always wrap in `ApiResponse<T>` via static factory methods
- `ApiResponse.success(data)` -- single object success
- `ApiResponse.success(data, message)` -- success with custom message
- `ApiResponse.success()` -- no-data success
- `ApiResponse.success(data, page, size, total)` -- paginated success
- Services: return DTOs directly (not wrapped in ApiResponse), or `Page<DTO>` for lists

## Module Design

**Exports:**
- Backend: No barrel files; each class is in its own file with standard Java package scoping
- Frontend types: barrel file at `src/types/index.ts` re-exports from domain-specific type files
- Frontend API modules: each domain has its own file in `src/api/`, imported individually

**Dependency Injection:**
- Backend: Constructor injection via Lombok `@RequiredArgsConstructor` (never field injection with `@Autowired`)
- All dependencies declared as `private final` fields

## DTO Patterns

**Request DTOs:**
- Annotated with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- Validation annotations: `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`
- Chinese validation messages: `@NotBlank(message = "核算周期不能为空")`
- No MapStruct mapper interfaces detected; mapping is done manually in services (MapStruct dependency exists in pom.xml but no mappers defined)

**Response DTOs:**
- Annotated with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- Include computed/display fields like `statusText`, `enterpriseName`
- Flat structure (no nested DTOs)

**Example request DTO** (`dto/CarbonReportRequest.java`):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonReportRequest {
    @NotBlank(message = "核算周期不能为空")
    @Size(max = 20, message = "核算周期格式不正确")
    private String accountingPeriod;

    @NotBlank(message = "报告标题不能为空")
    @Size(max = 200, message = "标题不能超过200字符")
    @Pattern(regexp = "^[^<>]*$", message = "标题不能包含特殊字符<>")
    private String title;

    @NotNull(message = "报告类型不能为空")
    private Integer reportType;
}
```

## Security Conventions

### Role-Based Access Control

**Four roles:** ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN

**Backend enforcement:**
- `@PreAuthorize("hasRole('ENTERPRISE')")` on controller methods
- `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")` for shared endpoints
- Custom annotations: `@RequirePermission` for fine-grained checks (AOP-based)
- `@DataIsolation` annotation for tenant data isolation

**Frontend enforcement:**
- Vue Router `meta.roles` for route-level access control
- JWT token contains `roles` array for client-side role checks
- Token stored in `localStorage` (remember me) or `sessionStorage`

### JWT Token Handling

**Backend:**
- `JwtAuthenticationFilter` validates tokens on every request (`security/JwtAuthenticationFilter.java`)
- `JwtTokenProvider` handles token creation and validation (`security/JwtTokenProvider.java`)
- Access token + refresh token pattern
- Token expiry checked via JWT `exp` claim

**Frontend:**
- `src/utils/auth.ts` manages token lifecycle
- Memory cache + localStorage/sessionStorage dual storage
- Automatic token refresh in Axios interceptor with request queuing
- `isTokenExpired()` checks JWT payload `exp` field

### Cross-Cutting Security Annotations

| Annotation | Purpose | AOP Aspect |
|------------|---------|------------|
| `@AuditLog` | Operation audit logging | `AuditLogAspect` |
| `@RateLimit` | Request throttling | `RateLimitAspect` |
| `@RequirePermission` | Permission checks | `PermissionAspect` |
| `@DataIsolation` | Tenant data isolation | `DataIsolationAspect` |
| `@DistributedLock` | Redis-based distributed locking | `DistributedLockAspect` |

All defined in `annotation/`, aspects in `aop/`.

## Database Conventions

### Table Naming

- Table names: `snake_case` -- `carbon_report`, `enterprise`, `user`
- Column names: `snake_case` -- `created_at`, `updated_at`, `is_deleted`
- Migration files: Flyway `V{N}__{description}.sql` -- `V1__init_schema.sql`, `V2__seed_data.sql`, `V4__enterprise_admission.sql`

### Soft Delete Pattern

All entities extend `BaseEntity` (`entity/BaseEntity.java`):
```java
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;
}
```

- `deleted` field: `Boolean`, default `false`
- Repository queries append `AndDeletedFalse` to derived query methods
- Example: `findByEnterpriseIdAndDeletedFalse()` in `CarbonReportRepository`

### Audit Fields

- `createdAt`: auto-populated via `@CreatedDate`, not updatable
- `updatedAt`: auto-populated via `@LastModifiedDate`
- JPA auditing enabled via `@EntityListeners(AuditingEntityListener.class)` on `BaseEntity`

### Pagination

- Frontend sends `pageNum` (1-based) and `pageSize`
- Axios interceptor converts to `page` and `size` before sending to backend
- Backend uses Spring Data `PageRequest.of(page, size)` with 0-based indexing internally
- Controller `@RequestParam(defaultValue = "1") Integer page` -- 1-based from client perspective
- Backend returns Spring `Page<T>` with `content`, `totalElements`, `totalPages`, `number`, `size`
- Frontend interceptor transforms to `{ items, total, page, size, totalPages }`

## API Conventions

### REST Endpoint Naming

- Base path: `/api/v1` (configured globally)
- Resource naming: plural nouns -- `/carbon/reports`, `/carbon/reports/{reportId}`
- Sub-resources: nested paths -- `/carbon/reports/{reportId}/submit`
- Actions: verb as sub-resource -- `/carbon/reports/{reportId}/submit`, `/carbon/review`
- Calculation endpoints: `/carbon/calculate/power-grid`, `/carbon/calculate/power-generation`
- Auth endpoints: `/auth/login`, `/auth/refresh`, `/auth/register`

### HTTP Methods

- `POST` for create and action endpoints -- `POST /carbon/reports`, `POST /carbon/reports/{id}/submit`
- `GET` for read endpoints -- `GET /carbon/reports`, `GET /carbon/reports/{id}`
- `DELETE` for soft delete -- `DELETE /carbon/reports/{id}`
- `PUT`/`PATCH` not heavily used (most mutations via POST to action endpoints)

### Response Format

All responses wrapped in `ApiResponse<T>` (`dto/ApiResponse.java`):
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "meta": {
    "requestId": "req_1234567890_1",
    "timestamp": "2024-01-01T12:00:00",
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 100,
      "totalPages": 10
    }
  }
}
```

- `@JsonInclude(JsonInclude.Include.NON_NULL)` -- null fields omitted
- `pagination` in meta only present for paginated responses
- Error responses omit `data` and `pagination`

## i18n Conventions

### Backend i18n

- Message files: `src/main/resources/i18n/messages_zh_CN.properties`, `messages_en_US.properties`
- Key pattern: `error.{domain}.{detail}` -- `error.carbon.report.not.found`
- Parameterized messages: `{0}`, `{1}` placeholders -- `error.param=请求参数错误：{0}`
- Constants in `ErrorMessage.java` map 1:1 to property keys
- `MessageUtils.getMessage(key, args)` resolves localized messages

### Frontend i18n

- Locale files: `src/i18n/locales/zh-CN.ts`, `src/i18n/locales/en-US.ts`
- Key structure: nested object by domain -- `common.confirm`, `menu.carbonAccounting`, `carbon.submit`
- Template interpolation: `{count}` -- `common.total: '共 {count} 条'`
- Uses `vue-i18n` for message resolution

## Git Conventions

### Commit Messages

Format: `<type>(<scope>): <description>`

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

Examples from git log:
```
fix: add null safety for enum fromCode() in TradeService and DoubleAuctionService
fix: correct role permissions for blockchain and carbon-neutral endpoints
test(08): complete UAT -- 20/20 checkpoints passed
test(12): complete UAT -- 22/22 checkpoints passed, all acceptance criteria met
```

### Branch Naming

- Feature branches: `claude/{descriptive-name}-{hash}` -- `claude/upbeat-franklin-9004e0`
- Main branch: `main`

---

*Convention analysis: 2026-05-18*
