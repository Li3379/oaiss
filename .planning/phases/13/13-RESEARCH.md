# Phase 13: Concurrency Safety & Credential Hardening - Research

**Researched:** 2026-05-19
**Domain:** Java/Spring Boot concurrency (distributed locking, optimistic locking, transaction boundaries) + security hardening (credential externalization, authorization annotations, actuator protection)
**Confidence:** HIGH

## Summary

Phase 13 addresses two critical categories: concurrency safety in financial operations (CON-01 through CON-03) and credential/authorization hardening (SEC-07 through SEC-13). The codebase already has a mature `@DistributedLock` annotation with a `DistributedLockAspect` and `RedisLockService` that is used in 3 other services (DigitalSignatureService, EnterpriseAdmissionService, ReviewerQualificationService), providing a proven pattern to reuse. However, the DoubleAuctionService uses JVM-level `synchronized` instead, which fails in multi-instance deployments. No `@Version` optimistic locking exists on any entity. The transaction boundary in `executeMatching()` has the lock INSIDE the transaction (synchronized on the method which also has `@Transactional`), meaning the lock is released before the transaction commits, creating a race window.

On the security side, docker-compose files have partially externalized credentials but still fall back to hardcoded passwords (including `minioadmin`). The `application.yml` defaults `DB_PASSWORD` to `123456`. FileController lacks `@PreAuthorize` and has a dangerous X-User-Id/X-User-Type header fallback that bypasses JWT authentication. SearchController lacks `@PreAuthorize`. The Prometheus actuator endpoint is currently `permitAll()` in SecurityConfig.

**Primary recommendation:** Replace `synchronized` with `@DistributedLock` using the existing annotation infrastructure, add `@Version` to financial entities with a Flyway migration, restructure `executeMatching()` so the lock wraps the transaction (lock outside, transaction inside), externalize all remaining hardcoded credentials, add `@PreAuthorize` to FileController and SearchController following existing patterns, remove header fallback, and restrict the Prometheus endpoint to authenticated internal access.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Distributed locking (CON-01) | API / Backend | — | Lock acquisition is a service-layer concern via AOP |
| Optimistic locking (CON-02) | Database / Storage | API / Backend | @Version is a JPA/database concern, caught at service layer |
| Transaction boundary refactor (CON-03) | API / Backend | — | Service method restructuring, no UI/infra change |
| Credential externalization (SEC-07/08/09) | CDN / Static | Database / Storage | Docker/Config files, no application logic change |
| Controller authorization (SEC-10/11/12) | API / Backend | — | @PreAuthorize is a controller-layer concern |
| Actuator protection (SEC-13) | API / Backend | Frontend Server (SSR) | SecurityConfig changes affect all endpoints |

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CON-01 | DoubleAuctionService synchronized -> @DistributedLock | Existing @DistributedLock annotation + RedisLockService proven in 3 other services |
| CON-02 | Financial entities add @Version optimistic lock | No @Version exists anywhere; Flyway migration needed; Spring Data JPA native support |
| CON-03 | executeMatching() transaction boundary refactor | Lock-inside-transaction antipattern identified; existing pattern in DigitalSignatureService shows lock-before-tx |
| SEC-07 | docker-compose.yml hardcoded passwords externalized | 3 compose files audited; partial env var usage already exists; fallback defaults are the problem |
| SEC-08 | application.yml remove DB_PASSWORD:123456 default | Line 17 of application.yml confirmed; env-only pattern already used for JWT_SECRET |
| SEC-09 | MinIO remove minioadmin default credentials | Found in docker-compose.yml, docker-compose.infra.yml, .env, .env.example, application-local.yml |
| SEC-10 | FileController add @PreAuthorize role annotations | FileController has 10 endpoints with zero @PreAuthorize; 15 other controllers use it extensively |
| SEC-11 | FileController remove X-User-Id/X-User-Type header fallback | Lines 372-386 of FileController; header fallback allows JWT bypass |
| SEC-12 | SearchController add @PreAuthorize role annotations | SearchController has 3 endpoints with zero @PreAuthorize |
| SEC-13 | Prometheus /actuator/prometheus endpoint add authentication | SecurityConfig line 80: `.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()` |
</phase_requirements>

---

## Requirement Group: CON (Concurrency Safety)

### CON-01: DoubleAuctionService synchronized -> @DistributedLock

