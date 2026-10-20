package com.pingpong.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 生成 BCrypt 密码哈希，用于初始化管理员账号或重置密码。
 *
 * 用法：java com.pingpong.util.PasswordHashGenerator &lt;明文密码&gt;
 * 将输出的哈希值填入 database/scripts/ 下的脚本。
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        if (args.length == 0 || args[0].trim().isEmpty()) {
            System.out.println("用法: PasswordHashGenerator <明文密码>");
            return;
        }

        String hashed = BCrypt.hashpw(args[0]);
        System.out.println("BCrypt hash: " + hashed);
        System.out.println("Verify     : " + BCrypt.checkpw(args[0], hashed));
    }
}
