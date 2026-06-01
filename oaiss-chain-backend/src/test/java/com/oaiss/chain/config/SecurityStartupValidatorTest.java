package com.oaiss.chain.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityStartupValidator 单元测试
 * Tests for C2/C3: Secret validation on startup
 */
class SecurityStartupValidatorTest {

    private static final String STRONG_JWT_SECRET =
            "a-very-strong-and-unique-jwt-secret-key-that-is-at-least-256-bits";
    private static final String VALID_RSA_KEK =
            Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());

    private Environment environment;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
    }

    @Test
    @DisplayName("生产环境+弱JWT密钥应抛出SecurityException")
    void validateOnStartup_productionWithWeakJwtSecret_shouldThrow() {
        // Given: production profile with weak JWT secret
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret",
                "oaiss-chain-dev-jwt-secret-key-must-be-at-least-256-bits-long");
        setField(validator, "dbPassword", "strongDbPassword123!");
        setField(validator, "redisPassword", "strongRedisPassword123!");
        setField(validator, "minioAccessKey", "strong-access-key");
        setField(validator, "minioSecretKey", "strong-secret-key");
        setField(validator, "rsaKek", VALID_RSA_KEK);
        setField(validator, "allowedOrigins", "https://app.example.com");
        setField(validator, "fabricCaEnabled", false);
        setField(validator, "requireOpsSecrets", false);

        // When & Then
        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+弱数据库密码应抛出SecurityException")
    void validateOnStartup_productionWithWeakDbPassword_shouldThrow() {
        // Given: production profile with weak DB password
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "dbPassword", "123456");

        // When & Then
        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+强密钥应通过校验")
    void validateOnStartup_productionWithStrongSecrets_shouldPass() {
        // Given: production profile with strong secrets
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod", "fabric"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "fabricEnabled", true);

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
        setField(validator, "redisPassword", "");
        setField(validator, "minioAccessKey", "minioadmin");
        setField(validator, "minioSecretKey", "minioadmin");
        setField(validator, "rsaKek", "not-base64");
        setField(validator, "allowedOrigins", "http://localhost:5173");

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
        setField(validator, "redisPassword", "");
        setField(validator, "minioAccessKey", "minioadmin");
        setField(validator, "minioSecretKey", "minioadmin");
        setField(validator, "rsaKek", "not-base64");
        setField(validator, "allowedOrigins", "http://localhost:5173");

        // When & Then - should NOT throw
        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+弱密码password应抛出SecurityException")
    void validateOnStartup_productionWithPasswordAsDbPassword_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "dbPassword", "password");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+弱MinIO凭证应抛出SecurityException")
    void validateOnStartup_productionWithWeakMinioCredentials_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "minioAccessKey", "minioadmin");
        setField(validator, "minioSecretKey", "minioadmin");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+模板占位符凭证应抛出SecurityException")
    void validateOnStartup_productionWithPlaceholderCredentials_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "dbPassword", "replace_with_strong_database_password");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("开发环境+弱MinIO凭证应仅警告不阻止启动")
    void validateOnStartup_devWithWeakMinioCredentials_shouldOnlyWarn() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setField(validator, "jwtSecret", STRONG_JWT_SECRET);
        setField(validator, "dbPassword", "strongDbPassword123!");
        setField(validator, "redisPassword", "strongRedisPassword123!");
        setField(validator, "minioAccessKey", "minioadmin");
        setField(validator, "minioSecretKey", "minioadmin");
        setField(validator, "rsaKek", VALID_RSA_KEK);
        setField(validator, "allowedOrigins", "http://localhost:5173");

        assertDoesNotThrow(validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+空JWT密钥应抛出SecurityException")
    void validateOnStartup_productionWithBlankJwtSecret_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "jwtSecret", "");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+短JWT密钥应抛出SecurityException")
    void validateOnStartup_productionWithShortJwtSecret_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "jwtSecret", "short-secret");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+空Redis密码应抛出SecurityException")
    void validateOnStartup_productionWithBlankRedisPassword_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "redisPassword", "");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+无效RSA_KEK应抛出SecurityException")
    void validateOnStartup_productionWithInvalidRsaKek_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "rsaKek", "not-base64");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+长度错误RSA_KEK应抛出SecurityException")
    void validateOnStartup_productionWithWrongLengthRsaKek_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "rsaKek", Base64.getEncoder().encodeToString("too-short".getBytes()));

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+CORS包含localhost应抛出SecurityException")
    void validateOnStartup_productionWithLocalhostCors_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "allowedOrigins", "https://app.example.com,http://localhost:5173");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+Fabric CA使用HTTP应抛出SecurityException")
    void validateOnStartup_productionWithHttpFabricCa_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod", "fabric"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "fabricEnabled", true);
        setField(validator, "fabricCaEnabled", true);
        setField(validator, "fabricCaEndpoint", "http://ca.org1.example.com:7054");
        setField(validator, "fabricCaAdminPassword", "strongFabricPassword123!");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+要求运维密钥但Grafana密码为空应抛出SecurityException")
    void validateOnStartup_productionWithRequiredOpsSecretsBlankGrafanaPassword_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "requireOpsSecrets", true);
        setField(validator, "grafanaAdminPassword", "");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+ML_SERVICE_SECRET为空应抛出SecurityException")
    void validateOnStartup_productionWithBlankMlServiceSecret_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "mlServiceSecret", "");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境+ML_SERVICE_SECRET为占位值应抛出SecurityException")
    void validateOnStartup_productionWithPlaceholderMlServiceSecret_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "mlServiceSecret", "replace_with_internal_ml_service_secret");

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("生产环境缺少 fabric profile 应阻止启动")
    void validateOnStartup_productionWithoutFabricProfile_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("staging 环境缺少 fabric profile 应阻止启动")
    void validateOnStartup_stagingWithoutFabricProfile_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("FABRIC_ENABLED 为 true 但未启用 fabric profile 应阻止启动")
    void validateOnStartup_fabricEnabledWithoutFabricProfile_shouldThrow() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "fabricEnabled", true);

        assertThrows(SecurityException.class, validator::validateOnStartup);
    }

    @Test
    @DisplayName("staging + fabric + 强密钥应通过校验")
    void validateOnStartup_stagingWithFabricProfile_shouldPass() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging", "fabric"});
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        setStrongProductionFields(validator);
        setField(validator, "fabricEnabled", true);

        assertDoesNotThrow(validator::validateOnStartup);
    }

    private void setStrongProductionFields(SecurityStartupValidator validator) {
        setField(validator, "jwtSecret", STRONG_JWT_SECRET);
        setField(validator, "dbPassword", "strongDbPassword123!");
        setField(validator, "redisPassword", "strongRedisPassword123!");
        setField(validator, "minioAccessKey", "strong-access-key");
        setField(validator, "minioSecretKey", "strong-secret-key");
        setField(validator, "rsaKek", VALID_RSA_KEK);
        setField(validator, "allowedOrigins", "https://app.example.com");
        setField(validator, "fabricEnabled", false);
        setField(validator, "fabricCaEnabled", false);
        setField(validator, "fabricCaEndpoint", "");
        setField(validator, "fabricCaAdminPassword", "");
        setField(validator, "mlServiceSecret", "strong-ml-service-secret");
        setField(validator, "requireOpsSecrets", false);
        setField(validator, "grafanaAdminPassword", "");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
