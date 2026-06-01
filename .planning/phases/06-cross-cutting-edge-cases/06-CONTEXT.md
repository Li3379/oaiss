# Phase 6: Cross-Cutting & Edge Cases - Context

**Gathered:** 2026-05-10
**Status:** Ready for planning

<domain>
## Phase Boundary

All AOP cross-cutting concerns verified, comprehensive edge case and negative testing completed, SEC-03/04 security fixes applied and verified, all discovered bugs from Phases 2-5 resolved. This phase covers 13 requirements across 3 sub-domains: AOP verification (AOP-01~04), edge cases (EDGE-01~06), and bug fixes (BUG-01~03). Depends on Phases 2-5 (all complete).

</domain>

<decisions>
## Implementation Decisions

### AOP Verification Strategy (AOP-01~04)

- **D-01:** @AuditLog, @RateLimit, @DistributedLock annotations exist in code but are NOT applied to any controller or service methods. Temporarily add them to key endpoints, test, then revert after verification.
- **D-02:** Temporary annotation placement:
  - `@AuditLog(module="test", action="test")` on `CarbonController.createReport()`
  - `@RateLimit(key="test", limit=3, period=60)` on `AuthController.login()`
  - `@DistributedLock(key="'test:' + #request")` on `DoubleAuctionController.matchOrders()`
- **D-03:** After testing, revert ALL temporary annotations via `git checkout` on modified files. Record AOP aspects as VERIFIED (code logic works when annotations are present).
- **D-04:** @DataIsolation is already applied to 3 endpoints in DigitalSignatureController (lines 160, 256, 297). Test directly without modification — enterprise001 tries to access enterprise002's signature data.
- **D-05:** For each AOP test:
  - AOP-01 (AuditLog): Trigger annotated endpoint → verify `operation_log` table has new row with correct module/action/userId
  - AOP-02 (RateLimit): Trigger annotated endpoint 4 times in 60s window (limit=3) → 4th call returns rate limit error → verify Redis key exists
  - AOP-03 (DataIsolation): enterprise001 tries GET signature data belonging to enterprise002 → verify blocked
  - AOP-04 (DistributedLock): Two concurrent requests to same lock key → verify second is rejected with "operation in progress"

### Security Fixes (BUG-02/03)

- **D-06:** SEC-03 fix: Change SecurityConfig Swagger endpoints from `permitAll()` to conditionally permit based on Spring profile. In non-docker profile, require authentication. Verify: curl without token → 401 on swagger-ui.
- **D-07:** SEC-04 fix: Remove `http://localhost:5173` default fallback from `@Value` annotation. Use `${CORS_ALLOWED_ORIGINS}` without default, or set dev-only default via `application-dev.yml`. Verify: CORS header not present for unauthorized origins.
- **D-08:** After security fixes, run `security-reviewer` agent on entire SecurityConfig for comprehensive review.

### Edge Case Testing (EDGE-01~06)

- **D-09:** EDGE-01 (Cross-role access): Exhaustive 6-role × key-endpoint matrix. Test each role against endpoints that DON'T belong to them. Expected: 403 Forbidden for all unauthorized access. Roles: ENTERPRISE(1), REVIEWER(2), THIRD_PARTY(3), ADMIN(4), AUTHENTICATOR(5), plus unauthenticated.
- **D-10:** EDGE-02 (State machine violations): Exhaustive ALL state × action combinations for:
  - CarbonReport: DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED × submit, review, approve, reject
  - DoubleAuctionOrder: PENDING, PARTIALLY_MATCHED, FULLY_MATCHED, CANCELLED × match, cancel
  - CarbonNeutralProject: 12+ states × verify, approve, reject transitions
  - P2P Trade: PENDING, ACCEPTED, REJECTED, COMPLETED × accept, reject, settle
- **D-11:** EDGE-03 (Financial integrity): Verify for all trade/auction operations:
  - Buy order total (price × quantity) matches deduction
  - Sell order proceeds match credit after matching
  - P2P trade: sender balance decreases by exact amount, receiver increases by exact amount
  - No fractional coin loss in any operation
  - Negative balance impossible
