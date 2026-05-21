<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.entity

JPA entity classes mapping to MySQL database tables. All concrete entities extend `BaseEntity`.

## Key Files (23 entities)

| File | Table | Domain | Description |
|------|-------|--------|-------------|
| `BaseEntity.java` | (abstract) | -- | Base class: `id`, `createdAt`, `updatedAt`, `deleted` (soft delete). `@MappedSuperclass` + JPA auditing. |
| `User.java` | `user` | Auth | System users. Fields: `username`, `password` (BCrypt, WRITE_ONLY), `phone`, `email`, `realName`, `userType` (1=ENTERPRISE, 2=REVIEWER, 3=THIRD_PARTY, 4=ADMIN), `status`, `allowedIps`, `lastLoginAt`, `lastLoginIp`. |
| `Enterprise.java` | `enterprise` | Enterprise | Enterprise profile linked to User via `userId`. Fields: `enterpriseName`, `creditCode` (USCC), `address`, `contactPerson`, `contactPhone`, `industry`, `scale`, `carbonQuota`, `carbonUsed`. |
| `Reviewer.java` | `reviewer` | Review | Reviewer profile linked to User via `userId`. Fields: `qualificationNo`, `level` (1-3), `organization`, `reviewableIndustries` (JSON), `completedReviews`, `status`. |
| `Authenticator.java` | `authenticator` | Review | Certification authority. Fields: `userId`, `orgName`, `orgCode`, `address`, `contactPerson`, `contactPhone`, `certScope` (JSON), `status`. |
| `ThirdPartyOrg.java` | `third_party_org` | Supervision | Third-party regulatory org linked to User via `userId`. Fields: `orgName`, `orgCode`, `orgType` (1-4), `supervisionScope` (JSON), `accessLevel` (1-3), `status`. |
| `CarbonReport.java` | `carbon_report` | Carbon | Carbon emission reports. Fields: `reportNo`, `enterpriseId`, `submitterId`, `accountingPeriod`, `title`, `reportType`, `emissionData` (JSON), `totalEmission`, `scope1/2/3Emission`, `calculationMethod`, `status`, `reviewerId`, `reviewComment`, `signatureData`, `blockchainTxHash`, `attachments`. |
| `Transaction.java` | `transaction` | Trade | Carbon trade records (auction + P2P + quota allocation). Fields: `tradeNo`, `tradeType` (1=auction, 2=P2P, 3=quota), `sellerId`, `buyerId`, `quantity`, `unitPrice`, `totalAmount`, `reportId`, `status` (0-4), `remark`, `blockchainTxHash`, `completedAt`. |
| `AuctionOrder.java` | `auction_order` | Trade | Double-auction buy/sell orders. Fields: `orderNo`, `userId`, `direction` (1=buy, 2=sell), `quantity`, `price`, `matchedQuantity`, `status` (0-3), `settlementPrice`, `matchedAt`. |
| `MatchingResult.java` | `matching_result` | Trade | Auction match results. Fields: `matchNo`, `buyOrderId`, `sellOrderId`, `buyerId`, `sellerId`, `matchedQuantity`, `settlementPrice`, `totalAmount`, `status` (0-2), `transactionId`, `settledAt`. |
| `CarbonCoinAccount.java` | `carbon_coin_account` | Coin | Carbon coin accounts. Fields: `userId`, `balance`, `totalRecharged`, `totalSpent`, `status`. |
| `CarbonCoinTransaction.java` | `carbon_coin_transaction` | Coin | Carbon coin transaction ledger. Fields: `txNo`, `userId`, `txType` (1-4), `amount`, `balanceBefore`, `balanceAfter`, `relatedQuota`, `relatedTradeId`, `counterpartId`, `remark`. |
| `CreditScore.java` | `credit_score` | Credit | Enterprise credit scores. Fields: `enterpriseId`, `score` (0-100, default 100), `level` (EXCELLENT/GOOD/WARNING/DANGER/FROZEN), `tradeRestricted`, `accountFrozen`, `lastEvaluatedAt`. |
| `CreditEvent.java` | `credit_event` | Credit | Credit score change audit log. Fields: `enterpriseId`, `eventType` (1-5), `eventDescription`, `pointsChanged`, `scoreBefore`, `scoreAfter`, `relatedReportId`, `relatedTradeId`, `triggeredBy`, `triggeredAt`. |
| `CarbonNeutralProject.java` | `carbon_neutral_project` | Project | Carbon reduction/sink projects. Fields: `projectNo`, `projectName`, `projectType` (1-5), `ownerId`, `description`, `location`, `expectedReduction`, `actualReduction`, `investmentAmount`, `startDate`, `endDate`, `status` (0-5), `certStatus`, `certOrg`, `methodology`, `accountingPeriod`, `issuedCredits`, `usedCredits`, `availableCredits`, `applicationData`, `attachments`. |
| `EmissionRating.java` | `emission_rating` | Rating | Annual enterprise emission rating. Fields: `enterpriseId`, `ratingYear`, `totalEmission`, `emissionIntensity`, `ratingLevel` (A-E), `ratingScore` (0-100), `percentileRank`, `reductionRatio`, `ratedBy`. |
| `RsaKeyPair.java` | `rsa_key_pair` | Signature | RSA key pairs for digital signatures. Fields: `userId`, `publicKey`, `privateKey` (sensitive, never expose in API), `keyStatus` (0-2), `expiresAt`, `keyVersion`, `keyUsage` (1-3). |
| `EnterpriseAdmission.java` | `enterprise_admission` | Admission | Enterprise admission certificates. Fields: `enterpriseId`, `certificateNo`, `issuedDate`, `expiryDate`, `status` (1=valid, 2=revoked). |
| `ReviewerQualification.java` | `reviewer_qualification` | Review | Reviewer qualification certificates. Fields: `reviewerId`, `qualificationType`, `certificateNo`, `issuingAuthority`, `issuedDate`, `expiryDate`, `status` (1=valid, 2=revoked). |
| `OperationLog.java` | `operation_log` | Audit | Operation audit log with indexes on `userId`, `createdAt`, `module`. Fields: `userId`, `username`, `userType`, `module`, `action`, `description`, `httpMethod`, `requestUrl`, `requestIp`, `executionTime`, `responseData`. |
| `AccountPermissionList.java` | `account_permission_list` | RBAC | Role permission definitions. Fields: `permissionName`, `permissionCode` (unique), `description`, `module`, `sortOrder`. |
| `EntryPermission.java` | `entry_permission` | RBAC | API entry access control per role. Fields: `userType`, `apiPath`, `httpMethod`, `allowed`. |
| `UserTypeList.java` | `user_type_list` | RBAC | System user type definitions. Fields: `typeCode` (ENTERPRISE/REVIEWER/THIRD_PARTY/ADMIN), `typeName`, `description`, `defaultRole`. |

