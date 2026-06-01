# EVAL-REVIEW -- Phase 7: AI Intelligent Prediction Foundation

**Audit Date:** 2026-05-15
**AI-SPEC Present:** Yes (domain context only, no AI-specific eval strategy)
**Overall Score:** 55/100
**Verdict:** NEEDS WORK

## Dimension Coverage

| Dimension | Status | Finding |
|-----------|--------|---------|
| Functional Correctness - MarketPrediction | PARTIAL | MarketPredictionServiceTest (5 tests) mocks MlServiceClient and verifies price/supply-demand prediction returns. MarketPredictionControllerTest (4 tests) verifies ApiResponse envelope. No integration test against the actual Python ML service. No contract test validating Prophet output structure. |
| Functional Correctness - EnterpriseInference | PARTIAL | EnterpriseInferenceServiceTest (5 tests) mocks MlServiceClient and tests anomaly detection + compliance classification paths. EnterpriseInferenceControllerTest (2 tests) verifies ApiResponse envelope. Same mock-only gap. |
| Functional Correctness - CarbonPrediction | PARTIAL | CarbonPredictionServiceTest (10 tests) mocks MlServiceClient and tests emission prediction including insufficient data scenarios. EmissionControllerTest covers the prediction endpoint. Same mock-only gap. |
| Robustness - ML Service Unavailable | COVERED | All three service tests verify graceful degradation when MlServiceClient throws exceptions. Services catch WebClientResponseException and return fallback data. |
| Robustness - Invalid horizonDays | PARTIAL | MarketPredictionControllerTest validates horizonDays=400 returns 400. MarketPredictionController has `@Validated` + `@Min(1) @Max(365)` on horizonDays. EnterpriseInferenceController and EmissionController do NOT validate horizonDays bounds. |
| Robustness - ML Service Timeout | MISSING | No test for WebClient timeout behavior. MlServiceConfig has connectTimeout=5s and readTimeout=30s, but no test verifies timeout handling. No circuit breaker. |
| Robustness - Empty/No Data | PARTIAL | CarbonPredictionServiceTest has `InsufficientDataTests` (2 tests) verifying behavior with 0 or 1 reports. EnterpriseInferenceServiceTest covers "no reports" scenario. No test for ML service returning empty prediction arrays. |
| Performance - ML Inference Latency | MISSING | No latency assertion in any test. No benchmark test. |
| Performance - WebClient Timeout Config | PARTIAL | MlServiceConfig.java configures connectTimeout=5s and readTimeout=30s. Config exists but not tested. |
| Performance - Rate Limiting on AI Endpoints | PARTIAL | MarketPredictionController has `@RateLimit` on all 3 endpoints. EnterpriseInferenceController has `@RateLimit` on inference endpoint. **EmissionController `/predict` is missing `@RateLimit`**. |
| Security - Role-Based Access | PARTIAL | MarketPredictionController: `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` on all 3 endpoints. EnterpriseInferenceController: `@PreAuthorize("hasAnyRole('ENTERPRISE', 'REVIEWER', 'THIRD_PARTY', 'ADMIN')")` on inference endpoint. **EmissionController `/predict` is missing `@PreAuthorize`** — any authenticated user can invoke AI prediction. |
| Security - Input Validation on horizonDays | PARTIAL | MarketPredictionController has `@Min(1) @Max(365)` via `@Validated`. EnterpriseInferenceController and EmissionController do NOT validate horizonDays. |
| Security - No Sensitive Data Leakage in Errors | MISSING | No test verifies error responses don't leak ML service URLs, stack traces, or internal details. |
| Guardrails - Prediction Confidence Bounds | PARTIAL | ML DTOs include lowerBound/upperBound fields. Service tests mock these. No test enforces responses ALWAYS include confidence bounds. |
| Guardrails - Model Version Stamped | PARTIAL | ML DTOs include modelVersion field. Service tests mock this. No test enforces every response carries modelVersion. |
| Guardrails - Fallback When ML Down | COVERED | All three service tests verify graceful degradation when ML service is unavailable. |
| Production Monitoring - Prometheus Metrics | MISSING | No Micrometer/MetricsRegistry usage in any ML service or controller. No `@Timed` annotations. Actuator dependency present but no ML-specific metrics. |
| Production Monitoring - Error Rate Tracking | MISSING | No error rate metric for ML service calls. |
| Production Monitoring - Latency Histograms | MISSING | No latency histogram for ML inference calls. |

**Coverage Score:** 2/19 COVERED (10.5%), 8 PARTIAL (42.1%), 9 MISSING (47.4%)

Detailed count:
- COVERED: 2 (ML Service Unavailable fallback, Fallback When ML Down)
- PARTIAL: 8 (MarketPrediction functional, EnterpriseInference functional, CarbonPrediction functional, Invalid horizonDays, Empty/No Data, Rate limiting, Role-based access, Input validation)
- MISSING: 9 (ML timeout, Inference latency, WebClient timeout test, Error leakage, Confidence bounds enforcement, Model version enforcement, Prometheus metrics, Error rate, Latency histograms)

## Infrastructure Audit

| Component | Status | Finding |
|-----------|--------|---------|
| Eval tooling (JUnit 5 + Mockito) | Installed | 6 test files, 26 tests total, all passing. Tests exercise service/controller logic with mocks. |
| Reference dataset | Missing | No .jsonl/.csv reference dataset for ML prediction validation. Tests use ad-hoc mock data. |
| CI/CD integration | Missing | No GitHub Actions workflows. Tests are manual-only. |
| Online guardrails | Partial | `@PreAuthorize` + `@RateLimit` on 2 of 3 AI controllers. EmissionController `/predict` lacks both. No circuit breaker. Confidence bounds and modelVersion not enforced at runtime. |
| Tracing | Missing | No ML observability tool. No distributed tracing for ML service calls. |

