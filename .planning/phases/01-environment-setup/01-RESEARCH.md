# Phase 1: Environment Setup & Auth Baseline - Research

**Researched:** 2026-05-08
**Domain:** Infrastructure provisioning, Flyway migrations, JWT auth lifecycle
**Confidence:** HIGH

## Summary

Phase 1 搭建一个完整的本地开发环境（MySQL/Redis/MinIO 在 Docker 中运行，后端/前端在本地运行），创建 V3 Flyway 测试种子数据迁移，验证 6 个种子账户的登录与角色路由，并交付一个自动化健康检查脚本。经过对全部源码的审查，所有基础设施组件、认证流程和前端路由均已实现且状态健康 -- 本阶段主要是配置（docker-compose.infra.yml、.env、V3 migration）和验证，不需要大量新代码。

**Primary recommendation:** 创建 docker-compose.infra.yml（从现有 docker-compose.yml 精简，只保留 mysql/redis/minio 三个服务），编写 V3__test_seed_data.sql（补充 enterprise003 用户 + 企业记录 + 碳币账户 + 信用评分 + AUTHENTICATOR 枚举条目），然后编写 bash 健康检查脚本做 fast-fail 验证。.env 文件已存在且包含可用默认值，无需重新创建。

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Minimal seed strategy -- each phase creates its own business data via UI during testing. V3 does NOT pre-seed carbon reports, auction orders, or projects.
- **D-02:** V3 includes: enterprise003 user + enterprise record + carbon coin account + credit score. All three enterprises have equal starting conditions.
- **D-03:** V3 adds AUTHENTICATOR(5) to user_type_list enum table. This is the only role gap fix -- VERIFIER/CERTIFIER gaps deferred to Phase 4.
- **D-04:** V3 checks and fixes missing tables: `rsa_key_pairs`, `operation_log`, `emission_rating`. If any are missing from V1/V2, V3 creates them.
- **D-05:** No pre-seeded business data -- no carbon reports, no auction orders, no carbon neutral projects. Each test phase creates its own data.
- **D-06:** Infrastructure Docker + local dev approach. MySQL, Redis, MinIO run in Docker. Backend runs via `mvn spring-boot:run` (default profile, Swagger enabled). Frontend runs via `npm run dev`.
- **D-07:** Create `docker-compose.infra.yml` for infrastructure-only startup. Original `docker-compose.yml` remains untouched for full-stack Docker mode.
- **D-08:** Create `.env` file with working defaults (not just template). Pre-fill DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO keys, CORS origins.
- **D-09:** Docker Desktop running status is an entry criteria for the test plan. Health check script verifies it before proceeding.
- **D-10:** Keep JWT at default expiration (45 minutes). Re-login when token expires during testing. No expiration time changes.
- **D-11:** Each role gets its own Incognito/Privacy browser window. Avoids role state confusion. Always use logout button when switching.
- **D-12:** Skip JWT refresh (ENV-09) testing in Phase 1. Focus on login -> access -> logout. Refresh mechanism deferred to later phase if needed.
- **D-13:** Verify logout blacklists the token -- after logout, attempt access with old token, expect 401 response.
- **D-14:** Automated health check script (bash/PowerShell) that verifies all 5 services: MySQL (3306 + table count), Redis (PING), MinIO (9001 console), Backend (Swagger UI), Frontend (5173).
- **D-15:** Fast-fail behavior -- script prints specific error and exits non-zero on first failure. Does not continue to subsequent checks.
- **D-16:** Health check includes Flyway migration verification: 21 tables exist + V3 seed data present.

### Claude's Discretion
- Exact health check script implementation (bash vs PowerShell, output format)
- V3 migration SQL structure and column choices
- How to handle Docker Desktop not running (prompt vs auto-detect)
- Whether to add enterprise003 to V2 or create separate V3 migration

