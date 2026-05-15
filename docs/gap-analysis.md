# OAISS Chain 文档-代码差距清单

> 生成日期：2026-05-12 | 对齐方向：代码补齐文档 | 优先级策略：A > B > C

---

## 总览

| 维度 | 文档规范 | 代码现状 | 差距 |
|------|---------|---------|------|
| Controller 数量 | 16 | 18（+2未记录） | EnterpriseController, ReviewerController 未在文档4.x节中独立说明 |
| API 端点数量 | 79 | 118 | 39个端点未在文档接口列表中体现 |
| 数据库表 | 21 | 21 | 一致 |
| 前端页面 | 20 | 21（+1 NotFound） | 基本一致 |
| Fabric 区块链 | 完整集成 | Mock 实现 | 完全缺失 |
| AI 智能模块 | 3个子功能 | 仅1个Stub | 缺2个核心功能 |
| 碳核算模型 | 官方公式（25+9参数） | 通用 scope1/2/3 计算 | 未实现行业专用公式 |
| 签发准入证书/审核员资格证 | 文档权限矩阵中有 | 仅有 Entity/Repository | 无 Controller 端点 |

---

## GAP-01: AI 市场智能预测（A级需求 3.6.1）

| 项目 | 说明 |
|------|------|
| **需求来源** | 01-需求分析 3.6.1，A级需求 |
| **文档描述** | 4.11.2 市场智能预测 — 基于近期数据进行未来市场交易情况预测（挂牌量、挂牌价、成交量、成交价、新注册用户数量等） |
| **代码现状** | 无实现。无 MarketPredictionService，无对应 Controller 端点 |
| **差距** | 完全缺失 |
| **优先级** | A |
| **工作量** | L（需新建 Service + Controller + 前端页面） |
| **技术债务** | 否，为核心需求 |

**需补齐内容**：
- 后端：`MarketPredictionService`（市场数据聚合 + 预测算法）
- 后端：新增端点 `GET /api/v1/prediction/market`（Admin/ThirdParty）
- 前端：市场预测可视化页面（图表+表格，嵌入管理员和第三方监管视图）
- 数据源：AuctionOrder, Transaction, User 表的历史数据

---

## GAP-02: AI 企业境况智能推断（A级需求 3.6.2）

| 项目 | 说明 |
|------|------|
| **需求来源** | 01-需求分析 3.6.2，A级需求 |
| **文档描述** | 4.11.3 企业境况智能推断 — 基于企业历史活动和客观信息推断环保性、违规检测 |
| **代码现状** | 无实现。无 EnterpriseInferenceService，无对应 Controller 端点 |
| **差距** | 完全缺失 |
| **优先级** | A |
| **工作量** | L |
| **技术债务** | 否，为核心需求 |

**需补齐内容**：
- 后端：`EnterpriseInferenceService`（企业行为分析 + 违规检测算法）
- 后端：新增端点 `GET /api/v1/prediction/enterprise/{id}/inference`（Admin/ThirdParty）
- 前端：企业推断可视化页面（嵌入管理员和第三方监管视图）
- 数据源：CarbonReport, Transaction, CreditScore, EmissionRating 表

---

## GAP-03: AI 碳排放预测 — Stub 需升级（A级需求）

| 项目 | 说明 |
|------|------|
| **需求来源** | 01-需求分析 3.6（AI功能），A级需求 |
| **文档描述** | 4.11.4 企业自身碳排放预测 — AI分析预测未来碳排放量 |
| **代码现状** | `CarbonPredictionService` 已存在，但是 **Stub 实现**（简单线性回归），注释写"生产环境替换为ML模型" |
| **差距** | 骨架已有，算法需替换为真实ML模型 |
| **优先级** | A |
| **工作量** | M |
| **技术债务** | 是（标记为 Stub） |

**现有端点**：`POST /emission/predict`（EmissionController）
**现有实现**：`CarbonPredictionService.predict()` — 基于最近12期报告的线性外推

