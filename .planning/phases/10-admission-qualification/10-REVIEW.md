---
phase: 10-admission-qualification
reviewed: 2026-05-15T21:15:00Z
depth: deep
files_reviewed: 18
files_reviewed_list:
  - oaiss-chain-backend/src/main/resources/db/migration/V4__enterprise_admission.sql
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/EnterpriseAdmission.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/EnterpriseAdmissionRepository.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ReviewerQualificationService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/ReviewerQualificationRepository.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/EnterpriseAdmissionServiceTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/ReviewerQualificationServiceTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/AdminControllerAdmissionTest.java
  - oaiss-chain-frontend/src/views/admin/CertificateManage.vue
  - oaiss-chain-frontend/src/api/admin.ts
  - oaiss-chain-frontend/src/router/index.ts
  - oaiss-chain-frontend/src/config/menu.ts
  - oaiss-chain-frontend/src/i18n/locales/zh-CN.ts
  - oaiss-chain-frontend/src/i18n/locales/en-US.ts
  - oaiss-chain-frontend/src/views/enterprise/UserProfile.vue
  - oaiss-chain-frontend/src/views/auditor/AuditList.vue
findings:
  critical: 3
  warning: 6
  info: 5
  total: 14
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-05-15T21:15:00Z
**Depth:** deep
**Files Reviewed:** 18
**Status:** issues_found

## Summary

Reviewed all 18 files from Phase 10 (Admission & Qualification Certificate). The implementation adds enterprise admission certificates and reviewer qualification certificates with admin CRUD, self-service "my certificate" endpoints, and a Vue management page.

Three critical issues were found: (1) ENTERPRISE/REVIEWER role users calling endpoints under `/admin/` path are blocked by the class-level `@PreAuthorize("hasRole('ADMIN')")` -- method-level `@PreAuthorize` does NOT override class-level in Spring Security by default (it ANDs them); (2) race condition in certificate number generation without distributed locking; (3) Flyway migration will fail if `reviewer_qualification` table already has rows with NULL or duplicate `certificate_no`.

Six warnings include missing reviewer existence validation, insecure random for certificate numbers, missing foreign key constraint, zero/negative page parameter causing runtime crash, hardcoded English text in Vue template, and inconsistent status code semantics.

## Critical Issues

### CR-01: Method-level @PreAuthorize does NOT override class-level -- ENTERPRISE/REVIEWER users are permanently locked out of /my endpoints

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/AdminController.java`
**Issue:** The `AdminController` class has `@PreAuthorize("hasRole('ADMIN')")` at the class level. The new `/enterprise-admission/my` endpoint has method-level `@PreAuthorize("hasRole('ENTERPRISE')")` and `/reviewer-qualification/my` has `@PreAuthorize("hasRole('REVIEWER')")`. In Spring Security with `@EnableMethodSecurity`, by default method-level and class-level `@PreAuthorize` annotations are ANDed together, not overridden. This means the effective authorization for these endpoints is `hasRole('ADMIN') AND hasRole('ENTERPRISE')`, which will ALWAYS be false because a user cannot simultaneously hold both roles. ENTERPRISE and REVIEWER users will get 403 Forbidden when calling these endpoints.

**Fix:** One of the following approaches:

Option A -- Move the `/my` endpoints out of `AdminController` into dedicated controllers without class-level `@PreAuthorize`:

```java
@RestController
@RequestMapping("/api/v1/enterprise-admission")
public class EnterpriseAdmissionController {

    @GetMapping("/my")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<EnterpriseAdmission> getMyAdmission(@AuthenticationPrincipal JwtUserDetails userDetails) {
        // ...
    }
}
```

Option B -- If keeping them in `AdminController`, change the class-level annotation to `@PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE', 'REVIEWER')")` and tighten the admin-only methods individually. However, this weakens the security posture of the entire controller.

Option A is strongly recommended.

### CR-02: Race condition in certificate number generation -- concurrent requests can produce duplicate certificate numbers

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java:75-82` and `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ReviewerQualificationService.java:68-75`
**Issue:** Both `generateCertificateNo()` methods follow a check-then-insert pattern: they query for the max existing certificate number, then generate a new one by incrementing. Under concurrent requests, two threads can read the same max number and generate the same certificate number, leading to a unique constraint violation (if the DB has one) or silent data corruption (if it doesn't). The V4 migration does add a UNIQUE KEY on `certificate_no`, so the failure mode would be a 500 error to the user rather than data corruption, but it is still a bug.

**Fix:** Use the project's existing `@DistributedLock` annotation (documented in CLAUDE.md under Cross-Cutting Concerns) on the `issueCertificate` methods, or use a database sequence for certificate number generation:

```java
@DistributedLock(key = "'cert:enterprise:' + #enterpriseId")
public EnterpriseAdmission issueCertificate(Long enterpriseId) {
    // ...
}
```

Alternatively, use an atomic approach with a database sequence or `INSERT ... ON CONFLICT` retry.

### CR-03: Flyway V4 migration ADD UNIQUE KEY will fail if reviewer_qualification table already has rows with NULL certificate_no

**File:** `oaiss-chain-backend/src/main/resources/db/migration/V4__enterprise_admission.sql:7`
**Issue:** The migration includes `ALTER TABLE reviewer_qualification ADD UNIQUE KEY uk_certificate_no (certificate_no)`. If the `reviewer_qualification` table already has rows (e.g., from a V2 or V3 seed migration) where `certificate_no` is NULL, MySQL allows multiple NULLs in a UNIQUE column, but if there are rows with empty strings `''` or if there are existing duplicate values, the ALTER TABLE will fail, halting application startup. There is no safety check (`WHERE certificate_no IS NOT NULL` or conditional migration).

**Fix:** Add a safety check before the ALTER TABLE:

```sql
-- Delete or fix any duplicate/empty certificate_no values before adding constraint
DELETE FROM reviewer_qualification WHERE certificate_no = '' AND is_deleted = 1;

