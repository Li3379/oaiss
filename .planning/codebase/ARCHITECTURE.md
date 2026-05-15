<!-- refreshed: 2026-05-15 -->
# Architecture

**Analysis Date:** 2026-05-15

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3 + TypeScript)                      │
│                      oaiss-chain-frontend (port 5173)                     │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ HTTP (JWT Bearer + CSRF cookie)
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application (port 8080)                  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                   Security Filter Chain                            │  │
│  │  JwtAuthenticationFilter -> SecurityConfig (role-based)            │  │
│  └───────────────────────────────┬────────────────────────────────────┘  │
│                                  v                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                   AOP Cross-Cutting Layer                          │  │
│  │  @AuditLog  @RateLimit  @DataIsolation  @DistributedLock          │  │
│  │  @RequirePermission                                                │  │
│  └───────────────────────────────┬────────────────────────────────────┘  │
│                                  v                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                   Controller Layer (20 controllers)                │  │
│  │  `controller/*.java` -- REST endpoints, @PreAuthorize             │  │
│  └───────────────────────────────┬────────────────────────────────────┘  │
│                                  v                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                   Service Layer (24+ services)                     │  │
│  │  `service/*.java` -- Business logic, transactional                │  │
│  │  `service/ml/*.java` -- ML/AI prediction services                 │  │
│  └─────────────┬───────────────────────────────┬─────────────────────┘  │
│                │                               │                         │
│                v                               v                         │
│  ┌──────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │  Repository Layer (JPA)  │  │  External Integrations              │  │
│  │  `repository/*.java`     │  │  Redis, MinIO, Blockchain          │  │
│  └──────────┬───────────────┘  └─────────────────────────────────────┘  │
│             │                                                            │
└─────────────┼────────────────────────────────────────────────────────────┘
              v
┌──────────────────────────────────────────────────────────────────────────┐
│  Data Layer                                                              │
│  MySQL 8 (JPA/Hibernate)  |  Redis 7 (cache/locks/rate-limit)           │
│  MinIO (file storage)     |  Hyperledger Fabric (blockchain)            │
└──────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| JwtAuthenticationFilter | Token validation, security context setup, whitelist bypass, path traversal protection | `security/JwtAuthenticationFilter.java` |
| SecurityConfig | Filter chain, CORS, CSRF, session policy, endpoint authorization rules | `config/SecurityConfig.java` |
| GlobalExceptionHandler | Unified exception-to-ApiResponse mapping | `exception/GlobalExceptionHandler.java` |
| BusinessException | Base business exception with i18n message key support | `exception/BusinessException.java` |
| ErrorCode | Numeric error code constants (1xxx-6xxx by module) | `constant/ErrorCode.java` |
| ApiResponse | Standard response envelope with code/message/data/meta | `dto/ApiResponse.java` |
| BaseEntity | JPA superclass with id, createdAt, updatedAt, deleted fields | `entity/BaseEntity.java` |
| JwtTokenProvider | JWT generation, parsing, validation (HMAC-SHA) | `security/JwtTokenProvider.java` |
| JwtUserDetails | Spring Security UserDetails implementation with roles/userType/enterpriseId | `security/JwtUserDetails.java` |
| EnterpriseContextHolder | Thread-local storage for enterprise context during request processing | `security/EnterpriseContextHolder.java` |
| BlockchainServicePort | Hexagonal port interface for blockchain operations | `service/BlockchainServicePort.java` |

## Pattern Overview

**Overall:** Classic layered architecture with AOP-driven cross-cutting concerns

**Key Characteristics:**
- Controller-Service-Repository three-layer separation
- Custom annotations + AOP aspects for cross-cutting concerns (audit, rate-limit, permissions, data isolation, distributed locks)
- Unified ApiResponse envelope for all endpoints
- JWT stateless authentication with token blacklist via Redis cache
- Role-based access control via both `@PreAuthorize` (Spring Security) and custom `@RequirePermission` (AOP)
- Hexagonal-style port adapter for blockchain integration (`BlockchainServicePort` interface with `FabricBlockchainService` and `MockBlockchainService` implementations)
- ML microservice integration via HTTP client (`MlServiceClient`) for AI prediction features

## Layers

