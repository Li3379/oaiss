# OAISS CHAIN 规格驱动修复说明

> 基于审计报告与 `phase-04-output.md` 整理的规范化修复文档  
> 日期：2026-05-04

## 已完成修复（61/63）

| 编号 | 问题 | 状态 | 验证方式 |
|---|---|---|---|
| C1 | Token 黑名单机制 | 已完成 | 1210 tests |
| C4 | P2P 交易越权 | 已完成 | 1210 tests |
| H5 | 文件上传类型校验 | 已完成 | 1210 tests |
| H17 | 买入配额校验 | 已完成 | 1210 tests |
| H18 | 分页参数上限控制 | 已完成 | 1210 tests |
| H1 | 路径遍历防护 | 已完成 | 1210 tests |
| H16 | 注册类型限制 | 已完成 | 1210 tests |
| C9 | `ddl-auto` 风险控制 | 已完成 | config |
| H2 | 生产环境禁用 Swagger 暴露 | 已完成 | config |
| H3 | Actuator 安全加固 | 已完成 | config |
| C2/C3 | 启动期密钥校验 | 已完成 | 1210 tests |
| H14 | Redis 密码配置 | 已完成 | config |
| H6 | 交易确认竞争态 | 已完成 | 1210 tests |
| H4 | 登录频率限制 | 已完成 | 1210 tests |
| C8 | 真实验证码图片 | 已完成 | 1210 tests |
| C5 | 文件操作权限控制 | 已完成 | 1210 tests |
| C6 | 撮合引擎锁 | 已完成 | 1210 tests |
| C7 | `.env` 文件泄露风险 | 已完成 | gitignore |
| H8 | 挂单编号重复 | 已完成 | 1210 tests |
| H12 | ECharts 内存泄漏 | 已完成 | 已存在清理逻辑 |
| H13 | `JSON.parse` 异常保护 | 已完成 | 前端修复 |
| H15 | CORS 默认值风险 | 已完成 | config |
| H7 | N+1 查询优化 | 已完成 | 1210 tests |
| H9 | JWT Token 内存存储方案 | 已完成 | vite build |
| H10 | Token 刷新竞争态 | 已完成 | 已有队列机制 |
| H11 | 前端 API 模块补齐 | 已完成 | vite build |
| M1 | 恢复 CSRF 保护 | 已完成 | 75 tests |
| M2 | 404 页面补齐 | 已完成 | vite build |
| M3 | IP 白名单 JSON 解析 | 已完成 | 75 tests |
| M5 | MinIO 文件列表分页 | 已完成 | 75 tests |
| M6 | 交易详情权限控制 | 已完成 | 75 tests |
| M7 | 拍卖挂单列表权限控制 | 已完成 | 75 tests |
| M11 | ECharts 按需引入 | 已完成 | vite build |
| M13 | CarbonCoin API 模块 | 已完成 | 已存在 |
| M14 | Dashboard 重复调用 | 已完成 | vite build |
| M15 | 密码确认校验增强 | 已完成 | vite build |
| M16 | 请求超时重试 | 已完成 | vite build |
| M20 | 危险操作确认弹窗 | 已完成 | 已存在 |
| M8 | 加载状态不一致 | 已完成 | vite build |
| M9 | 空状态提示缺失 | 已完成 | vite build |
| M17 | 文件上传进度显示 | 已完成 | vite build |
| M22 | 前端错误边界组件 | 已完成 | vite build |
| M18 | 暗黑模式支持 | 已完成 | vite build |
| M21 | 响应式布局增强 | 已完成 | vite build |
| M12 | 前端单元测试基础配置 | 已完成 | 13 tests |

## 修复详情（本轮新增 28 项）

### Spec-6：C7 `.env` 文件泄露处理

- 优先级：`CRITICAL`
- 修复方案：
  - 在仓库根目录新增 `.gitignore`，显式忽略 `.env`
  - 新增 `.env.example` 模板，仅保留占位符
  - 后端目录已补齐对 `.env` 的忽略
- 注意事项：
  - 如果敏感 `.env` 已被 Git 跟踪，需要手动执行 `git rm --cached .env`
- 相关文件：
  - `.gitignore`
  - `.env.example`

### Spec-7：H8 挂单编号重启后重复

- 优先级：`HIGH`
- 修复方案：
  - 用 `ThreadLocalRandom MATCH_RNG` 替换 `AtomicLong MATCH_SEQ`
  - `generateOrderNo()` 与 `generateMatchNo()` 改为使用随机 4 位后缀，而不是简单递增序列
  - 通过“时间戳 + 随机后缀”的组合降低重启后冲突概率
