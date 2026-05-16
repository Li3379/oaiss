---
phase: 3
slug: trading-engine
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-16
---

# Phase 3 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Client->API (transfer) | Untrusted input in transfer request (counterpartId, amount) crosses here | CarbonCoinTransferRequest DTO |
| Client->API (trade) | Untrusted input in P2P trade creation (buyerId, sellerId, quantity, unitPrice) crosses here | TradeRequest DTO |
| Client->API (auction) | Untrusted input in auction order (price, quantity, orderType) crosses here | AuctionOrderRequest DTO |
| API->DB (coin balance) | Balance read-modify-write cycle must be atomic | carbon_coin_account table |
| API->DB (trade settlement) | Settlement updates multiple entities atomically (quotas, tradable, balances) | enterprise, transaction, matching_result tables |
| API->Internal services | Cascading calls to CreditScoreService, EmissionRatingService on trade completion | Enterprise ID, trade data |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-03-01 | Tampering | TradeController.createP2PTrade | mitigate | @PreAuthorize("hasRole('ENTERPRISE')") on controller. TradeService enforces `currentUser.getUserId().equals(sellerId)` — only the seller can initiate their own trade. Verified at TradeService.java:68-69. | closed |
| T-03-02 | Tampering | Balance/quota manipulation via concurrent requests | mitigate | All mutating methods annotated @Transactional: CarbonCoinService.transfer (line 137), TradeService.createP2PTrade/createAuctionOrder/confirmTrade/cancelTrade (lines 53/107/142/191), DoubleAuctionService.placeBuyOrder/placeSellOrder/executeMatching (lines 74/104/145). Single-DB transaction boundaries ensure atomicity. | closed |
| T-03-03 | Tampering | Negative or zero transfer/trade amounts | mitigate | CarbonCoinTransferRequest: @DecimalMin("0.01") on amount (line 21). TradeRequest: @Positive on quantity (line 43) and unitPrice (line 50). Controllers enforce @Valid on all request bodies. | closed |
| T-03-04 | Repudiation | Self-transfer or same-party trade | mitigate | CarbonCoinService.transfer: `userId.equals(request.getCounterpartId())` check (line 139). TradeService.createP2PTrade: `sellerId.equals(buyerId)` check with TradeException.samePartyError (line 63). | closed |
| T-03-05 | Tampering | Concurrent double-spend via simultaneous matching | accept | DoubleAuctionService.executeMatching uses `synchronized` keyword (line 146) — prevents concurrent matching within a single JVM instance. Does NOT protect against multi-instance deployment. Accepted for v1 (single-instance deployment). Horizontal concurrency deferred to v2 with distributed lock. | closed |
| T-03-06 | Information disclosure | IDOR on trade details | mitigate | TradeService.getTrade: checks `isParticipant` (trade.sellerId/buyerId == currentUser.userId) at line 216-218. Non-participant non-admin/reviewer/third-party users are denied access. TradeController.getTrade: @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')") (line 115). | closed |
| T-03-07 | Tampering | Unauthorized auction order placement | mitigate | DoubleAuctionController: @PreAuthorize("hasRole('ENTERPRISE')") on both buy (line 46) and sell (line 62) endpoints. @Valid on request body enforces AuctionOrderRequest validation. | closed |
| T-03-08 | Elevation of privilege | Non-admin triggering matching engine | mitigate | DoubleAuctionController.executeMatching: @PreAuthorize("hasRole('ADMIN')") (line 77). Only ADMIN users can trigger price matching. | closed |
| T-03-09 | Tampering | Unauthorized coin recharge | mitigate | CarbonCoinController.recharge: @PreAuthorize("hasRole('ADMIN')") (line 66). Only ADMIN can recharge enterprise coin balances. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-03-01 | T-03-05 | Single-instance deployment for v1. synchronized provides sufficient protection against concurrent matching within one JVM. Multi-instance distributed lock (Redis-based @DistributedLock annotation exists but is not applied to executeMatching) deferred to v2. | Phase 3 PLAN | 2026-05-09 |

*Accepted risks do not resurface in future audit runs.*

---

## Code Review Security Findings (Post-Implementation)

| Finding | Category | Status | Detail |
|---------|----------|--------|--------|
| CR-01 | Credential exposure | FIXED | Hardcoded MySQL root password in 3 test scripts (20+ lines). Fixed: scripts now source db-config.sh with $MYSQL_CONN. |
| CR-02 | Credential exposure | FIXED | Hardcoded API login password (admin123) in 3 test scripts. Fixed: extracted to API_PASSWORD variable with env override. |
| WR-03 | Defense-in-depth | N/A | Review finding that reviewReport() has no service-layer role check was for Phase 2, not Phase 3. Phase 3 trade operations have both controller-level @PreAuthorize AND service-layer user identity checks (e.g., TradeService:68-69). |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-16 | 9 | 9 | 0 | Claude (gsd-secure-phase) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-05-16
