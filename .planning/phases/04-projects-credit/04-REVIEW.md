---
phase: 04-projects-credit
reviewed: 2026-05-16T03:46:00Z
depth: deep
files_reviewed: 24
files_reviewed_list:
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CreditScoreController.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CarbonNeutralProject.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CreditScore.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CarbonNeutralProjectRequest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CarbonNeutralProjectResponse.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CreditScoreResponse.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/ProjectVerificationRequest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CreditDeductionRequest.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/CarbonNeutralProjectRepository.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/CreditScoreRepository.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/CreditEventRepository.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/EnterpriseRepository.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/CreditLevelEnum.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/CreditEventTypeEnum.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/UserTypeEnum.java
  - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/CreditEvent.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/CarbonNeutralProjectControllerTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/controller/CreditScoreControllerTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CarbonNeutralProjectServiceTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CreditScoreServiceTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/repository/CreditScoreRepositoryTest.java
  - oaiss-chain-backend/src/test/java/com/oaiss/chain/dto/CarbonNeutralProjectResponseTest.java
  - oaiss-chain-frontend/src/api/carbonNeutral.ts
  - oaiss-chain-frontend/src/api/credit.ts
  - oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue
  - oaiss-chain-frontend/src/views/enterprise/CreditScore.vue
  - oaiss-chain-frontend/src/views/enterprise/CompanyDashboard.vue
  - oaiss-chain-frontend/src/types/carbon-neutral.ts
  - oaiss-chain-frontend/src/types/credit.ts
  - oaiss-chain-frontend/src/types/api.ts
  - scripts/project-lifecycle-test.sh
  - scripts/credit-score-test.sh
findings:
  critical: 4
  warning: 8
  info: 4
  total: 16
status: issues_found
---

# Phase 4: Code Review Report

**Reviewed:** 2026-05-16T03:46:00Z
**Depth:** deep
**Files Reviewed:** 24 (backend: 18, frontend: 7, scripts: 2)
**Status:** issues_found

## Summary

Phase 4 implements the Carbon Neutral Project Lifecycle and Credit Score systems. The backend controller-to-service-to-repository call chains are structurally sound, with proper state machine transitions for projects and threshold-based credit score evaluation. However, this review identified 4 critical security and logic defects, 8 warnings, and 4 informational findings.

**Key concerns:**
1. The `useCredits` endpoint has zero authorization -- any authenticated user can spend any project's carbon credits.
2. The `addBonusPoints` method accepts negative values, allowing admin bonus to become a deduction.
3. The `deductPoints` endpoint accepts the BONUS_GOOD_BEHAVIOR event type (code=5), which adds +5 points instead of deducting -- a privilege escalation path for REVIEWER/ADMIN.
4. Frontend-backend type mismatches for `projectType` (string vs integer) and `status` (string vs integer) mean the CarbonNeutral.vue create form and status display are non-functional against the real backend.

## Critical Issues

### CR-01: `useCredits` endpoint has no authentication or authorization

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java:221-234`
**Issue:** The `POST /carbon-neutral/{id}/use-credits` endpoint has no `@AuthenticationPrincipal` and no `@PreAuthorize` annotation. Any authenticated user (or even unauthenticated if security config allows) can consume any project's carbon credits. This is a data integrity and authorization vulnerability.
**Fix:**
```java
@PostMapping("/{id}/use-credits")
@SecurityRequirement(name = "Bearer Authentication")
public ApiResponse<CarbonNeutralProjectResponse> useCredits(
        @AuthenticationPrincipal JwtUserDetails currentUser,
        @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
        @RequestBody Map<String, java.math.BigDecimal> body) {
    java.math.BigDecimal amount = body.get("amount");
    return ApiResponse.success(projectService.useCredits(currentUser, id, amount));
}
```
The service `useCredits` method should also accept `JwtUserDetails` and call `validateOwner(currentUser, project)` before allowing credit consumption.

### CR-02: `addBonusPoints` does not validate that points are positive

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java:179-216`
**Issue:** The `addBonusPoints` method accepts any `Integer points` without validation. An admin could accidentally (or maliciously) pass a negative value, which would deduct points instead of adding them. The `Math.min(100, scoreBefore + points)` on line 194 would correctly cap the result but would allow deductions without creating the proper audit trail type.
**Fix:**
```java
public CreditScoreResponse addBonusPoints(Long enterpriseId, Integer points,
                                            String description, Long triggeredBy) {
    if (points == null || points <= 0) {
        throw new IllegalArgumentException("奖励分数必须为正整数");
    }
    // ... rest of method
}
```

