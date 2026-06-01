---
phase: 13-concurrency-credential-hardening
reviewed: 2026-05-21T12:00:00Z
depth: deep
files_reviewed: 21
files_reviewed_list:
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/DistributedLockAspect.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/Enterprise.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CarbonCoinAccount.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/AuctionOrder.java
  - oaiss-chain-backend/src/main/resources/db/migration/V5__add_optimistic_lock_version.sql
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/aop/DistributedLockAspectOrderTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceLockTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/entity/OptimisticLockTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceTest.java
  - docker-compose.yml
  - docker-compose.infra.yml
  - .env.example
  - oaiss-chain-backend/src/main/resources/application.yml
  - oaiss-chain-backend/src/main/resources/application-local.yml
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityStartupValidator.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/config/SecurityStartupValidatorTest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java
findings:
  critical: 3
  warning: 5
  info: 3
  total: 11
status: issues_found
---

# Phase 13: Code Review Report

**Reviewed:** 2026-05-21T12:00:00Z
**Depth:** deep
**Files Reviewed:** 21
**Status:** issues_found

## Summary

Phase 13 covers three areas: concurrency safety (distributed locks + optimistic locking), credential hardening, and authorization hardening. The implementation is mostly sound -- the `@DistributedLock` on `executeMatching()`, the `@Order(HIGHEST_PRECEDENCE)` on `DistributedLockAspect`, and the Flyway migration are all correct. The authorization changes to FileController, SearchController, SecurityConfig, and JwtAuthenticationFilter are well-executed.

However, the review uncovered **3 critical issues** that could cause production failures or security gaps:

1. **No `ObjectOptimisticLockingFailureException` handler anywhere in the codebase** -- the new `@Version` fields will cause unhandled 500 errors when concurrent updates conflict, with no retry and no user-friendly message.
2. **Hardcoded `adminpw` in `application.yml` and `FabricProperties.java`** -- the credential hardening plan (13-02) explicitly aimed to externalize all hardcoded credentials but missed the Fabric CA admin password.
3. **TradeService and CarbonCoinService modify `@Version` entities without `@DistributedLock`** -- concurrent financial operations on `Enterprise` and `CarbonCoinAccount` will trigger the unhandled optimistic lock exception.

Additionally, there is a **security inconsistency** between `JwtAuthenticationFilter` and `SecurityConfig` regarding Swagger/API-docs paths, and the `FileController.downloadFile` endpoint lacks role restriction.

## Critical Issues

### CR-01: No ObjectOptimisticLockingFailureException handler -- `@Version` will cause 500 errors

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/exception/GlobalExceptionHandler.java` (missing)
**Related files:** `Enterprise.java:106`, `CarbonCoinAccount.java:64`, `AuctionOrder.java:83`
**Issue:** The `@Version` annotation was added to three financial entities (Enterprise, CarbonCoinAccount, AuctionOrder) but no code anywhere in the service layer or exception handling catches `ObjectOptimisticLockingFailureException`. When two concurrent requests update the same entity row (e.g., two trades modifying the same Enterprise's carbon quota simultaneously), JPA will throw this exception. The `GlobalExceptionHandler` has no `@ExceptionHandler` for it, so it falls through to the generic `Exception.class` handler, returning a 500 Internal Server Error with the generic "system error" message.

The services most affected:
- `TradeService` (lines 142-177): calls `enterpriseRepository.save()` on Enterprise entities without `@DistributedLock`
- `CarbonCoinService` (lines 52-163): calls `accountRepository.save()` on CarbonCoinAccount entities without `@DistributedLock`
- `DoubleAuctionService.updateEnterpriseQuota()` (lines 340-353): calls `enterpriseRepository.save()` within the locked `executeMatching()` method -- this is safe due to the distributed lock, but the `placeBuyOrder`/`placeSellOrder` methods also modify entities

There is no retry mechanism and no user-facing error message for this expected concurrency scenario.

**Fix:**
Add an `@ExceptionHandler` to `GlobalExceptionHandler.java`:
```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(
        ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
    log.warn("Concurrent modification conflict: {} - {}", request.getRequestURI(), ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorCode.OPERATION_IN_PROGRESS, "数据已被其他操作修改，请刷新后重试"));
}
```
Additionally, consider adding retry logic in services that modify `@Version` entities, or add `@DistributedLock` to `TradeService` and `CarbonCoinService` methods.

### CR-02: Hardcoded `adminpw` credential in application.yml and FabricProperties.java

**File:** `oaiss-chain-backend/src/main/resources/application.yml:189`
**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricProperties.java:28`
**Issue:** Plan 13-02 (SEC-07, SEC-08, SEC-09) explicitly required externalizing all hardcoded credentials, but the Fabric CA admin password remains hardcoded as `"adminpw"` in two places:
1. `application.yml` line 189: `admin-password: adminpw`
2. `FabricProperties.java` line 28: `private String adminPassword = "adminpw";`

