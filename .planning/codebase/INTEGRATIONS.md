# External Integrations

**Analysis Date:** 2026-05-18

## APIs & External Services

### ML Service (Python FastAPI)
- **Service**: Python FastAPI microservice for carbon emission prediction, market forecasting, and enterprise compliance analysis
  - SDK/Client: Spring WebClient (non-blocking HTTP client from `spring-boot-starter-webflux`)
  - Config: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/MlServiceConfig.java`
  - Base URL: `http://localhost:8001` (configurable via `ML_SERVICE_URL` env var)
  - Connect timeout: 5s, Read timeout: 30s
  - Connection pool: 50 max connections, 10s pending acquire timeout
  - Auth: Optional `X-ML-Service-Secret` header (configured via `ml.service.secret`)
  - Resilience: Resilience4j circuit breaker (`mlService` instance)
    - Failure rate threshold: 50%, Slow call rate: 80%, Slow call duration: 5s
    - Wait in open state: 30s, Sliding window: 10 calls, Minimum calls: 5
    - Configured in `application.yml` under `resilience4j.circuitbreaker.instances.mlService`
  - Data serialization: camelCase (Java) to snake_case (Python) conversion via custom ObjectMapper
    - Configured in `MlServiceConfig.java` with `PropertyNamingStrategies.SNAKE_CASE`
  - Endpoints:
    - `GET /health` - Health check (returns `{"status": "healthy", "version": "1.0.0"}`)
    - `POST /predict/emission/forecast` - Prophet-based emission time-series forecasting
    - `POST /predict/market/*` - Market price/supply-demand prediction (Prophet, XGBoost)
    - `POST /predict/enterprise/*` - Compliance classification, anomaly detection (XGBoost, IsolationForest)
  - Router files:
    - `oaiss-chain-ml-service/app/routers/emission_router.py`
    - `oaiss-chain-ml-service/app/routers/market_router.py`
    - `oaiss-chain-ml-service/app/routers/enterprise_router.py`

### Hyperledger Fabric Blockchain
- **Service**: Hyperledger Fabric 2.5.x LTS network for carbon report and trade on-chain anchoring
  - SDK/Client: Fabric Gateway SDK 1.7.1 (Java), NOT the legacy fabric-gateway-java 2.2.x
  - Config: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java`
  - Properties: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricProperties.java`
  - Toggle: `fabric.enabled` config property (`false` = MockBlockchainService, `true` = FabricBlockchainService)
  - Profile: `@Profile("fabric")` on `FabricGatewayConfig` — only activates when Fabric profile is included
  - Connection: gRPC to Fabric peer endpoint (default: `peer0.org1.example.com:7051`)
  - TLS: Configurable via `fabric.tls-enabled` (default: `true`), uses Bouncy Castle for SSL context
  - Identity: X.509 certificate + private key from classpath resources (`fabric/crypto/`)
  - Network: Single-org MVP (Org1MSP), channel `mychannel`, chaincode `carbon-chaincode`
  - Timeouts: Connect 30s, Submit 60s
  - CA: Optional Fabric CA integration (`fabric.ca.enabled`, endpoint at `http://ca.org1.example.com:7054`)

## Data Storage

### Databases:
- **MySQL 8.0** - Primary relational database
  - Connection: `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/oaiss_chain`
  - Driver: `com.mysql.cj.jdbc.Driver` (`com.mysql:mysql-connector-j`, BOM-managed version)
  - ORM: Spring Data JPA with Hibernate, `MySQLDialect`
  - Connection pool: HikariCP (min-idle: 5, max-pool: 20, max-lifetime: 30min, timeout: 30s)
  - Configured in `application.yml` under `spring.datasource` and `spring.jpa`
  - Schema management: Flyway migrations at `src/main/resources/db/migration/`
    - `V1__init_schema.sql` - Initial schema (23 tables)
    - `V2__seed_data.sql` - Seed/reference data
    - `V4__enterprise_admission.sql` - Enterprise admission feature
  - DDL strategy: `validate` (default/docker), `create-drop` (test)
  - Charset: utf8mb4 with utf8mb4_unicode_ci collation
  - Docker: `mysql:8.0`, port 3306 (infra-only: 3307)

