---
status: complete
created: 2026-05-14
author: claude
source: UltraQA API Testing Cycle 1
---

# SPEC-008: 审核员角色权限不匹配

## 问题描述

`CarbonNeutralProjectController` 的 `/pending-verification` 端点要求 `VERIFIER` 或 `ADMIN` 角色，但审核员用户的角色是 `REVIEWER`，导致审核员无法访问待验证项目列表。

### 影响

- 审核员无法通过API访问 `/carbon-neutral/pending-verification`
- 返回2004 "error.permission.denied"
- 审核员的核心工作流（验证碳中和项目）被阻断

### 根因分析

`CarbonNeutralProjectController.java` 第310行：
```java
@PreAuthorize("hasRole('VERIFIER') or hasRole('ADMIN')")
@GetMapping("/pending-verification")
```

但系统中审核员的角色名称是 `REVIEWER`（`UserTypeEnum.REVIEWER`），不是 `VERIFIER`。

## 修复方案

将 `@PreAuthorize` 注解中的 `VERIFIER` 改为 `REVIEWER`：

```java
@PreAuthorize("hasRole('REVIEWER') or hasRole('ADMIN')")
@GetMapping("/pending-verification")
```

同时检查其他使用 `VERIFIER` 角色的端点是否也需要修复。

## 变更文件

| 文件 | 变更 |
|------|------|
| `CarbonNeutralProjectController.java` | `VERIFIER` → `REVIEWER` |

## 验证结果

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| GET /carbon-neutral/pending-verification (reviewer) | 2004 权限不足 | 200 成功 |
