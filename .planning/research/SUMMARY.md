# Project Research Summary

**Project:** OAISS CHAIN (Manual Testing Phase)
**Domain:** Carbon trading and blockchain platform -- Spring Boot 3.2.5 + Vue 3.5
**Researched:** 2026-05-08
**Confidence:** HIGH

## Executive Summary

OAISS CHAIN is a multi-role carbon trading platform with 16 REST controllers, 19 services, 5 user roles, and 21 database tables. The system is functionally complete but untested end-to-end. The current goal is to exercise all 65+ business flows across all 5 roles using real backend APIs and databases (no mocks), making the platform ready for continued blockchain development. The tech stack (Spring Boot + MySQL + Redis + MinIO) is mature and well-understood; testing tooling is largely built-in (Swagger UI, MinIO Console) with three additional tools needed (DBeaver, Bruno, RedisInsight).

The recommended approach is a phased manual testing strategy organized around the carbon report lifecycle as the central business flow. Testing must proceed in dependency order: authentication first, then data entry workflows, then approval workflows, then trading and settlement. The primary risk is test data pollution -- since there is no automatic cleanup between manual tests, testers must use Docker volume reset (`docker-compose down -v`) as the primary reset mechanism and plan test execution in a fixed dependency order.

Key blockers to watch: (1) JWT token expiration is the number one source of false failures -- testers should re-login every 45 minutes; (2) VERIFIER/CERTIFIER roles are referenced in `@PreAuthorize` annotations but do not exist in `UserTypeEnum`, which may cause 403 errors on carbon neutral project verification/certification flows; (3) the relationship between `TradeController` auction endpoints and `DoubleAuctionController` is unclear and needs validation; (4) only QUAL-05 (no input validation on auction orders with negative price/quantity) is a genuine new bug worth reporting -- all other CONCERNS.md issues are known and should not be re-filed.

## Key Findings

### Recommended Stack

Testing tools are organized into three tiers. Tier 1 (must install before testing): DBeaver Community Edition for MySQL inspection, Bruno (or Postman) for systematic API testing across 16 controllers, and RedisInsight for verifying rate limiting, caching, and distributed locks. Tier 2 (install as needed): Vue DevTools for frontend state debugging, HTTPie for quick CLI verification. Tier 3 (install only for specific issues): mitmproxy for complex network debugging.

**Core tools:**
- **Swagger UI** (built-in): Primary API explorer at `http://localhost:8080/api/v1/swagger-ui.html` -- zero setup, auto-generates from SpringDoc OpenAPI 2.5 annotations
- **DBeaver CE** (install required): Database state verification -- ER diagrams, data grid, SQL autocomplete for the 21-table schema
- **Bruno** (install required): Systematic API testing -- offline-first, git-friendly `.bru` files, environment variable support for storing JWT tokens per role
- **RedisInsight** (install required): Real-time profiler to observe `@RateLimit`, `@DistributedLock`, and `@Cacheable` behavior in action
- **MinIO Console** (built-in): Web UI at `http://localhost:9001` for verifying file uploads, downloads, and bucket contents
- **Chrome DevTools** (built-in): Network tab for API debugging, Application tab for sessionStorage token inspection, one Chrome profile per role for multi-role testing

**Seed accounts (all password: `admin123`):**
| Username | Role | Enterprise |
|----------|------|------------|
| admin | ADMIN | -- |
| enterprise001 | ENTERPRISE | Green Energy Tech (quota 50000) |
| enterprise002 | ENTERPRISE | Low Carbon Manufacturing (quota 80000) |
| reviewer001 | REVIEWER | -- |
| thirdparty001 | THIRD_PARTY | -- |
| authenticator001 | AUTHENTICATOR | -- |

### Expected Features (Test Coverage)

65+ business flows across 16 controllers, organized into 15 domains. Priority tiers:

**P0 (Critical Path -- 12 flows):**
- Login and token refresh (ALL roles)
- Carbon report create/submit/review lifecycle (ENTERPRISE + REVIEWER)
- Carbon coin balance view (ENTERPRISE)
- Double auction buy/sell/match (ENTERPRISE + ADMIN)
- P2P trade create/confirm (ENTERPRISE)
- Admin enable/disable user (ADMIN)

**P1 (Important -- 30+ flows):**
- User profile management
- Carbon coin transfer
- Carbon neutral project full lifecycle (13 sub-flows)
- Credit scoring (8 sub-flows including deduction, bonus, level evaluation, trade restriction)
- Digital signatures (7 sub-flows: generate, sign, verify, encrypt, decrypt)
- File upload/download
- Emission ratings and AI prediction
- Third-party monitoring
- Market overview search

