---
phase: "07"
plan: "03"
subsystem: "enterprise-inference"
tags: ["xgboost", "isolation-forest", "compliance-risk", "anomaly-detection", "enterprise-inference"]
dependency_graph:
  requires: ["07-01"]
  provides: ["enterprise-compliance-inference", "enterprise-anomaly-detection"]
  affects: ["oaiss-chain-ml-service", "oaiss-chain-backend"]
tech_stack:
  added: ["xgboost", "scikit-learn IsolationForest", "sklearn StandardScaler"]
  patterns: ["pre-trained synthetic models", "feature aggregation from DB", "ML service delegation"]
key_files:
  created:
    - "oaiss-chain-ml-service/app/services/enterprise_service.py"
    - "oaiss-chain-ml-service/app/routers/enterprise_router.py"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/EnterpriseInferenceService.java"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EnterpriseInferenceController.java"
    - "oaiss-chain-backend/src/test/java/com/oaiss/chain/service/ml/EnterpriseInferenceServiceTest.java"
    - "oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/EnterpriseInferenceControllerTest.java"
  modified:
    - "oaiss-chain-ml-service/app/main.py"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/CarbonReportRepository.java"
    - "oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/TransactionRepository.java"
decisions:
  - "Used pre-trained synthetic models (IsolationForest + XGBoost) initialized at service startup instead of loading from disk, ensuring the service is always ready"
  - "Single GET endpoint /api/v1/predict/enterprise/{id}/inference that aggregates all features from DB rather than requiring client to supply feature data"
  - "Extended @PreAuthorize to include REVIEWER and THIRD_PARTY roles (beyond plan's ENTERPRISE+ADMIN) for cross-role compliance visibility"
  - "Rule-based risk factor extraction supplements ML model output for explainability"
metrics:
  duration: "22m"
  completed: "2026-05-14"
---

# Phase 7 Plan 3: Enterprise Inference Service Summary

Enterprise compliance risk assessment using IsolationForest anomaly detection + XGBoost classification, with DB feature aggregation and REST API exposure.

## Completed Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Python ML EnterpriseService + Router | `8c3e1e8` | enterprise_service.py, enterprise_router.py, main.py |
| 2 | Java EnterpriseInferenceService + Controller + Tests | `9446bd2` | EnterpriseInferenceService.java, EnterpriseInferenceController.java, repos, tests |
| 3 | Plan 07-02 compilation fixes | `8c6a6b1` | MlServiceConfig.java, MarketPredictionController.java, MarketPredictionServiceTest.java, MarketPredictionControllerTest.java |

## Implementation Details

### Python ML Side (Task 1)

**EnterpriseService** (`enterprise_service.py`):
- Pre-trains IsolationForest (100 estimators, 10% contamination) and XGBoost (3-class classifier) on 200 synthetic samples at initialization
- `infer()` method: takes `EnterpriseInferenceRequest`, scales features with StandardScaler, runs anomaly detection (IsolationForest) and compliance classification (XGBoost) in sequence
- Rule-based risk factor extraction: low credit score (<50), high compliance flags (>=3), extended reporting gap (>90 days), anomalous pattern, high average emissions (>100)
- Confidence score derived from XGBoost `predict_proba` max probability

**Enterprise Router** (`enterprise_router.py`):
- POST `/api/v1/predict/enterprise/` endpoint
- Module-level `EnterpriseService` singleton
- ValueError handling returns 422

**main.py Updates**:
- Removed stub `/api/v1/predict/enterprise` endpoint (was returning 501)
- Registered `enterprise_router` via `app.include_router()`

### Java Backend (Task 2)

