# Test Data Management Architecture

**Domain:** Carbon trading platform (Spring Boot + MySQL + Redis + MinIO)
**Researched:** 2026-05-08
**Confidence:** HIGH (based on direct source code analysis)

---

## Overview

This document defines the test data management strategy for manual testing of OAISS CHAIN. The system has three stateful backends (MySQL, Redis, MinIO) and a stateless JWT auth layer. Test data management must handle all four coherently.

```
┌─────────────────────────────────────────────────────────┐
│                   Manual Tester                          │
│          (Browser at localhost:5173)                     │
└──────────┬──────────────────────────────────────────────┘
           │ HTTP + JWT Bearer
           v
┌──────────────────┐     ┌──────────────┐     ┌──────────────┐
│  Spring Boot API │────>│   MySQL 8    │     │   MinIO      │
│  (port 8080)     │────>│  (port 3306) │     │  (port 9000) │
│                  │────>│  21 tables   │     │  bucket:     │
│  JWT stateless   │     │  Flyway V1/V2│     │  oaiss-chain │
│  sessions        │     └──────────────┘     └──────────────┘
│                  │────>┌──────────────┐
│  @Cacheable      │     │   Redis 7    │
│  @RateLimit      │     │  (port 6379) │
│  @DistributedLock│     │  DB 0        │
└──────────────────┘     └──────────────┘
```

---

## 1. Database Seeding Strategy

### Current State

The project already has Flyway migrations:
- `V1__init_schema.sql` -- 21 tables, full schema
- `V2__seed_data.sql` -- 6 test users + reference data

### Existing Seed Accounts

All seed users share the password `admin123` (BCrypt hash: `$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva`).

| ID | Username | Role | user_type | Profile Table | Profile ID |
|----|----------|------|-----------|---------------|------------|
| 1 | admin | ADMIN | 4 | (none -- admin has no profile) | -- |
| 2 | enterprise001 | ENTERPRISE | 1 | enterprise | 1 (绿色能源科技有限公司) |
| 3 | enterprise002 | ENTERPRISE | 1 | enterprise | 2 (低碳制造股份有限公司) |
| 4 | reviewer001 | REVIEWER | 2 | reviewer | 1 |
| 5 | thirdparty001 | THIRD_PARTY | 3 | third_party_org | 1 |
| 6 | authenticator001 | AUTHENTICATOR | 5 | authenticator | 1 |

### Seeding Strategy: Flyway V3__test_seed_data.sql

**Approach:** Add a new Flyway migration `V3__test_seed_data.sql` for test-specific data that supplements V2. This keeps V2 as the "production-like" baseline and V3 as the "testing enrichment" layer.

**Why not modify V2:** V2 is already applied to the database. Flyway does not allow modifying applied migrations. A new V3 migration is the correct Flyway pattern.

**What V3 should contain:**

```sql
-- V3__test_seed_data.sql
-- Test-specific data for manual testing scenarios

SET NAMES utf8mb4;

-- ============================================================
-- 1. Additional enterprise users for multi-party testing
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`, `created_at`, `updated_at`)
VALUES
(7, 'enterprise003', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'enterprise003@example.com', '13800138006', '王五', 1, 1, NOW(), NOW());

INSERT INTO `enterprise` (`id`, `user_id`, `enterprise_name`, `credit_code`, `address`, `industry`, `scale`, `contact_person`, `contact_phone`, `cert_status`, `carbon_quota`, `carbon_used`, `carbon_tradable`, `created_at`, `updated_at`)
VALUES
(3, 7, '清洁能源发展有限公司', '91110000MA003AABCD', '北京市丰台区南四环西路1号', '新能源', '小型', '王五', '13800138006', 2, 30000, 5000, 25000, NOW(), NOW());

INSERT INTO `credit_score` (`enterprise_id`, `score`, `level`, `trade_restricted`, `account_frozen`, `created_at`, `updated_at`)
VALUES (3, 100, 'EXCELLENT', 0, 0, NOW(), NOW());

