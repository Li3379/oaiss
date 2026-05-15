# CONCERNS.md -- Technical Debt, Bugs, Security, Performance, Fragile Areas

> Last updated: 2026-05-15
> Status: Security audit 48/63 fixes applied; i18n migration complete; TypeScript migration complete.

---

## 1. Security

### CRITICAL

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-01 | **RSA private keys stored in database** -- `DigitalSignatureService` saves `privateKeyBase64` to the `rsa_key_pairs` table. If the DB is compromised, all signing keys are exposed. Private keys should be stored in a hardware security module (HSM) or at minimum encrypted at rest with a key derived from a master secret outside the DB. | `src/main/java/com/oaiss/chain/service/DigitalSignatureService.java:89-100` | OPEN |
| SEC-02 | **CSRF disabled globally** -- `SecurityConfig` disables CSRF entirely. The comment argues JWT-in-sessionStorage is safe, but if any future code sets cookies (e.g. the refresh-token cookie referenced in the audit), CSRF protection is silently absent. | `src/main/java/com/oaiss/chain/config/SecurityConfig.java:58` | OPEN |
| SEC-03 | **Swagger/OpenAPI endpoints publicly accessible** -- `/swagger-ui/**` and `/v3/api-docs/**` are `permitAll()` unconditionally, not gated to dev/test profiles. API schema disclosure in production aids attackers. | `src/main/java/com/oaiss/chain/config/SecurityConfig.java:72-77` | OPEN |
| SEC-04 | **CORS allows configurable origins but no validation** -- `allowedOrigins` is read from `app.cors.allowed-origins` with a default of `http://localhost:5173,http://localhost:5174`. If production config is missing, localhost leaks. No wildcard validation or env-specific enforcement. | `src/main/java/com/oaiss/chain/config/SecurityConfig.java:45-46`, `src/main/resources/application.yml:134` | OPEN |
| SEC-10 | **Hardcoded default DB password `123456`** -- `application.yml` defines `password: ${DB_PASSWORD:123456}`. If the environment variable is unset, the app starts with a trivially guessable credential. Same risk for Redis which defaults to empty password `${REDIS_PASSWORD:}`. | `src/main/resources/application.yml:17`, `src/main/resources/application.yml:48` | OPEN |
| SEC-11 | **Fabric CA admin password `adminpw` in config** -- `fabric.ca.admin-password: adminpw` is a well-known Hyperledger Fabric sample default. If Fabric CA is enabled in production without overriding this, the admin identity is trivially compromised. | `src/main/resources/application.yml:169` | OPEN |
| SEC-12 | **Registration endpoint has no format validation** -- `RegisterRequest` has no `@Email` on `email`, no `@Pattern` on `phone`, and no validation on `creditCode`, `enterpriseName`, `qualificationNo`, `orgCode`, or `orgName`. Any string passes into the database. | `src/main/java/com/oaiss/chain/dto/RegisterRequest.java:36-77` | OPEN |
| SEC-13 | **No rate limiting on registration** -- Login has rate limiting (5 attempts via `checkLoginRateLimit()`), but `/auth/register` has no rate limit. An attacker can mass-register accounts. | `src/main/java/com/oaiss/chain/controller/AuthController.java:64-93` | OPEN |
| SEC-14 | **Authorization only at controller layer** -- `@PreAuthorize` is applied only on controller methods (54 annotations across controllers). Service methods have no authorization checks. Any service method can be called from other services or tests without role verification. | All controllers in `src/main/java/com/oaiss/chain/controller/`, no `@PreAuthorize` in `src/main/java/com/oaiss/chain/service/` | OPEN |
| SEC-15 | **Token blacklist race condition** -- `JwtAuthenticationFilter` checks the blacklist cache *after* `validateToken()` succeeds. Between token validation and blacklist check, a concurrent request with the same token could slip through. | `src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java:74-81` | OPEN |

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-05 | **Login brute-force limit uses in-memory Redis counter with no sliding window** -- `AuthService` tracks attempts via `increment()` on a Redis key but the key has a fixed TTL. A distributed attacker can reset the counter by waiting for TTL expiry. | `src/main/java/com/oaiss/chain/service/AuthService.java:320-353` | OPEN |
| SEC-06 | **JWT secret has no startup validation** -- `JwtTokenProvider` uses `jwtSecret.getBytes()` directly. If the secret is too short (< 256 bits for HMAC-SHA), JJWT will throw at runtime but there is no `@PostConstruct` validation to fail fast. | `src/main/java/com/oaiss/chain/security/JwtTokenProvider.java:35-38` | OPEN |
| SEC-07 | **Error messages leak internal state** -- `ErrorMessage.java` contains Chinese-language error messages that are returned directly to the client. Some include entity IDs or field names (e.g., "企业信誉分不存在: " + enterpriseId). | `src/main/java/com/oaiss/chain/service/CreditScoreService.java:227`, `src/main/java/com/oaiss/chain/service/UserService.java` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-08 | **Emission factors and permissions hardcoded in CachePreloadService** -- These business-critical values are Java constants, not sourced from the database or config. Changing a factor requires a code change and redeployment. | `src/main/java/com/oaiss/chain/service/CachePreloadService.java:143-148` | OPEN |
| SEC-09 | **`@RateLimit` relies on Redis availability** -- If Redis is down, `RateLimitAspect` catches the exception and allows the request through (fail-open). This means rate limiting is non-functional during Redis outages. | `src/main/java/com/oaiss/chain/aop/RateLimitAspect.java:83` | OPEN |

