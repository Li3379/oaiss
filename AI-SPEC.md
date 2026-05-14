# AI-SPEC: OAISS CHAIN AI Prediction System

## System Overview

- **System Type**: Hybrid (RAG + Extraction + Classification + Time-Series Forecasting)
- **Model Provider**: Model-agnostic (Python ML microservice called from Java/Spring Boot)
- **Primary Framework**: Python ML Microservice (FastAPI + Prophet/XGBoost/scikit-learn)
- **Integration Pattern**: Spring Boot REST client calls Python ML service over HTTP

---

## 1. Use Cases

### 1.1 Market Prediction (市场智能预测)

Predict carbon trading market trends based on historical auction orders, transactions, and user behavior.

- **Input**: Time-series of auction prices, volumes, bid/ask spreads, transaction counts per period
- **Output**: Price forecast with confidence intervals (7-day, 30-day horizon), trend classification (up/stable/down)
- **Data Sources**: `AuctionOrder`, `Transaction`, `CarbonReport` entities

### 1.2 Enterprise Inference (企业境况智能推断)

Infer enterprise environmental compliance status based on carbon reports, transactions, credit scores, emission ratings.

- **Input**: Enterprise features -- report count, emission totals, credit score, emission rating, transaction volume, compliance flags
- **Output**: Compliance status classification (compliant / at-risk / non-compliant), anomaly score, risk factors
- **Data Sources**: `CarbonReport`, `CreditScore`, `EmissionRating`, `User`, `Transaction` entities

### 1.3 Carbon Emission Prediction Upgrade

Upgrade the current linear regression stub to a proper time-series forecasting model.

- **Input**: Historical emission data per enterprise (monthly totals, sector, report history)
- **Output**: Emission forecast with confidence intervals, trend detection, seasonality decomposition
- **Data Sources**: `CarbonReport`, `User` entities

---

## 2. Architecture Decision: Python ML Microservice vs Embedded Java ML

### Decision: Python ML Microservice via FastAPI

**Rationale:**

| Factor | Python Microservice | Embedded Java (DJL/Tribuo) |
|--------|-------------------|---------------------------|
| Time-series forecasting (Prophet, ARIMA) | Native support, mature libraries | No native Prophet; DJL requires exported ONNX models with preprocessing gap |
| XGBoost/LightGBM | First-class, battle-tested | DJL can load XGBoost via ONNX but loses feature importance; Tribuo has no XGBoost |
| scikit-learn classification | Industry standard | Tribuo has limited algorithm set; Weka is academic-focused |
| Model iteration speed | Fast (Jupyter, experiment tracking) | Slow (retrain in Java, redeploy) |
| Team skills | Standard data science stack | Requires Java ML expertise |
| Inference latency | ~50-200ms per call (HTTP overhead ~5-10ms on localhost) | ~10-50ms per call (no network hop) |
| Deployment complexity | Extra container in docker-compose | Single JAR |
| Model serving maturity | MLflow, BentoML, FastAPI patterns | Limited tooling |

**The Python ecosystem advantage for time-series (Prophet) and gradient boosting (XGBoost) is decisive.** DJL and Tribuo cannot run Prophet natively -- Prophet requires Stan's MCMC engine which is Python/R only. Exporting Prophet models to ONNX is not supported. For XGBoost classification, DJL can load ONNX exports but loses feature importance metadata. Tribuo lacks XGBoost entirely.

**When embedded Java ML WOULD be the right choice:** Simple logistic regression, random forest, or linear models where deployment simplicity outweighs model sophistication. For this project, the use cases demand Prophet and XGBoost, which are Python-only.

---

## 3. Framework Quick Reference

### 3.1 Python ML Service (FastAPI)

**Installation:**

```bash
pip install fastapi uvicorn prophet scikit-learn xgboost pandas numpy pydantic
```

**Key Imports:**

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from prophet import Prophet
from xgboost import XGBClassifier, XGBRegressor
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
```

**Entry Point Pattern (Market Prediction):**

```python
app = FastAPI(title="OAISS Chain ML Service", version="1.0.0")