**Current State:**
- File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`, line 146
- Method signature: `@Transactional public synchronized List<MatchingResultResponse> executeMatching()`
- Only one synchronized method in the entire codebase (verified by grep)
- Test at `DoubleAuctionServiceTest.java` line 353-359 explicitly verifies the `synchronized` modifier exists

**Required Change:**
1. Remove `synchronized` keyword from `executeMatching()`
2. Add `@DistributedLock(key = "'auction:matching'", expireTime = 30, waitTime = 0)` annotation
3. Update the test that asserts `synchronized` modifier exists (it will need to verify @DistributedLock instead)
4. Ensure `@DistributedLock` is placed BEFORE `@Transactional` on the method (so lock is acquired before transaction starts -- but see CON-03 for the full boundary fix)

**Dependencies:**
- `RedisLockService` -- already exists and is production-ready
- `DistributedLockAspect` -- already exists, uses `@Around` advice
- Redis must be available (it is -- healthcheck in docker-compose)

**Risks:**
- `waitTime = 0` means concurrent match requests will immediately fail with `BusinessException(OPERATION_IN_PROGRESS)`. This is acceptable for admin-triggered matching but must be documented.
- If Redis is down, the lock cannot be acquired and matching will fail. This is actually safer than `synchronized` which would silently allow concurrent execution in a multi-instance scenario.
- The `@DistributedLock` aspect currently does NOT have `@Order` specified. Per AGENTS.md recommendation, `@DistributedLock` should be outermost. Need to verify aspect execution order with `@Transactional`.

**Reusable Patterns:**
- `DigitalSignatureService.java` line 66-67: `@DistributedLock(key = "'keypair:generate:' + #userId", expireTime = 10)` followed by `@Transactional` -- exact pattern to follow
- `EnterpriseAdmissionService.java` line 47-48: same pattern
- `ReviewerQualificationService.java` line 50-51: same pattern

### CON-02: Financial entities add @Version optimistic lock

**Current State:**
- NO `@Version` annotation exists anywhere in the codebase (verified by grep)
- Three target entities:
  - `Enterprise.java`: Has `carbonQuota`, `carbonUsed`, `carbonTradable` (DECIMAL(15,4)) fields that are mutated during trades and matching
  - `CarbonCoinAccount.java`: Has `balance`, `totalRecharged`, `totalSpent` (DECIMAL(15,2)) fields mutated during carbon coin operations
  - `AuctionOrder.java`: Has `matchedQuantity`, `status`, `settlementPrice` fields mutated during matching
- None of the database tables have a `version` column (verified in V1__init_schema.sql)
- BaseEntity does NOT include a version field
- No `ObjectOptimisticLockingFailureException` handling exists anywhere

**Required Change:**
1. Add `@Version` field to each entity:
   ```java
   @Version
   private Long version;
   ```
2. Create Flyway migration `V5__add_optimistic_locking.sql`:
   ```sql
   ALTER TABLE enterprise ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ALTER TABLE carbon_coin_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ALTER TABLE auction_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ```
3. Add `OptimisticLockingFailureException` handling in services that update these entities (DoubleAuctionService, TradeService, CarbonCoinService, EnterpriseService)
4. The `@Version` field should NOT be included in `@Builder.Default` since JPA manages it automatically

**Dependencies:**
- Spring Data JPA provides native `@Version` support -- no additional library needed
- Flyway is already configured and active
- Next migration version is V5 (V4__enterprise_admission.sql is the latest)

**Risks:**
- Adding `NOT NULL DEFAULT 0` column to existing tables with data: MySQL supports this with instant ALTER for small tables, but large tables could lock. The enterprise/auction_order tables should be small enough.
- All existing tests that build entities with `@Builder` will now need to either (a) not set the version field, or (b) set it to null/0. Since `@Builder` generates an all-args constructor, adding a new field will break existing builder calls unless `@Builder.Default` is used. **This is the primary risk -- test compilation breakage.**
- The `BaseEntity` should NOT get `@Version` -- only the 3 financial entities need it. Adding it to all 23 entities would be over-engineering and would break many tests.
- `@Data` (Lombok) on `BaseEntity` generates `equals()`/`hashCode()` that includes all fields. Adding `version` to entities could cause issues with JPA detached entity merging if equals/hashCode changes after version increment. The entities use `@Getter`/`@Setter` (not `@Data`), so this is not a concern for Enterprise, CarbonCoinAccount, or AuctionOrder.

**Reusable Patterns:**
- Spring Data JPA `@Version` with `Long` type is the standard approach [CITED: https://docs.spring.io/spring-data/jpa/reference/#core.domain.optimistic-locking]
- `OptimisticLockingFailureException` is a Spring DAO exception that wraps Hibernate's `StaleObjectStateException`

### CON-03: executeMatching() transaction boundary refactor

**Current State:**
- DoubleAuctionService.executeMatching() at line 145-146:
  ```java
  @Transactional
  public synchronized List<MatchingResultResponse> executeMatching()
  ```
- With `synchronized`, the execution order is:
  1. Thread acquires JVM monitor (synchronized)
  2. Spring proxy starts transaction (@Transactional)
  3. Business logic executes
  4. Spring proxy commits transaction
  5. **Thread releases JVM monitor (synchronized)**
  
  The lock is released AFTER the transaction commits, which is correct for `synchronized`. However, when switching to `@DistributedLock`:
  - If `@DistributedLock` is on the same method as `@Transactional`, the AOP proxy order determines behavior
  - Currently, neither `DistributedLockAspect` nor the transaction interceptor has explicit `@Order`
  - Default behavior: `@Transactional` interceptor runs at `Ordered.LOWEST_PRECEDENCE` (outermost), which means lock would be INSIDE transaction -- a race condition

**Required Change:**
The correct order is: **Lock first, then transaction**. This ensures:
1. Lock acquired
2. Transaction started
3. Business logic
4. Transaction committed
5. Lock released

This can be achieved by:
- **Option A (Recommended):** Add `@Order(Ordered.HIGHEST_PRECEDENCE)` to `DistributedLockAspect` so it runs outermost (before @Transactional). This is already documented in AGENTS.md line 82: "Recommended order: `@DistributedLock` (outermost) > `@RateLimit` > `@DataIsolation` > `@RequirePermission` > `@AuditLog` (innermost)."
- **Option B:** Separate into two methods: an outer non-transactional method with `@DistributedLock` that calls an inner `@Transactional` method. This is more explicit but creates a public API surface that must be guarded.

**Dependencies:**
- `@EnableTransactionManagement(order = ...)`) may need adjustment if custom order is set
- Spring's default `@Transactional` interceptor order is `Integer.MAX_VALUE` (lowest precedence / outermost)

**Risks:**
- If aspect order is wrong, the lock will be released before the transaction commits, allowing another thread to acquire the lock and read stale data before the first thread's commit is visible. This is the exact race condition we're trying to prevent.
- The existing 3 usages of `@DistributedLock` (DigitalSignatureService, EnterpriseAdmissionService, ReviewerQualificationService) are also affected by this ordering issue. Fixing the order on `DistributedLockAspect` will fix them all.
- **Test verification:** The existing test `DoubleAuctionServiceTest` line 353-359 checks for `synchronized` modifier. This test must be rewritten to verify `@DistributedLock` annotation presence instead.

**Reusable Patterns:**
- Spring AOP `@Order` annotation on aspects
- AGENTS.md already documents the intended order: `@DistributedLock` outermost

---

## Requirement Group: SEC (Security Hardening)

### SEC-07: docker-compose.yml hardcoded passwords externalized

**Current State:**
Three docker-compose files exist:

1. **`docker-compose.yml`** (root, production-like):
   - `MYSQL_ROOT_PASSWORD: Oa1ssDb2026Pr0dSecureP4ss` (line 6) -- HARDCODED, not env var
   - `DB_PASSWORD: Oa1ssDb2026Pr0dSecureP4ss` (line 59) -- HARDCODED
   - `JWT_SECRET: Oa1ss2026Pr0dS3cur3JwtK3yF0rHmacSha256S1gn1ngD0ck3r` (line 63) -- HARDCODED
   - `REDIS_PASSWORD: ${REDIS_PASSWORD:-oaiss_redis_dev_2026}` (line 62) -- partially externalized, fallback is a real password
   - `MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY:-minioadmin}` (line 65) -- partially externalized, insecure fallback
   - `MINIO_SECRET_KEY: ${MINIO_SECRET_KEY:-minioadmin}` (line 66) -- partially externalized, insecure fallback

2. **`docker-compose.infra.yml`** (infra-only):
   - Same MYSQL_ROOT_PASSWORD hardcoded (line 6)
   - Same Redis/MinIO fallback patterns
   - MinIO credentials: `${MINIO_ACCESS_KEY:-minioadmin}` / `${MINIO_SECRET_KEY:-minioadmin}` (lines 39-40)

3. **`oaiss-chain-backend/docker-compose.yml`** (backend's own):
   - Already uses `${MYSQL_ROOT_PASSWORD}`, `${MYSQL_PASSWORD}`, `${JWT_SECRET}` properly (lines 14-15, 170)
   - Grafana password: `${GRAFANA_ADMIN_PASSWORD}` (line 79) -- properly externalized, no fallback
   - Redis: no password (line 40) -- different from production config

**Required Change:**
1. `docker-compose.yml`: Replace all hardcoded passwords with `${ENV_VAR}` (no defaults):
   - `MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}`
   - `DB_PASSWORD: ${DB_PASSWORD}`
   - `JWT_SECRET: ${JWT_SECRET}`
   - `REDIS_PASSWORD: ${REDIS_PASSWORD}` (remove fallback)
   - `MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}` (remove `:-minioadmin` fallback)
   - `MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}` (remove `:-minioadmin` fallback)

2. `docker-compose.infra.yml`: Same treatment for MySQL, Redis, MinIO

3. Update `.env.example` (root) to document that MinIO default credentials should NOT be used in production -- replace `minioadmin` with `change_me_minio_access` / `change_me_minio_secret`

**Dependencies:**
- Docker Compose will fail to start if required env vars are missing and no default is provided. This is INTENTIONAL -- fail-loud is better than silent insecure defaults.
- The `.env` file (root) already has production-like passwords; these will continue to work.
- CI/CD pipelines must be updated to inject these variables.

**Risks:**
- Removing defaults from docker-compose.yml means `docker-compose up` will fail if `.env` is missing. This is acceptable -- it forces operators to set credentials.
- The `.env.example` file is tracked in git and currently contains `minioadmin`. Changing it to placeholder values is a documentation change, not a security fix, but it sets expectations correctly.
- The `oaiss-chain-backend/docker-compose.yml` already uses proper env vars without defaults -- this is the model to follow.

### SEC-08: application.yml remove DB_PASSWORD:123456 default

**Current State:**
- `application.yml` line 17: `password: ${DB_PASSWORD:123456}` -- insecure default
- `application-local.yml` line 9: `password: ${DB_PASSWORD:Oa1ssDb2026Pr0dSecureP4ss}` -- real password as default
- `application-dev.yml` line 11: `password: ${DB_PASSWORD}` -- no default (GOOD)
- `application-docker.yml` line 9: `password: ${DB_PASSWORD}` -- no default (GOOD)

**Required Change:**
1. Change `application.yml` line 17 from `password: ${DB_PASSWORD:123456}` to `password: ${DB_PASSWORD}`
2. Change `application-local.yml` line 9 from `password: ${DB_PASSWORD:Oa1ssDb2026Pr0dSecureP4ss}` to `password: ${DB_PASSWORD}`
3. Similarly, `application-local.yml` line 15 has `password: ${REDIS_PASSWORD:oaiss_redis_dev_2026}` -- remove the default
4. `application-local.yml` line 23: `JWT_SECRET: ${JWT_SECRET:dGVzdC1qd3Qtc2VjcmV0LWZvci1sb2NhbC1kZXYtZW52}` -- remove default (base64 test key)

**Dependencies:**
- The `.env` file already has `DB_PASSWORD=Oa1ssDb2026Pr0dSecureP4ss` for local development
- Spring Boot will fail to start if `DB_PASSWORD` is not set. This is intentional.

**Risks:**
- Developers who run the app without `.env` will get a startup failure instead of silently connecting with `123456`. This is a breaking change for the developer experience, but it's the correct security posture.
- `application-local.yml` removing defaults means the local dev profile REQUIRES `.env` to be present. This is already documented in CLAUDE.md: "Copy `.env.example` to `.env`."

### SEC-09: MinIO remove minioadmin default credentials

**Current State:**
MinIO `minioadmin/minioadmin` is the well-known default that ships with MinIO. Found in:
- `docker-compose.yml` lines 39-40: `${MINIO_ACCESS_KEY:-minioadmin}` / `${MINIO_SECRET_KEY:-minioadmin}`
- `docker-compose.infra.yml` lines 39-40: same pattern
- `.env` (root) lines 26-27: `MINIO_ACCESS_KEY=minioadmin` / `MINIO_SECRET_KEY=minioadmin`
- `.env.example` (root) lines 26-27: `MINIO_ACCESS_KEY=minioadmin` / `MINIO_SECRET_KEY=minioadmin`
- `application-local.yml` lines 19-20: `${MINIO_ACCESS_KEY:minioadmin}` / `${MINIO_SECRET_KEY:minioadmin}`

**Required Change:**
1. Remove `:-minioadmin` fallbacks from all docker-compose files (covered by SEC-07)
2. Remove `:minioadmin` defaults from `application-local.yml`
3. Update `.env.example` to use `change_me_minio_access` / `change_me_minio_secret`
4. **Do NOT change `.env`** (it's the developer's local file, may contain real credentials)
5. Update `MinioConfig.java` defaults if any (currently no defaults in code, only in YAML)

**Dependencies:**
- MinIO will fail to start if `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` are not set in docker-compose. The `.env` file provides them.

**Risks:**
- If the developer's `.env` still has `minioadmin`, MinIO will work but log a warning. The real fix is the developer changing their `.env` values. We can add a startup check that logs a warning if MinIO credentials are the default.

### SEC-10: FileController add @PreAuthorize role annotations

**Current State:**
- `FileController.java` has 10 endpoints, NONE with `@PreAuthorize`:
  - `POST /file/upload` -- any authenticated user can upload
  - `POST /file/upload/batch` -- any authenticated user can batch upload
  - `GET /file/download` -- any authenticated user can download any file
  - `DELETE /file` -- has custom `checkDeletePermission()` logic but no @PreAuthorize
  - `DELETE /file/batch` -- same custom logic
  - `GET /file/info` -- any authenticated user can view any file's metadata
  - `GET /file/exists` -- any authenticated user can check any file
  - `GET /file/presigned-url` -- any authenticated user can get presigned URLs
  - `GET /file/presigned-upload-url` -- any authenticated user can get upload URLs
  - `GET /file/list` -- any authenticated user can list all files
  - `POST /file/copy` -- any authenticated user can copy any file

- **Comparison:** 15 other controllers use `@PreAuthorize` extensively. The pattern is:
  - `@PreAuthorize("hasRole('ENTERPRISE')")` for enterprise-only
  - `@PreAuthorize("hasRole('ADMIN')")` for admin-only
  - `@PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE', 'REVIEWER', 'THIRD_PARTY')")` for all roles

