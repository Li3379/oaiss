# Requirements: OAISS CHAIN Manual Testing

**Defined:** 2026-05-08
**Core Value:** 所有五种角色的核心业务流程在真实后端数据下端到端跑通，系统功能完整可用

## v1 Requirements

手工测试覆盖所有 5 种角色、16 个控制器、65+ 业务流程。每个需求对应一个可验证的测试场景。

### Phase 1: 环境搭建与认证基线

- [ ] **ENV-01**: Docker Compose 启动成功（MySQL, Redis, MinIO, 后端, 前端），所有健康检查通过
- [ ] **ENV-02**: Flyway 迁移执行成功（V1 schema + V2 seed data），21 张表创建完成
- [ ] **ENV-03**: 创建 V3__test_seed_data.sql 测试数据迁移（第三家企业用户、样本碳报告、拍卖订单、碳中和项目）
- [ ] **ENV-04**: 后端启动成功，Swagger UI 可访问 (`/api/v1/swagger-ui.html`)
- [ ] **ENV-05**: 前端启动成功，首页可访问 (`localhost:5173`)
- [ ] **ENV-06**: 6 个种子账户均可登录（admin, enterprise001, enterprise002, reviewer001, thirdparty001, authenticator001，密码均为 `admin123`）
- [ ] **ENV-07**: 每个角色登录后跳转到正确的角色首页（ROLE_HOME 映射验证）
- [ ] **ENV-08**: JWT 令牌获取成功，`Authorization: Bearer` 请求头有效
- [ ] **ENV-09**: 令牌刷新机制验证（POST /auth/refresh 返回新令牌对）
- [ ] **ENV-10**: 退出登录成功，令牌加入黑名单

### Phase 2: 碳排放报告生命周期

- [x] **CARB-01**: 企业用户创建碳排放报告（表单填写 + 文件上传到 MinIO）
- [x] **CARB-02**: 企业用户查看报告列表（分页、排序）
- [x] **CARB-03**: 企业用户查看报告详情
- [x] **CARB-04**: 企业用户提交报告（状态: DRAFT -> SUBMITTED）
- [ ] **CARB-05**: 审核员查看待审列表（状态筛选: UNDER_REVIEW）
- [ ] **CARB-06**: 审核员批准报告（状态: UNDER_REVIEW -> APPROVED）
- [ ] **CARB-07**: 审核员拒绝报告（状态: UNDER_REVIEW -> REJECTED，填写拒绝原因）
- [ ] **CARB-08**: 报告批准后自动触发信用评分更新
- [ ] **CARB-09**: 报告批准后自动触发放射评级计算
- [ ] **CARB-10**: 报告批准后自动触发区块链记录（mock 模式）
- [ ] **CARB-11**: 状态流转验证：非法状态转换被拒绝（如 DRAFT 直接到 APPROVED）
- [ ] **CARB-12**: 认证方查看认证列表、执行认证操作
- [ ] **CARB-13**: 跨角色访问控制验证：企业不能访问审核员接口

### Phase 3: 碳积分与交易引擎

- [ ] **COIN-01**: 企业用户查看碳积分余额
- [ ] **COIN-02**: 企业用户查看交易记录（分页）
- [ ] **COIN-03**: 企业用户向另一家企业转账碳积分
- [ ] **COIN-04**: 转账后双方余额正确更新（原子性验证）
- [ ] **COIN-05**: 余额不足时转账被拒绝
- [ ] **TRADE-01**: 企业用户挂买单（指定价格和数量）
- [ ] **TRADE-02**: 企业用户挂卖单（指定价格和数量）
- [ ] **TRADE-03**: 管理员触发撮合匹配
- [ ] **TRADE-04**: 撮合成功后交易记录创建
- [ ] **TRADE-05**: 撮合成功后买卖双方碳积分账户正确更新
- [ ] **TRADE-06**: 订单状态流转：PENDING -> FULLY_MATCHED / PARTIALLY_MATCHED / CANCELLED
- [ ] **TRADE-07**: P2P 交易发起（企业 A 向企业 B 发起交易请求）
- [ ] **TRADE-08**: P2P 交易接受（企业 B 接受交易）
- [ ] **TRADE-09**: P2P 交易拒绝（企业 B 拒绝交易）
- [ ] **TRADE-10**: P2P 交易结算验证（双方账户正确更新）
- [ ] **TRADE-11**: TradeController 与 DoubleAuctionController 的关系验证（确认是否共享撮合引擎）
- [ ] **TRADE-12**: 配额验证：配额不足时下单被拒绝
- [ ] **TRADE-13**: 交易操作必须顺序执行，不能并发（CON-01/02/03 已知问题）

