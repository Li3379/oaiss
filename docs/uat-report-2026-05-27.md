# OAISS CHAIN 项目验收测试报告 (UAT)

**日期**: 2026-05-27
**审查范围**: 全栈（前端 Vue 3 + 后端 Spring Boot + AOP 切面 + 安全基础设施）
**审查方法**: CodeGraph 结构分析 + 逐文件深度代码审查 + 前后端 API 一致性比对

---

## 执行摘要

| 指标 | 数值 |
|------|------|
| CRITICAL | 3 |
| HIGH | 19 |
| MEDIUM | 30 |
| LOW | 8 |
| **总计** | **60** |

**验收结论**: **BLOCK** — 存在 3 个 CRITICAL 和 19 个 HIGH 级别问题，需要修复后重新验收。

---

## CRITICAL 级别 (3)

### C-01: SpEL 注入导致远程代码执行 (RCE)
- **文件**: `oaiss-chain-backend/.../aop/DistributedLockAspect.java:91-107`
- **描述**: `@DistributedLock` 的 `key` 属性接受 SpEL 表达式，使用 `StandardEvaluationContext` 求值。攻击者可通过构造 SpEL 表达式实现 RCE。
- **修复**: 使用 `SimpleEvaluationContext.forReadOnlyDataBinding().build()` 替代 `StandardEvaluationContext`。

### C-02: 分布式锁释放存在 TOCTOU 竞态条件
- **文件**: `oaiss-chain-backend/.../aop/RedisLockService.java:113-132`
- **描述**: `releaseLock` 先 GET 锁值再 DELETE，中间锁可能已过期并被其他线程获取，导致误删其他线程的锁。
- **修复**: 使用 Lua 脚本原子执行 check-and-delete。

### C-03: 分布式锁时间单位不一致导致锁立即过期
- **文件**: `oaiss-chain-backend/.../aop/DistributedLockAspect.java:58-67`
- **描述**: `expireTime` 注解标注为"秒"，但当 `waitTime > 0` 时传入 `TimeUnit.MILLISECONDS`，导致 `expireTime` 被当作毫秒处理，锁几乎立即过期，分布式锁完全失效。
- **修复**: 统一时间单位，`expireTime` 始终以秒为单位传入。

---

## HIGH 级别 (19)

### 前端 (7)

#### H-01: VerifyList.vue 编码损坏导致区块链状态判断失败
- **文件**: `oaiss-chain-frontend/.../admin/VerifyList.vue:139`
- **描述**: `'姝ｅ父'` 是 `'正常'` 的编码损坏字符串，后端返回正确中文时匹配永远不命中。
- **修复**: 将 `'姝ｅ父'` 替换为 `'正常'`，确认文件编码为 UTF-8。

#### H-02: UserProfile.vue 绕过 Axios 拦截器直接使用 fetch()
- **文件**: `oaiss-chain-frontend/.../enterprise/UserProfile.vue:206-227`
- **描述**: `fetchSignatureKeyPair()` 使用原生 `fetch()` 绕过了 JWT 刷新、统一错误处理、基础 URL 配置。
- **修复**: 改用项目 Axios 客户端。

#### H-03: 分页参数命名不一致 (VerifyList.vue / SystemUsers.vue)
- **文件**: `VerifyList.vue:196`, `SystemUsers.vue:64`
- **描述**: 直接使用 `page`/`size` 绕过了 `request.ts` 拦截器的 `pageNum`/`pageSize` 标准化。
- **修复**: 改为 `pageNum`/`pageSize`。

#### H-04: 客户端过滤 + 服务端分页逻辑冲突
- **文件**: `SystemCarbon.vue:14-27`, `SystemUsers.vue:29-48`
- **描述**: 搜索通过 `computed` 在客户端执行，但分页器绑定的是客户端过滤后的数量，翻页时触发服务端分页请求返回未过滤数据。
- **修复**: 将搜索关键字传递给后端 API 进行服务端过滤。

#### H-05: 大量硬编码中文绕过 i18n
- **文件**: `EmissionData.vue:148-208`, `AuditList.vue:52-203`
- **描述**: 多处使用硬编码中文而非 `t()`，在 en-US locale 下仍显示中文。
- **修复**: 提取到 i18n 翻译文件。

