# GitHub Environment Secrets 填写模板

本文档是以下工作流所用 GitHub Environment 的填写指引：

- `.github/workflows/release-images.yml`
- `.github/workflows/deploy-release.yml`

建议分别创建以下两个 GitHub Environment：

- `staging`
- `production`

## 1. 最小必填密钥集

以下密钥是 `deploy-release.yml` 的必需项。

| 密钥 | 应填写内容 | `staging` 示例 | `production` 示例 |
|---|---|---|---|
| `DEPLOY_HOST` | 远程主机或跳板机地址 | `staging-app-01.example.com` | `prod-app-01.example.com` |
| `DEPLOY_PORT` | SSH 端口 | `22` | `22` |
| `DEPLOY_USER` | 远程部署用户 | `deploy` | `deploy` |
| `DEPLOY_SSH_KEY` | GitHub Actions 使用的 OpenSSH 私钥 | staging 主机部署私钥 | production 主机部署私钥 |
| `DEPLOY_TARGET_DIR` | 远端用于保存 compose 文件与环境文件的目录 | `/opt/oaiss-chain-staging` | `/opt/oaiss-chain-prod` |
| `DEPLOY_ENV_FILE` | 部署时上传的完整环境文件内容 | 由 `.env.staging.example` 填充后得到 | 由 `.env.prod.example` 填充后得到 |
| `FRONTEND_HEALTHCHECK_URL` | 部署完成后前端健康检查地址 | `https://staging.example.com/health` | `https://app.example.com/health` |
| `BACKEND_HEALTHCHECK_URL` | 部署完成后后端健康检查地址 | `https://staging.example.com/api/v1/actuator/health` | `https://app.example.com/api/v1/actuator/health` |

## 1.1 可选镜像仓库密钥

当发布镜像存放在私有仓库，且远端主机在 `docker pull` 前必须登录时，需要增加以下密钥。

| 密钥 | 应填写内容 | `staging` 示例 | `production` 示例 |
|---|---|---|---|
| `REGISTRY_HOST` | `docker login` 使用的镜像仓库域名 | `ghcr.io` | `ghcr.io` |
| `REGISTRY_USERNAME` | 镜像仓库用户名 | `oaiss-staging-bot` | `oaiss-prod-bot` |
| `REGISTRY_PASSWORD` | 镜像仓库令牌或密码 | staging 拉取专用 GHCR token | production 拉取专用 GHCR token |

## 2. `DEPLOY_ENV_FILE` 内建议包含的环境变量

`DEPLOY_ENV_FILE` 应是完整环境文件，不应只写少量变量。

### 2.1 发布镜像地址

这些值应指向已发布的 GHCR 镜像。

```env
BACKEND_IMAGE=ghcr.io/<owner>/oaiss-chain-backend:<release-tag>
FRONTEND_IMAGE=ghcr.io/<owner>/oaiss-chain-frontend:<release-tag>
ML_SERVICE_IMAGE=ghcr.io/<owner>/oaiss-chain-ml-service:<release-tag>
```

### 2.2 运行环境 Profile

```env
SPRING_PROFILES_ACTIVE=staging
```

生产环境应使用：

```env
SPRING_PROFILES_ACTIVE=prod
```

如启用 Fabric：

```env
SPRING_PROFILES_ACTIVE=staging,fabric
```

或：

```env
SPRING_PROFILES_ACTIVE=prod,fabric
```

### 2.3 数据库 / 缓存 / 对象存储

```env
DB_URL=jdbc:mysql://mysql-staging.example.internal:3306/oaiss_chain?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
DB_USERNAME=oaiss_app_staging
DB_PASSWORD=<真实密码>

REDIS_HOST=redis-staging.example.internal
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_PASSWORD=<真实密码>

MINIO_ENDPOINT=https://object-storage-staging.example.internal
MINIO_ACCESS_KEY=<真实 access key>
MINIO_SECRET_KEY=<真实 secret key>
MINIO_BUCKET=oaiss-chain-staging
```

生产环境必须指向专属的生产主机、生产库与生产桶，不可与 staging 共用。

