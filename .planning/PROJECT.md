# OAISS CHAIN

## What This Is

OAISS CHAIN 是一个碳交易与区块链平台，面向五种用户角色（企业、审核员、认证方、第三方监管、管理员）提供碳排放报告提交、碳积分管理、双重拍卖交易、P2P 交易、碳中和项目管理等完整业务流程。当前阶段目标是通过全角色全业务流程的手工测试，使系统达到功能完整、无 bug、可继续区块链开发的状态。

## Core Value

所有五种角色的核心业务流程（登录→数据录入→审批→交易→结算）必须在真实后端数据下端到端跑通，确保系统功能完整可用。

## Requirements

### Validated

<!-- 从现有代码推断的已完成功能 -->

- ✓ 用户认证系统（JWT 登录/注册/刷新令牌）— 已实现
- ✓ 五角色 RBAC 权限控制（@PreAuthorize + Vue Router meta.roles）— 已实现
- ✓ 碳排放报告提交与审核流程 — 已实现
- ✓ 双重拍卖交易引擎 — 已实现（存在并发问题 CON-01/02/03）
- ✓ P2P 交易流程 — 已实现
- ✓ 碳积分账户管理 — 已实现
- ✓ 碳中和项目管理 — 已实现
- ✓ 数字签名（RSA）— 已实现
- ✓ 文件上传（MinIO）— 已实现
- ✓ 区块链记录 — 已实现（mock 模式）
- ✓ 前端 i18n 国际化 — 已完成
- ✓ 前端 TypeScript 迁移 — 已完成

### Active

- [ ] **TEST-01**: 全角色登录验证 — 5 种角色均能成功登录，获取 JWT，访问各自角色首页
- [ ] **TEST-02**: 企业端碳排放报告 CRUD — 上传报告、查看列表、查看详情
- [ ] **TEST-03**: 审核员端报告审批 — 查看待审列表、批准/拒绝报告、状态流转验证
- [ ] **TEST-04**: 认证方端认证操作 — 查看认证列表、执行认证/驳回
- [ ] **TEST-05**: 企业端碳积分操作 — 查看余额、转账、交易记录
- [ ] **TEST-06**: 企业端双重拍卖交易 — 挂买单/卖单、撮合匹配、查看成交记录
- [ ] **TEST-07**: 企业端 P2P 交易 — 发起交易、对方接受/拒绝、结算验证
- [ ] **TEST-08**: 企业端碳中和项目 — 创建项目、状态变更、查看列表
- [ ] **TEST-09**: 企业端信用评分 — 查看评分、评分变动记录
- [ ] **TEST-10**: 管理员端系统管理 — 用户管理、系统配置、数据字典
- [ ] **TEST-11**: 第三方监管端 — 查看监控数据、交易审计
- [ ] **TEST-12**: 数字签名功能 — 生成密钥对、签名验证
- [ ] **TEST-13**: 文件上传下载 — MinIO 文件操作全流程
- [ ] **TEST-14**: 区块链记录查看 — 交易哈希记录、区块浏览
- [ ] **TEST-15**: 修复测试中发现的所有 bug
- [ ] **TEST-16**: 修复关键安全问题（SEC-03 Swagger 生产环境暴露、SEC-04 CORS 默认值）

### Out of Scope

- 区块链智能合约开发 — 测试完成后单独进行
- 性能优化（PERF-01~07）— 功能稳定后再优化
- 并发问题彻底修复（CON-01~05）— 需要架构级重构，记录但不阻塞测试
- 前端单元测试覆盖率提升（QUAL-06）— 当前 24 个测试文件足够
- 后端测试覆盖率提升至 90%（QUAL-07）— 当前 43% 够用
- Redis 高可用 / 分布式部署 — 单节点开发环境足够

## Context

**技术栈:**
- 后端: Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8, Redis 7, MinIO, JWT (jjwt 0.12.5), Flyway
- 前端: Vue 3.5, TypeScript, Vite 8, Element Plus 2.13, Pinia 3, Vue Router 5, ECharts 6, vue-i18n 11
- 基础设施: Docker Compose (MySQL, Redis, MinIO, 后端, 前端)

**项目状态:**
- 代码库已映射完成（7 个结构化文档，2042 行）
- i18n 迁移已完成
- TypeScript 迁移已完成
- 安全审计 48/63 修复已完成
- Flyway 迁移: V1__init_schema.sql, V2__seed_data.sql

**已知问题（不阻塞测试但需关注）:**
- SEC-01: RSA 私钥存储在数据库中
- SEC-03: Swagger 端点生产环境可访问
- CON-01: DoubleAuctionService 使用 synchronized，不适合分布式
- CON-04: 无 @Version 乐观锁
- PERF-02: Redis KEYS 命令在生产环境有性能风险

**测试环境:**
- 后端: localhost:8080
- 前端: localhost:5173
- 数据库: MySQL 8 (Docker)
- 缓存: Redis 7 (Docker)
- 文件存储: MinIO (Docker)

## Constraints

- **兼容性**: 测试过程中不能破坏现有功能，所有修改必须向后兼容
- **真实数据**: 必须使用真实后端 API 和数据库，不使用 mock 数据
- **全角色覆盖**: 5 种角色的业务流程都必须测试到
- **安全底线**: 测试中发现的安全问题必须修复，特别是 SEC-03 和 SEC-04
- **技术栈锁定**: Java 17 + Spring Boot 3.2.5 + Vue 3.5 + TypeScript，不引入新框架
- **数据库约束**: 使用 Flyway 管理迁移，不直接修改数据库 schema

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 全手工测试而非自动化 E2E | 用户明确要求手工测试，需要真实交互验证 | — Pending |
| 修复 SEC-03/04 作为测试前置条件 | Swagger 生产暴露和 CORS 默认值是低风险高回报修复 | — Pending |
| 不修复并发问题（CON-01~05）| 需要架构级重构，当前单节点开发环境可接受 | — Pending |
| 保留 mock 区块链模式 | 真实区块链集成是后续阶段目标 | — Pending |
| 测试数据使用 Flyway seed | V2__seed_data.sql 已有基础数据，测试在此基础上补充 | — Pending |

---
*Last updated: 2026-05-08 after codebase mapping*
