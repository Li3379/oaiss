<!-- refreshed: 2026-05-18 -->
# Codebase Structure

**Analysis Date:** 2026-05-18

## Top-Level Layout

```
OAISS CHAIN/
├── oaiss-chain-backend/          # Spring Boot 3.2.5 backend (Java 17)
├── oaiss-chain-frontend/         # Vue 3.5 SPA frontend (TypeScript)
├── oaiss-chain-ml-service/       # Python FastAPI ML microservice
├── oaiss-chain-chaincode/        # Hyperledger Fabric Go chaincode
├── fabric-config/                # Fabric network config (crypto, scripts)
├── scripts/                      # Build/test/deployment shell scripts
├── docs/                         # Project documentation
├── docker-compose.yml            # Full stack orchestration
├── docker-compose.infra.yml      # Infrastructure only (MySQL, Redis, MinIO)
├── docker-compose.fabric.yml     # Fabric network services
├── pom.xml                       # Parent Maven POM
├── CLAUDE.md                     # AI coding assistant instructions
└── .env.example                  # Environment variable template
```

## Backend Structure

```
oaiss-chain-backend/
├── pom.xml                                                # Maven build (Spring Boot 3.2.5, Java 17)
├── Dockerfile                                             # Container build
└── src/
    ├── main/
    │   ├── java/com/oaiss/chain/
    │   │   ├── OaissChainApplication.java                  # @SpringBootApplication entry point
    │   │   │
    │   │   ├── controller/                                # REST endpoints (21 files)
    │   │   │   ├── AuthController.java                    # /auth -- login, register, refresh, captcha, check-ip
    │   │   │   ├── UserController.java                    # /user -- profile, password change
    │   │   │   ├── AdminController.java                   # /admin -- users, dashboard, admission, qualification
    │   │   │   ├── EnterpriseController.java              # /enterprise -- enterprise profile CRUD
    │   │   │   ├── ReviewerController.java                # /reviewer -- reviewer profile CRUD
    │   │   │   ├── CarbonController.java                  # /carbon -- report create/submit/review, formulas
    │   │   │   ├── CarbonNeutralProjectController.java    # /carbon-neutral -- project registration/certification
    │   │   │   ├── CarbonCoinController.java             # /carbon-coin -- coin account, recharge, transfer
    │   │   │   ├── CreditScoreController.java            # /credit -- enterprise credit scores
    │   │   │   ├── DoubleAuctionController.java          # /auction -- double auction bid/match/settle
    │   │   │   ├── TradeController.java                  # /trade -- P2P trade create/accept/cancel
    │   │   │   ├── EmissionController.java               # /emission -- emission rating, AI prediction
    │   │   │   ├── BlockchainController.java             # /blockchain -- on-chain queries, explorer
    │   │   │   ├── DigitalSignatureController.java        # /signature -- RSA key gen, sign, verify
    │   │   │   ├── FileController.java                   # /file -- MinIO upload/download
    │   │   │   ├── SearchController.java                 # /search -- enterprise/reports/projects search
    │   │   │   ├── CaptchaController.java                # /captcha -- CAPTCHA generation
    │   │   │   ├── ThirdPartyController.java             # /third-party -- monitoring dashboard data
    │   │   │   ├── MarketPredictionController.java       # /ai/market -- AI market trend/price/supply-demand
    │   │   │   └── EnterpriseInferenceController.java    # /predict/enterprise -- AI compliance inference
    │   │   │
    │   │   ├── service/                                   # Business logic (29 files)
    │   │   │   ├── AuthService.java                      # Auth, JWT, token management
    │   │   │   ├── UserService.java                      # User CRUD
    │   │   │   ├── EnterpriseService.java                 # Enterprise profile
    │   │   │   ├── ReviewerService.java                   # Reviewer profile
    │   │   │   ├── CarbonService.java                    # Carbon report lifecycle (DRAFT->PENDING->APPROVED/REJECTED)
    │   │   │   ├── CarbonPredictionService.java          # Emission AI prediction via ML
    │   │   │   ├── CarbonNeutralProjectService.java      # Project registration and certification
    │   │   │   ├── CarbonCoinService.java                # Carbon coin account operations
    │   │   │   ├── CreditScoreService.java               # Credit score calculation (A-E scale)
    │   │   │   ├── DoubleAuctionService.java             # Double auction matching algorithm
    │   │   │   ├── TradeService.java                     # P2P trade execution
    │   │   │   ├── EmissionRatingService.java            # Annual emission rating
    │   │   │   ├── DigitalSignatureService.java          # RSA sign/verify
    │   │   │   ├── MinioService.java                     # MinIO file operations
    │   │   │   ├── SearchService.java                    # Cross-entity search
    │   │   │   ├── CaptchaService.java                   # CAPTCHA generation/validation
    │   │   │   ├── ThirdPartyService.java                # Monitoring data aggregation
    │   │   │   ├── BlockchainServicePort.java            # Blockchain port interface
    │   │   │   ├── FabricBlockchainService.java          # Real Fabric SDK impl (@Profile("fabric"))
    │   │   │   ├── MockBlockchainService.java            # Mock impl (@Primary, default)
    │   │   │   ├── RedisLockService.java                 # Redis distributed lock ops
    │   │   │   ├── MetricsService.java                  # Prometheus metrics
    │   │   │   ├── CachePreloadService.java              # Scheduled cache warming
    │   │   │   ├── EnterpriseAdmissionService.java       # Enterprise admission certificate
    │   │   │   ├── ReviewerQualificationService.java     # Reviewer qualification certificate
    │   │   │   ├── PowerGridFormulaService.java         # Power grid emission formula
    │   │   │   ├── PowerGenerationFormulaService.java    # Power generation emission formula
    │   │   │   ├── CaptchaVerifyResult.java              # CAPTCHA verification result type
    │   │   │   └── ml/                                   # ML/AI integration subpackage (3 files)
    │   │   │       ├── MlServiceClient.java              # WebClient HTTP client to ML service
    │   │   │       ├── MarketPredictionService.java      # Market trend/price/supply-demand prediction
    │   │   │       └── EnterpriseInferenceService.java  # Enterprise compliance inference
    │   │   │
    │   │   ├── repository/                               # Data access (22 files)
    │   │   │   ├── UserRepository.java                   # findByUsernameAndDeletedFalse, etc.
    │   │   │   ├── CarbonReportRepository.java           # findByEnterpriseIdAndStatusAndDeletedFalse
    │   │   │   ├── CarbonCoinAccountRepository.java      # findByUserIdAndDeletedFalse
    │   │   │   ├── CarbonCoinTransactionRepository.java  # findByAccountIdAndDeletedFalse
    │   │   │   ├── CarbonNeutralProjectRepository.java   # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── CreditScoreRepository.java            # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── CreditEventRepository.java            # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── EmissionRatingRepository.java         # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── EnterpriseAdmissionRepository.java    # findByEnterpriseIdAndStatus
    │   │   │   ├── EnterpriseRepository.java             # findByUserIdAndDeletedFalse
    │   │   │   ├── EntryPermissionRepository.java        # findByApiPathAndHttpMethodAndDeletedFalse
    │   │   │   ├── AccountPermissionListRepository.java  # findByPermissionCodeAndDeletedFalse
    │   │   │   ├── ReviewerQualificationRepository.java # findByReviewerIdAndStatus
    │   │   │   ├── ReviewerRepository.java              # findByUserIdAndDeletedFalse
    │   │   │   ├── RsaKeyPairRepository.java             # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── ThirdPartyOrgRepository.java          # findByUserIdAndDeletedFalse
    │   │   │   ├── TransactionRepository.java            # findByBuyerIdOrSellerIdAndDeletedFalse
    │   │   │   ├── AuctionOrderRepository.java           # findByEnterpriseIdAndDeletedFalse
    │   │   │   ├── MatchingResultRepository.java         # findByAuctionIdAndDeletedFalse
    │   │   │   ├── OperationLogRepository.java           # save audit log entries
    │   │   │   ├── AuthenticatorRepository.java          # findByEnterpriseIdAndDeletedFalse
    │   │   │   └── UserTypeListRepository.java           # userType lookups
    │   │   │
    │   │   ├── entity/                                   # JPA entities (24 files)
    │   │   │   ├── BaseEntity.java                       # @MappedSuperclass: id, createdAt, updatedAt, deleted
    │   │   │   ├── User.java                             # Core user entity
    │   │   │   ├── Enterprise.java                       # Enterprise profile
    │   │   │   ├── CarbonReport.java                     # Carbon emission report
    │   │   │   ├── CarbonCoinAccount.java                # Carbon coin account
    │   │   │   ├── CarbonCoinTransaction.java            # Carbon coin transfer/recharge record
    │   │   │   ├── CarbonNeutralProject.java             # Carbon neutral project
    │   │   │   ├── CreditScore.java                      # Credit rating (A-E)
    │   │   │   ├── CreditEvent.java                      # Credit event log
    │   │   │   ├── EmissionRating.java                   # Annual emission rating
    │   │   │   ├── EnterpriseAdmission.java              # Admission certificate
    │   │   │   ├── Reviewer.java                         # Reviewer profile
    │   │   │   ├── ReviewerQualification.java            # Qualification certificate
    │   │   │   ├── Transaction.java                      # P2P trade transaction
    │   │   │   ├── AuctionOrder.java                     # Double auction order
    │   │   │   ├── MatchingResult.java                   # Auction matching result
    │   │   │   ├── ThirdPartyOrg.java                    # Third-party org profile
    │   │   │   ├── RsaKeyPair.java                       # RSA key pair for digital signature
    │   │   │   ├── OperationLog.java                     # Audit log record
    │   │   │   ├── Authenticator.java                    # Enterprise authenticator
    │   │   │   ├── EntryPermission.java                  # API path permission config
    │   │   │   ├── AccountPermissionList.java            # Permission code registry
    │   │   │   └── UserTypeList.java                     # User type lookup
    │   │   │
    │   │   ├── dto/                                      # Request/response DTOs (45 files)
    │   │   │   ├── ApiResponse.java                     # Unified response envelope <T>
    │   │   │   ├── PageRequest.java                      # 1-based pageNum -> 0-based Spring Pageable
    │   │   │   ├── PageResponse.java                     # Spring Page -> { list, total, pageNum, pageSize, pages }
    │   │   │   ├── LoginRequest.java / LoginResponse.java
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── PasswordChangeRequest.java
    │   │   │   ├── UserInfoResponse.java / UserProfileUpdateRequest.java
    │   │   │   ├── CarbonReportRequest.java / CarbonReportResponse.java
    │   │   │   ├── CarbonNeutralProjectRequest.java / CarbonNeutralProjectResponse.java
    │   │   │   ├── CarbonCoinAccountResponse.java
    │   │   │   ├── CarbonCoinRechargeRequest.java / CarbonCoinTransferRequest.java
    │   │   │   ├── CreditScoreResponse.java / CreditEventResponse.java / CreditDeductionRequest.java
    │   │   │   ├── EmissionRatingRequest.java
    │   │   │   ├── EmissionForecastRequest.java / EmissionForecastResponse.java
    │   │   │   ├── TradeRequest.java / TradeResponse.java
    │   │   │   ├── AuctionOrderRequest.java / AuctionOrderResponse.java
    │   │   │   ├── MatchingResultResponse.java
    │   │   │   ├── MarketForecastRequest.java / MarketForecastResponse.java
    │   │   │   ├── EnterpriseInferenceRequest.java / EnterpriseInferenceResponse.java
    │   │   │   ├── CarbonPredictionRequest.java / CarbonPredictionResponse.java
    │   │   │   ├── PowerGenerationCalculationRequest.java / PowerGenerationCalculationResponse.java
    │   │   │   ├── PowerGridCalculationRequest.java / PowerGridCalculationResponse.java
    │   │   │   ├── ReviewRequest.java
    │   │   │   ├── ProjectVerificationRequest.java
    │   │   │   ├── SearchResponse.java
    │   │   │   ├── RsaKeyPairResponse.java
    │   │   │   ├── SignatureResult.java / SignatureVerifyRequest.java
    │   │   │   └── CaptchaResponse.java / CaptchaSendRequest.java
    │   │   │
    │   │   ├── config/                                   # Spring config (17 files)
    │   │   │   ├── SecurityConfig.java                   # Filter chain, CORS, session policy, security headers
    │   │   │   ├── RedisConfig.java                      # Redis connection, serializers
    │   │   │   ├── MinioConfig.java                      # MinIO client bean
    │   │   │   ├── MlServiceConfig.java                  # WebClient builder for ML service URL
    │   │   │   ├── FabricGatewayConfig.java              # Fabric Gateway beans (@Profile("fabric"))
    │   │   │   ├── FabricProperties.java                 # Fabric connection properties (@ConfigurationProperties)
    │   │   │   ├── JpaAuditingConfiguration.java         # @EnableJpaAuditing
    │   │   │   ├── SwaggerConfig.java                    # SpringDoc OpenAPI setup
    │   │   │   ├── CacheConfig.java                      # Redis cache manager, cache names
    │   │   │   ├── I18nConfig.java                       # MessageSource for i18n
    │   │   │   ├── MetricsConfig.java                    # Micrometer registry
    │   │   │   ├── ApiVersionConfig.java                 # API versioning via properties
    │   │   │   ├── SecurityStartupValidator.java         # Validates security config on startup
    │   │   │   ├── DatabaseHealthIndicator.java          # Custom DB health check
    │   │   │   ├── RedisHealthIndicator.java             # Custom Redis health check
    │   │   │   └── MinioHealthIndicator.java             # Custom MinIO health check
    │   │   │
    │   │   ├── security/                                 # Auth components (6 files)
    │   │   │   ├── JwtAuthenticationFilter.java          # OncePerRequestFilter: token extraction, validation, path normalization
    │   │   │   ├── JwtTokenProvider.java                 # JWT create/parse/validate (HMAC-SHA256 via jjwt 0.12.5)
    │   │   │   ├── JwtUserDetails.java                   # UserDetails with userId, role, userType, enterpriseId (Lombok @Builder)
    │   │   │   ├── JwtAuthenticationEntryPoint.java     # 401 JSON response
    │   │   │   ├── JwtAccessDeniedHandler.java           # 403 JSON response
    │   │   │   └── EnterpriseContextHolder.java          # ThreadLocal for enterprise context in data isolation
    │   │   │
    │   │   ├── annotation/                               # Custom AOP annotations (5 files)
    │   │   │   ├── AuditLog.java                         # @AuditLog(module, action, description, recordParams, recordResult, sensitiveFields)
    │   │   │   ├── RateLimit.java                        # @RateLimit(key, limit, period, limitType, message)
    │   │   │   ├── RequirePermission.java                # @RequirePermission(value, adminOnly, enterpriseOnly, reviewerOnly, thirdPartyOnly, message)
    │   │   │   ├── DataIsolation.java                    # @DataIsolation(enabled, skipAdmin)
    │   │   │   └── DistributedLock.java                  # @DistributedLock(key, waitTime, expireTime, errorMessage)
    │   │   │
    │   │   ├── aop/                                      # AOP aspects (5 files, 1:1 with annotations)
    │   │   │   ├── AuditLogAspect.java                   # Persists OperationLog to DB, masks sensitive fields
    │   │   │   ├── RateLimitAspect.java                  # Redis Lua script rate limiter (INCR + EXPIRE)
    │   │   │   ├── DataIsolationAspect.java              # Sets EnterpriseContextHolder ThreadLocal
    │   │   │   ├── DistributedLockAspect.java            # Redis lock acquire/release with SpEL key resolution
    │   │   │   └── PermissionAspect.java                 # UserType + permission code + API path check
    │   │   │
    │   │   ├── exception/                                # Exception hierarchy (7 files)
    │   │   │   ├── BusinessException.java                # Base: code + message
    │   │   │   ├── AuthenticationException.java          # 401 errors (2xxx)
    │   │   │   ├── AuthorizationException.java           # 403 errors (2xxx)
    │   │   │   ├── CarbonException.java                  # Carbon module errors (3xxx)
    │   │   │   ├── TradeException.java                   # Trade module errors (4xxx)
    │   │   │   ├── BlockchainException.java              # Blockchain module errors (5xxx)
    │   │   │   └── GlobalExceptionHandler.java            # @RestControllerAdvice -- centralized handler
    │   │   │
    │   │   ├── enums/                                     # Domain enumerations (9 files)
    │   │   │   ├── UserTypeEnum.java                     # ENTERPRISE(1), REVIEWER(2), THIRD_PARTY(3), ADMIN(99)
    │   │   │   ├── ReportStatusEnum.java                  # DRAFT, PENDING, APPROVED, REJECTED
    │   │   │   ├── TradeTypeEnum.java                     # P2P, AUCTION
    │   │   │   ├── TradeStatusEnum.java                   # PENDING, COMPLETED, CANCELLED
    │   │   │   ├── CreditLevelEnum.java                   # A, B, C, D, E
    │   │   │   ├── AuctionOrderStatusEnum.java            # PENDING, MATCHED, SETTLED, CANCELLED
    │   │   │   ├── MatchingStatusEnum.java                # UNMATCHED, PARTIAL, FULL
    │   │   │   ├── QualificationStatusEnum.java           # VALID, REVOKED
    │   │   │   └── CreditEventTypeEnum.java               # EMISSION_REPORT, TRADE_SETTLED, etc.
    │   │   │
    │   │   ├── constant/                                  # Constants (2 files)
    │   │   │   ├── ErrorCode.java                         # Numeric codes: 1xxx-6xxx by module
    │   │   │   └── ErrorMessage.java                       # String message keys for i18n
    │   │   │
    │   │   └── util/                                      # Utilities (4 files)
    │   │       ├── RsaKeyUtil.java                         # RSA key pair generation, sign, verify, encrypt, decrypt
    │   │       ├── HashUtil.java                           # SHA-256 hashing
    │   │       ├── MessageUtils.java                      # i18n message resolution helper
    │   │       └── CommonUtils.java                        # General helpers
    │   │
    │   └── resources/
    │       ├── application.yml                            # Default profile (base config)
    │       ├── application-dev.yml                        # Dev overrides (local MySQL/Redis)
    │       ├── application-docker.yml                     # Docker Compose profile
    │       ├── application-local.yml                      # Local development profile
    │       ├── application-test.yml                       # Test profile (Testcontainers)
    │       ├── logback-spring.xml                          # Logging pattern + level config
    │       ├── i18n/                                       # Backend message bundles
    │       │   ├── messages_zh_CN.properties               # Chinese messages
    │       │   └── messages_en_US.properties               # English messages
    │       ├── db/
    │       │   └── migration/                              # Flyway migrations
    │       │       ├── V1__init_schema.sql                 # Core tables (21+ tables)
    │       │       ├── V2__seed_data.sql                   # Seed data (admin user, permissions)
    │       │       └── V4__enterprise_admission.sql        # Admission/qualification tables
    │       └── fabric/
    │           └── crypto/                                 # Fabric TLS certs (dev only)
    │
    └── test/
        ├── java/com/oaiss/chain/
        │   ├── OaissChainApplicationTests.java             # Context load smoke test
        │   ├── BaseIntegrationTest.java                    # Testcontainers base class
        │   ├── controller/                                # Controller unit tests (18 files)
        │   │   ├── AuthControllerTest.java
        │   │   ├── UserControllerTest.java
        │   │   ├── AdminControllerTest.java
        │   │   ├── AdminControllerAdmissionTest.java
        │   │   ├── CarbonControllerTest.java
        │   │   ├── CarbonNeutralProjectControllerTest.java
        │   │   ├── CarbonCoinControllerTest.java
        │   │   ├── CreditScoreControllerTest.java
        │   │   ├── DoubleAuctionControllerTest.java
        │   │   ├── TradeControllerTest.java
        │   │   ├── EmissionControllerTest.java
        │   │   ├── BlockchainControllerTest.java
        │   │   ├── DigitalSignatureControllerTest.java
        │   │   ├── FileControllerTest.java
        │   │   ├── SearchControllerTest.java
        │   │   ├── CaptchaControllerTest.java
        │   │   ├── ThirdPartyControllerTest.java
        │   │   ├── MarketPredictionControllerTest.java
        │   │   ├── EnterpriseInferenceControllerTest.java
        │   │   └── ReviewerControllerTest.java (inferred)
        │   ├── service/                                   # Service unit tests (18 files)
        │   │   ├── UserServiceTest.java
        │   │   ├── EnterpriseAdmissionServiceTest.java
        │   │   ├── FabricBlockchainServiceTest.java
        │   │   ├── FabricProfileIntegrationTest.java
        │   │   ├── MockBlockchainServiceTest.java
        │   │   ├── MinioServiceTest.java
        │   │   ├── MetricsServiceTest.java
        │   │   ├── TradeServiceTest.java
        │   │   ├── ThirdPartyServiceTest.java
        │   │   ├── SearchServiceTest.java
        │   │   ├── RedisLockServiceTest.java
        │   │   ├── ReviewerQualificationServiceTest.java
        │   │   ├── PowerGenerationFormulaServiceTest.java
        │   │   ├── PowerGridFormulaServiceTest.java
        │   │   ├── HashUtilTest.java
        │   │   └── ml/                                    # ML integration tests (3 files)
        │   │       ├── EnterpriseInferenceServiceTest.java
        │   │       ├── MarketPredictionServiceTest.java
        │   │       └── MlServiceClientContractTest.java
        │   ├── repository/                                # Repository tests
        │   ├── aop/                                       # AOP aspect tests (5 files)
        │   │   ├── AuditLogAspectTest.java
        │   │   ├── DataIsolationAspectTest.java
        │   │   ├── DistributedLockAspectTest.java
        │   │   ├── PermissionAspectTest.java
        │   │   └── RateLimitAspectTest.java
        │   ├── security/                                  # Security tests
        │   ├── config/                                    # Config tests (3 files)
        │   │   ├── DatabaseHealthIndicatorTest.java
        │   │   ├── RedisHealthIndicatorTest.java
        │   │   └── SecurityStartupValidatorTest.java
        │   ├── dto/                                       # DTO tests (3 files)
        │   │   ├── PageRequestTest.java
        │   │   ├── PageResponseTest.java
        │   │   └── CarbonNeutralProjectResponseTest.java
        │   ├── entity/                                    # Entity tests
        │   │   └── AuthenticatorTest.java
        │   ├── exception/                                 # Exception tests (6 files)
        │   │   ├── GlobalExceptionHandlerTest.java
        │   │   ├── AuthenticationExceptionTest.java
        │   │   ├── AuthorizationExceptionTest.java
        │   │   ├── BlockchainExceptionTest.java
        │   │   ├── CarbonExceptionTest.java
        │   │   └── TradeExceptionTest.java
        │   ├── integration/                               # Integration tests
        │   │   └── UserIntegrationTest.java
        │   └── util/                                      # Utility tests (2 files)
        │       ├── CommonUtilsTest.java
        │       └── RsaKeyUtilTest.java
        ├── resources/
        │   └── application-test.yml                       # Test-specific Spring config
        └── e2e/                                           # E2E test scripts
            └── full_business_flow.py                      # Python E2E business flow test
```

