-- =============================================
-- 双碳链动系统测试补充数据
-- OAISS Chain Test Seed Data
-- AUTHENTICATOR 枚举 + enterprise003 测试账户
-- =============================================

SET NAMES utf8mb4;

-- 补充 AUTHENTICATOR 用户类型枚举 (id=5)
INSERT INTO `user_type_list` (`id`, `type_name`, `type_code`, `description`, `default_role`, `created_at`, `updated_at`)
VALUES (5, '认证机构', 'AUTHENTICATOR', '碳排放认证机构', 'ROLE_AUTHENTICATOR', NOW(), NOW());

-- enterprise003 测试账户 (id=7, user_type=1 即 ENTERPRISE)
INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`, `created_at`, `updated_at`)
VALUES (7, 'enterprise003', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'enterprise003@example.com', '13800138006', '孙六', 1, 1, NOW(), NOW());

-- enterprise003 企业信息 (id=3, user_id=7)
INSERT INTO `enterprise` (`id`, `user_id`, `enterprise_name`, `credit_code`, `address`, `industry`, `scale`, `contact_person`, `contact_phone`, `cert_status`, `carbon_quota`, `carbon_used`, `carbon_tradable`, `created_at`, `updated_at`)
VALUES (3, 7, '清洁能源发展有限公司', '91110000MA003AABCD', '北京市丰台区科技园南路8号', '清洁能源', '中型', '孙六', '13800138006', 2, 50000, 0, 50000, NOW(), NOW());

-- enterprise003 信誉评分 (id=3, enterprise_id=3)
INSERT INTO `credit_score` (`id`, `enterprise_id`, `score`, `level`, `trade_restricted`, `account_frozen`, `created_at`, `updated_at`)
VALUES (3, 3, 100, 'EXCELLENT', 0, 0, NOW(), NOW());

-- enterprise003 碳币账户 (user_id=7)
INSERT INTO `carbon_coin_account` (`user_id`, `balance`, `total_recharged`, `total_spent`, `status`, `created_at`, `updated_at`)
VALUES (7, 10000, 10000, 0, 1, NOW(), NOW());