INSERT INTO `carbon_coin_account` (`user_id`, `balance`, `total_recharged`, `total_spent`, `status`, `created_at`, `updated_at`)
VALUES (7, 10000, 10000, 0, 1, NOW(), NOW());

-- ============================================================
-- 2. Sample carbon reports in various states
-- ============================================================
INSERT INTO `carbon_report` (`report_no`, `enterprise_id`, `submitter_id`, `accounting_period`, `title`, `report_type`, `emission_data`, `total_emission`, `scope1_emission`, `scope2_emission`, `scope3_emission`, `calculation_method`, `status`, `created_at`, `updated_at`)
VALUES
-- PENDING report (waiting for reviewer)
('RPT-2026-001', 1, 2, '2025', '绿色能源科技2025年度碳排放报告', 1, '{"source":"coal","amount":5000}', 12000.0000, 8000.0000, 3000.0000, 1000.0000, '排放因子法', 0, NOW(), NOW()),
-- APPROVED report
('RPT-2026-002', 2, 3, '2025', '低碳制造2025年度碳排放报告', 1, '{"source":"electricity","amount":8000}', 25000.0000, 15000.0000, 8000.0000, 2000.0000, '排放因子法', 1, NOW(), NOW()),
-- REJECTED report
('RPT-2026-003', 1, 2, '2024', '绿色能源科技2024年度碳排放报告(已驳回)', 1, '{"source":"gasoline","amount":3000}', 8000.0000, 5000.0000, 2000.0000, 1000.0000, '排放因子法', 2, NOW(), NOW());

-- ============================================================
-- 3. Sample auction orders for trading tests
-- ============================================================
INSERT INTO `auction_order` (`order_no`, `user_id`, `direction`, `quantity`, `price`, `matched_quantity`, `status`, `created_at`, `updated_at`)
VALUES
-- Buy order from enterprise001
('ORD-BUY-001', 2, 1, 500.0000, 55.00, 0, 0, NOW(), NOW()),
-- Sell order from enterprise002
('ORD-SELL-001', 3, 2, 300.0000, 50.00, 0, 0, NOW(), NOW()),
-- Sell order from enterprise003
('ORD-SELL-002', 7, 2, 200.0000, 52.00, 0, 0, NOW(), NOW());

-- ============================================================
-- 4. Sample carbon neutral project
-- ============================================================
INSERT INTO `carbon_neutral_project` (`project_no`, `project_name`, `project_type`, `owner_id`, `description`, `location`, `expected_reduction`, `investment_amount`, `start_date`, `end_date`, `status`, `cert_status`, `created_at`, `updated_at`)
VALUES
('PRJ-2026-001', '光伏发电碳中和项目', 1, 2, '10MW分布式光伏发电项目', '北京市海淀区', 15000.0000, 5000000.00, '2026-01-01', '2027-12-31', 0, 0, NOW(), NOW());

-- ============================================================
-- 5. RSA key pair for enterprise001 (for signature testing)
-- ============================================================
-- Note: Real keys should be generated via the /signature/generate endpoint
-- This is a placeholder to ensure the test flow has a key to work with
```

### Seed Data Execution

```bash
# Flyway runs V3 automatically on next application startup
# Or trigger manually:
cd oaiss-chain-backend
mvn flyway:migrate

# Verify migration applied:
mysql -u root -p oaiss_chain -e "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### Verification Queries

