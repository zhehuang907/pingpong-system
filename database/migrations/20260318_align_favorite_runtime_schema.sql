SET @favorite_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'favorite'
);

SET @add_create_time_sql := IF(
    @favorite_table_exists = 1 AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'favorite'
          AND COLUMN_NAME = 'create_time'
    ) = 0,
    'ALTER TABLE `favorite` ADD COLUMN `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `equipment_id`',
    'SELECT 1'
);
PREPARE stmt FROM @add_create_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_update_time_sql := IF(
    @favorite_table_exists = 1 AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'favorite'
          AND COLUMN_NAME = 'update_time'
    ) = 0,
    'ALTER TABLE `favorite` ADD COLUMN `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @add_update_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_created_at := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'favorite'
      AND COLUMN_NAME = 'created_at'
);

SET @backfill_create_time_sql := IF(
    @favorite_table_exists = 1,
    IF(
        @has_created_at = 1,
        'UPDATE `favorite` SET `create_time` = COALESCE(`create_time`, `created_at`) WHERE `create_time` IS NULL',
        'UPDATE `favorite` SET `create_time` = COALESCE(`create_time`, CURRENT_TIMESTAMP) WHERE `create_time` IS NULL'
    ),
    'SELECT 1'
);
PREPARE stmt FROM @backfill_create_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @backfill_update_time_sql := IF(
    @favorite_table_exists = 1,
    IF(
        @has_created_at = 1,
        'UPDATE `favorite` SET `update_time` = COALESCE(`update_time`, `create_time`, `created_at`, CURRENT_TIMESTAMP) WHERE `update_time` IS NULL',
        'UPDATE `favorite` SET `update_time` = COALESCE(`update_time`, `create_time`, CURRENT_TIMESTAMP) WHERE `update_time` IS NULL'
    ),
    'SELECT 1'
);
PREPARE stmt FROM @backfill_update_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @normalize_created_at_sql := IF(
    @favorite_table_exists = 1 AND @has_created_at = 1,
    'ALTER TABLE `favorite` MODIFY COLUMN `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt FROM @normalize_created_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @normalize_create_time_sql := IF(
    @favorite_table_exists = 1,
    'ALTER TABLE `favorite` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''',
    'SELECT 1'
);
PREPARE stmt FROM @normalize_create_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @normalize_update_time_sql := IF(
    @favorite_table_exists = 1,
    'ALTER TABLE `favorite` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'SELECT 1'
);
PREPARE stmt FROM @normalize_update_time_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @legacy_user_fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'favorite'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'sys_user'
    LIMIT 1
);

SET @drop_legacy_user_fk_sql := IF(
    @legacy_user_fk_name IS NOT NULL,
    CONCAT('ALTER TABLE `favorite` DROP FOREIGN KEY `', @legacy_user_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @drop_legacy_user_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @favorite_user_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'favorite'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'user'
);

SET @add_runtime_user_fk_sql := IF(
    @favorite_table_exists = 1 AND @favorite_user_fk_exists = 0,
    'ALTER TABLE `favorite` ADD CONSTRAINT `fk_favorite_user_runtime` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)',
    'SELECT 1'
);
PREPARE stmt FROM @add_runtime_user_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
