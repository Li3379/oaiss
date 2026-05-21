<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.repository

Spring Data JPA repositories for data access. All extend `JpaRepository<Entity, Long>` and use the soft-delete pattern (`...AndDeletedFalse`) to filter out logically deleted records.

## Key Files (22 files)

| File | Entity | Key Custom Queries / Notes |
|------|--------|---------------------------|
| `UserRepository.java` | `User` | `findByUsernameAndDeletedFalse`, `findByEmailAndDeletedFalse`, `existsByUsernameAndDeletedFalse`, `existsByEmailAndDeletedFalse`. Supports login and duplicate-check flows. |
| `EnterpriseRepository.java` | `Enterprise` | `findByUserIdAndDeletedFalse`, `existsByUserIdAndDeletedFalse`. Links enterprise profile to user account. |
| `CarbonReportRepository.java` | `CarbonReport` | `findByEnterpriseIdAndDeletedFalse`, `findByEnterpriseIdAndStatusAndDeletedFalse`, `findByStatusAndDeletedFalse`, `countByEnterpriseIdAndDeletedFalse`. Core domain: report CRUD and status filtering. |
| `TradeRepository.java` | `Trade` | `findByBuyerIdAndDeletedFalse`, `findBySellerIdAndDeletedFalse`, `findByStatusAndDeletedFalse`. Supports P2P carbon trading. |
| `CarbonCoinAccountRepository.java` | `CarbonCoinAccount` | `findByEnterpriseIdAndDeletedFalse`, `existsByEnterpriseIdAndDeletedFalse`. Carbon coin wallet per enterprise. |
| `CarbonCoinTransactionRepository.java` | `CarbonCoinTransaction` | `findByAccountIdAndDeletedFalse`, `findByAccountIdAndTypeAndDeletedFalse`. Transaction history for carbon coin accounts. |
| `CreditScoreRepository.java` | `CreditScore` | `findByEnterpriseIdAndDeletedFalse`, `findTopByEnterpriseIdAndDeletedFalseOrderByCreatedAtDesc`. Latest score lookup for A-E rating. |
| `CarbonNeutralProjectRepository.java` | `CarbonNeutralProject` | `findByEnterpriseIdAndDeletedFalse`, `findByStatusAndDeletedFalse`. Carbon offset project management. |
| `DigitalSignatureRepository.java` | `DigitalSignature` | `findByReportIdAndDeletedFalse`, `existsByReportIdAndDeletedFalse`. RSA signature storage and verification. |
| `EmissionDataRepository.java` | `EmissionData` | `findByEnterpriseIdAndDeletedFalse`, `findByEnterpriseIdAndYearAndDeletedFalse`. Emission data per year for rating calculation. |
| `EmissionRatingRepository.java` | `EmissionRating` | `findByEnterpriseIdAndDeletedFalse`, `findByEnterpriseIdAndYearAndDeletedFalse`. Annual emission rating records. |
| `OperationLogRepository.java` | `OperationLog` | `findByUserIdAndDeletedFalse`, `findByEntityTypeAndEntityIdAndDeletedFalse`. Powers `@AuditLog` aspect query support. |
| `DoubleAuctionRepository.java` | `DoubleAuction` | `findByStatusAndDeletedFalse`, `findByCreatorIdAndDeletedFalse`. Double-auction market sessions. |
| `DoubleAuctionBidRepository.java` | `DoubleAuctionBid` | `findByAuctionIdAndDeletedFalse`, `findByAuctionIdAndBidTypeAndDeletedFalse`, `findTopByAuctionIdAndBidTypeAndDeletedFalseOrderByPriceDesc`. Bid ordering for clearing price calculation. |
| `BlockchainRecordRepository.java` | `BlockchainRecord` | `findByReportIdAndDeletedFalse`, `findByTransactionHashAndDeletedFalse`. On-chain record references. |
| `FileMetadataRepository.java` | `FileMetadata` | `findByReportIdAndDeletedFalse`, `findByObjectIdAndDeletedFalse`. MinIO file metadata tracking. |
| `NotificationRepository.java` | `Notification` | `findByUserIdAndReadStatusAndDeletedFalse`, `countByUserIdAndReadStatusAndDeletedFalse`. Unread notification badge count. |
| `ReviewCommentRepository.java` | `ReviewComment` | `findByReportIdAndDeletedFalse`, `findByReviewerIdAndDeletedFalse`. Auditor review feedback. |
| `EnterpriseAdmissionRepository.java` | `EnterpriseAdmission` | `findByEnterpriseIdAndDeletedFalse`, `findByStatusAndDeletedFalse`, `existsByEnterpriseIdAndStatusAndDeletedFalse`. Enterprise admission/approval workflow. |
| `ReviewerQualificationRepository.java` | `ReviewerQualification` | `findByReviewerIdAndDeletedFalse`, `findByStatusAndDeletedFalse`, `existsByReviewerIdAndIndustryAndDeletedFalse`. Reviewer industry qualification checks. |
| `EmissionFactorRepository.java` | `EmissionFactor` | `findByIndustryAndDeletedFalse`, `findByIndustryAndFuelTypeAndDeletedFalse`. Emission factor lookup by industry/fuel type. |
| `SystemConfigRepository.java` | `SystemConfig` | `findByConfigKeyAndDeletedFalse`, `existsByConfigKeyAndDeletedFalse`. Key-value system configuration store. |

