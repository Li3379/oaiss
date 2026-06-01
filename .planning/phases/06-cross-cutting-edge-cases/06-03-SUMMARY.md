---
phase: 06-cross-cutting-edge-cases
plan: 03
type: execute
wave: 3
status: complete
completed: 2026-05-10
---

# Plan 06-03 Summary: Edge Cases & Negative Testing

## Result: SUCCESS — 34/34 tests passed

## What was done

Created `scripts/edge-test.sh` covering all 6 EDGE requirements with 34 test cases across cross-role access, state machine violations, financial integrity, pagination, input validation, and i18n.

### EDGE-01: Cross-role access control (10 tests)
- All 5 roles tested against restricted endpoints
- ENTERPRISE blocked from REVIEWER/ADMIN endpoints (code 2004)
- REVIEWER blocked from ENTERPRISE endpoints (code 2004)
- ADMIN blocked from ENTERPRISE-only createReport (code 2004)
- THIRD_PARTY blocked from ENTERPRISE endpoints (code 2004)
- Unauthenticated requests get code 2000 (USER_NOT_LOGIN)
- Authenticator blocked from ENTERPRISE auction endpoints (code 2004)

### EDGE-02: State machine violations (6 tests)
- CarbonReport: Cannot approve DRAFT (code 3001), cannot re-submit SUBMITTED/APPROVED
- P2P Trade: Non-participant cannot confirm (code 2004), cannot double-confirm COMPLETED trade
- Seller uses userId (not enterpriseId) as sellerId/buyerId in P2P API

### EDGE-03: Financial integrity (1 test)
- P2P settlement transfers carbonTradable between enterprises
- Carbon tradable sum conserved before/after trade (93000 == 93000)
- Settlement: seller loses tradable, buyer gains equal amount

### EDGE-04: Pagination boundaries (6 tests)
- pageSize=1 returns data, pageNum=999 returns success (empty page)
- pageSize=1000 returns success, pageSize=0 handled without crash
- Auction orders and admin users pagination both work correctly

### EDGE-05: Input validation (7 tests)
- **Code gap found**: DoubleAuctionController accepts negative price, zero/negative quantity without validation
- Extreme price, XSS, SQL injection, and very long title all handled without server crash
- JPA parameterized queries prevent SQL injection; XSS stored as-is (frontend responsibility)

### EDGE-06: i18n verification (4 tests)
- zh-CN.ts and en-US.ts both exist in `src/i18n/locales/`
- Each has 673 translation entries (well above 50 minimum)

## Key technical findings

- **P2P Trade uses userId not enterpriseId**: sellerId/buyerId in TradeRequest correspond to user IDs, not enterprise table IDs. `createP2PTrade` validates `currentUser.getUserId().equals(sellerId)`.
- **Code gap — no input validation on auction orders**: DoubleAuctionController accepts negative/zero values. Deferred to v2 per project scope.
- **CarbonReport state error code is 3001** (CARBON_DATA_SUBMIT_FAILED), not 3003 (DATA_FORMAT_ERROR)
- **carbon_coin_account** table uses `user_id` (not `enterprise_id`) and `balance` column (not `carbon_coin`)
- **i18n files located at** `src/i18n/locales/zh-CN.ts` and `src/i18n/locales/en-US.ts` (nested under locales/)

## Files modified

| File | Change |
|------|--------|
| `scripts/edge-test.sh` | Created — 34-test edge case verification script |

## Duration

~5 minutes (no backend restart needed)
