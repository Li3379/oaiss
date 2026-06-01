# Phase 1: Environment Setup & Auth Baseline - Validation Strategy

**Phase:** 1 (01-environment-setup)
**Created:** 2026-05-08
**Status:** Active

## Test Framework

| Property | Value |
|----------|-------|
| Framework | Bash script (health-check.sh) + curl (login-test.sh) |
| Config file | None -- standalone scripts |
| Quick run command | `bash scripts/health-check.sh` |
| Full suite command | `bash scripts/health-check.sh && bash scripts/login-test.sh` |

## Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | Covered By |
|--------|----------|-----------|-------------------|------------|
| ENV-01 | Docker infra services running | smoke | `bash scripts/health-check.sh` (checks Docker, MySQL, Redis, MinIO) | Plan 01-01 Task 2 |
| ENV-02 | Flyway V1+V2 migration success, 21 tables | smoke | `docker exec oaiss-mysql mysql ... -e "SELECT COUNT(*)"` | Plan 01-01 Task 2 |
| ENV-03 | V3 migration created and applied | smoke | Verify V3 file exists + table counts after backend start | Plan 01-01 Task 1+2 |
| ENV-04 | Backend Swagger UI accessible | smoke | `curl -sf http://localhost:8080/api/v1/swagger-ui.html` | Plan 01-01 Task 2 |
| ENV-05 | Frontend homepage accessible | smoke | `curl -sf http://localhost:5173` | Plan 01-01 Task 2 |
| ENV-06 | 7 seed accounts login (6 original + enterprise003) | smoke | `bash scripts/login-test.sh` | Plan 01-02 Task 1 |
| ENV-07 | Role home page routing correct | manual | Browser checkpoint: verify ROLE_HOME per role | Plan 01-02 Task 2 |
| ENV-08 | JWT Bearer token valid | smoke | curl login + `GET /auth/me` with Bearer | Plan 01-02 Task 1 |
| ENV-09 | Token refresh | **SKIPPED** | Per D-12 decision | N/A |
| ENV-10 | Logout blacklists token | smoke | curl login -> access -> logout -> access again (expect 401) | Plan 01-02 Task 1 |

## Sampling Rate

- **Per task commit:** `bash scripts/health-check.sh` (for infrastructure tasks)
- **Per wave merge:** Manual verification of all ENV-XX items
- **Phase gate:** Full walkthrough of ENV-01 through ENV-10 (except ENV-09)

## Validation Checkpoints

### Wave 1 Exit (Plan 01-01 complete)
- [ ] `docker-compose -f docker-compose.infra.yml up -d` succeeds
- [ ] MySQL container healthy, 21 tables exist with V3 data
- [ ] Redis container healthy (PING returns PONG)
- [ ] MinIO container healthy (console at :9001)
- [ ] Backend starts via `mvn spring-boot:run`, Swagger loads
- [ ] Frontend starts via `npm run dev`, responds at :5173
- [ ] `bash scripts/health-check.sh` exits 0

### Wave 2 Exit (Plan 01-02 complete)
- [ ] All 7 seed accounts login successfully with password `admin123`
- [ ] Each role returns correct `userType` in login response
- [ ] JWT Bearer token works on `GET /auth/me`
- [ ] Logout blacklists token (subsequent request returns 401)
- [ ] Browser: each role navigates to correct ROLE_HOME page

## Coverage Summary

- **Total requirements:** 10 (ENV-01 through ENV-10)
- **Covered by automated tests:** 8 (ENV-01~06, ENV-08, ENV-10)
- **Covered by manual checkpoint:** 1 (ENV-07)
- **Skipped by design:** 1 (ENV-09, per D-12)
- **Coverage rate:** 9/10 = 90% (100% of non-deferred)

---

*Phase: 01-environment-setup*
*Validation strategy created: 2026-05-08*
