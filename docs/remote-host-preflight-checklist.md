# 远程主机部署前检查清单

本清单适用于将通过 `docker-compose.release.yml` 运行 OAISS CHAIN 的目标主机。

请在首次远程 `staging` 发布前使用一次，并在首次 `production` 发布前再次使用。

## 1. 主机基础信息

- [ ] 当前主机是目标部署机器，而不是开发者工作站
- [ ] 主机名与环境标识正确
- [ ] 系统时间与时区正确
- [ ] 已启用 NTP / 时间同步
- [ ] 操作系统包状态健康，足以支撑 Docker 正常运行

建议命令：

```bash
hostname
timedatectl status
date
```

## 2. Docker 运行时

- [ ] 已安装 Docker Engine
- [ ] `docker compose` 可用
- [ ] 部署用户可执行 Docker 命令
- [ ] Docker 根文件系统剩余空间足够同时容纳至少一个旧版本和一个新版本

建议命令：

```bash
docker --version
docker compose version
docker info
docker system df
df -h
```

## 3. 文件系统与部署目录

- [ ] 部署目录已存在，或具备创建条件
- [ ] 部署用户拥有部署目录，或对其有写权限
- [ ] 有足够空间存放环境文件、compose 文件、日志与镜像层
- [ ] 如果启用 Fabric，所需挂载的密钥或证书路径已存在

建议命令：

```bash
mkdir -p /opt/oaiss-chain-staging
ls -ld /opt/oaiss-chain-staging
df -h /opt
```

首次准备主机时，建议直接运行：

```bash
sudo ./scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-staging --deploy-user deploy
```

生产环境可改为：

```bash
sudo ./scripts/bootstrap-remote-release-host.sh --target-dir /opt/oaiss-chain-prod --deploy-user deploy
```

## 4. 网络与 DNS

- [ ] 主机可以解析镜像仓库域名
- [ ] 主机可以解析 MySQL 域名
- [ ] 主机可以解析 Redis 域名
- [ ] 主机可以解析对象存储域名
- [ ] 主机可以解析可选的 Fabric peer / CA 域名

建议命令：

```bash
getent hosts ghcr.io
getent hosts mysql.example.internal
getent hosts redis.example.internal
getent hosts object-storage.example.internal
```

## 5. 出站连通性

- [ ] 主机可以访问镜像仓库
- [ ] 主机可以访问数据库端点
- [ ] 主机可以访问 Redis 端点
- [ ] 主机可以访问对象存储端点
- [ ] 主机可以访问可选的 Fabric 端点

建议命令：

```bash
curl -I https://ghcr.io
nc -vz mysql.example.internal 3306
nc -vz redis.example.internal 6379
curl -I https://object-storage.example.internal
```

## 6. 入站暴露面

- [ ] 只有预期的公网端口对外开放
- [ ] 后端私有端口未被公网暴露，除非这是明确设计
- [ ] MySQL、Redis、对象存储管理端，以及可选 Fabric 节点未对公网暴露
- [ ] 防火墙或安全组规则与架构设计一致

建议命令：

```bash
ss -tulpn
sudo ufw status
```

## 7. TLS 与公网路由

- [ ] 公网 DNS 已指向正确主机或负载均衡器
- [ ] TLS 证书对外部域名有效
- [ ] 反向代理或负载均衡器正确传递 `X-Forwarded-Proto` 与 `X-Forwarded-For`
- [ ] 前端 `/health` 与后端 `/api/v1/actuator/health` 已按公网路径设计可访问

建议命令：

```bash
curl -I https://app.example.com/health
curl -I https://app.example.com/api/v1/actuator/health
openssl s_client -connect app.example.com:443 -servername app.example.com </dev/null
```

## 8. 镜像仓库访问

- [ ] 若镜像为私有仓库，远程主机可以执行 `docker login`
- [ ] 镜像仓库令牌有效
- [ ] 主机可以拉取目标发布标签

建议命令：

```bash
docker login ghcr.io
docker pull ghcr.io/<owner>/oaiss-chain-backend:<release-tag>
docker pull ghcr.io/<owner>/oaiss-chain-frontend:<release-tag>
docker pull ghcr.io/<owner>/oaiss-chain-ml-service:<release-tag>
```

## 9. 外部依赖

- [ ] MySQL 凭证有效
- [ ] Redis 凭证有效
- [ ] 对象存储凭证有效
- [ ] JWT / RSA / ML 共享密钥已准备妥当
- [ ] 如启用 Fabric，配置路径上的证书与私钥文件已存在

建议命令：

```bash
mysql -h mysql.example.internal -u oaiss_app -p
redis-cli -h redis.example.internal -a '<password>' ping
```

## 10. 可观测性

- [ ] 已知日志查看路径
- [ ] `runtime-logs/backend`、`runtime-logs/frontend`、`runtime-logs/ml-service` 已存在
- [ ] 已明确容器重启行为
- [ ] 指标面板 / 仪表盘可访问
- [ ] 告警投递链路已验证

建议命令：

```bash
docker compose --env-file /opt/oaiss-chain-prod/oaiss-chain.env -f /opt/oaiss-chain-prod/docker-compose.release.yml logs --tail=100
docker compose --env-file /opt/oaiss-chain-prod/oaiss-chain.env -f /opt/oaiss-chain-prod/docker-compose.release.yml ps
```

## 11. 备份与回滚准备

- [ ] 数据库备份命令已实际演练过
- [ ] 旧镜像标签已记录
- [ ] 回滚操作人员知道如何使用镜像覆盖参数重新执行 `deploy-release.yml`
- [ ] 知道工作流失败时会自动恢复上一个 `oaiss-chain.env`、`docker-compose.release.yml` 与 `scripts/prod-compose.sh`
- [ ] 观察窗口责任人已明确

## 12. 退出标准

只有满足以下条件，主机才可视为部署准备完成：

- 所有必需的主机层检查通过
- 镜像仓库访问能力已验证
- 外部依赖连通性已验证
- 公网路由与 TLS 配置正确
- 在真正开始部署前，备份与回滚路径都已明确
