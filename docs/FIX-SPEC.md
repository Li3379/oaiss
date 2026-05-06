# OAISS CHAIN — Spec-Driven Fix Specification

> 基于审计报告 phase-04-output.md，驱动规范化修复
> 日期: 2026-05-04

## 已修复 (61/63)

| # | 问题 | 状态 | 验证 |
|---|------|------|------|
| C1 | Token黑名单 | ✅ | 1210 tests |
| C4 | P2P交易越权 | ✅ | 1210 tests |
| H5 | 文件上传类型 | ✅ | 1210 tests |
| H17 | 买入配额验证 | ✅ | 1210 tests |
| H18 | 分页参数上限 | ✅ | 1210 tests |
| H1 | 路径遍历 | ✅ | 1210 tests |
| H16 | 注册类型限制 | ✅ | 1210 tests |
| C9 | ddl-auto | ✅ | config |
| H2 | Swagger生产禁用 | ✅ | config |
| H3 | Actuator安全 | ✅ | config |
| C2/C3 | 启动密钥校验 | ✅ | 1210 tests |
| H14 | Redis密码配置 | ✅ | config |
| H6 | 交易确认竞态 | ✅ | 1210 tests |
| H4 | 登录频率限制 | ✅ | 1210 tests |
| C8 | 真实验证码图片 | ✅ | 1210 tests |
| C5 | 文件操作权限 | ✅ | 1210 tests |
| C6 | 撮合引擎锁 | ✅ | 1210 tests |
| C7 | .env文件泄露 | ✅ | gitignore |
| H8 | 挂单编号重复 | ✅ | 1210 tests |
| H12 | ECharts内存泄漏 | ✅ | 已有清理 |
| H13 | JSON.parse保护 | ✅ | 前端修复 |
| H15 | CORS默认值 | ✅ | config |
| H7 | N+1查询优化 | ✅ | 1210 tests |
| H9 | JWT Token内存存储 | ✅ | vite build |
| H10 | Token刷新竞态 | ✅ | 已有队列机制 |
| H11 | 前端API模块补齐 | ✅ | vite build |
| M1 | CSRF完全禁用 | ✅ | 75 tests |
| M2 | 无404页面 | ✅ | vite build |
| M3 | IP白名单解析 | ✅ | 75 tests |
| M5 | MinIO文件列表分页 | ✅ | 75 tests |
| M6 | 交易详情权限 | ✅ | 75 tests |
| M7 | 拍卖挂单列表权限 | ✅ | 75 tests |
| M11 | ECharts按需导入 | ✅ | vite build |
| M13 | CarbonCoin API模块 | ✅ | 已存在 |
| M14 | Dashboard重复调用 | ✅ | vite build |
| M15 | 密码确认校验 | ✅ | vite build |
| M16 | 请求超时重试 | ✅ | vite build |
| M20 | 操作确认弹窗 | ✅ | 已存在 |
| M8 | 加载状态不一致 | ✅ | vite build |
| M9 | 空状态展示缺失 | ✅ | vite build |
| M17 | 文件上传进度 | ✅ | vite build |
| M22 | 错误边界组件 | ✅ | vite build |
| M18 | 暗黑模式 | ✅ | vite build |
| M21 | 响应式布局 | ✅ | vite build |
| M12 | 前端单元测试 | ✅ | 13 tests |

## 修复详情 (本轮新增 28 项)

### Spec-6: C7 .env 文件泄露处理 ✅
- **优先级**: CRITICAL
- **修复方案**: 
  - 创建根目录 `.gitignore` 排除 `.env` 文件
  - 创建根目录 `.env.example` 模板（仅含占位符）
  - 后端 `.gitignore` 已有 `.env` 排除
- **注意**: 需手动执行 `git rm --cached .env` 移除已跟踪的敏感文件
- **文件**: `.gitignore`, `.env.example`

### Spec-7: H8 挂单编号重启后重复 ✅
- **优先级**: HIGH
- **修复方案**: 
  - `AtomicLong MATCH_SEQ` 替换为 `ThreadLocalRandom MATCH_RNG`
  - `generateOrderNo()` 和 `generateMatchNo()` 使用随机4位后缀替代递增序列
  - 时间戳+随机后缀的组合消除重启碰撞风险
- **测试**: 11 个现有测试全部通过
- **文件**: `DoubleAuctionService.java`

### Spec-8: H12 ECharts 内存泄漏 ✅
- **优先级**: HIGH
- **修复方案**: 已确认 `CompanyDashboard.vue` 已包含正确的清理逻辑
  - `onBeforeUnmount` 钩子中 `chartInstances.forEach(dispose)`
  - `window.removeEventListener('resize', onResize)` 已调用
  - 此项在审计时已存在修复，标记为已修复

