package com.pingpong.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class CredentialPolicyUtil {

    public static final String USERNAME_RULE_TEXT = "用户名需以字母开头，长度 6 到 20 位，仅允许字母、数字和下划线";
    public static final String PASSWORD_RULE_TEXT = "密码需为 10 到 32 位，且包含大小写字母、数字和特殊字符";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{5,19}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{10,32}$");

    private CredentialPolicyUtil() {
    }

    public static void validateUsername(String username) {
        if (!StringUtils.hasText(username) || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new RuntimeException(USERNAME_RULE_TEXT);
        }
    }

    public static void validatePassword(String password) {
        if (!StringUtils.hasText(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException(PASSWORD_RULE_TEXT);
        }
    }
}
