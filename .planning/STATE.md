# STATE: OAISS CHAIN v2.0

**Milestone:** v2.0 安全与性能加固
**Created:** 2026-05-19
**Status:** PLANNED

## Current Phase

**Phase:** 13 - 并发安全与凭据加固
**Phase Status:** Planned (3 plans, all Wave 1)
**Next Action:** Execute `/gsd:execute-phase 13`

## Phase Progress

| Phase | Status | Plans Complete | Last Updated |
|-------|--------|----------------|--------------|
| 13. 并发安全与凭据加固 | Planned | 0/3 | 2026-05-19 |
| 14. 性能优化与代码质量 | Not started | 0/? | — |
| 15. DevOps 与回归验证 | Not started | 0/? | — |

## Phase 13 Plan Details

| Plan | Wave | Autonomous | Requirements | Objective |
|------|------|-----------|--------------|-----------|
| 13-01 | 1 | yes | CON-01, CON-02, CON-03 | Lock-before-transaction, @DistributedLock, @Version optimistic lock |
| 13-02 | 1 | yes | SEC-07, SEC-08, SEC-09 | Externalize docker-compose & YAML credentials, remove insecure defaults |
| 13-03 | 1 | yes | SEC-10, SEC-11, SEC-12, SEC-13 | @PreAuthorize, remove X-User-Id fallback, Prometheus auth |

All 3 plans are Wave 1 (no inter-dependencies) and can be executed in parallel.

## Active Requirements

### Priority A — CRITICAL (6 items)
- [ ] CON-01: DoubleAuctionService synchronized → @DistributedLock
- [ ] CON-02: 金融实体加 @Version 乐观锁
- [ ] CON-03: executeMatching() 事务边界重构
- [ ] SEC-07: docker-compose.yml 凭据外部化
- [ ] SEC-08: 移除 DB_PASSWORD:123456 默认值
- [ ] SEC-09: MinIO 移除 minioadmin 默认凭据

### Priority B — HIGH/MEDIUM (8 items)
- [ ] SEC-10: FileController 加 @PreAuthorize
- [ ] SEC-11: 移除 X-User-Id header fallback
- [ ] SEC-12: SearchController 加 @PreAuthorize
- [ ] SEC-13: Prometheus 端点加认证
- [ ] PERF-02: Redis KEYS → SCAN
- [ ] PERF-03: 缓存预加载异步化
- [ ] PERF-04: 拍卖订单查询分页化
- [ ] PERF-05: 外键索引补充 (Flyway V5)

### Priority C — MEDIUM/LOW (5 items)
- [ ] SEC-01: RSA 私钥加密存储
- [ ] SEC-02: CSRF 保护评估 (ADR)
- [ ] QUAL-01: @Transactional(readOnly=true)
- [ ] QUAL-02: Repository AndDeletedFalse 补全
- [ ] OPS-01: GitHub Actions CI/CD
- [ ] OPS-02: Dev Profile Flyway 修正

## Blockers

None.

## Notes

- All 19 requirements verified as real issues in current codebase (2026-05-19)
- 6 items from original list confirmed as already fixed (SEC-05, SEC-06, PERF-01, REQ-03, REQ-06, M4)
- v2.1 deferred items: M19 i18n, Fabric CA, Phase 11 skips, Phase 9 SUMMARY
- Phase 13 plans created: 13-01 (CON), 13-02 (SEC credentials), 13-03 (SEC authorization) -- all Wave 1, no inter-dependencies