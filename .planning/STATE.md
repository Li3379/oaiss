# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-08)

**Core value:** 所有五种角色的核心业务流程在真实后端数据下端到端跑通，系统功能完整可用
**Current focus:** Phase 6: Cross-Cutting & Edge Cases

## Current Position

Phase: 6 of 6 (Cross-Cutting & Edge Cases)
Plan: 0 of 3 in current phase
Status: Ready to execute — 3 plans in 3 waves
Last activity: 2026-05-10 -- Phase 6 planning complete (3 plans: bugfix, aop, edge)

Progress: [█████████████░] 83%

## Performance Metrics

**Velocity:**
- Total plans completed: 10
- Average duration: ~12 minutes
- Total execution time: 2.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Environment Setup | 2/2 | 0.5h | 0.25h |
| 2. Carbon Report Lifecycle | 3/3 | 0.9h | 0.18h |
| 3. Carbon Coin & Trading | 3/3 | 0.3h | 0.10h |
| 4. Projects & Credit | 2/2 | 0.3h | 0.15h |
| 5. Supporting Domains | 7/7 | 0.5h | 0.07h |

**Recent Trend:**
- Last 5 plans: 03-03 (success), 04-01 (success), 04-02 (success), 05-01~05-07 (all success)
- Trend: On track -- Phase 5 complete

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table and per-phase CONTEXT.md files.
Recent decisions affecting current work:

- [Roadmap]: 6 phases derived from 84 v1 requirements; granularity = standard
- [Roadmap]: Phase 5 can run in parallel with Phase 3/4 (only depends on Phase 1)
- [Roadmap]: VERIFIER/CERTIFIER role gap deferred to Phase 4 investigation
- [Roadmap]: CON-01/02/03 concurrency issues deferred to v2; test trading sequentially only
- [Phase 1]: Minimal seed — no business data pre-seeded, each phase creates its own via UI
- [Phase 1]: Infrastructure Docker + local dev — MySQL/Redis/MinIO in Docker, backend/frontend local
- [Phase 1]: Create docker-compose.infra.yml, pre-fill .env with defaults
- [Phase 1]: JWT default 45min expiry, one Incognito window per role, skip refresh testing
- [Phase 1]: Automated health check script, fast-fail, includes Flyway verification
- [01-01]: Host MySQL on port 3306 cannot be stopped (no admin); Docker MySQL runs on 3307
- [01-01]: V3 applied manually to host MySQL + registered in flyway_schema_history
- [01-01]: Fixed Redis health check: `&>/dev/null` before pipe suppressed output
- [01-02]: All 7 seed accounts pass login/verify/logout/blacklist cycle; post-logout returns code 2000 (app custom code)
- [Phase 2]: 12 decisions captured via discuss-phase — cascading side effects wiring, state machine fixes, field mapping bug
- [02-01]: Fix frontend carbon.ts dual bug + create enterprise CRUD test script -- COMPLETE 2026-05-09
- [02-02]: Wire cascading side effects (CreditScore/EmissionRating/Blockchain) into CarbonService.reviewReport() -- COMPLETE 2026-05-09
- [02-03]: Fix 3 bugs (pipefail, JSON escaping, rating_year truncation), verify all 13 CARB tests pass -- COMPLETE 2026-05-09
- [Phase 3]: TradeController and DoubleAuctionController are SEPARATE subsystems — no shared matching engine (resolves TRADE-11)
- [Phase 3]: Frontend trade.ts has TWO bugs: line 5 `carbonAmount`→`quantity`, line 6 `price`→`unitPrice`
- [Phase 3]: All 3 enterprises have carbon coin balance=10000, carbonTradable: 38000/55000/50000
- [Phase 3]: DoubleAuctionService.executeMatching() is `synchronized` — sequential testing only
- [Phase 3]: No API endpoint exposes enterprise quota fields (carbonTradable, carbonQuota, carbonUsed) — use direct DB queries for verification
- [03-01]: Carbon coin balance returns decimal format (10000.00) — strip decimals with cut -d. -f1 for bash arithmetic
- [03-02]: Backend connects to host MySQL (port 3306), NOT Docker MySQL (port 3307) — use `mysql -h 127.0.0.1 -P 3306` for all DB queries
- [03-02]: Auction order status values: PENDING=0, PARTIALLY_MATCHED=1, FULLY_MATCHED=2, CANCELLED=3
- [03-02]: `/auction/results` requires ENTERPRISE role, not ADMIN
- [03-02]: Test data reset required before auction tests to avoid stale order interference
- [03-03]: P2P trade lifecycle: seller creates (PENDING) → buyer confirms (PENDING→PROCESSING→COMPLETED atomically)
- [03-03]: P2P settlement: seller loses carbonTradable, buyer gains carbonTradable + carbonQuota
- [03-03]: TradeController and DoubleAuctionController confirmed as SEPARATE, INDEPENDENT subsystems (no shared matching engine)
- [04-01]: VERIFIER/CERTIFIER gap confirmed: @PreAuthorize('VERIFIER'/'CERTIFIER') but UserTypeEnum has AUTHENTICATOR(5). ADMIN workaround works.
- [04-01]: Project lifecycle: DRAFT(0)→PENDING(1)→APPROVED(2)→IMPLEMENTING(3)→TERMINATED(5), REJECTED(6). Invalid transitions → BusinessException(3003)
- [04-01]: Certification: certStatus NONE(0)→PENDING(1)→CERTIFIED(2), verificationStatus NONE(0)→PENDING(1)→VERIFIED(2). verifyProject() auto-issues credits = verifiedReduction
- [04-02]: check-permission/{enterpriseId} requires JWT authentication (not public as controller suggests)
- [04-02]: Credit score levels verified: EXCELLENT(80-100), GOOD(60-79), WARNING(40-59), DANGER(20-39), FROZEN(0-19)
- [04-02]: tradeRestricted activates at score<40 (DANGER), accountFrozen activates at score<20 (FROZEN)
- [04-02]: addBonusPoints caps score at 100
- [Phase 5]: 20 decisions captured via discuss-phase — 7 sub-domain scripts, 3 code gaps recorded
- [05-discuss]: 7 independent test scripts (one per sub-domain) replace ROADMAP 4-plan structure
- [05-discuss]: ADMIN-02/03 (create/edit user) are code gaps — no backend endpoints, no frontend UI
- [05-discuss]: Blockchain mock verification: API 200 + field existence + format validation (txMock_ prefix, 0x blockHash)
- [05-discuss]: TP-02 (trade audit) partially covered — no dedicated endpoint, /carbon-reports as proxy
- [05-discuss]: Emission factors hardcoded in CachePreloadService — test current values only
- [05-exec]: NonUniqueResultException in DigitalSignatureService when user has multiple keypairs (revoked+active) — DELETE old keypairs before testing
- [05-exec]: MinIO bucket 'oaiss-chain' was missing — created via mc mb
- [05-exec]: rsa_key_pair table is singular (not rsa_key_pairs)
- [05-exec]: DB password is 123456 (not root)
- [05-exec]: ADMIN-02/03 confirmed as code gaps (no create/edit user endpoints)
- [05-exec]: TP-02 confirmed as partial coverage (no dedicated trade audit endpoint)

