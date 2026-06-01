# 远程 Staging 演练手册

本文档是 OAISS CHAIN 首次基于已发布镜像、使用 `docker-compose.release.yml` 进行远程 `staging` 发布的操作手册。

## 目标

证明 OAISS CHAIN 可以在远程主机上部署运行，而无需在该主机上基于仓库源码现场构建，同时使用真实基础设施端点与适合 staging 的真实数据。

## 前置条件

1. GitHub Actions 工作流已经存在：
   - `.github/workflows/release-images.yml`
   - `.github/workflows/deploy-release.yml`
2. GitHub Environment `staging` 已按下列文档完成密钥配置：
   - `docs/github-actions-deploy-secrets.md`
   - 如果 GHCR 包是私有的，还需补齐镜像仓库登录密钥
3. 远程主机已具备以下条件：
   - Docker Engine / Docker Compose
   - 可出站访问 GHCR
   - 可出站访问 MySQL、Redis、对象存储，以及可选的 Fabric 端点
4. 已基于以下模板准备好完整环境文件：
   - `.env.staging.example`
5. 主机层准备状态已按以下清单核查：
   - `docs/remote-host-preflight-checklist.md`
6. 远程主机已执行初始化脚本：
   - `scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-staging --deploy-user deploy`
7. 真实 Fabric 文件已放入：
   - `/opt/oaiss-chain-staging/secrets/fabric`

## 步骤 1：本地校验模板

```bash
node scripts/validate-prod-env.mjs .env.staging.example
./scripts/prod-compose.sh --env-file .env.staging.example --compose-file docker-compose.release.yml config
```

Windows PowerShell：

```powershell
node scripts/validate-prod-env.mjs .env.staging.example
powershell -ExecutionPolicy Bypass -File .\scripts\prod-compose.ps1 -EnvFile .env.staging.example -ComposeFile docker-compose.release.yml -ComposeArgs config
```

## 步骤 2：发布 release 镜像

从 GitHub Actions 运行 `release-images.yml`。

建议输入：

- `image_tag`：候选发布标签或 commit SHA
- `push_latest`：staging 演练时建议设为 `false`

记录发布后的镜像地址：

- backend
- frontend
- ml-service

## 步骤 3：更新 staging 环境文件内容

在填写完成的 staging 环境文件中设置以下值：

- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `ML_SERVICE_IMAGE`

这三个值都应指向步骤 2 中实际发布成功的镜像。

并确认以下值已经填写正确：

- `SPRING_PROFILES_ACTIVE=staging,fabric`
- `FABRIC_ENABLED=true`
- `BACKEND_LOG_DIR=./runtime-logs/backend`
- `FRONTEND_LOG_DIR=./runtime-logs/frontend`
- `ML_LOG_DIR=./runtime-logs/ml-service`
- `FABRIC_SECRETS_DIR=./secrets/fabric`
- `FABRIC_SECRETS_MOUNT_PATH=/run/secrets/fabric`
- `FABRIC_PEER_TLS_CERT_PATH=/run/secrets/fabric/peer-tls-ca.crt`
- `FABRIC_CERT_PATH=/run/secrets/fabric/user-cert.pem`
- `FABRIC_KEY_PATH=/run/secrets/fabric/user-key.pem`

## 步骤 4：部署到远程 staging

运行 `deploy-release.yml`，并设置：

- `environment=staging`

可选项：

- 如果你想在不改动已保存环境文件的前提下部署一次性候选版本，可传入：
  - `backend_image`
  - `frontend_image`
  - `ml_service_image`

该工作流会执行以下动作：

1. 上传 `docker-compose.release.yml`
2. 上传 `scripts/prod-compose.sh`
3. 上传完整环境文件
4. 校验远程主机存在 `docker` 与 `docker compose`
5. 执行 `docker compose config`
6. 执行 `pull`
7. 执行 `up -d`
8. 检查前后端健康状态
9. 若失败，自动抓取远程 `ps/logs` 并尝试回滚到上一份
   env/compose/helper-script 备份

## 步骤 5：部署后检查

最少需要完成以下检查：

1. 前端 `/health` 返回 `ok`
2. 后端 actuator health 返回 `UP`
3. ML `/health` 能通过私有网络访问
4. 使用非演示 staging 账号登录成功
5. 企业侧核心路径可基于 staging 数据跑通：
   - 上传或提交碳报表
   - 列出报表
   - 打开报表详情
6. 至少一个依赖 ML 的工作流返回成功响应
7. 若启用了 Fabric，区块链状态类接口返回正常
8. 检查远程 `runtime-logs` 目录中已有新日志写入

## 步骤 6：观察窗口

至少覆盖一个业务周期，重点观察：

- 认证 / 登录成功率
- 报表提交成功率
- 后端 5xx 比例
- 前端可用性
- ML 推理错误率
- Redis / 数据库连接错误

## 步骤 7：回滚演练

在宣告 staging 就绪前，必须先证明回滚路径可用，并至少演练一次。

再次运行 `deploy-release.yml`，可选两种方式：

1. 复用已保存环境文件中的旧镜像地址
2. 使用工作流输入：
   - `backend_image`
   - `frontend_image`
   - `ml_service_image`

回滚通过标准：

- 旧镜像能够成功启动
- 前后端健康检查重新通过
- 回滚后登录与至少一条核心业务流程仍能成功

## 退出标准

只有满足以下条件，远程 staging 演练才算完成：

- release 镜像已成功发布
- 基于远程镜像的纯拉取部署成功
- 部署后健康检查通过
- 至少一条真实 staging 业务流验证通过
- 至少一次回滚演练通过

建议在完成后立即复制并填写：

- `docs/external-execution-evidence-template.md`