**Infrastructure Score:** 30/100

## Critical Gaps

### BLOCKER 1: EmissionController `/predict` Missing @PreAuthorize and @RateLimit

`EmissionController.predict()` (line 84) has no `@PreAuthorize` annotation. Any authenticated user including REVIEWER and THIRD_PARTY can invoke AI carbon emission prediction. Also missing `@RateLimit`, exposing the endpoint to abuse.

**Fix:** Add `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` and `@RateLimit(key = "emission_predict", limit = 10, period = 60)` to the `/predict` method.

### BLOCKER 2: No Circuit Breaker for ML Service

MlServiceClient uses plain WebClient with no Resilience4j circuit breaker. If the Python ML service becomes slow or unresponsive, every request blocks for up to 30s. Repeated failures cascade. No test for timeout behavior.

### BLOCKER 3: No Production Monitoring

Zero Prometheus metrics for ML service calls. No error rate counter, no latency histogram, no degraded-response counter. Without monitoring, ML service degradation is invisible in production.

### BLOCKER 4: Incomplete Input Validation (2 of 3 controllers)

EnterpriseInferenceController and EmissionController accept horizonDays without bounds check. Negative and >365 values pass directly to the Python ML service, which may produce nonsensical predictions.

### WARNING 1: EnterpriseInferenceController Overly Permissive Roles

`@PreAuthorize("hasAnyRole('ENTERPRISE', 'REVIEWER', 'THIRD_PARTY', 'ADMIN')")` grants access to REVIEWER and THIRD_PARTY roles. AI-SPEC intended ENTERPRISE + ADMIN only for AI endpoints. This may be intentional (cross-role compliance visibility) but should be verified.

### WARNING 2: No Evaluation Dataset

No reference dataset for validating ML prediction quality. Tests verify wiring only, not prediction accuracy. Model drift and regression are undetectable.

## Remediation Plan

### Must fix before production (BLOCKER):

1. **Add @PreAuthorize + @RateLimit to EmissionController `/predict`**
   - File: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EmissionController.java`
   - Add `@PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")` and `@RateLimit(key = "emission_predict", limit = 10, period = 60)`
   - Add imports for `PreAuthorize` and `RateLimit`
   - Add controller test asserting 403 for unauthorized roles

2. **Add Resilience4j circuit breaker to MlServiceClient**
   - Add `spring-cloud-starter-circuitbreaker-resilience4j` dependency
   - Wrap WebClient calls in CircuitBreaker decorator
   - Configure failure rate threshold (50%), slow call threshold (5s), wait duration (30s)
   - Add test simulating repeated failures triggering open circuit

3. **Add Micrometer metrics to ML service calls**
   - Inject MeterRegistry into MlServiceClient
   - Add Timer for ML call latency with histogram publish
   - Add Counter for errors, timeouts, degraded responses
   - Configure Prometheus scrape endpoint

4. **Complete horizonDays validation on all controllers**
   - Add `@Min(1) @Max(365)` + `@Validated` to EnterpriseInferenceController and EmissionController
   - Add parameterized tests for: 0, -1, 366, Integer.MAX_VALUE
   - Return 400 Bad Request with clear message

### Should fix soon (WARNING):

5. **Verify EnterpriseInferenceController role scope**
   - Confirm whether REVIEWER and THIRD_PARTY should access enterprise inference
   - If not, restrict to `hasAnyRole('ENTERPRISE', 'ADMIN')`

6. **Test ML service timeout behavior**
   - Add test using MockWebServer with delayed responses exceeding connect/read timeout
   - Verify WebClient raises TimeoutException and service returns degraded response

7. **Enforce guardrails at runtime**
   - Add validation: if ML response lacks confidence bounds or modelVersion, throw/return error
   - Add tests for each guardrail enforcement path

8. **Test error response data leakage**
   - Add tests triggering ML service errors (500, timeout, malformed response)
   - Assert error responses contain no ML service URLs, stack traces, or internal details

9. **Create ML prediction eval dataset**
   - Create `oaiss-chain-backend/src/test/resources/ml-eval/` directory
   - Build JSONL files with known inputs and expected prediction ranges
   - Use dataset in a new MlPredictionEvalTest.java

### Nice to have:

10. **Integration tests against real ML service**
    - Use Testcontainers to spin up Python FastAPI service
    - Run actual Prophet/XGBoost predictions and validate output format

11. **Add Python ML service unit tests**
    - Create `oaiss-chain-ml-service/tests/` directory
    - Test each router endpoint with sample data

12. **Add GitHub Actions CI pipeline**
    - Create `.github/workflows/backend-test.yml` running `mvn test`
    - Block PRs on test failure

## Files Audited

### Test files (6):
- `MarketPredictionServiceTest.java` (5 tests)
- `EnterpriseInferenceServiceTest.java` (5 tests)
- `CarbonPredictionServiceTest.java` (10 tests)
- `MarketPredictionControllerTest.java` (4 tests)
- `EnterpriseInferenceControllerTest.java` (2 tests)
- `EmissionControllerTest.java` (4 tests)

### Implementation files (7):
- `MarketPredictionService.java` — has @PreAuthorize + @RateLimit
- `EnterpriseInferenceService.java` — has @PreAuthorize (broad roles) + @RateLimit
- `CarbonPredictionService.java`
- `MlServiceClient.java` — no circuit breaker
- `MlServiceConfig.java` — timeout configured but not tested
- `MarketPredictionController.java` — has @PreAuthorize + @RateLimit + @Validated
- `EnterpriseInferenceController.java` — has @PreAuthorize (broad) + @RateLimit
- `EmissionController.java` — /predict MISSING @PreAuthorize + @RateLimit
