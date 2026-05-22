#!/usr/bin/env bash
# OAISS CHAIN - 统一停止脚本
# 用法: ./scripts/stop-all.sh [选项]
#   --with-fabric    同时停止 Fabric 网络
#   --infra-only     仅停止基础设施

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

WITH_FABRIC=false
INFRA_ONLY=false

for arg in "$@"; do
    case $arg in
        --with-fabric) WITH_FABRIC=true ;;
        --infra-only)  INFRA_ONLY=true ;;
        *) echo "未知参数: $arg"; exit 1 ;;
    esac
done

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[STOP]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

# 停止本地进程
log "停止本地进程..."
for pid_file in /tmp/oaiss-backend.pid /tmp/oaiss-ml.pid /tmp/oaiss-frontend.pid; do
    if [[ -f "$pid_file" ]]; then
        pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            log "停止 PID $pid..."
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$pid_file"
    fi
done

# 停止基础设施
log "停止基础设施容器..."
docker compose -f "$PROJECT_ROOT/docker-compose.infra.yml" down

# 停止 Fabric
if [[ "$WITH_FABRIC" == true ]]; then
    log "停止 Fabric 网络..."
    docker compose -f "$PROJECT_ROOT/docker-compose.fabric.yml" down
fi

if [[ "$INFRA_ONLY" != true ]]; then
    # 停止全栈 compose（如果之前用 docker-compose.yml 启动）
    log "停止全栈容器（如有）..."
    docker compose -f "$PROJECT_ROOT/docker-compose.yml" down 2>/dev/null || true
fi

log "所有服务已停止"