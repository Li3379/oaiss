---
phase: 18
slug: fabric-ca
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-05-22
---

# Phase 18 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (Backend)** | JUnit 5 + Mockito (spring-boot-starter-test) |
| **Framework (E2E)** | Playwright |
| **Config file (Backend)** | N/A — uses Spring Boot test annotations |
| **Config file (E2E)** | `oaiss-chain-frontend/playwright.config.ts` |
| **Quick run command** | `cd oaiss-chain-backend && mvn test -Dtest=FabricCAServiceTest` |
| **Full suite command** | `cd oaiss-chain-backend && mvn test` |
| **Estimated runtime** | ~15 seconds (unit) / ~120 seconds (E2E with Fabric) |

---

## Sampling Rate

- **After every task commit:** Run `cd oaiss-chain-backend && mvn test -Dtest=FabricCAServiceTest`
- **After every plan wave:** Run `cd oaiss-chain-backend && mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 18-01-01 | 01 | 1 | FABRIC-01 | T-18-01 | CA admin password never logged (@ToString.Exclude) | unit | `mvn test -Dtest=FabricCAServiceTest` | ❌ W0 | ⬜ pending |
| 18-01-02 | 01 | 1 | FABRIC-01 | T-18-02 | EnrollmentResult never serialized or persisted | unit | `mvn test -Dtest=FabricCAServiceTest` | ❌ W0 | ⬜ pending |
| 18-01-03 | 01 | 1 | FABRIC-01 | — | CA enrollment succeeds when CA available and enabled | unit | `mvn test -Dtest=FabricCAServiceTest` | ❌ W0 | ⬜ pending |
| 18-01-04 | 01 | 1 | FABRIC-01 | — | CA enrollment falls back to static crypto on failure | unit | `mvn test -Dtest=FabricGatewayConfigTest` | ❌ W0 | ⬜ pending |
| 18-01-05 | 01 | 1 | FABRIC-01 | — | Gateway uses CA identity when ca.enabled=true | unit | `mvn test -Dtest=FabricGatewayConfigTest` | ❌ W0 | ⬜ pending |
| 18-01-06 | 01 | 1 | FABRIC-01 | — | @Profile("fabric") conditional assembly correct | unit | `mvn test -Dtest=FabricCAServiceTest` | ❌ W0 | ⬜ pending |
| 18-01-07 | 01 | 2 | FABRIC-01 | — | CA enrollment status in blockchain status endpoint | e2e | `npx playwright test tests/e2e/v1.1/blockchain-formula-flow.spec.ts` | ✅ exists | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/FabricCAServiceTest.java` — stubs for FABRIC-01 (enrollment, fallback, profile)
- [ ] `oaiss-chain-backend/src/test/java/com/oaiss/chain/config/FabricGatewayConfigTest.java` — stubs for conditional identity loading
- [ ] `oaiss-chain-frontend/tests/e2e/v1.1/blockchain-formula-flow.spec.ts` — add CA enrollment describe block (file exists, needs modification)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CA TLS handshake in Docker network | FABRIC-01 | Requires running Fabric CA Docker service with TLS cert | 1. Start Fabric Docker stack 2. Set `fabric.ca.endpoint=https://ca.org1.example.com:7054` 3. Verify backend starts without SSLHandshakeException |
| Enrollment persistence across restarts | FABRIC-01 | Requires CA server state tracking across JVM restarts | 1. Start Fabric CA 2. Start backend with ca.enabled=true 3. Restart backend 4. Verify second enrollment succeeds or falls back gracefully |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
