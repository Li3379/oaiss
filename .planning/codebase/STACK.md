# Technology Stack

**Analysis Date:** 2026-05-18

## Languages

**Primary:**
- Java 17 (Eclipse Temurin) - Backend application (Spring Boot 3.2.5), defined in `oaiss-chain-backend/pom.xml` `<java.version>17</java.version>`
- TypeScript 6.0.3 - Frontend Vue 3 application, defined in `oaiss-chain-frontend/package.json`
- Python 3.11 - ML prediction microservice, defined in `oaiss-chain-ml-service/Dockerfile` `FROM python:3.11-slim`

**Secondary:**
- Go 1.21 - Hyperledger Fabric chaincode (smart contract), defined in `oaiss-chain-chaincode/go.mod` `go 1.21`
- SQL (MySQL 8.0 dialect) - Flyway database migrations at `oaiss-chain-backend/src/main/resources/db/migration/`
- YAML - Application configuration (`application*.yml`), Docker Compose files
- JavaScript (ES Modules) - Vite config (`vite.config.js`), Playwright tests

## Runtime

**Environment:**
- Java: Eclipse Temurin 17 JRE (production Docker), Maven 3.9 + Eclipse Temurin 17 JDK (build Docker)
  - Configured in `oaiss-chain-backend/Dockerfile`: `FROM eclipse-temurin:17-jre` (runtime), `FROM maven:3.9-eclipse-temurin-17` (build)
  - JVM flags: `-Xms512m -Xmx1024m -XX:+UseG1GC`
- Node.js 20 Alpine - Frontend build and production serve
  - Configured in `oaiss-chain-frontend/Dockerfile`: `FROM node:20-alpine`
- Python 3.11 Slim - ML service runtime
  - Configured in `oaiss-chain-ml-service/Dockerfile`: `FROM python:3.11-slim`

**Package Manager:**
- Backend: Apache Maven (no version pinned; inherits from spring-boot-starter-parent 3.2.5)
  - Lockfile: Not present (Maven uses pom.xml + local `.m2` repository cache)
- Frontend: npm (package-lock.json)
  - Lockfile: Present (standard npm mechanism)
- ML Service: pip (requirements.txt)
  - Lockfile: Not present (no pip freeze or poetry.lock)
- Chaincode: Go modules (go.mod + go.sum)
  - Lockfile: Present (go.sum)

## Frameworks

**Core:**
| Framework | Version | Location | Purpose |
|-----------|---------|----------|---------|
| Spring Boot | 3.2.5 | `oaiss-chain-backend/pom.xml` | Backend REST API, security, data, caching |
| Vue | 3.5.32 | `oaiss-chain-frontend/package.json` | Frontend SPA framework |
| FastAPI | 0.115.6 | `oaiss-chain-ml-service/requirements.txt` | ML prediction microservice |
| Hyperledger Fabric Contract API | 1.2.2 | `oaiss-chain-chaincode/go.mod` | Blockchain smart contracts (Go) |
| Fabric Gateway SDK | 1.7.1 | `oaiss-chain-backend/pom.xml` | Blockchain client integration (Java) |

**Frontend UI:**
| Package | Version | Purpose |
|---------|---------|---------|
| Element Plus | 2.13.7 | UI component library |
| @element-plus/icons-vue | 2.3.2 | Icon set for Element Plus |
| ECharts | 6.0.0 | Charting / data visualization |
| Vue Router | 5.0.6 | Client-side routing with role-based guards |
| Pinia | 3.0.4 | State management |
| vue-i18n | 11.4.0 | Internationalization (zh-CN, en-US) |
| Axios | 1.7.0 | HTTP client with request/response interceptors |

**ML/AI:**
| Package | Version | Purpose |
|---------|---------|---------|
| Prophet | 1.1.6 | Time-series emission and price forecasting |
| scikit-learn | 1.6.1 | IsolationForest anomaly detection, classification |
| XGBoost | 2.1.4 | Supply/demand prediction, compliance classification |
| pandas | 2.2.3 | Data manipulation for ML pipelines |
| numpy | 2.2.3 | Numerical computation |
| pydantic | 2.10.6 | Request/response data validation |
| pydantic-settings | 2.7.1 | Environment variable configuration |

