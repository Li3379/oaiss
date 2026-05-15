<!-- refreshed: 2026-05-15 -->
# Codebase Structure

**Analysis Date:** 2026-05-15

## Directory Layout

```
oaiss-chain-backend/
├── pom.xml                                            # Maven build config (Spring Boot 3.2.5, Java 17)
└── src/
    ├── main/
    │   ├── java/com/oaiss/chain/
    │   │   ├── OaissChainApplication.java             # Spring Boot entry point
    │   │   ├── controller/                            # REST API layer (20 controllers)
    │   │   ├── service/                               # Business logic layer (26 files)
    │   │   │   └── ml/                                # ML/AI prediction sublayer (3 files)
    │   │   ├── repository/                            # Data access layer (22 repositories)
    │   │   ├── entity/                                # JPA entities (23 entities)
    │   │   ├── dto/                                   # Request/response DTOs (46 files)
    │   │   ├── config/                                # Spring configuration (17 configs)
    │   │   ├── security/                              # JWT security components (7 classes)
    │   │   ├── annotation/                            # Custom annotations (5 annotations)
    │   │   ├── aop/                                   # AOP aspects (5 aspects)
    │   │   ├── exception/                             # Exception handling (7 classes)
    │   │   ├── enums/                                 # Enumerations (9 enums)
    │   │   ├── constant/                              # Constants (ErrorCode, ErrorMessage)
    │   │   └── util/                                  # Utilities (4 classes)
    │   └── resources/
    │       ├── application.yml                        # Default profile
    │       ├── application-dev.yml                    # Development profile
    │       ├── application-test.yml                   # Test profile (Testcontainers)
    │       ├── application-docker.yml                 # Docker Compose profile
    │       ├── logback-spring.xml                     # Logging configuration
    │       ├── i18n/                                  # Internationalization messages
    │       ├── db/
    │       │   └── migration/
    │       │       ├── V1__init_schema.sql            # Database schema (Flyway)
    │       │       └── V2__seed_data.sql              # Seed data
    │       └── fabric/
    │           └── crypto/                            # Hyperledger Fabric crypto material
    └── test/
        ├── java/com/oaiss/chain/
        │   ├── OaissChainApplicationTests.java        # Spring context load test
        │   ├── BaseIntegrationTest.java               # Integration test base class
        │   ├── controller/                            # Controller unit tests (16 files)
        │   ├── service/                               # Service unit tests (22 files)
        │   │   └── ml/                                # ML service tests (2 files)
        │   ├── repository/                            # Repository tests (8 files)
        │   ├── aop/                                   # AOP aspect tests (5 files)
        │   ├── security/                              # Security tests (4 files)
        │   ├── config/                                # Config tests (3 files)
        │   ├── dto/                                   # DTO tests (3 files)
        │   ├── entity/                                # Entity tests (1 file)
        │   ├── exception/                             # Exception tests (6 files)
        │   ├── integration/                           # Integration tests (1 file)
        │   └── util/                                  # Utility tests (2 files)
        ├── resources/                                 # Test resources
        └── e2e/                                       # E2E test placeholders
```

## Directory Purposes

**`controller/`:**
- Purpose: REST endpoint definitions, request validation, response wrapping
- Contains: 20 `@RestController` classes with Swagger/OpenAPI annotations
- Key files: `AuthController.java`, `CarbonController.java`, `AdminController.java`, `EnterpriseInferenceController.java`, `MarketPredictionController.java`

**`service/`:**
- Purpose: Business logic, transaction management, domain orchestration
- Contains: `@Service` classes, blockchain port interface and implementations, formula calculators
- Key files: `AuthService.java`, `CarbonService.java`, `BlockchainServicePort.java`, `FabricBlockchainService.java`, `MockBlockchainService.java`

**`service/ml/`:**
- Purpose: ML/AI prediction service integration
- Contains: Services that call external ML microservice via HTTP
- Key files: `EnterpriseInferenceService.java`, `MarketPredictionService.java`, `MlServiceClient.java`

**`repository/`:**
- Purpose: Spring Data JPA data access interfaces
- Contains: `JpaRepository` extensions with custom query methods
- Key files: `UserRepository.java`, `CarbonReportRepository.java`, `CarbonCoinAccountRepository.java`

