# Roadmap: OAISS CHAIN Manual Testing

## Overview

This roadmap drives OAISS CHAIN from "functionally complete but untested" to "production-ready for continued blockchain development." Testing proceeds in dependency order: environment and authentication first, then the central carbon report lifecycle, then trading, then projects and credit scoring, then supporting domains, and finally cross-cutting edge cases. Each phase delivers a verifiable slice of the platform. The entire initiative covers 84 v1 requirements across 6 phases, testing 16 REST controllers, 19 services, and 65+ business flows for 5 user roles.

## Phases

- [x] **Phase 1: Environment Setup & Auth Baseline** - Docker stack healthy, all 6 seed accounts login-verified, JWT and role routing validated *(completed 2026-05-08)*
- [x] **Phase 2: Carbon Report Lifecycle** - Full report CRUD, submission, review (approve/reject), cascading side effects (credit score, emission rating, blockchain), cross-role access control *(completed 2026-05-09)*
- [ ] **Phase 3: Carbon Coin & Trading Engine** - Carbon coin accounts, double auction buy/sell/match, P2P trade lifecycle, settlement correctness, trade controller relationship resolved
- [ ] **Phase 4: Carbon Neutral Projects & Credit Scoring** - Project lifecycle (12+ states), VERIFIER/CERTIFIER role gap resolved, credit score levels, trade restriction enforcement
- [ ] **Phase 5: Supporting Domains** - Digital signatures, file upload/download, emission ratings, blockchain explorer, admin user management, third-party monitoring, search
- [ ] **Phase 6: Cross-Cutting & Edge Cases** - AOP concerns verified, cross-role access negative tests, state machine violations, financial integrity, input validation, bug fixes (SEC-03/04)

## Phase Details

### Phase 1: Environment Setup & Auth Baseline
**Goal**: All infrastructure services are healthy, all 6 seed accounts can log in and reach their correct role home pages, JWT lifecycle (issue/refresh/revoke) works end-to-end.
**Depends on**: Nothing (first phase)
**Requirements**: ENV-01, ENV-02, ENV-03, ENV-04, ENV-05, ENV-06, ENV-07, ENV-08, ENV-09, ENV-10
**Success Criteria** (what must be TRUE):
  1. `docker-compose up` starts MySQL, Redis, MinIO, backend, and frontend; all health checks pass; Swagger UI loads at `/api/v1/swagger-ui.html`; frontend loads at `localhost:5173`
  2. Flyway migrations (V1 schema, V2 seed, V3 test data) execute successfully; all 21 tables exist with expected row counts
  3. All 6 seed accounts (admin, enterprise001, enterprise002, reviewer001, thirdparty001, authenticator001) log in with password `admin123` and each lands on the correct role home page
  4. JWT access token is obtained on login, `Authorization: Bearer <token>` works on protected endpoints, refresh endpoint returns a new token pair, and logout blacklists the token
**Entry Criteria**: Docker Desktop running; `.env` file created from `.env.example`; V3 Flyway migration script written
**Exit Criteria**: All 4 success criteria verified; any environment issues resolved; login matrix documented
**Risk Mitigation**:
  - **JWT expiration false failures**: Re-login every 45 minutes; keep DevTools Network tab open
  - **Role switching stale state**: One role per Incognito window; always use logout button
  - **V3 migration gap**: Create `V3__test_seed_data.sql` before testing starts
  - **Flyway schema drift**: Verify `rsa_key_pairs` table exists after migration; if not, add to V3
**Estimated Effort**: 2-3 hours (infrastructure + auth smoke test)
**Plans**: 3 plans in 2 waves in 2 waves

Plans:
- [x] 01-01-PLAN.md -- Docker infra compose, V3 migration (AUTHENTICATOR enum + enterprise003), health check script *(Wave 1)* -- COMPLETE 2026-05-08
- [x] 01-02-PLAN.md -- 7-account login test script, JWT access/logout blacklist verification, browser role-home routing checkpoint *(Wave 2)* -- COMPLETE 2026-05-08

### Phase 2: Carbon Report Lifecycle
**Goal**: The central business flow (enterprise creates report, submits for review, reviewer approves or rejects, cascading side effects fire) works end-to-end across both approval and rejection paths.
**Depends on**: Phase 1
**Requirements**: CARB-01, CARB-02, CARB-03, CARB-04, CARB-05, CARB-06, CARB-07, CARB-08, CARB-09, CARB-10, CARB-11, CARB-12, CARB-13
**Success Criteria** (what must be TRUE):
  1. Enterprise user can create a carbon report (form fill + file upload to MinIO), view it in a paginated list, view its detail, and submit it (DRAFT -> SUBMITTED)
  2. Reviewer can view the submitted report in the review queue, approve one report and reject another with a reason; status transitions to APPROVED or REJECTED correctly
  3. After approval, credit score updates, emission rating recalculates, and a blockchain record is created (mock mode) -- verified via DB inspection or API responses
  4. Illegal state transitions are rejected (e.g., DRAFT directly to APPROVED returns an error)
  5. Enterprise A cannot see Enterprise B's draft reports (data isolation); enterprise user cannot access reviewer endpoints (cross-role access control)
