---
phase: "07"
plan: "02"
subsystem: "ai-prediction"
tags: ["market-prediction", "prophet", "xgboost", "fastapi", "spring-boot"]
dependency_graph:
  requires: ["07-01"]
  provides: ["market-prediction-api", "market-forecast-endpoints"]
  affects: ["oaiss-chain-ml-service", "oaiss-chain-backend"]
tech_stack:
  added: ["prophet", "xgboost", "FastAPI-router"]
  patterns: ["repository-data-aggregation", "ml-microservice-call"]
key_files:
  created:
    - "oaiss-chain-ml-service/app/services/market_service.py"
    - "oaiss-chain-ml-service/app/routers/market_router.py"
    - "oaiss-chain-ml-service/app/schemas/market.py"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/MarketPredictionService.java"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/MarketPredictionController.java"
    - "oaiss-chain-backend/src/test/java/com/oaiss/chain/service/ml/MarketPredictionServiceTest.java"
    - "oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/MarketPredictionControllerTest.java"
  modified:
    - "oaiss-chain-ml-service/app/main.py"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/constant/ErrorCode.java"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/constant/ErrorMessage.java"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/AuctionOrderRepository.java"
decisions:
  - "Prophet for price forecasting with 80% confidence intervals; XGBoost for volume trend prediction"
  - "Single MarketService.predict() method used by all 3 router endpoints (trend/price/supply-demand differ by ML route only)"
  - "Controller uses @RequestParam horizonDays rather than request body DTO for simpler GET-like API"
  - "Added INSUFFICIENT_DATA error code (6003) for data validation"
  - "Added findTop60ByDeletedFalseOrderByCreatedAtDesc repository method for historical data aggregation"
metrics:
  duration: "15min"
  completed: "2026-05-14"
---

# Phase 07 Plan 02: Market Prediction Service Summary

Prophet-based carbon market trend prediction with XGBoost supply/demand volume forecasting, exposed via FastAPI router and Spring Boot controller.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Python ML MarketService + Router + Schemas | 04cafb3 | market_service.py, market_router.py, market.py |
| 2 | Java MarketPredictionService | 04cafb3 | MarketPredictionService.java |
| 3 | Java MarketPredictionController | 04cafb3 | MarketPredictionController.java |
| 4 | Java Unit Tests | 04cafb3 | MarketPredictionServiceTest.java, MarketPredictionControllerTest.java |

## Implementation Details

### Python ML Service

- **MarketService** (`app/services/market_service.py`): Core prediction engine using Prophet for carbon price time-series forecasting with 80% confidence intervals, and XGBoost for supply/demand volume trend prediction. Includes input validation (matching list lengths, minimum 2 data points), trend classification (up/down/stable based on 2% price delta threshold), and graceful XGBoost fallback to simple average on failure.
- **Market Router** (`app/routers/market_router.py`): Three POST endpoints at `/predict/market/trend`, `/predict/market/price`, `/predict/market/supply-demand` with dependency-injected MarketService and proper HTTPException handling.
- **Market Schemas** (`app/schemas/market.py`): Pydantic v2 `MarketForecastRequest` (dates, prices, volumes, horizon_days) and `MarketForecastResponse` (forecast_dates, forecast_prices, lower_bound, upper_bound, trend, model_version) with field validation.

### Java Backend

- **MarketPredictionService**: Aggregates recent 60 auction orders via `AuctionOrderRepository`, builds `MarketForecastRequest` from trade data (dates, prices, volumes), and calls ML service endpoints. Validates minimum 10 data points before calling ML service.
- **MarketPredictionController**: Three POST endpoints under `/api/v1/ai/market` with `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` and `@RateLimit` annotations. Accepts `horizonDays` request parameter (1-365, default 30).
- **ErrorCode/ErrorMessage**: Added `INSUFFICIENT_DATA` (6003) for data validation failures.
- **AuctionOrderRepository**: Added `findTop60ByDeletedFalseOrderByCreatedAtDesc()` for historical data retrieval.

### Unit Tests

- **MarketPredictionServiceTest**: 5 tests covering all 3 prediction methods, insufficient data validation, and request building.
- **MarketPredictionControllerTest**: 6 tests covering ENTERPRISE/ADMIN access, 403 for REVIEWER/THIRD_PARTY, horizonDays validation, and API response structure.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] Added data validation for insufficient historical records**
- **Found during:** Task 2 implementation
- **Issue:** Service would pass empty or sparse data to ML service without validation
- **Fix:** Added MIN_DATA_POINTS=10 check with INSUFFICIENT_DATA BusinessException
- **Files modified:** MarketPredictionService.java, ErrorCode.java, ErrorMessage.java
- **Commit:** 04cafb3

**2. [Rule 2 - Missing Critical Functionality] Added AuctionOrderRepository query method**
- **Found during:** Task 2 implementation
- **Issue:** Service needs historical trade data but no suitable repository query existed
- **Fix:** Added `findTop60ByDeletedFalseOrderByCreatedAtDesc()` following Spring Data naming convention
- **Files modified:** AuctionOrderRepository.java
- **Commit:** 04cafb3

## Known Stubs

None - all data paths are wired end-to-end.

## Threat Flags

None - no new authentication paths, file access patterns, or trust boundaries introduced beyond existing MlServiceClient.

## Self-Check: PASSED

All created and modified files verified present on disk:
- oaiss-chain-ml-service/app/services/market_service.py: FOUND
- oaiss-chain-ml-service/app/routers/market_router.py: FOUND
- oaiss-chain-ml-service/app/schemas/market.py: FOUND
- oaiss-chain-backend/.../MarketPredictionService.java: FOUND
- oaiss-chain-backend/.../MarketPredictionController.java: FOUND
- oaiss-chain-backend/.../MarketPredictionServiceTest.java: FOUND
- oaiss-chain-backend/.../MarketPredictionControllerTest.java: FOUND
- .planning/phases/07-.../07-02-SUMMARY.md: FOUND

Commit 04cafb3 verified in git log.
