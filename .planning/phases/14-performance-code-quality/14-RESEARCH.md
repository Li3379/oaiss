# Phase 14: Performance Optimization & Code Quality - Research

**Researched:** 2026-05-20
**Domain:** Spring Boot 3.2 / MySQL 8 / Redis 7 / JPA performance & security
**Confidence:** HIGH

## Summary

Phase 14 addresses 8 requirements across three categories: Performance (PERF-02 through PERF-05), Security (SEC-01, SEC-02), and Code Quality (QUAL-01, QUAL-02). The codebase investigation reveals concrete, bounded changes with minimal interdependency.

The most impactful finding is that **zero** `@Transactional` annotations use `readOnly=true` (47 total across services), and **7 repository methods** query by user-identifying columns without `AndDeletedFalse`. The Redis KEYS usage is limited to exactly one location (`CachePreloadService.getCacheStatistics()`). RSA private keys are stored as plaintext Base64 in MySQL. The CSRF disable is already justified inline in SecurityConfig but lacks a formal ADR document.

**Primary recommendation:** Each requirement is independently implementable. Execute in order: PERF-05 (index migration, no code change), PERF-02 (Redis SCAN), PERF-04 (pagination), QUAL-01/QUAL-02 (code quality), PERF-03 (@Async), SEC-01 (KEK envelope), SEC-02 (ADR document).

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PERF-02 | Replace Redis KEYS with SCAN | Single call site found in CachePreloadService:187; SCAN-based cursor approach documented below |
| PERF-03 | Cache preload async (@Async) | CachePreloadService.preloadCacheOnStartup() blocks ApplicationReadyEvent; @EnableAsync not configured |
| PERF-04 | Paginate auction order queries (List to Page) | AuctionOrderRepository has 2 List-returning methods for matching; these are bounded by active-status filters |
| PERF-05 | Add FK index migration (Flyway) | V1 schema has ~15 FK columns without indexes; only `operation_log` and `enterprise_admission` have explicit indexes |
| SEC-01 | RSA private key encrypted storage | RsaKeyPair.privateKey stored as plaintext Base64; KEK envelope encryption needed |
| SEC-02 | CSRF protection ADR | CSRF already disabled with inline comment in SecurityConfig:58; formal ADR document needed |
| QUAL-01 | @Transactional(readOnly=true) for reads | Zero uses of readOnly=true found in 47 @Transactional annotations across services |
| QUAL-02 | Repository findByUserId + AndDeletedFalse | 7 methods across 5 repositories lack AndDeletedFalse on soft-delete entities |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Redis KEYS->SCAN | API / Backend | -- | CachePreloadService is a backend service |
| Cache @Async | API / Backend | -- | Spring @Async runs in backend JVM |
| Auction pagination | API / Backend | -- | Repository + Service layer change |
| FK index migration | Database / Storage | -- | Flyway migration, no code change |
| RSA key encryption | API / Backend | Database / Storage | Encryption logic in service, encrypted value in DB |
| CSRF ADR | Documentation | -- | Markdown document only |
| @Transactional readOnly | API / Backend | -- | Service layer annotations |
| AndDeletedFalse | API / Backend | Database / Storage | Repository interface changes affect generated SQL |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.5 | Application framework | Project standard (pom.xml) |
| Spring Data JPA | 3.2.5 | Repository abstraction | Provides Page, Pageable, derived query methods |
| Spring Data Redis | 3.2.5 | Redis integration | RedisTemplate, Lettuce client |
| Flyway | (managed by Spring Boot) | DB migration | Already in use, V1-V5 migrations exist |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| javax.crypto | JDK 17 | AES encryption for KEK envelope | SEC-01 private key encryption |
| Spring @Async | 3.2.5 | Async method execution | PERF-03 cache preload |

**Installation:** No new packages needed. All capabilities exist in current dependencies.

## Package Legitimacy Audit

> No new packages are installed in this phase. All changes use existing Spring Boot / JDK standard library capabilities.

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### Recommended Project Structure
```
src/main/resources/db/migration/
  V6__add_fk_indexes.sql           # PERF-05: FK index migration

src/main/java/com/oaiss/chain/
  config/
    AsyncConfig.java                # PERF-03: @EnableAsync + TaskExecutor bean (NEW)
  service/
    CachePreloadService.java        # PERF-02, PERF-03: SCAN + @Async
  docs/
    adr/
      ADR-001-csrf-disabling.md     # SEC-02: Architecture Decision Record (NEW)
```

