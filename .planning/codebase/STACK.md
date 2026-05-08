# Technology Stack

**Project**: OAISS CHAIN (双碳链动系统)
**Description**: Blockchain-based trusted carbon accounting and trading platform
**Last updated**: 2026-05-08

---

## Languages

| Language | Version | Usage |
|----------|---------|-------|
| Java | 17 | Backend application |
| TypeScript | 6.x | Frontend application |
| JavaScript (ES Modules) | ES2022+ | Vite build config, Playwright tests |
| SQL | MySQL 8.0 dialect | Database schema, migrations |

---

## Runtime

| Runtime | Version | Notes |
|---------|---------|-------|
| JDK | 17 | Spring Boot target |
| Node.js | 18+ (implied by deps) | Frontend build & dev server |
| Maven | 3.x | Backend build tool |

---

## Backend Frameworks & Libraries

### Core (Spring Boot 3.2.5)

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot-starter-web | 3.2.5 | REST API layer |
| spring-boot-starter-data-jpa | 3.2.5 | ORM / data access |
| spring-boot-starter-security | 3.2.5 | Authentication & authorization |
| spring-boot-starter-validation | 3.2.5 | Bean validation (JSR 380) |
| spring-boot-starter-cache | 3.2.5 | Caching abstraction |
| spring-boot-starter-data-redis | 3.2.5 | Redis integration (Lettuce) |
| spring-boot-starter-aop | 3.2.5 | Aspect-oriented programming |
| spring-boot-starter-actuator | 3.2.5 | Health checks, metrics endpoints |

### Data & Storage

| Dependency | Version | Purpose |
|------------|---------|---------|
| mysql-connector-j | (managed) | MySQL 8.0 JDBC driver |
| flyway-mysql | (managed) | Database schema migrations |
| minio | 8.5.7 | S3-compatible object storage client |

### Security & Auth

| Dependency | Version | Purpose |
|------------|---------|---------|
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.5 | JWT token creation & validation |
| BCryptPasswordEncoder | (Spring Security) | Password hashing |

### API Documentation

| Dependency | Version | Purpose |
|------------|---------|---------|
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI 3 docs |

### Observability

| Dependency | Version | Purpose |
|------------|---------|---------|
| micrometer-registry-prometheus | (managed) | Prometheus metrics export |
| logstash-logback-encoder | 7.4 | JSON structured logging (ELK-compatible) |

### Code Quality & Build Plugins

| Plugin | Version | Purpose |
|--------|---------|---------|
| maven-pmd-plugin | 3.21.0 | Static analysis |
| spotbugs-maven-plugin | 4.7.3 | Bug detection |
| jacoco-maven-plugin | 0.8.11 | Code coverage (90% line minimum) |
| maven-surefire-plugin | 3.2.5 | Unit test runner |
| maven-failsafe-plugin | 3.2.5 | Integration test runner |

### Utilities

| Dependency | Version | Purpose |
|------------|---------|---------|
| lombok | (managed) | Boilerplate reduction |
| mapstruct | 1.5.5.Final | DTO/entity mapping |

---

## Frontend Frameworks & Libraries

### Core (Vue 3)

| Dependency | Version | Purpose |
|------------|---------|---------|
| vue | 3.5.32 | UI framework |
| vue-router | 5.0.6 | Client-side routing with role-based guards |
| pinia | 3.0.4 | State management |
| vue-i18n | 11.4.0 | Internationalization (zh-CN, en-US) |

### UI & Visualization

| Dependency | Version | Purpose |
|------------|---------|---------|
| element-plus | 2.13.7 | UI component library |
| @element-plus/icons-vue | 2.3.2 | Icon set |
| echarts | 6.0.0 | Charting / data visualization |
| axios | 1.7.0 | HTTP client |

### Dev & Build

