#!/usr/bin/env bash
# OAISS CHAIN - 统一启动脚本
# 用法: ./scripts/start-all.sh [选项]
#   --with-fabric    同时启动 Fabric 网络
#   --skip-frontend  跳过前端启动
#   --skip-ml        跳过 ML 服务启动
#   --skip-backend   跳过后端启动
#   --infra-only     仅启动基础设施（MySQL/Redis/MinIO）

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 加载 .env
if [[ -f "$PROJECT_ROOT/.env" ]]; then
    set -a; source "$PROJECT_ROOT/.env"; set +a
fi

# 默认值
WITH_FABRIC=false
SKIP_FRONTEND=false
SKIP_ML=false
SKIP_BACKEND=false
INFRA_ONLY=false

# 解析参数
for arg in "$@"; do
    case $arg in
        --with-fabric)  WITH_FABRIC=true ;;
        --skip-frontend) SKIP_FRONTEND=true ;;
        --skip-ml)      SKIP_ML=true ;;
        --skip-backend) SKIP_BACKEND=true ;;
        --infra-only)   INFRA_ONLY=true ;;
        *) echo "未知参数: $arg"; exit 1 ;;
    esac
done

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[START]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }

check_health() {
    local url=$1 name=$2 max_wait=${3:-30}
    local waited=0
    while (( waited < max_wait )); do
        if curl -sf "$url" > /dev/null 2>&1; then
            log "$name 已就绪"
            return 0
        fi
        sleep 1
        ((waited++))
    done
    warn "$name 在 ${max_wait}s 内未就绪（可能仍需等待）"
    return 1
}

# ─── 1. 基础设施 ───
log "启动基础设施（MySQL, Redis, MinIO）..."
docker compose -f "$PROJECT_ROOT/docker-compose.infra.yml" up -d

log "等待 MySQL 就绪..."
check_health "http://localhost:3306" "MySQL" 30 || true

log "等待 Redis 就绪..."
check_health "http://localhost:6379" "Redis" 15 || true

log "等待 MinIO 就绪..."
check_health "http://localhost:9002/minio/health/live" "MinIO" 30 || true

if [[ "$INFRA_ONLY" == true ]]; then
    log "仅基础设施模式，跳过应用服务"
    exit 0
fi

# ─── 2. Fabric 网络（可选）───
if [[ "$WITH_FABRIC" == true ]]; then
    log "启动 Fabric 网络..."
    docker compose -f "$PROJECT_ROOT/docker-compose.fabric.yml" up -d
    sleep 5
else
    warn "跳过 Fabric 网络（使用 --with-fabric 启用）"
fi

# ─── 3. 后端 ───
if [[ "$SKIP_BACKEND" != true ]]; then
    log "启动后端（Spring Boot）..."
    cd "$PROJECT_ROOT/oaiss-chain-backend"
    SPRING_PROFILES_ACTIVE=local \
    mvn spring-boot:run -Dspring-boot.run.profiles=local &
    BACKEND_PID=$!
    log "后端 PID: $BACKEND_PID，等待就绪..."
    check_health "http://localhost:8080/actuator/health" "Backend" 90 || true
else
    warn "跳过后端"
fi

# ─── 4. ML 服务 ───
if [[ "$SKIP_ML" != true ]]; then
    log "启动 ML 服务（FastAPI）..."
    cd "$PROJECT_ROOT/oaiss-chain-ml-service"
    if command -v uvicorn &> /dev/null; then
        uvicorn app.main:app --port 8001 &
        ML_PID=$!
        log "ML 服务 PID: $ML_PID"
    else
        warn "uvicorn 未安装，跳过 ML 服务"
    fi
else
    warn "跳过 ML 服务"
fi

# ─── 5. 前端 ───
if [[ "$SKIP_FRONTEND" != true ]]; then
    log "启动前端（Vue 3）..."
    cd "$PROJECT_ROOT/oaiss-chain-frontend"
    npm run dev &
    FRONTEND_PID=$!
    log "前端 PID: $FRONTEND_PID"
else
    warn "跳过前端"
fi

cd "$PROJECT_ROOT"
echo ""
log "========================================="
log "OAISS CHAIN 服务已启动"
log "  后端:   http://localhost:8080"
log "  前端:   http://localhost:5173"
log "  ML:     http://localhost:8001"
log "  MinIO:  http://localhost:9003"
log "========================================="
log "使用 ./scripts/stop-all.sh 停止所有服务"