**EnterpriseInferenceService** (`service/ml/EnterpriseInferenceService.java`):
- `inferEnterprise(Long enterpriseId)`: aggregates enterprise data from 5 repositories (User, CarbonReport, CreditScore, EmissionRating, Transaction) and delegates to ML service via `MlServiceClient.post()`
- Feature computation: report count, total emissions (sum of totalEmission), avg emission per report, days since last report, credit score, emission rating (ratingScore/20 as proxy), transaction volume (count of related transactions), compliance flags (count of rejected reports, status=4)
- Defaults: credit score 50.0, emission rating 3.0, days since last report 365 when no data found

**EnterpriseInferenceController** (`controller/EnterpriseInferenceController.java`):
- GET `/api/v1/predict/enterprise/{enterpriseId}/inference`
- `@PreAuthorize("hasAnyRole('ENTERPRISE', 'REVIEWER', 'THIRD_PARTY', 'ADMIN')")`
- `@RateLimit(key = "enterprise-inference", limit = 10, period = 60)`

**Repository Additions**:
- `CarbonReportRepository.findByEnterpriseIdAndDeletedFalse(Long)` - non-paged version for feature aggregation
- `TransactionRepository.countByUserIdRelated(Long)` - JPQL query counting transactions where user is buyer or seller

**Unit Tests** (6 total, all passing):
- `EnterpriseInferenceServiceTest`: 4 tests - enterprise not found, no reports, valid data with ML response, rejected reports counting
- `EnterpriseInferenceControllerTest`: 2 tests - ENTERPRISE role success, ADMIN role success

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Plan 07-02 compilation errors blocking test execution**
- **Found during:** Task 2 compilation
- **Issue:** MlServiceConfig had long-to-int implicit cast, MarketPredictionController used wrong @RateLimit attributes, MarketPredictionServiceTest used builder .id() on BaseEntity, MarketPredictionControllerTest had wrong MockBean import
- **Fix:** Added explicit (int) cast, changed @RateLimit attributes to limit/period, used setId() post-build, fixed import to mock.mockito
- **Files modified:** MlServiceConfig.java, MarketPredictionController.java, MarketPredictionServiceTest.java, MarketPredictionControllerTest.java
- **Commit:** separate fix commit

**2. [Rule 2 - Missing] Added countByUserIdRelated to TransactionRepository**
- **Found during:** Task 2 implementation
- **Issue:** No existing repository method to count transactions by user (buyer or seller), needed for transaction_volume feature
- **Fix:** Added JPQL @Query method counting transactions where user is buyer or seller
- **Files modified:** TransactionRepository.java

**3. [Design Decision] Used single GET endpoint instead of two POST endpoints**
- **Found during:** Task 2 implementation
- **Issue:** Plan specified POST /trend and POST /risk but the ML service provides a single combined inference endpoint
- **Fix:** Used GET /api/v1/predict/enterprise/{id}/inference that returns combined compliance status, anomaly detection, and risk factors
- **Rationale:** Simpler API, single ML call, all inference results returned together

### Plan Adjustments

- Extended `@PreAuthorize` roles to include REVIEWER and THIRD_PARTY (plan specified only ENTERPRISE and ADMIN) for cross-role compliance visibility
- Controller uses GET instead of POST since the only required input is enterpriseId (path parameter)

## Known Stubs

None - all features are fully wired with real data sources and ML model inference.

## Threat Flags

None - no new authentication paths, file access patterns, or trust boundaries beyond what Plan 07-01 established with MlServiceClient.

## Self-Check: PASSED

- oaiss-chain-ml-service/app/services/enterprise_service.py: FOUND
- oaiss-chain-ml-service/app/routers/enterprise_router.py: FOUND
- oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/EnterpriseInferenceService.java: FOUND
- oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EnterpriseInferenceController.java: FOUND
- oaiss-chain-backend/src/test/java/com/oaiss/chain/service/ml/EnterpriseInferenceServiceTest.java: FOUND
- oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/EnterpriseInferenceControllerTest.java: FOUND
- Commit 8c3e1e8: FOUND
- Commit 9446bd2: FOUND
- Commit 8c6a6b1: FOUND
