# OAISS CHAIN - 角色页面API映射矩阵

## 概览

本文档记录了OAISS CHAIN系统前后端的完整映射关系，包括：
- 4个角色类型的26个认证页面 + 2个公开页面
- 每个页面的API调用及其对应的HTTP方法和端点
- API模块与后端Controller的对应关系
- 功能验证清单

---

## 角色分布总览

| 角色 | 中文名称 | 英文标识 | 页面数量 | 目录 | 首页路由 |
|------|---------|---------|---------|------|---------|
| ENTERPRISE | 企业 | `ENTERPRISE` | 16 | views/enterprise/ | /enterprise/carbon/upload |
| REVIEWER | 审核员 | `REVIEWER` | 3 | views/auditor/ | /auditor/audit/list |
| THIRD_PARTY | 第三方监管 | `THIRD_PARTY` | 1 | views/third-party/ | /third-party/monitor |
| ADMIN | 管理员 | `ADMIN` | 6 | views/admin/ | /admin/system/users |
| **PUBLIC** | 公开 | - | 2 | views/ | - |

---

## 资源统计

| 维度 | 数量 |
|------|------|
| 认证页面总数 | 26 |
| 公开页面总数 | 2 |
| 前端API模块 | 22 |
| 后端Controller | 20 |

---

## 一、ENTERPRISE 角色（企业）— 16个页面

### 1.1 CarbonUpload.vue - 碳报告上传

**路径**: `views/enterprise/CarbonUpload.vue`
**路由**: `/enterprise/carbon/upload`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyReports()` | api/carbon | CarbonController | GET | /api/v1/carbon/my-reports | 获取我的碳报告列表 |
| `createReport()` | api/carbon | CarbonController | POST | /api/v1/carbon/reports | 创建碳报告 |
| `deleteReport()` | api/carbon | CarbonController | DELETE | /api/v1/carbon/reports/{id} | 删除碳报告 |
| `submitReport()` | api/carbon | CarbonController | POST | /api/v1/carbon/reports/{id}/submit | 提交碳报告审核 |
| `getReport()` | api/carbon | CarbonController | GET | /api/v1/carbon/reports/{id} | 获取碳报告详情 |

**验证清单**:
- [ ] 碳报告列表正确加载并分页
- [ ] 创建报告表单验证（标题、核算期、排放数据必填）
- [ ] 提交报告状态流转（DRAFT -> PENDING）
- [ ] 删除报告（仅DRAFT状态可删除）
- [ ] 查看报告详情弹窗

---

### 1.2 OrdersManage.vue - 订单管理

**路径**: `views/enterprise/OrdersManage.vue`
**路由**: `/enterprise/orders/manage`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyTrades()` | api/trade | TradeController | GET | /api/v1/trade/my-trades | 获取我的交易列表 |
| `cancelTrade()` | api/trade | TradeController | POST | /api/v1/trade/{id}/cancel | 取消交易 |

**验证清单**:
- [ ] 交易列表分页加载（支持交易编号、类型、日期范围筛选）
- [ ] 交易详情弹窗展示（买卖双方、数量、单价、总额、区块链哈希）
- [ ] 取消交易功能（仅PENDING状态可取消）

---

### 1.3 TradingMarket.vue - 双向拍卖市场

**路径**: `views/enterprise/TradingMarket.vue`
**路由**: `/enterprise/trading/market`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getAuctionOrders()` | api/auction | DoubleAuctionController | GET | /api/v1/auction/orders | 获取所有拍卖委托 |
| `getMyOrders()` | api/auction | DoubleAuctionController | GET | /api/v1/auction/my-orders | 获取我的拍卖委托 |
| `getMatchResults()` | api/auction | DoubleAuctionController | GET | /api/v1/auction/results | 获取撮合结果 |
| `submitBuyOrder()` | api/auction | DoubleAuctionController | POST | /api/v1/auction/buy | 提交买入委托 |
| `submitSellOrder()` | api/auction | DoubleAuctionController | POST | /api/v1/auction/sell | 提交卖出委托 |

**验证清单**:
- [ ] 全部委托/我的委托/撮合结果三个Tab切换
- [ ] 提交买入/卖出委托表单验证
- [ ] 委托列表分页展示
- [ ] 撮合结果展示（买方ID、卖方ID、撮合数量、价格、时间）

---

### 1.4 TradingP2P.vue - P2P交易

**路径**: `views/enterprise/TradingP2P.vue`
**路由**: `/enterprise/trading/p2p`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyTrades()` | api/trade | TradeController | GET | /api/v1/trade/my-trades | 获取我的交易列表 |
| `createP2PTrade()` | api/trade | TradeController | POST | /api/v1/trade/p2p | 创建P2P交易 |
| `cancelTrade()` | api/trade | TradeController | POST | /api/v1/trade/{id}/cancel | 取消交易 |