**Entry Criteria**: Phase 1 complete; MinIO container healthy; at least 2 enterprise accounts available
**Exit Criteria**: All 13 CARB requirements verified; approval and rejection paths both tested; cascading effects confirmed
**Risk Mitigation**:
  - **DB state pollution**: Use timestamped unique report names; reset via `docker-compose down -v` if stuck
  - **MinIO connection failures**: Verify MinIO container health before file upload tests
  - **JWT expiration mid-test**: Re-login before starting this phase; keep token fresh
**Estimated Effort**: 4-6 hours (full lifecycle across 2 enterprises, approval + rejection paths)
**Plans**: 3 plans in 3 waves

Plans:
- [x] 02-01-PLAN.md -- Fix carbon.ts field mapping bug, create API test script for enterprise report CRUD (CARB-01/02/03/04) + MinIO file upload *(Wave 1)* -- COMPLETE 2026-05-09
- [x] 02-02-PLAN.md -- Wire cascading side effects into CarbonService.reviewReport(), extend test script for review flows + state machine + cross-role access (CARB-05/06/07/11/12) *(Wave 2)* -- COMPLETE 2026-05-09
- [x] 02-03-PLAN.md -- Execute full test suite, verify side effects at runtime, fix failures, human checkpoint (CARB-08/09/10/13) *(Wave 3)* -- COMPLETE 2026-05-09

### Phase 3: Carbon Coin & Trading Engine
**Goal**: Carbon coin accounts show correct balances, double auction buy/sell/match works end-to-end with correct settlement, P2P trade lifecycle (create/accept/reject/settle) works, and the relationship between TradeController and DoubleAuctionController is resolved.
**Depends on**: Phase 1
**Requirements**: COIN-01, COIN-02, COIN-03, COIN-04, COIN-05, TRADE-01, TRADE-02, TRADE-03, TRADE-04, TRADE-05, TRADE-06, TRADE-07, TRADE-08, TRADE-09, TRADE-10, TRADE-11, TRADE-12, TRADE-13
**Success Criteria** (what must be TRUE):
  1. Enterprise user can view carbon coin balance and paginated transaction history; transfer coins to another enterprise; both balances update atomically
  2. Enterprise can place buy and sell orders; admin triggers matching; matched orders create trade records with correct balance updates on both sides
  3. P2P trade can be created by Enterprise A, accepted by Enterprise B, and both accounts settle correctly; rejection path also works
  4. Order status transitions (PENDING -> FULLY_MATCHED / PARTIALLY_MATCHED / CANCELLED) are correct; insufficient balance/quota orders are rejected
  5. The relationship between `TradeController` auction endpoints and `DoubleAuctionController` is documented (shared or separate matching engine)
**Entry Criteria**: Phase 1 complete; enterprise accounts have carbon coin balances (from V3 seed data or Phase 2 approved reports)
**Exit Criteria**: All 18 COIN+TRADE requirements verified; settlement financial integrity confirmed; TradeController/DoubleAuctionController relationship documented
**Risk Mitigation**:
  - **Trading race conditions**: Test trading sequentially, never concurrently; never open two tabs placing orders for the same enterprise
  - **Concurrent test execution**: Known issue CON-01/02/03 -- do not test concurrency; accept single-threaded behavior
  - **N+1 query performance**: If matching is slow, note as PERF-01 but do not block
  - **Financial integrity**: After every trade, verify both account balances match expected values via DB or API
**Estimated Effort**: 6-8 hours (dual trading paths, settlement verification, relationship investigation)
**Plans**: 3 plans in 2 waves

Plans:

**Wave 1** *(independent — can run in parallel)*
- [ ] 03-01-PLAN.md -- Carbon coin balance, history, transfer, insufficient balance rejection (COIN-01~05) *(Wave 1)*
- [ ] 03-02-PLAN.md -- Double auction buy/sell orders, admin matching, settlement, status transitions (TRADE-01~06, 12, 13) *(Wave 1)*

