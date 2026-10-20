-- 为 equipment 表添加 image_url 字段
-- 创建时间: 2026-04-22

SET NAMES utf8mb4;

-- 添加 image_url 列（如果不存在）
SET @dbname = DATABASE();
SET @tablename = 'equipment';
SET @columnname = 'image_url';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(500) NULL AFTER main_image')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 为没有 main_image 但有 image_url 的记录设置 main_image
UPDATE equipment 
SET main_image = image_url 
WHERE (main_image IS NULL OR main_image = '') 
  AND image_url IS NOT NULL 
  AND image_url <> '';

COMMIT;
