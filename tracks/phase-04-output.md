# OAISS CHAIN 双碳链动系统 — 全面项目问题发现报告

> 基于 SKILL 双层循环方法论，通过 6 个并行调查方案（安全/后端/前端/架构/API/测试）进行系统性问题发现。
> 调查方式：代码审查 + 智能体分析 + 静态分析。
> 报告日期：2026-05-02

---

## 总览

| 严重级别 | 数量 | 说明 |
|----------|------|------|
| CRITICAL | 9 | 必须立即修复，存在安全风险或数据丢失风险 |
| HIGH | 18 | 上线前必须修复，影响功能正确性 |
| MEDIUM | 22 | 应尽快修复，影响用户体验或代码质量 |
| LOW | 14 | 建议修复，提升代码可维护性 |
| **合计** | **63** | |

---

## 一、CRITICAL 问题（9 个）

### C1. 登出未实际失效 Token
**类别**: 安全 | **文件**: `AuthService.java:267-269`

```java
public void logout(String username) {
    // 将Token加入黑名单（实际项目中可使用Redis实现）
    log.info("User logged out: {}", username);
}
```

**问题**: logout 方法仅记录日志，未实际将 Token 加入黑名单。用户登出后，旧 Token 在过期前仍可使用。
**影响**: 账号被盗后无法通过登出限制攻击者访问。
**修复**: 使用 Redis 实现 Token 黑名单，登出时将 Token 的剩余有效期写入黑名单。

---

### C2. JWT Secret 硬编码回退值
**类别**: 安全 | **文件**: `application.yml:72`

```yaml
jwt:
  secret: ${JWT_SECRET:oaiss-chain-dev-jwt-secret-key-must-be-at-least-256-bits-long}
```

**问题**: JWT Secret 有硬编码默认值。如果未设置环境变量，系统使用可预测的密钥。
**影响**: 攻击者可伪造任意用户的 JWT Token。
**修复**: 删除默认值，启动时校验 Secret 存在且长度合规。

---

### C3. 数据库密码硬编码回退值
**类别**: 安全 | **文件**: `application.yml:17`

```yaml
password: ${DB_PASSWORD:123456}
```

**问题**: 数据库密码有硬编码默认值 `123456`。
**修复**: 删除默认值，强制通过环境变量配置。

---

### C4. P2P 交易创建无卖方身份验证（IDOR）
**类别**: 安全 | **文件**: `TradeService.java:47-89`

```java
Long sellerId = request.getSellerId(); // 直接使用请求中的 sellerId
Long buyerId = request.getBuyerId();
```

**问题**: `createP2PTrade` 直接使用请求中的 `sellerId`，未验证当前用户是否为卖方。任何企业用户可以代替其他用户创建交易。
**影响**: 严重越权漏洞，可伪造他人交易。
**修复**: 验证 `sellerId == currentUser.getUserId()` 或使用 `currentUser` 替代请求参数。

---

### C5. 文件操作无权限控制
**类别**: 安全 | **文件**: `FileController.java` 全文

**问题**: 所有文件操作（上传/下载/删除/预签名URL/列表/复制）仅需认证，无角色或所有权检查。任何已登录用户可以：
- 下载其他用户上传的文件
- 删除任意文件
- 获取任意文件的预签名 URL
- 列出所有文件

**修复**: 添加所有权校验或基于角色的权限控制。

---

### C6. 撮合引擎无分布式锁（竞态条件）
**类别**: 后端代码质量 | **文件**: `DoubleAuctionService.java:137`

**问题**: `executeMatching()` 方法没有加锁。多个管理员同时触发撮合可导致：
- 同一挂单被撮合多次
- 配额计算错误（双重加减）
- 数据不一致

**修复**: 使用 `RedisLockService` 或数据库悲观锁保护撮合操作。

---

### C7. .env 文件包含真实密码并已提交
**类别**: 安全 | **文件**: `.env`

```
DB_PASSWORD=123456
JWT_SECRET=oaiss-chain-jwt-secret-key-must-be-at-least-256-bits-long
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

**问题**: 敏感凭证已提交到版本控制。即使后续删除，Git 历史仍保留。
**修复**: 将 `.env` 加入 `.gitignore`，创建 `.env.example` 模板，轮换所有已泄露的密钥。

---

### C8. 验证码为伪实现（Base64 文本而非图片）
**类别**: 安全 | **文件**: `AuthService.java:228-230`

```java
String captchaImage = Base64.getEncoder().encodeToString(
    ("CAPTCHA: " + captcha).getBytes());
