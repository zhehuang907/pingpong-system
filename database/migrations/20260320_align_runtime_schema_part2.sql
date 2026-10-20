SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `behavior_log`
SET `update_time` = COALESCE(`update_time`, `create_time`, `created_at`, CURRENT_TIMESTAMP)
WHERE `update_time` IS NULL;

ALTER TABLE `behavior_log`
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'browse_history' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `browse_history` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `browse_history`
SET `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP)
WHERE `update_time` IS NULL;

ALTER TABLE `browse_history`
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment_like' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `comment_like` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `comment_like`
SET `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP)
WHERE `update_time` IS NULL;

ALTER TABLE `comment_like`
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recommend_record' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `recommend_record` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `recommend_record`
SET `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP)
WHERE `update_time` IS NULL;

ALTER TABLE `recommend_record`
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shopping_cart' AND COLUMN_NAME = 'deleted') = 0,
    'ALTER TABLE `shopping_cart` ADD COLUMN `deleted` TINYINT NULL DEFAULT 0 AFTER `update_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `shopping_cart`
SET `deleted` = COALESCE(`deleted`, 0)
WHERE `deleted` IS NULL;

ALTER TABLE `shopping_cart`
  MODIFY COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
