# Onboarding Guide: OAISS CHAIN

## Overview

OAISS CHAIN（双碳链动）是一个基于区块链的可信碳核算与交易平台。企业提交碳排放报告，审核员/认证员审核，第三方监管，管理员管理全系统。支持碳币交易、双向拍卖、P2P交易、碳中和项目等核心业务。

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend Language | Java | 17 |
| Backend Framework | Spring Boot | 3.2.5 |
| ORM | Spring Data JPA (Hibernate) | - |
| Database | MySQL | 8.0 |
| Cache | Redis | 7 (Lettuce) |
| Object Storage | MinIO | latest |
| Auth | JWT (jjwt) | 0.12.5 |
| DB Migration | Flyway | - |
| API Docs | SpringDoc OpenAPI | 2.5 |
| Build | Maven | - |
| Frontend Language | TypeScript | 6 |
| Frontend Framework | Vue | 3.5 |
| UI Library | Element Plus | 2.13 |
| State Management | Pinia | 3 |
| Router | Vue Router | 5 |
| Charts | ECharts | 6 |
| i18n | vue-i18n | 11 |
| Build Tool | Vite | 8 |
| Unit Testing (FE) | Vitest (happy-dom) | 4.1 |
| E2E Testing | Playwright | 1.59 |
| Infra | Docker Compose | 3.8 |

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Vue 3 SPA  │────▶│  Spring Boot API │────▶│   MySQL 8   │
│  (Vite/TS)  │◀────│  /api/v1/*       │────▶│   Redis 7   │
└─────────────┘     └──────────────────┘────▶│   MinIO      │
                                            └─────────────┘
```

- **Monolithic backend + SPA frontend**，通过 Docker Compose 编排
- **REST API** 风格，JWT Bearer Token 认证 + CSRF cookie
- 前端开发时代理 `/api` 到 `localhost:8080`（Vite proxy）

## User Roles

| Role | Chinese | Home Route | Description |
|------|---------|-----------|-------------|
| `ENTERPRISE` | 企业 | `/enterprise/carbon/upload` | 提交碳报告、交易、查看碳币 |
| `REVIEWER` | 审核员 | `/auditor/audit/list` | 审核碳排放数据 |
| `AUTHENTICATOR` | 认证员 | `/authenticator/verify/list` | 数字签名认证 |
| `THIRD_PARTY` | 第三方 | `/third-party/monitor` | 监管面板 |
| `ADMIN` | 管理员 | `/admin/system/users` | 用户管理、系统配置 |

## Key Entry Points

| What | Where |
|------|-------|
| Backend main | `oaiss-chain-backend/.../OaissChainApplication.java` |
| API base path | `server.servlet.context-path: /api/v1` (application.yml) |
| Security config | `config/SecurityConfig.java` |
| JWT filter | `security/JwtAuthenticationFilter.java` |
| Frontend main | `oaiss-chain-frontend/src/main.ts` |
| Router | `src/router/index.ts` (role-based guards) |
| API client | `src/api/request.ts` (axios + token refresh) |
| Auth utils | `src/utils/auth.ts` (token storage + JWT parse) |
| Store | `src/store/index.ts` (Pinia, user state from JWT) |
| DB schema | `src/main/resources/db/migration/V1__init_schema.sql` |

## Directory Map

### Backend (`oaiss-chain-backend/src/main/java/com/oaiss/chain/`)

| Directory | Purpose | Count |
|-----------|---------|-------|
| `controller/` | REST endpoints | 16 |
| `service/` | Business logic | 23 |
| `repository/` | Spring Data JPA interfaces | 22 |
| `entity/` | JPA entities (Lombok `@Data`) | 21 |
| `dto/` | Request/response DTOs | 30+ |
| `config/` | Spring configs (Security, Redis, MinIO, Swagger, etc.) | 14 |
| `annotation/` | Custom annotations | 5 |
| `aop/` | AOP aspects for annotations | 5 |
| `security/` | JWT filter, token provider, handlers | 6 |
| `enums/` | Business enumerations | 8 |
| `constant/` | ErrorCode, ErrorMessage | 2 |
| `exception/` | Custom exceptions + GlobalExceptionHandler | 7 |
| `util/` | Utility classes | 4 |

### Frontend (`oaiss-chain-frontend/src/`)

| Directory | Purpose | Count |
|-----------|---------|-------|
| `api/` | Axios API clients (per domain) | 17 |
| `types/` | TypeScript type definitions | 16 |
| `views/` | Page components (by role) | 20+ |
| `views/__tests__/` | Colocated Vitest unit tests | 20+ |
| `store/` | Pinia store (single `app` store) | 1 |
| `router/` | Vue Router with role guards | 1 |
| `i18n/` | zh-CN + en-US locales | 3 |
| `components/` | Shared components | 3 |
| `layout/` | App shell (sidebar + header) | 1 |
| `config/` | Menu config, image config | 2 |
| `utils/` | Auth, ECharts utilities | 2 |

## Request Lifecycle

典型请求流程（以创建碳报告为例）：

```
1. Vue component calls api/carbon.ts → axios request
2. request.ts interceptor: attach JWT Bearer token
   ├─ token expired? → auto-refresh via /auth/refresh
   └─ pageNum/pageSize → page/size param conversion
3. Vite proxy → http://localhost:8080/api/v1
4. Spring Security filter chain:
   JwtAuthenticationFilter → validate token → set SecurityContext
5. @PreAuthorize("hasRole('ENTERPRISE')") → role check
6. CarbonController.createReport()
   └─ CarbonService → business logic
      └─ CarbonReportRepository → Spring Data JPA → MySQL
7. ApiResponse<T> envelope wraps response
8. request.ts response interceptor:
   ├─ Spring Data Page → { items, total, page, size, totalPages }
   └─ error codes → ElMessage.error()
```

## Cross-Cutting Concerns (AOP)

5 个自定义注解 + 对应切面：

| Annotation | Purpose | Mechanism |
|------------|---------|-----------|
| `@AuditLog` | 操作审计日志 | AOP 记录到 OperationLog 表 |
| `@RateLimit` | 请求限流 | Redis 计数器 |
| `@RequirePermission` | 细粒度权限 | 权限表查询 |
| `@DataIsolation` | 租户数据隔离 | 企业 ID 过滤 |
| `@DistributedLock` | 分布式锁 | Redis SETNX |

## Conventions

### Naming

| What | Pattern | Example |
|------|---------|---------|
| Java classes | PascalCase | `CarbonReportService` |
| Java methods/fields | camelCase | `createReport()` |
| API paths | kebab-case | `/carbon-coin/account` |
| Vue components | PascalCase | `CarbonUpload.vue` |
| TS files | camelCase | `carbonCoin.ts` |
| View directories | kebab-case | `views/enterprise/`, `views/third-party/` |
| Test files | `*.test.ts` | `Login.test.ts` |
| Test location | `__tests__/` colocated | `views/__tests__/Login.test.ts` |

### Code Patterns

- **Backend**: Lombok `@Data`/`@Builder`/`@RequiredArgsConstructor` on entities/DTOs
- **Backend**: Constructor injection via `@RequiredArgsConstructor` (no `@Autowired`)
- **Backend**: `ApiResponse<T>` static factory methods for all responses
- **Backend**: `@PreAuthorize` for role-based access on controller methods
- **Backend**: Swagger annotations on all controller methods
- **Frontend**: Pinia Options API store (single `app` store)
- **Frontend**: Lazy-loaded routes via `() => import()`
- **Frontend**: Token stored in localStorage/sessionStorage + memory cache
- **Frontend**: JWT payload parsed client-side for role/userId extraction

### Git

- **Branch naming**: `claude/<adjective>-<name>-<hash>` (AI-generated)
- **Commit format**: Conventional commits: `type(scope): description`
  - Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`, `ci`
- **PR workflow**: Squash merge to main

## Common Tasks

| Task | Command |
|------|---------|
| Start backend dev server | `cd oaiss-chain-backend && mvn spring-boot:run` |
| Start frontend dev server | `cd oaiss-chain-frontend && npm run dev` |
| Run backend tests | `cd oaiss-chain-backend && mvn test` |
| Run backend integration tests | `cd oaiss-chain-backend && mvn verify` |
| Run frontend unit tests | `cd oaiss-chain-frontend && npm run test` |
| Run frontend E2E tests | `cd oaiss-chain-frontend && npm run test:e2e` |
| Build frontend for production | `cd oaiss-chain-frontend && npm run build` |
| Start full stack | `docker-compose up` |
| View Swagger API docs | `http://localhost:8080/api/v1/swagger-ui.html` |
| View MinIO console | `http://localhost:9003` |

## Environment Setup

1. Copy `.env.example` to `.env` and fill values
2. Key variables: `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
3. Backend runs on port **8080**, frontend dev on **5173**
4. MySQL on **3306**, Redis on **6379**, MinIO API on **9002**, console on **9003**

## Where to Look

| I want to... | Look at... |
|--------------|-----------|
| Add a REST endpoint | `controller/` → matching service → repository |
| Add a new entity/table | `entity/` + Flyway migration in `db/migration/` |
| Change auth behavior | `security/` + `config/SecurityConfig.java` |
| Add a frontend page | `views/<role>/` + add route in `router/index.ts` + add API in `api/` |
| Add a new API client | `api/<domain>.ts` + `types/<domain>.ts` |
| Fix a role/permission issue | Backend: `@PreAuthorize` on controller; Frontend: `meta.roles` in router |
| Add i18n strings | `i18n/locales/zh-CN.ts` + `en-US.ts` |
| Debug JWT issues | `security/JwtTokenProvider.java` + `utils/auth.ts` |
| Understand DB schema | `db/migration/V1__init_schema.sql` |
| Add a custom annotation | `annotation/` + matching aspect in `aop/` |
| Check API docs | Swagger UI at `/api/v1/swagger-ui.html` |