**`entity/`:**
- Purpose: JPA entity definitions mapped to MySQL tables
- Contains: `@Entity` classes extending `BaseEntity`; soft delete via `deleted` field
- Key files: `BaseEntity.java`, `User.java`, `CarbonReport.java`, `Enterprise.java`, `CarbonCoinAccount.java`

**`dto/`:**
- Purpose: API request/response data transfer objects
- Contains: Request DTOs (`*Request`), response DTOs (`*Response`), pagination, envelope
- Key files: `ApiResponse.java`, `PageRequest.java`, `PageResponse.java`

**`config/`:**
- Purpose: Spring framework configuration beans
- Contains: Security, Redis, MinIO, JPA auditing, Swagger, CORS, health indicators, Fabric, i18n, metrics, ML service
- Key files: `SecurityConfig.java`, `RedisConfig.java`, `FabricGatewayConfig.java`, `MlServiceConfig.java`

**`security/`:**
- Purpose: JWT authentication and authorization components
- Contains: Filter, token provider, user details, entry points, enterprise context
- Key files: `JwtAuthenticationFilter.java`, `JwtTokenProvider.java`, `JwtUserDetails.java`

**`annotation/`:**
- Purpose: Custom annotations for AOP-based cross-cutting concerns
- Contains: 5 annotations consumed by corresponding aspects in `aop/`
- Key files: `@AuditLog`, `@RateLimit`, `@RequirePermission`, `@DataIsolation`, `@DistributedLock`

**`aop/`:**
- Purpose: Aspect implementations for cross-cutting concerns
- Contains: `@Aspect` components using `@Around` advice
- Key files: `AuditLogAspect.java`, `RateLimitAspect.java`, `PermissionAspect.java`, `DataIsolationAspect.java`, `DistributedLockAspect.java`

**`exception/`:**
- Purpose: Exception hierarchy and global exception handling
- Contains: Domain-specific exceptions and `@RestControllerAdvice` handler
- Key files: `GlobalExceptionHandler.java`, `BusinessException.java`, `AuthenticationException.java`

**`enums/`:**
- Purpose: Domain enumerations
- Contains: User types, trade types/statuses, report statuses, credit levels, auction statuses
- Key files: `UserTypeEnum.java`, `ReportStatusEnum.java`, `TradeStatusEnum.java`

**`constant/`:**
- Purpose: Application-wide constants
- Contains: Error code definitions (1xxx-6xxx by module), error message strings
- Key files: `ErrorCode.java`, `ErrorMessage.java`

**`util/`:**
- Purpose: Stateless utility classes
- Contains: RSA key generation, hashing, i18n message resolution, general utilities
- Key files: `RsaKeyUtil.java`, `HashUtil.java`, `MessageUtils.java`, `CommonUtils.java`

## Key File Locations

**Entry Points:**
- `src/main/java/com/oaiss/chain/OaissChainApplication.java`: Spring Boot main class
- `src/main/java/com/oaiss/chain/config/SecurityConfig.java`: Security filter chain configuration
- `src/main/resources/application.yml`: Default Spring profile configuration

**Configuration:**
- `pom.xml`: Maven dependencies (Spring Boot 3.2.5, JJWT 0.12.5, SpringDoc 2.5, etc.)
- `src/main/resources/application-dev.yml`: Dev profile (local MySQL/Redis)
- `src/main/resources/application-test.yml`: Test profile (Testcontainers)
- `src/main/resources/application-docker.yml`: Docker Compose profile
- `src/main/resources/logback-spring.xml`: Logging levels and appenders

**Core Logic:**
- `src/main/java/com/oaiss/chain/service/AuthService.java`: Login, register, token management
- `src/main/java/com/oaiss/chain/service/CarbonService.java`: Carbon report lifecycle
- `src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`: Auction matching algorithm
- `src/main/java/com/oaiss/chain/service/TradeService.java`: P2P trade execution
- `src/main/java/com/oaiss/chain/service/BlockchainServicePort.java`: Blockchain operations interface
- `src/main/java/com/oaiss/chain/service/FabricBlockchainService.java`: Real Fabric implementation
- `src/main/java/com/oaiss/chain/service/MockBlockchainService.java`: Dev/test mock implementation