```sql
-- Verify all 5 role types have test accounts
SELECT u.id, u.username, u.user_type, u.status,
       CASE u.user_type
         WHEN 1 THEN 'ENTERPRISE'
         WHEN 2 THEN 'REVIEWER'
         WHEN 3 THEN 'THIRD_PARTY'
         WHEN 4 THEN 'ADMIN'
         WHEN 5 THEN 'AUTHENTICATOR'
       END AS role_name
FROM `user` u WHERE u.is_deleted = 0 ORDER BY u.user_type;

-- Verify enterprise profiles match user accounts
SELECT u.username, e.enterprise_name, e.carbon_quota, e.carbon_tradable, e.cert_status
FROM `user` u JOIN `enterprise` e ON u.id = e.user_id
WHERE u.is_deleted = 0 AND e.is_deleted = 0;

-- Verify carbon reports exist in all states
SELECT report_no, title, status,
       CASE status
         WHEN 0 THEN 'PENDING'
         WHEN 1 THEN 'APPROVED'
         WHEN 2 THEN 'REJECTED'
       END AS status_name
FROM `carbon_report` WHERE is_deleted = 0;

-- Verify carbon coin accounts have balance
SELECT u.username, cca.balance, cca.status
FROM `user` u JOIN `carbon_coin_account` cca ON u.id = cca.user_id
WHERE cca.is_deleted = 0;

-- Verify credit scores
SELECT u.username, cs.score, cs.level, cs.trade_restricted
FROM `user` u
JOIN `enterprise` e ON u.id = e.user_id
JOIN `credit_score` cs ON e.id = cs.enterprise_id
WHERE cs.is_deleted = 0;
```

---

## 2. Test User Management

### Login Procedure (All Roles)

Every login requires a captcha. The flow is:

```
Step 1: GET /api/v1/auth/captcha
        --> Returns { captchaKey, captchaImage (base64 PNG) }

Step 2: POST /api/v1/auth/login
        Body: {
          "username": "<username>",
          "password": "admin123",
          "captchaKey": "<from step 1>",
          "captcha": "<text from image>"
        }
        --> Returns { accessToken, refreshToken, userId, username, userType, realName, expiresIn }
```

### Quick Login via cURL

```bash
# Step 1: Get captcha
CAPTCHA_RESPONSE=$(curl -s http://localhost:8080/api/v1/auth/captcha)
CAPTCHA_KEY=$(echo $CAPTCHA_RESPONSE | jq -r '.data.captchaKey')
# Note: captchaImage is base64 PNG -- decode to read the text
echo $CAPTCHA_RESPONSE | jq -r '.data.captchaImage' | sed 's/data:image\/png;base64,//' | base64 -d > /tmp/captcha.png

# Step 2: Login (replace CAPTCHA_CODE with text from image)
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"enterprise001","password":"admin123","captchaKey":"'$CAPTCHA_KEY'","captchaCode":"CAPTCHA_CODE"}' | jq .
```

### Test Account Quick Reference Card

Keep this printed next to the testing terminal:

```
┌─────────────────────────────────────────────────────────────┐
│                    TEST ACCOUNTS                             │
│  Password for ALL accounts: admin123                         │
├──────────────┬──────────────┬────────────────────────────────┤
│ Username     │ Role         │ Login API                      │
├──────────────┼──────────────┼────────────────────────────────┤
│ admin        │ ADMIN        │ /auth/login                    │
│ enterprise001│ ENTERPRISE   │ /auth/login (ent. 绿色能源)     │
│ enterprise002│ ENTERPRISE   │ /auth/login (ent. 低碳制造)     │
│ enterprise003│ ENTERPRISE   │ /auth/login (ent. 清洁能源)     │
│ reviewer001  │ REVIEWER     │ /auth/login                    │
│ thirdparty001│ THIRD_PARTY  │ /auth/login                    │
│ authenticator001│ AUTHENTICATOR│ /auth/login                  │
└──────────────┴──────────────┴────────────────────────────────┘
```

---

## 3. JWT Token Management During Manual Testing

### Token Lifecycle

- **Access token TTL:** 1 hour (3,600,000 ms)
- **Refresh token TTL:** 7 days (604,800,000 ms)
- **Token location:** Frontend stores in `sessionStorage` (not cookies)
- **Token format:** `Authorization: Bearer <accessToken>`

### Token Claims Structure

```json
{
  "userId": 2,
  "roles": ["ENTERPRISE"],
  "userType": 1,
  "enterpriseId": 1,
  "type": "access",
  "sub": "enterprise001",
  "iat": 1715164800,
  "exp": 1715168400
}
```

### Strategy 1: Browser Multi-Tab (Recommended)

The simplest approach for manual testing:

1. Open Chrome with separate profiles (or incognito windows) for each role
2. Login to each role in its own tab/window
3. Switch between tabs to test multi-role interactions

