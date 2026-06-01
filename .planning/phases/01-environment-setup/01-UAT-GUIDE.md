# Phase 1 UAT - Complete Verification Guide

## Current Status

| Component | Status | Port |
|-----------|--------|------|
| Docker MySQL | ✅ Running | 3307 |
| Docker Redis | ✅ Running | 6379 |
| Docker MinIO | ✅ Running | 9000-9001 |
| Backend | ✅ Running | 8080 |
| Frontend | ❌ Not Running | 5173 |

---

## Test 1: Cold Start Smoke Test

### Verification Method

**Step 1: Stop all services**
```bash
# Stop backend (if running)
cd oaiss-chain-backend
# Windows: Find Java process and kill
netstat -ano | findstr :8080
taskkill //F //PID <PID>

# Stop frontend (if running)
cd oaiss-chain-frontend
# Ctrl+C in terminal running npm run dev

# Stop Docker containers (optional - for full cold start)
docker-compose -f docker-compose.infra.yml down
```

**Step 2: Clear ephemeral state**
```bash
# Clear Redis cache
docker exec oaiss-redis redis-cli FLUSHALL

# Clear MinIO bucket (optional)
docker exec oaiss-minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec oaiss-minio mc rb --force local/oaiss-chain
docker exec oaiss-minio mc mb local/oaiss-chain
```

**Step 3: Start services from scratch**
```bash
# Start Docker infrastructure
docker-compose -f docker-compose.infra.yml up -d

# Wait for MySQL to be ready (30 seconds)
sleep 30

# Start backend
cd oaiss-chain-backend
mvn spring-boot:run

# Wait for backend to start (60 seconds)
# Then start frontend
cd oaiss-chain-frontend
npm run dev
```

**Step 4: Verify primary query returns live data**
```bash
# Health check
bash scripts/health-check.sh

# Or manual verification:
curl -s http://localhost:8080/api/v1/auth/login -X POST -H "Content-Type: application/json" -d '{"account":"admin","password":"admin123"}' | head -c 100
```

**Expected Result:**
- Server boots without errors
- Flyway migrations complete (check logs for "Flyway migration successful")
- Health check passes all 8 checks
- Login API returns token

---

## Test 2-6: Automated Verification

Run the health check script:
```bash
bash scripts/health-check.sh
```

This verifies:
- Test 2: Docker Infrastructure (MySQL, Redis, MinIO)
- Test 3: Database Schema (21 tables)
- Test 4: Seed Data (7 accounts + enterprise003)
- Test 5: Health Check Script passes
- Test 6: Backend/Frontend accessible

---

## Test 7: Login/Auth Cycle

Run the login test script:
```bash
bash scripts/login-test.sh
```

This verifies all 7 accounts:
- admin (userType=4)
- enterprise001 (userType=1)
- enterprise002 (userType=1)
- enterprise003 (userType=1)
- reviewer001 (userType=2)
- thirdparty001 (userType=3)
- authenticator001 (userType=5)

Each account goes through:
1. Login → receive JWT token
2. Verify userType matches expected
3. Access /auth/me with Bearer token
4. Logout
5. Verify token blacklisted (returns code 2000)

---

## Test 8: Browser Role Routing

### Manual Browser Verification

1. Open browser to http://localhost:5173
2. Login as each role and verify routing:

| Role | Account | Password | Expected Home | Sidebar Items |
|------|---------|----------|---------------|---------------|
| ADMIN | admin | admin123 | /admin/system/users | 管理员, 系统管理, 用户管理... |
| ENTERPRISE | enterprise001 | ent001 | /enterprise/carbon/upload | 企业用户, 碳核算, 上传审核... |
| REVIEWER | reviewer001 | rev001 | /auditor/audit/list | 审核员, 审核材料... |
| THIRD_PARTY | thirdparty001 | tp001 | /third-party/monitor | 第三方监管, 监管中心... |
| AUTHENTICATOR | authenticator001 | auth001 | /authenticator/verify/list | 认证员, 认证管理... |

### Automated Playwright Verification

```bash
cd oaiss-chain-frontend
npx playwright test tests/e2e/smoke/auth-routing.spec.ts --headed
```

---

## Quick Start Commands

If services are not running, execute:

```bash
# Terminal 1: Backend
cd oaiss-chain-backend && mvn spring-boot:run

# Terminal 2: Frontend
cd oaiss-chain-frontend && npm run dev

# Terminal 3: Run verification
cd ..
bash scripts/health-check.sh
bash scripts/login-test.sh
```

---

## Response Format

For each test, respond with:
- **"pass"** or **"yes"** - if expected behavior matches
- **"skip"** - if cannot test (provide reason)
- **Description of issue** - if something is wrong (I will infer severity)

Ready to proceed with Test 1?