### File Storage:
- **MinIO** - S3-compatible object storage
  - Endpoint: `http://localhost:9002` (configurable via `MINIO_ENDPOINT`)
  - Client: `io.minio:minio:8.5.7`, configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/MinioConfig.java`
  - Auth: Access key / secret key (env vars `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`)
  - Bucket: `oaiss-chain` (configurable via `MINIO_BUCKET`)
  - Max file size: 100MB
  - Presigned URL expiry: 3600 seconds
  - Features: Upload (UUID naming, folder prefix), download (stream), delete, copy, presigned URLs, pagination, dangerous extension blocklist
  - Docker: `minio/minio:RELEASE.2025-04-22T22-12-26Z`, ports 9002 (API), 9003 (console)

### Caching:
- **Redis 7** - Cache, rate limiting, distributed locks
  - Connection: `localhost:6379` (configurable via `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
  - Client: Lettuce (Spring Data Redis), pool: max-active 8, max-idle 8
  - Serialization: `Jackson2JsonRedisSerializer<Object>` (values), `StringRedisSerializer` (keys)
    - Configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RedisConfig.java`
  - Templates: `RedisTemplate<String, Object>` (general), `RedisTemplate<String, Long>` (script operations)
  - Fallback: `ConcurrentMapCacheManager` when Redis is unavailable
    - Configured in `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/CacheConfig.java`
  - Cache names: `captcha`, `users`, `permissions`
  - Use cases:
    - Application caching via `@EnableCaching`
    - Rate limiting via `@RateLimit` annotation + AOP aspect
    - Distributed locks via `@DistributedLock` annotation + `RedisLockService`
    - Cache preloading via `CachePreloadService`
  - Docker: `redis:7-alpine`, port 6379, password-protected

## Authentication & Identity

### Auth Provider:
- **Custom JWT (self-managed)**
  - Implementation: HMAC-SHA256 signed JWT tokens
  - Library: `io.jsonwebtoken:jjwt:0.12.5` (api + impl + jackson modules)
  - Provider: `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtTokenProvider.java`
  - Filter: `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java`
  - Config: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java`
  - Secret: `JWT_SECRET` env var, used as HMAC signing key
  - Access token TTL: 1 hour (3,600,000 ms)
  - Refresh token TTL: 7 days (604,800,000 ms)
  - Password hashing: BCrypt via `BCryptPasswordEncoder`
  - Token claims: `userId`, `username` (subject), `roles`, `userType`, `enterpriseId`, `type` (access/refresh)
  - Token storage: Frontend stores in `sessionStorage` (not cookies)
  - Session policy: Stateless (`SessionCreationPolicy.STATELESS`)
  - Token refresh: Frontend interceptor detects expired access token, calls `/auth/refresh` with `Refresh-Token` header, queues concurrent requests during refresh (single-flight pattern)
  - Roles: `ENTERPRISE`, `REVIEWER`, `THIRD_PARTY`, `ADMIN`
  - Method security: `@PreAuthorize` annotations on controller methods, `@AuthenticationPrincipal` for user context
  - CSRF: Disabled (JWT in sessionStorage, not cookies, so CSRF not applicable)
  - CORS: Configured via `CorsConfigurationSource` bean, origins from `CORS_ALLOWED_ORIGINS` env var
  - Security headers: CSP, XSS protection, content-type options, frame-options deny, referrer policy

### Captcha:
- **Self-generated image captcha**
  - Service: `CaptchaService` with in-memory `ConcurrentHashMap` storage
  - Dimensions: 120x40px, 4-char alphanumeric code
  - Expiry: 300 seconds (5 minutes)
  - Endpoints: `/captcha/**` (public, no auth required)

### SMS/Email Verification:
- **Stub implementation** - logs codes to console, no actual delivery
  - Production requires SMS gateway and email service integration

