# Phase 3: Carbon Coin & Trading Engine - Pattern Map

**Mapped:** 2026-05-09
**Files analyzed:** 4 new/modified files
**Analogs found:** 4 / 4

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `scripts/coin-balance-test.sh` | test | request-response | `scripts/carbon-report-test.sh` | exact |
| `scripts/double-auction-test.sh` | test | request-response | `scripts/carbon-report-test.sh` | exact |
| `scripts/p2p-trade-test.sh` | test | request-response | `scripts/carbon-report-test.sh` | exact |
| `oaiss-chain-frontend/src/api/trade.ts` | config (frontend API) | request-response | `oaiss-chain-frontend/src/api/carbon.ts` | exact |

## Pattern Assignments

### `scripts/coin-balance-test.sh` (test, request-response)

**Analog:** `scripts/carbon-report-test.sh` (541 lines)

**Script skeleton pattern** (lines 1-30 of carbon-report-test.sh):
```bash
#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo -e "${YELLOW}[..]${NC} $1"; }

API="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)

TOTAL=0
PASSED=0
FAILED=0
```

**JSON field extraction helper** (lines 21-24):
```bash
extract_field() {
  local json="$1" field="$2"
  echo "$json" | { grep -o "\"$field\":[^,}]*" || true; } | head -1 | sed "s/\"$field\"://" | tr -d '"'
}
```

**Backend health check gate** (lines 27-29):
```bash
info "Checking backend availability..."
curl -sf "$API/swagger-ui.html" -o /dev/null || { fail "Backend not running. Start it first: cd oaiss-chain-backend && mvn spring-boot:run"; exit 1; }
ok "Backend is reachable"
```

**Login helper function** (lines 32-51):
```bash
login() {
  local username="$1"
  local resp=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"admin123\"}")

  local code=$(extract_field "$resp" "code")
  if [[ "$code" != "200" ]]; then
    fail "$username: Login failed (code=$code)"
    echo "$resp" >&2
    return 1
  fi

  local token=$(echo "$resp" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ -z "$token" ]]; then
    fail "$username: No accessToken in response"
    return 1
  fi
  echo "$token"
}
```

**Test assertion pattern** (lines 100-110, adapted):
```bash
# Each test case follows this structure:
TOTAL=$((TOTAL + 1))
if [[ "$RESP_CODE" == "200" ]]; then
  # Extract relevant fields and verify expected values
  ok "COIN-XX: Description (key=value)"
  PASSED=$((PASSED + 1))
else
  fail "COIN-XX: Description failed (code=$RESP_CODE)"
  echo "$RESP" >&2
  FAILED=$((FAILED + 1))
fi
```

**Summary output pattern** (lines 524-540):
```bash
echo ""
echo -e "========================================"
echo -e " Carbon Coin Test Results (COIN-01..05)"
echo -e "========================================"
echo -e " Total:  $TOTAL"
echo -e " Passed: ${GREEN}$PASSED${NC}"
if [[ $FAILED -gt 0 ]]; then
  echo -e " Failed: ${RED}$FAILED${NC}"
else
  echo -e " Failed: 0"
fi
echo -e "========================================"

if [[ $FAILED -gt 0 ]]; then
  exit 1
fi
```

**Target API endpoints (from CarbonCoinController.java):**

| Test ID | Method | Endpoint | Auth | Notes |
|---------|--------|----------|------|-------|
| COIN-01 | GET | `/carbon-coin/account` | ENTERPRISE | Auto-creates account; response fields: id, userId, balance, totalRecharged, totalSpent, status |
| COIN-02 | GET | `/carbon-coin/transactions?page=1&size=10` | ENTERPRISE | Paginated; params: userId, txType, page, size |
| COIN-03 | POST | `/carbon-coin/transfer` | ENTERPRISE, ADMIN | Body: `{counterpartId, amount, remark}` |
| COIN-04 | GET | `/carbon-coin/account` (both users) | ENTERPRISE, ADMIN | Verify both balances after transfer |
| COIN-05 | POST | `/carbon-coin/transfer` | ENTERPRISE | Negative test: amount > balance; expect code 4005 |

**Seed data for coin tests:**
- enterprise001 (userId=2): coin balance=10000, enterprise carbonTradable=38000
- enterprise002 (userId=3): coin balance=10000, enterprise carbonTradable=55000
- enterprise003 (userId=7): coin balance=10000, enterprise carbonTradable=50000

**Transfer request DTO (CarbonCoinTransferRequest.java):**
```json
{
  "counterpartId": 3,
  "amount": 500.00,
  "remark": "test transfer"
}
```
Validation: `@NotNull counterpartId`, `@NotNull @DecimalMin("0.01") amount`