class MarketForecastRequest(BaseModel):
    dates: list[str]          # ISO 8601 date strings
    prices: list[float]       # historical prices
    volumes: list[float]      # historical volumes
    horizon_days: int = Field(default=30, ge=1, le=365)

class MarketForecastResponse(BaseModel):
    forecast_dates: list[str]
    forecast_prices: list[float]
    lower_bound: list[float]
    upper_bound: list[float]
    trend: str                # "up" | "stable" | "down"

@app.post("/api/v1/predict/market", response_model=MarketForecastResponse)
async def predict_market(request: MarketForecastRequest):
    df = pd.DataFrame({"ds": request.dates, "y": request.prices})
    model = Prophet(interval_width=0.95)
    model.add_regressor("volume", pd.Series(request.volumes))
    model.fit(df)
    future = model.make_future_dataframe(periods=request.horizon_days)
    future["volume"] = request.volumes[-1]  # last known volume as fallback
    forecast = model.predict(future)
    # ... extract and return response
```

### 3.2 Abstractions Table

| Abstraction | Role | Java Counterpart |
|------------|------|-----------------|
| `FastAPI` app | HTTP server routing and validation | `@RestController` |
| `Pydantic` models | Request/response schema with validation | DTO classes with `@Valid` |
| `Prophet` model | Time-series forecasting with seasonality | No Java equivalent |
| `XGBClassifier` | Gradient boosted classification | No Java equivalent |
| `IsolationForest` | Unsupervised anomaly detection | No Java equivalent |
| `sklearn Pipeline` | Preprocessing + model composition | Custom service layer |

### 3.3 Spring Boot Client (Java Side)

**Maven Dependencies (add to oaiss-chain-backend/pom.xml):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Application Properties:**

```yaml
ml-service:
  base-url: http://ml-service:8000
  timeout-seconds: 30
  retry-max-attempts: 3
  retry-backoff-millis: 1000
```

**Client Pattern:**

```java
@Configuration
public class MlServiceClientConfig {

