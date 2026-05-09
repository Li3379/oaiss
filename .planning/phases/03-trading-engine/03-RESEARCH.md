# Phase 3: Carbon Coin & Trading Engine - Research

**Researched:** 2026-05-09
**Domain:** Carbon coin accounts, double auction matching engine, P2P trading
**Confidence:** HIGH

## Summary

Phase 3 covers three distinct subsystems: (1) **Carbon Coin accounts** -- balance viewing, transaction history, and peer-to-peer coin transfers managed by `CarbonCoinController` + `CarbonCoinService`; (2) **Double Auction** -- buy/sell order placement, an admin-triggered price-time-priority matching algorithm, and settlement in `DoubleAuctionController` + `DoubleAuctionService`; (3) **P2P Trade** -- seller-initiated trade requests, buyer confirmation, and cancellation via `TradeController` + `TradeService`. The two controllers (TradeController vs DoubleAuctionController) are **separate, independent subsystems** -- they do NOT share a matching engine. TradeController handles P2P (trade_type=2) and auction-style listings (trade_type=1) in the `transaction` table. DoubleAuctionController handles double-auction orders in the `auction_order` table with its own matching algorithm and `matching_result` table.

**Primary recommendation:** Plan three test scripts following the Phase 2 `carbon-report-test.sh` pattern: (1) coin-balance-test.sh for COIN-01..05, (2) double-auction-test.sh for TRADE-01..06/12/13, (3) p2p-trade-test.sh for TRADE-07..11. Fix the frontend `trade.ts` field name bug (`carbonAmount` should be `quantity`) before testing P2P.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COIN-01 | Enterprise views carbon coin balance | GET /api/v1/carbon-coin/account -- auto-creates account if missing. Seed data: all 3 enterprises have balance=10000 |
| COIN-02 | Enterprise views transaction history (paginated) | GET /api/v1/carbon-coin/transactions?page=X&size=X&txType=X -- returns Page of CarbonCoinTransaction |
| COIN-03 | Enterprise transfers coins to another enterprise | POST /api/v1/carbon-coin/transfer -- body: {counterpartId, amount, remark}. Validates balance sufficiency |
| COIN-04 | Both balances update atomically after transfer | CarbonCoinService.transfer() is @Transactional -- single DB transaction covers both accounts. Verify via two GET /account calls |
| COIN-05 | Insufficient balance rejected | Transfer > balance throws BusinessException(4005, "carbon coin balance insufficient"). Verify error code in response |
| TRADE-01 | Enterprise places buy order | POST /api/v1/auction/buy -- body: {quantity, price}. Validates carbonTradable >= quantity |
| TRADE-02 | Enterprise places sell order | POST /api/v1/auction/sell -- body: {quantity, price}. Validates carbonTradable >= quantity |
| TRADE-03 | Admin triggers matching | POST /api/v1/auction/match -- admin-only. DoubleAuctionService.executeMatching() is synchronized |
| TRADE-04 | Matching creates trade records | Matching creates Transaction (trade_type=1, status=COMPLETED) and MatchingResult entries |
| TRADE-05 | Buyer/seller accounts update after match | updateEnterpriseQuota() adjusts carbonTradable/carbonUsed/carbonQuota for both parties |
| TRADE-06 | Order status transitions (PENDING -> FULLY_MATCHED / PARTIALLY_MATCHED / CANCELLED) | AuctionOrderStatusEnum: PENDING(0), PARTIALLY_MATCHED(1), FULLY_MATCHED(2), CANCELLED(3) |
| TRADE-07 | P2P trade initiated by Enterprise A to Enterprise B | POST /api/v1/trade/p2p -- body: {tradeType:2, sellerId, buyerId, quantity, unitPrice}. Frontend bug: trade.ts checks `carbonAmount` but sends `quantity` |
| TRADE-08 | P2P trade accepted (confirmed) | POST /api/v1/trade/{tradeId}/confirm -- transitions PENDING -> PROCESSING -> COMPLETED |
| TRADE-09 | P2P trade rejected (cancelled) | POST /api/v1/trade/{tradeId}/cancel -- transitions to CANCELLED if PENDING or PROCESSING |
| TRADE-10 | P2P settlement verifies both account updates | confirmTrade() is @Transactional -- seller loses carbonTradable, buyer gains carbonQuota + carbonTradable |
| TRADE-11 | TradeController vs DoubleAuctionController relationship validated | Separate subsystems. TradeController uses `transaction` table. DoubleAuctionController uses `auction_order` + `matching_result` tables. No shared matching engine |
| TRADE-12 | Quota insufficient order rejected | Both placeBuyOrder/placeSellOrder validate enterprise.carbonTradable >= request.quantity. Throws TradeException.insufficientQuota |
| TRADE-13 | Trading operations must be sequential (no concurrency) | Known issue CON-01/02/03 -- synchronized on executeMatching(), @Transactional elsewhere. Test sequentially only |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Carbon coin balance display | API / Backend | Browser / Client | Backend owns balance logic; frontend calls GET /carbon-coin/account |
| Coin transfer | API / Backend | Browser / Client | @Transactional ensures atomicity; frontend submits form |
| Double auction order placement | API / Backend | Browser / Client | Backend validates quota, creates AuctionOrder; frontend submits |
| Matching engine | API / Backend | -- | Pure backend concern: synchronized matching algorithm in DoubleAuctionService |
| P2P trade lifecycle | API / Backend | Browser / Client | TradeService manages state machine; frontend presents UI |
| Settlement / quota update | API / Backend | Database / Storage | @Transactional + enterprise quota columns updated atomically |
| Transaction history | API / Backend | Browser / Client | Paginated query from backend; frontend renders table |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.2.5 | Application framework | Project standard, already in use |
| Spring Data JPA | (managed) | Data access | Transaction management, pagination |
| MySQL 8 | 8.x | Relational database | 21 tables already defined in Flyway migrations |
| BCrypt | (managed) | Password hashing | User authentication |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| curl + bash | system | API test scripts | All manual testing -- follow Phase 2 pattern |
| Flyway | (managed) | Database migration | V1-V3 already applied |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Shell test scripts | Postman collection | Scripts are version-controlled and follow Phase 2 pattern |
| curl-based testing | Playwright E2E | E2E deferred; shell scripts faster for API verification |