### Backend File Counts

| Directory | Count | Description |
|-----------|-------|-------------|
| controller/ | 21 | REST endpoint classes |
| service/ | 26 + 3 (ml/) | Business logic classes |
| repository/ | 22 | JPA repository interfaces |
| entity/ | 24 | JPA entity classes (includes BaseEntity) |
| dto/ | 45 | Request/response DTOs |
| config/ | 17 | Spring configuration classes |
| security/ | 6 | JWT auth components |
| annotation/ | 5 | Custom AOP annotations |
| aop/ | 5 | AOP aspect implementations |
| exception/ | 7 | Exception classes + global handler |
| enums/ | 9 | Domain enumerations |
| constant/ | 2 | Error codes and messages |
| util/ | 4 | Utility classes |
| **Total source** | **~195** | Java source files |

## Frontend Structure

```
oaiss-chain-frontend/
├── package.json                                          # Vite 8, Vue 3.5, TypeScript
├── vite.config.ts                                        # Dev server, API proxy
├── tsconfig.json                                         # TypeScript config
├── Dockerfile                                            # Nginx-based container build
└── src/
    ├── main.ts                                           # App entry: Vue create + plugins
    ├── App.vue                                           # Root component
    │
    ├── router/
    │   └── index.ts                                      # Vue Router with role-based guards
    │                                                      #   beforeEach: public/loggedIn/role check
    │                                                      #   28 routes organized by role
    │
    ├── store/
    │   └── index.ts                                      # Pinia store (single 'app' store)
    │                                                      #   State: loggedIn, role, username, userId, enterpriseId
    │                                                      #   Getters: roleLabel, homePath
    │                                                      #   Actions: login, logout, toggleSidebar
    │                                                      #   JWT claims parsed on init for session recovery
    │
    ├── api/                                              # Axios API clients (23 files)
    │   ├── request.ts                                    # Central HTTP client
    │   │                                                  #   Request: pageNum->page conversion, JWT injection, token refresh
    │   │                                                  #   Response: code check, Spring Page->TransformedPage, error handling
    │   ├── auth.ts                                       # Login, register, refresh, captcha
    │   ├── admin.ts                                      # User management, admission, qualification
    │   ├── carbon.ts                                     # Carbon report CRUD
    │   ├── carbonNeutral.ts                              # Project registration/certification
    │   ├── carbonCoin.ts                                 # Coin account, recharge, transfer
    │   ├── carbonFormula.ts                              # Emission formula calculators
    │   ├── credit.ts                                     # Credit score queries
    │   ├── trade.ts                                      # P2P trade operations
    │   ├── auction.ts                                    # Double auction bid/match/settle
    │   ├── emission.ts                                   # Emission rating, AI prediction
    │   ├── blockchain.ts                                 # On-chain queries, explorer
    │   ├── signature.ts                                  # RSA key gen, sign, verify
    │   ├── file.ts                                       # MinIO upload/download
    │   ├── search.ts                                     # Cross-entity search
    │   ├── captcha.ts                                    # CAPTCHA image retrieval
    │   ├── thirdParty.ts                                 # Monitoring dashboard data
    │   ├── marketPrediction.ts                            # AI market prediction endpoints
    │   ├── enterpriseInference.ts                         # AI enterprise inference endpoint
    │   └── user.ts                                       # Profile, password change
    │
    ├── views/                                            # Page components (role-organized)
    │   ├── Login.vue                                     # Login form with CAPTCHA
    │   ├── OfficialHome.vue                              # Public landing page
    │   ├── NotFound.vue                                  # 404 page
    │   ├── enterprise/                                   # ENTERPRISE role pages (16 views)
    │   │   ├── CarbonUpload.vue                          # Report upload + submission
    │   │   ├── OrdersManage.vue                          # P2P order management
    │   │   ├── TradingMarket.vue                         # Double auction market
    │   │   ├── TradingP2P.vue                            # P2P trading
    │   │   ├── CompanyDashboard.vue                      # ECharts data visualization
    │   │   ├── CreditScore.vue                           # Credit score display
    │   │   ├── CarbonCoin.vue                            # Carbon coin account
    │   │   ├── Blockchain.vue                            # Blockchain explorer
    │   │   ├── CarbonNeutral.vue                         # Project list
    │   │   ├── CarbonNeutralDetail.vue                   # Project detail (dynamic route :id)
    │   │   ├── EmissionData.vue                          # Emission rating + AI prediction
    │   │   ├── MarketPrediction.vue                      # AI market trend/price/SD prediction
    │   │   ├── EnterpriseInference.vue                   # AI enterprise compliance inference
    │   │   ├── CarbonFormulaCalculator.vue               # Emission formula calculator
    │   │   ├── EnterpriseInfo.vue                        # Enterprise information page
    │   │   └── UserProfile.vue                           # Profile settings
    │   ├── auditor/                                      # REVIEWER role pages (3 views)
    │   │   ├── AuditList.vue                             # Report review queue
    │   │   ├── ReviewHistory.vue                         # Review history
    │   │   └── ProjectReview.vue                         # Project review list
    │   ├── admin/                                        # ADMIN role pages (6 views)
    │   │   ├── SystemUsers.vue                           # User CRUD
    │   │   ├── SystemCarbon.vue                          # Carbon report admin
    │   │   ├── SystemConfig.vue                          # System configuration
    │   │   ├── DataStatistics.vue                        # Dashboard statistics
    │   │   ├── VerifyList.vue                            # Certification management
    │   │   └── CertificateManage.vue                     # Admission/qualification certificates
    │   ├── third-party/                                  # THIRD_PARTY role pages (1 view)
    │   │   └── Monitor.vue                               # Regulatory monitoring dashboard
    │   └── __tests__/                                    # Vitest unit tests (22 files)
    │       ├── Login.test.ts
    │       ├── OfficialHome.test.ts
    │       ├── NotFound.test.ts
    │       ├── AuditList.test.ts
    │       ├── CarbonUpload.test.ts
    │       ├── CarbonCoin.test.ts
    │       ├── CarbonNeutral.test.ts
    │       ├── CarbonNeutralDetail.test.ts
    │       ├── Blockchain.test.ts
    │       ├── CompanyDashboard.test.ts
    │       ├── CreditScore.test.ts
    │       ├── DataStatistics.test.ts
    │       ├── EmissionData.test.ts
    │       ├── Monitor.test.ts
    │       ├── OrdersManage.test.ts
    │       ├── SystemCarbon.test.ts
    │       ├── SystemConfig.test.ts
    │       ├── SystemUsers.test.ts
    │       ├── TradingMarket.test.ts
    │       ├── TradingP2P.test.ts
    │       ├── UserProfile.test.ts
    │       └── VerifyList.test.ts
    │
    ├── types/                                            # TypeScript type definitions (17 files)
    │   ├── api.ts                                        # ApiResponse<T>, SpringPage<T>, TransformedPage<T>
    │   ├── auth.ts                                       # Login/register types
    │   ├── carbon.ts                                     # Carbon report types
    │   ├── auction.ts                                    # Auction types
    │   ├── carbon-neutral.ts                             # Project types
    │   ├── credit.ts                                     # Credit score types
    │   ├── emission.ts                                   # Emission types
    │   ├── ai.ts                                         # AI prediction types
    │   ├── carbon-coin.ts                                # Carbon coin types
    │   ├── carbonFormula.ts                              # Formula types
    │   ├── file.ts                                       # File upload types
    │   ├── search.ts                                     # Search types
    │   ├── signature.ts                                  # Digital signature types
    │   ├── third-party.ts                                # Monitoring types
    │   ├── trade.ts                                      # Trade types
    │   ├── user.ts                                       # User types
    │   ├── enums.ts                                      # Shared enum types
    │   └── index.ts                                      # Re-exports
    │
    ├── components/                                       # Shared components (3 files)
    │   ├── ErrorBoundary.vue                             # Error boundary wrapper
    │   ├── LanguageSwitcher.vue                          # i18n language toggle
    │   ├── PageContainer.vue                             # Common page layout wrapper
    │   └── __tests__/
    │       └── ErrorBoundary.test.ts
    │
    ├── layout/
    │   └── index.vue                                     # App shell: sidebar + header + content area
    │
    ├── config/
    │   ├── menu.ts                                       # ROLE constants, ROLE_HOME, MENU_BY_ROLE tree
    │   └── images.ts                                     # Static image references
    │
    ├── i18n/
    │   ├── index.ts                                      # vue-i18n setup (createI18n)
    │   └── locales/
    │       ├── zh-CN.ts                                  # Chinese translations
    │       └── en-US.ts                                  # English translations
    │
    ├── utils/
    │   ├── auth.ts                                       # Token storage (sessionStorage), JWT parsing, expiry check
    │   ├── echarts.ts                                    # ECharts lazy initialization helper
    │   ├── format.ts                                     # Number/date formatters
    │   └── __tests__/
    │       └── auth.test.ts
    │
    └── assets/                                           # Static assets
        ├── hero.png
        ├── vite.svg
        └── vue.svg
```

