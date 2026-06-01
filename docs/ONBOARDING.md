# OAISS CHAIN 开发者入门指南

## 概览

OAISS CHAIN（双碳链动系统）是一个基于区块链的可信碳核算与碳交易平台。企业提交碳排放报告，审核员进行审核，第三方执行监管，管理员负责系统管理。系统支持碳币交易、双向撮合、P2P 交易、碳中和项目等核心业务。

## 技术栈

| 层级 | 技术 | 版本 |
|-------|-----------|---------|
| 后端语言 | Java | 17 |
| 后端框架 | Spring Boot | 3.2.5 |
| ORM | Spring Data JPA (Hibernate) | - |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7 (Lettuce) |
| 对象存储 | MinIO | latest |
| 认证 | JWT (jjwt) | 0.12.5 |
| 数据库迁移 | Flyway | - |
| API 文档 | SpringDoc OpenAPI | 2.5 |
| 构建工具 | Maven | - |
| 前端语言 | TypeScript | 6 |
| 前端框架 | Vue | 3.5 |
| UI 组件库 | Element Plus | 2.13 |
| 状态管理 | Pinia | 3 |
| 路由 | Vue Router | 5 |
| 图表 | ECharts | 6 |
| 国际化 | vue-i18n | 11 |
| 构建工具 | Vite | 8 |
| 前端单测 | Vitest (happy-dom) | 4.1 |
| E2E 测试 | Playwright | 1.59 |
| 基础设施 | Docker Compose | 3.8 |

## 架构

```text
Vue 3 SPA (Vite/TS)
    |
    | /api/v1/*
    v
Spring Boot API
    |
    +-- MySQL 8
    +-- Redis 7
    +-- MinIO
    +-- Fabric / ML（按 profile 启用）
```

- 采用 **单体后端 + SPA 前端** 的组合，并通过 Docker Compose 编排。
- API 风格为 **REST**，使用 JWT Bearer Token 认证，并配合 CSRF cookie。
- 前端开发环境会把 `/api` 代理到 `localhost:8080`（Vite proxy）。

## 用户角色

| 角色 | 中文名 | 首页路由 | 说明 |
|------|---------|-----------|-------------|
| `ENTERPRISE` | 企业 | `/enterprise/carbon/upload` | 提交碳报告、参与交易、查看碳币与项目数据 |
| `REVIEWER` | 审核员 | `/auditor/audit/list` | 审核碳排放与相关业务材料 |
| `THIRD_PARTY` | 第三方 | `/third-party/monitor` | 查看监管与监测面板 |
| `ADMIN` | 管理员 | `/admin/system/users` | 用户管理、系统配置、平台治理 |

## 关键入口

| 内容 | 位置 |
|------|-------|
| 后端启动类 | `oaiss-chain-backend/.../OaissChainApplication.java` |
| API 根路径 | `server.servlet.context-path: /api/v1`（`application.yml`） |
| 安全配置 | `config/SecurityConfig.java` |
| JWT 过滤器 | `security/JwtAuthenticationFilter.java` |
| 前端入口 | `oaiss-chain-frontend/src/main.ts` |
| 路由配置 | `src/router/index.ts`（带角色守卫） |
| API 客户端 | `src/api/request.ts`（axios + token refresh） |
| 认证工具 | `src/utils/auth.ts`（token 存储 + JWT 解析） |
| 状态管理 | `src/store/index.ts`（Pinia，从 JWT 初始化用户状态） |
| 数据库结构 | `src/main/resources/db/migration/V1__init_schema.sql` |

## 目录地图

### 后端目录（`oaiss-chain-backend/src/main/java/com/oaiss/chain/`）

| 目录 | 作用 | 数量 |
|-----------|---------|-------|
| `controller/` | REST 接口层 | 16 |
| `service/` | 业务逻辑层 | 23 |
| `repository/` | Spring Data JPA 接口 | 22 |
| `entity/` | JPA 实体（大量使用 Lombok `@Data`） | 21 |
| `dto/` | 请求/响应 DTO | 30+ |
| `config/` | Spring 配置（安全、Redis、MinIO、Swagger 等） | 14 |
| `annotation/` | 自定义注解 | 5 |
| `aop/` | 注解对应的 AOP 切面 | 5 |
| `security/` | JWT 过滤器、token provider、handler 等 | 6 |
| `enums/` | 业务枚举 | 8 |
| `constant/` | 错误码与错误信息常量 | 2 |
| `exception/` | 自定义异常与全局异常处理 | 7 |
| `util/` | 工具类 | 4 |

### 前端目录（`oaiss-chain-frontend/src/`）

| 目录 | 作用 | 数量 |
|-----------|---------|-------|
| `api/` | 按业务域拆分的 Axios API 客户端 | 17 |
| `types/` | TypeScript 类型定义 | 16 |
| `views/` | 页面组件（按角色拆分） | 20+ |
| `views/__tests__/` | 就地 Vitest 单元测试 | 20+ |
| `store/` | Pinia 状态管理（当前为单一 `app` store） | 1 |
| `router/` | 带角色守卫的 Vue Router | 1 |
| `i18n/` | `zh-CN` + `en-US` 国际化资源 | 3 |
| `components/` | 公共组件 | 3 |
| `layout/` | 应用壳层（侧边栏 + 顶栏） | 1 |
| `config/` | 菜单配置、图片配置 | 2 |
| `utils/` | 认证与 ECharts 工具 | 2 |

## 请求生命周期

以“创建碳报告”为例：