**需升级内容**：
- 替换为时序预测模型（Prophet/LSTM/ARIMA）
- 增加置信区间输出
- 前端 EmissionData.vue 已有调用，需同步升级图表展示

---

## GAP-04: Hyperledger Fabric 区块链 — Mock 需替换

| 项目 | 说明 |
|------|------|
| **需求来源** | 02-概要介绍（创新点），03-详细方案（核心架构） |
| **文档描述** | 4.12 区块链模块 — 基于Fabric的区块链存证，Fabric节点映射（3.2） |
| **代码现状** | `BlockchainService` 完全是 Mock 实现，所有方法返回 `tx_mock_*`，注释写"生产环境替换为实际SDK调用" |
| **差距** | 完全为 Mock，无真实 Fabric 交互 |
| **优先级** | B（核心架构但开发阶段 Mock 可接受） |
| **工作量** | XL（需搭建 Fabric 网络 + 编写链码 + Java SDK 集成） |
| **技术债务** | 是（标记为 Mock） |

**现有端点（均可工作，但返回 Mock 数据）**：
| 端点 | Mock行为 |
|------|---------|
| `GET /blockchain/status` | 返回 connected=true, mode=MOCK |
| `GET /blockchain/block/{blockNumber}` | 返回 Mock 区块信息 |
| `GET /blockchain/transaction/{txHash}` | 返回 Mock 交易信息 |
| `GET /blockchain/transactions` | 返回 Mock 分页列表 |
| `GET /blockchain/blocks/latest` | 返回 Mock 最新区块 |

**需替换内容**：
- 搭建 Fabric 测试网络（docker-compose 已有占位）
- 编写链码：carbon-report-cc, carbon-trade-cc, carbon-neutral-cc
- 集成 Fabric Gateway Java SDK
- 替换 BlockchainService 中所有 Mock 方法

---

## GAP-05: 碳核算模型 — 未实现行业专用公式

| 项目 | 说明 |
|------|------|
| **需求来源** | 04-碳核算模型介绍文档 |
| **文档描述** | 4.2 碳核算模型 — 发电企业25参数公式 + 电网企业9参数公式 |
| **代码现状** | `CarbonService.calculateEmissions()` 使用通用 scope1/2/3 计算（activity_data × emission_factor），**未实现**官方核算指南的行业专用公式 |
| **差距** | 有通用计算，缺行业专用公式 |
| **优先级** | B |
| **工作量** | M |
| **技术债务** | 否，但需新增 |

**现有实现**（`CarbonService.java:248-292`）：
```java
// 通用计算：scope = Σ(activity_data × emission_factor)
// 三个范围分别遍历 JSON 数组中的条目
```

**需新增**：
- `PowerGenerationAccountingService` — 发电企业碳核算（25参数）
- `GridEnterpriseAccountingService` — 电网企业碳核算（9参数）
- 前端：根据企业行业类型显示对应的核算表单
- 端点：`POST /carbon/reports/calculate`（根据行业类型自动选择模型）

---

## GAP-06: 签发准入证书（文档权限矩阵中列出）

| 项目 | 说明 |
|------|------|
| **需求来源** | 03-详细方案，文档权限矩阵 |
| **文档描述** | 权限矩阵第24行"签发准入证书" — Admin 权限 |
| **代码现状** | `EntryPermission` Entity + `EntryPermissionRepository` 已存在，但**无 Controller 端点、无 Service 方法** |
| **差距** | 仅有数据层，无业务层和接口层 |
| **优先级** | B |
| **工作量** | S |
| **技术债务** | 否 |

**需补齐**：
- `EntryPermissionService` — 签发/撤销/查询准入证书
- AdminController 新增端点：
  - `POST /admin/entry-permission/{enterpriseId}/issue` — 签发准入证书
  - `DELETE /admin/entry-permission/{enterpriseId}` — 撤销准入证书
  - `GET /admin/entry-permission` — 查询准入证书列表
- 前端：管理员页面增加准入证书管理

---

