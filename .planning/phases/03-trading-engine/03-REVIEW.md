---
phase: 03-trading-engine
reviewed: 2026-05-16T10:52:00+08:00
depth: deep
files_reviewed: 4
files_reviewed_list:
  - oaiss-chain-frontend/src/api/trade.ts
  - scripts/coin-balance-test.sh
  - scripts/double-auction-test.sh
  - scripts/p2p-trade-test.sh
findings:
  critical: 0
  warning: 5
  info: 3
  total: 8
status: issues_found
---

# Phase 3: Code Review Report

**Reviewed:** 2026-05-16T10:52:00+08:00
**Depth:** deep
**Files Reviewed:** 4
**Status:** issues_found

## Summary

Reviewed the Phase 3 trading engine files: the frontend trade API client and three bash test scripts for carbon coin, double auction, and P2P trade flows. Cross-referenced the frontend `TradeRequest` type and `trade.ts` API functions against the backend `TradeRequest.java` DTO -- field names (`quantity`, `unitPrice`) are correctly aligned after the previously documented bug fix. The auction subsystem (`AuctionOrderRequest.java`) intentionally uses `price` instead of `unitPrice`, and `TradingMarket.vue` correctly follows that convention.

The test scripts have significant quality issues: hardcoded database credentials that bypass the project's own `db-config.sh` configuration manager, no test data cleanup in the coin-balance and P2P tests (causing order-dependent flakiness), and a misleading pass-by-default assertion in the double auction script.

## Warnings

### WR-01: Hardcoded MySQL root password in all three test scripts

**File:** `scripts/coin-balance-test.sh:68,71,73`, `scripts/double-auction-test.sh:54,57,59,79,81,84,173,175,178`, `scripts/p2p-trade-test.sh:68,71,73,130,133,135`
**Issue:** All three scripts hardcode `mysql -u root -p123456` directly instead of sourcing the existing `scripts/db-config.sh` module, which already centralizes database credentials with environment variable override support (`DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`). The password `123456` is committed to version control in plaintext across 20+ lines.
**Fix:** Source `db-config.sh` at the top of each script and use its `MYSQL_CONN` variable:
```bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/db-config.sh"

# Then replace all mysql calls:
mysql $MYSQL_CONN -N -e "SELECT carbon_tradable FROM enterprise WHERE user_id=2"
```

### WR-02: No test data cleanup in coin-balance-test.sh

**File:** `scripts/coin-balance-test.sh`
**Issue:** COIN-03 transfers 500 coins from enterprise001 to enterprise002. The script never restores the original balances after the test run. Repeated executions will cause COIN-04's balance arithmetic assertions to pass on the cumulative state, masking potential bugs where the transfer amount is wrong (e.g., if the backend transferred 600 instead of 500, the second run's COIN-04 check would still pass because it only compares before/after deltas within that run -- but the absolute balances would be drifting from expected seed values). More critically, COIN-05's insufficient-balance test (amount=999999) may unexpectedly pass if the balance has grown large enough from accumulated transfers.
**Fix:** Add cleanup at the end of the script (before the summary) and/or reset balances at the start:
```bash
# Reset at start
mysql $MYSQL_CONN -e \
  "UPDATE carbon_coin_account SET balance=10000 WHERE user_id IN (2,3)" 2>/dev/null || true
```

### WR-03: No pre-test state reset in p2p-trade-test.sh

**File:** `scripts/p2p-trade-test.sh`
**Issue:** Unlike `double-auction-test.sh` which resets auction data and enterprise quotas at the top (lines 53-61), `p2p-trade-test.sh` performs no state reset. It assumes seed data values (e.g., SELLER_QUOTA_BEFORE, BUYER_TRADABLE_BEFORE) are at their original defaults. If `double-auction-test.sh` runs first (or any prior test run modified enterprise quotas), the before/after delta assertions in TRADE-10 will compute against stale baseline values and fail.
**Fix:** Add a state reset block at the start, consistent with double-auction-test.sh:
```bash
# Reset P2P trade state
mysql $MYSQL_CONN -e "DELETE FROM transaction WHERE trade_type=2" 2>/dev/null || true
mysql $MYSQL_CONN -e \
  "UPDATE enterprise SET carbon_tradable=38000, carbon_quota=50000, carbon_used=12000 WHERE user_id=2" 2>/dev/null
mysql $MYSQL_CONN -e \
  "UPDATE enterprise SET carbon_tradable=55000, carbon_quota=55000, carbon_used=0 WHERE user_id=3" 2>/dev/null
```