| Dependency | Version | Purpose |
|------------|---------|---------|
| vite | 8.0.10 | Build tool & dev server |
| @vitejs/plugin-vue | 6.0.6 | Vue SFC compilation |
| typescript | 6.0.3 | Type system |
| vue-tsc | 3.2.8 | Vue TypeScript type checking |

### Testing

| Dependency | Version | Purpose |
|------------|---------|---------|
| vitest | 4.1.5 | Unit test runner (happy-dom environment) |
| @vue/test-utils | 2.4.10 | Vue component testing utilities |
| @playwright/test | 1.59.1 | E2E testing (Chromium) |
| cross-env | 10.1.0 | Cross-platform env vars for test modes |

---

## Configuration

### Environment Profiles

| Profile | File | Purpose |
|---------|------|---------|
| (default) | application.yml | Base config: MySQL, Redis, Flyway, JWT, MinIO, Actuator, SpringDoc |
| dev | application-dev.yml | Local dev: H2 option, ddl-auto=update, Flyway disabled |
| docker | application-docker.yml | Container: env-var driven, Swagger disabled, production logging |
| test | application-test.yml | Tests: H2 in-memory, ddl-auto=create-drop, debug logging |

### Key Environment Variables

| Variable | Default | Used By |
|----------|---------|---------|
| `DB_PASSWORD` | (required) | MySQL connection |
| `REDIS_PASSWORD` | (empty) | Redis connection |
| `JWT_SECRET` | (required) | JWT signing |
| `MINIO_ACCESS_KEY` | (required) | MinIO auth |
| `MINIO_SECRET_KEY` | (required) | MinIO auth |
| `MINIO_ENDPOINT` | http://localhost:9000 | MinIO connection |
| `CORS_ALLOWED_ORIGINS` | http://localhost:5173 | CORS policy |
| `VITE_API_BASE_URL` | (frontend env) | API base URL |

### Frontend Dev Server

- Port: 5173 (Vite dev server)
- API proxy: `/api` -> `http://localhost:8080`
- Backend port: 8080

---

## Infrastructure (Docker Compose)

| Service | Image | Ports | Purpose |
|---------|-------|-------|---------|
| mysql | mysql:8.0 | 3306 | Primary database |
| redis | redis:7-alpine | 6379 | Caching, rate limiting, distributed locks |
| minio | minio/minio:latest | 9000 (API), 9001 (console) | Object storage for files |
| backend | Custom Dockerfile | 8080 | Spring Boot API |
| frontend | Custom Dockerfile | 5173->80 | Nginx-served SPA |

### Volumes

- `mysql-data` - MySQL persistent storage
- `redis-data` - Redis persistent storage
- `minio-data` - MinIO persistent storage

---

## Project Structure Summary

```
oaiss-chain-backend/
  src/main/java/com/oaiss/chain/
    controller/    (16 REST controllers)
    service/       (19 service classes)
    repository/    (Spring Data JPA)
    entity/        (22 JPA entities)
    dto/           (Request/response DTOs)
    config/        (Security, Redis, MinIO, etc.)
    annotation/    (Custom: @AuditLog, @RateLimit, @RequirePermission, @DataIsolation, @DistributedLock)
    aop/           (AOP aspects for cross-cutting concerns)
    security/      (JWT filter, entry point, access denied handler)
    enums/         (Enumerations)
    constant/      (ErrorCode, ErrorMessage)
    exception/     (Custom exceptions)
    util/          (RSA, etc.)
  src/main/resources/
    db/migration/  (Flyway: V1 schema, V2 seed data)
    application*.yml

oaiss-chain-frontend/
  src/
    api/           (17 API modules)
    types/         (16 type definition files)
    views/         (enterprise/, admin/, auditor/, authenticator/, third-party/)
    store/         (Pinia stores)
    router/        (Vue Router with role-based guards)
    i18n/          (zh-CN, en-US)
    components/    (Shared components)
    layout/        (App layout shell)
    config/        (Menu and image config)
    utils/         (Auth, ECharts)
  tests/e2e/       (Playwright: smoke/ and flows/)
```
