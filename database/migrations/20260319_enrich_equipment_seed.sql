-- =====================================================
-- 乒乓球装备推荐系统 — 装备数据丰富化迁移脚本
-- 迁移日期：2026-03-19
-- 新增约 20 条覆盖各分类的标准乒乓球装备数据
-- 类别 ID: 底板=1, 胶皮=2, 球鞋=3, 服装=4
-- 使用 WHERE NOT EXISTS 保证幂等执行
-- =====================================================

-- SECTION 1: 底板类 (category_id=1) — 8 条
INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '红双喜 狂飙龙 5', 1, '红双喜', 'Hurricane Long 5', 1580.00, 1780.00, 25, '马龙同款专业比赛底板，五层纯木加两层芳碳，极致控制与底劲兼备。', '/uploads/seed/dhs-hurricane-long5.jpg', '专业弧圈、高级进攻型', '五木两碳,内置芳碳,控制精准,旋转强烈', 4.95, 186, 5820, 1240, 680, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '红双喜 狂飙龙 5');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '蝴蝶 樊振东 ALC', 1, '蝴蝶', 'FZD ALC', 2180.00, 2480.00, 15, '樊振东同款，超级纤维底板，暴力美学之作，蓝芳碳带来速度极限与力量感。', '/uploads/seed/butterfly-fzd-alc.jpg', '专业弧圈、力量型进攻', '超级纤维结构,蓝芳碳,极速爆发,顶级相持', 4.92, 204, 6910, 1580, 720, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '蝴蝶 樊振东 ALC');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '红双喜 狂飙王 王楚钦同款', 1, '红双喜', 'Hurricane Wang', 1380.00, 1580.00, 20, '王楚钦同款底板，新一代利刃，灵动衔接与近台爆发的黄金平衡。', '/uploads/seed/dhs-hurricane-wang.jpg', '专业快攻、近台进攻', '五层纯木,轻量灵动,近台衔接快', 4.90, 156, 4830, 960, 510, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '红双喜 狂飙王 王楚钦同款');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT 'STIGA 奥恰洛夫碳王 OC', 1, '斯帝卡', 'Carbonado', 1280.00, 1480.00, 18, '奥恰洛夫用刀，独特碳素内置结构，提供极佳速度与弧线同时保留控制感。', '/uploads/seed/stiga-carbonado.jpg', '专业弧圈、进攻型', '碳素内置,高弹性,弧线出色', 4.82, 128, 3640, 740, 420, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = 'STIGA 奥恰洛夫碳王 OC');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '银河 MC-2 全能五木底板', 1, '银河', 'MC-2', 269.00, 329.00, 96, '经典五木结构，控制细腻，适合初中级爱好者训练与业余比赛使用。', '/uploads/seed/galaxy-mc2.jpg', '初中级训练、控制型打法', '五木结构,轻量手感,控球稳定,性价比高', 4.60, 68, 1820, 320, 480, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '银河 MC-2 全能五木底板');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '亚萨卡 蝶王 D70', 1, '亚萨卡', 'D70', 880.00, 980.00, 30, '亚萨卡旗舰弧圈底板，五木两碳设计，速度与旋转的完美融合。', '/uploads/seed/yasaka-d70.jpg', '弧圈结合快攻、中高级', '五木两碳,弧圈专用,上台率高,旋转强', 4.75, 94, 2760, 530, 310, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '亚萨卡 蝶王 D70');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '红双喜 6星入门直拍', 1, '红双喜', '6star', 98.00, 128.00, 200, '适合初学者快速入门的直拍底板套装，附赠双面套胶，开箱即可对打。', '/uploads/seed/dhs-6star-penholder.jpg', '初学者、入门练习', '直拍设计,附赠套胶,轻量便携,性价比高', 4.30, 210, 3480, 580, 1200, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '红双喜 6星入门直拍');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '斯帝卡 Allround Classic 全能', 1, '斯帝卡', 'Allround Classic', 450.00, 550.00, 45, '均衡系全能底板，软木结构手感细腻，削球与控制型玩家的理想选择。', '/uploads/seed/stiga-allround-classic.jpg', '控制型、削球、全面型', '七层纯木,全能均衡,震感柔和', 4.68, 112, 2180, 390, 260, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '斯帝卡 Allround Classic 全能');

