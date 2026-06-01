# Phase 5: Supporting Domains - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

All secondary platform features verified as functional: digital signatures (RSA keypair generation, signing, verification), file management (MinIO upload/download), emission ratings (data viewing, rating calculation, emission factors), blockchain explorer (mock mode status, blocks, transactions), admin user management (list, status toggle, dashboard), third-party monitoring (statistics, carbon reports), and cross-entity search. This phase covers 20 requirements across 7 sub-domains, all independent of each other and depending only on Phase 1 infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Test Organization (supersedes ROADMAP 4-plan structure)
- **D-01:** 7 independent test scripts, one per sub-domain. Each script has its own login + test + verify cycle. Replaces ROADMAP's 05-01~05-04 plan structure.
- **D-02:** Sequential execution order: Signatures → Files → Emissions → Blockchain → Admin → ThirdParty → Search. If a script fails, pause and fix before continuing.
- **D-03:** Script naming: `sign-test.sh`, `file-test.sh`, `emission-test.sh`, `blockchain-test.sh`, `admin-test.sh`, `thirdparty-test.sh`, `search-test.sh`. Follow existing `scripts/login-test.sh` pattern (curl + ok/fail/info helpers).

### Admin Management Scope (ADMIN-01~05)
- **D-04:** Verify existing endpoints only. `AdminController` has 4 endpoints: `GET /admin/users` (list), `PUT /admin/users/{userId}/status` (enable/disable), `GET /admin/dashboard`, `GET /admin/statistics`.
- **D-05:** ADMIN-02 (create user) and ADMIN-03 (edit user) are code gaps — no backend endpoints exist, no frontend UI entry point. Record as known gaps in CONTEXT.md and test results. Do NOT attempt to fix.
- **D-06:** `SystemConfig.vue` manages config items with frontend-only state (no backend API persistence). ADMIN-05 verification covers dashboard + statistics endpoints only.

### Blockchain Mock Depth (BLOCK-01~03)
- **D-07:** Verify API 200 + field existence + format validation + frontend form interaction. Use ADMIN role only (covers all `@PreAuthorize` combinations).
- **D-08:** Format validation rules:
  - `txHash` starts with `tx_mock_` prefix
  - `blockNumber` is positive integer
  - `blockHash` starts with `0x` prefix
  - `status` field present on transactions
  - `connected`, `channel`, `mode` fields present on connection status
- **D-09:** Frontend blockchain explorer UI must render mock data correctly — test pagination, block detail drill-down, transaction lookup.

### Third-Party Monitoring Scope (TP-01~02)
- **D-10:** Verify all 4 `ThirdPartyController` endpoints: `GET /org-info`, `GET /carbon-reports` (with filters), `GET /statistics`, `PUT /contact`.
- **D-11:** TP-01 (monitoring data) fully covered by `/statistics` + `/carbon-reports`.
- **D-12:** TP-02 (trade audit records) partially covered — `/carbon-reports` provides monitoring-perspective report query but no dedicated trade audit endpoint exists. Mark TP-02 as partial coverage in test results.
- **D-13:** Class-level `@PreAuthorize("hasRole('THIRD_PARTY')")` — all endpoints require THIRD_PARTY role.

### Emission Ratings (EMIT-01~03)
- **D-14:** `EmissionController` has 4 endpoints: `GET /ratings/{enterpriseId}`, `POST /ratings` (create/recalculate), `GET /rankings/{year}`, `GET /predict`. Verify all with ADMIN or AUTHENTICATOR role.
- **D-15:** Emission factors are hardcoded in `CachePreloadService` — accept that config changes require backend restart. Test current values only.

### Digital Signatures (SIGN-01~03)
- **D-16:** `DigitalSignatureController` has 7 endpoints: keypair generate/get/delete, sign, verify, encrypt, decrypt. Core flow: generate keypair → sign data → verify signature.
- **D-17:** RSA key pairs stored in `rsa_key_pairs` table. Verify DB record created after keypair generation.

### File Management (FILE-01~03)
- **D-18:** File upload already partially tested in Phase 2 (carbon report attachments). This phase verifies the full `FileController` lifecycle: upload, download, delete, info, presigned URLs.
- **D-19:** MinIO console at `localhost:9001` — verify accessible (may need separate login credentials from `.env`).

