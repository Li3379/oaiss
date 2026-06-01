---
phase: 07-ai-intelligent-prediction-foundation
plan: 01
subsystem: ml-service
tags: [fastapi, webclient, pydantic, docker, scaffold]
dependency_graph:
  requires: []
  provides: [ml-service-scaffold, ml-webclient, ml-dtos]
  affects: [07-02, 07-03, 07-04]
tech_stack:
  added: [Python 3.11, FastAPI 0.115.6, Pydantic 2.10.6, uvicorn, spring-boot-starter-webflux]
  patterns: [microservice, @ConfigurationProperties, WebClient with timeout]
key_files:
  created:
    - oaiss-chain-ml-service/app/main.py
    - oaiss-chain-ml-service/app/config.py
    - oaiss-chain-ml-service/app/schemas/market.py
    - oaiss-chain-ml-service/app/schemas/enterprise.py
    - oaiss-chain-ml-service/app/schemas/emission.py
    - oaiss-chain-ml-service/Dockerfile
    - oaiss-chain-ml-service/requirements.txt
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/MlServiceConfig.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/MlServiceClient.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/MarketForecastRequest.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/MarketForecastResponse.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/EnterpriseInferenceRequest.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/EnterpriseInferenceResponse.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/EmissionForecastRequest.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/EmissionForecastResponse.java
  modified:
    - oaiss-chain-backend/pom.xml
    - oaiss-chain-backend/src/main/resources/application.yml
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/constant/ErrorCode.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/constant/ErrorMessage.java
    - docker-compose.yml
    - .env.example
decisions:
  - Used oaiss-chain-ml-service/ directory name (plan specified oaiss-chain-ml/ but user prompt specified oaiss-chain-ml-service/)
  - Used port 8001 for ML service (plan specified 8000 but user prompt specified 8001)
  - Used Python 3.11-slim in Dockerfile (plan specified 3.12-slim but user prompt specified 3.11)
  - Used @ConfigurationProperties pattern for MlServiceConfig instead of separate Config+ClientConfig classes
  - Used service/ml/ package for MlServiceClient (plan specified client/ package but user prompt specified service/ml/)
  - Added ML error code range 6xxx to ErrorCode/ErrorMessage constants
metrics:
  duration: ~13min
  completed: 2026-05-14T08:26:05Z
  tasks: 2
  files: 29
---

# Phase 07 Plan 01: ML Service Scaffold Summary

FastAPI ML microservice scaffold with /health endpoint, Pydantic schemas for 3 prediction types, Spring Boot WebClient integration with error handling, and Docker Compose ml-service entry.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Scaffold Python ML service | ffc3261 | 15 files (app/, Dockerfile, requirements.txt, schemas/) |
| 2 | WebClient integration + infrastructure | 3a6c8aa | 14 files (DTOs, config, client, pom.xml, yml, compose, env) |

## Key Deliverables

### Python ML Service (oaiss-chain-ml-service/)
- FastAPI app with `/health` endpoint returning `{status: "healthy", version: "1.0.0"}`
- 3 stub prediction endpoints returning 501 (Plans 07-02/03/04 will implement)
- Pydantic v2 schemas: MarketForecastRequest/Response, EnterpriseInferenceRequest/Response, EmissionForecastRequest/Response
- `config.py` using pydantic-settings BaseSettings (MODEL_DIR, LOG_LEVEL)
- Dockerfile with Python 3.11-slim, uvicorn on port 8001

### Spring Boot Integration
- `MlServiceConfig` with `@ConfigurationProperties(prefix = "ml.service")` and `mlWebClient` bean
- `MlServiceClient` generic `post(endpoint, request, responseType)` method with WebClient error handling
- 6 Java DTOs field-aligned with Pydantic schemas
- ErrorCode 6xxx range for ML service errors (ML_SERVICE_UNAVAILABLE, ML_SERVICE_ERROR, ML_PREDICTION_FAILED)
- `spring-boot-starter-webflux` added to pom.xml (WebClient only, backend remains Spring MVC)
- `ml.service.url`, `connect-timeout`, `read-timeout` in application.yml

### Docker Compose
- `ml-service` container: Python FastAPI on port 8001, health check on `/health`
- `.env.example` includes `ML_SERVICE_URL=http://ml-service:8001`

## Deviations from Plan

### Naming Adjustments

**1. Directory name: oaiss-chain-ml-service vs oaiss-chain-ml**
- **Found during:** Task 1 execution
- **Issue:** Plan specified `oaiss-chain-ml/` directory; user prompt specified `oaiss-chain-ml-service/`
- **Fix:** Followed user prompt directive (oaiss-chain-ml-service/)
- **Files:** All ML service files
- **Commit:** ffc3261

**2. Port: 8001 vs 8000**
- **Found during:** Task 1 execution
- **Issue:** Plan specified port 8000; user prompt specified port 8001
- **Fix:** Followed user prompt directive (8001)
- **Files:** Dockerfile, docker-compose.yml, application.yml, .env.example
- **Commit:** ffc3261, 3a6c8aa

**3. Python version: 3.11 vs 3.12**
- **Found during:** Task 1 execution
- **Issue:** Plan specified python:3.12-slim; user prompt specified Python 3.11
- **Fix:** Followed user prompt directive (3.11-slim)
- **Files:** Dockerfile
- **Commit:** ffc3261

### Architectural Decisions

**4. @ConfigurationProperties pattern for MlServiceConfig**
- **Found during:** Task 2 execution
- **Issue:** Plan specified separate Config+ClientConfig classes; simpler to combine
- **Fix:** Used single `@ConfigurationProperties` class with embedded `@Bean`
- **Files:** MlServiceConfig.java
- **Commit:** 3a6c8aa

**5. Package location: service/ml/ vs client/**
- **Found during:** Task 2 execution
- **Issue:** Plan specified `client/` package; user prompt specified `service/ml/`
- **Fix:** Followed user prompt directive (service/ml/)
- **Files:** MlServiceClient.java
- **Commit:** 3a6c8aa

## Known Stubs

| Stub | File | Description | Resolution Plan |
|------|------|-------------|-----------------|
| Stub endpoints | oaiss-chain-ml-service/app/main.py | `/api/v1/predict/market`, `/enterprise`, `/emission` return 501 | Plan 07-02 (market), 07-03 (enterprise), 07-04 (emission) |
| Empty services dir | oaiss-chain-ml-service/app/services/__init__.py | No service logic yet | Plan 07-02/03/04 |
| Empty models dir | oaiss-chain-ml-service/app/models/ | No trained model artifacts | Plan 07-02/03/04 |
| Empty routers dir | oaiss-chain-ml-service/app/routers/ | No separate router modules | Plan 07-02/03/04 |

## Self-Check

- oaiss-chain-ml-service/app/main.py: FOUND
- oaiss-chain-ml-service/requirements.txt: FOUND
- oaiss-chain-ml-service/Dockerfile: FOUND
- docker-compose.yml ml-service: FOUND
- pom.xml webflux: FOUND
- MlServiceConfig.java mlWebClient bean: FOUND
- MlServiceClient.java post method: FOUND
- application.yml ml.service.url: FOUND
- .env.example ML_SERVICE_URL: FOUND
- 6 Java DTOs: FOUND (MarketForecast, EnterpriseInference, EmissionForecast Request+Response)

## Self-Check: PASSED