### Phase 4: 碳中和项目与信用评分

- [ ] **PROJ-01**: 企业用户创建碳中和项目
- [ ] **PROJ-02**: 项目状态流转验证（12+ 状态转换）
- [ ] **PROJ-03**: 项目列表查看（分页、筛选）
- [ ] **PROJ-04**: 项目详情查看
- [ ] **PROJ-05**: VERIFIER/CERTIFIER 角色问题验证（@PreAuthorize 引用了不存在的角色）
- [ ] **CRED-01**: 企业用户查看信用评分
- [ ] **CRED-02**: 信用评分等级验证（WARNING(40), FROZEN(20)）
- [ ] **CRED-03**: 评分低于 WARNING 时交易受限验证
- [ ] **CRED-04**: 评分低于 FROZEN 时账户冻结验证
- [ ] **CRED-05**: 评分变动记录查看

### Phase 5: 支撑领域

- [ ] **SIGN-01**: RSA 密钥对生成
- [ ] **SIGN-02**: 数据签名操作
- [ ] **SIGN-03**: 签名验证操作
- [ ] **FILE-01**: 文件上传到 MinIO
- [ ] **FILE-02**: 文件下载验证
- [ ] **FILE-03**: MinIO 控制台验证（localhost:9001）
- [ ] **EMIT-01**: 放射数据查看
- [ ] **EMIT-02**: 放射评级查看
- [ ] **EMIT-03**: 排放因子验证（注意：硬编码在 CachePreloadService 中）
- [ ] **BLOCK-01**: 区块链记录查看（mock 模式）
- [ ] **BLOCK-02**: 区块链浏览器功能验证
- [ ] **BLOCK-03**: Mock 返回数据结构正确性验证
- [ ] **ADMIN-01**: 管理员查看用户列表
- [ ] **ADMIN-02**: 管理员创建用户
- [ ] **ADMIN-03**: 管理员编辑用户
- [ ] **ADMIN-04**: 管理员禁用/启用用户
- [ ] **ADMIN-05**: 系统配置查看和修改
- [ ] **TP-01**: 第三方监管查看监控数据
- [ ] **TP-02**: 第三方监管查看交易审计记录
- [ ] **SRCH-01**: 跨实体搜索功能验证

### Phase 6: 横切关注点与边界测试

- [ ] **AOP-01**: @AuditLog 验证：操作日志正确记录到 operation_log 表
- [ ] **AOP-02**: @RateLimit 验证：请求频率限制生效（注意：Redis 不可用时 fail-open）
- [ ] **AOP-03**: @DataIsolation 验证：企业数据隔离（enterprise001 不能看到 enterprise002 的数据）
- [ ] **AOP-04**: @DistributedLock 验证：并发操作锁机制
- [ ] **EDGE-01**: 跨角色访问控制（6 组角色互访测试）
- [ ] **EDGE-02**: 状态机违规（7 种非法状态转换）
- [ ] **EDGE-03**: 财务完整性（交易金额/数量一致性）
- [ ] **EDGE-04**: 分页边界（空列表、单条、满页、超页）
- [ ] **EDGE-05**: 输入验证（负数价格、零数量、超长文本）
- [ ] **EDGE-06**: i18n 验证（前端中英文切换，后端错误消息）
- [x] **BUG-01**: 修复测试中发现的所有 bug
- [x] **BUG-02**: 修复 SEC-03（Swagger 生产环境暴露）
- [x] **BUG-03**: 修复 SEC-04（CORS 默认 localhost 值）

## v1.1 Requirements

v1.1.0 需求对齐：补齐需求文档中定义但代码中缺失的功能模块。12 个 Gap 项，分 A/B/C 三级优先级。

### Priority A -- 核心缺失功能

- [ ] **REQ-01**: AI 市场智能预测 -- MarketPredictionService 实现市场趋势预测、碳价走势分析、供需预测
- [ ] **REQ-02**: AI 企业境况智能推断 -- EnterpriseInferenceService 实现企业碳排放趋势推断、合规风险评估
- [ ] **REQ-03**: AI 碳排放预测升级 -- CarbonPredictionService 从 Stub 升级为真实 ML 模型（回归预测）
- [ ] **REQ-04**: 前端 AI 模块页面 -- MarketPrediction.vue + EnterpriseInference.vue + 碳排放预测可视化

### Priority B -- 重要功能补齐