**Required Change:**
Add `@PreAuthorize` to each endpoint based on business requirements:
- Upload endpoints: `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` -- enterprises upload reports/certificates, admins can upload too
- Download/info/exists: `@PreAuthorize("isAuthenticated()")` -- any authenticated user can read (but see SEC-11 for identity enforcement)
- Delete: Already has `checkDeletePermission()` logic; add `@PreAuthorize("isAuthenticated()")` as a baseline (the custom logic does the fine-grained check)
- Presigned URLs: `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` -- presigned URLs are powerful, restrict them
- List files: `@PreAuthorize("hasRole('ADMIN')")` -- listing all files is an admin operation
- Copy: `@PreAuthorize("hasRole('ADMIN')")` -- copying files is an admin operation

**Dependencies:**
- `@EnableMethodSecurity` is already on `SecurityConfig` (line 38)
- `@PreAuthorize` import is from `org.springframework.security.access.prepost.PreAuthorize`

**Risks:**
- Adding `@PreAuthorize` to FileController may break existing frontend API calls if the frontend sends requests without proper JWT roles. Must verify frontend role-based access matches.
- The `checkDeletePermission()` method currently falls back to X-User-Id/X-User-Type headers (SEC-11). After removing the header fallback, the `@AuthenticationPrincipal` will be the sole source of identity, and `@PreAuthorize` will enforce role requirements before the method body executes.

