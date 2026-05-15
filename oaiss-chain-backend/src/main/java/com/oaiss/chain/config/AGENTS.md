<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.config

Spring configuration classes.

## Key Files (13 files)

- `SecurityConfig.java` — Spring Security config (JWT filter chain, CORS, CSRF)
- `RedisConfig.java` — Redis connection and serialization
- `MinioConfig.java` — MinIO client configuration
- `SwaggerConfig.java` — SpringDoc OpenAPI / Swagger UI
- `CacheConfig.java` — Spring Cache with Redis backend
- `I18nConfig.java` — Message source for i18n
- `MetricsConfig.java` — Micrometer metrics
- `JpaAuditingConfiguration.java` — JPA auditing (`created_at`, `updated_at`)
- `DatabaseHealthIndicator.java` — DB health check
- `MinioHealthIndicator.java` — MinIO health check
- `RedisHealthIndicator.java` — Redis health check
- `SecurityStartupValidator.java` — Validates security config on startup
- `ApiVersionConfig.java` — API versioning
