#!/bin/bash
# scripts/cleanup-test-data.sh
# 清理测试数据，保留种子数据
# 使用方法: ./scripts/cleanup-test-data.sh

set -e

# 加载数据库配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/db-config.sh"

echo "=========================================="
echo "清理测试数据"
echo "=========================================="
echo ""
print_db_config
echo ""

# 检查MySQL连接
if ! check_mysql_health; then
    echo "错误: MySQL连接失败"
    exit 1
fi

# 执行清理
if docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
    # 使用Docker容器
    docker exec -i "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
-- 清理测试相关的排放评级
DELETE FROM emission_rating
WHERE remark LIKE '%TEST%'
   OR remark LIKE '%UAT%'
   OR remark LIKE '%CARB-TEST%';

-- 清理测试相关的信用事件
DELETE FROM credit_event
WHERE event_type LIKE '%TEST%'
   OR event_type LIKE '%UAT%';

-- 清理测试相关的碳报告
DELETE FROM carbon_report
WHERE title LIKE 'TEST-%'
   OR title LIKE 'UAT-%'
   OR title LIKE '%CARB-TEST%'
   OR title LIKE '%STATE-TEST%'
   OR title LIKE '%<script>%'
   OR title LIKE '%OR 1=1%'
   OR report_no LIKE '%TEST%'
   OR report_no LIKE '%UAT%';

-- 清理测试相关的操作日志
DELETE FROM operation_log
WHERE description LIKE '%TEST%'
   OR description LIKE '%UAT%';

-- 显示清理结果
SELECT '清理完成' AS status;
SELECT CONCAT('排放评级: ', COUNT(*)) AS count FROM emission_rating;
SELECT CONCAT('碳报告: ', COUNT(*)) AS count FROM carbon_report;
SELECT CONCAT('信用事件: ', COUNT(*)) AS count FROM credit_event;
EOF
else
    # 使用本地MySQL
    mysql $MYSQL_CONN "$DB_NAME" <<EOF
-- 清理测试相关的排放评级
DELETE FROM emission_rating
WHERE remark LIKE '%TEST%'
   OR remark LIKE '%UAT%'
   OR remark LIKE '%CARB-TEST%';

-- 清理测试相关的信用事件
DELETE FROM credit_event
WHERE event_type LIKE '%TEST%'
   OR event_type LIKE '%UAT%';

-- 清理测试相关的碳报告
DELETE FROM carbon_report
WHERE title LIKE 'TEST-%'
   OR title LIKE 'UAT-%'
   OR title LIKE '%CARB-TEST%'
   OR title LIKE '%STATE-TEST%'
   OR title LIKE '%<script>%'
   OR title LIKE '%OR 1=1%'
   OR report_no LIKE '%TEST%'
   OR report_no LIKE '%UAT%';

-- 清理测试相关的操作日志
DELETE FROM operation_log
WHERE description LIKE '%TEST%'
   OR description LIKE '%UAT%';

-- 显示清理结果
SELECT '清理完成' AS status;
SELECT CONCAT('排放评级: ', COUNT(*)) AS count FROM emission_rating;
SELECT CONCAT('碳报告: ', COUNT(*)) AS count FROM carbon_report;
SELECT CONCAT('信用事件: ', COUNT(*)) AS count FROM credit_event;
EOF
fi

echo ""
echo "✓ 测试数据清理完成"
