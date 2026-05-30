# OAISS CHAIN 部署运行手册

本文描述 OAISS CHAIN 在 `staging` 与 `prod` 环境的推荐发布流程。

## 1. 适用范围

- `staging`：生产等价验证，允许受控种子数据
- `prod`：正式生产环境，禁止 demo/测试数据

运行 profile：

- `staging`
- `staging,fabric`
- `prod`
- `prod,fabric`

## 2. 发布前提

任意部署前需确认：

1. CI 已通过：
   - 生产 env 模板校验
   - 生产 compose dry-run 校验
   - release 镜像构建/发布 workflow
   - 后端安全与配置契约测试
   - ML 共享密钥测试
   - 前端 smoke / E2E 检查
2. 已准备仓库外的完整 env 文件：
   - staging 示例：`C:\secure\oaiss-chain-staging.env`
   - production 示例：`C:\secure\oaiss-chain-prod.env`
   - 仓库模板：`.env.staging.example`、`.env.prod.example`
3. 必要密钥已准备：
   - `DB_PASSWORD`
   - `REDIS_PASSWORD`
   - `JWT_SECRET`
   - `RSA_KEK`
   - `MINIO_ACCESS_KEY`
   - `MINIO_SECRET_KEY`
   - `ML_SERVICE_SECRET`
4. 基础设施端点可达：
   - MySQL
   - Redis
   - 对象存储（MinIO 兼容）
   - 可选 Fabric peer / CA
5. 数据库备份方案已确认。

## 3. 部署前检查

### 3.1 校验环境模板结构

```bash
node scripts/validate-prod-env.mjs
```

该命令用于保证模板完整性，并防止生产配置回退到 localhost。

### 3.2 安全展开 compose 配置

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-prod.env -ComposeArgs config
```

Linux/macOS：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env config
```

应检查：

- `SPRING_PROFILES_ACTIVE` 是否为 `staging` / `staging,fabric` / `prod` / `prod,fabric`
- backend 依赖是否不存在 localhost
- frontend 绑定端口是否符合预期

如果远程主机必须使用预构建镜像（禁止现场 build），请同样校验 `docker-compose.release.yml`：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env --compose-file docker-compose.release.yml config
```

### 3.3 数据库备份

```bash
mysqldump --single-transaction --routines --triggers \
  -h "$DB_HOST" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
  > "backups/oaiss-chain-$(date +%Y%m%d-%H%M%S).sql"
```

## 4. Staging 部署

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-staging.env -ComposeArgs pull
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-staging.env -ComposeArgs up,-d,--build
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-staging.env -ComposeArgs ps
```

Linux/macOS：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-staging.env pull
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-staging.env up -d --build
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-staging.env ps
```

推荐 staging profile：

- `SPRING_PROFILES_ACTIVE=staging`
- 或 `SPRING_PROFILES_ACTIVE=staging,fabric`

## 5. Production 部署

Production 部署必须在远程 staging 演练通过后进行，且演练中至少完成一次回滚 drill。

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-prod.env -ComposeArgs pull
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-prod.env -ComposeArgs up,-d,--build
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile C:\secure\oaiss-chain-prod.env -ComposeArgs ps
```

Linux/macOS：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env pull
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env up -d --build
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env ps
```

image-only 方式：

```bash
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env --compose-file docker-compose.release.yml pull
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env --compose-file docker-compose.release.yml up -d
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env --compose-file docker-compose.release.yml ps
```

推荐 production profile：

- `SPRING_PROFILES_ACTIVE=prod`
- 或 `SPRING_PROFILES_ACTIVE=prod,fabric`

## 6. 部署后验证

staging 或 production 发布后都应执行：

```bash
curl -f http://127.0.0.1/health
curl -f http://127.0.0.1/api/v1/actuator/health
```

应用层检查项：

1. backend 健康状态为 `UP`
2. frontend `/health` 返回 `ok`
3. ML `/health` 正常
4. 目标环境的非 demo 账号可登录
5. production 不暴露 Swagger
6. 监控看板与告警可达
7. 如启用 Fabric，区块链状态接口行为正确

## 6.1 GitHub Actions 远程发布

仓库包含两个发布 workflow：

1. `.github/workflows/release-images.yml`
   - 发布 backend/frontend/ml-service 镜像到 GHCR
2. `.github/workflows/deploy-release.yml`
   - 通过 SSH 将 `docker-compose.release.yml` 部署到远程主机
   - 支持可选镜像覆盖参数（用于推进新版本或回滚旧版本）

`deploy-release.yml` 推荐配置的 GitHub Environment secrets：

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_TARGET_DIR`
- `DEPLOY_ENV_FILE`
- `FRONTEND_HEALTHCHECK_URL`
- `BACKEND_HEALTHCHECK_URL`

相关文档：

- `docs/github-actions-deploy-secrets.md`
- `docs/github-environment-secrets-template.md`
- `docs/staging-github-secrets-fillout.md`
- `docs/production-github-secrets-fillout.md`
- `docs/remote-staging-rehearsal.md`
- `docs/production-observation-window.md`
- `docs/remote-host-preflight-checklist.md`
- `docs/go-live-gate-matrix.md`

## 7. Canary 与观察窗口

production 发布后重点观察：

1. 错误率
2. p95 / p99 延迟
3. 登录成功率
4. 报告提交流程成功率
5. ML 推理成功率

观察窗口期间请保留上一版本镜像 tag 与 env 文件，便于快速回滚。

## 8. 回滚

触发回滚时：

1. 切换到上一版稳定镜像（或指定旧镜像 tag）
2. 使用同一套包装脚本重新执行 `up -d`
3. 重新检查健康状态
4. 若数据库迁移已执行，采用前向修复迁移，禁止回写历史迁移

示例：

```bash
export IMAGE_TAG=<previous-known-good-sha>
./scripts/prod-compose.sh --env-file /secure/path/oaiss-chain-prod.env up -d
curl -f http://127.0.0.1/api/v1/actuator/health
```

## 9. 运维注意事项

- 不要复用本地 `.env` 到 staging/production
- 不要在已注入本地开发变量的 shell 里直接执行原始 `docker compose -f docker-compose.prod.yml ...`
- 优先使用包装脚本，避免进程级环境变量污染