**验证清单**:
- [ ] P2P交易列表分页展示（支持名称、身份、交易编号筛选）
- [ ] 创建P2P交易表单（数量、单价、备注）
- [ ] 取消交易（仅PENDING状态可取消）

---

### 1.5 CompanyDashboard.vue - 企业看板

**路径**: `views/enterprise/CompanyDashboard.vue`
**路由**: `/enterprise/company/dashboard`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyReports()` | api/carbon | CarbonController | GET | /api/v1/carbon/my-reports | 获取碳报告数据（图表） |
| `getMyTrades()` | api/trade | TradeController | GET | /api/v1/trade/my-trades | 获取交易数据（图表） |
| `getMyScore()` | api/credit | CreditScoreController | GET | /api/v1/credit/my-score | 获取信用评分 |
| `getMyEnterpriseAdmission()` | api/enterprise | EnterpriseController | GET | /api/v1/enterprise/admission/my | 获取准入证书状态 |

**验证清单**:
- [ ] 概览卡片（碳币总量、碳配额、信用评分）正确展示
- [ ] 6个ECharts图表渲染（交易柱状图、AI预测折线、AI建议柱状图、排放饼图、交易饼图、信用折线图）
- [ ] 资产筛选过滤功能（资产编号、类别、关键词）
- [ ] 时间维度切换（日/月/年）
- [ ] 准入证书状态显示

---

### 1.6 CreditScore.vue - 信用评分

**路径**: `views/enterprise/CreditScore.vue`
**路由**: `/enterprise/credit/score`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyScore()` | api/credit | CreditScoreController | GET | /api/v1/credit/my-score | 获取当前信用评分 |
| `getScoreHistory()` | api/credit | CreditScoreController | GET | /api/v1/credit/history | 获取信用变动历史 |

**验证清单**:
- [ ] 当前信用评分和等级（A-E）正确展示
- [ ] 信用变动历史分页展示（事件类型、变动分值、原因、时间）
- [ ] 事件类型标签样式正确

---

### 1.7 CarbonCoin.vue - 碳币账户

**路径**: `views/enterprise/CarbonCoin.vue`
**路由**: `/enterprise/carbon-coin/account`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyAccount()` | api/carbonCoin | CarbonCoinController | GET | /api/v1/carbon-coin/account | 获取碳币账户信息 |
| `getTransactions()` | api/carbonCoin | CarbonCoinController | GET | /api/v1/carbon-coin/transactions | 获取交易记录 |
| `transferCoins()` | api/carbonCoin | CarbonCoinController | POST | /api/v1/carbon-coin/transfer | 碳币转账 |

**验证清单**:
- [ ] 账户信息卡片（余额、冻结、累计充值、累计消费）展示
- [ ] 交易记录分页展示（流水号、类型、金额、余额、备注）
- [ ] 碳币转账弹窗（对方ID、金额、备注）及余额校验

---

### 1.8 Blockchain.vue - 区块链浏览器

**路径**: `views/enterprise/Blockchain.vue`
**路由**: `/enterprise/blockchain/browser`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getLatestBlocks()` | api/blockchain | BlockchainController | GET | /api/v1/blockchain/blocks/latest | 获取最新区块列表 |
| `getTransactions()` | api/blockchain | BlockchainController | GET | /api/v1/blockchain/transactions | 获取链上交易列表 |

**验证清单**:
- [ ] 区块/交易两个Tab切换
- [ ] 区块列表分页展示（高度、哈希、类型、交易数、矿工、时间戳）
- [ ] 交易列表分页展示（哈希、区块高度、发送方、接收方、金额、状态）
- [ ] 区块类型标签（GENESIS/REGULAR/REWARD）
- [ ] 交易状态标签（PENDING/CONFIRMED/FAILED）

---

### 1.9 CarbonNeutral.vue - 碳中和项目管理