## Entity Relationships

```
User (1) ──→ (1) Enterprise       via userId
User (1) ──→ (1) Reviewer         via userId
User (1) ──→ (1) ThirdPartyOrg    via userId
User (1) ──→ (1) Authenticator    via userId
User (1) ──→ (1) CarbonCoinAccount via userId
User (1) ──→ (*) RsaKeyPair       via userId

Enterprise (1) ──→ (1) CreditScore      via enterpriseId
Enterprise (1) ──→ (*) CreditEvent      via enterpriseId
Enterprise (1) ──→ (*) EmissionRating   via enterpriseId
Enterprise (1) ──→ (*) CarbonReport     via enterpriseId
Enterprise (1) ──→ (*) EnterpriseAdmission via enterpriseId
Enterprise (1) ──→ (*) CarbonNeutralProject via ownerId

CarbonReport (1) ──→ (*) Transaction  via reportId
AuctionOrder (buy) + AuctionOrder (sell) ──→ MatchingResult
MatchingResult (1) ──→ (0..1) Transaction  via transactionId
Reviewer (1) ──→ (*) ReviewerQualification via reviewerId
```

## For AI Agents

- **All entities extend `BaseEntity`** providing `id` (auto-increment), `createdAt`/`updatedAt` (JPA auditing), `deleted` (soft delete, 0=active, 1=deleted).
- Use `@Entity` + `@Table(name = "...")` + `@Getter`/`@Setter`/`@NoArgsConstructor`/`@AllArgsConstructor`/`@Builder` (Lombok).
- `BaseEntity` uses `@Data` (not `@Getter`/`@Setter`) because it is `@MappedSuperclass`.
- No JPA relationship annotations (`@ManyToOne`/`@OneToMany`) are used -- all references are via `Long` foreign key IDs for simplicity and performance.
- **Never expose entities directly in API responses** -- always map to DTOs in the `dto/` package.
- Repository queries must include `AndDeletedFalse` for soft-delete filtering.
- `User.password` is annotated `@JsonProperty(access = WRITE_ONLY)` -- never serialized in responses.
- `RsaKeyPair.privateKey` is sensitive -- never include in DTO responses (`RsaKeyPairResponse` omits it).
- `OperationLog` has database indexes on `user_id`, `created_at`, and `module` for query performance.
- Status fields use integer codes; see `enums/` package for corresponding enum classes (e.g., `ReportStatusEnum`, `TradeStatusEnum`, `QualificationStatusEnum`).
- `BigDecimal` is used for all monetary values and emission quantities with appropriate precision/scale.
- JSON-serialized fields (`emissionData`, `certScope`, `supervisionScope`, `reviewableIndustries`, `allowedIps`) are stored as `TEXT` columns.