```
Tab 1: enterprise001 (localhost:5173) -- submit report, place auction order
Tab 2: reviewer001   (localhost:5173) -- review report, approve/reject
Tab 3: admin         (localhost:5173) -- manage users, system config
Tab 4: thirdparty001 (localhost:5173) -- monitor transactions
```

### Strategy 2: Token Rotation via Browser DevTools

For quick role switching without re-login:

```
1. Login as enterprise001, copy accessToken from:
   - DevTools > Application > sessionStorage > token
   - Or: Network tab > any API request > Authorization header

2. To switch roles, login as reviewer001 in another tab
   Copy the new accessToken

3. Use DevTools Console to inject:
   localStorage.setItem('token', '<new-access-token>')
   // Then refresh the page
```

### Strategy 3: API Testing via cURL/Postman

For scripted test flows:

```bash
# Store tokens in environment variables
export ENTERPRISE_TOKEN="<from login response>"
export REVIEWER_TOKEN="<from login response>"
export ADMIN_TOKEN="<from login response>"

# Use in requests
curl -H "Authorization: Bearer $ENTERPRISE_TOKEN" \
  http://localhost:8080/api/v1/carbon/reports

curl -H "Authorization: Bearer $REVIEWER_TOKEN" \
  http://localhost:8080/api/v1/carbon/reports/pending
```

### Token Refresh

When access token expires (after 1 hour):

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Refresh-Token: <refreshToken>"
# Returns new accessToken + refreshToken pair
```

### Token Blacklist (Logout)

On logout, the token is added to Redis blacklist cache (`tokenBlacklist`). A logged-out token will be rejected even if not expired.

```bash
# Verify a token is blacklisted (should return 401)
curl -H "Authorization: Bearer <logged-out-token>" \
  http://localhost:8080/api/v1/users/me
# Expected: 401 Unauthorized
```

---

## 4. Redis State Management

### Redis Key Inventory

The application uses Redis for four distinct purposes. Each has different key patterns:

| Purpose | Key Pattern | TTL | How to Clear |
|---------|-------------|-----|--------------|
| **Cache preload** | `user_types:all`, `system:config`, `permissions:all`, `emission_factors:default` | 6-24h | Auto-reloaded on startup |
| **Rate limiting** | `rate_limit:*` | Varies (annotation-defined) | `redis-cli DEL 'rate_limit:*'` |
| **Login attempts** | `login_fail_<username>` | Varies | `redis-cli DEL 'login_fail_enterprise001'` |
| **Token blacklist** | (Spring Cache `tokenBlacklist` keys) | Matches token TTL | `redis-cli --scan --pattern '*tokenBlacklist*' \| xargs redis-cli DEL` |
| **Distributed locks** | (Spring Cache `distributedLock` keys) | Short (seconds) | Auto-expires |

### Clear All Redis State (Full Reset)

```bash
# Nuclear option: flush entire Redis DB 0
redis-cli -n 0 FLUSHDB

# Verify empty
redis-cli -n 0 DBSIZE
# Expected: (integer) 0
```

### Selective Redis Clearing

```bash
# Clear only rate limit keys
redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli DEL

# Clear only login attempt counters
redis-cli --scan --pattern 'login_fail_*' | xargs -r redis-cli DEL

# Clear only cached data (preloaded caches)
redis-cli DEL user_types:all system:config permissions:all emission_factors:default

# Check what keys exist
redis-cli -n 0 KEYS '*'
```

### Redis State After Application Restart

When the backend restarts, `CachePreloadService` automatically re-populates:
- `user_types:all` (SET of role names, 24h TTL)
- `system:config` (HASH of config values, 12h TTL)
- `permissions:all` (SET of permission codes, 6h TTL)
- `emission_factors:default` (HASH of emission factors, 24h TTL)

**No manual action needed** for these caches after restart.

### Rate Limit Interference During Testing

The `@RateLimit` annotation uses Redis-based sliding window counters. During rapid manual testing, you may hit rate limits unexpectedly.

**Solution:** Clear rate limit keys before starting a test session:

```bash
redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli DEL
```

**Or:** Increase rate limit thresholds in the annotation values for the testing period.

---

## 5. MinIO Test File Management

### MinIO Access

```
API endpoint:     http://localhost:9000
Console endpoint: http://localhost:9001
Default bucket:   oaiss-chain
Credentials:      minioadmin / minioadmin (Docker default)
```

### MinIO Console (Browser)

Open `http://localhost:9001` in browser to:
- Browse uploaded files
- Verify file uploads from test scenarios
- Delete test files manually
- Create/delete buckets

