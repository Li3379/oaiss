<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

# src/main — Production Source Code

Main production source code for the Spring Boot application.

## Structure

| Directory | Purpose |
|-----------|---------|
| `java/com/oaiss/chain/` | Java source packages (controller, service, repository, entity, dto, config, etc.) |
| `resources/` | Application configuration, Flyway migrations, i18n (see resources/AGENTS.md) |

## Key Packages

| Package | Purpose |
|---------|---------|
| `controller/` | REST endpoints (16 controllers) |
| `service/` | Business logic (23 services) |
| `repository/` | Spring Data JPA repositories (22) |
| `entity/` | JPA entities (21 entities) |
| `dto/` | Request/response DTOs (30+) |
| `config/` | Spring config (Security, Redis, MinIO, etc.) |
| `annotation/` | Custom annotations (@AuditLog, @RateLimit, etc.) |
| `aop/` | AOP aspects |
| `security/` | JWT filter, entry point |
| `enums/` | Enumerations |
| `constant/` | ErrorCode, ErrorMessage |
| `exception/` | Custom exceptions |
| `util/` | Utilities |

## Conventions

- API base path: `/api/v1`
- Response envelope: `ApiResponse<T>` with `{ code, message, data, meta }`
- Auth: JWT Bearer token + CSRF cookie
- Role-based access: `@PreAuthorize` on controllers
- Lombok: `@Data`, `@Builder`, `@RequiredArgsConstructor`
