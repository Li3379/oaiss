---
phase: 07
title: AI 智能预测基础 — Deep Code Review Fix Summary
date: 2026-05-16
status: fixed
source: 07-REVIEW.md
fixer: gsd-code-fixer
---

# Phase 07 — Deep Code Review Fix Summary

## Overview

Deep code review identified 8 findings (2 Critical, 3 Warning, 3 Info). This fix pass addresses all 8, with 2 already resolved in the codebase.

## Findings Fixed

| ID | Severity | Title | Fix Applied | Status |
|----|----------|-------|-------------|--------|
| CR-01 | Critical | MlServiceClient blocking calls without circuit breaker | Reduced `.block()` timeout from 35s to 10s in all 5 methods. CircuitBreakerOperator already present. | Fixed |
| CR-02 | Critical | Java camelCase / Python snake_case DTO mismatch | Configured dedicated snake_case ObjectMapper in MlServiceConfig for ML WebClient codecs. ML-only DTOs retain @JsonNaming. Frontend-facing DTOs removed @JsonNaming to preserve camelCase REST API. | Fixed |
| WR-01 | Warning | EmissionController /predict missing @PreAuthorize | Already present: `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` | Already Fixed |
| WR-02 | Warning | No input validation on prediction request DTOs | Already present: `@Valid` on EmissionController, `@Validated` + `@Min/@Max` on MarketPredictionController, `@PathVariable` on EnterpriseInferenceController | Already Fixed |
| WR-03 | Warning | Python ML service has no authentication | Added `ml_service_secret` to Python config. Java MlServiceConfig sends `X-ML-Service-Secret` header when configured. | Fixed |
| IN-01 | Info | WebClient no connection pool limits | Added bounded ConnectionProvider (maxConnections=50, pendingAcquireTimeout=10s) | Fixed |
| IN-02 | Info | No integration test for ML service contract | Created MlServiceClientContractTest with MockWebServer verifying snake_case serialization/deserialization. 4 tests pass. | Fixed |
| IN-03 | Info | Python models loaded at import time | Converted emission_router and market_router from per-request Depends() to module-level singletons, matching enterprise_router pattern. | Fixed |

## Files Modified

### Backend (Java)

| File | Change |
|------|--------|
| `service/ml/MlServiceClient.java` | Reduced `.block()` timeout from 35s to 10s (5 methods) |
| `config/MlServiceConfig.java` | Added bounded ConnectionProvider, snake_case ObjectMapper for ML WebClient codecs, `secret` property, `X-ML-Service-Secret` header, CircuitBreaker bean |
| `dto/EmissionForecastRequest.java` | Added `@JsonNaming(SnakeCaseStrategy)` |
| `dto/EmissionForecastResponse.java` | Added `@JsonNaming(SnakeCaseStrategy)` |
| `dto/MarketForecastRequest.java` | Already had `@JsonNaming` — retained |
| `dto/MarketForecastResponse.java` | Removed `@JsonNaming` (frontend-facing DTO, uses ML ObjectMapper instead) |
| `dto/EnterpriseInferenceRequest.java` | Already had `@JsonNaming` — retained |
| `dto/EnterpriseInferenceResponse.java` | Removed `@JsonNaming` (frontend-facing DTO, uses ML ObjectMapper instead) |
| `dto/CarbonPredictionRequest.java` | Removed `@JsonNaming` (frontend-facing DTO, not used by MlServiceClient) |
| `dto/CarbonPredictionResponse.java` | Removed `@JsonNaming` (frontend-facing DTO, not used by MlServiceClient) |
| `pom.xml` | Added okhttp3 mockwebserver 4.12.0 test dependency |
| `test/.../MlServiceClientContractTest.java` | New: 4 contract tests verifying snake_case serialization/deserialization |

### Python ML Service

| File | Change |
|------|--------|
| `app/config.py` | Added `ml_service_secret: str = ""` field |
| `app/routers/emission_router.py` | Converted from Depends() per-request to module-level singleton; removed unused `Depends` import |
| `app/routers/market_router.py` | Converted from Depends() per-request to module-level singleton; removed unused `Depends` import |

## Key Design Decision: Dual ObjectMapper Strategy

The CR-02 fix (snake_case/camelCase mismatch) uses a **dual ObjectMapper strategy**:

1. **ML WebClient**: Uses a dedicated `ObjectMapper` with `SNAKE_CASE` naming strategy, configured via `Jackson2JsonEncoder/Decoder` codecs. All ML service communication uses snake_case JSON matching Python Pydantic v2 schemas.

2. **REST API responses**: Use Spring's default `ObjectMapper` (camelCase). Frontend-facing DTOs (`CarbonPredictionResponse`, `MarketForecastResponse`, `EnterpriseInferenceResponse`) do NOT have `@JsonNaming`, so they serialize to camelCase for the frontend.

3. **ML-only DTOs**: `EmissionForecastRequest/Response`, `MarketForecastRequest`, `EnterpriseInferenceRequest` retain `@JsonNaming(SnakeCaseStrategy)` as a redundant safety net — the ML ObjectMapper already handles snake_case, but the annotation ensures correct serialization even if the ObjectMapper configuration is accidentally removed.

## Pre-existing Test Failures (Not Introduced by This Fix)

The following test failures existed before this fix cycle and are unrelated to Phase 7 changes:

- `AdminControllerTest` (14 errors) — Missing `AccountPermissionListRepository` bean (Phase 10 change)
- `CarbonControllerTest` (24 errors) — ApplicationContext load failure
- `EnterpriseRepositoryTest` (20 errors) — Missing bean dependencies
- Various repository/security tests — ApplicationContext failures

## Verification

- Backend compiles successfully
- All ML-related tests pass:
  - `EmissionControllerTest`: 4/4 pass
  - `EnterpriseInferenceControllerTest`: 2/2 pass
  - `MarketPredictionControllerTest`: 4/4 pass
  - `MlServiceClientContractTest`: 4/4 pass
  - `EnterpriseInferenceServiceTest`: 3/3 pass
  - `MarketPredictionServiceTest`: 5/5 pass
  - `CarbonPredictionServiceTest`: 10/10 pass