    @Bean
    public WebClient mlWebClient(
            @Value("${ml-service.base-url}") String baseUrl,
            @Value("${ml-service.timeout-seconds}") int timeout) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().responseTimeout(Duration.ofSeconds(timeout))))
                .build();
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class MlServiceClient {

    private final WebClient mlWebClient;

    public MarketForecastResponse predictMarket(MarketForecastRequest request) {
        return mlWebClient.post()
                .uri("/api/v1/predict/market")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MarketForecastResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(ex -> ex instanceof WebClientException))
                .block();
    }
}
```

### 3.4 Pitfalls

1. **Prophet requires minimum 2 periods of data for seasonality detection.** With less than 2 full seasonal cycles (e.g., 2 years for yearly seasonality), Prophet falls back to a simple trend model. Supply at least 2 years of historical data or set `yearly_seasonality=False` and `weekly_seasonality=False` when data is sparse.

2. **XGBoost feature importance is lost when exported to ONNX.** If you need SHAP values or feature importance for the enterprise inference explainability, the model must run in Python. The ONNX export path strips this metadata. This is the primary reason to avoid embedded Java inference for classification tasks.

3. **Prophet is not thread-safe for concurrent fitting.** Each request that requires model fitting must create a new `Prophet()` instance. For pre-trained models, serialize with `model.to_json()` / `Prophet.from_json()` but note that deserialized models are read-only (predict only, no refit). Run model training in a background worker or scheduled task.

4. **IsolationForest contamination parameter is sensitive.** The `contamination` parameter (expected fraction of anomalies) directly controls the anomaly threshold. Default is 0.1 (10%). For carbon compliance, the actual non-compliance rate is likely 2-5%. Setting contamination too high produces false positives; too low misses non-compliant enterprises. Tune on labeled validation data.

5. **FastAPI + Prophet model loading at startup blocks the event loop.** Loading large Prophet models during `@app.on_startup()` blocks the async event loop. Use `app.state.model` loaded in a background thread or load lazily on first request with a threading lock.

6. **WebClient.block() in a reactive context causes deadlocks.** The Java `MlServiceClient` uses `block()` for simplicity. If called from a reactive handler (WebFlux controller), this will deadlock. Either use the reactive chain (`.subscribeOn(Schedulers.boundedElastic())`) or keep the existing Spring MVC controllers and use `block()` safely.

### 3.5 Folder Structure

```
oaiss-chain-ml/                         # New Python ML service (separate module)
├── Dockerfile
├── requirements.txt
├── app/
│   ├── __init__.py
│   ├── main.py                         # FastAPI app entry point
│   ├── config.py                       # Settings from env vars
│   ├── models/                         # Pre-trained model artifacts
│   │   ├── market_prophet.json
│   │   ├── enterprise_xgb.json
│   │   └── emission_prophet.json
│   ├── schemas/                        # Pydantic request/response models
│   │   ├── market.py
│   │   ├── enterprise.py
│   │   └── emission.py
│   ├── services/                       # Business logic per use case
│   │   ├── market_prediction.py
│   │   ├── enterprise_inference.py
│   │   └── emission_prediction.py
│   └── training/                       # Model training scripts (offline)
│       ├── train_market.py
│       ├── train_enterprise.py
│       └── train_emission.py
│
oaiss-chain-backend/src/main/java/com/oaiss/chain/
├── client/                             # New package: ML service HTTP client
│   ├── MlServiceClient.java
│   └── MlServiceClientConfig.java
├── dto/                                # Existing, add ML request/response DTOs
│   ├── MarketForecastRequest.java
│   ├── MarketForecastResponse.java
│   ├── EnterpriseInferenceRequest.java
│   ├── EnterpriseInferenceResponse.java
│   ├── EmissionForecastRequest.java
│   └── EmissionForecastResponse.java
├── service/                            # Existing, replace stub
│   └── CarbonPredictionService.java    # Upgrade from linear regression stub
```

### 3.6 Sources

- DJL (Deep Java Library): https://djl.ai
- Prophet: https://facebook.github.io/prophet/
- XGBoost: https://xgboost.readthedocs.io/
- scikit-learn IsolationForest: https://scikit-learn.org/stable/modules/generated/sklearn.ensemble.IsolationForest.html
- FastAPI: https://fastapi.tiangolo.com/
- Spring Boot WebClient: https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.reactive.webclient

---

## 4. Implementation Guidance

### 4.1 Model Selection and Parameters

| Use Case | Algorithm | Model | Key Parameters |
|----------|-----------|-------|----------------|
| Market Prediction | Prophet | `Prophet` | `interval_width=0.95`, `changepoint_prior_scale=0.05`, add `volume` regressor |
| Enterprise Inference | XGBoost | `XGBClassifier` | `n_estimators=200`, `max_depth=5`, `learning_rate=0.1`, `objective=binary:logistic` |
| Enterprise Anomaly | IsolationForest | `IsolationForest` | `contamination=0.03`, `n_estimators=100`, `max_samples='auto'` |
| Emission Prediction | Prophet | `Prophet` | `interval_width=0.90`, `seasonality_mode='multiplicative'`, add `sector` regressor |

### 4.2 Core Pattern: Market Prediction Service (Python)

```python
# oaiss-chain-ml/app/services/market_prediction.py

from prophet import Prophet
from datetime import datetime
import pandas as pd
import numpy as np

