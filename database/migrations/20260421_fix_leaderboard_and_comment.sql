-- =====================================================
-- 修复 leaderboard_snapshot 和 comment 表结构
-- 执行时间: 2026-04-21
-- =====================================================

USE pingpong_recommendation;

-- 1. 重建 leaderboard_snapshot 表
DROP TABLE IF EXISTS `leaderboard_snapshot`;

CREATE TABLE `leaderboard_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `board_type` VARCHAR(50) NOT NULL,
  `board_name` VARCHAR(100) NOT NULL,
  `equipment_id` BIGINT NOT NULL,
  `rank_position` INT NOT NULL,
  `total_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `hot_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `value_score` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `time_decay` DECIMAL(10,4) NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `avg_rating` DECIMAL(4,2) NOT NULL DEFAULT 0,
  `satisfaction_rate` DECIMAL(6,2) NOT NULL DEFAULT 0,
  `latest_interaction_time` DATETIME NULL,
  `board_reason` VARCHAR(255) NULL,
  `refresh_mode` VARCHAR(20) NOT NULL DEFAULT 'FULL',
  `refreshed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leaderboard_snapshot_board_equipment` (`board_type`, `equipment_id`),
  KEY `idx_leaderboard_snapshot_rank` (`board_type`, `rank_position`),
  KEY `idx_leaderboard_snapshot_score` (`board_type`, `total_score`),
  KEY `idx_leaderboard_snapshot_time` (`board_type`, `latest_interaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 为 comment 表添加审核相关字段
ALTER TABLE `comment` 
ADD COLUMN `review_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态（PENDING/APPROVED/REJECTED）' AFTER `status`,
ADD COLUMN `reviewed_by` BIGINT NULL COMMENT '审核人ID' AFTER `review_status`,
ADD COLUMN `reviewed_at` DATETIME NULL COMMENT '审核时间' AFTER `reviewed_by`,
ADD COLUMN `review_remark` VARCHAR(255) NULL COMMENT '审核备注' AFTER `reviewed_at`;

-- 验证修改
SELECT '=== 表结构修复完成 ===' AS status;
DESCRIBE leaderboard_snapshot;
DESCRIBE comment;
