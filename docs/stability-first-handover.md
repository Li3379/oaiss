# OAISS CHAIN 稳定性优先接手手册

## 目标

这份手册面向第一次正式接手 OAISS CHAIN 的开发者，目标不是先讲完整历史，而是先建立三件事：

1. 这套系统现在是否可启动、可登录、可走通核心业务闭环。
2. 出问题时应该沿哪条主链路排查，而不是在零散页面或脚本之间来回跳。
3. ML、Fabric、发布守卫这些扩展或交付能力，怎样在不误判主系统状态的前提下被验证。

## 系统分层

### 核心必跑层

- `oaiss-chain-frontend/`：Vue 3 + TypeScript 前端
- `oaiss-chain-backend/`：Spring Boot 3.2.5 后端
- `docker-compose.infra.yml`：MySQL、Redis、MinIO、ML 基础设施
- `scripts/health-check.sh`：本地环境和种子数据的统一健康检查

### 扩展能力层

- `oaiss-chain-ml-service/`：预测与推理服务，后端通过 `MlServiceClient` 调用
- `oaiss-chain-chaincode/`：Hyperledger Fabric 链码
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/BlockchainServicePort.java`：区块链接入抽象
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/FabricBlockchainService.java`：Fabric 实现

### 交付保障层

- `.github/workflows/e2e-tests.yml`：CI 验证和守卫
- `.github/workflows/release-images.yml`：镜像发布
- `.github/workflows/deploy-release.yml`：远程部署
- `scripts/validate-prod-env.mjs`：生产和 staging 环境模板校验
- `scripts/closure-audit.mjs`：仓库闭环审计

## 主排障轴

默认沿下面这条主链路理解系统：

1. 前端登录页或业务页发起请求
2. `oaiss-chain-frontend/src/api/request.ts` 注入 JWT、处理刷新令牌、做分页转换
3. `oaiss-chain-backend/.../security/JwtAuthenticationFilter.java` 校验 token 并写入安全上下文
4. `oaiss-chain-backend/.../config/SecurityConfig.java` 处理公开端点、鉴权、CORS 和安全头
5. Controller 上的 `@PreAuthorize` 做角色控制
6. Service 处理业务逻辑、AOP 横切和扩展集成
7. Repository 落库或查询
8. 统一以 `ApiResponse<T>` 返回

优先盯住三个文件：

- `oaiss-chain-frontend/src/api/request.ts`
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java`
- `scripts/health-check.sh`

## 代表性业务流

把“碳报告生命周期”当作默认代表流：

1. 企业创建草稿报告
2. 企业提交报告
3. 审核员审核通过或驳回
4. 管理员认证
5. 认证后的附加副作用触发，例如信用分变更或链上提交

对应现成脚本：

- `scripts/carbon-report-test.sh`

这条业务流覆盖了：

- 登录与权限
- 角色切换
- Controller -> Service -> Repository 主链路
- 数据隔离
- 可选的附加副作用

## 推荐接手顺序

### 第一轮：确认最短闭环

按顺序执行：

1. `bash ./scripts/health-check.sh`
2. `bash ./scripts/login-test.sh`
3. `bash ./scripts/carbon-report-test.sh`

如果在 Windows PowerShell 中执行，可以直接用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stability-baseline.ps1
```

### 第二轮：验证扩展能力不会拖垮主链路

- ML 服务健康检查：`curl http://localhost:8001/health`
- 区块链浏览能力：`bash ./scripts/blockchain-test.sh`

注意：

- `scripts/login-test.sh` 和 `scripts/blockchain-test.sh` 不要并行跑。
- 登录脚本会做登出和 token 黑名单验证；区块链脚本会复用登录 token，并行执行可能造成假阴性。

### 第三轮：验证交付守卫

- `node ./scripts/validate-prod-env.mjs`
- `node ./scripts/validate-prod-env.mjs .env.staging.example`
- `node ./scripts/closure-audit.mjs`

如需串行执行以上步骤，可使用：

```bash
bash ./scripts/stability-baseline.sh --with-ml-health --with-delivery-guards
```

## Profile 使用规则

避免混用下面几组 profile：

| Profile | 用途 |
| --- | --- |
| `local` | 本地开发，默认不开启 Fabric |
| `local,fabric` | 本地开发并启用 Fabric |
| `docker` | CI 或 Compose 场景下的后端运行 |
| `staging,fabric` | 远程 staging 发布 |
| `prod,fabric` | 远程 production 发布 |

如果只是判断“核心系统是否可用”，优先用 `local`。
只有在明确验证区块链链路时，才切到 `local,fabric`。

## 出问题先看哪里

| 症状 | 优先检查 |
| --- | --- |
| 后端登录不可用 | `AuthController`、`AuthService`、`SecurityConfig`、`JwtAuthenticationFilter` |
| 前端已登录但接口 401/403 | `request.ts`、`utils/auth.ts`、后端 `@PreAuthorize`、`SecurityConfig` |
| 企业页面分页异常 | 前端分页转换、后端 `page/size` 入参 |
| 业务接口成功但页面无数据 | 对应 `api/*.ts`、视图层数据映射、响应结构 |
| ML 相关页面超时 | `MlServiceClient` 降级逻辑、ML `/health` |
| Fabric 页面异常 | 当前 profile、Fabric 密钥路径、`FabricBlockchainService` |
| 发布配置疑似回退到本地值 | `validate-prod-env.mjs`、`docker-compose.release.yml`、部署工作流 |

## 建议命令

### 只做最短闭环验证

```bash
bash ./scripts/stability-baseline.sh
```

### 加上 ML 和区块链验证

```bash
bash ./scripts/stability-baseline.sh --with-ml-health --with-blockchain
```

### 加上后后端/前端回归与交付守卫

```bash
bash ./scripts/stability-baseline.sh \
  --with-backend-tests \
  --with-frontend-tests \
  --with-frontend-build \
  --with-delivery-guards
```

## 默认接手原则

- 先判断系统是否可信，再讨论是否扩展。
- 先走主链路，再看边角模块。
- 先复用已有 smoke、CI 和守卫脚本，不平行造第二套标准。
- 改造时优先补测试薄弱点和交付守卫，不先改公共协议。
