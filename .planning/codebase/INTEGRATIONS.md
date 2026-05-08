# External Integrations

**Project**: OAISS CHAIN (双碳链动系统)
**Last updated**: 2026-05-08

---

## Databases

### MySQL 8.0 (Primary Database)

- **Connection**: `jdbc:mysql://localhost:3306/oaiss_chain`
- **Driver**: `com.mysql.cj.jdbc.Driver`
- **Connection Pool**: HikariCP (min-idle: 5, max-pool: 20, max-lifetime: 30min)
- **ORM**: Spring Data JPA with Hibernate, MySQL dialect
- **Schema Management**: Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`)
- **DDL Strategy**: `validate` (prod/docker), `update` (dev), `create-drop` (test)
- **Tables**: 21 tables (user, enterprise, reviewer, authenticator, carbon_report, transaction, auction_order, matching_result, rsa_key_pair, credit_score, credit_event, carbon_coin_account, carbon_coin_transaction, emission_rating, carbon_neutral_project, operation_log, etc.)
- **Charset**: utf8mb4 with utf8mb4_unicode_ci collation
- **Docker**: `mysql:8.0` on port 3306

### Redis 7 (Caching & Distributed State)

- **Connection**: `localhost:6379` (configurable via `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
- **Client**: Lettuce (Spring Data Redis)
- **Serialization**: Jackson2JsonRedisSerializer (values), StringRedisSerializer (keys)
- **Pool**: max-active: 8, max-idle: 8, min-idle: 0
- **Use Cases**:
  - Application caching (`@EnableCaching`)
  - Rate limiting (`@RateLimit` annotation + AOP)
  - Distributed locks (`@DistributedLock` annotation + `RedisLockService`)
  - Cache preloading (`CachePreloadService`)
- **Docker**: `redis:7-alpine` on port 6379

---

## Object Storage

### MinIO (S3-Compatible)

- **Endpoint**: `http://localhost:9000` (configurable via `MINIO_ENDPOINT`)
- **Auth**: Access key / secret key (configurable via `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`)
- **Default Bucket**: `oaiss-chain` (configurable via `MINIO_BUCKET`)
- **Max File Size**: 100 MB
- **Presigned URL Expiry**: 3600 seconds (1 hour)
- **Client Library**: `io.minio:minio:8.5.7`
- **Capabilities**:
  - File upload (with folder prefix, UUID-based naming)
  - File download (stream-based)
  - File delete (single and batch)
  - File copy
  - Presigned URLs for direct upload/download
  - File listing with pagination
  - Custom metadata (uploader ID tracking)
  - Dangerous extension blocklist (jsp, exe, sh, php, py, dll, etc.)
- **Docker**: `minio/minio:latest` on ports 9000 (API), 9001 (console)

---

## Blockchain

### Hyperledger Fabric (Mock Implementation)

- **Status**: Mock/development mode -- no real blockchain connection
- **Planned Channels**: `carbon-channel`
- **Planned Chaincodes**: `carbon-report-cc`, `carbon-trade-cc`
- **Service**: `BlockchainService` provides mock implementations for:
  - `invokeChaincode()` -- simulate data on-chain commit
  - `queryChaincode()` -- simulate chain data query
  - `commitReportToChain()` -- carbon report anchoring
  - `commitTradeToChain()` -- trade record anchoring
  - `queryBlock()` / `queryTransaction()` -- block/tx lookup
  - `verifySignature()` -- signature verification (always returns true in mock)
  - `checkConnection()` -- connection status
  - `listTransactions()` / `listLatestBlocks()` -- paginated mock data
- **Integration Points**: BlockchainController exposes REST endpoints at `/blockchain/**`
- **Note**: Production deployment requires replacing mock with actual Hyperledger Fabric SDK calls

---

## Authentication & Authorization

### JWT (Self-Managed)

- **Library**: `io.jsonwebtoken:jjwt:0.12.5`
- **Signing**: HMAC with configurable secret (`JWT_SECRET` env var)
- **Access Token TTL**: 1 hour (3,600,000 ms)
- **Refresh Token TTL**: 7 days (604,800,000 ms)
- **Token Flow**:
  1. User authenticates via `/auth/login` (username + password + captcha)
  2. Server returns access + refresh tokens
  3. Frontend stores tokens in sessionStorage
  4. Axios interceptor auto-attaches `Bearer` token
  5. On 401, frontend attempts refresh via `/auth/refresh` with `Refresh-Token` header
  6. Concurrent requests queue during refresh (single-flight pattern)
- **Password Hashing**: BCrypt
- **Roles**: ENTERPRISE, REVIEWER, AUTHENTICATOR, THIRD_PARTY, ADMIN
- **Method Security**: `@PreAuthorize` on controller methods
- **Session Policy**: Stateless (no server-side sessions)

### CSRF

- **Status**: Disabled (JWT in sessionStorage, not cookies, so CSRF not applicable)

### CORS

- **Allowed Origins**: Configurable via `CORS_ALLOWED_ORIGINS` (default: `http://localhost:5173`)
- **Allowed Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Allowed Headers**: Authorization, Content-Type, Refresh-Token
- **Credentials**: Enabled
- **Max Age**: 3600 seconds

---

## Captcha

### Image Captcha (Self-Generated)

- **Service**: `CaptchaService` -- in-memory `ConcurrentHashMap` storage
- **Dimensions**: 120x40 px, 4-char alphanumeric code
- **Expiry**: 300 seconds (5 minutes)
- **Endpoints**: `/captcha/**` (public)
- **Features**: Distortion lines, noise dots, random colors

### SMS / Email Verification Codes

- **Status**: Stub implementation (logs codes, no actual delivery)
- **Storage**: In-memory `ConcurrentHashMap`
- **Code Format**: 6-digit numeric
- **Note**: Production requires integration with SMS gateway and email service