#### H-06: 广泛使用 `any` 类型
- **文件**: `Monitor.vue`, `AuditList.vue`, `VerifyList.vue`, `ReviewHistory.vue`, `UserProfile.vue` 等
- **描述**: 10+ 处使用 `any` 类型，丧失 TypeScript 类型安全。
- **修复**: 定义具体接口类型，使用 `unknown` 替代 `any`。

#### H-07: 前端 API 返回类型不匹配
- **文件**: `auction.ts:20`, `search.ts:4-9`, `admin.ts` 全文, `trade.ts:15,35`
- **描述**: `getMatchResults` 返回 `Promise<MatchingResultResponse[]>` 但后端返回 `Page<>`；`searchReports`/`searchTrades` 返回数组但后端返回分页；`admin.ts` 所有函数返回 `Promise<unknown>`。
- **修复**: 修正返回类型定义。

### 后端服务 (6)

#### H-08: 多个服务存在 N+1 查询问题
- **文件**: `CreditScoreService.java:381-424`, `CarbonNeutralProjectService.java:490-548`, `ReviewerService.java:127-135`, `CarbonService.java:296-343`
- **描述**: `toResponse` 方法在分页映射中对每条记录查询关联实体，产生 N+1 查询。最严重的是 `CarbonNeutralProjectService`（每条 3 次查询）。
- **修复**: 批量加载关联数据，使用 `Map<Long, String>` 缓存。

#### H-09: UserService.getUserById 缺乏权限控制
- **文件**: `UserService.java:48-55`
- **描述**: 任何已认证用户可通过 ID 枚举获取其他用户的 PII（手机号、邮箱、最后登录 IP）。
- **修复**: 增加权限校验，仅管理员或用户本人可查看。

#### H-10: CarbonCoinService.transfer 未锁定双方账户
- **文件**: `CarbonCoinService.java:141-182`
- **描述**: 转账只锁定转出方，未锁定转入方。并发转账可能导致余额不一致。
- **修复**: 使用双方账户的分布式锁，按固定顺序加锁避免死锁。

#### H-11: 权限校验只验证权限码存在性
- **文件**: `PermissionAspect.java:95-109`
- **描述**: `@RequirePermission` 切面仅查询权限码是否存在于数据库，不检查当前用户是否拥有该权限。
- **修复**: 查询用户权限列表并校验。

#### H-12: DataIsolationAspect 未使用 resourceIdParam
- **文件**: `DataIsolationAspect.java:44-86`
- **描述**: `@DataIsolation` 注解定义了 `resourceIdParam` 但切面完全未使用，资源级数据隔离形同虚设。
- **修复**: 在切面中读取 `resourceIdParam` 并校验资源归属。

#### H-13: AuditLogAspect 同步写数据库阻塞请求线程
- **文件**: `AuditLogAspect.java:129-133`
- **描述**: 注释写了"异步保存"但实际是同步调用 `operationLogRepository.save()`。
- **修复**: 使用 `@Async` 或 `ApplicationEventPublisher` 异步处理。

### 安全 (3)

#### H-14: RateLimitAspect Redis 故障时限流失效 (Fail-Open)
- **文件**: `RateLimitAspect.java:81-86`
- **描述**: Redis 不可用时请求直接放行，所有限流保护完全失效。
- **修复**: 关键接口（登录）应采用 fail-closed 策略。

#### H-15: 碳报告前端 API 编码损坏
- **文件**: `oaiss-chain-frontend/.../api/carbon.ts:10,15,28,33,42,51`
- **描述**: 所有中文错误消息都是乱码（如 `'鎶ュ憡鏍囬涓嶈兘涓虹┖'`），运行时显示不可读文本。
- **修复**: 确认文件编码为 UTF-8 并重新输入中文。

#### H-16: 后端 CarbonController 审计日志编码损坏
- **文件**: `CarbonController.java:169,182`
- **描述**: `@AuditLog(module = "纰虫牳绠?")` 应为 `"碳核算"`。
- **修复**: 修正文件编码。

### API 一致性 (3)

