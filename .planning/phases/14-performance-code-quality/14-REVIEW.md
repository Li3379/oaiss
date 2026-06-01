---
phase: 14-performance-code-quality
reviewed: 2026-05-21T12:00:00Z
depth: deep
files_reviewed: 11
files_reviewed_list:
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CachePreloadService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/AsyncConfig.java
  - oaiss-chain-backend/src/main/resources/db/migration/V6__add_fk_indexes.sql
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CachePreloadServiceTest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/util/AesGcmEncryptorTest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/RsaKeyPair.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/RsaKeyPairRepository.java
  - oaiss-chain-backend/src/main/resources/db/migration/V7__encrypt_rsa_private_keys.sql
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/util/AesGcmEncryptor.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DigitalSignatureServiceTest.java
  - oaiss-chain-backend/docs/adr/ADR-001-csrf-disabling.md
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MinioService.java
findings:
  critical: 3
  warning: 7
  info: 4
  total: 14
status: issues_found
---

# Phase 14: Code Review Report

**Reviewed:** 2026-05-21T12:00:00Z
**Depth:** deep
**Files Reviewed:** 14 (11 primary + 3 cross-file)
**Status:** issues_found

## Summary

Phase 14 covers two sub-plans: (1) performance optimization replacing Redis KEYS with SCAN, adding async config, and FK indexes; (2) security and code quality adding AES-256-GCM encryption for RSA private keys, @Transactional(readOnly=true) on read-only methods, and AndDeletedFalse on repository queries.

Three critical issues found: (1) `validateKeyStatus` performs a write (`rsaKeyPairRepository.save`) inside `getKeyPair` which has no `@Transactional`, causing a silent no-flush or `TransactionRequiredException`; the same write also leaks into methods annotated `@Transactional(readOnly=true)` like `verifySignature`, violating the read-only contract. (2) `RsaKeyMigrationRunner` uses a single `@Transactional` for all key migrations -- if any key fails mid-batch, all encrypted keys are rolled back while the DB column is already at `encrypted=1` from Flyway, leaving a confusing state. (3) The `CachePreloadServiceTest` mocks `redisTemplate.keys()` but the production code uses SCAN via `RedisCallback` -- the tests never exercise the actual implementation and will pass regardless of whether the SCAN logic works.

## Critical Issues

### CR-01: validateKeyStatus performs DB write inside non-transactional and readOnly methods

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DigitalSignatureService.java:357-368`
**Issue:** The `validateKeyStatus` private method calls `rsaKeyPairRepository.save(keyPair)` to auto-update expired key status. This method is called from:
1. `getKeyPair(Long userId)` (line 140) -- has **no** `@Transactional` annotation. The save either silently does nothing (no flush without a transaction) or throws `TransactionRequiredException` depending on the JPA provider configuration.
2. `verifySignature(Long userId, ...)` (line 202) -- has `@Transactional(readOnly = true)`. Writing inside a read-only transaction is provider-dependent: Hibernate may silently ignore the flush, or throw an exception at flush time.
3. `signReport`, `encryptForReviewer`, `decryptForReviewer`, `decryptForEnterprise` -- these have no `@Transactional(readOnly = true)` but also lack `@Transactional`, so the same no-transaction problem applies.

The auto-expiry feature is broken in all callers. Expired keys will never actually have their status updated to `KEY_STATUS_EXPIRED` in the database.

**Fix:**
```java
// Option A: Separate the status check from the status update.
// Remove the save from validateKeyStatus and create a dedicated method:
@Transactional
public void markExpiredKeys(Long userId) {
    rsaKeyPairRepository.findLatestByUserId(userId).ifPresent(kp -> {
        if (kp.getExpiresAt() != null && kp.getExpiresAt().isBefore(LocalDateTime.now())
                && kp.getKeyStatus() != KEY_STATUS_EXPIRED) {
            kp.setKeyStatus(KEY_STATUS_EXPIRED);
            rsaKeyPairRepository.save(kp);
        }
    });
}

// Option B: Add @Transactional to getKeyPair and keep readOnly off it, 
// and remove validateKeyStatus from verifySignature (read-only).
```

### CR-02: RsaKeyMigrationRunner single transaction for all keys -- partial failure rolls back all progress

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java:33-56`
**Issue:** The `migratePlaintextKeys()` method wraps the entire migration loop in a single `@Transactional`. If the encryption of the Nth key fails (e.g., KEK misconfiguration for a specific key size, transient DB error), the entire batch is rolled back. On next application restart, it re-attempts all keys from scratch. For deployments with many keys, this creates an unbounded retry loop with no forward progress.

Additionally, if the `AesGcmEncryptor` constructor fails (no `RSA_KEK` set), the `RsaKeyMigrationRunner` bean will fail to initialize, preventing the entire application from starting -- even though migration of existing keys is a background task that should not block startup.

