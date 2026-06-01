---
status: complete
created: 2026-05-14
author: claude
source: UltraQA API Testing Cycle 1
---

# SPEC-009: EnterpriseController管理员无法访问企业详情

## 问题描述

`EnterpriseController` 类级别添加了 `@PreAuthorize("hasRole('ENTERPRISE')")`，导致管理员（ADMIN角色）无法通过 `/enterprise/{enterpriseId}` 查看企业详情。

### 影响

- 管理员无法查看指定企业信息
- 返回2004 "error.permission.denied"
- 管理后台的企业管理功能受限

### 根因分析

`EnterpriseController.java` 第28行：
```java
@PreAuthorize("hasRole('ENTERPRISE')")
@RequestMapping("/enterprise")
```

类级别限制所有端点仅企业用户可访问，但管理员应有权限查看企业信息。

## 修复方案

将类级别的 `@PreAuthorize` 移除，改为在需要企业身份的端点上单独添加：

```java
// 类级别：移除 @PreAuthorize
@RequestMapping("/enterprise")
public class EnterpriseController {

    // 仅企业用户可访问的端点
    @PreAuthorize("hasRole('ENTERPRISE')")
    @GetMapping("/info")
    public ApiResponse<Enterprise> getEnterpriseInfo() { ... }

    // 管理员也可访问的端点
    @GetMapping("/{enterpriseId}")
    public ApiResponse<Enterprise> getEnterpriseById() { ... }
}
```

## 变更文件

| 文件 | 变更 |
|------|------|
| `EnterpriseController.java` | 移除类级别 `@PreAuthorize`，按端点添加 |

## 验证结果

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| GET /enterprise/1 (admin) | 2004 权限不足 | 200 成功 |
| GET /enterprise/info (enterprise) | 200 成功 | 200 成功 |
