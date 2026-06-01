# Roadmap: OAISS CHAIN

## Milestones

- **v1.0 Manual Testing** - Phases 1-6 (shipped 2026-05-13) — [Archive](milestones/v1.0-ROADMAP.md)
- **v1.1.0 需求对齐** - Phases 7-12 (shipped 2026-05-18) — [Archive](milestones/v1.1.0-ROADMAP.md)
- **v2.0 安全与性能加固** - Phases 13-15 (shipped 2026-05-21) — [Archive](milestones/v2.0-ROADMAP.md)
- **v2.1 测试基础设施修复与收尾** - Phases 16-18 (shipped 2026-05-22) — [Archive](milestones/v2.1-ROADMAP.md)

## Overview

OAISS CHAIN 碳交易与区块链管理平台。v1.0–v2.1 已完成。v2.1 修复 E2E 测试基础设施，补齐验收缺口，清理技术债务。

## Active Milestone

**v2.1 测试基础设施修复与收尾** — 7 requirements across 3 phases. ✅ SHIPPED 2026-05-22

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

<details>
<summary>v2.1 测试基础设施修复与收尾 (Phases 16-18) - SHIPPED 2026-05-22</summary>

- [x] Phase 16: E2E 测试基础设施修复 (2/2 plans) — Auth fixture timeout, d9/d10 孤悬测试, isFabricAvailable()
- [x] Phase 17: 验收缺口补齐与 i18n 清理 (2/2 plans) — REQ-06/REQ-03 E2E, 硬编码中文提取
- [x] Phase 18: Fabric CA 可选集成 (1/1 plan) — FabricCAService registerEnrollment, @Profile("fabric")

</details>

### Next Milestone (Planned)

**v3.0 生产部署与运维** — TBD

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
| 17. 验收缺口补齐与 i18n 清理 | v2.1 | 2/2 | Complete | 2026-05-22 |
| 18. Fabric CA 可选集成 | v2.1 | 1/1 | Complete | 2026-05-22 |

---
*Roadmap created: 2026-05-08*
*v1.0 shipped: 2026-05-13*
*v1.1.0 shipped: 2026-05-18*
*v2.0 shipped: 2026-05-21*
*v2.1 shipped: 2026-05-22*