### Deferred Ideas (OUT OF SCOPE)
- JWT refresh token mechanism testing -- deferred to Phase 2+ if needed
- VERIFIER/CERTIFIER role gap investigation -- Phase 4
- Full Docker stack mode (backend+frontend in Docker) -- available via original docker-compose.yml but not used for testing
- Production environment hardening (SEC-03/04 fixes) -- Phase 6
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ENV-01 | Docker Compose 启动成功（MySQL, Redis, MinIO, 后端, 前端） | docker-compose.infra.yml 只需 mysql/redis/minio，后端前端本地运行 |
| ENV-02 | Flyway 迁移执行成功（V1 schema + V2 seed data），21 张表 | V1 创建 21 表已验证，Flyway 配置 baseline-on-migrate: true |
| ENV-03 | 创建 V3 测试数据迁移 | V3 需包含 enterprise003 + AUTHENTICATOR 枚举，不含业务数据 |
| ENV-04 | 后端启动成功，Swagger UI 可访问 | 默认 profile 下 Swagger 开放，路径 /api/v1/swagger-ui.html |
| ENV-05 | 前端启动成功，首页可访问 | Vite proxy 将 /api 转发到 localhost:8080 |
| ENV-06 | 6 个种子账户均可登录 | 登录支持可选验证码，V3 新增 enterprise003 为第 6 个可测账户 |
| ENV-07 | 每个角色登录后跳转到正确的角色首页 | ROLE_HOME 映射 5 个角色到对应路径，已从 menu.ts 验证 |
| ENV-08 | JWT 令牌获取成功，Authorization: Bearer 有效 | JwtTokenProvider 使用 HMAC-SHA，claims 含 userId/roles/userType |
| ENV-09 | 令牌刷新机制验证 | **按 D-12 决定跳过**，不在 Phase 1 测试 |
| ENV-10 | 退出登录成功，令牌加入黑名单 | AuthService.logout() 将 token 存入 Redis tokenBlacklist 缓存 |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Infrastructure (MySQL/Redis/MinIO) | Docker | -- | D-06 决定：基础设施 Docker 化 |
| Backend startup & Swagger | API / Backend | -- | `mvn spring-boot:run` 本地运行，默认 profile |
| Frontend dev server | Browser / Client | -- | `npm run dev` 本地运行，Vite proxy 转发 API |
| JWT token generation & validation | API / Backend | -- | JwtTokenProvider 完全在后端 |
| Token blacklist on logout | API / Backend + Redis | -- | CacheManager + Redis cache "tokenBlacklist" |
| Role-based home page routing | Browser / Client | -- | Vue Router beforeEach guard + ROLE_HOME map |
| Flyway migration execution | Database / Storage | API / Backend | Spring Boot 启动时自动执行 Flyway |
| Health check script | OS / CLI | -- | Bash 脚本验证 5 个服务 |

## Standard Stack

### Core
| Library / Tool | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Docker Desktop | 29.4.2 | Infrastructure containers | [VERIFIED: `docker --version`] |
| Java (JDK) | 17 | Backend runtime | [VERIFIED: `java -version`] |
| Maven | 3.8.4 | Backend build tool | [VERIFIED: `mvn --version`] |
| Node.js | 24.15.0 | Frontend runtime | [VERIFIED: `node --version`] |
| MySQL | 8.0 (Docker) | Relational database | [VERIFIED: docker-compose.yml image: mysql:8.0] |
| Redis | 7-alpine (Docker) | Cache, token blacklist, rate limiting | [VERIFIED: docker-compose.yml image: redis:7-alpine] |
| MinIO | latest (Docker) | Object storage (file uploads) | [VERIFIED: docker-compose.yml image: minio/minio:latest] |
| Flyway | Spring Boot managed | Database migrations | [VERIFIED: application.yml flyway.enabled: true] |
| Spring Boot | 3.2.5 | Backend framework | [CITED: CLAUDE.md] |
| jjwt | 0.12.5 | JWT token handling | [CITED: CLAUDE.md] |
| Vue 3 | 3.5 | Frontend framework | [CITED: CLAUDE.md] |
| Vite | 8.x (config) | Frontend dev server + build | [VERIFIED: vite.config.js + package.json] |

