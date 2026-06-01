---
status: complete
created: 2026-05-14
author: claude
source: UltraQA API Testing Cycle 1
---

# SPEC-010: Refresh Token端点错误码不规范

## 问题描述

`/auth/refresh` 端点在未提供token或提供无效token时，返回通用错误码1000（"error.system"），而非明确的错误码2002（Token无效）。

### 影响

- 前端无法区分"token无效"和"系统内部错误"
- 无法给出精准的错误提示
- 不符合RESTful API错误码规范

### 根因分析

两层原因：
1. `AuthController.java` 中 `@RequestHeader("Refresh-Token")` 默认 `required=true`，缺失 Header 时 Spring 抛出 `MissingRequestHeaderException`，被 `GlobalExceptionHandler` 捕获映射为通用1000错误码
2. `AuthService.refreshToken()` 未对 null/empty 参数做前置校验

## 修复方案

两处修改：

### 1. AuthController.java — Header 设为可选

```java
@RequestHeader(value = "Refresh-Token", required = false) String refreshToken
```

### 2. AuthService.java — 添加空值校验

```java
public LoginResponse refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
        throw AuthenticationException.tokenInvalid();
    }
    if (!jwtTokenProvider.validateToken(refreshToken)
            || !jwtTokenProvider.isRefreshToken(refreshToken)) {
        throw AuthenticationException.tokenInvalid();
    }
    // ... 正常逻辑
}
```

## 变更文件

| 文件 | 变更 |
|------|------|
| `AuthController.java` | `@RequestHeader` 添加 `required = false` |
| `AuthService.java` | `refreshToken()` 添加空值校验 |

## 验证结果

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| POST /auth/refresh (no header) | 1000 error.system | 2002 Token无效 |
| POST /auth/refresh (empty header) | 1000 error.system | 2002 Token无效 |
