---
phase: 13-concurrency-credential-hardening
fixed_at: 2026-05-21T14:30:00Z
review_path: .planning/phases/13-concurrency-credential-hardening/13-REVIEW.md
iteration: 1
findings_in_scope: 7
fixed: 7
skipped: 0
status: all_fixed
---

# Phase 13: Code Review Fix Report

**Fixed at:** 2026-05-21T14:30:00Z
**Source review:** .planning/phases/13-concurrency-credential-hardening/13-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 7
- Fixed: 7
- Skipped: 0

## Fixed Issues

### CR-01: Add ObjectOptimisticLockingFailureException handler

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/exception/GlobalExceptionHandler.java`
**Commit:** da9b465
**Applied fix:** Added `@ExceptionHandler(ObjectOptimisticLockingFailureException.class)` method returning HTTP 409 CONFLICT with message "数据已被其他操作修改，请刷新后重试". Uses existing `ErrorCode.OPERATION_IN_PROGRESS`. Logs at WARN level.

### CR-02: Externalize adminpw credential

**Files modified:** `oaiss-chain-backend/src/main/resources/application.yml`, `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricProperties.java`
**Commit:** 7fd9d8d
**Applied fix:** Changed `admin-password: adminpw` to `admin-password: ${FABRIC_CA_ADMIN_PASSWORD:}` in application.yml. Removed default `"adminpw"` from `FabricProperties.java` so the field is null when not configured.

### CR-03: Add @DistributedLock to TradeService and CarbonCoinService

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/TradeService.java`, `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonCoinService.java`
**Commit:** 8336df5
**Applied fix:** Added `@DistributedLock(key = "'trade:seller:' + #currentUser.userId", expireTime = 30)` to `TradeService.createP2PTrade`. Added `@DistributedLock(key = "'carbon:coin:' + #userId", expireTime = 30)` to `CarbonCoinService.recharge`, `buyQuota`, `sellQuota`, and `transfer` methods.

### WR-01: Remove Swagger paths from JWT filter whitelist

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java`
**Commit:** 2574fbe
**Applied fix:** Removed `/swagger-ui`, `/v1/api-docs`, `/v3/api-docs` from WHITELIST_PATHS. Kept only paths that are truly permitAll() in SecurityConfig (login, register, captcha, refresh, check-ip, actuator/health).

### WR-02: Add role restriction to FileController.downloadFile

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java`
**Commit:** 8168b41
**Applied fix:** Added `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` to the `downloadFile` method, restricting file downloads to enterprise and admin users only.

### WR-03: Add @DistributedLock to placeBuyOrder/placeSellOrder

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`
**Commit:** 9ac3020
**Applied fix:** Added `@DistributedLock(key = "'auction:order:' + #currentUser.userId", expireTime = 10)` to both `placeBuyOrder` and `placeSellOrder` methods to prevent TOCTOU race conditions on enterprise quota reads.

### WR-04: Bind MinIO ports to 127.0.0.1

**Files modified:** `docker-compose.infra.yml`, `docker-compose.yml`
**Commit:** 09ad7fd
**Applied fix:** Changed MinIO port bindings from `"9000:9000"`/`"9001:9001"` to `"127.0.0.1:9000:9000"`/`"127.0.0.1:9001:9001"` in docker-compose.infra.yml, and similarly from `"9002:9000"`/`"9003:9001"` to `"127.0.0.1:9002:9000"`/`"127.0.0.1:9003:9001"` in docker-compose.yml.

---

_Fixed: 2026-05-21T14:30:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