This is the same class of defect that was fixed for DB passwords, Redis passwords, MinIO keys, and JWT secrets. The `SecurityStartupValidator` also does not check for weak Fabric credentials. While `fabric.enabled=false` by default, this is still a hardcoded secret in source code.

The `docker-compose.fabric.yml` file also uses `admin:adminpw` in multiple places (CouchDB, Fabric CA bootstrap) but these are infrastructure config files that are acceptable as reference templates -- the Java code hardcoding is the real issue.

**Fix:**
Externalize to environment variables:
```yaml
# application.yml
fabric:
  ca:
    admin-password: ${FABRIC_CA_ADMIN_PASSWORD:adminpw}
```
```java
// FabricProperties.java
private String adminPassword = "adminpw"; // keep as default, but allow override
```
Or better: remove the default entirely and require the env var when `fabric.enabled=true`.

### CR-03: TradeService and CarbonCoinService lack distributed locks for financial entity modifications

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/TradeService.java:53,107,142`
**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonCoinService.java:52,71,93,116,137`
**Issue:** `TradeService` modifies `Enterprise` entities (via `enterpriseRepository.save()`) at lines 176-177 without `@DistributedLock`. `CarbonCoinService` modifies `CarbonCoinAccount` entities (via `accountRepository.save()`) at lines 81, 104, 125, 162-163 without `@DistributedLock`. Both entities now have `@Version` fields.

While the `@Version` provides a safety net (preventing silent lost updates), the combination of:
1. No distributed lock (concurrent requests can enter the same transaction)
2. No `ObjectOptimisticLockingFailureException` handler (CR-01)
3. No retry mechanism

means that concurrent financial operations will fail with an unhandled exception rather than being serialized correctly. This is a correctness and reliability issue for a financial platform.

For `CarbonCoinService` in particular: operations like `recharge()`, `buyQuota()`, `sellQuota()`, and `transfer()` all read-modify-write the `CarbonCoinAccount` balance without any concurrency protection.

**Fix:**
Add `@DistributedLock` to financial mutation methods:
```java
// CarbonCoinService.java
@DistributedLock(key = "'carbon:coin:' + #userId", expireTime = 30)
@Transactional
public CarbonCoinAccountResponse recharge(Long userId, CarbonCoinRechargeRequest request) { ... }

// TradeService.java -- lock on seller ID to prevent concurrent trades
@DistributedLock(key = "'trade:seller:' + #currentUser.userId", expireTime = 30)
@Transactional
public TradeResponse createP2PTrade(JwtUserDetails currentUser, TradeRequest request) { ... }
```

## Warnings

### WR-01: JwtAuthenticationFilter whitelist bypasses JWT for Swagger/API-docs paths -- inconsistent with SecurityConfig

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java:51-53`
**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java:73-78`
**Issue:** The `JwtAuthenticationFilter` whitelist at lines 51-53 includes `"/swagger-ui"`, `"/v1/api-docs"`, and `"/v3/api-docs"`. The `isWhitelisted()` method (lines 148-159) performs prefix matching: `normalizedPath.startsWith(whitelist + "/")`. This means any path starting with `/swagger-ui` or `/v1/api-docs` bypasses JWT validation entirely -- the filter calls `filterChain.doFilter()` and returns immediately.

However, `SecurityConfig` (lines 73-78) requires `.authenticated()` for these same paths. The result is: the JWT filter skips these requests (no `SecurityContext` is populated), and Spring Security then sees no authenticated principal and returns 401. This is not a security hole -- the requests still require auth via SecurityConfig -- but it creates a confusing architecture where the filter's whitelist is meaningless for these paths. Worse, if someone later changes SecurityConfig to `permitAll()` for Swagger, the JWT filter will already be bypassing those paths.

The whitelist should match the SecurityConfig policy: if Swagger requires authentication, remove it from the filter's whitelist so the JWT token is actually processed.

