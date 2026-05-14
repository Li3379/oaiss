# OAISS CHAIN

## What This Is

OAISS CHAIN 是一个碳交易与区块链平台，面向五种用户角色（企业、审核员、认证方、第三方监管、管理员）提供碳排放报告提交、碳积分管理、双重拍卖交易、P2P 交易、碳中和项目管理等完整业务流程。v1.0 已完成全角色手工测试，v1.1.0 目标是补齐需求文档中缺失的 AI 智能预测、区块链真实对接、碳核算行业公式等核心功能，达到 E2E 测试覆盖率 90%、通过率 90%+。

## Core Value

v1.1.0 需求对齐：所有需求文档中定义但代码中缺失的功能模块必须实现并验证，E2E 测试覆盖率 90%、通过率 90%+。

## Current Milestone: v1.1.0 — 需求对齐

**质量标准:** E2E 测试覆盖率 90%, 通过率 90%+

### 12 Gap Items (by priority)

**A — 核心缺失功能 (4 items)**
- A-1: AI 市场智能预测 — MarketPredictionService + 前端页面
- A-2: AI 企业境况智能推断 — EnterpriseInferenceService + 前端页面
- A-3: AI 碳排放预测升级 — CarbonPredictionService 从 Stub 升级为 ML 模型
- A-4: 前端 AI 模块页面 — MarketPrediction.vue + EnterpriseInference.vue

**B — 重要功能补齐 (4 items)**
- B-5: Hyperledger Fabric 真实对接 — BlockchainService 从 Mock 升级为 Fabric SDK
- B-6: 碳核算行业专用公式 — 发电行业 25 参数 + 电网行业 9 参数公式
- B-7: 签发准入证书 — EntryPermissionService + AdminController + 前端
- B-8: 签发审核员资格证 — ReviewerQualificationService + AdminController + 前端

**C — 覆盖率补齐 (4 items)**
- C-9: 前端 API 覆盖补齐 — 39 个缺失 endpoint 的前端调用
- C-10: 前端 Enterprise/Reviewer 视图功能补齐 — 缺失的 CRUD 操作
- C-11: 文档 API 总览修正 — Swagger 与实际 endpoint 对齐
- C-12: 身份认证区块链方案 — Fabric CA 集成 (optional)

## Requirements

### Validated (v1.0)

- ✓ 用户认证系统（JWT 登录/注册/刷新令牌）— 已实现
- ✓ 五角色 RBAC 权限控制（@PreAuthorize + Vue Router meta.roles）— 已实现
- ✓ 碳排放报告提交与审核流程 — 已实现
- ✓ 双重拍卖交易引擎 — 已实现
- ✓ P2P 交易流程 — 已实现
- ✓ 碳积分账户管理 — 已实现
- ✓ 碳中和项目管理 — 已实现
- ✓ 数字签名（RSA）— 已实现
- ✓ 文件上传（MinIO）— 已实现
- ✓ 区块链记录 — 已实现（mock 模式）
- ✓ 前端 i18n 国际化 — 已完成
- ✓ 前端 TypeScript 迁移 — 已完成
- ✓ 全角色手工测试完成 — 60/60 tests passed
- ✓ 安全审计修复 — 48/63 完成 (SEC-03/04 已修)

### Active (v1.1.0)

- [ ] **REQ-01**: AI 市场智能预测 — MarketPredictionService 实现市场趋势预测、碳价走势分析、供需预测
- [ ] **REQ-02**: AI 企业境况智能推断 — EnterpriseInferenceService 实现企业碳排放趋势推断、合规风险评估
- [ ] **REQ-03**: AI 碳排放预测升级 — CarbonPredictionService 从 Stub 升级为真实 ML 模型（回归预测）
- [ ] **REQ-04**: 前端 AI 模块页面 — MarketPrediction.vue + EnterpriseInference.vue + 碳排放预测可视化
- [ ] **REQ-05**: Hyperledger Fabric 真实对接 — BlockchainService 从 Mock 升级为 Fabric Gateway SDK
- [ ] **REQ-06**: 碳核算行业专用公式 — 发电行业 25 参数公式 + 电网行业 9 参数公式实现
- [ ] **REQ-07**: 签发准入证书 — EntryPermissionService + AdminController endpoint + 前端管理页面
- [ ] **REQ-08**: 签发审核员资格证 — ReviewerQualificationService + AdminController endpoint + 前端管理页面
- [ ] **REQ-09**: 前端 API 覆盖补齐 — 39 个缺失后端 endpoint 的前端 API 调用模块
- [ ] **REQ-10**: 前端 Enterprise/Reviewer 视图功能补齐 — 缺失的 CRUD 操作和详情页面
- [ ] **REQ-11**: 文档 API 总览修正 — Swagger 文档与实际 endpoint 对齐
- [ ] **REQ-12**: 身份认证区块链方案 — Fabric CA 集成 (optional, 可延期)

