# Phase 16: E2E 测试基础设施修复 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-22
**Phase:** 16-e2e-test-infra-fix
**Areas discussed:** Auth fixture 修复策略, d9/d10 迁移方式, 测试前置条件与健壮性

---

## Auth Fixture 修复策略

| Option | Description | Selected |
|--------|-------------|----------|
| A. 加 Content-Type header | 在 loginViaApi() 中添加 headers: { 'Content-Type': 'application/json' }。最小改动，直接修复根因 | ✓ |
| B. request.newContext 独立请求 | 创建独立 APIRequestContext 发送登录请求，更健壮但增加复杂度 | |
| C. 统一使用 loginViaToken mock token | 不依赖后端 auth API，但 mock token 无法通过 JWT 验证 | |

**Token 存储位置（子问题）：**

| Option | Description | Selected |
|--------|-------------|----------|
| 不统一存储位置 | loginViaToken 用 sessionStorage，loginViaApi 用 localStorage，两者都能工作 | ✓ |
| 统一为 localStorage | 将 loginViaToken 也改为 localStorage（加 remember_me），减少认知复杂度 | |

**User's choice:** A (加 Content-Type header) + 不统一存储位置
**Notes:** 根因确认 — Playwright `page.request.post(url, { data: obj })` 默认 form-encoded，Spring Boot `@RequestBody` 期望 JSON。前端 `getToken()` 检查两个 storage 所以都能工作。

---

## d9/d10 迁移方式

| Option | Description | Selected |
|--------|-------------|----------|
| A. 原样迁移到 v1.1/ | 直接将 .js 文件移到 v1.1/ 目录，创建缺失 page objects，最小改动 | ✓ |
| B. 转译 TypeScript 重写 | 转为 TypeScript，使用现有 page object 模式重写，代码质量更高但工作量大 | |
| C. d9 迁移 + d10 合并 | 只迁移 d9（blockchain 无重叠），d10 内容合并到已有 carbon-report-flow.spec.ts | |

**User's choice:** A (原样迁移)
**Notes:** d9/d10 是 .js 文件（非 TypeScript），保持原样避免引入新 bug。需创建 BlockchainExplorerPage.ts 和 CarbonFormulaCalculatorPage.ts。

---

## 测试前置条件与健壮性

| Option | Description | Selected |
|--------|-------------|----------|
| A. 保持现状 | CI 管道已处理 health check，本地开发者自行确保后端运行 | ✓ |
| B. 加 backend 健康检查快速失败 | 在 loginViaApi 中加 5s HEAD /actuator/health 检查，不可用时立即报错 | |
| C. globalSetup 检查 + skip | 在 playwright.config.ts globalSetup 中检查后端，不可用时跳过所有测试 | |

**User's choice:** A (保持现状)
**Notes:** CI 管道 (`e2e-tests.yml`) 已有 120s health check 逻辑。本地开发环境手动管理即可，不增加额外复杂度。

---

## Claude's Discretion

- auth.ts 具体代码改动细节（在 D-01 约束内选择最简洁实现）
- d9/d10 迁移后的 import 路径修正细节
- page objects 的具体方法设计（遵循现有 Layout.ts / CarbonUploadPage.ts 模式）
- isFabricAvailable() 在哪些测试文件中使用

## Deferred Ideas

None — discussion stayed within phase scope