## GAP-07: 签发审核员资格证（文档权限矩阵中列出）

| 项目 | 说明 |
|------|------|
| **需求来源** | 03-详细方案，文档权限矩阵 |
| **文档描述** | 权限矩阵第25行"签发审核员资格证" — Admin 权限 |
| **代码现状** | `ReviewerQualification` Entity + `ReviewerQualificationRepository` 已存在，但**无 Controller 端点、无 Service 方法** |
| **差距** | 仅有数据层，无业务层和接口层 |
| **优先级** | B |
| **工作量** | S |
| **技术债务** | 否 |

**需补齐**：
- `ReviewerQualificationService` — 签发/撤销/查询审核员资格证
- AdminController 新增端点：
  - `POST /admin/reviewer-qualification/{reviewerId}/issue` — 签发审核员资格证
  - `DELETE /admin/reviewer-qualification/{reviewerId}` — 撤销资格证
  - `GET /admin/reviewer-qualification` — 查询资格证列表
- 前端：管理员页面增加审核员资格证管理

---

## GAP-08: EnterpriseController 未在文档中独立说明

| 项目 | 说明 |
|------|------|
| **需求来源** | 代码实际存在 |
| **文档描述** | 无独立章节 |
| **代码现状** | EnterpriseController 有4个端点：GET /enterprise/info, GET /enterprise/quota, PUT /enterprise/contact, GET /enterprise/{id} |
| **差距** | 文档缺少该 Controller 的说明 |
| **优先级** | C |
| **工作量** | XS（仅文档更新） |

**代码端点**：
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/enterprise/info` | 获取当前企业信息 | ENTERPRISE |
| GET | `/enterprise/quota` | 获取碳配额信息 | ENTERPRISE |
| PUT | `/enterprise/contact` | 更新联系方式 | ENTERPRISE |
| GET | `/enterprise/{id}` | 根据ID查询企业 | ENTERPRISE |

---

## GAP-09: ReviewerController 未在文档中独立说明

| 项目 | 说明 |
|------|------|
| **需求来源** | 代码实际存在 |
| **文档描述** | 无独立章节 |
| **代码现状** | ReviewerController 有4个端点：GET /reviewer/info, GET /reviewer/reports/pending, GET /reviewer/history, GET /reviewer/statistics |
| **差距** | 文档缺少该 Controller 的说明 |
| **优先级** | C |
| **工作量** | XS（仅文档更新） |

**代码端点**：
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/reviewer/info` | 获取审核员信息 | REVIEWER |
| GET | `/reviewer/reports/pending` | 获取待审核报告 | REVIEWER |
| GET | `/reviewer/history` | 审核历史 | REVIEWER |
| GET | `/reviewer/statistics` | 审核统计 | REVIEWER |

---

## GAP-10: AdminController 额外端点未在文档中记录

| 项目 | 说明 |
|------|------|
| **需求来源** | 代码实际存在 |
| **文档描述** | 4.15.2 仅列出4个端点 |
| **代码现状** | AdminController 实际有6个端点，多出2个 |
| **差距** | 文档遗漏2个端点 |
| **优先级** | C |
| **工作量** | XS（仅文档更新） |

**文档已记录**：`/admin/users`, `/admin/users/{userId}/status`, `/admin/dashboard`, `/admin/statistics`