**路径**: `views/enterprise/CarbonNeutral.vue`
**路由**: `/enterprise/carbon-neutral/projects`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyProjects()` | api/carbonNeutral | CarbonNeutralProjectController | GET | /api/v1/carbon-neutral/my | 获取我的碳中和项目 |
| `createProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral | 创建碳中和项目 |
| `submitProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/submit | 提交项目审核 |
| `startProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/start | 启动项目实施 |
| `applyCertification()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/apply-certification | 申请认证 |
| `terminateProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/terminate | 终止项目 |

**验证清单**:
- [ ] 项目列表分页展示（名称、类型、描述、预期减排量、状态、时间）
- [ ] 创建项目表单验证
- [ ] 项目状态流转（DRAFT->PENDING->APPROVED->IMPLEMENTING->COMPLETED/CERTIFIED）
- [ ] 操作按钮按状态条件显示

---

### 1.10 CarbonNeutralDetail.vue - 碳中和项目详情

**路径**: `views/enterprise/CarbonNeutralDetail.vue`
**路由**: `/enterprise/carbon-neutral/projects/:id`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getProject()` | api/carbonNeutral | CarbonNeutralProjectController | GET | /api/v1/carbon-neutral/{id} | 获取项目详情 |
| `submitProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/submit | 提交项目审核 |
| `startProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/start | 启动项目实施 |
| `submitVerification()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/submit-verification | 提交核证 |
| `updateMonitoring()` | api/carbonNeutral | CarbonNeutralProjectController | PUT | /api/v1/carbon-neutral/{id}/monitoring | 更新监测数据 |
| `applyCertification()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/apply-certification | 申请认证 |
| `terminateProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/terminate | 终止项目 |

**验证清单**:
- [ ] 项目详情描述列表正确展示
- [ ] 信息/监测两个Tab切换
- [ ] 操作按钮按状态条件显示（提交审核/启动/提交核证/申请认证/终止）
- [ ] 监测数据表单（排放数据、描述）

---

### 1.11 EmissionData.vue - 排放数据

**路径**: `views/enterprise/EmissionData.vue`
**路由**: `/enterprise/emission/data`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMyRating()` | api/emission | EmissionController | GET | /api/v1/emission/my-rating | 获取我的排放评级 |
| `getIndustryRankings()` | api/emission | EmissionController | GET | /api/v1/emission/rankings/{year} | 获取行业排名 |
| `predictEmission()` | api/emission | EmissionController | POST | /api/v1/emission/predict | AI排放预测 |

**验证清单**:
- [ ] 排放评级/行业排名/AI预测三个Tab切换
- [ ] 排放评级列表（等级A-D、总排放、行业均值、评分、评级时间）
- [ ] 行业排名列表（年份筛选、排名、企业名称、排放量、等级）
- [ ] AI预测功能（历史数据输入、预测月数、结果展示：趋势/预测值/置信度/建议）

---

### 1.12 UserProfile.vue - 个人中心

**路径**: `views/enterprise/UserProfile.vue`
**路由**: `/enterprise/user/profile`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getProfile()` | api/user | UserController | GET | /api/v1/user/profile | 获取用户资料 |
| `updateProfile()` | api/user | UserController | PUT | /api/v1/user/profile | 更新用户资料 |
| `changePassword()` | api/user | UserController | PUT | /api/v1/user/password | 修改密码 |
| `getMyEnterpriseAdmission()` | api/enterprise | EnterpriseController | GET | /api/v1/enterprise/admission/my | 获取准入证书状态 |

**验证清单**:
- [ ] 基本信息/修改密码两个Tab切换
- [ ] 用户资料表单（姓名、邮箱、电话、公司、地址）编辑保存
- [ ] 修改密码表单（当前密码、新密码、确认密码）验证
- [ ] 准入证书状态卡片

---

### 1.13 MarketPrediction.vue - 市场预测

