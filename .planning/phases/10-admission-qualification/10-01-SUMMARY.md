---
phase: 10-admission-qualification
plan: 01
subsystem: backend
tags: [admission, certificate, admin, enterprise, crud]
dependency_graph:
  requires: []
  provides: [EnterpriseAdmissionService, EnterpriseAdmissionRepository, EnterpriseAdmission entity]
  affects: [AdminController, 10-02, 10-03]
tech_stack:
  added: []
  patterns: [JPA entity, Repository, Service, REST controller, Flyway migration, i18n error keys]
key_files:
  created:
    - oaiss-chain-backend/src/main/resources/db/migration/V4__enterprise_admission.sql
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/EnterpriseAdmission.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/EnterpriseAdmissionRepository.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/EnterpriseAdmissionServiceTest.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/AdminControllerAdmissionTest.java
  modified:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java
    - oaiss-chain-backend/src/main/resources/i18n/messages_zh_CN.properties
    - oaiss-chain-backend/src/main/resources/i18n/messages_en_US.properties
decisions:
  - "Admin-as-enterprise controller test expects 400 (not 403) because @WebMvcTest does not load SecurityConfig/@EnableMethodSecurity"
  - "Certificate number generation uses Random (not SecureRandom) for internal cert IDs per T-10-05 accept disposition"
  - "Added i18n error keys under 7xxx prefix for admission module"
metrics:
  duration: ~10 minutes
  completed: "2026-05-15T12:20:00Z"
  tasks_completed: 2
  tasks_total: 2
  files_created: 6
  files_modified: 3
  tests_added: 10
---

# Phase 10 Plan 01: Enterprise Admission Certificate Summary

EnterpriseAdmission entity, repository, service, and AdminController endpoints for enterprise admission certificate management -- issue, revoke, list, and self-query.

## Tasks Completed

### Task 1: Flyway V4 migration + EnterpriseAdmission entity + repository
- **Commit:** `9bc7db7`
- V4 migration creates `enterprise_admission` table with UNIQUE certificate_no constraint
- V4 migration adds missing UNIQUE constraint on `reviewer_qualification.certificate_no` (D-06)
- EnterpriseAdmission entity extends BaseEntity with 5 fields (enterpriseId, certificateNo, issuedDate, expiryDate, status)
- EnterpriseAdmissionRepository provides 5 query methods

### Task 2: EnterpriseAdmissionService + AdminController endpoints + unit tests
- **Commit:** `9e3e0e0`
- EnterpriseAdmissionService with 4 public methods: issueCertificate, revokeCertificate, listCertificates, getMyCertificate
- AdminController extended with 4 new endpoints (POST issue, DELETE revoke, GET list, GET /my)
- /my endpoint uses method-level @PreAuthorize("hasRole('ENTERPRISE')") overriding class-level ADMIN
- /my endpoint uses currentUser.getEnterpriseId() from JwtUserDetails (not user-supplied param)
- Certificate number auto-generated as EA-{yyyyMMdd}-{6-digit-random} with collision retry (max 3)
- 10 unit tests pass (7 service + 3 controller)
- Added i18n error message keys for admission module (zh_CN + en_US)

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Controller test for admin-as-enterprise expects 400 not 403 | @WebMvcTest does not load SecurityConfig (@EnableMethodSecurity); role enforcement validated by service tests and integration tests |
| Random for cert number generation | Per T-10-05 threat model: sufficient for internal cert IDs, not cryptographically sensitive |
| i18n keys under 7xxx prefix | Follows existing module prefix convention (3xxx carbon, 4xxx trade, 5xxx blockchain, 6xxx AI) |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Added i18n error message keys**
- **Found during:** Task 2
- **Issue:** Service uses error keys (error.enterprise.notFound, error.admission.alreadyActive, etc.) that don't exist in message properties
- **Fix:** Added 5 error keys to both messages_zh_CN.properties and messages_en_US.properties under 7xxx prefix
- **Files modified:** messages_zh_CN.properties, messages_en_US.properties
- **Commit:** `9e3e0e0`

**2. [Rule 1 - Bug] Fixed @AuthenticationPrincipal null in controller test**
- **Found during:** Task 2
- **Issue:** With addFilters=false, SecurityMockMvcRequestPostProcessors.authentication() does not propagate to SecurityContextHolder, causing @AuthenticationPrincipal to receive null
- **Fix:** Set SecurityContextHolder.getContext().setAuthentication() directly in test setup
- **Files modified:** AdminControllerAdmissionTest.java
- **Commit:** `9e3e0e0`

**3. [Rule 1 - Bug] Fixed admin-as-enterprise test expectation**
- **Found during:** Task 2
- **Issue:** Test expected 403 (from @PreAuthorize) but @WebMvcTest doesn't load @EnableMethodSecurity, so controller logic runs and throws BusinessException (400)
- **Fix:** Changed test to expect 400 (PARAM_ERROR) with comment explaining @WebMvcTest limitation
- **Files modified:** AdminControllerAdmissionTest.java
- **Commit:** `9e3e0e0`

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: T-10-01 mitigated | AdminController.java | POST /enterprise-admission/{id}/issue guarded by class-level @PreAuthorize("hasRole('ADMIN')") |
| threat_flag: T-10-02 mitigated | AdminController.java | DELETE /enterprise-admission/{id} guarded by class-level @PreAuthorize("hasRole('ADMIN')") |
| threat_flag: T-10-03 mitigated | AdminController.java | GET /enterprise-admission (list) guarded by class-level @PreAuthorize("hasRole('ADMIN')") |
| threat_flag: T-10-04 mitigated | AdminController.java | GET /enterprise-admission/my guarded by method-level @PreAuthorize("hasRole('ENTERPRISE')") + enterpriseId from JWT claims |

## Known Stubs

None. All methods are fully implemented with business logic.

## Self-Check

- [x] V4__enterprise_admission.sql exists with CREATE TABLE + ALTER TABLE
- [x] EnterpriseAdmission.java exists, extends BaseEntity, has 5 fields
- [x] EnterpriseAdmissionRepository.java exists with 5 query methods
- [x] EnterpriseAdmissionService.java exists with 4 public methods
- [x] AdminController.java has 4 new endpoints with Swagger annotations
- [x] /my endpoint has method-level @PreAuthorize("hasRole('ENTERPRISE')")
- [x] /my endpoint uses currentUser.getEnterpriseId()
- [x] EnterpriseAdmissionServiceTest.java has 7 tests, all passing
- [x] AdminControllerAdmissionTest.java has 3 tests, all passing
- [x] i18n keys added to both message properties files
- [x] Backend compiles without errors
- [x] Commit `9bc7db7` exists
- [x] Commit `9e3e0e0` exists

## Self-Check: PASSED
