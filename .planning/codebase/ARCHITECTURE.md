# OAISS CHAIN - Architecture

> Last updated: 2026-05-08

## Overview

OAISS CHAIN is a carbon trading and blockchain platform built as a monorepo with a Java/Spring Boot backend and a Vue 3/TypeScript frontend. The system serves five distinct user roles through role-based access control at both the API and UI layers.

## Architectural Pattern

**Layered monolith** following a classic 3-tier pattern:

```
[Vue SPA] --HTTP/JSON--> [Spring Boot API] --JPA--> [MySQL]
                              |                        |
                              +---> [Redis]            +---> [Flyway migrations]
                              +---> [MinIO]
                              +---> [Blockchain service]
```

- **Frontend**: Single-page application (Vue 3 + Vite), role-based routing and menus
- **Backend**: Spring Boot 3.2.5 monolith with AOP cross-cutting concerns
- **Storage**: MySQL 8 (primary), Redis 7 (caching/sessions/locks), MinIO (file storage)
- **Infrastructure**: Docker Compose for all services

## Backend Layers

### Layer 1: Security Filter Chain

Entry point for every authenticated request.

```
HTTP Request
  --> JwtAuthenticationFilter (extracts & validates JWT from Authorization header)
    --> Spring Security FilterChain (CSRF disabled, stateless sessions)
      --> @PreAuthorize on controller methods (role-based authorization)
```

Key components:
- `JwtAuthenticationFilter` - extracts JWT from `Authorization: Bearer` header, loads `JwtUserDetails` into security context
- `JwtTokenProvider` - issues/validates JWTs (access + refresh tokens)
- `SecurityConfig` - configures permit-all endpoints (`/auth/login`, `/auth/register`, `/captcha/**`, `/actuator/health`), all other endpoints require authentication
- `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler` - 401/403 error responses

### Layer 2: AOP Cross-Cutting Concerns

Five custom annotations with corresponding aspects intercept controller/service calls:

| Annotation | Aspect | Purpose |
|---|---|---|
| `@AuditLog` | `AuditLogAspect` | Records operation logs to `OperationLog` table |
| `@RateLimit` | `RateLimitAspect` | Redis-based request throttling |
| `@RequirePermission` | `PermissionAspect` | Additional permission checks beyond `@PreAuthorize` |
| `@DataIsolation` | `DataIsolationAspect` | Tenant-level data isolation (scopes queries to enterprise) |
| `@DistributedLock` | `DistributedLockAspect` | Redis-based distributed locking for concurrent operations |

### Layer 3: Controllers (REST API)

16 controllers under `com.oaiss.chain.controller`, all prefixed with `/api/v1`. Each returns `ApiResponse<T>`.

| Controller | Base Path | Role Access | Domain |
|---|---|---|---|
| `AuthController` | `/auth` | Public + authenticated | Login, register, captcha, token refresh, logout |
| `UserController` | `/users` | Authenticated | User profile CRUD |
| `AdminController` | `/admin` | ADMIN | System user management |
| `CarbonController` | `/carbon` | ENTERPRISE | Carbon report submission |
| `CarbonCoinController` | `/carbon-coin` | ENTERPRISE | Carbon coin accounts, transfers |
| `CarbonNeutralProjectController` | `/carbon-neutral` | ENTERPRISE | Carbon neutral project management |
| `CreditScoreController` | `/credit` | ENTERPRISE | Credit scoring |
| `DoubleAuctionController` | `/auction` | ENTERPRISE | Double auction trading |
| `TradeController` | `/trade` | ENTERPRISE | P2P trading |
| `EmissionController` | `/emission` | ENTERPRISE | Emission data & ratings |
| `BlockchainController` | `/blockchain` | ENTERPRISE | Blockchain explorer |
| `DigitalSignatureController` | `/signature` | ENTERPRISE | RSA digital signatures |
| `FileController` | `/files` | Authenticated | File upload/download via MinIO |
| `SearchController` | `/search` | Authenticated | Cross-entity search |
| `CaptchaController` | `/captcha` | Public | CAPTCHA generation |
| `ThirdPartyController` | `/third-party` | THIRD_PARTY | Third-party monitoring |

### Layer 4: Services (Business Logic)

19 services under `com.oaiss.chain.service`. Services contain the core business logic and are injected into controllers. Key services:

- `AuthService` - authentication, registration, token management
- `CarbonService` - carbon report lifecycle
- `DoubleAuctionService` - auction order matching algorithm
- `TradeService` - P2P trade execution
- `BlockchainService` - blockchain interaction
- `CreditScoreService` - credit score calculation
- `CarbonCoinService` - carbon coin account management
- `RedisLockService` - distributed lock implementation
- `MinioService` - MinIO file operations
- `MetricsService` - application metrics

### Layer 5: Repositories (Data Access)

21 Spring Data JPA repositories under `com.oaiss.chain.repository`. Standard `JpaRepository` interfaces with custom query methods. All entities extend `BaseEntity` which provides:

- `id` (Long, auto-increment)
- `createdAt` (audited)
- `updatedAt` (audited)
- `deleted` (soft delete flag)

### Layer 6: Database

