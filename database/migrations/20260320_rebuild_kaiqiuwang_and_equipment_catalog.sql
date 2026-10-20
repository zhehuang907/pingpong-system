START TRANSACTION;

SET NAMES utf8mb4;

UPDATE kickoff_king_season
SET
  season_code = 'KQW-2026',
  season_name = '2026 开球网积分赛事',
  hero_title = '把开球网积分走势、分站焦点和装备路线串成一条决策链',
  hero_subtitle = '从开球网城市分站到器材推荐，把积分变化、选手打法和你的画像偏好放进同一套视图里。',
  season_status = 'live',
  total_stops = 6,
  completed_stops = 3,
  current_stop_name = '杭州分站',
  announcement = '当前进入春季抢分窗口，分站积分、打法路线和装备选择已经开始出现明显分层。',
  highlight_tags = '["开球网积分赛","城市分站","打法跟练","装备路线"]',
  update_time = NOW();

UPDATE kickoff_king_stage
SET
  stage_name = '春季揭幕积分站',
  city_name = '深圳',
  stage_type = '城市分站',
  stage_status = 'completed',
  theme = '抢开局与前三板压制',
  focus_skill = '旋转',
  highlight_desc = '揭幕站把高质量发球与前三板转换效率直接拉成积分差距，也是开球网赛事的首个关注峰值。'
WHERE id = 1;

UPDATE kickoff_king_stage
SET
  stage_name = '华南抢分站',
  city_name = '海口',
  stage_type = '城市分站',
  stage_status = 'completed',
  theme = '中远台提速与反衔接',
  focus_skill = '速度',
  highlight_desc = '这一站更强调连续提速和反衔接质量，装备选择开始向速度型和弹击型倾斜。'
WHERE id = 2;

UPDATE kickoff_king_stage
SET
  stage_name = '华东积分焦点站',
  city_name = '杭州',
  stage_type = '城市分站',
  stage_status = 'live',
  theme = '落点组织与二跳控制',
  focus_skill = '控制',
  highlight_desc = '当前分站更看重二跳短球、落点组织和控场能力，稳定型器材开始成为讨论中心。'
WHERE id = 3;

UPDATE kickoff_king_stage
SET
  stage_name = '西南总决选拔站',
  city_name = '成都',
  stage_type = '区域选拔',
  stage_status = 'upcoming',
  theme = '高压对抗与质量衔接',
  focus_skill = '进攻',
  highlight_desc = '进入选拔赛后，对抗强度和质量衔接会明显上升，装备路线也会更偏向高强度进攻。'
WHERE id = 4;

UPDATE kickoff_king_standing
SET association_name = '开球网·华东联队', hot_form = '近三站两冠一亚'
WHERE rank_position = 1;

UPDATE kickoff_king_standing
SET association_name = '开球网·南方俱乐部', hot_form = '连续三站闯入四强'
WHERE rank_position = 2;

UPDATE kickoff_king_standing
SET association_name = '开球网·北城挑战队', hot_form = '上一站积分涨幅最快'
WHERE rank_position = 3;

UPDATE kickoff_king_standing
SET association_name = '开球网·江海联合', hot_form = '稳居争冠观察组'
WHERE rank_position = 4;

UPDATE kickoff_king_standing
SET association_name = '开球网·青训联盟', hot_form = '新人组选手持续冲榜'
WHERE rank_position = 5;

UPDATE kickoff_king_standing
SET association_name = '开球网·东南协作队', hot_form = '追分组头号抢分手'
WHERE rank_position = 6;

UPDATE kickoff_king_standing
SET association_name = '开球网·西部战队', hot_form = '控制流代表持续升温'
WHERE rank_position = 7;

UPDATE kickoff_king_standing
SET association_name = '开球网·城市挑战队', hot_form = '积分曲线保持上扬'
WHERE rank_position = 8;

CREATE TEMPORARY TABLE equipment_dedup_map (
  old_id BIGINT PRIMARY KEY,
  new_id BIGINT NOT NULL
);

INSERT INTO equipment_dedup_map (old_id, new_id) VALUES
  (21, 30),
  (22, 52),
  (23, 42),
  (28, 45),
  (32, 46),
  (33, 47),
  (34, 48),
  (36, 49),
  (37, 50),
  (38, 52),
  (39, 51),
  (41, 53);

