package com.oaiss.chain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 安全启动校验器
 * 检测已知的弱密钥和默认密码，在生产环境下阻止启动
 *
 * @author OAISS Team
 */
@Slf4j
@Component
public class SecurityStartupValidator {

    private static final Set<String> WEAK_JWT_SECRETS = Set.of(
            "oaiss-chain-dev-jwt-secret-key-must-be-at-least-256-bits-long",
            "dev-only-jwt-secret-key-must-be-at-least-256-bits-long-for-hmac-sha",
            "oaiss-chain-jwt-secret-key-must-be-at-least-256-bits-long"
    );

    private static final Set<String> WEAK_DB_PASSWORDS = Set.of(
            "123456", "password", "root", "admin", "test"
    );

    private static final Set<String> WEAK_MINIO_CREDENTIALS = Set.of(
            "minioadmin", "minio", "admin", "accesskey", "secretkey"
    );

    private static final Set<String> PRODUCTION_PROFILES = Set.of(
            "docker", "prod", "production"
    );

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${minio.access-key:}")
    private String minioAccessKey;

    @Value("${minio.secret-key:}")
    private String minioSecretKey;

    private final Environment environment;

    public SecurityStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void validateOnStartup() {
        boolean isProduction = isProductionProfile();

        validateJwtSecret(isProduction);
        validateDbPassword(isProduction);
        validateMinioCredentials(isProduction);

        if (isProduction) {
            log.info("Security startup validation PASSED for production environment");
        }
    }

    private void validateJwtSecret(boolean isProduction) {
        if (WEAK_JWT_SECRETS.contains(jwtSecret)) {
            String message = "SECURITY WARNING: JWT secret is using a known default value. "
                    + "Set JWT_SECRET environment variable to a strong random key (at least 256 bits).";
            if (isProduction) {
                throw new SecurityException("FATAL: " + message);
            }
            log.warn(message);
        }
    }

    private void validateDbPassword(boolean isProduction) {
        if (WEAK_DB_PASSWORDS.contains(dbPassword)) {
            String message = "SECURITY WARNING: Database password is using a weak default value. "
                    + "Set DB_PASSWORD environment variable to a strong password.";
            if (isProduction) {
                throw new SecurityException("FATAL: " + message);
            }
            log.warn(message);
        }
    }

    private void validateMinioCredentials(boolean isProduction) {
        if (WEAK_MINIO_CREDENTIALS.contains(minioAccessKey) || WEAK_MINIO_CREDENTIALS.contains(minioSecretKey)) {
            String message = "SECURITY WARNING: MinIO credentials are using weak default values. "
                    + "Set MINIO_ACCESS_KEY and MINIO_SECRET_KEY environment variables to strong values.";
            if (isProduction) {
                throw new SecurityException("FATAL: " + message);
            }
            log.warn(message);
        }
    }

    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(PRODUCTION_PROFILES::contains);
    }
}