**Security:**
- `src/main/java/com/oaiss/chain/security/JwtAuthenticationFilter.java`: JWT extraction, validation, whitelist
- `src/main/java/com/oaiss/chain/security/JwtTokenProvider.java`: JWT create/parse/validate
- `src/main/java/com/oaiss/chain/security/JwtUserDetails.java`: Spring Security principal

**Testing:**
- `src/test/java/com/oaiss/chain/BaseIntegrationTest.java`: Testcontainers base class
- `src/test/java/com/oaiss/chain/controller/*Test.java`: Controller tests (MockMvc)
- `src/test/java/com/oaiss/chain/service/*Test.java`: Service unit tests (mocked dependencies)

## Naming Conventions

**Files:**
- Controllers: `{Domain}Controller.java` (e.g., `CarbonController.java`)
- Services: `{Domain}Service.java` (e.g., `CarbonService.java`)
- Repositories: `{Entity}Repository.java` (e.g., `CarbonReportRepository.java`)
- Entities: PascalCase singular (e.g., `CarbonReport.java`)
- DTOs: `{Purpose}{Request|Response}.java` (e.g., `CarbonReportRequest.java`)
- Aspects: `{Concern}Aspect.java` (e.g., `AuditLogAspect.java`)
- Configs: `{Purpose}Config.java` (e.g., `SecurityConfig.java`)
- Enums: `{Domain}Enum.java` (e.g., `UserTypeEnum.java`)
- Tests: `{ClassName}Test.java` (co-located in `src/test/` mirroring main structure)

**Directories:**
- All lowercase, singular (e.g., `controller/`, `service/`, `repository/`)
- Sub-packages by concern (e.g., `service/ml/` for ML services)

## Controller-to-Service Mapping

| Controller | Base Path | Primary Service(s) | Role Access |
|-----------|-----------|---------------------|-------------|
| `AuthController` | `/auth` | `AuthService`, `CaptchaService` | Public + authenticated |
| `UserController` | `/users` | `UserService` | Authenticated |
| `AdminController` | `/admin` | `UserService` | ADMIN |
| `EnterpriseController` | `/enterprise` | `EnterpriseService` | ENTERPRISE |
| `ReviewerController` | `/reviewer` | `ReviewerService` | REVIEWER |
| `CarbonController` | `/carbon` | `CarbonService`, `PowerGridFormulaService`, `PowerGenerationFormulaService` | ENTERPRISE (create/submit), REVIEWER (review), all authenticated (read) |
| `CarbonCoinController` | `/carbon-coin` | `CarbonCoinService` | ENTERPRISE |
| `CarbonNeutralProjectController` | `/carbon-neutral` | `CarbonNeutralProjectService` | ENTERPRISE |
| `CreditScoreController` | `/credit` | `CreditScoreService` | ENTERPRISE |
| `DoubleAuctionController` | `/auction` | `DoubleAuctionService` | ENTERPRISE |
| `TradeController` | `/trade` | `TradeService` | ENTERPRISE |
| `EmissionController` | `/emission` | `EmissionRatingService`, `CarbonPredictionService` | ENTERPRISE |
| `BlockchainController` | `/blockchain` | `BlockchainServicePort` (Fabric or Mock) | ENTERPRISE |
| `DigitalSignatureController` | `/signature` | `DigitalSignatureService` | ENTERPRISE |
| `FileController` | `/files` | `MinioService` | Authenticated |
| `SearchController` | `/search` | `SearchService` | Authenticated |
| `CaptchaController` | `/captcha` | `CaptchaService` | Public |
| `ThirdPartyController` | `/third-party` | `ThirdPartyService` | THIRD_PARTY |
| `EnterpriseInferenceController` | `/predict/enterprise` | `EnterpriseInferenceService` | ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN |
| `MarketPredictionController` | `/api/v1/ai/market` | `MarketPredictionService` | ENTERPRISE, ADMIN |

## Entity Relationships

