# Phase 5 Discuss-Phase Checkpoint

**Started:** 2026-05-09
**Areas to discuss:** 4

## Resolved Areas

### 1. Admin Management Scope (ADMIN-01~05)

**Decision:** Verify existing endpoints; record gaps as known issues.

- **ADMIN-01** (list users): `GET /admin/users` — EXISTS, verify
- **ADMIN-02** (create user): NO backend endpoint, NO frontend UI — recorded as code gap
- **ADMIN-03** (edit user): NO backend endpoint, NO frontend UI — recorded as code gap
- **ADMIN-04** (enable/disable): `PUT /admin/users/{userId}/status` — EXISTS, verify
- **ADMIN-05** (system config): `GET /admin/dashboard` + `GET /admin/statistics` — EXISTS, verify

**Rationale:** Neither backend `AdminController` nor frontend `SystemUsers.vue` have create/edit user functionality. `SystemConfig.vue` manages config items locally (no backend persistence). Fixing this is out of scope for manual testing — record as known gap.

### 2. Blockchain Mock Depth (BLOCK-01~03)

**Decision:** Verify API 200 + field existence + format validation + frontend form interaction. ADMIN role only.

- **API verification:** All 5 endpoints return 200 with correct response envelope
- **Field existence:** `connected`, `channel`, `mode` on status; `blockNumber`, `blockHash`, `txCount` on blocks; `txHash`, `status` on transactions
- **Format validation:** txHash starts with `tx_mock_`, blockNumber is positive integer, blockHash starts with `0x`
- **Frontend interaction:** Test blockchain explorer UI — pagination, block detail drill-down, transaction lookup
- **Role access:** ADMIN only (covers all endpoints per `@PreAuthorize`)

**Rationale:** Mock data is structurally sufficient for verification. No code changes needed. Format checks ensure downstream consumers won't break. Frontend interaction validates the explorer UI actually renders mock data.

### 3. Test Ordering & Batching

**Decision:** 7 independent scripts (one per sub-domain), sequential execution.

- Each sub-domain gets its own test script with its own login + test + verify cycle
- Execution order: Signatures → Files → Emissions → Blockchain → Admin → ThirdParty → Search
- If a script fails, pause and fix before continuing
- ROADMAP's 4-plan structure superseded: 7 scripts replace 05-01~05-04

**Script breakdown:**
1. `sign-test.sh` — SIGN-01~03 (RSA keypair, sign, verify)
2. `file-test.sh` — FILE-01~03 (MinIO upload, download, console)
3. `emission-test.sh` — EMIT-01~03 (emission data, ratings, factors)
4. `blockchain-test.sh` — BLOCK-01~03 (status, blocks, transactions)
5. `admin-test.sh` — ADMIN-01~05 (user list, status toggle, dashboard, statistics)
6. `thirdparty-test.sh` — TP-01~02 (monitoring, carbon reports)
7. `search-test.sh` — SRCH-01 (cross-entity search)

**Rationale:** Finer granularity isolates failures to specific sub-domains. Self-contained scripts are easier to re-run individually.

### 4. Third-Party Monitoring Scope (TP-01~02)

**Decision:** Verify all 4 endpoints; TP-02 marked as partially covered.

- **TP-01** (monitoring data): Fully covered by `GET /third-party/statistics` + `GET /third-party/carbon-reports`
- **TP-02** (trade audit records): Partially covered — `/carbon-reports` provides monitoring-perspective report query, but no dedicated trade audit endpoint exists. Mark as partial coverage.
- **Additional endpoints to verify:** `GET /third-party/org-info` (org info), `PUT /third-party/contact` (update contact)
- **Role:** THIRD_PARTY only (class-level `@PreAuthorize`)

**Rationale:** ThirdPartyController has no trade-specific audit endpoint. Carbon reports query is the closest proxy for monitoring. Full verification of available endpoints + clear documentation of TP-02 gap.

## All 4 Areas Resolved

Ready to write CONTEXT.md.