## Architecture Patterns

### System Architecture Diagram

```
                    +-------------------+
                    |  Enterprise User  |
                    +--------+----------+
                             |
                    +--------v----------+
                    |   Browser (Vue)   |
                    |  CarbonCoin.vue   |
                    |  TradingP2P.vue   |
                    |  TradingMarket.vue|
                    |  OrdersManage.vue |
                    +--------+----------+
                             | HTTP (JWT Bearer)
                             v
              +--------------+---------------+
              |       Spring Boot API        |
              |  /api/v1 prefix              |
              +--+----------+----------+-----+
                 |          |          |
       +---------+   +------+------+   +--------+
       | CarbonCoin|   | Trade     |   | Double  |
       | Controller|   | Controller|   | Auction |
       +-----+-----+   +-----+-----+   | Ctrl   |
             |               |         +----+----+
       +-----v-----+   +-----v-----+        |
       | CarbonCoin|   | Trade     |   +----v----+
       | Service   |   | Service   |   | Double  |
       +-----+-----+   +-----+-----+   | Auction |
             |               |         | Service |
       +-----v-----+   +-----v-----+   +----+----+
       | carbon_   |   | transaction|        |
       | coin_*    |   | table      |   +----v----+
       | tables    |   +-----+------+   | auction_|
       +-----------+         |          | order   |
                        +----v-----+    | matching|
                        | enterprise|    | _result |
                        | table     |    +---------+
                        +-----------+
```