### Pending Todos

None yet.

### Blockers/Concerns

- ~~V3__test_seed_data.sql Flyway migration does not exist yet~~ -- RESOLVED in 01-01
- ~~docker-compose.infra.yml does not exist yet~~ -- RESOLVED in 01-01
- ~~.env file may not exist~~ -- RESOLVED (exists with working defaults)
- VERIFIER/CERTIFIER roles referenced in @PreAuthorize but not in UserTypeEnum -- CONFIRMED in Phase 4, ADMIN workaround verified
- ~~TradeController vs DoubleAuctionController relationship unclear~~ -- RESOLVED: separate subsystems (Phase 3 research)
- Host MySQL on port 3306 blocks Docker oaiss-mysql on same port; workaround: Docker on 3307

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Concurrency | CON-01/02/03 -- synchronized + no @Version | v2 | 2026-05-08 |
| Security | SEC-01 -- RSA private key in DB | v2 | 2026-05-08 |
| Security | SEC-02 -- CSRF protection | v2 | 2026-05-08 |
| Performance | PERF-01~04 -- N+1, Redis KEYS, async cache | v2 | 2026-05-08 |

## Session Continuity

Last session: 2026-05-09
Stopped at: Phase 5 complete (7/7 scripts, 81/81 tests passed); ready for Phase 6 planning
Resume file: /gsd-discuss-phase 6 or /gsd-plan-phase 6