**Transfer error codes (from CarbonCoinService.java lines 139-206):**
- 4001: "cannot transfer to yourself" (userId == counterpartId)
- 4003: "counterpart account not found"
- 4004: "coin account disabled"
- 4005: "insufficient coin balance"

---

### `scripts/double-auction-test.sh` (test, request-response)

**Analog:** `scripts/carbon-report-test.sh` (541 lines)

Uses identical skeleton (lines 1-30), extract_field (lines 21-24), login helper (lines 32-51), health check (lines 27-29), assertion pattern (lines 100-110), and summary (lines 524-540).

**Target API endpoints (from DoubleAuctionController.java):**

| Test ID | Method | Endpoint | Auth | Notes |
|---------|--------|----------|------|-------|
| TRADE-01 | POST | `/auction/buy` | ENTERPRISE | Body: `{quantity, price}` |
| TRADE-02 | POST | `/auction/sell` | ENTERPRISE | Body: `{quantity, price}` |
| TRADE-03 | POST | `/auction/match` | ADMIN | No body; triggers matching |
| TRADE-04 | GET | `/auction/results?page=1&size=10` | ENTERPRISE | Verify matching results |
| TRADE-05 | GET | `/auction/my-orders` (both users) | ENTERPRISE | Verify quota changes |
| TRADE-06 | GET | `/auction/orders?status=X` | ANY | Check status transitions |
| TRADE-12 | POST | `/auction/buy` | ENTERPRISE | Negative: quantity > carbonTradable |
| TRADE-13 | N/A | Test design | -- | Sequential only, no concurrency |

**Key difference from coin tests:** Must login as admin (for `/auction/match`).

**Login pattern for 3 roles:**
```bash
TOKEN_E1=$(login "enterprise001")
TOKEN_E2=$(login "enterprise002")
TOKEN_ADMIN=$(login "admin")
```

**AuctionOrderRequest DTO (AuctionOrderRequest.java):**
```json
{"quantity": 100, "price": 50.00}
```
Fields: quantity (BigDecimal), price (BigDecimal). No validation annotations -- validation is in service layer.

**Matching algorithm rules (from DoubleAuctionService.java lines 146-269):**
1. Buy orders sorted by price DESC (highest bid first)
2. Sell orders sorted by price ASC (lowest ask first)
3. Match only when buyPrice >= sellPrice
4. Settlement price = (buyPrice + sellPrice) / 2
5. Match quantity = min(buyRemaining, sellRemaining)
6. Creates MatchingResult + Transaction (trade_type=1, status=COMPLETED)
7. Updates enterprise quotas: seller loses carbonTradable, buyer gains carbonQuota + carbonTradable

**Order status values (AuctionOrderStatusEnum):**
- 0 = PENDING
- 1 = PARTIALLY_MATCHED
- 2 = FULLY_MATCHED
- 3 = CANCELLED

**Quota validation (DoubleAuctionService.java lines 76-82):**
- Both buy AND sell validate `enterprise.carbonTradable >= request.quantity`
- Error: TradeException.insufficientQuota (same check for both directions)

**Quota update after match (DoubleAuctionService.java lines 338-352):**
```java
seller.setCarbonTradable(seller.getCarbonTradable().subtract(quantity));
seller.setCarbonUsed(seller.getCarbonUsed().add(quantity));
buyer.setCarbonQuota(buyer.getCarbonQuota().add(quantity));
buyer.setCarbonTradable(buyer.getCarbonTradable().add(quantity));
```

---

### `scripts/p2p-trade-test.sh` (test, request-response)

**Analog:** `scripts/carbon-report-test.sh` (541 lines)

Uses identical skeleton, extract_field, login helper, health check, assertion pattern, and summary.

**Target API endpoints (from TradeController.java):**

| Test ID | Method | Endpoint | Auth | Notes |
|---------|--------|----------|------|-------|
| TRADE-07 | POST | `/trade/p2p` | ENTERPRISE | Body: `{tradeType:2, sellerId, buyerId, quantity, unitPrice}` |
| TRADE-08 | POST | `/trade/{tradeId}/confirm` | ENTERPRISE, ADMIN | Buyer confirms |
| TRADE-09 | POST | `/trade/{tradeId}/cancel` | ENTERPRISE | Cancel PENDING trade |
| TRADE-10 | GET (verify) | Check enterprise quotas | ENTERPRISE | Verify settlement |
| TRADE-11 | N/A | Code analysis | -- | Document controller relationship |

**TradeRequest DTO (TradeRequest.java):**
```json
{
  "tradeType": 2,
  "sellerId": 2,
  "buyerId": 3,
  "quantity": 50,
  "unitPrice": 10.00,
  "reportId": null,
  "remark": "P2P test trade"
}
```
Validation: `@NotNull tradeType`, `@NotNull @Positive quantity`, `@NotNull @Positive unitPrice`

