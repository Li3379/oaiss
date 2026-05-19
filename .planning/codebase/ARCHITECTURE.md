<!-- refreshed: 2026-05-18 -->
# Architecture

**Analysis Date:** 2026-05-18

## System Overview

OAISS CHAIN is a carbon emissions management and trading platform with four deployable services orchestrated via Docker Compose. The backend is a monolithic Spring Boot application, the frontend is a Vue 3 SPA, the ML service is a Python FastAPI microservice, and Hyperledger Fabric provides optional blockchain integration.

```text
                          +-------------------+
                          |   Browser / SPA   |
                          |  Vue 3 + Pinia    |
                          |  :5173 (dev)      |
                          +--------+----------+
                                   |
                        HTTP /api/v1 (Axios)
                        JWT Bearer + Refresh
                                   |
                 +-----------------v-----------------+
                 |         Spring Boot 3.2.5         |
                 |         Backend (:8080)            |
                 |  +-----------+  +---------------+ |
                 |  | RestControllers (21)           | |
                 |  +-----+-----+  +-------+-------+ |     +-------------------+
                 |        |         |  AOP Aspects   | |     |  FastAPI ML Svc   |
                 |  +-----v-----+   |  (@AuditLog,   | |     |  :8001            |
                 |  | Services  |<--+   @RateLimit,  +----->+  Emission Pred    |
                 |  |   (29)    |   |   @DataIsol,   | |  WB |  Enterprise Inf   |
                 |  +-----+-----+   |   @DistLock,   | |     |  Market Forecast  |
                 |        |         |   @RequirePerm)| |     +-------------------+
                 |  +-----v-----+   +---------------+ |
                 |  | Repos (22)|                     |
                 |  +-----+-----+                     |
                 |        |                            |
                 |  +-----v-----+  +--------+--------+|
                 |  |   MySQL   |  | Redis 7 | MinIO ||
                 |  |   :3306   |  |  :6379  | :9002 ||
                 |  +-----------+  +---------+-------+|
                 +-------------------------------------+
                                   |
                     +-------------v-------------+
                     |  Hyperledger Fabric       |
                     |  (optional, fabric.enabled|
                     |   = false by default)     |
                     |  Go chaincode             |
                     +---------------------------+
```

## Component Responsibilities

| Component | Responsibility | Key Files |
|-----------|----------------|-----------|
| Backend API | All REST endpoints, auth, business logic, persistence | `oaiss-chain-backend/src/main/java/com/oaiss/chain/` |
| Frontend SPA | User interface for all 4 roles, i18n, routing | `oaiss-chain-frontend/src/` |
| ML Service | Emission prediction, enterprise inference, market forecasting | `oaiss-chain-ml-service/app/` |
| Chaincode | Hyperledger Fabric smart contract for carbon data | `oaiss-chain-chaincode/chaincode.go` |
| Docker Compose | Service orchestration (mysql, redis, minio, backend, frontend, ml-service) | `docker-compose.yml` |

## Pattern Overview

**Overall:** Layered monolith with AOP cross-cutting concerns

**Key Characteristics:**
- Strict Controller -> Service -> Repository -> Entity layering in backend
- 5 custom annotations driving cross-cutting behavior via Spring AOP
- Role-based access control at 4 levels: route guards, controller `@PreAuthorize`, `@RequirePermission` annotation, `@DataIsolation` tenant filter
- JWT stateless authentication with access + refresh token pair
- Frontend pagination transformation (pageNum/pageSize <-> page/size) in Axios interceptor
- ML service integration via WebClient with Resilience4j circuit breaker and fallback responses

## Layers

### Backend: Controller Layer
- Purpose: REST endpoint definitions, request validation, response wrapping
- Location: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/`
- Contains: 21 `@RestController` classes
- Depends on: Service layer, `ApiResponse<T>` wrapper
- Used by: Frontend Axios HTTP calls, Swagger UI

### Backend: Service Layer
- Purpose: Business logic orchestration, transaction management
- Location: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/`
- Contains: 26 service classes + `service/ml/` sub-package (3 classes including `MlServiceClient`)
- Depends on: Repository layer, Redis, MinIO, ML service WebClient
- Used by: Controller layer

### Backend: Repository Layer
- Purpose: Data access via Spring Data JPA
- Location: `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/`
- Contains: 22 repository interfaces extending `JpaRepository`
- Depends on: Entity classes, MySQL via Hibernate
- Used by: Service layer

