# OAISS CHAIN 规范文档索引

**版本**: 1.0.0
**日期**: 2026-05-10

---

## 文档概览

基于 `docs/raw/` 目录下五个原始需求文档，本项目已完成需求差异分析并生成相应的技术规范文档。

---

## 文档清单

### 1. 差异分析报告

| 文档 | 路径 | 说明 |
|------|------|------|
| GAP-ANALYSIS.md | [docs/specs/GAP-ANALYSIS.md](./GAP-ANALYSIS.md) | 原始需求与当前实现的完整对比分析 |

### 2. 技术规范文档

| 文档 | 路径 | 对应原始文档 | 优先级 |
|------|------|-------------|--------|
| CARBON-CALCULATION-SPEC.md | [docs/specs/CARBON-CALCULATION-SPEC.md](./CARBON-CALCULATION-SPEC.md) | 04-碳核算模型介绍文档.md | P0 |
| AI-MODULE-SPEC.md | [docs/specs/AI-MODULE-SPEC.md](./AI-MODULE-SPEC.md) | 01-项目需求分析.md (3.6节) | P1 |
| BLOCKCHAIN-INTEGRATION-SPEC.md | [docs/specs/BLOCKCHAIN-INTEGRATION-SPEC.md](./BLOCKCHAIN-INTEGRATION-SPEC.md) | 03-项目详细方案.md (5.2.3节) | P1 |

---

## 差异汇总

### 核心差距 (P0)

| ID | 差距 | 影响 | 文档引用 |
|----|------|------|----------|
| GAP-001 | 发电企业碳核算模型未完整实现 | 碳核算准确性 | CARBON-CALCULATION-SPEC.md |
| GAP-002 | 电网企业碳核算模型未实现 | 功能缺失 | CARBON-CALCULATION-SPEC.md |
| GAP-003 | Fabric区块链未真实对接 | 区块链核心价值 | BLOCKCHAIN-INTEGRATION-SPEC.md |

### 重要差距 (P1)

| ID | 差距 | 影响 | 文档引用 |
|----|------|------|----------|
| GAP-004 | AI预测为Stub实现 | 预测准确性 | AI-MODULE-SPEC.md |
| GAP-005 | 企业境况智能推断未实现 | 功能缺失 | AI-MODULE-SPEC.md |
| GAP-006 | 身份认证未采用区块链方案 | 去中心化身份 | BLOCKCHAIN-INTEGRATION-SPEC.md |

---

## 实施优先级建议

### 第一阶段 (P0 - 预计4周)

```
Week 1-2: 碳核算模型实现
├── 扩展 CarbonReport 实体字段
├── 实现发电企业计算服务 (25参数)
└── 实现电网企业计算服务 (9参数)

Week 3-4: 区块链基础搭建
├── 本地Fabric网络部署
├── Chaincode开发与测试
└── 后端SDK集成
```

### 第二阶段 (P1 - 预计4周)

```
Week 1-2: AI模块升级
├── 集成Prophet预测模型
├── 实现企业境况推断服务
└── 风险预警模块

Week 3-4: 区块链深度集成
├── 现有服务改造
├── 测试网部署
└── 性能优化
```

---

## 原始文档引用

| 原始文档 | 关键章节 |
|----------|----------|
| 01-项目需求分析.md | 3.1 碳核算模块、3.6 AI智能模块 |
| 02-项目概要介绍.md | 2 创意描述 |
| 03-项目详细方案.md | 5.2 核心算法、6 系统设计 |
| 04-碳核算模型介绍文档.md | 发电企业公式、电网企业公式 |
| 05-项目测试文档.md | 3 测试结果及发现 |

---

## 附录：原始需求快速索引

### A. 功能模块需求对照表

| 模块 | 原始文档章节 | SPEC文档 |
|------|-------------|----------|
| 用户登录 | 01-3.5 | - (已实现) |
| 碳核算 | 01-3.1, 04-全文 | CARBON-CALCULATION-SPEC.md |
| 数据审核 | 01-3.1.2 | - (已实现) |
| 碳交易 | 01-3.2 | BLOCKCHAIN-INTEGRATION-SPEC.md |
| 搜索查询 | 01-3.3 | - (已实现) |
| 管理调控 | 01-3.4 | - (已实现) |
| AI智能 | 01-3.6 | AI-MODULE-SPEC.md |

### B. 算法需求对照表

| 算法 | 原始文档章节 | 当前状态 | SPEC文档 |
|------|-------------|----------|----------|
| RSA数字签名 | 03-5.2.1 | ✅ 已实现 | - |
| 双向拍卖 | 03-5.2.2 | ✅ 已实现 | - |
| Fabric交易 | 03-5.2.3 | ⚠️ 模拟 | BLOCKCHAIN-INTEGRATION-SPEC.md |
| 身份认证 | 03-5.2.4 | ⚠️ JWT | BLOCKCHAIN-INTEGRATION-SPEC.md |
| 碳核算模型 | 04-全文 | ⚠️ Stub | CARBON-CALCULATION-SPEC.md |