### Frontend File Counts

| Directory | Count | Description |
|-----------|-------|-------------|
| api/ | 23 | Axios API modules + request.ts |
| views/ | 27 | Page Vue components (3 top-level + 16 enterprise + 3 auditor + 6 admin + 1 third-party) |
| views/__tests__/ | 22 | Vitest unit test files |
| types/ | 18 | TypeScript type definition files |
| components/ | 3 | Shared reusable components |
| store/ | 1 | Single Pinia store |
| router/ | 1 | Single router file |
| config/ | 2 | Menu config + image config |
| utils/ | 3 | Utility modules |
| i18n/ | 3 | i18n setup + 2 locale files |
| layout/ | 1 | App layout shell |
| **Total source** | **~107** | TypeScript/Vue source files |

## ML Service Structure

```
oaiss-chain-ml-service/
├── requirements.txt                                      # Python deps (FastAPI, scikit-learn, prophet)
├── Dockerfile                                            # Container build
└── app/
    ├── __init__.py
    ├── main.py                                           # FastAPI app + lifespan + /health endpoint
    │                                                      #   Includes 3 routers: enterprise, market, emission
    ├── config.py                                         # Settings (model params, logging, env vars)
    │
    ├── routers/                                          # API routers (3 files)
    │   ├── __init__.py
    │   ├── market_router.py                              # /predict/market -- trend, price, supply-demand
    │   ├── enterprise_router.py                          # /api/v1/predict/enterprise -- compliance inference
    │   └── emission_router.py                            # /predict/emission -- emission forecasting
    │
    ├── schemas/                                          # Pydantic models (3 files)
    │   ├── __init__.py
    │   ├── market.py                                     # MarketForecastRequest/Response
    │   ├── enterprise.py                                 # EnterpriseInferenceRequest/Response
    │   └── emission.py                                   # EmissionForecastRequest/Response
    │
    ├── services/                                         # Business logic (3 files)
    │   ├── __init__.py
    │   ├── market_service.py                             # Prophet time-series models
    │   ├── enterprise_service.py                         # IsolationForest + XGBoost inference
    │   └── emission_service.py                           # Prophet emission forecasting
    │
    └── models/                                           # Trained model artifacts (mounted read-only)
        └── __init__.py
```

