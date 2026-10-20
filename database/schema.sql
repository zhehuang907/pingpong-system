-- ============================================================================
--  乒乓球装备推荐系统 — 数据库结构 + 演示数据
--  PingPong Recommendation System — Database Schema & Demo Seed
-- ----------------------------------------------------------------------------
--  引擎 / 字符集 : InnoDB / utf8mb4 (utf8mb4_unicode_ci)
--  表数量        : 27
--  演示数据      : category, tag, equipment, equipment_tag, equipment_combination,
--                  player_equipment_relation, star_player, system_config
--  导入方式      : mysql -u root -p pingpong_recommendation < schema.sql
--
--  本文件为「完整结构 + 非个人演示数据」快照，不含用户、评论、浏览行为、
--  登录审计等隐私数据。管理员账号创建见 database/scripts/。
--  结构与后端实体的一致性由 RuntimeSchemaCompatibilityIntegrationTest 保证。
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
  `session_id` bigint NOT NULL COMMENT '会话 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息角色 user/assistant',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '消息状态',
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
  `finish_reason` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束原因',
  `prompt_tokens` int DEFAULT NULL COMMENT '提示 token 数',
  `completion_tokens` int DEFAULT NULL COMMENT '回复 token 数',
  `total_tokens` int DEFAULT NULL COMMENT '总 token 数',
  `related_equipment_ids` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联装备 ID 列表',
  `error_message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_session_id` (`session_id`),
  KEY `idx_ai_chat_message_user_id` (`user_id`),
  KEY `idx_ai_chat_message_role` (`role`),
  KEY `idx_ai_chat_message_create_time` (`create_time`),
  KEY `idx_ai_chat_message_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天消息表';
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话标题',
  `scene` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EQUIPMENT_ADVISOR' COMMENT '会话场景',
  `last_message_preview` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近一条消息预览',
  `message_count` int NOT NULL DEFAULT '0' COMMENT '消息数量',
  `last_message_at` datetime DEFAULT NULL COMMENT '最近消息时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_session_user_id` (`user_id`),
  KEY `idx_ai_chat_session_last_message_at` (`last_message_at`),
  KEY `idx_ai_chat_session_create_time` (`create_time`),
  KEY `idx_ai_chat_session_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天会话表';
DROP TABLE IF EXISTS `behavior_log`;
CREATE TABLE `behavior_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `equipment_id` bigint DEFAULT NULL,
  `behavior_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weight` int DEFAULT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP 地址',
  `url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求 URL',
  `method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块',
  `operation` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作',
  `params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `result` text COLLATE utf8mb4_unicode_ci COMMENT '返回结果',
  `time_taken` bigint DEFAULT NULL COMMENT '耗时 (ms)',
  `user_agent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_module` (`module`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_behavior_log_user_type` (`user_id`,`behavior_type`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志表';
DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `content_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容类型',
  `source_page` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源页面',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话 ID',
  `enter_time` datetime DEFAULT NULL COMMENT '进入时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离开时间',
  `duration_seconds` int NOT NULL DEFAULT '0' COMMENT '停留时长(秒)',
  `interaction_count` int NOT NULL DEFAULT '0' COMMENT '交互次数',
  `interaction_actions` text COLLATE utf8mb4_unicode_ci COMMENT '交互动作 JSON',
  `content_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容快照',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_user_equipment_session` (`user_id`,`equipment_id`,`session_id`),
  KEY `idx_enter_time` (`enter_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类 ID',
  `level` tinyint DEFAULT '1' COMMENT '层级',
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装备分类表';
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论 ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `images` json DEFAULT NULL COMMENT '图片数组',
  `rating` tinyint DEFAULT '5' COMMENT '评分 1-5',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `reply_count` int DEFAULT '0' COMMENT '回复数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-隐藏 1-显示',
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'APPROVED' COMMENT '瀹℃牳鐘舵?锛圥ENDING/APPROVED/REJECTED锛',
  `reviewed_by` bigint DEFAULT NULL COMMENT '瀹℃牳浜篒D',
  `reviewed_at` datetime DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  `review_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀹℃牳澶囨敞',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_comment_equipment_status_time` (`equipment_id`,`status`,`create_time`),
  KEY `idx_comment_equipment_parent_status` (`equipment_id`,`parent_id`,`status`),
  KEY `idx_comment_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
DROP TABLE IF EXISTS `comment_like`;
CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `comment_id` bigint NOT NULL COMMENT '评论 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';
DROP TABLE IF EXISTS `equipment`;
CREATE TABLE `equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '装备 ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '装备名称',
  `category_id` bigint NOT NULL COMMENT '分类 ID',
  `brand` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '型号',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `stock` int DEFAULT '0' COMMENT '库存',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `parameters` json DEFAULT NULL COMMENT '参数规格',
  `images` json DEFAULT NULL COMMENT '图片 URL 数组',
  `main_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主图',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `video_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频 URL',
  `suitable_for` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用人群',
  `features` text COLLATE utf8mb4_unicode_ci COMMENT '特点',
  `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
  `rating_count` int DEFAULT '0' COMMENT '评分人数',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `favorite_count` int DEFAULT '0' COMMENT '收藏次数',
  `sales_count` int DEFAULT '0' COMMENT '销量',
  `tags` json DEFAULT NULL COMMENT '标签 ID 数组',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-下架 1-上架',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐',
  `level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '入门级' COMMENT '等级',
  `hot_score` int DEFAULT '0' COMMENT '热门分数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand` (`brand`),
  KEY `idx_price` (`price`),
  KEY `idx_status` (`status`),
  KEY `idx_is_recommend` (`is_recommend`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_favorite_count` (`favorite_count`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_equipment_status_rating` (`status`,`rating`)
) ENGINE=InnoDB AUTO_INCREMENT=168 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装备表';
DROP TABLE IF EXISTS `equipment_combination`;
CREATE TABLE `equipment_combination` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `blade_id` bigint DEFAULT NULL,
  `forehand_rubber_id` bigint DEFAULT NULL,
  `backhand_rubber_id` bigint DEFAULT NULL,
  `compatibility_score` decimal(5,2) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `equipment_tag`;
CREATE TABLE `equipment_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `tag_id` bigint NOT NULL COMMENT '标签 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_tag` (`equipment_id`,`tag_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_tag_id` (`tag_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=619 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装备 - 标签关联表';
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_equipment` (`user_id`,`equipment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_favorite_equipment_time` (`equipment_id`,`create_time`),
  KEY `idx_favorite_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';
DROP TABLE IF EXISTS `kickoff_king_season`;
CREATE TABLE `kickoff_king_season` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `season_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `season_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hero_title` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hero_subtitle` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `season_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'live',
  `total_stops` int NOT NULL DEFAULT '0',
  `completed_stops` int NOT NULL DEFAULT '0',
  `current_stop_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `season_start` date DEFAULT NULL,
  `season_end` date DEFAULT NULL,
  `announcement` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `highlight_tags` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kickoff_king_season_code` (`season_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `kickoff_king_stage`;
CREATE TABLE `kickoff_king_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `season_id` bigint NOT NULL,
  `stage_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'regular',
  `stage_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'upcoming',
  `stage_date` date NOT NULL,
  `theme` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `focus_skill` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `highlight_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kickoff_king_stage_season` (`season_id`),
  KEY `idx_kickoff_king_stage_date` (`stage_date`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `kickoff_king_standing`;
CREATE TABLE `kickoff_king_standing` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `season_id` bigint NOT NULL,
  `rank_position` int NOT NULL,
  `player_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `association_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `points` int NOT NULL DEFAULT '0',
  `stage_wins` int NOT NULL DEFAULT '0',
  `podium_count` int NOT NULL DEFAULT '0',
  `serve_style` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hot_form` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kickoff_king_rank` (`season_id`,`rank_position`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `leaderboard_share_stat`;
CREATE TABLE `leaderboard_share_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `equipment_id` bigint NOT NULL DEFAULT '0',
  `channel` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `share_count` int NOT NULL DEFAULT '0',
  `last_share_user_id` bigint DEFAULT NULL,
  `last_shared_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_share_stat` (`board_type`,`equipment_id`,`channel`),
  KEY `idx_leaderboard_share_stat_board` (`board_type`,`channel`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `leaderboard_snapshot`;
CREATE TABLE `leaderboard_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `board_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `equipment_id` bigint NOT NULL,
  `rank_position` int NOT NULL,
  `total_score` decimal(10,4) NOT NULL DEFAULT '0.0000',
  `hot_score` decimal(10,4) NOT NULL DEFAULT '0.0000',
  `value_score` decimal(10,4) NOT NULL DEFAULT '0.0000',
  `time_decay` decimal(10,4) NOT NULL DEFAULT '0.0000',
  `favorite_count` int NOT NULL DEFAULT '0',
  `comment_count` int NOT NULL DEFAULT '0',
  `avg_rating` decimal(4,2) NOT NULL DEFAULT '0.00',
  `satisfaction_rate` decimal(6,2) NOT NULL DEFAULT '0.00',
  `latest_interaction_time` datetime DEFAULT NULL,
  `board_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FULL',
  `refreshed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_snapshot_board_equipment` (`board_type`,`equipment_id`),
  KEY `idx_leaderboard_snapshot_rank` (`board_type`,`rank_position`),
  KEY `idx_leaderboard_snapshot_score` (`board_type`,`total_score`),
  KEY `idx_leaderboard_snapshot_time` (`board_type`,`latest_interaction_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2262 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `leaderboard_subscription`;
CREATE TABLE `leaderboard_subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `board_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint NOT NULL DEFAULT '1',
  `last_viewed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_subscription` (`user_id`,`board_type`),
  KEY `idx_leaderboard_subscription_user` (`user_id`,`active`),
  CONSTRAINT `fk_leaderboard_subscription_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `login_audit`;
CREATE TABLE `login_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `success` tinyint NOT NULL DEFAULT '0',
  `ip_address` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_fingerprint` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `risk_level` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOW',
  `warning_message` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_login_audit_username_time` (`username`,`create_time`),
  KEY `idx_login_audit_user_success_time` (`user_id`,`success`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `player_equipment_relation`;
CREATE TABLE `player_equipment_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `player_id` bigint NOT NULL,
  `equipment_id` bigint NOT NULL,
  `type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_current` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_player_equipment` (`player_id`,`equipment_id`,`type`),
  KEY `idx_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `recommend_record`;
CREATE TABLE `recommend_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `recommend_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐类型 PERSONAL/SIMILAR/HOT',
  `recommend_score` decimal(5,4) DEFAULT NULL COMMENT '推荐打分',
  `recommend_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推荐理由',
  `is_clicked` tinyint DEFAULT '0' COMMENT '是否点击',
  `is_purchased` tinyint DEFAULT '0' COMMENT '是否购买',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_recommend_type` (`recommend_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐记录表';
DROP TABLE IF EXISTS `schedule_job_lock`;
CREATE TABLE `schedule_job_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `locked_until` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_job_lock` (`job_key`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `equipment_id` bigint NOT NULL COMMENT '装备 ID',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `selected` tinyint DEFAULT '1' COMMENT '是否选中',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user_equipment` (`user_id`,`equipment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';
DROP TABLE IF EXISTS `star_player`;
CREATE TABLE `star_player` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '球员 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '球员姓名',
  `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家/地区',
  `gender` tinyint DEFAULT '1' COMMENT '性别 1-男 2-女',
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '国际级' COMMENT '水平等级',
  `play_style` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打法风格',
  `ranking` int DEFAULT '0' COMMENT '世界排名',
  `achievements` text COLLATE utf8mb4_unicode_ci COMMENT '主要成就',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `banner_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '横幅图片',
  `intro` text COLLATE utf8mb4_unicode_ci COMMENT '简介',
  `highlights` json DEFAULT NULL COMMENT '高光时刻',
  `featured` tinyint DEFAULT '0' COMMENT '是否推荐',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-下架 1-上架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_star_player_name` (`name`),
  KEY `idx_star_player_ranking` (`ranking`),
  KEY `idx_star_player_featured` (`featured`),
  KEY `idx_star_player_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='明星球员表';
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置项名称',
  `config_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属分类',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '#409EFF' COMMENT '标签颜色',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_name` (`name`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码 (加密)',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像 URL',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `gender` tinyint DEFAULT '0' COMMENT '性别 0-未知 1-男 2-女',
  `level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '初级' COMMENT '水平等级 初级/中级/高级',
  `play_style` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打法风格 快攻/弧圈/削球/全能',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_role` (`role`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
DROP TABLE IF EXISTS `user_preference_profile`;
CREATE TABLE `user_preference_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `level` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `play_style` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `grip_style` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `budget_min` decimal(10,2) DEFAULT NULL,
  `budget_max` decimal(10,2) DEFAULT NULL,
  `weekly_frequency` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `primary_goal` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority_focus` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_categories` text COLLATE utf8mb4_unicode_ci,
  `preferred_brands` text COLLATE utf8mb4_unicode_ci,
  `answer_snapshot` text COLLATE utf8mb4_unicode_ci,
  `preference_tags` text COLLATE utf8mb4_unicode_ci,
  `updated_source` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_preference_profile` (`user_id`),
  KEY `idx_level` (`level`),
  KEY `idx_play_style` (`play_style`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

LOCK TABLES `category` WRITE;
INSERT INTO `category` VALUES (41,'球拍',0,1,NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(42,'胶皮',0,1,NULL,2,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(43,'服装',0,1,NULL,3,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(44,'球鞋',0,1,NULL,4,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(45,'配件',0,1,NULL,5,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(46,'底板',41,2,NULL,1,1,'2026-04-21 15:15:01','2026-04-22 01:43:21',0),(47,'成品拍',41,2,NULL,2,1,'2026-04-21 15:15:01','2026-04-22 01:43:21',0),(48,'反胶',42,2,NULL,1,1,'2026-04-21 15:15:01','2026-04-22 01:43:21',0),(49,'正胶',42,2,NULL,2,1,'2026-04-21 15:15:01','2026-04-22 01:43:21',0),(50,'长胶',42,2,NULL,3,1,'2026-04-21 15:15:01','2026-04-22 01:43:21',0);
UNLOCK TABLES;
LOCK TABLES `tag` WRITE;
INSERT INTO `tag` VALUES (61,'快攻','打法','#FF6B6B',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(62,'弧圈','打法','#4ECDC4',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(63,'削球','打法','#45B7D1',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(64,'全能','打法','#96CEB4',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(65,'入门','水平','#FFEAA7',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(66,'进阶','水平','#DFE6E9',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(67,'专业','水平','#6C5CE7',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(68,'红双喜','品牌','#D63031',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(69,'蝴蝶','品牌','#0984E3',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(70,'银河','品牌','#00B894',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(71,'斯帝卡','品牌','#FDCB6E',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(72,'旋转强','特点','#A29BFE',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(73,'弹性好','特点','#FD79A8',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(74,'控制好','特点','#00CEC9',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(75,'轻量','特点','#E17055',0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0);
UNLOCK TABLES;
LOCK TABLES `equipment` WRITE;
INSERT INTO `equipment` VALUES (133,'红双喜 狂飙龙 5',46,'红双喜','Hurricane Long 5',1580.00,1780.00,2,'马龙同款专业比赛底板，五层纯木加两层芳碳，极致控制与底劲兼备。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'专业弧圈、高级进攻型',NULL,4.95,186,5824,1241,680,NULL,1,1,'专业级',98,'2026-04-21 15:15:01','2026-04-23 21:51:23',0),(134,'蝴蝶 樊振东 ALC',46,'蝴蝶','FZD ALC',2180.00,2480.00,15,'樊振东同款，超级纤维底板，暴力美学之作，蓝芳碳带来速度极限与力量感。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'专业弧圈、力量型进攻',NULL,4.92,204,6911,1581,720,NULL,1,1,'专业级',96,'2026-04-21 15:15:01','2026-04-23 21:38:11',0),(135,'红双喜 狂飙王 王楚钦同款',46,'红双喜','Hurricane Wang',1380.00,1580.00,20,'王楚钦同款底板，新一代利刃，灵动衔接与近台爆发的黄金平衡。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'专业快攻、近台进攻',NULL,4.90,156,4831,960,510,NULL,1,1,'专业级',94,'2026-04-21 15:15:01','2026-04-23 21:38:14',0),(136,'STIGA 奥恰洛夫碳王 OC',46,'斯帝卡','Carbonado',1280.00,1480.00,18,'奥恰洛夫用刀，独特碳素内置结构，提供极佳速度与弧线同时保留控制感。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'专业弧圈、进攻型',NULL,4.82,128,3641,740,420,NULL,1,1,'专业级',88,'2026-04-21 15:15:01','2026-04-23 21:51:38',0),(137,'银河 MC-2 全能五木底板',46,'银河','MC-2',269.00,329.00,96,'经典五木结构，控制细腻，适合初中级爱好者训练与业余比赛使用。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'初中级训练、控制型打法',NULL,4.60,68,1820,320,480,NULL,1,1,'入门级',78,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(138,'亚萨卡 蝶王 D70',46,'亚萨卡','D70',880.00,980.00,30,'亚萨卡旗舰弧圈底板，五木两碳设计，速度与旋转的完美融合。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'弧圈结合快攻、中高级',NULL,4.75,94,2760,530,310,NULL,1,0,'进阶级',82,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(139,'红双喜 6星入门直拍',47,'红双喜','6star',98.00,128.00,200,'适合初学者快速入门的直拍底板套装，附赠双面套胶，开箱即可对打。',NULL,'[\"/api/upload/equipment/equipment_032_6.png\"]','/api/upload/equipment/equipment_032_6.png',NULL,NULL,'初学者、入门练习',NULL,4.30,210,3480,580,1200,NULL,1,0,'入门级',72,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(140,'斯帝卡 Allround Classic 全能',46,'斯帝卡','Allround Classic',450.00,550.00,45,'均衡系全能底板，软木结构手感细腻，削球与控制型玩家的理想选择。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'控制型、削球、全面型',NULL,4.68,112,2181,390,260,NULL,1,0,'进阶级',76,'2026-04-21 15:15:01','2026-04-23 21:51:44',0),(141,'银河 N-10 七层进攻底板',46,'银河','N-10',329.00,369.00,120,'适合快攻结合弧圈的全能型七层底板，出球扎实，手感直接。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'进阶快攻、两面弧圈',NULL,4.70,36,228,42,58,NULL,1,1,'进阶级',70,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(142,'银河 MC-2 五木底板',46,'银河','MC-2',269.00,329.00,96,'经典五木结构，控制细腻，适合初中级爱好者与通勤训练使用。',NULL,'[\"/api/upload/equipment/equipment_027_alc.jpg\"]','/api/upload/equipment/equipment_027_alc.jpg',NULL,NULL,'初中级训练、控制型打法',NULL,4.60,24,176,31,40,NULL,1,1,'入门级',68,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(143,'红双喜 国狂Pro 蓝海绵',48,'红双喜','BlueSponge',480.00,568.00,60,'国家队特供同款，蓝海绵版本，更强旋转与更高平台稳定性，馆球专属。',NULL,'[\"/api/upload/equipment/equipment_004_tenergy-05.png\"]','/api/upload/equipment/equipment_004_tenergy-05.png',NULL,NULL,'专业弧圈、馆球环境',NULL,4.93,248,7200,1680,890,NULL,1,1,'专业级',95,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(144,'蝴蝶 Dignics 09C 陈梦同款',48,'蝴蝶','Dignics 09C',620.00,680.00,40,'陈梦同款，超级张力粘性反胶，旋转与速度融合，东京奥运夺冠装备。',NULL,'[\"/api/upload/equipment/equipment_004_tenergy-05.png\"]','/api/upload/equipment/equipment_004_tenergy-05.png',NULL,NULL,'专业弧圈、力量型',NULL,4.95,312,8940,2100,760,NULL,1,1,'专业级',97,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(145,'蝴蝶 Tenergy 05',48,'蝴蝶','T05',458.00,498.00,50,'高性能反胶套胶，旋转与速度的完美结合。',NULL,'[\"/api/upload/equipment/equipment_004_tenergy-05.png\"]','/api/upload/equipment/equipment_004_tenergy-05.png',NULL,NULL,'专业选手',NULL,4.90,2100,2100,450,1200,NULL,1,1,'专业级',92,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(146,'亚萨卡 天极Neo 反胶',48,'亚萨卡','Neo Energy',238.00,278.00,80,'中德合作款，高弹海绵配合粘性面胶，价格亲民但性能媲美高端产品。',NULL,'[\"/api/upload/equipment/equipment_004_tenergy-05.png\"]','/api/upload/equipment/equipment_004_tenergy-05.png',NULL,NULL,'中级弧圈、进阶爱好者',NULL,4.72,168,3680,720,540,NULL,1,0,'进阶级',80,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(147,'银河 酷摩短颗粒套胶',48,'银河','Pips',98.00,128.00,120,'经典短颗粒正胶，快攻风格专用，击球感扎实，注重速度而非旋转。',NULL,'[\"/api/upload/equipment/equipment_004_tenergy-05.png\"]','/api/upload/equipment/equipment_004_tenergy-05.png',NULL,NULL,'快攻型、直拍推挡',NULL,4.45,86,1640,280,380,NULL,1,0,'入门级',65,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(148,'多尼克 F1长胶套胶',50,'多尼克','F1 Longpips',168.00,198.00,60,'专为削球和防守型选手设计的长胶套胶，变化多端让对手难以判断。',NULL,'[\"/api/upload/equipment/donic-f1-longpips.jpg\"]','/api/upload/equipment/donic-f1-longpips.jpg',NULL,NULL,'削球、防守反攻',NULL,5.00,1,1421,210,180,NULL,1,0,'进阶级',62,'2026-04-21 15:15:01','2026-04-23 21:25:02',0),(149,'友谊729 天翼反胶套胶',48,'友谊729','TianYi',129.00,149.00,150,'控制与性价比并重的训练专用反胶，适合初中级，日常对打无忧。',NULL,'[\"/api/upload/equipment/729-tianyi.jpg\"]','/api/upload/equipment/729-tianyi.jpg',NULL,NULL,'入门训练、控制型',NULL,4.50,142,2840,490,820,NULL,1,0,'入门级',72,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(150,'蝴蝶 LEZOLINE-11 专业球鞋',44,'蝴蝶','LEZOLINE-11',680.00,780.00,35,'专业乒乓球比赛用鞋，轻量化鞋底，全方向防滑纹路，启动与变向灵敏。',NULL,'[\"/api/upload/equipment/butterfly-lezoline11.jpg\"]','/api/upload/equipment/butterfly-lezoline11.jpg',NULL,NULL,'比赛型、专业训练',NULL,4.82,134,2860,580,320,NULL,1,1,'专业级',85,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(151,'李宁 AYZP001 国家队球鞋',44,'李宁','AYZP001',480.00,580.00,48,'国家队队员配备款，超轻外底配合高回弹缓震，纵向爆发力强。',NULL,'[\"/api/upload/equipment/lining-ayzp001.jpg\"]','/api/upload/equipment/lining-ayzp001.jpg',NULL,NULL,'专业赛场、进阶训练',NULL,4.78,102,2240,460,280,NULL,1,0,'专业级',78,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(152,'双鱼 F55 馆内训练球鞋',44,'双鱼','F55',239.00,299.00,88,'抓地稳定、回弹轻快，适合馆内对抗和日常训练，性价比突出。',NULL,'[\"/api/upload/equipment/dhs-f55-shoes.jpg\"]','/api/upload/equipment/dhs-f55-shoes.jpg',NULL,NULL,'日常训练、馆内比赛',NULL,4.55,68,1680,240,310,NULL,1,0,'入门级',65,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(153,'李宁 国家队比赛服套装',43,'李宁','AAYT041',298.00,368.00,80,'国家队同款比赛服（上衣加短裤套装），速干透气，运动剪裁自由度高。',NULL,'[\"/api/upload/equipment/lining-national-jersey.jpg\"]','/api/upload/equipment/lining-national-jersey.jpg',NULL,NULL,'比赛、训练、社团',NULL,4.60,88,1920,360,420,NULL,1,0,'专业级',70,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(154,'红双喜 20只装专业比赛球',45,'红双喜','D40+',88.00,108.00,500,'国际乒联认证赛用球，40+新材料，旋转与速度均衡，适合各类场馆训练。',NULL,'[\"/api/upload/equipment/dhs-d40plus.jpg\"]','/api/upload/equipment/dhs-d40plus.jpg',NULL,NULL,'正式训练、比赛',NULL,4.80,320,4680,860,1980,NULL,1,1,'专业级',88,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(155,'蝴蝶 球拍保护套 专业版',45,'蝴蝶','CS-8',58.00,78.00,200,'防震海绵内衬拍套，保护套胶不变形，适合各尺寸横拍与直拍收纳。',NULL,'[\"/api/upload/equipment/butterfly-cs8-case.jpg\"]','/api/upload/equipment/butterfly-cs8-case.jpg',NULL,NULL,'全类型球拍',NULL,4.65,168,1480,320,680,NULL,1,0,'全能级',60,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(156,'尼塔库 Acoustic 经典五木底板',46,'Nittaku','Acoustic',1280.00,1480.00,16,'尼塔库 Acoustic 是经典五木底板代表作，持球感细腻，弧圈质量高。',NULL,'[\"/api/upload/equipment/nittaku-acoustic.jpg\"]','/api/upload/equipment/nittaku-acoustic.jpg',NULL,NULL,'两面弧圈、中近台控制',NULL,4.91,86,188,34,19,NULL,1,1,'专业级',82,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(157,'蝴蝶 Timo Boll CAF 内置纤维底板',46,'蝴蝶','Timo Boll CAF',1180.00,1380.00,14,'Timo Boll CAF 以内置 CAF 纤维提供更柔和的出球和更高的稳定性。',NULL,'[\"/api/upload/equipment/butterfly-tb-caf.jpg\"]','/api/upload/equipment/butterfly-tb-caf.jpg',NULL,NULL,'进阶快攻弧圈、容错优先',NULL,4.82,64,146,28,15,NULL,1,0,'进阶级',75,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(158,'斯帝卡 Carbonado 190 碳素底板',46,'斯帝卡','Carbonado 190',1680.00,1880.00,12,'Carbonado 190 以硬朗直接的出球和高支撑著称。',NULL,'[\"/api/upload/equipment/stiga-carbonado190.jpg\"]','/api/upload/equipment/stiga-carbonado190.jpg',NULL,NULL,'高强度快攻、主动发力',NULL,4.88,59,133,22,11,NULL,1,0,'专业级',78,'2026-04-21 15:15:01','2026-04-23 21:51:45',0),(159,'蝴蝶 Rozena 高容错反胶',48,'蝴蝶','Rozena',328.00,368.00,48,'Rozena 是经典高容错反胶，脱板清晰、稳定好控。',NULL,'[\"/api/upload/equipment/butterfly-rozena.jpg\"]','/api/upload/equipment/butterfly-rozena.jpg',NULL,NULL,'入门进阶、稳定上手',NULL,4.79,102,214,46,39,NULL,1,1,'进阶级',74,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(160,'骄猛 Vega Europe 反胶套胶',48,'XIOM','Vega Europe',268.00,299.00,52,'Vega Europe 以较柔和海绵和良好容错见长。',NULL,'[\"/api/upload/equipment/xiom-vega-europe.jpg\"]','/api/upload/equipment/xiom-vega-europe.jpg',NULL,NULL,'反手升级、近台快带',NULL,4.76,84,173,31,24,NULL,1,0,'进阶级',70,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(161,'红双喜 狂飙8 反胶套胶',48,'红双喜','狂飙8',198.00,228.00,74,'狂飙8 延续粘性套胶旋转优势，同时提升支撑与中远台续航。',NULL,'[\"/api/upload/equipment/dhs-kuangbiao8.jpg\"]','/api/upload/equipment/dhs-kuangbiao8.jpg',NULL,NULL,'正手弧圈、训练比赛',NULL,4.81,110,242,53,44,NULL,1,1,'进阶级',76,'2026-04-21 15:15:01','2026-04-23 21:49:50',0),(162,'优拉 Dynaryz ACC 张力反胶',48,'JOOLA','Dynaryz ACC',399.00,459.00,36,'Dynaryz ACC 提供高弹高抓的现代张力手感。',NULL,'[\"/api/upload/equipment/joola-dynaryz-acc.jpg\"]','/api/upload/equipment/joola-dynaryz-acc.jpg',NULL,NULL,'专业弧圈、中台发力',NULL,4.86,71,158,27,18,NULL,1,0,'专业级',72,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(163,'美津浓 Wave Drive Neo 3 专业球鞋',44,'Mizuno','Wave Drive Neo 3',899.00,999.00,22,'Wave Drive Neo 3 注重贴地启动、脚感反馈和横向支撑。',NULL,'[\"/api/upload/equipment/mizuno-wave-drive-neo3.jpg\"]','/api/upload/equipment/mizuno-wave-drive-neo3.jpg',NULL,NULL,'比赛强度、高频移动',NULL,4.87,57,119,19,13,NULL,1,1,'专业级',74,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(164,'亚瑟士 ATTACK EXCOUNTER 2 球鞋',44,'ASICS','ATTACK EXCOUNTER 2',599.00,699.00,28,'ATTACK EXCOUNTER 2 兼顾灵活移动与鞋面包裹。',NULL,'[\"/api/upload/equipment/asics-attack-excounter2.jpg\"]','/api/upload/equipment/asics-attack-excounter2.jpg',NULL,NULL,'馆内训练、业余比赛',NULL,4.72,43,98,16,12,NULL,1,0,'进阶级',65,'2026-04-21 15:15:01','2026-04-22 01:44:19',0),(165,'红双喜 方形专业拍包',45,'红双喜','Racket Bag Pro',129.00,159.00,66,'方形专业拍包适合携带底板、套胶、毛巾与随身配件。',NULL,'[\"/api/upload/equipment/dhs-racket-bag.jpg\"]','/api/upload/equipment/dhs-racket-bag.jpg',NULL,NULL,'日常通勤、训练携带',NULL,4.68,38,77,14,20,NULL,1,0,'进阶级',55,'2026-04-21 15:15:01','2026-04-22 01:44:19',0);
UNLOCK TABLES;
LOCK TABLES `equipment_tag` WRITE;
INSERT INTO `equipment_tag` VALUES (509,133,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(510,133,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(511,133,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(512,133,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(513,134,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(514,134,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(515,134,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(516,134,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(517,134,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(518,135,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(519,135,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(520,135,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(521,136,71,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(522,136,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(523,136,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(524,136,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(525,137,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(526,137,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(527,137,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(528,137,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(529,138,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(530,138,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(531,138,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(532,139,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(533,139,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(534,139,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(535,139,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(536,140,71,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(537,140,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(538,140,63,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(539,140,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(540,141,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(541,141,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(542,141,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(543,142,70,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(544,142,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(545,142,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(546,143,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(547,143,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(548,143,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(549,143,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(550,144,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(551,144,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(552,144,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(553,144,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(554,144,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(555,145,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(556,145,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(557,145,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(558,145,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(559,145,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(560,146,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(561,146,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(562,146,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(563,147,70,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(564,147,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(565,147,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(566,148,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(567,148,63,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(568,148,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(569,149,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(570,149,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(571,149,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(572,150,75,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(573,150,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(574,150,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(575,151,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(576,151,75,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(577,151,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(578,152,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(579,152,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(580,152,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(581,153,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(582,153,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(583,154,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(584,154,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(585,155,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(586,155,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(587,156,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(588,156,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(589,156,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(590,157,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(591,157,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(592,157,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(593,157,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(594,158,71,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(595,158,61,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(596,158,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(597,159,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(598,159,69,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(599,159,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(600,159,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(601,160,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(602,160,74,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(603,160,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(604,161,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(605,161,68,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(606,161,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(607,161,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(608,162,72,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(609,162,73,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(610,162,62,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(611,162,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(612,163,75,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(613,163,67,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(614,164,66,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(615,164,75,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(616,164,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(617,165,64,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(618,165,65,'2026-04-21 15:15:01','2026-04-21 15:15:01',0);
UNLOCK TABLES;
LOCK TABLES `equipment_combination` WRITE;
UNLOCK TABLES;
LOCK TABLES `player_equipment_relation` WRITE;
UNLOCK TABLES;
LOCK TABLES `star_player` WRITE;
INSERT INTO `star_player` VALUES (1,'马龙','中国',1,'国际级','全能型',1,'双圈大满贯得主，乒乓球历史上最伟大的球员之一','/assets/champions/ma-long.png','/assets/champions/ma-long-celebrating.jpg','马龙，1988年出生于辽宁鞍山，中国乒乓球队队长，史上首位集奥运会、世锦赛、世界杯、亚运会、亚锦赛、亚洲杯、巡回赛总决赛、全运会单打冠军于一身的超级全满贯男子选手。',NULL,1,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(2,'樊振东','中国',1,'国际级','暴力进攻型',2,'世界杯冠军，世锦赛冠军，奥运会银牌得主','/assets/champions/fan-zhendong.png','/assets/champions/fan-zhendong-wttc2016.jpg','樊振东，1997年出生于广东广州，中国乒乓球队主力成员，以其暴力正手和全面的技术著称，是当今世界乒坛最具统治力的选手之一。',NULL,1,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(3,'王楚钦','中国',1,'国际级','快攻型',3,'世锦赛男双冠军，世界杯男单亚军','/assets/champions/wang-chuqin.png','/assets/champions/wang-chuqin-acttc2016.jpeg','王楚钦，2000年出生于吉林，中国新生代主力，以其凌厉的近台快攻和顽强的意志品质成为国乒未来之星。',NULL,1,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(4,'陈梦','中国',2,'国际级','弧圈结合快攻',1,'奥运会女单冠军，世锦赛女单冠军','/assets/champions/chen-meng.png','/assets/champions/chen-meng-attc2017.jpeg','陈梦，1994年出生于山东青岛，中国女乒主力，以其稳定的相持能力和强大的心理素质著称，是当代女子乒坛的领军人物。',NULL,1,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(5,'孙颖莎','中国',2,'国际级','暴力进攻型',2,'世锦赛女单冠军，世界杯女单冠军','/assets/champions/sun-yingsha.png','/assets/champions/sun-yingsha-medal-2018.jpg','孙颖莎，2000年出生于河北石家庄，以其超强的正手爆冲和灵活的步法著称，是国乒新生代的杰出代表。',NULL,1,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(6,'张继科','中国',1,'国际级','暴力弧圈型',0,'大满贯得主，445天完成大满贯记录','/assets/champions/zhang-jike.png','/assets/champions/zhang-jike-a43baa54.png','张继科，1988年出生于山东青岛，国乒历史上第3位大满贯选手，以其霸王拧和极具观赏性的打法闻名。',NULL,0,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(7,'王曼昱','中国',2,'国际级','男子化打法',4,'世锦赛女单冠军，女团冠军','/assets/champions/wang-manyu.png','/assets/champions/wang-manyu-attc2017.jpeg','王曼昱，1999年出生于黑龙江，以其男子化的打法和强大的正手能力著称，是女乒新生代的顶尖选手。',NULL,0,1,'2026-04-21 14:37:00','2026-04-21 14:37:00',0),(8,'马龙','中国',1,'国际级','全能型',1,'双圈大满贯得主，乒乓球历史上最伟大的球员之一','/assets/champions/ma-long.png','/assets/champions/ma-long-celebrating.jpg','马龙，1988年出生于辽宁鞍山，中国乒乓球队队长，史上首位集奥运会、世锦赛、世界杯、亚运会、亚锦赛、亚洲杯、巡回赛总决赛、全运会单打冠军于一身的超级全满贯男子选手。',NULL,1,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(9,'樊振东','中国',1,'国际级','暴力进攻型',2,'世界杯冠军，世锦赛冠军，奥运会银牌得主','/assets/champions/fan-zhendong.png','/assets/champions/fan-zhendong-wttc2016.jpg','樊振东，1997年出生于广东广州，中国乒乓球队主力成员，以其暴力正手和全面的技术著称，是当今世界乒坛最具统治力的选手之一。',NULL,1,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(10,'王楚钦','中国',1,'国际级','快攻型',3,'世锦赛男双冠军，世界杯男单亚军','/assets/champions/wang-chuqin.png','/assets/champions/wang-chuqin-acttc2016.jpeg','王楚钦，2000年出生于吉林，中国新生代主力，以其凌厉的近台快攻和顽强的意志品质成为国乒未来之星。',NULL,1,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(11,'陈梦','中国',2,'国际级','弧圈结合快攻',1,'奥运会女单冠军，世锦赛女单冠军','/assets/champions/chen-meng.png','/assets/champions/chen-meng-attc2017.jpeg','陈梦，1994年出生于山东青岛，中国女乒主力，以其稳定的相持能力和强大的心理素质著称，是当代女子乒坛的领军人物。',NULL,1,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(12,'孙颖莎','中国',2,'国际级','暴力进攻型',2,'世锦赛女单冠军，世界杯女单冠军','/assets/champions/sun-yingsha.png','/assets/champions/sun-yingsha-medal-2018.jpg','孙颖莎，2000年出生于河北石家庄，以其超强的正手爆冲和灵活的步法著称，是国乒新生代的杰出代表。',NULL,1,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(13,'张继科','中国',1,'国际级','暴力弧圈型',0,'大满贯得主，445天完成大满贯记录','/assets/champions/zhang-jike.png','/assets/champions/zhang-jike-a43baa54.png','张继科，1988年出生于山东青岛，国乒历史上第3位大满贯选手，以其霸王拧和极具观赏性的打法闻名。',NULL,0,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(14,'王曼昱','中国',2,'国际级','男子化打法',4,'世锦赛女单冠军，女团冠军','/assets/champions/wang-manyu.png','/assets/champions/wang-manyu-attc2017.jpeg','王曼昱，1999年出生于黑龙江，以其男子化的打法和强大的正手能力著称，是女乒新生代的顶尖选手。',NULL,0,1,'2026-04-21 14:37:24','2026-04-21 14:37:24',0),(15,'马龙','中国',1,'国际级','全能型',1,'双圈大满贯得主，乒乓球历史上最伟大的球员之一','/assets/champions/ma-long.png','/assets/champions/ma-long-celebrating.jpg','马龙，1988年出生于辽宁鞍山，中国乒乓球队队长，史上首位集奥运会、世锦赛、世界杯、亚运会、亚锦赛、亚洲杯、巡回赛总决赛、全运会单打冠军于一身的超级全满贯男子选手。',NULL,1,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(16,'樊振东','中国',1,'国际级','暴力进攻型',2,'世界杯冠军，世锦赛冠军，奥运会银牌得主','/assets/champions/fan-zhendong.png','/assets/champions/fan-zhendong-wttc2016.jpg','樊振东，1997年出生于广东广州，中国乒乓球队主力成员，以其暴力正手和全面的技术著称，是当今世界乒坛最具统治力的选手之一。',NULL,1,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(17,'王楚钦','中国',1,'国际级','快攻型',3,'世锦赛男双冠军，世界杯男单亚军','/assets/champions/wang-chuqin.png','/assets/champions/wang-chuqin-acttc2016.jpeg','王楚钦，2000年出生于吉林，中国新生代主力，以其凌厉的近台快攻和顽强的意志品质成为国乒未来之星。',NULL,1,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(18,'陈梦','中国',2,'国际级','弧圈结合快攻',1,'奥运会女单冠军，世锦赛女单冠军','/assets/champions/chen-meng.png','/assets/champions/chen-meng-attc2017.jpeg','陈梦，1994年出生于山东青岛，中国女乒主力，以其稳定的相持能力和强大的心理素质著称，是当代女子乒坛的领军人物。',NULL,1,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(19,'孙颖莎','中国',2,'国际级','暴力进攻型',2,'世锦赛女单冠军，世界杯女单冠军','/assets/champions/sun-yingsha.png','/assets/champions/sun-yingsha-medal-2018.jpg','孙颖莎，2000年出生于河北石家庄，以其超强的正手爆冲和灵活的步法著称，是国乒新生代的杰出代表。',NULL,1,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(20,'张继科','中国',1,'国际级','暴力弧圈型',0,'大满贯得主，445天完成大满贯记录','/assets/champions/zhang-jike.png','/assets/champions/zhang-jike-a43baa54.png','张继科，1988年出生于山东青岛，国乒历史上第3位大满贯选手，以其霸王拧和极具观赏性的打法闻名。',NULL,0,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(21,'王曼昱','中国',2,'国际级','男子化打法',4,'世锦赛女单冠军，女团冠军','/assets/champions/wang-manyu.png','/assets/champions/wang-manyu-attc2017.jpeg','王曼昱，1999年出生于黑龙江，以其男子化的打法和强大的正手能力著称，是女乒新生代的顶尖选手。',NULL,0,1,'2026-04-21 14:44:22','2026-04-21 14:44:22',0),(22,'马龙','中国',1,'国际级','全能型',1,'双圈大满贯得主，乒乓球历史上最伟大的球员之一','/assets/champions/ma-long.png','/assets/champions/ma-long-celebrating.jpg','马龙，1988年出生于辽宁鞍山，中国乒乓球队队长，史上首位集奥运会、世锦赛、世界杯、亚运会、亚锦赛、亚洲杯、巡回赛总决赛、全运会单打冠军于一身的超级全满贯男子选手。',NULL,1,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(23,'樊振东','中国',1,'国际级','暴力进攻型',2,'世界杯冠军，世锦赛冠军，奥运会银牌得主','/assets/champions/fan-zhendong.png','/assets/champions/fan-zhendong-wttc2016.jpg','樊振东，1997年出生于广东广州，中国乒乓球队主力成员，以其暴力正手和全面的技术著称，是当今世界乒坛最具统治力的选手之一。',NULL,1,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(24,'王楚钦','中国',1,'国际级','快攻型',3,'世锦赛男双冠军，世界杯男单亚军','/assets/champions/wang-chuqin.png','/assets/champions/wang-chuqin-acttc2016.jpeg','王楚钦，2000年出生于吉林，中国新生代主力，以其凌厉的近台快攻和顽强的意志品质成为国乒未来之星。',NULL,1,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(25,'陈梦','中国',2,'国际级','弧圈结合快攻',1,'奥运会女单冠军，世锦赛女单冠军','/assets/champions/chen-meng.png','/assets/champions/chen-meng-attc2017.jpeg','陈梦，1994年出生于山东青岛，中国女乒主力，以其稳定的相持能力和强大的心理素质著称，是当代女子乒坛的领军人物。',NULL,1,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(26,'孙颖莎','中国',2,'国际级','暴力进攻型',2,'世锦赛女单冠军，世界杯女单冠军','/assets/champions/sun-yingsha.png','/assets/champions/sun-yingsha-medal-2018.jpg','孙颖莎，2000年出生于河北石家庄，以其超强的正手爆冲和灵活的步法著称，是国乒新生代的杰出代表。',NULL,1,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(27,'张继科','中国',1,'国际级','暴力弧圈型',0,'大满贯得主，445天完成大满贯记录','/assets/champions/zhang-jike.png','/assets/champions/zhang-jike-a43baa54.png','张继科，1988年出生于山东青岛，国乒历史上第3位大满贯选手，以其霸王拧和极具观赏性的打法闻名。',NULL,0,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(28,'王曼昱','中国',2,'国际级','男子化打法',4,'世锦赛女单冠军，女团冠军','/assets/champions/wang-manyu.png','/assets/champions/wang-manyu-attc2017.jpeg','王曼昱，1999年出生于黑龙江，以其男子化的打法和强大的正手能力著称，是女乒新生代的顶尖选手。',NULL,0,1,'2026-04-21 14:44:52','2026-04-21 14:44:52',0),(29,'马龙','中国',1,'国际级','全能型',1,'双圈大满贯得主，乒乓球历史上最伟大的球员之一','/assets/champions/ma-long.png','/assets/champions/ma-long-celebrating.jpg','马龙，1988年出生于辽宁鞍山，中国乒乓球队队长，史上首位集奥运会、世锦赛、世界杯、亚运会、亚锦赛、亚洲杯、巡回赛总决赛、全运会单打冠军于一身的超级全满贯男子选手。',NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(30,'樊振东','中国',1,'国际级','暴力进攻型',2,'世界杯冠军，世锦赛冠军，奥运会银牌得主','/assets/champions/fan-zhendong.png','/assets/champions/fan-zhendong-wttc2016.jpg','樊振东，1997年出生于广东广州，中国乒乓球队主力成员，以其暴力正手和全面的技术著称，是当今世界乒坛最具统治力的选手之一。',NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(31,'王楚钦','中国',1,'国际级','快攻型',3,'世锦赛男双冠军，世界杯男单亚军','/assets/champions/wang-chuqin.png','/assets/champions/wang-chuqin-acttc2016.jpeg','王楚钦，2000年出生于吉林，中国新生代主力，以其凌厉的近台快攻和顽强的意志品质成为国乒未来之星。',NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(32,'陈梦','中国',2,'国际级','弧圈结合快攻',1,'奥运会女单冠军，世锦赛女单冠军','/assets/champions/chen-meng.png','/assets/champions/chen-meng-attc2017.jpeg','陈梦，1994年出生于山东青岛，中国女乒主力，以其稳定的相持能力和强大的心理素质著称，是当代女子乒坛的领军人物。',NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(33,'孙颖莎','中国',2,'国际级','暴力进攻型',2,'世锦赛女单冠军，世界杯女单冠军','/assets/champions/sun-yingsha.png','/assets/champions/sun-yingsha-medal-2018.jpg','孙颖莎，2000年出生于河北石家庄，以其超强的正手爆冲和灵活的步法著称，是国乒新生代的杰出代表。',NULL,1,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(34,'张继科','中国',1,'国际级','暴力弧圈型',0,'大满贯得主，445天完成大满贯记录','/assets/champions/zhang-jike.png','/assets/champions/zhang-jike-a43baa54.png','张继科，1988年出生于山东青岛，国乒历史上第3位大满贯选手，以其霸王拧和极具观赏性的打法闻名。',NULL,0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(35,'王曼昱','中国',2,'国际级','男子化打法',4,'世锦赛女单冠军，女团冠军','/assets/champions/wang-manyu.png','/assets/champions/wang-manyu-attc2017.jpeg','王曼昱，1999年出生于黑龙江，以其男子化的打法和强大的正手能力著称，是女乒新生代的顶尖选手。',NULL,0,1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0);
UNLOCK TABLES;
LOCK TABLES `system_config` WRITE;
INSERT INTO `system_config` VALUES (29,'recommend.hot.count','10','热门推荐数量',NULL,'热门推荐数量',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(30,'recommend.personal.count','20','个性化推荐数量',NULL,'个性化推荐数量',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(31,'recommend.similar.count','5','相似推荐数量',NULL,'相似推荐数量',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(32,'cache.homepage.expire','3600','首页缓存时间',NULL,'首页缓存时间(秒)',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(33,'cache.equipment.expire','1800','装备详情缓存时间',NULL,'装备详情缓存时间(秒)',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(34,'upload.max.size','5242880','上传文件大小限制',NULL,'上传文件大小限制(字节)',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0),(35,'jwt.expire.time','86400','JWT过期时间',NULL,'JWT过期时间(秒)',1,'2026-04-21 15:15:01','2026-04-21 15:15:01',0);
UNLOCK TABLES;
SET FOREIGN_KEY_CHECKS = 1;
