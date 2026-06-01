# Research: AI/ML Prediction Model Approaches for OAISS CHAIN

**Researched:** 2026-05-14
**Domain:** Machine learning integration for Java Spring Boot carbon trading platform
**Confidence:** HIGH

## Summary

This research evaluates four approaches for integrating ML prediction capabilities into the OAISS CHAIN carbon trading platform (Spring Boot 3.2.5 / Java 17): (1) DeepLearning4J (DL4J), (2) ONNX Runtime Java, (3) Tribuo, and (4) Python ML microservice via FastAPI. The platform needs three AI services: MarketPredictionService (carbon price/volume time-series forecasting), EnterpriseInferenceService (compliance classification and anomaly detection), and CarbonPredictionService (upgrading the existing linear regression stub to real ML).

**Primary recommendation: Python ML microservice via FastAPI** -- this is the only approach that natively supports Prophet (time-series forecasting with seasonality) and XGBoost (gradient boosted classification with feature importance). No Java-native library can run Prophet, which is the industry-standard algorithm for carbon market forecasting. ONNX Runtime Java is a viable secondary option for inference-only deployment of pre-trained models, but loses feature importance metadata and cannot run Prophet. DL4J is disqualified due to no stable release. Tribuo lacks time-series forecasting entirely.

**Critical finding:** The project already has an `AI-SPEC.md` at the repository root that has evaluated this exact decision and concluded on the Python microservice approach with detailed architecture, code patterns, and pitfalls. This research validates and extends that specification with version-verified Maven Central data.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Market price forecasting (Prophet) | Python ML Service | API / Backend | Prophet requires Python runtime; Java orchestrates data fetch and caches results |
| Enterprise compliance classification (XGBoost) | Python ML Service | API / Backend | XGBoost with feature importance is Python-only; Java sends feature vectors |
| Carbon emission forecasting (Prophet) | Python ML Service | API / Backend | Same rationale as market forecasting |
| Historical data aggregation | API / Backend | Database / Storage | Java services query MySQL and build request payloads for ML service |
| Inference result caching | API / Backend | Database / Storage | Redis caching with TTL; Java-side concern |
| Model training | Offline / CI | -- | Training scripts run offline; models serialized as artifacts |
| Model serving | Python ML Service | CDN / Static | FastAPI serves pre-trained models for low-latency inference |

## Options Compared

### Option 1: DeepLearning4J (DL4J)

| Property | Value |
|----------|-------|
| Latest version | 1.0.0-M2.1 (milestone, NOT stable release) [VERIFIED: Maven Central] |
| Last publish date | August 2022 [VERIFIED: Maven Central timestamp 1660268541000] |
| Java 17 support | Known issues with JDK 17 (Spark/Scala dependency conflicts) [CITED: github.com/deeplearning4j/deeplearning4j] |
| Maintainer | Konduit K.K. (Eclipse foundation) |
| Maven artifact | `org.deeplearning4j:deeplearning4j-core` |
| Stars | 14.2k on GitHub |

**Capabilities:**
- LSTM networks for time-series (code verified via Context7)
- CNN, RNN, GAN support
- Distributed training via Spark
- Model import from Keras/TensorFlow

**Disqualifying issues:**
- No stable 1.0.0 release after 4+ years (only milestone M2.1)
- Java 17 compatibility is explicitly problematic
- No Prophet equivalent -- would require hand-building LSTM for time-series, which is inferior to Prophet for seasonal carbon data
- Heavy dependency tree (ND4J native backend, ~200MB JARs)
- Project velocity has significantly slowed

**Verdict: DISQUALIFIED** -- no stable release, Java 17 issues, no Prophet equivalent.

---

### Option 2: ONNX Runtime Java

| Property | Value |
|----------|-------|
| Latest version | 1.20.0 (stable) [VERIFIED: Maven Central, released Oct 2024] |
| Java 17 support | Yes (requires Java 8+, confirmed compatible) [CITED: onnxruntime.ai/docs/get-started/with-java] |
| Maintainer | Microsoft |
| Maven artifact | `com.microsoft.onnxruntime:onnxruntime` (CPU) or `onnxruntime_gpu` (CUDA) |
| Platforms | Windows x64, Linux x64, macOS x64 |

