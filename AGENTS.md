<!-- Generated: 2026-05-11 | Updated: 2026-05-19 -->

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
| `oaiss-chain-ml-service/` | Python FastAPI ML prediction microservice |
| `oaiss-chain-chaincode/` | Hyperledger Fabric Go chaincode |
| `fabric-config/` | Fabric network configuration (crypto, scripts) |
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


<claude-mem-context>
# Memory Context

# [OAISS CHAIN] recent context, 2026-05-22 5:56pm GMT+8

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (18,971t read) | 0t work

### May 16, 2026
S114 gsd:progress — User requested a progress check on OAISS CHAIN v1.1.0 project status (May 16, 10:34 AM)
S115 GSD resume-work 2 → verify-work + secure-phase — closing all gates on Phase 2 carbon report lifecycle (May 16, 10:36 AM)
S116 GSD resume-work 2 → verify-work + secure-phase + validate-phase — closing all Phase 2 gates (May 16, 10:37 AM)
S117 Deep code review of Phase 7 (AI Intelligent Prediction Foundation) using gsd:code-review 7 --depth=deep (May 16, 10:43 AM)
S118 Phase 7 AI Intelligent Prediction Foundation — Deep code review fix and test stabilization (May 16, 10:48 AM)
S119 gsd:code-review-fix 4 --all — Auto-fix all 16 code review findings from Phase 4 (碳中和项目生命周期 + 信用评分) deep review (May 16, 12:07 PM)
S120 gsd:verify-work 4 — Verify Phase 04 (projects-credit) code review fixes via UAT testing (May 16, 2:12 PM)
S121 Deep code review of Phase 6 (Cross-Cutting Edge Cases) in OAISS CHAIN project — gsd:code-review 6 --depth=deep (May 16, 2:21 PM)
1290 4:03p 🔵 DigitalSignatureController @DataIsolation endpoints have @PreAuthorize('ENTERPRISE') but generateKeyPair allows broader roles
1291 4:04p 🔵 Phase 6 deep code review completed: 12 findings (3 critical, 5 warning, 4 info)
S122 gsd:verify-work — Phase 8 UAT verification completed, continuing to survey remaining uncovered phases (May 16, 4:05 PM)
1292 4:07p 🔵 CLAUDE.md documents pageNum/pageSize→page/size conversion in frontend interceptor
1293 " ✅ Phase 6 code review fix worktree created on branch review-fix-06-temp
1294 4:09p 🔴 CR-01 fixed: edge-test.sh admin pagination now uses correct page/size parameters
1295 4:10p 🔴 CR-02 partial fix: aop-test.sh AuditLog injection now uses line-number-based sed
1296 " 🔴 CR-02 complete: all three aop-test.sh sed injections now use line-number-based targeting
1297 4:11p 🔵 DistributedLock annotation supports SpEL key expressions for parameterized locking
1298 4:20p 🔵 User invoked /gsd:explore command
1299 4:21p 🔵 Codebase planning documentation exists with 2154 lines across 7 files
1300 4:23p ✅ STACK.md updated with ML service, Fabric blockchain, and expanded infrastructure docs
1301 4:24p ✅ INTEGRATIONS.md updated with ML service, real Fabric SDK, expanded schema, and data flow diagram
### May 18, 2026
1302 12:00a ⚖️ Full-stack QA workflow plan established
1306 " 🔵 OAISS CHAIN project architecture and module structure identified
1307 " 🔵 Known test failure: carbon trading state loss on page navigation
1303 " 🔵 OAISS CHAIN UAT Status: 8 of 12 Phases Tracked
1304 " 🟣 Phase 08: AI Frontend Pages with ML Visualization Built
1305 " 🟣 Phase 08: GB/T 32150-2015 Carbon Emission Formula Calculators Implemented
1308 12:01a 🔵 Requirements docs require GBK-to-UTF-8 encoding conversion
1309 " 🔵 Detailed project architecture: RSA signatures, double auction, Fabric chaincode
1310 " 🔵 Complete frontend-backend API and view mapping for all four roles
1311 12:02a 🔵 Infrastructure stack and local development configuration mapped
1312 " 🔵 Frontend routing with role-based guards and complete route table
1313 " 🔵 Phase 09 (Blockchain Integration) Not Yet Executed
1314 " 🔵 Phase 09-01 Plan: Fabric Network Setup + Gateway SDK Integration
1315 12:03a 🔵 Phase 09 Full Plan Structure: 3 Sub-Plans for Fabric Blockchain Integration
1318 12:05a 🔵 Fabric Gateway SDK Dependency Already in pom.xml
1321 " 🔵 Phase 09 Partially Implemented Despite No Formal Execution Summaries
1323 " 🔵 Fabric SDK Dependencies Match Plan 09-01 Specifications Exactly
1328 " 🟣 Go Chaincode carbon-chaincode Implements On-Chain Carbon Reports and Trade Records
1329 " 🟣 FabricBlockchainService Implements BlockchainServicePort with @Profile("fabric")
1330 " 🟣 FabricGatewayConfig Creates Gateway/Network/Contract Beans Under @Profile("fabric")
1331 " 🟣 Docker Compose Fabric Overlay Deploys Full Fabric Network with 5 Services
1332 " 🔵 BlockchainServicePort Interface Defines 10 Methods for Blockchain Abstraction
1335 12:07a 🔵 Fabric Configuration: Spring Profile Toggle with Default-Off and Crypto Paths
1336 " 🟣 FabricProperties Includes Nested CA Configuration for Plan 09-03
1339 " ✅ Phase 09 UAT Created: 14/15 Passed, 1 Skipped (Optional Fabric CA)
1343 12:08a ✅ UAT Coverage Now 10 of 12 Phases
1344 " 🔵 Phase 10 Has Execution Summaries — Can Be UAT Verified
1346 " 🔵 Phase 11 Also Has All Execution Summaries — Ready for UAT
1348 " 🟣 Phase 10-03: Frontend Certificate Management with Dual-Tab Admin Page
1351 12:09a 🟣 Phase 10: Enterprise Admission and Reviewer Qualification Full-Stack Implementation
1352 " 🟣 Phase 11: Frontend Coverage Completion — 46 API Functions, 3 New Views, Swagger Tag Renumbering
1354 " 🔵 Phases 10 and 11 All Deliverables Verified Present on Disk
1356 12:10a 🔵 Phase 10 and 11 UAT Verification: All Code Artifacts Confirmed
1363 12:11a ✅ Phase 10 UAT Created: 17/17 Passed — Admission & Qualification Full Stack Verified
1365 " ✅ Phase 11 UAT Created: 16/16 Passed — Frontend Coverage & Swagger Complete
S123 gsd:verify-work — Complete UAT verification for all remaining phases (08, 09, 10, 11) of the OAISS CHAIN project (May 18, 12:12 AM)
1383 12:15a ✅ User requested session continuation
### May 22, 2026
1385 3:11p 🔴 TradeController path variable regex constraint to fix counterparties 400 error
1386 " 🔵 OAISS CHAIN service health status and E2E test infrastructure
</claude-mem-context>