**Fix:**
```java
@EventListener(ApplicationReadyEvent.class)
public void migratePlaintextKeys() {
    List<RsaKeyPair> unencryptedKeys = rsaKeyPairRepository.findByEncryptedAndDeletedFalse(0);
    if (unencryptedKeys.isEmpty()) {
        log.info("No unencrypted RSA private keys found -- migration skipped");
        return;
    }
    log.info("Found {} unencrypted RSA private key(s) -- starting migration", unencryptedKeys.size());
    int success = 0;
    for (RsaKeyPair keyPair : unencryptedKeys) {
        try {
            migrateSingleKey(keyPair);
            success++;
        } catch (Exception e) {
            log.error("Failed to migrate key id={}, userId={}: {}",
                keyPair.getId(), keyPair.getUserId(), e.getMessage());
            // Continue with next key instead of aborting entire batch
        }
    }
    log.info("RSA private key migration complete -- {}/{} key(s) encrypted",
        success, unencryptedKeys.size());
}

@Transactional
public void migrateSingleKey(RsaKeyPair keyPair) {
    String encryptedKey = aesGcmEncryptor.encrypt(keyPair.getPrivateKey());
    keyPair.setPrivateKey(encryptedKey);
    keyPair.setEncrypted(1);
    rsaKeyPairRepository.save(keyPair);
}
```

### CR-03: CachePreloadServiceTest mocks redisTemplate.keys() but production code uses SCAN

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CachePreloadServiceTest.java:117-118,131`
**Issue:** The test at lines 117-118 mocks `redisTemplate.keys("cache1*")` and `redisTemplate.keys("cache2*")` and asserts `totalKeys == 3`. However, the production `getCacheStatistics()` method (CachePreloadService.java:194-205) uses `redisTemplate.execute(RedisCallback)` with `connection.keyCommands().scan()` -- it never calls `redisTemplate.keys()`. The mocked `keys()` calls are never exercised by the code under test. The `redisTemplate.execute(RedisCallback)` call returns the unmocked default (`null`), which is handled by the null check `keyCount != null ? keyCount : 0L` at line 205.

This means the test asserts `totalKeys == 3` based on mocks that are never called. The actual SCAN implementation has zero test coverage. If the SCAN logic has a bug (e.g., wrong cursor handling, infinite loop), these tests would still pass.

**Fix:**
```java
@Test
@DisplayName("获取缓存统计信息")
void testGetCacheStatistics() {
    when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2"));
    when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
        RedisCallback<Long> callback = invocation.getArgument(0);
        // Simulate the callback behavior or return a fixed count
        return 3L;
    });

    CachePreloadService.CacheStatistics stats = cachePreloadService.getCacheStatistics();
    assertNotNull(stats);
    assertEquals(2, stats.totalCaches());
    // Verify the SCAN callback was actually invoked
    verify(redisTemplate, atLeastOnce()).execute(any(RedisCallback.class));
}
```

## Warnings

### WR-01: AesGcmEncryptor key length not validated -- arbitrary-length Base64 accepted

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/util/AesGcmEncryptor.java:37-38`
**Issue:** The constructor decodes the Base64 KEK and passes it directly to `SecretKeySpec` without validating key length. AES-256 requires exactly 32 bytes. If the environment variable contains a Base64 string that decodes to anything other than 16, 24, or 32 bytes, `SecretKeySpec` accepts it but `Cipher.init` will throw `InvalidKeyException` at runtime during the first encrypt/decrypt call. This deferred failure mode makes the root cause hard to diagnose.

**Fix:**
```java
byte[] keyBytes = Base64.getDecoder().decode(kekBase64);
if (keyBytes.length != 32) {
    throw new IllegalStateException(
        "RSA_KEK must decode to exactly 32 bytes (256 bits) for AES-256, got " + keyBytes.length);
}
this.keySpec = new SecretKeySpec(keyBytes, "AES");
```

### WR-02: MinioService methods have @Transactional(readOnly=true) but never access the database

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MinioService.java:238,257,283,326`
**Issue:** Four methods (`fileExists`, `getFileInfo`, `getPresignedUrl`, `listFiles`) are annotated with `@Transactional(readOnly = true)`. However, `MinioService` has no repository dependencies -- it only interacts with the MinIO client. The `@Transactional` annotation is misleading: it opens a read-only database transaction for no reason, consuming a connection from the connection pool for a purely network-I/O operation. Under load, this wastes connection pool resources.

**Fix:** Remove `@Transactional(readOnly = true)` from all four `MinioService` methods. They do not perform database operations.

### WR-03: CachePreloadService.getCacheStatistics has @Transactional(readOnly=true) but only accesses Redis

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CachePreloadService.java:187`
**Issue:** `getCacheStatistics()` only interacts with `CacheManager` and `RedisTemplate` -- it never touches JPA repositories or the database. The `@Transactional(readOnly = true)` annotation opens a database transaction for a Redis-only operation. Same concern as MinioService.

**Fix:** Remove `@Transactional(readOnly = true)` from `getCacheStatistics()`. It is a Redis-only operation.