**路径**: `views/enterprise/MarketPrediction.vue`
**路由**: `/enterprise/market-prediction`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getMarketTrend()` | api/marketPrediction | MarketPredictionController | POST | /api/v1/ai/market/trend | 市场趋势预测 |
| `getMarketPrice()` | api/marketPrediction | MarketPredictionController | POST | /api/v1/ai/market/price | 市场价格预测 |
| `getSupplyDemand()` | api/marketPrediction | MarketPredictionController | POST | /api/v1/ai/market/supply-demand | 供需关系预测 |

**验证清单**:
- [ ] 预测类型切换（趋势/价格/供需）
- [ ] 预测周期切换（7/30/90/180天）
- [ ] 统计卡片（趋势方向、模型版本、数据点数）
- [ ] ECharts折线图渲染（价格+置信区间）
- [ ] 类型/周期切换时自动刷新

---

### 1.14 EnterpriseInference.vue - 企业合规推理

**路径**: `views/enterprise/EnterpriseInference.vue`
**路由**: `/enterprise/enterprise-inference`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getEnterpriseInference()` | api/enterpriseInference | EnterpriseInferenceController | GET | /api/v1/predict/enterprise/{id}/inference | 获取企业合规推理结果 |

**验证清单**:
- [ ] 合规状态/置信度/异常评分/异常检测四个统计卡片
- [ ] 风险因素标签列表
- [ ] 模型版本显示
- [ ] 刷新按钮功能

---

### 1.15 CarbonFormulaCalculator.vue - 碳排放公式计算器

**路径**: `views/enterprise/CarbonFormulaCalculator.vue`
**路由**: `/enterprise/carbon-formula`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `calculatePowerGeneration()` | api/carbonFormula | CarbonController | POST | /api/v1/carbon/calculate/power-generation | 火力发电排放计算 |
| `calculatePowerGrid()` | api/carbonFormula | CarbonController | POST | /api/v1/carbon/calculate/power-grid | 电网排放计算 |

**验证清单**:
- [ ] 火力发电/电网两个Tab切换
- [ ] 火力发电表单（5种煤参数：消耗量/热值/含碳量/氧化率 + 脱硫参数）
- [ ] 电网表单（输送电量/线损率/排放因子/发电量/输入输出电量）
- [ ] 计算结果展示（总排放、分项排放、公式参考）
- [ ] 火力发电结果含燃料明细表格

---

### 1.16 EnterpriseInfo.vue - 企业信息

**路径**: `views/enterprise/EnterpriseInfo.vue`
**路由**: `/enterprise/info`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getEnterpriseInfo()` | api/enterprise | EnterpriseController | GET | /api/v1/enterprise/info | 获取企业基本信息 |
| `getQuotaInfo()` | api/enterprise | EnterpriseController | GET | /api/v1/enterprise/quota | 获取碳配额信息 |
| `updateContact()` | api/enterprise | EnterpriseController | PUT | /api/v1/enterprise/contact | 更新联系方式 |

**验证清单**:
- [ ] 企业信息描述列表（公司名、行业、联系人、电话、注册日期、地址）
- [ ] 碳配额信息（总额、已用、可交易、周期）
- [ ] 编辑联系人弹窗（联系人、联系电话）

---

## 二、REVIEWER 角色（审核员）— 3个页面

### 2.1 AuditList.vue - 审核列表

**路径**: `views/auditor/AuditList.vue`
**路由**: `/auditor/audit/list`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getPendingReports()` | api/reviewer | ReviewerController | GET | /api/v1/reviewer/reports/pending | 获取待审核报告 |
| `getReportList()` | api/carbon | CarbonController | GET | /api/v1/carbon/reports | 获取全部报告列表 |
| `reviewReport()` | api/carbon | CarbonController | POST | /api/v1/carbon/review | 审核报告（通过/驳回） |
| `getMyReviewerQualification()` | api/reviewer | ReviewerController | GET | /api/v1/reviewer/qualification/my | 获取审核员资格证状态 |
| `getStatistics()` | api/reviewer | ReviewerController | GET | /api/v1/reviewer/statistics | 获取审核统计 |
| `getReviewerInfo()` | api/reviewer | ReviewerController | GET | /api/v1/reviewer/info | 获取审核员信息 |

**验证清单**:
- [ ] 审核员资格证状态展示
- [ ] 审核统计卡片（总审核数、通过数、驳回数、通过率）
- [ ] 待审核/全部报告两个Tab切换
- [ ] 报告列表分页展示
- [ ] 审核弹窗（通过/驳回 + 审核意见）

---

### 2.2 ReviewHistory.vue - 审核历史

