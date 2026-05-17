# Roadmap: OAISS CHAIN

## Milestones

- **v1.0 Manual Testing** - Phases 1-6 (shipped 2026-05-13)
- **v1.1.0 需求对齐** - Phases 7-12 (in progress)

## Overview

v1.0 验证了 84 个需求的全角色手工测试。v1.1.0 补齐需求文档中定义但代码中缺失的 AI 智能预测、区块链真实对接、碳核算行业公式、准入/资格证签发、前端覆盖率等 12 个 Gap 项，最终达到 E2E 自动化测试覆盖率 90%、通过率 90%+ 的验收标准。

## Phases

**Phase Numbering:**
- Integer phases (7-12): Planned v1.1.0 milestone work
- v1.0 phases (1-6): Complete and collapsed below

- [ ] **Phase 7: AI 智能预测基础** - 后端 AI 预测服务实现（市场预测、企业推断、碳排放 ML 模型）
- [ ] **Phase 8: AI 前端 + 碳核算公式** - 前端 AI 模块页面 + 发电/电网行业专用碳核算公式
- [ ] **Phase 9: 区块链真实对接** - Hyperledger Fabric Gateway SDK 集成，替换 Mock 实现
- [ ] **Phase 10: 准入与资格证** - 准入证书签发 + 审核员资格证签发（后端+前端）
- [ ] **Phase 11: 前端覆盖率补齐** - 39 缺失 API 调用模块、Enterprise/Reviewer 视图 CRUD、Swagger 对齐
- [ ] **Phase 12: E2E 测试与验收** - 全量 E2E 自动化测试，达标覆盖率 90%+ 通过率 90%+

## Phase Details

<details>
<summary>v1.0 Manual Testing (Phases 1-6) - SHIPPED 2026-05-13</summary>

### Phase 1: Environment Setup & Auth Baseline
**Goal**: All infrastructure services healthy, all 6 seed accounts login-verified, JWT lifecycle works.
**Depends on**: Nothing
**Requirements**: ENV-01 through ENV-10
**Success Criteria**:
  1. Docker stack healthy, Swagger UI loads, frontend loads
  2. Flyway migrations execute, 21 tables exist
  3. All 6 seed accounts log in and reach correct role home pages
  4. JWT access/refresh/revoke lifecycle works
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

### v1.1.0 需求对齐 (In Progress)

**Milestone Goal:** 所有需求文档中定义但代码中缺失的功能模块实现并验证，E2E 测试覆盖率 90%、通过率 90%+

**Quality Standard:** E2E 测试覆盖率 90%, 通过率 90%+

#### Phase 7: AI 智能预测基础
**Goal**: 用户可通过后端 API 获取市场趋势预测、企业境况推断、碳排放 ML 回归预测
**Depends on**: Phase 6 (v1.0 complete)
**Requirements**: REQ-01, REQ-02, REQ-03
**Success Criteria** (what must be TRUE):
  1. 调用 MarketPredictionService API 可返回碳价走势分析、供需预测、市场趋势数据
  2. 调用 EnterpriseInferenceService API 可返回指定企业的排放趋势推断和合规风险评估
  3. CarbonPredictionService 不再返回 Stub 硬编码值，而是基于历史排放数据执行 ML 回归预测
  4. 三个 AI 服务共享统一的模型加载与推理框架（技术选型：Python FastAPI 微服务 + Prophet/XGBoost/IsolationForest）
**Plans**: 4 plans

Plans:
- [ ] 07-01-PLAN.md — Python ML 微服务骨架 + Spring Boot WebClient 集成 + Docker Compose
- [ ] 07-02-PLAN.md — MarketPredictionService 实现（Prophet 价格预测 + XGBoost 供需预测）
- [ ] 07-03-PLAN.md — EnterpriseInferenceService 实现（IsolationForest 异常检测 + XGBoost 合规分类）
- [ ] 07-04-PLAN.md — CarbonPredictionService Stub 升级为 Prophet 回归模型