**文档遗漏**：
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/admin/config` | 获取系统配置 | ADMIN |
| GET | `/admin/permissions` | 获取权限列表 | ADMIN |

---

## GAP-11: 文档 API 总览数字不准确

| 项目 | 说明 |
|------|------|
| **需求来源** | 文档8.1节 |
| **文档描述** | "16个Controller，79个接口" |
| **代码现状** | 18个Controller，118个接口 |
| **差距** | 数字严重偏低 |
| **优先级** | C |
| **工作量** | XS（仅文档更新） |

**差异明细**：

| Controller | 文档记录 | 代码实际 | 差异 |
|-----------|---------|---------|------|
| AuthController | 8 | 8 | 一致 |
| UserController | 6 | 6 | 一致 |
| CarbonController | 7 | 7 | 一致 |
| TradeController | 7 | 7 | 一致 |
| CreditScoreController | 11 | 11 | 一致 |
| DigitalSignatureController | 7 | 7 | 一致 |
| AdminController | 4 | **6** | +2 |
| BlockchainController | 5 | 5 | 一致 |
| CarbonCoinController | 4 | 4 | 一致 |
| DoubleAuctionController | 6 | 6 | 一致 |
| CarbonNeutralProjectController | 16 | **17** | +1（`GET /projects`） |
| EmissionController | 4 | 4 | 一致 |
| SearchController | 3 | 3 | 一致 |
| ThirdPartyController | 4 | 4 | 一致 |
| FileController | 11 | 11 | 一致 |
| CaptchaController | 4 | 4 | 一致 |
| **EnterpriseController** | 0 | **4** | 未记录 |
| **ReviewerController** | 0 | **4** | 未记录 |

**CarbonNeutralProjectController 额外端点**：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/carbon-neutral/projects` | 项目列表（与 /search 不同，无搜索条件） |

---

## GAP-12: 前端 API 模块未完全覆盖后端端点

| 项目 | 说明 |
|------|------|
| **需求来源** | 前后端对齐 |
| **差距** | 以下后端端点无对应前端 API 调用 |
| **优先级** | C |
| **工作量** | S |

**后端有但前端未调用的端点**：

| 后端端点 | 前端模块 | 状态 |
|---------|---------|------|
| `POST /auth/register` | auth.ts | 缺失 |
| `POST /auth/captcha` | auth.ts | 缺失（用 captcha.ts 的 `/captcha/generate`） |
| `POST /auth/refresh` | auth.ts | 缺失 |
| `GET /auth/check-ip` | auth.ts | 缺失 |
| `GET /auth/me` | auth.ts | 缺失 |
| `PUT /auth/password` | auth.ts | 缺失（用 user.ts 的 `/user/password`） |
| `POST /auction/match` | auction.ts | 缺失（管理员操作） |
| `POST /carbon-coin/recharge` | carbonCoin.ts | 缺失（管理员操作） |
| `GET /credit/ranking` | credit.ts | 缺失 |
| `GET /credit/{enterpriseId}` | credit.ts | 缺失 |
| `GET /credit/{enterpriseId}/history` | credit.ts | 缺失 |
| `POST /credit/deduct` | credit.ts | 缺失 |
| `POST /credit/bonus` | credit.ts | 缺失 |
| `POST /credit/evaluate/{id}` | credit.ts | 缺失 |
| `GET /credit/restricted` | credit.ts | 缺失 |
| `GET /credit/frozen` | credit.ts | 缺失 |
| `GET /credit/check-permission/{id}` | credit.ts | 缺失 |
| `GET /blockchain/block/{n}` | blockchain.ts | 缺失 |
| `GET /blockchain/transaction/{hash}` | blockchain.ts | 缺失 |
| `POST /signature/sign` | signature.ts | 缺失 |
| `POST /signature/verify` | signature.ts | 缺失 |
| `POST /signature/encrypt` | signature.ts | 缺失 |
| `POST /signature/decrypt` | signature.ts | 缺失 |
| `POST /emission/ratings` | emission.ts | 缺失 |
| `GET /enterprise/info` | 无对应模块 | 完全缺失 |
| `GET /enterprise/quota` | 无对应模块 | 完全缺失 |
| `PUT /enterprise/contact` | 无对应模块 | 完全缺失 |
| `GET /enterprise/{id}` | 无对应模块 | 完全缺失 |
| `GET /reviewer/info` | 无对应模块 | 完全缺失 |
| `GET /reviewer/reports/pending` | 无对应模块 | 完全缺失 |
| `GET /reviewer/history` | 无对应模块 | 完全缺失 |
| `GET /reviewer/statistics` | 无对应模块 | 完全缺失 |
| `POST /trade/auction` | trade.ts | 缺失 |
| `POST /trade/{id}/confirm` | trade.ts | 缺失 |
| `GET /trade/{id}` | trade.ts | 缺失 |
| `GET /trade/list` | trade.ts | 缺失 |
| `GET /user/{userId}` | user.ts | 缺失 |
| `GET /user/check-username` | user.ts | 缺失 |
| `GET /user/check-email` | user.ts | 缺失 |