**Controller Layer:**
- Purpose: HTTP endpoint definitions, request validation, response wrapping
- Location: `src/main/java/com/oaiss/chain/controller/`
- Contains: `@RestController` classes with `@RequestMapping`, Swagger annotations, `@PreAuthorize`
- Depends on: Service layer, DTOs, Security context (`@AuthenticationPrincipal JwtUserDetails`)
- Used by: Frontend via REST API (`/api/v1/*`)

**Service Layer:**
- Purpose: Business logic, transaction management, orchestration
- Location: `src/main/java/com/oaiss/chain/service/` and `service/ml/`
- Contains: `@Service` classes with `@Transactional`, domain operations
- Depends on: Repository layer, external services (Redis, MinIO, Blockchain, ML microservice)
- Used by: Controller layer

**Repository Layer:**
- Purpose: Data access abstraction via Spring Data JPA
- Location: `src/main/java/com/oaiss/chain/repository/`
- Contains: `JpaRepository` interfaces with custom query methods
- Depends on: JPA entities, database connection
- Used by: Service layer

**Entity Layer:**
- Purpose: JPA entity definitions mapped to database tables
- Location: `src/main/java/com/oaiss/chain/entity/`
- Contains: `@Entity` classes extending `BaseEntity`
- Depends on: JPA annotations, Lombok
- Used by: Repository layer, Service layer

**DTO Layer:**
- Purpose: Request/response data transfer objects, API envelope
- Location: `src/main/java/com/oaiss/chain/dto/`
- Contains: Request DTOs (`*Request`), response DTOs (`*Response`), `ApiResponse`, `PageRequest`, `PageResponse`
- Depends on: Jakarta Validation, Jackson, Lombok
- Used by: Controller layer, Service layer

**Security Layer:**
- Purpose: Authentication, authorization, JWT handling
- Location: `src/main/java/com/oaiss/chain/security/`
- Contains: JWT filter, token provider, user details, entry point, access denied handler, enterprise context
- Depends on: JJWT library, Spring Security, Redis (token blacklist)
- Used by: SecurityConfig, Controller layer

**AOP Layer:**
- Purpose: Cross-cutting concerns via aspect-oriented programming
- Location: `src/main/java/com/oaiss/chain/aop/` (aspects) and `annotation/` (annotations)
- Contains: `@Aspect` components for audit logging, rate limiting, data isolation, distributed locking, permission checking
- Depends on: Redis (rate limit, distributed lock), OperationLogRepository (audit), SecurityContext
- Used by: Applied to Controller/Service methods via annotations

**Configuration Layer:**
- Purpose: Spring framework configuration
- Location: `src/main/java/com/oaiss/chain/config/`
- Contains: Security, Redis, MinIO, JPA auditing, Swagger, CORS, health indicators, Fabric gateway, i18n, metrics, ML service
- Depends on: Spring Boot auto-configuration, application properties
- Used by: Spring IoC container

## Data Flow

### Primary Request Path (Authenticated CRUD)