**Edge Cases (must validate):**
- Cross-role access control (ENTERPRISE blocked from review, REVIEWER blocked from trading)
- Data isolation (Enterprise A cannot see Enterprise B's drafts)
- State machine violations (submit non-draft, review non-submitted, delete non-draft)
- Financial integrity (negative balance prevention, zero-price order rejection)

### Architecture Approach

The system follows a standard Spring Boot layered architecture with AOP cross-cutting concerns. Controllers delegate to services, services use JPA repositories, and state is managed across three backends (MySQL for persistence, Redis for caching/rate-limiting/locks, MinIO for files). Authentication is stateless JWT with tokens stored in `sessionStorage` (access, 1h TTL) and `localStorage` (refresh, 7d TTL). Role-based access control operates at two layers: Vue Router `meta.roles` for frontend navigation guards and `@PreAuthorize` for backend endpoint protection.

**Major components:**
1. **Auth Layer** -- JWT stateless sessions, captcha-based login, token refresh with single-flight pattern
2. **Carbon Report Lifecycle** -- Central business flow: DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED -> ON_CHAIN, with service interactions to CreditScoreService, EmissionRatingService, and BlockchainService
3. **Trading Engine** -- Dual paths: DoubleAuctionController (batch matching by admin, `synchronized` singleton) and TradeController (P2P direct trades + separate auction endpoint)
4. **Carbon Neutral Projects** -- Complex 9-state lifecycle including verification and certification phases
5. **AOP Cross-Cutting** -- `@AuditLog`, `@RateLimit`, `@RequirePermission`, `@DataIsolation`, `@DistributedLock` annotations with corresponding aspects

### Critical Pitfalls

1. **JWT Token Expiration (Pitfall 1)** -- Access token expires after 1 hour. Mid-test, all API calls return 401. Prevention: re-login every 45 minutes, keep DevTools Network tab open, verify token freshness after role switches.

2. **Database State Pollution (Pitfall 3)** -- No automatic cleanup between manual tests. Test A's data contaminates Test B. Prevention: test in fixed dependency order, use timestamped unique identifiers, reset via `docker-compose down -v` when stuck.

3. **Trading Race Conditions (Pitfall 5)** -- `executeMatching()` is `synchronized` but `@Transactional` commits after lock release. No `@Version` optimistic locking. Prevention: test trading sequentially, never concurrently, never open two tabs placing orders simultaneously for the same enterprise.

4. **Role Switching Without Cleanup (Pitfall 6)** -- Stale sessionStorage/Pinia state from previous role leaks into new session. Prevention: one role per Incognito window, always use logout button, verify menu items after login.

5. **Redis Cache Stale Data (Pitfall 4)** -- Emission factors and permissions are hardcoded Java constants loaded on startup, not read from DB. No cache invalidation endpoint. Prevention: accept that config changes require backend restart.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Environment Setup and Authentication Baseline
**Rationale:** Nothing else can be tested without a working environment and verified login for all 5 roles. This phase validates the infrastructure layer (Docker, MySQL, Redis, MinIO) and the auth layer (JWT, captcha, role routing).
**Delivers:** Confirmed working Docker stack, all 6 seed accounts login-verified, frontend role-based routing validated for each role's home page.
**Addresses:** Flows 1.1 (Login), 1.3 (Token Refresh), 1.4 (Logout)
**Avoids:** Pitfall 6 (role switching) and Pitfall 7 (CORS) by catching environment issues early.
**Data requirement:** V3 Flyway migration must be created and applied before this phase starts.

### Phase 2: Carbon Report Lifecycle (End-to-End)
**Rationale:** The carbon report lifecycle is the central business flow. It touches ENTERPRISE (create/submit), REVIEWER (approve/reject), and cascades into CreditScoreService, EmissionRatingService, and BlockchainService. Testing this validates the core multi-role workflow pattern that all other flows follow.
**Delivers:** Verified report creation, submission, review (approve + reject), data isolation between enterprises, status state machine correctness.
**Addresses:** Flows 3.1-3.6 (Carbon Reports), plus cascading service interactions.
**Avoids:** Pitfall 3 (DB pollution) by using unique report names with timestamps.
**Research flag:** Needs validation of the report status state machine -- the 6-state flow (DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED -> ON_CHAIN) is complex.

### Phase 3: Carbon Coin and Trading Engine
**Rationale:** Trading depends on carbon coin accounts (Phase 2 approved reports may affect carbon quotas). Trading has two paths (DoubleAuctionController and TradeController) whose relationship is unclear. This phase resolves that ambiguity and validates the most concurrency-sensitive code.
**Delivers:** Verified carbon coin recharge/transfer, buy/sell order placement, admin matching execution, P2P trade creation/confirmation/cancellation, settlement correctness (balance updates, quota updates, blockchain hash recording).
**Addresses:** Flows 4.1-4.4 (Carbon Coin), 5.1-5.5 (Double Auction), 6.1-6.7 (P2P Trading)
**Avoids:** Pitfall 5 (race conditions) by enforcing sequential test execution. Pitfall 9 (pagination) by testing boundary conditions.
**Research flag:** Needs deeper investigation -- the relationship between `TradeController.POST /trade/auction` and `DoubleAuctionController.POST /auction/buy|sell` is not documented. Do these create different order types? Do they share the matching engine?

### Phase 4: Carbon Neutral Projects and Credit Scoring
**Rationale:** Project lifecycle depends on approved carbon reports (Phase 2). Credit scoring is affected by report approval/rejection and affects trading permissions. These two domains are tightly coupled (verification/certification phases reference credit levels).
**Delivers:** Verified 9-state project lifecycle (PREPARING through CERTIFIED), credit score deduction/bonus, level evaluation, trade restriction enforcement on WARNING/FROZEN enterprises.
**Addresses:** Flows 7.1-7.13 (Carbon Neutral Projects), 8.1-8.8 (Credit Scoring)
**Avoids:** Pitfall 14 (hardcoded emission factors) by testing credit scoring as read-only -- do not expect runtime factor changes.
**Research flag:** VERIFIER and CERTIFIER roles are referenced in `@PreAuthorize` on `CarbonNeutralProjectController` but do not exist in `UserTypeEnum`. This will likely cause 403 errors on flows 7.7 (Verify Project) and 7.9 (Complete Certification). Must validate and document as a blocking bug if confirmed.

### Phase 5: Supporting Domains (Signatures, Files, Emission, Blockchain, Admin, Third-Party, Search)
**Rationale:** These domains are secondary to the core carbon lifecycle but complete the platform. They can be tested in any order once the core flows are verified. Grouping them avoids context-switching overhead.
**Delivers:** Verified digital signature generation/sign/verify/encrypt/decrypt, file upload/download via MinIO, emission ratings and AI prediction, blockchain explorer (mock mode), admin user management, third-party monitoring, search and market overview.
**Addresses:** Flows 9.1-9.7 (Signatures), 10.1-10.7 (Files), 11.1-11.4 (Emission), 12.1-12.5 (Blockchain), 14.1-14.4 (Admin), 15.1-15.4 (Third-Party), 13.1-13.3 (Search)
**Avoids:** Pitfall 11 (MinIO failures) by verifying MinIO container health before file tests. Pitfall 1 (JWT expiration) by re-login before long test sessions.

### Phase 6: Cross-Cutting Concerns and Edge Cases
**Rationale:** AOP concerns (`@AuditLog`, `@RateLimit`, `@RequirePermission`, `@DataIsolation`, `@DistributedLock`) are best tested after all functional flows are verified. Edge cases (cross-role access, state machine violations, financial integrity) form a comprehensive regression pass.
**Delivers:** Verified audit logging, rate limiting behavior, permission enforcement, data isolation between enterprises, distributed lock serialization. Complete edge case coverage.
**Addresses:** All cross-cutting concerns from the AOP matrix, all edge cases from FEATURES.md.
**Avoids:** Pitfall 8 (role name mismatches) by systematic negative testing of each role against each endpoint.
**Research flag:** Rate limiting thresholds and distributed lock key patterns need code-level verification to design effective test cases.

### Phase Ordering Rationale

- **Authentication first (Phase 1):** Every other phase requires multi-role login. If auth is broken, nothing else can be tested.
- **Carbon reports second (Phase 2):** This is the central business object. Trading, projects, and credit all reference carbon reports.
- **Trading third (Phase 3):** Depends on carbon coin accounts and (optionally) approved reports. Has the highest concurrency risk.
- **Projects and credit fourth (Phase 4):** Depend on the report lifecycle for data. Credit scoring gates trading permissions.
- **Supporting domains fifth (Phase 5):** Independent of each other, can be parallelized. Best grouped for efficiency.
- **Edge cases last (Phase 6):** Requires all functional flows working. Forms a comprehensive regression pass.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3 (Trading Engine):** Relationship between TradeController auction endpoints and DoubleAuctionController is unclear. Needs code-level analysis of matching algorithm, order table schema, and settlement logic.
- **Phase 4 (Carbon Neutral Projects):** VERIFIER/CERTIFIER role mismatch with UserTypeEnum needs investigation. May require a code fix before testing can proceed.
- **Phase 6 (AOP Concerns):** Rate limit thresholds, distributed lock key patterns, and audit log field mappings need code-level verification.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Auth):** Well-documented JWT + captcha flow. Seed data is ready. Straightforward.
- **Phase 5 (Supporting Domains):** Standard CRUD patterns. File upload, blockchain explorer, admin management follow established Spring Boot patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All tools verified against project config (docker-compose.yml, application.yml, SecurityConfig.java). Seed accounts confirmed in V2__seed_data.sql. |
| Features | HIGH | 65+ flows derived directly from 16 controller source files. Priority tiers assigned based on business criticality. |
| Architecture | HIGH | Component boundaries, service interactions, and data flow traced from source code. AOP concerns verified via annotation scanning. |
| Pitfalls | HIGH | All 20 pitfalls derived from direct code analysis (JwtTokenProvider, DoubleAuctionService, CachePreloadService, SecurityConfig). CONCERNS.md cross-referenced. |

