<!-- Parent: ../../../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain

Root Java package for the OAISS CHAIN Spring Boot application.

## Key Files

- `OaissChainApplication.java` — Spring Boot main class with `@SpringBootApplication`

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `annotation/` | Custom annotations (`@AuditLog`, `@RateLimit`, `@DistributedLock`, `@DataIsolation`, `@RequirePermission`) |
| `aop/` | AOP aspects implementing cross-cutting concerns |
| `config/` | Spring configuration classes (Security, Redis, MinIO, Swagger, Cache, i18n, Metrics) |
| `constant/` | Error codes and message constants |
| `controller/` | REST API controllers (18 controllers) |
| `dto/` | Data Transfer Objects for request/response (36 classes) |
| `entity/` | JPA entities mapping to database tables (22 entities) |
| `enums/` | Enumeration types (8 enums) |
| `exception/` | Custom exceptions and global handler |
| `repository/` | Spring Data JPA repositories (21 interfaces) |
| `security/` | JWT authentication/authorization components |
| `service/` | Business logic services (21 services) |
| `util/` | Utility classes (`CommonUtils`, `HashUtil`, `MessageUtils`, `RsaKeyUtil`) |
