---
status: draft
priority: high
created: 2026-05-10
author: claude
related:
  - Phase 2 UAT findings
  - Test data management
---

# SPEC: 测试数据清理机制

## 问题描述

当前测试存在以下问题：
1. 多次运行测试会创建重复数据
2. Emission Rating唯一性约束导致后续测试失败
3. 需要手动清理数据库
4. 测试数据污染开发环境

## 当前状态

```sql
-- 问题：企业每年只能有一个评级
-- 第一次测试：创建2025年评级 -> 成功
-- 第二次测试：创建2025年评级 -> 失败（唯一约束冲突）
SELECT * FROM emission_rating WHERE enterprise_id = 1 AND rating_year = '2025';
-- 结果：已存在记录，阻塞测试
```

## 解决方案分析

### 方案A: 测试前清理（推荐）

**优点**：
- 确保测试环境干净
- 测试可重复运行
- 不影响其他数据

**缺点**：
- 需要识别测试数据
- 可能误删有用数据

**实施**：
```bash
# 在测试脚本开头添加清理逻辑
cleanup_test_data() {
    mysql -uroot -p123456 oaiss_chain -e "
        DELETE FROM emission_rating WHERE remark LIKE '%TEST%' OR remark LIKE '%UAT%';
        DELETE FROM credit_event WHERE event_type LIKE '%TEST%';
        DELETE FROM carbon_report WHERE title LIKE '%TEST%' OR title LIKE '%UAT%' OR title LIKE '%CARB-TEST%';
        DELETE FROM carbon_report WHERE report_no LIKE '%TEST%' OR report_no LIKE '%UAT%';
    " 2>/dev/null || true
}
```

### 方案B: 使用测试专用企业

**优点**：
- 隔离测试数据
- 不影响真实数据

**缺点**：
- 需要预置测试企业
- 增加种子数据复杂度

**实施**：
```sql
-- 创建测试专用企业
INSERT INTO enterprise (id, enterprise_name, credit_code, user_id)
VALUES (999, 'TEST_ENTERPRISE', 'TEST99999999999999', 999);

-- 测试后清理
DELETE FROM emission_rating WHERE enterprise_id = 999;
DELETE FROM carbon_report WHERE enterprise_id = 999;
```

### 方案C: 使用事务回滚

**优点**：
- 自动清理
- 数据一致性

**缺点**：
- 不适用于集成测试
- 无法测试提交行为

**实施**：
```java
@Test
@Transactional
@Rollback
void testCarbonReportLifecycle() {
    // 测试代码自动回滚
}
```

### 方案D: 使用Testcontainers

**优点**：
- 完全隔离
- 每次测试全新环境

**缺点**：
- 启动慢
- 资源消耗大

## 推荐方案

**采用方案A + 方案B组合**：

1. 创建测试专用清理脚本
2. 使用特定前缀标识测试数据
3. 集成到测试脚本和CI流程

## 实施细节

### 1. 测试数据命名规范

```markdown
## 测试数据命名规范

| 实体 | 命名规则 | 示例 |
|------|----------|------|
| Carbon Report | TEST-{功能}-{时间戳} | TEST-CARBON-1778393173 |
| Emission Rating | remark字段标记 | TEST_RATING |
| Credit Event | event_type字段标记 | TEST_BONUS |

测试数据必须包含以下标识之一：
- 标题/名称以 TEST- 或 UAT- 开头
- remark字段包含 TEST 或 UAT
- report_no包含 TEST 或 UAT
```

### 2. 清理脚本

```bash
#!/bin/bash
# scripts/cleanup-test-data.sh
# 清理测试数据，保留种子数据

set -e

MYSQL_HOST="${DB_HOST:-localhost}"
MYSQL_PORT="${DB_PORT:-3306}"
MYSQL_USER="${DB_USERNAME:-root}"
MYSQL_PASS="${DB_PASSWORD:-123456}"
MYSQL_DB="oaiss_chain"

echo "Cleaning test data..."

mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" "$MYSQL_DB" <<EOF
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
WHERE operation_desc LIKE '%TEST%'
   OR operation_desc LIKE '%UAT%';

-- 显示清理结果
SELECT 'Cleanup complete:' AS status;
SELECT CONCAT('Emission ratings: ', COUNT(*)) AS count FROM emission_rating;
SELECT CONCAT('Carbon reports: ', COUNT(*)) AS count FROM carbon_report;
SELECT CONCAT('Credit events: ', COUNT(*)) AS count FROM credit_event;
EOF

echo "Test data cleanup complete!"
```

### 3. 集成到测试脚本

```bash
# scripts/carbon-report-test.sh 修改

#!/bin/bash
set -e

# ... 现有变量定义 ...

# 新增：测试前清理
cleanup_test_data() {
    echo "Cleaning up existing test data..."
    bash scripts/cleanup-test-data.sh 2>/dev/null || true
}

# 新增：测试后清理（可选）
cleanup_after_test() {
    if [ "$KEEP_TEST_DATA" != "true" ]; then
        echo "Cleaning up test data..."
        bash scripts/cleanup-test-data.sh 2>/dev/null || true
    fi
}

# 主流程
main() {
    cleanup_test_data  # 测试前清理
    
    # ... 现有测试逻辑 ...
    
    cleanup_after_test  # 测试后清理
}

trap cleanup_after_test EXIT  # 确保异常时也清理
```

### 4. 添加npm脚本

```json
// package.json
{
  "scripts": {
    "test:cleanup": "bash scripts/cleanup-test-data.sh",
    "test:carbon": "bash scripts/cleanup-test-data.sh && bash scripts/carbon-report-test.sh"
  }
}
```

### 5. CI集成

```yaml
# .github/workflows/test.yml
jobs:
  test:
    steps:
      - name: Cleanup test data
        run: bash scripts/cleanup-test-data.sh

      - name: Run tests
        run: bash scripts/carbon-report-test.sh

      - name: Cleanup after test
        if: always()
        run: bash scripts/cleanup-test-data.sh
```

## 验证清单

- [ ] 清理脚本可独立运行
- [ ] 测试脚本集成清理逻辑
- [ ] 多次运行测试不冲突
- [ ] 种子数据不受影响
- [ ] CI流程正常工作

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 误删有用数据 | 高 | 使用明确的命名规范，只删除匹配模式的数据 |
| 清理失败 | 中 | 使用 `|| true` 忽略错误，不阻塞测试 |
| 性能影响 | 低 | 清理操作简单，影响小 |

## 回滚方案

如果清理脚本出现问题：
1. 从备份恢复数据
2. 移除测试前清理，仅保留测试后清理
3. 手动清理冲突数据