- **D-12:** EDGE-04 (Pagination boundaries): Test all paginated endpoints with: page 0 (empty result), page beyond total, pageSize=1, pageSize=1000, pageSize=0. Endpoints: carbon reports, trades, auction orders, projects, users, logs.
- **D-13:** EDGE-05 (Input validation): Test endpoints accepting numeric input with: negative price (-1), zero quantity (0), negative quantity, extremely large values (MAX_LONG), XSS strings (<script>), SQL injection (' OR 1=1), oversized text (10KB string). Key endpoints: createReport, createOrder, createProject.
- **D-14:** EDGE-06 (i18n): Verify frontend language switch (zh-CN ↔ en-US) via Vue i18n. Backend error messages use Chinese by default — verify ErrorCode messages are consistent. NOTE: i18n is M4 deferred item — basic verification only.

### Bug Fixes from Prior Phases (BUG-01)

- **D-15:** BUG-01 covers all bugs discovered during Phases 2-5:
  - Phase 5: NonUniqueResultException in DigitalSignatureService (multiple keypairs per user)
  - Any additional bugs found during EDGE testing in this phase
- **D-16:** Fix approach: Fix code → add test → verify fix → commit. Each fix is a separate commit.

### Test Script Organization

- **D-17:** Three scripts by domain:
  - `scripts/aop-test.sh` — AOP-01~04 (4 requirements)
  - `scripts/edge-test.sh` — EDGE-01~06 (6 requirements)
  - `scripts/bugfix-test.sh` — BUG-01~03 (3 requirements, including SEC fixes)
- **D-18:** Execution order: bugfix-test.sh first (fix SEC-03/04 before other tests), then aop-test.sh, then edge-test.sh.
- **D-19:** aop-test.sh requires backend restart to apply temporary annotations. Script will:
  1. Back up modified files
  2. Apply annotations
  3. Restart backend (or note that restart is needed)
  4. Run tests
  5. Revert modifications

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend - AOP Aspects
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/AuditLogAspect.java` — Around advice, saves to OperationLogRepository
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/RateLimitAspect.java` — Redis Lua script, fail-open on Redis error
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/DataIsolationAspect.java` — EnterpriseContextHolder, skipAdmin=true default
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/DistributedLockAspect.java` — SpEL key parsing, RedisLockService

### Backend - AOP Annotations
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/annotation/AuditLog.java` — module, action, description, recordParams, recordResult, sensitiveFields
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/annotation/RateLimit.java` — key, period(60s), limit(100), limitType(DEFAULT/IP/USER/IP_USER), message
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/annotation/DataIsolation.java` — enabled, skipAdmin, resourceIdParam, resourceType
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/annotation/DistributedLock.java` — key(SpEL), expireTime(30s), waitTime(0ms), errorMessage

### Backend - Security Config
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java` — CSRF disabled, CORS config, Swagger permitAll
- `oaiss-chain-backend/src/main/resources/application.yml` — Swagger enabled, CORS default localhost:5173
- `oaiss-chain-backend/src/main/resources/application-docker.yml` — Swagger disabled, CORS from env var

### Backend - State Machines (for EDGE-02)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonController.java` — Report states: DRAFT→SUBMITTED→UNDER_REVIEW→APPROVED/REJECTED
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/DoubleAuctionController.java` — Order states: PENDING→PARTIALLY_MATCHED/FULLY_MATCHED/CANCELLED
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java` — 12+ project states
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/TradeController.java` — P2P trade: PENDING→ACCEPTED/REJECTED→COMPLETED

### Frontend - i18n
- `oaiss-chain-frontend/src/i18n/` — zh-CN and en-US locale files

### Infrastructure
- `scripts/login-test.sh` — Pattern for API test scripts
- Phase 5 test scripts (`scripts/sign-test.sh` etc.) — Reference for assertion helpers

### Requirements
- `.planning/REQUIREMENTS.md` — AOP-01~04, EDGE-01~06, BUG-01~03

</canonical_refs>

<code_context>
## Existing Code Insights

### AOP Usage Analysis
| Annotation | Applied In Code | Testable via API |
|------------|----------------|-----------------|
| @AuditLog | NONE (0 controllers) | Needs temporary annotation |
| @RateLimit | NONE (0 controllers) | Needs temporary annotation |
| @DataIsolation | DigitalSignatureController (3 endpoints: sign, encrypt, decrypt) | Directly testable |
| @DistributedLock | NONE (0 controllers/services) | Needs temporary annotation |

### Role Access Matrix (for EDGE-01)
| Controller | ENTERPRISE | REVIEWER | THIRD_PARTY | ADMIN | AUTHENTICATOR |
|------------|-----------|----------|-------------|-------|---------------|
| CarbonController (create/submit) | YES | NO | NO | NO | NO |
| CarbonController (review/approve) | NO | YES | NO | NO | NO |
| CarbonController (list all) | NO | YES(+) | YES(+) | YES(+) | YES(+) |
| DoubleAuctionController (orders) | YES | NO | NO | NO | NO |
| DoubleAuctionController (match) | NO | NO | NO | YES | NO |
| TradeController (P2P) | YES | NO | NO | NO | NO |
| CreditScoreController (view) | YES | NO | NO | NO | NO |
| CreditScoreController (admin) | NO | YES(+) | NO | YES | NO |
| AdminController (all) | NO | NO | NO | YES | NO |
| ThirdPartyController (all) | NO | NO | YES | NO | NO |
| BlockchainController (status) | NO | NO | NO | YES | YES |
| DigitalSignatureController (keypair) | YES(+) | YES(+) | YES(+) | NO | NO |

### Report State Machine (for EDGE-02)
```
DRAFT --submit--> SUBMITTED
SUBMITTED --review--> UNDER_REVIEW
UNDER_REVIEW --approve--> APPROVED
UNDER_REVIEW --reject--> REJECTED
REJECTED --resubmit?--> SUBMITTED (verify if allowed)

Illegal transitions to test:
- DRAFT → APPROVED (skip review)
- DRAFT → UNDER_REVIEW (skip submit)
- DRAFT → REJECTED (skip submit)
- SUBMITTED → APPROVED (skip review)
- APPROVED → SUBMITTED (no backwards)
- REJECTED → APPROVED (no direct path)
```

### Security Config Issues
- **SEC-03**: Lines 72-77 permitAll swagger endpoints unconditionally. Docker profile disables via springdoc config, but default profile is open.
- **SEC-04**: Line 44 has `http://localhost:5173` as default. Line 99 applies to `/**`. Docker profile requires env var but default falls back to localhost.

### Known Bugs from Prior Phases
- NonUniqueResultException in DigitalSignatureService when user has multiple RSA keypairs (one revoked + one active)
- ADMIN-02/03: No create/edit user endpoints (code gap, NOT a bug)
- TP-02: No dedicated trade audit endpoint (code gap, NOT a bug)

</code_context>

<specifics>
## Specific Ideas

- AOP test script needs careful orchestration: modify code → compile → restart → test → revert → compile → restart
- For @RateLimit testing, set very low limit (3 requests/60s) to make testing fast
- For @DistributedLock, can test by sending two curl requests simultaneously (`&` background)
- EDGE-01 needs 6 JWT tokens (one per role) — extract all at script start
- EDGE-02 state machine tests need seed data: create entities in known states first
- EDGE-04 pagination needs at least some data — run after Phases 2-5 data exists
- EDGE-05 input validation tests should not corrupt existing data — use unique test entities
- SEC-03 fix: simplest approach is adding `.requestMatchers("/swagger-ui/**").authenticated()` to SecurityConfig for default profile, or using `@Profile("!docker")` conditional
- SEC-04 fix: change `@Value` default to empty list or remove default entirely
- Backend restart is needed after SEC fixes — plan accordingly

</specifics>

<deferred>
## Deferred Ideas

- EDGE-06 i18n deep testing — M4 deferred item, basic verification only
- AOP performance benchmarks — verify correctness, not performance
- Rate limit bypass via header spoofing — security hardening, not functional testing
- Distributed lock TTL expiry behavior — edge case of edge cases
- Comprehensive security audit of all 14 controllers — separate initiative

</deferred>

---

*Phase: 06-cross-cutting-edge-cases*
*Context gathered: 2026-05-10*