### SEC-11: FileController remove X-User-Id/X-User-Type header fallback

**Current State:**
- `FileController.java` lines 372-386:
  ```java
  private Long resolveUserId(JwtUserDetails currentUser, HttpServletRequest request) {
      if (currentUser != null) {
          return currentUser.getUserId();
      }
      String header = request.getHeader("X-User-Id");
      return header != null ? Long.parseLong(header) : null;
  }

  private Integer resolveUserType(JwtUserDetails currentUser, HttpServletRequest request) {
      if (currentUser != null) {
          return currentUser.getUserType();
      }
      String header = request.getHeader("X-User-Type");
      return header != null ? Integer.parseInt(header) : null;
  }
  ```
- These methods are called by `checkDeletePermission()` (line 350-351)
- The `X-User-Id` and `X-User-Type` headers are NOT in the CORS allowed headers list (`SecurityConfig` line 119: `List.of("Authorization", "Content-Type", "Refresh-Token")`), meaning a browser-based attacker cannot set them. However, a non-browser client (curl, Postman, another service) CAN set them.
- The fallback only applies when `currentUser` is null (i.e., when `@AuthenticationPrincipal` injection fails). With `@PreAuthorize("isAuthenticated()")` on the endpoint, unauthenticated requests are rejected before reaching this code.

