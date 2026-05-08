# Phase 1: Environment Setup & Auth Baseline - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning

<domain>
## Phase Boundary

All infrastructure services (MySQL, Redis, MinIO) are healthy, all 6 seed accounts can log in and reach their correct role home pages, JWT lifecycle (issue/revoke) works end-to-end, and automated health verification passes. This phase delivers a verified development environment ready for business flow testing.

</domain>

<decisions>
## Implementation Decisions

### V3 Test Seed Data Content
- **D-01:** Minimal seed strategy — each phase creates its own business data via UI during testing. V3 does NOT pre-seed carbon reports, auction orders, or projects.
- **D-02:** V3 includes: enterprise003 user + enterprise record + carbon coin account + credit score. All three enterprises have equal starting conditions.
- **D-03:** V3 adds AUTHENTICATOR(5) to user_type_list enum table. This is the only role gap fix — VERIFIER/CERTIFIER gaps deferred to Phase 4.
- **D-04:** V3 checks and fixes missing tables: `rsa_key_pairs`, `operation_log`, `emission_rating`. If any are missing from V1/V2, V3 creates them.
- **D-05:** No pre-seeded business data — no carbon reports, no auction orders, no carbon neutral projects. Each test phase creates its own data.

### Dev Stack Configuration
- **D-06:** Infrastructure Docker + local dev approach. MySQL, Redis, MinIO run in Docker. Backend runs via `mvn spring-boot:run` (default profile, Swagger enabled). Frontend runs via `npm run dev`.
- **D-07:** Create `docker-compose.infra.yml` for infrastructure-only startup. Original `docker-compose.yml` remains untouched for full-stack Docker mode.
- **D-08:** Create `.env` file with working defaults (not just template). Pre-fill DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO keys, CORS origins.
- **D-09:** Docker Desktop running status is an entry criteria for the test plan. Health check script verifies it before proceeding.

### JWT Expiration & Test Session Strategy
- **D-10:** Keep JWT at default expiration (45 minutes). Re-login when token expires during testing. No expiration time changes.
- **D-11:** Each role gets its own Incognito/Privacy browser window. Avoids role state confusion. Always use logout button when switching.
- **D-12:** Skip JWT refresh (ENV-09) testing in Phase 1. Focus on login → access → logout. Refresh mechanism deferred to later phase if needed.
- **D-13:** Verify logout blacklists the token — after logout, attempt access with old token, expect 401 response.

### Environment Health Verification
- **D-14:** Automated health check script (bash/PowerShell) that verifies all 5 services: MySQL (3306 + table count), Redis (PING), MinIO (9001 console), Backend (Swagger UI), Frontend (5173).
- **D-15:** Fast-fail behavior — script prints specific error and exits non-zero on first failure. Does not continue to subsequent checks.
- **D-16:** Health check includes Flyway migration verification: 21 tables exist + V3 seed data present.

### Claude's Discretion
- Exact health check script implementation (bash vs PowerShell, output format)
- V3 migration SQL structure and column choices
- How to handle Docker Desktop not running (prompt vs auto-detect)
- Whether to add enterprise003 to V2 or create separate V3 migration

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Infrastructure & Configuration
- `.planning/codebase/STACK.md` — Full tech stack, Docker Compose config, environment profiles
- `.planning/codebase/ARCHITECTURE.md` — Layered monolith architecture, 6 backend layers, data flows
- `.env.example` — Template for environment variables (DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO keys)

### Database & Migrations
- `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql` — Schema definition (21 tables)
- `oaiss-chain-backend/src/main/resources/db/migration/V2__seed_data.sql` — Seed data (6 users, enterprise records, credit scores, carbon coin accounts)

### Security & Auth
- `.planning/codebase/CONCERNS.md` §Security — SEC-01~09 concerns, especially SEC-03 (Swagger exposure) and SEC-04 (CORS defaults)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java` — Security filter chain, CORS config, Swagger endpoints

### Requirements & Roadmap
- `.planning/REQUIREMENTS.md` §Phase 1 — ENV-01 through ENV-10 requirements
- `.planning/ROADMAP.md` §Phase 1 — Success criteria, entry/exit criteria, risk mitigation

</canonical_refs>

<specifics>
## Specific Ideas

- V2 seed data has user_type_list with ENTERPRISE(1), REVIEWER(2), THIRD_PARTY(3), ADMIN(4) — missing AUTHENTICATOR(5). The authenticator001 user has user_type=5 but no matching enum entry.
- Backend Docker profile (`SPRING_PROFILES_ACTIVE=docker`) disables Swagger — this is why local dev with default profile is needed for ENV-04.
- MySQL in docker-compose.yml mounts Flyway migrations to `/docker-entrypoint-initdb.d` — migrations run automatically on first start.
- Six seed accounts: admin, enterprise001, enterprise002, reviewer001, thirdparty001, authenticator001 — all password `admin123`.

</specifics>

<deferred>
## Deferred Ideas

- JWT refresh token mechanism testing — deferred to Phase 2+ if needed
- VERIFIER/CERTIFIER role gap investigation — Phase 4
- Full Docker stack mode (backend+frontend in Docker) — available via original docker-compose.yml but not used for testing
- Production environment hardening (SEC-03/04 fixes) — Phase 6

</deferred>

---

*Phase: 01-environment-setup*
*Context gathered: 2026-05-08*
