USE pingpong_recommendation;

-- Create admin user（将 <BCRYPT_HASH> 替换为你自行生成的哈希）
INSERT INTO user (username, password, email, nickname, role, status, level, create_time, update_time, deleted) 
VALUES (
    'quickadmin2026', 
    '<BCRYPT_HASH>', 
    'quickadmin@test.com', 
    'Quick Admin', 
    'ADMIN', 
    1,
    'Advanced',
    NOW(),
    NOW(),
    0
);

SELECT id, username, nickname, email, role FROM user WHERE username='quickadmin2026';