### Backend: Entity Layer
- Purpose: JPA-mapped domain objects
- Location: `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/`
- Contains: 24 entity classes, all extending `BaseEntity`
- Depends on: `BaseEntity` (id, createdAt, updatedAt, deleted fields)
- Used by: Repository layer, Service layer

### Frontend: Views Layer
- Purpose: Page-level Vue components organized by user role
- Location: `oaiss-chain-frontend/src/views/`
- Contains: 4 role subdirectories + 3 top-level views (Login, OfficialHome, NotFound)
- Depends on: API modules (`src/api/`), Pinia store, Vue Router
- Used by: Router

### Frontend: API Layer
- Purpose: Type-safe HTTP client modules for each backend domain
- Location: `oaiss-chain-frontend/src/api/`
- Contains: 22 `.ts` modules + `request.ts` (shared Axios instance)
- Depends on: `request.ts` interceptor, TypeScript types (`src/types/`)
- Used by: View components

## Data Flow

### Primary Request Path (Authenticated CRUD)

1. **User action** in Vue component (`oaiss-chain-frontend/src/views/`)
2. **API module** (`oaiss-chain-frontend/src/api/*.ts`) calls `request.get/post/...`
3. **Request interceptor** (`oaiss-chain-frontend/src/api/request.ts:42-94`):
   - Converts `pageNum/pageSize` params to `page/size`
   - Attaches JWT from `sessionStorage` as `Authorization: Bearer <token>`
   - If token expired: auto-refresh via `/auth/refresh` with queued request deduplication