**Required Change:**
1. Remove the header fallback from both `resolveUserId()` and `resolveUserType()` methods
2. Simplify to:
   ```java
   private Long resolveUserId(JwtUserDetails currentUser) {
       return currentUser.getUserId();
   }
   private Integer resolveUserType(JwtUserDetails currentUser) {
       return currentUser.getUserType();
   }
   ```
3. Remove `HttpServletRequest request` parameter from `checkDeletePermission()` and the delete endpoints
4. Update delete endpoint signatures to no longer accept `HttpServletRequest`

**Dependencies:**
- With `@PreAuthorize("isAuthenticated()")` on delete endpoints, `currentUser` will never be null when the method body executes (Spring Security guarantees it)
- If `currentUser` is null despite `@PreAuthorize`, it indicates a Spring Security misconfiguration, and throwing NPE is the correct behavior (fail-closed)

**Risks:**
- Low risk. The header fallback was a workaround for when JWT authentication was not enforced. With `@PreAuthorize`, it's unreachable code.

### SEC-12: SearchController add @PreAuthorize role annotations

**Current State:**
- `SearchController.java` has 3 endpoints, NONE with `@PreAuthorize`:
  - `GET /search/reports` -- search carbon reports, any authenticated user
  - `GET /search/trades` -- search trade records, any authenticated user
  - `GET /search/market-overview` -- market overview, any authenticated user
- Each endpoint has `@SecurityRequirement(name = "Bearer Authentication")` in OpenAPI annotations but no Spring Security enforcement

**Required Change:**
Add `@PreAuthorize` based on business logic:
- `/search/reports`: `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")` -- all roles can search reports (enterprises see their own, reviewers see pending, third-party sees all)
- `/search/trades`: `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")` -- similar to reports
- `/search/market-overview`: `@PreAuthorize("isAuthenticated()")` -- market overview is public to all authenticated users (no sensitive data)

Note: The `SearchService` should handle data isolation (enterprises only see their own data), which is a separate concern from authorization.

**Dependencies:**
- Same as SEC-10

**Risks:**
- Low risk. SearchController endpoints already require JWT authentication (SecurityConfig `anyRequest().authenticated()`). Adding `@PreAuthorize` just makes the role requirements explicit.

### SEC-13: Prometheus /actuator/prometheus endpoint add authentication

**Current State:**
- `SecurityConfig.java` line 80:
  ```java
  .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
  ```