- **ORM**: Spring Data JPA with Hibernate
- **Migrations**: Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`)
- **Profiles**: `application.yml` (default), `application-dev.yml`, `application-test.yml`, `application-docker.yml`

## Frontend Layers

### Request Pipeline

```
Vue Component
  --> API module (src/api/*.ts) --typed request functions
    --> Axios instance (src/api/request.ts) --interceptors
      --> Backend REST API
```

**Axios interceptors** handle:
1. **Request**: Token injection (`Authorization: Bearer`), pagination param renaming (`pageNum/pageSize` -> `page/size`), automatic token refresh on expiry
2. **Response**: `ApiResponse` unwrapping, Spring Data `Page` format transformation to `{ items, total, page, size, totalPages }`, error handling with `ElMessage` notifications, retry on network errors (max 2 attempts)

### Routing & Authorization

- Vue Router with `createWebHistory`
- Route guards in `router.beforeEach`:
  - Public routes (`meta.public: true`) bypass auth
  - Authenticated routes redirect to `/login` if no token
  - Role-based routes check `meta.roles` against stored role
- Role-specific home paths defined in `ROLE_HOME` mapping

### State Management

Single Pinia store (`store/index.ts`) manages:
- Authentication state (tokens, role, user info)
- Role-based home path resolution

### View Structure

Views organized by role domain:
- `views/enterprise/` (11 views) - main business interface
- `views/admin/` (4 views) - system administration
- `views/auditor/` (1 view) - review interface
- `views/authenticator/` (1 view) - certification interface
- `views/third-party/` (1 view) - monitoring interface
- Root views: `Login.vue`, `OfficialHome.vue`, `NotFound.vue`

## Data Flow

### Authentication Flow

```
1. POST /auth/captcha --> returns captcha image + key
2. POST /auth/login {username, password, captchaKey, captchaCode}
   --> AuthService validates credentials + captcha
   --> Returns {accessToken, refreshToken}
3. Frontend stores tokens, sets role in Pinia store
4. Subsequent requests: Authorization: Bearer {accessToken}
5. On 401: POST /auth/refresh with Refresh-Token header
   --> Returns new token pair
```

### Carbon Report Flow

```
1. ENTERPRISE uploads report via CarbonUpload.vue
2. File uploaded to MinIO via FileController
3. CarbonReport entity created with PENDING status
4. REVIEWER sees report in AuditList.vue, approves/rejects
5. On approval: CarbonReport status -> APPROVED, EmissionRating calculated
6. CreditScore updated via CreditScoreService
7. Blockchain hash recorded via BlockchainService
```

### Trading Flow

```
Double Auction:
1. ENTERPRISE places buy/sell order via TradingMarket.vue
2. DoubleAuctionService attempts order matching
3. On match: Transaction created, CarbonCoinAccounts updated
4. BlockchainService records transaction hash

P2P Trade:
1. ENTERPRISE initiates trade via TradingP2P.vue
2. TradeService creates trade request
3. Counterparty accepts/rejects
4. On acceptance: Transaction executed, accounts settled
```

## Key Abstractions

| Abstraction | Location | Purpose |
|---|---|---|
| `ApiResponse<T>` | `dto/ApiResponse.java` | Unified response envelope with `code`, `message`, `data`, `meta` |
| `BaseEntity` | `entity/BaseEntity.java` | Common entity fields (id, timestamps, soft delete) |
| `PageRequest/PageResponse` | `dto/` | Pagination DTOs |
| `BusinessException` | `exception/BusinessException.java` | Base business exception with error codes |
| `GlobalExceptionHandler` | `exception/GlobalExceptionHandler.java` | Maps all exceptions to `ApiResponse` error format |
| `ErrorCode` | `constant/ErrorCode.java` | Numeric error code constants |
| `UserTypeEnum` | `enums/UserTypeEnum.java` | 5 user roles: ENTERPRISE(1), REVIEWER(2), THIRD_PARTY(3), ADMIN(4), AUTHENTICATOR(5) |

## Entry Points

### Backend

- **Application**: `com.oaiss.chain.OaissChainApplication` (Spring Boot main class)
- **Security**: `SecurityConfig.securityFilterChain()` - filter chain configuration
- **API Docs**: `/swagger-ui.html` (SpringDoc OpenAPI 2.5)
- **Health**: `/actuator/health`, `/actuator/prometheus`

### Frontend

- **Dev server**: `npm run dev` (Vite, port 5173)
- **Entry point**: `src/main.ts` -> App -> Router -> Layout -> Views
- **API base**: `VITE_API_BASE_URL` env variable (defaults to backend at port 8080)

## Technology Stack Summary

| Layer | Technology | Version |
|---|---|---|
| Language (BE) | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| ORM | Spring Data JPA / Hibernate | via Spring Boot |
| Database | MySQL | 8 |
| Cache | Redis | 7 |
| File Storage | MinIO | latest |
| Auth | JWT (jjwt) | 0.12.5 |
| Migrations | Flyway | via Spring Boot |
| API Docs | SpringDoc OpenAPI | 2.5 |
| Language (FE) | TypeScript | 6 |
| Framework | Vue | 3.5 |
| Build Tool | Vite | 8 |
| UI Library | Element Plus | 2.13 |
| State | Pinia | 3 |
| Router | Vue Router | 5 |
| Charts | ECharts | 6 |
| i18n | vue-i18n | 11 |
| HTTP Client | Axios | (via request.ts) |