### Digital Signatures (RSA):
- **Self-managed RSA key pairs**
  - Algorithm: SHA256withRSA
  - Key storage: `rsa_key_pair` table in MySQL (public + private keys in Base64)
  - Key lifecycle: 2-year expiry, versioned, three states (active, revoked, expired)
  - Use case: Report data signing and verification in carbon report audit workflow

## Monitoring & Observability

### Error Tracking:
- No dedicated error tracking service (e.g., Sentry). Errors are captured via:
  - Spring Boot logging (SLF4J + Logback)
  - JSON structured logs via `logstash-logback-encoder:7.4`
  - Spring Actuator health probes

### Logs:
- Framework: SLF4J + Logback with Logstash JSON encoder
- Format: JSON structured logs (`logstash-logback-encoder:7.4`)
- Pattern: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- Level: INFO (root and `com.oaiss.chain`), INFO (`org.springframework.security`)
- Output: stdout (container-friendly, ELK-compatible)
- No ELK stack deployed in Docker Compose; logs are stdout for external collection

### Metrics:
- Prometheus endpoint: `/actuator/prometheus` (public, no auth required)
- Library: `micrometer-registry-prometheus` (BOM-managed)
- Histograms: HTTP server request percentiles (p50, p95, p99)
- Health probes: Liveness and readiness at `/actuator/health`
- Custom health indicators: Database (`DatabaseHealthIndicator`), Redis (`RedisHealthIndicator`), MinIO (`MinioHealthIndicator`)
- Actuator base path: `/actuator`
- Exposed endpoints: health, info, metrics, prometheus
- Environment show-values: never (security)

## CI/CD & Deployment

### Hosting:
- **Docker Compose** - All services run in Docker containers
- No cloud deployment configuration detected
- No CI pipeline configuration detected (no `.github/workflows/`, no Jenkinsfile, no `.gitlab-ci.yml`)

### Container Details:
- Backend: Multi-stage build (Maven build + JRE runtime), non-root user `oaiss`
- Frontend: Multi-stage build (npm build + Node.js static serve), health check via fetch
- ML Service: Single-stage Python build with gcc/g++, non-root user `mluser`
- MySQL: Official image with health check (mysqladmin ping)
- Redis: Alpine image with password authentication and health check (redis-cli ping)
- MinIO: Specific release image with console address binding

## Environment Configuration

**Required env vars:**
- `DB_PASSWORD` - MySQL root password
- `REDIS_PASSWORD` - Redis authentication password
- `JWT_SECRET` - HMAC-SHA256 signing key for JWT tokens
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` - MinIO credentials

**Optional env vars:**
- `DB_HOST` (default: `localhost`), `DB_PORT` (default: `3306`), `DB_USERNAME` (default: `root`)
- `REDIS_HOST` (default: `localhost`), `REDIS_PORT` (default: `6379`)
- `MINIO_ENDPOINT` (default: `http://localhost:9002`), `MINIO_BUCKET` (default: `oaiss-chain`)
- `ML_SERVICE_URL` (default: `http://localhost:8001`)
- `CORS_ALLOWED_ORIGINS` (default: `http://localhost:5173,http://localhost:5174`)
- `SPRING_PROFILES_ACTIVE` (default: none)

**Secrets location:**
- `.env` file at project root (gitignored)
- Template: `.env.example` at project root
- Docker Compose uses inline env vars and `.env` file interpolation

## Webhooks & Callbacks

**Incoming:**
- None detected. The application does not expose any webhook endpoints.

**Outgoing:**
- None detected. The application does not call external webhook URLs.

## Frontend-Backend Integration

### REST API Communication:
- **Library**: Axios 1.7.0
- **Config**: `oaiss-chain-frontend/src/api/request.ts`
- **Base URL**: `VITE_API_BASE_URL` environment variable, proxied to `http://localhost:8080` via Vite dev server
- **Timeout**: 15 seconds
- **Retry**: 2 attempts with exponential backoff (1s base delay) for network errors (`ECONNABORTED`, `Network Error`)
- **Success codes**: 200, 0
- **Pagination transform**:
  - Request: Frontend sends `pageNum`/`pageSize`, Axios interceptor converts to `page`/`size`
  - Response: Spring Data `Page<T>` (with `content`, `totalElements`, `number`, `size`, `totalPages`) transformed to `{ items, total, page, size, totalPages }`
