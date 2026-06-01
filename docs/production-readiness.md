# OAISS CHAIN 生产就绪说明

本文用于记录 OAISS CHAIN 从开发环境（Dev）走向生产环境（Prod）的约束与完成标准。

## 当前基线说明

- 当前运行时角色基线为：`ENTERPRISE / REVIEWER / THIRD_PARTY / ADMIN`。
- 旧版需求文档里出现的 `authenticator` 属于历史命名，不应作为当前可登录角色。
- 开发机启用 Fabric 时推荐使用 `local,fabric`；远程 staging / production 发布必须使用 `staging,fabric` 或 `prod,fabric`。

## 环境 Profile 说明

| Profile | 用途 | 数据策略 |
|---|---|---|
| `local` | 开发者本机（本地或 Docker 依赖） | 仅本地数据 |
| `dev` | 共享开发/联调用环境 | 可销毁测试数据 |
| `staging` | 生产等价验证环境 | 允许 staging 种子数据 |
| `prod` | 正式生产运行环境 | 禁止 demo/测试数据 |

`docker` 视为本地容器 profile。远程 staging / production 发布必须使用 `SPRING_PROFILES_ACTIVE=staging,fabric` 或 `prod,fabric`，避免回退到非 Fabric 链路。

## 生产配置规则

- 生产环境必须使用 `application-prod.yml`。
- 生产依赖配置中禁止 `localhost` 回退值。
- 生产 MySQL 必须使用启用 TLS 的 `DB_URL`。
- 保持 `spring.jpa.hibernate.ddl-auto=validate`。
- 保持 Flyway `validate-on-migrate=true` 且 `repair-on-migrate=false`。
- 生产环境禁用 Swagger/OpenAPI。
- `CORS_ALLOWED_ORIGINS` 仅允许真实前端域名。
- 生产必须配置非占位符的 `ML_SERVICE_SECRET`，并保证 backend 与 ML 服务一致。
- 已填充的敏感信息必须放在仓库外或密钥管理系统中。

## 种子数据策略

Flyway 迁移文件一旦在共享环境执行，不得直接修改历史迁移内容。

生产环境种子数据规则：

- 允许：建表/结构数据、字典/权限目录等系统启动必需数据。
- 禁止：demo 用户、测试企业、示例碳报告、示例交易、示例证书、QA 临时记录。
- 仅用于 staging 的数据应放在生产 Flyway 路径之外，例如 `db/seed/staging`。

生产上线前应审计 `V2__seed_data.sql`，将每一行标记为 `reference` 或 `staging-demo`。  
所有 `staging-demo` 数据应通过“前向修复迁移”或“仅 staging 的种子脚本”移出生产路径。

后端也已在 `prod` / `staging` 启动时执行校验：如果存在 `enterprise001`、`enterprise002`、`enterprise003`、`reviewer001`、`thirdparty001` 这类 demo 用户，启动将失败。

## 验证命令

当前已在本地完成的重点探针：

- 后端生产安全/配置闸门：
  `SecurityStartupValidatorTest`、`CorsAllowedOriginsConfigTest`、`ProductionDataPolicyValidatorTest`、`MlServiceConfigTest`
- ML 共享密钥闸门：
  `python -m unittest oaiss-chain-ml-service.tests.test_security`
- 本地 Fabric 路径健康验证：
  `BlockchainProfileTest`、`FabricGatewayConfigTest`、`FabricCAServiceTest`、`FabricProfileIntegrationTest`

后端安全重点验证：

```bash
cd oaiss-chain-backend
mvn test -Dtest=SecurityStartupValidatorTest,AuthControllerTest
```

后端全量验证：

```bash
cd oaiss-chain-backend
mvn test
mvn verify
```

前端验证：

```bash
cd oaiss-chain-frontend
npm ci
npm run test
npm run build
npm run test:e2e
```

ML 服务健康检查：

```bash
cd oaiss-chain-ml-service
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
curl -f http://127.0.0.1:8001/health
```

ML 共享密钥验证：

```bash
curl -i -X POST http://127.0.0.1:8001/predict/market/trend \
  -H "Content-Type: application/json" \
  -H "X-ML-Service-Secret: $ML_SERVICE_SECRET" \
  -d '{"dates":["2025-01-01","2025-01-02"],"prices":[50,51],"volumes":[100,110],"horizon_days":2}'
```

链码验证：

```bash
cd oaiss-chain-chaincode
go test ./...
```

## 生产发布检查清单

- `prod,fabric` profile 已使用强密钥与真实生产域名配置。
- 在生产等价的 staging 数据副本上，Flyway 迁移校验通过。
- 数据库备份与恢复流程已实测可用。
- 公共认证入口已启用限流。
- backend->ML 调用要求共享密钥，ML 预测接口对错误/缺失 `X-ML-Service-Secret` 请求应拒绝。
- 生产环境 Swagger/OpenAPI 已禁用。
- HTTPS 已在反向代理或负载均衡层启用。
- MySQL、Redis、MinIO 控制台、Prometheus、Grafana、Fabric 节点未直接暴露公网。
- backend、frontend、ML、DB、Redis、MinIO、Fabric 的健康检查均通过。
- Prometheus/Grafana 看板与 Alertmanager 通知已验证。
- Canary 观察具备明确的错误率、延迟、业务成功率回滚阈值。

