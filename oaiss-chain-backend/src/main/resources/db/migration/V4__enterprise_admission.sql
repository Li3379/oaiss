-- V4: Create enterprise_admission table (D-01, D-06)
-- Also add missing UNIQUE constraint on reviewer_qualification.certificate_no

CREATE TABLE `enterprise_admission` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`     DATETIME     NOT NULL,
    `updated_at`     DATETIME     NOT NULL,
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
    `enterprise_id`  BIGINT       NOT NULL,
    `certificate_no` VARCHAR(50)  NOT NULL,
    `issued_date`    DATE         NULL,
    `expiry_date`    DATE         NULL,
    `status`         INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_enterprise_admission_certificate_no` (`certificate_no`),
    INDEX `idx_enterprise_admission_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `reviewer_qualification`
    ADD UNIQUE KEY `uk_reviewer_qualification_certificate_no` (`certificate_no`);