-- Or use a conditional approach
SET @exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviewer_qualification'
    AND INDEX_NAME = 'uk_certificate_no');

SET @sql = IF(@exists = 0,
    'ALTER TABLE reviewer_qualification ADD UNIQUE KEY uk_certificate_no (certificate_no)',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

Also consider whether `certificate_no` should be `NOT NULL` for the `reviewer_qualification` table, since it is required once a certificate is issued. If there are rows without certificates, a partial unique index or a conditional approach is needed.

## Warnings

### WR-01: ReviewerQualificationService.issueCertificate() does not verify reviewer exists -- will throw NullPointerException

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ReviewerQualificationService.java:42-50`
**Issue:** Unlike `EnterpriseAdmissionService.issueCertificate()` which calls `enterpriseRepository.findById(enterpriseId).orElseThrow(...)`, the `ReviewerQualificationService.issueCertificate()` method directly calls `reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(reviewerId)` and never checks whether the reviewer (User entity) actually exists. If an invalid `reviewerId` is passed, no error is thrown -- the method just proceeds to create a certificate for a non-existent reviewer. This is inconsistent with the EnterpriseAdmission pattern and could lead to orphaned certificate records.

**Fix:**

```java
public ReviewerQualification issueCertificate(Long reviewerId) {
    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    // ... proceed with existing logic
}
```

### WR-02: Certificate number generation uses java.util.Random instead of SecureRandom

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java:75` and `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ReviewerQualificationService.java:68`
**Issue:** Both services use `new Random().nextInt(9000) + 1000` to generate a 4-digit random suffix for certificate numbers. `java.util.Random` is not cryptographically secure and its output is predictable if the seed can be inferred. Certificate numbers are sensitive identifiers -- an attacker could predict future certificate numbers and potentially forge or pre-generate them.

**Fix:**

```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

private String generateCertificateNo(String prefix) {
    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String seqPart = String.format("%04d", SECURE_RANDOM.nextInt(9000) + 1000);
    return prefix + datePart + seqPart;
}
```

### WR-03: Missing foreign key constraint on enterprise_admission.enterprise_id

**File:** `oaiss-chain-backend/src/main/resources/db/migration/V4__enterprise_admission.sql:3`
**Issue:** The `enterprise_admission` table has an `enterprise_id BIGINT` column but no `FOREIGN KEY ... REFERENCES enterprise(id)` constraint. This allows inserting admission records for non-existent enterprises, leading to data integrity violations. The existing V1 schema uses foreign keys for other tables (e.g., `carbon_report` references `enterprise`), so this is inconsistent with project conventions.

**Fix:**

```sql
ALTER TABLE enterprise_admission
    ADD CONSTRAINT fk_enterprise_admission_enterprise
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id);
```

### WR-04: Page parameter of 0 or negative causes IllegalArgumentException in PageRequest.of()

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java:30` and `ReviewerQualificationService.java:25`
**Issue:** Both `listCertificates` methods use `PageRequest.of(page - 1, size)`. The controller declares `@RequestParam(defaultValue = "1") int page`, but does not validate that `page >= 1`. If a client sends `page=0` or `page=-1`, the result is `PageRequest.of(-1, ...)` or `PageRequest.of(-2, ...)`, which throws `IllegalArgumentException` at runtime, returning a 500 error.

**Fix:**

```java
@GetMapping("/enterprise-admission")
public ApiResponse<Page<EnterpriseAdmission>> listEnterpriseAdmissions(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    // ...
}
```

Add `@Validated` to the controller class and use `javax.validation.constraints.Min`/`@Max`.

### WR-05: Hardcoded English label in CertificateManage.vue instead of i18n key

**File:** `oaiss-chain-frontend/src/views/admin/CertificateManage.vue:225`
**Issue:** The table column for "Qualification Type" uses a hardcoded English string `"Qualification Type"` as the column label instead of an i18n translation key like `{{ $t('certificateManage.qualificationType') }}`. This breaks the bilingual (zh-CN / en-US) support that the project requires.

**Fix:**

```vue
<el-table-column :label="$t('certificateManage.qualificationType')" prop="qualificationType" />
```

And add the corresponding keys to both `zh-CN.ts` and `en-US.ts`.

### WR-06: ReviewerQualification status code inconsistency -- entity comment says 0/1 but service uses 2 for REVOKED

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/ReviewerQualification.java:20-22` and `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/ReviewerQualificationService.java:58`
**Issue:** The entity class documents status as "0-Invalid, 1-Valid", but the `revokeCertificate()` method sets `status = 2` for "REVOKED". This is inconsistent with the entity documentation and there is no enum constant for status value 2. If another developer reads the entity comment and assumes only 0/1 are valid values, they may write incorrect filtering logic.

**Fix:** Create a proper enum (like the project's existing `UserTypeEnum` pattern) for qualification status:

```java
public enum QualificationStatus {
    INVALID(0, "Invalid"),
    VALID(1, "Valid"),
    REVOKED(2, "Revoked");

    private final int code;
    private final String description;
}
```

And use it in both the entity and service.

## Info

### IN-01: EnterpriseAdmissionService and ReviewerQualificationService duplicate certificate generation logic

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/EnterpriseAdmissionService.java` and `ReviewerQualificationService.java`
**Issue:** Both services contain identical `generateCertificateNo()` logic with the same `Random().nextInt(9000) + 1000` pattern. This violates DRY and increases the risk of inconsistent changes (e.g., fixing the random generation in one service but not the other).

**Fix:** Extract a shared `CertificateNumberGenerator` utility class.

### IN-02: CertificateManage.vue tab labels use hardcoded Chinese text

**File:** `oaiss-chain-frontend/src/views/admin/CertificateManage.vue:15-18`
**Issue:** The tab labels `"Enterprise Admission"` and `"Reviewer Qualification"` (or their Chinese equivalents) appear to be hardcoded strings in the template rather than using `$t()` i18n keys. This is inconsistent with the project's i18n requirements.

**Fix:** Use `$t('certificateManage.tabEnterpriseAdmission')` and `$t('certificateManage.tabReviewerQualification')`.

### IN-03: ReviewerQualificationServiceTest missing "reviewer not found" test case

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/ReviewerQualificationServiceTest.java`
**Issue:** Since the service does not validate reviewer existence (see WR-01), the test also has no test case for the "reviewer not found" scenario. When WR-01 is fixed by adding existence validation, a corresponding test case should be added.

**Fix:** Add test after fixing WR-01:

```java
@Test
void issueCertificate_reviewerNotFound_throwsException() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.issueCertificate(999L));
}
```

### IN-04: EnterpriseAdmission entity uses @Column(columnDefinition) which couples JPA to MySQL dialect

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/EnterpriseAdmission.java:25`
**Issue:** The entity uses `@Column(columnDefinition = "TEXT")` for the `qualificationScope` field. While this works with MySQL, it couples the JPA mapping to the MySQL dialect. If the project ever needs to support a different database, this will break. This is consistent with other entities in the project, so it is only an observation.

**Fix:** No immediate fix needed -- this matches existing project conventions. Consider using `@Lob` annotation for better portability if database abstraction is desired in the future.

### IN-05: V4 migration does not include IF NOT EXISTS guards for table creation

**File:** `oaiss-chain-backend/src/main/resources/db/migration/V4__enterprise_admission.sql`
**Issue:** The `CREATE TABLE enterprise_admission` statement does not use `IF NOT EXISTS`. While Flyway tracks applied migrations and won't re-run V4, this is a defensive programming observation. If the migration is ever run manually (e.g., during debugging), it will fail on re-execution.

**Fix:** Consider adding `IF NOT EXISTS` as a safety measure, though this is low priority since Flyway handles migration tracking.

---

## Top 3 Most Important Issues to Fix

1. **CR-01** -- ENTERPRISE/REVIEWER users are permanently locked out of `/my` endpoints because method-level `@PreAuthorize` ANDs with class-level `@PreAuthorize("hasRole('ADMIN')")`. This is a functional showstopper: the self-service certificate lookup feature simply does not work.

2. **CR-02** -- Race condition in certificate number generation. Under concurrent load, duplicate certificate numbers will be generated, causing either 500 errors (if the DB UNIQUE constraint catches it) or data corruption (if it doesn't).

3. **CR-03** -- Flyway migration will fail if `reviewer_qualification` already has data with problematic `certificate_no` values, preventing application startup in environments with existing data.

## Overall Assessment

**FAIL** -- The implementation has a critical authorization bug (CR-01) that renders the self-service certificate endpoints completely non-functional. This must be fixed before shipping. The race condition (CR-02) and migration safety issue (CR-03) are also blocking. The warnings should be addressed in the same pass to avoid accumulating technical debt in a feature that just shipped.

---

_Reviewed: 2026-05-15T21:15:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
