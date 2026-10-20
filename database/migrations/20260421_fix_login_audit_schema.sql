-- 修复 login_audit 表缺失的字段
-- 注意:如果字段已存在会报错,可忽略
ALTER TABLE `login_audit` 
ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;

-- MySQL 8.0.13+ 支持 IF NOT EXISTS,旧版本需要手动检查
-- 如果上面语句失败,使用以下语句:
-- ALTER TABLE `login_audit` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;