INSERT INTO favorite (user_id, equipment_id, create_time, update_time, created_at, deleted)
SELECT
  f.user_id,
  m.new_id,
  MIN(f.create_time),
  NOW(),
  MIN(f.created_at),
  0
FROM favorite f
JOIN equipment_dedup_map m ON f.equipment_id = m.old_id
WHERE f.deleted = 0
GROUP BY f.user_id, m.new_id
ON DUPLICATE KEY UPDATE
  deleted = 0,
  update_time = NOW(),
  create_time = LEAST(COALESCE(favorite.create_time, VALUES(create_time)), VALUES(create_time)),
  created_at = LEAST(COALESCE(favorite.created_at, VALUES(created_at)), VALUES(created_at));

UPDATE favorite f
JOIN equipment_dedup_map m ON f.equipment_id = m.old_id
SET
  f.deleted = 1,
  f.update_time = NOW()
WHERE f.deleted = 0;

INSERT INTO shopping_cart (user_id, equipment_id, quantity, selected, create_time, update_time, deleted)
SELECT
  sc.user_id,
  m.new_id,
  SUM(sc.quantity),
  MAX(sc.selected),
  MIN(sc.create_time),
  NOW(),
  0
FROM shopping_cart sc
JOIN equipment_dedup_map m ON sc.equipment_id = m.old_id
WHERE sc.deleted = 0
GROUP BY sc.user_id, m.new_id
ON DUPLICATE KEY UPDATE
  quantity = GREATEST(1, shopping_cart.quantity + VALUES(quantity)),
  selected = GREATEST(shopping_cart.selected, VALUES(selected)),
  deleted = 0,
  update_time = NOW(),
  create_time = LEAST(shopping_cart.create_time, VALUES(create_time));

UPDATE shopping_cart sc
JOIN equipment_dedup_map m ON sc.equipment_id = m.old_id
SET
  sc.deleted = 1,
  sc.update_time = NOW()
WHERE sc.deleted = 0;

INSERT INTO equipment_tag (equipment_id, tag_id, deleted)
SELECT DISTINCT
  m.new_id,
  et.tag_id,
  0
FROM equipment_tag et
JOIN equipment_dedup_map m ON et.equipment_id = m.old_id
WHERE et.deleted = 0
ON DUPLICATE KEY UPDATE
  deleted = 0;

UPDATE equipment_tag et
JOIN equipment_dedup_map m ON et.equipment_id = m.old_id
SET et.deleted = 1
WHERE et.deleted = 0;

UPDATE browse_history bh
JOIN equipment_dedup_map m ON bh.equipment_id = m.old_id
SET bh.equipment_id = m.new_id;

UPDATE behavior_log bl
JOIN equipment_dedup_map m ON bl.equipment_id = m.old_id
SET bl.equipment_id = m.new_id;

UPDATE comment c
JOIN equipment_dedup_map m ON c.equipment_id = m.old_id
SET c.equipment_id = m.new_id;

UPDATE market_item mi
JOIN equipment_dedup_map m ON mi.equipment_id = m.old_id
SET mi.equipment_id = m.new_id;

UPDATE recommend_record rr
JOIN equipment_dedup_map m ON rr.equipment_id = m.old_id
SET rr.equipment_id = m.new_id;

INSERT INTO leaderboard_share_stat (board_type, equipment_id, channel, share_count, last_share_user_id, last_shared_at, create_time, update_time)
SELECT
  stat.board_type,
  m.new_id,
  stat.channel,
  SUM(stat.share_count),
  MAX(stat.last_share_user_id),
  MAX(stat.last_shared_at),
  MIN(stat.create_time),
  NOW()
FROM leaderboard_share_stat stat
JOIN equipment_dedup_map m ON stat.equipment_id = m.old_id
GROUP BY stat.board_type, m.new_id, stat.channel
ON DUPLICATE KEY UPDATE
  share_count = leaderboard_share_stat.share_count + VALUES(share_count),
  last_share_user_id = COALESCE(VALUES(last_share_user_id), leaderboard_share_stat.last_share_user_id),
  last_shared_at = GREATEST(
    COALESCE(leaderboard_share_stat.last_shared_at, '1970-01-01 00:00:00'),
    COALESCE(VALUES(last_shared_at), '1970-01-01 00:00:00')
  ),
  update_time = NOW();