#### Phase 8: AI 前端 + 碳核算公式
**Goal**: 用户可在前端查看 AI 预测可视化图表；企业上报碳排放时使用行业专用核算公式
**Depends on**: Phase 7
**Requirements**: REQ-04, REQ-06
**Success Criteria** (what must be TRUE):
  1. 用户可在 MarketPrediction.vue 页面查看碳价走势、供需预测、市场趋势图表
  2. 用户可在 EnterpriseInference.vue 页面查看企业排放趋势推断和合规风险评估图表
  3. 碳排放预测页面展示 ML 模型输出的回归预测结果（替代原 Stub 占位数据）
  4. 发电行业企业上报时可用 25 参数专用碳核算公式计算排放量
  5. 电网行业企业上报时可用 9 参数专用碳核算公式计算排放量
**Plans**: 3 plans
**UI hint**: yes

Plans:
- [ ] 08-01-PLAN.md — MarketPrediction.vue + EnterpriseInference.vue + 碳排放预测可视化升级
- [ ] 08-02-PLAN.md — 发电行业 25 参数碳核算公式实现
- [ ] 08-03-PLAN.md — 电网行业 9 参数碳核算公式实现

#### Phase 9: 区块链真实对接
**Goal**: 区块链记录从 Mock 模式升级为 Hyperledger Fabric 真实链上存储与查询
**Depends on**: Phase 6 (v1.0 complete)
**Requirements**: REQ-05, REQ-12
**Success Criteria** (what must be TRUE):
  1. BlockchainService 调用 Fabric Gateway SDK 将交易记录写入 Hyperledger Fabric 网络
  2. 区块链浏览器查询返回真实链上数据（交易哈希、区块号、时间戳）
  3. 碳报告审批后的上链流程走真实 Fabric 通道（而非 Mock 返回假数据）
  4. Fabric CA 可为用户签发区块链身份证书（REQ-12 optional，可降级为 mock CA）
**Plans**: 3 plans

Plans:
- [ ] 09-01: Fabric 网络搭建（Docker Compose + Channel + Chaincode 部署）
- [ ] 09-02: Fabric Gateway SDK Java 集成 + BlockchainService 替换
- [ ] 09-03: Fabric CA 集成（REQ-12 optional，可降级）

#### Phase 10: 准入与资格证
**Goal**: 管理员可签发企业准入证书和审核员资格证，企业和审核员可在前端查看证书状态
**Depends on**: Phase 6 (v1.0 complete)
**Requirements**: REQ-07, REQ-08
**Success Criteria** (what must be TRUE):
  1. 管理员可通过 Admin API 签发企业准入证书（EnterpriseAdmission），证书状态可在数据库查询
  2. 管理员可通过 Admin API 签发审核员资格证（ReviewerQualification），资格证状态可在数据库查询
  3. 企业用户可在前端查看自身准入证书状态（已签发/未签发/已吊销）
  4. 审核员可在前端查看自身资格证状态（已签发/未签发/已吊销）
**Plans**: 3 plans
**UI hint**: yes

Plans:
- [ ] 10-01-PLAN.md — EnterpriseAdmission 实体 + 仓库 + 服务 + AdminController 端点 + Flyway V4 迁移 (REQ-07)
- [ ] 10-02-PLAN.md — ReviewerQualificationService + AdminController 端点 (REQ-08)
- [ ] 10-03-PLAN.md — 前端证书管理页面 + 企业/审核员证书状态展示 (REQ-07 + REQ-08)

#### Phase 11: 前端覆盖率补齐
**Goal**: 前端 API 模块覆盖全部后端 endpoint，Enterprise/Reviewer 视图功能完整，Swagger 文档与实际 API 对齐
**Depends on**: Phase 7, Phase 8, Phase 9, Phase 10
**Requirements**: REQ-09, REQ-10, REQ-11
**Success Criteria** (what must be TRUE):
  1. 前端 api/ 目录下 39 个缺失的 endpoint 调用全部补齐（与后端 Swagger 一一对应）
  2. Enterprise 视图缺失的 CRUD 操作（报告编辑/删除、交易详情、项目申请等）全部可用
  3. Reviewer 视图缺失的审核操作（批量审核、审核历史、资格证查看等）全部可用
  4. Swagger 文档与实际 endpoint 100% 对齐（无遗漏、无过时描述、无错误参数）
