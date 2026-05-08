# CONCERNS.md — Technical Debt, Bugs, Security, Performance, Fragile Areas

> Last updated: 2026-05-08
> Status: Security audit 48/63 fixes applied; i18n migration complete; TypeScript migration complete.

---

## 1. Security

### CRITICAL

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-01 | **RSA private keys stored in database** — `DigitalSignatureService` saves `privateKeyBase64` to the `rsa_key_pairs` table. If the DB is compromised, all signing keys are exposed. Private keys should be stored in a hardware security module (HSM) or at minimum encrypted at rest with a key derived from a master secret outside the DB. | `DigitalSignatureService.java:89-100` | OPEN |
| SEC-02 | **CSRF disabled globally** — `SecurityConfig` disables CSRF entirely. The comment argues JWT-in-sessionStorage is safe, but if any future code sets cookies (e.g. the refresh-token cookie referenced in the audit), CSRF protection is silently absent. | `SecurityConfig.java:57` | OPEN |
| SEC-03 | **Swagger/OpenAPI endpoints publicly accessible** — `/swagger-ui/**` and `/v3/api-docs/**` are `permitAll()` unconditionally, not gated to dev/test profiles. API schema disclosure in production aids attackers. | `SecurityConfig.java:72-77` | OPEN |
| SEC-04 | **CORS allows configurable origins but no validation** — `allowedOrigins` is read from `app.cors.allowed-origins` with a default of `http://localhost:5173`. If production config is missing, localhost leaks. No wildcard validation or env-specific enforcement. | `SecurityConfig.java:44` | OPEN |

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-05 | **Login brute-force limit uses in-memory Redis counter with no sliding window** — `AuthService` tracks attempts via `increment()` on a Redis key but the key has a fixed TTL. A distributed attacker can reset the counter by waiting for TTL expiry. | `AuthService.java:320-353` | OPEN |
| SEC-06 | **JWT secret defaults to empty string** — `SecurityStartupValidator` reads `${jwt.secret:}` with an empty default. If `.env` is missing, the app may start with an empty secret (validator should abort, but worth verifying). | `SecurityStartupValidator.java:37` | OPEN |
| SEC-07 | **Error messages leak internal state** — `ErrorMessage.java` contains Chinese-language error messages that are returned directly to the client. Some include entity IDs or field names (e.g., "企业信誉分不存在: " + enterpriseId). | `CreditScoreService.java:227`, `UserService.java` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| SEC-08 | **Emission factors and permissions hardcoded in CachePreloadService** — These business-critical values are Java constants, not sourced from the database or config. Changing a factor requires a code change and redeployment. | `CachePreloadService.java:143-148` | OPEN |
| SEC-09 | **`@RateLimit` relies on Redis availability** — If Redis is down, `RateLimitAspect` catches the exception and allows the request through (fail-open). This means rate limiting is non-functional during Redis outages. | `RateLimitAspect.java:83` | OPEN |

---

## 2. Concurrency & Data Integrity

### CRITICAL

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| CON-01 | **`synchronized` on Spring singleton `executeMatching()`** — The double-auction matching method uses `synchronized`, which blocks all other threads at the JVM level. Under load, this serializes all auction operations. More critically, it does not protect against race conditions with the database — two JVM instances (horizontal scaling) would run matching concurrently without coordination. | `DoubleAuctionService.java:146` | OPEN |
| CON-02 | **Non-atomic quota updates in `updateEnterpriseQuota()`** — Seller and buyer quota updates are done via separate `save()` calls with no optimistic locking (`@Version`) or pessimistic locking. A concurrent matching round could over-deduct quotas. | `DoubleAuctionService.java:338-352` | OPEN |
| CON-03 | **`@Transactional` on `synchronized` method** — The transaction commits after the lock is released. Another thread can read stale data between lock release and commit, causing phantom reads. | `DoubleAuctionService.java:145-146` | OPEN |

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| CON-04 | **No `@Version` optimistic locking on any entity** — None of the JPA entities use `@Version`. Concurrent updates to `CarbonCoin`, `Trade`, `CarbonNeutralProject` records can silently overwrite each other. | All entity classes | OPEN |
| CON-05 | **`@Transactional` without `readOnly=true` on read methods** — All service methods annotated with `@Transactional` default to read-write. Read-only queries acquire unnecessary write locks and skip the read-only optimization in Spring Data. | Multiple services | OPEN |

