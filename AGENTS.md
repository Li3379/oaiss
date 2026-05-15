<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# OAISS CHAIN

## Purpose
双碳链动系统 (Dual-Carbon Chain System) — A full-stack carbon accounting, carbon trading, and blockchain attestation platform. Java 17 / Spring Boot 3.2.5 backend with Vue 3 / TypeScript frontend, deployed via Docker Compose with MySQL, Redis, and MinIO.

## Key Files
| File | Description |
|------|-------------|
| `CLAUDE.md` | Project-specific AI behavioral guidelines and tech stack reference |
| `docker-compose.yml` | Full-stack Docker Compose (MySQL, Redis, MinIO, backend, frontend) |
| `docker-compose.infra.yml` | Infrastructure-only Docker Compose (MySQL, Redis, MinIO) |
| `.env.example` | Environment variable template |
| `pom.xml` | Maven parent POM (if multi-module) |

## Subdirectories
| Directory | Purpose |
|-----------|---------|
| `oaiss-chain-backend/` | Spring Boot 3.2.5 backend (see `oaiss-chain-backend/AGENTS.md`) |
| `oaiss-chain-frontend/` | Vue 3 + TypeScript frontend (see `oaiss-chain-frontend/AGENTS.md`) |
| `docs/` | Project documentation and specs (see `docs/AGENTS.md`) |
| `scripts/` | Shell test scripts for API endpoints (see `scripts/AGENTS.md`) |
| `tracks/` | Prompt engineering tracks and verification records (see `tracks/AGENTS.md`) |

## For AI Agents

### Working In This Directory
- Always check `.env.example` before running any service; copy to `.env` and fill in secrets
- Backend runs on port 8080, frontend dev server on port 5173
- Run `docker-compose up` for full stack, or `docker-compose -f docker-compose.infra.yml up` for infrastructure only
- Use conventional commits: `feat|fix|docs|test|refactor|chore(scope): description`

### Testing Requirements
- Backend: `cd oaiss-chain-backend && mvn test` (unit), `mvn verify` (integration)
- Frontend: `cd oaiss-chain-frontend && npm run test` (Vitest unit), Playwright E2E
- Shell scripts in `scripts/` provide API-level smoke tests

### Common Patterns
- Response envelope: `ApiResponse<T>` with `{ code, message, data, meta }`
- Pagination: frontend `pageNum/pageSize` → backend `page/size` → Spring Data `Page`
- Auth: JWT Bearer token, roles: ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN
- Cross-cutting concerns via custom annotations: `@AuditLog`, `@RateLimit`, `@RequirePermission`, `@DataIsolation`, `@DistributedLock`

## Dependencies

### External
- Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8, Redis 7, MinIO
- Vue 3.5, TypeScript, Vite 8, Element Plus 2.13, Pinia 3, ECharts 6
- Docker Compose for infrastructure

<!-- MANUAL: Any manually added notes below this line are preserved on regeneration -->
