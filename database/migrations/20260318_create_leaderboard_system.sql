CREATE TABLE IF NOT EXISTS `leaderboard_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `board_type` VARCHAR(50) NOT NULL,
  `board_name` VARCHAR(100) NOT NULL,
  `equipment_id` BIGINT NOT NULL,
  `rank_position` INT NOT NULL,
  `total_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `hot_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `value_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `time_decay` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `avg_rating` DECIMAL(4,2) NOT NULL DEFAULT 0,
  `satisfaction_rate` DECIMAL(6,2) NOT NULL DEFAULT 0,
  `latest_interaction_time` DATETIME NULL,
  `board_reason` VARCHAR(255) NULL,
  `refresh_mode` VARCHAR(20) NOT NULL DEFAULT 'FULL',
  `refreshed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_snapshot_board_equipment` (`board_type`, `equipment_id`),
  KEY `idx_leaderboard_snapshot_rank` (`board_type`, `rank_position`),
  KEY `idx_leaderboard_snapshot_score` (`board_type`, `total_score`),
  KEY `idx_leaderboard_snapshot_time` (`board_type`, `latest_interaction_time`),
  CONSTRAINT `fk_leaderboard_snapshot_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `leaderboard_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `board_type` VARCHAR(50) NOT NULL,
  `active` TINYINT NOT NULL DEFAULT 1,
  `last_viewed_at` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_subscription` (`user_id`, `board_type`),
  KEY `idx_leaderboard_subscription_user` (`user_id`, `active`),
  CONSTRAINT `fk_leaderboard_subscription_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `leaderboard_share_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `board_type` VARCHAR(50) NOT NULL,
  `equipment_id` BIGINT NOT NULL DEFAULT 0,
  `channel` VARCHAR(30) NOT NULL,
  `share_count` INT NOT NULL DEFAULT 0,
  `last_share_user_id` BIGINT NULL,
  `last_shared_at` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_share_stat` (`board_type`, `equipment_id`, `channel`),
  KEY `idx_leaderboard_share_stat_board` (`board_type`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `schedule_job_lock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_key` VARCHAR(100) NOT NULL,
  `owner` VARCHAR(64) NOT NULL,
  `locked_until` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_job_lock` (`job_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_favorite_equipment_time') = 0,
  'ALTER TABLE `favorite` ADD INDEX `idx_favorite_equipment_time` (`equipment_id`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_favorite_user_time') = 0,
  'ALTER TABLE `favorite` ADD INDEX `idx_favorite_user_time` (`user_id`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_equipment_status_time') = 0,
  'ALTER TABLE `comment` ADD INDEX `idx_comment_equipment_status_time` (`equipment_id`, `status`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_equipment_parent_status') = 0,
  'ALTER TABLE `comment` ADD INDEX `idx_comment_equipment_parent_status` (`equipment_id`, `parent_id`, `status`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_user_time') = 0,
  'ALTER TABLE `comment` ADD INDEX `idx_comment_user_time` (`user_id`, `create_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment' AND INDEX_NAME = 'idx_equipment_status_rating') = 0,
  'ALTER TABLE `equipment` ADD INDEX `idx_equipment_status_rating` (`status`, `rating`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
