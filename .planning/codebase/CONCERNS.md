# Codebase Concerns

**Analysis Date:** 2026-05-18

---

## Security Concerns

### CRITICAL: Hardcoded Passwords in docker-compose.yml

- Issue: MySQL root password and JWT secret are hardcoded directly in `docker-compose.yml` rather than sourced exclusively from environment variables.
- Files: `docker-compose.yml` (lines 6, 59, 63)
- Impact: Credentials are committed to version control and visible in git history. Anyone with repo access has production credentials.
- Current mitigation: `.env.example` exists; `SecurityStartupValidator` blocks known weak secrets in production profiles.
- Fix approach: Remove all hardcoded credential values. Use `${VAR}` without defaults, or use `${VAR:-changeme}` with obviously-invalid defaults that cause startup failure.

### CRITICAL: Hardcoded Default DB Password in application.yml

- Issue: `application.yml` uses `${DB_PASSWORD:123456}` with a weak default password that activates when the env var is missing.
- Files: `oaiss-chain-backend/src/main/resources/application.yml` (line 17)
- Impact: If `DB_PASSWORD` is not set, the application connects using `123456`.
- Current mitigation: `SecurityStartupValidator.java` warns on weak passwords in production profiles (`docker`, `prod`, `production`), but dev profile is unprotected.
- Fix approach: Remove default values from sensitive properties. Fail startup if required env vars are missing.

### CRITICAL: MinIO Default Credentials in docker-compose.yml

- Issue: MinIO uses `${MINIO_ACCESS_KEY:-minioadmin}` and `${MINIO_SECRET_KEY:-minioadmin}` -- the well-known default MinIO credentials.
- Files: `docker-compose.yml` (lines 39-40, 65-66)
- Impact: If env vars are not set, MinIO is accessible with `minioadmin/minioadmin`, granting full object storage access.
- Fix approach: Remove defaults or use randomly generated values. Document required env var setup.

### HIGH: FileController Lacks @PreAuthorize Annotations

- Issue: `FileController.java` has zero `@PreAuthorize` annotations. All endpoints are accessible to any authenticated user regardless of role. File download, list, info, presigned-url, copy, and upload endpoints have no role restrictions.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java`
- Impact: Any authenticated user (enterprise, reviewer, third-party) can list, download, copy, and get presigned URLs for any file in the system.
- Current mitigation: Delete operations check ownership via `checkDeletePermission()`, but read operations have no access control.
- Fix approach: Add `@PreAuthorize` annotations with appropriate role restrictions. Add tenant/owner checks for read operations.

### HIGH: FileController Trusts X-User-Id and X-User-Type Headers

- Issue: `FileController.resolveUserId()` and `resolveUserType()` fall back to reading `X-User-Id` and `X-User-Type` HTTP headers when `JwtUserDetails` is null. These headers can be spoofed by any client.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java` (lines 372-386)
- Impact: An attacker can bypass file ownership checks by setting `X-User-Id` to another user's ID and `X-User-Type` to `4` (admin).
- Fix approach: Remove header-based fallback entirely. Require `@AuthenticationPrincipal` to always be non-null (guaranteed by the security filter chain for authenticated endpoints).

### HIGH: SearchController Lacks @PreAuthorize Annotations

- Issue: `SearchController.java` has zero `@PreAuthorize` annotations. Search endpoints exposing market data, enterprise conditions, and predictions are accessible to any authenticated user.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java`
- Impact: Potentially sensitive business intelligence data is accessible to all roles without restriction.
- Fix approach: Add appropriate role-based access restrictions to search endpoints.

### HIGH: Prometheus Endpoint Unauthenticated

- Issue: `/actuator/prometheus` is explicitly permitted without authentication in `SecurityConfig`, exposing application metrics to unauthenticated users.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java` (line 80)
- Impact: Internal metrics (request rates, response times, JVM stats) are publicly accessible. Attackers can profile the application.
- Fix approach: Require authentication for Prometheus endpoint, or restrict to internal network via IP-based rules.

### MEDIUM: JWT Token Not Invalidated on Password Change

