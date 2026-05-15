# STATE -- v1.1.0 需求对齐

## Milestone

- **Version**: v1.1.0
- **Name**: 需求对齐
- **Quality Standard**: E2E 测试覆盖率 90%, 通过率 90%+
- **Started**: 2026-05-14

## Progress

- **Current Phase**: 9 (区块链真实对接)
- **Phases Completed**: 8 / 12
- **Plans Completed**: 7

## Phase Status

| Phase | Name | Status | Plans |
|-------|------|--------|-------|
| 7 | AI 智能预测基础 | Complete | 4/4 |
| 8 | AI 前端 + 碳核算公式 | Complete | 3/3 |
| 9 | 区块链真实对接 | Complete | 3/3 |
| 10 | 准入与资格证 | Ready to execute | 3/3 |
| 11 | 前端覆盖率补齐 | Not started | 0/4 |
| 12 | E2E 测试与验收 | Not started | 0/6 |

Progress: [========....] 67% (8 of 12 phases done; Phase 10 planned with 3/3 plans)

## Active Context

### Key Decisions

- AI 模型技术选型: Resolved — Python FastAPI 微服务 (Prophet + XGBoost + IsolationForest)
- Fabric SDK 版本: Resolved — Fabric Gateway SDK 1.7.1 + Fabric 2.5.x LTS
- 碳核算公式参数: Resolved — GB/T 32150-2015 发电25参数 + 电网9参数
- REQ-12 (Fabric CA): optional，可降级为 mock CA

### Pending Todos

None yet.

### Blockers/Concerns

- Fabric network Docker setup on Windows — Plan 09-01 Task 7 must verify container health
- Phase 9 and Phase 10 are independent; can run in parallel

## Phase 7 Completion Summary

**Phase 7: AI 智能预测基础** — Complete (2026-05-14)

All 4 plans executed successfully:
- **07-01**: Python ML service scaffold + Spring Boot WebClient + Docker Compose
- **07-02**: MarketPredictionService (Prophet 价格预测 + XGBoost 供需预测)
- **07-03**: EnterpriseInferenceService (IsolationForest 异常检测 + XGBoost 合规分类)
- **07-04**: CarbonPredictionService Stub → ML Prophet 回归预测

Key deliverables:
- `oaiss-chain-ml-service/` — Python FastAPI ML 微服务 (port 8001)
- `MlServiceClient` + `MlServiceConfig` — Spring Boot WebClient 集成
- 3 AI controllers: MarketPredictionController, EnterpriseInferenceController, EmissionController
- Docker Compose ml-service container
- Backend compiles, unit tests pass

## Phase 8 Completion Summary

**Phase 8: AI 前端 + 碳核算公式** — Complete (2026-05-15)

All 3 plans executed successfully:
- **08-01**: Frontend AI Pages (MarketPrediction.vue + EnterpriseInference.vue + API clients + router/menu/i18n)
- **08-02**: PowerGenerationFormulaService (25-parameter GB/T 32150-2015 formula + frontend calculator)
- **08-03**: PowerGridFormulaService (9-parameter GB/T 32150-2015 formula + frontend calculator tab)

Key deliverables:
- 2 AI visualization pages: MarketPrediction (ECharts line+confidence band), EnterpriseInference (compliance+anomaly+risk)
- 2 carbon formula services: PowerGenerationFormulaService, PowerGridFormulaService
- 2 REST endpoints: POST /carbon/calculate/power-generation, POST /carbon/calculate/power-grid
- 1 frontend calculator: CarbonFormulaCalculator.vue (tabbed: power generation + power grid)
- 12 unit tests (6 per formula service), all pass
- Backend compiles, frontend builds

## Phase 9 Planning Summary

**Phase 9: 区块链真实对接** — Planning (2026-05-15)

3 plans created:
- **09-01**: Fabric 网络搭建 + Gateway SDK 集成 (8 tasks — Docker Compose Fabric 网络, BlockchainServicePort 接口提取, FabricGatewayConfig, FabricBlockchainService, crypto 材料, 单元测试)
- **09-02**: Chaincode 开发 + BlockchainService 真实替换 (7 tasks — Go 链码 carbon-chaincode, 部署脚本, submitTransaction/evaluateTransaction 实现, 集成测试)
- **09-03**: Fabric CA 集成 — REQ-12 optional (5 tasks — FabricCaService, JWT-to-Fabric 身份映射, 降级方案确认)

Key decisions:
- Fabric Gateway SDK 1.7.1 (not legacy fabric-gateway-java 2.2.x)
- Go 链码 (not Java/Node) — Fabric 生态主流选择
- 共享组织身份作为 MVP 默认（REQ-12 降级为 mock CA）
- Profile 切换: `fabric.enabled=false` (Mock) / `fabric.enabled=true` (Fabric)

## Phase 10 Planning Summary

**Phase 10: 准入与资格证** — Ready to execute (2026-05-15)

3 plans created:
- **10-01**: EnterpriseAdmission 准入证书 (2 tasks — Flyway V4 migration + entity + repository, service + AdminController endpoints + unit tests)
- **10-02**: ReviewerQualification 审核员资格证 (2 tasks — paginated repository methods + service + AdminController endpoints, unit tests)
- **10-03**: 前端证书管理页面 (3 tasks — API client + i18n + router/menu, CertificateManage.vue admin page, enterprise/auditor status views)

Key decisions:
- EnterpriseAdmission 是新实体，不复用 EntryPermission（D-01）
- 证书状态: ACTIVE(1) + REVOKED(2)，无记录 = 未签发（D-02）
- 证书编号自动生成: EA-{date}-{random} / RQ-{date}-{random}（D-04）
- 重复签发防护：已有 ACTIVE 证书时拒绝（D-07）
- 吊销 = status 1→2，仅 ACTIVE 可吊销（D-08）

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Concurrency | CON-01/02/03 | Deferred to v2 | v1.0 |
| Security | SEC-01/02/05/06 | Deferred to v2 | v1.0 |
| Performance | PERF-01~04 | Deferred to v2 | v1.0 |

## Session Continuity

Last session: 2026-05-15
Stopped at: Phase 10 planned (3/3 plans), ready for execution
Resume file: None