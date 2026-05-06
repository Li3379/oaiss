# OAISS CHAIN 代码完成度审计设计文档

**日期:** 2026-05-05
**状态:** 已批准
**范围:** 全栈代码完成度综合审计 + 测试覆盖差距分析 + 延期项目规划

---

## 1. 项目概述

OAISS CHAIN（碳排放数据可信管理与交易系统）是一个全栈应用：
- **后端:** Spring Boot 3.2.5, MySQL, Redis, MinIO, JWT
- **前端:** Vue 3, Element Plus, Pinia, ECharts, Vite 8.0.10
- **架构:** 16 REST Controllers, 19 Services, 22 Vue Pages, 17 API Modules

---

## 2. 模块审计矩阵

### 2.1 后端 Controller（16 个）

| # | Controller | 前缀 | 端点数 | 前端 API 模块 | Controller 测试 |
|---|-----------|------|--------|-------------|----------------|
| 1 | AuthController | `/auth` | 8 | auth.js | ✅ |
| 2 | UserController | `/user` | 6 | user.js | ✅ |
| 3 | CarbonController | `/carbon` | 7 | carbon.js | ✅ |
| 4 | TradeController | `/trade` | 7 | trade.js | ✅ |
| 5 | DoubleAuctionController | `/auction` | 6 | auction.js | ✅ |
| 6 | BlockchainController | `/blockchain` | 5 | blockchain.js | ✅ |
| 7 | CarbonCoinController | `/carbon-coin` | 4 | carbonCoin.js | ✅ |
| 8 | CreditScoreController | `/credit` | 11 | credit.js | ✅ |
| 9 | DigitalSignatureController | `/signature` | 7 | signature.js | ✅ |
| 10 | EmissionController | `/emission` | 4 | emission.js | ✅ |
| 11 | SearchController | `/search` | 3 | search.js | ✅ |
| 12 | FileController | `/file` | 11 | file.js | ✅ |
| 13 | CaptchaController | `/captcha` | 4 | captcha.js | ✅ |
| 14 | AdminController | `/admin` | 4 | admin.js | ✅ |
| 15 | ThirdPartyController | `/third-party` | 4 | thirdParty.js | ❌ |
| 16 | CarbonNeutralProjectController | `/carbon-neutral` | 15 | carbonNeutral.js | ❌ |

### 2.2 后端 Service（19 个）

全部 19 个 Service 均有 `@Service` 注解和对应测试：

| 类型 | Services | 测试状态 |
|------|---------|---------|
| 业务服务（16） | AuthService, UserService, CarbonService, TradeService, DoubleAuctionService, BlockchainService, CarbonCoinService, CreditScoreService, DigitalSignatureService, CarbonPredictionService, EmissionRatingService, SearchService, MinioService, CaptchaService, CarbonNeutralProjectService, ThirdPartyService | ✅ 19/19 |
| 基础设施（3） | RedisLockService, CachePreloadService, MetricsService | ✅ 全覆盖 |

### 2.3 前端 API 模块（17 个）

16 个映射到后端 Controller + 1 个 HTTP 客户端封装（request.js）。全部确认存在且路径一致。

### 2.4 前端 Vue 页面（22 个）

| 角色 | 页面数 | 页面列表 |
|------|--------|---------|
| Enterprise | 12 | CarbonUpload, OrdersManage, TradingMarket, TradingP2P, CompanyDashboard, CreditScore, CarbonCoin, Blockchain, CarbonNeutral, CarbonNeutralDetail, EmissionData, UserProfile |
| Auditor | 1 | AuditList |
| Authenticator | 1 | VerifyList |
| ThirdParty | 1 | Monitor |
| Admin | 4 | SystemUsers, SystemCarbon, SystemConfig, DataStatistics |
| 共享 | 3 | Login, OfficialHome, NotFound |

### 2.5 路由与角色映射

路由文件 `router/index.js` 定义 22 条路由，5 种角色（ENTERPRISE, REVIEWER, AUTHENTICATOR, THIRD_PARTY, ADMIN），含 auth guard 和角色权限校验。

---

## 3. 测试覆盖差距分析

### 3.1 后端测试（67 个测试文件）

| 类别 | 有测试 | 总数 | 覆盖率 |
|------|--------|------|--------|
| Controller | 14 | 16 | 87.5% |
| Service | 19 | 19 | 100% |
| Repository | 8 | 21 | 38% |
| Security/AOP/Config | 10 | 10 | 100% |
| Exception/DTO/Entity | 16 | 16 | 100% |

**缺失的 Controller 测试（2 个）：**

1. **ThirdPartyControllerTest** — 第三方监控接口（4 个端点）
   - `GET /third-party/org-info`
   - `GET /third-party/carbon-reports`
   - `GET /third-party/statistics`
   - `PUT /third-party/contact`

2. **CarbonNeutralProjectControllerTest** — 碳中和项目管理（15 个端点，最复杂）
   - CRUD: POST, PUT, GET, GET /search, GET /my
   - 流程: POST /submit, POST /review, POST /start, POST /submit-verification
   - 验证: POST /verify, POST /use-credits
   - 监控: PUT /monitoring
   - 认证: POST /apply-certification, POST /certify, POST /terminate
   - 查询: GET /pending-verification

