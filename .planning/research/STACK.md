# Technology Stack: Manual Testing Tools

**Project**: OAISS CHAIN (manual testing phase)
**Researched**: 2026-05-08
**Mode**: Ecosystem

---

## Overview

OAISS CHAIN is a Spring Boot 3.2.5 + Vue 3.5 multi-role carbon trading platform with 16 REST controllers, 5 user roles, MySQL 8, Redis 7, and MinIO object storage. Manual testing requires tools across 7 domains: API testing, database inspection, Redis inspection, MinIO file verification, browser-based testing, test data management, and multi-role session management.

---

## 1. API Testing Client

### Primary: Swagger UI (Built-in, Zero Setup)

| Property | Value |
|----------|-------|
| Tool | SpringDoc OpenAPI 2.5 Swagger UI |
| URL | `http://localhost:8080/api/v1/swagger-ui.html` |
| Status | Already configured in project (application.yml) |
| Cost | Free |

**Why this is primary:** The project already has SpringDoc OpenAPI 2.5 configured. Swagger UI provides an interactive API explorer that auto-generates from controller annotations. For 16 REST controllers, this is the fastest way to exercise every endpoint without writing any requests manually. It handles request/response schemas, shows available parameters, and lets testers execute requests directly in the browser.

**How testers use it:**
1. Start backend: `cd oaiss-chain-backend && mvn spring-boot:run`
2. Open `http://localhost:8080/api/v1/swagger-ui.html`
3. First call `POST /auth/login` with one of the 5 seed accounts to get a JWT token
4. Click "Authorize" button in Swagger UI, paste `Bearer <token>`
5. Now all subsequent requests are authenticated with that role

**Limitation:** Swagger UI does not persist sessions across browser refreshes well. For multi-role testing (switching between roles), a dedicated API client is needed.

### Secondary: Bruno (Recommended) or Postman

| Property | Bruno | Postman | Insomnia |
|----------|-------|---------|----------|
| Offline-first | Yes | No (cloud-first) | Partial |
| Git-friendly (.bru files) | Yes | No (JSON export) | No |
| Environment variables | Yes | Yes | Yes |
| Collection runner | Yes | Yes | Limited |
| Request chaining | Yes | Yes | Yes |
| Free tier | Fully free | Limited | Free |
| License | MIT | Proprietary | MIT/Apache |