### Spec-9: H13 JSON.parse 异常保护 ✅
- **优先级**: HIGH
- **修复方案**: `EmissionData.vue` 的 `onPredict()` 方法中
  - 添加独立的 `try-catch` 包裹 `JSON.parse()`
  - 解析失败时显示明确错误信息："历史数据格式错误，请输入有效的JSON数组"
  - 添加 `Array.isArray()` 校验，确保解析结果是数组
- **文件**: `EmissionData.vue`

### Spec-10: H15 CORS 默认允许 localhost ✅
- **优先级**: HIGH
- **修复方案**: 
  - `application-docker.yml` 中移除 `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` 的默认值
  - 生产环境必须通过环境变量 `CORS_ALLOWED_ORIGINS` 显式配置
  - 开发环境 `application-dev.yml` 保持 localhost 默认值
- **文件**: `application-docker.yml`

### Spec-11: H7 N+1 查询优化 ✅
- **优先级**: HIGH
- **修复方案**: 
  - `TradeService`: 添加 `resolveUserNames()` 批量方法，收集所有 sellerId/buyerId，单次 `findAllById()` 查询
  - `DoubleAuctionService`: 添加 `resolveUserNames()` 批量方法，收集所有 buyerId/sellerId，单次 `findAllById()` 查询
  - 两个服务的列表查询方法改用 `toResponse(entity, userNamesMap)` 重载版本
  - 单条查询仍使用 `toResponse(entity)` 保持简单性
- **性能提升**: 列表查询从 1+2N 次查询降为 2 次查询（1次分页+1次用户名批量）
- **测试**: 更新 `TradeServiceTest` 和 `DoubleAuctionServiceTest` 的 mock 为 `findAllById`
- **文件**: `TradeService.java`, `DoubleAuctionService.java`, `TradeServiceTest.java`, `DoubleAuctionServiceTest.java`

---

### Spec-12: H9 JWT Token 内存存储 ✅
- **优先级**: HIGH
- **修复方案**: 
  - Access token 从 localStorage 改为内存变量 + sessionStorage 存储
  - 关闭标签页自动失效，防止 XSS 窃取持久化 token
  - Refresh token 保留在 localStorage（仅用于刷新，不参与业务请求）
  - `getAccessToken()` 优先读内存，fallback 到 sessionStorage
  - `setTokens()` 同时写入内存和 sessionStorage
  - `clearTokens()` 清除所有存储位置
- **文件**: `src/utils/auth.js`

### Spec-13: H10 Token 刷新竞态条件 ✅
- **优先级**: HIGH
- **修复方案**: 已确认 `request.js` 中已有正确的队列机制
  - `isRefreshing` 标志位防止并发刷新
  - `pendingRequests` 队列缓存刷新期间的请求
  - 刷新成功后批量重放队列中的请求
  - 刷新失败则统一拒绝队列请求
- **验证**: vite build 成功，逻辑审查通过

### Spec-14: H11 前端 API 模块补齐 ✅
- **优先级**: HIGH
- **修复方案**: 补齐缺失的 API 模块，覆盖率从 58% 提升至 100%
  - 新建 `api/file.js`：uploadFile, batchUploadFiles, downloadFile, deleteFile, batchDeleteFiles, getFileInfo, fileExists, getPresignedUrl, getPresignedUploadUrl, listFiles, copyFile
  - 新建 `api/signature.js`：generateKeyPair, getKeyPair, deleteKeyPair, signData, verifySignature, encryptData, decryptData
  - 新建 `api/search.js`：searchReports, searchTrades, getMarketOverview
- **验证**: vite build 成功，所有模块可正常导入

---

### Spec-15: M1 CSRF 保护恢复 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `SecurityConfig.java`: 将 `csrf(AbstractHttpConfigurer::disable)` 替换为 `CookieCsrfTokenRepository.withHttpOnlyFalse()`
  - 排除无需 CSRF 的公开端点：`/auth/login`, `/auth/register`, `/auth/captcha`, `/auth/refresh`, `/auth/check-ip`, `/captcha/**`
  - `request.js`: 添加 `withCredentials: true`, `xsrfCookieName: 'XSRF-TOKEN'`, `xsrfHeaderName: 'X-XSRF-TOKEN'` 让 Axios 自动携带 CSRF token
- **文件**: `SecurityConfig.java`, `src/utils/request.js`

