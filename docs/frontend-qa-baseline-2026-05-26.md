# Frontend QA Baseline 2026-05-26

- Date: 2026-05-26
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080/api/v1`
- Primary suite: `oaiss-chain-frontend/tests/e2e/smoke/full-functional.spec.ts`
- Full regression evidence:
  - `oaiss-chain-frontend/test-results/oaiss-full-functional-2026-05-26T10-32-50-304Z/full-functional-report.md`
- Working QA log:
  - `.gstack/qa-reports/qa-report-localhost-2026-05-22.md`

## Baseline Result

- Total: `82`
- Passed: `81`
- Failed: `0`
- Skipped: `1`
- Pass rate: `98.78%`

## Current Status

- No active functional failures remain in the latest enterprise-led frontend full regression baseline.
- `S15-02` password change remains intentionally skipped to preserve the shared `enterprise001 / admin123` credential for subsequent shared-session runs.

## Key Closures Validated On 2026-05-26

- `S2-03` order detail action passes after selector stabilization in `full-functional.spec.ts`.
- `S4-04` P2P confirm flow is present and passes live regression.
- `S7-04` credit ranking UI is available and passes live regression.
- `S9-02` blockchain status indicator is visible and passes live regression.
- `S9-04` blockchain transaction hash query is available and passes live regression.
- `S10-05` and `S10-06` carbon-neutral project submit/detail lifecycle pass live regression.
- `S12-02` and `S12-03` carbon formula calculator flows pass live regression.
- `S15-05` digital signature management area passes live regression.
- `PROJECT-REVIEW-01` and `PROJECT-REVIEW-02` were re-closed after reviewer queue probe stabilization confirmed verify and deduct actions are both exposed.
- `ENTERPRISE-INFERENCE-SEMANTIC-02` was fixed by normalizing backend status variants and aligning localized UI rendering.

## Source/Test Changes Associated With This Baseline

- `oaiss-chain-frontend/src/views/enterprise/EnterpriseInference.vue`
- `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts`
- `oaiss-chain-frontend/src/i18n/locales/en-US.ts`
- `oaiss-chain-frontend/tests/e2e/smoke/full-functional.spec.ts`
- `oaiss-chain-frontend/tests/e2e/smoke/enterprise-inference-clamp-probe.spec.ts`
- `oaiss-chain-frontend/tests/e2e/smoke/company-dashboard-summary-time-probe.spec.ts`
- `oaiss-chain-frontend/tests/e2e/smoke/role-lifecycle-probe.spec.ts`

## Notes

- The detailed exploratory history, batch-by-batch closures, and probe artifacts remain in `.gstack/qa-reports/qa-report-localhost-2026-05-22.md`.
- The `.gstack/` directory is git-ignored, so this file exists to preserve the latest validated baseline in tracked project documentation.
