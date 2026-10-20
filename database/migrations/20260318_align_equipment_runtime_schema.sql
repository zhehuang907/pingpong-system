SET @schema_name := DATABASE();

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'model'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `model` VARCHAR(100) NULL AFTER `brand`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'original_price'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `original_price` DECIMAL(10,2) NULL AFTER `price`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'stock'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `stock` INT NOT NULL DEFAULT 0 AFTER `original_price`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'parameters'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `parameters` JSON NULL AFTER `description`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'images'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `images` JSON NULL AFTER `parameters`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'main_image'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `main_image` VARCHAR(255) NULL AFTER `images`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'suitable_for'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `suitable_for` VARCHAR(255) NULL AFTER `video_url`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'features'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `features` TEXT NULL AFTER `suitable_for`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'rating'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `rating` DECIMAL(3,2) NOT NULL DEFAULT 0 AFTER `features`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'rating_count'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `rating_count` INT NOT NULL DEFAULT 0 AFTER `rating`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'view_count'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `view_count` INT NOT NULL DEFAULT 0 AFTER `rating_count`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'favorite_count'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `favorite_count` INT NOT NULL DEFAULT 0 AFTER `view_count`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'sales_count'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `sales_count` INT NOT NULL DEFAULT 0 AFTER `favorite_count`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'tags'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `tags` JSON NULL AFTER `sales_count`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'status'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 AFTER `tags`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'is_recommend'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `is_recommend` TINYINT NOT NULL DEFAULT 0 AFTER `status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'create_time'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `is_recommend`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'update_time'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND COLUMN_NAME = 'deleted'),
  'SELECT 1',
  'ALTER TABLE `equipment` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `update_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `equipment`
SET
  `original_price` = COALESCE(`original_price`, `price`),
  `stock` = CASE WHEN `stock` IS NULL OR `stock` = 0 THEN 99 ELSE `stock` END,
  `main_image` = CASE
    WHEN (`main_image` IS NULL OR `main_image` = '') AND `image_url` IS NOT NULL AND `image_url` <> '' THEN `image_url`
    ELSE `main_image`
  END,
  `images` = CASE
    WHEN `images` IS NULL AND `image_url` IS NOT NULL AND `image_url` <> '' THEN JSON_ARRAY(`image_url`)
    ELSE `images`
  END,
  `suitable_for` = CASE
    WHEN (`suitable_for` IS NULL OR `suitable_for` = '') AND `level` IS NOT NULL AND `level` <> '' THEN `level`
    ELSE `suitable_for`
  END,
  `rating` = CASE
    WHEN `rating` IS NULL OR `rating` = 0 THEN LEAST(5, GREATEST(0, ROUND(COALESCE(`hot_score`, 0) / 30, 2)))
    ELSE `rating`
  END,
  `rating_count` = COALESCE(`rating_count`, 0),
  `view_count` = GREATEST(COALESCE(`view_count`, 0), COALESCE(`hot_score`, 0)),
  `favorite_count` = COALESCE(`favorite_count`, 0),
  `sales_count` = COALESCE(`sales_count`, 0),
  `status` = COALESCE(`status`, 1),
  `is_recommend` = CASE
    WHEN `is_recommend` IS NULL THEN CASE WHEN COALESCE(`hot_score`, 0) >= 100 THEN 1 ELSE 0 END
    ELSE `is_recommend`
  END,
  `create_time` = COALESCE(`create_time`, `created_at`, NOW()),
  `update_time` = COALESCE(`update_time`, `updated_at`, NOW()),
  `deleted` = COALESCE(`deleted`, 0);

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND INDEX_NAME = 'idx_equipment_status'),
  'SELECT 1',
  'CREATE INDEX `idx_equipment_status` ON `equipment` (`status`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND INDEX_NAME = 'idx_equipment_view_count'),
  'SELECT 1',
  'CREATE INDEX `idx_equipment_view_count` ON `equipment` (`view_count`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'equipment' AND INDEX_NAME = 'idx_equipment_favorite_count'),
  'SELECT 1',
  'CREATE INDEX `idx_equipment_favorite_count` ON `equipment` (`favorite_count`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
