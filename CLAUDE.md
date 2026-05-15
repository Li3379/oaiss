# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## Project: OAISS CHAIN

### Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8, Redis 7, MinIO, JWT (jjwt 0.12.5), Flyway, SpringDoc OpenAPI 2.5
- **Frontend**: Vue 3.5, TypeScript 6, Vite 8, Element Plus 2.13, Pinia 3, Vue Router 5, ECharts 6, vue-i18n 11
- **Testing**: Backend: JUnit 5 + Testcontainers + JaCoCo (90% line coverage); Frontend: Vitest (happy-dom) + Playwright E2E
- **Infra**: Docker Compose (MySQL, Redis, MinIO, backend, frontend)

### Build & Run

```bash
# Frontend
cd oaiss-chain-frontend && npm run dev       # dev server (port 5173)
cd oaiss-chain-frontend && npm run test      # unit tests
cd oaiss-chain-frontend && npm run build     # production build

# Backend
cd oaiss-chain-backend && mvn spring-boot:run          # dev (port 8080)
cd oaiss-chain-backend && mvn test                     # unit tests
cd oaiss-chain-backend && mvn verify                   # integration tests

# Full stack
docker-compose up
```

### Project Structure

```
oaiss-chain-backend/src/main/java/com/oaiss/chain/
├── controller/    # REST endpoints (18 controllers)
├── service/       # Business logic (21 services)
├── repository/    # Spring Data JPA repositories (21)
├── entity/        # JPA entities (21 entities)
├── dto/           # Request/response DTOs (30+)
├── config/        # Spring config (Security, Redis, MinIO, etc.)
├── annotation/    # Custom annotations (@AuditLog, @RateLimit, etc.)
├── aop/           # AOP aspects
├── security/      # JWT filter, entry point
├── enums/         # Enumerations
├── constant/      # ErrorCode, ErrorMessage
├── exception/     # Custom exceptions
└── util/          # Utilities

oaiss-chain-frontend/src/
├── api/           # Axios API clients (17 modules)
├── types/         # TypeScript type definitions
├── views/         # Page components (enterprise/, admin/, auditor/, third-party/)
├── store/         # Pinia stores
├── router/        # Vue Router with role-based guards
├── i18n/          # Internationalization (zh-CN, en-US)
├── components/    # Shared components
├── layout/        # App layout shell
├── config/        # Menu and image config
└── utils/         # Auth, ECharts utilities
```

### API Conventions

- Base path: `/api/v1`
- Response envelope: `ApiResponse<T>` with `{ code, message, data, meta }`
- Pagination: frontend sends `pageNum`/`pageSize` → converted to `page`/`size`; backend returns Spring Data `Page` → frontend transforms to `{ items, total, page, size, totalPages }`
- Auth: JWT Bearer token + CSRF cookie; roles: ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN
- Role-based routing: `@PreAuthorize` on controllers, Vue Router `meta.roles` on frontend

### Roles

| Role | Chinese | Home Route |
|------|---------|-----------|
| ENTERPRISE | 企业 | `/enterprise/carbon/upload` |
| REVIEWER | 审核员 | `/auditor/audit/list` |
| THIRD_PARTY | 第三方监管 | `/third-party/monitor` |
| ADMIN | 管理员 | `/admin/system/users` (认证功能归入 ADMIN) |

### Naming & Git Conventions

- Java: PascalCase classes, camelCase methods; Lombok `@Data`/`@Builder`/`@RequiredArgsConstructor`
- Frontend: PascalCase `.vue` components, camelCase `.ts` files, kebab-case directories (`third-party/`)
- Tests: `*.test.ts` in colocated `__tests__/` dirs; backend: `*Test.java` / `*IntegrationTest.java`
- DB schema: `db/migration/V1__init_schema.sql` (Flyway, validate-only in prod)
- Git: Conventional commits (`feat|fix|docs|test|refactor|chore(scope): description`)

### Key Domain Concepts

- **Carbon Report (碳报告)**: 企业上报碳排放数据 → 审核员审核 → 状态流转 (DRAFT→PENDING→APPROVED/REJECTED)
- **Trade (碳交易)**: P2P交易 + 双向拍卖(DoubleAuction) 两种模式
- **Carbon Coin (碳币)**: 平台代币，企业间充值/转账
- **Credit Score (信用评分)**: 基于排放数据的信用评级(A-E五级)
- **Carbon Neutral Project (碳中和项目)**: 减排项目注册与认证
- **Digital Signature (数字签名)**: RSA密钥对 + 报告签名/验签
- **Emission Rating (排放评级)**: 年度企业排放评级

### Data Conventions

- **Soft delete**: All entities extend `BaseEntity` with `deleted` field; Repository queries use `...AndDeletedFalse`
- **Audit fields**: `BaseEntity` provides `createdAt`, `updatedAt` (auto-populated by JPA auditing)
- **Pagination**: Frontend `pageNum` (1-based) → Backend `page` (0-based, Spring Data); always use `PageRequest.of(page, size)`

### Cross-Cutting Concerns (AOP)

Custom annotations with corresponding aspects:
- `@AuditLog` → operation logging
- `@RateLimit` → request throttling
- `@RequirePermission` → permission checks
- `@DataIsolation` → tenant data isolation
- `@DistributedLock` → Redis-based distributed locking

### Testing Patterns

- Backend unit tests: `*Test.java` (Surefire)
- Backend integration tests: `*IntegrationTest.java` (Failsafe + Testcontainers)
- Frontend unit tests: `*.test.ts` colocated in `__tests__/` directories (Vitest + happy-dom)
- Frontend E2E: Playwright

### Request Lifecycle

A typical API request flows through:

1. **Frontend**: Vue component → `api/*.ts` (Axios client) → `request.ts` interceptor (JWT injection, pageNum→page conversion, token refresh, SpringPage→TransformedPage conversion) → HTTP `/api/v1/*`
2. **Backend**: `JwtAuthenticationFilter` (token validation, whitelist check, path traversal protection) → `SecurityConfig` (role-based access) → `@RestController` → `@Service` (business logic, `@AuditLog`/`@RateLimit`/`@DataIsolation`/`@DistributedLock`/`@RequirePermission`) → `Repository` (Spring Data JPA) → MySQL
3. **Response**: `ApiResponse<T>` envelope with `{ code, message, data, meta }`; paginated data wrapped in `PageResponse<T>` with `{ list, total, pageNum, pageSize, pages }`

### Environment Variables

Copy `.env.example` to `.env`. Key variables: `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`.

### Where to Look

| I want to... | Look at... |
|--------------|-----------|
| Add a REST endpoint | `controller/` → `service/` → `repository/` → `entity/` |
| Add a frontend page | `views/<role>/` → `router/index.ts` → `config/menu.ts` → `api/*.ts` |
| Add a JPA entity | `entity/` → `repository/` → Flyway migration in `db/migration/` |
| Add a custom annotation | `annotation/` → `aop/` (matching Aspect) |
| Change auth behavior | `security/JwtAuthenticationFilter` → `security/JwtTokenProvider` → `config/SecurityConfig` |
| Change API response format | `dto/PageResponse` (backend) → `types/api.ts` + `api/request.ts` interceptor (frontend) |
| Add i18n translations | `i18n/locales/zh-CN.ts` + `i18n/locales/en-US.ts` |
| Debug pagination issues | Frontend sends `pageNum/pageSize` → `request.ts` converts to `page/size` → backend returns Spring `Page` → `request.ts` transforms to `{ items, total, page, size, totalPages }` |