---

## 3. Performance

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| PERF-01 | **N+1 queries in `DoubleAuctionService.toMatchResponse()`** — The non-batch overload (lines 415-424) issues two `findById` calls per matching result. For a page of 20 results, this is 40 extra queries. The batch overload exists but is only used in `listMatchingResults`. | `DoubleAuctionService.java:416-419` | OPEN |
| PERF-02 | **`redisTemplate.keys("*")` in `getCacheStatistics()`** — The `KEYS` command is O(N) and blocks the Redis event loop. In production with large key sets, this causes latency spikes for all Redis operations. | `CachePreloadService.java:187` | OPEN |
| PERF-03 | **Cache preload runs synchronously on startup** — `@EventListener(ApplicationReadyEvent.class)` blocks application readiness. If Redis is slow or down, startup hangs or fails. Should be async or use `@Async`. | `CachePreloadService.java:37-60` | OPEN |
| PERF-04 | **No pagination on `findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc/Asc`** — The matching algorithm loads ALL active orders into memory. With thousands of pending orders, this causes GC pressure and OOM risk. | `DoubleAuctionService.java:153-158` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| PERF-05 | **`OfficialHome.vue` at 563 lines** — The largest Vue file is likely a monolithic page with inline logic, styles, and template. Should be decomposed into sub-components. | `views/OfficialHome.vue` | OPEN |
| PERF-06 | **`CarbonNeutralProjectService` at 580 lines with 14 `@Transactional` methods** — This service is a God object handling too many concerns. Each method repeats the same entity-lookup + validation + save pattern. | `CarbonNeutralProjectService.java` | OPEN |
| PERF-07 | **No database connection pool tuning visible** — No explicit HikariCP configuration found. Default pool size (10) may be insufficient under load. | `application.yml` (absent) | OPEN |

---

## 4. Code Quality & Maintainability

### HIGH

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| QUAL-01 | **Swallowed exception in `UserService.toUserInfoResponse()`** — `catch (Exception ignored)` silently swallows enum lookup failures, defaulting to "未知". If the enum is corrupted, no log or alert is produced. | `UserService.java:139` | OPEN |
| QUAL-02 | **Generic `catch (Exception e)` in 30+ locations** — `MinioService` alone has 14 broad catch blocks. Many log and rethrow, but the pattern masks the root cause type. Specific exceptions (e.g., `MinioException`, `IOException`) should be caught. | `MinioService.java` (14), `DigitalSignatureService.java` (6), others | OPEN |
| QUAL-03 | **Hardcoded Chinese strings in service layer** — `DoubleAuctionService` uses literal "买入"/"卖出"/"未知" instead of i18n keys or enums. This contradicts the completed i18n migration. | `DoubleAuctionService.java:377-378, 417-418` | OPEN |
| QUAL-04 | **Magic numbers** — `keyUsage(3)` in `DigitalSignatureService`, status codes like `KEY_STATUS_ACTIVE`, `KEY_STATUS_REVOKED` are raw ints rather than enums. | `DigitalSignatureService.java:96` | OPEN |
| QUAL-05 | **No input validation on `AuctionOrderRequest`** — The `placeBuyOrder`/`placeSellOrder` methods check quota but do not validate that `price > 0` or `quantity > 0` at the service layer. Relies entirely on DTO validation annotations (which may not be present). | `DoubleAuctionService.java:75, 105` | OPEN |

