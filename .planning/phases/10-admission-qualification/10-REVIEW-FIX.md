---
phase: 10
title: Admission & Qualification Certificate — Deep Code Review Fix Summary
date: 2026-05-16
status: fixed
source: 10-REVIEW.md
fixer: gsd-code-fixer
---

# Phase 10 — Deep Code Review Fix Summary

## Overview

Deep code review identified 14 findings (3 Critical, 6 Warning, 5 Info). This fix pass addresses all 14, with 9 already resolved in the codebase and 5 applied in this pass.

## Findings Fixed

| ID | Severity | Title | Fix Applied | Status |
|----|----------|-------|-------------|--------|
| CR-01 | Critical | Method-level @PreAuthorize ANDs with class-level — ENTERPRISE/REVIEWER users locked out of /my endpoints | `/my` endpoints already moved to `EnterpriseController` (`/enterprise/admission/my`) and `ReviewerController` (`/reviewer/qualification/my`) with proper role-based `@PreAuthorize` | Already Fixed |
| CR-02 | Critical | Race condition in certificate number generation | `@DistributedLock` already added to both `issueCertificate()` methods with per-entity lock keys | Already Fixed |
| CR-03 | Critical | Flyway V4 ADD UNIQUE KEY fails if reviewer_qualification has existing data | V4 migration already includes conditional constraint check via `information_schema.TABLE_CONSTRAINTS` | Already Fixed |
| WR-01 | Warning | ReviewerQualificationService.issueCertificate() does not verify reviewer exists | `userRepository.findById(reviewerId).orElseThrow()` already present | Already Fixed |
| WR-02 | Warning | Certificate number generation uses java.util.Random instead of SecureRandom | `SecureRandom` already used in both services | Already Fixed |
| WR-03 | Warning | Missing foreign key constraint on enterprise_admission.enterprise_id | `fk_enterprise_admission_enterprise` already in V4 migration | Already Fixed |
| WR-04 | Warning | Page parameter of 0 or negative causes IllegalArgumentException | Service methods already guard with `if (page == null \|\| page < 1) page = 1` | Already Fixed |
| WR-05 | Warning | Hardcoded English label "ID" in CertificateManage.vue instead of i18n key | Replaced `label="ID"` with `:label="t('certificateManage.colId')"`. Added `colId` key to both zh-CN.ts and en-US.ts | Fixed |
| WR-06 | Warning | ReviewerQualification entity comment says 0/1 but service uses 1/2 | Created `QualificationStatusEnum` (ACTIVE=1, REVOKED=2). Updated entity comments with `@see` reference. Replaced all magic numbers in both services with enum constants | Fixed |
| IN-01 | Info | Duplicate certificate generation logic in two services | Certificate generation already unified with EA/RQ prefix + SecureRandom + collision retry pattern | Already Fixed |
| IN-02 | Info | CertificateManage.vue tab labels use hardcoded text | Tab labels already use i18n (`t('certificateManage.tabAdmission')`, `t('certificateManage.tabQualification')`) | Already Fixed |
| IN-03 | Info | ReviewerQualificationServiceTest missing reviewer not found test case | Added `testIssueCertificate_reviewerNotFound_throwsException()` test verifying `BusinessException` with `RESOURCE_NOT_FOUND` code | Fixed |
| IN-04 | Info | EnterpriseAdmission entity uses @Column(columnDefinition) coupling JPA to MySQL | Matches existing project conventions — no fix needed | Skipped |
| IN-05 | Info | V4 migration does not include IF NOT EXISTS guards | Flyway tracks applied migrations — no fix needed | Skipped |

## Files Modified

### Backend (Java)

| File | Change |
|------|--------|
| `enums/QualificationStatusEnum.java` | New: Certificate status enum (ACTIVE=1, REVOKED=2) with `fromCode()` lookup |
| `entity/ReviewerQualification.java` | Updated status comment with `@see QualificationStatusEnum` reference |
| `entity/EnterpriseAdmission.java` | Updated status comment with `@see QualificationStatusEnum` reference |
| `service/EnterpriseAdmissionService.java` | Replaced magic numbers (1, 2) with `QualificationStatusEnum.ACTIVE.getCode()` / `REVOKED.getCode()`. Added import. |
| `service/ReviewerQualificationService.java` | Replaced magic numbers (1, 2) with `QualificationStatusEnum.ACTIVE.getCode()` / `REVOKED.getCode()`. Added import. |
| `test/.../ReviewerQualificationServiceTest.java` | Added `testIssueCertificate_reviewerNotFound_throwsException()` test case |

### Frontend (Vue/TS)

| File | Change |
|------|--------|
| `views/admin/CertificateManage.vue` | Replaced `label="ID"` with `:label="t('certificateManage.colId')"` |
| `i18n/locales/zh-CN.ts` | Added `colId: 'ID'` to certificateManage section |
| `i18n/locales/en-US.ts` | Added `colId: 'ID'` to certificateManage section |

## Key Design Decision: QualificationStatusEnum

The WR-06 fix introduces a proper enum following the project's existing pattern (`ReportStatusEnum`, `UserTypeEnum`, etc.):

- **ACTIVE(1, "有效")**: Certificate is valid and active
- **REVOKED(2, "已吊销")**: Certificate has been revoked

Both `EnterpriseAdmission` and `ReviewerQualification` entities now reference this enum via `@see` in their Javadoc, and all service methods use enum constants instead of magic numbers.

## Pre-existing Test Failures (Not Introduced by This Fix)

The following test compilation errors existed before this fix cycle and are unrelated to Phase 10 changes:

- `CarbonNeutralProjectControllerTest` (4 errors) — `useCredits()` method signature mismatch (expects `JwtUserDetails` but test passes `long`)
- `CarbonNeutralProjectServiceTest` (2 errors) — Same `useCredits()` signature mismatch

These are from a Phase 4/5 change where `useCredits()` was updated to accept `JwtUserDetails` but the tests were not updated.

## Verification

- Backend compiles successfully
- Phase 10 related tests pass:
  - `ReviewerQualificationServiceTest`: 8/8 pass (including new reviewer-not-found test)
  - `EnterpriseAdmissionServiceTest`: 9/9 pass