### CR-03: `deductPoints` endpoint accepts BONUS_GOOD_BEHAVIOR event type, enabling unauthorized score increases

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java:118-167`
**Issue:** The `deductPoints` method uses `CreditEventTypeEnum.fromCode(eventType)` to look up the event type, then applies `typeEnum.getDefaultPoints()`. If `eventType=5` (BONUS_GOOD_BEHAVIOR) is passed, `defaultPoints` is +5, and the method will ADD 5 points to the score via `scoreBefore + pointsToDeduct`. Since the `/credit/deduct` endpoint is accessible to both ADMIN and REVIEWER roles, any reviewer can grant bonus points to enterprises -- a privilege that should be ADMIN-only (per the `/credit/bonus` endpoint's `@PreAuthorize("hasRole('ADMIN')")`).
**Fix:**
```java
@Transactional
public CreditScoreResponse deductPoints(Long enterpriseId, Integer eventType,
                                          String description, Long triggeredBy,
                                          Long relatedReportId) {
    // ... existing code ...
    CreditEventTypeEnum typeEnum = CreditEventTypeEnum.fromCode(eventType);
    if (typeEnum == null) {
        throw new IllegalArgumentException("无效的事件类型: " + eventType);
    }
    if (typeEnum.getDefaultPoints() > 0) {
        throw new IllegalArgumentException("扣分接口不接受奖励类型事件，请使用 /credit/bonus 接口");
    }
    // ... rest of method
}
```

### CR-04: Frontend project type values are strings but backend expects integers -- create form is broken

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue:28-33`
**Issue:** The `projectTypeOptions` in CarbonNeutral.vue uses string values (`'ENERGY_SAVING'`, `'RENEWABLE_ENERGY'`, `'AFFORESTATION'`, `'CCUS'`, `'OTHER'`), but the backend `CarbonNeutralProjectRequest.projectType` is `@NotNull Integer` expecting 1-5. The `createProject` API call sends `projectType: 'ENERGY_SAVING'` which will fail Jackson deserialization with a type mismatch error, or be silently rejected. The create project dialog is non-functional against the real backend.
**Fix:**
```typescript
const projectTypeOptions = [
  { label: 'carbonNeutral.typeCarbonSink', value: 1 },
  { label: 'carbonNeutral.typeCCUS', value: 2 },
  { label: 'carbonNeutral.typeNewEnergy', value: 3 },
  { label: 'carbonNeutral.typeEnergySaving', value: 4 },
  { label: 'carbonNeutral.typeOther', value: 5 },
]
```

## Warnings

### WR-01: Frontend status display uses string keys but backend returns integer status codes

**File:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue:80-98`
**Issue:** The `getProjectStatusTag` and `getProjectStatusText` functions use string keys (`'PENDING'`, `'VERIFIED'`, `'REJECTED'`, `'COMPLETED'`), but the backend returns integer status codes (0=DRAFT, 1=PENDING, 2=APPROVED, 3=IMPLEMENTING, 4=COMPLETED, 5=TERMINATED, 6=REJECTED). The `map[status]` lookup will always return `undefined` for integer keys, causing all project statuses to display as `'info'` tag with the raw integer value as text.
**Fix:**
```typescript
const getProjectStatusTag = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',      // DRAFT
    1: 'warning',   // PENDING
    2: 'success',   // APPROVED
    3: 'primary',   // IMPLEMENTING
    4: 'success',   // COMPLETED
    5: 'danger',    // TERMINATED
    6: 'danger',    // REJECTED
  }
  return map[status] || 'info'
}

const getProjectStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: t('carbonNeutral.statusDraft'),
    1: t('carbonNeutral.statusPending'),
    2: t('carbonNeutral.statusApproved'),
    3: t('carbonNeutral.statusImplementing'),
    4: t('carbonNeutral.statusCompleted'),
    5: t('carbonNeutral.statusTerminated'),
    6: t('carbonNeutral.statusRejected'),
  }
  return map[status] || String(status)
}
```

### WR-02: Frontend credit score level type mapping does not match backend levels

**File:** `oaiss-chain-frontend/src/views/enterprise/CreditScore.vue:58-67`
**Issue:** The `getScoreLevelType` function maps `'AAA'`, `'AA'`, `'A'`, `'B'`, `'C'` to Element Plus tag types, but the backend `CreditLevelEnum` returns `'EXCELLENT'`, `'GOOD'`, `'WARNING'`, `'DANGER'`, `'FROZEN'`. The lookup `map[scoreData.level]` will always return `undefined`, causing the level tag to always display as `'info'` type regardless of actual level.
**Fix:**
```typescript
const getScoreLevelType = (level: string) => {
  const map: Record<string, string> = {
    'EXCELLENT': 'success',
    'GOOD': 'primary',
    'WARNING': 'warning',
    'DANGER': 'danger',
    'FROZEN': 'danger',
  }
  return map[level] || 'info'
}
```

### WR-03: `terminateProject` does not validate project status before termination

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java:374-388`
**Issue:** The `terminateProject` method sets status to STATUS_TERMINATED without checking the current status. A DRAFT, PENDING, REJECTED, COMPLETED, or already TERMINATED project can all be terminated. This breaks the state machine: DRAFT->TERMINATED should not be possible without going through PENDING first, and COMPLETED projects should not be terminable. Additionally, it overwrites `reviewComment` with the termination reason, destroying any previous review comment.
**Fix:**
```java
@Transactional
public CarbonNeutralProjectResponse terminateProject(JwtUserDetails currentUser, Long projectId,
                                                      String reason) {
    CarbonNeutralProject project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(3002, "项目不存在"));

    validateOwner(currentUser, project);

    if (project.getStatus() != STATUS_IMPLEMENTING && project.getStatus() != STATUS_APPROVED) {
        throw new BusinessException(3003, "只有实施中或审核通过的项目可以终止");
    }

    project.setStatus(STATUS_TERMINATED);
    project.setTerminationReason(reason); // Use a dedicated field instead of reviewComment
    project = projectRepository.save(project);

    log.info("Project terminated: {} - {}", project.getProjectNo(), reason);
    return toResponse(project);
}
```

### WR-04: Search endpoint has no `@PreAuthorize` -- any authenticated user can search all projects

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java:82-101`
**Issue:** The `GET /carbon-neutral/search` endpoint has no role restriction. Any authenticated user can search across all enterprises' projects. While `GET /carbon-neutral/my` correctly scopes to the current user's projects, the search endpoint exposes the entire project catalog. If this is intentional (e.g., for a marketplace), it should be documented. If not, it should be restricted.
**Fix:** Add appropriate authorization:
```java
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")
```
Or if enterprise users should also search:
```java
// No change needed, but document the intentional public access
```

### WR-05: `EnterpriseRepository.findByUserId` does not filter soft-deleted records

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/EnterpriseRepository.java:23`
**Issue:** The repository has `findByUserId(Long userId)` (no deleted filter) alongside `findByUserIdAndDeletedFalse(Long userId)`. The `CarbonNeutralProjectService` uses `findByUserId` (lines 88, 414, 442) which will return soft-deleted enterprises, allowing operations on deleted enterprise records. The `CreditScoreService` correctly uses `findByUserIdAndDeletedFalse` (lines 298, 318).
**Fix:** In `CarbonNeutralProjectService`, replace all calls to `enterpriseRepository.findByUserId(...)` with `enterpriseRepository.findByUserIdAndDeletedFalse(...)`:
```java
// Line 88, 414, 442 — change:
enterpriseRepository.findByUserId(currentUser.getUserId())
// To:
enterpriseRepository.findByUserIdAndDeletedFalse(currentUser.getUserId())
```

### WR-06: Project search JPQL query does not escape LIKE special characters

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/CarbonNeutralProjectRepository.java:27-35`
**Issue:** The `search` method uses `p.projectName LIKE %:keyword%` with a raw keyword parameter. If the keyword contains SQL LIKE special characters (`%`, `_`), the search will behave unexpectedly. For example, a keyword of `%` would match all records.
**Fix:** Escape the keyword in the service layer before passing to the repository:
```java
// In CarbonNeutralProjectService.searchProjects():
String escapedKeyword = keyword;
if (keyword != null) {
    escapedKeyword = keyword.replace("%", "\\%").replace("_", "\\_");
}
```
Or use a Criteria API approach with proper escaping.

### WR-07: `CarbonNeutralProjectService.toResponse` has N+1 query pattern

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java:469-531`
**Issue:** The `toResponse` method makes up to 3 database queries per project (enterprise lookup, reviewer lookup, verifier lookup). When called in a loop via `projects.map(this::toResponse)` (lines 406, 425, 436), this creates an N+1 query problem. For a page of 20 projects, this could mean 60+ additional queries.
**Fix:** This is a warning rather than critical because the page size is typically small (20). For production scale, consider:
1. Using `@EntityGraph` or a custom JPQL query with JOIN FETCH
2. Batch-loading enterprise and user names in a single query
3. Caching enterprise names (they rarely change)