### MEDIUM

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| QUAL-06 | **Frontend: 24 test files for 22 Vue components** — Test coverage is minimal. Only 24 `.test.ts` files exist for the entire frontend. No tests for `api/`, `store/`, `router/`, or `utils/` modules. | `oaiss-chain-frontend/src/` | OPEN |
| QUAL-07 | **Backend: 70 test files for 161 main source files** — Test ratio is ~43%. Services like `MinioService` (448 lines), `CarbonNeutralProjectService` (580 lines) likely have low coverage. | `oaiss-chain-backend/src/` | OPEN |
| QUAL-08 | **Inconsistent error code usage** — Some services throw `BusinessException(4003, "对方账户不存在")` with inline codes, while others use `ErrorCode.TOKEN_INVALID` constants. The `CarbonCoinService` mixes both patterns. | `CarbonCoinService.java:148, 194` | OPEN |
| QUAL-09 | **Duplicate `toMatchResponse` methods** — `DoubleAuctionService` has two overloads: one that does N+1 queries and one that uses a pre-fetched map. The N+1 variant should be removed or refactored. | `DoubleAuctionService.java:415, 426` | OPEN |

---

## 5. Infrastructure & Operations

| ID | Concern | Location | Status |
|----|---------|----------|--------|
| INFRA-01 | **No Flyway migration for `rsa_key_pairs` table visible** — If the security audit added this table, ensure a Flyway migration exists and is idempotent. | `db/migration/` | VERIFY |
| INFRA-02 | **`application.yml` not checked for production defaults** — CORS, JWT expiration, Redis TTL, and pool sizes are all injected via `@Value` with defaults. Production must override all of these. | Config classes | OPEN |
| INFRA-03 | **No health check for MinIO** — `DatabaseHealthIndicator` and `RedisHealthIndicator` exist but no MinIO health indicator. Storage outages are silent until a user uploads. | `config/` | OPEN |
| INFRA-04 | **`cookies.txt` committed to repo** — A `cookies.txt` file exists in the backend root. This may contain session cookies and should be gitignored. | `oaiss-chain-backend/cookies.txt` | OPEN |

---

## 6. Fragile Areas (High Change Risk)

| Area | Why Fragile | Files |
|------|-------------|-------|
| **Double Auction Matching** | `synchronized` + no optimistic locking + mutable entity state. Any change to the matching algorithm risks data corruption under concurrency. | `DoubleAuctionService.java` |
| **Carbon Neutral Project** | 580 lines, 14 transactional methods, repeated entity lookup boilerplate. Adding a new status transition requires touching many methods. | `CarbonNeutralProjectService.java` |
| **Security Config** | CSRF disabled, Swagger public, CORS from config. Any change to auth flow (e.g., adding refresh-token cookies) requires re-evaluating all three. | `SecurityConfig.java` |
| **Cache Preload** | Hardcoded business values (emission factors, permissions). Any regulatory change to carbon factors requires a code change. | `CachePreloadService.java` |
| **File Upload (MinIO)** | 448 lines with 14 broad catch blocks. Error paths are unclear; a MinIO SDK upgrade could silently change exception types. | `MinioService.java` |

---

## 7. Deferred / Tracked Items

| Item | Source | Status |
|------|--------|--------|
| TypeScript migration | `MEMORY.md` | COMPLETE |
| i18n migration | `MEMORY.md` | COMPLETE |
| Security audit (48/63) | `MEMORY.md` | 15 items remaining |
| M4 TypeScript deferred | `MEMORY.md/deferred-projects.md` | DEFERRED |
| M19 i18n deferred | `MEMORY.md/deferred-projects.md` | DEFERRED |

---

## Summary

**Top 5 actions by impact:**

1. **Fix auction concurrency** (CON-01, CON-02, CON-03) — Replace `synchronized` with distributed lock + optimistic locking. Data corruption risk.
2. **Remove RSA private key storage** (SEC-01) — Use HSM or at minimum encrypt-at-rest. Critical security exposure.
3. **Restrict Swagger to dev profile** (SEC-03) — One-line change, high security payoff.
4. **Replace `KEYS` with `SCAN`** (PERF-02) — Prevents Redis latency spikes in production.
5. **Add `@Version` to mutable entities** (CON-04) — Prevents silent data overwrites across the entire trading system.
