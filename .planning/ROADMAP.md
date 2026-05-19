# Roadmap: OAISS CHAIN

## Milestones

- **v1.0 Manual Testing** - Phases 1-6 (shipped 2026-05-13) — [Archive](milestones/v1.0-ROADMAP.md)
- **v1.1.0 需求对齐** - Phases 7-12 (shipped 2026-05-18) — [Archive](milestones/v1.1.0-ROADMAP.md)

## Overview

v1.0 验证了 84 个需求的全角色手工测试。v1.1.0 补齐需求文档中定义但代码中缺失的 AI 智能预测、区块链真实对接、碳核算行业公式、准入/资格证签发、前端覆盖率等 12 个 Gap 项，最终达到 E2E 自动化测试覆盖率 95%、验收报告 APPROVE。

## Next Milestone

_To be defined via `/gsd:new-milestone`_

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
**Plans**: 3 plans (08-01 through 08-03) -- Complete

### Phase 9: 区块链真实对接
**Goal**: 区块链记录从 Mock 模式升级为 Hyperledger Fabric 真实链上存储与查询
**Depends on**: Phase 6
**Plans**: 3 plans (09-01 through 09-03) -- Complete

### Phase 10: 准入与资格证
**Goal**: 管理员可签发企业准入证书和审核员资格证，企业和审核员可在前端查看证书状态
**Depends on**: Phase 6
**Plans**: 3 plans (10-01 through 10-03) -- Complete

### Phase 11: 前端覆盖率补齐
**Goal**: 前端 API 模块覆盖全部后端 endpoint，Enterprise/Reviewer 视图功能完整，Swagger 文档与实际 API 对齐
**Depends on**: Phase 7, 8, 9, 10
**Plans**: 4 plans (11-01 through 11-04) -- Complete

### Phase 12: E2E 测试与验收
**Goal**: 全量 E2E 自动化测试通过，覆盖率 90%+，通过率 90%+，v1.1.0 验收达标
**Depends on**: Phase 7, 8, 9, 10, 11
**Plans**: 6 plans (12-01 through 12-06) -- Complete

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
| 8. AI 前端 + 碳核算公式 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 9. 区块链真实对接 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 10. 准入与资格证 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 11. 前端覆盖率补齐 | v1.1.0 | 4/4 | Complete | 2026-05-16 |
| 12. E2E 测试与验收 | v1.1.0 | 6/6 | Complete | 2026-05-17 |

## Known Issues & Deferred Items

| Item | Status | Impact |
|------|--------|--------|
| CON-01/02/03 (concurrency) | Deferred to v2 | Do not test concurrent trading; accept single-threaded behavior |
| SEC-01/02/05/06 (security) | Deferred to v2 | Low risk in dev/test environment |
| PERF-01~04 (performance) | Deferred to v2 | No load testing; acceptable for current scale |
| REQ-06 carbon formula E2E | Coverage gap | Backend endpoints exist, E2E tests incomplete |
| REQ-03 emission prediction E2E | Coverage gap | No dedicated E2E test |
| REQ-12 (Fabric CA) | Optional | Can degrade to mock CA; does not block |

---
*Roadmap created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