### Out of Scope

- 性能优化（PERF-01~07）— 功能稳定后再优化
- 并发问题彻底修复（CON-01~05）— 需要架构级重构，记录但不阻塞
- Redis 高可用 / 分布式部署 — 单节点开发环境足够

## Context

**技术栈:**
- 后端: Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8, Redis 7, MinIO, JWT (jjwt 0.12.5), Flyway
- 前端: Vue 3.5, TypeScript, Vite 8, Element Plus 2.13, Pinia 3, Vue Router 5, ECharts 6, vue-i18n 11
- AI/ML: Python (scikit-learn/PyTorch) 或 Java (DL4J/ONNX Runtime) — 待研究确定
- 区块链: Hyperledger Fabric 2.x + Fabric Gateway SDK — 待研究确定
- 基础设施: Docker Compose (MySQL, Redis, MinIO, 后端, 前端)

**项目状态 (v1.0 完成):**
- 全角色手工测试 60/60 passed
- 安全审计 48/63 修复完成
- i18n + TypeScript 迁移完成
- Flyway: V1__init_schema.sql, V2__seed_data.sql

**已知代码缺失 (v1.1.0 目标):**
- MarketPredictionService / EnterpriseInferenceService — 完全缺失
- CarbonPredictionService — Stub 实现
- BlockchainService — Mock 实现
- EntryPermissionService / ReviewerQualificationService — 仅 Entity+Repository
- 碳核算公式 — 通用 scope 计算，无行业专用公式
- 前端 — 39 个 API endpoint 未覆盖，Enterprise/Reviewer 视图功能不完整

## Constraints

- **兼容性**: v1.1.0 修改不能破坏 v1.0 已验证功能，所有修改必须向后兼容
- **质量标准**: E2E 测试覆盖率 90%, 通过率 90%+
- **技术栈锁定**: Java 17 + Spring Boot 3.2.5 + Vue 3.5 + TypeScript，AI/区块链模块除外
- **数据库约束**: 使用 Flyway 管理迁移 (V3__v1_1_schema.sql)
- **AI 模型**: 优先使用 Java 原生方案 (DL4J/ONNX Runtime)，避免引入 Python 微服务
- **区块链**: 使用 Fabric Gateway SDK (Java), 不使用旧版 Fabric SDK

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 全手工测试而非自动化 E2E (v1.0) | 用户明确要求手工测试 | — Completed |
| 修复 SEC-03/04 作为测试前置条件 | Swagger 生产暴露和 CORS 默认值是低风险高回报修复 | — Completed |
| 不修复并发问题（CON-01~05）| 需要架构级重构，当前单节点开发环境可接受 | — Deferred to v2 |
| 保留 mock 区块链模式 (v1.0) | 真实区块链集成是 v1.1.0 目标 | — Now REQ-05 |
| AI 模型技术选型待研究 | 需要评估 DL4J vs ONNX Runtime vs Python 微服务 | — Pending research |
| Fabric SDK 版本待研究 | Fabric Gateway SDK vs 旧版 SDK | — Pending research |
| 碳核算公式参数来源 | 发电 25 参数 + 电网 9 参数的具体公式需研究 | — Pending research |
| REQ-12 (Fabric CA) 可延期 | 身份认证区块链方案复杂度高，可降级为 optional | — Pending |

---
*Last updated: 2026-05-14 for v1.1.0 milestone*