### Recommended Project Structure
```
scripts/
  coin-balance-test.sh      # COIN-01..05
  double-auction-test.sh    # TRADE-01..06, TRADE-12, TRADE-13
  p2p-trade-test.sh         # TRADE-07..11
```

### Pattern 1: Coin Transfer (Atomic Balance Update)
**What:** Single @Transactional method deducts from sender and credits receiver
**When to use:** COIN-03/04/05 tests
**Example:**
```bash
# Source: CarbonCoinService.transfer() analysis
# POST /api/v1/carbon-coin/transfer
curl -s -X POST "$API/carbon-coin/transfer" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"counterpartId": 3, "amount": 500.00, "remark": "test transfer"}'
# Verify both accounts:
curl -s "$API/carbon-coin/account" -H "Authorization: Bearer $TOKEN_E1"
curl -s "$API/carbon-coin/account?userId=3" -H "Authorization: Bearer $TOKEN_E1"
```

### Pattern 2: Double Auction Matching
**What:** Place buy + sell orders, admin triggers matching, verify settlement
**When to use:** TRADE-01..06 tests
**Example:**
```bash
# Source: DoubleAuctionController + DoubleAuctionService analysis
# Enterprise A places buy order
curl -s -X POST "$API/auction/buy" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 100, "price": 50.00}'
# Enterprise B places sell order
curl -s -X POST "$API/auction/sell" \
  -H "Authorization: Bearer $TOKEN_E2" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 100, "price": 45.00}'
# Admin triggers matching (buy price 50 >= sell price 45 -> match at 47.50)
curl -s -X POST "$API/auction/match" \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```

### Pattern 3: P2P Trade Lifecycle
**What:** Seller creates P2P trade, buyer confirms, settlement occurs
**When to use:** TRADE-07..10 tests
**Example:**
```bash
# Source: TradeController + TradeService analysis
# Enterprise 1 (seller, user_id=2) creates P2P trade to Enterprise 2 (buyer, user_id=3)
curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType": 2, "sellerId": 2, "buyerId": 3, "quantity": 50, "unitPrice": 10.00}'
# Enterprise 2 confirms the trade
curl -s -X POST "$API/trade/$TRADE_ID/confirm" \
  -H "Authorization: Bearer $TOKEN_E2"
```

### Anti-Patterns to Avoid
- **Concurrent trading tests:** The system uses synchronized + @Transactional but has known race conditions (CON-01/02/03). NEVER test concurrent operations.
- **Mixing TradeController auction with DoubleAuctionController:** TradeController's `/trade/auction` creates a `transaction` record (trade_type=1) with buyerId=0. DoubleAuctionController's `/auction/buy` and `/auction/sell` create `auction_order` records. These are separate flows.
- **Testing coin transfer without checking both balances:** Must verify BOTH sender and receiver balance changes for COIN-04.
- **Assuming carbon coin balance = carbon quota:** They are different. Carbon coin (carbon_coin_account.balance) is a virtual currency. Carbon quota (enterprise.carbon_quota/carbon_tradable) is the emission allowance.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Balance atomicity | Manual begin/commit | Spring @Transactional | Transaction management already in CarbonCoinService.transfer() |
| Matching algorithm | Custom matching code | DoubleAuctionService.executeMatching() | Price-time-priority algorithm already implemented |
| JWT auth in tests | Manual token handling | login() helper from Phase 2 | Reuse proven pattern from carbon-report-test.sh |
| JSON field extraction | Complex jq parsing | grep + sed extract_field() | Proven pattern from Phase 2 test scripts |

**Key insight:** All trading logic is already implemented. This phase is purely about testing existing code, not building new features. The only code change needed is fixing the frontend `trade.ts` field name bug.

## Runtime State Inventory