### WR-04: refreshAllCaches calls @Async method synchronously within the same bean

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CachePreloadService.java:166-181`
**Issue:** `refreshAllCaches()` calls `preloadCacheOnStartup()` at line 180. However, `preloadCacheOnStartup()` is annotated with `@Async("cachePreloadExecutor")`. Spring's `@Async` only works when called through the proxy -- self-invocation within the same bean bypasses the proxy and runs synchronously. So `refreshAllCaches()` blocks on the preload instead of dispatching it asynchronously. Whether this is intentional is unclear, but it contradicts the `@Async` contract.

**Fix:** Either:
1. Remove the `@Async` annotation from `preloadCacheOnStartup()` and let it run synchronously always, or
2. Inject a self-reference (`@Lazy private CachePreloadService self`) and call `self.preloadCacheOnStartup()` to go through the proxy, or
3. Document that the synchronous call from `refreshAllCaches()` is intentional.

### WR-05: CachePreloadService still uses unused imports for SCAN

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CachePreloadService.java:13-14`
**Issue:** The imports `RedisCallback` and `ScanOptions` are present (lines 13-14), confirming SCAN is used in `getCacheStatistics`. However, the preload methods (`preloadUserTypeCache`, etc.) still use `redisTemplate.hasKey()` + `redisTemplate.opsForSet().add()` pattern rather than a SCAN-based approach for checking existence. The plan stated "Replaced Redis KEYS command with SCAN" but the `hasKey` calls in preload methods remain -- these are O(1) operations and acceptable, but the commit message could be misleading about the scope of the KEYS-to-SCAN migration.

This is informational only since `hasKey` is O(1) and does not use KEYS.

### WR-06: AesGcmEncryptor uses platform-default charset for String<->byte[] conversion

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/util/AesGcmEncryptor.java:52,78`
**Issue:** Line 52 uses `plaintext.getBytes()` (no charset specified) and line 78 uses `new String(ciphertext)` (no charset specified). These rely on the platform default charset. If the application runs on different JVM locales, encrypt on one and decrypt on another (e.g., different Docker images), the round-trip could fail or corrupt data.

**Fix:**
```java
// Line 52:
byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

// Line 78:
return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
```

### WR-07: RsaKeyMigrationRunner encrypts without null/guard check on privateKey

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java:45-46`
**Issue:** At line 45, `keyPair.getPrivateKey()` is passed to `aesGcmEncryptor.encrypt()` without a null check. If any row in `rsa_key_pair` has a NULL `private_key` column (the column is `nullable = false` in the entity, but the database may have pre-existing data), the encryptor will throw a `NullPointerException` or encrypt the literal string "null". The entity declares `nullable = false` but existing data may predate this constraint.

**Fix:** Add a null guard before encryption:
```java
String plaintextKey = keyPair.getPrivateKey();
if (plaintextKey == null || plaintextKey.isBlank()) {
    log.warn("Skipping RSA key pair id={} with null/blank private key", keyPair.getId());
    continue;
}
```

## Info

### IN-01: CachePreloadServiceTest uses @MockitoSettings(strictness = Strictness.LENIENT)

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CachePreloadServiceTest.java:30`
**Issue:** Using `Strictness.LENIENT` suppresses unnecessary stubbing and argument mismatch warnings. This can hide real test issues where mocks are set up incorrectly. The `lenient()` calls in `setUp()` (lines 50-51) are understandable for shared setup, but class-level `LENIENT` hides problems in all tests.

**Fix:** Prefer targeted `lenient().when(...)` only where needed, and use default `STRICT_STRICTNESS` at the class level.

### IN-02: ADR-001 CSRF Disabling -- accurate but should note cookie-based JWT session is a future risk

**File:** `oaiss-chain-backend/docs/adr/ADR-001-csrf-disabling.md`
**Issue:** The ADR correctly documents the rationale for disabling CSRF when using sessionStorage JWT. However, the CLAUDE.md project instructions state "Auth: JWT Bearer token + CSRF cookie" which contradicts the ADR's claim that tokens are in sessionStorage. This documentation inconsistency should be reconciled -- either the app uses CSRF cookies (in which case CSRF protection should be enabled) or it uses sessionStorage (in which case the CLAUDE.md should be updated).

### IN-03: AsyncConfig queueCapacity=0 may silently reject tasks

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/AsyncConfig.java:18`
**Issue:** `executor.setQueueCapacity(0)` means when the single thread is busy, additional submissions are rejected (default abort policy). No `RejectedExecutionHandler` is configured. If `refreshAllCaches` triggers `preloadCacheOnStartup` while another preload is running (race condition from manual refresh during startup), the task will be rejected with a `TaskRejectedException`. Since the self-invocation issue (WR-04) makes this synchronous anyway, the queue capacity setting is moot for the current code, but it becomes a latent bug if `@Async` is ever properly proxied.

### IN-04: V7 migration has no IF NOT EXISTS guard for the column

**File:** `oaiss-chain-backend/src/main/resources/db/migration/V7__encrypt_rsa_private_keys.sql:2`
**Issue:** `ALTER TABLE rsa_key_pair ADD COLUMN encrypted TINYINT(1) NOT NULL DEFAULT 0` lacks `IF NOT EXISTS`. Flyway tracks applied migrations, so this is normally safe. However, if someone manually ran this migration and forgot to update the Flyway schema history, re-running would fail. This is a minor defensive coding concern, consistent with Flyway's normal behavior.

---

_Reviewed: 2026-05-21T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