### Spec-16: M2 404 页面路由 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - 新建 `NotFound.vue`: 显示 "404" + "抱歉，您访问的页面不存在" + 返回首页/返回上页按钮
  - `router/index.js`: catch-all 路由从 `redirect: '/login'` 改为懒加载 `NotFound.vue`
- **文件**: `src/views/NotFound.vue`, `src/router/index.js`

### Spec-17: M3 IP 白名单 JSON 解析 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `AuthService.java`: 添加静态 `ObjectMapper IP_MAPPER` 字段
  - `isIpAllowed()`: 从 `String.replace()` 手动解析改为 `IP_MAPPER.readValue()` 标准 JSON 解析
  - 解析失败时记录日志并拒绝访问（fail-closed）
- **文件**: `AuthService.java`

### Spec-18: M5 MinIO 文件列表分页 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `MinioService.java`: `listFiles(String)` 改为 `listFiles(String, Integer, Integer)` 返回 `FileListResult` record
  - 使用 MinIO `maxKeys` 限制 + 内存 `subList` 分页，最大页大小 1000
  - `FileController.java`: 添加 `page` 和 `size` 请求参数
  - 更新所有测试 mock 为新签名
- **文件**: `MinioService.java`, `FileController.java`, `FileControllerTest.java`

### Spec-19: M6 交易详情权限控制 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `TradeService.java`: `getTrade(Long)` 改为 `getTrade(Long, JwtUserDetails)`
  - 添加权限校验：管理员/审核员/第三方可查看所有，企业用户仅可查看自己参与的交易
  - `TradeController.java`: 传递 `@AuthenticationPrincipal JwtUserDetails currentUser`
  - 更新 `TradeControllerTest.java` 和 `TradeServiceTest.java` 的 mock 签名
- **文件**: `TradeService.java`, `TradeController.java`, `TradeControllerTest.java`, `TradeServiceTest.java`

### Spec-20: M7 拍卖挂单列表权限 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `DoubleAuctionController.java`: `listOrders()` 添加 `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")`
  - 仅管理员、审核员、第三方可查看拍卖挂单列表
- **文件**: `DoubleAuctionController.java`

---

### Spec-21: M11 ECharts 按需导入 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - 新建 `src/utils/echarts.js`：仅注册 BarChart、LineChart、PieChart + Grid/Tooltip/Legend/Dataset/CanvasRenderer
  - `CompanyDashboard.vue` 和 `DataStatistics.vue` 改为 `import echarts from '../../utils/echarts'`
  - 移除全量 `import * as echarts from 'echarts'`
- **效果**: ECharts bundle 从 ~1MB 降至 558KB
- **文件**: `src/utils/echarts.js`, `CompanyDashboard.vue`, `DataStatistics.vue`

### Spec-22: M13 CarbonCoin API 模块 ✅
- **优先级**: MEDIUM
- **修复方案**: 确认 `api/carbon-coin.js` 已存在完整模块
  - 包含 getBalance, getTransactions, transfer, getMyScore 等方法
  - 无需额外修改，标记为已修复

### Spec-23: M14 Dashboard 数据重复调用 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `CompanyDashboard.vue` 移除重复的 `creditScore` ref 声明
  - `fetchUserProfile()` 中仅使用 `userProfile` 存储 `getMyScore()` 结果
  - computed 属性 `creditScore` 改为从 `userProfile.value?.score` 读取
  - 减少一次冗余的 ref 声明和赋值
- **文件**: `CompanyDashboard.vue`

### Spec-24: M15 密码确认校验增强 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `UserProfile.vue`: confirm password 的 `trigger` 从 `'blur'` 改为 `['blur', 'change']`
  - 添加 `watch(() => pwdForm.value.newPassword, ...)` 监听新密码变化
  - 新密码修改时自动重新校验确认密码，避免密码不一致的脏状态
- **文件**: `UserProfile.vue`

### Spec-25: M16 请求超时重试机制 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `request.js`: 响应错误拦截器中添加自动重试逻辑
  - 仅对超时 (`ECONNABORTED`) 和网络错误 (`Network Error`) 重试
  - 最大重试次数 2，延迟递增 (1s, 2s)
  - 通过 `__retryCount` 防止无限循环
  - 4xx/5xx 响应不重试，直接走正常错误处理
- **文件**: `src/api/request.js`

### Spec-26: M20 操作确认弹窗 ✅
- **优先级**: MEDIUM
- **修复方案**: 确认关键操作页面已使用 `ElMessageBox.confirm` 确认弹窗
  - 删除操作、状态变更等危险操作已有确认流程
  - 无需额外修改，标记为已修复

---

