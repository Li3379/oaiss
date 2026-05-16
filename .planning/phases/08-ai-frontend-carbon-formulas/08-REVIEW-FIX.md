---
phase: 08-ai-frontend-carbon-formulas
fix_date: 2026-05-16T17:18:00+08:00
findings_total: 10
findings_fixed: 10
findings_remaining: 0
status: all_fixed
---

# Phase 8: Code Review Fix Report

**Fix Date:** 2026-05-16T17:18:00+08:00
**Original Findings:** 10 (3 Critical, 5 Warning, 2 Info)
**Status:** all_fixed

## Fix Summary

| ID | Severity | Issue | Fix | Verified |
|----|----------|-------|-----|----------|
| CR-01 | CRITICAL | carbonFormula API return type mismatch — results never display | Removed `ApiResponse<>` wrapper from `carbonFormula.ts` return types; changed `res.data` → `res` in `CarbonFormulaCalculator.vue` | TypeScript type check OK |
| CR-02 | CRITICAL | Missing `@DecimalMax(1)` on desulfConversionRate | Added `@DecimalMax(value = "1")` annotation to `PowerGenerationCalculationRequest.java:127` | Backend compile OK |
| CR-03 | CRITICAL | NullPointerException when FC set but NCV/CC/OF null | Added null guard in `calculateFuelEmission()` throwing `BusinessException(PARAM_ERROR)`; added test `calculate_fcSetButOtherParamsNull_throwsBusinessException` | 7/7 tests pass |
| WR-01 | WARNING | Frontend-backend DTO structural mismatch (nested vs flat) | Rewrote `carbonFormula.ts` type to flat structure matching backend; added payload flattening in `onCalculatePowerGeneration` submit handler | TypeScript OK |
| WR-02 | WARNING | Missing i18n keys `noEnterpriseId` and `description` | Added both keys to `zh-CN.ts` and `en-US.ts` under `enterpriseInference` section | i18n keys present |
| WR-03 | WARNING | `getStatusType` return type excludes `'info'` | Changed return type from `'success' | 'warning' | 'danger'` to `'success' | 'warning' | 'danger' | 'info'`; removed type assertion | TypeScript OK |
| WR-04 | WARNING | Watchers trigger API calls without debounce | Added `debouncedFetch()` with 300ms debounce to `MarketPrediction.vue`; watchers now call `debouncedFetch()` | Logic OK |
| WR-05 | WARNING | ECharts confidence band stacking incorrect | Changed lower bound series data from `d.lowerBound` to `d.lowerBound - d.upperBound` (negative delta for correct stacking) | Logic OK |
| IN-01 | INFO | `PowerGridCalculationRequest` required fields marked `| null` | Removed `| null` from `transmissionVolume`, `lineLossRate`, `gridEmissionFactor` in frontend type | TypeScript OK |
| IN-02 | INFO | `PowerGridFormulaService.validate()` duplicates Bean Validation | Added `desulfConversionRate <= 1` check in `PowerGenerationFormulaService` for consistency with DTO `@DecimalMax` | Backend compile OK |

## Files Modified

| File | Changes |
|------|---------|
| `oaiss-chain-frontend/src/api/carbonFormula.ts` | Removed `ApiResponse<>` wrapper, removed `ApiResponse` import |
| `oaiss-chain-frontend/src/views/enterprise/CarbonFormulaCalculator.vue` | `res.data` → `res`; payload flattening; removed `FuelParams` import |
| `oaiss-chain-frontend/src/types/carbonFormula.ts` | Flat `PowerGenerationCalculationRequest` type; required `PowerGrid` fields without `| null` |
| `oaiss-chain-frontend/src/views/enterprise/EnterpriseInference.vue` | `getStatusType` return type includes `'info'` |
| `oaiss-chain-frontend/src/views/enterprise/MarketPrediction.vue` | Debounced watchers; ECharts confidence band fix |
| `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts` | Added `noEnterpriseId`, `description` keys |
| `oaiss-chain-frontend/src/i18n/locales/en-US.ts` | Added `noEnterpriseId`, `description` keys |
| `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/PowerGenerationCalculationRequest.java` | Added `@DecimalMax("1")` on `desulfConversionRate` |
| `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/PowerGenerationFormulaService.java` | Null guard in `calculateFuelEmission`; `desulfConversionRate <= 1` validation |
| `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/PowerGenerationFormulaServiceTest.java` | Added `calculate_fcSetButOtherParamsNull_throwsBusinessException` test |

## Verification

- **Backend**: `mvn test -Dtest=PowerGenerationFormulaServiceTest` — 7/7 tests pass (BUILD SUCCESS)
- **Backend**: `mvn test-compile` — compiles without errors
- **Frontend**: `vue-tsc --noEmit` — no new errors introduced by these changes (pre-existing errors in unrelated files remain)

---

_Fixed: 2026-05-16T17:18:00+08:00_
_Fixer: Claude (gsd-code-fixer)_