相关文档：

- `docs/production-observation-window.md`
- `docs/remote-host-preflight-checklist.md`
- `docs/go-live-gate-matrix.md`

## Docker Compose 生产部署

生产 compose 文件默认只编排应用服务：

- `frontend`：公网 HTTP 入口与 `/api/v1` 反向代理
- `backend`：私有 Docker 网络中的 Spring Boot API
- `ml-service`：私有 Docker 网络中的 FastAPI ML 服务

若远程主机禁止“现场源码构建”，请使用 `docker-compose.release.yml`。  
该文件是 image-only 模式，要求 `BACKEND_IMAGE`、`FRONTEND_IMAGE`、`ML_SERVICE_IMAGE` 指向已发布镜像。

MySQL、Redis、对象存储应为托管服务或私有基础设施端点，并通过 `.env` 注入。生产环境不要暴露数据库、Redis 或 MinIO 控制台端口。  
如果采用此 compose 一体化部署，建议设置 `ML_SERVICE_URL=http://ml-service:8001`，便于 backend 解析内部服务名。

示例部署：

```bash
cp .env.prod.example /secure/path/oaiss-chain.env
# 填写 /secure/path/oaiss-chain.env 的真实值
COMPOSE_DISABLE_ENV_FILE=1 docker compose --env-file /secure/path/oaiss-chain.env -f docker-compose.prod.yml pull
COMPOSE_DISABLE_ENV_FILE=1 docker compose --env-file /secure/path/oaiss-chain.env -f docker-compose.prod.yml up -d --build
COMPOSE_DISABLE_ENV_FILE=1 docker compose --env-file /secure/path/oaiss-chain.env -f docker-compose.prod.yml ps
```

远程 image-only 示例：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env --compose-file docker-compose.release.yml pull
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env --compose-file docker-compose.release.yml up -d
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env --compose-file docker-compose.release.yml ps
```

注意：如果仓库根目录存在本地 `.env`，Docker Compose 会隐式读取，可能覆盖你本来期望从生产 env 文件读取的值。生产执行时请显式设置 `COMPOSE_DISABLE_ENV_FILE=1`。

Windows（已有本地变量污染时）建议使用包装脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain.env -ComposeArgs pull
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain.env -ComposeArgs up,-d,--build
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain.env -ComposeArgs ps
```

Linux/macOS 同理：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env pull
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env up -d --build
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain.env ps
```

如果你在 Windows 的 WSL 中执行 `prod-compose.sh`，请确保 Docker Desktop 已开启该发行版的 WSL 集成，否则脚本会在 compose 启动前因 `docker` 不可用而失败。

仓库提供了静态模板校验脚本：

```bash
node scripts/validate-prod-env.mjs
```

该校验用于保证模板完整性并防止生产配置回退到 `localhost`。  
由于 `.env.prod.example` 需要可安全入库，占位密钥会以 warning（而非 error）方式提示。

对真实发布环境文件，应使用严格模式：

```bash
node scripts/validate-prod-env.mjs --require-real-secrets /secure/path/oaiss-chain.env
```

严格模式会拒绝占位符镜像、关键业务密钥，以及按启用状态必须存在的运维 / Fabric 密钥。

CI/CD 目前分两层：

- `.github/workflows/release-images.yml`：构建并发布 backend/frontend/ml-service 镜像到 GHCR
- `.github/workflows/deploy-release.yml`：上传 `docker-compose.release.yml` 与 compose 包装脚本到远程主机，并执行 image-only 发布

健康检查：

```bash
curl -f http://127.0.0.1/health
curl -f http://127.0.0.1/api/v1/actuator/health
```

如果 TLS 终止在此 compose 之外，请在 LB 或 Caddy/Nginx 侧确保转发 `X-Forwarded-Proto` 与 `X-Forwarded-For`。

## 回滚策略

- 应用回滚基于“上一版本镜像 tag”。
- 数据库回滚遵循前向修复原则：禁止改历史迁移，需新增修复迁移。
- 生产迁移前必须先做数据库备份。
- 回滚时应保留上一版 env 文件与镜像 tag。

回滚示例：

```bash
export IMAGE_TAG=<previous-known-good-sha>
COMPOSE_DISABLE_ENV_FILE=1 docker compose --env-file /secure/path/oaiss-chain.env -f docker-compose.prod.yml up -d
curl -f http://127.0.0.1/api/v1/actuator/health
```

迁移前备份示例：

```bash
mysqldump --single-transaction --routines --triggers \
  -h "$DB_HOST" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
  > "backups/oaiss-chain-$(date +%Y%m%d-%H%M%S).sql"
```

若迁移已在生产执行，禁止直接改原迁移文件；应新增前向修复迁移并重新发布应用镜像。

分阶段上线流程请参见 `docs/deployment-runbook.md`。  
本地 `local,fabric` 最终验收快照请参见 `docs/final-acceptance-checklist.md`。
