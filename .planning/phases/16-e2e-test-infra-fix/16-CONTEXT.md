# Phase 16: E2E 测试基础设施修复 - Context

**Gathered:** 2026-05-22
**Status:** Ready for planning

<domain>
## Phase Boundary

修复 E2E 测试基础设施三大阻塞项：(1) auth fixture `loginViaApi()` 发送 form-encoded 而非 JSON 导致 30s timeout，阻塞 68 flow + 10+ v1.1 测试；(2) d9/d10 孤悬测试不在 Playwright testDir 中从未执行；(3) `isFabricAvailable()` 已导出但无测试使用。依赖 Phase 15（v2.0 shipped）。

**Requirements:** E2E-01 (auth fixture timeout), E2E-02 (d9/d10 orphaned specs), E2E-03 (isFabricAvailable unwired)

**Success Criteria:**
1. `loginViaApi()` 正确提取 accessToken，不再 30s timeout
2. 至少 3 个 flow 测试端到端通过
3. d9/d10 specs 被 Playwright 发现并执行
4. BlockchainExplorerPage.ts 和 CarbonFormulaCalculatorPage.ts page objects 创建
5. isFabricAvailable() 在至少 1 个测试中使用

</domain>

<decisions>
## Implementation Decisions

### Auth Fixture 修复
- **D-01:** 在 `loginViaApi()` 中添加 `headers: { 'Content-Type': 'application/json' }` 修复根因。Playwright 的 `page.request.post(url, { data: obj })` 默认发送 `application/x-www-form-urlencoded`，Spring Boot `@RequestBody` 期望 JSON。加 header 后 Playwright 自动序列化为 JSON。
- **D-02:** 不统一 token 存储位置。`loginViaToken()` 用 sessionStorage，`loginViaApi()` 用 localStorage。前端 `getToken()` 两者都检查（先 localStorage 后 sessionStorage），都能正常工作。改动最小。

### d9/d10 迁移
- **D-03:** d9/d10 原样迁移到 `tests/e2e/v1.1/` 目录，不转译 TypeScript。创建缺失的 page objects (BlockchainExplorerPage.ts, CarbonFormulaCalculatorPage.ts)，更新 import 路径。不合并到已有 flow 测试（避免引入不确定性）。

### 测试前置条件
- **D-04:** 保持现状，不加额外 backend 健康检查。CI 管道 (`e2e-tests.yml`) 已有 health check 逻辑（120s timeout via actuator/health）。本地开发者自行确保后端运行。

### Claude's Discretion
- 具体的 auth.ts 代码改动细节（在 D-01 约束内选择最简洁的实现）
- d9/d10 迁移后的 import 路径修正细节
- page objects 的具体方法设计（遵循现有 page object 模式）
- isFabricAvailable() 在哪些测试文件中使用

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### E2E Test Infrastructure
- `oaiss-chain-frontend/tests/e2e/fixtures/auth.ts` — loginViaApi() 需修复的 auth fixture（D-01 修复目标）
- `oaiss-chain-frontend/tests/e2e/fixtures/test-env.ts` — isFabricAvailable() 和 isMlServiceAvailable() 定义
- `oaiss-chain-frontend/playwright.config.ts` — Playwright 配置，testDir 仅扫描 smoke/flows/v1.1/
- `oaiss-chain-frontend/tests/e2e/d9-blockchain-browser.spec.js` — 孤悬测试，需迁移到 v1.1/
- `oaiss-chain-frontend/tests/e2e/d10-carbon-report.spec.js` — 孤悬测试，需迁移到 v1.1/

### Existing Page Objects & Patterns
- `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/Layout.ts` — 现有 page object 基础模式
- `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/LoginPage.ts` — 登录 page object 模式参考
- `oaiss-chain-frontend/tests/e2e/fixtures/page-objects/CarbonUploadPage.ts` — 业务 page object 模式参考

### Backend Auth API
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AuthController.java` — 登录端点，@RequestBody 期望 JSON
- `oaiss-chain-frontend/src/utils/auth.ts` — 前端 token 存储工具，双存储策略（localStorage + sessionStorage）

### CI/CD Pipeline
- `.github/workflows/e2e-tests.yml` — CI 管道，已有 health check 和 Playwright 执行逻辑

### Planning Docs
- `.planning/REQUIREMENTS.md` — E2E-01, E2E-02, E2E-03 详细描述和验收标准
- `.planning/codebase/TESTING.md` — 项目测试框架、结构、模式完整文档

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `tests/e2e/fixtures/page-objects/` 目录已有 12+ page objects 可参考模式（constructor + goto + expectLoaded）
- `tests/e2e/fixtures/test-env.ts` 已实现 isFabricAvailable()，无需重写，只需 import
- `tests/e2e/v1.1/` 目录已存在且有 5 个测试文件，d9/d10 迁移后 import 路径需调整
- CI 管道 `e2e-tests.yml` 已配置 `TEST_MODE=v1.1 npx playwright test` 执行 v1.1 测试

### Established Patterns
- Page Object 模式：每个 page object 有 `constructor(page)`, `goto()`, `expectLoaded()` 方法
- Auth fixture 双策略：`loginViaToken()` (mock JWT, sessionStorage) 用于 smoke 测试；`loginViaApi()` (real API, localStorage) 用于 flow/v1.1 测试
- Playwright config 通过 `TEST_MODE` 环境变量切换 testDir：默认 smoke，flow，v1.1
- d9/d10 是 .js 文件，不遵循 TypeScript 约定，迁移时保持原样

### Integration Points
- `loginViaApi()` 修复后直接影响 `tests/e2e/flows/` 下 15 个 flow 测试和 `tests/e2e/v1.1/` 下 5 个 v1.1 测试
- d9/d10 迁移到 v1.1/ 后自动被 `TEST_MODE=v1.1` 发现
- isFabricAvailable() 接入后影响 blockchain 相关测试的 skip 逻辑
- `loginWithMonitor()` 依赖 `loginViaApi()`，修复后自动恢复

### Known Gaps (record, do not fix)
- d10-carbon-report.spec.js 与 flows/carbon-report-flow.spec.ts 存在部分测试用例重叠
- d9/d10 的 page objects 可能引用了不存在的路径（迁移时需验证）
- `buildStorageState()` 返回空 localStorage 数组，未被任何测试使用

</code_context>

<specifics>
## Specific Ideas

- `loginViaApi()` 修复只需在 `page.request.post()` 调用中添加 `headers: { 'Content-Type': 'application/json' }` 一行
- d9/d10 迁移只需 `mv` 文件到 `v1.1/` 目录，然后修复 import 路径中的 `../fixtures/` 相对路径
- 新建 BlockchainExplorerPage.ts 参考 EnterpriseInferencePage.ts 或 MarketPredictionPage.ts 的模式
- 新建 CarbonFormulaCalculatorPage.ts 参考 CarbonUploadPage.ts 的模式
- isFabricAvailable() 使用方式：在 blockchain 相关测试的 `test.beforeAll` 中调用，`test.skip(!available, 'Fabric not available')`

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 16-e2e-test-infra-fix*
*Context gathered: 2026-05-22*