**路径**: `views/auditor/ReviewHistory.vue`
**路由**: `/auditor/review/history`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getReviewHistory()` | api/reviewer | ReviewerController | GET | /api/v1/reviewer/history | 获取审核历史记录 |

**验证清单**:
- [ ] 审核历史列表分页展示（报告编号、企业名称、标题、审核结果、审核意见、时间）
- [ ] 审核结果标签样式（通过=success、驳回=danger）

---

### 2.3 ProjectReview.vue - 碳中和项目审核

**路径**: `views/auditor/ProjectReview.vue`
**路由**: `/auditor/project/review`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getPendingVerification()` | api/carbonNeutral | CarbonNeutralProjectController | GET | /api/v1/carbon-neutral/pending-verification | 获取待审核/待核证项目 |
| `reviewProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/{id}/review | 审核项目（通过/驳回） |
| `verifyProject()` | api/carbonNeutral | CarbonNeutralProjectController | POST | /api/v1/carbon-neutral/verify | 核证项目 |
| `deductPoints()` | api/credit | CreditScoreController | POST | /api/v1/credit/deduct | 扣减信用分 |

**验证清单**:
- [ ] 待审核项目列表分页展示
- [ ] 审核弹窗（通过/驳回 + 意见）
- [ ] 核证弹窗（通过/不通过 + 意见）
- [ ] 信用扣减弹窗（事件类型：违规/欺诈/其他 + 描述）
- [ ] 操作按钮按状态条件显示

---

## 三、THIRD_PARTY 角色（第三方监管）— 1个页面

### 3.1 Monitor.vue - 监控页面

**路径**: `views/third-party/Monitor.vue`
**路由**: `/third-party/monitor`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getCarbonReports()` | api/thirdParty | ThirdPartyController | GET | /api/v1/third-party/carbon-reports | 获取碳报告数据 |
| `getStatistics()` | api/thirdParty | ThirdPartyController | GET | /api/v1/third-party/statistics | 获取统计数据 |

**验证清单**:
- [ ] 统计卡片（总报告数、待审核、已通过、已驳回、通过率）正确展示
- [ ] 碳报告列表分页展示（报告编号、企业名称、年份、总排放、状态、提交时间）
- [ ] 报告状态标签（草稿/待审核/已通过/已驳回/已上链）

---

## 四、ADMIN 角色（管理员）— 6个页面

### 4.1 SystemUsers.vue - 系统用户管理

**路径**: `views/admin/SystemUsers.vue`
**路由**: `/admin/system/users`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getUserList()` | api/admin | AdminController | GET | /api/v1/admin/users | 获取用户列表 |
| `updateUserStatus()` | api/admin | AdminController | PUT | /api/v1/admin/users/{id}/status | 启用/禁用用户 |

**验证清单**:
- [ ] 用户列表分页展示（用户名、邮箱、角色类型、状态、创建时间）
- [ ] 角色类型筛选（企业/审核员/第三方/管理员）
- [ ] 状态筛选（启用/禁用）
- [ ] 启用/禁用用户切换（带确认提示）

---

### 4.2 SystemCarbon.vue - 系统碳管理

**路径**: `views/admin/SystemCarbon.vue`
**路由**: `/admin/system/carbon`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getReportList()` | api/carbon | CarbonController | GET | /api/v1/carbon/reports | 获取所有碳报告 |

**验证清单**:
- [ ] 碳报告列表分页展示（编号、企业、标题、总排放、状态、审核员、时间）
- [ ] 关键词搜索功能

---

### 4.3 SystemConfig.vue - 系统配置

**路径**: `views/admin/SystemConfig.vue`
**路由**: `/admin/system/config`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| 无API调用 | - | - | - | - | 使用localStorage本地存储 |

**验证清单**:
- [ ] 配置项CRUD操作（描述、名称、服务器主机、环境变量、网络地址）
- [ ] 配置保存到localStorage
- [ ] 页面刷新后配置保持
- [ ] 前端分页和筛选功能

---

### 4.4 DataStatistics.vue - 数据统计

**路径**: `views/admin/DataStatistics.vue`
**路由**: `/admin/data/statistics`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getStatistics()` | api/admin | AdminController | GET | /api/v1/admin/statistics | 获取系统统计数据 |

**验证清单**:
- [ ] 统计卡片（总用户数、企业数、审核员数、第三方数）
- [ ] ECharts用户类型饼图渲染
- [ ] 数据实时加载

---

### 4.5 VerifyList.vue - 认证核验

**路径**: `views/admin/VerifyList.vue`
**路由**: `/admin/verify/list`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getReportList()` | api/carbon | CarbonController | GET | /api/v1/carbon/reports | 获取报告列表 |
| `reviewReport()` | api/carbon | CarbonController | POST | /api/v1/carbon/review | 审核报告（认证通过/驳回） |
| `getStatus()` | api/blockchain | BlockchainController | GET | /api/v1/blockchain/status | 获取区块链状态 |

