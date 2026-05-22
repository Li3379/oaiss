# Roadmap: OAISS CHAIN

## Milestones

- **v1.0 Manual Testing** - Phases 1-6 (shipped 2026-05-13) — [Archive](milestones/v1.0-ROADMAP.md)
- **v1.1.0 需求对齐** - Phases 7-12 (shipped 2026-05-18) — [Archive](milestones/v1.1.0-ROADMAP.md)
- **v2.0 安全与性能加固** - Phases 13-15 (shipped 2026-05-21) — [Archive](milestones/v2.0-ROADMAP.md)
- **v2.1 测试基础设施修复与收尾** - Phases 16-18 (active)

## Overview

OAISS CHAIN 碳交易与区块链管理平台。v1.0–v2.0 已完成。v2.1 修复 E2E 测试基础设施，补齐验收缺口，清理技术债务。

## Active Milestone

**v2.1 测试基础设施修复与收尾** — 7 requirements across 3 phases.

## Phase Details

<details>
<summary>v1.0 Manual Testing (Phases 1-6) - SHIPPED 2026-05-13</summary>

- [x] Phase 1: Environment Setup & Auth Baseline (2/2 plans)
- [x] Phase 2: Carbon Report Lifecycle (3/3 plans)
- [x] Phase 3: Carbon Coin & Trading Engine (3/3 plans)
- [x] Phase 4: Carbon Neutral Projects & Credit Scoring (2/2 plans)
- [x] Phase 5: Supporting Domains (7/7 plans)
- [x] Phase 6: Cross-Cutting & Edge Cases (3/3 plans)

</details>

<details>
<summary>v1.1.0 需求对齐 (Phases 7-12) - SHIPPED 2026-05-18</summary>

- [x] Phase 7: AI 智能预测基础 (4/4 plans)
- [x] Phase 8: AI 前端 + 碳核算公式 (3/3 plans)
- [x] Phase 9: 区块链真实对接 (3/3 plans)
- [x] Phase 10: 准入与资格证 (3/3 plans)
- [x] Phase 11: 前端覆盖率补齐 (4/4 plans)
- [x] Phase 12: E2E 测试与验收 (6/6 plans)

</details>

<details>
<summary>v2.0 安全与性能加固 (Phases 13-15) - SHIPPED 2026-05-21</summary>

- [x] Phase 13: 并发安全与凭据加固 (3/3 plans) — @DistributedLock, @Version, 凭据外部化, @PreAuthorize
- [x] Phase 14: 性能优化与代码质量 (2/2 plans) — Redis SCAN, @Async, FK indexes, RSA encryption, readOnly=true
- [x] Phase 15: DevOps 与回归验证 (3/3 plans) — CI/CD Trivy, Flyway validate, E2E 回归无 v2.0 新增回归

</details>

### v2.1 测试基础设施修复与收尾 (Phases 16-18) - ACTIVE

#### Phase 16: E2E 测试基础设施修复

**Goal**: 修复 auth fixture timeout，接入 d9/d10 孤悬测试，接入 isFabricAvailable() 钩子
**Depends on**: Phase 15 (v2.0 shipped)
**Requirements**: E2E-01, E2E-02, E2E-03
**Priority**: HIGH — 解除 78+ 测试阻塞
**Plans**: 2 plans

Plans:
- [x] 16-01-PLAN.md — Auth Fixture Fix: 修复 loginViaApi token 提取，恢复 flow/v1.1 测试运行 (E2E-01)
- [x] 16-02-PLAN.md — Test Discovery Fix: 迁移 d9/d10 到 v1.1 目录，创建缺失 page objects，接入 isFabricAvailable (E2E-02, E2E-03)

Success Criteria:
1. loginViaApi() 正确提取 accessToken，不再 30s timeout
2. 至少 3 个 flow 测试端到端通过
3. d9/d10 specs 被 Playwright 发现并执行
4. BlockchainExplorerPage.ts 和 CarbonFormulaCalculatorPage.ts page objects 创建
5. isFabricAvailable() 在至少 1 个测试中使用