class MarketPredictionService:
    """Market trend forecasting using Prophet with confidence intervals."""

    def forecast(self, dates: list[str], prices: list[float],
                 volumes: list[float], horizon_days: int = 30) -> dict:
        # 1. Build Prophet-compatible DataFrame
        df = pd.DataFrame({
            "ds": pd.to_datetime(dates),
            "y": prices,
            "volume": volumes
        })

        # 2. Configure and fit model
        model = Prophet(
            interval_width=0.95,                   # 95% confidence interval
            changepoint_prior_scale=0.05,          # moderate trend flexibility
            yearly_seasonality=True,               # carbon markets have yearly cycles
            weekly_seasonality=False,              # no weekly pattern in carbon trading
            daily_seasonality=False                # daily noise is not useful
        )
        model.add_regressor("volume")              # volume as exogenous feature
        model.fit(df)

        # 3. Generate future dataframe
        future = model.make_future_dataframe(periods=horizon_days)
        # Fill volume for future dates with last known value (conservative)
        future["volume"] = df["volume"].iloc[-1]
        # Fill volume for historical dates with actuals
        future.loc[future.index <= len(df) - 1, "volume"] = df["volume"].values

        # 4. Predict
        forecast = model.predict(future)

        # 5. Extract forecast horizon only
        forecast_tail = forecast.tail(horizon_days)

        # 6. Determine trend: compare last forecast vs last actual
        last_actual = prices[-1]
        final_forecast = forecast_tail["yhat"].iloc[-1]
        change_pct = (final_forecast - last_actual) / last_actual
        if change_pct > 0.03:
            trend = "up"
        elif change_pct < -0.03:
            trend = "down"
        else:
            trend = "stable"

        return {
            "forecast_dates": forecast_tail["ds"].dt.strftime("%Y-%m-%d").tolist(),
            "forecast_prices": forecast_tail["yhat"].round(2).tolist(),
            "lower_bound": forecast_tail["yhat_lower"].round(2).tolist(),
            "upper_bound": forecast_tail["yhat_upper"].round(2).tolist(),
            "trend": trend
        }
```

### 4.3 Core Pattern: Enterprise Inference Service (Python)

```python
# oaiss-chain-ml/app/services/enterprise_inference.py

import xgboost as xgb
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import numpy as np

class EnterpriseInferenceService:
    """Classify enterprise compliance and detect anomalies."""

    # Feature order must match training
    FEATURE_NAMES = [
        "report_count", "total_emissions", "credit_score",
        "emission_rating", "transaction_volume", "compliance_flags",
        "avg_emission_per_report", "days_since_last_report"
    ]

    def __init__(self):
        self.classifier = None       # XGBClassifier loaded from disk
        self.anomaly_detector = None # IsolationForest loaded from disk
        self.scaler = None           # StandardScaler loaded from disk

    def infer(self, features: dict) -> dict:
        # 1. Build feature vector in correct order
        X = np.array([[features[f] for f in self.FEATURE_NAMES]])

        # 2. Scale features (using pre-fitted scaler)
        X_scaled = self.scaler.transform(X)

        # 3. Classification: compliant / at-risk / non-compliant
        proba = self.classifier.predict_proba(X_scaled)[0]
        classes = self.classifier.classes_.tolist()
        status = classes[np.argmax(proba)]
        confidence = float(np.max(proba))

        # 4. Anomaly detection
        anomaly_score = self.anomaly_detector.decision_function(X_scaled)[0]
        is_anomaly = self.anomaly_detector.predict(X_scaled)[0] == -1

        return {
            "compliance_status": status,
            "confidence": round(confidence, 4),
            "anomaly_score": round(float(anomaly_score), 4),
            "is_anomaly": bool(is_anomaly),
            "risk_factors": self._get_risk_factors(features, proba, classes)
        }

    def _get_risk_factors(self, features: dict, proba, classes) -> list[str]:
        """Identify which features contribute most to risk."""
        risk_factors = []
        if features["credit_score"] < 60:
            risk_factors.append("low_credit_score")
        if features["compliance_flags"] > 2:
            risk_factors.append("high_compliance_flags")
        if features["days_since_last_report"] > 90:
            risk_factors.append("stale_report")
        if features["avg_emission_per_report"] > features["emission_rating"] * 1.2:
            risk_factors.append("emission_exceeds_rating")
        return risk_factors
```

### 4.4 Core Pattern: Spring Boot Client Integration (Java)

```java
// Replace the existing CarbonPredictionService stub

@Service
@RequiredArgsConstructor
@Slf4j
public class CarbonPredictionService {

    private final MlServiceClient mlServiceClient;
    private final CarbonReportRepository carbonReportRepository;
    private final UserRepository userRepository;

