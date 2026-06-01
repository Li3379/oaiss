# Milestones

## v2.1 — 测试基础设施修复与收尾

**Shipped:** 2026-05-22
**Phases:** 16-18 (3 phases, 5 plans)
**Requirements:** 7/7 satisfied

### Accomplishments

1. **Auth fixture timeout 修复** — `loginViaApi()` 正确提取 accessToken，解除 68+ 测试阻塞 (Phase 16)
2. **d9/d10 孤悬测试接入** — 迁移到 v1.1/ 目录，Playwright 可发现并执行 (Phase 16)
3. **isFabricAvailable() 钩子接入** — blockchain-formula-flow.spec.ts 中使用 `test.skip` guard (Phase 16)
4. **REQ-06 碳核算公式 E2E 覆盖** — blockchain-formula-flow.spec.ts 覆盖 power-generation + power-grid (Phase 17)
5. **REQ-03 排放预测 E2E 覆盖** — ai-prediction-flow.spec.ts 包含 5 个测试用例 (Phase 17)
6. **i18n 硬编码中文清理** — 4 文件 7 处提取为 vue-i18n keys (Phase 17)
7. **Fabric CA 可选集成** — FabricCAService registerEnrollment() + @Profile("fabric") 条件装配 (Phase 18)

### Key Decisions

- D-01: EnrollmentResult made public (not package-private) for cross-package access from FabricGatewayConfig

### Stats

- Commits: 13 (2026-05-22)
- Files changed: 37
- Lines: +2814/-228
- Duration: ~3.5 hours

---

## v2.0 — 安全与性能加固

**Shipped:** 2026-05-21
**Phases:** 13-15 (3 phases, 8 plans)
**Requirements:** 19/19 satisfied

### Accomplishments

1. **并发安全** — @DistributedLock 替代 synchronized，@Version 乐观锁，事务边界重构 (Phase 13)
2. **凭据安全** — docker-compose 密码外部化，移除弱默认值，MinIO 凭据清理 (Phase 13)
3. **授权安全** — FileController/SearchController @PreAuthorize，Prometheus 认证 (Phase 13)
4. **性能优化** — Redis SCAN，@Async 缓存，订单分页，外键索引 (Phase 14)
5. **代码质量** — RSA 私钥加密，@Transactional(readOnly=true)，Repository 补全 (Phase 14)
6. **DevOps** — GitHub Actions CI/CD，Flyway validate，Trivy 扫描 (Phase 15)

---

## v1.1.0 — 需求对齐

**Shipped:** 2026-05-18
**Phases:** 7-12 (6 phases, 22 plans)
**Requirements:** 12/12 satisfied

### Accomplishments

1. AI 智能预测基础 (MarketPrediction, EnterpriseInference, CarbonPrediction)
2. AI 前端 + 碳核算公式 (发电 25 参数 + 电网 9 参数)
3. 区块链真实对接 (FabricGatewayConfig + FabricBlockchainService)
4. 准入与资格证 (EnterpriseAdmission + ReviewerQualification)
5. 前端覆盖率补齐 (39 缺失 endpoint)
6. E2E 测试与验收 (95% 覆盖)

---

## v1.0 — Manual Testing

**Shipped:** 2026-05-13
**Phases:** 1-6 (6 phases, 12 plans)
**Requirements:** 84/84 validated

### Accomplishments

1. Environment Setup & Auth Baseline
2. Carbon Report Lifecycle
3. Carbon Coin & Trading Engine
4. Carbon Neutral Projects & Credit Scoring
5. Supporting Domains
6. Cross-Cutting & Edge Cases
