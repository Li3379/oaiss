# OAISS CHAIN 完整验收探索流程

> 从前端页面交互入手，逐步深入后端代码，重点覆盖项目特色功能。
> 执行日期：2026-05-27 | 服务状态：前端 :5173 ✅ 后端 :8080 ✅

---

## 总览：8 大验收模块

| # | 模块 | 角色 | 前端页面 | 后端入口 | 特色功能 |
|---|------|------|----------|----------|----------|
| M1 | 登录与角色路由 | 全部 | Login.vue → 4 个角色首页 | AuthController | JWT + 角色守卫 |
| M2 | 碳报告生命周期 | 企业→审核员 | CarbonUpload → AuditList | CarbonController → ReviewerController | 状态流转 DRAFT→ON_CHAIN |
| M3 | 碳交易系统 | 企业 | TradingP2P / TradingMarket / OrdersManage | TradeController | P2P交易 + 双向拍卖 + @DistributedLock |
| M4 | 信用评分 | 企业 | CreditScore | CreditScoreController | A-E 五级 + 交易限制/冻结 |
| M5 | 数字签名 | 企业 | UserProfile | DigitalSignatureController | RSA 密钥对 + AES-GCM 加密 |
| M6 | 碳中和项目 | 企业→审核员 | CarbonNeutral → ProjectReview | CarbonNeutralProjectController | 三重状态(项目/认证/核证) |
| M7 | AI 智能预测 | 企业 | EnterpriseInference / MarketPrediction / CarbonFormulaCalculator | ML 服务 (FastAPI) | IsolationForest + XGBoost + Prophet |
| M8 | 区块链存证 | 企业 | Blockchain | BlockchainController | Fabric Chaincode (Go) |

---

## M1: 登录与角色路由验证

### 1.1 浏览器操作
```
步骤：
1. 打开 http://localhost:5173
2. 验证登录页面加载（Login.vue）
3. 用 4 个角色账号分别登录：
   - 企业 (ENTERPRISE) → 跳转 /enterprise/carbon/upload
   - 审核员 (REVIEWER) → 跳转 /auditor/audit/list
   - 第三方 (THIRD_PARTY) → 跳转 /third-party/monitor
   - 管理员 (ADMIN) → 跳转 /admin/system/users
4. 验证左侧菜单栏按角色过滤
5. 验证无权访问其他角色路由时的 403/重定向行为
```

### 1.2 后端代码验证
- `SecurityConfig.java`: 角色权限配置
- `JwtAuthenticationFilter.java`: Token 验证 + 白名单
- `JwtTokenProvider.java`: Token 生成/解析
- `UserController.java`: 登录接口

### 1.3 验收标准
- [ ] 4 个角色均能成功登录
- [ ] 登录后跳转到正确的角色首页
- [ ] 菜单栏按角色正确过滤
- [ ] 跨角色访问被拒绝
- [ ] Token 过期后自动刷新

---

## M2: 碳报告生命周期（核心流程）

### 2.1 浏览器操作 — 企业端
```
步骤：
1. 以企业身份登录
2. 进入 "碳报告上传" 页面 (CarbonUpload.vue)
3. 填写碳排放数据：
   - 核算周期、报告标题、报告类型（季度/年度）
   - 排放数据 JSON（Scope 1/2/3）
   - 计算方法
4. 保存为草稿（状态 → DRAFT）
5. 提交报告（状态 → SUBMITTED）
6. 查看报告列表，验证状态显示
```

### 2.2 浏览器操作 — 审核员端
```
步骤：
1. 以审核员身份登录
2. 进入 "审核列表" 页面 (AuditList.vue)
3. 查看待审核报告列表
4. 点击报告查看详情
5. 执行审核操作：
   - 审核通过（状态 → APPROVED）
   - 或审核拒绝（状态 → REJECTED）
6. 验证审核历史记录
```

### 2.3 后端代码验证
- `CarbonController.java`: 报告 CRUD
- `ReviewerController.java`: 审核接口
- `CarbonService.java`: 状态流转逻辑
- `ReportStatusEnum.java`: 状态枚举 + 可编辑/可提交/可审核判断
- `CarbonReport.java`: 实体字段

### 2.4 状态流转验证
```
DRAFT(0) → SUBMITTED(1) → UNDER_REVIEW(2) → APPROVED(3) → ON_CHAIN(5)
                                            → REJECTED(4) → 可重新编辑提交
```

### 2.5 验收标准
- [ ] 企业能创建碳报告并保存草稿
- [ ] 草稿报告可编辑，已提交报告不可编辑
- [ ] 审核员能看到待审核列表
- [ ] 审核通过/拒绝后状态正确流转
- [ ] 被拒绝的报告可重新编辑提交
- [ ] 审核历史记录完整

---