**Plans**: 4 plans
**UI hint**: yes

Plans:
- [ ] 11-01: 前端 API 模块补齐（39 缺失 endpoint 对应的 api/*.ts 调用）
- [ ] 11-02: Enterprise 视图功能补齐（缺失的 CRUD 操作和详情页面）
- [ ] 11-03: Reviewer 视图功能补齐（缺失的审核操作和资格证页面）
- [ ] 11-04: Swagger 文档对齐修正（endpoint 描述、参数、响应与实际代码一致）

#### Phase 12: E2E 测试与验收
**Goal**: 全量 E2E 自动化测试通过，覆盖率 90%+，通过率 90%+，v1.1.0 验收达标
**Depends on**: Phase 7, Phase 8, Phase 9, Phase 10, Phase 11
**Requirements**: (quality gate -- covers all REQ-01 through REQ-12)
**Success Criteria** (what must be TRUE):
  1. E2E 自动化测试覆盖 v1.1.0 全部 12 个需求项的验证场景
  2. E2E 测试覆盖率 >= 90%（覆盖核心业务流程 endpoint）
  3. E2E 测试通过率 >= 90%（允许少量非阻塞 flaky 失败）
  4. v1.0 已验证功能未被 v1.1.0 修改破坏（回归测试通过）
  5. v1.1.0 milestone 验收报告签署通过
**Plans**: 6 plans

Plans:
- [ ] 12-01: E2E 测试框架搭建（Playwright 配置 + 测试数据管理 + CI 集成）
- [ ] 12-02: REQ-01~04 AI 模块 E2E 测试（市场预测 + 企业推断 + 碳排放预测 + 前端页面）
- [ ] 12-03: REQ-05~06 区块链 + 碳核算 E2E 测试（Fabric 上链查询 + 行业公式计算）
- [ ] 12-04: REQ-07~08 准入 + 资格证 E2E 测试（签发 + 查询 + 吊销 + 前端展示）
- [ ] 12-05: REQ-09~11 前端覆盖率 E2E 测试（API 调用 + 视图操作 + Swagger 一致性）
- [ ] 12-06: 回归测试 + 验收报告（v1.0 功能未破坏 + 覆盖率/通过率达标确认）

## Progress

**Execution Order:**
Phases 7-12 execute primarily in numeric order.
Phase 9 and Phase 10 are independent of each other and can run in parallel once Phase 6 is complete.
Phase 8 depends on Phase 7. Phase 11 depends on Phases 7-10. Phase 12 depends on all prior v1.1.0 phases.

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
| SEC-01 (RSA private key in DB) | Deferred to v2 | Low risk in dev/test environment |
| SEC-02 (CSRF protection) | Deferred to v2 | CSRF currently disabled; acceptable for testing |
| REQ-12 (Fabric CA) | Optional in Phase 9 | Can degrade to mock CA; does not block v1.1.0 |
| AI model tech selection | Resolved: Python FastAPI microservice (Prophet + XGBoost + IsolationForest) | No longer blocks Phase 7 |
| Carbon formula parameters | Resolved: 发电 25 参数 + 电网 9 参数 per GB/T 32150-2015 | No longer blocks Phase 8 |
| Fabric SDK version | Resolved: Fabric Gateway SDK 1.7.1 + Fabric 2.5.x LTS | No longer blocks Phase 9 |

---
*Roadmap created: 2026-05-08*
*v1.1.0 phases added: 2026-05-14*
*Phase 7 planned: 2026-05-14 -- AI tech selection resolved (Python FastAPI), 4 plans created*
*Phase 8 planned: 2026-05-14 -- carbon formula parameters resolved (GB/T 32150-2015), 3 plans created*
*Phase 10 planned: 2026-05-15 -- 3 plans created (EnterpriseAdmission + ReviewerQualification + Frontend)*
*Based on: REQUIREMENTS.md (12 v1.1.0 requirements), PROJECT.md, research/SUMMARY.md*
*Granularity: standard (6 v1.1.0 phases, derived from requirement dependencies)*