**Testing:**
| Package | Version | Purpose |
|---------|---------|---------|
| JUnit 5 (via spring-boot-starter-test) | BOM-managed | Backend unit tests |
| Testcontainers | 1.19.7 | Backend integration tests (MySQL + Redis containers) |
| testcontainers-redis | 2.2.2 | Redis module for Testcontainers |
| JaCoCo | 0.8.11 | Code coverage (90% line minimum per package) |
| H2 Database | BOM-managed | In-memory DB for backend unit tests |
| OkHttp MockWebServer | 4.12.0 | ML service contract tests |
| Vitest | 4.1.5 | Frontend unit tests |
| happy-dom | 20.9.0 | DOM environment for Vitest |
| @vue/test-utils | 2.4.10 | Vue component testing |
| Playwright | 1.59.1 | Frontend E2E tests |
| testify | 1.5.0 | Go chaincode tests |

**Build/Dev:**
| Tool | Version | Purpose |
|------|---------|---------|
| Maven Surefire | 3.2.5 | Unit test runner (`*Test.java`, `*Tests.java`) |
| Maven Failsafe | 3.2.5 | Integration test runner (`*IntegrationTest.java`) |
| Vite | 8.0.10 | Frontend dev server and build tool |
| @vitejs/plugin-vue | 6.0.6 | Vue SFC compilation for Vite |
| vue-tsc | 3.2.8 | TypeScript type checking for Vue SFCs |
| Flyway (via Spring Boot) | BOM-managed | Database schema migrations |
| PMD | 3.21.0 | Static code analysis (`failOnViolation: false`) |
| SpotBugs | 4.8.6.2 | Bug pattern detection (`failOnError: false`) |
| uvicorn[standard] | 0.34.0 | ASGI server for FastAPI |
| cross-env | 10.1.0 | Cross-platform env vars for test modes |

## Key Dependencies

**Backend Critical:**

| Package | Version | Why It Matters |
|---------|---------|----------------|
| spring-boot-starter-web | 3.2.5 (BOM) | REST API, embedded Tomcat, request routing |
| spring-boot-starter-data-jpa | 3.2.5 (BOM) | ORM via Hibernate, Spring Data repository pattern |
| spring-boot-starter-security | 3.2.5 (BOM) | JWT authentication, role-based authorization, security headers |
| spring-boot-starter-data-redis | 3.2.5 (BOM) | Redis caching, distributed locks, rate limiting |
| spring-boot-starter-webflux | 3.2.5 (BOM) | Non-blocking WebClient for ML service HTTP calls |
| spring-boot-starter-cache | 3.2.5 (BOM) | Cache abstraction with Redis-backed implementation |
| spring-boot-starter-validation | 3.2.5 (BOM) | Bean validation (JSR-380) for request DTOs |
| spring-boot-starter-aop | 3.2.5 (BOM) | AOP aspects for @AuditLog, @RateLimit, @DistributedLock, @DataIsolation |
| spring-boot-starter-actuator | 3.2.5 (BOM) | Health probes, metrics, Prometheus endpoint |
| jjwt (api/impl/jackson) | 0.12.5 | JWT token generation, parsing, validation (HMAC-SHA) |
| mysql-connector-j | BOM-managed | MySQL 8.0 JDBC driver |
| minio | 8.5.7 | S3-compatible object storage client for file management |
| fabric-gateway | 1.7.1 | Hyperledger Fabric Gateway SDK for blockchain transactions |
| grpc-netty-shaded | 1.62.2 | gRPC transport layer for Fabric peer communication |
| grpc-protobuf | 1.62.2 | Protobuf serialization for gRPC messages |
| grpc-stub | 1.62.2 | gRPC client stub generation |
| bcpkix-jdk18on | 1.78.1 | Bouncy Castle PKI for Fabric certificate handling |
| resilience4j-spring-boot3 | 2.2.0 | Circuit breaker for ML service fault tolerance |
| resilience4j-reactor | 2.2.0 | Reactor integration for circuit breaker with WebClient |
| mapstruct | 1.5.5.Final | Compile-time DTO-to-entity mapping code generation |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI and OpenAPI 3.0 documentation |
| logstash-logback-encoder | 7.4 | JSON structured logging for ELK compatibility |
| micrometer-registry-prometheus | BOM-managed | Prometheus-compatible metrics export |
| lombok | BOM-managed | @Data, @Builder, @RequiredArgsConstructor boilerplate reduction |
| flyway-mysql | BOM-managed | Database schema versioning and migration |

## Infrastructure

**Docker Compose** (`docker-compose.yml`):