4. **Backend receives request** at `:8080/api/v1/*` (context-path set in `application.yml:4`)
5. **JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java:58-128`):
   - Checks whitelist (login, register, captcha, swagger, actuator)
   - Normalizes path to prevent traversal (`normalizePath`)
   - Validates JWT, checks token blacklist in Redis cache `tokenBlacklist`
   - Builds `JwtUserDetails` with userId, username, roles, userType, enterpriseId
   - Sets `SecurityContextHolder` authentication
6. **Security filter chain** (`config/SecurityConfig.java:62-84`): Verifies endpoint access rules
7. **Controller method** (`controller/*.java`): `@PreAuthorize`, `@Valid` on request body
8. **AOP aspects** (if annotated): `@AuditLog`, `@RateLimit`, `@DataIsolation`, `@DistributedLock`, `@RequirePermission`
9. **Service** (`service/*.java`): Business logic, calls repositories and external services
10. **Repository** (`repository/*.java`): Spring Data JPA generates SQL, soft-delete via `...AndDeletedFalse` queries
11. **Response**: Service returns domain objects -> Controller wraps in `ApiResponse<T>` or `PageResponse<T>`
12. **Response interceptor** (`request.ts:104-160`): Checks `code` field, transforms Spring Data `Page` to `{ items, total, page, size, totalPages }`
13. **Error path**: `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java`) catches all exceptions, returns `ApiResponse<Void>` with appropriate HTTP status

### ML Prediction Flow

1. Controller calls service method (e.g., `MarketPredictionService`)
2. Service calls `MlServiceClient` (`service/ml/MlServiceClient.java`)
3. `MlServiceClient` sends WebClient POST to ML service (e.g., `POST /predict/market/trend`)
4. Circuit breaker (`resilience4j`) wraps the call with fallback responses
5. Timer metrics recorded via `MeterRegistry`
6. On failure: returns fallback response (e.g., `{ trend: "unknown", modelVersion: "fallback" }`)

### Token Refresh Flow

1. Request interceptor detects expired access token (`isTokenExpired`)
2. If not already refreshing, sends `POST /auth/refresh` with `Refresh-Token` header
3. Backend `JwtTokenProvider` validates refresh token, issues new access + refresh pair
4. Subsequent queued requests are resolved with new token (`onTokenRefreshed`)
5. On refresh failure: all queued requests rejected, tokens cleared, redirect to `/login`

## Key Abstractions

### BaseEntity (Soft Delete + Audit)
- Purpose: Common fields for all entities (id, createdAt, updatedAt, deleted)
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/BaseEntity.java`
- Pattern: `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`
- Fields: `id` (Long, auto-increment), `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`), `deleted` (Boolean, default false)
- All repositories use `...AndDeletedFalse` suffix for soft-delete filtering

### ApiResponse<T> (Response Envelope)
- Purpose: Unified JSON response wrapper with code, message, data, meta
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/ApiResponse.java`
- Shape: `{ code: int, message: String, data: T, meta: { requestId, timestamp, pagination? } }`
- Pattern: Builder with static factory methods `success()`, `success(data)`, `success(data, page, size, total)`, `error(code, message)`
- `@JsonInclude(NON_NULL)` omits null fields

### PageResponse<T> (Pagination Envelope)
- Purpose: Consistent pagination wrapping Spring Data `Page<T>`
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/PageResponse.java`
- Shape: `{ list, total, pageNum (1-based), pageSize, pages, hasPrevious, hasNext, isFirst, isLast }`
- Pattern: Static factory `PageResponse.of(page)` and `PageResponse.of(page, converter)` for entity-to-DTO mapping
- Converts 0-based Spring page number to 1-based `pageNum`

### BlockchainServicePort (Strategy Pattern)
- Purpose: Abstract blockchain operations to allow mock/real switching
- Interface: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/BlockchainServicePort.java`
- Mock impl: `MockBlockchainService.java` (`@Primary`, always active)
- Real impl: `FabricBlockchainService.java` (`@Profile("fabric")`, opt-in)

### Custom Annotation + Aspect Pairs
- Purpose: Declarative cross-cutting concerns via Spring AOP
- Pattern: Each annotation in `annotation/` has a matching `@Aspect` in `aop/`

### JwtUserDetails
- Purpose: Custom UserDetails carrying JWT-extracted claims
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtUserDetails.java`
- Carries: userId, username, roles, userType, enterpriseId

### EnterpriseContextHolder
- Purpose: ThreadLocal-based enterprise context for data isolation
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/EnterpriseContextHolder.java`
- Set by `DataIsolationAspect`, cleared in `finally` block

### MlServiceClient (Circuit Breaker)
- Purpose: Resilient HTTP client for ML service with fallback
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/MlServiceClient.java`
- Pattern: WebClient + Resilience4j `CircuitBreaker` + Micrometer `Timer`
- Endpoints: `/predict/market/trend`, `/predict/market/price`, `/predict/market/supply-demand`, `/api/v1/predict/enterprise/`, `/predict/emission/forecast`

## Entry Points

### Backend Application
- Location: `oaiss-chain-backend/src/main/java/com/oaiss/chain/OaissChainApplication.java`
- Triggers: `mvn spring-boot:run` or Docker container start
- Responsibilities: Spring Boot auto-configuration, component scanning, Flyway migration

### Frontend Application
- Location: `oaiss-chain-frontend/src/main.ts`
- Triggers: `npm run dev` (Vite dev server) or Nginx in Docker
- Responsibilities: Mount Vue app, initialize Pinia, install router and i18n

### ML Service Application
- Location: `oaiss-chain-ml-service/app/main.py`
- Triggers: `uvicorn app.main:app` or Docker container start
- Responsibilities: FastAPI app with 3 routers (enterprise, market, emission), health check at `/health`

### Blockchain Chaincode
- Location: `oaiss-chain-chaincode/chaincode.go`
- Triggers: Fabric peer chaincode instantiation
- Responsibilities: 7 smart contract functions (RecordCarbonReport, VerifyReport, CreateTrade, SettleTrade, RecordCarbonCoinTx, UpdateCreditScore, CreateAdmission)

## Security Architecture

### Authentication Flow
1. User submits credentials to `POST /api/v1/auth/login`
2. `AuthService` validates against BCrypt-hashed password
3. `JwtTokenProvider.generateAccessToken()` creates JWT with claims: userId, username, roles, userType, enterpriseId
4. `JwtTokenProvider.generateRefreshToken()` creates longer-lived refresh token (7 days)
5. Both tokens returned to frontend, stored in `sessionStorage`

### Filter Chain Order
1. `JwtAuthenticationFilter` (before `UsernamePasswordAuthenticationFilter`)
   - Path normalization (traversal protection)
   - Whitelist check (login, register, captcha, swagger, actuator)
   - Token extraction from `Authorization: Bearer` header
   - Token validation via HMAC-SHA256 (`jjwt 0.12.5`)
   - Token blacklist check via Redis cache `tokenBlacklist`
   - `SecurityContextHolder` population with `JwtUserDetails`
2. Spring Security authorization rules (`SecurityConfig.java:62-84`)
   - `permitAll()` for auth endpoints, health, prometheus
   - `authenticated()` for everything else (fine-grained via `@PreAuthorize`)

### Role-Based Access Control
- **4 Roles**: ENTERPRISE (type=1), REVIEWER (type=2), THIRD_PARTY (type=3), ADMIN (type=99)
- **Route-level**: Vue Router `beforeEach` guard checks `meta.roles` against Pinia store role (`router/index.ts:186-198`)
- **Endpoint-level**: `@PreAuthorize` on controller methods
- **Annotation-level**: `@RequirePermission` with `adminOnly()`, `enterpriseOnly()`, `reviewerOnly()`, `thirdPartyOnly()`, and permission code checks against `AccountPermissionList` table
- **Data-level**: `@DataIsolation` sets `EnterpriseContextHolder` for tenant-scoped queries

### Security Headers
Configured in `SecurityConfig.java:91-105`:
- CSP: `default-src 'self'`, restricted script/style/img sources
- X-Frame-Options: DENY
- X-XSS-Protection: enabled, block mode
- Referrer-Policy: strict-origin-when-cross-origin
- X-Content-Type-Options: nosniff

### CSRF
- Disabled explicitly (`csrf(AbstractHttpConfigurer::disable)`)
- Rationale: JWT tokens stored in `sessionStorage` (not cookies), so browsers never auto-attach them. CSRF is not applicable to Bearer token APIs.

### Rate Limiting
- `@RateLimit` annotation with configurable `period`, `limit`, `limitType` (IP, USER, IP_USER, GLOBAL)
- Redis-backed with Lua script for atomic increment + expire
- Graceful degradation: if Redis check fails, request proceeds

## Cross-Cutting Concerns

### @AuditLog
- Aspect: `aop/AuditLogAspect.java`
- Behavior: Records userId, username, userType, module, action, description, HTTP method/URL, IP, user agent, request params (with sensitive field masking), response result, execution time, success/failure status
- Storage: Persists to `OperationLog` entity via `OperationLogRepository`
- Parameters: `module`, `action`, `description`, `recordParams`, `recordResult`, `sensitiveFields`

### @RateLimit
- Aspect: `aop/RateLimitAspect.java`
- Behavior: Redis INCR + EXPIRE via Lua script, throws `BusinessException(REQUEST_TOO_FREQUENT)` on limit exceeded
- Key pattern: `rate_limit:{class}:{method}:{limitType}:{identifier}`

### @DataIsolation
- Aspect: `aop/DataIsolationAspect.java`
- Behavior: Sets `EnterpriseContextHolder` from JWT claims, validates enterprise users have enterpriseId, optionally skips admin users (`skipAdmin=true`)
- Always clears context in `finally` block

### @DistributedLock
- Aspect: `aop/DistributedLockAspect.java`
- Behavior: Acquires Redis lock via `RedisLockService` with SpEL expression key support, releases in `finally`
- Supports retry with configurable `waitTime` and `expireTime`

### @RequirePermission
- Aspect: `aop/PermissionAspect.java`
- Behavior: Checks userType restrictions (adminOnly, enterpriseOnly, etc.), validates permission codes against `AccountPermissionList`, validates API path permissions against `EntryPermission` table
- Admin users (type=99) bypass all permission code checks

### GlobalExceptionHandler
- File: `exception/GlobalExceptionHandler.java`
- Handles: `BusinessException`, `AuthenticationException`, `AuthorizationException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `AccessDeniedException`, `BadCredentialsException`, and catch-all `Exception`
- Returns: `ApiResponse<Void>` with error code and message mapped to HTTP status

## Error Handling

**Strategy:** Centralized exception handling with categorized error codes

**Error code ranges** (defined in `constant/ErrorCode.java`):
- `1xxx`: Common errors (param, not found, timeout, rate limit, file upload)
- `2xxx`: Auth errors (login, token, permission, captcha, account)
- `3xxx`: Carbon accounting errors (report, data, signature, emission factor)
- `4xxx`: Trade errors (balance, auction, order, P2P)
- `5xxx`: Blockchain errors (connection, chaincode, RSA key operations)
- `6xxx`: ML/AI errors (service unavailable, prediction failed, insufficient data)

**Patterns:**
- Services throw `BusinessException(errorCode, message)` for business rule violations
- `GlobalExceptionHandler` maps error codes to HTTP status codes via `getHttpStatus()` (2xxx -> 401/403, 1xxx -> 400/404)
- Frontend response interceptor shows `ElMessage.error()` for non-200 codes, handles 401 (redirect to login), 403, 404 specifically

## Deployment Architecture

```text
docker-compose.yml defines 6 services:

+----------------+     +----------------+     +----------------+
|   frontend     |     |    backend     |     |   ml-service   |
|   :5173->80    |---->|   :8080        |---->|   :8001        |
|   Nginx        |     |   Spring Boot  |     |   FastAPI      |
+----------------+     +-------+--------+     +----------------+
                               |
                  +------------+------------+
                  |            |            |
           +------v---+ +------v---+ +------v---+
           |  MySQL   | |  Redis   | |  MinIO   |
           |  :3306   | |  :6379   | | :9002/3  |
           +----------+ +----------+ +----------+

Named volumes: mysql-data, redis-data, minio-data
Backend depends_on: mysql (healthy), redis (healthy), minio (started)
Frontend depends_on: backend
ML service: models mounted read-only from host
Health checks: mysql (mysqladmin ping), redis (redis-cli ping), ml-service (HTTP /health)
```

**Additional compose files:**
- `docker-compose.infra.yml`: Infrastructure-only (mysql, redis, minio) for local development
- `docker-compose.fabric.yml`: Hyperledger Fabric network (optional, disabled by default)

**Backend profiles:**
- Default: Local development with `application-dev.yml`
- `docker`: Activated in Docker Compose via `SPRING_PROFILES_ACTIVE: docker`, uses `application-docker.yml`
- `fabric`: Activates `FabricBlockchainService` instead of `MockBlockchainService`

## Architectural Constraints

- **Threading:** Single-threaded request processing per servlet thread; `EnterpriseContextHolder` uses `ThreadLocal` for tenant isolation; `MlServiceClient` blocks servlet thread up to 10s per ML call
- **Global state:** `EnterpriseContextHolder` (ThreadLocal), `SecurityContextHolder` (ThreadLocal), `isRefreshing` flag in frontend request interceptor (module-level boolean)
- **Circular imports:** None detected; strict layering (controller -> service -> repository) prevents cycles
- **Database:** MySQL only, accessed via JPA/Hibernate; `ddl-auto: validate` in production (Flyway manages schema in `db/migration/`)
- **Blockchain:** Fabric integration is optional (`fabric.enabled: false` by default); `MockBlockchainService` provides fallback
- **ML service:** Circuit breaker with fallback ensures backend remains operational when ML service is down; `resilience4j` configured with 50% failure rate threshold, 10-request sliding window
- **Soft delete convention:** All entities extend `BaseEntity` with `deleted` field. Every repository query must include `AndDeletedFalse`. No Hibernate filter for automatic enforcement -- relies on developer discipline.
- **Pagination convention:** Frontend sends 1-based `pageNum`/`pageSize`, interceptor converts to 0-based `page`/`size` for Spring Data. Backend returns `Page<T>`, frontend interceptor transforms to `{ items, total, page, size, totalPages }`.

## Anti-Patterns

### AuditLogAspect Synchronous Save

**What happens:** `AuditLogAspect` saves `OperationLog` to database synchronously in the `finally` block (`operationLogRepository.save()`)
**Why it's wrong:** Database write on every audited request adds latency to the response path; a slow DB slows the user-facing request
**Do this instead:** Use `@Async` or a message queue to persist audit logs asynchronously. The comment in the code says "avoid impacting main flow performance" but the implementation is synchronous.

### Soft-delete without Hibernate filter

**What happens:** Every repository method must manually include `AndDeletedFalse` in query method names. There is no automatic Hibernate `@Filter` to enforce soft-delete globally.
**Why it's wrong:** Easy to forget `AndDeletedFalse` on new repository methods, leaking soft-deleted records.
**Do this instead:** When adding new repository methods, always append `AndDeletedFalse`. Consider adding a Hibernate `@FilterDef`/`@Filter` on `BaseEntity` for automatic enforcement.

### MlServiceClient Blocking Calls

**What happens:** `MlServiceClient` uses `WebClient` (reactive) but calls `.block(Duration.ofSeconds(10))` on every request
**Why it's wrong:** Blocks the servlet thread for up to 10 seconds waiting for ML service response, reducing throughput under load
**Do this instead:** For a servlet-based application this is acceptable as a pragmatic choice, but be aware that under high concurrency the Tomcat thread pool can be exhausted by blocked ML calls

---

*Architecture analysis: 2026-05-18*
