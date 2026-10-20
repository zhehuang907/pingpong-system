-- =====================================================
-- 创建缺失的数据库表
-- 执行时间: 2026-04-21
-- 说明: 创建 equipment_combination, login_audit, player_equipment_relation, user_preference_profile 表
-- =====================================================

USE pingpong_recommendation;

-- 1. 装备组合表
CREATE TABLE IF NOT EXISTS `equipment_combination` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `name` VARCHAR(100) NOT NULL,
  `blade_id` BIGINT NULL,
  `forehand_rubber_id` BIGINT NULL,
  `backhand_rubber_id` BIGINT NULL,
  `compatibility_score` DECIMAL(5,2) NULL,
  `description` TEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 登录审计表
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
  PRIMARY KEY (`id`),
  KEY `idx_login_audit_username_time` (`username`, `create_time`),
  KEY `idx_login_audit_user_success_time` (`user_id`, `success`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 球员装备关系表
CREATE TABLE IF NOT EXISTS `player_equipment_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `player_id` BIGINT NOT NULL,
  `equipment_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NULL,
  `is_current` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_player_equipment` (`player_id`, `equipment_id`, `type`),
  KEY `idx_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 用户偏好档案表
CREATE TABLE IF NOT EXISTS `user_preference_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `level` VARCHAR(32) NULL,
  `play_style` VARCHAR(64) NULL,
  `grip_style` VARCHAR(64) NULL,
  `budget_min` DECIMAL(10,2) NULL,
  `budget_max` DECIMAL(10,2) NULL,
  `weekly_frequency` VARCHAR(32) NULL,
  `primary_goal` VARCHAR(64) NULL,
  `priority_focus` VARCHAR(64) NULL,
  `preferred_categories` TEXT NULL,
  `preferred_brands` TEXT NULL,
  `answer_snapshot` TEXT NULL,
  `preference_tags` TEXT NULL,
  `updated_source` VARCHAR(32) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_preference_profile` (`user_id`),
  KEY `idx_level` (`level`),
  KEY `idx_play_style` (`play_style`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 验证表是否创建成功
SELECT '=== 表创建完成 ===' AS status;
SHOW TABLES LIKE 'equipment_combination';
SHOW TABLES LIKE 'login_audit';
SHOW TABLES LIKE 'player_equipment_relation';
SHOW TABLES LIKE 'user_preference_profile';