**验证清单**:
- [ ] 统计卡片（待认证、已通过、已驳回、区块链状态）
- [ ] 报告列表（支持状态筛选和关键词搜索）
- [ ] 查看详情弹窗（排放明细：煤/油/气/电）
- [ ] 认证通过/驳回操作

---

### 4.6 CertificateManage.vue - 证书管理

**路径**: `views/admin/CertificateManage.vue`
**路由**: `/admin/certificates`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `getEnterpriseAdmissionList()` | api/admin | AdminController | GET | /api/v1/admin/enterprise-admission | 获取企业准入证书列表 |
| `issueEnterpriseAdmission()` | api/admin | AdminController | POST | /api/v1/admin/enterprise-admission/{id}/issue | 颁发企业准入证书 |
| `revokeEnterpriseAdmission()` | api/admin | AdminController | DELETE | /api/v1/admin/enterprise-admission/{id} | 吊销企业准入证书 |
| `getReviewerQualificationList()` | api/admin | AdminController | GET | /api/v1/admin/reviewer-qualification | 获取审核员资格证列表 |
| `issueReviewerQualification()` | api/admin | AdminController | POST | /api/v1/admin/reviewer-qualification/{id}/issue | 颁发审核员资格证 |
| `revokeReviewerQualification()` | api/admin | AdminController | DELETE | /api/v1/admin/reviewer-qualification/{id} | 吊销审核员资格证 |

**验证清单**:
- [ ] 准入证书/审核员资格证两个Tab切换
- [ ] 证书列表分页展示（ID、关联ID、证书编号、颁发日期、状态、创建时间）
- [ ] 颁发证书弹窗（输入企业ID或审核员ID）
- [ ] 吊销证书功能（带确认提示，仅ACTIVE状态可吊销）

---

## 五、公开页面 — 2个页面

### 5.1 OfficialHome.vue - 官方首页

**路径**: `views/OfficialHome.vue`
**路由**: `/official-home`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| 无API调用 | - | - | - | - | 纯静态展示页面 |

**验证清单**:
- [ ] 导航栏锚点跳转
- [ ] Hero区域展示
- [ ] 核心功能卡片（P2P交易/区块链/AI分析/数字签名）
- [ ] 系统介绍 + 指标展示（分布式率/A+等级/节点数/延迟）
- [ ] 用户角色卡片（企业/管理员/审核员/第三方）
- [ ] 生态系统图片展示
- [ ] 页脚链接

---

### 5.2 Login.vue - 登录页

**路径**: `views/Login.vue`
**路由**: `/login`

| API调用 | API模块 | 后端Controller | HTTP方法 | 端点 | 功能 |
|---------|---------|---------------|---------|------|------|
| `generateCaptcha()` | api/captcha | CaptchaController | GET | /api/v1/captcha/generate | 获取图形验证码 |
| `login()` | api/auth | AuthController | POST | /api/v1/auth/login | 用户登录 |

**验证清单**:
- [ ] 登录表单验证（账号/密码/验证码必填）
- [ ] 图形验证码展示和点击刷新
- [ ] 登录成功后跳转到角色首页
- [ ] 记住密码功能（localStorage）
- [ ] 登录失败后自动刷新验证码

---

## 后端Controller映射总览

