---
phase: 03-trading-engine
fixed_at: 2026-05-16T11:30:00+08:00
review_path: .planning/phases/03-trading-engine/03-REVIEW.md
iteration: 1
findings_in_scope: 8
fixed: 8
skipped: 0
status: all_fixed
---

# Phase 3: Code Review Fix Report

**Fixed at:** 2026-05-16T11:30:00+08:00
**Source review:** .planning/phases/03-trading-engine/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 8
- Fixed: 8
- Skipped: 0

## Fixed Issues

### WR-01: Hardcoded MySQL root password in all three test scripts

**Files modified:** `scripts/coin-balance-test.sh`, `scripts/double-auction-test.sh`, `scripts/p2p-trade-test.sh`
**Commit:** ca67d40, a8d7cda, ecd6821
**Applied fix:** Sourced `scripts/db-config.sh` at the top of each script and replaced all hardcoded `mysql -u root -p123456 -h 127.0.0.1 -P 3306 oaiss_chain` calls with `mysql $MYSQL_CONN`. Credentials are now centralized in db-config.sh with environment variable override support.

### WR-02: No test data cleanup in coin-balance-test.sh

**Files modified:** `scripts/coin-balance-test.sh`
**Commit:** ca67d40
**Applied fix:** Added a balance reset block after login that resets `carbon_coin_account` balances to seed values (10000) for user_ids 2 and 3, preventing accumulated state from drifting across runs.

### WR-03: No pre-test state reset in p2p-trade-test.sh

**Files modified:** `scripts/p2p-trade-test.sh`
**Commit:** ecd6821
**Applied fix:** Added a state reset block after login (consistent with double-auction-test.sh) that deletes P2P transactions and restores enterprise quotas/tradable/used to seed values before running tests.

### WR-04: db-config.sh defaults to port 3307 but all test scripts use port 3306

**Files modified:** `scripts/coin-balance-test.sh`, `scripts/double-auction-test.sh`, `scripts/p2p-trade-test.sh`
**Commit:** ca67d40, a8d7cda, ecd6821
**Applied fix:** Added `DB_PORT=3306` as an inline environment variable before sourcing `db-config.sh`. This overrides the Docker default (3307) to match the local dev environment these scripts target, without modifying db-config.sh itself.

### WR-05: Hardcoded API login credentials in all test scripts

**Files modified:** `scripts/coin-balance-test.sh`, `scripts/double-auction-test.sh`, `scripts/p2p-trade-test.sh`
**Commit:** ca67d40, a8d7cda, ecd6821
**Applied fix:** Extracted `API_PASSWORD="${API_PASSWORD:-admin123}"` variable at the top of each script. All curl login calls now use `$API_PASSWORD` instead of the hardcoded string. The variable supports environment override for staging environments.

### IN-01: TRADE-13 is a vacuous pass

**Files modified:** `scripts/double-auction-test.sh`
**Commit:** a8d7cda
**Applied fix:** Replaced the vacuous "check if prior tests passed" logic with a meaningful assertion: verifies that `DoubleAuctionService.java` contains an `@Transactional` annotation, confirming the matching engine has transactional guarantees.

### IN-02: TRADE-11 is documentation, not a test assertion

**Files modified:** `scripts/p2p-trade-test.sh`
**Commit:** ecd6821
**Applied fix:** Converted TRADE-11 from an unconditional pass that inflated TOTAL/PASSED counts to a comment-only documentation block. It no longer increments test counters. Updated summary header to reflect TRADE-07..10 range.

### IN-03: extract_field JSON parsing via grep is fragile

**Files modified:** `scripts/coin-balance-test.sh`, `scripts/double-auction-test.sh`, `scripts/p2p-trade-test.sh`
**Commit:** ca67d40, a8d7cda, ecd6821
**Applied fix:** Updated `extract_field()` to prefer `jq` when available (`jq -r ".$field // empty"`), falling back to the existing grep-based approach. This handles nested objects and substring field name collisions correctly when jq is installed.

---

_Fixed: 2026-05-16T11:30:00+08:00_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