---

## Digital Signatures (RSA)

### Self-Managed RSA Key Pairs

- **Algorithm**: SHA256withRSA
- **Key Storage**: `rsa_key_pair` table in MySQL (public + private keys in Base64)
- **Key Lifecycle**: 2-year expiry, versioned, three states (active, revoked, expired)
- **Capabilities**:
  - Key pair generation per user
  - Report data signing
  - Signature verification
  - RSA encryption/decryption for reviewer-enterprise data exchange
- **Integration**: Used in carbon report audit workflow

---

## Monitoring & Observability

### Prometheus (Metrics Export)

- **Endpoint**: `/actuator/prometheus` (public, no auth required)
- **Library**: `micrometer-registry-prometheus`
- **Metrics**:
  - HTTP request histograms (p50, p95, p99)
  - JVM metrics
  - Database connection pool metrics
  - Custom application metrics via `MetricsService`
- **Health Probes**: Liveness and readiness at `/actuator/health`

### ELK-Compatible Logging

- **Format**: JSON structured logs via `logstash-logback-encoder:7.4`
- **Fields**: timestamp, level, logger, thread, message
- **Note**: No ELK stack deployed in Docker Compose; logs are stdout for container collection

---

## API Documentation

### SpringDoc OpenAPI 2.5

- **Swagger UI**: `/swagger-ui.html`
- **API Docs**: `/v1/api-docs`
- **Profiles**: Enabled in dev/test, disabled in docker (production)

---

## Frontend API Integration

### REST API Client

- **Library**: Axios 1.7.0
- **Base URL**: `VITE_API_BASE_URL` environment variable
- **Timeout**: 15 seconds
- **Retry**: 2 attempts with exponential backoff (1s base delay) for network errors
- **Pagination Transform**: Frontend sends `pageNum`/`pageSize`, interceptor converts to `page`/`size`; response transforms Spring Data `Page` to `{ items, total, page, size, totalPages }`
- **Error Handling**: Global `ElMessage` notifications for 401, 403, 404, and generic errors

### API Modules (16 modules)

| Module | Backend Controller | Purpose |
|--------|--------------------|---------|
| auth.ts | AuthController | Login, register, refresh, captcha |
| user.ts | UserController | User profile management |
| admin.ts | AdminController | System admin operations |
| carbon.ts | CarbonController | Carbon report CRUD |
| trade.ts | TradeController | Carbon credit trading |
| auction.ts | (DoubleAuctionController) | Double auction orders & matching |
| emission.ts | EmissionController | Emission ratings |
| credit.ts | CreditScoreController | Credit score management |
| carbonCoin.ts | CarbonCoinController | Carbon coin accounts & transactions |
| carbonNeutral.ts | CarbonNeutralProjectController | Carbon neutral projects |
| signature.ts | DigitalSignatureController | RSA key management & signing |
| blockchain.ts | BlockchainController | Chain status, blocks, transactions |
| file.ts | FileController | File upload/download via MinIO |
| search.ts | SearchController | Cross-entity search |
| thirdParty.ts | ThirdPartyController | Third-party org management |
| captcha.ts | CaptchaController | Captcha generation & verification |

---

## External Services (Not Yet Integrated)

| Service | Status | Notes |
|---------|--------|-------|
| Hyperledger Fabric | Mock only | BlockchainService returns mock data |
| SMS Gateway | Stub only | CaptchaService logs codes |
| Email Service | Stub only | CaptchaService logs codes |
| ELK Stack | Logging only | JSON logs emitted but no deployed stack |
| Prometheus Server | Endpoint ready | `/actuator/prometheus` exposed |

---

## Webhooks

- **None**: The application does not currently expose or consume any webhook endpoints.

---

## Third-Party Libraries / SDKs

| Library | Version | Integration Type |
|---------|---------|-----------------|
| MinIO Java SDK | 8.5.7 | Object storage |
| JJWT | 0.12.5 | JWT operations |
| MapStruct | 1.5.5.Final | DTO mapping |
| Lettuce | (Spring managed) | Redis client |
| HikariCP | (Spring managed) | Connection pooling |

---

## Database Schema (21 Tables)

### Core Domain

| Table | Purpose |
|-------|---------|
| `user` | User accounts (all roles) |
| `user_type_list` | Role type definitions |
| `enterprise` | Enterprise profiles with carbon quota tracking |
| `reviewer` | Carbon audit reviewer profiles |
| `reviewer_qualification` | Reviewer certifications |
| `authenticator` | Certification body profiles |
| `third_party_org` | Third-party organization profiles |

### Carbon Accounting

| Table | Purpose |
|-------|---------|
| `carbon_report` | Carbon emission reports (with blockchain tx hash) |
| `emission_rating` | Enterprise emission ratings by year |
| `carbon_neutral_project` | Carbon offset / neutral projects |

### Trading

| Table | Purpose |
|-------|---------|
| `transaction` | Carbon credit trade records (with blockchain tx hash) |
| `auction_order` | Double auction bid/ask orders |
| `matching_result` | Auction matching / settlement results |

### Carbon Coin (Virtual Currency)

| Table | Purpose |
|-------|---------|
| `carbon_coin_account` | User carbon coin balances |
| `carbon_coin_transaction` | Carbon coin transaction ledger |

### Trust & Security

| Table | Purpose |
|-------|---------|
| `rsa_key_pair` | RSA key pairs for digital signatures |
| `credit_score` | Enterprise credit scores |
| `credit_event` | Credit score change audit trail |
| `account_permission_list` | Permission definitions |
| `entry_permission` | API access control per role |
| `operation_log` | Audit trail for all operations |
