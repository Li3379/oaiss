# OAISS CHAIN - Project Structure

> Last updated: 2026-05-08

## Root Layout

```
OAISS CHAIN/
├── CLAUDE.md                    # Project conventions, tech stack, build commands
├── PLAN.md                      # Project plan
├── SKILL.md                     # Skill definitions
├── docker-compose.yml           # Full-stack Docker Compose (MySQL, Redis, MinIO, backend, frontend)
├── pom.xml                      # Root Maven POM (if multi-module)
├── scripts/                     # Operational scripts
├── docs/                        # Documentation
├── logs/                        # Application logs
├── tracks/                      # Track files
├── oaiss-chain-backend/         # Java/Spring Boot backend
├── oaiss-chain-frontend/        # Vue 3/TypeScript frontend
└── .planning/                   # Generated planning artifacts
    └── codebase/                # Architecture documentation (this file)
```

## Backend Structure

```
oaiss-chain-backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/oaiss/chain/
    │   │   ├── OaissChainApplication.java          # Spring Boot entry point
    │   │   │
    │   │   ├── controller/                          # REST API layer (16 controllers)
    │   │   │   ├── AuthController.java              # /auth - login, register, captcha, refresh
    │   │   │   ├── UserController.java              # /users - profile management
    │   │   │   ├── AdminController.java             # /admin - system administration
    │   │   │   ├── CarbonController.java            # /carbon - carbon reports
    │   │   │   ├── CarbonCoinController.java        # /carbon-coin - carbon coin accounts
    │   │   │   ├── CarbonNeutralProjectController.java  # /carbon-neutral - projects
    │   │   │   ├── CreditScoreController.java       # /credit - credit scoring
    │   │   │   ├── DoubleAuctionController.java     # /auction - double auction trading
    │   │   │   ├── TradeController.java             # /trade - P2P trading
    │   │   │   ├── EmissionController.java          # /emission - emission data & ratings
    │   │   │   ├── BlockchainController.java        # /blockchain - blockchain explorer
    │   │   │   ├── DigitalSignatureController.java  # /signature - RSA signatures
    │   │   │   ├── FileController.java              # /files - MinIO file operations
    │   │   │   ├── SearchController.java            # /search - cross-entity search
    │   │   │   ├── CaptchaController.java           # /captcha - CAPTCHA generation
    │   │   │   └── ThirdPartyController.java        # /third-party - monitoring
    │   │   │
    │   │   ├── service/                             # Business logic layer (19 services)
    │   │   │   ├── AuthService.java                 # Authentication & token management
    │   │   │   ├── UserService.java                 # User CRUD
    │   │   │   ├── CarbonService.java               # Carbon report lifecycle
    │   │   │   ├── CarbonCoinService.java           # Carbon coin operations
    │   │   │   ├── CarbonNeutralProjectService.java # Carbon neutral projects
    │   │   │   ├── CarbonPredictionService.java     # Carbon prediction analytics
    │   │   │   ├── CreditScoreService.java          # Credit score calculation
    │   │   │   ├── DoubleAuctionService.java        # Auction matching algorithm
    │   │   │   ├── TradeService.java                # P2P trade execution
    │   │   │   ├── EmissionRatingService.java       # Emission ratings
    │   │   │   ├── BlockchainService.java           # Blockchain interaction
    │   │   │   ├── DigitalSignatureService.java     # RSA digital signatures
    │   │   │   ├── SearchService.java               # Search aggregation
    │   │   │   ├── ThirdPartyService.java           # Third-party monitoring
    │   │   │   ├── CaptchaService.java              # CAPTCHA generation/validation
    │   │   │   ├── MinioService.java                # MinIO file operations
    │   │   │   ├── RedisLockService.java            # Distributed locking
    │   │   │   ├── CachePreloadService.java         # Cache warm-up on startup
    │   │   │   └── MetricsService.java              # Application metrics
    │   │   │
    │   │   ├── repository/                          # Data access layer (21 repositories)
    │   │   │   ├── UserRepository.java
    │   │   │   ├── EnterpriseRepository.java
    │   │   │   ├── ReviewerRepository.java
    │   │   │   ├── AuthenticatorRepository.java
    │   │   │   ├── ThirdPartyOrgRepository.java
    │   │   │   ├── CarbonReportRepository.java
    │   │   │   ├── CarbonCoinAccountRepository.java
    │   │   │   ├── CarbonCoinTransactionRepository.java
    │   │   │   ├── CarbonNeutralProjectRepository.java
    │   │   │   ├── CreditScoreRepository.java
    │   │   │   ├── CreditEventRepository.java
    │   │   │   ├── EmissionRatingRepository.java
    │   │   │   ├── AuctionOrderRepository.java
    │   │   │   ├── TransactionRepository.java
    │   │   │   ├── MatchingResultRepository.java
    │   │   │   ├── OperationLogRepository.java
    │   │   │   ├── EntryPermissionRepository.java
    │   │   │   ├── AccountPermissionListRepository.java
    │   │   │   ├── RsaKeyPairRepository.java
    │   │   │   ├── ReviewerQualificationRepository.java
    │   │   │   └── UserTypeListRepository.java
    │   │   │
    │   │   ├── entity/                              # JPA entities (22 entities)
    │   │   │   ├── BaseEntity.java                  # @MappedSuperclass: id, timestamps, soft delete
    │   │   │   ├── User.java                        # Core user entity
    │   │   │   ├── Enterprise.java                  # Enterprise profile
    │   │   │   ├── Reviewer.java                    # Reviewer profile
    │   │   │   ├── Authenticator.java               # Authenticator profile
    │   │   │   ├── ThirdPartyOrg.java               # Third-party organization
    │   │   │   ├── CarbonReport.java                # Carbon emission reports
    │   │   │   ├── CarbonCoinAccount.java           # Carbon coin accounts
    │   │   │   ├── CarbonCoinTransaction.java       # Carbon coin transactions
    │   │   │   ├── CarbonNeutralProject.java        # Carbon neutral projects
    │   │   │   ├── CreditScore.java                 # Credit scores
    │   │   │   ├── CreditEvent.java                 # Credit events
    │   │   │   ├── EmissionRating.java              # Emission ratings
    │   │   │   ├── AuctionOrder.java                # Auction orders
    │   │   │   ├── Transaction.java                 # Trade transactions
    │   │   │   ├── MatchingResult.java              # Auction matching results
    │   │   │   ├── OperationLog.java                # Audit log entries
    │   │   │   ├── EntryPermission.java             # Entry permissions
    │   │   │   ├── AccountPermissionList.java        # Account permissions
    │   │   │   ├── RsaKeyPair.java                  # RSA key pairs
    │   │   │   ├── ReviewerQualification.java        # Reviewer qualifications
    │   │   │   └── UserTypeList.java                # User type mappings
    │   │   │
    │   │   ├── dto/                                 # Request/response DTOs (35 DTOs)
    │   │   │   ├── ApiResponse.java                 # Unified response envelope {code, message, data, meta}
    │   │   │   ├── PageRequest.java                 # Pagination request
    │   │   │   ├── PageResponse.java                # Pagination response
    │   │   │   ├── LoginRequest.java / LoginResponse.java
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── PasswordChangeRequest.java
    │   │   │   ├── UserProfileUpdateRequest.java
    │   │   │   ├── UserInfoResponse.java
    │   │   │   ├── CaptchaResponse.java / CaptchaSendRequest.java
    │   │   │   ├── CarbonReportRequest.java / CarbonReportResponse.java
    │   │   │   ├── CarbonPredictionRequest.java / CarbonPredictionResponse.java
    │   │   │   ├── CarbonCoinAccountResponse.java
    │   │   │   ├── CarbonCoinRechargeRequest.java / CarbonCoinTransferRequest.java
    │   │   │   ├── CarbonNeutralProjectRequest.java / CarbonNeutralProjectResponse.java
    │   │   │   ├── CreditScoreResponse.java / CreditEventResponse.java / CreditDeductionRequest.java
    │   │   │   ├── EmissionRatingRequest.java
    │   │   │   ├── AuctionOrderRequest.java / AuctionOrderResponse.java
    │   │   │   ├── TradeRequest.java / TradeResponse.java
    │   │   │   ├── MatchingResultResponse.java
    │   │   │   ├── ReviewRequest.java / ProjectVerificationRequest.java
    │   │   │   ├── SearchResponse.java
    │   │   │   ├── SignatureResult.java / SignatureVerifyRequest.java
    │   │   │   └── RsaKeyPairResponse.java
    │   │   │
    │   │   ├── config/                              # Spring configuration (11 configs)
    │   │   │   ├── SecurityConfig.java              # Security filter chain, CORS, password encoder
    │   │   │   ├── SecurityStartupValidator.java    # Validates security config on startup
    │   │   │   ├── RedisConfig.java                 # Redis connection & serialization
    │   │   │   ├── CacheConfig.java                 # Cache manager configuration
    │   │   │   ├── MinioConfig.java                 # MinIO client configuration
    │   │   │   ├── SwaggerConfig.java               # SpringDoc OpenAPI configuration
    │   │   │   ├── ApiVersionConfig.java            # API versioning
    │   │   │   ├── MetricsConfig.java               # Micrometer/Prometheus metrics
    │   │   │   ├── JpaAuditingConfiguration.java    # JPA auditing (createdAt, updatedAt)
    │   │   │   ├── DatabaseHealthIndicator.java     # Custom DB health check
    │   │   │   └── RedisHealthIndicator.java        # Custom Redis health check
    │   │   │
    │   │   ├── security/                            # JWT security components (6 classes)
    │   │   │   ├── JwtAuthenticationFilter.java     # Servlet filter: extracts & validates JWT
    │   │   │   ├── JwtTokenProvider.java            # JWT creation & validation
    │   │   │   ├── JwtUserDetails.java              # Spring Security UserDetails implementation
    │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 handler
    │   │   │   ├── JwtAccessDeniedHandler.java      # 403 handler
    │   │   │   └── EnterpriseContextHolder.java     # Thread-local enterprise context
    │   │   │
    │   │   ├── annotation/                          # Custom annotations (5 annotations)
    │   │   │   ├── AuditLog.java                    # @AuditLog - operation logging
    │   │   │   ├── RateLimit.java                   # @RateLimit - request throttling
    │   │   │   ├── RequirePermission.java           # @RequirePermission - permission checks
    │   │   │   ├── DataIsolation.java               # @DataIsolation - tenant data scoping
    │   │   │   └── DistributedLock.java             # @DistributedLock - Redis locking
    │   │   │
    │   │   ├── aop/                                 # AOP aspects (5 aspects)
    │   │   │   ├── AuditLogAspect.java
    │   │   │   ├── RateLimitAspect.java
    │   │   │   ├── PermissionAspect.java
    │   │   │   ├── DataIsolationAspect.java
    │   │   │   └── DistributedLockAspect.java
    │   │   │
    │   │   ├── exception/                           # Exception handling (7 classes)
    │   │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice - maps all exceptions
    │   │   │   ├── BusinessException.java           # Base business exception
    │   │   │   ├── AuthenticationException.java     # Auth failures
    │   │   │   ├── AuthorizationException.java      # Permission failures
    │   │   │   ├── BlockchainException.java         # Blockchain errors
    │   │   │   ├── CarbonException.java             # Carbon domain errors
    │   │   │   └── TradeException.java              # Trade domain errors
    │   │   │
    │   │   ├── enums/                               # Enumerations (8 enums)
    │   │   │   ├── UserTypeEnum.java                # ENTERPRISE(1), REVIEWER(2), THIRD_PARTY(3), ADMIN(4), AUTHENTICATOR(5)
    │   │   │   ├── TradeTypeEnum.java               # Trade types
    │   │   │   ├── TradeStatusEnum.java             # Trade statuses
    │   │   │   ├── AuctionOrderStatusEnum.java      # Auction order statuses
    │   │   │   ├── MatchingStatusEnum.java          # Matching statuses
    │   │   │   ├── ReportStatusEnum.java            # Report statuses
    │   │   │   ├── CreditLevelEnum.java             # Credit levels
    │   │   │   └── CreditEventTypeEnum.java         # Credit event types
    │   │   │
    │   │   ├── constant/                            # Constants
    │   │   │   ├── ErrorCode.java                   # Numeric error codes
    │   │   │   └── ErrorMessage.java                # Error message strings
    │   │   │
    │   │   └── util/                                # Utilities (3 classes)
    │   │       ├── CommonUtils.java                 # General utilities
    │   │       ├── HashUtil.java                    # Hashing utilities
    │   │       └── RsaKeyUtil.java                  # RSA key generation
    │   │
    │   └── resources/
    │       ├── application.yml                      # Default profile
    │       ├── application-dev.yml                  # Development profile
    │       ├── application-test.yml                 # Test profile
    │       ├── application-docker.yml               # Docker profile
    │       ├── logback-spring.xml                   # Logging configuration
    │       └── db/
    │           ├── migration/
    │           │   ├── V1__init_schema.sql           # Database schema
    │           │   └── V2__seed_data.sql             # Seed data
    │           └── data.sql                          # Additional data
    │
    └── test/                                        # Backend tests
```

