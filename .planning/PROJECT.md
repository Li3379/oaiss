# PROJECT: OAISS CHAIN

## Current State

**Shipped:** v2.1 测试基础设施修复与收尾 (2026-05-22)
**Status:** Production ready

OAISS CHAIN 是一个碳交易与区块链管理平台，支持企业碳排放上报、审核员审核、碳币交易、碳中和项目认证等全链路业务。v1.0-v2.1 已完成，平台达到生产部署安全标准。

**Tech Stack:** Java 17 + Spring Boot 3.2.5 + MySQL 8 + Redis 7 + MinIO + JWT | Vue 3.5 + TypeScript + Vite + Element Plus + ECharts | Python FastAPI (Prophet/XGBoost/IsolationForest) | Hyperledger Fabric 2.x + Gateway SDK 1.7.1

**Shipped Milestones:**
- v1.0 Manual Testing (Phases 1-6, 2026-05-13) — 84 需求全角色手工测试
- v1.1.0 需求对齐 (Phases 7-12, 2026-05-18) — 12 Gap 项补齐，E2E 95% 覆盖
- v2.0 安全与性能加固 (Phases 13-15, 2026-05-21) — 19 安全/性能项，CI/CD 自动化
- v2.1 测试基础设施修复与收尾 (Phases 16-18, 2026-05-22) — E2E auth 修复，i18n 清理，Fabric CA 可选集成

## Next Milestone Goals

**v3.0 生产部署与运维** — 待定，需根据用户反馈和业务需求定义。

## What This Is

碳交易与区块链管理平台 — 企业碳排放上报、审核员审核、碳币交易、碳中和项目认证、AI 智能预测、区块链存证、行业碳核算公式计算。

5 个角色: 企业 (ENTERPRISE)、审核员 (REVIEWER)、第三方监管 (THIRD_PARTY)、管理员 (ADMIN)、认证机构 (CERTIFIER)

## Core Value

生产就绪的碳交易与区块链管理平台 — 安全、高性能、CI/CD 自动化保障。

<details>
<summary>v2.1 Requirements (7 items — all complete)</summary>

### Category A — E2E 测试基础设施 (HIGH)
- [x] E2E-01: Auth fixture timeout 修复
- [x] E2E-02: d9/d10 孤悬测试接入
- [x] E2E-03: isFabricAvailable() 测试钩子接入

### Category B — 验收缺口补齐 (HIGH)
- [x] GAP-01: REQ-06 碳核算公式 E2E 测试
- [x] GAP-02: REQ-03 /emission/predict E2E 测试

### Category C — 代码清理 (MEDIUM)
- [x] I18N-01: M19 硬编码中文提取

### Category D — 可选功能 (LOW)
- [x] FABRIC-01: Fabric CA 应用层集成

</details>

<details>
<summary>v2.0 Requirements (19 items — all complete)</summary>

### Priority A — 并发安全 (CRITICAL)
- [x] CON-01: DoubleAuctionService synchronized → @DistributedLock
- [x] CON-02: 金融实体加 @Version 乐观锁 (Enterprise, CarbonCoinAccount, AuctionOrder)
- [x] CON-03: executeMatching() 事务边界重构 (锁在事务内)

### Priority A — 凭据安全 (CRITICAL)
- [x] SEC-07: docker-compose.yml 硬编码密码外部化
- [x] SEC-08: application.yml 移除 DB_PASSWORD:123456 默认值
- [x] SEC-09: MinIO 移除 minioadmin 默认凭据

### Priority B — 授权安全 (HIGH)
- [x] SEC-10: FileController 加 @PreAuthorize 角色注解
- [x] SEC-11: FileController 移除 X-User-Id/X-User-Type header fallback
- [x] SEC-12: SearchController 加 @PreAuthorize 角色注解
- [x] SEC-13: Prometheus /actuator/prometheus 端点加认证

### Priority B — 性能优化 (MEDIUM)
- [x] PERF-02: Redis KEYS 命令替换为 SCAN
- [x] PERF-03: 缓存预加载异步化 (@Async)
- [x] PERF-04: 拍卖订单查询分页化 (List→Page)
- [x] PERF-05: 外键列添加数据库索引 (Flyway V5)

### Priority C — 代码质量 (MEDIUM)
- [x] SEC-01: RSA 私钥加密存储 (KEK envelope)
- [x] SEC-02: CSRF 保护评估 (当前 JWT 架构下合理，记录决策)
- [x] QUAL-01: 读操作加 @Transactional(readOnly=true)
- [x] QUAL-02: Repository findByUserId 补全 AndDeletedFalse

### Priority C — DevOps 与收尾 (LOW)
- [x] OPS-01: GitHub Actions CI/CD 管道
- [x] OPS-02: dev profile 改 Flyway + validate (禁用 ddl-auto:update)

</details>

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
- [x] ENV-01~10: Docker, Swagger, Frontend, Flyway, Login, JWT lifecycle

### Carbon Report (CARB-01~13)
- [x] CARB-01~13: Full carbon report lifecycle

### Carbon Coin & Trading (COIN-01~05, TRADE-01~13)
- [x] COIN-01~05: Carbon coin accounts, recharge, transfer
- [x] TRADE-01~13: P2P trade, double auction, settlement

### Projects & Credit (PROJ-01~05, CRED-01~05)
- [x] PROJ-01~05: Carbon neutral project lifecycle
- [x] CRED-01~05: Credit score levels enforced

### Supporting Domains (SIGN, FILE, EMIT, BLOCK, ADMIN, TP, SRCH)
- [x] SIGN-01~03, FILE-01~03, EMIT-01~03, BLOCK-01~03, ADMIN-01~05, TP-01~02, SRCH-01

### Cross-Cutting (AOP, EDGE, BUG)
- [x] AOP-01~04, EDGE-01~06, BUG-01~03

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
| V2-01 | @DistributedLock 替代 synchronized | 多实例部署竞态风险 | Resolved |
| V2-02 | @Version 乐观锁保护金融实体 | 防止并发更新丢失 | Resolved |
| V2-03 | 凭据全部外部化，移除弱默认值 | 生产部署安全底线 | Resolved |
| V2-04 | EnrollmentResult public (not package-private) | 跨包访问需求 (FabricGatewayConfig → service) | Resolved |

## Deferred Items

| Category | Items | Deferred To | Status |
|----------|-------|-------------|--------|
| E2E smoke | route 失败修复 | backlog | LOW |
| Prophet | Windows CmdStan 环境 | backlog | LOW |
| Auction | 订单分页化 (PERF-04) | backlog | LOW |

---
*Project created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
*v2.0 shipped: 2026-05-21*
*v2.1 shipped: 2026-05-22*
*Last updated: 2026-05-22 after v2.1 milestone*
