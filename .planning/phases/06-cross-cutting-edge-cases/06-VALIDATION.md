---
phase: 6
slug: cross-cutting-edge-cases
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-10
updated: 2026-05-16
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Bash + curl (API test scripts) |
| **Config file** | None — per-script configuration |
| **Quick run command** | `bash scripts/bugfix-test.sh` |
| **Full suite command** | `bash scripts/bugfix-test.sh && bash scripts/aop-test.sh && bash scripts/edge-test.sh` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run relevant script (bugfix-test.sh, aop-test.sh, or edge-test.sh)
- **After every plan wave:** Run full suite command
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | BUG-01 | T-06-03 | findLatestByUserId prevents NonUniqueResultException | integration | `bash scripts/bugfix-test.sh` | ✅ exists | ✅ green |
| 06-01-01 | 01 | 1 | BUG-02 | T-06-01 | Swagger requires authentication | integration | `bash scripts/bugfix-test.sh` | ✅ exists | ✅ green |
| 06-01-01 | 01 | 1 | BUG-03 | T-06-02 | CORS no localhost default | integration | `bash scripts/bugfix-test.sh` | ✅ exists | ✅ green |
| 06-01-02 | 01 | 1 | BUG-01~03 | — | All fixes verified via script | integration | `bash scripts/bugfix-test.sh` | ✅ exists | ✅ green |
| 06-02-01 | 02 | 2 | AOP-01 | — | AuditLog records to operation_log | integration | `bash scripts/aop-test.sh` | ✅ exists | ✅ green |
| 06-02-01 | 02 | 2 | AOP-02 | — | RateLimit blocks 4th request in window | integration | `bash scripts/aop-test.sh` | ✅ exists | ✅ green |
| 06-02-01 | 02 | 2 | AOP-03 | — | DataIsolation sets enterprise context | integration | `bash scripts/aop-test.sh` | ✅ exists | ✅ green |
| 06-02-01 | 02 | 2 | AOP-04 | — | DistributedLock serializes concurrent ops | integration | `bash scripts/aop-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-01 | — | Cross-role access returns 403/2004 | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-02 | — | State machine violations rejected | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-03 | — | Financial integrity maintained | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-04 | — | Pagination boundary handling | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-05 | — | Input validation rejects bad data | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |
| 06-03-01 | 03 | 3 | EDGE-06 | — | i18n language switching | integration | `bash scripts/edge-test.sh` | ✅ exists | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `scripts/bugfix-test.sh` — created in Plan 06-01 Task 2
- [x] `scripts/aop-test.sh` — created in Plan 06-02 Task 1
- [x] `scripts/edge-test.sh` — created in Plan 06-03 Task 1

*All test scripts are created as part of this phase's implementation. No pre-existing infrastructure needed beyond bash + curl + mysql CLI.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AOP annotation revert verification | AOP-01~04 | Need to verify modified files are restored after AOP tests | Run `git diff` after aop-test.sh to confirm no lingering annotations |
| i18n frontend visual check | EDGE-06 | Browser rendering required for visual language switch | Open frontend, switch language in settings, verify UI text changes |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** verified 2026-05-16

## Validation Audit 2026-05-16

| Metric | Count |
|--------|-------|
| Requirements total | 14 |
| Automated (green) | 14 |
| Manual-only | 2 |
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |

All 14 requirements verified via 3 test scripts (60/60 tests passed across bugfix-test.sh, aop-test.sh, edge-test.sh). Two manual-only items remain: AOP annotation revert verification (git diff) and i18n frontend visual check (browser required).