    /**
     * Emission prediction -- replaces the linear regression stub.
     * Fetches historical data from MySQL, calls Python ML service.
     */
    public EmissionForecastResponse predictEmission(Long enterpriseId, int horizonDays) {
        // 1. Fetch historical data from MySQL
        List<CarbonReport> reports = carbonReportRepository
                .findByUserIdOrderByReportDateAsc(enterpriseId);

        // 2. Validate minimum data points (Prophet needs >= 2 periods)
        if (reports.size() < 30) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_DATA,
                    "At least 30 data points required for prediction");
        }

        // 3. Build request DTO
        EmissionForecastRequest request = EmissionForecastRequest.builder()
                .dates(reports.stream().map(r -> r.getReportDate().toString()).toList())
                .emissions(reports.stream().map(CarbonReport::getEmissionAmount).toList())
                .sector(reports.get(0).getUser().getSector())
                .horizonDays(horizonDays)
                .build();

        // 4. Call Python ML service
        return mlServiceClient.predictEmission(request);
    }

    /**
     * Market prediction -- aggregate auction data and forecast.
     */
    public MarketForecastResponse predictMarket(int horizonDays) {
        // Aggregate from AuctionOrder and Transaction repositories
        // ... build MarketForecastRequest from aggregated time-series
        return mlServiceClient.predictMarket(request);
    }

    /**
     * Enterprise inference -- classify compliance status.
     */
    public EnterpriseInferenceResponse inferEnterprise(Long enterpriseId) {
        // Build feature vector from multiple entities
        // ... aggregate from CarbonReport, CreditScore, EmissionRating, Transaction
        return mlServiceClient.inferEnterprise(request);
    }
}
```

### 4.5 State Management

**Model State (Python side):**
- Pre-trained models stored as JSON/pickle artifacts in `app/models/`
- Loaded at FastAPI startup via `lifespan` context manager (async-safe)
- Models are immutable at inference time -- no state mutation during prediction
- Model retraining is an offline process via `training/` scripts, not triggered by inference requests

**Application State (Java side):**
- All state remains in MySQL/Redis as currently implemented
- The ML service is stateless from the Java perspective -- requests contain all needed data
- Cache inference results in Redis with TTL (1 hour for market predictions, 24 hours for enterprise inference) to avoid redundant ML calls
- Cache key pattern: `ml:forecast:{type}:{entityId}:{date}`

### 4.6 Context Window Strategy

Not applicable for this architecture. The Python ML service processes structured tabular data (DataFrames), not natural language. Context window limits of LLMs are not a constraint here.

The relevant data size constraint is Prophet's handling of long time series:
- Prophet performance degrades on series with > 10,000 data points (daily data spanning 27+ years)
- For carbon trading data at daily granularity, this is not a concern (typical history: 1-3 years = 365-1095 points)
- If sub-daily data is needed, Prophet supports it but requires `freq` parameter specification

### 4.7 Docker Compose Integration

Add to `docker-compose.yml`:

```yaml
  ml-service:
    build:
      context: ./oaiss-chain-ml
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      - MODEL_DIR=/app/models
      - LOG_LEVEL=INFO
    volumes:
      - ./oaiss-chain-ml/app/models:/app/models:ro
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - oaiss-network
```

---

## 4b. AI Systems Best Practices

### 4b.1 Structured Outputs with Pydantic

All ML service endpoints use Pydantic models for request/response validation. The FastAPI framework enforces schema validation automatically -- invalid requests return 422 with detailed field-level errors.

**Example Pydantic Model for Enterprise Inference:**

```python
# oaiss-chain-ml/app/schemas/enterprise.py

from pydantic import BaseModel, Field
from enum import Enum
from typing import Optional

class ComplianceStatus(str, Enum):
    COMPLIANT = "compliant"
    AT_RISK = "at_risk"
    NON_COMPLIANT = "non_compliant"