> This is a testing phase, not a rename/refactor phase. Runtime state inventory is for verifying existing seed data.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | V2 seed: enterprises user_id=2 (carbonTradable=38000), user_id=3 (carbonTradable=55000). V3 seed: user_id=7 (carbonTradable=50000). Carbon coin accounts: users 2,3,7 each have balance=10000 | None -- sufficient for testing |
| Live service config | None -- all config in application.yml and .env | None |
| OS-registered state | None | None |
| Secrets/env vars | .env contains DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET | None -- already configured in Phase 1 |
| Build artifacts | Backend runs via mvn spring-boot:run; frontend via npm run dev | None |

## Common Pitfalls

### Pitfall 1: Frontend trade.ts Field Name Bug
**What goes wrong:** `api/trade.ts` line 5 checks `data?.carbonAmount` but TradingP2P.vue sends `{quantity, unitPrice}`. P2P trades always fail client-side validation.
**Why it happens:** Field name mismatch between API module and view component.
**How to avoid:** Fix `trade.ts` to check `data?.quantity` instead of `data?.carbonAmount`.
**Warning signs:** P2P trade creation returns "carbon quota quantity must be greater than 0" error from frontend.

### Pitfall 2: P2P Trade Requires sellerId = currentUser
**What goes wrong:** P2P trade creation validates `currentUser.getUserId().equals(sellerId)`. If you send sellerId for a different user, it throws unauthorizedTrade.
**Why it happens:** Authorization check in TradeService.createP2PTrade().
**How to avoid:** Always set sellerId to the logged-in user's userId when calling POST /trade/p2p.
**Warning signs:** Error "unauthorized: current user X is not the specified trading party Y".

### Pitfall 3: Double Auction Buy Order Also Validates carbonTradable
**What goes wrong:** The buy order endpoint validates `enterprise.carbonTradable >= quantity`, which seems counterintuitive for buyers. The code checks the same quota field for both buy and sell orders.
**Why it happens:** DoubleAuctionService.placeBuyOrder() reuses the same enterprise quota validation for both directions.
**How to avoid:** Ensure buyer enterprise has sufficient carbonTradable before placing buy orders. Use enterprise003 (carbonTradable=50000) if enterprise001/002 are depleted.
**Warning signs:** TradeException.insufficientQuota on buy order placement.

### Pitfall 4: Matching Only Works When buyPrice >= sellPrice
**What goes wrong:** If all buy orders have prices lower than all sell orders, matching returns an empty list.
**Why it happens:** The algorithm breaks when `buyOrder.getPrice() < sellOrder.getPrice()`.
**How to avoid:** Place buy orders with higher prices than sell orders. Example: buy at 50, sell at 45.
**Warning signs:** executeMatching() returns empty array "No matching opportunity".

### Pitfall 5: Coin Transfer Self-Check
**What goes wrong:** Transferring coins to yourself throws BusinessException(4001, "cannot transfer to yourself").
**Why it happens:** CarbonCoinService.transfer() checks `userId.equals(request.getCounterpartId())`.
**How to avoid:** Always transfer between different users.
**Warning signs:** Error code 4001 on transfer.

### Pitfall 6: P2P Confirm Requires Participation
**What goes wrong:** Only seller or buyer can confirm a trade. A third party calling confirm gets tradeNotFound.
**Why it happens:** TradeService.confirmTrade() validates `sellerId == currentUserId || buyerId == currentUserId`.
**How to avoid:** Use buyer's token to confirm (since seller creates the trade).
**Warning signs:** "trade not found" when trying to confirm with non-participant token.

## Code Examples

Verified patterns from codebase analysis:

### Login Helper (reuse from Phase 2)
```bash
# Source: scripts/carbon-report-test.sh
login() {
  local username="$1"
  local resp=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"admin123\"}")
  local token=$(echo "$resp" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  echo "$token"
}
TOKEN_E1=$(login "enterprise001")
TOKEN_E2=$(login "enterprise002")
TOKEN_ADMIN=$(login "admin")
```

