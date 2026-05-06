-- =============================================
-- 双碳链动系统初始数据脚本
-- OAISS Chain Initial Data Script
-- MySQL 8.0+
-- Author: OAISS Team
-- Date: 2026-05-02
-- =============================================

SET NAMES utf8mb4;

-- =============================================
-- 1. 初始管理员账户
-- 密码: admin123 (BCrypt加密)
-- =============================================

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`)
VALUES
(1, 'admin', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'admin@oaiss.com', '13800138000', '系统管理员', 4, 1);

-- =============================================
-- 2. 用户类型列表
-- =============================================

INSERT INTO `user_type_list` (`id`, `type_name`, `type_code`, `description`, `default_role`)
VALUES
(1, '企业用户', 'ENTERPRISE', '碳排放企业用户', 'ROLE_ENTERPRISE'),
(2, '审核员', 'REVIEWER', '碳报告审核员', 'ROLE_REVIEWER'),
(3, '第三方机构', 'THIRD_PARTY', '第三方监管机构', 'ROLE_THIRD_PARTY'),
(4, '管理员', 'ADMIN', '系统管理员', 'ROLE_ADMIN');

-- =============================================
-- 3. 权限初始化（API级别权限）
-- =============================================

INSERT INTO `entry_permission` (`id`, `user_type`, `api_path`, `http_method`, `is_allowed`)
VALUES
(1, 4, '/api/v1/admin/**', 'GET', 1),
(2, 4, '/api/v1/admin/**', 'POST', 1),
(3, 4, '/api/v1/admin/**', 'PUT', 1),
(4, 4, '/api/v1/admin/**', 'DELETE', 1),
(5, 1, '/api/v1/enterprise/**', 'GET', 1),
(6, 1, '/api/v1/enterprise/**', 'POST', 1),
(7, 1, '/api/v1/enterprise/**', 'PUT', 1),
(8, 2, '/api/v1/reviewer/**', 'GET', 1),
(9, 2, '/api/v1/reviewer/**', 'POST', 1),
(10, 3, '/api/v1/third-party/**', 'GET', 1),
(11, 3, '/api/v1/third-party/**', 'POST', 1);

-- =============================================
-- 4. 账户权限定义
-- =============================================

INSERT INTO `account_permission_list` (`id`, `permission_name`, `permission_code`, `description`, `module`, `sort_order`)
VALUES
(1, '碳报告管理', 'CARBON_REPORT', '碳报告的创建、编辑、提交', 'carbon', 1),
(2, '碳报告审核', 'CARBON_REVIEW', '审核企业提交的碳报告', 'carbon', 2),
(3, '碳交易管理', 'CARBON_TRADE', '碳配额交易操作', 'trade', 3),
(4, '拍卖管理', 'AUCTION_MANAGE', '拍卖订单管理', 'trade', 4),
(5, '信誉评分管理', 'CREDIT_MANAGE', '用户信誉评分管理', 'credit', 5),
(6, '第三方监管', 'THIRD_PARTY_MONITOR', '第三方监管权限', 'monitor', 6),
(7, '系统管理', 'SYSTEM_ADMIN', '系统管理权限', 'admin', 7),
(8, '数据查询', 'DATA_QUERY', '数据查询与统计', 'data', 8),
(9, '区块链存证', 'BLOCKCHAIN_NOTARY', '区块链存证操作', 'blockchain', 9),
(10, '碳币管理', 'CARBON_COIN', '碳币账户管理', 'carbon_coin', 10);

-- =============================================
-- 5. 示例企业数据
-- =============================================

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`)
VALUES
(2, 'enterprise001', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'enterprise001@example.com', '13800138001', '张三', 1, 1),
(3, 'enterprise002', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'enterprise002@example.com', '13800138002', '李四', 1, 1);