### WR-04: db-config.sh defaults to port 3307 but all test scripts use port 3306

**File:** `scripts/db-config.sh:14` vs `scripts/coin-balance-test.sh:68`, `scripts/double-auction-test.sh:54`, `scripts/p2p-trade-test.sh:68`
**Issue:** `db-config.sh` defines `DB_PORT_DEFAULT="3307"` (matching Docker Compose), but all three test scripts hardcode port 3306 in their mysql calls. If the scripts were updated to source `db-config.sh` (per WR-01), they would switch from port 3306 to 3307, potentially breaking against a local MySQL instance. The port mismatch means either `db-config.sh` has the wrong default for local dev, or the test scripts assume a non-Docker setup that was never documented.
**Fix:** Either update `db-config.sh` to document both ports (Docker vs local), or add an override comment in each test script explaining the port assumption. Consider adding `DB_PORT=3306` as an environment variable when running tests locally.

### WR-05: Hardcoded API login credentials in all test scripts

**File:** `scripts/coin-balance-test.sh:27,35`, `scripts/double-auction-test.sh:27,35`, `scripts/p2p-trade-test.sh:27,35`
**Issue:** All scripts hardcode `password":"admin123"` in the login helper and backend health check. While these are test-only seed accounts, the password is committed to version control. If these scripts are reused against a staging environment with different credentials, they will fail silently (the login helper returns 1 on failure, but `set -e` means the script exits without a clear error about credential mismatch).
**Fix:** Extract to a variable at the top of each script:
```bash
API_PASSWORD="${API_PASSWORD:-admin123}"
# Then use in login():
-d "{\"username\":\"$username\",\"password\":\"$API_PASSWORD\"}"
```

## Info

### IN-01: TRADE-13 is a vacuous pass -- not a concurrency test

**File:** `scripts/double-auction-test.sh:261-272`
**Issue:** TRADE-13 is documented as "Sequential-only execution verification" but the test logic is `if [[ $FAILED -eq 0 ]]; then ok ...; else fail ...`. This passes whenever all prior tests passed, regardless of whether the matching engine actually handles concurrency correctly. It adds a test count to TOTAL without verifying any actual behavior.
**Fix:** Either remove TRADE-13 from the test suite (it is not testing anything), or replace it with a meaningful assertion -- for example, verify that the `DoubleAuctionService.executeMatching()` method is annotated with `@Transactional` or `synchronized`, or run two concurrent match requests and verify only one succeeds.

### IN-02: TRADE-11 is documentation, not a test assertion

**File:** `scripts/p2p-trade-test.sh:209-223`
**Issue:** TRADE-11 prints controller relationship documentation to stdout and unconditionally passes. It increments TOTAL and PASSED without asserting any runtime behavior. This inflates the pass count and gives a false sense of coverage.
**Fix:** Move this documentation to a comment block or a separate README section. If kept as a "test," at minimum verify that both `/trade/p2p` and `/auction/buy` endpoints are reachable and return distinct response shapes.

### IN-03: extract_field JSON parsing via grep is fragile

**File:** `scripts/coin-balance-test.sh:20-23`, `scripts/double-auction-test.sh:20-23`, `scripts/p2p-trade-test.sh:20-23`
**Issue:** The `extract_field` helper uses `grep -o "\"$field\":[^,}]*"` to parse JSON. This breaks on: (1) nested objects (e.g., `"data":{"id":1}` -- searching for `"id"` would match `"id":1}` with the trailing brace), (2) string values containing commas, (3) field names that are substrings of other keys (e.g., searching for `"id"` matches `"orderId"`). For the current test scripts this works because the API responses use flat structures, but it is a latent reliability issue.
**Fix:** For a more robust approach, use `jq` if available:
```bash
extract_field() {
  local json="$1" field="$2"
  echo "$json" | jq -r ".$field // empty" 2>/dev/null || \
    echo "$json" | grep -o "\"$field\":[^,}]*" | head -1 | sed "s/\"$field\"://" | tr -d '"'
}
```

---

_Reviewed: 2026-05-16T10:52:00+08:00_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
