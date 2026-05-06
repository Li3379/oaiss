package com.oaiss.chain.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityStartupValidator 单元测试
 * Tests for C2/C3: Secret validation on startup
 */
class SecurityStartupValidatorTest {

    private Environment environment;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
    }

    @Test
    @DisplayName("生产环境+弱JWT密钥应抛出SecurityException")
    void validateOnStartup_productionWithWeakJwtSecret_shouldThrow() {
        // Given: production profile with weak JWT secret
        when(environment.getActiveProfiles()).thenReturn(new String[]{"docker"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret",
                "oaiss-chain-dev-jwt-secret-key-must-be-at-least-256-bits-long");
        setField(validator, "dbPassword", "strongDbPassword123!");

        // When & Then
        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+弱数据库密码应抛出SecurityException")
    void validateOnStartup_productionWithWeakDbPassword_shouldThrow() {
        // Given: production profile with weak DB password
        when(environment.getActiveProfiles()).thenReturn(new String[]{"docker"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret", "a-very-strong-and-unique-jwt-secret-key-that-is-at-least-256-bits");
        setField(validator, "dbPassword", "123456");

        // When & Then
        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+强密钥应通过校验")
    void validateOnStartup_productionWithStrongSecrets_shouldPass() {
        // Given: production profile with strong secrets
        when(environment.getActiveProfiles()).thenReturn(new String[]{"docker"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret", "a-very-strong-and-unique-jwt-secret-key-that-is-at-least-256-bits");
        setField(validator, "dbPassword", "strongDbPassword123!");

        // When & Then - should not throw
        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("开发环境+弱JWT密钥应仅警告不阻止启动")
    void validateOnStartup_devWithWeakJwtSecret_shouldOnlyWarn() {
        // Given: dev profile with weak JWT secret
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret",
                "dev-only-jwt-secret-key-must-be-at-least-256-bits-long-for-hmac-sha");
        setField(validator, "dbPassword", "123456");

        // When & Then - should NOT throw, just warn
        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("无profile+弱密钥应仅警告不阻止启动")
    void validateOnStartup_noProfileWithWeakSecrets_shouldOnlyWarn() {
        // Given: no active profile with weak secrets
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret",
                "oaiss-chain-dev-jwt-secret-key-must-be-at-least-256-bits-long");
        setField(validator, "dbPassword", "123456");

        // When & Then - should NOT throw
        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+弱密码password应抛出SecurityException")
    void validateOnStartup_productionWithPasswordAsDbPassword_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret", "a-very-strong-and-unique-jwt-secret-key-that-is-at-least-256-bits");
        setField(validator, "dbPassword", "password");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    private void setField(Object target, String fieldName, String value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