**Recommendation: Bruno** because:
- Offline-first: no account required, no cloud sync concerns
- Git-friendly: `.bru` files are human-readable and can be committed to the repo for team sharing
- Free and open-source (MIT license)
- Supports environment variables, request chaining (use response from login as token for subsequent requests)
- Lightweight (~60MB vs Postman's ~300MB+)

**Alternative: Postman** if the tester already has it installed and prefers it. The recommendation is "use what you have" -- the key requirement is environment variable support for storing JWT tokens per role.

**Setup for OAISS CHAIN:**
```
Environment variables to configure:
  - base_url: http://localhost:8080/api/v1
  - admin_token: (from admin login response)
  - enterprise_token: (from enterprise login response)
  - reviewer_token: (from reviewer login response)
  - authenticator_token: (from authenticator login response)
  - thirdparty_token: (from thirdparty login response)
```

### CLI Alternative: HTTPie (for scripted verification)

| Property | Value |
|----------|-------|
| Tool | HTTPie (https://httpie.io) |
| Install | `pip install httpie` or `brew install httpie` |
| Cost | Free (CLI), paid (Desktop app) |

**When to use:** Quick one-off API checks from the terminal, or when verifying specific bug fixes. Not for full testing sessions.

```bash
# Example: Login as admin
http POST localhost:8080/api/v1/auth/login username=admin password=admin123 captcha=test

# Example: Get carbon reports with token
http GET localhost:8080/api/v1/carbon/reports Authorization:"Bearer <token>"
```

---

## 2. Database Inspection (MySQL 8)

### Primary: DBeaver Community Edition

| Property | Value |
|----------|-------|
| Tool | DBeaver CE |
| Version | 24.x+ |
| Install | https://dbeaver.io/download/ |
| Cost | Free (Community), Paid (Pro) |
| Platform | Windows, macOS, Linux |

**Why DBeaver:**
- Free, open-source, actively maintained
- Native MySQL 8 support with proper utf8mb4 handling (the project uses utf8mb4_unicode_ci collation)
- ER diagram viewer: visualize the 21-table schema and relationships
- Data grid with inline editing: useful for verifying post-operation state
- SQL editor with autocomplete: write ad-hoc queries to verify data
- Export results to CSV for documentation

**Connection setup:**
```
Host: localhost
Port: 3306
Database: oaiss_chain
Username: root
Password: (from DB_PASSWORD in .env, default: 123456)
```

**Key queries for manual testing:**

```sql
-- Verify user accounts across all 5 roles
SELECT id, username, real_name, user_type, status FROM user ORDER BY id;

-- Check carbon report status flow
SELECT id, enterprise_id, report_year, status, reviewer_id, created_at
FROM carbon_report ORDER BY id DESC LIMIT 20;

-- Verify auction order states after matching
SELECT id, user_id, order_type, price, quantity, status, created_at
FROM auction_order ORDER BY id DESC LIMIT 20;

-- Check carbon coin balances after transactions
SELECT user_id, balance, total_recharged, total_spent
FROM carbon_coin_account;

-- Verify blockchain tx hashes are populated
SELECT id, blockchain_tx_hash FROM carbon_report WHERE blockchain_tx_hash IS NOT NULL;

-- Check credit score changes
SELECT cs.enterprise_id, cs.score, cs.level, ce.event_type, ce.reason
FROM credit_score cs
JOIN credit_event ce ON cs.enterprise_id = ce.enterprise_id
ORDER BY ce.created_at DESC;
```

### Alternative: MySQL Workbench

| Property | Value |
|----------|-------|
| Tool | MySQL Workbench |
| Cost | Free (Oracle official) |
| Platform | Windows, macOS, Linux |

Use if already installed. DBeaver is preferred for its broader database support and better UI, but MySQL Workbench handles this project's needs adequately.

### CLI: mysql client (Docker exec)

```bash
# Access MySQL inside Docker container
docker exec -it oaiss-mysql mysql -u root -p oaiss_chain

# Quick count check
docker exec -it oaiss-mysql mysql -u root -p123456 oaiss_chain -e "SELECT COUNT(*) FROM carbon_report;"
```

---

## 3. Redis State Inspection

### Primary: RedisInsight (Official Redis GUI)

| Property | Value |
|----------|-------|
| Tool | RedisInsight |
| Version | 2.x+ |
| Install | https://redis.com/redis-enterprise/redis-insight/ |
| Cost | Free |
| Platform | Windows, macOS, Linux |

**Why RedisInsight:**
- Official tool from Redis, Inc.
- Built-in CLI for manual commands
- Real-time Profiler: see all Redis commands as they happen (useful for verifying rate limiting, distributed locks, cache operations)
- Key browser: inspect cached data, TTL values, key patterns
- Memory analysis: understand Redis usage patterns
- Slow Log viewer: identify performance issues

**Connection setup:**
```
Host: localhost
Port: 6379
Password: (from REDIS_PASSWORD in .env, may be empty for dev)
Database: 0
```

**What to verify in Redis during manual testing:**

```redis
-- Check cached data keys
KEYS *

-- Inspect rate limiting state (if @RateLimit is active)
KEYS rate_limit:*

-- Check distributed lock state (if @DistributedLock is active)
KEYS lock:*

-- View cached user sessions or data
GET cache::*

-- Monitor real-time activity
MONITOR
```

**Note:** The project uses `@RateLimit` and `@DistributedLock` annotations backed by Redis. RedisInsight's Profiler is the only way to observe these cross-cutting concerns in action during manual testing.

### Alternative: Redis CLI (Docker exec)

```bash
# Quick Redis check
docker exec -it oaiss-redis redis-cli

# List all keys
docker exec -it oaiss-redis redis-cli KEYS "*"

# Monitor real-time commands
docker exec -it oaiss-redis redis-cli MONITOR
```

---

## 4. MinIO File Storage Inspection

### Primary: MinIO Console (Built-in Web UI)

| Property | Value |
|----------|-------|
| Tool | MinIO Console |
| URL | `http://localhost:9001` |
| Username | minioadmin (from docker-compose.yml) |
| Password | minioadmin |
| Cost | Free (built into MinIO) |

**Why this is sufficient:** MinIO ships with a full web console. No additional tool needed.

**What testers can do:**
- Browse the `oaiss-chain` bucket and subfolders
- View uploaded files (carbon reports, documents)
- Download files to verify content
- Check file metadata (size, content-type, upload date)
- Verify UUID-based naming convention
- Test presigned URL generation and expiry

**Test flow for file upload (TEST-13):**
1. Upload a file via the frontend or API
2. Open MinIO Console at `http://localhost:9001`
3. Navigate to `oaiss-chain` bucket
4. Verify the file exists with correct name pattern
5. Download and verify content matches

---

## 5. Browser DevTools (Frontend Testing)

### Primary: Chrome DevTools (Built-in)

Chrome DevTools is the standard for manual frontend testing. No installation needed.

**Key panels for OAISS CHAIN testing:**

| Panel | What to Check |
|-------|---------------|
| **Network** | API request/response payloads, status codes, JWT token in Authorization header, pagination params |
| **Application > Session Storage** | Verify `accessToken` and `refreshToken` are stored correctly per role |
| **Console** | JavaScript errors, Vue warnings, API error messages |
| **Elements** | DOM structure, Element Plus component rendering, role-based visibility |
| **Performance** | Page load times, API response times |

**Multi-role testing with Chrome Profiles:**

The project stores JWT tokens in `sessionStorage` (not cookies), which means:
- Each browser tab shares the same sessionStorage within a domain
- To test multiple roles simultaneously, use **Chrome profiles** or **incognito windows**

**Recommended approach:**
```
Role 1 (Admin):        Chrome profile "Admin" or regular window
Role 2 (Enterprise):   Chrome profile "Enterprise" or incognito window 1
Role 3 (Reviewer):     Chrome profile "Reviewer" or incognito window 2
Role 4 (Authenticator): Chrome profile "Authenticator" or incognito window 3
Role 5 (Third Party):  Chrome profile "Third Party" or incognito window 4
```

Each profile maintains its own sessionStorage, allowing simultaneous multi-role testing.

### Vue DevTools Browser Extension

| Property | Value |
|----------|-------|
| Tool | Vue.js devtools |
| Install | Chrome Web Store: "Vue.js devtools" |
| Cost | Free |

**Why useful for OAISS CHAIN:**
- Inspect Pinia store state in real-time (see what data the frontend holds)
- Verify Vue Router guards are blocking unauthorized access correctly
- Inspect component props to verify data binding
- Check Element Plus component state

---

## 6. Test Data Management

### Seed Data (Already Available)

The project's `V2__seed_data.sql` provides 6 test accounts covering all 5 roles:

| Username | Password | Role | Profile |
|----------|----------|------|---------|
| `admin` | `admin123` | ADMIN (user_type=4) | System administrator |
| `enterprise001` | `admin123` | ENTERPRISE (user_type=1) | "Green Energy Tech" -- quota 50000, used 12000 |
| `enterprise002` | `admin123` | ENTERPRISE (user_type=1) | "Low Carbon Manufacturing" -- quota 80000, used 25000 |
| `reviewer001` | `admin123` | REVIEWER (user_type=2) | Carbon audit reviewer, 156 completed reviews |
| `thirdparty001` | `admin123` | THIRD_PARTY (user_type=3) | China Carbon Emission Registration Authority |
| `authenticator001` | `admin123` | AUTHENTICATOR (user_type=5) | China Quality Certification Center |

**Additional seed data:**
- 2 enterprises with carbon quotas and credit scores (100/EXCELLENT)
- 2 carbon coin accounts with 10000 balance each
- 1 reviewer with qualification records
- API permission entries for all role types

### Test Data Reset Strategy

**Option A: Flyway baseline reset (recommended for full reset)**
```bash
# Stop backend, drop and recreate database
docker exec -it oaiss-mysql mysql -u root -p123456 -e "DROP DATABASE oaiss_chain; CREATE DATABASE oaiss_chain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# Restart backend -- Flyway will re-run V1 and V2
```

**Option B: Selective data cleanup (for partial reset)**
```sql
-- Clean test-generated data while preserving seed data
DELETE FROM carbon_coin_transaction WHERE id > 0;
DELETE FROM matching_result WHERE id > 0;
DELETE FROM auction_order WHERE id > 0;
DELETE FROM transaction WHERE id > 0;
DELETE FROM carbon_report WHERE id > 0;
-- Reset carbon coin balances
UPDATE carbon_coin_account SET balance = 10000, total_spent = 0;
-- Reset enterprise carbon usage
UPDATE enterprise SET carbon_used = 12000, carbon_tradable = 38000 WHERE id = 1;
UPDATE enterprise SET carbon_used = 25000, carbon_tradable = 55000 WHERE id = 2;
```

### Additional Test Data Generation

**For scenarios requiring more data (e.g., pagination testing, bulk operations):**

No external tool needed. Use the existing API endpoints to create test data:
1. Login as enterprise001
2. Create multiple carbon reports via `POST /carbon/reports`
3. Create multiple auction orders via `POST /auction/orders`

**If bulk data is needed:** Write a simple shell script using `curl` to loop-create records, or use Swagger UI's "Try it out" feature repeatedly.

---

## 7. Session/Cookie Management for Multi-Role Testing

### Understanding the Auth Architecture

The project uses JWT Bearer tokens in `sessionStorage` (not cookies). This means:
- No CSRF vulnerability (tokens not auto-attached by browser)
- No server-side session state to manage
- Each API request must include `Authorization: Bearer <token>` header
- Tokens expire after 1 hour (access) / 7 days (refresh)

### Multi-Role Testing Strategy

**Approach 1: Multiple Browser Profiles (Recommended for UI testing)**

Create 5 Chrome profiles, one per role. Each profile has independent sessionStorage.

**Approach 2: Swagger UI Token Swapping (Recommended for API-only testing)**

1. Open Swagger UI
2. Login as role A, copy token, authorize
3. Test all role A endpoints
4. Click "Authorize" again, paste role B token
5. Test all role B endpoints

**Approach 3: Bruno/Postman Environment Switching (Recommended for systematic API testing)**

Configure 5 environments in Bruno, each with a pre-obtained JWT token:
- `env-admin`: `token = <admin JWT>`
- `env-enterprise1`: `token = <enterprise001 JWT>`
- `env-enterprise2`: `token = <enterprise002 JWT>`
- `env-reviewer`: `token = <reviewer001 JWT>`
- `env-thirdparty`: `token = <thirdparty001 JWT>`
- `env-authenticator`: `token = <authenticator001 JWT>`

Switch environments to change the active token. All requests automatically use the selected role's token.

---

## 8. Additional Tools

### Network Proxy (Optional): mitmproxy

| Property | Value |
|----------|-------|
| Tool | mitmproxy |
| Install | `pip install mitmproxy` |
| Cost | Free |

**When needed:** Debugging complex request/response flows, especially:
- Token refresh race conditions (the project has a single-flight pattern for concurrent refresh)
- Verifying CORS headers
- Inspecting multipart file upload payloads
- Capturing exact request/response for bug reports

**Not required** for basic manual testing. Chrome DevTools Network panel covers 90% of debugging needs.

### JSON Formatter (Browser Extension)

| Property | Value |
|----------|-------|
| Tool | JSON Formatter or JSON Viewer |
| Install | Chrome Web Store |
| Cost | Free |

**Why:** API responses in Swagger UI and raw browser responses are easier to read with a JSON formatter extension. Trivial but improves daily testing efficiency.

---

## Recommended Tool Stack Summary

| Need | Tool | Cost | Already Available |
|------|------|------|-------------------|
| API testing (quick) | Swagger UI | Free | Yes (SpringDoc 2.5 configured) |
| API testing (systematic) | Bruno | Free | No (install required) |
| API testing (CLI) | HTTPie | Free | No (install optional) |
| Database inspection | DBeaver CE | Free | No (install required) |
| Redis inspection | RedisInsight | Free | No (install required) |
| MinIO file verification | MinIO Console | Free | Yes (docker-compose exposes port 9001) |
| Frontend testing | Chrome DevTools | Free | Yes (built-in) |
| Vue state inspection | Vue DevTools | Free | No (browser extension) |
| Network debugging | Chrome DevTools Network | Free | Yes (built-in) |
| Test data | Flyway seed + API | Free | Yes (V2__seed_data.sql) |
| Data reset | Docker MySQL CLI | Free | Yes (docker exec) |

---

## Installation Priority

**Tier 1 -- Install before testing starts:**
1. **DBeaver CE** -- Database state verification is critical for every test case
2. **Bruno** (or Postman if already installed) -- Systematic API testing across 16 controllers
3. **RedisInsight** -- Verify rate limiting, caching, distributed locks

**Tier 2 -- Install during testing if needed:**
4. **Vue DevTools** -- When frontend state issues arise
5. **HTTPie** -- For quick CLI-based API verification

**Tier 3 -- Install only if specific issues arise:**
6. **mitmproxy** -- For complex network debugging

---

## Sources

- Project `docker-compose.yml`: confirms MinIO console at port 9001, MySQL at 3306, Redis at 6379
- Project `application.yml`: confirms SpringDoc OpenAPI 2.5 at `/swagger-ui.html`
- Project `V2__seed_data.sql`: confirms 6 test accounts with passwords
- Project `SecurityConfig.java`: confirms JWT Bearer auth, stateless sessions, CORS config
- Project `request.ts`: confirms sessionStorage-based token storage, auto-refresh logic
- Project `INTEGRATIONS.md`: confirms MinIO bucket `oaiss-chain`, Redis usage patterns