class EnterpriseInferenceRequest(BaseModel):
    enterprise_id: int = Field(..., gt=0)
    report_count: int = Field(..., ge=0)
    total_emissions: float = Field(..., ge=0.0)
    credit_score: float = Field(..., ge=0.0, le=100.0)
    emission_rating: float = Field(..., ge=0.0)
    transaction_volume: float = Field(..., ge=0.0)
    compliance_flags: int = Field(..., ge=0)
    avg_emission_per_report: float = Field(..., ge=0.0)
    days_since_last_report: int = Field(..., ge=0)

class EnterpriseInferenceResponse(BaseModel):
    enterprise_id: int
    compliance_status: ComplianceStatus
    confidence: float = Field(..., ge=0.0, le=1.0)
    anomaly_score: float
    is_anomaly: bool
    risk_factors: list[str] = Field(default_factory=list)
    model_version: str = "1.0.0"
```

**Java Side Validation:**

```java
// oaiss-chain-backend/.../dto/EnterpriseInferenceResponse.java
@Data
@Builder
public class EnterpriseInferenceResponse {
    @NotNull
    private Long enterpriseId;

    @NotBlank
    private String complianceStatus; // "compliant" | "at_risk" | "non_compliant"

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double confidence;

    private Double anomalyScore;
    private Boolean isAnomaly;
    private List<String> riskFactors;
    private String modelVersion;
}
```

**Retry Logic:**

- The Java `MlServiceClient` retries on `WebClientException` (network/timeout) up to 3 times with exponential backoff (1s, 2s, 4s)
- On 422 (validation error): do NOT retry -- log the request body at WARN level and throw `BusinessException` to the caller
- On 500 (ML service error): retry once; if still failing, return a fallback response with `complianceStatus=UNKNOWN` and `confidence=0.0`, log at ERROR level with full stack trace
- On 503 (ML service unavailable): retry with backoff; after 3 failures, use circuit breaker pattern to stop hammering the service

### 4b.2 Async-First Design

**Python (FastAPI) Side:**

FastAPI is async by default. Prophet's `fit()` method is CPU-bound and blocking. Use `asyncio.to_thread()` to offload blocking calls:

```python
import asyncio
from functools import partial

@app.post("/api/v1/predict/market")
async def predict_market(request: MarketForecastRequest):
    # Offload blocking Prophet fit/predict to thread pool
    result = await asyncio.to_thread(_sync_predict_market, request)
    return result

def _sync_predict_market(request: MarketForecastRequest) -> dict:
    # This runs in a thread -- Prophet is safe here
    service = MarketPredictionService()
    return service.forecast(
        dates=request.dates,
        prices=request.prices,
        volumes=request.volumes,
        horizon_days=request.horizon_days
    )
```

**The one common mistake:** Running `Prophet.fit()` directly in an async endpoint handler without `asyncio.to_thread()`. This blocks the FastAPI event loop, preventing all other requests from being processed until the model finishes fitting. Always use `asyncio.to_thread()` or `run_in_executor()` for CPU-bound ML work.

**Java (Spring Boot) Side:**

The existing backend uses Spring MVC (synchronous), not WebFlux. The `WebClient.block()` call in `MlServiceClient` is safe in this context. If the backend migrates to WebFlux controllers, switch from `block()` to the reactive chain:

```java
// Reactive version (only if using WebFlux controllers)
public Mono<MarketForecastResponse> predictMarketReactive(MarketForecastRequest request) {
    return mlWebClient.post()
            .uri("/api/v1/predict/market")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MarketForecastResponse.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
}
```

**Stream vs. Await:** For ML inference, always `await` the full response. Streaming is not useful here because the model must compute the complete forecast before any results are available. Streaming is relevant only for LLM token generation, which this system does not use.

### 4b.3 Prompt Engineering Discipline

This system does not use LLM prompting (no text generation). However, the principle of explicit input specification applies:

**System vs. User Prompt Separation Analogy:**

- "System prompt" = model configuration and hyperparameters (set at model training/loading time)
- "User prompt" = per-request data (the feature vector or time-series data sent in each request)

**Rules for ML Request Design:**

1. **All features must be explicitly named and typed.** No "pass a map and hope the model knows what to do." Use Pydantic models with `Field()` constraints.
2. **Never send raw strings to numerical models.** All categorical features must be encoded before reaching the model (sector -> one-hot, status -> ordinal). The Java side sends the raw value; the Python side applies the encoder.
3. **Set `horizon_days` explicitly with bounds.** `Field(default=30, ge=1, le=365)` -- never allow unbounded prediction horizons. Beyond 90 days, confidence intervals widen to the point of uselessness.
4. **Set `max_tokens` equivalent: enforce output bounds.** Pydantic validators enforce min/max on all numerical outputs. Never allow unbounded arrays or infinite response sizes.

### 4b.4 Context Window Management

**RAG Pattern (Data Retrieval for ML):**

For the enterprise inference use case, feature engineering aggregates data from multiple tables. The "context window" analog is the query window:

- **Time window for features:** Use the last 12 months of data for enterprise features. Older data is less predictive and increases query latency.
- **Truncation strategy:** When an enterprise has > 1000 carbon reports, aggregate to monthly totals rather than sending every individual report to the feature engineering layer.

**Multi-Source Aggregation:**

```java
// Enterprise feature aggregation with time window
LocalDateTime cutoff = LocalDateTime.now().minusMonths(12);