### ML Service File Counts

| Directory | Count | Description |
|-----------|-------|-------------|
| routers/ | 3 | FastAPI router modules |
| schemas/ | 3 | Pydantic request/response models |
| services/ | 3 | Prediction service implementations |
| models/ | 0 | Model artifact storage (mounted volume) |
| **Total source** | **~12** | Python source files |

## Blockchain (Chaincode) Structure

```
oaiss-chain-chaincode/
├── go.mod                                                # Go module (fabric-contract-api-go)
├── chaincode.go                                          # 7 smart contract functions
└── chaincode_test.go                                     # Go unit tests
```

Chaincode functions:
- `RecordCarbonReport(reportID, data)` -- store carbon report on ledger
- `VerifyReport(reportID)` -- check report existence and status
- `CreateTrade(tradeID, data)` -- record trade on ledger
- `SettleTrade(tradeID)` -- mark trade as settled
- `RecordCarbonCoinTx(txID, data)` -- record carbon coin transaction
- `UpdateCreditScore(enterpriseID, score)` -- update enterprise credit score
- `CreateAdmission(enterpriseID, data)` -- record admission certificate on-chain

## Fabric Network Config

```
fabric-config/
├── crypto-config.yaml                                    # Org and peer definitions
├── configtx.yaml                                         # Channel configuration
├── docker-compose.fabric.yml                             # Fabric network services
└── scripts/                                              # Network start/stop scripts
```