### MinIO via mc CLI (MinIO Client)

```bash
# Install mc (MinIO Client)
# Windows: scoop install minio  OR  download from https://min.io/docs/minio/linux/reference/minio-mc.html

# Configure alias
mc alias set local http://localhost:9000 minioadmin minioadmin

# List files in bucket
mc ls local/oaiss-chain/

# Download a file for verification
mc get local/oaiss-chain/<file-path> ./downloaded-file.pdf

# Remove all test files (use with caution)
mc rm --recursive --force local/oaiss-chain/

# Verify bucket is empty
mc ls local/oaiss-chain/
```

### MinIO via API (cURL)

```bash
# List objects (requires AWS Signature V4 -- use mc CLI instead)
# Better to use MinIO Console at http://localhost:9001
```

### File Upload Test Flow

The application uses MinIO for file uploads via `FileController` at `/api/v1/files`:

```
1. Upload: POST /api/v1/files/upload (multipart/form-data)
   --> Returns file URL/path in MinIO

2. Download: GET /api/v1/files/download/{fileId}
   --> Streams file from MinIO

3. The file path is stored in carbon_report.attachments or carbon_neutral_project.attachments
```

### MinIO State Reset

```bash
# Option 1: Delete all objects in the bucket
mc rm --recursive --force local/oaiss-chain/

# Option 2: Remove and recreate the bucket
mc rb --force local/oaiss-chain/
mc mb local/oaiss-chain/

# Option 3: Nuclear -- remove MinIO data volume
docker-compose down -v  # removes all volumes including minio-data
docker-compose up -d
```

---

## 6. Database Snapshot and Restore

### Full Database Snapshot (Before Test Session)

```bash
# Create a snapshot before starting manual testing
mysqldump -u root -p --single-transaction --routines --triggers \
  oaiss_chain > /tmp/oaiss_chain_pre_test_$(date +%Y%m%d_%H%M%S).sql

# Verify snapshot file size (should be non-trivial)
ls -lh /tmp/oaiss_chain_pre_test_*.sql
```

### Full Database Restore (After Test Session)

```bash
# Drop and recreate database
mysql -u root -p -e "DROP DATABASE oaiss_chain; CREATE DATABASE oaiss_chain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Restore from snapshot
mysql -u root -p oaiss_chain < /tmp/oaiss_chain_pre_test_YYYYMMDD_HHMMSS.sql

# Verify restoration
mysql -u root -p oaiss_chain -e "SELECT COUNT(*) FROM user;"
```

### Docker Volume Snapshot

For a complete environment reset (MySQL + Redis + MinIO):

```bash
# Stop containers (preserve volumes)
docker-compose down

# Backup MySQL volume
docker run --rm -v oaiss-chain_mysql-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/mysql-data-backup.tar.gz -C /data .

# Backup all volumes
docker run --rm -v oaiss-chain_mysql-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/all-volumes-backup.tar.gz -C /data .

# Restore MySQL volume
docker run --rm -v oaiss-chain_mysql-data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/mysql-data-backup.tar.gz -C /data
```

### Quick Reset: Flyway Clean + Migrate

```bash
# WARNING: This destroys ALL data
cd oaiss-chain-backend
mvn flyway:clean flyway:migrate

# Verify: should have exactly V1 and V2 applied
mysql -u root -p oaiss_chain -e "SELECT * FROM flyway_schema_history;"
```

---

## 7. Transaction Isolation During Testing

### Spring Boot Default Behavior

- **Default isolation level:** `READ_COMMITTED` (MySQL InnoDB default)
- **Transaction management:** `@Transactional` on service methods
- **Read-write transactions:** Default for all `@Transactional` methods
- **Read-only transactions:** Not explicitly configured (all methods are read-write by default)