| Controller | 路径前缀 | 主要功能 | 对应前端角色 |
|-----------|---------|---------|-------------|
| CarbonController | /api/v1/carbon | 碳报告CRUD、审核、碳排放计算 | ENTERPRISE, REVIEWER, ADMIN |
| TradeController | /api/v1/trade | P2P交易、交易管理 | ENTERPRISE |
| DoubleAuctionController | /api/v1/auction | 双向拍卖委托/撮合 | ENTERPRISE |
| CreditScoreController | /api/v1/credit | 信用评分、历史、扣减 | ENTERPRISE, REVIEWER |
| EnterpriseController | /api/v1/enterprise | 企业信息、配额、准入证书 | ENTERPRISE |
| CarbonCoinController | /api/v1/carbon-coin | 碳币账户、交易、转账 | ENTERPRISE |
| BlockchainController | /api/v1/blockchain | 区块链浏览、状态查询 | ENTERPRISE, ADMIN |
| CarbonNeutralProjectController | /api/v1/carbon-neutral | 碳中和项目全生命周期 | ENTERPRISE, REVIEWER |
| EmissionController | /api/v1/emission | 排放评级、行业排名、AI预测 | ENTERPRISE |
| UserController | /api/v1/user | 用户资料、密码修改 | ENTERPRISE |
| MarketPredictionController | /api/v1/ai/market | 市场趋势/价格/供需预测 | ENTERPRISE |
| EnterpriseInferenceController | /api/v1/predict/enterprise | 企业合规推理 | ENTERPRISE |
| ReviewerController | /api/v1/reviewer | 审核员信息、待审核报告、历史 | REVIEWER |
| ThirdPartyController | /api/v1/third-party | 第三方碳报告、统计 | THIRD_PARTY |
| AdminController | /api/v1/admin | 用户管理、统计、证书管理 | ADMIN |
| AuthController | /api/v1/auth | 登录、注册、登出 | PUBLIC |
| CaptchaController | /api/v1/captcha | 图形验证码生成/验证 | PUBLIC |

---

## 前端API模块文件索引

| API模块 | 文件路径 | 对应后端Controller |
|---------|---------|-------------------|
| api/carbon | src/api/carbon.ts | CarbonController |
| api/trade | src/api/trade.ts | TradeController |
| api/auction | src/api/auction.ts | DoubleAuctionController |
| api/credit | src/api/credit.ts | CreditScoreController |
| api/enterprise | src/api/enterprise.ts | EnterpriseController |
| api/carbonCoin | src/api/carbonCoin.ts | CarbonCoinController |
| api/blockchain | src/api/blockchain.ts | BlockchainController |
| api/carbonNeutral | src/api/carbonNeutral.ts | CarbonNeutralProjectController |
| api/emission | src/api/emission.ts | EmissionController |
| api/user | src/api/user.ts | UserController |
| api/marketPrediction | src/api/marketPrediction.ts | MarketPredictionController |
| api/enterpriseInference | src/api/enterpriseInference.ts | EnterpriseInferenceController |
| api/carbonFormula | src/api/carbonFormula.ts | CarbonController |
| api/reviewer | src/api/reviewer.ts | ReviewerController |
| api/thirdParty | src/api/thirdParty.ts | ThirdPartyController |
| api/admin | src/api/admin.ts | AdminController |
| api/auth | src/api/auth.ts | AuthController |
| api/captcha | src/api/captcha.ts | CaptchaController |
| api/request | src/api/request.ts | (Axios拦截器配置) |
| api/search | src/api/search.ts | SearchController |
| api/file | src/api/file.ts | FileController |
| api/signature | src/api/signature.ts | DigitalSignatureController |

---

## 验证执行计划

### Phase 1: ENTERPRISE角色验证 (16个页面)
优先级: 高 | 预计时间: 3-4天

1. 按页面顺序逐一验证功能
2. 记录发现的每个问题
3. 生成问题清单并分类

### Phase 2: ADMIN角色验证 (6个页面)
优先级: 高 | 预计时间: 1-2天

### Phase 3: REVIEWER角色验证 (3个页面)
优先级: 中 | 预计时间: 1天

### Phase 4: THIRD_PARTY角色验证 (1个页面)
优先级: 中 | 预计时间: 0.5天

### Phase 5: 公开页面验证 (2个页面)
优先级: 低 | 预计时间: 0.5天

---

## 问题跟踪模板

### 问题记录格式
```markdown
### 问题 #[编号]: [简短描述]
- **页面**: [页面名称]
- **角色**: [角色类型]
- **严重程度**: [高/中/低]
- **问题描述**: [详细描述]
- **预期行为**: [应该如何]
- **实际行为**: [当前如何]
- **复现步骤**:
  1. [步骤1]
  2. [步骤2]
- **相关API**: [API名称]
- **建议修复方案**: [修复建议]
- **状态**: [待修复/修复中/已修复]
```

---

## 附录

### A. 测试账号准备
需要准备不同角色的测试账号进行功能验证：
- [ ] ENTERPRISE角色账号
- [ ] REVIEWER角色账号
- [ ] THIRD_PARTY角色账号
- [ ] ADMIN角色账号

### B. 后端Controller文件位置
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/`

---

**文档版本**: v2.0
**创建日期**: 2025-05-10
**最后更新**: 2026-05-19
**维护者**: AI Assistant