## For AI Agents

### Working Instructions

1. **Soft-delete is mandatory**: Every custom query MUST include `AndDeletedFalse` in its predicate. Never write a repository query without it, or logically deleted records will leak. Spring Data's derived query method names automatically include the condition when `deleted` is a field parameter.
2. **All repositories use `Long` as ID type**: Every entity extends `BaseEntity` which uses `@Id @GeneratedValue Long id`. Do not use `Integer` or `UUID` in repository type parameters.
3. **No native queries**: All queries use Spring Data derived method names or `@Query` with JPQL. If you need a complex query, prefer `@Query("SELECT e FROM Entity e WHERE e.field = :val AND e.deleted = false")` over native SQL.
4. **Pagination support**: For list endpoints returning many records, use `Page<Entity> findBy...AndDeletedFalse(Pageable)` instead of `List<Entity>`. This matches the backend pagination pattern (0-based page, Spring Data `PageRequest`).
5. **Do not add `@Modifying` queries for soft delete**: Soft delete is handled at the service layer (`entity.setDeleted(true); repository.save(entity)`), not via bulk update queries. This ensures `@UpdatedAt` audit fields are populated.
6. **`existsBy...` for uniqueness checks**: Use `existsBy...AndDeletedFalse` for duplicate validation before create. Do not query and check `isEmpty()` -- `existsBy` is more efficient (translates to `SELECT COUNT` or `SELECT EXISTS`).
7. **Ordering in method names**: Use `OrderBy...Desc`/`OrderBy...Asc` suffixes for sorted queries (e.g., `findTopByEnterpriseIdAndDeletedFalseOrderByCreatedAtDesc` for latest credit score). Avoid adding `SORT` parameters when the method name already implies ordering.

### Common Patterns

```java
// Standard soft-delete-aware lookup
Optional<Entity> findByIdAndDeletedFalse(Long id);

// List by parent with soft-delete filter
List<Entity> findByParentIdAndDeletedFalse(Long parentId);

// Paginated list
Page<Entity> findByStatusAndDeletedFalse(Status status, Pageable pageable);

// Existence check (for validation)
boolean existsByUniqueFieldAndDeletedFalse(String uniqueField);

// Latest record lookup
Optional<Entity> findTopByEnterpriseIdAndDeletedFalseOrderByCreatedAtDesc(Long enterpriseId);
```

### Testing Notes

- Use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` + Testcontainers for integration tests.
- For unit tests of services, mock repositories with `@MockBean` or Mockito `@Mock`.
- When mocking `Page<Entity>` returns, use `PageImpl` with a list and `PageRequest.of(0, size)`.
- Always verify that service-layer soft-delete calls `entity.setDeleted(true)` + `save()` rather than `repository.deleteById()`.

### Adding a New Repository

1. Create `NewEntityRepository.java` extending `JpaRepository<NewEntity, Long>`.
2. Add `@Repository` annotation (optional but conventional in this project).
3. Add `findByIdAndDeletedFalse` as the minimum custom query.
4. Add any filter queries needed by the corresponding service, always ending with `AndDeletedFalse`.
5. No need to declare `findAll()` with delete filter -- the base `findAll()` is typically unused; services use paginated variants instead.