### 2.4 认证与加密

```env
JWT_SECRET=<真实随机密钥>
RSA_KEK=<base64 32-byte key>
```

生产环境不得复用 staging 的 JWT 或 RSA 密钥。

### 2.5 ML 服务联动

```env
ML_LOG_LEVEL=INFO
ML_SERVICE_URL=http://ml-service:8001
ML_SERVICE_SECRET=<后端与 ML 服务共享的密钥>
```

如果后端和 ML 服务运行在同一个 release compose 网络中，`ML_SERVICE_URL=http://ml-service:8001` 通常无需修改。

### 2.6 公网访问来源

```env
CORS_ALLOWED_ORIGINS=https://staging.example.com
```

生产环境只应允许真实前端域名，例如：

```env
CORS_ALLOWED_ORIGINS=https://app.example.com
```

### 2.7 可选 Fabric 配置

如果远程环境启用区块链集成：

```env
FABRIC_ENABLED=true
FABRIC_MSP_ID=Org1MSP
FABRIC_CHANNEL_NAME=mychannel
FABRIC_CHAINCODE_NAME=carbon-chaincode
FABRIC_PEER_ENDPOINT=peer0.org1.example.internal:7051
FABRIC_TLS_ENABLED=true
FABRIC_PEER_TLS_CERT_PATH=/run/secrets/fabric/peer-tls-ca.crt
FABRIC_CERT_PATH=/run/secrets/fabric/user-cert.pem
FABRIC_KEY_PATH=/run/secrets/fabric/user-key.pem
FABRIC_CA_ENABLED=false
```

## 3. 各值通常来源于哪里

| 密钥或环境变量 | 常见来源 |
|---|---|
| `DEPLOY_HOST`、`DEPLOY_PORT`、`DEPLOY_USER`、`DEPLOY_TARGET_DIR` | 运维资产清单 / 云主机初始化配置 |
| `DEPLOY_SSH_KEY` | 运维生成的独立部署私钥 |
| `REGISTRY_HOST`、`REGISTRY_USERNAME`、`REGISTRY_PASSWORD` | 镜像仓库管理员或 CI 机器人账号 |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 托管 MySQL 或私有数据库服务 |
| `REDIS_HOST`、`REDIS_PASSWORD` | 托管 Redis 或私有 Redis 服务 |
| `MINIO_*` | MinIO 或兼容 S3 的对象存储凭证 |
| `JWT_SECRET` | 安全负责人或平台负责人生成 |
| `RSA_KEK` | 安全负责人或平台负责人生成 |
| `ML_SERVICE_SECRET` | 生成一次后由后端与 ML 服务共享 |
| `FRONTEND_HEALTHCHECK_URL`、`BACKEND_HEALTHCHECK_URL` | 公网入口 / 反向代理域名 |
| `FABRIC_*` | Fabric 运维负责人或区块链环境管理员 |

## 4. 安全发布顺序建议

推荐顺序如下：

1. 先填写 `staging` Environment secrets
2. 运行 `release-images.yml` 发布镜像
3. 使用 `deploy-release.yml` 部署到 `staging`
4. 校验健康状态，并跑通至少一条真实 staging 业务链路
5. 使用镜像覆盖参数做一次回滚演练
6. 完成以上步骤后，再填写 `production` Environment secrets

## 5. 保存前快速核对

- 主机地址没有指向 `localhost`
- staging 与 production 使用不同主机和不同凭证
- staging 与 production 的 JWT / RSA / ML 共享密钥彼此独立
- `DEPLOY_ENV_FILE` 已包含镜像地址
- 若镜像为私有仓库，已配置镜像登录密钥
- 健康检查地址与真实入口路径一致
- 目标环境中已存在非演示账号

## 6. 相关文档

- `docs/github-actions-deploy-secrets.md`
- `docs/staging-github-secrets-fillout.md`
- `docs/production-github-secrets-fillout.md`
- `docs/remote-staging-rehearsal.md`
- `docs/deployment-runbook.md`
- `.env.staging.example`
- `.env.prod.example`