**Fix:** Remove Swagger/API-docs entries from `JwtAuthenticationFilter.WHITELIST_PATHS` (lines 51-53), keeping only paths that are truly `permitAll()` in SecurityConfig:
```java
private static final List<String> WHITELIST_PATHS = List.of(
    "/api/v1/auth/login",
    "/api/v1/auth/register",
    "/api/v1/auth/captcha",
    "/api/v1/auth/refresh",
    "/api/v1/auth/check-ip",
    "/api/v1/captcha/",
    "/actuator/health"
);
```

### WR-02: FileController.downloadFile lacks role restriction -- any authenticated user can download any file

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java:114`
**Issue:** The class-level `@PreAuthorize("isAuthenticated()")` applies to all endpoints, but `downloadFile()` (line 114) has no additional role restriction. This means any authenticated user with any role (ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN) can download any file by object name. There is no ownership check on download -- the `checkDeletePermission` method exists for delete operations but there is no equivalent for downloads.

While the presigned URL endpoints are restricted to ENTERPRISE/ADMIN, the direct download endpoint is not. A REVIEWER or THIRD_PARTY user could potentially download enterprise-sensitive files (carbon reports, certificates) if they know or can guess the object name.

**Fix:** Either add role restriction or ownership check to `downloadFile`:
```java
@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
@GetMapping("/download")
public void downloadFile(...) { ... }
```
Or add an ownership check similar to `checkDeletePermission`, or verify the requesting user's enterprise has access to the file.

### WR-03: placeBuyOrder reads enterprise carbonTradable but does not lock -- TOCTOU race condition

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java:75-100`
**Issue:** `placeBuyOrder()` (line 76) reads `enterprise.getCarbonTradable()` and compares it with `request.getQuantity()` (line 81) within a `@Transactional` block, but without any distributed lock. Between the read and the eventual `auctionOrderRepository.save()` (line 95), another request could have modified the enterprise's `carbonTradable`. The buy order is placed based on a stale quota value.

The same issue exists in `placeSellOrder()` (lines 105-131). While `@Version` on Enterprise will cause a concurrent modification to fail (see CR-01), the order itself is saved before the Enterprise is updated (in the matching phase), meaning an order can be placed that exceeds the actual available quota.

**Fix:** Add `@DistributedLock` keyed on user ID to `placeBuyOrder` and `placeSellOrder`:
```java
@DistributedLock(key = "'auction:order:' + #currentUser.userId", expireTime = 10)
@Transactional
public AuctionOrderResponse placeBuyOrder(JwtUserDetails currentUser, AuctionOrderRequest request) { ... }
```

### WR-04: Docker Compose MinIO ports exposed on all interfaces

**File:** `docker-compose.infra.yml:42-43`
**Issue:** MinIO ports are bound as `"9000:9000"` and `"9001:9001"` without a `127.0.0.1` prefix, exposing MinIO's API and console on all network interfaces. Compare with MySQL (`"127.0.0.1:3307:3306"` at line 12) and Redis (`"127.0.0.1:6379:6379"` at line 25) which are correctly restricted to localhost.

This is a defense-in-depth issue: even though MinIO requires authentication, exposing its API and web console publicly increases the attack surface. The `docker-compose.yml` (main) correctly uses `"9002:9000"` and `"9003:9001"` but also without `127.0.0.1` binding.

**Fix:**
```yaml
# docker-compose.infra.yml
ports:
  - "127.0.0.1:9000:9000"
  - "127.0.0.1:9001:9001"
```

### WR-05: Flyway V5 migration may fail on non-empty tables if JPA ddl-auto=validate

**File:** `oaiss-chain-backend/src/main/resources/db/migration/V5__add_optimistic_lock_version.sql`
**Issue:** The migration adds `version BIGINT NOT NULL DEFAULT 0` to three tables. This is correct for existing rows (DEFAULT 0 applies). However, if the application is running with `ddl-auto: validate` (as set in `application.yml` line 29), Hibernate will check the schema at startup. If any code path creates entity objects via `@Builder` without `@Builder.Default`, the `version` field would be null, and JPA would fail on insert.

The implementation correctly uses `@Builder.Default private Long version = 0L` on all three entities, so this is not a current bug. However, the risk remains that future developers adding new entity construction paths may forget the `@Builder.Default` and encounter `NULL` version values that violate the `NOT NULL` constraint. This is mitigated but worth noting as a maintenance risk.

No code change required -- flagging as awareness item.

## Info

