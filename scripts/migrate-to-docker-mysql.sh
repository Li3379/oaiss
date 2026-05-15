#!/bin/bash
# scripts/migrate-to-docker-mysql.sh
# 数据迁移脚本：从本地MySQL迁移到Docker MySQL

set -e

# 加载数据库配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/db-config.sh"

echo "=========================================="
echo "MySQL数据迁移脚本"
echo "=========================================="
echo ""
echo "此脚本将："
echo "1. 备份本地MySQL数据"
echo "2. 停止本地MySQL服务"
echo "3. 重启Docker MySQL使用3306端口"
echo "4. 导入数据到Docker MySQL"
echo ""

# 配置
BACKUP_DIR="./backups"
BACKUP_FILE="${BACKUP_DIR}/oaiss_chain_backup_$(date +%Y%m%d_%H%M%S).sql"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "步骤1: 备份本地MySQL数据..."
echo "备份文件: $BACKUP_FILE"

mysqldump -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✓ 备份成功: $(ls -lh "$BACKUP_FILE" | awk '{print $5}')"
else
    echo "✗ 备份失败，请检查MySQL连接"
    exit 1
fi

echo ""
echo "步骤2: 停止本地MySQL服务..."
echo "请手动执行以下命令（需要管理员权限）："
echo ""
echo "Windows:"
echo "  net stop mysql"
echo "  或在服务管理器中停止MySQL服务"
echo ""
echo "Linux/Mac:"
echo "  sudo systemctl stop mysql"
echo "  或 sudo service mysql stop"
echo ""
read -p "本地MySQL已停止？(y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "迁移已取消"
    exit 0
fi

echo ""
echo "步骤3: 重启Docker MySQL..."
# 停止并删除现有容器
docker-compose -f docker-compose.infra.yml down
# 重新启动（使用3306端口）
docker-compose -f docker-compose.infra.yml up -d mysql

# 等待MySQL就绪
echo "等待MySQL启动..."
sleep 10

# 检查健康状态
for i in {1..30}; do
    if docker exec "$MYSQL_CONTAINER" mysqladmin ping -h localhost -u"$DB_USERNAME" -p"$DB_PASSWORD" --silent 2>/dev/null; then
        echo "✓ Docker MySQL已就绪"
        break
    fi
    sleep 1
done

echo ""
echo "步骤4: 导入数据到Docker MySQL..."
docker exec -i "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✓ 数据导入成功"
else
    echo "✗ 数据导入失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "迁移完成！"
echo "=========================================="
echo ""
echo "验证："
docker exec "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "
    SELECT 'Tables count:' AS info, COUNT(*) AS value FROM information_schema.tables WHERE table_schema = '$DB_NAME'
    UNION ALL
    SELECT 'Carbon reports:', COUNT(*) FROM $DB_NAME.carbon_report
    UNION ALL
    SELECT 'Enterprises:', COUNT(*) FROM $DB_NAME.enterprise;
"

echo ""
echo "备份文件位置: $BACKUP_FILE"
echo "请妥善保管备份文件"