1. **Frontend** sends HTTP request with `Authorization: Bearer <JWT>` header (`api/request.ts` interceptor injects token)
2. **JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`) extracts JWT, validates signature/expiry, checks Redis token blacklist, builds `JwtUserDetails` and sets `SecurityContextHolder`
3. **SecurityConfig** (`config/SecurityConfig.java`) applies URL-based authorization rules
4. **AOP aspects** execute in order: `@RateLimit` (Redis Lua script counter) -> `@RequirePermission` (user type + API path check) -> `@DataIsolation` (enterprise context setup) -> `@AuditLog` (operation logging) -> `@DistributedLock` (Redis lock if annotated)
5. **Controller** (`controller/*.java`) receives `@AuthenticationPrincipal JwtUserDetails`, validates `@Valid @RequestBody`, calls service, returns `ApiResponse<T>`
6. **Service** (`service/*.java`) executes business logic within `@Transactional`, calls repository
7. **Repository** (`repository/*.java`) executes JPA queries against MySQL
8. **Response** flows back as `ApiResponse` with `{ code, message, data, meta }` envelope

### Authentication Flow

1. Frontend POSTs to `/api/v1/auth/login` with `{ username, password, captchaCode, captchaKey }`
2. `AuthService` validates credentials via BCrypt, generates access + refresh JWT via `JwtTokenProvider`
3. Access token includes: `userId`, `username`, `roles`, `userType`, `enterpriseId`
4. Frontend stores tokens in sessionStorage; Axios interceptor attaches `Authorization: Bearer <token>`
5. Token refresh: POST to `/api/v1/auth/refresh` with refresh token; new access token returned in `Authorization` header

### Blockchain Integration Flow

1. Service calls `BlockchainServicePort` interface methods (e.g., `commitReportToChain`)
2. Spring profile selects implementation: `FabricBlockchainService` (real Fabric SDK, `@Profile("fabric")`) or `MockBlockchainService` (`@Profile("mock")`)
3. `FabricGatewayConfig` (`config/FabricGatewayConfig.java`) creates `Gateway`, `Network`, and `Contract` beans from crypto material in `resources/fabric/crypto/`
4. Chaincode invocations go through Hyperledger Fabric Gateway SDK

### ML Prediction Flow

1. Controller calls `EnterpriseInferenceService` or `MarketPredictionService` (`service/ml/`)
2. Service calls `MlServiceClient` (`service/ml/MlServiceClient.java`) which makes HTTP requests to an external ML microservice
3. ML service URL configured via `MlServiceConfig` (`config/MlServiceConfig.java`)
4. Responses returned as `EnterpriseInferenceResponse` or `MarketForecastResponse`

**State Management:**
- No server-side session (STATELESS session policy)
- JWT claims carry all user context (userId, roles, userType, enterpriseId)
- `EnterpriseContextHolder` (ThreadLocal) provides enterprise context during request lifecycle, cleared in `finally` block
- Redis used for: token blacklist, distributed locks, rate limit counters, cache

## Key Abstractions

**ApiResponse Envelope:**
- Purpose: Unified response format for all endpoints
- Pattern: Static factory methods (`ApiResponse.success()`, `ApiResponse.error()`)
- Files: `dto/ApiResponse.java`
- Includes: `code`, `message`, `data`, `meta` (with `requestId`, `timestamp`, optional `pagination`)

**PageRequest / PageResponse:**
- Purpose: Standardized pagination across the API
- Pattern: `PageRequest.toPageable()` converts 1-based pageNum to 0-based Spring Data Pageable; `PageResponse.of(Page)` converts back
- Files: `dto/PageRequest.java`, `dto/PageResponse.java`

**BlockchainServicePort (Hexagonal Port):**
- Purpose: Abstract blockchain operations behind an interface
- Pattern: Port/Adapter -- `BlockchainServicePort` (interface) -> `FabricBlockchainService` / `MockBlockchainService`
- Files: `service/BlockchainServicePort.java`, `service/FabricBlockchainService.java`, `service/MockBlockchainService.java`

**BusinessException with i18n:**
- Purpose: Domain exceptions with internationalized messages
- Pattern: Message keys prefixed with `error.` resolved via `MessageUtils.getMessage()` from `resources/i18n/messages*.properties`
- Files: `exception/BusinessException.java`, `util/MessageUtils.java`

## Entry Points

**REST API:**
- Location: `src/main/java/com/oaiss/chain/controller/*.java`
- Base path: `/api/v1` (configured as context-path)
- Triggers: Frontend Axios HTTP requests
- Responsibilities: Request validation, service delegation, response wrapping

**Application Bootstrap:**
- Location: `src/main/java/com/oaiss/chain/OaissChainApplication.java`
- Triggers: `mvn spring-boot:run` or JAR execution
- Responsibilities: Spring Boot application startup, component scanning

**Scheduled Tasks:**
- Location: `service/CachePreloadService.java`
- Triggers: `@Scheduled` annotations for cache warming
- Responsibilities: Pre-load frequently accessed data into Redis

## Architectural Constraints

- **Threading:** Single-threaded request handling per HTTP request (Tomcat thread pool); no explicit thread creation in business logic. Distributed locks (`@DistributedLock`) prevent concurrent modification of shared resources.
- **Global state:** `EnterpriseContextHolder` uses ThreadLocal for enterprise context; cleared in `finally` blocks. `SecurityContextHolder` is Spring Security's standard thread-local.
- **No circular dependencies:** Controller -> Service -> Repository is strictly unidirectional. Services do not call controllers. Repositories do not call services.
- **Profile-based implementations:** Blockchain service uses `@Profile("fabric")` / `@Profile("mock")` for environment-specific implementations. Fabric config only activates under `fabric` profile.
- **Soft delete:** All entities extend `BaseEntity` with `deleted` boolean field. Repository queries must use `...AndDeletedFalse` method naming convention.
- **ML service external dependency:** AI prediction features (`service/ml/`) depend on an external ML microservice reachable via HTTP. If unavailable, predictions fail but core platform functions are unaffected.

## Anti-Patterns

### UserTypeEnum vs Integer userType

**What happens:** `User.entity` stores `userType` as `Integer`, and `DataIsolationAspect` / `PermissionAspect` compare against integer constants (e.g., `USER_TYPE_ADMIN = 99`). However, `UserTypeEnum` defines ADMIN as code `4`.
**Why it's wrong:** The admin user type code is inconsistent -- `UserTypeEnum.ADMIN` is `4`, but AOP aspects check for `99`. This could cause admin bypass logic to fail silently.
**Do this instead:** Use `UserTypeEnum` consistently everywhere. Replace integer constants in AOP aspects with `UserTypeEnum.ADMIN.getCode()`. See `enums/UserTypeEnum.java`, `aop/DataIsolationAspect.java:38`, `aop/PermissionAspect.java:48`.

### Duplicate ErrorCode values

**What happens:** `ErrorCode.OPERATION_IN_PROGRESS` and `ErrorCode.FILE_TYPE_NOT_SUPPORTED` both have value `1009`.
**Why it's wrong:** Ambiguous error codes make debugging and client-side error handling unreliable.
**Do this instead:** Assign unique error codes. See `constant/ErrorCode.java:53-56`.

## Error Handling

**Strategy:** Centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) catches all exceptions and maps them to `ApiResponse.error(code, message)` with appropriate HTTP status codes.

**Exception Hierarchy:**
- `BusinessException` -- base for all domain errors, carries `ErrorCode` + i18n message key
  - `AuthenticationException` -- auth failures (HTTP 401)
  - `AuthorizationException` -- permission denied (HTTP 403)
  - `CarbonException` -- carbon domain errors (3xxx codes)
  - `TradeException` -- trade domain errors (4xxx codes)
  - `BlockchainException` -- blockchain errors (5xxx codes)

**HTTP Status Mapping (in GlobalExceptionHandler.getHttpStatus):**
- 1xxx error codes -> 400 BAD_REQUEST (except 1002 -> 404 NOT_FOUND)
- 2000-2003 -> 401 UNAUTHORIZED
- 2004 -> 403 FORBIDDEN
- Spring validation errors -> 400 with field-level error list
- Uncaught exceptions -> 500 with generic message (no internal details leaked)

**Patterns:**
- Controllers throw `BusinessException` with specific `ErrorCode` constant
- Services validate business rules and throw `BusinessException.paramError()`, `.notFound()`, `.authFailed()`, etc.
- `GlobalExceptionHandler` logs all exceptions at WARN level (ERROR for uncaught)
- Validation errors (`@Valid`) are caught and returned as structured field-level error lists

## Cross-Cutting Concerns

**Logging:**
- SLF4J + Logback (`logback-spring.xml`)
- AOP `@AuditLog` persists operation logs to `operation_log` table via `OperationLogRepository`
- Sensitive fields (password, token, secret) automatically redacted from audit logs

**Validation:**
- Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Min`, `@Max`, etc.) on DTO fields
- Controller-level `@Valid @RequestBody` triggers validation before service invocation
- Custom validation in services for business rules (e.g., report status transitions)

**Authentication:**
- JWT Bearer token in `Authorization` header
- `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`
- Token blacklist via Redis cache (`tokenBlacklist`) for logout support
- Path traversal protection via path normalization in whitelist matching

**Rate Limiting:**
- `@RateLimit` annotation with Redis Lua script (atomic incr + expire)
- Supports: global, per-IP, per-user, per-IP+user rate limiting
- Graceful degradation: if Redis fails, request proceeds (fail-open)

**Data Isolation:**
- `@DataIsolation` annotation sets enterprise context via `EnterpriseContextHolder`
- Admin users skip isolation by default (`skipAdmin = true`)
- Enterprise users validated to have non-null enterpriseId

**Distributed Locking:**
- `@DistributedLock` with SpEL key expressions
- Redis-based lock via `RedisLockService` (tryLock/releaseLock)
- Configurable wait time and expiry

---

*Architecture analysis: 2026-05-15*
