<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.entity

JPA entity classes mapping to database tables.

## Key Files (22 entities)

`User`, `Enterprise`, `CarbonReport`, `Transaction`, `AuctionOrder`, `CarbonCoinAccount`, `CarbonCoinTransaction`, `CarbonNeutralProject`, `CreditScore`, `CreditEvent`, `EmissionRating`, `MatchingResult`, `Authenticator`, `Reviewer`, `ReviewerQualification`, `ThirdPartyOrg`, `RsaKeyPair`, `OperationLog`, `UserTypeList`, `EntryPermission`, `AccountPermissionList` (all extend `BaseEntity`).

## For AI Agents

All entities extend `BaseEntity` (`created_at`, `updated_at` via JPA auditing). Use `@Entity` + `@Table` annotations. Lombok `@Data` for getters/setters. Never expose entities directly in API responses -- always map to DTOs.