INSERT INTO `enterprise` (`id`, `user_id`, `enterprise_name`, `credit_code`, `address`, `industry`, `scale`, `contact_person`, `contact_phone`, `cert_status`, `carbon_quota`, `carbon_used`, `carbon_tradable`)
VALUES
(1, 2, '绿色能源科技有限公司', '91110000MA001AABCD', '北京市海淀区中关村大街1号', '新能源', '中型', '张三', '13800138001', 2, 50000.0000, 12000.0000, 38000.0000),
(2, 3, '低碳制造股份有限公司', '91110000MA002AABCD', '北京市朝阳区建国路100号', '制造业', '大型', '李四', '13800138002', 2, 80000.0000, 25000.0000, 55000.0000);

-- =============================================
-- 6. 示例审核员数据
-- =============================================

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`)
VALUES
(4, 'reviewer001', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'reviewer001@example.com', '13800138003', '王审核', 2, 1);

INSERT INTO `reviewer` (`id`, `user_id`, `qualification_no`, `level`, `organization`, `reviewable_industries`, `completed_reviews`, `status`)
VALUES
(1, 4, 'REV20260001', 3, '碳核算审核中心', '["新能源","制造业","化工"]', 156, 1);

-- =============================================
-- 7. 示例审核员资质
-- =============================================

INSERT INTO `reviewer_qualification` (`id`, `reviewer_id`, `qualification_type`, `certificate_no`, `issuing_authority`, `issued_date`, `expiry_date`, `status`)
VALUES
(1, 1, '碳核算审核员', 'CERT-2026-001', '中国碳排放权交易协会', '2024-01-15', '2027-01-15', 1);

-- =============================================
-- 8. 示例第三方机构数据
-- =============================================

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`)
VALUES
(5, 'thirdparty001', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'thirdparty001@example.com', '13800138004', '赵监管', 3, 1);

INSERT INTO `third_party_org` (`id`, `user_id`, `org_name`, `org_code`, `org_type`, `supervision_scope`, `contact_person`, `contact_phone`, `address`, `access_level`, `status`)
VALUES
(1, 5, '中国碳排放权注册登记机构', 'ORG001', 1, '["全部行业"]', '赵监管', '13800138004', '北京市西城区金融大街15号', 3, 1);

-- =============================================
-- 9. 示例认证机构数据
-- =============================================

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `real_name`, `user_type`, `status`)
VALUES
(6, 'authenticator001', '$2a$10$hBOIu5glgwd4b8ohK/K/mOCFTZmHnDBskNnzr9ngA5J6jFCH57Vva', 'authenticator001@example.com', '13800138005', '钱认证', 5, 1);

INSERT INTO `authenticator` (`id`, `user_id`, `org_name`, `org_code`, `address`, `contact_person`, `contact_phone`, `cert_scope`, `status`)
VALUES
(1, 6, '中国质量认证中心', 'CQC001', '北京市朝阳区朝外大街甲10号', '钱认证', '13800138005', '["碳排放核查","碳中和认证","碳信用验证"]', 1);

-- =============================================
-- 10. 初始信誉评分
-- =============================================

INSERT INTO `credit_score` (`id`, `enterprise_id`, `score`, `level`, `trade_restricted`, `account_frozen`)
VALUES
(1, 1, 100, 'EXCELLENT', 0, 0),
(2, 2, 100, 'EXCELLENT', 0, 0);

-- =============================================
-- 11. 初始碳币账户
-- =============================================

INSERT INTO `carbon_coin_account` (`user_id`, `balance`, `total_recharged`, `total_spent`, `status`)
VALUES
(2, 10000.00, 10000.00, 0.00, 1),
(3, 10000.00, 10000.00, 0.00, 1);

-- =============================================
-- 系统配置说明
-- =============================================

-- 提示：
-- 1. 生产环境请修改默认密码
-- 2. 建议启用双因素认证（MFA）
-- 3. 定期审计用户权限
-- 4. 配置合适的密码策略
