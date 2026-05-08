# Phase 1 Discussion Log

**Date:** 2026-05-08
**Phase:** Environment Setup & Auth Baseline

---

## Area 1: V3 Test Seed Data Content

**Questions asked:** 4

| # | Question | Options | Selection |
|---|----------|---------|-----------|
| 1 | What should V3 add to V2? | Full pre-seed, **Minimal seed**, Phased pre-seed | Minimal seed |
| 2 | What specifically should V3 include? | **Users + schema gaps only**, Users + account setup, Users + infra setup | Users + schema gaps only |
| 3 | Should V3 fix schema gaps like missing AUTHENTICATOR in user_type_list? | **Full gap check**, User only | Full gap check |
| 4 | Should V3 also add VERIFIER/CERTIFIER roles? | **Fix AUTHENTICATOR only**, Fix all role gaps, Skip role fixes | Fix AUTHENTICATOR only |

**Key decisions:**
- Minimal seed: no business data pre-seeded, each phase creates its own via UI
- enterprise003 added as third enterprise with equal starting conditions
- AUTHENTICATOR(5) gap fixed, VERIFIER/CERTIFIER deferred to Phase 4
- Missing tables (rsa_key_pairs, operation_log, emission_rating) checked and fixed in V3

---

## Area 2: Docker vs Local Dev Stack

**Questions asked:** 4

| # | Question | Options | Selection |
|---|----------|---------|-----------|
| 1 | What dev stack approach for Phase 1 testing? | Full Docker, **Infrastructure Docker + local dev**, All local | Infrastructure Docker + local dev |
| 2 | How to handle .env configuration? | **Pre-filled .env**, Manual .env, Template only | Pre-filled .env |
| 3 | How to start infrastructure services? | **Separate infra compose file**, Modify main compose, Manual service names | Separate infra compose file |
| 4 | Docker Desktop status check as entry criteria? | **Check and prompt**, Assume running, Auto-start | Check and prompt |

**Key decisions:**
- MySQL/Redis/MinIO in Docker, backend via `mvn spring-boot:run`, frontend via `npm run dev`
- Create `docker-compose.infra.yml` — original `docker-compose.yml` untouched
- Pre-fill `.env` with working defaults (not just template)
- Docker Desktop running check as entry criteria

**Rationale:** Backend Docker profile disables Swagger (ENV-04 requires it). Local dev with default profile enables Swagger UI.

---

## Area 3: JWT Expiration & Test Session Strategy

**Questions asked:** 4

| # | Question | Options | Selection |
|---|----------|---------|-----------|
| 1 | JWT token expiry handling during testing? | **Short expiry + manual re-login**, Extend expiry, Auto-refresh | Short expiry + manual re-login |
| 2 | How to manage multi-role sessions for 6 accounts? | **One window per role**, Same window switching, Different browsers | One window per role |
| 3 | Should Phase 1 test JWT refresh mechanism? | Verify refresh, **Skip refresh testing**, Full JWT lifecycle | Skip refresh testing |
| 4 | Verify logout blacklists token? | **Verify token失效**, Normal logout only, Full logout flow | Verify token失效 |

**Key decisions:**
- Keep default JWT expiration (45 min), re-login when expired
- Each role in its own Incognito/Privacy window
- Skip ENV-09 (refresh) — focus on login → access → logout
- Verify ENV-10 (logout) — after logout, old token returns 401

---

## Area 4: Environment Health Verification

**Questions asked:** 4

| # | Question | Options | Selection |
|---|----------|---------|-----------|
| 1 | Health check approach? | **Automated script**, Manual checklist, Minimal check | Automated script |
| 2 | Which services to verify? | **All 5 services**, Application only, Infrastructure only | All 5 services |
| 3 | Behavior on service failure? | **Fast fail**, Warn and continue, Auto-recover | Fast fail |
| 4 | Include Flyway migration verification? | **Include Flyway check**, Connectivity only, Full data verification | Include Flyway check |

**Key decisions:**
- Automated health check script (bash/PowerShell)
- Checks: MySQL (3306 + tables), Redis (PING), MinIO (9001), Backend (Swagger), Frontend (5173)
- Fast-fail: exit non-zero on first failure with specific error message
- Includes Flyway verification: 21 tables + V3 seed data

---

## Summary

**Total questions:** 16
**Areas discussed:** 4
**Decisions captured:** 16 (D-01 through D-16)
**Deferred items:** 4 (JWT refresh, VERIFIER/CERTIFIER roles, full Docker mode, production hardening)

---

*Log created: 2026-05-08*
