---
status: draft
priority: high
created: 2026-05-10
author: claude
related:
  - Phase 2 UAT findings
  - Database configuration
---

# SPEC: 数据库配置统一化

## 问题描述

当前项目存在两个MySQL实例：
1. **本地MySQL**: 运行在端口3306，存储实际开发数据
2. **Docker MySQL**: 运行在端口3307，配置在docker-compose.yml中

应用配置连接`localhost:3306`，导致：
- 开发环境与Docker环境数据分离
- 新成员可能困惑数据来源
- CI/CD环境可能使用不同配置

## 当前状态

```yaml
# docker-compose.yml
services:
  mysql:
    ports:
      - "3307:3306"  # Docker暴露3307

# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oaiss_chain  # 连接本地3306
```

## 解决方案分析

### 方案A: 统一使用Docker MySQL（推荐）

**优点**：
- 环境一致性：开发、测试、生产环境配置统一
- 隔离性：不污染本地开发机
- 可移植性：新成员只需docker-compose up

**缺点**：
- 需要迁移现有本地数据
- Docker性能略低于本地MySQL

**实施步骤**：
1. 导出本地MySQL数据
2. 修改docker-compose.yml端口映射为3306:3306
3. 停止本地MySQL服务
4. 导入数据到Docker MySQL
5. 更新application.yml使用Docker服务名

### 方案B: 统一使用本地MySQL

**优点**：
- 性能最优
- 无需Docker依赖

**缺点**：
- 环境不一致
- 新成员需要额外配置
- CI/CD需要单独配置

**实施步骤**：
1. 移除docker-compose中的MySQL服务
2. 保持application.yml连接localhost:3306

### 方案C: 使用Profile区分环境

**优点**：
- 灵活性最高
- 支持多环境切换

**缺点**：
- 配置复杂度增加
- 可能导致混淆

**实施步骤**：
1. 创建application-dev.yml（本地MySQL）
2. 创建application-docker.yml（Docker MySQL）
3. 通过环境变量激活profile

## 推荐方案

**采用方案A + 方案C组合**：

1. Docker作为主要开发环境
2. 保留profile支持本地开发
3. CI/CD使用Docker配置

## 配置变更

### docker-compose.yml

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: oaiss-mysql
    ports:
      - "3306:3306"  # 改为3306
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: oaiss_chain
    volumes:
      - mysql_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/oaiss_chain?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
```

### application-local.yml（新增）

```yaml
# 本地开发时激活：spring.profiles.active=local
# 连接本地MySQL（如果开发者不想用Docker）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oaiss_chain?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

## 数据迁移计划

```bash
#!/bin/bash
# scripts/migrate-to-docker-mysql.sh

# 1. 导出本地数据
mysqldump -uroot -p123456 oaiss_chain > backup_local.sql

# 2. 停止本地MySQL（Windows）
net stop mysql

# 3. 修改docker-compose.yml端口为3306

# 4. 重启Docker MySQL
docker-compose down
docker-compose up -d mysql

# 5. 等待MySQL就绪
sleep 10

# 6. 导入数据
docker exec -i oaiss-mysql mysql -uroot -p123456 oaiss_chain < backup_local.sql

echo "Migration complete!"
```

## 验证清单

- [ ] Docker MySQL运行在端口3306
- [ ] 本地MySQL已停止或卸载
- [ ] 应用启动成功
- [ ] 数据完整迁移
- [ ] 测试脚本运行正常
- [ ] Flyway迁移正常

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 数据丢失 | 高 | 先备份，验证后再删除本地MySQL |
| 端口冲突 | 中 | 确保本地MySQL已停止 |
| 性能下降 | 低 | Docker MySQL性能足够开发使用 |

## 回滚方案

如果方案A出现问题：
1. 恢复docker-compose.yml端口为3307
2. 启动本地MySQL
3. 从备份恢复数据