**P2P trade creation rules (TradeService.java lines 54-101):**
1. tradeType must be 2 (P2P)
2. sellerId != buyerId (same party error)
3. currentUser.userId must equal sellerId (authorization check)
4. seller enterprise must have carbonTradable >= quantity

**Confirm trade rules (TradeService.java lines 142-186):**
1. Only PENDING trades can be confirmed
2. Only participants (sellerId or buyerId == currentUserId) can confirm
3. Status transitions: PENDING -> PROCESSING -> COMPLETED
4. Settlement: seller loses carbonTradable, gains carbonUsed; buyer gains carbonQuota + carbonTradable

**Cancel trade rules (TradeService.java lines 192-206):**
1. Only cancellable statuses (check `status.isCancellable()`)
2. Transitions to CANCELLED

**TradeStatusEnum values:**
- 0 = PENDING
- 1 = PROCESSING
- 2 = COMPLETED
- 3 = CANCELLED

**Critical: P2P test must use seller's token for creation, buyer's token for confirmation:**
```bash
# Seller (enterprise001, userId=2) creates trade
TOKEN_E1=$(login "enterprise001")
P2P_RESP=$(curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType":2,"sellerId":2,"buyerId":3,"quantity":50,"unitPrice":10.00}')

# Buyer (enterprise002, userId=3) confirms
TOKEN_E2=$(login "enterprise002")
P2P_ID=$(extract_field "$P2P_RESP" "id")
CONFIRM_RESP=$(curl -s -X POST "$API/trade/$P2P_ID/confirm" \
  -H "Authorization: Bearer $TOKEN_E2")
```

**Create second P2P trade and cancel it (for TRADE-09):**
```bash
P2P2_RESP=$(curl -s -X POST "$API/trade/p2p" \
  -H "Authorization: Bearer $TOKEN_E1" \
  -H "Content-Type: application/json" \
  -d '{"tradeType":2,"sellerId":2,"buyerId":3,"quantity":30,"unitPrice":8.00}')
P2P2_ID=$(extract_field "$P2P2_RESP" "id")

CANCEL_RESP=$(curl -s -X POST "$API/trade/$P2P2_ID/cancel" \
  -H "Authorization: Bearer $TOKEN_E1")
# Expect status to change to CANCELLED (status=3)
```

---

### `oaiss-chain-frontend/src/api/trade.ts` (config, request-response)

**Analog:** `oaiss-chain-frontend/src/api/carbon.ts` (35 lines)

**Bug fix required:** Line 5 checks `data?.carbonAmount` but the backend TradeRequest DTO uses `quantity`. The frontend TradingP2P.vue component sends `{quantity, unitPrice}`.

**Current (buggy) code** (trade.ts lines 1-8):
```typescript
import request from './request'
import type { TradeRequest, TradeResponse, PageRequest } from '../types'

export function createP2PTrade(data: TradeRequest): Promise<TradeResponse> {
  if (!data?.carbonAmount || data.carbonAmount <= 0) return Promise.reject(new Error('碳配额数量必须大于0'))
  if (!data?.price || data.price <= 0) return Promise.reject(new Error('价格必须大于0'))
  return request.post('/trade/p2p', data)
}
```

**Fix pattern:** Change `data?.carbonAmount` to `data?.quantity` and `data?.price` to `data?.unitPrice` to match the backend TradeRequest DTO fields.

**Correct code should be:**
```typescript
export function createP2PTrade(data: TradeRequest): Promise<TradeResponse> {
  if (!data?.quantity || data.quantity <= 0) return Promise.reject(new Error('碳配额数量必须大于0'))
  if (!data?.unitPrice || data.unitPrice <= 0) return Promise.reject(new Error('价格必须大于0'))
  return request.post('/trade/p2p', data)
}
```

Note: `data?.price` on line 6 should also become `data?.unitPrice` to match the backend DTO field name.

**API module pattern** (from carbon.ts, the analog):
```typescript
import request from './request'
import type { CarbonReportRequest, CarbonReportResponse, PageRequest } from '../types'

export function createReport(data: CarbonReportRequest): Promise<CarbonReportResponse> {
  if (!data?.title) return Promise.reject(new Error('报告标题不能为空'))
  return request.post('/carbon/reports', data)
}
```

Pattern: validate required fields client-side, then delegate to `request.post()`. Error messages are in Chinese (zh-CN).

---

## Shared Patterns

### Script Skeleton
**Source:** `scripts/carbon-report-test.sh` lines 1-30
**Apply to:** All three test scripts (coin-balance-test.sh, double-auction-test.sh, p2p-trade-test.sh)
```bash
#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
info() { echo -e "${YELLOW}[..]${NC} $1"; }

API="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)

TOTAL=0
PASSED=0
FAILED=0
```

