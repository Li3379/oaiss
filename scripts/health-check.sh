#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}[..]${NC} $1"; }

# 1. Docker Desktop running (D-09)
info "Checking Docker Desktop..."
docker info &>/dev/null || fail "Docker Desktop is not running. Start Docker Desktop and retry."
ok "Docker Desktop is running"

# 2. MySQL healthy on port 3306 (D-14)
info "Checking MySQL (oaiss-mysql)..."
docker exec oaiss-mysql mysqladmin ping -h localhost --silent &>/dev/null \
  || fail "MySQL not responding. Run: docker-compose -f docker-compose.infra.yml up -d"
ok "MySQL is healthy on :3306"

# 3. Flyway tables count (D-16)
info "Checking Flyway migrations (21 tables)..."
TABLE_COUNT=$(docker exec oaiss-mysql mysql -u root -p123456 -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='oaiss_chain'" -sN 2>/dev/null)
[[ "$TABLE_COUNT" -ge 21 ]] || fail "Expected 21+ tables, found $TABLE_COUNT. Flyway migration may have failed."
ok "Database has $TABLE_COUNT tables (>= 21 expected)"

# 4. V3 seed data present (D-16)
info "Checking V3 seed data..."
AUTH_COUNT=$(docker exec oaiss-mysql mysql -u root -p123456 -e \
  "SELECT COUNT(*) FROM oaiss_chain.user_type_list WHERE type_code='AUTHENTICATOR'" -sN 2>/dev/null)
[[ "$AUTH_COUNT" -ge 1 ]] || fail "AUTHENTICATOR enum entry not found. V3 migration may not have run."
ENT003_COUNT=$(docker exec oaiss-mysql mysql -u root -p123456 -e \
  "SELECT COUNT(*) FROM oaiss_chain.user WHERE username='enterprise003'" -sN 2>/dev/null)
[[ "$ENT003_COUNT" -ge 1 ]] || fail "enterprise003 user not found. V3 migration may not have run."
ok "V3 seed data verified (AUTHENTICATOR enum + enterprise003)"

# 5. Redis PING (D-14)
info "Checking Redis (oaiss-redis)..."
docker exec oaiss-redis redis-cli ping 2>/dev/null | grep -q PONG \
  || fail "Redis not responding. Run: docker-compose -f docker-compose.infra.yml up -d"
ok "Redis is healthy on :6379"

# 6. MinIO console (D-14)
info "Checking MinIO console..."
curl -sf http://localhost:9001 >/dev/null \
  || fail "MinIO console not responding on :9001. Check oaiss-minio container."
ok "MinIO console accessible on :9001"

# 7. Backend Swagger UI (ENV-04)
info "Checking backend Swagger UI..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/swagger-ui.html 2>/dev/null)
[[ "$HTTP_CODE" =~ ^(200|302)$ ]] || fail "Backend Swagger UI returned HTTP $HTTP_CODE (expected 200/302). Is backend running via 'mvn spring-boot:run'?"
ok "Backend Swagger UI accessible (HTTP $HTTP_CODE)"

# 8. Frontend (ENV-05)
info "Checking frontend dev server..."
curl -sf http://localhost:5173 >/dev/null \
  || fail "Frontend not responding on :5173. Is frontend running via 'npm run dev'?"
ok "Frontend accessible on :5173"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN} All health checks passed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Services verified:"
echo "  - MySQL    : localhost:3306 (21 tables, V3 data present)"
echo "  - Redis    : localhost:6379"
echo "  - MinIO    : localhost:9000 (console :9001)"
echo "  - Backend  : localhost:8080 (Swagger UI)"
echo "  - Frontend : localhost:5173"
echo ""
echo "Seed accounts (password: admin123):"
echo "  admin, enterprise001, enterprise002, enterprise003"
echo "  reviewer001, thirdparty001, authenticator001"
