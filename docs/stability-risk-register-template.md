# OAISS CHAIN Stability Risk Register Template

Use this template after running `scripts/stability-baseline.sh` so that takeover findings are recorded in a consistent format.

## Session

- Date:
- Operator:
- Environment:
- Baseline command:
- Report directory:

## Status Summary

| Layer | Status | Notes |
| --- | --- | --- |
| Core required layer | `PASS` / `FAIL` / `PARTIAL` | Frontend, backend, MySQL, Redis, MinIO |
| Extension layer | `PASS` / `FAIL` / `PARTIAL` | ML, Fabric, chaincode |
| Delivery guard layer | `PASS` / `FAIL` / `PARTIAL` | Env validation, closure audit, release checks |

## Risks

| ID | Layer | Symptom | Impact | Evidence | Next action | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| R-001 | Core / Extension / Delivery |  |  |  |  |  |
| R-002 | Core / Extension / Delivery |  |  |  |  |  |
| R-003 | Core / Extension / Delivery |  |  |  |  |  |

## Follow-up Queue

1. Immediate blockers:
2. Near-term hardening:
3. Nice-to-have cleanup:

## Sign-off

- Ready for feature work:
- Ready for deeper regression:
- Ready for staging rehearsal:
