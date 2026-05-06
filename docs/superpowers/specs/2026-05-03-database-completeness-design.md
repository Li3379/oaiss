# 数据库 SQL 全面完善设计

> 日期: 2026-05-03 | 状态: 已批准 | 方案: B（整体重建）

## 背景

当前数据库存在以下问题：

1. **种子数据重复** — `data.sql` 和 `V2__seed_data.sql` 内容几乎完全相同
2. **缺少 AUTHENTICATOR 类型** — `user_type_list` 仅定义4种类型，但认证机构用户使用 `user_type=5`
3. **索引严重不足** — 仅 `operation_log` 有索引，其他20张表的 FK 列和高频查询列均无索引
4. **无物理外键约束** — DDL 中没有任何 FOREIGN KEY，仅靠 Java 代码维护关联关系

## 方案

**方案B：整体重建 V1 + 修复 V2 + 删除 data.sql**

- 将 FK 约束和索引直接写入 `V1__init_schema.sql`
- 修复 `V2__seed_data.sql`（补充 AUTHENTICATOR 类型和相关权限）
- 删除 `data.sql`（与 V2 重复且会冲突）

## 变更清单

### 文件操作

| 操作 | 文件 | 说明 |
|------|------|------|
| 重写 | `V1__init_schema.sql` | 添加33个FK + 约30个索引 + 调整建表顺序 |
| 修复 | `V2__seed_data.sql` | 补充 AUTHENTICATOR 类型(5) + 认证机构API权限 |
| 删除 | `data.sql` | 与 V2 重复，Flyway 已管理数据迁移 |
| 更新 | `DATABASE_SCHEMA.md` | 同步更新文档 |

### V1 变更详情

#### 建表顺序（确保 FK 引用正确）

```
user → user_type_list → enterprise → reviewer → reviewer_qualification
→ third_party_org → authenticator → carbon_report → transaction
→ account_permission_list → entry_permission → auction_order
→ matching_result → rsa_key_pair → credit_score → credit_event
→ carbon_coin_account → carbon_coin_transaction → emission_rating
→ carbon_neutral_project → operation_log
```

#### 新增外键（33个）

所有 FK 使用 `ON DELETE RESTRICT`，命名规则 `fk_{子表}_{父表}`：

| # | 子表 | 列 | 父表 | 列 |
|---|------|-----|------|-----|
| 1 | enterprise | user_id | user | id |
| 2 | reviewer | user_id | user | id |
| 3 | reviewer_qualification | reviewer_id | reviewer | id |
| 4 | third_party_org | user_id | user | id |
| 5 | authenticator | user_id | user | id |
| 6 | carbon_report | enterprise_id | enterprise | id |
| 7 | carbon_report | submitter_id | user | id |
| 8 | carbon_report | reviewer_id | user | id |
| 9 | transaction | seller_id | user | id |
| 10 | transaction | buyer_id | user | id |
| 11 | transaction | report_id | carbon_report | id |
| 12 | auction_order | user_id | user | id |
| 13 | matching_result | buy_order_id | auction_order | id |
| 14 | matching_result | sell_order_id | auction_order | id |
| 15 | matching_result | buyer_id | user | id |
| 16 | matching_result | seller_id | user | id |
| 17 | matching_result | transaction_id | transaction | id |
| 18 | rsa_key_pair | user_id | user | id |
| 19 | credit_score | enterprise_id | enterprise | id |
| 20 | credit_event | enterprise_id | enterprise | id |
| 21 | credit_event | related_report_id | carbon_report | id |
| 22 | credit_event | related_trade_id | transaction | id |
| 23 | credit_event | triggered_by | user | id |
| 24 | carbon_coin_account | user_id | user | id |
| 25 | carbon_coin_transaction | user_id | user | id |
| 26 | carbon_coin_transaction | related_trade_id | transaction | id |
| 27 | carbon_coin_transaction | counterpart_id | user | id |
| 28 | emission_rating | enterprise_id | enterprise | id |
| 29 | emission_rating | rated_by | user | id |
| 30 | carbon_neutral_project | owner_id | user | id |
| 31 | carbon_neutral_project | reviewer_id | user | id |
| 32 | carbon_neutral_project | verifier_id | user | id |
| 33 | operation_log | user_id | user | id |