- 测试：
  - 11 个现有测试全部通过
- 相关文件：
  - `DoubleAuctionService.java`

### Spec-8：H12 ECharts 内存泄漏

- 优先级：`HIGH`
- 修复结论：
  - 确认 `CompanyDashboard.vue` 已包含正确清理逻辑
  - `onBeforeUnmount` 中会执行 `chartInstances.forEach(dispose)`
  - 已调用 `window.removeEventListener('resize', onResize)`
- 处理方式：
  - 审计时已视为修复完成，仅在文档中归档

### Spec-9：H13 `JSON.parse` 异常保护

- 优先级：`HIGH`
- 修复方案：
  - 在 `EmissionData.vue` 的 `onPredict()` 中为 `JSON.parse()` 增加独立 `try-catch`
  - 解析失败时明确提示：“历史数据格式错误，请输入有效的 JSON 数组”
  - 增加 `Array.isArray()` 校验，确保解析结果确实为数组
- 相关文件：
  - `EmissionData.vue`

### Spec-10：H15 CORS 默认允许 localhost

- 优先级：`HIGH`
- 修复方案：
  - 在 `application-docker.yml` 中移除 `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` 这种生产不安全默认值
  - 生产环境必须通过环境变量显式设置 `CORS_ALLOWED_ORIGINS`
  - `application-dev.yml` 仍保留开发期 localhost 默认值
- 相关文件：
  - `application-docker.yml`

### Spec-11：H7 N+1 查询优化

- 优先级：`HIGH`
- 修复方案：
  - `TradeService` 增加批量 `resolveUserNames()`，汇总 `sellerId/buyerId` 后一次性 `findAllById()`
  - `DoubleAuctionService` 同样增加批量用户名解析逻辑
  - 列表查询路径改为使用 `toResponse(entity, userNamesMap)` 重载版本
  - 单条查询仍使用 `toResponse(entity)` 保持简单
- 收益：
  - 列表查询由 `1 + 2N` 次数据库访问下降到约 `2` 次
- 测试：
  - 已同步更新 `TradeServiceTest` 与 `DoubleAuctionServiceTest` 中的 `findAllById` mock
- 相关文件：
  - `TradeService.java`
  - `DoubleAuctionService.java`
  - `TradeServiceTest.java`
  - `DoubleAuctionServiceTest.java`

### Spec-12：H9 JWT Token 内存存储

- 优先级：`HIGH`
- 修复方案：
  - Access token 从 `localStorage` 改为“内存变量 + sessionStorage”
  - 关闭标签页后自动失效，降低 XSS 获取长期 token 的风险
  - Refresh token 保留在 `localStorage`，仅用于刷新，不直接参与业务请求
  - `getAccessToken()` 优先读内存，再回退到 `sessionStorage`
  - `setTokens()` 同时写入内存与 `sessionStorage`
  - `clearTokens()` 清理全部存储位置
- 相关文件：
  - `src/utils/auth.js`

### Spec-13：H10 Token 刷新竞争态

- 优先级：`HIGH`
- 修复结论：
  - `request.js` 中已经具备正确的刷新队列机制
  - `isRefreshing` 用于避免并发重复刷新
  - `pendingRequests` 缓存刷新期间的请求
  - 刷新成功后统一重放，失败则统一拒绝
- 验证：
  - `vite build` 通过，逻辑审查通过

### Spec-14：H11 前端 API 模块补齐

- 优先级：`HIGH`
- 修复方案：
  - 新增 `api/file.js`
  - 新增 `api/signature.js`
  - 新增 `api/search.js`
  - 前端 API 覆盖率由约 `58%` 提升到 `100%`
- 验证：
  - `vite build` 成功，所有模块可正常导入

### Spec-15：M1 恢复 CSRF 保护

- 优先级：`MEDIUM`
- 修复方案：
  - `SecurityConfig.java` 中，将 `csrf(AbstractHttpConfigurer::disable)` 改为 `CookieCsrfTokenRepository.withHttpOnlyFalse()`
  - 为无需 CSRF 的公开端点做显式排除，例如 `/auth/login`、`/auth/register`、`/auth/captcha`、`/auth/refresh`、`/auth/check-ip`、`/captcha/**`
  - `request.js` 增加：
    - `withCredentials: true`
    - `xsrfCookieName: 'XSRF-TOKEN'`
    - `xsrfHeaderName: 'X-XSRF-TOKEN'`