---

## 2. Concurrency & Data Integrity

### CRITICAL

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| CON-01 | **`synchronized` on Spring singleton `executeMatching()`** -- The double-auction matching method uses `synchronized`, which blocks all other threads at the JVM level. Under load, this serializes all auction operations. More critically, it does not protect against race conditions with the database -- two JVM instances (horizontal scaling) would run matching concurrently without coordination. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:146` | OPEN |
| CON-02 | **Non-atomic quota updates in `updateEnterpriseQuota()`** -- Seller and buyer quota updates are done via separate `save()` calls with no optimistic locking (`@Version`) or pessimistic locking. A concurrent matching round could over-deduct quotas. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:338-352` | OPEN |
| CON-03 | **`@Transactional` on `synchronized` method** -- The transaction commits after the lock is released. Another thread can read stale data between lock release and commit, causing phantom reads. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:145-146` | OPEN |
| CON-06 | **Trade confirmation has double-spend risk** -- `TradeService.confirmTrade()` reads the transaction status, checks it is PENDING, then updates to PROCESSING. Two concurrent requests for the same trade could both pass the PENDING check before either writes PROCESSING. No `@Version`, no `@DistributedLock`, no `SELECT ... FOR UPDATE`. | `src/main/java/com/oaiss/chain/service/TradeService.java:142-186` | OPEN |

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| CON-04 | **No `@Version` optimistic locking on any entity** -- None of the JPA entities use `@Version`. Concurrent updates to `CarbonCoin`, `Trade`, `CarbonNeutralProject` records can silently overwrite each other. | All entity classes in `src/main/java/com/oaiss/chain/entity/` | OPEN |
| CON-05 | **`@Transactional` without `readOnly=true` on read methods** -- All service methods annotated with `@Transactional` default to read-write. Read-only queries acquire unnecessary write locks and skip the read-only optimization in Spring Data. | Multiple services | OPEN |

---

## 3. Performance

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| PERF-01 | **N+1 queries in `CarbonNeutralProjectService.toResponse()`** -- Each entity triggers 2-3 extra DB queries (owner name, reviewer name, verifier name). A page of 20 projects generates 40-60 extra queries. | `src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java:469-483` | OPEN |
| PERF-02 | **`redisTemplate.keys("*")` in `getCacheStatistics()`** -- The `KEYS` command is O(N) and blocks the Redis event loop. In production with large key sets, this causes latency spikes for all Redis operations. | `src/main/java/com/oaiss/chain/service/CachePreloadService.java:187` | OPEN |
| PERF-03 | **Cache preload runs synchronously on startup** -- `@EventListener(ApplicationReadyEvent.class)` blocks application readiness. If Redis is slow or down, startup hangs or fails. Should be async or use `@Async`. | `src/main/java/com/oaiss/chain/service/CachePreloadService.java:37-60` | OPEN |
| PERF-04 | **No pagination on auction order queries** -- `findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc/Asc` loads ALL active orders into memory. With thousands of pending orders, this causes GC pressure and OOM risk. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:153-158` | OPEN |
| PERF-08 | **No `@Cacheable` annotations despite full cache infrastructure** -- Spring Cache is configured (Redis + CacheManager + CachePreloadService), but no service method uses `@Cacheable`, `@CacheEvict`, or `@CachePut`. All data is fetched from the database on every request. | All files in `src/main/java/com/oaiss/chain/service/` (0 matches for `@Cacheable`) | OPEN |
| PERF-09 | **Market overview counts on every call** -- `SearchService.getMarketOverview()` executes three separate `count()` queries with no caching. Dashboard statistics endpoint generates 3 DB queries per call. | `src/main/java/com/oaiss/chain/service/SearchService.java:67-79` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| PERF-06 | **`CarbonNeutralProjectService` at 580 lines with 14 `@Transactional` methods** -- This service is a God object handling too many concerns. Each method repeats the same entity-lookup + validation + save pattern. | `src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` | OPEN |
| PERF-07 | **`@Transactional` on read-only login method** -- `AuthService.login()` is annotated `@Transactional` but performs mostly reads. Only the last-login update needs a write transaction. Opens a write transaction for the entire login flow, holding DB connections longer than necessary. | `src/main/java/com/oaiss/chain/service/AuthService.java:50` | OPEN |
| PERF-10 | **HikariCP pool configured but small** -- `maximum-pool-size: 20` and `minimum-idle: 5` in `application.yml`. Under concurrent load with N+1 queries, 20 connections may be exhausted quickly. | `src/main/resources/application.yml:19-24` | OPEN |

