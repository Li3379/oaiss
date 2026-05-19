# PROJECT: OAISS CHAIN

## Current State

**Shipped:** v1.1.0 需求对齐 (2026-05-18)

OAISS CHAIN 是一个碳交易与区块链管理平台，支持企业碳排放上报、审核员审核、碳币交易、碳中和项目认证等全链路业务。v1.1.0 补齐了 AI 智能预测、区块链真实对接、碳核算行业公式、准入/资格证签发、前端覆盖率等 12 个 Gap 项，E2E 测试覆盖率 95%，验收报告 APPROVE。

**Tech Stack:** Java 17 + Spring Boot 3.2.5 + MySQL 8 + Redis 7 + MinIO + JWT | Vue 3.5 + TypeScript + Vite + Element Plus + ECharts | Python FastAPI (Prophet/XGBoost/IsolationForest) | Hyperledger Fabric 2.x + Gateway SDK 1.7.1

**Shipped Milestones:**
- v1.0 Manual Testing (Phases 1-6, 2026-05-13) — 84 需求全角色手工测试
- v1.1.0 需求对齐 (Phases 7-12, 2026-05-18) — 12 Gap 项补齐，E2E 95% 覆盖

## Next Milestone Goals

_To be defined via `/gsd:new-milestone`_

Potential areas:
- Performance optimization (PERF-01~04)
- Concurrency testing (CON-01~03)
- Security hardening (SEC-01~06)
- Production deployment readiness

## What This Is

碳交易与区块链管理平台 — 企业碳排放上报、审核员审核、碳币交易、碳中和项目认证、AI 智能预测、区块链存证、行业碳核算公式计算。

5 个角色: 企业 (ENTERPRISE)、审核员 (REVIEWER)、第三方监管 (THIRD_PARTY)、管理员 (ADMIN)、认证机构 (CERTIFIER)

## Core Value

v1.1.0 需求对齐 — 所有需求文档中定义但代码中缺失的功能模块已实现并验证。

<details>
<summary>v1.1.0 Requirements (12 items — all complete)</summary>

### Priority A -- 核心缺失功能
- [x] REQ-01: AI 市场智能预测 (MarketPredictionService)
- [x] REQ-02: AI 企业境况智能推断 (EnterpriseInferenceService)
- [x] REQ-03: AI 碳排放预测升级 (CarbonPredictionService ML)
- [x] REQ-04: 前端 AI 模块页面 (MarketPrediction.vue + EnterpriseInference.vue)

### Priority B -- 重要功能补齐
- [x] REQ-05: Hyperledger Fabric 真实对接 (FabricGatewayConfig + FabricBlockchainService)
- [x] REQ-06: 碳核算行业专用公式 (发电 25 参数 + 电网 9 参数)
- [x] REQ-07: 签发准入证书 (EnterpriseAdmissionService)
- [x] REQ-08: 签发审核员资格证 (ReviewerQualificationService)

### Priority C -- 覆盖率补齐
- [x] REQ-09: 前端 API 覆盖补齐 (39 缺失 endpoint)
- [x] REQ-10: 前端 Enterprise/Reviewer 视图功能补齐
- [x] REQ-11: 文档 API 总览修正 (Swagger 对齐)
- [x] REQ-12: 身份认证区块链方案 (Fabric CA optional)

</details>

<details>
<summary>v1.0 Requirements (84 items — all validated)</summary>

### Environment (ENV-01~10)
- [x] ENV-01: Docker stack healthy
- [x] ENV-02: Swagger UI loads
- [x] ENV-03: Frontend loads
- [x] ENV-04: Flyway migrations execute
- [x] ENV-05: 21 tables exist
- [x] ENV-06: All 6 seed accounts login
- [x] ENV-07: JWT access token works
- [x] ENV-08: JWT refresh token works
- [x] ENV-09: JWT revoke works
- [x] ENV-10: Role-based home pages

### Carbon Report (CARB-01~13)
- [x] CARB-01~13: Full carbon report lifecycle (create, submit, review, approve/reject, side effects)

### Carbon Coin & Trading (COIN-01~05, TRADE-01~13)
- [x] COIN-01~05: Carbon coin accounts, recharge, transfer
- [x] TRADE-01~13: P2P trade, double auction, settlement

### Projects & Credit (PROJ-01~05, CRED-01~05)
- [x] PROJ-01~05: Carbon neutral project lifecycle
- [x] CRED-01~05: Credit score levels enforced

### Supporting Domains (SIGN, FILE, EMIT, BLOCK, ADMIN, TP, SRCH)
- [x] SIGN-01~03: Digital signatures
- [x] FILE-01~03: File upload/download
- [x] EMIT-01~03: Emission records
- [x] BLOCK-01~03: Blockchain records
- [x] ADMIN-01~05: Admin operations
- [x] TP-01~02: Third-party monitoring
- [x] SRCH-01: Search functionality

### Cross-Cutting (AOP, EDGE, BUG)
- [x] AOP-01~04: Audit log, rate limit, permission, data isolation
- [x] EDGE-01~06: Edge cases
- [x] BUG-01~03: Bug fixes

</details>

## Key Decisions

| ID | Decision | Rationale | Status |
|----|----------|-----------|--------|
| D-01 | EnterpriseAdmission 新实体不复用 EntryPermission | 语义不同：准入证书 vs 准入权限 | Resolved |
| D-02 | 证书状态: ACTIVE(1) + REVOKED(2)，无记录=未签发 | 简化状态机 | Resolved |
| D-04 | 证书编号自动生成: EA-{date}-{random} / RQ-{date}-{random} | 避免冲突 | Resolved |
| D-07 | 重复签发防护：已有 ACTIVE 证书时拒绝 | 防止重复 | Resolved |
| D-08 | 吊销 = status 1→2，仅 ACTIVE 可吊销 | 状态流转约束 | Resolved |
| AI-01 | AI 模型技术选型: Python FastAPI | Prophet/XGBoost/IsolationForest 生态成熟 | Resolved |
| BC-01 | Fabric Gateway SDK 1.7.1 | 非 legacy fabric-gateway-java 2.2.x | Resolved |
| CF-01 | 碳核算公式参数: GB/T 32150-2015 | 国家标准 | Resolved |

## Deferred Items

| Category | Items | Deferred To |
|----------|-------|-------------|
| Concurrency | CON-01/02/03 | v2 |
| Security | SEC-01/02/05/06 | v2 |
| Performance | PERF-01~04 | v2 |
| E2E Coverage | REQ-03/REQ-06 gaps | v2 |
| Fabric CA | REQ-12 (optional) | v2 |

---
*Project created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
