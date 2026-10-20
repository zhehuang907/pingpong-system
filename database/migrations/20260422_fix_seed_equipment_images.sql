-- 修复种子数据的装备图片路径
-- 将不存在的 seed 图片替换为实际存在的 equipment 图片或使用通用占位图

SET NAMES utf8mb4;
START TRANSACTION;

-- 方案1: 为种子数据装备分配实际存在的图片（按类别匹配）
-- 底板类 (category_id = 6) - 使用 equipment_027_alc.jpg
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_027_alc.jpg',
    images = JSON_ARRAY('/api/upload/equipment/equipment_027_alc.jpg'),
    update_time = NOW()
WHERE id IN (133, 134, 135, 136, 137, 138, 140, 141, 142)
  AND category_id = 6;

-- 成品拍类 (category_id = 7) - 使用 equipment_032_6.png  
UPDATE equipment
SET main_image = '/api/upload/equipment/equipment_032_6.png',
    images = JSON_ARRAY('/api/upload/equipment/equipment_032_6.png'),
    update_time = NOW()
WHERE id = 139
  AND category_id = 7;

-- 套胶类 (category_id = 8) - 使用 equipment_004_tenergy-05.png
UPDATE equipment
SET main_image = '/api/upload/equipment/equipment_004_tenergy-05.png',
    images = JSON_ARRAY('/api/upload/equipment/equipment_004_tenergy-05.png'),
    update_time = NOW()
WHERE id IN (143, 144, 145, 146, 147)
  AND category_id = 8;

COMMIT;