- Issue: When a user changes their password via `AuthService.changePassword()`, existing JWT tokens remain valid until natural expiry (1 hour). There is no mechanism to blacklist tokens after password change.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/AuthService.java` (lines 284-307)
- Impact: If an attacker obtains a JWT token and the user changes their password, the attacker retains access for up to 1 hour.
- Fix approach: After password change, blacklist all existing tokens for that user (similar to the logout blacklist mechanism using the `tokenBlacklist` cache).

### MEDIUM: RSA Private Keys Stored in Database in Plaintext

- Issue: The `rsa_key_pair` table stores `private_key` as plaintext TEXT with no encryption at rest.
- Files: `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql` (lines 267-281), `oaiss-chain-backend/src/main/java/com/oaiss/chain/util/RsaKeyUtil.java`
- Impact: Database compromise exposes all users' private signing keys, enabling forgery of digital signatures on carbon reports.
- Fix approach: Encrypt private keys at rest using a KEK (Key Encryption Key) managed via vault or environment variable. Decrypt only in memory during signing operations.

### MEDIUM: CORS Allows Localhost Origins in Production Config

- Issue: `application.yml` defaults CORS to `http://localhost:5173,http://localhost:5174`, and `docker-compose.yml` overrides to `http://localhost:5173,http://localhost:80`.
- Files: `oaiss-chain-backend/src/main/resources/application.yml` (line 134), `docker-compose.yml` (line 67)
- Impact: In production deployments, if `CORS_ALLOWED_ORIGINS` is not properly overridden, localhost origins are accepted.
- Fix approach: Do not provide localhost defaults in production profiles. Require explicit origin configuration.

### MEDIUM: Refresh Tokens Not Rotated

- Issue: `AuthService.refreshToken()` issues a new refresh token but does not invalidate the old one. Both old and new refresh tokens remain valid simultaneously.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/AuthService.java` (lines 186-230)
- Impact: Stolen refresh tokens can be replayed indefinitely within their 7-day validity window.
- Fix approach: Blacklist old refresh tokens when issuing new ones, or implement a refresh token family with reuse detection.

### LOW: Fabric admin-password Hardcoded in application.yml

- Issue: `fabric.ca.admin-password: adminpw` is hardcoded in the main `application.yml`.
- Files: `oaiss-chain-backend/src/main/resources/application.yml` (line 189)
- Impact: Currently mitigated by `fabric.enabled: false` and `fabric.ca.enabled: false`. No active risk until Fabric integration is enabled.
- Fix approach: Externalize to `${FABRIC_CA_ADMIN_PASSWORD:}` when Fabric integration is activated.

---

## Architecture Concerns

### HIGH: N+1 Query Pattern in CarbonNeutralProjectService.toResponse()

- Issue: The `toResponse()` method makes up to 3 additional DB queries per project (enterprise name, reviewer name, verifier name). When called from `searchProjects()` or `getMyProjects()`, this creates N+1 queries -- 3 extra queries per row in the result page.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` (lines 481-543)
- Impact: A page of 20 projects triggers ~60 extra DB queries. Performance degrades linearly with page size.
- Fix approach: Use `@EntityGraph` on the repository query, or batch-load enterprise/reviewer/verifier names by collecting IDs and doing bulk lookups (pattern already implemented in `DoubleAuctionService.resolveUserNames()`).

### HIGH: N+1 Query Pattern in CreditScoreService.toScoreResponse() and toEventResponse()

- Issue: `toScoreResponse()` queries `enterpriseRepository.findById()` per score entry. `toEventResponse()` queries `userRepository.findById()` per event entry. Both are called inside `.map()` on paginated results.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java` (lines 377-419)
- Impact: Score ranking page and event history pages have N+1 query issues. A page of 20 events triggers 20 extra user lookups.
- Fix approach: Batch-load enterprise names and user names using `findAllById()` before mapping.

### HIGH: DoubleAuctionService Uses JVM-Level synchronized Instead of Distributed Lock

- Issue: `executeMatching()` uses Java `synchronized` keyword (line 146), which only works within a single JVM. In a multi-instance deployment, concurrent matching executions will cause race conditions leading to duplicate matches and incorrect quota updates.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java` (line 146)
- Impact: Duplicate matches, incorrect quota updates, and data corruption in multi-instance deployments.
- Current mitigation: `@DistributedLock` annotation and `RedisLockService` exist in the codebase but are not applied to this method.
- Fix approach: Replace `synchronized` with `@DistributedLock` annotation using the Redis-based distributed lock. Apply lock to matching execution with a short TTL.

