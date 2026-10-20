-- =====================================================
-- 乒乓球装备推荐系统 - 数据库增量迁移脚本
-- 用于在保留现有数据的情况下升级表结构
-- =====================================================

USE pingpong_recommendation;

-- 1. 为现有表添加缺失字段（如果已存在则会跳过，建议逐条执行）

-- 用户表
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `user` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 装备分类表
ALTER TABLE `category` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `category` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 标签表
ALTER TABLE `tag` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `tag` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 装备表
ALTER TABLE `equipment` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `equipment` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 器材标签关联表
ALTER TABLE `equipment_tag` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';

-- 收藏表
ALTER TABLE `favorite` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `favorite` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `favorite` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 评论表
ALTER TABLE `comment` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `comment` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `comment` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 评论点赞表
ALTER TABLE `comment_like` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `comment_like` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `comment_like` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 浏览历史表
ALTER TABLE `browse_history` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `browse_history` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `browse_history` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 行为日志表
ALTER TABLE `behavior_log` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `behavior_log` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `behavior_log` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 定时任务锁表
ALTER TABLE `schedule_job_lock` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `schedule_job_lock` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 登录审计表
ALTER TABLE `login_audit` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `login_audit` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `login_audit` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 系统配置表
ALTER TABLE `system_config` ADD COLUMN IF NOT EXISTS `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）';
ALTER TABLE `system_config` ADD INDEX IF NOT EXISTS `idx_deleted` (`deleted`);

-- 2. 创建新表（如果不存在）

-- 购物车表
CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `equipment_id` BIGINT NOT NULL COMMENT '器材ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- 推荐记录表
CREATE TABLE IF NOT EXISTS `recommend_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT COMMENT '用户ID（空表示游客）',
  `equipment_id` BIGINT NOT NULL COMMENT '推荐器材ID',
  `score` DECIMAL(10,4) COMMENT '推荐得分',
  `recommend_type` VARCHAR(50) COMMENT '推荐类型：HOT, PERSONAL, SIMILAR',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删，1=已删）',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_create_time` (`create_time`),
  INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐记录表';
