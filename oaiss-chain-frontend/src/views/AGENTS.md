<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# views/ — Page-Level Vue Components

Page-level Vue components organized by user role.

## Key Files (3 public pages)

- `OfficialHome.vue` — Public landing page (官网首页)
- `Login.vue` — Login page (登录页)
- `NotFound.vue` — 404 page (404页)

## Subdirectories

- `enterprise/` — Enterprise user pages, 16 pages (see [enterprise/AGENTS.md](enterprise/AGENTS.md))
- `auditor/` — Reviewer/auditor pages, 3 pages (see [auditor/AGENTS.md](auditor/AGENTS.md))
- `third-party/` — Third-party monitor pages, 1 page (see [third-party/AGENTS.md](third-party/AGENTS.md))
- `admin/` — Admin pages, 6 pages (see [admin/AGENTS.md](admin/AGENTS.md))

## Enterprise Pages (16)

| # | File | Chinese | Description |
|---|------|---------|-------------|
| 1 | `enterprise/CarbonUpload.vue` | 碳报告上传 | Carbon report upload — enterprise home route |
| 2 | `enterprise/OrdersManage.vue` | 订单管理 | Trade order management |
| 3 | `enterprise/TradingMarket.vue` | 交易市场 | Double auction market |
| 4 | `enterprise/TradingP2P.vue` | P2P交易 | P2P trading |
| 5 | `enterprise/CompanyDashboard.vue` | 企业仪表盘 | Enterprise dashboard overview |
| 6 | `enterprise/CreditScore.vue` | 信用评分 | Credit score display |
| 7 | `enterprise/CarbonCoin.vue` | 碳币账户 | Carbon coin account management |
| 8 | `enterprise/Blockchain.vue` | 区块链浏览器 | Blockchain transaction viewer |
| 9 | `enterprise/CarbonNeutral.vue` | 碳中和项目列表 | Carbon neutral project list |
| 10 | `enterprise/CarbonNeutralDetail.vue` | 碳中和项目详情 | Project detail view |
| 11 | `enterprise/EmissionData.vue` | 排放数据 | Emission data management |
| 12 | `enterprise/UserProfile.vue` | 用户资料 | Enterprise user profile |
| 13 | `enterprise/MarketPrediction.vue` | AI市场预测 | AI market prediction (ML) |
| 14 | `enterprise/EnterpriseInference.vue` | AI企业推断 | AI enterprise inference (ML) |
| 15 | `enterprise/CarbonFormulaCalculator.vue` | 碳核算公式计算器 | Carbon accounting formula calculator |
| 16 | `enterprise/EnterpriseInfo.vue` | 企业信息 | Enterprise information |

## Auditor Pages (3)

| # | File | Chinese | Description |
|---|------|---------|-------------|
| 1 | `auditor/AuditList.vue` | 审核列表 | Report audit list — auditor home route |
| 2 | `auditor/ReviewHistory.vue` | 审核历史 | Review history |
| 3 | `auditor/ProjectReview.vue` | 项目审核 | Carbon neutral project review |

## Third-Party Pages (1)

| # | File | Chinese | Description |
|---|------|---------|-------------|
| 1 | `third-party/Monitor.vue` | 监控面板 | Monitoring dashboard — third-party home route |

## Admin Pages (6)

| # | File | Chinese | Description |
|---|------|---------|-------------|
| 1 | `admin/SystemUsers.vue` | 用户管理 | User management (CRUD, role assignment) — admin home route |
| 2 | `admin/SystemCarbon.vue` | 碳系统管理 | Carbon data administration |
| 3 | `admin/SystemConfig.vue` | 系统配置 | System configuration |
| 4 | `admin/DataStatistics.vue` | 数据统计 | Data statistics dashboard |
| 5 | `admin/VerifyList.vue` | 验证列表 | Verification list |
| 6 | `admin/CertificateManage.vue` | 证书管理 | Certificate management |

## For AI Agents

- Each role's home route is noted above. Route definitions live in `router/index.ts` with `meta.roles` guards.
- Views call their corresponding API module in `api/` — e.g., `CarbonUpload.vue` calls `api/carbon.ts`.
- Enterprise is the largest view directory (16 pages) covering the primary user role.
- The `enterprise/` subdirectory includes ML-powered pages (`MarketPrediction.vue`, `EnterpriseInference.vue`) that call `/api/v1/prediction` and `/api/v1/inference`.
- When adding a new page: create the `.vue` file in the correct role directory, add the route in `router/index.ts`, add the menu entry in `config/menu.ts`, and create the API module in `api/` if needed.
