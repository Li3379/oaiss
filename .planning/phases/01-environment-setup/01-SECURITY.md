---
phase: 01
slug: environment-setup
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-15
---

# Phase 01 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Host→Docker | Host machine to Docker containers | MySQL data, Redis cache, MinIO objects |
| Browser→Frontend | User browser to Vue dev server | HTML/JS/CSS assets, API proxied requests |
| Frontend→Backend | Vue app to Spring Boot API | JWT tokens, REST API payloads, carbon data |
| Backend→MySQL/Redis/MinIO | Spring Boot to infrastructure | Persistent data, session cache, file storage |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-01-01 | Information Disclosure | Swagger UI | accept | Dev-only; Swagger disabled in production profile | closed |
| T-01-02 | Tampering | JWT secret in .env | accept | Dev-only; .env in .gitignore, production uses vault | closed |
| T-01-03 | Information Disclosure | MinIO default credentials | accept | Dev-only; env var references with defaults for local dev | closed |
| T-01-04 | Spoofing | BCrypt hash reuse in seed data | accept | Testing convenience; production uses unique hashes | closed |
| T-01-05 | DoS | Health check endpoint (no rate limiting) | accept | Operator-only tool; not exposed externally | closed |
| T-01-06 | Tampering | Login brute force | accept | Rate limiting exists (not tested in Phase 1); production adds WAF | closed |
| T-01-07 | Information Disclosure | Token in sessionStorage | accept | Dev/testing only; production uses httpOnly cookies | closed |
| T-01-08 | Repudiation | Logout blacklist in Redis | mitigate | Verified: login-test.sh confirms token blacklisted after logout (Step 5 of each account test) | closed |
| T-01-09 | Spoofing | Same password for all seed accounts | accept | Testing convenience; production enforces password policy | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-01-01 | Dev environment only; Swagger disabled in prod profile | Phase 1 PLAN | 2026-05-15 |
| AR-02 | T-01-02 | .env gitignored; production uses secret manager | Phase 1 PLAN | 2026-05-15 |
| AR-03 | T-01-03 | Local dev only; credentials parameterized via env vars | Phase 1 PLAN | 2026-05-15 |
| AR-04 | T-01-04 | Seed data for testing; not used in production | Phase 1 PLAN | 2026-05-15 |
| AR-05 | T-01-05 | Internal operator tool; no external exposure | Phase 1 PLAN | 2026-05-15 |
| AR-06 | T-01-06 | Rate limiting infrastructure exists; full testing deferred | Phase 1 PLAN | 2026-05-15 |
| AR-07 | T-01-07 | Dev/testing only; production migrates to httpOnly cookies | Phase 1 PLAN | 2026-05-15 |
| AR-09 | T-01-09 | Test accounts; production enforces unique credentials | Phase 1 PLAN | 2026-05-15 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-15 | 9 | 9 | 0 | gsd-secure-phase |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-05-15