- [ ] **REQ-05**: Hyperledger Fabric 真实对接 -- BlockchainService 从 Mock 升级为 Fabric Gateway SDK
- [ ] **REQ-06**: 碳核算行业专用公式 -- 发电行业 25 参数公式 + 电网行业 9 参数公式实现
- [ ] **REQ-07**: 签发准入证书 -- EntryPermissionService + AdminController endpoint + 前端管理页面
- [ ] **REQ-08**: 签发审核员资格证 -- ReviewerQualificationService + AdminController endpoint + 前端管理页面

### Priority C -- 覆盖率补齐

- [ ] **REQ-09**: 前端 API 覆盖补齐 -- 39 个缺失后端 endpoint 的前端 API 调用模块
- [ ] **REQ-10**: 前端 Enterprise/Reviewer 视图功能补齐 -- 缺失的 CRUD 操作和详情页面
- [ ] **REQ-11**: 文档 API 总览修正 -- Swagger 文档与实际 endpoint 对齐
- [ ] **REQ-12**: 身份认证区块链方案 -- Fabric CA 集成 (optional, 可延期)

## v2 Requirements

延期到后续阶段，不阻塞当前测试。

### 性能优化

- **PERF-01**: N+1 查询修复（DoubleAuctionService.toMatchResponse）
- **PERF-02**: Redis KEYS 命令替换为 SCAN
- **PERF-03**: 缓存预加载异步化
- **PERF-04**: 订单查询分页化

### 并发重构

- **CON-01**: 替换 synchronized 为分布式锁
- **CON-02**: 添加 @Version 乐观锁
- **CON-03**: 修复 @Transactional + synchronized 时序问题
- **CON-04**: 所有可变实体添加 @Version

### 安全加固

- **SEC-01**: RSA 私钥加密存储
- **SEC-02**: CSRF 保护按需启用
- **SEC-05**: 登录防暴力破解滑动窗口
- **SEC-06**: JWT secret 空值校验

## Out of Scope

| Feature | Reason |
|---------|--------|
| 区块链智能合约开发 | 测试完成后单独进行 |
| 前端单元测试覆盖率提升 | 当前 24 个测试文件足够 |
| 后端测试覆盖率提升至 90% | 当前 43% 够用 |
| Redis 高可用 / 分布式部署 | 单节点开发环境足够 |
| 生产环境部署 | 当前阶段仅验证功能完整性 |

## Traceability

### v1.0 Traceability

