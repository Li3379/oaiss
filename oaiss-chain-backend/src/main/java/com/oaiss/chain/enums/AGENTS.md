<!-- Parent: ../AGENTS.md -->

# Enums Directory — OAISS Chain

> Domain enumerations that govern state machines, role assignments, and classification logic across the entire platform.
> Every status transition and category check depends on these enums.

Generated: 2026-05-19 | Updated: 2026-05-19

---

## Key Files

| File | Purpose | Key Constants |
|------|---------|--------------|
| `UserTypeEnum.java` | User role classification | `ENTERPRISE`, `REVIEWER`, `THIRD_PARTY`, `ADMIN` |
| `ReportStatusEnum.java` | Carbon report lifecycle states | `DRAFT`, `PENDING`, `APPROVED`, `REJECTED` |
| `TradeStatusEnum.java` | Trade transaction states | `PENDING`, `COMPLETED`, `CANCELLED` |
| `TradeTypeEnum.java` | Trade mechanism type | `P2P`, `AUCTION` |
| `AuctionOrderStatusEnum.java` | Double-auction order states | `PENDING`, `PARTIAL_FILLED`, `FILLED`, `CANCELLED` |
| `CreditLevelEnum.java` | Enterprise credit rating (A-best, E-worst) | `A`, `B`, `C`, `D`, `E` |
| `CreditEventTypeEnum.java` | Credit score change trigger events | `REPORT_SUBMIT`, `REPORT_APPROVE`, `REPORT_REJECT`, `TRADE_COMPLETE` |
| `MatchingStatusEnum.java` | P2P trade matching states | `PENDING`, `MATCHED`, `UNMATCHED`, `CANCELLED` |
| `QualificationStatusEnum.java` | Enterprise qualification review states | `PENDING`, `APPROVED`, `REJECTED` |

---

## Enum Details

### UserTypeEnum
Platform role partitioning. Drives `@PreAuthorize` access control on controllers and route guards on the frontend.
- `ENTERPRISE` — Carbon-emitting enterprises (data submitters)
- `REVIEWER` — Auditors who review/approve reports
- `THIRD_PARTY` — External regulatory monitors
- `ADMIN` — System administrators (includes auth management)

### ReportStatusEnum
State machine for carbon reports. Transition rules: `DRAFT -> PENDING -> APPROVED|REJECTED`. Rejected reports may be resubmitted (back to `PENDING`).

### TradeStatusEnum
Trade transaction lifecycle. `PENDING` trades are open; `COMPLETED` when both parties settle; `CANCELLED` by either party before settlement.

### TradeTypeEnum
Selects the trading mechanism: `P2P` for direct peer-to-peer negotiation; `AUCTION` for double-auction (blind bid/ask matching).

### AuctionOrderStatusEnum
Fine-grained order states within the double-auction subsystem. `PARTIAL_FILLED` indicates partial execution; order progresses to `FILLED` once fully matched.

### CreditLevelEnum
Five-tier credit rating. `A` = excellent (lowest risk), `E` = poor (highest risk). Computed by `CreditScoreService` based on emission data and behavior history.

### CreditEventTypeEnum
Events that trigger credit score recalculation. Each type maps to a score delta defined in `CreditScoreService`.

### MatchingStatusEnum
P2P trade matching engine states. `PENDING` = awaiting counterparty; `MATCHED` = pairing found; `UNMATCHED` = expired without match.

### QualificationStatusEnum
Enterprise qualification certification states. Only `APPROVED` enterprises can submit carbon reports and participate in trades.

---

## Cross-References

| Enum | Used By |
|------|---------|
| `UserTypeEnum` | `User` entity, `SecurityConfig`, `JwtAuthenticationFilter`, all controllers with `@PreAuthorize` |
| `ReportStatusEnum` | `CarbonReport` entity, `CarbonReportService`, `AuditService` |
| `TradeStatusEnum` | `Trade` entity, `TradeService`, `DoubleAuctionService` |
| `TradeTypeEnum` | `Trade` entity, `TradeController`, `DoubleAuctionController` |
| `AuctionOrderStatusEnum` | `AuctionOrder` entity, `DoubleAuctionService` |
| `CreditLevelEnum` | `CreditScore` entity, `CreditScoreService` |
| `CreditEventTypeEnum` | `CreditScoreService`, `CreditScoreController` |
| `MatchingStatusEnum` | `Trade` entity, `TradeService` (P2P matching engine) |
| `QualificationStatusEnum` | `Enterprise` entity, `EnterpriseService` |

---

## AI Agent Notes

- **Adding a new enum value**: Append to the existing enum class. Add a corresponding `fromCode()` case if the enum uses a code-based lookup. Update any `switch` statements in services that consume this enum.
- **Adding a new enum class**: Follow the existing pattern — implement a `code` field (int or String), a `fromCode()` static factory method, and a `getDescription()` method. Place the file in this directory.
- **State transitions**: Never mutate enum fields. All transition logic belongs in the service layer (`*Service.java`), not in the enum itself.
- **Null safety**: All `fromCode()` methods must handle unknown codes gracefully (return `null` or throw a dedicated exception). Several bugs have been fixed around this — see commit `3388d32`.
