# 碳核算与碳交易业务需求 SPEC

> 日期: 2026-05-09 | 状态: 已批准 | 来源: docs/raw/01-项目需求分析.md

## 1. 概述

基于原始需求分析文档，提取 OAISS CHAIN 系统 6 大业务模块的功能需求、数据模型和需求分级，并与当前代码实现做交叉验证。

### 需求来源

原始文档定义了 6 个核心模块、11 张数据表、10 项需求（A/B/C 三级）。

---

## 2. 模块需求矩阵

| # | 模块 | 需求分级 | 后端实现 | 前端实现 | 差距 |
|---|------|---------|---------|---------|------|
| 1 | 用户登录 | A | AuthController (8端点) | Login.vue | ✅ 完整 |
| 2 | 碳核算-智能核算 | A | CarbonService | CarbonUpload.vue | ✅ 完整 |
| 3 | 碳核算-数据审核 | B | CarbonService + `/review` | AuditList.vue | ✅ 完整 |
| 4 | 碳交易-双向拍卖 | A | DoubleAuctionService | TradingMarket.vue | ✅ 完整 |
| 5 | 碳交易-P2P | C | TradeService | TradingP2P.vue | ✅ 完整 |
| 6 | 搜索查询-账户历史 | A | SearchService | CompanyDashboard.vue | ✅ 完整 |
| 7 | 搜索查询-市场信息 | B | SearchService + `/market-overview` | DataStatistics.vue | ✅ 完整 |
| 8 | 管理调控-用户管理 | A | AdminController | SystemUsers.vue | ✅ 完整 |
| 9 | AI-市场智能预测 | A | CarbonPredictionService (Stub) | EmissionData.vue | ⚠️ Stub实现 |
| 10 | AI-企业境况推断 | A | EmissionRatingService | EmissionData.vue | ✅ 完整 |

---

## 3. 需求与实现 Gap 分析

### 3.1 完全满足的需求（8/10）

需求 ID 3.1.1、3.1.2、3.2.1、3.2.2、3.3.1、3.3.2、3.4.1、3.5.1 均已在后端 Service + 前端 View 中完整实现。

### 3.2 部分满足的需求（2/10）

**3.6.1 市场智能预测（需求分级 A）**
- 原始需求：基于过去一周市场信息（交易量、交易价、挂牌量、挂牌价）预测未来市场走势
- 当前实现：`CarbonPredictionService` 仅基于历史碳排放数据做线性趋势外推，**不含交易市场预测**
- Gap：缺少交易量/交易价/挂牌量/挂牌价的预测，缺少基于市场数据的 AI 模型

**3.6.2 企业境况智能推断（需求分级 A）**
- 原始需求：基于企业历史活动和客观信息，智能推断企业发展状况及环保性
- 当前实现：`EmissionRatingService` 基于碳排放量生成 A/B/C/D/E 评级，含行业排名
- Gap：缺少"企业发展状况推断"和"违规/超额排放评估"的 AI 推断能力

### 3.3 数据模型差异

原始需求定义 11 张表，当前实现 21 张表。原始表结构较简略（如 transaction 表仅 5 字段），当前实现大幅扩展。

| 原始表 | 当前实现表 | 差异 |
|--------|-----------|------|
| user (7字段) | user (15字段) | 新增 password, phone, status, allowed_ips, last_login 等 |
| enterprise (10字段) | enterprise (16字段) | 新增 carbon_quota, carbon_used, carbon_tradable, cert_status 等 |
| carbon_report (6字段) | carbon_report (21字段) | 新增 scope1/2/3_emission, signature_data, blockchain_tx_hash 等 |
| transaction (5字段) | transaction (15字段) | 新增 trade_type, unit_price, total_amount, status 等 |
| — | auction_order (新增) | 双向拍卖挂单表 |
| — | matching_result (新增) | 撮合结果表 |
| — | carbon_coin_account (新增) | 碳币账户 |
| — | carbon_coin_transaction (新增) | 碳币交易流水 |
| — | credit_score (新增) | 信誉评分 |
| — | credit_event (新增) | 信誉事件 |
| — | emission_rating (新增) | 排放评级 |
| — | carbon_neutral_project (新增) | 碳中和项目 |
| — | rsa_key_pair (新增) | RSA密钥对 |
| — | operation_log (新增) | 操作日志 |

---

## 4. 验证方法

| 验证项 | 方法 |
|--------|------|
| 需求覆盖率 | 10 项需求逐条对照后端 Controller + 前端 View |
| API 完整性 | 后端 112 端点 vs 前端 108 端点覆盖（96.4%） |
| 数据模型完整性 | 21 张 Entity ↔ 21 张表 ↔ 21 个 Repository 全部对齐 |

---

## 5. 决策记录

| # | 决策 | 原因 |
|---|------|------|
| 1 | AI 预测保留 Stub 实现 | 原始需求要求 A 级，但当前无 ML 模型环境，线性外推满足演示需求 |
| 2 | 数据模型大幅扩展 | 原始需求表结构过于简略，无法支撑完整业务流程 |
| 3 | 新增 10 张表 | 碳币、信誉、碳中和、排放评级、数字签名等模块在原始需求中有功能描述但无表设计 |