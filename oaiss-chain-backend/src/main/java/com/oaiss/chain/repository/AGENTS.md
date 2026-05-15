<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.repository

Spring Data JPA repository interfaces for database access.

## Key Files (21 repositories)

`UserRepository`, `EnterpriseRepository`, `CarbonReportRepository`, `TransactionRepository`, `AuctionOrderRepository`, `CarbonCoinAccountRepository`, `CarbonCoinTransactionRepository`, `CarbonNeutralProjectRepository`, `CreditScoreRepository`, `CreditEventRepository`, `EmissionRatingRepository`, `MatchingResultRepository`, `AuthenticatorRepository`, `ReviewerRepository`, `ReviewerQualificationRepository`, `ThirdPartyOrgRepository`, `RsaKeyPairRepository`, `OperationLogRepository`, `UserTypeListRepository`, `EntryPermissionRepository`, `AccountPermissionListRepository`

## For AI Agents

Extend `JpaRepository<Entity, Long>`. Use `@Query` for custom queries. Use Spring Data method naming conventions (`findBy...`, `countBy...`). For complex queries, use `@Query` with JPQL.
