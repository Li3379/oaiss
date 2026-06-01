# Plan 04-01 Summary: Carbon Neutral Project Lifecycle + Role Gap

**Status**: COMPLETE
**Requirements**: PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05
**Script**: `scripts/project-lifecycle-test.sh`

## Results

| Test | Requirement | Result |
|------|-------------|--------|
| PROJ-01 | Create project (DRAFT) | PASS - id=1, status=0 |
| PROJ-01 | View my projects list | PASS |
| PROJ-01 | View project detail | PASS - status=0 |
| PROJ-02 | Submit for review (DRAFT→PENDING) | PASS - status=1 |
| PROJ-03 | Approve project A (PENDING→APPROVED) | PASS - status=2 |
| PROJ-03 | Reject project B (PENDING→REJECTED) | PASS - status=6 |
| PROJ-04 | Start implementation (APPROVED→IMPLEMENTING) | PASS - status=3 |
| PROJ-04 | Terminate project (IMPLEMENTING→TERMINATED) | PASS - status=5 |
| PROJ-05 | Submit for verification | PASS - verificationStatus=1 |
| PROJ-05 | /verify as AUTHENTICATOR → 403 | PASS - VERIFIER role gap confirmed |
| PROJ-05 | /verify as ADMIN (workaround) | PASS - verificationStatus=2, issuedCredits=3000 |
| PROJ-05 | Apply for certification | PASS - certStatus=1 |
| PROJ-05 | /certify as AUTHENTICATOR → 403 | PASS - CERTIFIER role gap confirmed |
| PROJ-05 | /certify as ADMIN (workaround) | PASS - certStatus=2, certNo=CERT-TEST-001 |

**Total**: 17/17 passed, 0 failed

## VERIFIER/CERTIFIER Role Gap Evidence

**Confirmed**: `@PreAuthorize` annotations reference roles that don't exist in `UserTypeEnum`.

| Endpoint | @PreAuthorize | UserTypeEnum values | Result |
|----------|---------------|---------------------|--------|
| POST /carbon-neutral/verify | `hasRole('VERIFIER') or hasRole('ADMIN')` | ENTERPRISE, REVIEWER, THIRD_PARTY, ADMIN, AUTHENTICATOR | 403 for AUTHENTICATOR |
| POST /carbon-neutral/{id}/certify | `hasRole('CERTIFIER') or hasRole('ADMIN')` | Same as above | 403 for AUTHENTICATOR |
| GET /carbon-neutral/pending-verification | `hasRole('VERIFIER') or hasRole('ADMIN')` | Same as above | 403 for AUTHENTICATOR |

**Root cause**: Controller uses `VERIFIER` and `CERTIFIER` role names, but `UserTypeEnum` defines the role as `AUTHENTICATOR`. The JWT filter maps enum to Spring Security role `ROLE_AUTHENTICATOR`, not `ROLE_VERIFIER` or `ROLE_CERTIFIER`.

**Workaround**: ADMIN role can access all three endpoints. No code fix applied — documented as known issue for Phase 6.

## Project Status Transitions Verified

```
DRAFT(0) → PENDING(1) → APPROVED(2) → IMPLEMENTING(3) → TERMINATED(5)
                            ↓
                       REJECTED(6)

IMPLEMENTING(3) → verification_status=PENDING → verification_status=VERIFIED
cert_status=NONE → cert_status=PENDING → cert_status=CERTIFIED
```

## Files Created

- `scripts/project-lifecycle-test.sh` — 17 test assertions covering full lifecycle