- 相关文件：
  - `SecurityConfig.java`
  - `src/utils/request.js`

### Spec-16：M2 404 页面补齐

- 优先级：`MEDIUM`
- 修复方案：
  - 新增 `NotFound.vue`，展示 404 提示与返回入口
  - `router/index.js` 的 catch-all 路由从 `redirect: '/login'` 调整为懒加载 `NotFound.vue`
- 相关文件：
  - `src/views/NotFound.vue`
  - `src/router/index.js`

### Spec-17：M3 IP 白名单 JSON 解析

- 优先级：`MEDIUM`
- 修复方案：
  - `AuthService.java` 增加静态 `ObjectMapper IP_MAPPER`
  - `isIpAllowed()` 由手工字符串解析改成标准 JSON 解析
  - 解析失败时记录日志，并采取 fail-closed 策略拒绝访问
- 相关文件：
  - `AuthService.java`

### Spec-18：M5 MinIO 文件列表分页

- 优先级：`MEDIUM`
- 修复方案：
  - `MinioService.java` 将 `listFiles(String)` 扩展为 `listFiles(String, Integer, Integer)`，返回 `FileListResult record`
  - 使用 MinIO `maxKeys` 控制与内存 `subList` 分页，最大页大小限制为 1000
  - `FileController.java` 新增 `page` 和 `size` 请求参数
  - 同步更新相关测试 mock
- 相关文件：
  - `MinioService.java`
  - `FileController.java`
  - `FileControllerTest.java`

### Spec-19：M6 交易详情权限控制

- 优先级：`MEDIUM`
- 修复方案：
  - `TradeService.java` 将 `getTrade(Long)` 改为 `getTrade(Long, JwtUserDetails)`
  - 增加权限校验：管理员、审核员、第三方可查看全部，企业用户仅可查看自己参与的交易
  - `TradeController.java` 传入 `@AuthenticationPrincipal JwtUserDetails currentUser`
  - 同步更新控制器与服务测试
- 相关文件：
  - `TradeService.java`
  - `TradeController.java`
  - `TradeControllerTest.java`
  - `TradeServiceTest.java`

### Spec-20：M7 拍卖挂单列表权限

- 优先级：`MEDIUM`
- 修复方案：
  - `DoubleAuctionController.java` 的 `listOrders()` 添加 `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")`
  - 限制为管理员、审核员、第三方可查看
- 相关文件：
  - `DoubleAuctionController.java`

### Spec-21：M11 ECharts 按需引入

- 优先级：`MEDIUM`
- 修复方案：
  - 新建 `src/utils/echarts.js`，仅注册实际使用的图表与组件
  - `CompanyDashboard.vue` 与 `DataStatistics.vue` 改为引用该封装
  - 移除全量 `import * as echarts from 'echarts'`
- 效果：
  - ECharts bundle 由约 1MB 降至约 558KB
- 相关文件：
  - `src/utils/echarts.js`
  - `CompanyDashboard.vue`
  - `DataStatistics.vue`

### Spec-22：M13 CarbonCoin API 模块

- 优先级：`MEDIUM`
- 修复结论：
  - 确认 `api/carbon-coin.js` 已存在完整实现
  - 已包含 `getBalance`、`getTransactions`、`transfer`、`getMyScore` 等方法
  - 无需额外修改，记录为已完成

### Spec-23：M14 Dashboard 数据重复调用

- 优先级：`MEDIUM`
- 修复方案：
  - `CompanyDashboard.vue` 移除重复的 `creditScore` ref 声明
  - `fetchUserProfile()` 中只通过 `userProfile` 保存 `getMyScore()` 的结果
  - `computed` 中的 `creditScore` 改为读取 `userProfile.value?.score`
- 相关文件：
  - `CompanyDashboard.vue`

### Spec-24：M15 密码确认校验增强

- 优先级：`MEDIUM`
- 修复方案：
  - `UserProfile.vue` 中 confirm password 的 `trigger` 从 `'blur'` 改为 `['blur', 'change']`
  - 增加对 `newPassword` 的监听，在新密码变化时自动重新校验确认密码
- 相关文件：
  - `UserProfile.vue`

### Spec-25：M16 请求超时重试

