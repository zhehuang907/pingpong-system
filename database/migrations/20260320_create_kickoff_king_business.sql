CREATE TABLE IF NOT EXISTS `kickoff_king_season` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `season_code` VARCHAR(32) NOT NULL,
  `season_name` VARCHAR(100) NOT NULL,
  `hero_title` VARCHAR(160) NOT NULL,
  `hero_subtitle` VARCHAR(255) NULL,
  `season_status` VARCHAR(32) NOT NULL DEFAULT 'live',
  `total_stops` INT NOT NULL DEFAULT 0,
  `completed_stops` INT NOT NULL DEFAULT 0,
  `current_stop_name` VARCHAR(100) NULL,
  `season_start` DATE NULL,
  `season_end` DATE NULL,
  `announcement` VARCHAR(255) NULL,
  `highlight_tags` TEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kickoff_king_season_code` (`season_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `kickoff_king_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `season_id` BIGINT NOT NULL,
  `stage_name` VARCHAR(100) NOT NULL,
  `city_name` VARCHAR(64) NOT NULL,
  `stage_type` VARCHAR(32) NOT NULL,
  `stage_status` VARCHAR(32) NOT NULL DEFAULT 'upcoming',
  `stage_date` DATE NULL,
  `theme` VARCHAR(120) NULL,
  `focus_skill` VARCHAR(64) NULL,
  `highlight_desc` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kickoff_king_stage_season` (`season_id`, `stage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `kickoff_king_standing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `season_id` BIGINT NOT NULL,
  `rank_position` INT NOT NULL,
  `player_name` VARCHAR(64) NOT NULL,
  `association_name` VARCHAR(64) NULL,
  `points` INT NOT NULL DEFAULT 0,
  `stage_wins` INT NOT NULL DEFAULT 0,
  `podium_count` INT NOT NULL DEFAULT 0,
  `serve_style` VARCHAR(100) NULL,
  `hot_form` VARCHAR(100) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kickoff_king_rank` (`season_id`, `rank_position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `kickoff_king_season` (
  `season_code`, `season_name`, `hero_title`, `hero_subtitle`, `season_status`,
  `total_stops`, `completed_stops`, `current_stop_name`, `season_start`, `season_end`,
  `announcement`, `highlight_tags`
)
SELECT
  'KK-2026',
  '2026 开球王积分赛季',
  '把开球质量、前三板节奏和积分走势一起看懂',
  '从分站积分到装备路线，把赛事热度直接变成你的决策参考。',
  'live',
  6,
  3,
  '杭州站',
  '2026-02-01',
  '2026-10-30',
  '当前进入中盘积分拉锯阶段，争冠组与追分组的装备选择都开始出现明显分化。',
  JSON_ARRAY('发球抢攻', '积分追踪', '器材专题', '画像联动')
WHERE NOT EXISTS (
  SELECT 1 FROM `kickoff_king_season` WHERE `season_code` = 'KK-2026'
);

SET @season_id = (
  SELECT `id` FROM `kickoff_king_season` WHERE `season_code` = 'KK-2026' LIMIT 1
);

INSERT INTO `kickoff_king_stage` (`season_id`, `stage_name`, `city_name`, `stage_type`, `stage_status`, `stage_date`, `theme`, `focus_skill`, `highlight_desc`)
SELECT @season_id, '揭幕站', '深圳', '积分赛', 'completed', '2026-02-16', '快节奏抢开局', '旋转', '首站积分把发球质量和前三板抢攻的重要性彻底拉满。'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_stage` WHERE `season_id` = @season_id AND `stage_name` = '揭幕站');

INSERT INTO `kickoff_king_stage` (`season_id`, `stage_name`, `city_name`, `stage_type`, `stage_status`, `stage_date`, `theme`, `focus_skill`, `highlight_desc`)
SELECT @season_id, '海口站', '海口', '积分赛', 'completed', '2026-03-02', '中远台速度对冲', '速度', '胶皮出球效率和底板一板质量成为追分选手的分水岭。'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_stage` WHERE `season_id` = @season_id AND `stage_name` = '海口站');

INSERT INTO `kickoff_king_stage` (`season_id`, `stage_name`, `city_name`, `stage_type`, `stage_status`, `stage_date`, `theme`, `focus_skill`, `highlight_desc`)
SELECT @season_id, '杭州站', '杭州', '积分赛', 'live', '2026-03-28', '控场发球与二跳落点', '控制', '当前分站更强调落点组织与二跳短球控制，器材容错开始主导对局。'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_stage` WHERE `season_id` = @season_id AND `stage_name` = '杭州站');

INSERT INTO `kickoff_king_stage` (`season_id`, `stage_name`, `city_name`, `stage_type`, `stage_status`, `stage_date`, `theme`, `focus_skill`, `highlight_desc`)
SELECT @season_id, '成都总决选拔', '成都', '晋级赛', 'upcoming', '2026-04-18', '高压对抗与抗压落点', '进攻', '总决选拔会把强对抗和连续上手能力进一步放大。'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_stage` WHERE `season_id` = @season_id AND `stage_name` = '成都总决选拔');

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 1, '林启航', '华东联队', 2480, 2, 3, '高抛旋转发球 + 三板冲压', '近三站两冠一亚'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 1);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 2, '周沐辰', '南方俱乐部', 2310, 1, 3, '短球控制 + 反手先上手', '连续三站进四强'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 2);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 3, '顾星野', '北城队', 2195, 1, 2, '左手侧旋发球 + 快撕衔接', '上一站积分暴涨'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 3);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 4, '沈嘉屿', '江海联合', 2050, 0, 2, '落点调动 + 中路封锁', '稳居争冠组'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 4);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 5, '叶知行', '青训联盟', 1960, 0, 1, '反手拧拉 + 快节奏压迫', '新人王冲榜'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 5);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 6, '唐亦鸣', '东南协作队', 1885, 0, 1, '低平快发球 + 正手抢冲', '追分组头号选手'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 6);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 7, '陆闻溪', '西部战队', 1760, 0, 1, '摆短控制 + 连续相持', '控制打法代表'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 7);

INSERT INTO `kickoff_king_standing` (`season_id`, `rank_position`, `player_name`, `association_name`, `points`, `stage_wins`, `podium_count`, `serve_style`, `hot_form`)
SELECT @season_id, 8, '陈澈', '城市挑战队', 1680, 0, 0, '侧上旋变化 + 二跳落点', '保持积分上升'
WHERE NOT EXISTS (SELECT 1 FROM `kickoff_king_standing` WHERE `season_id` = @season_id AND `rank_position` = 8);
