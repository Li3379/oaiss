#!/usr/bin/env bash
# OAISS CHAIN - 后端启动脚本
# 用法: ./scripts/start-backend.sh [选项]
#   --with-fabric    使用 local,fabric profiles

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

WITH_FABRIC=false

for arg in "$@"; do
    case $arg in
        --with-fabric) WITH_FABRIC=true ;;
        *) echo "未知参数: $arg"; exit 1 ;;
    esac
done

if [[ -f "$PROJECT_ROOT/.env" ]]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

if [[ "$WITH_FABRIC" == true ]]; then
    export SPRING_PROFILES_ACTIVE=local,fabric
    SPRING_ARGS=(-Dspring-boot.run.profiles=local,fabric)
else
    export SPRING_PROFILES_ACTIVE=local
    SPRING_ARGS=(-Dspring-boot.run.profiles=local)
fi

echo
echo "========================================"
echo " OAISS CHAIN Backend"
echo " Profiles: $SPRING_PROFILES_ACTIVE"
echo "========================================"
echo

cd "$PROJECT_ROOT/oaiss-chain-backend"
exec mvn spring-boot:run "${SPRING_ARGS[@]}"