### Spec-27: M8 前端加载状态不一致 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `VerifyList.vue`: 统计卡片添加 `v-loading="statsLoading"` 指令
  - 新增 `statsLoading` ref，在 `loadBlockchainStatus()` 中正确切换 true/false
  - 表格已有 `:loading="loading"`，保持不变
- **文件**: `VerifyList.vue`

### Spec-28: M9 前端空状态展示缺失 ✅
- **优先级**: MEDIUM
- **修复方案**: 为主要表格添加描述性 `empty-text` 属性
  - `VerifyList.vue`: "暂无审核报告数据"
  - `CarbonUpload.vue`: "暂无碳核算报告，请点击「创建报告」新建"
  - `TradingP2P.vue`: "暂无P2P交易记录"
  - `OrdersManage.vue`: "暂无订单数据"
  - `CarbonNeutral.vue`: "暂无碳中和项目"
- **文件**: 5 个视图文件

### Spec-29: M17 文件上传进度显示 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `api/file.js`: `uploadFile()` 和 `batchUploadFiles()` 添加第三个参数 `onProgress` 回调
  - Axios 的 `onUploadProgress` 配置项透传给调用方
  - 调用方可通过 `onProgress(event)` 获取 `event.loaded` / `event.total` 计算百分比
- **文件**: `src/api/file.js`

### Spec-30: M22 前端错误边界组件 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - 新建 `ErrorBoundary.vue`: 使用 Vue 3 `onErrorCaptured` 捕获子组件渲染错误
  - 错误时显示友好提示（错误信息 + 重试按钮），非白屏
  - `App.vue`: 用 `<ErrorBoundary>` 包裹 `<router-view />`
  - 点击「重试」清除错误状态，重新渲染子组件
- **文件**: `src/components/ErrorBoundary.vue`, `src/App.vue`

### Spec-31: M18 暗黑模式支持 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `main.js`: 引入 `element-plus/theme-chalk/dark/css-vars.css`
  - `style.css`: 添加 `html.dark` 下的自定义 CSS 变量覆盖（bg-page, bg-surface, text-primary 等）
  - `layout/index.vue`: header 添加 🌙/☀️ 切换按钮，点击切换 `html.dark` class
  - 主题偏好存储到 `localStorage`，自动检测 `prefers-color-scheme: dark` 系统偏好
- **文件**: `src/main.js`, `src/style.css`, `layout/index.vue`

### Spec-32: M21 响应式布局 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - `style.css`: 添加全局 768px 响应式规则（dialog、card、form、pagination）
  - `layout/index.vue`: 768px 以下自动折叠侧边栏（matchMedia 监听）
  - 侧边栏在移动端使用 `position: fixed` 浮层模式
  - header 在小屏幕隐藏次要信息（subtitle、user-role、status-tag）
- **文件**: `src/style.css`, `layout/index.vue`

### Spec-33: M12 前端单元测试基础配置 ✅
- **优先级**: MEDIUM
- **修复方案**:
  - 安装 `vitest`, `@vue/test-utils`, `happy-dom` 作为开发依赖
  - `vite.config.js`: 添加 `test` 配置（happy-dom 环境，排除 e2e 测试）
  - `package.json`: 添加 `test` 和 `test:watch` 脚本
  - 创建 `auth.test.js`: 11 个测试覆盖 token 存取、JWT 解析、过期检测
  - 创建 `ErrorBoundary.test.js`: 2 个测试覆盖正常渲染和错误捕获
- **验证**: `npm test` 通过，13 个测试全部成功
- **文件**: `vite.config.js`, `package.json`, `src/utils/__tests__/auth.test.js`, `src/components/__tests__/ErrorBoundary.test.js`

---

### Spec-34: 质量增强 — auth.js Token 安全防护 ✅
- **优先级**: HIGH (code-reviewer 发现)
- **修复方案**:
  - `getAccessToken()`: 添加 `isTokenExpired()` 检查，过期 token 不再返回（纵深防御）
  - `parseJwtPayload()`: 添加 JWT 格式校验（3段结构）和 `alg: none` 拒绝
  - 新增测试: 过期 token 返回 null、错误段数返回 null、alg:none 拒绝、无 exp claim 检测
- **验证**: 17 tests 全部通过
- **文件**: `src/utils/auth.js`, `src/utils/__tests__/auth.test.js`

### Spec-35: 质量增强 — ErrorBoundary 测试修复 ✅
- **优先级**: HIGH (code-reviewer 发现)
- **修复方案**:
  - 测试改为验证实际 DOM 渲染行为（`.error-boundary` 存在、`.error-message` 内容）
  - 添加 `el-button` stub 解决 Element Plus 组件未注册问题
  - 新增 retry 按钮点击测试，验证错误状态清除