### JSON Field Extraction
**Source:** `scripts/carbon-report-test.sh` lines 21-24
**Apply to:** All three test scripts
```bash
extract_field() {
  local json="$1" field="$2"
  echo "$json" | { grep -o "\"$field\":[^,}]*" || true; } | head -1 | sed "s/\"$field\"://" | tr -d '"'
}
```

### Login Helper
**Source:** `scripts/carbon-report-test.sh` lines 32-51
**Apply to:** All three test scripts
```bash
login() {
  local username="$1"
  local resp=$(curl -s -X POST "$API/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"admin123\"}")
  local code=$(extract_field "$resp" "code")
  if [[ "$code" != "200" ]]; then
    fail "$username: Login failed (code=$code)"
    echo "$resp" >&2
    return 1
  fi
  local token=$(echo "$resp" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [[ -z "$token" ]]; then
    fail "$username: No accessToken in response"
    return 1
  fi
  echo "$token"
}
```

### Test Assertion Pattern
**Source:** `scripts/carbon-report-test.sh` lines 100-110
**Apply to:** Every test case in all three scripts
```bash
TOTAL=$((TOTAL + 1))
RESP_CODE=$(extract_field "$RESP" "code")
if [[ "$RESP_CODE" == "200" ]]; then
  # verify expected field values
  ok "REQ-ID: Description (key=value)"
  PASSED=$((PASSED + 1))
else
  fail "REQ-ID: Description failed (code=$RESP_CODE)"
  echo "$RESP" >&2
  FAILED=$((FAILED + 1))
fi
```

### Negative Test Pattern
**Source:** `scripts/carbon-report-test.sh` lines 377-394 (CARB-11)
**Apply to:** COIN-05 (insufficient balance), TRADE-12 (insufficient quota)
```bash
TOTAL=$((TOTAL + 1))
NEG_RESP=$(curl -s -X POST "$API/endpoint" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"field": "bad_value"}')
NEG_CODE=$(extract_field "$NEG_RESP" "code")
if [[ "$NEG_CODE" != "200" ]]; then
  ok "REQ-ID: Negative case correctly rejected (code=$NEG_CODE)"
  PASSED=$((PASSED + 1))
else
  fail "REQ-ID: Should have been rejected but returned 200"
  FAILED=$((FAILED + 1))
fi
```

### Cross-Role Access Test Pattern
**Source:** `scripts/carbon-report-test.sh` lines 440-454 (CARB-12b)
**Apply to:** If testing unauthorized role access in trading endpoints
```bash
TOTAL=$((TOTAL + 1))
CROSS_RESP=$(curl -s -w "\n%{http_code}" -X POST "$API/endpoint" \
  -H "Authorization: Bearer $WRONG_ROLE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"body": "here"}')
CROSS_HTTP=$(echo "$CROSS_RESP" | tail -1)
if [[ "$CROSS_HTTP" == "403" ]]; then
  ok "REQ-ID: Wrong role correctly denied (HTTP 403)"
  PASSED=$((PASSED + 1))
else
  fail "REQ-ID: Expected 403, got HTTP=$CROSS_HTTP"
  FAILED=$((FAILED + 1))
fi
```

### API Response Envelope
**Source:** All backend controllers return `ApiResponse<T>`
**Apply to:** All curl response parsing
```
{ "code": 200, "message": "success", "data": { ... } }
```
Error responses: `{ "code": 4xxx, "message": "error description", "data": null }`

## No Analog Found

Files with no close match in the codebase (planner should use RESEARCH.md patterns instead):

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| (none) | -- | -- | All files have direct analogs from Phase 2 |

All four files have exact or near-exact analogs in the existing codebase. The three test scripts are direct structural copies of `carbon-report-test.sh` with different API endpoints and test cases. The frontend fix follows the same pattern as the Phase 2 `carbon.ts` fix.

## Metadata

**Analog search scope:**
- `scripts/` (2 existing test scripts)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/controller/` (3 controllers: CarbonCoin, DoubleAuction, Trade)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/service/` (3 services: CarbonCoin, DoubleAuction, Trade)
- `oaiss-chain-backend/src/main/java/com/oaiss/chain/dto/` (3 DTOs: CarbonCoinTransferRequest, TradeRequest, AuctionOrderRequest)
- `oaiss-chain-frontend/src/api/` (4 modules: trade.ts, auction.ts, carbonCoin.ts, carbon.ts)
- `.planning/phases/02-carbon-report-lifecycle/` (3 plan files for context)

**Files scanned:** 19
**Pattern extraction date:** 2026-05-09