### Pattern 1: Redis SCAN replacing KEYS
**What:** Use `RedisTemplate.execute(RedisCallback)` with SCAN cursor instead of `keys()`.
**When to use:** Anywhere that iterates over Redis keys by pattern.
**Example:**
```java
// Source: Spring Data Redis documentation
Long countKeysByPattern(RedisTemplate<String, Object> redisTemplate, String pattern) {
    return redisTemplate.execute((RedisCallback<Long>) connection -> {
        long count = 0;
        org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.keyCommands().scan(
            org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build()
        );
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        return count;
    });
}
```

### Pattern 2: @Async with dedicated TaskExecutor
**What:** Configure a thread pool for async operations, apply @Async to cache preload.
**When to use:** CachePreloadService.preloadCacheOnStartup blocks ApplicationReadyEvent.
**Example:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "cachePreloadExecutor")
    public Executor cachePreloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("cache-preload-");
        executor.initialize();
        return executor;
    }
}
```

### Pattern 3: KEK Envelope Encryption
**What:** Encrypt RSA private keys at rest using AES-256-GCM with a Key Encryption Key derived from a master secret.
**When to use:** SEC-01: RsaKeyPair.privateKey currently plaintext Base64.
**Example:**
```java
// Encrypt: AES/GCM/NoPadding with KEK from environment variable
// Store: encrypted_ciphertext_base64 in private_key column
// Decrypt: On-demand, only when signing/decrypting operations needed
// Key rotation: New KEK re-encrypts all existing private keys
```

### Anti-Patterns to Avoid
- **KEYS in production Redis:** `redisTemplate.keys(pattern)` blocks the Redis single thread, causing latency spikes for all connected clients. [CITED: redis.io/commands/keys]
- **@Transactional without readOnly on reads:** Forces Hibernate to track dirty checking, maintain snapshots, and skip read-only optimizations at the JDBC driver level. [CITED: spring.io/blog]
- **findByUserId without AndDeletedFalse:** Returns soft-deleted records that should be invisible to business logic. Compromises data isolation.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AES encryption | Custom cipher code | javax.crypto.Cipher + AES/GCM/NoPadding | Standard JDK library, audited implementation |
| Redis SCAN cursor | Custom key iteration | RedisTemplate.execute(RedisCallback) + ScanOptions | Handles cursor iteration, reconnects properly |
| Thread pool | Raw Thread/Runnable | ThreadPoolTaskExecutor | Spring lifecycle management, graceful shutdown |

## Common Pitfalls

### Pitfall 1: SCAN without COUNT hint
**What goes wrong:** Default SCAN COUNT (10) makes too many round-trips on large datasets.
**Why it happens:** SCAN default is 10 keys per iteration.
**How to avoid:** Set COUNT(100) or higher in ScanOptions.
**Warning signs:** Elevated Redis latency during cache statistics calls.

### Pitfall 2: @Async without explicit executor
**What goes wrong:** Falls back to SimpleAsyncTaskExecutor which creates unbounded threads.
**Why it happens:** No @EnableAsync or no TaskExecutor bean defined.
**How to avoid:** Define a dedicated TaskExecutor bean with bounded pool.
**Warning signs:** Thread count growth in production monitoring.

### Pitfall 3: KEK stored in application.yml
**What goes wrong:** KEK committed to git, leaked in config files.
**Why it happens:** Convenience during development.
**How to avoid:** Load KEK from environment variable only: `${RSA_KEK:}`.
**Warning signs:** KEK visible in application.yml or docker-compose.yml.

### Pitfall 4: Adding AndDeletedFalse to methods used for auth
**What goes wrong:** Login/auth flows that look up user by username suddenly exclude soft-deleted users that might be the login target.
**Why it happens:** Blindly adding AndDeletedFalse to all findBy methods.
**How to avoid:** Audit each call site. Auth lookup methods (findByUsername, findByPhone) intentionally skip soft-delete check since deleted users cannot log in anyway. Methods querying business entities (Enterprise, Reviewer, etc.) need AndDeletedFalse.
**Warning signs:** Login failures for specific user accounts after migration.

### Pitfall 5: Flyway migration naming gap
**What goes wrong:** No V3 migration exists (sequence goes V2 -> V4). Creating V6 works fine.
**Why it happens:** V3 was likely removed during development.
**How to avoid:** Use V6__ prefix for the next migration. Do NOT renumber existing migrations.

## Code Examples

### PERF-02: KEYS to SCAN conversion (CachePreloadService:181-194)
```java
// Current code (line 187):
Set<String> keys = redisTemplate.keys(cacheName + "*");
if (keys != null) {
    totalKeys += keys.size();
}