## M3: 碳交易系统（特色功能）

### 3.1 浏览器操作 — P2P 交易
```
步骤：
1. 以企业身份登录
2. 进入 "P2P 交易" 页面 (TradingP2P.vue)
3. 创建卖单：
   - 选择交易类型 P2P
   - 填写碳配额数量、单价
   - 指定买方企业
4. 验证交易创建成功
5. 进入 "订单管理" 页面 (OrdersManage.vue)
6. 确认/取消交易
```

### 3.2 浏览器操作 — 双向拍卖
```
步骤：
1. 进入 "交易市场" 页面 (TradingMarket.vue)
2. 查看拍卖订单簿
3. 创建拍卖订单（买单/卖单）
4. 验证撮合逻辑
```

### 3.3 后端代码验证
- `TradeController.java`: P2P + 拍卖接口
- `TradeService.java`:
  - `createP2PTrade()`: @DistributedLock 防并发
  - `createAuctionOrder()`: 拍卖撮合
  - `confirmTrade()`: 交易确认 + 配额扣减
  - `cancelTrade()`: 交易取消
- `TradeTypeEnum.java`: 交易类型枚举
- `Enterprise.java`: carbonQuota / carbonTradable 字段

### 3.4 关键验证点
- **分布式锁**: `@DistributedLock(key = "'trade:seller:' + #currentUser.userId")`
- **配额校验**: 卖方余额 ≥ 交易数量
- **自交易防护**: sellerId ≠ buyerId
- **越权防护**: 当前用户必须是卖方

### 3.5 验收标准
- [ ] P2P 交易创建成功
- [ ] 拍卖订单创建成功
- [ ] 交易确认后双方配额正确变动
- [ ] 余额不足时交易被拒绝
- [ ] 自交易被拒绝
- [ ] 并发交易不超卖（分布式锁生效）

---

## M4: 信用评分系统（特色功能）

### 4.1 浏览器操作
```
步骤：
1. 以企业身份登录
2. 进入 "信用评分" 页面 (CreditScore.vue)
3. 查看当前信用分和等级
4. 验证等级对应关系：
   - EXCELLENT: 80-100 (优秀)
   - GOOD: 60-79 (良好)
   - WARNING: 40-59 (警告)
   - DANGER: 20-39 (危险)
   - FROZEN: 0-19 (冻结)
5. 验证低分时的交易限制提示
```

### 4.2 后端代码验证
- `CreditScoreController.java`: 评分查询接口
- `CreditScoreService.java`: 评分计算 + `checkThresholds()`
- `CreditLevelEnum.java`: 等级划分
- `CreditScore.java`: 实体（score, level, tradeRestricted, accountFrozen）

### 4.3 验收标准
- [ ] 信用分正确显示
- [ ] 等级与分数对应正确
- [ ] WARNING 等级有警告提示
- [ ] DANGER 等级限制交易
- [ ] FROZEN 等级冻结账户

---

## M5: 数字签名（特色功能）

### 5.1 浏览器操作
```
步骤：
1. 以企业身份登录
2. 进入 "个人中心" → "数字签名" (UserProfile.vue)
3. 生成 RSA 密钥对
4. 验证公钥显示（私钥不暴露）
5. 对碳报告执行签名操作
6. 验证签名结果
```

### 5.2 后端代码验证
- `DigitalSignatureController.java`: generateKeyPair / sign / verify
- `DigitalSignatureService.java`: RSA 签名/验签逻辑
- `RsaKeyPair.java`: 密钥实体（publicKey, privateKey, keyStatus, encrypted）
- `RsaKeyMigrationRunner.java`: 私钥 AES-256-GCM 加密迁移
- `RsaKeyPairRepository.java`: 密钥查询

### 5.3 关键验证点
- **私钥安全**: API 响应不暴露 privateKey
- **密钥加密**: encrypted=true 时私钥已 AES-GCM 加密
- **密钥版本**: keyVersion 支持密钥轮换
- **密钥状态**: 有效(1)/失效(0)/过期(2)

### 5.4 验收标准
- [ ] 密钥对生成成功
- [ ] 公钥可查看，私钥不暴露
- [ ] 签名操作成功
- [ ] 验签操作正确
- [ ] 密钥状态管理正确

---

## M6: 碳中和项目（特色功能）

### 6.1 浏览器操作 — 企业端
```
步骤：
1. 以企业身份登录
2. 进入 "碳中和项目" 页面 (CarbonNeutral.vue)
3. 创建新项目：
   - 选择类型（碳汇/CCUS/可再生能源/节能改造/其他）
   - 填写预计减排量、投资金额、方法学
4. 提交审核（状态 DRAFT → PENDING）
5. 查看项目详情 (CarbonNeutralDetail.vue)
```