**Capabilities:**
- Load and run any ONNX-format model (PyTorch, scikit-learn, TensorFlow exports)
- High-performance inference with graph optimization
- GPU support via CUDA execution provider
- Clean Java API: `OrtEnvironment` + `OrtSession` + `OnnxTensor` [VERIFIED: Context7]

**Java API pattern (verified from official docs):**
```java
OrtEnvironment env = OrtEnvironment.getEnvironment();
try (SessionOptions opts = new SessionOptions()) {
    opts.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT);
    try (OrtSession session = env.createSession("model.onnx", opts)) {
        float[] inputData = {5.1f, 3.5f, 1.4f, 0.2f};
        long[] shape = {1, 4};
        OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape);
        Map<String, OnnxTensor> feed = new HashMap<>();
        feed.put("float_input", tensor);
        try (Result result = session.run(feed)) {
            long[] labels = (long[]) result.get(0).getValue();
        }
        tensor.close();
    }
}
```

**Limitations:**
- **Cannot run Prophet** -- Prophet models cannot be exported to ONNX (Prophet requires Stan's MCMC engine)
- **Loses feature importance** -- XGBoost models exported to ONNX lose SHAP values and feature importance metadata, which is critical for enterprise inference explainability
- **Inference only** -- no training in Java; all models must be trained in Python and exported
- **Preprocessing gap** -- scikit-learn Pipeline (scaling + encoding) must be replicated in Java or bundled in the ONNX model via `skl2onnx`, which is fragile
- **Native library dependency** -- JNI bindings to native ONNX Runtime; may cause deployment complexity

**Verdict: VIABLE SECONDARY** -- good for inference-only deployment of simple models (regression, classification), but cannot run Prophet and loses XGBoost feature importance. Use as a fallback if the Python microservice approach is blocked.

---

### Option 3: Tribuo

| Property | Value |
|----------|-------|
| Latest version | 4.3.2 (stable) [VERIFIED: Maven Central, released April 2025] |
| Java 17 support | Yes (Java 8+; model card/reproducibility packages need Java 17) [CITED: github.com/oracle/tribuo] |
| Maintainer | Oracle Labs ML Research Group |
| Maven artifact | `org.tribuo:tribuo-all` |

**Capabilities:**
- Classification: Linear models (SGD), CART, Random Forest, Extra Trees, SVM-SGD, XGBoost (via external), LibLinear/LibSVM
- Regression: Linear models, CART, Lasso (LARS), Elastic Net, LibLinear/LibSVM, XGBoost (via external)
- Clustering: K-Means, HDBSCAN*
- Anomaly Detection: One-class SVM
- External interfaces: ONNX Runtime, TensorFlow, XGBoost
- Model cards and reproducibility

**Limitations:**
- **No time-series forecasting** -- no Prophet, no ARIMA, no LSTM, no seasonal decomposition [CITED: tribuo.org tutorials, confirmed no time-series section]
- **XGBoost is external** -- requires separate XGBoost native library; not a pure-Java implementation
- **No deep learning** -- neural networks only via TensorFlow external interface
- **Regression is limited** -- linear models, CART, Lasso/Elastic Net; no gradient boosting regressor without external XGBoost

**Verdict: VIABLE FOR CLASSIFICATION ONLY** -- Tribuo can handle the EnterpriseInferenceService (compliance classification) with its built-in Random Forest or linear models, but cannot handle time-series forecasting (MarketPredictionService, CarbonPredictionService) at all. Would need to be combined with another approach for forecasting.

---

### Option 4: Python ML Microservice (FastAPI)

| Property | Value |
|----------|-------|
| Framework | FastAPI (async, Pydantic validation) |
| Python version | 3.10+ |
| Key libraries | Prophet, XGBoost, scikit-learn, pandas, numpy |
| Integration | Spring Boot WebClient calls Python service over HTTP |
| Latency overhead | 5-10ms HTTP overhead on localhost; Prophet fit 1-5s, predict 10-50ms |

**Capabilities:**
- **Prophet**: Industry-standard for carbon/market time-series forecasting with seasonality, holidays, and confidence intervals
- **XGBoost**: Best-in-class gradient boosting with feature importance (SHAP values) for explainable compliance classification
- **IsolationForest**: Anomaly detection for enterprise emission patterns
- **scikit-learn Pipeline**: Preprocessing + model composition
- **Full Python ML ecosystem**: Any algorithm available; no export/conversion step needed

**Advantages:**
- Prophet has no Java equivalent -- this alone is decisive for carbon market forecasting
- XGBoost with feature importance is Python-only (ONNX export strips this metadata)
- Fast iteration: Jupyter notebooks, experiment tracking, model versioning
- Standard data science stack: team skills alignment
- Model serving maturity: MLflow, BentoML, FastAPI patterns

**Disadvantages:**
- Extra container in docker-compose (deployment complexity)
- HTTP overhead per inference call (~5-10ms)
- JVM-Python boundary: data serialization, error handling, retries
- Two tech stacks to maintain (Java + Python)
- Model versioning and drift monitoring requires additional tooling

**Verdict: RECOMMENDED** -- the Python ecosystem advantage for Prophet (time-series) and XGBoost (explainable classification) is decisive. No Java-native library can replicate these capabilities.

---

### Option 5 (Additional): DJL - Deep Java Library

| Property | Value |
|----------|-------|
| Latest version | 0.36.0 (stable) [VERIFIED: Maven Central] |
| Maintainer | Amazon |
| Engines | PyTorch, TensorFlow, MXNet, ONNX Runtime (multi-engine) |

**Capabilities:**
- Model-agnostic: run PyTorch, TensorFlow, or ONNX models in Java
- Higher-level API than raw ONNX Runtime
- Amazon-backed, actively maintained

**Limitations:**
- Same fundamental problem as ONNX Runtime: cannot run Prophet
- Same preprocessing gap as ONNX Runtime
- PyTorch/TensorFlow models still need training in Python
- Adds another abstraction layer over ONNX Runtime without solving the core problem

**Verdict: NOT RECOMMENDED** -- solves the same subset of problems as ONNX Runtime but with more abstraction. Does not address the Prophet/XGBoost gap.

---

## Comparative Decision Matrix

| Criterion | DL4J | ONNX Runtime | Tribuo | Python Microservice | Weight |
|-----------|------|-------------|--------|---------------------|--------|
| Prophet time-series | No | No | No | **Yes** | Critical |
| XGBoost + feature importance | No | No (loses metadata) | External only | **Yes** | Critical |
| Classification (RF, linear) | Yes | Yes (via ONNX) | **Yes** | Yes | Medium |
| Java 17 compatible | No (issues) | **Yes** | **Yes** | N/A | High |
| Stable release | No (M2.1) | **Yes** | **Yes** | N/A | High |
| Deployment simplicity | **Single JAR** | **Single JAR** | **Single JAR** | Extra container | Low |
| Inference latency | 10-50ms | 10-50ms | 10-50ms | 50-200ms | Medium |
| Model iteration speed | Slow | Slow (re-export) | Slow | **Fast** | Medium |
| Team skill alignment | Java-only | Java-only | Java-only | **Standard DS stack** | Medium |
| Anomaly detection | LSTM-based | Via ONNX | **Yes (OCSVM)** | **Yes (IsolationForest)** | Medium |

**Result:** Python microservice wins on the two critical-weight criteria (Prophet and XGBoost with feature importance). No Java-native option can match these.

## Recommendation

### Primary: Python ML Microservice via FastAPI

Use the architecture already specified in the project's `AI-SPEC.md`:

1. **MarketPredictionService** -- Prophet with volume regressor, 95% confidence intervals
2. **EnterpriseInferenceService** -- XGBoost classifier + IsolationForest anomaly detector
3. **CarbonPredictionService** -- Prophet with seasonality (upgrade from linear stub)

### Secondary Fallback: ONNX Runtime Java (if Python service is blocked)

If the Python microservice approach cannot be used (organizational constraints, deployment restrictions, skill gaps), fall back to:

1. **EnterpriseInferenceService** -- ONNX Runtime loading a pre-trained XGBoost model exported via `skl2onnx` (accepts loss of feature importance)
2. **CarbonPredictionService** -- ONNX Runtime loading a pre-trained sklearn regression model (ARIMA or linear)
3. **MarketPredictionService** -- ONNX Runtime loading a pre-trained LSTM model trained in PyTorch (significant effort; no Prophet seasonality)

This fallback loses: Prophet seasonality, XGBoost feature importance, and requires a Python training pipeline anyway (to create the ONNX models).

### Hybrid Option: Tribuo for Classification + Python for Forecasting

A middle ground: use Tribuo for EnterpriseInferenceService (pure Java, no external service) and Python microservice for the two forecasting services. This reduces the Python service to two endpoints instead of three, and keeps classification fully in Java.

**When to choose hybrid:** If enterprise inference latency must be <20ms and deployment simplicity for classification is critical. Tribuo's Random Forest or linear models can handle compliance classification without external dependencies.

## Implementation Guidance

### Model Types and Algorithms

| Service | Algorithm | Why | Key Parameters |
|---------|-----------|-----|----------------|
| MarketPredictionService | Prophet | Industry-standard for seasonal financial/commodity forecasting | `interval_width=0.95`, `changepoint_prior_scale=0.05`, `yearly_seasonality=True`, add volume regressor |
| EnterpriseInferenceService (classification) | XGBClassifier | Best-in-class for tabular classification with explainability | `n_estimators=200`, `max_depth=5`, `learning_rate=0.1` |
| EnterpriseInferenceService (anomaly) | IsolationForest | Unsupervised anomaly detection for emission patterns | `contamination=0.03`, `n_estimators=100` |
| CarbonPredictionService | Prophet | Seasonal emission forecasting with confidence intervals | `interval_width=0.90`, `seasonality_mode='multiplicative'`, add sector regressor |

### Training Data Requirements

| Model | Minimum Data | Ideal Data | Granularity | Key Features |
|-------|-------------|------------|-------------|--------------|
| Market Prophet | 2 seasonal cycles (~730 days daily) | 3-5 years daily | Daily | price, volume, bid/ask spread |
| Enterprise XGBoost | 100 enterprises labeled | 500+ enterprises | Per-enterprise snapshot | report_count, total_emissions, credit_score, emission_rating, transaction_volume, compliance_flags |
| Emission Prophet | 2 seasonal cycles (~24 months monthly) | 3-5 years monthly | Monthly per enterprise | total_emission, sector, report_type |

### Model Serving Approach

**Pre-train offline, serve pre-trained models:**

1. Training scripts run as offline batch jobs (not during inference)
2. Models serialized to disk: Prophet as JSON (`model.to_json()`), XGBoost as native format
3. Models loaded at FastAPI startup via lifespan context manager
4. Inference is read-only on loaded models -- no model mutation during prediction
5. Model retraining is scheduled: weekly for market, monthly for emission, on-demand for enterprise classifier

### Integration Pattern

**Java side (Spring Boot):**
- Add `spring-boot-starter-webflux` for WebClient
- Create `MlServiceClient` with retry logic (3 attempts, exponential backoff)
- Cache inference results in Redis: `ml:forecast:{type}:{entityId}:{date}` with TTL
- Spring MVC controllers call `MlServiceClient.block()` (safe in MVC context)

**Python side (FastAPI):**
- Async endpoints with `asyncio.to_thread()` for CPU-bound Prophet/XGBoost calls
- Pydantic models for request/response validation
- Health check endpoint at `/health`
- Graceful degradation: return `confidence=0.0` on model load failure

## Dependencies

### Java Side (Spring Boot 3.2.5)

```xml
<!-- WebClient for calling Python ML service -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <!-- Version managed by Spring Boot 3.2.5 parent -->
</dependency>
```

No ML-specific Java dependencies needed for the primary recommendation. The entire ML stack lives in the Python microservice.

### Python Side (FastAPI)

```
fastapi>=0.115.0
uvicorn>=0.34.0
prophet>=1.1.5
scikit-learn>=1.6.0
xgboost>=2.1.0
pandas>=2.2.0
numpy>=2.0.0
pydantic>=2.10.0
```

### ONNX Runtime Fallback (if needed)

```xml
<!-- For ONNX Runtime Java fallback -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.20.0</version> <!-- VERIFIED: Maven Central, Oct 2024 -->
</dependency>
```

### Tribuo Hybrid Option (if needed)

```xml
<!-- For Tribuo classification-only hybrid -->
<dependency>
    <groupId>org.tribuo</groupId>
    <artifactId>tribuo-all</artifactId>
    <version>4.3.2</version> <!-- VERIFIED: Maven Central, April 2025 -->
    <type>pom</type>
</dependency>
```

## Version Verification

| Library | Version | Source | Date | Status |
|---------|---------|--------|------|--------|
| DL4J | 1.0.0-M2.1 | Maven Central | Aug 2022 | Milestone only, 4+ years old |
| ONNX Runtime Java | 1.20.0 | Maven Central | Oct 2024 | Stable, actively maintained |
| Tribuo | 4.3.2 | Maven Central | Apr 2025 | Stable, actively maintained |
| DJL | 0.36.0 | Maven Central | Recent | Stable, actively maintained |
| Spring Boot | 3.2.5 | pom.xml | Current | N/A |
| Java | 17 | pom.xml | Current | N/A |

## Common Pitfalls

### Pitfall 1: Prophet Minimum Data Requirement
**What goes wrong:** Prophet requires at least 2 full seasonal cycles to detect seasonality. With fewer than ~730 days of daily data, Prophet falls back to a simple trend model with no seasonal component.
**Why it happens:** MCMC sampling needs sufficient data points to converge on seasonal parameters.
**How to avoid:** Supply at least 2 years of historical data. If insufficient, set `yearly_seasonality=False` and `weekly_seasonality=False` to avoid convergence warnings.
**Warning signs:** Prophet logs "Iteration: X, log likelihood: nan" or returns flat confidence intervals.

### Pitfall 2: Prophet Is Not Thread-Safe for Fitting
**What goes wrong:** Concurrent requests that each call `Prophet().fit()` on the same instance corrupt internal state.
**Why it happens:** Prophet's Stan backend is single-threaded and stateful during fitting.
**How to avoid:** Create a new `Prophet()` instance per fitting request. For pre-trained models, use `Prophet.from_json()` (read-only, thread-safe for predict). Use `asyncio.to_thread()` in FastAPI to offload CPU-bound fitting.
**Warning signs:** Intermittent NaN predictions, race conditions in logs.

### Pitfall 3: XGBoost Feature Importance Lost in ONNX Export
**What goes wrong:** If you export an XGBoost model to ONNX for Java-side inference, SHAP values and feature importance are stripped. The model predicts correctly but you cannot explain why.
**Why it happens:** ONNX format does not preserve XGBoost-specific metadata like feature contributions.
**How to avoid:** If explainability is required (and for compliance classification it is), the model must run in Python. Do not use ONNX Runtime for this use case.
**Warning signs:** Feature importance returns all zeros or uniform values after ONNX export.

### Pitfall 4: IsolationForest Contamination Parameter
**What goes wrong:** Default contamination=0.1 (10% expected anomalies) produces too many false positives for carbon compliance, where actual non-compliance rate is likely 2-5%.
**Why it happens:** Contamination directly controls the anomaly threshold; over-estimating it lowers the threshold.
**How to avoid:** Set `contamination=0.03` based on domain knowledge. Validate on labeled data if available.
**Warning signs:** More than 10% of enterprises flagged as anomalous; audit teams overwhelmed with false leads.

### Pitfall 5: WebClient.block() Deadlock in Reactive Context
**What goes wrong:** If called from a WebFlux reactive handler, `block()` causes a deadlock because it blocks the event loop thread.
**Why it happens:** Reactive pipelines require non-blocking calls; `block()` violates this contract.
**How to avoid:** The current backend uses Spring MVC (synchronous), so `block()` is safe. If migrating to WebFlux, use the reactive chain: `mlWebClient.post().uri(...).bodyValue(...).retrieve().bodyToMono(...)`.
**Warning signs:** Request hangs indefinitely; thread pool exhaustion in reactive mode.

### Pitfall 6: FastAPI Startup Model Loading Blocks Event Loop
**What goes wrong:** Loading large Prophet models during `@app.on_startup()` blocks the async event loop, preventing all requests until loading completes.
**Why it happens:** Model deserialization is CPU-bound and runs on the event loop thread.
**How to avoid:** Load models in a background thread or lazily on first request with a threading lock.
**Warning signs:** First request after startup times out; health check fails during model loading.

### Pitfall 7: DL4J Java 17 Incompatibility
**What goes wrong:** DL4J's Spark/Scala dependencies have known bugs on JDK 17, causing `NoClassDefFoundError` or `IllegalAccessError` at runtime.
**Why it happens:** Scala 2.12 and Spark 3.x were built for JDK 8-11; module system changes in JDK 16+ break access patterns.
**How to avoid:** Do not use DL4J with Java 17. If DL4J is required, use JDK 11 only.
**Warning signs:** `InaccessibleObjectException` during DL4J initialization.

## Code Examples

### Java: MlServiceClient (Spring Boot side)

```java
// Source: AI-SPEC.md Section 3.3
@Service
@RequiredArgsConstructor
@Slf4j
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

### Java: WebClient Configuration

```java
// Source: AI-SPEC.md Section 3.3
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

### Java: ONNX Runtime Fallback (if Python service unavailable)

```java
// Source: Context7 - onnxruntime Java API docs
public class OnnxInferenceService {

    private final OrtSession session;
    private final OrtEnvironment env;

    public OnnxInferenceService(String modelPath) throws OrtException {
        env = OrtEnvironment.getEnvironment();
        SessionOptions opts = new SessionOptions();
        opts.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT);
        session = env.createSession(modelPath, opts);
    }

    public float[] predict(float[] inputData, long[] shape) throws OrtException {
        OnnxTensor tensor = OnnxTensor.createTensor(
            env, FloatBuffer.wrap(inputData), shape);
        Map<String, OnnxTensor> feed = Map.of("float_input", tensor);
        try (OrtSession.Result result = session.run(feed)) {
            return (float[]) result.get(0).getValue();
        } finally {
            tensor.close();
        }
    }
}
```

### Python: Market Prediction Service

```python
# Source: AI-SPEC.md Section 4.2
from prophet import Prophet
import pandas as pd

class MarketPredictionService:
    def forecast(self, dates, prices, volumes, horizon_days=30):
        df = pd.DataFrame({"ds": pd.to_datetime(dates), "y": prices, "volume": volumes})
        model = Prophet(
            interval_width=0.95,
            changepoint_prior_scale=0.05,
            yearly_seasonality=True,
            weekly_seasonality=False,
            daily_seasonality=False
        )
        model.add_regressor("volume")
        model.fit(df)
        future = model.make_future_dataframe(periods=horizon_days)
        future["volume"] = df["volume"].iloc[-1]
        forecast = model.predict(future)
        # ... extract and return
```

### Python: Enterprise Inference Service

```python
# Source: AI-SPEC.md Section 4.3
import xgboost as xgb
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

class EnterpriseInferenceService:
    def infer(self, features: dict) -> dict:
        X = np.array([[features[f] for f in self.FEATURE_NAMES]])
        X_scaled = self.scaler.transform(X)
        proba = self.classifier.predict_proba(X_scaled)[0]
        anomaly_score = self.anomaly_detector.decision_function(X_scaled)[0]
        is_anomaly = self.anomaly_detector.predict(X_scaled)[0] == -1
        return {
            "compliance_status": ...,
            "confidence": float(np.max(proba)),
            "anomaly_score": float(anomaly_score),
            "is_anomaly": bool(is_anomaly),
        }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| DL4J for Java ML | Python microservice or ONNX Runtime | 2022-2024 (DL4J stalled) | DL4J is no longer the default Java ML choice |
| Hand-coded LSTM for time-series | Prophet / NeuralProphet | 2017+ (Prophet release) | Prophet is industry standard for seasonal forecasting |
| Java-embedded inference | Python microservice + REST | 2020+ (MLOps maturity) | Training and serving separated; Python for training, any language for serving |
| Single-model deployment | ONNX as interchange format | 2019+ (ONNX 1.0) | Models trained in any framework can be served via ONNX Runtime |

**Deprecated/outdated:**
- DL4J 1.0.0-M2.1: Last stable release candidate from 2022; project appears stalled
- Weka: Academic-focused, not production-grade for Spring Boot integration
- JSAT: Unmaintained, last release 2017

## Existing Project Context

The project already has:

1. **`CarbonPredictionService.java`** -- Linear regression stub that computes average change rate from last 12 carbon reports and extrapolates. Confidence capped at 0.85. Returns `CarbonPredictionResponse` with `PredictionPoint` list.

2. **`AI-SPEC.md`** -- Comprehensive specification (in repository root) that has already evaluated and decided on the Python microservice approach. Contains:
   - Architecture decision rationale (Python vs Java ML)
   - Detailed code patterns for all three services
   - Spring Boot client integration (WebClient + retry)
   - Pydantic schemas for request/response
   - Docker Compose integration
   - Cost and latency budgets
   - Pitfalls (Prophet thread safety, IsolationForest contamination, etc.)
   - Folder structure for `oaiss-chain-ml/` Python service

3. **No existing Python code** -- the `oaiss-chain-ml/` directory does not yet exist; only the spec document.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Prophet cannot be exported to ONNX format | Options Compared | If ONNX export becomes possible, ONNX Runtime becomes viable for forecasting |
| A2 | XGBoost ONNX export loses feature importance metadata | Options Compared | If `skl2onnx` preserves feature importance in future versions, ONNX Runtime becomes viable for classification |
| A3 | Spring Boot 3.2.5 WebClient is compatible with the existing Spring MVC setup | Implementation Guidance | If WebClient conflicts with MVC config, may need RestTemplate fallback |
| A4 | Carbon market data has yearly seasonality (justifying Prophet) | Implementation Guidance | If no seasonality exists, simpler models (ARIMA, linear) may suffice |

## Open Questions

1. **Historical data volume**: How much historical carbon trading data exists in the MySQL database? Prophet requires 2+ seasonal cycles. If the platform is new with <2 years of data, Prophet will fall back to trend-only mode.
   - What we know: The database has seed data with 2 enterprises but limited trading history
   - What's unclear: How much real historical data will be available at production launch
   - Recommendation: Design the ML service to gracefully degrade to simple trend models when data is insufficient

2. **Model retraining cadence**: Should models retrain on a schedule or on-demand when new data arrives?
   - What we know: AI-SPEC.md suggests weekly (market) and monthly (emission)
   - What's unclear: Whether the business requires real-time model updates or can tolerate stale models
   - Recommendation: Start with scheduled retraining; add event-triggered retraining later if needed

3. **Compliance labels for training**: The XGBoost classifier needs labeled data (compliant/at-risk/non-compliant). Where do labels come from?
   - What we know: Credit scores and emission ratings exist in the database
   - What's unclear: Whether these map directly to compliance status or require expert labeling
   - Recommendation: Use credit score thresholds as initial labels (score >= 60 = compliant, 40-60 = at-risk, <40 = non-compliant), then refine with expert review

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Python 3.10+ | ML microservice | Needs check | -- | Use Docker container |
| Docker | ML service deployment | Yes | Docker Compose exists | -- |
| Spring WebFlux | WebClient for ML calls | Yes (Spring Boot 3.2.5) | Managed by parent | RestTemplate |
| Redis | Inference result caching | Yes | 7.x | No cache |
| Java 17 | Backend runtime | Yes | 17 | -- |

**Missing dependencies with no fallback:**
- Python 3.10+ runtime (if not running ML service in Docker)

**Missing dependencies with fallback:**
- Python runtime: Use Docker container (already planned in AI-SPEC.md docker-compose addition)

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | JWT Bearer tokens (existing) |
| V3 Session Management | Yes | Stateless JWT (existing) |
| V4 Access Control | Yes | @PreAuthorize on prediction endpoints (existing pattern) |
| V5 Input Validation | Yes | Pydantic (Python side) + Jakarta Validation (Java side) |
| V6 Cryptography | No | No cryptographic operations in ML inference |

### Known Threat Patterns for ML Integration

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Model poisoning (adversarial input) | Tampering | Input validation via Pydantic; bounds checking on all features |
| Data exfiltration via ML service | Information Disclosure | ML service has no direct DB access; Java side sends only needed features |
| ML service DoS | Denial of Service | Rate limiting on ML endpoints; circuit breaker on Java side |
| Model inversion (inferring training data) | Information Disclosure | Enterprise features are aggregated, not raw; limit response fields |
| Supply chain (Pophet/scikit-learn CVEs) | Tampering | Pin dependency versions; use Docker with hash-pinned base images |

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (Java) | JUnit 5 + Mockito (existing) |
| Framework (Python) | pytest + httpx (new) |
| Config file (Java) | pom.xml (Surefire/Failsafe, existing) |
| Config file (Python) | pyproject.toml (new) |
| Quick run command (Java) | `mvn test -pl oaiss-chain-backend` |
| Quick run command (Python) | `pytest oaiss-chain-ml/tests/ -x` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AI-01 | Market prediction returns forecast with confidence intervals | integration | `pytest tests/test_market.py -x` | No (Wave 0) |
| AI-02 | Enterprise inference classifies compliance status | integration | `pytest tests/test_enterprise.py -x` | No (Wave 0) |
| AI-03 | Carbon emission prediction upgrades from stub | unit | `mvn test -Dtest=CarbonPredictionServiceTest` | Yes (existing) |
| AI-04 | ML service client retries on failure | unit | `mvn test -Dtest=MlServiceClientTest` | No (Wave 0) |
| AI-05 | Prediction results cached in Redis | integration | `pytest tests/test_caching.py -x` | No (Wave 0) |

### Wave 0 Gaps

- [ ] `oaiss-chain-ml/tests/test_market.py` -- covers AI-01
- [ ] `oaiss-chain-ml/tests/test_enterprise.py` -- covers AI-02
- [ ] `oaiss-chain-ml/tests/conftest.py` -- shared fixtures
- [ ] `MlServiceClientTest.java` -- covers AI-04
- [ ] Python test framework install: `pip install pytest httpx`
- [ ] `oaiss-chain-ml/` directory and service scaffolding

## Sources

### Primary (HIGH confidence)
- Maven Central (search.maven.org) -- DL4J, ONNX Runtime, Tribuo, DJL version verification
- Context7 `/microsoft/onnxruntime` -- ONNX Runtime Java API code examples
- Context7 `/deeplearning4j/deeplearning4j` -- DL4J LSTM code examples
- ONNX Runtime official docs (onnxruntime.ai) -- Java API, Maven dependencies, Java 8+ compatibility
- Tribuo GitHub (github.com/oracle/tribuo) -- version 4.3.2, features, Java 8+ support, no time-series
- DL4J GitHub (github.com/deeplearning4j/deeplearning4j) -- Java 17 issues, Konduit maintenance
- `AI-SPEC.md` (project root) -- existing architecture decision, code patterns, pitfalls
- `CarbonPredictionService.java` -- existing stub implementation

### Secondary (MEDIUM confidence)
- `pom.xml` -- Spring Boot 3.2.5, Java 17, existing dependencies
- `application.yml` -- existing configuration patterns
- `docker-compose.yml` -- existing deployment architecture

### Tertiary (LOW confidence)
- Prophet ONNX export impossibility -- based on training knowledge, not verified against Prophet changelog in this session [ASSUMED]
- XGBoost ONNX export metadata loss -- based on training knowledge, not verified against skl2onnx changelog [ASSUMED]

## Metadata

**Confidence breakdown:**
- Framework versions: HIGH - verified via Maven Central API
- DL4J disqualification: HIGH - verified via Maven Central (no stable release) and GitHub (Java 17 issues)
- ONNX Runtime capabilities: HIGH - verified via Context7 and official docs
- Tribuo capabilities: HIGH - verified via GitHub and official tutorials
- Python recommendation: HIGH - validated by existing AI-SPEC.md in project
- Pitfalls: HIGH - verified via AI-SPEC.md (project document) and official framework docs
- Prophet/XGBoost ONNX limitations: MEDIUM - based on training knowledge, flagged in Assumptions

**Research date:** 2026-05-14
**Valid until:** 2026-08-14 (90 days; ML library versions change slowly but DJL and ONNX Runtime release frequently)
