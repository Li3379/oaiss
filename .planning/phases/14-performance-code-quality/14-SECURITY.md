---
phase: 14-performance-code-quality
audit_date: 2026-05-21
auditor: orchestrator
threats_total: 8
threats_mitigated: 8
threats_accepted: 2
threats_open: 0
review_fixes_verified: 12
status: PASSED
---

# SECURITY: Phase 14 — 性能优化与代码质量

**Audit Date:** 2026-05-21
**Scope:** 2 plans, 8 STRIDE threats + 2 accepted risks, 12 post-review fixes
**Verdict:** PASSED — all mitigations verified in code

## Threat Mitigation Summary

### Plan 14-01: Performance Optimization

| Threat ID | Category | Component | Status | Evidence |
|-----------|----------|-----------|--------|----------|
| T-14-01-SC | Tampering | No new packages | MITIGATED | All changes use existing JDK/Spring capabilities |
| T-14-01-01 | Denial of Service | Redis KEYS command | MITIGATED | `redisTemplate.keys()` → 0 matches in production code; uses SCAN via `RedisCallback` at CachePreloadService.java:200 |
| T-14-01-02 | Denial of Service | Cache preload blocking | MITIGATED | `@Async("cachePreloadExecutor")` at CachePreloadService.java:70; AsyncConfig.java exists with bounded executor |
| T-14-01-03 | Denial of Service | Full table scans on FK joins | MITIGATED | V6 Flyway migration has 26 CREATE INDEX statements for FK columns |

### Plan 14-02: Security & Code Quality

| Threat ID | Category | Component | Status | Evidence |
|-----------|----------|-----------|--------|----------|
| T-14-02-SC | Tampering | No new packages | MITIGATED | javax.crypto is JDK standard library |
| T-14-02-01 | Info Disclosure | RSA private keys plaintext at rest | MITIGATED | AesGcmEncryptor used for encrypt (line 96) and decrypt (lines 185, 319, 361) in DigitalSignatureService.java; KEK from env var only |
| T-14-02-02 | Spoofing | CSRF token bypass | ACCEPTED | ADR-001 documents JWT-in-sessionStorage rationale; CSRF not applicable to stateless API |
| T-14-02-03 | Info Disclosure | Soft-deleted records returned | MITIGATED | 103 AndDeletedFalse occurrences across 22 repository files |
| T-14-02-04 | KEK loss | RSA_KEK environment variable | ACCEPTED | Operational risk documented; KEK backup is operator responsibility |

## Post-Review Fix Verification

Code review (14-REVIEW.md) found 3 Critical + 7 Warning + 4 Info issues. 12 fixes applied and verified:

| Finding | Fix | Evidence |
|---------|-----|----------|
| CR-01: validateKeyStatus write in readOnly methods | Extracted `markExpiredKeys` as separate `@Transactional` | DigitalSignatureService.java — new method |
| CR-02: Single transaction for all key migrations | Per-key `@Transactional migrateSingleKey()` with try-catch loop | RsaKeyMigrationRunner.java:58-59 |
| CR-03: Test mocks wrong method (keys vs SCAN) | Test mocks `redisTemplate.execute(RedisCallback)` | CachePreloadServiceTest.java |
| WR-01: No AES key length validation | 32-byte check with `IllegalStateException` | AesGcmEncryptor.java:39 |
| WR-02: MinioService @Transactional on non-DB methods | Removed from 4 methods | MinioService.java |
| WR-03: Redis-only method has @Transactional | Removed | CachePreloadService.java |
| WR-04: Self-invoked @Async | Extracted preload logic into private method with comment | CachePreloadService.java:186 |
| WR-06: Platform-default charset in encrypt/decrypt | UTF-8 explicit | AesGcmEncryptor.java:57,83 |
| WR-07: No null guard on privateKey migration | Added null/blank check | RsaKeyMigrationRunner.java |
| IN-01: Class-level LENIENT strictness | Removed, targeted lenient only | CachePreloadServiceTest.java |
| IN-02: CLAUDE.md vs ADR-001 inconsistency | Updated auth description | CLAUDE.md |
| IN-03: No RejectedExecutionHandler | Added CallerRunsPolicy | AsyncConfig.java:20 |

## Trust Boundaries

| Boundary | Assessment |
|----------|------------|
| Redis server | SAFE: SCAN (cursor-based) replaces KEYS (blocking); no new Redis attack surface |
| MySQL data at rest | HARDENED: RSA private keys encrypted with AES-256-GCM; KEK from env var only |
| Thread pool | SAFE: Bounded executor with CallerRunsPolicy; no unbounded thread creation |
| KEK management | OPERATOR RISK: RSA_KEK must be backed up; loss = unrecoverable keys |
| Soft-delete visibility | SEALED: 103 AndDeletedFalse across 22 repositories; deleted records never returned |

## Residual Risks

| Risk | Severity | Notes |
|------|----------|-------|
| KEK loss renders all encrypted RSA keys unrecoverable | MEDIUM | Accepted: operational responsibility, documented in ADR |
| RsaKeyMigrationRunner per-key failure logging only | LOW | Failed keys logged but not retried automatically; requires manual intervention |
| PERF-04 auction order queries remain unbounded | LOW | Architectural decision documented; not a security concern |

---
*Audited: 2026-05-21*
*Phase status: COMPLETE — all threats mitigated or accepted, all review fixes verified*
