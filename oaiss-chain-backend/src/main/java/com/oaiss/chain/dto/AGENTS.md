<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.dto

Data Transfer Objects for API request/response payloads.

## Key Files (36 DTOs)

`ApiResponse.java` (generic envelope), `LoginRequest`/`Response`, `RegisterRequest`, `CarbonReportRequest`/`Response`, `TradeRequest`/`Response`, `AuctionOrderRequest`/`Response`, `CarbonCoinAccountResponse`, `CreditScoreResponse`, `PageRequest`/`PageResponse`, and more.

## For AI Agents

Use Lombok `@Data`/`@Builder`. Keep DTOs flat -- no nested entities. Use validation annotations (`@NotNull`, `@Size`, etc.) on request DTOs.
