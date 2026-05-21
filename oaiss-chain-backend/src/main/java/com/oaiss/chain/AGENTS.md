<!-- Parent: ../../../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

# com.oaiss.chain

Root Java package for the OAISS CHAIN Spring Boot application.

## Key Files

- `OaissChainApplication.java` — Spring Boot main class with `@SpringBootApplication`

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `annotation/` | Custom annotations (5: `@AuditLog`, `@RateLimit`, `@DistributedLock`, `@DataIsolation`, `@RequirePermission`) |
| `aop/` | AOP aspects implementing cross-cutting concerns (5 aspects) |
| `config/` | Spring configuration classes (16: Security, Redis, MinIO, Swagger, Cache, i18n, Metrics, Fabric, ML, etc.) |
| `constant/` | Error codes and message constants (2: ErrorCode, ErrorMessage) |
| `controller/` | REST API controllers (20 controllers) |
| `dto/` | Data Transfer Objects for request/response (45 classes) |
| `entity/` | JPA entities mapping to database tables (23 entities incl. BaseEntity) |
| `enums/` | Enumeration types (9 enums) |
| `exception/` | Custom exceptions and global handler (7 classes) |
| `repository/` | Spring Data JPA repositories (22 interfaces) |
| `security/` | JWT authentication/authorization components (6 classes) |
| `service/` | Business logic services (31: 28 in root + 3 in ml/ subpackage) |
| `util/` | Utility classes (4: `CommonUtils`, `HashUtil`, `MessageUtils`, `RsaKeyUtil`) |