## Frontend Structure

```
oaiss-chain-frontend/
├── package.json
├── package-lock.json
├── vite.config.ts
├── tsconfig.json
├── playwright.config.ts                              # Playwright E2E config
└── src/
    ├── main.ts                                       # Vue app entry point
    ├── App.vue                                       # Root component
    │
    ├── api/                                          # API client modules (17 files)
    │   ├── request.ts                                # Axios instance + interceptors (auth, pagination, retry, error handling)
    │   ├── auth.ts                                   # /auth endpoints
    │   ├── user.ts                                   # /users endpoints
    │   ├── admin.ts                                  # /admin endpoints
    │   ├── carbon.ts                                 # /carbon endpoints
    │   ├── carbonCoin.ts                             # /carbon-coin endpoints
    │   ├── carbonNeutral.ts                          # /carbon-neutral endpoints
    │   ├── credit.ts                                 # /credit endpoints
    │   ├── auction.ts                                # /auction endpoints
    │   ├── trade.ts                                  # /trade endpoints
    │   ├── emission.ts                               # /emission endpoints
    │   ├── blockchain.ts                             # /blockchain endpoints
    │   ├── signature.ts                              # /signature endpoints
    │   ├── file.ts                                   # /files endpoints
    │   ├── search.ts                                 # /search endpoints
    │   ├── captcha.ts                                # /captcha endpoints
    │   └── thirdParty.ts                             # /third-party endpoints
    │
    ├── types/                                        # TypeScript type definitions (16 files)
    │   ├── index.ts                                  # Re-exports all types
    │   ├── api.ts                                    # ApiResponse<T>, SpringPage<T>
    │   ├── auth.ts                                   # Login/Register types
    │   ├── user.ts                                   # User types
    │   ├── carbon.ts                                 # Carbon report types
    │   ├── carbon-coin.ts                            # Carbon coin types
    │   ├── carbon-neutral.ts                         # Carbon neutral project types
    │   ├── credit.ts                                 # Credit score types
    │   ├── auction.ts                                # Auction types
    │   ├── trade.ts                                  # Trade types
    │   ├── emission.ts                               # Emission types
    │   ├── signature.ts                              # Digital signature types
    │   ├── search.ts                                 # Search types
    │   ├── file.ts                                   # File types
    │   ├── third-party.ts                            # Third-party types
    │   └── enums.ts                                  # Frontend enums
    │
    ├── views/                                        # Page components (22 views)
    │   ├── Login.vue                                 # Login page (public)
    │   ├── OfficialHome.vue                          # Public landing page
    │   ├── NotFound.vue                              # 404 page
    │   ├── enterprise/                               # ENTERPRISE role views (11)
    │   │   ├── CarbonUpload.vue                      # Upload carbon reports for review
    │   │   ├── OrdersManage.vue                      # Order management
    │   │   ├── TradingMarket.vue                     # Double auction market
    │   │   ├── TradingP2P.vue                        # P2P trading
    │   │   ├── CompanyDashboard.vue                  # Data visualization dashboard
    │   │   ├── CreditScore.vue                       # Credit score view
    │   │   ├── CarbonCoin.vue                        # Carbon coin account
    │   │   ├── Blockchain.vue                        # Blockchain explorer
    │   │   ├── CarbonNeutral.vue                     # Carbon neutral projects list
    │   │   ├── CarbonNeutralDetail.vue               # Project detail (/:id)
    │   │   ├── EmissionData.vue                      # Emission data
    │   │   └── UserProfile.vue                       # User profile settings
    │   ├── admin/                                    # ADMIN role views (4)
    │   │   ├── SystemUsers.vue                       # User management
    │   │   ├── SystemCarbon.vue                      # Carbon accounting management
    │   │   ├── SystemConfig.vue                      # System configuration
    │   │   └── DataStatistics.vue                    # Data statistics
    │   ├── auditor/                                  # REVIEWER role views (1)
    │   │   └── AuditList.vue                         # Carbon emission audit list
    │   ├── authenticator/                            # AUTHENTICATOR role views (1)
    │   │   └── VerifyList.vue                        # Certification verification list
    │   └── third-party/                              # THIRD_PARTY role views (1)
    │       └── Monitor.vue                           # Monitoring dashboard
    │
    ├── router/
    │   └── index.ts                                  # Vue Router: routes + beforeEach guard
    │
    ├── store/
    │   └── index.ts                                  # Pinia store: auth state, role, home paths
    │
    ├── layout/
    │   └── index.vue                                 # App shell: sidebar + header + content area
    │
    ├── components/                                   # Shared components (3)
    │   ├── ErrorBoundary.vue                         # Error boundary wrapper
    │   ├── LanguageSwitcher.vue                      # i18n language toggle
    │   └── PageContainer.vue                         # Page layout container
    │
    ├── config/                                       # Configuration (2 files)
    │   ├── menu.ts                                   # Role-based menu definitions (MENU_BY_ROLE, ROLE_HOME)
    │   └── images.ts                                 # Image asset references
    │
    ├── i18n/                                         # Internationalization
    │   ├── zh-CN.json                                # Chinese translations
    │   └── en-US.json                                # English translations
    │
    ├── utils/                                        # Utilities (3 files)
    │   ├── auth.ts                                   # Token management (get/set/clear, expiry check)
    │   ├── echarts.ts                                # ECharts helper utilities
    │   └── __tests__/
    │       └── auth.test.ts                          # Auth utility unit tests
    │
    ├── assets/                                       # Static assets (images, fonts)
    │
    ├── views/__tests__/                              # View unit tests
    │
    └── tests/e2e/                                    # Playwright E2E tests
        ├── fixtures/                                 # Test fixtures
        │   ├── auth.ts
        │   ├── api-mock.ts
        │   ├── auth-monitor.ts
        │   ├── auth-resilient.ts
        │   └── test-data.ts
        ├── flows/                                    # E2E flow tests
        └── smoke/                                    # Smoke tests
```

