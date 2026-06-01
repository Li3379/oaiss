---
status: complete
created: 2026-05-14
author: claude
source: Full-Role Business Flow Testing
---

# SPEC-006: 验证码错误与过期错误码区分

## 问题描述

登录流程中，`AuthService.validateCaptcha()` 对所有验证码验证失败统一抛出 `captchaExpired()`（错误码 2007），不区分"验证码输入错误"和"验证码已过期"两种失败场景。

### 影响

- 用户输入错误验证码时收到"验证码已过期"提示，误导用户认为验证码已失效
- 无法区分暴力破解（持续错误）和正常过期场景
- 前端无法给出精准的错误提示

### 根因分析

`CaptchaService.verifyCaptcha()` 返回 `boolean`， AuthService 的 `validateCaptcha()` 方法将所有 `false` 结果统一映射为 `captchaExpired()` 异常。

## 修复方案

### 1. 新增 `CaptchaVerifyResult` 枚举

```java
public enum CaptchaVerifyResult {
    SUCCESS,
    NOT_FOUND,
    EXPIRED,
    WRONG_CODE
}
```

### 2. 新增 `verifyCaptchaDetailed()` 方法

在 `CaptchaService` 中添加返回 `CaptchaVerifyResult` 的详细验证方法，保留原 `verifyCaptcha()` 兼容旧调用。

### 3. 更新 `AuthService.validateCaptcha()`

根据 `CaptchaVerifyResult` 抛出对应异常：
- `SUCCESS` → 正常返回
- `NOT_FOUND` / `EXPIRED` → `AuthenticationException.captchaExpired()` (2007)
- `WRONG_CODE` → `AuthenticationException.captchaError()` (2006)

## 变更文件

| 文件 | 变更 |
|------|------|
| `CaptchaVerifyResult.java` | 新增枚举类 |
| `CaptchaService.java` | 新增 `verifyCaptchaDetailed()`，重构 `verifyCaptcha()` |
| `AuthService.java` | `validateCaptcha()` 使用详细结果分发异常 |

## 验证结果

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| 验证码输入错误 | 2007 "验证码已过期" | 2006 "验证码错误" |
| 验证码不存在/过期 | 2007 "验证码已过期" | 2007 "验证码已过期" |
| CaptchaController.verify() | 正常 | 正常（向后兼容） |