### 6.2 浏览器操作 — 审核员端
```
步骤：
1. 以审核员身份登录
2. 进入 "项目审核" 页面 (ProjectReview.vue)
3. 审核碳中和项目
4. 验证认证状态和核证状态
```

### 6.3 后端代码验证
- `CarbonNeutralProjectController.java`: 项目 CRUD + 审核
- `CarbonNeutralProjectService.java`:
  - 项目状态: DRAFT(0)→PENDING(1)→APPROVED(2)→IMPLEMENTING(3)→COMPLETED(4)→TERMINATED(5)/REJECTED(6)
  - 认证状态: NONE(0)→PENDING(1)→CERTIFIED(2)/FAILED(3)
  - 核证状态: NONE(0)→PENDING(1)→VERIFIED(2)/FAILED(3)
- `CarbonNeutralProject.java`: 实体

### 6.4 验收标准
- [ ] 项目创建成功
- [ ] 项目状态流转正确
- [ ] 认证状态独立管理
- [ ] 核证状态独立管理
- [ ] 项目列表分页正确

---

## M7: AI 智能预测（核心特色功能）

### 7.1 浏览器操作 — 企业合规推理
```
步骤：
1. 以企业身份登录
2. 进入 "企业推理" 页面 (EnterpriseInference.vue)
3. 触发合规推理请求
4. 查看推理结果：
   - 合规状态 (compliant/at_risk/non_compliant)
   - 置信度
   - 异常分数
   - 风险因素
```

### 7.2 浏览器操作 — 市场预测
```
步骤：
1. 进入 "市场预测" 页面 (MarketPrediction.vue)
2. 设置预测天数
3. 查看预测结果：
   - 价格趋势
   - 置信区间
   - 趋势方向
```

### 7.3 浏览器操作 — 碳排放公式计算器
```
步骤：
1. 进入 "碳排放公式" 页面 (CarbonFormulaCalculator.vue)
2. 使用 GB/T 32150-2015 计算器
3. 输入排放源数据
4. 验证计算结果
```

### 7.4 后端代码验证
- `CarbonController.java`: 预测接口
- `CarbonPredictionService.java`: Prophet 预测
- `MarketPredictionService.java`: 市场趋势预测
- `EnterpriseInferenceService.java`: IsolationForest + XGBoost 推理
- `MlServiceClient.java`: ML 服务调用（含熔断）
- ML 服务: `oaiss-chain-ml-service/app/routers/`

### 7.5 关键验证点
- **数据不足处理**: 历史数据 < 2 期时返回低置信度
- **特征聚合**: 自动从数据库提取企业特征
- **熔断机制**: ML 服务不可用时降级处理

### 7.6 验收标准
- [ ] 企业合规推理返回正确结果
- [ ] 市场预测返回趋势数据
- [ ] 碳排放计算器功能正确
- [ ] 数据不足时有友好提示
- [ ] ML 服务不可用时有降级处理

---

## M8: 区块链存证（特色功能）

### 8.1 浏览器操作
```
步骤：
1. 以企业身份登录
2. 进入 "区块链" 页面 (Blockchain.vue)
3. 查看已上链的碳报告
4. 验证上链状态和交易哈希
```

### 8.2 后端代码验证
- `BlockchainController.java`: 上链接口
- `FabricBlockchainService.java`: Fabric Gateway SDK 集成（@Profile("fabric")）
- `FabricGatewayConfig.java`: Gateway/Network/Contract Bean
- Chaincode: `oaiss-chain-chaincode/chaincode.go` — CarbonReport + TradeRecord

### 8.3 验收标准
- [ ] 区块链页面加载正常
- [ ] 已上链报告显示交易哈希
- [ ] Fabric 功能可通过 profile 切换启用/禁用

---

## 执行策略

### 第一阶段：浏览器自动化探索（使用 Playwright MCP）
1. 按 M1→M8 顺序逐模块操作浏览器
2. 截图记录每个关键页面
3. 验证页面元素和交互行为

### 第二阶段：后端代码深度审查（使用 CodeGraph + 子智能体）
1. 对每个模块的关键 Service 进行代码走读
2. 验证业务逻辑与页面行为的一致性
3. 检查安全边界和异常处理

### 第三阶段：集成验证
1. 端到端流程测试（企业提交→审核员审核→上链）
2. 跨模块数据一致性验证
3. 异常场景覆盖

---

## 测试账号

| 角色 | 用户名 | 密码 | 路由前缀 |
|------|--------|------|----------|
| 企业 | (待确认) | (待确认) | /enterprise/* |
| 审核员 | (待确认) | (待确认) | /auditor/* |
| 第三方 | (待确认) | (待确认) | /third-party/* |
| 管理员 | (待确认) | (待确认) | /admin/* |

> 需要确认测试账号后才能开始浏览器自动化验收。