### Implications for Manual Testing

1. **Dirty reads are NOT possible** -- InnoDB with READ_COMMITTED prevents reading uncommitted data
2. **Non-repeatable reads ARE possible** -- Two reads in the same transaction may see different data if another transaction committed between them
3. **Phantom reads ARE possible** -- Same query may return different row counts

### Testing Multi-Step Workflows

When testing workflows that span multiple API calls (e.g., submit report -> review report -> check status):

```
Step 1: POST /carbon/reports (as enterprise001)
        --> Report created with status=0 (PENDING)
        --> Transaction committed immediately

Step 2: GET /carbon/reports/{id} (as reviewer001)
        --> Should see status=0 (PENDING)
        --> This is a NEW transaction, sees committed data

Step 3: PUT /carbon/reports/{id}/approve (as reviewer001)
        --> Report status updated to 1 (APPROVED)
        --> Transaction committed

Step 4: GET /carbon/reports/{id} (as enterprise001)
        --> Should see status=1 (APPROVED)
```

**No special isolation handling needed** for manual testing. Each API call is an independent transaction.

### Verifying Transactional Consistency

After performing a multi-step operation, verify related tables are consistent:

```sql
-- After approving a carbon report, verify emission rating was created
SELECT cr.report_no, cr.status, er.rating_level, er.rating_score
FROM carbon_report cr
LEFT JOIN emission_rating er ON cr.enterprise_id = er.enterprise_id
WHERE cr.report_no = 'RPT-2026-001';

-- After a trade, verify carbon coin accounts were updated
SELECT u.username, cca.balance, cca.total_spent,
       (SELECT COUNT(*) FROM carbon_coin_transaction cct WHERE cct.user_id = u.id) as tx_count
FROM `user` u
JOIN `carbon_coin_account` cca ON u.id = cca.user_id
WHERE u.id IN (2, 3);

-- After auction matching, verify matching_result and transaction records
SELECT ao.order_no, ao.status, ao.matched_quantity, ao.settlement_price,
       mr.match_no, mr.status as match_status
FROM auction_order ao
LEFT JOIN matching_result mr ON ao.id = mr.buy_order_id OR ao.id = mr.sell_order_id
WHERE ao.is_deleted = 0;
```

---

## 8. Database State Verification

### Verification Queries by Test Scenario

#### TEST-01: Login Verification

```sql
-- Check last login time was updated
SELECT username, last_login_time, last_login_ip
FROM `user`
WHERE username IN ('admin','enterprise001','enterprise002','reviewer001','thirdparty001','authenticator001')
ORDER BY username;
```

#### TEST-02: Carbon Report CRUD

```sql
-- After creating a report, verify it exists
SELECT report_no, title, status, enterprise_id, submitter_id
FROM carbon_report
WHERE report_no = 'RPT-2026-NEW'
  AND is_deleted = 0;

-- Verify the enterprise association is correct
SELECT cr.report_no, e.enterprise_name, u.username as submitter
FROM carbon_report cr
JOIN enterprise e ON cr.enterprise_id = e.id
JOIN `user` u ON cr.submitter_id = u.id
WHERE cr.report_no = 'RPT-2026-NEW';
```

#### TEST-03: Report Approval Flow

```sql
-- Before approval: status should be 0 (PENDING)
SELECT report_no, status, reviewer_id, review_comment, reviewed_at
FROM carbon_report WHERE report_no = 'RPT-2026-001';

-- After approval: status should be 1, reviewer_id set, reviewed_at populated
SELECT report_no, status, reviewer_id, review_comment, reviewed_at
FROM carbon_report WHERE report_no = 'RPT-2026-001';

-- Verify emission rating was generated
SELECT er.enterprise_id, er.rating_year, er.total_emission, er.rating_level
FROM emission_rating er
WHERE er.enterprise_id = 1 AND er.rating_year = '2025';
```

#### TEST-05: Carbon Coin Operations

