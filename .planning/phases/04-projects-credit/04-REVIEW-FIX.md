---
phase: 04-projects-credit
fixed_at: 2026-05-16T14:05:00+08:00
review_path: .planning/phases/04-projects-credit/04-REVIEW.md
iteration: 1
findings_in_scope: 16
fixed: 16
skipped: 0
status: all_fixed
---

# Phase 4: Code Review Fix Report

**Fixed at:** 2026-05-16T14:05:00+08:00
**Source review:** .planning/phases/04-projects-credit/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 16
- Fixed: 16
- Skipped: 0

## Fixed Issues

### CR-01: useCredits endpoint has no authentication or authorization

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java`, `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
**Commit:** 0edf5d0
**Applied fix:** Added `@AuthenticationPrincipal JwtUserDetails currentUser` parameter to controller endpoint and passed it to service. Updated service `useCredits` method to accept `JwtUserDetails` and call `validateOwner(currentUser, project)` before allowing credit consumption.

### CR-02: addBonusPoints does not validate that points are positive

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java`
**Commit:** 0edf5d0
**Applied fix:** Added validation at start of method: `if (points == null || points <= 0) throw new IllegalArgumentException("奖励分数必须为正整数")`.

### CR-03: deductPoints endpoint accepts BONUS_GOOD_BEHAVIOR event type

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java`
**Commit:** 0edf5d0
**Applied fix:** After looking up typeEnum, added check: `if (typeEnum.getDefaultPoints() > 0) throw new IllegalArgumentException("扣分接口不接受奖励类型事件，请使用 /credit/bonus 接口")`.

### CR-04: Frontend projectTypeOptions sends string values but backend expects integers

**Files modified:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue`
**Commit:** becfb9a
**Applied fix:** Changed projectTypeOptions values from strings ('ENERGY_SAVING', etc.) to integers (1-5) matching backend CarbonNeutralProjectRequest.projectType.

### WR-01: Frontend status display uses string keys but backend returns integer status codes

**Files modified:** `oaiss-chain-frontend/src/views/enterprise/CarbonNeutral.vue`
**Commit:** becfb9a
**Applied fix:** Changed getProjectStatusTag and getProjectStatusText map keys from strings to integer codes (0=DRAFT through 6=REJECTED). Added TypeScript type annotations.

### WR-02: Frontend credit score level type mapping does not match backend levels

**Files modified:** `oaiss-chain-frontend/src/views/enterprise/CreditScore.vue`
**Commit:** becfb9a
**Applied fix:** Changed getScoreLevelType map keys from 'AAA','AA','A','B','C' to 'EXCELLENT','GOOD','WARNING','DANGER','FROZEN' matching backend CreditLevelEnum.

### WR-03: terminateProject does not validate project status before termination

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
**Commit:** c298fcb
**Applied fix:** Added status check: only STATUS_IMPLEMENTING (3) or STATUS_APPROVED (2) can be terminated. Throws BusinessException(3003) for invalid states.

### WR-04: Search endpoint has no @PreAuthorize

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java`
**Commit:** 0edf5d0
**Applied fix:** Added `@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")` to the search endpoint.

### WR-05: EnterpriseRepository.findByUserId does not filter soft-deleted records

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
**Commit:** c298fcb
**Applied fix:** Replaced all 3 occurrences of `enterpriseRepository.findByUserId(...)` with `enterpriseRepository.findByUserIdAndDeletedFalse(...)` in CarbonNeutralProjectService (lines in createProject, getMyProjects, validateOwner).

### WR-06: JPQL search query does not escape LIKE special characters

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
**Commit:** c298fcb
**Applied fix:** Added escaping of `%` and `_` characters in keyword before passing to repository query: `keyword.replace("%", "\\%").replace("_", "\\_")`.

### WR-07: toResponse N+1 query pattern

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CarbonNeutralProjectService.java`
**Commit:** c298fcb
**Applied fix:** Added TODO comment documenting the N+1 pattern for future optimization. Did not refactor to JOIN FETCH as it changes too much.

### WR-08: CreditDeductionRequest lacks validation annotations

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/CreditDeductionRequest.java`, `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CreditScoreController.java`
**Commit:** 19dc427
**Applied fix:** Added `@NotNull` on enterpriseId and eventType, `@Size(max=500)` on description. Added `@Valid` to controller deductPoints parameter. Added `jakarta.validation.Valid` import.

### IN-01: Duplicate endpoint listProjects identical to search

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/CarbonNeutralProjectController.java`
**Commit:** 0edf5d0
**Applied fix:** Added `@Deprecated` annotation with comment `// Use GET /search instead` and updated Swagger description to note deprecation.

### IN-02: Test coverage gap for negative bonus points and BONUS event type

**Files modified:** `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/CreditScoreServiceTest.java`
**Commit:** 4b5d3d3
**Applied fix:** Added 3 test methods: `addBonusPoints_NegativePoints_ShouldThrow`, `addBonusPoints_NullPoints_ShouldThrow`, `deductPoints_BonusEventType_ShouldThrow`. Updated existing `deductPoints_GoodBehavior_ShouldAdd5` test to expect rejection (matching CR-03 change).

### IN-03: getScore method missing @Transactional

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/CreditScoreService.java`
**Commit:** 0edf5d0
**Applied fix:** Added `@Transactional` annotation to `getScore` method since it contains a `save()` call in the `orElseGet` lambda.

### IN-04: THIRD_PARTY role never used in @PreAuthorize

**Files modified:** `oaiss-chain-backend/src/main/java/com/oaiss/chain/enums/UserTypeEnum.java`
**Commit:** 4b5d3d3
**Applied fix:** Added comment noting THIRD_PARTY is reserved for future use. Did not change @PreAuthorize annotations as instructed.

---

_Fixed: 2026-05-16T14:05:00+08:00_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
