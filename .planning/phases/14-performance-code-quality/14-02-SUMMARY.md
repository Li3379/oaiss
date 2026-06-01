---
phase: 14-performance-code-quality
plan: 02
subsystem: security, database, code-quality
tags: [aes-256-gcm, encryption, csrf, transactional, soft-delete, flyway, jpa]

# Dependency graph
requires:
  - phase: 13-concurrency-credential-hardening
    provides: AesGcmEncryptor, repository AndDeletedFalse pattern, @Version on entities
provides:
  - AES-256-GCM encryption for RSA private keys at rest
  - AesGcmEncryptorTest with 6 test cases
  - RsaKeyMigrationRunner for encrypting existing plaintext keys on startup
  - V7 Flyway migration adding encrypted column to rsa_key_pair
  - ADR-001 documenting CSRF disabling rationale
  - readOnly=true on 25 read-only @Transactional methods
  - AndDeletedFalse on 6 repository query methods with all service/test call sites updated
affects: [15-devops-regression]

# Tech tracking
tech-stack:
  added: []
  patterns: [AES-256-GCM key wrapping for private key encryption at rest, @Transactional(readOnly=true) for read-only queries]

key-files:
  created:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/util/AesGcmEncryptorTest.java
    - oaiss-chain-backend/docs/adr/ADR-001-csrf-disabling.md
  modified:
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/RsaKeyPair.java
    - oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/RsaKeyPairRepository.java
    - oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DigitalSignatureServiceTest.java

key-decisions:
  - "RSA private keys encrypted at rest with AES-256-GCM using RSA_KEK environment variable"
  - "CSRF disabled for stateless JWT API with sessionStorage-based tokens (documented in ADR-001)"
  - "RsaKeyMigrationRunner encrypts existing plaintext keys on ApplicationReadyEvent"

patterns-established:
  - "AES-256-GCM key wrapping: random 12-byte IV per encryption, IV prepended to ciphertext, stored as Base64"
  - "@Transactional(readOnly=true) on all read-only service methods for database optimization"
  - "AndDeletedFalse suffix on all repository queries for soft-delete entities"

requirements-completed: [SEC-01, SEC-02, QUAL-01, QUAL-02]

# Metrics
duration: 21min
completed: 2026-05-20
---

# Phase 14 Plan 02: Security & Code Quality Summary

AES-256-GCM encryption for RSA private keys at rest, CSRF ADR, @Transactional(readOnly=true) on 25 methods, AndDeletedFalse on 6 repositories

## Performance

- **Duration:** 21 min
- **Started:** 2026-05-20T01:12:36Z
- **Completed:** 2026-05-20T01:33:00Z
- **Tasks:** 4
- **Files modified:** 17

## Accomplishments
- RSA private keys encrypted at rest with AES-256-GCM; existing plaintext keys migrated on startup
- CSRF disabling documented in ADR-001 with alternatives analysis
- 25 read-only @Transactional methods marked with readOnly=true for database optimization
- 6 repository query methods updated with AndDeletedFalse; all service and test call sites aligned

## Task Commits

Each task was committed atomically:

1. **Task 1: AES-256-GCM encryption for RSA private keys (SEC-01)** - `04b616f` + `3ace36f` (feat)
2. **Task 2: CSRF ADR document (SEC-02)** - `2e03de7` (docs)
3. **Task 3: readOnly=true on @Transactional methods (QUAL-01)** - `9690341` (perf)
4. **Task 4: AndDeletedFalse on repository queries (QUAL-02)** - `1b92ca0` (fix)

## Files Created/Modified
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/config/RsaKeyMigrationRunner.java` - Encrypts plaintext RSA private keys on ApplicationReadyEvent
- `oaiss-chain-backend/src/test/java/com/oaiss/chain/util/AesGcmEncryptorTest.java` - 6 tests: round-trip, IV uniqueness, wrong key, tampered ciphertext, blank KEK, empty string
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/entity/RsaKeyPair.java` - Added encrypted field (TINYINT, default 0)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/repository/RsaKeyPairRepository.java` - Added findByEncryptedAndDeletedFalse query
- `oaiss-chain-backend/src/test/java/com/oaiss/chain/service/DigitalSignatureServiceTest.java` - Added AesGcmEncryptor mock, fixed repository method stubs
- `oaiss-chain-backend/docs/adr/ADR-001-csrf-disabling.md` - ADR documenting CSRF disabling rationale
- `oaiss-chain-backend/src/main/resources/db/migration/V7__encrypt_rsa_private_keys.sql` - Adds encrypted column
- Service files with @Transactional(readOnly=true) and AndDeletedFalse call site fixes (CarbonCoinService, EnterpriseInferenceService, and corresponding test files)

## Decisions Made
- Used identity mock (pass-through) for AesGcmEncryptor in DigitalSignatureServiceTest since tests set plaintext keys directly; real encryption tested separately in AesGcmEncryptorTest
- RsaKeyMigrationRunner uses @Transactional on the entire migration method for atomicity

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed pre-existing compilation errors in service/test call sites**
- **Found during:** Task 1 (compilation required to run tests)
- **Issue:** Repository methods were renamed to add AndDeletedFalse in a prior plan, but service and test call sites still referenced old method names (CarbonCoinService.findByUserId, EnterpriseInferenceService.findByEnterpriseIdOrderByRatingYearDesc, etc.)
- **Fix:** Updated all service and test call sites to use the correct AndDeletedFalse method names
- **Files modified:** CarbonCoinService.java, EnterpriseInferenceService.java, CarbonCoinServiceTest.java, EmissionRatingServiceTest.java, ThirdPartyServiceTest.java, CarbonCoinAccountRepositoryTest.java, EnterpriseInferenceServiceTest.java
- **Verification:** `mvn compile` passes, all targeted tests pass
- **Committed in:** 3ace36f (part of Task 1 commit)

**2. [Rule 3 - Blocking] Fixed pre-existing test stubs using wrong repository methods**
- **Found during:** Task 1 (DigitalSignatureServiceTest failing with UnnecessaryStubbingException and key-not-found errors)
- **Issue:** Test stubs used findByUserIdAndDeletedFalse but service methods actually call findLatestByUserId
- **Fix:** Updated all test stubs from findByUserIdAndDeletedFalse to findLatestByUserId
- **Files modified:** DigitalSignatureServiceTest.java
- **Verification:** 27 tests pass with 0 failures
- **Committed in:** 3ace36f (part of Task 1 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both auto-fixes were necessary to unblock compilation and test execution. The fixes aligned with Task 4 scope (AndDeletedFalse) and corrected pre-existing test bugs.

## Issues Encountered
- Pre-existing AdminControllerTest failures (14 errors) due to missing AccountPermissionListRepository bean in test context -- unrelated to this plan, not fixed

## User Setup Required

**RSA_KEK environment variable is required.** Generate with: `openssl rand -base64 32`
Set in environment or application.yml before starting the application.

## Next Phase Readiness
- All 4 security and code quality requirements complete
- Phase 14 complete, ready for Phase 15 (DevOps & Regression)
- Pre-existing controller test failures in AdminControllerTest, CarbonControllerTest, etc. should be addressed in Phase 15

---
*Phase: 14-performance-code-quality*
*Completed: 2026-05-20*