#### H-17: credit.ts addBonus 参数传递方式不匹配
- **文件**: `oaiss-chain-frontend/.../api/credit.ts:32-36`
- **描述**: 前端将 `{ enterpriseId, points, reason }` 作为 JSON body 发送，但后端使用 `@RequestParam` 接收三个独立参数。
- **修复**: 前端改为 query params 或后端改为 `@RequestBody`。

#### H-18: captcha.ts verify 返回类型不匹配
- **文件**: `oaiss-chain-frontend/.../api/captcha.ts:8-11`
- **描述**: 前端期望 `{ valid: boolean }` 但后端返回 `ApiResponse<Boolean>`（直接返回 `true`/`false`）。
- **修复**: 前端改为 `Promise<boolean>` 或后端改为返回对象。

#### H-19: carbon.ts CERTIFY_REJECTED 与 REVIEW_REJECTED 状态码冲突
- **文件**: `oaiss-chain-frontend/.../api/carbon.ts:5,7`
- **描述**: `CERTIFY_REJECTED = 4` 与 `REVIEW_REJECTED = 4` 相同，认证拒绝和审核拒绝无法区分。
- **修复**: 为认证拒绝使用独立状态码。

---

## MEDIUM 级别 (30)

### 前端 (15)

| ID | 文件 | 描述 |
|----|------|------|
| M-01 | `TradingMarket.vue:9,213` | `searchKeyword` 搜索框绑定了 ref 但 API 调用未使用，搜索功能无效 |
| M-02 | `TradingP2P.vue` | `quantity`/`unitPrice` 初始化为空字符串 `""` 但 `el-input-number` 期望数值 |
| M-03 | `Monitor.vue:58` | 生产代码中 `console.error` 语句 |
| M-04 | `Monitor.vue:257-262` | 状态过滤下拉缺少 UNDER_REVIEW(2) |
| M-05 | `CompanyDashboard.vue:130-133` | `creditTrend` 使用合成数据而非真实 API |
| M-06 | `CompanyDashboard.vue` | 饼图数据使用硬编码比例 (0.5, 0.3, 0.15, 0.05) |
| M-07 | `CarbonCoin.vue:258-261` | 转账目标用户使用数字输入框而非用户选择器 |
| M-08 | `CarbonCoin.vue:88` | 非空断言 `!` 缺少运行时保障 |
| M-09 | `CertificateManage.vue:225` | `$t` 与 `t` 使用不一致 |
| M-10 | 多文件 | `ref([])` 缺少泛型参数，推断为 `Ref<never[]>` |
| M-11 | 多文件 | 回调参数缺少类型注解，默认推断为 `any` |
| M-12 | `UserProfile.vue:82` | 不安全的类型断言 `as ProfileViewModel` |
| M-13 | `SystemUsers.vue:54-59` | `userTypeMap` 在模块初始化时调用 `t()`，不响应 locale 变更 |
| M-14 | `AuditList.vue:52` | 状态映射使用硬编码中文回退 |
| M-15 | `TradingMarket.vue` | `direction` 表单字段模板中为字符串但比较时为数字 |

### 后端服务 (10)

| ID | 文件 | 描述 |
|----|------|------|
| M-16 | `TradeController.java` | `parseDateTime` 静默返回 null，用户以为筛选生效但实际未生效 |
| M-17 | `AuthService.java:83,207` | 魔法数字 `user.getUserType() == 1` 未使用枚举 |
| M-18 | `AuthService.java:330-357` | 登录频率限制存在 TOCTOU 竞态 |
| M-19 | `CreditScoreService.java:137,234,307` | 使用 `IllegalArgumentException` 而非 `BusinessException` |
| M-20 | `CreditScoreService.java:251` | 分页参数未校验，`page=0` 或 null 时异常 |
| M-21 | `CarbonNeutralProjectService.java:480-483` | `generateProjectNo` 碰撞风险（4位随机数） |
| M-22 | `ReviewerService.java:54` | 使用 `RuntimeException` 而非 `BusinessException` |
| M-23 | `ReviewerService.java:120-125` | `incrementCompletedReviews` 非原子操作，并发丢失更新 |
| M-24 | `UserService.java:163` | 响应暴露 `lastLoginIp` 敏感信息 |
| M-25 | `CarbonService.java:258-294` | `parseEmissionTotals` 静默吞掉 JSON 异常，将无效数据当零排放处理 |