```
User (1) ──────── (1) Enterprise       # User -> Enterprise profile (via userType=1)
User (1) ──────── (1) Reviewer         # User -> Reviewer profile (via userType=2)
User (1) ──────── (1) Authenticator    # User -> Authenticator profile (via userType=3)
User (1) ──────── (N) CarbonCoinAccount # User -> Carbon coin accounts

Enterprise (1) ── (N) CarbonReport     # Enterprise submits reports
Enterprise (1) ── (1) CarbonCoinAccount # Enterprise has coin account
Enterprise (1) ── (N) CarbonNeutralProject # Enterprise owns projects
Enterprise (1) ── (N) EmissionRating   # Enterprise has emission ratings
Enterprise (1) ── (1) CreditScore      # Enterprise has credit score

CarbonReport (1) ── (N) CreditEvent    # Report generates credit events
CreditScore (1) ── (N) CreditEvent     # Score tracks events

AuctionOrder (N) ── (N) MatchingResult # Auction orders matched
Transaction (1) ── (1) CarbonCoinTransaction # Trade -> coin transfer

User (1) ──────── (N) OperationLog     # User operations logged
User (1) ──────── (1) RsaKeyPair       # User RSA keys for signing
```

**Note:** Entities use foreign key IDs (e.g., `enterpriseId` Long field) rather than JPA `@ManyToOne` relationships in most cases. This is a deliberate choice for simplicity and performance -- no lazy loading issues, no N+1 queries from relationship traversal.

## Where to Add New Code

**New REST Endpoint:**
1. Controller: Create `controller/{Domain}Controller.java` with `@RestController`, `@RequestMapping("/{domain}")`
2. Service: Create `service/{Domain}Service.java` with `@Service`, `@Transactional`
3. Repository: Create `repository/{Entity}Repository.java` extending `JpaRepository`
4. Entity: Create `entity/{Entity}.java` extending `BaseEntity`
5. DTOs: Create `dto/{Entity}Request.java` and `dto/{Entity}Response.java`
6. Add `@PreAuthorize` for role-based access
7. Add `@AuditLog` for operations that need audit trails

**New Frontend Page:**
1. View: Create `views/{role}/{PageName}.vue` in the appropriate role directory
2. Route: Add route in `router/index.ts` with `meta.roles`
3. Menu: Add menu entry in `config/menu.ts` under the appropriate role
4. API: Add endpoints in `api/{domain}.ts`
5. Types: Add TypeScript types in `types/{domain}.ts`

**New JPA Entity:**
1. Entity: Create `entity/{Entity}.java` extending `BaseEntity`
2. Repository: Create `repository/{Entity}Repository.java` extending `JpaRepository`
3. Migration: Add Flyway migration in `db/migration/V{N}__{description}.sql`
4. Soft delete: All queries must filter by `deleted = false`

**New Custom Annotation + Aspect:**
1. Annotation: Create `annotation/{Name}.java` with `@Target(ElementType.METHOD)`, `@Retention(RetentionPolicy.RUNTIME)`
2. Aspect: Create `aop/{Name}Aspect.java` with `@Aspect`, `@Component`, `@Around` advice
3. Apply annotation to target controller/service methods

**New Configuration:**
- Create `config/{Purpose}Config.java` with `@Configuration`
- Use `@Profile` for environment-specific beans (e.g., `@Profile("fabric")`)
- Add corresponding properties to `application.yml` profiles

**Utilities:**
- Stateless helpers: `util/{Name}Util.java`
- i18n messages: Add keys to `resources/i18n/messages*.properties`

## Special Directories

**`resources/fabric/crypto/`:**
- Purpose: Hyperledger Fabric TLS certificates, user certificates, private keys
- Generated: Yes (by Fabric CA)
- Committed: No (should be in `.gitignore` for production; committed for dev/test)

**`resources/db/migration/`:**
- Purpose: Flyway database migrations
- Generated: Manually authored
- Committed: Yes
- Naming: `V{number}__{description}.sql` (Flyway convention)

**`resources/i18n/`:**
- Purpose: Internationalization message bundles
- Generated: Manually authored
- Committed: Yes

**`test/e2e/`:**
- Purpose: End-to-end test placeholders
- Generated: Manually authored
- Committed: Yes

---

*Structure analysis: 2026-05-15*
