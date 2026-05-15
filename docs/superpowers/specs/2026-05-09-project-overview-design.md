# 项目概要与技术架构 SPEC

> 日期: 2026-05-09 | 状态: 已批准 | 来源: docs/raw/02-项目概要介绍.md

## 1. 概述

基于原始概要介绍文档，提取 OAISS CHAIN 系统的技术创新点、功能特性与应用对象定义，并验证当前实现与原始设计的一致性。

---

## 2. 创新点实现验证

### 2.1 算法创新点

| 创新点 | 原始描述 | 当前实现 | 状态 |
|--------|---------|---------|------|
| RSA 数字签名 | 碳核算过程中企业/审核员的加密/解密和数字签名 | `DigitalSignatureService` — 密钥对生成、签名、验签、加密、解密 | ✅ 完整 |
| 双向拍卖撮合 | 双向拍卖配合 P2P 的双阶段交易机制 | `DoubleAuctionService` — 价格优先/时间优先撮合算法 + P2P 交易 | ✅ 完整 |
| Fabric 交易模型 | 不同类型节点设置，用户身份与节点类型匹配 | `BlockchainService` — Mock 实现 | ⚠️ Mock |
| 区块链身份认证 | 用户上链操作前完成身份认证 | JWT + RBAC 认证，区块链层为 Mock | ⚠️ Mock |

### 2.2 功能创新点

| 创新点 | 原始描述 | 当前实现 | 状态 |
|--------|---------|---------|------|
| AI 预测及建议 | 基于历史碳排放预测未来趋势，给出建议 | `CarbonPredictionService` — Stub 线性外推 | ⚠️ Stub |
| 双向拍卖撮合 | 系统撮合后再进入 P2P 阶段 | `DoubleAuctionService.executeMatching()` | ✅ 完整 |
| 信誉分机制 | 初始信誉分、审核扣分、低分限制交易 | `CreditScoreService` — 5 级信誉等级 + 扣分/加分/限制/冻结 | ✅ 完整 |
| 多方审核核算 | 企业自核 → 审核员核查 → 管理员审查 → 第三方核查 | CarbonService 审核流程 + AuditList + VerifyList + Monitor | ✅ 完整 |
| 碳币交易市场 | 碳币充值、购买配额、出售配额、转账 | `CarbonCoinService` — 充值/购买/出售/转账 | ✅ 完整 |
| 碳排放评级系统 | 数字评分制度评定 A/B/C/D/E | `EmissionRatingService` — 评级 + 行业排名 | ✅ 完整 |
| 碳中和项目支持 | 碳汇/CCUS/可再生能源等项目管理 + 激励 | `CarbonNeutralProjectService` — 全生命周期 16 个端点 | ✅ 完整 |

### 2.3 非功能创新点

| 创新点 | 原始描述 | 当前实现 | 状态 |
|--------|---------|---------|------|
| 分布式区块链 | Fabric 区块链技术 | Mock 实现 | ⚠️ Mock |
| 超级账本 | Hyperledger Fabric | 未集成真实 Fabric 网络 | ⚠️ 未集成 |
| 服务熔断机制 | 压力过大时执行熔断 | 未实现（无 Hystrix/Resilience4j） | ❌ 缺失 |
| 数据持久化 | MySQL + 区块链双存储 | MySQL + Flyway 迁移完整 | ✅ MySQL 部分 |
| 信誉分服务 | 独立信誉分管理 | `CreditScoreService` + `CreditEvent` | ✅ 完整 |

---

## 3. 应用对象验证

| 角色 | 原始描述 | 当前实现 |
|------|---------|---------|
| 碳排放企业 | 无需专业知识，简单界面操作 | ENTERPRISE 角色，12 个 Vue 页面 |
| 数据审核员 | 熟悉业务流程，后台审核 | REVIEWER 角色，AuditList.vue |
| 第三方监管机构 | 深入了解系统机制 | THIRD_PARTY 角色，Monitor.vue |
| 管理员 | 系统管理、用户管理 | ADMIN 角色，4 个管理页面 |
| 认证机构 | （概要文档未单独列出） | AUTHENTICATOR 角色，VerifyList.vue |

---

## 4. Gap 总结

| Gap | 严重度 | 建议 |
|-----|--------|------|
| 区块链为 Mock 实现 | 中 | 按用户确认，区块链模块预期未完成 |
| 服务熔断机制缺失 | 低 | 竞赛环境无需生产级熔断 |
| AI 预测为 Stub | 中 | 线性外推满足演示，生产环境需替换 ML 模型 |