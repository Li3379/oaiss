---
phase: 07-ai-intelligent-prediction-foundation
plan: 04
type: execute
wave: 3
status: complete
completed_at: 2026-05-14
requirements: [REQ-03]
---

# Plan 07-04 Summary: CarbonPredictionService Stub → ML Prophet Regression

## Objective

Upgrade CarbonPredictionService from stub/hardcoded linear regression to real ML-based Prophet regression prediction via Python FastAPI microservice.

## Execution Results

### Task 1: Python EmissionService (Already Complete)

The Python emission service was already implemented from prior work:
- `oaiss-chain-ml-service/app/services/emission_service.py` — Prophet-based emission regression with confidence intervals and trend classification
- `oaiss-chain-ml-service/app/routers/emission_router.py` — FastAPI router at `/predict/emission/forecast`

**Path correction**: Router prefix changed from `/api/v1/predict` to `/predict/emission` to match the convention used by market_router and enterprise_router. POST path changed from `/emission` to `/forecast`. Full endpoint: `/predict/emission/forecast`.

### Task 2: CarbonPredictionService.java Refactored

**File**: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonPredictionService.java`

Changes:
- Injected `MlServiceClient` alongside existing `CarbonReportRepository`
- Replaced ALL stub/hardcoded prediction logic with ML service call via `MlServiceClient.post("/predict/emission/forecast", ...)`
- Built `EmissionForecastRequest` from historical CarbonReport data (dates + emissions)
- Mapped `EmissionForecastResponse` to existing `CarbonPredictionResponse` format
- Daily Prophet forecasts grouped into monthly averages via `groupByMonth()` method
- `predictMonths` converted to `horizonDays` (months × 30)
- Insufficient data (<2 reports) returns low-confidence response without calling ML service
- Null emission values default to 0.0
- Logging at INFO/WARN levels

### Task 2b: main.py Cleanup

**File**: `oaiss-chain-ml-service/app/main.py`

- Removed stub 501 endpoint for `/api/v1/predict/emission`
- Removed unused `EmissionForecastRequest`/`EmissionForecastResponse` imports
- Emission router already registered via `app.include_router(emission_router)`

### Task 3: EmissionController Compatibility Verified

`EmissionController` at `/emission/predict` calls `carbonPredictionService.predict(CarbonPredictionRequest)` and returns `ApiResponse<CarbonPredictionResponse>`. Public API contract unchanged — only service internals changed.

### Task 4: Unit Tests Created

**File**: `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CarbonPredictionServiceTest.java`

10 tests in 4 nested groups:
1. **Insufficient Data** (2 tests): 1 report, 0 reports → low confidence, no ML call
2. **ML Service Call** (5 tests): successful call, monthly grouping, average calculation, predictMonths→horizonDays conversion, null predictMonths default
3. **ML Service Failure** (2 tests): ML unavailable (6000), ML error (6001) → BusinessException propagated
4. **Null Emissions** (1 test): null totalEmission → 0.0 in ML request

All 10 tests pass. Backend compiles successfully.

## Verification

| Criterion | Result |
|-----------|--------|
| No stub/hardcoded values in CarbonPredictionService | PASS — grep for stub/hardcode/Math.random returns empty |
| ML service endpoint registered in main.py | PASS — emission_router included, stub removed |
| Backend compiles | PASS — mvn compile exit 0 |
| Unit tests pass | PASS — 10/10 tests pass |
| EmissionController contract unchanged | PASS — same request/response types |

## Deviations

1. Router path changed from `/api/v1/predict/emission` to `/predict/emission/forecast` — matches market/enterprise router convention (no `/api/v1` prefix in router, that's handled by FastAPI app mounting)
2. Insufficient data returns low-confidence response instead of throwing BusinessException — preserves backward compatibility with existing frontend behavior

## Files Modified

| File | Change |
|------|--------|
| `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonPredictionService.java` | Full rewrite: Stub → MlServiceClient + Prophet |
| `oaiss-chain-ml-service/app/main.py` | Removed stub endpoint, cleaned imports |
| `oaiss-chain-ml-service/app/routers/emission_router.py` | Path prefix correction |
| `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CarbonPredictionServiceTest.java` | 10 new unit tests |

## Success Criteria Met

- ✅ EmissionService.predict() returns valid Prophet-generated emission forecasts
- ✅ CarbonPredictionService calls MlServiceClient — no stub values
- ✅ Existing /api/v1/emission/predict endpoint works unchanged from client perspective
- ✅ Confidence intervals (lower_bound, upper_bound) populated from Prophet
- ✅ Trend classification (up/down/stable) returned