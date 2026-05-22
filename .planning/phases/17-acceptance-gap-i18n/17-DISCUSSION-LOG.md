---
phase: 17-acceptance-gap-i18n
date: 2026-05-22
---

# Phase 17 Discussion Log

## Phase Overview

Phase 17 covers 3 requirements: GAP-01 (REQ-06 carbon formula E2E), GAP-02 (REQ-03 emission predict E2E), I18N-01 (hardcoded Chinese extraction).

## Analysis

### GAP-01/GAP-02 Status

Codebase scouting revealed that the "gap" tests already exist:

- `blockchain-formula-flow.spec.ts` — 7 test cases covering power-generation (25-param), power-grid (9-param), formula calculator frontend, and blockchain API
- `ai-prediction-flow.spec.ts` — 5 test cases covering `/emission/predict` with various scenarios (valid data, data points, optional params, validation, rate limit)
- `CORE_ENDPOINTS` — 3 entries for carbon formulas and emission prediction (coverage-report.ts:69-73)

The real blocker was the auth fixture timeout (fixed in Phase 16). **No new test code needed.**

### I18N-01 Hardcoded Chinese

Found 2 instances in Vue views:
- `VerifyList.vue:123` — status code 5 maps to `'已上链'` (not using i18n)
- `Monitor.vue:138` — `t('monitor.statApprovalRate') || '审核通过率'` (fallback Chinese)

Found ~20+ instances in API files (validation messages like `'用户名不能为空'`).

Scoping to 4 files matching ROADMAP's "4 files 7 places" target.

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| D-01 | GAP-01/02 verification-only | Tests already exist, Phase 16 unblocked them |
| D-02 | Skip CarbonFormulaCalculatorPage.ts | No consumer, speculative |
| D-03 | I18N scope: 4 files ~5 instances | VerifyList.vue, Monitor.vue, auth.ts, emission.ts |
| D-04 | API i18n via `i18n.global.t()` | Standard pattern for non-component files |

## User Interaction

User chose "自行分析并决策" for both questions — deferred all decisions to Claude's analysis.
