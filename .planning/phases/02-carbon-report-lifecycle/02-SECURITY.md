---
phase: 2
slug: carbon-report-lifecycle
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-16
---

# Phase 2 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Client->API | Untrusted input in report create (title, emissionData, attachments) crosses here | User-submitted form data (text, JSON) |
| API->MinIO | File upload from client through API to object storage | Binary file attachments |
| Client->API (review) | Reviewer-submitted reviewResult and reviewComment cross trust boundary | Review decision + comment text |
| API->Internal services | Cascading calls to CreditScoreService, EmissionRatingService, BlockchainService | Enterprise ID, emission totals |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-02-01 | Tampering | POST /carbon/reports | mitigate | @Valid on CarbonReportRequest: @NotBlank(title), @Size(max=200), @NotNull(reportType), @NotBlank(emissionData), @Size(max=20) on accountingPeriod | closed |
| T-02-02 | Elevation of privilege | GET /carbon/my-reports | mitigate | enterpriseId resolved from JWT via enterpriseRepository.findByUserId(currentUser.getUserId()), not from request param. UAT data isolation tests (CARB-13b/13c) confirm. | closed |
| T-02-03 | Information disclosure | GET /carbon/reports/{id} | accept | Detail endpoint has no @PreAuthorize restriction; any authenticated user can view any report. Accepted for Phase 2 — reports are non-sensitive operational data. | closed |
| T-02-04 | Elevation of privilege | POST /carbon/review | mitigate | @PreAuthorize("hasRole('REVIEWER')") on CarbonController. Cross-role test (CARB-13a) confirms HTTP 403 for ENTERPRISE role. | closed |
| T-02-05 | Tampering | reviewResult field | accept | Reviewer could send reviewResult=0 (DRAFT) or reviewResult=5 (ON_CHAIN). Accepted — real reviewers are trusted users. ReportStatusEnum guards prevent invalid state transitions. | closed |
| T-02-06 | Tampering | Cascading service calls | mitigate | All three calls (creditScoreService, emissionRatingService, blockchainService) execute within single @Transactional on reviewReport(). Any failure rolls back entire approval per D-03. | closed |
| T-02-07 | Repudiation | Report review | accept | Reviewer ID and timestamp recorded in report entity (reviewerId, reviewedAt). No digital signature on review action in Phase 2. | closed |
| T-02-08 | Denial of service | Cascading side effects | accept | Sequential synchronous calls (credit score + emission rating + blockchain) could slow response. Acceptable for single-reviewer workflow with low concurrent review volume. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-02-01 | T-02-03 | Reports are non-sensitive operational data. Detail endpoint open to any authenticated user for transparency. | Phase 2 PLAN | 2026-05-09 |
| AR-02-02 | T-02-05 | Real reviewers are trusted users with vetted credentials. ReportStatusEnum guards prevent critical state corruption. | Phase 2 PLAN | 2026-05-09 |
| AR-02-03 | T-02-07 | Reviewer identity captured via reviewerId field. Digital signature deferred to Phase 2+ (digital signature feature). | Phase 2 PLAN | 2026-05-10 |
| AR-02-04 | T-02-08 | Single-reviewer workflow with low concurrency. Sequential calls within @Transactional keep data consistent. | Phase 2 PLAN | 2026-05-10 |

*Accepted risks do not resurface in future audit runs.*

---

## Code Review Security Findings (Post-Implementation)

| Finding | Category | Status | Detail |
|---------|----------|--------|--------|
| CR-01 | Data integrity | FIXED | createReport() emission data parsing treated arrays as scalars → totalEmission always 0 on create. Fixed via shared parseEmissionTotals() method. |
| WR-02 | Secret exposure | FIXED | MySQL password leaked in process list via `-p` flag. Fixed with MYSQL_PWD environment variable. |
| WR-03 | Defense-in-depth | Mitigated | reviewReport() has no service-layer role check. Mitigated by @PreAuthorize on controller. |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-16 | 8 | 8 | 0 | Claude (gsd-secure-phase) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-05-16