### MEDIUM: Inconsistent Soft-Delete Filtering in Repositories

- Issue: Several `findByUserId()` methods lack the `AndDeletedFalse` suffix, meaning they can return soft-deleted records. The following repositories are affected:
  - `EnterpriseRepository.findByUserId()` -- line 22
  - `CarbonCoinAccountRepository.findByUserId()` -- line 17
  - `AuthenticatorRepository.findByUserId()` -- line 20
  - `ReviewerRepository.findByUserId()` -- line 22
  - `ThirdPartyOrgRepository.findByUserId()` -- line 22
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/` (all listed above)
- Impact: Soft-deleted enterprises, accounts, and organizations could be returned by queries, leading to data integrity issues.
- Fix approach: Add `AndDeletedFalse` to all `findByUserId()` methods, or add `@Where(clause = "is_deleted = false")` to entity classes.

### MEDIUM: God Classes in Service Layer

- Issue: Several service classes exceed 400 lines, indicating high responsibility concentration:
  - `CarbonNeutralProjectService.java` (592 lines) -- project CRUD, all status transitions, verification, certification, monitoring, response mapping
  - `DoubleAuctionService.java` (464 lines) -- order placement, matching algorithm, quota updates, response mapping
  - `MinioService.java` (448 lines) -- file upload/download/delete/list/copy/presign with records and validation
  - `AuthService.java` (430 lines) -- login, register, refresh, captcha, IP validation, rate limiting, logout
  - `CreditScoreService.java` (421 lines) -- score CRUD, deduction, bonus, threshold management, response mapping
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/`
- Impact: Harder to test, maintain, and reason about. Changes to one concern risk breaking others.
- Fix approach: Extract response mapping into dedicated mapper/converter classes. Extract matching algorithm into a `MatchingEngine` class. Extract auth rate limiting into `LoginRateLimitService`.

### MEDIUM: No Optimistic Locking on Financial Entities

- Issue: Entities like `Enterprise` (carbon quota), `CarbonCoinAccount` (balance), and `AuctionOrder` (matched quantity) have no `@Version` field. Concurrent updates can be silently lost.
- Files:
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/Enterprise.java`
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CarbonCoinAccount.java`
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/AuctionOrder.java`
- Impact: Lost updates under concurrent writes (e.g., two transfers from the same account could overdraw).
- Fix approach: Add `@Version Long version` to entities with financial fields. Handle `OptimisticLockingFailureException` in service layer.

### LOW: Missing @Transactional(readOnly=true) on Read Operations

- Issue: No read-only service methods use `@Transactional(readOnly = true)`. All query methods run in read-write transaction mode or without explicit transaction annotation.
- Files: All service files in `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/`
- Impact: Minor performance overhead (Hibernate dirty checking runs unnecessarily). No data corruption risk.
- Fix approach: Add `@Transactional(readOnly = true)` to all service methods that only read data (`get*`, `list*`, `search*`, `check*` methods).

---

## Code Quality Concerns

### MEDIUM: Repeated Auto-Initialization Pattern in CreditScoreService

- Issue: The CreditScore auto-initialization block (create with `score=100`, `level=EXCELLENT`, `tradeRestricted=false`, `accountFrozen=false`) is copy-pasted 4 times in `CreditScoreService.java` (lines 70-77, 94-103, 125-131, 188-197).
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java`
- Impact: If default values change, all 4 locations must be updated. Risk of divergence.
- Fix approach: Extract a private `getOrCreateScore(Long enterpriseId)` method that returns the existing score or creates and saves a new one with defaults.

### MEDIUM: Status Constants as Raw Integers in CarbonNeutralProjectService

