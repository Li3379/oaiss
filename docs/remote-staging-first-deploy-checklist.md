# 远程 Staging 首次上线操作单

本操作单面向第一次把 OAISS CHAIN 从本机开发环境推进到远程 `staging,fabric` 环境。

目标是让执行人员按顺序完成，不需要在多份文档之间来回拼接步骤。

## 1. 执行前确认

- [ ] 本地 `local,fabric` 验收已通过
- [ ] 已阅读 `docs/remote-host-preflight-checklist.md`
- [ ] 已阅读 `docs/staging-github-secrets-fillout.md`
- [ ] 已确认目标是远程 `staging`，不是 `production`

## 2. 远程主机初始化

在远程 Linux 主机执行：

```bash
sudo ./scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-staging --deploy-user deploy
```

执行后确认以下目录存在：

- [ ] `/opt/oaiss-chain-staging/backups`
- [ ] `/opt/oaiss-chain-staging/scripts`
- [ ] `/opt/oaiss-chain-staging/runtime-logs/backend`
- [ ] `/opt/oaiss-chain-staging/runtime-logs/frontend`
- [ ] `/opt/oaiss-chain-staging/runtime-logs/ml-service`
- [ ] `/opt/oaiss-chain-staging/secrets/fabric`

## 3. 放置真实 Fabric 文件

将真实文件放入：

`/opt/oaiss-chain-staging/secrets/fabric`

要求至少包含：

- [ ] `peer-tls-ca.crt`
- [ ] `user-cert.pem`
- [ ] `user-key.pem`

## 4. 填写 GitHub Environment `staging`

在 GitHub 中创建或更新 Environment：

- [ ] `staging`

按 [staging-github-secrets-fillout.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/staging-github-secrets-fillout.md) 填写：

- [ ] `DEPLOY_HOST`
- [ ] `DEPLOY_PORT`
- [ ] `DEPLOY_USER`
- [ ] `DEPLOY_SSH_KEY`
- [ ] `DEPLOY_TARGET_DIR`
- [ ] `DEPLOY_ENV_FILE`
- [ ] `FRONTEND_HEALTHCHECK_URL`
- [ ] `BACKEND_HEALTHCHECK_URL`

若远程主机拉私有镜像需要登录仓库：

- [ ] `REGISTRY_HOST`
- [ ] `REGISTRY_USERNAME`
- [ ] `REGISTRY_PASSWORD`

## 5. 准备 `DEPLOY_ENV_FILE`

必须至少确认以下值已经填写为真实值：

- [ ] `SPRING_PROFILES_ACTIVE=staging,fabric`
- [ ] `BACKEND_IMAGE`
- [ ] `FRONTEND_IMAGE`
- [ ] `ML_SERVICE_IMAGE`
- [ ] `DB_URL`
- [ ] `DB_USERNAME`
- [ ] `DB_PASSWORD`
- [ ] `REDIS_HOST`
- [ ] `REDIS_PASSWORD`
- [ ] `MINIO_ENDPOINT`
- [ ] `MINIO_ACCESS_KEY`
- [ ] `MINIO_SECRET_KEY`
- [ ] `JWT_SECRET`
- [ ] `RSA_KEK`
- [ ] `ML_SERVICE_SECRET`
- [ ] `BACKEND_LOG_DIR=./runtime-logs/backend`
- [ ] `FRONTEND_LOG_DIR=./runtime-logs/frontend`
- [ ] `ML_LOG_DIR=./runtime-logs/ml-service`
- [ ] `FABRIC_ENABLED=true`
- [ ] `FABRIC_SECRETS_DIR=./secrets/fabric`
- [ ] `FABRIC_SECRETS_MOUNT_PATH=/run/secrets/fabric`
- [ ] `FABRIC_PEER_TLS_CERT_PATH=/run/secrets/fabric/peer-tls-ca.crt`
- [ ] `FABRIC_CERT_PATH=/run/secrets/fabric/user-cert.pem`
- [ ] `FABRIC_KEY_PATH=/run/secrets/fabric/user-key.pem`

## 6. 本地模板干跑

执行：

```powershell
node scripts/validate-prod-env.mjs .env.staging.example
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile .env.staging.example -ComposeFile docker-compose.release.yml -ComposeArgs config
```

确认：

- [ ] 校验脚本通过
- [ ] release compose 可成功展开
- [ ] 展开结果包含 `SPRING_PROFILES_ACTIVE: staging,fabric`
- [ ] 展开结果包含 `/run/secrets/fabric`
- [ ] 展开结果包含 `/app/logs`

## 7. 发布镜像

在 GitHub Actions 运行：

- [ ] `.github/workflows/release-images.yml`

记录实际发布结果：

- [ ] backend image tag
- [ ] frontend image tag
- [ ] ml-service image tag

## 8. 远程部署

在 GitHub Actions 运行：

- [ ] `.github/workflows/deploy-release.yml`

参数要求：

- [ ] `environment=staging`

若要部署一次性候选镜像，可附加：

- [ ] `backend_image`
- [ ] `frontend_image`
- [ ] `ml_service_image`

## 9. 部署后验收

至少完成以下检查：

- [ ] 前端 `/health` 返回成功
- [ ] 后端 `/api/v1/actuator/health` 返回 `UP`
- [ ] ML `/health` 正常
- [ ] staging 非演示账号可登录
- [ ] 至少一条企业侧真实业务链路跑通
- [ ] 至少一条依赖 ML 的业务链路跑通
- [ ] 区块链状态接口正常
- [ ] `runtime-logs` 下已有实际日志输出

## 10. 回滚演练

在 staging 宣告就绪前必须完成一次：

- [ ] 使用旧镜像重新运行 `deploy-release.yml`
- [ ] 或使用 workflow 输入覆盖到旧镜像
- [ ] 回滚后前后端健康检查再次通过
- [ ] 回滚后至少一条核心业务流程仍成功

## 11. 完成标准

只有以下全部满足，才算远程 staging 首次上线完成：

- [ ] 远程主机初始化完成
- [ ] GitHub `staging` secrets 完成
- [ ] 发布镜像成功
- [ ] 远程部署成功
- [ ] 业务验收通过
- [ ] 回滚演练通过

## 12. 下一步

完成后进入：

- [production-github-secrets-fillout.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/production-github-secrets-fillout.md)
- [deployment-runbook.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/deployment-runbook.md)
- [production-observation-window.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/production-observation-window.md)
- [external-execution-evidence-template.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/external-execution-evidence-template.md)