## Infrastructure & Config Files

```
project-root/
├── docker-compose.yml                                    # Full stack: MySQL, Redis, MinIO, backend, frontend, ml-service
├── docker-compose.infra.yml                              # Infrastructure only (MySQL, Redis, MinIO)
├── docker-compose.fabric.yml                             # Fabric network (in fabric-config/)
├── pom.xml                                               # Parent Maven POM (multi-module)
├── scripts/                                              # Build/test/deployment scripts
│   ├── helpers.sh                                        # Shared shell test utilities
│   ├── search-test.sh                                    # API integration test
│   ├── file-test.sh                                      # File upload/download test
│   ├── blockchain-test.sh                                # Blockchain API test
│   └── thirdparty-test.sh                                # Third-party monitoring test
└── .env.example                                          # Environment variable template
```

### Backend Configuration Files

| File | Purpose |
|------|---------|
| `oaiss-chain-backend/src/main/resources/application.yml` | Base config: server, datasource, JPA, Redis, JWT, MinIO, Flyway, Actuator, ML service, Resilience4j, Fabric |
| `oaiss-chain-backend/src/main/resources/application-dev.yml` | Dev overrides for local development |
| `oaiss-chain-backend/src/main/resources/application-local.yml` | Local alternative profile |
| `oaiss-chain-backend/src/main/resources/application-docker.yml` | Docker Compose profile (service hostnames) |
| `oaiss-chain-backend/src/main/resources/application-test.yml` | Test profile (Testcontainers) |
| `oaiss-chain-backend/src/main/resources/logback-spring.xml` | Logging pattern and levels |
| `oaiss-chain-backend/src/main/resources/i18n/messages_zh_CN.properties` | Chinese error messages |
| `oaiss-chain-backend/src/main/resources/i18n/messages_en_US.properties` | English error messages |

