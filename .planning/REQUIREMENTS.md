# REQUIREMENTS: v2.1 测试基础设施修复与收尾

> Milestone: v2.1 | Created: 2026-05-22 | Status: DRAFT
> All items are deferred from v2.0 milestone — existing features needing test coverage and cleanup

## Overview

v2.1 修复 E2E 测试基础设施（auth fixture timeout 导致 68 flow 测试失败），补齐 v1.1.0 验收缺口（REQ-06 碳核算公式 E2E、REQ-03 /emission/predict），清理 i18n 残留，完成 Fabric CA 可选集成。共 4 类需求。

## Category A — E2E 测试基础设施 (HIGH)

### E2E-01: Auth fixture timeout 修复

**Problem:** Flow 和 v1.1 测试使用 `loginViaApi()` 调用 `POST /api/v1/auth/login`，返回的 `accessToken` 为 undefined 或 auth fixture 无法正确提取，导致所有 flow 测试在 30s 超时。
**Evidence:** 15-03-SUMMARY.md — 68 flow + 10+ v1.1 测试失败，分类为 Category 1: Authentication/API Timeout
**Solution:** 修复 `fixtures/auth.ts` 中 `loginViaApi()` 的 token 提取逻辑，确保与当前 API 响应格式兼容。
**Acceptance Criteria:**
- [ ] `loginViaApi()` 正确提取 `accessToken`
- [ ] Flow 测试不再因 auth timeout 失败
- [ ] 至少 1 个 flow 测试端到端通过

### E2E-02: d9/d10 孤悬测试接入

**Problem:** `d9-blockchain-browser.spec.js` (11 tests) 和 `d10-carbon-report.spec.js` (14 tests) 位于 `tests/e2e/` 根目录，不在 Playwright config 的任何 testDir 中，从未被执行。
**Evidence:** 12-VERIFICATION.md — "NOT_WIRED: File at tests/e2e/ root, not in any configured testDir"
**Solution:** 将 d9/d10 specs 迁移到 `tests/e2e/v1.1/` 目录，或更新 `playwright.config.ts` 增加对应的 testDir/mode。同时更新 page objects 路径。
**Acceptance Criteria:**
- [ ] d9/d10 specs 被 Playwright config 发现并执行
- [ ] 测试用例在 TEST_MODE=v1.1 下可运行
- [ ] 对应的 page objects (BlockchainExplorerPage, CarbonFormulaCalculatorPage) 创建

### E2E-03: isFabricAvailable() 测试钩子接入

**Problem:** `test-env.ts` 导出 `isFabricAvailable()` 但无任何测试文件 import/使用。
**Evidence:** 12-VERIFICATION.md — "isFabricAvailable() exported but never imported/used in any test"
**Solution:** 在需要 Fabric 的测试中 import `isFabricAvailable`，添加 `test.skip` 逻辑。
**Acceptance Criteria:**
- [ ] 至少 1 个测试文件使用 `isFabricAvailable()` 做 skip 判断
- [ ] Fabric 不可用时测试优雅跳过

## Category B — 验收缺口补齐 (HIGH)

### GAP-01: REQ-06 碳核算公式 E2E 测试

**Problem:** 碳核算公式后端已完全实现（CarbonController `/calculate/power-generation` 和 `/calculate/power-grid`、PowerGenerationFormulaService、PowerGridFormulaService、前端 CarbonFormulaCalculator.vue），但无任何 E2E 测试。12-ACCEPTANCE-REPORT.md 错误声称"no backend controller exists"。
**Evidence:** 12-VERIFICATION.md — "REQ-06 Carbon Formulas NOT COVERED: zero E2E tests exist despite fully implemented backend"
**Solution:** 创建 `blockchain-formula-flow.spec.ts` (v1.1 目录) 和 `CarbonFormulaCalculatorPage.ts` page object，覆盖发电公式和电网公式计算流程。
**Acceptance Criteria:**
- [ ] `tests/e2e/v1.1/blockchain-formula-flow.spec.ts` 创建
- [ ] 测试覆盖 `/carbon/calculate/power-generation` 和 `/carbon/calculate/power-grid`
- [ ] `CarbonFormulaCalculatorPage.ts` page object 创建
- [ ] CORE_ENDPOINTS 数组包含碳核算公式 endpoint

### GAP-02: REQ-03 /emission/predict E2E 测试

**Problem:** `/emission/predict` endpoint (EmissionController line 96-107) 无 E2E 测试覆盖，且未被列入 `coverage-report.ts` 的 CORE_ENDPOINTS 数组。
**Evidence:** 12-VERIFICATION.md — "REQ-03 /emission/predict endpoint not tested, not in CORE_ENDPOINTS"
**Solution:** 在 `ai-prediction-flow.spec.ts` 中添加 emission predict 测试用例，将 `/emission/predict` 加入 CORE_ENDPOINTS。
**Acceptance Criteria:**
- [ ] ai-prediction-flow.spec.ts 包含 `/emission/predict` 测试用例
- [ ] CORE_ENDPOINTS 包含 `/emission/predict`
- [ ] ML 服务不可用时测试优雅 skip

## Category C — 代码清理 (MEDIUM)

### I18N-01: M19 硬编码中文提取

**Problem:** 4 个前端文件共 7 处硬编码中文字符串，未通过 vue-i18n `$t()` 函数调用。
**Evidence:** v1.1.0 代码审查发现，v2.0 延期记录
**Solution:** 将硬编码中文提取到 `i18n/locales/zh-CN.ts` 和 `en-US.ts`，替换为 `$t('key')` 调用。
**Acceptance Criteria:**
- [ ] 4 个文件的 7 处硬编码中文提取为 i18n key
- [ ] zh-CN.ts 和 en-US.ts 包含对应翻译
- [ ] 界面显示无变化（回归验证）

## Category D — 可选功能 (LOW)

### FABRIC-01: Fabric CA 应用层集成

**Problem:** FabricProperties 已有 `ca` 嵌套配置 (adminId, adminPassword, url)，但应用层无 CA 注册/登记逻辑。Fabric CA 为 REQ-12 可选需求。
**Evidence:** Phase 9 FabricProperties.ca 配置存在，无对应 Service 实现
**Solution:** 实现 FabricCAService，提供 registerEnrollment() 方法，集成到现有 Fabric 区块链流程。
**Acceptance Criteria:**
- [ ] FabricCAService 实现 registerEnrollment()
- [ ] CA 服务不可用时不影响 Fabric 基本功能
- [ ] @Profile("fabric") 条件装配

## Out of Scope

| Item | Reason |
|------|--------|
| 14-01-SUMMARY.md 补写 | 纯文档，可在任何时间补写 |
| E2E smoke route 失败修复 | 需要排查前端路由配置，范围不明确 |
| Prophet Windows 修复 | 环境问题（需 CmdStan），非代码问题 |
| 拍卖订单分页化实现 | PERF-04 已记录为设计保留无界 |

## Success Criteria

v2.1 完成标准：
1. **E2E auth 修复** — flow 测试不再因 auth timeout 全部失败
2. **零孤悬测试** — d9/d10 接入 Playwright config，可被发现和执行
3. **REQ-06/REQ-03 覆盖** — 碳核算公式和排放预测有 E2E 测试
4. **i18n 零硬编码** — 7 处硬编码中文全部提取
5. **Fabric CA 可选集成** — CA 服务可用时增强，不可用时不影响基本功能
