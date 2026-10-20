CREATE TABLE IF NOT EXISTS `login_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `username` VARCHAR(20) NOT NULL,
  `success` TINYINT NOT NULL DEFAULT 0,
  `ip_address` VARCHAR(64) NULL,
  `user_agent` VARCHAR(255) NULL,
  `device_fingerprint` VARCHAR(128) NULL,
  `device_name` VARCHAR(64) NULL,
  `risk_level` VARCHAR(16) NOT NULL DEFAULT 'LOW',
  `warning_message` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_login_audit_username_time` (`username`, `create_time`),
  KEY `idx_login_audit_user_success_time` (`user_id`, `success`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