// Replacement pattern:
Long keyCount = redisTemplate.execute((RedisCallback<Long>) connection -> {
    long count = 0;
    try (var cursor = connection.keyCommands().scan(
            ScanOptions.scanOptions().match(cacheName + "*").count(100).build())) {
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
    }
    return count;
});
totalKeys += keyCount != null ? keyCount : 0;
```

### PERF-04: Unbounded List queries (AuctionOrderRepository:25, 30)
```java
// Current (returns unbounded List for matching algorithm):
List<AuctionOrder> findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc(
    Integer direction, List<Integer> statuses);

List<AuctionOrder> findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc(
    Integer direction, List<Integer> statuses);
```
**Note:** These are used by `executeMatching()` which needs ALL active orders for the matching algorithm. They cannot be paginated without fundamentally changing the matching algorithm. The "List to Page" conversion should apply to any consumer-facing query endpoints, not the internal matching engine. The repository already has Page<> variants for user-facing queries (lines 35, 40, 45, 50). Verify whether there are any controller-facing List<> returns that should be Page<>.

### QUAL-01: @Transactional(readOnly=true) pattern
```java
// Current (all 47 @Transactional annotations):
@Transactional
public CarbonReportResponse getReport(Long reportId) { ... }

// Target for read-only methods:
@Transactional(readOnly = true)
public CarbonReportResponse getReport(Long reportId) { ... }
```

### QUAL-02: Missing AndDeletedFalse examples
```java
// Current - CarbonCoinAccountRepository:17
Optional<CarbonCoinAccount> findByUserId(Long userId);
// CarbonCoinAccount extends BaseEntity (has soft delete), needs AndDeletedFalse

// Current - CarbonCoinTransactionRepository:19, 21
Page<CarbonCoinTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
Page<CarbonCoinTransaction> findByUserIdAndTxTypeOrderByCreatedAtDesc(Long userId, Integer txType, Pageable pageable);
// CarbonCoinTransaction extends BaseEntity, needs AndDeletedFalse

// Current - EmissionRatingRepository:18, 20, 22
List<EmissionRating> findByEnterpriseIdOrderByRatingYearDesc(Long enterpriseId);
Optional<EmissionRating> findByEnterpriseIdAndRatingYear(Long enterpriseId, String ratingYear);
List<EmissionRating> findByRatingYearOrderByTotalEmissionAsc(String ratingYear);
// EmissionRating extends BaseEntity, all need AndDeletedFalse

// Current - ReviewerRepository:22
Optional<Reviewer> findByUserId(Long userId);
// Reviewer extends BaseEntity, needs AndDeletedFalse

// Current - ThirdPartyOrgRepository:22
Optional<ThirdPartyOrg> findByUserId(Long userId);
// ThirdPartyOrg extends BaseEntity, needs AndDeletedFalse

