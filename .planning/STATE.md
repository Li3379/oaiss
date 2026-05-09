# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-05-08)

**Core value:** 所有五种角色的核心业务流程在真实后端数据下端到端跑通，系统功能完整可用
**Current focus:** Phase 3: Carbon Coin & Trading Engine

## Current Position

Phase: 3 of 6 (Carbon Coin & Trading Engine)
Plan: 3 of 3 in current phase
Status: Ready to execute
Last activity: 2026-05-09 -- Phase 3 planning complete (3 plans, 2 waves)

Progress: [████████░░] 42%

## Performance Metrics

**Velocity:**
- Total plans completed: 5
- Average duration: ~13 minutes
- Total execution time: 1.4 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Environment Setup | 2/2 | 0.5h | 0.25h |
| 2. Carbon Report Lifecycle | 3/3 | 0.9h | 0.18h |

**Recent Trend:**
- Last 5 plans: 01-01 (success), 01-02 (success), 02-01 (success), 02-02 (success), 02-03 (success)
- Trend: On track -- Phase 2 complete

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

### Pending Todos

None yet.

### Blockers/Concerns

- ~~V3__test_seed_data.sql Flyway migration does not exist yet~~ -- RESOLVED in 01-01
- ~~docker-compose.infra.yml does not exist yet~~ -- RESOLVED in 01-01
- ~~.env file may not exist~~ -- RESOLVED (exists with working defaults)
- VERIFIER/CERTIFIER roles referenced in @PreAuthorize but not in UserTypeEnum -- may block Phase 4
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
Stopped at: Phase 3 planned (3 plans, 2 waves); ready to execute
Resume file: .planning/phases/03-trading-engine/03-01-PLAN.md
