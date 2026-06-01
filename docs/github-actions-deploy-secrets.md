# GitHub Actions 部署密钥说明

本文档列出 `.github/workflows/deploy-release.yml` 所需的 GitHub Environment Secrets。

如需按项填写、并查看每个密钥通常应从哪里取得，请同时参考：

- `docs/github-environment-secrets-template.md`
- `docs/staging-github-secrets-fillout.md`
- `docs/production-github-secrets-fillout.md`

## 必填密钥

| 密钥 | 用途 | 示例 |
|---|---|---|
| `DEPLOY_HOST` | 远程部署主机地址 | `staging-app-01.example.com` |
| `DEPLOY_PORT` | SSH 端口 | `22` |
| `DEPLOY_USER` | SSH 登录用户 | `deploy` |
| `DEPLOY_SSH_KEY` | GitHub Actions 使用的私钥 | OpenSSH 私钥 |
| `DEPLOY_TARGET_DIR` | 远端用于存放 Compose 文件与环境文件的目录 | `/opt/oaiss-chain` |
| `DEPLOY_ENV_FILE` | 部署时上传的完整环境文件内容 | `oaiss-chain.env` 的完整内容 |
| `FRONTEND_HEALTHCHECK_URL` | 前端健康检查地址，可为公网或内网 | `https://staging.example.com/health` |
| `BACKEND_HEALTHCHECK_URL` | 后端 actuator 健康检查地址 | `https://staging.example.com/api/v1/actuator/health` |

## 可选镜像仓库密钥

当远程主机在拉取镜像前必须先登录私有镜像仓库时，需要配置以下变量，例如私有 GHCR 包。

| 密钥 | 用途 | 示例 |
|---|---|---|
| `REGISTRY_HOST` | `docker login` 使用的镜像仓库域名 | `ghcr.io` |
| `REGISTRY_USERNAME` | 镜像仓库用户名 | `oaiss-deploy-bot` |
| `REGISTRY_PASSWORD` | 镜像仓库令牌或密码 | GHCR personal access token |

## 说明

- `DEPLOY_ENV_FILE` 应包含完整环境文件内容，至少覆盖以下配置：
  - `BACKEND_IMAGE`
  - `FRONTEND_IMAGE`
  - `ML_SERVICE_IMAGE`
  - 数据库、Redis、MinIO、JWT、RSA、ML 相关设置
  - `BACKEND_LOG_DIR`、`FRONTEND_LOG_DIR`、`ML_LOG_DIR`
  - 启用真实区块链时的 `SPRING_PROFILES_ACTIVE=staging,fabric` 或 `prod,fabric`
  - `FABRIC_SECRETS_DIR`、`FABRIC_SECRETS_MOUNT_PATH`
  - `FABRIC_PEER_TLS_CERT_PATH`、`FABRIC_CERT_PATH`、`FABRIC_KEY_PATH`
  - 如 `REQUIRE_OPS_SECRETS=true`：`GRAFANA_ADMIN_PASSWORD`
  - 如配置 `ALERT_SMTP_HOST`：`ALERT_SMTP_PASSWORD`
  - 如配置 `ALERT_WEBHOOK_URL`：`ALERT_WEBHOOK_SECRET`
  - 如 `FABRIC_ENABLED=true`：`FABRIC_COUCHDB_PASSWORD`
  - 如 `FABRIC_CA_ENABLED=true`：`FABRIC_CA_ADMIN_PASSWORD`
- `deploy-release.yml` 支持以下可选工作流输入：
  - `backend_image`
  - `frontend_image`
  - `ml_service_image`
  这些值会覆盖 `DEPLOY_ENV_FILE` 中对应的镜像地址，适用于受控发布新标签或快速回滚到已知稳定版本。
- 当 `REGISTRY_HOST`、`REGISTRY_USERNAME`、`REGISTRY_PASSWORD` 三项同时存在时，工作流会在远端主机执行 `pull` 前先执行 `docker login`。
- 工作流会先在本地渲染 `DEPLOY_ENV_FILE`，再执行 `node scripts/validate-prod-env.mjs --require-real-secrets`；占位符镜像、关键业务密钥，以及按功能启用而要求存在的运维 / Fabric 密钥会在上传前被拒绝。
- 工作流现在会在部署前自动备份远端 `oaiss-chain.env` 和 `docker-compose.release.yml`，并在健康检查失败时抓取远程 `ps/logs` 后尝试自动回滚。
- 建议为 `staging` 与 `production` 分别创建独立的 GitHub Environment。
- 使用最小权限原则创建独立部署密钥。
- SSH 密钥与应用密钥应分别轮换，不要绑定为同一变更流程。
- 使用 `docker-compose.release.yml` 时，远程主机不需要仓库源码，只需要 Docker、Compose 包装脚本，以及访问镜像仓库和基础设施端点的网络权限。
- 首次上机前建议在远程主机执行：
  - `scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-staging --deploy-user deploy`
  - 或 `scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-prod --deploy-user deploy`
