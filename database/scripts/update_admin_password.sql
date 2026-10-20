-- 更新指定账号的密码（将 <BCRYPT_HASH> 替换为你自行生成的 BCrypt 哈希值）
-- 生成方式：python -c "import bcrypt;print(bcrypt.hashpw(b'YourPassword', bcrypt.gensalt()).decode())"
UPDATE `user` 
SET `password` = '<BCRYPT_HASH>' 
WHERE `username` = 'admin';

-- 验证更新
SELECT `username`, `password`, `email`, `role`, `status` FROM `user` WHERE `username` = 'admin';