### Get Carbon Coin Account
```bash
# Source: CarbonCoinController.getAccount() analysis
# GET /api/v1/carbon-coin/account (current user)
curl -s "$API/carbon-coin/account" \
  -H "Authorization: Bearer $TOKEN_E1"
# Response: {code:200, data:{id:X, userId:2, balance:10000, totalRecharged:10000, totalSpent:0, status:1}}

# GET /api/v1/carbon-coin/account?userId=3 (other user, requires ADMIN or self)
curl -s "$API/carbon-coin/account?userId=3" \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```

### Transfer Carbon Coins
```bash
# Source: CarbonCoinController.transfer() analysis
curl -s -X POST "$API/carbon-coin/transfer" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"counterpartId": 3, "amount": 500, "remark": "Phase 3 test"}'
# Response: {code:200, data:{balance:9500, ...}}
```

### Place Double Auction Orders + Match
```bash
# Source: DoubleAuctionController analysis
# Buy order (enterprise001, user_id=2)
BUY_RESP=$(curl -s -X POST "$API/auction/buy" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 100, "price": 50}')
BUY_ORDER_NO=$(extract_field "$BUY_RESP" "orderNo")

# Sell order (enterprise002, user_id=3)
SELL_RESP=$(curl -s -X POST "$API/auction/sell" \
  -H "Authorization: Bearer $TOKEN_E2" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 80, "price": 45}')
SELL_ORDER_NO=$(extract_field "$SELL_RESP" "orderNo")

# Admin triggers matching
MATCH_RESP=$(curl -s -X POST "$API/auction/match" \
  -H "Authorization: Bearer $TOKEN_ADMIN")
# Settlement price = (50 + 45) / 2 = 47.50
# Matched quantity = min(100, 80) = 80
```