---

## 4. Code Quality & Maintainability

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| QUAL-01 | **Swallowed exception in `UserService.toUserInfoResponse()`** -- `catch (Exception ignored)` silently swallows enum lookup failures, defaulting to "未知". If the enum is corrupted, no log or alert is produced. | `src/main/java/com/oaiss/chain/service/UserService.java:139` | OPEN |
| QUAL-02 | **Generic `catch (Exception e)` in 30+ locations** -- `MinioService` alone has 14 broad catch blocks. Many log and rethrow, but the pattern masks the root cause type. Specific exceptions (e.g., `MinioException`, `IOException`) should be caught. | `src/main/java/com/oaiss/chain/service/MinioService.java` (14), `src/main/java/com/oaiss/chain/service/DigitalSignatureService.java` (6), others | OPEN |
| QUAL-03 | **Hardcoded Chinese strings in service layer** -- `DoubleAuctionService` uses literal "买入"/"卖出"/"未知" instead of i18n keys or enums. This contradicts the completed i18n migration. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:377-378, 417-418` | OPEN |
| QUAL-04 | **Magic numbers for user types** -- `AuthService.login()` checks `user.getUserType() == 1` to identify enterprise users. `UserTypeEnum` exists but is not consistently used in service code. | `src/main/java/com/oaiss/chain/service/AuthService.java:83`, `src/main/java/com/oaiss/chain/service/AuthService.java:207` | OPEN |
| QUAL-05 | **Integer-based status constants instead of enums** -- `CarbonNeutralProjectService` defines 7 status constants and 4 cert/verify status constants as `public static final int`. No type safety -- any integer can be passed where a status is expected. | `src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java:62-80` | OPEN |
| QUAL-10 | **Duplicate `toResponse`/`toMatchResponse` methods** -- `TradeService` has two `toResponse()` overloads (one with `userNames` map, one without). `DoubleAuctionService` has the same pattern. The N+1 variant should be removed. | `src/main/java/com/oaiss/chain/service/TradeService.java:267-284`, `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:415-433` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| QUAL-06 | **Frontend: 24 test files for 22 Vue components** -- Test coverage is minimal. Only 24 `.test.ts` files exist for the entire frontend. No tests for `api/`, `store/`, `router/`, or `utils/` modules. | `oaiss-chain-frontend/src/` | OPEN |
| QUAL-07 | **Backend: 79 test files for ~100 main source files** -- Test ratio is ~79%, but integration tests are sparse (only `UserIntegrationTest.java`). Critical flows (trade matching, carbon report lifecycle, auction) lack integration coverage. | `src/test/java/com/oaiss/chain/` | OPEN |
| QUAL-08 | **Inconsistent error code usage** -- Some services throw `BusinessException(4003, "对方账户不存在")` with inline codes, while others use `ErrorCode.TOKEN_INVALID` constants. The `CarbonCoinService` mixes both patterns. | `src/main/java/com/oaiss/chain/service/CarbonCoinService.java:148, 194` | OPEN |
| QUAL-09 | **Duplicate `toMatchResponse` methods** -- `DoubleAuctionService` has two overloads: one that does N+1 queries and one that uses a pre-fetched map. The N+1 variant should be removed or refactored. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:415, 426` | OPEN |

---

## 5. Technical Debt

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| DEBT-01 | **Deleted `BlockchainService.java` but controller still exists** -- `BlockchainService` was deleted (git status shows `D`), but `BlockchainController` still references blockchain functionality. May delegate to `MockBlockchainService` or `FabricBlockchainService`. | `src/main/java/com/oaiss/chain/service/BlockchainService.java` (deleted), `src/main/java/com/oaiss/chain/controller/BlockchainController.java` | VERIFY |
| DEBT-02 | **`MockBlockchainService` vs `FabricBlockchainService` ambiguity** -- Both implementations exist. `fabric.enabled=false` means mock is always active. No `@ConditionalOnProperty` observed to switch between them. | `src/main/java/com/oaiss/chain/service/MockBlockchainService.java`, `src/main/java/com/oaiss/chain/service/FabricBlockchainService.java` | OPEN |
| DEBT-03 | **PMD/SpotBugs not enforced** -- Both PMD (`failOnViolation=false`) and SpotBugs (`failOnError=false`) are configured but do not fail the build on violations. Code quality issues accumulate silently. | `pom.xml:279`, `pom.xml:292` | OPEN |
| DEBT-04 | **No `TODO`/`FIXME` comments found** -- The codebase has zero `TODO`/`FIXME`/`HACK` markers, which is either very clean or indicates that known issues are tracked elsewhere (or not at all). | Entire `src/` tree | INFO |