| Requirement | Phase | Plan | Status |
|-------------|-------|------|--------|
| ENV-01 | Phase 1 | 01-01 | Pending |
| ENV-02 | Phase 1 | 01-01 | Pending |
| ENV-03 | Phase 1 | 01-01 | Pending |
| ENV-04 | Phase 1 | 01-01 | Pending |
| ENV-05 | Phase 1 | 01-01 | Pending |
| ENV-06 | Phase 1 | 01-02 | Pending |
| ENV-07 | Phase 1 | 01-02 | Pending |
| ENV-08 | Phase 1 | 01-02 | Pending |
| ENV-09 | Phase 1 | 01-02 | Pending |
| ENV-10 | Phase 1 | 01-02 | Pending |
| CARB-01 | Phase 2 | 02-01 | Complete |
| CARB-02 | Phase 2 | 02-01 | Complete |
| CARB-03 | Phase 2 | 02-01 | Complete |
| CARB-04 | Phase 2 | 02-01 | Complete |
| CARB-05 | Phase 2 | 02-02 | Pending |
| CARB-06 | Phase 2 | 02-02 | Pending |
| CARB-07 | Phase 2 | 02-02 | Pending |
| CARB-08 | Phase 2 | 02-03 | Pending |
| CARB-09 | Phase 2 | 02-03 | Pending |
| CARB-10 | Phase 2 | 02-03 | Pending |
| CARB-11 | Phase 2 | 02-02 | Pending |
| CARB-12 | Phase 2 | 02-02 | Pending |
| CARB-13 | Phase 2 | 02-03 | Pending |
| COIN-01 | Phase 3 | 03-01 | Pending |
| COIN-02 | Phase 3 | 03-01 | Pending |
| COIN-03 | Phase 3 | 03-01 | Pending |
| COIN-04 | Phase 3 | 03-01 | Pending |
| COIN-05 | Phase 3 | 03-01 | Pending |
| TRADE-01 | Phase 3 | 03-02 | Pending |
| TRADE-02 | Phase 3 | 03-02 | Pending |
| TRADE-03 | Phase 3 | 03-02 | Pending |
| TRADE-04 | Phase 3 | 03-02 | Pending |
| TRADE-05 | Phase 3 | 03-02 | Pending |
| TRADE-06 | Phase 3 | 03-02 | Pending |
| TRADE-07 | Phase 3 | 03-03 | Pending |
| TRADE-08 | Phase 3 | 03-03 | Pending |
| TRADE-09 | Phase 3 | 03-03 | Pending |
| TRADE-10 | Phase 3 | 03-03 | Pending |
| TRADE-11 | Phase 3 | 03-03 | Pending |
| TRADE-12 | Phase 3 | 03-02 | Pending |
| TRADE-13 | Phase 3 | 03-02 | Pending |
| PROJ-01 | Phase 4 | 04-01 | Pending |
| PROJ-02 | Phase 4 | 04-01 | Pending |
| PROJ-03 | Phase 4 | 04-01 | Pending |
| PROJ-04 | Phase 4 | 04-01 | Pending |
| PROJ-05 | Phase 4 | 04-01 | Pending |
| CRED-01 | Phase 4 | 04-02 | Pending |
| CRED-02 | Phase 4 | 04-02 | Pending |
| CRED-03 | Phase 4 | 04-02 | Pending |
| CRED-04 | Phase 4 | 04-02 | Pending |
| CRED-05 | Phase 4 | 04-02 | Pending |
| SIGN-01 | Phase 5 | 05-01 | Pending |
| SIGN-02 | Phase 5 | 05-01 | Pending |
| SIGN-03 | Phase 5 | 05-01 | Pending |
| FILE-01 | Phase 5 | 05-02 | Pending |
| FILE-02 | Phase 5 | 05-02 | Pending |
| FILE-03 | Phase 5 | 05-02 | Pending |
| EMIT-01 | Phase 5 | 05-03 | Pending |
| EMIT-02 | Phase 5 | 05-03 | Pending |
| EMIT-03 | Phase 5 | 05-03 | Pending |
| BLOCK-01 | Phase 5 | 05-04 | Pending |
| BLOCK-02 | Phase 5 | 05-04 | Pending |
| BLOCK-03 | Phase 5 | 05-04 | Pending |
| ADMIN-01 | Phase 5 | 05-05 | Pending |
| ADMIN-02 | Phase 5 | 05-05 | Pending |
| ADMIN-03 | Phase 5 | 05-05 | Pending |
| ADMIN-04 | Phase 5 | 05-05 | Pending |
| ADMIN-05 | Phase 5 | 05-05 | Pending |
| TP-01 | Phase 5 | 05-06 | Pending |
| TP-02 | Phase 5 | 05-06 | Pending |
| SRCH-01 | Phase 5 | 05-07 | Pending |
| BUG-01 | Phase 6 | 06-01 | Complete |
| BUG-02 | Phase 6 | 06-01 | Complete |
| BUG-03 | Phase 6 | 06-01 | Complete |
| AOP-01 | Phase 6 | 06-02 | Pending |
| AOP-02 | Phase 6 | 06-02 | Pending |
| AOP-03 | Phase 6 | 06-02 | Pending |
| AOP-04 | Phase 6 | 06-02 | Pending |
| EDGE-01 | Phase 6 | 06-03 | Pending |
| EDGE-02 | Phase 6 | 06-03 | Pending |
| EDGE-03 | Phase 6 | 06-03 | Pending |
| EDGE-04 | Phase 6 | 06-03 | Pending |
| EDGE-05 | Phase 6 | 06-03 | Pending |
| EDGE-06 | Phase 6 | 06-03 | Pending |

### v1.1 Traceability

| Requirement | Phase | Plan | Status |
|-------------|-------|------|--------|
| REQ-01 | Phase 7 | 07-02 | Pending |
| REQ-02 | Phase 7 | 07-03 | Pending |
| REQ-03 | Phase 7 | 07-04 | Pending |
| REQ-04 | Phase 8 | 08-01 | Pending |
| REQ-05 | Phase 9 | 09-02 | Pending |
| REQ-06 | Phase 8 | 08-02 | Pending |
| REQ-07 | Phase 10 | 10-01 | Pending |
| REQ-08 | Phase 10 | 10-02 | Pending |
| REQ-09 | Phase 11 | 11-01 | Pending |
| REQ-10 | Phase 11 | 11-02 | Pending |
| REQ-11 | Phase 11 | 11-04 | Pending |
| REQ-12 | Phase 9 | 09-03 | Pending (optional) |

**Coverage:**
- v1.0 requirements: 84 total, 84 mapped to phases, 0 unmapped
- v1.1 requirements: 12 total, 12 mapped to phases, 0 unmapped
- Total: 96 requirements, 96 mapped

---
*Requirements defined: 2026-05-08*
*Last updated: 2026-05-14 -- v1.1.0 traceability added (REQ-01~12 mapped to Phases 7-12)*