DELETE stat
FROM leaderboard_share_stat stat
JOIN equipment_dedup_map m ON stat.equipment_id = m.old_id;

INSERT IGNORE INTO player_equipment_relation (player_id, equipment_id, type, is_current)
SELECT
  per.player_id,
  m.new_id,
  per.type,
  per.is_current
FROM player_equipment_relation per
JOIN equipment_dedup_map m ON per.equipment_id = m.old_id;

DELETE per
FROM player_equipment_relation per
JOIN equipment_dedup_map m ON per.equipment_id = m.old_id;

UPDATE equipment_combination ec
JOIN equipment_dedup_map m ON ec.blade_id = m.old_id
SET ec.blade_id = m.new_id;

UPDATE equipment_combination ec
JOIN equipment_dedup_map m ON ec.forehand_rubber_id = m.old_id
SET ec.forehand_rubber_id = m.new_id;

UPDATE equipment_combination ec
JOIN equipment_dedup_map m ON ec.backhand_rubber_id = m.old_id
SET ec.backhand_rubber_id = m.new_id;

UPDATE equipment
SET
  name = '银河 MC-2 全能五木底板',
  brand = '银河',
  model = 'MC-2',
  original_price = 329.00,
  stock = 96,
  main_image = '/uploads/seed/galaxy-mc2.jpg',
  suitable_for = '初中级训练、控制型打法',
  features = '五木结构,轻量手感,控球稳定,性价比高',
  rating = 4.60,
  rating_count = 68,
  view_count = 1820,
  favorite_count = 320,
  sales_count = 480,
  status = 1,
  is_recommend = 1,
  update_time = NOW()
WHERE id = 30;

UPDATE equipment
SET
  name = '双鱼 F55 馆内训练球鞋',
  brand = '双鱼',
  model = 'F55',
  original_price = 299.00,
  stock = 88,
  main_image = '/uploads/seed/dhs-f55-shoes.jpg',
  suitable_for = '日常训练、馆内比赛',
  features = '轻量鞋底,防滑耐磨,包裹稳定,性价比高',
  rating = 4.55,
  rating_count = 68,
  view_count = 1680,
  favorite_count = 240,
  sales_count = 310,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 42;

UPDATE equipment
SET
  name = '红双喜 狂飙王 王楚钦同款',
  brand = '红双喜',
  model = '狂飙王',
  original_price = 1580.00,
  stock = 20,
  main_image = '/uploads/seed/dhs-hurricane-wang.jpg',
  suitable_for = '专业快攻、近台进攻',
  features = '五层纯木,轻量灵动,近台衔接快,王楚钦同款',
  rating = 4.90,
  rating_count = 156,
  view_count = 4831,
  favorite_count = 960,
  sales_count = 510,
  status = 1,
  is_recommend = 1,
  update_time = NOW()
WHERE id = 45;

UPDATE equipment
SET
  name = '红双喜 6星入门直拍',
  brand = '红双喜',
  model = '6星',
  original_price = 128.00,
  stock = 200,
  main_image = '/uploads/seed/dhs-6star-penholder.jpg',
  suitable_for = '初学者、入门练习',
  features = '直拍设计,附赠套胶,轻量便携,性价比高',
  rating = 4.30,
  rating_count = 210,
  view_count = 3480,
  favorite_count = 580,
  sales_count = 1200,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 46;

UPDATE equipment
SET
  name = '斯帝卡 Allround Classic 全能',
  brand = '斯帝卡',
  model = 'Allround Classic',
  original_price = 550.00,
  stock = 45,
  main_image = '/uploads/seed/stiga-allround-classic.jpg',
  suitable_for = '控制型、削球、全面型',
  features = '七层纯木,全能均衡,震感柔和,入门专业过渡',
  rating = 4.68,
  rating_count = 112,
  view_count = 2181,
  favorite_count = 390,
  sales_count = 260,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 47;

