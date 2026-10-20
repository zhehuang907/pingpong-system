-- ============================================================================
--  20261020_align_behavior_log_and_system_config.sql
-- ----------------------------------------------------------------------------
--  修复运行期库表漂移（由 RuntimeSchemaCompatibilityIntegrationTest 检出）：
--    behavior_log  : 补齐 equipment_id / behavior_type / weight / created_at
--    system_config : 补齐 config_type
--
--  背景：OperationLogAspect 会向 behavior_log 写入 equipment_id、behavior_type
--  与 weight（用于记录带权重的用户行为），SystemConfig 实体通过
--  @TableField("config_type") 映射 config_type 列。部分环境的历史库由
--  behavior_log 重建而来，缺少这些列，会在拦截器落库时抛出 Unknown column。
--
--  脚本可重复执行：所有 ALTER 均带存在性判断。
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------- behavior_log ----------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'equipment_id') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `equipment_id` BIGINT NULL AFTER `user_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'behavior_type') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `behavior_type` VARCHAR(32) NULL AFTER `equipment_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'weight') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `weight` INT NULL AFTER `behavior_type`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND COLUMN_NAME = 'created_at') = 0,
    'ALTER TABLE `behavior_log` ADD COLUMN `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `behavior_log`
SET `created_at` = COALESCE(`created_at`, `create_time`, CURRENT_TIMESTAMP)
WHERE `created_at` IS NULL;

-- 行为类型索引：推荐相关行为按「用户 + 类型 + 时间」查询
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'behavior_log' AND INDEX_NAME = 'idx_behavior_log_user_type') = 0,
    'ALTER TABLE `behavior_log` ADD INDEX `idx_behavior_log_user_type` (`user_id`, `behavior_type`, `created_at`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------- system_config ----------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'config_type') = 0,
    'ALTER TABLE `system_config` ADD COLUMN `config_type` VARCHAR(32) NULL AFTER `config_name`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