-- SECTION 2: 套胶类 (category_id=2) — 6 条
INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '红双喜 国狂Pro 蓝海绵', 2, '红双喜', 'BlueSponge', 480.00, 568.00, 60, '国家队特供同款，蓝海绵版本，更强旋转与更高平台稳定性，馆球专属。', '/uploads/seed/dhs-blue-sponge.jpg', '专业弧圈、馆球环境', '蓝海绵,粘性套胶,旋转首位,馆球最优', 4.93, 248, 7200, 1680, 890, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '红双喜 国狂Pro 蓝海绵');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '蝴蝶 Dignics 09C 陈梦同款', 2, '蝴蝶', 'Dignics 09C', 620.00, 680.00, 40, '陈梦同款，超级张力粘性反胶，旋转与速度融合，东京奥运夺冠装备。', '/uploads/seed/butterfly-dignics09c.jpg', '专业弧圈、力量型', '张力粘性反胶,超强旋转,高速弹性', 4.95, 312, 8940, 2100, 760, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '蝴蝶 Dignics 09C 陈梦同款');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '亚萨卡 天极Neo 反胶', 2, '亚萨卡', 'Neo Energy', 238.00, 278.00, 80, '中德合作款，高弹海绵配合粘性面胶，价格亲民但性能媲美高端产品。', '/uploads/seed/yasaka-neotenergy.jpg', '中级弧圈、进阶爱好者', '粘性反胶,高弹海绵,弧线长,性价比高', 4.72, 168, 3680, 720, 540, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '亚萨卡 天极Neo 反胶');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '银河 酷摩短颗粒套胶', 2, '银河', 'Pips', 98.00, 128.00, 120, '经典短颗粒正胶，快攻风格专用，击球感扎实，注重速度而非旋转。', '/uploads/seed/galaxy-pips.jpg', '快攻型、直拍推挡', '短颗粒,快攻专用,击球扎实,速度优先', 4.45, 86, 1640, 280, 380, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '银河 酷摩短颗粒套胶');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '多尼克 F1长胶套胶', 2, '多尼克', 'F1 Longpips', 168.00, 198.00, 60, '专为削球和防守型选手设计的长胶套胶，变化多端让对手难以判断。', '/uploads/seed/donic-f1-longpips.jpg', '削球、防守反攻', '长胶套胶,变化丰富,削球稳定,防守利器', 4.48, 74, 1420, 210, 180, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '多尼克 F1长胶套胶');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '友谊729 天翼反胶套胶', 2, '友谊729', 'TianYi', 129.00, 149.00, 150, '控制与性价比并重的训练专用反胶，适合初中级，日常对打无忧。', '/uploads/seed/729-tianyi.jpg', '入门训练、控制型', '反胶套胶,吃球稳定,性价比高', 4.50, 142, 2840, 490, 820, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '友谊729 天翼反胶套胶');

-- SECTION 3: 球鞋类 (category_id=3) — 3 条
INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '蝴蝶 LEZOLINE-11 专业球鞋', 3, '蝴蝶', 'LEZOLINE-11', 680.00, 780.00, 35, '专业乒乓球比赛用鞋，轻量化鞋底，全方向防滑纹路，启动与变向灵敏。', '/uploads/seed/butterfly-lezoline11.jpg', '比赛型、专业训练', '轻量专业,全向防滑,减震支撑,透气舒适', 4.82, 134, 2860, 580, 320, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '蝴蝶 LEZOLINE-11 专业球鞋');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '李宁 AYZP001 国家队球鞋', 3, '李宁', 'AYZP001', 480.00, 580.00, 48, '国家队队员配备款，超轻外底配合高回弹缓震，纵向爆发力强。', '/uploads/seed/lining-ayzp001.jpg', '专业赛场、进阶训练', '超轻鞋底,高回弹缓震,包裹舒适,防滑耐磨', 4.78, 102, 2240, 460, 280, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '李宁 AYZP001 国家队球鞋');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '双鱼 F55 馆内训练球鞋', 3, '双鱼', 'F55', 239.00, 299.00, 88, '抓地稳定、回弹轻快，适合馆内对抗和日常训练，性价比突出。', '/uploads/seed/dhs-f55-shoes.jpg', '日常训练、馆内比赛', '轻量鞋底,防滑耐磨,包裹稳定', 4.55, 68, 1680, 240, 310, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '双鱼 F55 馆内训练球鞋');