### Backend Flyway Migrations

| File | Purpose |
|------|---------|
| `db/migration/V1__init_schema.sql` | Core tables (21+ tables for all entities) |
| `db/migration/V2__seed_data.sql` | Seed data (admin user, permissions, initial config) |
| `db/migration/V4__enterprise_admission.sql` | Admission and qualification certificate tables |

### Frontend Configuration Files

| File | Purpose |
|------|---------|
| `oaiss-chain-frontend/vite.config.ts` | Vite dev server config, API proxy |
| `oaiss-chain-frontend/tsconfig.json` | TypeScript compiler config |
| `oaiss-chain-frontend/package.json` | Dependencies and scripts |

## Entry Points

| Entry Point | Location | Trigger |
|-------------|----------|---------|
| Spring Boot main | `oaiss-chain-backend/src/main/java/com/oaiss/chain/OaissChainApplication.java` | `mvn spring-boot:run` or Docker |
| Vue app bootstrap | `oaiss-chain-frontend/src/main.ts` | `npm run dev` (port 5173) or Nginx |
| FastAPI app | `oaiss-chain-ml-service/app/main.py` | `uvicorn app.main:app` (port 8001) |
| Go chaincode | `oaiss-chain-chaincode/chaincode.go` | Fabric peer install |
| Docker Compose stack | `docker-compose.yml` | `docker-compose up` |