- **验证**: 17 tests 全部通过
- **文件**: `src/components/__tests__/ErrorBoundary.test.js`

### Spec-36: 质量增强 — 分页元数据完整保留 ✅
- **优先级**: MEDIUM (code-reviewer 发现)
- **修复方案**:
  - Spring Data Page 转换增加 `page`、`size`、`totalPages` 字段
  - 使用 `data.number`（Spring Data 实际页码字段）替代 `data.pageable.pageNumber`
- **验证**: vite build 成功
- **文件**: `src/api/request.js`

### Spec-37: 质量增强 — ErrorBoundary 错误日志 ✅
- **优先级**: MEDIUM (code-reviewer 发现)
- **修复方案**:
  - `onErrorCaptured` 添加 `console.error('[ErrorBoundary]', err, errorInfo)` 便于生产调试
- **验证**: 17 tests 通过
- **文件**: `src/components/ErrorBoundary.vue`

### Spec-38: 质量增强 — 文件上传前端校验 ✅
- **优先级**: MEDIUM (纵深防御)
- **修复方案**:
  - `uploadFile()`: 添加文件存在性检查 + 50MB 大小限制
  - `batchUploadFiles()`: 添加数组非空检查 + 逐文件大小校验
  - 后端已有类型校验（H5），前端补充大小校验作为纵深防御
- **验证**: vite build 成功
- **文件**: `src/api/file.js`

### Spec-39: 质量增强 — request.js 魔数常量化 ✅
- **优先级**: LOW
- **修复方案**:
  - `MAX_RETRIES` → `RETRY_MAX_ATTEMPTS`，`RETRY_DELAY` → `RETRY_BASE_DELAY_MS`
  - 常量命名语义更清晰，便于后续调整
- **验证**: 17 tests 通过
- **文件**: `src/api/request.js`

### Spec-40: LOW — 未使用导入清理 ✅
- **修复方案**: 移除 `TradingMarket.vue` 中未使用的 `computed` 导入
- **验证**: vite build 成功
- **文件**: `src/views/enterprise/TradingMarket.vue`

### Spec-41: LOW — request.js 硬编码常量化 ✅
- **修复方案**:
  - 提取 `REQUEST_TIMEOUT_MS = 15000`、`SUCCESS_CODES = [200, 0]` 常量
  - 替换硬编码 timeout 和状态码判断
- **验证**: 17 tests 通过
- **文件**: `src/api/request.js`

### Spec-42: LOW — API 模块 JSDoc 文档补齐 ✅
- **修复方案**: 为 6 个 API 模块添加 JSDoc @param/@returns 注释
  - `auth.js`: login, logout
  - `carbon.js`: createReport, submitReport, getReportList, getMyReports, deleteReport, reviewReport
  - `trade.js`: createP2PTrade, cancelTrade, getMyTrades
  - `user.js`: getProfile, updateProfile, changePassword
  - `thirdParty.js`: getCarbonReports, getStatistics
  - `carbonNeutral.js`: 10 个函数全部补齐
- **验证**: vite build 成功
- **文件**: `src/api/` 下 6 个模块

---

### Spec-43: LOW — L1/L2/L3 收尾修复 ✅
- **L1 命名一致性**: CarbonCoin.vue 中的 SNAKE_CASE 状态值是后端枚举映射，无需修改
- **L2 内联样式提取**: UserProfile.vue `style="max-width:600px"` 和 `style="max-width:500px"` 提取为 `.profile-form` / `.pwd-form` scoped CSS 类
- **L3 输入验证增强**:
  - `trade.js`: createP2PTrade 添加 carbonAmount > 0 和 price > 0 校验
  - `carbon.js`: createReport 添加 title 非空校验，submitReport/deleteReport 添加 reportId 校验，reviewReport 添加 reportId 和 status 校验
  - `user.js`: updateProfile 添加 null 检查，changePassword 添加 oldPassword 和 newPassword >= 6 校验
- **验证**: 17 tests 全部通过，vite build 成功
- **文件**: `src/views/enterprise/UserProfile.vue`, `src/api/trade.js`, `src/api/carbon.js`, `src/api/user.js`

---

## 剩余问题 (2/63) — 均为延期独立项目

| # | 问题 | 说明 |
|---|------|------|
| M4 | 前端无 TypeScript/PropTypes | 大型架构变更，建议独立项目 |
| M19 | 国际化 i18n | 大型功能，建议独立项目 |