### Supporting
| Library / Tool | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Bash | 5.2.37 | Health check script | [VERIFIED: `bash --version` on Windows/Git Bash] |
| curl | (system) | Health check HTTP probes | 验证 Swagger UI、前端首页可用性 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| bash health check | PowerShell script | Windows 原生 PS 更可靠但 bash 在 Git Bash 下也能用；bash 跨平台更好 |
| docker-compose.infra.yml | 原始 docker-compose.yml | 原始包含 backend/frontend Docker 构建，infra 版本更轻量 |

**Installation:**
```bash
# No new installations needed -- all tools already available
# Docker Desktop, Java 17, Maven 3.8.4, Node 24.15.0, Bash 5.2 all verified
```

## Architecture Patterns

### System Architecture Diagram

```
                     ┌─────────────────────┐
                     │  Docker Desktop      │
                     │  (entry criteria)    │
                     └──────────┬──────────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
     ┌────────▼──────┐ ┌───────▼───────┐ ┌───────▼────────┐
     │  MySQL 8.0    │ │  Redis 7      │ │  MinIO         │
     │  :3306        │ │  :6379        │ │  :9000/:9001   │
     │               │ │               │ │                │
     │  Flyway V1-V3 │ │ tokenBlacklist│ │ file storage   │
     │  21 tables    │ │ rate limits   │ │ buckets        │
     └────────┬──────┘ └───────┬───────┘ └───────┬────────┘
              │                 │                  │
              └─────────────────┼──────────────────┘
                                │
                     ┌──────────▼──────────┐
                     │  Spring Boot :8080  │
                     │  (default profile)  │
                     │  Swagger enabled    │
                     │  JWT + BCrypt       │
                     └──────────┬──────────┘
                                │
                     ┌──────────▼──────────┐
                     │  Vite Dev :5173     │
                     │  (proxy /api → 8080)│
                     │  Vue 3 + Pinia      │
                     └─────────────────────┘
                                │
                     ┌──────────▼──────────┐
                     │  Browser            │
                     │  (Incognito × roles)│
                     └─────────────────────┘
```

### Recommended Project Structure (new files only)
```
docker-compose.infra.yml              # Infrastructure-only compose (mysql + redis + minio)
oaiss-chain-backend/src/main/resources/db/migration/
  V3__test_seed_data.sql              # enterprise003 + AUTHENTICATOR enum
scripts/
  health-check.sh                     # Automated fast-fail health verification
```

### Pattern 1: Docker Infrastructure-Only Compose
**What:** Extract mysql/redis/minio from docker-compose.yml into a separate file.
**When to use:** Local development where backend/frontend run natively.
**Key differences from original:**
- No `backend` service (run via `mvn spring-boot:run` instead)
- No `frontend` service (run via `npm run dev` instead)
- No Docker builds needed
- Remove `depends_on` for backend/frontend
- Keep all volumes, ports, health checks unchanged
**Example:**
```yaml
# docker-compose.infra.yml
version: '3.8'
services:
  mysql:
    # Copy exactly from docker-compose.yml mysql service
    # Same image, ports, volumes, environment, healthcheck
  redis:
    # Copy exactly from docker-compose.yml redis service
  minio:
    # Copy exactly from docker-compose.yml minio service
volumes:
  mysql-data:
  redis-data:
  minio-data:
```

### Pattern 2: V3 Migration (Additive Only)
**What:** V3 adds missing enum entry and third enterprise user.
**When to use:** Extend seed data without modifying V1/V2.
**Critical rules:**
- Use explicit IDs that don't collide with V2 data (user IDs start at 7, enterprise ID at 3)
- AUTHENTICATOR(5) in user_type_list -- this is the enum gap fix
- enterprise003 must have enterprise record + carbon_coin_account + credit_score to match enterprise001/002
- Do NOT add business data (carbon reports, auction orders, projects)
**Example SQL structure:**
```sql
-- V3__test_seed_data.sql
SET NAMES utf8mb4;

-- 1. Add AUTHENTICATOR to user_type_list enum (D-03)
INSERT INTO `user_type_list` (`id`, `type_name`, `type_code`, `description`, `default_role`, `created_at`, `updated_at`)
VALUES (5, '认证机构', 'AUTHENTICATOR', '碳排放认证机构', 'ROLE_AUTHENTICATOR', NOW(), NOW());

-- 2. Third enterprise user (D-02)
INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`, `created_at`, `updated_at`)
VALUES (7, 'enterprise003', '<same BCrypt hash>', 'enterprise003@example.com', '13800138006', '王五', 1, 1, NOW(), NOW());