- **Token refresh**: Automatic via request interceptor; single-flight pattern queues concurrent requests during refresh
- **Error handling**: Global `ElMessage` notifications for 401 (redirect to login), 403, 404, network errors

### API Modules (22 modules in `oaiss-chain-frontend/src/api/`):

| Module | Purpose |
|--------|---------|
| `request.ts` | Axios instance with interceptors |
| `auth.ts` | Login, register, refresh, captcha |
| `user.ts` | User profile management |
| `admin.ts` | System admin, admission, qualification |
| `carbon.ts` | Carbon report CRUD |
| `trade.ts` | Carbon credit trading |
| `auction.ts` | Double auction orders and matching |
| `emission.ts` | Emission ratings and prediction |
| `credit.ts` | Credit score management |
| `carbonCoin.ts` | Carbon coin accounts and transactions |
| `carbonNeutral.ts` | Carbon neutral projects |
| `signature.ts` | RSA key management and signing |
| `blockchain.ts` | Chain status, blocks, transactions |
| `file.ts` | File upload/download via MinIO |
| `search.ts` | Cross-entity search |
| `thirdParty.ts` | Third-party organization management |
| `captcha.ts` | Captcha generation and verification |
| `marketPrediction.ts` | Market price/supply-demand prediction |
| `enterpriseInference.ts` | Compliance/anomaly/risk inference |
| `carbonFormula.ts` | Power generation/grid formula calculation |
| `reviewer.ts` | Reviewer operations |

## Blockchain Integration Detail

### Chaincode (Go):
- **Location**: `oaiss-chain-chaincode/chaincode.go`
- **Language**: Go 1.21, uses `github.com/hyperledger/fabric-contract-api-go v1.2.2`
- **Smart contract**: `CarbonChaincode` with functions:
  - `CreateCarbonReport` - Store carbon report on ledger
  - `CreateTradeRecord` - Store trade record on ledger
  - `GetCarbonReport` - Query report by ID
  - `GetTradeRecord` - Query trade by ID
  - `GetAllReports` - List all reports (range query)
  - `GetAllTrades` - List all trades (range query)
- **Data structures**: `CarbonReport` (reportId, enterpriseId, data, status, txHash, createdAt), `TradeRecord` (tradeId, sellerId, buyerId, amount, price, txHash, createdAt)
- **Tests**: `oaiss-chain-chaincode/chaincode_test.go` (uses `github.com/stretchr/testify v1.5.0`)

### Backend Blockchain Service:
- **Interface**: `BlockchainServicePort` (abstract interface)
- **Mock**: `MockBlockchainService` (development fallback, used when `fabric.enabled=false`)
- **Real**: `FabricBlockchainService` (Gateway SDK calls, used when `fabric.enabled=true`)
- **Controller**: `BlockchainController` at `/blockchain/**`
- **Capabilities**: invokeChaincode, queryChaincode, commitReportToChain, commitTradeToChain, queryBlock, queryTransaction, verifySignature, checkConnection, listTransactions, listLatestBlocks

## Integration Status Summary

| Service | Status | Notes |
|---------|--------|-------|
| MySQL 8.0 | Running | Primary database, Flyway-managed schema |
| Redis 7 | Running | Cache, rate limiting, distributed locks |
| MinIO | Running | Object storage for file uploads |
| ML Service (FastAPI) | Running | Docker container on port 8001, circuit breaker protected |
| Hyperledger Fabric | SDK integrated, network optional | `fabric.enabled` toggle; mock by default |
| Fabric CA | Optional | Shared org identity as MVP default |
| SMS Gateway | Stub only | CaptchaService logs codes, no delivery |
| Email Service | Stub only | CaptchaService logs codes, no delivery |
| ELK Stack | Logging ready, not deployed | JSON logs emitted to stdout |
| Prometheus | Endpoint ready | `/actuator/prometheus` exposed |
| CI/CD | Not configured | No pipeline files detected |

---

*Integration audit: 2026-05-18*