```text
1. Vue 组件调用 api/carbon.ts -> 发起 axios 请求
2. request.ts 拦截器附加 JWT Bearer token
   - token 过期时，自动通过 /auth/refresh 刷新
   - 把 pageNum/pageSize 转成 page/size
3. Vite 代理转发到 http://localhost:8080/api/v1
4. Spring Security 过滤链执行：
   JwtAuthenticationFilter -> 校验 token -> 写入 SecurityContext
5. @PreAuthorize("hasRole('ENTERPRISE')") 进行角色检查
6. CarbonController.createReport()
   -> CarbonService 处理业务逻辑
      -> CarbonReportRepository 通过 Spring Data JPA 落库 MySQL
7. 响应统一用 ApiResponse<T> 包装
8. request.ts 响应拦截器：
   - 把 Spring Data Page 转为 { items, total, page, size, totalPages }
   - 错误码映射为 ElMessage.error()
```

## 横切关注点（AOP）

系统中有 5 个自定义注解及其对应切面：

| 注解 | 作用 | 机制 |
|------------|---------|-----------|
| `@AuditLog` | 操作审计日志 | AOP 记录到 `OperationLog` 表 |
| `@RateLimit` | 请求限流 | 基于 Redis 计数 |
| `@RequirePermission` | 细粒度权限控制 | 权限表校验 |
| `@DataIsolation` | 租户数据隔离 | 通过企业 ID 过滤 |
| `@DistributedLock` | 分布式锁 | 基于 Redis `SETNX` |

## 约定

### 命名约定

| 对象 | 规则 | 示例 |
|------|---------|---------|
| Java 类名 | PascalCase | `CarbonReportService` |
| Java 方法/字段 | camelCase | `createReport()` |
| API 路径 | kebab-case | `/carbon-coin/account` |
| Vue 组件 | PascalCase | `CarbonUpload.vue` |
| TS 文件 | camelCase | `carbonCoin.ts` |
| 页面目录 | kebab-case | `views/enterprise/`, `views/third-party/` |
| 测试文件 | `*.test.ts` | `Login.test.ts` |
| 测试位置 | `__tests__/` 就地放置 | `views/__tests__/Login.test.ts` |

### 代码模式

- **后端**：实体和 DTO 中广泛使用 Lombok `@Data` / `@Builder` / `@RequiredArgsConstructor`
- **后端**：主要使用构造器注入，不使用 `@Autowired`
- **后端**：所有接口响应统一使用 `ApiResponse<T>` 静态工厂方法
- **后端**：控制器方法通过 `@PreAuthorize` 进行角色控制
- **后端**：控制器方法都带 Swagger 注解
- **前端**：Pinia Options API 风格，当前为单一 `app` store
- **前端**：路由默认采用 `() => import()` 懒加载
- **前端**：token 保存在 localStorage/sessionStorage + 内存缓存
- **前端**：客户端会解析 JWT payload 获取 role/userId

### Git 约定

- **分支命名**：`claude/<adjective>-<name>-<hash>`（AI 生成分支命名规则）
- **提交格式**：Conventional Commits，例如 `type(scope): description`
  - 可用类型：`feat`、`fix`、`docs`、`test`、`refactor`、`chore`、`perf`、`ci`
- **PR 策略**：合并到主分支时使用 squash merge

## 常用任务

| 任务 | 命令 |
|------|---------|
| 启动后端开发服务 | `scripts/start-backend.bat` / `./scripts/start-backend.sh` |
| 停止后端开发服务 | `scripts/stop-backend.bat` / `./scripts/stop-backend.sh` |
| 启动前端开发服务 | `cd oaiss-chain-frontend && npm run dev` |
| 运行后端测试 | `cd oaiss-chain-backend && mvn test` |
| 运行后端集成测试 | `cd oaiss-chain-backend && mvn verify` |
| 运行前端单元测试 | `cd oaiss-chain-frontend && npm run test` |
| 运行前端 E2E 测试 | `cd oaiss-chain-frontend && npm run test:e2e` |
| 构建前端生产包 | `cd oaiss-chain-frontend && npm run build` |
| 启动整套栈 | `docker compose up` |
| 查看 Swagger API 文档 | `http://localhost:8080/api/v1/swagger-ui.html` |
| 查看 MinIO 控制台 | `http://localhost:9003` |

## 环境准备

1. 将 `.env.example` 复制为 `.env` 并填写真实值
2. 关键变量包括：`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`RSA_KEK`
3. 后端默认运行在 **8080**，前端开发服务默认运行在 **5173**
4. MySQL 默认 **3306**，Redis 默认 **6379**，MinIO API 默认 **9002**，控制台默认 **9003**
5. 如使用 `local,fabric`，优先使用 `scripts/start-all.bat --with-fabric` 或 `./scripts/start-all.sh --with-fabric`，确保 Spring Boot 启动前已经注入 `.env` 变量

## 遇到问题时看哪里

| 我想做什么 | 建议查看 |
|--------------|-----------|
| 新增 REST 接口 | `controller/` -> 对应 `service/` -> `repository/` |
| 新增实体/表 | `entity/` + `db/migration/` 中的 Flyway 脚本 |
| 修改认证行为 | `security/` + `config/SecurityConfig.java` |
| 新增前端页面 | `views/<role>/` + `router/index.ts` + `api/` |
| 新增 API 客户端 | `api/<domain>.ts` + `types/<domain>.ts` |
| 修角色/权限问题 | 后端看 `@PreAuthorize`，前端看路由 `meta.roles` |
| 增加 i18n 文案 | `i18n/locales/zh-CN.ts` + `en-US.ts` |
| 排查 JWT 问题 | `security/JwtTokenProvider.java` + `utils/auth.ts` |
| 理解数据库结构 | `db/migration/V1__init_schema.sql` |
| 新增自定义注解 | `annotation/` + `aop/` 中对应切面 |
| 查看 API 文档 | Swagger UI：`/api/v1/swagger-ui.html` |
