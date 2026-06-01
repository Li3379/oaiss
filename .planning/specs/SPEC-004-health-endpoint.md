---
status: draft
priority: medium
created: 2026-05-10
author: claude
related:
  - Phase 2 UAT findings
  - Health check
  - Testing
---

# SPEC: 健康检查端点优化

## 问题描述

当前健康检查存在以下问题：
1. Swagger UI需要认证才能访问
2. 测试脚本使用Swagger UI做健康检查失败
3. 缺少专门的、无需认证的健康检查端点

## 当前状态

```bash
# 测试脚本中的健康检查
curl -sf "$API/swagger-ui.html"  # 失败：需要认证

# 临时修复：使用login端点
curl -sf "$API/auth/login" -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}'
```

**问题**：
- login端点不是专门的健康检查
- 可能返回200但服务内部有问题
- 无法区分"服务启动"和"服务健康"

## 解决方案分析

### 方案A: 添加公开健康检查端点（推荐）

**优点**：
- 标准化
- 无需认证
- 可扩展

**实施**：
```java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0"
        ));
    }
}
```

### 方案B: 使用Spring Actuator

**优点**：
- 开箱即用
- 功能丰富
- 标准化

**缺点**：
- 需要配置公开端点
- 可能暴露敏感信息

**实施**：
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never  # 不暴露详情
```

### 方案C: 修改Swagger安全配置

**优点**：
- 最小改动

**缺点**：
- Swagger可能包含敏感API信息
- 不符合最小权限原则

**实施**：
```java
// SecurityConfig.java
.requestMatchers("/swagger-ui/**", "/v1/api-docs/**").permitAll()
```

## 推荐方案

**采用方案A + 方案B组合**：

1. 添加自定义健康检查端点（简单、快速）
2. 配置Actuator健康检查（标准化、可扩展）
3. 两者都无需认证

## 实施细节

### 1. 创建健康检查Controller

```java
// controller/HealthController.java
package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查Controller
 * 提供无需认证的健康检查端点
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${spring.application.name:oaiss-chain-backend}")
    private String applicationName;

    @Value("${app.version:1.0.0}")
    private String version;

    /**
     * 简单健康检查
     * 用于负载均衡、测试脚本等场景
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
            "status", "UP",
            "application", applicationName,
            "version", version,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 就绪检查
     * 检查服务是否准备好接收请求
     */
    @GetMapping("/ready")
    public ApiResponse<Map<String, Object>> ready() {
        // 可以添加数据库、Redis等依赖检查
        return ApiResponse.success(Map.of(
            "status", "READY",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 存活检查
     * 用于Kubernetes liveness probe
     */
    @GetMapping("/live")
    public ApiResponse<Map<String, Object>> live() {
        return ApiResponse.success(Map.of(
            "status", "ALIVE",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

### 2. 配置安全白名单

```java
// config/SecurityConfig.java

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ... 其他配置 ...
        .authorizeHttpRequests(auth -> auth
            // 健康检查端点公开
            .requestMatchers("/health/**").permitAll()
            // Actuator健康检查公开
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            // ... 其他规则 ...
        );
    return http.build();
}
```

### 3. 配置Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized  # 仅授权用户看详情
      probes:
        enabled: true
    info:
      enabled: true
    prometheus:
      enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    db:
      enabled: true
    redis:
      enabled: true
```

### 4. 添加版本信息

```yaml
# application.yml
app:
  version: 1.0.0

# 或使用Maven资源过滤
app:
  version: @project.version@
```

```xml
<!-- pom.xml -->
<build>
  <resources>
    <resource>
      <directory>src/main/resources</directory>
      <filtering>true</filtering>
    </resource>
  </resources>
</build>
```

### 5. 更新测试脚本

```bash
# scripts/carbon-report-test.sh

# 修改前
wait_for_backend() {
    echo "Waiting for backend to be ready..."
    for i in {1..30}; do
        if curl -sf "$API/auth/login" -H "Content-Type: application/json" \
            -d '{"username":"admin","password":"admin123"}' -o /dev/null 2>/dev/null; then
            echo "Backend is ready!"
            return 0
        fi
        sleep 1
    done
    echo "Backend not ready after 30 seconds"
    return 1
}

# 修改后
wait_for_backend() {
    echo "Waiting for backend to be ready..."
    for i in {1..30}; do
        if curl -sf "$API/health" -o /dev/null 2>/dev/null; then
            echo "Backend is ready!"
            return 0
        fi
        sleep 1
    done
    echo "Backend not ready after 30 seconds"
    return 1
}
```

### 6. Docker健康检查

```yaml
# docker-compose.yml
services:
  backend:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

### 7. Kubernetes探针配置

```yaml
# k8s/deployment.yaml
livenessProbe:
  httpGet:
    path: /api/v1/health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/v1/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

## 健康检查端点对比

| 端点 | 用途 | 认证 | 返回内容 |
|------|------|------|----------|
| `/health` | 通用健康检查 | 否 | status, version, timestamp |
| `/health/ready` | 就绪检查 | 否 | status, timestamp |
| `/health/live` | 存活检查 | 否 | status, timestamp |
| `/actuator/health` | 详细健康检查 | 否 | status, components |
| `/actuator/info` | 应用信息 | 否 | app, version |

## 验证清单

- [ ] HealthController创建
- [ ] 安全配置更新
- [ ] Actuator配置更新
- [ ] 测试脚本更新
- [ ] Docker健康检查配置
- [ ] 手动测试各端点

## 测试命令

```bash
# 简单健康检查
curl http://localhost:8080/api/v1/health

# 就绪检查
curl http://localhost:8080/api/v1/health/ready

# 存活检查
curl http://localhost:8080/api/v1/health/live

# Actuator健康检查
curl http://localhost:8080/api/v1/actuator/health

# 预期响应
{
  "code": 1000,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "oaiss-chain-backend",
    "version": "1.0.0",
    "timestamp": "2026-05-10T16:00:00"
  }
}
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 暴露敏感信息 | 低 | 不返回详细信息，仅返回状态 |
| 被滥用 | 低 | 可添加速率限制 |
| 与Actuator冲突 | 无 | 使用不同路径 |

## 回滚方案

如果出现问题：
1. 移除HealthController
2. 恢复使用login端点做健康检查
3. 恢复原有安全配置
