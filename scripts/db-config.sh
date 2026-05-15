#!/bin/bash
# scripts/db-config.sh
# 数据库配置统一管理
# Database configuration management
#
# 使用方法:
#   source scripts/db-config.sh
#
# 环境变量优先级高于默认值:
#   DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD

# 默认配置（Docker MySQL）
DB_HOST_DEFAULT="localhost"
DB_PORT_DEFAULT="3306"
DB_USERNAME_DEFAULT="root"
DB_PASSWORD_DEFAULT="123456"
DB_NAME_DEFAULT="oaiss_chain"

# Docker容器名称
MYSQL_CONTAINER="oaiss-mysql"

# 应用环境变量（如果设置）
DB_HOST="${DB_HOST:-$DB_HOST_DEFAULT}"
DB_PORT="${DB_PORT:-$DB_PORT_DEFAULT}"
DB_USERNAME="${DB_USERNAME:-$DB_USERNAME_DEFAULT}"
DB_PASSWORD="${DB_PASSWORD:-$DB_PASSWORD_DEFAULT}"
DB_NAME="${DB_NAME:-$DB_NAME_DEFAULT}"

# MySQL连接字符串
MYSQL_CONN="-h$DB_HOST -P$DB_PORT -u$DB_USERNAME -p$DB_PASSWORD"

# Docker MySQL连接（如果容器运行中）
docker_mysql_exec() {
    if docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
        docker exec "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$@"
    else
        echo "Error: MySQL container '$MYSQL_CONTAINER' is not running"
        return 1
    fi
}

# 检查MySQL是否健康
check_mysql_health() {
    if docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
        docker exec "$MYSQL_CONTAINER" mysqladmin ping -h localhost -u"$DB_USERNAME" -p"$DB_PASSWORD" --silent 2>/dev/null
    else
        mysql $MYSQL_CONN -e "SELECT 1" >/dev/null 2>&1
    fi
}

# 打印配置信息
print_db_config() {
    echo "数据库配置:"
    echo "  Host: $DB_HOST:$DB_PORT"
    echo "  User: $DB_USERNAME"
    echo "  Database: $DB_NAME"
    echo "  Container: $MYSQL_CONTAINER"
}