## API Base Paths

All backend endpoints are prefixed with `/api/v1` (configured as `server.servlet.context-path` in `application.yml`):

| Controller | Path | Roles |
|-----------|------|-------|
| AuthController | `/api/v1/auth` | Public + authenticated |
| UserController | `/api/v1/user` | Authenticated |
| AdminController | `/api/v1/admin` | ADMIN |
| EnterpriseController | `/api/v1/enterprise` | ENTERPRISE |
| ReviewerController | `/api/v1/reviewer` | REVIEWER |
| CarbonController | `/api/v1/carbon` | ENTERPRISE (write), REVIEWER (review), all (read) |
| CarbonNeutralProjectController | `/api/v1/carbon-neutral` | ENTERPRISE |
| CarbonCoinController | `/api/v1/carbon-coin` | ENTERPRISE |
| CreditScoreController | `/api/v1/credit` | ENTERPRISE |
| DoubleAuctionController | `/api/v1/auction` | ENTERPRISE |
| TradeController | `/api/v1/trade` | ENTERPRISE |
| EmissionController | `/api/v1/emission` | ENTERPRISE |
| BlockchainController | `/api/v1/blockchain` | ENTERPRISE |
| DigitalSignatureController | `/api/v1/signature` | ENTERPRISE |
| FileController | `/api/v1/file` | Authenticated |
| SearchController | `/api/v1/search` | Authenticated |
| CaptchaController | `/api/v1/captcha` | Public |
| ThirdPartyController | `/api/v1/third-party` | THIRD_PARTY |
| MarketPredictionController | `/api/v1/ai/market` | ENTERPRISE, ADMIN |
| EnterpriseInferenceController | `/api/v1/predict/enterprise` | ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN |

