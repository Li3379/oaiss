# Staging GitHub 密钥填写单

这是 GitHub Environment `staging` 的最简实用填写清单。

请结合以下文件一起使用：

- `docs/github-environment-secrets-template.md`
- `.env.staging.example`
- `docs/remote-staging-rehearsal.md`

## 1. GitHub Environment 名称

在 GitHub 中创建或更新以下 Environment：

- `staging`

## 2. 必填 GitHub Environment Secrets

直接在 GitHub 中填写以下值：

| 密钥 | 填写内容 |
|---|---|
| `DEPLOY_HOST` | 真实 staging 主机，例如 `staging-app-01.example.com` |
| `DEPLOY_PORT` | 通常为 `22` |
| `DEPLOY_USER` | 远程部署用户，例如 `deploy` |
| `DEPLOY_SSH_KEY` | staging 部署用户使用的私钥 |
| `DEPLOY_TARGET_DIR` | 远端部署目录，例如 `/opt/oaiss-chain-staging` |
| `FRONTEND_HEALTHCHECK_URL` | 例如 `https://staging.example.com/health` |
| `BACKEND_HEALTHCHECK_URL` | 例如 `https://staging.example.com/api/v1/actuator/health` |

## 3. 可选镜像仓库密钥

如果远程 staging 主机在拉取发布镜像前必须先登录镜像仓库，请增加以下值：

| 密钥 | 填写内容 |
|---|---|
| `REGISTRY_HOST` | `ghcr.io` |
| `REGISTRY_USERNAME` | 镜像拉取用户或机器人账号 |
| `REGISTRY_PASSWORD` | 镜像仓库 token / 密码 |

重要说明：

- 这三项要么全部填写，要么全部留空

## 4. `DEPLOY_ENV_FILE` 密钥内容

将 `DEPLOY_ENV_FILE` 设置为下面这份完整内容，并把占位符替换成真实值。

```env
SPRING_PROFILES_ACTIVE=staging,fabric
LOG_LEVEL=INFO
APP_LOG_LEVEL=INFO

BACKEND_IMAGE=ghcr.io/<owner>/oaiss-chain-backend:<staging-release-tag>
FRONTEND_IMAGE=ghcr.io/<owner>/oaiss-chain-frontend:<staging-release-tag>
ML_SERVICE_IMAGE=ghcr.io/<owner>/oaiss-chain-ml-service:<staging-release-tag>

CORS_ALLOWED_ORIGINS=https://staging.example.com

DB_URL=jdbc:mysql://mysql-staging.example.internal:3306/oaiss_chain?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
DB_USERNAME=oaiss_app_staging
DB_PASSWORD=<真实 staging 数据库密码>
DB_POOL_MIN_IDLE=3
DB_POOL_MAX_SIZE=10
DB_POOL_CONNECTION_TIMEOUT_MS=30000
DB_POOL_LEAK_DETECTION_MS=0

REDIS_HOST=redis-staging.example.internal
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_PASSWORD=<真实 staging redis 密码>

JWT_SECRET=<真实 staging jwt 密钥>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
RSA_KEK=<base64 32-byte key>

MINIO_ENDPOINT=https://object-storage-staging.example.internal
MINIO_ACCESS_KEY=<真实 staging 对象存储 access key>
MINIO_SECRET_KEY=<真实 staging 对象存储 secret key>
MINIO_BUCKET=oaiss-chain-staging
MINIO_PRESIGNED_URL_EXPIRY_SECONDS=1800

ML_LOG_LEVEL=INFO
ML_SERVICE_URL=http://ml-service:8001
ML_SERVICE_SECRET=<真实 staging ML 共享密钥>
ML_SERVICE_CONNECT_TIMEOUT=5s
ML_SERVICE_READ_TIMEOUT=30s

REQUIRE_OPS_SECRETS=true
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<真实 staging grafana 密码>
ALERT_SMTP_HOST=
ALERT_SMTP_USERNAME=
ALERT_SMTP_PASSWORD=
ALERT_WEBHOOK_URL=
ALERT_WEBHOOK_SECRET=
BACKEND_LOG_DIR=./runtime-logs/backend
FRONTEND_LOG_DIR=./runtime-logs/frontend
ML_LOG_DIR=./runtime-logs/ml-service

FABRIC_ENABLED=true
FABRIC_MSP_ID=Org1MSP
FABRIC_CHANNEL_NAME=mychannel
FABRIC_CHAINCODE_NAME=carbon-chaincode
FABRIC_PEER_ENDPOINT=peer0.org1.staging.internal:7051
FABRIC_TLS_ENABLED=true
FABRIC_PEER_TLS_CERT_PATH=/run/secrets/fabric/peer-tls-ca.crt
FABRIC_CERT_PATH=/run/secrets/fabric/user-cert.pem
FABRIC_KEY_PATH=/run/secrets/fabric/user-key.pem
FABRIC_CA_ENABLED=false
FABRIC_CA_ENDPOINT=https://ca.org1.staging.internal:7054
FABRIC_CA_ADMIN_NAME=admin
FABRIC_CA_ADMIN_PASSWORD=<真实 staging fabric ca 密码>
FABRIC_COUCHDB_USER=admin
FABRIC_COUCHDB_PASSWORD=<真实 staging couchdb 密码>
FABRIC_SECRETS_DIR=./secrets/fabric
FABRIC_SECRETS_MOUNT_PATH=/run/secrets/fabric
```

## 5. 保存前检查

快速核对以下项目：

- `BACKEND_IMAGE`、`FRONTEND_IMAGE`、`ML_SERVICE_IMAGE` 指向同一个候选发布标签
- `SPRING_PROFILES_ACTIVE=staging,fabric`
- `CORS_ALLOWED_ORIGINS` 与真实 staging 域名一致
- `DB_URL`、`REDIS_HOST`、`MINIO_ENDPOINT` 没有指向 `localhost`
- `JWT_SECRET`、`RSA_KEK`、`ML_SERVICE_SECRET` 都已替换为真实值
- `BACKEND_LOG_DIR`、`FRONTEND_LOG_DIR`、`ML_LOG_DIR` 已填写
- `FABRIC_SECRETS_DIR` 与远程主机上的 secrets 目录一致
- `FABRIC_PEER_TLS_CERT_PATH`、`FABRIC_CERT_PATH`、`FABRIC_KEY_PATH` 已填写
- 目标环境中已经存在非演示用的 staging 登录账号

## 6. 保存后执行顺序

按以下顺序运行：

1. `release-images.yml`
2. `deploy-release.yml`，并设置 `environment=staging`
3. 在远程主机先运行 `scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-staging --deploy-user deploy`
4. 将真实 Fabric 文件放入 `/opt/oaiss-chain-staging/secrets/fabric`
5. 按 `docs/remote-staging-rehearsal.md` 执行验收