- Both `/actuator/health` and `/actuator/prometheus` are currently open (no authentication required)
- `application.yml` exposes: `health,info,metrics,prometheus` (line 93)
- Prometheus server (in backend's docker-compose.yml) scrapes from the backend at `metrics_path: '/api/actuator/prometheus'` (line 27 of prometheus.yml)
- `JwtAuthenticationFilter` whitelists `/actuator` paths (line 54), meaning the JWT filter skips all actuator endpoints entirely

**Required Change:**
1. Remove `/actuator/prometheus` from the `permitAll()` matcher in SecurityConfig:
   ```java
   .requestMatchers("/actuator/health").permitAll()  // keep health open for k8s probes
   .requestMatchers("/actuator/prometheus").authenticated()  // require auth for prometheus
   ```
2. Update `JwtAuthenticationFilter` whitelist: remove `/actuator` prefix match and only whitelist `/actuator/health`
3. Configure Prometheus scraper to include auth credentials (basic auth or bearer token) in `prometheus.yml`
4. Alternatively, restrict prometheus endpoint to internal network only via `management.server.port` (separate management port)

**Dependencies:**
- Prometheus scrape config (`monitoring/prometheus/prometheus.yml`) must be updated to include auth headers
- The `application-docker.yml` already restricts actuator details (`show-details: when-authorized`), which is consistent with this change

**Risks:**
- **Prometheus scraping will break** until the scraper config is updated with credentials. This is the primary operational risk.
- Option: Use a separate management port (`management.server.port=8081`) that is only accessible from the internal Docker network, not exposed to the host. This is the cleanest approach but requires more config changes.
- The health endpoint MUST remain `permitAll()` for Kubernetes/Docker health probes (the docker-compose healthcheck uses `curl -f http://localhost:8080/api/v1/actuator/health`).

---

## Standard Stack

### Core (Already in Project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Data JPA | 3.2.5 (via Spring Boot) | ORM with @Version support | Native optimistic locking [VERIFIED: Spring Boot 3.2.5 parent pom] |
| Spring Security | 6.2.x (via Spring Boot 3.2.5) | @PreAuthorize, SecurityFilterChain | Already in use project-wide [VERIFIED: pom.xml line 46] |
| Redis (Lettuce) | 7 (via Spring Boot) | Distributed locking backend | Already used by RedisLockService [VERIFIED: pom.xml line 170] |
| Flyway | 10.x (via Spring Boot) | Database migration | Already configured with 3 migrations [VERIFIED: pom.xml line 200] |
| Micrometer Prometheus | 1.12.x (via Spring Boot) | Metrics endpoint | Already configured [VERIFIED: pom.xml line 182] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring AOP | 6.2.x (via Spring Boot) | Aspect ordering for @DistributedLock | Aspect order fix (CON-03) [VERIFIED: pom.xml line 213] |

No new dependencies needed for this phase. All required libraries are already in the project.

---

## Architecture Patterns

### Pattern 1: @DistributedLock before @Transactional (Lock-Outside-Transaction)

**What:** The distributed lock must be acquired BEFORE the transaction starts, and released AFTER the transaction commits. This prevents the race condition where Thread A commits a transaction and releases the lock, but Thread B acquires the lock and reads data before Thread A's commit is visible.

**When to use:** Any method that both modifies shared state AND needs concurrency protection.

**Example (from existing code):**
```java
// Source: DigitalSignatureService.java line 66-67
@DistributedLock(key = "'keypair:generate:' + #userId", expireTime = 10)
@Transactional
public RsaKeyPairResponse generateKeyPair(Long userId) {
    // Lock acquired -> Transaction started -> Business logic -> Transaction committed -> Lock released
}
```

**Anti-pattern (current DoubleAuctionService):**
```java
// BAD: synchronized is inside @Transactional due to proxy ordering
@Transactional
public synchronized List<MatchingResultResponse> executeMatching() {
    // Transaction started -> Lock acquired -> Business logic -> Lock released -> Transaction committed
    // ^^ Race condition: another thread can acquire the lock before this thread's commit is visible
}
```

### Pattern 2: @Version Optimistic Locking with Retry

**What:** Add `@Version` to entities that are concurrently modified. When JPA detects a version mismatch on save, it throws `OptimisticLockingFailureException`. The service layer catches this and either retries or returns a conflict error.

**When to use:** Entities whose financial fields are updated by multiple concurrent operations (Enterprise quotas, CarbonCoinAccount balances, AuctionOrder matching).

**Example:**
```java
@Entity
public class Enterprise extends BaseEntity {
    @Version
    private Long version;
    
    // ... existing fields ...
}

// In service:
try {
    enterpriseRepository.save(enterprise);
} catch (OptimisticLockingFailureException e) {
    throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, 
        "数据已被其他操作修改，请刷新后重试");
}
```

### Pattern 3: @PreAuthorize Role Guard on Controllers

**What:** Every controller endpoint must have an explicit `@PreAuthorize` annotation that declares which roles can access it.

**When to use:** All controller methods. No endpoint should rely solely on `anyRequest().authenticated()` in SecurityConfig.

**Example (from existing code):**
```java
// Source: DoubleAuctionController.java line 46-47
@PreAuthorize("hasRole('ENTERPRISE')")
public ApiResponse<AuctionOrderResponse> placeBuyOrder(...) { ... }

// Source: DoubleAuctionController.java line 77
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<List<MatchingResultResponse>> executeMatching() { ... }
```

### Anti-Patterns to Avoid

- **synchronized on Spring beans:** Fails silently in multi-instance deployments. Always use `@DistributedLock` for shared state.
- **Header-based identity fallback (X-User-Id):** Allows JWT authentication bypass. Always rely on `@AuthenticationPrincipal`.
- **Hardcoded credential defaults in config:** `123456` or `minioadmin` as defaults in YAML/docker-compose. Use `${ENV_VAR}` with no default.
- **Lock-inside-transaction:** `@Transactional` on the same method as the lock without explicit aspect ordering creates a race condition.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Distributed locking | Custom Redis lock in each service method | `@DistributedLock` annotation + `DistributedLockAspect` | Already exists, proven in 3 services, SpEL support |
| Optimistic locking | Manual version check-and-compare | JPA `@Version` | Spring Data JPA handles version increment and conflict detection natively |
| Optimistic lock exception handling | Generic catch-all in each service | Central `@ControllerAdvice` for `OptimisticLockingFailureException` | Single point of error response formatting |
| Role-based access control | Manual role checks in method body | `@PreAuthorize` annotation | Spring Security enforces before method execution, cannot be bypassed by bugs in method logic |

---

## Common Pitfalls

### Pitfall 1: Aspect Execution Order with @DistributedLock and @Transactional

**What goes wrong:** If `@Transactional` AOP proxy wraps the method before `@DistributedLock` proxy, the transaction starts first, then the lock is acquired inside it. When the method returns, the lock is released (finally block in DistributedLockAspect), then the transaction commits. Another thread can acquire the lock between release and commit, reading stale data.

**Why it happens:** Spring's `@Transactional` interceptor has default order `Ordered.LOWEST_PRECEDENCE` (runs outermost). Without explicit `@Order` on `DistributedLockAspect`, the order is undefined.

**How to avoid:** Add `@Order(Ordered.HIGHEST_PRECEDENCE)` to `DistributedLockAspect` so it runs before `@Transactional`.

**Warning signs:** Concurrent modifications cause data inconsistency that is hard to reproduce in testing (single-instance dev works, multi-instance prod fails).

### Pitfall 2: @Version Breaking @Builder Calls

**What goes wrong:** Adding `@Version private Long version;` to an entity that uses `@Builder` adds a new parameter to the generated builder. All existing `Entity.builder().field1().field2()...build()` calls will fail compilation because `version` is now a required builder parameter.

**Why it happens:** Lombok `@Builder` generates an all-args constructor. Adding a field adds a constructor parameter.

**How to avoid:** Use `@Builder.Default` on the version field: `@Version @Builder.Default private Long version = 0L;`. This makes the builder include a default value, so existing builder calls that don't set `version` still compile.

**Warning signs:** Compilation errors in test files after adding `@Version` to entities.

### Pitfall 3: Removing Credential Defaults Breaking Docker Compose Up

**What goes wrong:** After removing `:-default` fallbacks from docker-compose.yml, running `docker-compose up` without `.env` fails with "variable not set".

**Why it happens:** Docker Compose requires all referenced env vars to have values.

**How to avoid:** Ensure `.env.example` is well-documented and includes all required variables. Add a comment in docker-compose.yml noting that `.env` is required.

**Warning signs:** `docker-compose up` fails with "required variable ... is missing a value".

### Pitfall 4: Prometheus Scraping Failure After Requiring Auth

**What goes wrong:** After requiring authentication on `/actuator/prometheus`, the Prometheus scraper (which previously accessed it without auth) gets 401 responses and stops collecting metrics.

**Why it happens:** Prometheus scrape config in `prometheus.yml` does not include auth headers.

**How to avoid:** Update `prometheus.yml` to include `basic_auth` or `bearer_token` configuration before deploying the security change. Alternatively, use a separate management port.

**Warning signs:** Grafana dashboards show "No data" after deployment.

---

## Code Examples

### Adding @DistributedLock to executeMatching()

```java
// Source: Existing pattern from DigitalSignatureService.java line 66-67
// Applied to DoubleAuctionService.java

@DistributedLock(key = "'auction:matching'", expireTime = 30, waitTime = 0)
@Transactional
public List<MatchingResultResponse> executeMatching() {
    // ... existing logic unchanged ...
}
```

### Adding @Version to Enterprise Entity

```java
// Source: Spring Data JPA documentation
// Applied to Enterprise.java

@Entity
@Table(name = "enterprise")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enterprise extends BaseEntity {
    
    @Version
    @Builder.Default
    private Long version = 0L;
    
    // ... existing fields unchanged ...
}
```

### Flyway Migration for @Version Columns

```sql
-- V5__add_optimistic_locking.sql
ALTER TABLE enterprise ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE carbon_coin_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE auction_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

### Adding @PreAuthorize to FileController

```java
// Source: Existing pattern from DoubleAuctionController.java
// Applied to FileController.java

@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
public ApiResponse<MinioService.UploadResult> uploadFile(...) { ... }

@DeleteMapping
@PreAuthorize("isAuthenticated()")
public ApiResponse<Void> deleteFile(...) { ... }
```

### Removing Header Fallback

```java
// BEFORE (current, insecure):
private Long resolveUserId(JwtUserDetails currentUser, HttpServletRequest request) {
    if (currentUser != null) {
        return currentUser.getUserId();
    }
    String header = request.getHeader("X-User-Id");
    return header != null ? Long.parseLong(header) : null;
}

// AFTER (secure):
private Long resolveUserId(JwtUserDetails currentUser) {
    return currentUser.getUserId(); // @PreAuthorize guarantees non-null
}
```

### Restricting Prometheus Endpoint

```java
// BEFORE (current, open):
.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

// AFTER (restricted):
.requestMatchers("/actuator/health").permitAll()  // k8s probes
.requestMatchers("/actuator/prometheus").hasRole('ADMIN')  // monitoring only
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| JVM synchronized | Redis distributed lock | Already in project (3 services) | DoubleAuctionService is the last holdout |
| No optimistic locking | JPA @Version | Standard since JPA 2.0 | Not yet used in this project |
| Hardcoded defaults | Env-only config | Industry standard since 12-factor apps | Partially adopted, needs completion |
| Header-based identity | @AuthenticationPrincipal only | Spring Security best practice | FileController is the last holdout |

**Deprecated/outdated:**
- `synchronized` on Spring singleton beans: Does not work in multi-instance deployments. Replace with `@DistributedLock`.
- X-User-Id/X-User-Type header fallback: Security anti-pattern that allows authentication bypass.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | FileController upload should be ENTERPRISE+ADMIN only (not all authenticated users) | SEC-10 | If ENTERPRISE users don't actually upload files, the restriction is too tight and breaks functionality |
| A2 | SearchController market-overview should be accessible to all authenticated users | SEC-12 | If market data contains sensitive information, needs stricter role check |
| A3 | Prometheus endpoint should require ADMIN role (not just authenticated) | SEC-13 | If monitoring tools use non-admin service accounts, they'll lose access |
| A4 | Flyway next migration version is V5 (V4 is latest) | CON-02 | If another migration was added between research and implementation, V5 will conflict |
| A5 | @Builder.Default with `= 0L` will preserve existing builder call compatibility | CON-02 | If Lombok version doesn't handle @Builder.Default with @Version correctly, compilation may fail |

---

## Open Questions

1. **Prometheus auth strategy: Role-based vs. separate management port?**
   - What we know: SecurityConfig currently has `permitAll()` for prometheus. Prometheus scraper config needs updating either way.
   - What's unclear: Whether to use `hasRole('ADMIN')` on the prometheus endpoint, or use a separate `management.server.port` that is only accessible within the Docker network.
   - Recommendation: Separate management port is cleaner for infrastructure (Prometheus can scrape without auth on the internal network), but `hasRole('ADMIN')` is simpler to implement. Let the planner decide based on deployment complexity.

2. **@DistributedLock expireTime for executeMatching()?**
   - What we know: Matching processes all pending buy/sell orders. With large datasets, this could take seconds.
   - What's unclear: The maximum execution time for a matching round with realistic data volumes.
   - Recommendation: Use `expireTime = 30` (same as RedisLockService default) with `waitTime = 0` (fail fast). This matches the existing DigitalSignatureService pattern. If matching takes longer than 30s in production, the lock will auto-expire (safety valve), and the next matching attempt will proceed.

3. **Should CarbonCoinService also get @DistributedLock for transfer operations?**
   - What we know: CarbonCoinService.transfer() (line 137) modifies two accounts atomically but has no concurrency protection beyond @Transactional.
   - What's unclear: Whether concurrent transfers on the same account can cause lost updates.
   - Recommendation: Out of scope for this phase (not in requirements), but @Version (CON-02) on CarbonCoinAccount will catch concurrent modifications. Add @DistributedLock as a future enhancement.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 17 | Backend compilation | Yes | 17 (LTS) | -- |
| Maven 3.8+ | Build | Yes | 3.8.4 | -- |
| Redis | @DistributedLock | Yes (Docker) | 7-alpine | -- |
| MySQL | Flyway migration | Yes (Docker) | 8.0 | -- |
| MinIO | FileController tests | Yes (Docker) | RELEASE.2025-04-22 | -- |
| Docker Compose | Integration testing | Yes | -- | -- |

**Missing dependencies with no fallback:** None

**Missing dependencies with fallback:** None

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test |
| Config file | `application-test.yml` (H2 in-memory) |
| Quick run command | `mvn test -pl oaiss-chain-backend -Dtest=DoubleAuctionServiceTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Full suite command | `cd oaiss-chain-backend && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CON-01 | executeMatching() uses @DistributedLock instead of synchronized | unit | `mvn test -Dtest=DoubleAuctionServiceTest` | Yes (needs update) |
| CON-02 | Enterprise/CarbonCoinAccount/AuctionOrder have @Version; concurrent update throws OptimisticLockingFailureException | unit | `mvn test -Dtest=EnterpriseTest,CarbonCoinAccountTest,AuctionOrderTest` | No -- Wave 0 |
| CON-03 | DistributedLockAspect runs before @Transactional (lock-outside-tx) | unit | `mvn test -Dtest=DistributedLockAspectTest` | No -- Wave 0 |
| SEC-07 | docker-compose.yml has no hardcoded passwords | integration | Manual verification / shell script | No -- Wave 0 |
| SEC-08 | application.yml has no DB_PASSWORD default | integration | `grep -c 'DB_PASSWORD:123456' application.yml` | No -- Wave 0 |
| SEC-09 | No minioadmin in compose/YAML files | integration | `grep -r 'minioadmin' docker-compose* application*.yml` | No -- Wave 0 |
| SEC-10 | FileController endpoints have @PreAuthorize | unit | `mvn test -Dtest=FileControllerTest` | No -- Wave 0 |
| SEC-11 | FileController does not read X-User-Id/X-User-Type headers | unit | `mvn test -Dtest=FileControllerTest` | No -- Wave 0 |
| SEC-12 | SearchController endpoints have @PreAuthorize | unit | `mvn test -Dtest=SearchControllerTest` | No -- Wave 0 |
| SEC-13 | /actuator/prometheus requires authentication | integration | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/actuator/prometheus` should return 401 | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest=<SpecificTest> -pl oaiss-chain-backend`
- **Per wave merge:** `cd oaiss-chain-backend && mvn test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `DoubleAuctionServiceTest.java` -- update synchronized test to verify @DistributedLock (CON-01)
- [ ] `EnterpriseTest.java` -- verify @Version field and optimistic locking (CON-02)
- [ ] `CarbonCoinAccountTest.java` -- verify @Version field (CON-02)
- [ ] `AuctionOrderTest.java` -- verify @Version field (CON-02)
- [ ] `DistributedLockAspectTest.java` -- verify aspect order vs @Transactional (CON-03)
- [ ] `FileControllerTest.java` -- verify @PreAuthorize annotations and no header fallback (SEC-10, SEC-11)
- [ ] `SearchControllerTest.java` -- verify @PreAuthorize annotations (SEC-12)
- [ ] Security config test -- verify /actuator/prometheus requires auth (SEC-13)

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | JWT Bearer + @AuthenticationPrincipal (no header fallback) |
| V3 Session Management | Yes | Stateless sessions (SessionCreationPolicy.STATELESS) |
| V4 Access Control | Yes | @PreAuthorize on all controller endpoints |
| V5 Input Validation | No | Not in scope for this phase |
| V6 Cryptography | Yes | Credential externalization (no hardcoded secrets) |
| V7 Error Handling | Yes | OptimisticLockingFailureException handling |

### Known Threat Patterns for Java/Spring Boot

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Concurrent modification (lost update) | Tampering | @Version optimistic locking + @DistributedLock |
| Authentication bypass via header injection | Spoofing | Remove X-User-Id/X-User-Type fallback, rely on @AuthenticationPrincipal only |
| Credential exposure in version control | Information Disclosure | Externalize all secrets to env vars, no defaults in tracked files |
| Unauthorized metrics access | Information Disclosure | Require authentication on /actuator/prometheus |
| Race condition in financial matching | Tampering | Lock-outside-transaction pattern with @DistributedLock before @Transactional |

---

## Sources

### Primary (HIGH confidence)
- Codebase analysis: DoubleAuctionService.java, DistributedLockAspect.java, RedisLockService.java, SecurityConfig.java, FileController.java, SearchController.java
- Flyway migrations: V1__init_schema.sql, V2__seed_data.sql, V4__enterprise_admission.sql
- Docker Compose: docker-compose.yml, docker-compose.infra.yml, oaiss-chain-backend/docker-compose.yml
- Application config: application.yml, application-dev.yml, application-docker.yml, application-local.yml

### Secondary (MEDIUM confidence)
- AGENTS.md documentation on aspect ordering (DistributedLock outermost)
- CLAUDE.md project conventions

### Tertiary (LOW confidence)
- None -- all findings verified against codebase

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all dependencies already in project, verified in pom.xml
- Architecture: HIGH - existing @DistributedLock pattern proven in 3 services, Spring Data JPA @Version is standard
- Pitfalls: HIGH - aspect ordering, @Builder compatibility, and Docker Compose credential issues are well-documented
- Security: HIGH - all current states verified by reading source code

**Research date:** 2026-05-19
**Valid until:** 2026-06-19 (30 days - stable codebase, no library upgrades planned)