```

**问题**: "验证码"仅是将明文验证码编码为 Base64，前端解码即可获取答案。完全无防护作用。
**影响**: 登录接口可被暴力破解。
**修复**: 使用图片生成库（如 Java AWT 或 Hutool）生成真实验证码图片。

---

### C9. `ddl-auto: update` 在生产配置中
**类别**: 架构 | **文件**: `application.yml:29`

```yaml
ddl-auto: update
```

**问题**: Hibernate `ddl-auto: update` 在生产环境可能导致数据丢失或表结构被意外修改。PLAN.md 标记为已修复，但实际未修复。
**修复**: 改为 `validate`，由 Flyway 管理 DDL。

---

## 二、HIGH 问题（18 个）

### H1. 白名单路径匹配使用 startsWith（可绕过）
**文件**: `JwtAuthenticationFilter.java:132-135`

使用 `startsWith` 而非精确匹配，攻击者可用 `/api/v1/auth/login/../admin/users` 等路径绕过认证。

### H2. Swagger UI 公开可访问
**文件**: `SecurityConfig.java:68-73` — 生产环境暴露所有 API 结构。

### H3. Actuator Health 暴露内部细节
**文件**: `application.yml:103` — `show-details: always` 暴露数据库连接信息。

### H4. 无登录频率限制
**文件**: `AuthController.java:32-61` — `/auth/login` 可被暴力破解。

### H5. 文件上传无类型白名单
**文件**: `MinioService.java:330-339` — 可上传 .jsp/.exe 等危险文件。

### H6. 交易确认竞态条件
**文件**: `TradeService.java:128-166` — 买卖双方同时确认可导致配额双重加减。

### H7. N+1 查询性能问题
**文件**: `TradeService.java:220-246`, `DoubleAuctionService.java:360-415` — 列表查询触发大量额外 SQL。

### H8. 挂单编号重启后可能重复
**文件**: `DoubleAuctionService.java:65` — `AtomicLong` 重启后重置。

### H9. JWT Token 存储在 localStorage
**文件**: `auth.js:1-22` — XSS 攻击可直接窃取。

### H10. Token 刷新竞态条件
**文件**: `request.js:40-60` — 多个并发请求可能触发重复刷新。

### H11. 前端 API 覆盖率仅 58%
**文件**: `src/api/` vs `controller/` — 缺失 DigitalSignature/File/Search 模块。

### H12. ECharts 内存泄漏
**文件**: `CompanyDashboard.vue:37,228-233` — 页面导航未销毁 ECharts 实例。

### H13. JSON.parse 未做异常保护
**文件**: `EmissionData.vue:60` — 格式错误导致组件崩溃。

### H14. Redis 密码为空
**文件**: `application.yml:53` — Redis 无密码保护。

### H15. CORS 默认允许 localhost
**文件**: `SecurityConfig.java:44` — 生产环境默认允许跨域。

### H16. 注册接口无限制（可注册管理员）
**文件**: `AuthService.java:113-165` — 允许任意用户类型注册。

### H17. 买入挂单未验证配额
**文件**: `DoubleAuctionService.java:70-90` — Enterprise 获取但未检查。

### H18. 分页参数无上限
**文件**: 多个 Controller — `size=999999` 可导致 OOM。

---

## 三、MEDIUM 问题（22 个）

| # | 问题 | 文件 |
|---|------|------|
| M1 | CSRF 完全禁用 | SecurityConfig.java:53 |
| M2 | 无 404 页面 | router/index.js:139 |
| M3 | IP 白名单使用字符串替换解析 | AuthService.java:327-329 |
| M4 | 前端无 TypeScript/PropTypes | 全部 .vue |
| M5 | MinioService.listFiles 无分页 | MinioService.java:277 |
| M6 | 交易详情端点无权限控制 | TradeController.java:175 |
| M7 | 拍卖挂单列表无权限控制 | DoubleAuctionController.java:90 |
| M8 | 前端加载状态不一致 | 多个 View |
| M9 | 前端空状态展示缺失 | 多个 View |
| M10 | 缺少 signature/file/search API 模块 | src/api/ |
| M11 | 全量 ECharts 导入（~1MB） | main.js |
| M12 | 前端单元测试为零 | src/ |
| M13 | CarbonCoin 缺少 API 模块文件 | src/api/ |
| M14 | CompanyDashboard 重复 API 调用 | CompanyDashboard.vue |
| M15 | 前端密码确认校验不严谨 | 注册/修改密码组件 |
| M16 | 缺少请求超时重试机制 | request.js |
| M17 | 缺少文件上传进度显示 | 文件上传组件 |
| M18 | 前端缺少暗黑模式支持 | 全局 |
| M19 | 缺少国际化支持 | 全局 |
| M20 | 缺少操作确认弹窗 | 交易/删除操作 |
| M21 | 响应式设计不完善 | 多个组件 |
| M22 | 前端缺少错误边界组件 | 全局 |

---

## 四、LOW 问题（14 个）

| # | 问题 |
|---|------|
| L1 | 路由命名不统一（camelCase vs kebab-case） |
| L2 | 无 403 页面 |
| L3 | JWT 时钟偏移未处理 |
| L4 | 前端缺少 JSDoc 注释 |
| L5 | 未使用的搜索过滤器变量 |
| L6 | 硬编码数组访问未做边界检查 |
| L7 | 日志级别在生产环境过于详细 |
| L8 | 缺少 API 版本管理策略 |
| L9 | 前端未配置 CSP Header |
| L10 | 未使用 HTTPS |
| L11 | 健康检查端点未完全保护 |
| L12 | 缺少请求链路追踪 |
| L13 | 前端缺少代码分割 |
| L14 | Docker 镜像未设置资源限制 |

---

## 五、测试覆盖分析

### 后端测试
- JaCoCo 要求：90%（配置存在但未验证达标）
- 关键缺失：交易并发测试、拍卖撮合竞态测试、权限绕过测试、文件上传安全测试、Token 黑名单测试

### 前端测试
- Playwright E2E：5 个测试文件
- 单元测试：0 个
- 关键缺失：Token 刷新测试、表单验证测试、路由守卫测试、API 错误处理测试

---

## 六、优先修复路线图

### P0 — 立即修复（安全漏洞）
1. C1: 实现 Token 黑名单
2. C2/C3: 删除硬编码密码回退值
3. C4: 修复 P2P 交易越权
4. C5: 添加文件操作权限控制
5. C7: 处理 .env 文件泄露
6. C8: 实现真实验证码

### P1 — 上线前修复（数据安全）
7. C6: 撮合引擎加分布式锁
8. C9: ddl-auto 改为 validate
9. H1: 修复白名单路径匹配
10. H14: 配置 Redis 密码
11. H16: 限制注册用户类型
12. H6: 交易确认加锁

### P2 — 第一迭代修复（功能完善）
13. H11: 补齐前端 API 模块
14. H5: 添加文件类型白名单
15. H4: 添加登录频率限制
16. H7: 解决 N+1 查询
17. H17: 买入挂单验证配额
18. H18: 分页参数上限

### P3 — 第二迭代修复（体验优化）
19. H12: 修复 ECharts 内存泄漏
20. H13: JSON.parse 异常保护
21. M1-M22: 逐一处理 MEDIUM 级别问题

---

## 七、SKILL 执行记录

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | 验收清单定义（6 维度 38 项） | ✅ |
| Phase 2 | 6 个调查方案创建 | ✅ |
| Phase 3-R1 | 安全审查（代码审查 + 智能体） | ✅ |
| Phase 3-R1 | 后端代码质量（智能体详细分析） | ✅ |
| Phase 3-R1 | 前端代码质量（智能体详细分析） | ✅ |
| Phase 3-R1 | 架构设计（代码审查） | ✅ |
| Phase 3-R1 | API 一致性（智能体详细分析） | ✅ |
| Phase 3-R1 | 测试覆盖（代码审查） | ✅ |
| Phase 4 | 最终报告汇编 | ✅ |

---

*报告生成时间: 2026-05-02 | 基于 SKILL auto-optimize 双层循环方法论*
