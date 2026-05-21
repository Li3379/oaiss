<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.service

Business logic layer -- the core of the application. All services are `@Service` Spring beans, typically `@Transactional`, calling repositories for data access and other services for cross-domain logic. Controllers must never access repositories directly; they go through services.

## Key Files

| File | Purpose |
|------|---------|
| `AuthService.java` | Authentication, registration, JWT token management |
| `UserService.java` | User CRUD, profile management |
| `EnterpriseService.java` | Enterprise data management |
| `EnterpriseAdmissionService.java` | Enterprise admission/approval workflow |
| `CarbonService.java` | Carbon report CRUD and calculations (GB/T 32150-2015) |
| `CarbonPredictionService.java` | AI-based carbon emission prediction (delegates to `ml/`) |
| `CarbonCoinService.java` | Carbon coin account management and transfers |
| `CarbonNeutralProjectService.java` | Carbon neutral project lifecycle |
| `CreditScoreService.java` | Credit score calculation and scoring events |
| `EmissionRatingService.java` | Emission rating management |
| `DoubleAuctionService.java` | Double auction matching engine |
| `TradeService.java` | P2P carbon trading |
| `BlockchainServicePort.java` | Blockchain service interface (port/abstraction) |
| `FabricBlockchainService.java` | Hyperledger Fabric blockchain implementation |
| `MockBlockchainService.java` | Mock blockchain for dev/test environments |
| `DigitalSignatureService.java` | RSA digital signature sign/verify operations |
| `MinioService.java` | File upload/download via MinIO object storage |
| `SearchService.java` | Full-text search across entities |
| `ReviewerService.java` | Review workflow for carbon reports |
| `ReviewerQualificationService.java` | Reviewer qualification management |
| `ThirdPartyService.java` | Third-party monitoring data and oversight |
| `CaptchaService.java` | CAPTCHA generation and verification |
| `CaptchaVerifyResult.java` | CAPTCHA verification result DTO (companion to CaptchaService) |
| `RedisLockService.java` | Distributed lock management via Redis |
| `CachePreloadService.java` | Cache warming/preloading on application startup |
| `MetricsService.java` | Application metrics collection and reporting |
| `PowerGenerationFormulaService.java` | Power generation carbon formula calculations |
| `PowerGridFormulaService.java` | Power grid carbon formula calculations |

## Subdirectories

| Directory | Files | Purpose |
|-----------|-------|---------|
| `ml/` | `EnterpriseInferenceService.java`, `MarketPredictionService.java`, `MlServiceClient.java` | ML model services: enterprise inference, market prediction, and HTTP client for external ML service calls |

## For AI Agents

- Services are `@Service` beans with `@Transactional`. Call repositories for data, other services for cross-domain logic. Never access repositories directly from controllers.
- Cross-cutting concerns are applied via AOP annotations on service methods: `@AuditLog`, `@RateLimit`, `@DataIsolation`, `@DistributedLock`, `@RequirePermission`.
- `BlockchainServicePort` is the abstraction; `FabricBlockchainService` (production) and `MockBlockchainService` (dev/test) are the two implementations, selected via Spring profiles.
- Services in `ml/` communicate with an external ML service via `MlServiceClient` (HTTP). The root-level `CarbonPredictionService` delegates to `ml/MarketPredictionService`.
- `CaptchaVerifyResult.java` is a value object returned by `CaptchaService`, not a standalone service.
- All entities extend `BaseEntity` (soft delete via `deleted` field). Service queries must use `...AndDeletedFalse` repository methods.

## Dependencies

- **Upstream**: `controller/` calls services; services must not depend on controllers.
- **Downstream**: `repository/` (data access), `entity/` (domain models), `dto/` (request/response objects), `enums/` (enumerations), `constant/` (error codes).
- **Cross-cutting**: `annotation/` + `aop/` (applied via annotations on service methods), `security/` (JWT utilities), `config/` (Spring configuration beans).
- **External**: MinIO (file storage), Redis (caching, locks), Hyperledger Fabric (blockchain), ML service (predictions/inference).