| Service | Image | Ports | Purpose |
|---------|-------|-------|---------|
| mysql | mysql:8.0 | 127.0.0.1:3306:3306 | Primary database (utf8mb4) |
| redis | redis:7-alpine | 127.0.0.1:6379:6379 | Cache, session, rate limiting, distributed locks |
| minio | minio/minio:RELEASE.2025-04-22T22-12-26Z | 9002:9000 / 9003:9001 | S3-compatible object/file storage |
| backend | Custom multi-stage (maven:3.9 + eclipse-temurin:17) | 8080:8080 | Spring Boot REST API |
| frontend | Custom multi-stage (node:20-alpine) | 5173:80 | Vue SPA served by Node.js static server |
| ml-service | Custom (python:3.11-slim) | 8001:8001 | Python FastAPI ML prediction API |

**Infrastructure-only** (`docker-compose.infra.yml`):
- Runs MySQL (port 3307), Redis (port 6379), MinIO (port 9000/9001) for local development without building backend/frontend images.
- Same image versions as main compose file.

**Connection Pooling:**
- MySQL: HikariCP (min-idle: 5, max-pool: 20, max-lifetime: 30min, timeout: 30s)
  - Configured in `oaiss-chain-backend/src/main/resources/application.yml`
- Redis: Lettuce (max-active: 8, max-idle: 8, min-idle: 0, timeout: 10s)
  - Configured in `oaiss-chain-backend/src/main/resources/application.yml`
- ML Service WebClient: ConnectionProvider (max-connections: 50, pending-acquire-timeout: 10s)
  - Configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/MlServiceConfig.java`

**Database:**
- MySQL 8.0 with utf8mb4 charset and utf8mb4_unicode_ci collation
- Schema managed by Flyway: `V1__init_schema.sql`, `V2__seed_data.sql`, `V4__enterprise_admission.sql`
- DDL strategy: `validate` (default/docker), `create-drop` (test)

**Caching:**
- Primary: Redis 7 via Spring Cache abstraction (`@EnableCaching`)
- Fallback: `ConcurrentMapCacheManager` when Redis is unavailable
  - Configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/CacheConfig.java`
- Cache names: `captcha`, `users`, `permissions`
- Serialization: Jackson2JsonRedisSerializer (values), StringRedisSerializer (keys)
  - Configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RedisConfig.java`

**Monitoring:**
- Spring Actuator endpoints: health, info, metrics, prometheus
- Prometheus metrics via Micrometer with histograms (p50, p95, p99)
- JSON structured logging via logstash-logback-encoder 7.4
- Custom health indicators: Database, Redis, MinIO
  - Located at `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/DatabaseHealthIndicator.java`, `RedisHealthIndicator.java`, `MinioHealthIndicator.java`

## Configuration

**Environment:**
- Spring Boot externalized configuration with profile-based overrides
- Profiles: `default`, `dev`, `local`, `docker`, `test`, `fabric`
  - `application.yml` - Base config
  - `application-dev.yml` - Local dev overrides
  - `application-local.yml` - Local machine setup
  - `application-docker.yml` - Container environment
  - `application-test.yml` - Test environment (H2)
- Environment variables override YAML properties

**Required env vars** (from `.env.example`):
- `DB_PASSWORD` - MySQL root password
- `REDIS_PASSWORD` - Redis authentication password
- `JWT_SECRET` - HMAC-SHA256 signing key (256-bit)
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` - MinIO credentials
- `ML_SERVICE_URL` - ML service endpoint URL (default: `http://localhost:8001`)
- `CORS_ALLOWED_ORIGINS` - Allowed frontend origins (default: `http://localhost:5173,http://localhost:5174`)
- `SPRING_PROFILES_ACTIVE` - Active Spring profile

**Build configs:**
- Backend: `oaiss-chain-backend/pom.xml` (Maven, inherits spring-boot-starter-parent 3.2.5)
- Frontend: `oaiss-chain-frontend/vite.config.js` (Vite 8 with Vue plugin, path alias `@/`)
- ML Service: `oaiss-chain-ml-service/requirements.txt` (pip)
- Chaincode: `oaiss-chain-chaincode/go.mod` (Go modules, requires Go 1.21+)

## Platform Requirements

**Development:**
- Java 17 JDK (Eclipse Temurin recommended)
- Apache Maven 3.9+
- Node.js 20+ (Alpine or standard)
- Python 3.11+
- Go 1.21+ (chaincode development only)
- Docker & Docker Compose (for infrastructure services or full-stack)
- Git

**Production:**
- Docker Compose orchestrates all 6 services
- Backend: JVM with G1GC, 512MB-1GB heap
- Frontend: Node.js static file server (`docker/server.mjs`)
- ML Service: uvicorn ASGI server with standard workers
- All containers run as non-root users (oaiss, mluser)
- Graceful shutdown with 30s timeout on backend

---

*Stack analysis: 2026-05-18*
