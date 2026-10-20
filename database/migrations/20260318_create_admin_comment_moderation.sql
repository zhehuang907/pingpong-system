SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'review_status') = 0,
    'ALTER TABLE `comment` ADD COLUMN `review_status` TINYINT NULL AFTER `status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'reviewed_by') = 0,
    'ALTER TABLE `comment` ADD COLUMN `reviewed_by` BIGINT NULL AFTER `review_status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'reviewed_at') = 0,
    'ALTER TABLE `comment` ADD COLUMN `reviewed_at` DATETIME NULL AFTER `reviewed_by`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'review_remark') = 0,
    'ALTER TABLE `comment` ADD COLUMN `review_remark` VARCHAR(255) NULL AFTER `reviewed_at`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `comment`
SET `review_status` = COALESCE(`review_status`, 1);

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_review_status_time') = 0,
    'ALTER TABLE `comment` ADD INDEX `idx_comment_review_status_time` (`review_status`, `status`, `create_time`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_reviewed_by') = 0,
    'ALTER TABLE `comment` ADD INDEX `idx_comment_reviewed_by` (`reviewed_by`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `comment`
  MODIFY COLUMN `review_status` TINYINT NOT NULL DEFAULT 1;