### Search (SRCH-01)
- **D-20:** `SearchController` has 3 endpoints: `GET /reports`, `GET /trades`, `GET /market-overview`. Verify cross-entity search returns correct results across enterprises.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend - Phase 5 Controllers
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/DigitalSignatureController.java` — 7 endpoints for RSA operations
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/FileController.java` — 11 endpoints for MinIO file operations
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/EmissionController.java` — 4 endpoints for emission data/ratings
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/BlockchainController.java` — 5 endpoints for blockchain explorer
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java` — 4 endpoints (list, status, dashboard, statistics)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/ThirdPartyController.java` — 4 endpoints (org-info, reports, statistics, contact)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/SearchController.java` — 3 endpoints (reports, trades, market-overview)

### Backend - Key Services
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/BlockchainService.java` — Mock implementation, txHash format: `tx_mock_{timestamp}_{uuid}`
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/MinioService.java` — File upload/download with presigned URLs
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EmissionRatingService.java` — Rating calculation with hardcoded thresholds

### Frontend - Phase 5 Views
- `oaiss-chain-frontend/src/views/admin/SystemUsers.vue` — User list + status toggle (no create/edit)
- `oaiss-chain-frontend/src/views/admin/SystemConfig.vue` — Config management (frontend-only state)

### Infrastructure & Patterns
- `.planning/codebase/INTEGRATIONS.md` — Full external integration docs (MySQL, Redis, MinIO, Blockchain mock, JWT, RSA)
- `scripts/login-test.sh` — Pattern for API test scripts (login → verify → report)
- `.planning/ROADMAP.md` — Phase 5 success criteria, entry/exit criteria

### Requirements
- `.planning/REQUIREMENTS.md` — SIGN-01~03, FILE-01~03, EMIT-01~03, BLOCK-01~03, ADMIN-01~05, TP-01~02, SRCH-01

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `scripts/login-test.sh` — Bash script pattern with curl calls, ok/fail/info output helpers, JWT token extraction. All 7 new scripts follow this pattern.
- `scripts/health-check.sh` — Can verify environment readiness before Phase 5 tests.
- Phase 2 file upload via `POST /file/upload` already tested — FILE-01 has a working reference path.

### Known Code Gaps (record, do not fix)
- **ADMIN-02/03:** No create-user or edit-user endpoints in `AdminController`. No frontend UI for these operations. `SystemUsers.vue` only imports `getUserList` and `updateUserStatus`.
- **TP-02:** No dedicated trade audit endpoint in `ThirdPartyController`. `/carbon-reports` is the closest proxy.

### Role Access Summary
| Sub-domain | Primary Role | Endpoints |
|------------|-------------|-----------|
| Signatures | ENTERPRISE | 7 endpoints (keypair, sign, verify, encrypt, decrypt) |
| Files | Any authenticated | 11 endpoints (upload, download, delete, etc.) |
| Emissions | ADMIN/AUTHENTICATOR | 4 endpoints (ratings, rankings, predict) |
| Blockchain | ADMIN/AUTHENTICATOR/THIRD_PARTY | 5 endpoints (status, blocks, transactions) |
| Admin | ADMIN | 4 endpoints (users, dashboard, statistics) |
| Third-Party | THIRD_PARTY | 4 endpoints (org-info, reports, statistics, contact) |
| Search | Any authenticated | 3 endpoints (reports, trades, market-overview) |

### Mock Data Formats
- Blockchain txHash: `tx_mock_{System.currentTimeMillis()}_{UUID.substring(0,8)}`
- Blockchain blockHash: `0x{UUID.replace("-","").substring(0,32)}`
- Blockchain connection: `{connected: true, channel: "carbon-channel", peers: 2, orderers: 1, mode: "MOCK"}`
- All mock transactions have status `"VALID"`

</code_context>

<specifics>
## Specific Ideas

- Each test script should start with a fresh login to avoid JWT expiration issues across 7 scripts
- Use `enterprise001` for signature tests (ENTERPRISE role), `admin` for blockchain/admin tests, `thirdparty001` for third-party tests
- MinIO console at `localhost:9001` uses credentials from `.env` (MINIO_ACCESS_KEY/MINIO_SECRET_KEY)
- Emission ratings may already have data from Phase 2 report approvals — check before creating new test data
- Blockchain mock returns random data each call — do not assert specific values, assert structure only
- Search tests need existing data from Phases 2-4 — run search tests last to leverage accumulated test data

</specifics>

<deferred>
## Deferred Ideas

- ADMIN-02/03 (create/edit user) — requires new backend endpoints + frontend UI. Out of scope for manual testing phase.
- TP-02 (trade audit endpoint) — requires new `ThirdPartyController` endpoint. Record as gap, defer to implementation phase.
- SystemConfig backend persistence — currently frontend-only. Would need new `SystemConfigController` endpoints.
- Blockchain mock → real Hyperledger Fabric integration — separate initiative after testing phase.

</deferred>

---

*Phase: 05-supporting-domains*
*Context gathered: 2026-05-09*
