-- =====================================================
-- 创建新的管理员账户
-- 用户名: pingpong_admin
-- 密码  : 自行设定，并替换下方 <BCRYPT_HASH> 占位符
--         Python: python -c "import bcrypt;print(bcrypt.hashpw(b'YourPassword', bcrypt.gensalt()).decode())"
-- =====================================================

USE pingpong_recommendation;

-- 1. 检查是否已存在该用户名的账户，如果存在则删除
DELETE FROM user WHERE username = 'pingpong_admin';

-- 2. 插入新的管理员账户（将 <BCRYPT_HASH> 替换为你生成的哈希）
INSERT INTO user (
    username, 
    password, 
    email, 
    nickname, 
    role, 
    status, 
    level,
    create_time, 
    update_time,
    deleted
) VALUES (
    'pingpong_admin',
    '<BCRYPT_HASH>',
    'admin_2026@pingpong.com',
    '乒乓球管理员',
    'ADMIN',
    1,
    '高级',
    NOW(),
    NOW(),
    0
);

-- 3. 验证创建结果
SELECT 
    id,
    username,
    nickname,
    email,
    role,
    status,
    level,
    create_time
FROM user 
WHERE username = 'pingpong_admin';