-- 3. Enterprise record for enterprise003 (D-02)
INSERT INTO `enterprise` (...) VALUES (3, 7, ...);

-- 4. Credit score for enterprise003 (D-02)
INSERT INTO `credit_score` (...) VALUES (3, 3, 100, 'EXCELLENT', 0, 0, NOW(), NOW());

-- 5. Carbon coin account for enterprise003 (D-02)
INSERT INTO `carbon_coin_account` (...) VALUES (7, 10000, 10000, 0, 1, NOW(), NOW());
```

### Pattern 3: Fast-Fail Health Check Script
**What:** Bash script that checks each dependency and exits on first failure.
**When to use:** Pre-test validation before manual testing sessions.
**Order of checks:** Docker Desktop -> MySQL -> Redis -> MinIO -> Backend -> Frontend -> Flyway tables
**Example:**
```bash
#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m' GREEN='\033[0;32m' NC='\033[0m'
ok() { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

# 1. Docker Desktop
docker info &>/dev/null || fail "Docker Desktop is not running"

# 2. MySQL on 3306
mysqladmin ping -h 127.0.0.1 -P 3306 -u root -p123456 &>/dev/null || fail "MySQL not responding on :3306"
TABLE_COUNT=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='oaiss_chain'" -sN 2>/dev/null)
[[ "$TABLE_COUNT" -ge 21 ]] || fail "Expected 21+ tables, found $TABLE_COUNT"

# 3. Redis PING
redis-cli -h 127.0.0.1 -p 6379 PING &>/dev/null || fail "Redis not responding on :6379"

# 4. MinIO console
curl -sf http://localhost:9001 >/dev/null || fail "MinIO console not responding on :9001"

# 5. Backend Swagger UI
curl -sf http://localhost:8080/api/v1/swagger-ui.html -o /dev/null || fail "Backend Swagger UI not accessible"

# 6. Frontend
curl -sf http://localhost:5173 >/dev/null || fail "Frontend not responding on :5173"

ok "All health checks passed"
```

### Anti-Patterns to Avoid
- **不要修改 V1 或 V2 migration:** Flyway 会校验已有 migration 的 checksum，任何修改都会导致启动失败。只能添加新的 V3 migration。[VERIFIED: application.yml flyway.enabled: true]
- **不要在 docker-compose.infra.yml 中添加 backend/frontend 服务:** D-06 决定本地运行后端前端，Docker 只管基础设施。
- **不要预填充业务数据:** D-01/D-05 明确不做 carbon reports / auction orders / projects。
- **不要在健康检查脚本中 sleep 等待服务:** 使用 `interval` 重试而非固定 sleep。Fast-fail 是 D-15 的要求。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JWT token generation | Custom HMAC implementation | jjwt 0.12.5 `JwtTokenProvider` | 已实现，处理了 key 生成、claims、过期 |
| Password hashing | Custom hash function | BCryptPasswordEncoder (Spring Security) | V2 seed data 使用 BCrypt hash |
| Token blacklist | Custom DB table | Redis CacheManager "tokenBlacklist" | 已实现，Redis TTL 自动清理 |
| CORS configuration | Manual filter | Spring Security CORS config | 已在 SecurityConfig.java 配置 |
| Flyway migration ordering | Custom SQL runner | Spring Boot Flyway auto-run | baseline-on-migrate: true 已配置 |
| API pagination | Custom transform | Vite interceptor (request.ts) | pageNum/pageSize -> page/size 已实现 |

**Key insight:** 本阶段几乎所有基础设施和认证逻辑都已实现，不需要编写新功能代码。主要工作是配置（docker-compose.infra.yml、V3 migration）和验证（健康检查脚本）。

## Runtime State Inventory

> Phase 1 涉及新环境搭建，不涉及 rename/refactor/migration。

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | MySQL volume (mysql-data): 新建，Flyway 自动迁移 | 无需处理 -- 新建即包含 V1+V2+V3 |
| Live service config | Docker volumes: mysql-data, redis-data, minio-data | `docker-compose -f docker-compose.infra.yml up -d` 创建 |
| OS-registered state | 无 | 无 |
| Secrets/env vars | .env 文件已存在，密码为开发默认值 (123456, minioadmin) | 无需修改 -- 开发环境足够 |
| Build artifacts | 无 | 无 -- 后端前端本地运行，不做 Docker build |

## Common Pitfalls

### Pitfall 1: Docker volume 残留导致 Flyway 不执行 V3
**What goes wrong:** 如果之前用 docker-compose.yml 启动过，MySQL volume 中已有 Flyway history 记录 V1 和 V2。添加 V3 后直接启动即可，但如果 volume 中有旧的 checksum 不匹配（比如手动修改过 V1/V2），Flyway 会报错。
**Why it happens:** Flyway 默认校验已执行 migration 的 checksum。
**How to avoid:** 不要修改 V1/V2。如果之前有过不一致的修改，需要 `docker-compose down -v` 删除 volume 重建。
**Warning signs:** 后端启动日志出现 `FlywayMigrationSqlException` 或 `Migration checksum mismatch`。

### Pitfall 2: Docker profile 下 Swagger 不可访问
**What goes wrong:** 如果后端使用 `SPRING_PROFILES_ACTIVE=docker`，Swagger UI 会被禁用。
**Why it happens:** application-docker.yml 中 `springdoc.swagger-ui.enabled: false`。
**How to avoid:** 确保后端使用 default profile（不加 `-Dspring.profiles.active=docker`），`mvn spring-boot:run` 默认就是 default profile。
**Warning signs:** 访问 `/api/v1/swagger-ui.html` 返回 404。

### Pitfall 3: 验证码拦截登录请求
**What goes wrong:** Login.vue 前端要求输入验证码（captchaInput 是 required field），但后端 LoginRequest 中 captcha 和 captchaKey 是可选的。前端总是发送 captcha，但后端只在 captchaKey != null 时验证。
**Why it happens:** 前端登录表单在 mount 时调用 `generateCaptcha()`，将 captchaKey 填入请求。如果 CaptchaService 或 Redis 出问题，验证码生成失败会导致登录流程卡住。
**How to avoid:** 健康检查脚本应验证 captcha 端点可用 (`POST /api/v1/auth/captcha`)。测试登录时确保验证码图片正常加载。
**Warning signs:** 登录页面验证码图片显示为空白占位符。

### Pitfall 4: V3 migration 中 AUTHENTICATOR 枚举 ID 冲突
**What goes wrong:** 如果 V3 插入 `user_type_list` id=5 的记录，但数据库中已有 id=5（不太可能但需注意）。
**Why it happens:** V2 只插入了 id 1-4，id=5 应该可用。
**How to avoid:** 使用 `INSERT IGNORE` 或先 `SELECT` 检查是否存在，幂等设计。
**Warning signs:** Flyway 执行 V3 时报 `Duplicate entry '5' for key 'PRIMARY'`。

### Pitfall 5: 前端 Vite proxy 在后端未启动时静默失败
**What goes wrong:** `npm run dev` 即使后端未启动也能成功，但所有 API 调用会返回 proxy 错误。
**Why it happens:** Vite proxy 配置只做 HTTP 转发，不验证目标是否可达。
**How to avoid:** 健康检查脚本先验证后端 (8080) 再验证前端 (5173)。启动顺序：Docker infra -> 后端 -> 前端。
**Warning signs:** 前端可访问但登录请求返回 Network Error。

### Pitfall 6: Windows 下 bash 脚本中 mysql/redis-cli 命令不可用
**What goes wrong:** health-check.sh 使用 `mysqladmin`、`redis-cli` 等命令，但 Windows 环境下这些可能不在 PATH 中。
**Why it happens:** MySQL 和 Redis 运行在 Docker 容器内，host 上不一定有对应 CLI 客户端。
**How to avoid:** 使用 `docker exec` 执行健康检查命令（如 `docker exec oaiss-mysql mysqladmin ping`），或者使用纯 HTTP/TCP 检查（如 `curl` 检查端口连通性）。
**Warning signs:** 脚本执行时 `command not found: mysqladmin`。

### Pitfall 7: BCrypt 密码 hash 复用
**What goes wrong:** V3 中 enterprise003 的密码需要使用相同的 BCrypt hash（对应明文 `admin123`）。
**Why it happens:** V2 中所有用户使用同一个 BCrypt hash `$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva`。
**How to avoid:** 直接复制 V2 中的 hash 值到 V3 INSERT 语句中。
**Warning signs:** enterprise003 登录失败，报 "用户名或密码错误"。

## Code Examples

### Login Request (no captcha -- API-only testing)
```bash
# Source: [VERIFIED: AuthController.java + AuthService.java]
# Login without captcha (captcha/captchaKey are optional in LoginRequest)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# Response: { "code": 200, "data": { "accessToken": "...", "refreshToken": "...", "userType": 4, ... } }
```

### Login Request (with captcha -- browser testing)
```bash
# Source: [VERIFIED: Login.vue + CaptchaService]
# Step 1: Get captcha
curl -X POST http://localhost:8080/api/v1/auth/captcha
# Response: { "code": 200, "data": { "captchaKey": "...", "captchaImage": "data:image/png;base64,..." } }

# Step 2: Login with captcha
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","captchaKey":"...","captcha":"..."}'
```

### Verify Token Blacklist After Logout
```bash
# Source: [VERIFIED: AuthService.java:266-275 + JwtAuthenticationFilter.java:76-80]
# Step 1: Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

# Step 2: Access protected endpoint (should succeed)
curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"

# Step 3: Logout
curl -s -X POST http://localhost:8080/api/v1/auth/logout -H "Authorization: Bearer $TOKEN"

# Step 4: Access again with same token (should fail -- blacklisted)
curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
# Expected: 401 Unauthorized
```

### Role-to-Home-Path Mapping (Frontend)
```typescript
// Source: [VERIFIED: oaiss-chain-frontend/src/config/menu.ts]
export const ROLE_HOME: Record<RoleType, string> = {
  ENTERPRISE:    '/enterprise/carbon/upload',
  REVIEWER:      '/auditor/audit/list',
  AUTHENTICATOR: '/authenticator/verify/list',
  THIRD_PARTY:   '/third-party/monitor',
  ADMIN:         '/admin/system/users',
}
```

### Role-to-UserType Mapping (Backend)
```java
// Source: [VERIFIED: UserTypeEnum.java + V2__seed_data.sql]
ENTERPRISE(1)    -> enterprise001, enterprise002, (enterprise003 after V3)
REVIEWER(2)      -> reviewer001
THIRD_PARTY(3)   -> thirdparty001
ADMIN(4)         -> admin
AUTHENTICATOR(5) -> authenticator001
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Full Docker stack | Infra-only Docker + local dev | D-06 decision | 更快的迭代周期，无需 Docker build 后端前端 |
| V2 seed data only | V3 adds enterprise003 + AUTHENTICATOR enum | D-02/D-03 decision | 三家企业等价起始条件，完整 5 角色枚举 |
| Manual environment checks | Automated health check script | D-14/D-15 decision | Fast-fail 验证减少调试时间 |
| JWT refresh testing included | Skip refresh in Phase 1 | D-12 decision | 减少本阶段复杂度，聚焦 login/logout |

**Deprecated/outdated:**
- `SPRING_PROFILES_ACTIVE=docker` for local testing: disables Swagger, only for production Docker mode

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | All 21 tables in V1 are sufficient and no additional tables are needed by V3 | V3 Migration | LOW -- V1 DDL verified to include rsa_key_pairs, operation_log, emission_rating |
| A2 | BCrypt hash from V2 can be reused for enterprise003 password | V3 Migration | LOW -- same hash used for all 6 accounts in V2 |
| A3 | Docker Desktop will have port 3306, 6379, 9000, 9001 available (no conflicts) | Environment | MEDIUM -- other MySQL/Redis instances on host could conflict |
| A4 | `docker exec` works in Git Bash for health check commands | Health Check | LOW -- standard Docker CLI command, no known Git Bash issues |

**If this table is empty:** All claims in this research were verified or cited -- no user confirmation needed.

## Open Questions (RESOLVED)

1. **Windows mysql/redis CLI availability** — RESOLVED
   - What we know: MySQL and Redis run inside Docker containers. Host machine may not have mysqladmin/redis-cli.
   - Resolution: Plan 01-01 uses `docker exec` for MySQL/Redis health checks (confirmed in health-check.sh action).

2. **Whether V3 should use INSERT IGNORE for idempotency** — RESOLVED
   - What we know: V3 will be a Flyway migration that runs once. Flyway guarantees single execution.
   - Resolution: Plan 01-01 uses standard INSERT (Flyway manages re-running via flyway_schema_history).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker Desktop | Infrastructure | Available | 29.4.2 | -- |
| Java JDK 17 | Backend | Available | 17 LTS | -- |
| Maven | Backend build | Available | 3.8.4 | -- |
| Node.js | Frontend | Available | 24.15.0 | -- |
| Bash | Health check script | Available | 5.2.37 (Git Bash) | PowerShell |
| curl | Health check HTTP probes | Available | (system) | -- |
| MySQL client (host) | Health check mysqladmin | Not on PATH | -- | `docker exec oaiss-mysql` |
| Redis CLI (host) | Health check PING | Not on PATH | -- | `docker exec oaiss-redis` |

**Missing dependencies with no fallback:**
- None -- all dependencies available or have viable fallbacks.

**Missing dependencies with fallback:**
- mysqladmin / redis-cli: Use `docker exec oaiss-mysql mysqladmin ping` and `docker exec oaiss-redis redis-cli ping` instead of host-installed clients.

## Validation Architecture

> nyquist_validation is enabled per .planning/config.json.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Bash script (health-check.sh) |
| Config file | None -- standalone script |
| Quick run command | `bash scripts/health-check.sh` |
| Full suite command | `bash scripts/health-check.sh` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ENV-01 | Docker infra services running | smoke | `bash scripts/health-check.sh` (checks Docker, MySQL, Redis, MinIO) | Wave 0 |
| ENV-02 | Flyway V1+V2 migration success | smoke | `docker exec oaiss-mysql mysql -u root -p123456 -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='oaiss_chain'"` | Wave 0 |
| ENV-03 | V3 migration created and applied | manual | Verify V3 file exists, then verify table counts after backend start | Wave 0 |
| ENV-04 | Backend Swagger UI accessible | smoke | `curl -sf http://localhost:8080/api/v1/swagger-ui.html` | Wave 0 |
| ENV-05 | Frontend homepage accessible | smoke | `curl -sf http://localhost:5173` | Wave 0 |
| ENV-06 | 6 seed accounts login | manual | Login via browser or curl for each account | Wave 0 |
| ENV-07 | Role home page routing correct | manual | Verify ROLE_HOME mapping per role | N/A (manual) |
| ENV-08 | JWT Bearer token valid | manual | curl login + access protected endpoint | N/A (manual) |
| ENV-09 | Token refresh | **SKIPPED** | Per D-12 decision | N/A |
| ENV-10 | Logout blacklists token | manual | curl login -> access -> logout -> access again | N/A (manual) |

### Sampling Rate
- **Per task commit:** `bash scripts/health-check.sh` (if script task)
- **Per wave merge:** Manual verification of all ENV-XX items
- **Phase gate:** Full manual walkthrough of ENV-01 through ENV-10 (except ENV-09)

### Wave 0 Gaps
- [ ] `scripts/health-check.sh` -- covers ENV-01, ENV-02, ENV-04, ENV-05 smoke checks
- [ ] `V3__test_seed_data.sql` -- covers ENV-03 (test seed data migration)
- [ ] `docker-compose.infra.yml` -- covers ENV-01 (infrastructure-only compose)

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Spring Security + JWT (jjwt 0.12.5) |
| V3 Session Management | yes | Stateless JWT, Redis token blacklist on logout |
| V4 Access Control | yes | @PreAuthorize annotations + Vue Router meta.roles |
| V5 Input Validation | yes | Jakarta Validation (@Valid @NotBlank on LoginRequest) |
| V6 Cryptography | yes | BCrypt password hashing, HMAC-SHA JWT signing |

### Known Threat Patterns for Spring Boot + JWT Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Brute force login | Tampering | Rate limiting via checkLoginRateLimit() + Redis |
| Token theft | Information Disclosure | Token stored in sessionStorage (not cookie), HTTPS in production |
| CSRF | Tampering | Disabled (JWT in sessionStorage, not auto-attached by browser) |
| SQL injection | Tampering | Spring Data JPA parameterized queries |
| Swagger exposure in production | Information Disclosure | application-docker.yml disables Swagger; default profile enables for dev only |

## Sources

### Primary (HIGH confidence)
- `docker-compose.yml` -- Service definitions, ports, volumes, health checks
- `V1__init_schema.sql` -- 21 table DDL, verified complete
- `V2__seed_data.sql` -- 6 seed accounts, BCrypt hash, enterprise/reviewer/third-party/authenticator records
- `SecurityConfig.java` -- JWT filter chain, CORS config, Swagger endpoints
- `JwtTokenProvider.java` -- Token generation, validation, expiration (3600000ms = 1 hour)
- `AuthService.java` -- Login flow, logout blacklist, captcha handling
- `AuthController.java` -- REST endpoints for /auth/login, /auth/logout, /auth/refresh, /auth/captcha
- `request.ts` -- Axios interceptor, pagination transform, token refresh logic
- `auth.ts` -- Token storage (sessionStorage access, localStorage refresh)
- `router/index.ts` -- Vue Router guards, role-based routing
- `store/index.ts` -- Pinia store, role extraction from JWT, homePath getter
- `menu.ts` -- ROLE_HOME mapping, MENU_BY_ROLE configuration
- `UserTypeEnum.java` -- 5 user types including AUTHENTICATOR(5)
- `CacheConfig.java` -- Redis cache manager with fallback to ConcurrentMapCache
- `application.yml` -- Flyway config, JWT config, MinIO config
- `application-docker.yml` -- Docker profile overrides (disables Swagger)
- `.env` -- Working defaults for local development

### Secondary (MEDIUM confidence)
- `LoginRequest.java` -- Captcha fields are optional (nullable)
- `vite.config.js` -- Proxy /api -> localhost:8080, Vitest config
- `.env.development` -- VITE_API_BASE_URL=/api/v1

### Tertiary (LOW confidence)
- None -- all findings verified from source code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all tools/versions verified via CLI commands
- Architecture: HIGH -- all components verified from source code (docker-compose, SecurityConfig, JwtTokenProvider, AuthService, Vue Router, Pinia store)
- Pitfalls: HIGH -- derived from direct code analysis of Flyway config, docker profile, login flow
- V3 migration design: HIGH -- table schemas and seed data patterns verified from V1/V2
- Health check design: MEDIUM -- bash available but mysqladmin/redis-cli need docker exec workaround

**Research date:** 2026-05-08
**Valid until:** 30 days (stable infrastructure, no fast-moving dependencies)
