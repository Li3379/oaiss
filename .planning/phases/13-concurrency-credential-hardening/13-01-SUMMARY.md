---
phase: 13-concurrency-credential-hardening
plan: 01
subsystem: concurrency
tags: [distributed-lock, optimistic-lock, aspect-order, flyway]
dependency_graph:
  requires: []
  provides: [CON-01, CON-02, CON-03]
  affects: [DoubleAuctionService, DistributedLockAspect, Enterprise, CarbonCoinAccount, AuctionOrder]
tech_stack:
  added: [jakarta.persistence.Version, org.springframework.core.annotation.Order]
  patterns: [lock-before-transaction, optimistic-locking]
key_files:
  created:
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/aop/DistributedLockAspectOrderTest.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceLockTest.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/entity/OptimisticLockTest.java
    - oaiss-chain-backend/src/main/resources/db/migration/V5__add_optimistic_lock_version.sql
  modified:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/DistributedLockAspect.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/Enterprise.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CarbonCoinAccount.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/AuctionOrder.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DoubleAuctionServiceTest.java
decisions:
  - "@Builder.Default cannot be tested via reflection (CLASS retention); verified via builder().build() default value instead"
  - "V5 Flyway migration chosen (V3 and V4 already exist)"
metrics:
  duration: 16min
  completed: 2026-05-19
  tasks: 3
  files: 10
---

# Phase 13 Plan 01: Concurrency Safety Summary

Lock-before-transaction guarantee via @Order(HIGHEST_PRECEDENCE), distributed lock replacing synchronized on auction matching, and @Version optimistic lock on 3 financial entities.

## Changes Made

### Task 1: Fix DistributedLockAspect execution order (CON-03)
- Added `@Order(Ordered.HIGHEST_PRECEDENCE)` to `DistributedLockAspect`
- This ensures the distributed lock aspect wraps the transaction proxy, so the lock is acquired before the transaction starts and released after it commits
- Created `DistributedLockAspectOrderTest` with 2 tests verifying @Order annotation presence and value

### Task 2: Replace synchronized with @DistributedLock on executeMatching (CON-01)
- Removed `synchronized` keyword from `DoubleAuctionService.executeMatching()`
- Added `@DistributedLock(key = "'auction:matching'", expireTime = 30, waitTime = 0)` before `@Transactional`
- Distributed lock works across multiple instances unlike synchronized (JVM-local only)
- waitTime=0 means non-blocking: if lock is held, call fails immediately (correct for auction matching)
- Created `DoubleAuctionServiceLockTest` with 5 tests verifying annotation attributes and synchronized removal
- Updated `DoubleAuctionServiceTest`: replaced synchronized assertion with @DistributedLock assertion

### Task 3: Add @Version optimistic lock to financial entities (CON-02)
- Added `@Version @Builder.Default private Long version = 0L` to Enterprise, CarbonCoinAccount, AuctionOrder
- @Builder.Default is required because these entities use @Builder; without it, builder would set version=null
- Created Flyway V5 migration adding `version BIGINT NOT NULL DEFAULT 0` to all 3 tables
- Created `OptimisticLockTest` with 6 tests: 3 verifying @Version annotation, 3 verifying builder default value

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] @Builder.Default reflection tests failed**
- **Found during:** Task 3 test execution
- **Issue:** Lombok @Builder.Default has CLASS retention (not RUNTIME), so `field.getAnnotation(Builder.Default.class)` always returns null
- **Fix:** Replaced reflection-based @Builder.Default tests with builder default-value tests: `Entity.builder().build().getVersion() == 0L`
- **Files modified:** OptimisticLockTest.java
- **Commit:** dd2560c

None otherwise - plan executed as written.

## Test Results

| Test Class | Tests | Status |
|------------|-------|--------|
| DistributedLockAspectOrderTest | 2 | PASS |
| DistributedLockAspectTest | 15 | PASS |
| DoubleAuctionServiceLockTest | 5 | PASS |
| DoubleAuctionServiceTest | 11 | PASS |
| OptimisticLockTest | 6 | PASS |
| **Total** | **39** | **PASS** |

## Verification Checks

| Check | Expected | Actual |
|-------|----------|--------|
| @Order in DistributedLockAspect | 1 | 1 |
| synchronized in DoubleAuctionService | 0 | 0 |
| @DistributedLock in DoubleAuctionService | >=1 | 1 |
| @Version in Enterprise | 1 | 1 |
| @Version in CarbonCoinAccount | 1 | 1 |
| @Version in AuctionOrder | 1 | 1 |
| V5 migration file exists | YES | YES |
| mvn compile | SUCCESS | SUCCESS |

## Commits

| Commit | Message |
|--------|---------|
| 94b4d9f | test(13-01): add failing test for DistributedLockAspect @Order annotation / feat(13-01): add @Order(Ordered.HIGHEST_PRECEDENCE) to DistributedLockAspect |
| 5871f3f | test(13-01): add failing tests for @DistributedLock on executeMatching / feat(13-01): replace synchronized with @DistributedLock on executeMatching (CON-01) |
| e7dfe6f | test(13-01): add failing tests for @Version on financial entities / feat(13-01): add @Version optimistic lock to financial entities (CON-02) |
| dd2560c | fix(13-01): replace @Builder.Default reflection tests with builder default-value tests |

## Self-Check: PASSED

All created files verified present. All commits verified in git log.
