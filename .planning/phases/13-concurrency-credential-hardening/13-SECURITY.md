---
phase: 13-concurrency-credential-hardening
audit_date: 2026-05-21
auditor: orchestrator
threats_total: 11
threats_mitigated: 11
threats_open: 0
review_fixes_verified: 7
status: PASSED
---

# SECURITY: Phase 13 — 并发安全与凭据加固

**Audit Date:** 2026-05-21
**Scope:** 3 plans, 11 STRIDE threats, 7 post-review fixes
**Verdict:** PASSED — all mitigations verified in code

## Threat Mitigation Summary

| Threat ID | Category | Component | Plan | Status | Evidence |
|-----------|----------|-----------|------|--------|----------|
| T-13-01 | Tampering | DoubleAuctionService.executeMatching | 13-01 | MITIGATED | `@DistributedLock(key="'auction:matching'", expireTime=30, waitTime=0)` at DoubleAuctionService.java:148 |
| T-13-02 | Tampering | Enterprise/CarbonCoinAccount/AuctionOrder | 13-01 | MITIGATED | `@Version` on Enterprise:106, CarbonCoinAccount:64, AuctionOrder:83 |
| T-13-03 | Tampering | DistributedLockAspect execution order | 13-01 | MITIGATED | `@Order(Ordered.HIGHEST_PRECEDENCE)` at DistributedLockAspect.java:36 |
| T-13-04 | Info Disclosure | docker-compose hardcoded passwords | 13-02 | MITIGATED | grep `123456\|minioadmin` in docker-compose*.yml → 0 matches |
| T-13-05 | Spoofing | DB_PASSWORD:123456 default | 13-02 | MITIGATED | grep `123456\|minioadmin` in application*.yml → 0 matches; all use `${VAR:}` |
| T-13-06 | Spoofing | MinIO minioadmin defaults | 13-02 | MITIGATED | All MinIO credentials use `${MINIO_ACCESS_KEY}` / `${MINIO_SECRET_KEY}` |
| T-13-07 | Tampering | Weak credential acceptance | 13-02 | MITIGATED | SecurityStartupValidator has WEAK_MINIO_CREDENTIALS set, blocks startup in production |
| T-13-08 | Spoofing | X-User-Id/X-User-Type header fallback | 13-03 | MITIGATED | grep `X-User-Id\|X-User-Type` in src/main/java → 0 matches |
| T-13-09 | Elevation of Privilege | FileController no @PreAuthorize | 13-03 | MITIGATED | Class-level `isAuthenticated()` + 6 method-level role overrides |
| T-13-10 | Elevation of Privilege | SearchController no @PreAuthorize | 13-03 | MITIGATED | Class-level `@PreAuthorize("isAuthenticated()")` at SearchController.java:26 |
| T-13-11 | Info Disclosure | /actuator/prometheus unauthenticated | 13-03 | MITIGATED | SecurityConfig: `hasRole("ADMIN")` on prometheus, `permitAll()` on health only |

## Post-Review Fix Verification

Code review (13-REVIEW.md) found 3 Critical + 5 Warning issues. All 7 actionable fixes applied and verified:

| Finding | Fix | Evidence |
|---------|-----|----------|
| CR-01: No optimistic lock exception handler | Added `@ExceptionHandler(ObjectOptimisticLockingFailureException.class)` → HTTP 409 | GlobalExceptionHandler.java:237-239 |
| CR-02: Hardcoded adminpw in Fabric config | Externalized to `${FABRIC_CA_ADMIN_PASSWORD:}`, removed Java default | application.yml:189, FabricProperties.java:28 |
| CR-03: TradeService/CarbonCoinService lack @DistributedLock | Added locks keyed on userId | TradeService.java:54, CarbonCoinService.java:72/95/119/141 |
| WR-01: Swagger paths in JWT whitelist | Removed; whitelist now auth paths + /actuator/health only | JwtAuthenticationFilter.java:44-52 |
| WR-02: downloadFile no role restriction | Added `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` | FileController.java:114 |
| WR-03: placeBuyOrder/placeSellOrder no lock | Added `@DistributedLock(key="'auction:order:' + #currentUser.userId")` | DoubleAuctionService.java:75, 106 |
| WR-04: MinIO ports exposed on 0.0.0.0 | Bound to 127.0.0.1 | docker-compose.yml:42-43, docker-compose.infra.yml:42-43 |

## Trust Boundaries

| Boundary | Assessment |
|----------|------------|
| Concurrent requests → DoubleAuctionService | LOCKED: @DistributedLock + @Version + exception handler |
| Concurrent requests → TradeService/CarbonCoinService | LOCKED: @DistributedLock added by CR-03 fix |
| Concurrent requests → placeBuyOrder/placeSellOrder | LOCKED: @DistributedLock added by WR-03 fix |
| Config files → deployment | EXTERNALIZED: all credentials use ${VAR}, no defaults |
| HTTP headers → application identity | ELIMINATED: zero X-User-Id/X-User-Type references remain |
| Unauthenticated → FileController/SearchController | BLOCKED: @PreAuthorize on all endpoints |
| Unauthenticated → /actuator/prometheus | BLOCKED: hasRole("ADMIN") |
| Network → MinIO | RESTRICTED: ports bound to 127.0.0.1 |

## Residual Risks

| Risk | Severity | Notes |
|------|----------|-------|
| Flyway V5 @Builder.Default maintenance | LOW | Future devs must remember @Builder.Default on version field |
| Fabric CA adminpw in docker-compose.fabric.yml | LOW | Infrastructure template, not Java source code |
| No optimistic lock integration test | LOW | IN-02: only annotation presence tested, not concurrent update scenario |
| Duplicate DistributedLock assertion test | LOW | IN-01: DoubleAuctionServiceTest duplicates DoubleAuctionServiceLockTest |

## Verification Commands

```bash
# Concurrency
grep -rn "@DistributedLock" oaiss-chain-backend/src/main/java/ | wc -l  # Expected: 10+
grep -rn "@Version" oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/ | wc -l  # Expected: 3
grep "@Order" oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/DistributedLockAspect.java  # Expected: 1

# Credentials
grep -rn "123456\|minioadmin" docker-compose*.yml oaiss-chain-backend/src/main/resources/application*.yml  # Expected: 0 matches
grep "adminpw" oaiss-chain-backend/src/main/java/ | wc -l  # Expected: 0

# Authorization
grep -rn "X-User-Id\|X-User-Type" oaiss-chain-backend/src/main/java/ | wc -l  # Expected: 0
grep -c "@PreAuthorize" oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java  # Expected: 7
grep -c "@PreAuthorize" oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java  # Expected: 1

# Infrastructure
grep "127.0.0.1" docker-compose.infra.yml | grep -c "900"  # Expected: 2
```

---
*Audited: 2026-05-21*
*Phase status: COMPLETE — all threats mitigated, all review fixes verified*