#### 新增索引（约30个）

已有 UK 的 FK 列跳过（如 enterprise.user_id 已有 UK）。命名规则 `idx_{表名}_{列名}`：

| 表 | 索引列 | 类型 | 原因 |
|----|--------|------|------|
| reviewer_qualification | reviewer_id | INDEX | FK关联查询 |
| reviewer_qualification | status | INDEX | 资质状态筛选 |
| carbon_report | enterprise_id | INDEX | 按企业查询报告 |
| carbon_report | submitter_id | INDEX | 按提交人查询 |
| carbon_report | status | INDEX | 报告状态筛选 |
| carbon_report | reviewer_id | INDEX | 按审核员查询 |
| carbon_report | created_at | INDEX | 时间范围查询 |
| transaction | seller_id | INDEX | 卖方交易记录 |
| transaction | buyer_id | INDEX | 买方交易记录 |
| transaction | status | INDEX | 交易状态筛选 |
| transaction | created_at | INDEX | 时间范围查询 |
| auction_order | user_id | INDEX | 用户拍卖订单 |
| auction_order | status | INDEX | 订单状态筛选 |
| matching_result | buy_order_id | INDEX | 买单撮合查询 |
| matching_result | sell_order_id | INDEX | 卖单撮合查询 |
| matching_result | status | INDEX | 撮合状态筛选 |
| rsa_key_pair | user_id | INDEX | 用户密钥查询 |
| credit_event | enterprise_id | INDEX | 企业信誉事件 |
| credit_event | event_type | INDEX | 事件类型筛选 |
| carbon_coin_transaction | user_id | INDEX | 用户碳币记录 |
| carbon_coin_transaction | tx_type | INDEX | 交易类型筛选 |
| emission_rating | enterprise_id | INDEX | 企业评级查询 |
| emission_rating | rating_year | INDEX | 年度评级查询 |
| carbon_neutral_project | owner_id | INDEX | 项目所有者查询 |
| carbon_neutral_project | status | INDEX | 项目状态筛选 |
| carbon_neutral_project | cert_status | INDEX | 认证状态筛选 |
| user | user_type | INDEX | 按类型筛选用户 |
| user | status | INDEX | 按状态筛选用户 |
| entry_permission | user_type | INDEX | 按用户类型查权限 |
| entry_permission | api_path | INDEX | 按API路径查权限 |

### V2 变更详情

#### 修复1：补充 AUTHENTICATOR 用户类型

在 `user_type_list` INSERT 中新增第5条：
```sql
(5, '认证机构', 'AUTHENTICATOR', '碳排放认证机构', 'ROLE_AUTHENTICATOR', NOW(), NOW())
```

#### 修复2：补充认证机构 API 权限

在 `entry_permission` INSERT 中新增3条：
```sql
(12, 5, '/api/v1/authenticator/**', 'GET', 1, NOW(), NOW()),
(13, 5, '/api/v1/authenticator/**', 'POST', 1, NOW(), NOW()),
(14, 5, '/api/v1/authenticator/**', 'PUT', 1, NOW(), NOW())
```

## 不包含的内容

- 不修改任何 Java 代码（Entity、Repository、Controller）
- 不修改 `application.yml` 配置
- 不新增数据库表或字段
- 不修改现有字段类型或约束

## 验证标准

1. 所有21张表的 FK 约束正确创建，可通过 `SHOW CREATE TABLE` 验证
2. 所有索引正确创建，可通过 `SHOW INDEX FROM {table}` 验证
3. V2 种子数据包含5种用户类型（含 AUTHENTICATOR）
4. `data.sql` 已删除
5. Flyway 迁移可干净执行（无冲突）
6. `DATABASE_SCHEMA.md` 文档与实际 schema 同步

## 数据库重置步骤（开发环境）

```sql
DROP DATABASE IF EXISTS oaiss_chain;
CREATE DATABASE oaiss_chain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Flyway 将在应用启动时自动执行 V1 + V2
```