// Current - AuthenticatorRepository:20
Optional<Authenticator> findByUserId(Long userId);
// Authenticator extends BaseEntity, needs AndDeletedFalse
```

**IMPORTANT NUANCE on QUAL-02:** Some of these methods (e.g., `EnterpriseRepository.findByUserId` without AndDeletedFalse) are used alongside `findByUserIdAndDeletedFalse` in the same service. The non-AndDeletedFalse versions may be intentional for internal/admin use. Each call site must be audited before blindly adding AndDeletedFalse. The `UserRepository` uses `findByUsername` without AndDeletedFalse -- this is correct because deleted users need to be found during login to return a proper "account disabled" error.

## Detailed Requirement Analysis

### PERF-02: Redis KEYS to SCAN

**Current State:**
- Exactly **1 call site** in production code: `CachePreloadService.java:187`
- Test code mirrors this at `CachePreloadServiceTest.java:117,118,131`
- The method `getCacheStatistics()` iterates cache names and calls `redisTemplate.keys(cacheName + "*")` to count keys
- `RateLimitAspect.java:46` uses `KEYS[1]` in a Lua script -- this is NOT the Redis KEYS command; it is the Lua KEYS table variable used with EVAL. No change needed.

**Required Change:**
- Replace `redisTemplate.keys(pattern)` with `RedisTemplate.execute(RedisCallback)` using SCAN
- Add `import org.springframework.data.redis.core.ScanOptions`
- Update test to verify SCAN-based behavior

**Dependencies:** None.
**Risks:** Low. SCAN is fully supported by Redis 7.
**Reusable Patterns:** None existing in codebase; new pattern.

---

### PERF-03: Cache Preload Async (@Async)

**Current State:**
- `CachePreloadService.preloadCacheOnStartup()` is triggered by `@EventListener(ApplicationReadyEvent.class)`
- It runs **synchronously**, blocking the application readiness signal
- No `@EnableAsync` configuration exists in the entire codebase
- No `TaskExecutor` beans configured

**Required Change:**
1. Create `AsyncConfig.java` with `@EnableAsync` and a `cachePreloadExecutor` bean
2. Add `@Async("cachePreloadExecutor")` to `preloadCacheOnStartup()`
3. Alternatively: keep the `@EventListener` synchronous but call an `@Async` delegate method (so that the preload is async but the event listener completes immediately)

**Dependencies:** None.
**Risks:**
- @Async on @EventListener methods: Spring proxies the @EventListener, so @Async works correctly on ApplicationReadyEvent
- Cache preload errors in async mode won't propagate; logging must be comprehensive (already is)
- Services calling cached data immediately after startup might hit cold cache

**Reusable Patterns:** None existing.

---

### PERF-04: Auction Order Query Pagination

**Current State:**
- `AuctionOrderRepository` has two `List<>` returning methods (lines 25, 30):
  - `findByDirectionAndStatusInAndDeletedFalseOrderByPriceDesc`
  - `findByDirectionAndStatusInAndDeletedFalseOrderByPriceAsc`
- Both are used by `DoubleAuctionService.executeMatching()` (lines 155-160)
- The matching algorithm needs ALL active orders to perform double-sided matching
- The repository already has `Page<>` variants for user-facing queries (lines 35, 40, 45, 50)

**Required Change:**
- The `executeMatching()` algorithm fundamentally requires all active orders; paginating it would break matching correctness
- The actual risk is unbounded growth of active orders over time
- **Recommendation:** Add a `Pageable` parameter with a reasonable cap (e.g., top 200) to the List-returning repository methods, OR add a `findTopN` variant
- Verify controller-facing methods do not expose List<> returns

**Dependencies:** None.
**Risks:**
- Capping matching results could miss valid matches at the tail
- The requirement says "List to Page" but the matching algorithm semantically needs all results
- May need to document this as a deliberate architectural tradeoff

**Reusable Patterns:** `findTop60ByDeletedFalseOrderByCreatedAtDesc` (line 55) shows an existing bounded query pattern.

---

### PERF-05: Foreign Key Index Supplement

**Current State:**
- V1__init_schema.sql defines 21 tables
- Most FK columns (`user_id`, `enterprise_id`, `reviewer_id`, `buyer_id`, `seller_id`, etc.) have NO index
- Only `operation_log` (V1) and `enterprise_admission` (V4) have explicit FK indexes
- Tables with unique constraints on FK columns (e.g., `uk_enterprise_user_id`) get implicit indexes from MySQL

**FK columns missing indexes (comprehensive list):**

| Table | FK Column | Has Index? | Has UK/PK Covering? |
|-------|-----------|------------|---------------------|
| `enterprise` | `user_id` | No | Yes (uk_enterprise_user_id) |
| `reviewer` | `user_id` | No | Yes (uk_reviewer_user_id) |
| `reviewer_qualification` | `reviewer_id` | No | No |
| `third_party_org` | `user_id` | No | Yes (uk_third_party_org_user_id) |
| `authenticator` | `user_id` | No | Yes (uk_authenticator_user_id) |
| `carbon_report` | `enterprise_id` | No | No |
| `carbon_report` | `submitter_id` | No | No |
| `carbon_report` | `reviewer_id` | No | No |
| `transaction` | `seller_id` | No | No |
| `transaction` | `buyer_id` | No | No |
| `transaction` | `report_id` | No | No |
| `auction_order` | `user_id` | No | No |
| `matching_result` | `buy_order_id` | No | No |
| `matching_result` | `sell_order_id` | No | No |
| `matching_result` | `buyer_id` | No | No |
| `matching_result` | `seller_id` | No | No |
| `matching_result` | `transaction_id` | No | No |
| `rsa_key_pair` | `user_id` | No | No |
| `credit_score` | `enterprise_id` | No | Yes (uk_credit_score_enterprise_id) |
| `credit_event` | `enterprise_id` | No | No |
| `credit_event` | `related_report_id` | No | No |
| `credit_event` | `related_trade_id` | No | No |
| `credit_event` | `triggered_by` | No | No |
| `carbon_coin_account` | `user_id` | No | Yes (uk_carbon_coin_account_user_id) |
| `carbon_coin_transaction` | `user_id` | No | No |
| `carbon_coin_transaction` | `related_trade_id` | No | No |
| `carbon_coin_transaction` | `counterpart_id` | No | No |
| `emission_rating` | `enterprise_id` | No | No |
| `emission_rating` | `rated_by` | No | No |
| `carbon_neutral_project` | `owner_id` | No | No |
| `carbon_neutral_project` | `reviewer_id` | No | No |
| `carbon_neutral_project` | `verifier_id` | No | No |
| `enterprise_admission` | `enterprise_id` | No | Yes (V4 idx_enterprise_admission_enterprise_id) |

**Required Change:**
- Create `V6__add_fk_indexes.sql` (next available number after V5)
- Add indexes for FK columns that lack both explicit index and unique constraint coverage
- Skip columns already covered by unique key constraints (MySQL creates implicit indexes for UNIQUE)
- The `is_deleted` column is also commonly queried alongside FK columns; consider composite indexes

**Dependencies:** None. Flyway will execute on next startup.
**Risks:**
- Large tables: index creation locks the table (use `ALGORITHM=INPLACE` or `LOCK=NONE` for online DDL in MySQL 8)
- Flyway runs at startup; large tables may cause slow startup
- In development/staging with small datasets, this is negligible

---

### SEC-01: RSA Private Key Encrypted Storage

**Current State:**
- `RsaKeyPair.java:45` -- `privateKey` stored as plaintext Base64 string in MySQL `TEXT` column
- `DigitalSignatureService.generateKeyPair()` (line 94) calls `RsaKeyUtil.encodeKey(keyPair.getPrivate())` and saves directly
- `DigitalSignatureService.signReport()` (line 171) calls `RsaKeyUtil.decodePrivateKey(keyPair.getPrivateKey())` to use the key
- `DigitalSignatureService.decryptForReviewer()` (line 294) and `decryptForEnterprise()` (line 330) also use private key
- `RsaKeyPair` entity extends `BaseEntity` with soft delete
- `RsaKeyPairRepository` has 5 query methods, all already include `AndDeletedFalse` (good)

**Required Change:**
1. Add a KEK (Key Encryption Key) loaded from environment variable `${RSA_KEK:}`
2. Create an encryption utility (e.g., `AesGcmEncryptor`) using AES-256-GCM
3. Modify `DigitalSignatureService.generateKeyPair()` to encrypt before saving
4. Modify `DigitalSignatureService` private key usage to decrypt on demand
5. Create a data migration for existing plaintext keys (Flyway V7 or inline migration script)
6. Add startup validation that RSA_KEK is present

**Dependencies:**
- Environment variable `RSA_KEK` must be set in all deployment environments
- docker-compose.yml / .env.example must be updated

**Risks:**
- Data migration: existing plaintext keys must be encrypted without downtime
- KEK loss: if RSA_KEK is lost, all existing private keys become unrecoverable
- Performance: AES-GCM decrypt is negligible for per-request signing operations
- Backward compatibility: need migration strategy for existing keypairs

**Reusable Patterns:** None existing; must create new.

---

### SEC-02: CSRF Protection ADR

**Current State:**
- `SecurityConfig.java:58` -- `.csrf(AbstractHttpConfigurer::disable)`
- Inline comment (lines 53-57) explains the decision:
  - JWT-based stateless API is not vulnerable to CSRF
  - Tokens stored in sessionStorage (not cookies)
  - Spring Security 6.x deferred CSRF tokens caused POST 401 issues
- The disable is intentional and well-reasoned

**Required Change:**
- Create a formal ADR (Architecture Decision Record) document
- Document the decision context, alternatives considered, consequences
- Store in `docs/adr/ADR-001-csrf-disabling.md` or similar location

**Dependencies:** None.
**Risks:** Documentation-only change, no functional impact.
**Reusable Patterns:** Standard ADR format (Context, Decision, Status, Consequences).

---

### QUAL-01: @Transactional(readOnly=true) for Read Operations

**Current State:**
- **47 total** `@Transactional` annotations across all service classes
- **Zero** use `readOnly = true`
- The `@Transactional` usage breaks down as:
  - Write operations (create, update, delete, save): ~35 annotations -- correctly use `@Transactional` without readOnly
  - Read operations (get, list, find, search, check): ~12 annotations that should have `readOnly=true`
  - Mixed operations (getOrCreate, login which updates lastLoginTime): ~2-3 annotations -- these do writes and should NOT be readOnly

**Read-only methods needing `@Transactional(readOnly = true)`:**

| Service | Line | Method | Notes |
|---------|------|--------|-------|
| CachePreloadService | 181 | `getCacheStatistics()` | Read-only |
| DigitalSignatureService | 138 | `getKeyPair(Long userId)` | Read-only |
| DigitalSignatureService | 200 | `verifySignature(...)` | Read-only |
| CarbonService | 182 | `getReport(Long reportId)` | Read-only |
| CarbonCoinService | 182 | `getTransactions(...)` | Read-only |
| EnterpriseService | 32 | `getEnterpriseInfo(Long userId)` | Read-only |
| EnterpriseService | 40 | `getQuotaInfo(Long userId)` | Read-only |
| EnterpriseService | 88 | `getEnterpriseById(Long enterpriseId)` | Read-only |
| EmissionRatingService | 44 | `getEnterpriseRatings(Long enterpriseId)` | Read-only |
| EmissionRatingService | 89 | `getIndustryRanking(String year)` | Read-only |
| CreditScoreService | 92 | `getScore(Long enterpriseId)` | May auto-create -- verify |
| CreditScoreService | 286 | `checkTradePermission(Long enterpriseId)` | Read-only |
| CreditScoreService | 301 | `getScoreByUserId(Long userId)` | Read-only |
| CreditScoreService | 265 | `getRestrictedEnterprises()` | Read-only |
| CreditScoreService | 274 | `getFrozenEnterprises()` | Read-only |
| ReviewerService | 37 | `getReviewerInfo(Long userId)` | Read-only |
| ReviewerService | 46 | `getPendingReports(...)` | Read-only |
| ReviewerService | 58 | `getReviewHistory(...)` | Read-only |
| ReviewerService | 70 | `getStatistics(Long userId)` | Read-only |
| ThirdPartyService | 42 | `getCurrentOrg(JwtUserDetails)` | Read-only |
| TradeService | 211 | `getTrade(Long tradeId, ...)` | Read-only |
| UserService | 36 | `getCurrentUserInfo(JwtUserDetails)` | Read-only |
| UserService | 47 | `getUserById(Long userId)` | Read-only |
| MinioService | 237 | `fileExists(String objectName)` | No DB transaction, but annotated |
| MinioService | 255 | `getFileInfo(String objectName)` | No DB transaction |
| MinioService | 280 | `getPresignedUrl(String objectName)` | No DB transaction |
| MinioService | 322 | `listFiles(...)` | No DB transaction |

**Important:** Methods that do NOT have `@Transactional` but are read-only (e.g., `listOrders`, `listMyOrders`, `listMatchingResults`, `searchProjects`, `getMyProjects`, `listTrades`, `listMyTrades`, `getCreditHistory`, `getScoreRanking`) don't need the annotation added -- Spring Data JPA repository methods are transactional by default. Only fix methods that already have `@Transactional` without readOnly.

**Also important:** Some methods currently annotated `@Transactional` that LOOK read-only actually do writes. For example:
- `CreditScoreService.getScore()` (line 91) has `@Transactional` and calls `orElseGet()` which may auto-create a CreditScore entity -- this is a **write** operation, should NOT be readOnly
- `AuthService.login()` updates `lastLoginTime` and `lastLoginIp` -- this is a **write** operation

**Required Change:**
- Add `readOnly = true` to genuinely read-only methods that already have `@Transactional`
- Do NOT add `@Transactional(readOnly=true)` to methods that don't already have `@Transactional` (JPA repository methods handle their own transactions)

**Dependencies:** None.
**Risks:**
- Misclassifying a write method as readOnly will cause `HibernateException: Read-only transactions not allowed for write operations`
- Test suite should catch this if coverage is adequate

---

### QUAL-02: Repository findByUserId AndDeletedFalse

**Current State:**
All 22 repository interfaces audited. The following methods on entities extending `BaseEntity` (soft-delete) are missing `AndDeletedFalse`:

| Repository | Line | Method | Severity |
|------------|------|--------|----------|
| `AuthenticatorRepository` | 20 | `findByUserId(Long userId)` | HIGH |
| `CarbonCoinAccountRepository` | 17 | `findByUserId(Long userId)` | HIGH |
| `CarbonCoinAccountRepository` | 19 | `existsByUserId(Long userId)` | MEDIUM |
| `CarbonCoinTransactionRepository` | 19 | `findByUserIdOrderByCreatedAtDesc(...)` | HIGH |
| `CarbonCoinTransactionRepository` | 21 | `findByUserIdAndTxTypeOrderByCreatedAtDesc(...)` | HIGH |
| `CarbonCoinTransactionRepository` | 23 | `sumAmountByUserIdAndTxType(...)` | HIGH |
| `EmissionRatingRepository` | 18 | `findByEnterpriseIdOrderByRatingYearDesc(...)` | HIGH |
| `EmissionRatingRepository` | 20 | `findByEnterpriseIdAndRatingYear(...)` | HIGH |
| `EmissionRatingRepository` | 22 | `findByRatingYearOrderByTotalEmissionAsc(...)` | MEDIUM |
| `ReviewerRepository` | 22 | `findByUserId(Long userId)` | HIGH |
| `ThirdPartyOrgRepository` | 22 | `findByUserId(Long userId)` | HIGH |

**Methods that DO NOT need AndDeletedFalse (intentionally without):**

| Repository | Method | Reason |
|------------|--------|--------|
| `UserRepository.findByUsername` | Auth lookup | Deleted users need to be found to return proper error |
| `UserRepository.findByPhone` | Auth lookup | Same reason |
| `EnterpriseRepository.findByUserId` (line 22) | Has AndDeletedFalse variant (line 27) | Legacy method used in some flows |
| `EnterpriseRepository.findByCreditCode` | Business key lookup | Credit code is unique across deleted/active |
| `ReviewerRepository.findByQualificationNo` | Business key lookup | Same reasoning |
| `TransactionRepository.findByTradeNo` | Business key lookup | Trade numbers are globally unique |
| `CarbonReportRepository.findByReportNo` | Business key lookup | Report numbers are globally unique |
| `MatchingResultRepository.findByUserIdRelated` | JPQL with explicit `m.deleted = false` | Already filters in query |

**Required Change:**
- Add `AndDeletedFalse` to each method listed above
- For each method, also check and update call sites in services (some callers may be relying on the current behavior to find soft-deleted records)
- Methods using `@Query` with explicit `deleted = false` in JPQL do NOT need renaming

**Dependencies:** None.
**Risks:**
- `CarbonCoinAccountRepository.findByUserId` is called from `CarbonCoinService.getOrCreateAccount()` which has `@Transactional` -- the `orElseGet()` creates account if missing, so the read must NOT find deleted accounts. Adding AndDeletedFalse is correct here.
- `EmissionRating` methods: emission ratings are queried by enterprise_id. If an enterprise is soft-deleted, their emission ratings should probably still be accessible for historical purposes. Verify with business requirements.
- Call site audit is mandatory for each method change.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `KEYS` command | `SCAN` cursor | Redis 2.8+ | Non-blocking key iteration |
| `@Transactional` everywhere | `@Transactional(readOnly=true)` for reads | Spring Best Practice | Hibernate flush optimization, JDBC read-only hint |
| Plaintext secrets in DB | KEK envelope encryption | OWASP ASVS V2 | Encrypted at rest |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | PERF-04 matching List<> methods can remain as-is since the algorithm needs all results | PERF-04 | If the requirement strictly mandates pagination, a bounded query approach is needed |
| A2 | `CreditScoreService.getScore()` auto-creates via `orElseGet()`, so it must remain writable `@Transactional` | QUAL-01 | If orElseGet is removed, method could become readOnly |
| A3 | EmissionRating soft-delete filtering may not be desired for historical queries | QUAL-02 | Business requirements may override technical recommendation |
| A4 | No V3 migration exists; V6 is the correct next Flyway version | PERF-05 | Confirmed by directory listing showing only V1, V2, V4, V5 |

## Open Questions

1. **PERF-04 -- List vs Page for matching:** The requirement says "auction order query pagination (List to Page)". The matching algorithm fundamentally needs all active orders. Should we:
   - (a) Add a bounded cap (e.g., top 500) to the List queries?
   - (b) Add new Page<> methods alongside the existing List<> methods?
   - (c) Document the matching engine as an exception and only paginate consumer-facing endpoints?
   - Recommendation: (c) with optional (a) as a safety cap.

2. **SEC-01 -- Data migration strategy:** Should existing plaintext private keys be migrated:
   - (a) In a Flyway migration using a Java-based migration callback?
   - (b) As a one-time startup task in the application?
   - (c) Lazily (encrypt on next read)?
   - Recommendation: (a) with a Flyway Java migration for deterministic rollout.

3. **QUAL-02 -- EmissionRating soft-delete semantics:** Should emission ratings for soft-deleted enterprises still be queryable? This affects whether `findByEnterpriseIdOrderByRatingYearDesc` should add `AndDeletedFalse`.

## Environment Availability

Step 2.6: SKIPPED (no new external dependencies -- all changes use existing JDK, Spring Boot, and MySQL capabilities).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (unit), Testcontainers (integration) |
| Config file | `pom.xml` (Surefire + Failsafe plugins) |
| Quick run command | `cd oaiss-chain-backend && mvn test -pl . -Dtest=CachePreloadServiceTest -DfailIfNoTests=false` |
| Full suite command | `cd oaiss-chain-backend && mvn verify` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERF-02 | SCAN replaces KEYS in CachePreloadService | unit | `mvn test -Dtest=CachePreloadServiceTest` | YES |
| PERF-03 | Cache preload runs async | unit | `mvn test -Dtest=CachePreloadServiceTest` | YES (needs update) |
| PERF-04 | Bounded auction queries | unit | `mvn test -Dtest=DoubleAuctionServiceTest` | Needs creation |
| PERF-05 | FK indexes exist | integration | Flyway validates on startup | N/A (migration) |
| SEC-01 | Private key encrypted at rest | unit | `mvn test -Dtest=DigitalSignatureServiceTest` | Needs creation |
| SEC-02 | ADR document exists | manual | File existence check | N/A (doc) |
| QUAL-01 | readOnly on read methods | unit | `mvn test -Dtest=*ServiceTest` | Partial |
| QUAL-02 | AndDeletedFalse on queries | unit | `mvn test -Dtest=*RepositoryTest` | Needs creation |

### Sampling Rate
- **Per task commit:** `mvn test -pl .`
- **Per wave merge:** `mvn verify`
- **Phase gate:** Full suite green before verify-work

### Wave 0 Gaps
- [ ] `DigitalSignatureServiceTest.java` -- covers SEC-01 encryption round-trip
- [ ] `DoubleAuctionServiceTest.java` -- covers PERF-04 bounded queries
- [ ] Repository-level tests for QUAL-02 AndDeletedFalse verification
- [ ] `AsyncConfig` integration test for PERF-03

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Already covered by JWT in Phase 13 |
| V3 Session Management | no | Stateless JWT, no sessions |
| V4 Access Control | no | No changes to authz model |
| V5 Input Validation | no | No new inputs |
| V6 Cryptography | yes | SEC-01: AES-256-GCM envelope encryption for RSA private keys |

### Known Threat Patterns for Spring Boot + MySQL

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Plaintext secrets in DB | Information Disclosure | AES-256-GCM envelope encryption (SEC-01) |
| CSRF token bypass | Spoofing | ADR documents decision + sessionStorage JWT (SEC-02) |
| Full table scan on FK | Denial of Service | FK index migration (PERF-05) |
| Redis KEYS blocking | Denial of Service | SCAN replacement (PERF-02) |

## Sources

### Primary (HIGH confidence)
- Codebase analysis: Direct file reads of all repository, service, config, entity, and migration files
- V1__init_schema.sql: Complete schema with index audit
- Spring Boot 3.2 application.yml: Redis, JPA, Flyway configuration verified

### Secondary (MEDIUM confidence)
- Redis SCAN documentation pattern: Standard Redis best practice [ASSUMED]
- Spring @Async + @EventListener interaction: Standard Spring behavior [ASSUMED]

### Tertiary (LOW confidence)
- None -- all findings verified by direct codebase inspection

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all verified from pom.xml and application.yml
- Architecture: HIGH - direct codebase inspection of all relevant files
- Pitfalls: HIGH - derived from concrete code patterns found in the codebase

**Research date:** 2026-05-20
**Valid until:** 2026-06-20 (stable codebase, no framework upgrades expected)