## Naming Conventions

### Backend (Java)

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, singular | `com.oaiss.chain.controller` |
| Controller | `{Domain}Controller` | `CarbonController` |
| Service | `{Domain}Service` | `CarbonService` |
| Repository | `{Entity}Repository` | `CarbonReportRepository` |
| Entity | PascalCase, singular | `CarbonReport` |
| DTO | `{Purpose}{Request/Response}` | `CarbonReportRequest`, `LoginResponse` |
| Enum | `{Domain}Enum` | `UserTypeEnum`, `TradeStatusEnum` |
| Aspect | `{Concern}Aspect` | `AuditLogAspect` |
| Annotation | PascalCase | `@AuditLog`, `@RateLimit` |
| Exception | `{Domain}Exception` | `BusinessException`, `TradeException` |
| Config | `{Purpose}Config` | `SecurityConfig`, `RedisConfig` |

### Frontend (TypeScript/Vue)

| Element | Convention | Example |
|---|---|---|
| API module | camelCase, matches backend domain | `carbon.ts`, `auction.ts` |
| Type file | kebab-case, matches domain | `carbon-coin.ts`, `credit.ts` |
| View component | PascalCase, role-prefixed directory | `views/enterprise/CarbonUpload.vue` |
| Shared component | PascalCase | `PageContainer.vue` |
| Config file | camelCase | `menu.ts`, `images.ts` |
| Test file | `{name}.test.ts` | `auth.test.ts` |
| E2E spec | descriptive kebab-case | in `flows/` and `smoke/` |

### Database

| Element | Convention | Example |
|---|---|---|
| Table | snake_case, plural | `carbon_reports`, `users` |
| Column | snake_case | `created_at`, `is_deleted` |
| Migration | `V{number}__{description}.sql` | `V1__init_schema.sql` |
| Foreign key | `{referenced_table}_id` | `user_id`, `enterprise_id` |

## Role-to-View Mapping

| Role | Code | Home Path | Views |
|---|---|---|---|
| ENTERPRISE | 1 | `/enterprise/carbon/upload` | 11 views (CarbonUpload, TradingMarket, TradingP2P, OrdersManage, CompanyDashboard, CreditScore, CarbonCoin, Blockchain, CarbonNeutral, CarbonNeutralDetail, EmissionData, UserProfile) |
| REVIEWER | 2 | `/auditor/audit/list` | 1 view (AuditList) |
| AUTHENTICATOR | 5 | `/authenticator/verify/list` | 1 view (VerifyList) |
| THIRD_PARTY | 3 | `/third-party/monitor` | 1 view (Monitor) |
| ADMIN | 4 | `/admin/system/users` | 4 views (SystemUsers, SystemCarbon, SystemConfig, DataStatistics) |
