-- =====================================================
-- Create Admin User Account
-- Username: pingpong_admin
-- Password: 自行设定，并替换下方 <BCRYPT_HASH> 占位符
--
-- 生成 BCrypt 哈希：
--   Java   : java com.pingpong.util.PasswordHashGenerator <明文密码>
--   Python : python -c "import bcrypt;print(bcrypt.hashpw(b'YourPassword', bcrypt.gensalt()).decode())"
-- =====================================================

USE pingpong_recommendation;

-- Delete existing account if exists
DELETE FROM user WHERE username = 'pingpong_admin';

-- Insert new admin account（将 <BCRYPT_HASH> 替换为上一步生成的哈希）
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
    'pingpong.admin@system.com',
    'PingPong Admin',
    'ADMIN',
    1,
    'Advanced',
    NOW(),
    NOW(),
    0
);

-- Verify the created account
SELECT 
    id,
    username,
    nickname,
    email,
    role,
    status,
    create_time
FROM user 
WHERE username = 'pingpong_admin';
