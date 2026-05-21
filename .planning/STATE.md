# STATE: OAISS CHAIN v2.0

**Milestone:** v2.0 安全与性能加固
**Created:** 2026-05-19
**Status:** IN PROGRESS

## Current Phase

**Phase:** 15 - DevOps 与回归验证
**Phase Status:** Verified (3/3 plans complete, VERIFICATION.md: PASSED)
**Next Action:** Phase 15 complete. All v2.0 phases done.

## Phase Progress

| Phase | Status | Plans Complete | Last Updated |
|-------|--------|----------------|--------------|
| 13. 并发安全与凭据加固 | Complete | 3/3 | 2026-05-19 |
| 14. 性能优化与代码质量 | Complete | 2/2 | 2026-05-20 |
| 15. DevOps 与回归验证 | Complete | 3/3 | 2026-05-20 |

## Phase 13 Plan Details

| Plan | Wave | Autonomous | Requirements | Objective |
|------|------|-----------|--------------|-----------|
| 13-01 | 1 | yes | CON-01, CON-02, CON-03 | Lock-before-transaction, @DistributedLock, @Version optimistic lock |
| 13-02 | 1 | yes | SEC-07, SEC-08, SEC-09 | Externalize docker-compose & YAML credentials, remove insecure defaults |
| 13-03 | 1 | yes | SEC-10, SEC-11, SEC-12, SEC-13 | @PreAuthorize, remove X-User-Id fallback, Prometheus auth |

All 3 plans are Wave 1 (no inter-dependencies) and can be executed in parallel.

## Active Requirements

### Priority A — CRITICAL (6 items)
- [x] CON-01: DoubleAuctionService synchronized -> @DistributedLock
- [x] CON-02: 金融实体加 @Version 乐观锁
- [x] CON-03: executeMatching() 事务边界重构
- [x] SEC-07: docker-compose.yml 凭据外部化
- [x] SEC-08: 移除 DB_PASSWORD:123456 默认值
- [x] SEC-09: MinIO 移除 minioadmin 默认凭据

### Priority B — HIGH/MEDIUM (8 items)
- [x] SEC-10: FileController 加 @PreAuthorize
- [x] SEC-11: 移除 X-User-Id header fallback
- [x] SEC-12: SearchController 加 @PreAuthorize
- [x] SEC-13: Prometheus 端点加认证
- [x] PERF-02: Redis KEYS → SCAN
- [x] PERF-03: 缓存预加载异步化
- [x] PERF-04: 拍卖订单查询分页化 (文档记录: 设计上保持无界)
- [x] PERF-05: 外键索引补充 (Flyway V6)

### Priority C — MEDIUM/LOW (5 items)
- [x] SEC-01: RSA 私钥加密存储
- [x] SEC-02: CSRF 保护评估 (ADR)
- [x] QUAL-01: @Transactional(readOnly=true)
- [x] QUAL-02: Repository AndDeletedFalse 补全
- [x] OPS-01: GitHub Actions CI/CD
- [x] OPS-02: Dev Profile Flyway 修正

## Blockers

None.

## Notes

- All 19 requirements verified as real issues in current codebase (2026-05-19)
- 6 items from original list confirmed as already fixed (SEC-05, SEC-06, PERF-01, REQ-03, REQ-06, M4)
- v2.1 deferred items: M19 i18n, Fabric CA, Phase 11 skips, Phase 9 SUMMARY
- Phase 13 plans created: 13-01 (CON), 13-02 (SEC credentials), 13-03 (SEC authorization) -- all Wave 1, no inter-dependencies