```sql
-- Check balance before transfer
SELECT u.username, cca.balance
FROM `user` u JOIN `carbon_coin_account` cca ON u.id = cca.user_id
WHERE u.id IN (2, 3);

-- After transfer: verify balance changed and transaction recorded
SELECT u.username, cca.balance, cca.total_spent
FROM `user` u JOIN `carbon_coin_account` cca ON u.id = cca.user_id
WHERE u.id IN (2, 3);

-- Verify transaction ledger
SELECT cct.tx_no, cct.tx_type, cct.amount, cct.balance_before, cct.balance_after, u.username
FROM carbon_coin_transaction cct
JOIN `user` u ON cct.user_id = u.id
ORDER BY cct.created_at DESC LIMIT 10;
```

#### TEST-06: Double Auction Trading

```sql
-- Check open orders
SELECT order_no, user_id, direction,
       CASE direction WHEN 1 THEN 'BUY' WHEN 2 THEN 'SELL' END as dir,
       quantity, price, matched_quantity, status
FROM auction_order
WHERE status = 0 AND is_deleted = 0;

-- After matching: verify matching_result created
SELECT mr.match_no, mr.matched_quantity, mr.settlement_price, mr.total_amount, mr.status,
       buy.order_no as buy_order, sell.order_no as sell_order
FROM matching_result mr
JOIN auction_order buy ON mr.buy_order_id = buy.id
JOIN auction_order sell ON mr.sell_order_id = sell.id
ORDER BY mr.created_at DESC LIMIT 5;

-- Verify transaction record created
SELECT trade_no, trade_type, seller_id, buyer_id, quantity, unit_price, total_amount, status
FROM `transaction`
ORDER BY created_at DESC LIMIT 5;
```

#### TEST-07: P2P Trading

```sql
-- Verify trade created
SELECT trade_no, trade_type, seller_id, buyer_id, quantity, unit_price, total_amount, status
FROM `transaction`
WHERE trade_type = 2  -- P2P type
ORDER BY created_at DESC LIMIT 5;

-- Verify carbon coin accounts updated
SELECT u.username, cca.balance, cca.total_spent
FROM `user` u JOIN `carbon_coin_account` cca ON u.id = cca.user_id;
```

#### TEST-10: Admin User Management

```sql
-- Verify admin can see all users
SELECT id, username, user_type, status, created_at
FROM `user` WHERE is_deleted = 0 ORDER BY id;

-- After admin creates a new user, verify it exists
SELECT id, username, user_type, status
FROM `user` WHERE username = 'new_test_user';
```

#### TEST-12: Digital Signature

```sql
-- Verify RSA key pair was generated
SELECT rkp.user_id, rkp.key_status, rkp.key_version, rkp.key_usage,
       LENGTH(rkp.public_key) as pub_key_len,
       LENGTH(rkp.private_key) as priv_key_len,
       rkp.expires_at
FROM rsa_key_pair rkp
WHERE rkp.user_id = 2 AND rkp.is_deleted = 0;

-- Verify signature on report
SELECT report_no, signature_data IS NOT NULL as has_signature
FROM carbon_report
WHERE report_no = 'RPT-2026-001';
```

#### TEST-13: File Upload/Download

```sql
-- Verify file references in reports
SELECT report_no, attachments
FROM carbon_report
WHERE attachments IS NOT NULL AND is_deleted = 0;

-- Verify file references in projects
SELECT project_no, attachments
FROM carbon_neutral_project
WHERE attachments IS NOT NULL AND is_deleted = 0;
```

---

## 9. Complete Test Environment Setup (Step-by-Step)

### Phase 1: Infrastructure Startup

```bash
# 1. Start Docker services
cd /path/to/OAISS\ CHAIN
docker-compose up -d

# 2. Verify all services are healthy
docker-compose ps
# Expected: mysql (healthy), redis (healthy), minio (running)

# 3. Verify MySQL is ready
mysql -u root -p123456 -e "SELECT 1" oaiss_chain

# 4. Verify Redis is ready
redis-cli ping
# Expected: PONG

# 5. Verify MinIO is ready
curl -s http://localhost:9000/minio/health/live
# Expected: 200 OK
```

