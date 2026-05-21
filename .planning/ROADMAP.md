# Roadmap: OAISS CHAIN

## Milestones

- **v1.0 Manual Testing** - Phases 1-6 (shipped 2026-05-13) — [Archive](milestones/v1.0-ROADMAP.md)
- **v1.1.0 需求对齐** - Phases 7-12 (shipped 2026-05-18) — [Archive](milestones/v1.1.0-ROADMAP.md)
- **v2.0 安全与性能加固** - Phases 13-15 (shipped 2026-05-20)

## Overview

v1.0 验证了 84 个需求的全角色手工测试。v1.1.0 补齐 12 个 Gap 项，E2E 95% 覆盖。v2.0 解决并发、安全、性能问题，通过 CI/CD 与回归验证，达到生产部署就绪标准。三个里程碑全部完成。

## Next Milestone

**TBD** — v2.1 deferred items or new feature work.

## Phase Details

<details>
<summary>v1.0 Manual Testing (Phases 1-6) - SHIPPED 2026-05-13</summary>

### Phase 1: Environment Setup & Auth Baseline
**Goal**: All infrastructure services healthy, all 6 seed accounts login-verified, JWT lifecycle works.
**Depends on**: Nothing
**Requirements**: ENV-01 through ENV-10
**Plans**: 2 plans (01-01, 01-02) -- Complete

### Phase 2: Carbon Report Lifecycle
**Goal**: Central business flow (create, submit, review, cascading side effects) works end-to-end.
**Depends on**: Phase 1
**Requirements**: CARB-01 through CARB-13
**Plans**: 3 plans (02-01, 02-02, 02-03) -- Complete

### Phase 3: Carbon Coin & Trading Engine
**Goal**: Carbon coin accounts, double auction, P2P trade, settlement correctness.
**Depends on**: Phase 1
**Requirements**: COIN-01 through COIN-05, TRADE-01 through TRADE-13
**Plans**: 3 plans (03-01, 03-02, 03-03) -- Complete

### Phase 4: Carbon Neutral Projects & Credit Scoring
**Goal**: Project lifecycle through all states, credit score levels enforced.
**Depends on**: Phase 2
**Requirements**: PROJ-01 through PROJ-05, CRED-01 through CRED-05
**Plans**: 2 plans (04-01, 04-02) -- Complete

### Phase 5: Supporting Domains
**Goal**: Signatures, files, emissions, blockchain, admin, third-party, search verified.
**Depends on**: Phase 1
**Requirements**: SIGN-01~03, FILE-01~03, EMIT-01~03, BLOCK-01~03, ADMIN-01~05, TP-01~02, SRCH-01
**Plans**: 7 plans (05-01 through 05-07) -- Complete

### Phase 6: Cross-Cutting & Edge Cases
**Goal**: AOP concerns verified, edge cases complete, SEC-03/04 fixed, bugs resolved.
**Depends on**: Phases 2-5
**Requirements**: AOP-01~04, EDGE-01~06, BUG-01~03
**Plans**: 3 plans (06-01, 06-02, 06-03) -- Complete

</details>

<details>
<summary>v1.1.0 需求对齐 (Phases 7-12) - SHIPPED 2026-05-18</summary>

### Phase 7: AI 智能预测基础
**Goal**: 用户可通过后端 API 获取市场趋势预测、企业境况推断、碳排放 ML 回归预测
**Depends on**: Phase 6
**Plans**: 4 plans (07-01 through 07-04) -- Complete

### Phase 8: AI 前端 + 碳核算公式
**Goal**: 用户可在前端查看 AI 预测可视化图表；企业上报碳排放时使用行业专用核算公式
**Depends on**: Phase 7
**Plans**: 3 plans (08-01, 08-02, 08-03) -- Complete

### Phase 9: 区块链真实对接
**Goal**: 区块链记录从 Mock 模式升级为 Hyperledger Fabric 真实链上存储与查询
**Depends on**: Phase 6
**Plans**: 3 plans (09-01, 09-02, 09-03) -- Complete

### Phase 10: 准入与资格证
**Goal**: 管理员可签发企业准入证书和审核员资格证，企业和审核员可在前端查看证书状态
**Depends on**: Phase 6
**Plans**: 3 plans (10-01, 10-02, 10-03) -- Complete

### Phase 11: 前端覆盖率补齐
**Goal**: 前端 API 模块覆盖全部后端 endpoint，Enterprise/Reviewer 视图功能完整，Swagger 文档与实际 API 对齐
**Depends on**: Phase 7, 8, 9, 10
**Plans**: 4 plans (11-01 through 11-04) -- Complete

### Phase 12: E2E 测试与验收
**Goal**: 全量 E2E 自动化测试通过，覆盖率 90%+，通过率 90%+，v1.1.0 验收达标
**Depends on**: Phase 7, 8, 9, 10, 11
**Plans**: 6 plans (12-01 through 12-06) -- Complete

</details>

<details open>
<summary>v2.0 安全与性能加固 (Phases 13-15) - ACTIVE</summary>