### 3.2 前端测试（2 个单元测试 + 5 个 E2E spec）

| 类别 | 现有 | 目标 | 差距 |
|------|------|------|------|
| 单元测试 | 2 文件 / 17 测试 | 20+ 文件 / 100+ 测试 | ~90% |
| E2E 测试 | 5 specs | 12+ specs | ~60% |
| Vue 页面测试 | 0 | 22 页面 | 100% |

**现有前端测试：**
- `src/utils/__tests__/auth.test.js` — 11 tests
- `src/components/__tests__/ErrorBoundary.test.js` — 6 tests
- `tests/e2e/d1-login.spec.js`
- `tests/e2e/d2-carbon-upload.spec.js`
- `tests/e2e/d3-auction-market.spec.js`
- `tests/e2e/d4-p2p-trade.spec.js`
- `tests/e2e/d5-admin-users.spec.js`

**缺失的 E2E specs：**
- ThirdParty 监控流程
- CarbonNeutral 项目全流程
- CreditScore 查询流程
- Blockchain 浏览流程
- CarbonCoin 交易流程
- Auditor 审核流程
- Authenticator 验证流程

---

## 4. 前后端一致性发现

| 发现 | 状态 | 影响 |
|------|------|------|
| DigitalSignature 有 API 模块但无独立 Vue 页面 | ⚠️ 低 | 签名功能可能嵌入其他页面 |
| Authenticator 角色（VerifyList.vue）无独立后端 Controller | ⚠️ 低 | 可能复用 CarbonController `/review` |
| carbon.js 缺少 `getReport(id)` 单报告查询 | ⚠️ 中 | 后端有 `GET /carbon/reports/{reportId}` 但前端未封装 |
| 16 个后端 Controller ↔ 16 个前端 API 模块映射完整 | ✅ | 无遗漏 |

---

## 5. 延期项目实施方案

### 5.1 M4：TypeScript 迁移（3 阶段，3-5 天）

**Phase 1 — 工具链 + API 层（1-2 天）**
- 配置 tsconfig.json, vite-env.d.ts
- 迁移 `request.js` → `request.ts`
- 迁移 16 个 api 模块 → `.ts`，添加接口类型定义

**Phase 2 — Store + Utils + Composables（1 天）**
- 迁移 Pinia stores，定义 state/action 类型
- 迁移 utils，添加函数签名
- 迁移 composables

**Phase 3 — Vue 页面渐进迁移（1-2 天）**
- `.vue` 文件添加 `<script setup lang="ts">`
- 组件 props 类型定义
- 模板类型检查

### 5.2 M19：i18n 国际化（3 阶段，2-3 天）

**Phase 1 — 框架集成 + 中文提取（1 天）**
- 安装 vue-i18n，配置 plugin
- 提取所有中文字符串到 `zh-CN.json`

**Phase 2 — 英文语言包（1 天）**
- 创建 `en-US.json`
- 组件中使用 `$t()` 替换硬编码

**Phase 3 — 切换 + 持久化（0.5-1 天）**
- 语言切换组件
- localStorage 持久化
- URL 路由策略（可选）

---

## 6. 推荐实施优先级

| 优先级 | 任务 | 预计工期 | 依赖 |
|--------|------|---------|------|
| **P0** | 补齐 ThirdPartyControllerTest + CarbonNeutralProjectControllerTest | 1 天 | 无 |
| **P1** | 前端核心页面单元测试（Top 5: Login, CarbonUpload, TradingMarket, AuditList, SystemUsers） | 2-3 天 | 无 |
| **P1** | 补充 E2E specs（Admin, ThirdParty, CarbonNeutral 流程） | 1-2 天 | 无 |
| **P2** | 前端其余页面单元测试（17 个页面） | 3-5 天 | P1 完成 |
| **P3** | M4 TypeScript 迁移 | 3-5 天 | 无 |
| **P3** | M19 i18n 国际化 | 2-3 天 | 无 |

**总预估工期：12-18 天（可并行优化至 8-12 天）**

---

## 7. 验证方法

- **后端测试:** `mvn test` 全量通过
- **前端单元测试:** `npm run test:unit` 全量通过
- **前端 E2E:** `npm run test:e2e` 全量通过
- **构建验证:** `npm run build` + `mvn package` 无错误
- **覆盖率报告:** Jaest（后端）+ Vitest coverage（前端）≥ 80%

---

## 8. 验证轮次记录

本设计经过以下验证轮次：

1. **claude-context MCP 语义搜索** — 搜索 REST API endpoints、Vue pages、test files、security configuration
2. **Glob 精确枚举** — 枚举所有 controller、service、api、vue、test 文件
3. **Grep 交叉比对** — 提取所有 @Mapping 注解和 API 调用函数，前后端路径一一对照

三轮验证结果一致，无遗漏模块。