### Phase 2: Database Preparation

```bash
# 1. Create test seed data migration (V3)
# File: oaiss-chain-backend/src/main/resources/db/migration/V3__test_seed_data.sql
# Content: as defined in Section 1 above

# 2. Start backend (triggers Flyway migration)
cd oaiss-chain-backend
mvn spring-boot:run

# 3. Verify V3 migration applied
mysql -u root -p123456 oaiss_chain -e \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"

# 4. Verify seed data
mysql -u root -p123456 oaiss_chain -e \
  "SELECT id, username, user_type FROM user WHERE is_deleted = 0;"
```

### Phase 3: Redis Verification

```bash
# 1. Wait for backend to complete cache preload (check logs)
# Look for: "=== Cache Preload Completed ==="

# 2. Verify preloaded caches exist
redis-cli TYPE user_types:all      # should be: set
redis-cli TYPE system:config       # should be: hash
redis-cli TYPE permissions:all     # should be: set
redis-cli TYPE emission_factors:default  # should be: hash

# 3. Clear any stale rate limit data
redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli DEL
```

### Phase 4: Frontend Startup

```bash
# 1. Start frontend dev server
cd oaiss-chain-frontend
npm run dev

# 2. Open browser tabs for each role
# Tab 1: http://localhost:5173 (login as enterprise001)
# Tab 2: http://localhost:5173 (login as reviewer001)
# Tab 3: http://localhost:5173 (login as admin)
# Tab 4: http://localhost:5173 (login as thirdparty001)
# Tab 5: http://localhost:5173 (login as authenticator001)
```

### Phase 5: Baseline Verification

```bash
# Run all verification queries from Section 8 to confirm data is correct
# Run the login flow for each role to confirm auth works
# Verify MinIO console at http://localhost:9001 is accessible
```

---

## 10. Full Environment Reset Procedure

Use this between major test sessions or when data becomes inconsistent:

```bash
# 1. Stop all services
docker-compose down -v   # -v removes volumes (destroys all data)

# 2. Clean Flyway state (not needed if volumes are removed)
# Only needed if you want to reset without losing Docker volumes:
# mysql -u root -p123456 -e "DROP DATABASE oaiss_chain; CREATE DATABASE oaiss_chain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Restart everything
docker-compose up -d

# 4. Wait for MySQL healthcheck
# mysql container status should be "healthy"

# 5. Start backend (triggers Flyway V1 + V2 + V3)
cd oaiss-chain-backend
mvn spring-boot:run

# 6. Verify
mysql -u root -p123456 oaiss_chain -e "SELECT COUNT(*) as user_count FROM user;"
# Expected: 7 (6 from V2 + 1 from V3)

# 7. Start frontend
cd oaiss-chain-frontend
npm run dev
```

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Database seeding | HIGH | Directly analyzed V1/V2 SQL and entity structure |
| JWT token management | HIGH | Read JwtTokenProvider and AuthService source code |
| Redis key patterns | HIGH | Read RateLimitAspect, CachePreloadService, AuthService |
| MinIO integration | MEDIUM | Read MinioService references but not full source |
| Transaction isolation | HIGH | Standard Spring Boot + InnoDB defaults, confirmed in config |
| Verification queries | HIGH | Derived from actual schema (V1) and entity relationships |

## Sources

- `oaiss-chain-backend/src/main/resources/db/migration/V1__init_schema.sql` -- 21 table definitions
- `oaiss-chain-backend/src/main/resources/db/migration/V2__seed_data.sql` -- 6 seed users + reference data
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/AuthService.java` -- Login, register, refresh, logout flow
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/security/JwtTokenProvider.java` -- JWT generation and validation
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RedisConfig.java` -- Redis serialization config
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CachePreloadService.java` -- Cache key patterns and TTLs
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/aop/RateLimitAspect.java` -- Rate limit key patterns
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/SecurityConfig.java` -- Security filter chain, permit-all endpoints
- `oaiss-chain-backend/src/main/resources/application.yml` -- All configuration values
- `docker-compose.yml` -- Infrastructure service definitions
