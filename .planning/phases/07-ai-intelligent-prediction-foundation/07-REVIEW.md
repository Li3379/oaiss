---
phase: 07
title: AI 智能预测基础 — Deep Code Review
depth: deep
reviewer: gsd-code-reviewer
date: 2026-05-16
status: findings
summary: "8 findings (2 Critical, 3 Warning, 3 Info). Key issues: MlServiceClient blocking calls without circuit breaker, snake_case/camelCase DTO mismatch between Java and Python, and missing input validation on prediction endpoints."
---

# Phase 07 — Deep Code Review

## Scope

| Layer | Files |
|-------|-------|
| Backend Service | `service/ml/MlServiceClient.java`, `service/ml/MarketPredictionService.java`, `service/ml/EnterpriseInferenceService.java`, `service/CarbonPredictionService.java` |
| Backend Controller | `controller/MarketPredictionController.java`, `controller/EnterpriseInferenceController.java`, `controller/EmissionController.java` |
| Backend Config | `config/MlServiceConfig.java` |
| Backend DTO | `dto/CarbonPredictionRequest.java`, `dto/CarbonPredictionResponse.java`, `dto/MarketForecastRequest.java`, `dto/MarketForecastResponse.java`, `dto/EnterpriseInferenceRequest.java`, `dto/EnterpriseInferenceResponse.java` |
| Python ML Service | `ml-service/app/main.py`, `routers/market_router.py`, `routers/enterprise_router.py`, `routers/emission_router.py`, `services/market_service.py`, `services/enterprise_service.py`, `services/emission_service.py`, `schemas/enterprise.py`, `schemas/emission.py` |
| Frontend | `api/marketPrediction.ts`, `views/enterprise/MarketPrediction.vue` |
| Tests | `service/ml/EnterpriseInferenceServiceTest.java`, `service/ml/MarketPredictionServiceTest.java`, `service/CarbonPredictionServiceTest.java` |

## Findings

### CR-01: MlServiceClient uses blocking `.block()` without circuit breaker — service-wide outage risk

**Severity**: Critical
**File**: `service/ml/MlServiceClient.java` (all typed methods)
**Category**: Reliability

All `MlServiceClient` methods call `.block(Duration.ofSeconds(30))` on the WebClient response. If the Python ML service becomes unresponsive or slow:

1. Every calling thread in the Spring Boot server blocks for up to 30 seconds
2. Tomcat's default thread pool (200 threads) can be exhausted by concurrent prediction requests
3. No circuit breaker means the backend never fails fast — it always waits the full timeout
4. This cascades to all other endpoints sharing the same thread pool, causing a full outage

**Impact**: A single slow ML service can take down the entire OAISS CHAIN backend.

**Fix**: Add Resilience4j circuit breaker + retry with fallback:
```java
@CircuitBreaker(name = "mlService", fallbackMethod = "predictionFallback")
@Retry(name = "mlService")
public CarbonPredictionResponse predictEmission(CarbonPredictionRequest request) {
    return webClient.post()
        .uri("/emission/predict")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(CarbonPredictionResponse.class)
        .block(Duration.ofSeconds(10));
}
```
Also reduce timeout from 30s to 10s for prediction endpoints.

---

### CR-02: Java DTOs use camelCase but Python ML service expects snake_case — deserialization failure

**Severity**: Critical
**File**: `dto/CarbonPredictionRequest.java`, `dto/MarketForecastRequest.java`, `dto/EnterpriseInferenceRequest.java`
**Category**: Correctness

The Python ML service uses Pydantic v2 schemas with `snake_case` field names (e.g., `enterprise_id`, `prediction_months`, `horizon_days`). The Java DTOs use `camelCase` (e.g., `enterpriseId`, `predictionMonths`, `horizonDays`) without `@JsonProperty` annotations.

When `MlServiceClient` serializes Java DTOs via Jackson and sends to the Python service, the JSON payload contains camelCase keys. Pydantic v2 by default does NOT alias camelCase to snake_case, so the Python service receives `null` for all fields — predictions run with default/missing values and return garbage results.

**Evidence**: No `@JsonProperty` or `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` found on any DTO class.

**Impact**: All ML prediction endpoints return incorrect or default-valued results silently — no error is thrown.

**Fix**: Add class-level `@JsonNaming` to each DTO:
```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CarbonPredictionRequest { ... }
```
Or configure the `ObjectMapper` in `MlServiceConfig` with `setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)`.

---

### WR-01: EmissionController `/predict` endpoint has no `@PreAuthorize` — horizontal privilege escalation

**Severity**: Warning
**File**: `controller/EmissionController.java`
**Category**: Security

The `/emission/predict` endpoint lacks any authorization annotation. Any authenticated user (ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN) can call it. If prediction should be ENTERPRISE-only (it predicts for a specific enterprise), then REVIEWER and THIRD_PARTY users can invoke predictions for arbitrary enterprises.