- Issue: Status values are defined as `public static final int` constants rather than using the enum pattern (`AuctionOrderStatusEnum`, `MatchingStatusEnum`). Methods like `getStatusText()`, `getCertStatusText()`, and `getVerificationStatusText()` use raw switch statements on int values.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` (lines 62-80, 557-590)
- Impact: Inconsistent with the enum pattern used elsewhere. Type-unsafe -- any int can be passed. Compiler cannot catch invalid status values.
- Fix approach: Create `ProjectStatusEnum`, `CertStatusEnum`, and `VerificationStatusEnum` enums following the pattern of existing enums. Use `fromCode()` and `getDescription()` methods.

### MEDIUM: Large Vue Components Exceeding 500 Lines

- Issue: Several Vue single-file components exceed the 500-line threshold:
  - `OfficialHome.vue` (646 lines)
  - `CompanyDashboard.vue` (525 lines)
  - `TradingMarket.vue` (390 lines)
  - `CarbonCoin.vue` (368 lines)
  - `CarbonFormulaCalculator.vue` (358 lines)
- Files: `oaiss-chain-frontend/src/views/`
- Impact: Harder to maintain and test. Template, script, and style all in one file increases cognitive load.
- Fix approach: Extract sub-components for reusable UI sections. Extract composables for complex logic.

### MEDIUM: AOP Annotations Used Inconsistently

- Issue: The custom AOP annotations (`@AuditLog`, `@RateLimit`, `@DataIsolation`, `@DistributedLock`, `@RequirePermission`) appear in only 16 files out of 60+ service/controller files. Critical operations lack audit logging and rate limiting:
  - `DoubleAuctionService` -- no `@AuditLog`, no `@DistributedLock` on matching
  - `CarbonCoinService` -- no `@DistributedLock` on transfers
  - `FileController` -- no `@AuditLog`, no `@RateLimit` on uploads
- Files:
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonCoinService.java`
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java`
- Impact: Financial transactions and file operations are not audit-logged. File uploads have no rate limiting.
- Fix approach: Apply `@AuditLog` to all mutation endpoints. Apply `@RateLimit` to upload and registration endpoints. Apply `@DistributedLock` to matching and transfer operations.

### LOW: JSON.parse Without Try-Catch in Frontend

- Issue: Several `JSON.parse()` calls lack try-catch protection. If stored data is corrupted, the app crashes with an unhandled SyntaxError.
- Files:
  - `oaiss-chain-frontend/src/utils/auth.ts` (lines 193, 195) -- JWT payload parsing
  - `oaiss-chain-frontend/src/views/Login.vue` (line 51) -- saved form data
  - `oaiss-chain-frontend/src/views/admin/SystemConfig.vue` (line 48) -- saved configs
  - `oaiss-chain-frontend/src/views/enterprise/EmissionData.vue` (line 60) -- historical data
- Impact: Corrupted localStorage data causes unhandled exceptions and potential white-screen crash.
- Fix approach: Wrap `JSON.parse()` calls in try-catch. Return safe defaults on parse failure.

### LOW: Single TODO Comment Documents Known N+1 Issue

- Issue: One TODO comment in `CarbonNeutralProjectService.toResponse()` documents the N+1 query issue.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` (line 478)
- Impact: Known issue tracked only as an inline comment rather than a ticketed work item.
- Fix approach: Create a dedicated issue for the N+1 fix and remove the TODO.

---

## Infrastructure Concerns

### HIGH: MinIO Port Exposed to All Interfaces

- Issue: MinIO ports `9002:9000` and `9003:9001` are bound to all interfaces (no `127.0.0.1` prefix), unlike MySQL and Redis which are bound to `127.0.0.1`.
- Files: `docker-compose.yml` (lines 43-44)
- Impact: MinIO API and console are accessible from any network interface. Combined with default credentials (`minioadmin/minioadmin`), this is a significant exposure.
- Fix approach: Bind MinIO ports to `127.0.0.1` like MySQL and Redis, or place behind a reverse proxy with authentication.

### MEDIUM: Backend and Frontend Ports Exposed to All Interfaces

- Issue: Backend port `8080:8080` and frontend port `5173:80` are bound to all interfaces without `127.0.0.1`.
- Files: `docker-compose.yml` (lines 54, 83)
- Impact: API server and frontend are directly accessible from the network without a reverse proxy.
- Fix approach: Bind to `127.0.0.1` or deploy behind a reverse proxy (nginx) with TLS termination.

### MEDIUM: No Database Indexes on Foreign Key Columns

