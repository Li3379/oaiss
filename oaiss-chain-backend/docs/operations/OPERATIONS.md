# OAISS Chain 运维文档

> **文档编号**: OAISS-OPS-001  
> **版本**: v1.0.0  
> **更新日期**: 2026-04-26  
> **项目**: 双碳链动系统 - 基于区块链的可信碳核算与交易平台

---

## 目录

1. [部署指南](#一部署指南)
2. [配置说明](#二配置说明)
3. [监控告警](#三监控告警)
4. [日志管理](#四日志管理)
5. [故障排查](#五故障排查)
6. [备份恢复](#六备份恢复)
7. [安全加固](#七安全加固)

---

## 一、部署指南

### 1.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | OpenJDK 或 Oracle JDK |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存服务 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 2.0+ | 容器编排 |

### 1.2 快速部署

```bash
# 1. 克隆项目
git clone https://github.com/oaiss/oaiss-chain-backend.git
cd oaiss-chain-backend

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，设置数据库密码、JWT密钥等

# 3. 启动所有服务
docker-compose up -d

# 4. 检查服务状态
docker-compose ps
```

### 1.3 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端应用 | 8080 | Spring Boot API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Prometheus | 9090 | 监控指标 |
| Grafana | 3000 | 可视化面板 |
| Elasticsearch | 9200 | 日志存储 |
| Kibana | 5601 | 日志可视化 |

### 1.4 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/api/v1/actuator/health

# 就绪探针
curl http://localhost:8080/api/v1/actuator/health/readiness

# 存活探针
curl http://localhost:8080/api/v1/actuator/health/liveness
```

---

## 二、配置说明

### 2.1 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oaiss_chain?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      connection-timeout: 30000
```

### 2.2 Redis配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 10000
```

### 2.3 JWT配置

```yaml
jwt:
  secret: ${JWT_SECRET}  # 生产环境必须设置，至少256位
  expiration: 3600000    # 1小时
  refresh-expiration: 604800000  # 7天
```

### 2.4 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

---

## 三、监控告警

### 3.1 Prometheus指标

访问 `http://localhost:9090` 查看Prometheus面板。

**关键指标:**

| 指标名称 | 说明 | 告警阈值 |
|----------|------|----------|
| `jvm_memory_used_bytes` | JVM内存使用 | > 80% |
| `http_server_requests_seconds` | HTTP请求延迟 | P99 > 1s |
| `hikaricp_connections_active` | 活跃数据库连接 | > 80% |
| `oaiss_active_users` | 活跃用户数 | - |

### 3.2 Grafana面板

访问 `http://localhost:3000` (admin/admin123)

**预置面板:**
- JVM监控面板
- 数据库连接池监控
- 业务指标面板

### 3.3 告警配置

告警规则文件: `monitoring/prometheus/alert_rules.yml`

```yaml
groups:
  - name: oaiss-alerts
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM内存使用率过高"
```

### 3.4 告警通知

AlertManager配置: `monitoring/alertmanager/alertmanager.yml`

支持的通知渠道:
- 邮件通知
- 企业微信
- 钉钉机器人

---

## 四、日志管理

### 4.1 日志格式

应用日志采用JSON格式，便于ELK收集分析:

```json
{
  "@timestamp": "2026-04-26T16:00:00.000+08:00",
  "level": "INFO",
  "logger_name": "com.oaiss.chain.service.AuthService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "用户登录成功",
  "app_name": "oaiss-chain-backend",
  "mdc": {
    "enterpriseId": "10001",
    "userId": "1",
    "requestId": "req_1234567890_1"
  }
}
```

### 4.2 日志文件位置

```
logs/
├── oaiss-chain-backend.json      # JSON格式日志 (ELK)
├── oaiss-chain-backend-error.log # 错误日志
└── archive/                       # 归档日志
```

### 4.3 Kibana使用

1. 访问 `http://localhost:5601`
2. 创建索引模式: `oaiss-chain-*`
3. 配置时间字段: `@timestamp`

**常用查询:**
```
# 查询错误日志
level: ERROR

# 查询特定企业
mdc.enterpriseId: "10001"

# 查询慢请求
level: WARN AND message: "slow"
```

### 4.4 日志保留策略

| 日志类型 | 保留时间 | 存储位置 |
|----------|----------|----------|
| 应用日志 | 30天 | Elasticsearch |
| 错误日志 | 60天 | 文件系统 |
| 审计日志 | 90天 | 数据库 |

---

## 五、故障排查

### 5.1 常见问题

#### 问题1: 应用无法启动

**症状:** 应用启动失败，日志显示数据库连接错误

**排查步骤:**
```bash
# 1. 检查数据库是否运行
docker-compose ps mysql

# 2. 检查数据库连接
mysql -h localhost -u root -p

# 3. 检查网络连通性
telnet localhost 3306

# 4. 查看应用日志
docker-compose logs app
```

#### 问题2: 内存溢出

**症状:** 应用崩溃，日志显示 `OutOfMemoryError`

**排查步骤:**
```bash
# 1. 检查JVM内存使用
curl http://localhost:8080/api/v1/actuator/metrics/jvm.memory.used

# 2. 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 3. 分析堆转储 (使用MAT或VisualVM)
```

#### 问题3: 接口响应慢

**症状:** API响应时间超过1秒

**排查步骤:**
```bash
# 1. 检查数据库慢查询
# 登录MySQL执行:
SHOW PROCESSLIST;
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;

# 2. 检查Redis连接
redis-cli ping

# 3. 查看Prometheus指标
curl http://localhost:8080/api/v1/actuator/metrics/http.server.requests
```

### 5.2 性能分析工具

**Arthas (在线诊断):**
```bash
# 下载并启动
curl -O https://arthas.aliyun.com/math-game.jar
java -jar math-game.jar

# 诊断命令
dashboard          # 查看面板
thread             # 线程信息
memory             # 内存信息
trace              # 方法调用追踪
```

**JFR (Java Flight Recorder):**
```bash
# 启动JFR记录
jcmd <pid> JFR.start name=profile duration=60s filename=recording.jfr

# 分析JFR文件 (使用JDK Mission Control)
```

---

## 六、备份恢复

### 6.1 数据库备份

**自动备份脚本:**
```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/mysql"
MYSQL_USER="root"
MYSQL_PASS="your_password"
DATABASE="oaiss_chain"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u$MYSQL_USER -p$MYSQL_PASS --single-transaction --routines --triggers $DATABASE | gzip > $BACKUP_DIR/oaiss_chain_$DATE.sql.gz

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: oaiss_chain_$DATE.sql.gz"
```

**定时任务 (crontab):**
```bash
# 每天凌晨2点执行备份
0 2 * * * /opt/oaiss/scripts/backup.sh >> /var/log/oaiss/backup.log 2>&1
```

### 6.2 数据恢复

```bash
# 解压备份文件
gunzip oaiss_chain_20260426.sql.gz

# 恢复数据库
mysql -u root -p oaiss_chain < oaiss_chain_20260426.sql
```

### 6.3 Redis备份

```bash
# 手动触发RDB快照
redis-cli BGSAVE

# 复制RDB文件
cp /var/lib/redis/dump.rdb /backup/redis/dump_$(date +%Y%m%d).rdb
```

---

## 七、安全加固

### 7.1 网络安全

```yaml
# docker-compose.yml 网络隔离
networks:
  frontend:
    driver: bridge
  backend:
    internal: true  # 内部网络，无法访问外网
```

### 7.2 敏感信息管理

**使用环境变量:**
```bash
# .env 文件 (不要提交到Git)
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_at_least_256_bits
REDIS_PASSWORD=your_redis_password
```

**使用Docker Secrets:**
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### 7.3 SSL/TLS配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 7.4 安全检查清单

- [ ] 修改默认管理员密码
- [ ] 启用JWT密钥轮换
- [ ] 配置IP白名单
- [ ] 启用HTTPS
- [ ] 定期更新依赖版本
- [ ] 启用审计日志
- [ ] 配置防火墙规则

---

## 附录

### A. 常用命令

```bash
# 查看应用状态
docker-compose ps

# 查看日志
docker-compose logs -f app

# 重启服务
docker-compose restart app

# 扩容应用
docker-compose up -d --scale app=3

# 清理资源
docker-compose down -v
```

### B. 联系方式

- 技术支持: support@oaiss.com
- 文档更新: docs@oaiss.com
- 紧急联系: +86-xxx-xxxx-xxxx

---

**文档维护**: 运维团队  
**最后更新**: 2026-04-26