### Phase 13: 并发安全与凭据加固
**Goal**: 消除金融交易竞态风险，外部化所有硬编码凭据，补全授权注解
**Depends on**: Phase 12 (v1.1.0 shipped)
**Requirements**: CON-01~03, SEC-07~13
**Priority**: CRITICAL — 生产部署安全底线
**Plans**: 3 plans
Plans:
- [ ] 13-01-PLAN.md — Concurrency Safety: lock-before-transaction, @DistributedLock, @Version optimistic lock (CON-01, CON-02, CON-03)
- [ ] 13-02-PLAN.md — Credential Hardening: externalize docker-compose & YAML credentials, remove insecure defaults (SEC-07, SEC-08, SEC-09)
- [ ] 13-03-PLAN.md — Authorization Hardening: @PreAuthorize on FileController/SearchController, remove X-User-Id fallback, Prometheus auth (SEC-10, SEC-11, SEC-12, SEC-13)

### Phase 14: 性能优化与代码质量
**Goal**: 解决性能瓶颈 (Redis SCAN、缓存异步、订单分页、外键索引)，补全事务注解和软删除过滤
**Depends on**: Phase 13
**Requirements**: PERF-02~05, SEC-01~02, QUAL-01~02
**Plans**: 2 plans
**Priority**: HIGH — 生产负载就绪
Plans:
- [ ] 14-01-PLAN.md — Performance Optimization: Redis SCAN, @Async cache preload, V6 FK indexes (PERF-02, PERF-03, PERF-04, PERF-05)
- [ ] 14-02-PLAN.md — Security & Code Quality: RSA key encryption, CSRF ADR, readOnly=true, AndDeletedFalse (SEC-01, SEC-02, QUAL-01, QUAL-02)

### Phase 15: DevOps 与回归验证
**Goal**: 建立 CI/CD 管道，修正 dev profile，全量 E2E 回归测试通过
**Depends on**: Phase 14
**Requirements**: OPS-01~02, 全量 E2E 回归
**Plans**: 3 plans
**Priority**: MEDIUM — 自动化保障
Plans:
- [x] 15-01-PLAN.md — Dev Profile Fix: enable Flyway, set ddl-auto: validate (OPS-02)
- [x] 15-02-PLAN.md — CI/CD Pipeline Fix: fix logic bugs, add MinIO service, remove hardcoded creds (OPS-01)
- [x] 15-03-PLAN.md — E2E Regression: full smoke + flow + v1.1 regression test suite

</details>

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Environment Setup & Auth Baseline | v1.0 | 2/2 | Complete | 2026-05-08 |
| 2. Carbon Report Lifecycle | v1.0 | 3/3 | Complete | 2026-05-09 |
| 3. Carbon Coin & Trading Engine | v1.0 | 3/3 | Complete | 2026-05-09 |
| 4. Carbon Neutral Projects & Credit Scoring | v1.0 | 2/2 | Complete | 2026-05-09 |
| 5. Supporting Domains | v1.0 | 7/7 | Complete | 2026-05-09 |
| 6. Cross-Cutting & Edge Cases | v1.0 | 3/3 | Complete | 2026-05-13 |
| 7. AI 智能预测基础 | v1.1.0 | 4/4 | Complete | 2026-05-14 |
| 8. AI 前端 + 碼核算公式 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 9. 区块链真实对接 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 10. 准入与资格证 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 11. 前端覆盖率补齐 | v1.1.0 | 4/4 | Complete | 2026-05-16 |
| 12. E2E 测试与验收 | v1.1.0 | 6/6 | Complete | 2026-05-17 |
| 13. 并发安全与凭据加固 | v2.0 | 3/3 | Complete | 2026-05-19 |
| 14. 性能优化与代码质量 | v2.0 | 2/2 | Complete | 2026-05-20 |
| 15. DevOps 与回归验证 | v2.0 | 3/3 | Complete | 2026-05-20 |

## Known Issues & Deferred Items

| Item | Status | Impact | v2.0 Coverage |
|------|--------|--------|---------------|
| CON-01/02/03 (concurrency) | Resolved in v2.0 | Financial data corruption risk | Phase 13 |
| SEC-07/08/09 (credentials) | Resolved in v2.0 | Production deployment blocker | Phase 13 |
| SEC-10/11/12/13 (authorization) | Resolved in v2.0 | Data access control gap | Phase 13 |
| PERF-02/03/04 (performance) | Resolved in v2.0 | Production load risk | Phase 14 |
| PERF-05 (indexes) | Resolved in v2.0 | Slow query risk | Phase 14 |
| SEC-01 (RSA plaintext) | Resolved in v2.0 | Key compromise risk | Phase 14 |
| SEC-02 (CSRF) | Resolved in v2.0 | ADR documentation | Phase 14 |
| QUAL-01/02 (code quality) | Resolved in v2.0 | Maintainability | Phase 14 |
| OPS-01/02 (DevOps) | Resolved in v2.0 | No CI/CD | Phase 15 |
| M19 i18n 残留 | Deferred to v2.1 | LOW | — |
| Fabric CA integration | Deferred to v2.1 | Optional | — |
| Phase 11 skipped items | Deferred to v2.1 | LOW | — |

---
*Roadmap created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
*v2.0 shipped: 2026-05-20*