**Wave 2** *(blocked on Wave 1 — 03-03 depends on 03-02)*
- [ ] 03-03-PLAN.md -- P2P trade lifecycle + trade.ts fix + controller relationship documentation (TRADE-07~11) *(Wave 2, depends on 03-02)*

**Cross-cutting constraints:**
- TradeController (P2P) and DoubleAuctionController (auction) are separate subsystems — do NOT share matching engine
- Sequential testing only (CON-01/02/03 deferred to v2)
- After every trade, verify both account balances via direct DB query (`docker exec oaiss-mysql mysql`)

### Phase 4: Carbon Neutral Projects & Credit Scoring
**Goal**: Carbon neutral project lifecycle works through all states (including verification and certification), credit score levels are correctly enforced (WARNING at 40, FROZEN at 20), and trading restrictions based on credit level are verified.
**Depends on**: Phase 2 (approved reports provide data for projects and credit scoring)
**Requirements**: PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05, CRED-01, CRED-02, CRED-03, CRED-04, CRED-05
**Success Criteria** (what must be TRUE):
  1. Enterprise user can create a carbon neutral project, view project list (paginated, filtered), and view project detail
  2. Project status transitions work through all 12+ states; VERIFIER/CERTIFIER role gap is resolved (either roles exist or annotations are fixed)
  3. Enterprise user can view credit score and score change history; score levels (WARNING at 40, FROZEN at 20) are correctly evaluated
  4. When credit score drops below WARNING threshold, trading operations are restricted; when below FROZEN, account is effectively frozen
**Entry Criteria**: Phase 2 complete (approved reports exist for credit scoring); VERIFIER/CERTIFIER role issue investigated
**Exit Criteria**: All 10 PROJ+CRED requirements verified; role gap either fixed or documented as known blocker with workaround
**Risk Mitigation**:
  - **VERIFIER/CERTIFIER role mismatch**: `@PreAuthorize` references roles not in `UserTypeEnum`. Investigate early; if 403 on verification/certification flows, either add roles to enum or document as known issue
  - **Hardcoded emission factors**: Credit scoring uses `CachePreloadService` constants. Do not expect runtime factor changes; test as read-only
  - **Phase dependency on Phase 2**: If Phase 2 approval flow has bugs, Phase 4 credit scoring cannot be tested. Resolve Phase 2 blockers first
**Estimated Effort**: 4-5 hours (project lifecycle + credit scoring + role gap investigation)
**Plans**: 3 plans in 2 waves

Plans:
- [ ] 04-01: Carbon neutral project CRUD, status transitions, VERIFIER/CERTIFIER role gap investigation
- [ ] 04-02: Credit score viewing, level evaluation, trade restriction enforcement

### Phase 5: Supporting Domains
**Goal**: All secondary platform features (digital signatures, file management, emission ratings, blockchain explorer, admin user management, third-party monitoring, search) are verified as functional.
**Depends on**: Phase 1
**Requirements**: SIGN-01, SIGN-02, SIGN-03, FILE-01, FILE-02, FILE-03, EMIT-01, EMIT-02, EMIT-03, BLOCK-01, BLOCK-02, BLOCK-03, ADMIN-01, ADMIN-02, ADMIN-03, ADMIN-04, ADMIN-05, TP-01, TP-02, SRCH-01
**Success Criteria** (what must be TRUE):
  1. RSA key pair generation, data signing, and signature verification all work end-to-end
  2. File upload to MinIO and download verification work; MinIO console accessible at `localhost:9001`
  3. Emission data and emission ratings are viewable; emission factors (hardcoded in CachePreloadService) load correctly on startup
  4. Blockchain records are viewable in mock mode; block explorer shows structurally correct data (proper tx hash format, block number format)
  5. Admin can list, create, edit, enable/disable users and modify system configuration; third-party can view monitoring data and trade audit records; cross-entity search returns correct results
**Entry Criteria**: Phase 1 complete; all Docker services (especially MinIO) healthy
**Exit Criteria**: All 20 supporting domain requirements verified; no untested controllers remaining
**Risk Mitigation**:
  - **MinIO connectivity**: Verify MinIO container health before file tests; check bucket creation
  - **Blockchain mock correctness**: Verify mock return data has proper tx hash and block number formats so downstream consumers do not break
  - **Hardcoded emission factors**: Accept that config changes require backend restart; test current values only
  - **Independent testing order**: These domains can be tested in any order; if one blocks, skip and continue
**Estimated Effort**: 5-7 hours (7 sub-domains, some can be tested in parallel if using multiple browser windows)
**Plans**: 3 plans in 2 waves