-- SECTION 4: 服装配件类 (category_id=4) — 3 条
INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '李宁 国家队比赛服套装', 4, '李宁', 'AAYT041', 298.00, 368.00, 80, '国家队同款比赛服（上衣加短裤套装），速干透气，运动剪裁自由度高。', '/uploads/seed/lining-national-jersey.jpg', '比赛、训练、社团', '速干面料,透气轻薄,运动剪裁', 4.60, 88, 1920, 360, 420, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '李宁 国家队比赛服套装');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '红双喜 20只装专业比赛球', 4, '红双喜', 'D40+', 88.00, 108.00, 500, '国际乒联认证赛用球，40+新材料，旋转与速度均衡，适合各类场馆训练。', '/uploads/seed/dhs-d40plus.jpg', '正式训练、比赛', '新材料球,耐摩耐打,圆度高,旋转均衡', 4.80, 320, 4680, 860, 1980, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '红双喜 20只装专业比赛球');

INSERT INTO equipment (name, category_id, brand, model, price, original_price, stock, description, main_image, suitable_for, features, rating, rating_count, view_count, favorite_count, sales_count, status, is_recommend)
SELECT '蝴蝶 球拍保护套 专业版', 4, '蝴蝶', 'CS-8', 58.00, 78.00, 200, '防震海绵内衬拍套，保护套胶不变形，适合各尺寸横拍与直拍收纳。', '/uploads/seed/butterfly-cs8-case.jpg', '全类型球拍', '防震内衬,防潮材质,轻巧便携', 4.65, 168, 1480, 320, 680, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '蝴蝶 球拍保护套 专业版');

-- SECTION 5: Tag associations (INSERT IGNORE prevents FK violations on non-existent tags)
INSERT IGNORE INTO equipment_tag (equipment_id, tag_id)
SELECT e.id, t.id FROM equipment e CROSS JOIN tag t
WHERE
  (e.name = '红双喜 狂飙龙 5' AND t.name IN ('弧圈', '专业', '旋转强', '红双喜'))
  OR (e.name = '蝴蝶 樊振东 ALC' AND t.name IN ('弧圈', '快攻', '专业', '蝴蝶', '弹性好'))
  OR (e.name = '红双喜 狂飙王 王楚钦同款' AND t.name IN ('快攻', '专业', '红双喜'))
  OR (e.name = 'STIGA 奥恰洛夫碳王 OC' AND t.name IN ('弧圈', '专业', '斯帝卡', '弹性好'))
  OR (e.name = '银河 MC-2 全能五木底板' AND t.name IN ('全能', '控制好', '入门', '进阶'))
  OR (e.name = '亚萨卡 蝶王 D70' AND t.name IN ('弧圈', '进阶', '亚萨卡', '旋转强'))
  OR (e.name = '红双喜 6星入门直拍' AND t.name IN ('快攻', '入门', '红双喜', '控制好'))
  OR (e.name = '斯帝卡 Allround Classic 全能' AND t.name IN ('削球', '全能', '斯帝卡', '控制好'))
  OR (e.name = '红双喜 国狂Pro 蓝海绵' AND t.name IN ('弧圈', '专业', '旋转强', '红双喜'))
  OR (e.name = '蝴蝶 Dignics 09C 陈梦同款' AND t.name IN ('弧圈', '专业', '蝴蝶', '旋转强', '弹性好'))
  OR (e.name = '亚萨卡 天极Neo 反胶' AND t.name IN ('弧圈', '进阶', '亚萨卡', '旋转强'))
  OR (e.name = '银河 酷摩短颗粒套胶' AND t.name IN ('快攻', '入门', '进阶'))
  OR (e.name = '友谊729 天翼反胶套胶' AND t.name IN ('全能', '控制好', '入门'))
  OR (e.name = '多尼克 F1长胶套胶' AND t.name IN ('削球', '全能', '控制好'))
  OR (e.name = '蝴蝶 LEZOLINE-11 专业球鞋' AND t.name IN ('专业', '蝴蝶', '轻量'))
  OR (e.name = '李宁 AYZP001 国家队球鞋' AND t.name IN ('专业', '进阶', '轻量'))
  OR (e.name = '双鱼 F55 馆内训练球鞋' AND t.name IN ('进阶', '入门', '控制好'))
  OR (e.name = '李宁 国家队比赛服套装' AND t.name IN ('专业', '进阶'))
  OR (e.name = '红双喜 20只装专业比赛球' AND t.name IN ('专业', '红双喜'))
  OR (e.name = '蝴蝶 球拍保护套 专业版' AND t.name IN ('全能', '蝴蝶'));

-- Summary
SELECT COUNT(*) AS total_equipment FROM equipment WHERE deleted = 0;