List<CarbonReport> reports = carbonReportRepository
    .findByUserIdAndReportDateAfter(enterpriseId, cutoff);
```

**If data exceeds practical limits:**

1. Aggregate to coarser granularity (daily -> weekly -> monthly)
2. Use statistical summaries (mean, std, trend) instead of raw data points
3. For Prophet, cap at 5 years of daily data; beyond that, sample to weekly

**Cache Invalidation:**

- Market prediction cache: invalidate when new auction closes (event-driven)
- Enterprise inference cache: invalidate when new carbon report is submitted
- Emission forecast cache: invalidate daily (scheduled) or on new report submission

### 4b.5 Cost and Latency Budget

**Per-Call Cost Estimate (Monthly, 1000 enterprises, 50 daily market queries):**

| Operation | Calls/Month | Latency (p95) | Compute Cost |
|-----------|------------|---------------|-------------|
| Market prediction | ~1,500 | 500ms | Prophet fit is CPU-intensive; pre-train weekly |
| Enterprise inference | ~4,000 | 50ms | XGBoost inference is fast; pre-trained model |
| Emission prediction | ~3,000 | 400ms | Prophet fit per enterprise; pre-train monthly |
| **Total** | ~8,500 | - | ~0.5 vCPU + 1GB RAM sustained |

**Cost Optimization Strategies:**

1. **Exact-match caching in Redis.** Same enterprise features -> same result. TTL 1-24 hours depending on use case. Expected cache hit rate: 60-80% for enterprise inference (features don't change rapidly).

2. **Semantic caching not applicable.** Unlike LLM outputs, ML inference on structured data is deterministic. Exact-match caching is sufficient.

3. **Cheaper models for sub-tasks.** Use IsolationForest (fast, lightweight) for anomaly pre-screening before running the full XGBoost classifier. If IsolationForest says "not anomalous" with high confidence, skip the classifier call and return "compliant" directly. This saves ~40% of XGBoost inference calls.

4. **Pre-train models offline.** Do NOT fit Prophet models on every inference request when possible. Pre-train weekly (market) or monthly (emission) and load the serialized model for prediction. Fitting Prophet takes 1-5 seconds; predicting from a pre-trained model takes 10-50ms.

5. **Batch inference.** For the "infer all enterprises" admin dashboard, use batch prediction instead of N individual calls. XGBoost natively supports batch inference (`predict_proba()` on a 2D array) with near-constant latency regardless of batch size.

6. **Model serving resource sizing.** The Python ML service needs:
   - CPU: 2 cores (Prophet fitting is single-threaded but can run in parallel across requests)
   - RAM: 2GB (Prophet models ~50MB each, XGBoost ~10MB, overhead ~500MB)
   - No GPU needed -- Prophet and XGBoost are CPU-only algorithms