---

## 6. Infrastructure & Operations

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| INFRA-01 | **No Flyway migration for `rsa_key_pairs` table visible** -- If the security audit added this table, ensure a Flyway migration exists and is idempotent. | `db/migration/` | VERIFY |
| INFRA-02 | **`application.yml` not checked for production defaults** -- CORS, JWT expiration, Redis TTL, and pool sizes are all injected via `@Value` with defaults. Production must override all of these. | `src/main/resources/application.yml` | OPEN |
| INFRA-03 | **No health check for MinIO** -- `DatabaseHealthIndicator` and `RedisHealthIndicator` exist but no MinIO health indicator. Storage outages are silent until a user uploads. | `src/main/java/com/oaiss/chain/config/` | OPEN |

---

## 7. Fragile Areas (High Change Risk)

| Area | Why Fragile | Files |
|------|-------------|-------|
| **Double Auction Matching** | `synchronized` + no optimistic locking + mutable entity state. Any change to the matching algorithm risks data corruption under concurrency. | `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java` |
| **Trade Confirmation** | Read-check-write pattern without locks. Concurrent confirmation causes double-spend of carbon quotas. | `src/main/java/com/oaiss/chain/service/TradeService.java:142-186` |
| **Carbon Neutral Project** | 580 lines, 14 transactional methods, repeated entity lookup boilerplate. Adding a new status transition requires touching many methods. | `src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` |
| **Security Config** | CSRF disabled, Swagger public, CORS from config. Any change to auth flow (e.g., adding refresh-token cookies) requires re-evaluating all three. | `src/main/java/com/oaiss/chain/config/SecurityConfig.java` |
| **Cache Preload** | Hardcoded business values (emission factors, permissions). Any regulatory change to carbon factors requires a code change. | `src/main/java/com/oaiss/chain/service/CachePreloadService.java` |
| **File Upload (MinIO)** | 448 lines with 14 broad catch blocks. Error paths are unclear; a MinIO SDK upgrade could silently change exception types. | `src/main/java/com/oaiss/chain/service/MinioService.java` |
| **JWT Authentication Filter** | Token blacklist race condition + path whitelist must be kept in sync with `SecurityConfig` permit rules. | `src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java` |

---

## 8. Deferred / Tracked Items

| Item | Source | Status |
|------|--------|--------|
| TypeScript migration | `MEMORY.md` | COMPLETE |
| i18n migration | `MEMORY.md` | COMPLETE |
| Security audit (48/63) | `MEMORY.md` | 15 items remaining |
| M4 TypeScript deferred | `MEMORY.md/deferred-projects.md` | DEFERRED |
| M19 i18n deferred | `MEMORY.md/deferred-projects.md` | DEFERRED |

---

## Summary

**Top 10 actions by impact:**

1. **Fix auction concurrency** (CON-01, CON-02, CON-03) -- Replace `synchronized` with `@DistributedLock` + `@Version`. Data corruption risk.
2. **Fix trade confirmation double-spend** (CON-06) -- Add `@DistributedLock` on trade ID or use `UPDATE ... WHERE status = PENDING` with row-count check.
3. **Add `@Version` to mutable entities** (CON-04) -- Prevents silent data overwrites across the entire trading system.
4. **Remove RSA private key storage** (SEC-01) -- Use HSM or at minimum encrypt-at-rest. Critical security exposure.
5. **Remove hardcoded default credentials** (SEC-10, SEC-11) -- Fail fast if `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET` are unset.
6. **Restrict Swagger to dev profile** (SEC-03) -- One-line change, high security payoff.
7. **Add input validation to registration** (SEC-12) -- `@Email`, `@Pattern` on phone, `@NotBlank` on business fields.
8. **Replace `KEYS` with `SCAN`** (PERF-02) -- Prevents Redis latency spikes in production.
9. **Add `@Cacheable` to read-heavy services** (PERF-08) -- Free performance gain with existing infrastructure.
10. **Add integration tests for critical flows** (QUAL-07) -- Trade lifecycle, auction matching, report approval.

---

*Concerns audit: 2026-05-15*