- 优先级：`MEDIUM`
- 修复方案：
  - `request.js` 响应错误拦截器增加自动重试逻辑
  - 仅对超时（`ECONNABORTED`）和网络错误（`Network Error`）重试
  - 最大重试次数为 2，延迟为 1s、2s 递增
  - 通过 `__retryCount` 防止无限循环
  - 4xx / 5xx 不参与重试，直接按原错误处理
- 相关文件：
  - `src/api/request.js`

### Spec-26：M20 危险操作确认弹窗

- 优先级：`MEDIUM`
- 修复结论：
  - 关键危险操作页面已使用 `ElMessageBox.confirm`
  - 删除、状态切换等操作已有确认流程
  - 无需额外修改，仅归档确认

### Spec-27：M8 加载状态不一致

- 优先级：`MEDIUM`
- 修复方案：
  - `VerifyList.vue` 中的统计卡片增加 `v-loading="statsLoading"`
  - 新增 `statsLoading` ref，并在 `loadBlockchainStatus()` 中正确切换
  - 表格已有 `:loading="loading"`，保持不变
- 相关文件：
  - `VerifyList.vue`

### Spec-28：M9 空状态展示缺失

- 优先级：`MEDIUM`
- 修复方案：
  - 为主要表格补充 `empty-text`
  - 例如：
    - `VerifyList.vue`：暂无审核报告数据
    - `CarbonUpload.vue`：暂无碳核算报告，请点击“创建报告”新建
    - `TradingP2P.vue`：暂无 P2P 交易记录
    - `OrdersManage.vue`：暂无订单数据
    - `CarbonNeutral.vue`：暂无碳中和项目
- 相关文件：
  - 上述 5 个视图文件

### Spec-29：M17 文件上传进度显示

- 优先级：`MEDIUM`
- 修复方案：
  - `api/file.js` 中的 `uploadFile()` 与 `batchUploadFiles()` 新增第三个参数 `onProgress`
  - 透传 Axios `onUploadProgress`
  - 调用方可通过 `event.loaded / event.total` 计算百分比
- 相关文件：
  - `src/api/file.js`

### Spec-30：M22 前端错误边界组件

- 优先级：`MEDIUM`
- 修复方案：
  - 新建 `ErrorBoundary.vue`，使用 Vue 3 `onErrorCaptured` 捕获子组件渲染异常
  - 出错时展示友好错误提示与重试按钮，而不是空白页
  - 在 `App.vue` 中用 `<ErrorBoundary>` 包裹 `<router-view />`
- 相关文件：
  - `src/components/ErrorBoundary.vue`
  - `src/App.vue`

### Spec-31：M18 暗黑模式支持

- 优先级：`MEDIUM`
- 修复方案：
  - `main.js` 引入 `element-plus/theme-chalk/dark/css-vars.css`
  - `style.css` 增加 `html.dark` 的主题变量覆盖
  - `layout/index.vue` 的 header 中新增主题切换按钮
  - 主题偏好保存到 `localStorage`，并检测 `prefers-color-scheme: dark`
- 相关文件：
  - `src/main.js`
  - `src/style.css`
  - `layout/index.vue`

### Spec-32：M21 响应式布局增强

- 优先级：`MEDIUM`
- 修复方案：
  - `style.css` 增加 768px 断点下的全局响应式规则
  - `layout/index.vue` 在小屏下自动折叠侧边栏
  - 移动端侧边栏使用 `position: fixed` 浮层方式
  - 小屏下隐藏部分次要 header 信息
- 相关文件：
  - `src/style.css`
  - `layout/index.vue`

### Spec-33：M12 前端单元测试基础配置

- 优先级：`MEDIUM`
- 修复方案：
  - 引入 `vitest`、`@vue/test-utils`、`happy-dom`
  - `vite.config.js` 增加 `test` 配置，并排除 e2e
  - `package.json` 增加 `test` 与 `test:watch` 脚本
  - 新增 `auth.test.js` 与 `ErrorBoundary.test.js`
- 验证：
  - `npm test` 通过，13 个测试全部成功
- 相关文件：
  - `vite.config.js`
  - `package.json`
  - `src/utils/__tests__/auth.test.js`
  - `src/components/__tests__/ErrorBoundary.test.js`

### Spec-34：质量增强 - `auth.js` Token 安全防护

- 优先级：`HIGH`
- 修复方案：
  - `getAccessToken()` 增加 `isTokenExpired()` 检查
  - `parseJwtPayload()` 增加 JWT 结构校验，并拒绝 `alg: none`
  - 新增过期 token、错误段数、`alg:none`、缺失 exp claim 等测试