**Impact**: Horizontal privilege escalation — non-enterprise users can trigger ML predictions.

**Fix**: Add `@PreAuthorize("hasRole('ENTERPRISE')")` or `@PreAuthorize("hasRole('ADMIN')")` to the predict endpoint.

---

### WR-02: Prediction request DTOs lack `@Valid` and field-level validation

**Severity**: Warning
**File**: `controller/MarketPredictionController.java`, `controller/EnterpriseInferenceController.java`, `controller/EmissionController.java`
**Category**: Security / Robustness

None of the controller methods use `@Valid` or `@Validated` on request body parameters. The DTO fields have no Bean Validation annotations (`@NotNull`, `@Min`, `@Max`, `@Positive`).

This means:
- `enterpriseId` can be null or negative
- `predictionMonths` / `horizonDays` can be 0, negative, or extremely large (e.g., 999999)
- The ML service receives invalid input and may crash or return meaningless results

**Impact**: Invalid inputs propagate to the Python ML service, causing crashes or garbage predictions.

**Fix**: Add validation annotations to DTOs and `@Valid` in controllers:
```java
public class CarbonPredictionRequest {
    @NotNull @Positive
    private Long enterpriseId;
    @NotNull @Min(1) @Max(60)
    private Integer predictionMonths;
}
```

---

### WR-03: Python ML service has no authentication — internal API is wide open

**Severity**: Warning
**File**: `oaiss-chain-ml-service/app/main.py`
**Category**: Security

The Python FastAPI service has no authentication middleware. If the ML service port (8000) is exposed (e.g., in Docker with port mapping), anyone can call prediction endpoints directly, bypassing the Java backend's auth layer.

**Impact**: Direct ML service access bypasses RBAC, rate limiting, and audit logging.

**Fix**: Add a shared-secret API key check:
```python
@app.middleware("http")
async def verify_internal_token(request: Request, call_next):
    token = request.headers.get("X-Internal-Token")
    if token != os.environ.get("ML_SERVICE_TOKEN"):
        return JSONResponse(status_code=401, content={"detail": "Unauthorized"})
    return await call_next(request)
```

---

### IN-01: MlServiceConfig creates WebClient without connection pool limits

**Severity**: Info
**File**: `config/MlServiceConfig.java`
**Category**: Performance

The `MlServiceConfig` creates a `WebClient` with custom timeouts but no `ConnectionProvider` configuration. Default Reactor Netty uses unlimited connections, which can exhaust file descriptors under load.

**Fix**: Configure a bounded connection pool:
```java
ConnectionProvider provider = ConnectionProvider.builder("ml-service")
    .maxConnections(50)
    .pendingAcquireTimeout(Duration.ofSeconds(10))
    .build();
HttpClient httpClient = HttpClient.create(provider)
    .responseTimeout(Duration.ofSeconds(15));
```

---

### IN-02: Test coverage uses mocks only — no integration test for ML service contract

**Severity**: Info
**File**: `service/ml/EnterpriseInferenceServiceTest.java`, `service/ml/MarketPredictionServiceTest.java`
**Category**: Testing

All three test files mock `MlServiceClient` and never test the actual HTTP contract with the Python service. The snake_case/camelCase mismatch (CR-02) would have been caught by a single integration test hitting the real Python service.

**Fix**: Add at least one `@SpringBootTest` integration test that starts the Python ML service (or uses WireMock with snake_case responses) and verifies end-to-end prediction flow.

---

### IN-03: Python ML service models are loaded at import time — cold start latency

**Severity**: Info
**File**: `oaiss-chain-ml-service/app/services/emission_service.py`, `services/market_service.py`
**Category**: Performance

Prophet/XGBoost models are loaded into memory at module import time. If the model files are large or the service scales to multiple workers, this causes:
1. Slow container startup (10-30s for Prophet)
2. High memory usage per worker
3. No lazy loading for infrequently-used models

**Fix**: Consider lazy-loading models on first request with a module-level cache, or pre-warm during health checks.

---

## Summary

| ID | Severity | Category | Title |
|----|----------|----------|-------|
| CR-01 | Critical | Reliability | MlServiceClient blocking calls without circuit breaker |
| CR-02 | Critical | Correctness | Java camelCase / Python snake_case DTO mismatch |
| WR-01 | Warning | Security | EmissionController /predict missing @PreAuthorize |
| WR-02 | Warning | Security | No input validation on prediction request DTOs |
| WR-03 | Warning | Security | Python ML service has no authentication |
| IN-01 | Info | Performance | WebClient no connection pool limits |
| IN-02 | Info | Testing | No integration test for ML service contract |
| IN-03 | Info | Performance | Python models loaded at import time |

**Critical path**: CR-02 is the most impactful — it means all ML predictions are currently returning incorrect results. CR-01 is a reliability time-bomb that will cause full outage under ML service degradation.