### IN-01: Duplicate DistributedLock assertion tests in DoubleAuctionServiceTest and DoubleAuctionServiceLockTest

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceTest.java:352-364`
**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceLockTest.java:19-83`
**Issue:** `DoubleAuctionServiceTest.testExecuteMatchingHasDistributedLock()` (lines 352-364) duplicates the checks in `DoubleAuctionServiceLockTest` (which tests the annotation presence, key, expireTime, waitTime, and non-synchronized modifier). The test in `DoubleAuctionServiceTest` was likely the original C6 test that was replaced but not removed when the dedicated `DoubleAuctionServiceLockTest` was created.

**Fix:** Remove the duplicate test at `DoubleAuctionServiceTest.java:350-364` since `DoubleAuctionServiceLockTest` provides more thorough coverage.

### IN-02: OptimisticLockTest does not test concurrent update exception

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/entity/OptimisticLockTest.java`
**Issue:** The test class only verifies (1) the presence of `@Version` annotation and (2) the builder default value. The plan specified a test for "Concurrent update to the same entity row throws ObjectOptimisticLockingFailureException" but this test was not implemented. This would require an integration test with a real database (Testcontainers), so it may have been deferred for practical reasons. The missing test coverage is noted.

**Fix:** Consider adding an integration test that loads an entity in two persistence contexts, modifies both, and verifies the second save throws `ObjectOptimisticLockingFailureException`.

### IN-03: application.yml exposes actuator env endpoint with show-values: never

**File:** `oaiss-chain-backend/src/main/resources/application.yml:103`
**Issue:** The `management.endpoint.env.show-values: never` setting at line 103 is good practice but the `env` endpoint is not included in `management.endpoints.web.exposure.include` (line 93: only `health,info,metrics,prometheus`). This means the `env` endpoint configuration is inert. Not a defect -- just noting the dead configuration.

## Cross-File Analysis

### Lock-Before-Transaction Guarantee

The `@Order(Ordered.HIGHEST_PRECEDENCE)` on `DistributedLockAspect` (line 36) ensures the distributed lock aspect wraps the transaction proxy. Verified that:
- `DoubleAuctionService.executeMatching()` uses both `@DistributedLock` and `@Transactional` -- lock acquired first
- `DigitalSignatureService.generateKeyPair()` uses both `@DistributedLock` and `@Transactional` -- same pattern
- `EnterpriseAdmissionService` and `ReviewerQualificationService` use `@DistributedLock` with `@Transactional` -- same pattern

The `@Order` approach works correctly in Spring's default proxy model ( JDK dynamic proxies or CGLIB). However, if any service uses AspectJ compile-time or load-time weaving, the `@Order` annotation on the aspect class would be ignored and the ordering would need to be declared in the weaving configuration. This is not a current issue since the project uses Spring's proxy-based AOP.

### X-User-Id / X-User-Type Header Removal

Verified: no `X-User-Id` or `X-User-Type` references remain in any Java source file under `src/main/java`. The `FileController` now uses `@AuthenticationPrincipal JwtUserDetails currentUser` for identity resolution. The removal is complete.

### Remaining Hardcoded Credentials

| Location | Status |
|----------|--------|
| `docker-compose.yml` / `.infra.yml` | Externalized to `${ENV_VAR}` -- GOOD |
| `application.yml` (DB, Redis, MinIO, JWT) | Externalized to `${ENV_VAR}` -- GOOD |
| `application.yml` (Fabric CA adminpw) | **STILL HARDCODED** -- CR-02 |
| `FabricProperties.java` (adminpw default) | **STILL HARDCODED** -- CR-02 |
| `docker-compose.fabric.yml` (CouchDB, CA bootstrap) | Hardcoded but infrastructure template -- acceptable |
| `.env` / `oaiss-chain-backend/.env` | Contains real MinIO default creds (`minioadmin`) -- not tracked in git |

### Flyway V5 Compatibility

The migration `V5__add_optimistic_lock_version.sql` adds `BIGINT NOT NULL DEFAULT 0` to existing tables. This is safe for existing data -- all existing rows get `version=0`. The `@Builder.Default` annotation on the `version` field in all three entities ensures builder-constructed objects also default to `0L`. Verified that migrations V1-V4 exist and V5 is the next in sequence.

### Optimistic Lock Exception Handling Gap

Confirmed: zero references to `ObjectOptimisticLockingFailureException` or `OptimisticLockingFailureException` exist anywhere in `src/main/java`. The `GlobalExceptionHandler` does not handle this exception. Combined with the fact that `TradeService` and `CarbonCoinService` modify `@Version` entities without distributed locks, this creates a reliability gap where expected concurrency conflicts will surface as unhandled 500 errors.

---

_Reviewed: 2026-05-21T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