### AOP 切面 (5)

| ID | 文件 | 描述 |
|----|------|------|
| M-26 | `DistributedLockAspect.java:93` | SpEL 检测启发式不可靠，`#` 字符误触发 |
| M-27 | `RedisLockService.java:87-98` | 自旋重试无退避抖动，高并发惊群效应 |
| M-28 | `AuditLogAspect.java:188-191` | 敏感字段脱敏使用正则替换，存在绕过风险 |
| M-29 | `AuditLog.java:45` | `recordParams` 默认 true 可能导致 PII 明文记录 |
| M-30 | 所有 Aspect | 多个切面缺少 `@Order` 执行顺序控制 |

---

## LOW 级别 (8)

| ID | 文件 | 描述 |
|----|------|------|
| L-01 | `TradeController.java` | `parseDateTime` 静默返回 null |
| L-02 | `ReviewerQualificationService.java:111` | `getMyCertificate` 命名单复数不一致 |
| L-03 | `CarbonNeutralProjectService.java:550-596` | 项目类型使用硬编码数字而非常量 |
| L-04 | `CarbonCoinService.java:216-219` | `generateTxNo` 碰撞风险（同秒4位随机数） |
| L-05 | `CarbonCoinService.java:145-210` | 硬编码业务错误码未使用 `ErrorCode` 常量类 |
| L-06 | `DigitalSignatureService.java:383-392` | `markExpiredKeys` 是 public 但语义上是内部方法 |
| L-07 | `DigitalSignatureService.java` 多处 | 重复的"获取密钥+标记过期+重新获取"模式 |
| L-08 | `FabricBlockchainService.java:170,195` | 分页总数硬编码 100/10000 |

---

## 编码损坏问题汇总

编码损坏 (mojibake) 是贯穿前后端的系统性问题，影响以下文件：

| 文件 | 损坏内容 | 正确内容 |
|------|---------|---------|
| `frontend/api/carbon.ts` | `'鎶ュ憡鏍囬涓嶈兘涓虹┖'` 等 | `'报告标题不能为空'` 等 |
| `frontend/views/admin/VerifyList.vue:139` | `'姝ｅ父'` | `'正常'` |
| `backend/controller/CarbonController.java:169,182` | `"纰虫牳绠?"` | `"碳核算"` |

**根因**: 文件在非 UTF-8 环境下被编辑或转换，导致中文字符编码损坏。
**建议**: 全项目扫描 GBK/GB2312 编码损坏字符串，统一转换为 UTF-8。

---

## 架构级问题

### 1. N+1 查询普遍存在
4 个核心服务（CarbonService, CreditScoreService, CarbonNeutralProjectService, ReviewerService）的 `toResponse` 方法在分页映射中产生 N+1 查询。在数据量增长后会严重影响性能。

### 2. 分布式锁实现存在多个缺陷
- SpEL 注入风险 (CRITICAL)
- 锁释放竞态条件 (CRITICAL)  
- 时间单位不一致 (CRITICAL)
- 自旋无 jitter (MEDIUM)
- 未覆盖双方账户 (HIGH)

### 3. 数据隔离切面未完整实现
`@DataIsolation` 注解定义了 `resourceIdParam` 但切面未使用，资源级越权访问风险。

### 4. 前后端类型定义不一致
多个 API 模块使用 `Promise<unknown>` 返回类型，丧失 TypeScript 类型安全。部分分页 API 返回类型与后端不匹配。

---

## 建议优先修复顺序

1. **立即修复** (CRITICAL): 分布式锁 3 个问题 — 这些是安全和正确性阻塞项
2. **高优先级** (HIGH): 权限控制 (H-09, H-11, H-12)、编码损坏 (H-01, H-15, H-16)、并发安全 (H-10)
3. **中优先级** (MEDIUM): N+1 查询、客户端/服务端分页冲突、i18n 硬编码
4. **低优先级** (LOW): 命名规范、魔法数字、类型注解

---

*报告生成工具: Claude Code UAT Workflow*
*审查覆盖: 20 Controllers, 31 Services, 5 AOP Aspects, 22 Frontend API Modules, 46+ Vue Components*