### WR-08: `CreditDeductionRequest` lacks validation annotations

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CreditDeductionRequest.java`
**Issue:** The `CreditDeductionRequest` DTO has no `@NotNull`, `@NotBlank`, or other Bean Validation annotations. The controller method `deductPoints` accepts it with `@RequestBody` but does not use `@Valid`. This means `enterpriseId` could be null, `eventType` could be null, and the request would still be processed until it hits a runtime error in the service layer.
**Fix:**
```java
@Data
public class CreditDeductionRequest {
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @NotNull(message = "事件类型不能为空")
    private Integer eventType;

    @Size(max = 500, message = "描述不能超过500字符")
    private String description;

    private Long relatedReportId;
}
```
And add `@Valid` to the controller parameter:
```java
public ApiResponse<CreditScoreResponse> deductPoints(
        @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
        @Valid @RequestBody CreditDeductionRequest request) {
```

## Info

### IN-01: Duplicate endpoint `listProjects` identical to `search`

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java:103-122`
**Issue:** The `GET /carbon-neutral/projects` endpoint is an exact duplicate of `GET /carbon-neutral/search` -- same parameters, same service call, same response. This is dead code that increases maintenance surface without adding functionality.
**Fix:** Remove the `/projects` endpoint or deprecate it with `@Deprecated` if it exists for backward compatibility.

### IN-02: Test coverage gap for negative bonus points and BONUS event type via deduct endpoint

**File:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CreditScoreServiceTest.java`
**Issue:** No test verifies that `addBonusPoints` rejects negative point values, and no test verifies that `deductPoints` rejects the BONUS_GOOD_BEHAVIOR event type. These are the exact bugs found in CR-02 and CR-03.
**Fix:** Add tests:
```java
@Test
@DisplayName("加分 - 负数分数抛出异常")
void addBonusPoints_NegativePoints_ShouldThrow() {
    assertThrows(IllegalArgumentException.class,
        () -> creditScoreService.addBonusPoints(1L, -10, "无效", 1L));
}

@Test
@DisplayName("扣分 - 奖励类型不可用于扣分接口")
void deductPoints_BonusEventType_ShouldThrow() {
    when(creditScoreRepository.findByEnterpriseIdAndDeletedFalse(1L))
        .thenReturn(Optional.of(testCreditScore));
    assertThrows(IllegalArgumentException.class,
        () -> creditScoreService.deductPoints(1L,
            CreditEventTypeEnum.BONUS_GOOD_BEHAVIOR.getCode(), "无效", 1L, null));
}
```

### IN-03: Read-only methods annotated with `@Transactional`

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java:91-106`
**Issue:** The `getScore` method (line 91) is not annotated with `@Transactional` but contains a `save()` call in the `orElseGet` lambda. This means the save happens outside a transaction, which could cause data inconsistency in concurrent scenarios. Other read-like methods (`getCreditHistory`, `getScoreRanking`, `getRestrictedEnterprises`, `getFrozenEnterprises`, `checkTradePermission`) correctly omit `@Transactional` since they are pure reads.
**Fix:** Add `@Transactional` to `getScore` since it may perform a write:
```java
@Transactional
public CreditScoreResponse getScore(Long enterpriseId) {
    // ... existing code
}
```

### IN-04: `THIRD_PARTY` role defined in `UserTypeEnum` but never used in any `@PreAuthorize`

**File:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/UserTypeEnum.java:16`
**Issue:** The `THIRD_PARTY` role (code=3) is defined but never appears in any `@PreAuthorize` annotation across the controllers reviewed. The `CarbonNeutralProjectController` verification endpoints use `hasRole('REVIEWER') or hasRole('ADMIN')` instead of including `THIRD_PARTY`. If third-party regulators should be able to verify/certify projects (as the domain model suggests), this is a functional gap. If not, the unused enum value is dead code.
**Fix:** Either add `THIRD_PARTY` to relevant `@PreAuthorize` annotations:
```java
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN', 'THIRD_PARTY')")
```
Or document that `THIRD_PARTY` is reserved for future use.

---

_Reviewed: 2026-05-16T03:46:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