## Naming Conventions

### Backend Java Files
- **Controllers**: PascalCase + `Controller` suffix (e.g., `CarbonController.java`)
- **Services**: PascalCase + `Service` suffix (e.g., `CarbonService.java`)
- **Repositories**: PascalCase + `Repository` suffix (e.g., `CarbonReportRepository.java`)
- **Entities**: PascalCase noun (e.g., `CarbonReport.java`)
- **DTOs**: PascalCase + `Request`/`Response` suffix (e.g., `CarbonReportRequest.java`, `CarbonReportResponse.java`)
- **Annotations**: PascalCase noun (e.g., `AuditLog.java`)
- **Aspects**: PascalCase + `Aspect` suffix (e.g., `AuditLogAspect.java`)
- **Enums**: PascalCase + `Enum` suffix (e.g., `ReportStatusEnum.java`)
- **Tests**: Match source file name + `Test` suffix (e.g., `CarbonControllerTest.java`)

### Frontend Files
- **Views**: PascalCase `.vue` files (e.g., `CarbonUpload.vue`)
- **API modules**: camelCase `.ts` files (e.g., `carbonCoin.ts`)
- **Type definitions**: camelCase `.ts` files (e.g., `carbon-coin.ts`)
- **Directories**: kebab-case for multi-word (e.g., `third-party/`)
- **Tests**: Match source file name + `.test.ts` suffix (e.g., `CarbonUpload.test.ts`)

### Flyway Migrations
- Pattern: `V{N}__{description}.sql` (double underscore, e.g., `V1__init_schema.sql`)

## Where to Add New Code

### New REST Endpoint
1. Controller: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/{Domain}Controller.java`
2. Service: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/{Domain}Service.java`
3. Repository: `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/{Entity}Repository.java`
4. Entity: `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/{Entity}.java` (extend `BaseEntity`)
5. DTOs: `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/{Entity}Request.java` + `{Entity}Response.java`
6. Migration: `oaiss-chain-backend/src/main/resources/db/migration/V{N}__{description}.sql`
7. Test: `oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/{Domain}ControllerTest.java`

### New Frontend Page
1. View: `oaiss-chain-frontend/src/views/{role}/{PageName}.vue` (role = enterprise, auditor, admin, third-party)
2. Route: Add entry in `oaiss-chain-frontend/src/router/index.ts` with `meta.roles: [ROLE.XXX]`
3. Menu: Add entry in `oaiss-chain-frontend/src/config/menu.ts` under `MENU_BY_ROLE[ROLE.XXX]`
4. API: Add calls in `oaiss-chain-frontend/src/api/{domain}.ts`
5. Types: Add definitions in `oaiss-chain-frontend/src/types/{domain}.ts`
6. i18n: Add keys in `oaiss-chain-frontend/src/i18n/locales/zh-CN.ts` and `en-US.ts`
7. Test: Add `oaiss-chain-frontend/src/views/__tests__/{PageName}.test.ts`

### New JPA Entity
1. Entity: `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/{Entity}.java` (extend `BaseEntity`)
2. Repository: `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/{Entity}Repository.java` (extend `JpaRepository`)
3. Migration: `oaiss-chain-backend/src/main/resources/db/migration/V{N}__{description}.sql`
4. All repository query methods must append `AndDeletedFalse`

### New Custom Annotation + Aspect
1. Annotation: `oaiss-chain-backend/src/main/java/com/oaiss/chain/annotation/{Name}.java`
2. Aspect: `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/{Name}Aspect.java`
3. Test: `oaiss-chain-backend/src/test/java/com/oaiss/chain/aop/{Name}AspectTest.java`

### New AI/ML Endpoint (Python)
1. Router: `oaiss-chain-ml-service/app/routers/{domain}_router.py`
2. Schema: `oaiss-chain-ml-service/app/schemas/{domain}.py` (Pydantic models)
3. Service: `oaiss-chain-ml-service/app/services/{domain}_service.py`
4. Register: Add `app.include_router()` in `oaiss-chain-ml-service/app/main.py`
5. Java client: Add method in `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/MlServiceClient.java`
6. Java service: Add in `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ml/{Domain}Service.java`
7. Controller: Add endpoint in existing or new controller

## Special Directories

**`oaiss-chain-backend/src/main/resources/fabric/crypto/`**:
- Purpose: Fabric TLS certificates and user credentials for blockchain connection
- Generated: Yes (by Fabric CA or cryptogen tool)
- Committed: Yes (dev/test certs only; production uses external secret management)

**`oaiss-chain-ml-service/app/models/`**:
- Purpose: Trained ML model artifacts
- Generated: Yes (by ML training pipeline)
- Committed: No (mounted as Docker volume, read-only)

**`oaiss-chain-frontend/src/views/__tests__/`**:
- Purpose: Co-located Vitest unit tests for all view components
- Pattern: One `.test.ts` file per `.vue` page component

**`oaiss-chain-backend/src/test/e2e/`**:
- Purpose: Python-based E2E business flow tests
- Runs against: Full Docker Compose stack

---

*Structure analysis: 2026-05-18*
