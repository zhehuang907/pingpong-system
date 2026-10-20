SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'parent_id') = 0,
    'ALTER TABLE `comment` ADD COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0 AFTER `equipment_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'images') = 0,
    'ALTER TABLE `comment` ADD COLUMN `images` TEXT NULL AFTER `content`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'rating') = 0,
    'ALTER TABLE `comment` ADD COLUMN `rating` INT NULL AFTER `images`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'like_count') = 0,
    'ALTER TABLE `comment` ADD COLUMN `like_count` INT NULL AFTER `rating`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'reply_count') = 0,
    'ALTER TABLE `comment` ADD COLUMN `reply_count` INT NULL AFTER `like_count`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE `comment` ADD COLUMN `status` TINYINT NULL AFTER `reply_count`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE `comment` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `comment` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `comment`
SET `parent_id` = COALESCE(`parent_id`, 0),
    `rating` = COALESCE(`rating`, `score`, 5),
    `like_count` = COALESCE(`like_count`, 0),
    `reply_count` = COALESCE(`reply_count`, 0),
    `status` = COALESCE(`status`, 1),
    `create_time` = COALESCE(`create_time`, `created_at`, CURRENT_TIMESTAMP),
    `update_time` = COALESCE(`update_time`, `create_time`, `created_at`, CURRENT_TIMESTAMP);

ALTER TABLE `comment`
  MODIFY COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0,
  MODIFY COLUMN `rating` INT NOT NULL DEFAULT 5,
  MODIFY COLUMN `like_count` INT NOT NULL DEFAULT 0,
  MODIFY COLUMN `reply_count` INT NOT NULL DEFAULT 0,
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1,
  MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  MODIFY COLUMN `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP;

SET @comment_user_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'sys_user'
    LIMIT 1
);
SET @sql := IF(
    @comment_user_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `comment` DROP FOREIGN KEY `', @comment_user_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'user') = 0,
    'ALTER TABLE `comment` ADD CONSTRAINT `fk_comment_user_runtime` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'parent_id') = 0,
    'ALTER TABLE `category` ADD COLUMN `parent_id` BIGINT NULL AFTER `name`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'level') = 0,
    'ALTER TABLE `category` ADD COLUMN `level` TINYINT NULL AFTER `parent_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'icon') = 0,
    'ALTER TABLE `category` ADD COLUMN `icon` VARCHAR(255) NULL AFTER `level`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'sort') = 0,
    'ALTER TABLE `category` ADD COLUMN `sort` INT NULL AFTER `icon`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE `category` ADD COLUMN `status` TINYINT NULL AFTER `sort`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE `category` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `category` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'deleted') = 0,
    'ALTER TABLE `category` ADD COLUMN `deleted` TINYINT NULL DEFAULT 0 AFTER `update_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `category`
SET `parent_id` = COALESCE(`parent_id`, 0),
    `level` = COALESCE(`level`, 1),
    `sort` = COALESCE(`sort`, 0),
    `status` = COALESCE(`status`, 1),
    `create_time` = COALESCE(`create_time`, CURRENT_TIMESTAMP),
    `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP),
    `deleted` = COALESCE(`deleted`, 0);

ALTER TABLE `category`
  MODIFY COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0,
  MODIFY COLUMN `level` TINYINT NOT NULL DEFAULT 1,
  MODIFY COLUMN `sort` INT NOT NULL DEFAULT 0,
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1,
  MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  MODIFY COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'category') = 0,
    'ALTER TABLE `tag` ADD COLUMN `category` VARCHAR(50) NULL AFTER `name`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'color') = 0,
    'ALTER TABLE `tag` ADD COLUMN `color` VARCHAR(20) NULL AFTER `category`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'sort') = 0,
    'ALTER TABLE `tag` ADD COLUMN `sort` INT NULL AFTER `color`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'status') = 0,
    'ALTER TABLE `tag` ADD COLUMN `status` TINYINT NULL AFTER `sort`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE `tag` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE `tag` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag' AND COLUMN_NAME = 'deleted') = 0,
    'ALTER TABLE `tag` ADD COLUMN `deleted` TINYINT NULL DEFAULT 0 AFTER `update_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `tag`
SET `color` = COALESCE(`color`, '#409EFF'),
    `sort` = COALESCE(`sort`, 0),
    `status` = COALESCE(`status`, 1),
    `create_time` = COALESCE(`create_time`, CURRENT_TIMESTAMP),
    `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP),
    `deleted` = COALESCE(`deleted`, 0);

ALTER TABLE `tag`
  MODIFY COLUMN `color` VARCHAR(20) NOT NULL DEFAULT '#409EFF',
  MODIFY COLUMN `sort` INT NOT NULL DEFAULT 0,
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1,
  MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  MODIFY COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'username') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `username` VARCHAR(50) NULL AFTER `user_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'ip') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `ip` VARCHAR(50) NULL AFTER `username`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'url') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `url` VARCHAR(255) NULL AFTER `ip`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'method') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `method` VARCHAR(20) NULL AFTER `url`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'module') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `module` VARCHAR(50) NULL AFTER `method`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'operation') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `operation` VARCHAR(100) NULL AFTER `module`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'params') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `params` TEXT NULL AFTER `operation`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'result') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `result` TEXT NULL AFTER `params`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'time_taken') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `time_taken` BIGINT NULL AFTER `result`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'user_agent') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `user_agent` VARCHAR(255) NULL AFTER `time_taken`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER `user_agent`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `behavior_log`
SET `create_time` = COALESCE(`create_time`, `created_at`, CURRENT_TIMESTAMP);

ALTER TABLE `behavior_log`
  MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP;

SET @behavior_user_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'behavior_log'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'sys_user'
    LIMIT 1
);
SET @sql := IF(
    @behavior_user_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `behavior_log` DROP FOREIGN KEY `', @behavior_user_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'user') = 0,
    'ALTER TABLE `behavior_log` ADD CONSTRAINT `fk_behavior_user_runtime` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `equipment`
SET `created_at` = COALESCE(`created_at`, `create_time`, CURRENT_TIMESTAMP),
    `updated_at` = COALESCE(`updated_at`, `update_time`, `created_at`, CURRENT_TIMESTAMP);

ALTER TABLE `equipment`
  MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment_combination' AND COLUMN_NAME = 'created_at') > 0,
    'ALTER TABLE `equipment_combination` MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @combo_user_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'equipment_combination'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'sys_user'
    LIMIT 1
);
SET @sql := IF(
    @combo_user_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `equipment_combination` DROP FOREIGN KEY `', @combo_user_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment_combination' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'user') = 0,
    'ALTER TABLE `equipment_combination` ADD CONSTRAINT `fk_combo_user_runtime` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_item' AND COLUMN_NAME = 'created_at') > 0,
    'ALTER TABLE `market_item` MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @market_user_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_item'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'sys_user'
    LIMIT 1
);
SET @sql := IF(
    @market_user_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `market_item` DROP FOREIGN KEY `', @market_user_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_item' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'user') = 0,
    'ALTER TABLE `market_item` ADD CONSTRAINT `fk_market_user_runtime` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
