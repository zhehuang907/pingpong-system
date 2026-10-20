CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车项 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `equipment_id` BIGINT NOT NULL COMMENT '装备 ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_cart_user_equipment` (`user_id`, `equipment_id`),
  INDEX `idx_cart_user_id` (`user_id`),
  INDEX `idx_cart_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

SET @equipment_model_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'equipment'
    AND index_name = 'idx_equipment_model'
);
SET @equipment_model_index_sql := IF(
  @equipment_model_index_exists = 0,
  'ALTER TABLE `equipment` ADD INDEX `idx_equipment_model` (`model`)',
  'SELECT 1'
);
PREPARE equipment_model_index_stmt FROM @equipment_model_index_sql;
EXECUTE equipment_model_index_stmt;
DEALLOCATE PREPARE equipment_model_index_stmt;

INSERT INTO `equipment` (
  `name`, `category_id`, `brand`, `model`, `price`, `original_price`, `stock`,
  `description`, `main_image`, `suitable_for`, `features`, `rating`, `rating_count`,
  `view_count`, `favorite_count`, `sales_count`, `status`, `is_recommend`
)
SELECT
  '银河 N-10 七层进攻底板',
  COALESCE((SELECT id FROM category WHERE name IN ('底板', '球拍') ORDER BY level ASC, id ASC LIMIT 1), 1),
  '银河',
  'N-10',
  329.00,
  369.00,
  120,
  '适合快攻结合弧圈的全能型七层底板，出球扎实，手感直接。',
  '/uploads/seed/galaxy-n10.jpg',
  '进阶快攻、两面弧圈',
  '七层纯木,支撑稳定,上手快',
  4.70,
  36,
  228,
  42,
  58,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM equipment WHERE name = '银河 N-10 七层进攻底板'
);

INSERT INTO `equipment` (
  `name`, `category_id`, `brand`, `model`, `price`, `original_price`, `stock`,
  `description`, `main_image`, `suitable_for`, `features`, `rating`, `rating_count`,
  `view_count`, `favorite_count`, `sales_count`, `status`, `is_recommend`
)
SELECT
  '银河 MC-2 五木底板',
  COALESCE((SELECT id FROM category WHERE name IN ('底板', '球拍') ORDER BY level ASC, id ASC LIMIT 1), 1),
  '银河',
  'MC-2',
  269.00,
  329.00,
  96,
  '经典五木结构，控制细腻，适合初中级爱好者与通勤训练使用。',
  '/uploads/seed/galaxy-mc2.jpg',
  '初中级训练、控制型打法',
  '五木结构,轻量手感,控球稳定',
  4.60,
  24,
  176,
  31,
  40,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM equipment WHERE name = '银河 MC-2 五木底板'
);

INSERT INTO `equipment` (
  `name`, `category_id`, `brand`, `model`, `price`, `original_price`, `stock`,
  `description`, `main_image`, `suitable_for`, `features`, `rating`, `rating_count`,
  `view_count`, `favorite_count`, `sales_count`, `status`, `is_recommend`
)
SELECT
  '729 天翼反胶套胶',
  COALESCE((SELECT id FROM category WHERE name IN ('胶皮', '套胶') ORDER BY level ASC, id ASC LIMIT 1), 2),
  '友谊 729',
  '天翼',
  129.00,
  149.00,
  150,
  '以控制和性价比见长的反胶套胶，适合入门和训练场景。',
  '/uploads/seed/729-tianyi.jpg',
  '入门训练、控制型打法',
  '反胶套胶,吃球稳定,性价比高',
  4.50,
  42,
  204,
  37,
  62,
  1,
  1
WHERE NOT EXISTS (
  SELECT 1 FROM equipment WHERE name = '729 天翼反胶套胶'
);

INSERT INTO `equipment` (
  `name`, `category_id`, `brand`, `model`, `price`, `original_price`, `stock`,
  `description`, `main_image`, `suitable_for`, `features`, `rating`, `rating_count`,
  `view_count`, `favorite_count`, `sales_count`, `status`, `is_recommend`
)
SELECT
  '双鱼 F55 专业球鞋',
  COALESCE((SELECT id FROM category WHERE name IN ('球鞋', '鞋子') ORDER BY level ASC, id ASC LIMIT 1), 4),
  '双鱼',
  'F55',
  239.00,
  299.00,
  88,
  '抓地稳定、回弹轻快，适合馆内对抗和日常训练。',
  '/uploads/seed/dhs-f55-shoes.jpg',
  '日常训练、馆内比赛',
  '轻量鞋底,防滑耐磨,包裹稳定',
  4.55,
  18,
  128,
  15,
  27,
  1,
  0
WHERE NOT EXISTS (
  SELECT 1 FROM equipment WHERE name = '双鱼 F55 专业球鞋'
);

INSERT INTO `equipment` (
  `name`, `category_id`, `brand`, `model`, `price`, `original_price`, `stock`,
  `description`, `main_image`, `suitable_for`, `features`, `rating`, `rating_count`,
  `view_count`, `favorite_count`, `sales_count`, `status`, `is_recommend`
)
SELECT
  '李宁 竞训速干比赛服',
  COALESCE((SELECT id FROM category WHERE name IN ('服装', '服饰') ORDER BY level ASC, id ASC LIMIT 1), 3),
  '李宁',
  'AT-DRY',
  169.00,
  219.00,
  140,
  '轻薄透气的比赛训练服，适合夏季馆内训练与团队活动。',
  '/uploads/seed/li-ning-jersey.jpg',
  '训练、比赛、社团活动',
  '速干面料,透气轻薄,运动剪裁',
  4.48,
  16,
  102,
  11,
  19,
  1,
  0
WHERE NOT EXISTS (
  SELECT 1 FROM equipment WHERE name = '李宁 竞训速干比赛服'
);
