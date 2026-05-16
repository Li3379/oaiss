---
phase: 06-cross-cutting-edge-cases
slug: cross-cutting-edge-cases
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-16
---

# Phase 6 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Unauthenticated client -> Backend API | Swagger endpoints require authentication | API documentation access |
| Cross-origin browser -> Backend API | CORS restricted to configured origins | HTTP requests |
| Concurrent requests -> DigitalSignatureService | Race condition on keypair generation | RSA key pairs |
| Temporary code modification -> Running backend | AOP test annotations added/reverted | Controller source files |
| Redis state -> Rate limit verification | Rate limit keys cleaned up after tests | Redis keys |
| Unauthorized roles -> Protected endpoints | @PreAuthorize enforces role matrix | All API endpoints |
| Client input -> Backend validation | Input validation rejects malicious data | Form fields, query params |
| State machine transitions | Invalid transitions blocked at service layer | Entity status fields |
| Trade settlement -> Account balances | Financial integrity via balance conservation | Carbon coin accounts |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-06-01 | Information Disclosure | SecurityConfig swagger permitAll | mitigate | BUG-02 fix: changed to `.authenticated()` — verified by bugfix-test.sh Tests 9-10 | closed |
| T-06-02 | Tampering | SecurityConfig CORS default localhost | mitigate | BUG-03 fix: removed `@Value` default fallback — verified by bugfix-test.sh Tests 11-12 | closed |
| T-06-03 | Denial of Service | DigitalSignatureService NonUniqueResultException | mitigate | BUG-01 fix: `findLatestByUserId` with LIMIT 1 + `@DistributedLock` on generateKeyPair — verified by bugfix-test.sh Tests 1-8 | closed |
| T-06-04 | Tampering | Temporary annotation left in code | mitigate | aop-test.sh uses `git checkout` to revert ALL changes; trap ensures cleanup on failure — verified by git diff showing no residual changes | closed |
| T-06-05 | Denial of Service | Rate limit Redis key left after test | mitigate | aop-test.sh cleans up keys before AND after rate limit test section — verified by Redis key inspection | closed |
| T-06-06 | Spoofing | Role-based access control | mitigate | EDGE-01 tests verify `@PreAuthorize` blocks unauthorized roles (code 2004) — 10 cross-role access tests passed | closed |
| T-06-07 | Tampering | Input validation bypass | mitigate | EDGE-05 tests verify negative/XSS/SQL injection rejected without server crash — 7 input validation tests passed | closed |
| T-06-08 | Tampering | State machine bypass | mitigate | EDGE-02 tests verify illegal transitions rejected with proper error codes — 6 state machine violation tests passed | closed |
| T-06-09 | Tampering | Financial manipulation | mitigate | EDGE-03 tests verify carbon tradable balance conservation via DB queries — sum conserved (93000 == 93000) | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|

No accepted risks.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-16 | 9 | 9 | 0 | Claude (gsd-security-auditor) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-05-16
