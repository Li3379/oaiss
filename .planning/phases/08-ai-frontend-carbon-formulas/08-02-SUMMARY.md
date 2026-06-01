---
phase: 08-ai-frontend-carbon-formulas
plan: 02
type: execute
wave: 2
status: complete
completed_at: 2026-05-15
requirements: [REQ-06]
---

# Plan 08-02 Summary: Power Generation 25-Parameter Formula

## Objective

Implement GB/T 32150-2015 power generation 25-parameter carbon emission formula with backend service, REST endpoint, and frontend calculator.

## Execution Results

### Task 1: Backend Formula Service + Endpoint

Files created:
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/PowerGenerationCalculationRequest.java` — 25-field DTO with Bean Validation
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/PowerGenerationCalculationResponse.java` — Response DTO with FuelEmissionDetail inner class
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/PowerGenerationFormulaService.java` — BigDecimal arithmetic (scale=4, HALF_UP), CO2_TO_C_RATIO = 44/12
- `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/PowerGenerationFormulaServiceTest.java` — 6 unit tests, all pass

Files modified:
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonController.java` — Added POST /calculate/power-generation endpoint with ENTERPRISE role guard

Formula: E_combustion = Σ(FC_i × NCV_i × CC_i × OF_i × 44/12) + E_desulfurization

### Task 2: Frontend Calculator

Files created/modified:
- `oaiss-chain-frontend/src/types/carbonFormula.ts` — Type definitions for both power generation and power grid
- `oaiss-chain-frontend/src/api/carbonFormula.ts` — API client with calculatePowerGeneration + calculatePowerGrid
- `oaiss-chain-frontend/src/views/enterprise/CarbonFormulaCalculator.vue` — Tabbed page (Power Generation + Power Grid)
- `oaiss-chain-frontend/src/router/index.ts` — Added carbon-formula route
- `oaiss-chain-frontend/src/config/menu.ts` — Added carbonFormula menu item
- `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts` — Added carbonFormula section
- `oaiss-chain-frontend/src/i18n/locales/en-US.ts` — Added carbonFormula section

## Verification

| Criterion | Result |
|-----------|--------|
| Backend compiles | PASS — mvn compile BUILD SUCCESS |
| Unit tests pass | PASS — 6/6 PowerGenerationFormulaServiceTest |
| POST /calculate/power-generation | PASS — endpoint registered with ENTERPRISE role |
| Frontend TypeScript | PASS — 0 errors in new files |
| Vite build | PASS — built in ~1.3s |

## Files Modified

| File | Change |
|------|--------|
| PowerGenerationCalculationRequest.java | NEW — 25-field request DTO |
| PowerGenerationCalculationResponse.java | NEW — response with breakdown |
| PowerGenerationFormulaService.java | NEW — GB/T 32150-2015 formula |
| CarbonController.java | MODIFIED — added power-generation endpoint |
| PowerGenerationFormulaServiceTest.java | NEW — 6 unit tests |
| carbonFormula.ts (types) | NEW — type definitions |
| carbonFormula.ts (api) | NEW — API client |
| CarbonFormulaCalculator.vue | NEW — tabbed calculator page |
| router/index.ts | MODIFIED — added route |
| menu.ts | MODIFIED — added menu item |
| zh-CN.ts / en-US.ts | MODIFIED — added i18n |

## Success Criteria Met

- ✅ PowerGenerationFormulaService implements GB/T 32150-2015 formula
- ✅ All 6 unit tests pass (zeros, known values, desulfurization, negative FC, OF>1, null params)
- ✅ CarbonController POST /calculate/power-generation endpoint with ENTERPRISE role guard
- ✅ Frontend calculator form with 25 parameter inputs
- ✅ Route, menu, and i18n entries registered