**Overall confidence:** HIGH

The research is based entirely on direct source code analysis rather than documentation or external references. All 4 research documents trace findings to specific source files and line numbers.

### Gaps to Address

- **VERIFIER/CERTIFIER role existence:** @PreAuthorize references these roles but UserTypeEnum does not define them. Must validate during Phase 1 login testing -- if these roles cannot authenticate, Phases 4.6-4.9 (project verification/certification) are blocked.
- **TradeController vs DoubleAuctionController relationship:** Two separate controllers handle trading. TradeController has both P2P and auction endpoints; DoubleAuctionController has its own auction endpoints. Whether they share the same matching engine and order table is unclear. Must resolve during Phase 3 planning.
- **Blockchain mock correctness:** BlockchainService runs in mock mode. Mock return values should be verified to be structurally correct (proper tx hash format, block number format) so downstream code that consumes blockchain responses does not break.
- **V3 Flyway migration:** The test seed data migration (V3__test_seed_data.sql) described in STACK.md does not yet exist. It must be created before Phase 1 testing begins.
- **INFRA-01 (rsa_key_pairs table):** If this table was added outside Flyway, schema drift may cause Flyway validation failures. Must verify during Phase 1 environment setup.

## Sources

### Primary (HIGH confidence)
- `V1__init_schema.sql` -- 21 table definitions, full DDL
- `V2__seed_data.sql` -- 6 seed accounts, reference data, BCrypt password hashes
- `SecurityConfig.java` -- JWT filter chain, CORS config, permit-all endpoints, CSRF disabled
- `JwtTokenProvider.java` -- Token generation, validation, 1h/7d TTL
- `request.ts` -- Axios interceptor, auto-refresh logic, pagination transform (pageNum/pageSize -> page/size)
- `auth.ts` -- Token storage (sessionStorage for access, localStorage for refresh)
- `router/index.ts` -- Vue Router role guards, meta.roles configuration
- `DoubleAuctionService.java` -- synchronized matching, @Transactional boundary issues
- `CachePreloadService.java` -- Hardcoded emission factors, Redis key patterns, KEYS command usage
- `RateLimitAspect.java` -- Rate limit key patterns, Redis sliding window
- All 16 controller source files -- Endpoint definitions, @PreAuthorize annotations, request/response types

### Secondary (MEDIUM confidence)
- `CONCERNS.md` -- Known issues catalog (SEC-01 through INFRA-04), cross-referenced with pitfall analysis
- `application.yml` -- Configuration values (JWT expiration, CORS origins, SpringDoc config)
- `docker-compose.yml` -- Infrastructure service definitions (ports, volumes, health checks)

### Tertiary (LOW confidence)
- TradeController/DoubleAuctionController relationship -- inferred from separate source files, not explicitly documented
- VERIFIER/CERTIFIER role gap -- inferred from @PreAuthorize annotations vs UserTypeEnum, needs runtime validation
- Blockchain mock behavior -- not traced to implementation, assumed from "mock mode" references

---
*Research completed: 2026-05-08*
*Ready for roadmap: yes*