UPDATE equipment
SET
  name = '红双喜 国狂Pro 蓝海绵',
  brand = '红双喜',
  model = '国狂Pro',
  original_price = 568.00,
  stock = 60,
  main_image = '/uploads/seed/dhs-blue-sponge.jpg',
  suitable_for = '专业弧圈、馆球环境',
  features = '蓝海绵,粘性套胶,旋转首位,馆球最优',
  rating = 5.00,
  rating_count = GREATEST(rating_count, 1),
  view_count = 7218,
  favorite_count = 1682,
  sales_count = 890,
  status = 1,
  is_recommend = 1,
  update_time = NOW()
WHERE id = 48;

UPDATE equipment
SET
  name = '亚萨卡 天极Neo 反胶',
  brand = '亚萨卡',
  model = '天极Neo',
  original_price = 278.00,
  stock = 80,
  main_image = '/uploads/seed/yasaka-neotenergy.jpg',
  suitable_for = '中级弧圈、进阶爱好者',
  features = '粘性反胶,高弹海绵,弧线长,性价比高',
  rating = 4.72,
  rating_count = 168,
  view_count = 3680,
  favorite_count = 720,
  sales_count = 540,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 49;

UPDATE equipment
SET
  name = '银河 酷摩短颗粒套胶',
  brand = '银河',
  model = '酷摩',
  original_price = 128.00,
  stock = 120,
  main_image = '/uploads/seed/galaxy-pips.jpg',
  suitable_for = '快攻型、直拍推挡',
  features = '短颗粒,快攻专用,击球扎实,速度优先',
  rating = 4.45,
  rating_count = 86,
  view_count = 1640,
  favorite_count = 280,
  sales_count = 380,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 50;

UPDATE equipment
SET
  name = '多尼克 F1 长胶套胶',
  brand = '多尼克',
  model = 'F1',
  original_price = 198.00,
  stock = 60,
  main_image = '/uploads/seed/donic-f1-longpips.jpg',
  suitable_for = '削球、防守反攻',
  features = '长胶套胶,变化丰富,削球稳定,防守利器',
  rating = 4.48,
  rating_count = 74,
  view_count = 1420,
  favorite_count = 210,
  sales_count = 180,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 51;

UPDATE equipment
SET
  name = '友谊729 天翼反胶套胶',
  brand = '友谊729',
  model = '天翼',
  original_price = 149.00,
  stock = 150,
  main_image = '/uploads/seed/729-tianyi.jpg',
  suitable_for = '入门训练、控制型',
  features = '反胶套胶,吃球稳定,性价比高,适合训练',
  rating = 4.50,
  rating_count = 142,
  view_count = 2840,
  favorite_count = 490,
  sales_count = 820,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 52;

UPDATE equipment
SET
  name = '李宁 AYZP001 国家队球鞋',
  brand = '李宁',
  model = 'AYZP001',
  original_price = 580.00,
  stock = 48,
  main_image = '/uploads/seed/lining-ayzp001.jpg',
  suitable_for = '专业赛场、进阶训练',
  features = '超轻鞋底,高回弹缓震,包裹舒适,防滑耐磨',
  rating = 4.78,
  rating_count = 102,
  view_count = 2240,
  favorite_count = 460,
  sales_count = 280,
  status = 1,
  is_recommend = 0,
  update_time = NOW()
WHERE id = 53;

UPDATE equipment e
JOIN equipment_dedup_map m ON e.id = m.old_id
SET
  e.status = 0,
  e.deleted = 1,
  e.update_time = NOW();

CREATE TEMPORARY TABLE equipment_refresh_ids (
  equipment_id BIGINT PRIMARY KEY
);

INSERT INTO equipment_refresh_ids (equipment_id)
SELECT old_id FROM equipment_dedup_map;

INSERT IGNORE INTO equipment_refresh_ids (equipment_id)
SELECT new_id FROM equipment_dedup_map;

DELETE snapshot
FROM leaderboard_snapshot snapshot
JOIN equipment_refresh_ids refresh_ids ON snapshot.equipment_id = refresh_ids.equipment_id;

COMMIT;

SELECT season_code, season_name, current_stop_name FROM kickoff_king_season;
SELECT id, name, brand, model, category_id, price, status, deleted FROM equipment WHERE deleted = 0 ORDER BY category_id, brand, name;