#### Phase 17: 验收缺口补齐与 i18n 清理

**Goal**: 补齐 REQ-06/REQ-03 E2E 测试，提取硬编码中文到 i18n
**Depends on**: Phase 16 (auth fixture 必须先修复)
**Requirements**: GAP-01, GAP-02, I18N-01
**Priority**: HIGH — 补齐 v1.1.0 验收缺口
**Plans**: 2 plans

Plans:
- [ ] 17-01-PLAN.md — E2E Gap Tests: 碳核算公式测试 + 排放预测测试 + CORE_ENDPOINTS 更新 (GAP-01, GAP-02)
- [ ] 17-02-PLAN.md — i18n Cleanup: 提取 4 文件 7 处硬编码中文到 vue-i18n keys (I18N-01)

Success Criteria:
1. blockchain-formula-flow.spec.ts 覆盖 /carbon/calculate/power-generation 和 /carbon/calculate/power-grid
2. ai-prediction-flow.spec.ts 包含 /emission/predict 测试用例
3. CORE_ENDPOINTS 包含碳核算公式和排放预测 endpoint
4. 4 文件 7 处硬编码中文提取为 i18n key
5. zh-CN.ts 和 en-US.ts 包含对应翻译

#### Phase 18: Fabric CA 可选集成

**Goal**: 实现 FabricCAService registerEnrollment，集成到现有 Fabric 区块链流程
**Depends on**: Phase 16 (E2E 基础设施就绪)
**Requirements**: FABRIC-01
**Priority**: LOW — 可选功能
**Plans**: 1 plan

Plans:
- [ ] 18-01-PLAN.md — Fabric CA Integration: FabricCAService registerEnrollment + @Profile("fabric") 条件装配 (FABRIC-01)

Success Criteria:
1. FabricCAService 实现 registerEnrollment()
2. CA 服务不可用时不影响 Fabric 基本功能
3. @Profile("fabric") 条件装配正确
4. E2E 测试中 isFabricAvailable() 覆盖 CA 功能

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Environment Setup & Auth Baseline | v1.0 | 2/2 | Complete | 2026-05-08 |
| 2. Carbon Report Lifecycle | v1.0 | 3/3 | Complete | 2026-05-09 |
| 3. Carbon Coin & Trading Engine | v1.0 | 3/3 | Complete | 2026-05-09 |
| 4. Carbon Neutral Projects & Credit Scoring | v1.0 | 2/2 | Complete | 2026-05-09 |
| 5. Supporting Domains | v1.0 | 7/7 | Complete | 2026-05-09 |
| 6. Cross-Cutting & Edge Cases | v1.0 | 3/3 | Complete | 2026-05-13 |
| 7. AI 智能预测基础 | v1.1.0 | 4/4 | Complete | 2026-05-14 |
| 8. AI 前端 + 碳核算公式 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 9. 区块链真实对接 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 10. 准入与资格证 | v1.1.0 | 3/3 | Complete | 2026-05-15 |
| 11. 前端覆盖率补齐 | v1.1.0 | 4/4 | Complete | 2026-05-16 |
| 12. E2E 测试与验收 | v1.1.0 | 6/6 | Complete | 2026-05-17 |
| 13. 并发安全与凭据加固 | v2.0 | 3/3 | Complete | 2026-05-19 |
| 14. 性能优化与代码质量 | v2.0 | 2/2 | Complete | 2026-05-20 |
| 15. DevOps 与回归验证 | v2.0 | 3/3 | Complete | 2026-05-21 |
| 16. E2E 测试基础设施修复 | v2.1 | 2/2 | Complete | 2026-05-22 |
| 17. 验收缺口补齐与 i18n 清理 | v2.1 | 0/2 | Not started | — |
| 18. Fabric CA 可选集成 | v2.1 | 0/1 | Not started | — |

---
*Roadmap created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
*v2.0 shipped: 2026-05-21*
*v2.1 started: 2026-05-22*