Plans:
- [ ] 05-01: Digital signatures (generate, sign, verify) + file upload/download via MinIO
- [ ] 05-02: Emission ratings + blockchain explorer (mock mode)
- [ ] 05-03: Admin user management + system configuration
- [ ] 05-04: Third-party monitoring + cross-entity search

### Phase 6: Cross-Cutting & Edge Cases
**Goal**: All AOP cross-cutting concerns are verified, comprehensive edge case and negative testing is complete, critical security fixes (SEC-03, SEC-04) are applied, and all discovered bugs are resolved.
**Depends on**: Phases 2, 3, 4, 5 (all functional flows must work before edge case testing)
**Requirements**: AOP-01, AOP-02, AOP-03, AOP-04, EDGE-01, EDGE-02, EDGE-03, EDGE-04, EDGE-05, EDGE-06, BUG-01, BUG-02, BUG-03
**Success Criteria** (what must be TRUE):
  1. `@AuditLog` records operations to the `operation_log` table with correct details; `@RateLimit` rejects requests exceeding threshold (fail-open when Redis is down)
  2. `@DataIsolation` prevents Enterprise A from accessing Enterprise B's data across all endpoints; `@DistributedLock` serializes concurrent operations
  3. Cross-role access control blocks all 6 role-pair combinations from unauthorized endpoints; state machine violations (7 illegal transitions) return proper errors
  4. Financial integrity holds: trade amounts and quantities are consistent; pagination works at boundaries (empty, single, full, overflow); input validation rejects negative prices, zero quantities, oversized text
  5. SEC-03 (Swagger production exposure) and SEC-04 (CORS default localhost value) are fixed; all bugs found during testing are resolved
**Entry Criteria**: Phases 2-5 complete; all functional flows verified; bug list compiled from earlier phases
**Exit Criteria**: All 13 cross-cutting + edge case requirements verified; SEC-03/04 fixed; all discovered bugs resolved or documented as known issues
**Risk Mitigation**:
  - **Rate limit thresholds unknown**: Code-level verification of `RateLimitAspect` needed to design effective test cases
  - **Distributed lock key patterns**: Verify lock key generation logic before testing
  - **i18n edge cases**: Switch language in frontend; verify error messages display in correct locale
  - **Bug fix regression**: After each fix, re-test the affected flow to ensure no new issues
**Estimated Effort**: 4-6 hours (AOP verification + systematic edge case matrix + bug fixes)
**Plans**: 3 plans in 2 waves

Plans:
- [ ] 06-01: AOP concerns verification (AuditLog, RateLimit, DataIsolation, DistributedLock)
- [ ] 06-02: Cross-role access control matrix + state machine violation tests
- [ ] 06-03: Financial integrity, pagination boundaries, input validation, i18n
- [ ] 06-04: Bug fixes (SEC-03, SEC-04, discovered bugs) + regression verification

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6
Phase 5 can run in parallel with Phase 3/4 since it only depends on Phase 1.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Environment Setup & Auth Baseline | 2/2 | Complete | 2026-05-08 |
| 2. Carbon Report Lifecycle | 3/3 | Complete | 2026-05-09 |
| 3. Carbon Coin & Trading Engine | 3/3 | Planned | - |
| 4. Carbon Neutral Projects & Credit Scoring | 0/2 | Not started | - |
| 5. Supporting Domains | 0/4 | Not started | - |
| 6. Cross-Cutting & Edge Cases | 0/4 | Not started | - |

**Total Requirements:** 84 v1 requirements across 6 phases
**Estimated Total Effort:** 25-36 hours of manual testing

## Known Issues & Deferred Items

| Item | Status | Impact |
|------|--------|--------|
| CON-01/02/03 (concurrency) | Deferred to v2 | Do not test concurrent trading; accept single-threaded behavior |
| SEC-01 (RSA private key in DB) | Deferred to v2 | Low risk in dev/test environment |
| SEC-02 (CSRF protection) | Deferred to v2 | CSRF currently disabled; acceptable for testing |
| SEC-03 (Swagger production exposure) | Fix in Phase 6 | Must fix before production readiness |
| SEC-04 (CORS localhost default) | Fix in Phase 6 | Must fix before production readiness |
| VERIFIER/CERTIFIER role gap | Investigate in Phase 4 | May block project verification/certification flows |
| TradeController/DoubleAuctionController | Investigate in Phase 3 | Relationship unclear; needs code-level resolution |
| V3 Flyway migration | Create in Phase 1 | Required before any testing begins |

---
*Roadmap created: 2026-05-08*
*Based on: REQUIREMENTS.md (84 v1 requirements), PROJECT.md, research/SUMMARY.md*
*Granularity: standard (6 phases, derived from requirement categories)*
