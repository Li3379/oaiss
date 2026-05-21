<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.config

Spring configuration classes that wire up infrastructure beans, security, caching, health checks, and integrations.

## Key Files (16 files)

| File | Category | Description |
|------|----------|-------------|
| `SecurityConfig.java` | Security | Spring Security filter chain: JWT filter injection, CORS configuration, CSRF disable (stateless), session management (STATELESS), `@EnableMethodSecurity` for `@PreAuthorize`, BCrypt password encoder, role-based URL authorization, security headers. |
| `SecurityStartupValidator.java` | Security | `@Component` that validates security config on `ContextRefreshedEvent`. Blocks startup in production if weak JWT secrets or default DB passwords are detected. |
| `RedisConfig.java` | Cache/Redis | Redis connection and serialization: `RedisTemplate<String, Object>` with `Jackson2JsonRedisSerializer` + `StringRedisSerializer`, `@EnableCaching`. Supports `JavaTimeModule` for JDK date/time types. |
| `CacheConfig.java` | Cache/Redis | Spring Cache abstraction with `@EnableCaching`. Primary `RedisCacheManager` (transaction-aware); falls back to `ConcurrentMapCacheManager` when Redis is unavailable. |
| `MinioConfig.java` | Storage | MinIO object storage client: `@ConfigurationProperties(prefix = "minio")` for endpoint/accessKey/secretKey/bucketName. Produces `MinioClient` bean. |
| `FabricGatewayConfig.java` | Blockchain | Hyperledger Fabric gateway: `@Profile("fabric")`, `@EnableConfigurationProperties(FabricProperties.class)`. Creates gRPC channel, `Gateway`, `Network`, and `Contract` beans for chaincode invocation. |
| `FabricProperties.java` | Blockchain | `@ConfigurationProperties(prefix = "fabric")`: mspId, channelName, chaincodeName, peerEndpoint, TLS config, cert/key paths, connect/submit timeouts, nested `Ca` properties (endpoint, adminName, adminPassword). |
| `MlServiceConfig.java` | ML/Integration | ML service client: `@ConfigurationProperties(prefix = "ml.service")` for URL/secret. Produces `WebClient` bean with Reactor Netty connection pooling, timeout handlers, and Resilience4j `CircuitBreaker`. |
| `SwaggerConfig.java` | API Docs | SpringDoc OpenAPI / Swagger UI: `OpenAPI` bean with API info (title, description, version, contact), JWT `SecurityScheme` (Bearer), global `SecurityRequirement`. |
| `ApiVersionConfig.java` | Web | `WebMvcConfigurer` for API versioning. Defines `CURRENT_VERSION = "v1"` and `SUPPORTED_VERSIONS`. Configures `PathMatchConfigurer` for version-prefixed URL routing. |
| `I18nConfig.java` | Web | `MessageSource` bean using `ReloadableResourceBundleMessageSource` with `classpath:i18n/messages` basename and UTF-8 encoding for zh-CN/en-US. |
| `JpaAuditingConfiguration.java` | JPA | `@EnableJpaAuditing` in a standalone `@Configuration` class (separated from the main application class to avoid `@WebMvcTest` failures from missing JPA entities). Powers `createdAt`/`updatedAt` auto-fill on `BaseEntity`. |
| `MetricsConfig.java` | Monitoring | Micrometer/Prometheus metrics: `MeterRegistryCustomizer` adding `application` tag, `MeterFilter` to deny/allow specific metric names. |
| `DatabaseHealthIndicator.java` | Health | Actuator health check for MySQL: validates `DataSource` connection with 2-second timeout. |
| `MinioHealthIndicator.java` | Health | Actuator health check for MinIO: verifies bucket existence via `MinioClient`. |
| `RedisHealthIndicator.java` | Health | Actuator health check for Redis: executes PING command, confirms PONG response. |

## For AI Agents

### Working Instructions

1. **SecurityConfig is the most critical file** -- it defines the entire HTTP security chain. When adding a new public endpoint (no auth required), add it to the whitelist in `SecurityConfig`. When adding role restrictions, use `@PreAuthorize` annotations on controller methods instead of modifying `SecurityConfig` URL rules.
2. **Health indicators** follow the Actuator pattern: implement `HealthIndicator`, return `Health.up()` or `Health.down()`. To add a new health check (e.g., for Fabric), create a new `*HealthIndicator.java` in this package.
3. **Fabric config is profile-gated**: `FabricGatewayConfig` only activates when `fabric` profile is set (`@Profile("fabric")`). Without the profile, all Fabric beans are absent and `BlockchainServicePort` uses its mock/fallback implementation.
4. **CacheConfig graceful degradation**: Redis is primary; if unavailable, the `@ConditionalOnMissingBean` fallback creates an in-memory `ConcurrentMapCacheManager`. Do not assume Redis is always present in tests.
5. **MlServiceConfig circuit breaker**: The ML WebClient is wrapped in a Resilience4j `CircuitBreaker`. Services calling ML endpoints should handle `CallNotPermittedException` when the breaker is open.
6. **JpaAuditingConfiguration is standalone** to allow `@WebMvcTest` slices to load without JPA. Never merge `@EnableJpaAuditing` back into the main application class.
7. **MinioConfig is both config and properties**: It uses `@ConfigurationProperties(prefix = "minio")` AND `@Configuration` in one class. Properties are: `endpoint`, `accessKey`, `secretKey`, `bucketName`.

### Configuration Properties Reference

| Prefix | File | Key Properties |
|--------|------|----------------|
| `fabric` | `FabricProperties` | `enabled`, `mspId`, `channelName`, `chaincodeName`, `peerEndpoint`, `tlsEnabled`, `ca.enabled`, `ca.endpoint` |
| `minio` | `MinioConfig` | `endpoint`, `accessKey`, `secretKey`, `bucketName` |
| `ml.service` | `MlServiceConfig` | `url`, `secret` |

### Testing Notes

- Use `@WebMvcTest` for controller tests -- `JpaAuditingConfiguration` will not load (by design).
- Use `@SpringBootTest` with `@ActiveProfiles("test")` for integration tests that need the full config.
- Mock `MinioClient`, `RedisTemplate`, and `WebClient` (ML) beans in unit tests -- do not connect to real infrastructure.
- `SecurityStartupValidator` will block startup if `JWT_SECRET` matches known weak values. Set a strong secret in test `application-test.yml`.

### Dependencies

- **SecurityConfig** depends on `security/` package: `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`
- **FabricGatewayConfig** depends on `fabric-sdk` (Hyperledger Fabric Java SDK, gRPC)
- **MlServiceConfig** depends on `WebFlux` (Reactor Netty), `Resilience4j`
- **CacheConfig** depends on Spring Data Redis
- **SwaggerConfig** depends on `springdoc-openapi-starter-webmvc-ui`
