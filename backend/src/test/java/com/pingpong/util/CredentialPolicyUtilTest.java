package com.pingpong.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialPolicyUtilTest {

    @Test
    void validateUsernameShouldAcceptSecureUniqueFormat() {
        assertDoesNotThrow(() -> CredentialPolicyUtil.validateUsername("PaddleAce_26"));
    }

    @Test
    void validateUsernameShouldRejectUnsafeFormat() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> CredentialPolicyUtil.validateUsername("26ace")
        );

        assertEquals(CredentialPolicyUtil.USERNAME_RULE_TEXT, exception.getMessage());
    }

    @Test
    void validatePasswordShouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> CredentialPolicyUtil.validatePassword("PingPong@2026!A"));
    }

    @Test
    void validatePasswordShouldRejectWeakPassword() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> CredentialPolicyUtil.validatePassword("weakpass")
        );

        assertEquals(CredentialPolicyUtil.PASSWORD_RULE_TEXT, exception.getMessage());
    }
}
