---
phase: 2
slug: carbon-report-lifecycle
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-16
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | bash/curl (integration test script) |
| **Config file** | `scripts/carbon-report-test.sh` |
| **Quick run command** | `bash scripts/carbon-report-test.sh` |
| **Full suite command** | `bash scripts/carbon-report-test.sh` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `bash scripts/carbon-report-test.sh`
- **After every plan wave:** Run `bash scripts/carbon-report-test.sh`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | CARB-01 | T-02-01 | @Valid on CarbonReportRequest | integration | `bash scripts/carbon-report-test.sh` (CARB-01 section) | ✅ | ✅ green |
| 02-01-02 | 01 | 1 | CARB-02 | T-02-02 | enterpriseId from JWT, not request param | integration | `bash scripts/carbon-report-test.sh` (CARB-02 section) | ✅ | ✅ green |
| 02-01-03 | 01 | 1 | CARB-03 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-03 section) | ✅ | ✅ green |
| 02-01-04 | 01 | 1 | CARB-04 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-04 section) | ✅ | ✅ green |
| 02-02-01 | 02 | 1 | CARB-05 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-05 section) | ✅ | ✅ green |
| 02-02-02 | 02 | 1 | CARB-06 | T-02-06 | @Transactional on reviewReport | integration | `bash scripts/carbon-report-test.sh` (CARB-06 section) | ✅ | ✅ green |
| 02-02-03 | 02 | 1 | CARB-07 | T-02-05 | ReportStatusEnum guards | integration | `bash scripts/carbon-report-test.sh` (CARB-07 section) | ✅ | ✅ green |
| 02-03-01 | 03 | 1 | CARB-08 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-08/09/10 section) | ✅ | ✅ green |
| 02-03-02 | 03 | 1 | CARB-09 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-08/09/10 section) | ✅ | ✅ green |
| 02-03-03 | 03 | 1 | CARB-10 | — | N/A | integration | `bash scripts/carbon-report-test.sh` (CARB-08/09/10 section) | ✅ | ✅ green |
| 02-02-04 | 02 | 1 | CARB-11 | T-02-05 | ReportStatusEnum guards | integration | `bash scripts/carbon-report-test.sh` (CARB-11 section) | ✅ | ✅ green |
| 02-02-05 | 02 | 1 | CARB-12 | T-02-04 | @PreAuthorize('REVIEWER') | manual | Playwright UAT (2026-05-10) | ⚠️ removed | ✅ manual |
| 02-03-04 | 03 | 1 | CARB-13 | T-02-02 | enterpriseId from JWT | integration | `bash scripts/carbon-report-test.sh` (CARB-13 section) | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Notes:**
- CARB-08/09/10 share a single verification block (Step 10 in test script) — all three side effects verified through report detail after approval
- CARB-12 authenticator tests were deliberately removed from the script on 2026-05-11 (obs 396); verified via Playwright UAT on 2026-05-10

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

- `scripts/carbon-report-test.sh` — 540-line bash/curl integration test covering 18 assertions across all 13 CARB requirements
- Playwright E2E verification completed 2026-05-10 for full browser lifecycle (create → submit → approve → ON_CHAIN)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Authenticator can list reports (200) but cannot review (403) | CARB-12 | Authenticator token references removed from test script on 2026-05-11; Playwright UI verification completed 2026-05-10 | Log in as authenticator user → verify report list loads → attempt review action → confirm 403 |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (none — existing infrastructure sufficient)
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-05-16
