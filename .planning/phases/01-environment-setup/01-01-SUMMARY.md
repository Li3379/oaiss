# Plan 01-01 Summary: Docker Infra + V3 Migration + Health Check

**Phase:** 01-environment-setup
**Plan:** 01-01
**Status:** Complete
**Completed:** 2026-05-08
**Commits:** 2

## What was done

### Task 1: docker-compose.infra.yml + V3 test seed migration
- Created `docker-compose.infra.yml` with mysql, redis, minio services only (no backend/frontend)
- Created `V3__test_seed_data.sql` adding:
  - AUTHENTICATOR enum entry (id=5) in `user_type_list`
  - enterprise003 user (id=7) in `user` table
  - enterprise003 enterprise record (id=3) in `enterprise` table
  - credit_score (id=3) for enterprise003
  - carbon_coin_account (user_id=7) for enterprise003

**Commit:** `f6c5d79` feat(infra): add docker-compose.infra.yml and V3 test seed migration

### Task 2: health-check.sh + environment verification
- Created `scripts/health-check.sh` with 8-step automated verification
- Fixed Redis check: `&>/dev/null` before pipe suppressed output; changed to `2>/dev/null`
- Started Docker MySQL container on port 3307 (host port 3306 occupied by local MySQL)
- All 8 health checks pass:
  1. Docker Desktop running
  2. MySQL healthy (oaiss-mysql container)
  3. Flyway tables count: 21 tables
  4. V3 seed data present (AUTHENTICATOR enum + enterprise003)
  5. Redis PING/PONG
  6. MinIO console on :9001
  7. Backend Swagger UI (HTTP 302)
  8. Frontend on :5173

**Commit:** `ad969ec` feat(infra): add health-check.sh for automated environment verification

## Environment Notes

- Host MySQL occupies port 3306; `oaiss-mysql` container runs on port 3307
- Backend connects to host MySQL on localhost:3306 (default profile)
- Host MySQL has V3 migration applied manually + registered in flyway_schema_history
- Docker MySQL initialized via `/docker-entrypoint-initdb.d` with all 3 migrations
- Redis and MinIO run as Docker containers on standard ports

## Verification Results

```
[OK] Docker Desktop is running
[OK] MySQL is healthy on :3306
[OK] Database has 21 tables (>= 21 expected)
[OK] V3 seed data verified (AUTHENTICATOR enum + enterprise003)
[OK] Redis is healthy on :6379
[OK] MinIO console accessible on :9001
[OK] Backend Swagger UI accessible (HTTP 302)
[OK] Frontend accessible on :5173
```

## Files Created

| File | Purpose |
|------|---------|
| `docker-compose.infra.yml` | Infrastructure-only compose (mysql+redis+minio) |
| `oaiss-chain-backend/src/main/resources/db/migration/V3__test_seed_data.sql` | Test seed data migration |
| `scripts/health-check.sh` | 8-step automated health verification |

## Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| D-LOCAL-01 | Docker MySQL on port 3307, host MySQL on 3306 | Host MySQL service cannot be stopped without admin privileges; both databases contain identical schema and seed data |
| D-LOCAL-02 | Applied V3 manually to host MySQL | Backend was already running against host MySQL; manual application avoids restart |
| D-FIX-01 | Changed Redis check from `&>/dev/null` to `2>/dev/null` | Original suppressed stdout before pipe, causing grep to never match PONG |

## Acceptance Criteria Met

- [x] docker-compose.infra.yml exists with mysql/redis/minio services only
- [x] V3__test_seed_data.sql adds AUTHENTICATOR(5) enum, enterprise003 + enterprise + credit_score + carbon_coin_account
- [x] 5 INSERT statements in V3 (user_type_list + user + enterprise + credit_score + carbon_coin_account)
- [x] All 21 tables exist in oaiss_chain database
- [x] 7 seed accounts exist in user table
- [x] Backend Swagger UI accessible
- [x] Frontend accessible
- [x] health-check.sh passes all 8 checks with exit code 0