### P2P Trade Full Lifecycle
```bash
# Source: TradeController analysis
# Enterprise001 (user_id=2, seller) creates P2P to Enterprise002 (user_id=3, buyer)
P2P_RESP=$(curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType": 2, "sellerId": 2, "buyerId": 3, "quantity": 50, "unitPrice": 10}')
P2P_ID=$(extract_field "$P2P_RESP" "id")

# Enterprise002 (buyer) confirms
CONFIRM_RESP=$(curl -s -X POST "$API/trade/$P2P_ID/confirm" \
  -H "Authorization: Bearer $TOKEN_E2")

# Verify: seller loses 50 from carbonTradable, buyer gains 50 to carbonQuota + carbonTradable
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Single TradeController for all trades | Separate DoubleAuctionController for double auction | Design phase | Two independent subsystems, not a shared engine |
| synchronized on matching | Distributed lock (planned) | v2 (deferred) | Current synchronized is OK for sequential testing |

**Deprecated/outdated:**
- The `TradeController.createAuctionOrder()` endpoint creates `transaction` records with trade_type=1 and buyerId=0. This is a simplified "auction listing" flow separate from the proper double auction in `DoubleAuctionController`. Both exist but serve different purposes.

## Seed Data Summary

[VERIFIED: V2__seed_data.sql + V3__test_seed_data.sql]

| Account | user_id | enterprise_id | carbon_quota | carbon_tradable | coin_balance |
|---------|---------|---------------|-------------|-----------------|-------------|
| enterprise001 | 2 | 1 | 50000 | 38000 | 10000 |
| enterprise002 | 3 | 2 | 80000 | 55000 | 10000 |
| enterprise003 | 7 | 3 | 50000 | 50000 | 10000 |
| admin | 1 | -- | -- | -- | -- (no account, auto-created on GET) |
| reviewer001 | 4 | -- | -- | -- | -- |
| authenticator001 | 6 | -- | -- | -- | -- |

## Assumptions Log

> List all claims tagged [ASSUMED] in this research.

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Backend is running on localhost:8080 during testing | Seed Data | Tests fail -- need to start backend first |
| A2 | Phase 2 test data (approved carbon reports) exists in DB | Test Design | Side effects already triggered; no blocking dependency |
| A3 | The frontend bug in trade.ts (carbonAmount vs quantity) is the only P2P frontend issue | Pitfalls | May need additional fixes if other mismatches exist |

**Claims verified during research:**
- All entity schemas, controller endpoints, service methods, and enums verified by reading source code [VERIFIED: codebase]
- Seed data verified by reading V2 and V3 migration SQL [VERIFIED: Flyway migrations]
- Frontend API module bug confirmed by reading trade.ts line 5 vs TradingP2P.vue line 113 [VERIFIED: codebase]

## Open Questions

1. **TradeController `/trade/auction` vs DoubleAuctionController -- which should tests use?**
   - What we know: TradeController has `/trade/auction` (creates transaction with trade_type=1, buyerId=0). DoubleAuctionController has `/auction/buy` and `/auction/sell` (creates auction_order records with matching algorithm).
   - What's unclear: Whether TradeController's auction endpoint is actively used or deprecated.
   - Recommendation: Test DoubleAuctionController for TRADE-01..06 (it has the matching engine). Document TradeController's `/trade/auction` as a separate simplified path but do not make it the primary test target.

2. **Carbon coin balance and carbon quota are independent systems?**
   - What we know: `carbon_coin_account.balance` (virtual currency) is separate from `enterprise.carbon_tradable` (emission quota). Transfer affects coin balance. Trading affects carbon quota.
   - What's unclear: Whether any flow bridges the two (e.g., buying quota with coins).
   - Recommendation: `CarbonCoinService.buyQuota()` and `sellQuota()` methods exist but are not exposed via controller endpoints. Test only the transfer flow (COIN-01..05) and note buyQuota/sellQuota as unused code.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| MySQL (host) | Backend data store | Host on 3306 | 8.x | Docker on 3307 |
| Redis | Rate limiting, caching | Docker | 7.x | -- |
| Backend (Spring Boot) | All API tests | Requires manual start | 3.2.5 | -- |
| curl | Test scripts | System | -- | -- |
| bash | Test scripts | Git Bash | -- | -- |

**Missing dependencies with no fallback:**
- None -- all infrastructure established in Phase 1.

**Missing dependencies with fallback:**
- None.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Bash + curl (manual API testing) |
| Config file | None -- scripts are self-contained |
| Quick run command | `bash scripts/coin-balance-test.sh` |
| Full suite command | `bash scripts/coin-balance-test.sh && bash scripts/double-auction-test.sh && bash scripts/p2p-trade-test.sh` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| COIN-01 | View carbon coin balance | curl GET | `bash scripts/coin-balance-test.sh` | Wave 0 |
| COIN-02 | View transaction history | curl GET | `bash scripts/coin-balance-test.sh` | Wave 0 |
| COIN-03 | Transfer coins | curl POST | `bash scripts/coin-balance-test.sh` | Wave 0 |
| COIN-04 | Atomic balance update | curl GET (verify both) | `bash scripts/coin-balance-test.sh` | Wave 0 |
| COIN-05 | Insufficient balance rejected | curl POST (negative test) | `bash scripts/coin-balance-test.sh` | Wave 0 |
| TRADE-01 | Place buy order | curl POST | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-02 | Place sell order | curl POST | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-03 | Admin matching | curl POST | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-04 | Matching creates records | curl GET verify | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-05 | Settlement updates | curl GET verify | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-06 | Order status transitions | curl GET verify | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-07 | P2P create | curl POST | `bash scripts/p2p-trade-test.sh` | Wave 0 |
| TRADE-08 | P2P accept/confirm | curl POST | `bash scripts/p2p-trade-test.sh` | Wave 0 |
| TRADE-09 | P2P reject/cancel | curl POST | `bash scripts/p2p-trade-test.sh` | Wave 0 |
| TRADE-10 | P2P settlement verify | curl GET verify | `bash scripts/p2p-trade-test.sh` | Wave 0 |
| TRADE-11 | Controller relationship | Code analysis | Documented in RESEARCH.md | N/A |
| TRADE-12 | Quota validation | curl POST (negative) | `bash scripts/double-auction-test.sh` | Wave 0 |
| TRADE-13 | Sequential only | Test design | Single-threaded script execution | N/A |

### Sampling Rate
- **Per task commit:** Run relevant test script(s)
- **Per wave merge:** Run all test scripts for completed wave
- **Phase gate:** All three test scripts pass (0 failures)

### Wave 0 Gaps
- [ ] `scripts/coin-balance-test.sh` -- covers COIN-01..05
- [ ] `scripts/double-auction-test.sh` -- covers TRADE-01..06, TRADE-12, TRADE-13
- [ ] `scripts/p2p-trade-test.sh` -- covers TRADE-07..11
- [ ] Fix `oaiss-chain-frontend/src/api/trade.ts` line 5: change `carbonAmount` to `quantity`

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT Bearer tokens, @PreAuthorize role checks |
| V3 Session Management | yes | Stateless JWT, token blacklist on logout |
| V4 Access Control | yes | @PreAuthorize on all trading endpoints (ENTERPRISE, ADMIN roles) |
| V5 Input Validation | yes | @Valid annotations on request DTOs, balance/quota checks |
| V6 Cryptography | no | No crypto in trading flow (blockchain is mock) |

### Known Threat Patterns for Spring Boot + Trading

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Unauthorized trade creation | Tampering | @PreAuthorize("hasRole('ENTERPRISE')") + sellerId == currentUser check |
| Balance manipulation | Tampering | @Transactional atomicity, server-side balance calculation |
| Negative amount transfer | Tampering | @DecimalMin("0.01") validation on request DTOs |
| Self-transfer | Repudiation | userId.equals(counterpartId) check in service |
| Concurrent double-spend | Tampering | synchronized on matching (partial); CON-01/02/03 deferred to v2 |
| IDOR on trade details | Information Disclosure | isParticipant check in TradeService.getTrade() |

## Sources

### Primary (HIGH confidence)
- [VERIFIED: CarbonCoinController.java] - Account, recharge, transfer, transactions endpoints
- [VERIFIED: CarbonCoinService.java] - Balance operations, transfer atomicity, validation
- [VERIFIED: DoubleAuctionController.java] - Buy/sell order, matching, results endpoints
- [VERIFIED: DoubleAuctionService.java] - Price-time-priority matching algorithm, settlement
- [VERIFIED: TradeController.java] - P2P trade, auction listing, confirm, cancel, list endpoints
- [VERIFIED: TradeService.java] - P2P lifecycle, confirmation, cancellation logic
- [VERIFIED: Entity classes] - CarbonCoinAccount, CarbonCoinTransaction, AuctionOrder, MatchingResult, Transaction, Enterprise
- [VERIFIED: Enums] - TradeStatusEnum, TradeTypeEnum, AuctionOrderStatusEnum, MatchingStatusEnum
- [VERIFIED: V2__seed_data.sql + V3__test_seed_data.sql] - Seed data with carbon coin balances and enterprise quotas
- [VERIFIED: Frontend API modules] - trade.ts, auction.ts, carbonCoin.ts field names and endpoints

### Secondary (MEDIUM confidence)
- [CITED: SecurityConfig.java] - CORS, CSRF disabled, JWT filter chain
- [CITED: JwtUserDetails.java] - Role checking, userId access

### Tertiary (LOW confidence)
- None -- all findings verified from source code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All code read and analyzed from source
- Architecture: HIGH - Controller/service/entity relationships verified by reading all relevant source files
- Pitfalls: HIGH - Bugs (trade.ts field name) confirmed by reading both API module and view component
- Seed data: HIGH - V2 and V3 migrations read and balance values confirmed

**Research date:** 2026-05-09
**Valid until:** 2026-06-09 (stable codebase, no framework upgrades expected)