- Issue: The Flyway migration `V1__init_schema.sql` creates indexes only on the `operation_log` table. All foreign key columns (`enterprise_id`, `submitter_id`, `reviewer_id`, `buyer_id`, `seller_id`, `user_id`, `owner_id`, `verifier_id`, etc.) lack indexes.
- Files: `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql`
- Impact: Queries filtering by these columns perform full table scans. Performance degrades as data grows.
- Fix approach: Add a `V5__add_missing_indexes.sql` migration with indexes on:
  - `carbon_report(enterprise_id, status)`
  - `transaction(buyer_id, seller_id, status)`
  - `auction_order(user_id, direction, status)`
  - `matching_result(buyer_id, seller_id, status)`
  - `credit_event(enterprise_id, triggered_at)`
  - `emission_rating(enterprise_id, rating_year)`
  - `carbon_neutral_project(owner_id, status)`
  - `rsa_key_pair(user_id, key_status)`
  - `carbon_coin_transaction(user_id, tx_type)`
  - `entry_permission(user_type, api_path)`

### MEDIUM: dev Profile Uses ddl-auto: update

- Issue: `application-dev.yml` sets `spring.jpa.hibernate.ddl-auto: update`, which allows Hibernate to modify the schema. This can silently alter tables and conflict with Flyway migrations.
- Files: `oaiss-chain-backend/src/main/resources/application-dev.yml` (line 15)
- Impact: Schema drift between environments. Local dev may have different schema than production.
- Fix approach: Use `ddl-auto: validate` in all profiles. Rely exclusively on Flyway for schema management.

### MEDIUM: No CI/CD Pipeline

- Issue: No CI/CD configuration exists (no `.github/workflows/`, no `Jenkinsfile`, no `gitlab-ci.yml`). All deployment is manual.
- Files: None found
- Impact: No automated testing on PRs. No automated deployment. Risk of regressions shipping to production.
- Fix approach: Add GitHub Actions workflow for build, test, and deploy. Run backend tests with Testcontainers, frontend tests with Vitest.

### LOW: Missing Flyway Migration V3

- Issue: Migration versions jump from `V2__seed_data.sql` to `V4__enterprise_admission.sql` with no `V3` file.
- Files: `oaiss-chain-backend/src/main/resources/db/migration/`
- Impact: No functional impact (Flyway uses version numbers, not sequential), but indicates a deleted or merged migration.
- Fix approach: Add a comment in V4 noting that V3 was consolidated.

---

## Performance Concerns

### HIGH: MinioService.listFiles() Loads All Objects Into Memory

- Issue: `listFiles()` fetches ALL objects matching a prefix from MinIO into an in-memory `ArrayList`, then manually paginates via `subList()`. For buckets with thousands of files, this is extremely slow and memory-intensive.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MinioService.java` (lines 322-355)
- Impact: O(n) memory and time for listing, where n is total files. Can cause OOM with large buckets.
- Fix approach: Use MinIO's built-in pagination with continuation tokens, or cap the listing to a reasonable batch size (e.g., 1000) before in-memory pagination.

### MEDIUM: DoubleAuctionService Loads All Active Orders Into Memory

- Issue: `executeMatching()` loads all active buy and sell orders into memory. For large markets with thousands of open orders, this creates significant memory and CPU pressure.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java` (lines 153-158)
- Impact: Memory scales O(n+m). Matching loop is O(n*m) in the worst case.
- Fix approach: Implement top-of-book matching -- only load the best bid and best ask, match them, and repeat iteratively.

### MEDIUM: No Caching on Frequently Accessed Reference Data

- Issue: Enterprise name lookups, user real name lookups, and project type/status text mappings are queried from the database on every request with no caching.
- Files:
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java` (toResponse)
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java` (toScoreResponse, toEventResponse)
- Impact: Repeated identical DB queries for reference data that rarely changes.
- Fix approach: Add `@Cacheable` annotations on lookup methods, or use a reference data cache that refreshes periodically.

### LOW: ML Service WebClient Timeout Mismatch

- Issue: `application.yml` configures `read-timeout: 30s` for the ML service WebClient but the `resilience4j.timelimiter` is set to `35s`. If a `block()` call is used with a shorter default timeout, it may throw before the WebClient timeout fires.
- Files: `oaiss-chain-backend/src/main/resources/application.yml` (lines 148-150, 168-170)
- Impact: Premature timeout exceptions on slow ML predictions.
- Fix approach: Align all timeout values. Ensure `block()`, WebClient `readTimeout`, and `resilience4j` timelimiter are consistent (all 30s or all 35s).