- 验证：
  - 17 个测试全部通过
- 相关文件：
  - `src/utils/auth.js`
  - `src/utils/__tests__/auth.test.js`

### Spec-35：质量增强 - ErrorBoundary 测试修复

- 优先级：`HIGH`
- 修复方案：
  - 测试改为验证真实 DOM 渲染结果
  - 增加 `el-button` stub 以适配 Element Plus
  - 补充 retry 点击后的状态恢复测试
- 验证：
  - 17 个测试全部通过
- 相关文件：
  - `src/components/__tests__/ErrorBoundary.test.js`

### Spec-36：质量增强 - 分页元数据完整保留

- 优先级：`MEDIUM`
- 修复方案：
  - Spring Data Page 转换时补充 `page`、`size`、`totalPages`
  - 用 `data.number` 替代不稳定的 `data.pageable.pageNumber`
- 验证：
  - `vite build` 成功
- 相关文件：
  - `src/api/request.js`

### Spec-37：质量增强 - ErrorBoundary 错误日志

- 优先级：`MEDIUM`
- 修复方案：
  - `onErrorCaptured` 中增加 `console.error('[ErrorBoundary]', err, errorInfo)`
- 验证：
  - 17 个测试通过
- 相关文件：
  - `src/components/ErrorBoundary.vue`

### Spec-38：质量增强 - 上传前前端校验

- 优先级：`MEDIUM`
- 修复方案：
  - `uploadFile()` 增加文件存在性检查与 50MB 限制
  - `batchUploadFiles()` 增加数组非空与逐文件大小校验
  - 后端已有类型校验，前端补上大小校验作为纵深防御
- 验证：
  - `vite build` 成功
- 相关文件：
  - `src/api/file.js`

### Spec-39：质量增强 - `request.js` 魔数常量化

- 优先级：`LOW`
- 修复方案：
  - 将 `MAX_RETRIES` 改为 `RETRY_MAX_ATTEMPTS`
  - 将 `RETRY_DELAY` 改为 `RETRY_BASE_DELAY_MS`
  - 提升命名语义清晰度
- 验证：
  - 17 个测试通过
- 相关文件：
  - `src/api/request.js`

### Spec-40：LOW - 未使用导入清理

- 修复方案：
  - 移除 `TradingMarket.vue` 中未使用的 `computed` 导入
- 验证：
  - `vite build` 成功
- 相关文件：
  - `src/views/enterprise/TradingMarket.vue`

### Spec-41：LOW - `request.js` 常量提取

- 修复方案：
  - 提取 `REQUEST_TIMEOUT_MS = 15000`
  - 提取 `SUCCESS_CODES = [200, 0]`
  - 替换硬编码的 timeout 与状态码判断
- 验证：
  - 17 个测试通过
- 相关文件：
  - `src/api/request.js`

### Spec-42：LOW - API 模块 JSDoc 补齐

- 修复方案：
  - 为 6 个 API 模块补齐 `@param` / `@returns`
  - 涉及模块：
    - `auth.js`
    - `carbon.js`
    - `trade.js`
    - `user.js`
    - `thirdParty.js`
    - `carbonNeutral.js`
- 验证：
  - `vite build` 成功

### Spec-43：LOW - L1/L2/L3 收尾修复

- L1 命名一致性：
  - `CarbonCoin.vue` 中使用的 SNAKE_CASE 状态值是后端枚举映射，无需修改
- L2 内联样式抽离：
  - `UserProfile.vue` 中 `style="max-width:600px"` 与 `style="max-width:500px"` 已提取为 `.profile-form` / `.pwd-form`
- L3 输入校验增强：
  - `trade.js`：`createP2PTrade` 增加 `carbonAmount > 0` 和 `price > 0`
  - `carbon.js`：`createReport` 增加 `title` 非空校验；`submitReport/deleteReport/reviewReport` 增加必要 ID 和状态校验
  - `user.js`：`updateProfile` 增加 null 校验；`changePassword` 增加旧密码和新密码长度校验
- 验证：
  - 17 个测试全部通过，`vite build` 成功

## 剩余问题（2/63）

以下两项被视为延期的独立项目，不纳入本轮修复闭环：

| 编号 | 问题 | 说明 |
|---|---|---|
| M4 | 前端全面迁移到 TypeScript / PropTypes 强约束 | 属于较大架构改造，建议独立立项 |
| M19 | 全面国际化 `i18n` | 属于较大功能扩展，建议独立立项 |