---

## GAP-13: 前端缺少 AI 智能模块页面

| 项目 | 说明 |
|------|------|
| **需求来源** | 01-需求分析 3.6 |
| **文档描述** | 4.11 AI智能模块 — 市场预测、企业推断、碳排放预测 |
| **代码现状** | EmissionData.vue 调用了 `predictEmission`，但无市场预测和企业推断页面 |
| **差距** | 缺少2个AI功能页面 |
| **优先级** | A（与 GAP-01/02 关联） |
| **工作量** | M（需新建2个 Vue 页面） |

**需新建页面**：
- `admin/MarketPrediction.vue` — AI市场智能预测（管理员端）
- `admin/EnterpriseInference.vue` — AI企业境况推断（管理员端）
- 可选：在 `third-party/Monitor.vue` 中集成预测和推断标签页

---

## 优先级汇总

### A 级（核心需求，必须补齐）

| GAP ID | 名称 | 工作量 | 类型 |
|--------|------|--------|------|
| GAP-01 | AI 市场智能预测 | L | 新功能 |
| GAP-02 | AI 企业境况智能推断 | L | 新功能 |
| GAP-03 | AI 碳排放预测升级 | M | Stub→真实 |
| GAP-13 | 前端 AI 页面 | M | 新页面 |

### B 级（重要需求，优先补齐）

| GAP ID | 名称 | 工作量 | 类型 |
|--------|------|--------|------|
| GAP-04 | Fabric 区块链集成 | XL | Mock→真实 |
| GAP-05 | 碳核算行业专用公式 | M | 新功能 |
| GAP-06 | 签发准入证书 | S | 新端点 |
| GAP-07 | 签发审核员资格证 | S | 新端点 |

### C 级（文档对齐，低优先级）

| GAP ID | 名称 | 工作量 | 类型 |
|--------|------|--------|------|
| GAP-08 | EnterpriseController 文档 | XS | 文档更新 |
| GAP-09 | ReviewerController 文档 | XS | 文档更新 |
| GAP-10 | AdminController 额外端点 | XS | 文档更新 |
| GAP-11 | API 总览数字修正 | XS | 文档更新 |
| GAP-12 | 前端 API 覆盖补齐 | S | 前端补齐 |

---

## 技术债务清单

| 编号 | 描述 | 当前状态 | 影响 | 建议处理时间 |
|------|------|---------|------|------------|
| TD-01 | BlockchainService 为 Mock 实现 | 所有上链操作返回 Mock 数据 | 数据不可信，无法真正防篡改 | B 级需求完成时 |
| TD-02 | CarbonPredictionService 为 Stub | 简单线性回归，非 AI 模型 | 预测准确度低 | A 级需求完成时 |
| TD-03 | 碳核算仅通用公式 | 未区分发电/电网行业 | 核算不符合官方指南 | B 级需求完成时 |
| TD-04 | 前端大量后端 API 未调用 | 39个端点无前端调用 | 功能不完整 | 按模块逐步补齐 |

---

## 工作量估算

| 优先级 | GAP 数 | 预估工作量 |
|--------|--------|-----------|
| A 级 | 4 项 | 约 3-4 周（2人） |
| B 级 | 4 项 | 约 4-6 周（2人，含 Fabric 搭建） |
| C 级 | 5 项 | 约 1 周（1人） |
| **合计** | **13 项** | **约 8-11 周** |

---

> 本文档基于 product-specification.md 与代码库（18 Controller / 118 API / 21 表 / 21 页面）的逐项对照生成，结合 MCP 语义搜索验证。