---

## Testing Concerns

### MEDIUM: Controllers Without @PreAuthorize Have No Authorization Test Coverage

- Issue: `FileController` and `SearchController` have no `@PreAuthorize` annotations, meaning authorization boundaries cannot be tested. Any existing tests only verify happy-path functionality.
- Files:
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java`
  - `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java`
- Impact: No verification that role-based access restrictions work for file operations and search endpoints.
- Fix approach: Add authorization tests that verify cross-role access is denied where appropriate.

### LOW: No Integration Tests for Concurrent Matching

- Issue: `DoubleAuctionService.executeMatching()` has a race condition risk in multi-instance deployments (uses `synchronized` not distributed lock), but there are no integration tests verifying concurrent matching behavior.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`
- Impact: Race conditions may only appear in production under load.
- Fix approach: Add a concurrent integration test that calls `executeMatching()` from multiple threads simultaneously and verifies no duplicate matches or quota corruption.

---

## Technical Debt

### MEDIUM: MockBlockchainService Active by Default

- Issue: `fabric.enabled: false` in `application.yml` means all blockchain operations use `MockBlockchainService`, which returns simulated data. Production deployment requires Fabric setup that has not been validated.
- Files: `oaiss-chain-backend/src/main/resources/application.yml` (line 174), `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/FabricGatewayConfig.java`
- Impact: Blockchain-related features (on-chain hashing, transaction verification) are non-functional in current deployment.
- Fix approach: Validate Fabric integration on WSL2/Docker before production. Add health check for Fabric connectivity.

### MEDIUM: CaptchaService Uses Image-Based Captcha (Not Real SMS/Email)

- Issue: `CaptchaService` generates image-based captchas. The CLAUDE.md mentions SMS/email stubs, meaning real notification delivery is not implemented.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CaptchaService.java`
- Impact: No real multi-factor authentication. Captcha is client-side solvable (not a strong identity proof).
- Fix approach: Integrate real SMS gateway or email service for production. Consider using a captcha service like reCAPTCHA.

### LOW: No API Versioning Strategy

- Issue: All endpoints are at `/api/v1/...` but there is no mechanism to support multiple API versions simultaneously. Breaking changes require all clients to upgrade at once.
- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/ApiVersionConfig.java`
- Impact: Future API evolution will be constrained.
- Fix approach: Document versioning strategy. Consider URL-path or header-based versioning for v2.

---

## Fragile Areas

### DoubleAuctionService -- High-Value Financial Logic

- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/DoubleAuctionService.java`
- Why fragile: Combines matching algorithm, enterprise quota updates, order status management, and transaction creation in single methods. Any bug in the matching loop can cause financial data corruption.
- Safe modification: Add `@DistributedLock` before changing matching logic. Always test with concurrent order scenarios. Ensure `updateEnterpriseQuota()` is atomic.
- Test coverage: Controller tests exist but no concurrent matching tests.

### AuthService -- Authentication Bypass Risk

- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/AuthService.java`
- Why fragile: Central to all authentication. The `validateRegistrationUserType()` blocklist approach (allowing only specific types) is correct, but any change to `UserTypeEnum` without updating `ALLOWED_REGISTRATION_TYPES` could open registration to new roles.
- Safe modification: When adding new user types, verify both the allow-list for registration and the `SecurityConfig` whitelist paths.
- Test coverage: `AuthServiceTest` exists with 42 edges in the graph -- well-tested.

### CarbonNeutralProjectService -- Most Complex Service (592 Lines)

- Files: `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
- Why fragile: Manages the entire project lifecycle (6 statuses, 3 sub-statuses for certification/verification) in a single class. The `toResponse()` method has a known N+1 query issue. Status transition validation is scattered across individual methods rather than using a state machine.
- Safe modification: Review all status constants and transition rules before modifying any status-related method. Ensure `validateOwner()` is called before any mutation.
- Test coverage: `CarbonNeutralProjectServiceTest` and `CarbonNeutralProjectControllerTest` exist.

---

*Concerns audit: 2026-05-18*
