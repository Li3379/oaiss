---
phase: 11
slug: frontend-coverage
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-05-16
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest with happy-dom |
| **Config file** | `oaiss-chain-frontend/vitest.config.ts` |
| **Quick run command** | `cd oaiss-chain-frontend && npm run test` |
| **Full suite command** | `cd oaiss-chain-frontend && npm run test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd oaiss-chain-frontend && npm run test`
- **After every plan wave:** Run `cd oaiss-chain-frontend && npm run build`
- **Before `/gsd-verify-work`:** Frontend builds with zero type errors + all tests green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | REQ-09 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | REQ-09 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 2 | REQ-10 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-02-02 | 02 | 2 | REQ-10 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-03-01 | 03 | 2 | REQ-10 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-03-02 | 03 | 2 | REQ-10 | — | N/A | unit | `cd oaiss-chain-frontend && npm run test` | ❌ W0 | ⬜ pending |
| 11-04-01 | 04 | 3 | REQ-11 | — | N/A | unit | `cd oaiss-chain-backend && mvn test` | ❌ W0 | ⬜ pending |
| 11-04-02 | 04 | 3 | REQ-11 | — | N/A | unit | `cd oaiss-chain-backend && mvn test` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `oaiss-chain-frontend/src/api/__tests__/` — stubs for API module tests covering REQ-09
- [ ] `oaiss-chain-frontend/src/views/__tests__/` — stubs for view component tests covering REQ-10
- [ ] `oaiss-chain-backend/src/test/.../SwaggerTagTest.java` — validation for unique @Tag annotations covering REQ-11

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Swagger UI renders correctly with unique tag groupings | REQ-11 | Visual verification of Swagger UI layout | Open Swagger UI, verify no merged tags |
| Enterprise views display all CRUD operations | REQ-10 | E2E verification with login flow | Login as enterprise, navigate all views |
| Reviewer views display all review operations | REQ-10 | E2E verification with login flow | Login as reviewer, navigate all views |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
