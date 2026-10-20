SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'content_type') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `content_type` VARCHAR(64) NULL COMMENT '浏览内容类型' AFTER `equipment_id`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'source_page') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `source_page` VARCHAR(128) NULL COMMENT '来源页面' AFTER `content_type`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'session_id') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `session_id` VARCHAR(64) NULL COMMENT '单次浏览会话 ID' AFTER `source_page`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'enter_time') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `enter_time` DATETIME NULL COMMENT '进入时间' AFTER `session_id`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'leave_time') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `leave_time` DATETIME NULL COMMENT '离开时间' AFTER `enter_time`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'duration_seconds') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `duration_seconds` INT NOT NULL DEFAULT 0 COMMENT '停留时长（秒）' AFTER `leave_time`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'interaction_count') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `interaction_count` INT NOT NULL DEFAULT 0 COMMENT '交互次数' AFTER `duration_seconds`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'interaction_actions') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `interaction_actions` TEXT NULL COMMENT '交互动作 JSON' AFTER `interaction_count`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'content_snapshot') = 0,
    "ALTER TABLE `browse_history` ADD COLUMN `content_snapshot` VARCHAR(255) NULL COMMENT '内容快照' AFTER `interaction_actions`",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND INDEX_NAME = 'idx_user_equipment_session') = 0,
    "ALTER TABLE `browse_history` ADD INDEX `idx_user_equipment_session` (`user_id`, `equipment_id`, `session_id`)",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND INDEX_NAME = 'idx_enter_time') = 0,
    "ALTER TABLE `browse_history` ADD INDEX `idx_enter_time` (`enter_time`)",
    "SELECT 1"
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `user_preference_profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `level` VARCHAR(32) NULL COMMENT '技术水平',
    `play_style` VARCHAR(64) NULL COMMENT '打法偏好',
    `grip_style` VARCHAR(64) NULL COMMENT '握拍方式',
    `budget_min` DECIMAL(10, 2) NULL COMMENT '预算下限',
    `budget_max` DECIMAL(10, 2) NULL COMMENT '预算上限',
    `weekly_frequency` VARCHAR(32) NULL COMMENT '使用频率',
    `primary_goal` VARCHAR(64) NULL COMMENT '使用目标',
    `priority_focus` VARCHAR(64) NULL COMMENT '优先考虑因素',
    `preferred_categories` TEXT NULL COMMENT '偏好分类 JSON',
    `preferred_brands` TEXT NULL COMMENT '偏好品牌 JSON',
    `answer_snapshot` TEXT NULL COMMENT '问答快照 JSON',
    `preference_tags` TEXT NULL COMMENT '偏好标签 JSON',
    `updated_source` VARCHAR(32) NULL COMMENT '更新来源',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY `uk_user_preference_profile` (`user_id`),
    INDEX `idx_level` (`level`),
    INDEX `idx_play_style` (`play_style`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好档案表';
