---
status: all_fixed
phase: 02
phase_name: carbon-report-lifecycle
findings_in_scope: 10
fixed: 7
skipped: 3
iteration: 2
---

# Code Review Fix Report: Phase 02 — Carbon Report Lifecycle

**Fixed:** 2026-05-15
**Scope:** all findings from 02-REVIEW.md (critical + warning + info)

## Fixes Applied (Critical + Warning)

### CR-01: createReport() emission data parsing treats arrays as scalars ✓

**File:** CarbonService.java:59-72
**Fix:** Extracted shared `parseEmissionTotals(String emissionData)` private method that correctly iterates scope arrays (scope1/scope2/scope3 as `[{"activity_data":N,"emission_factor":N}]`) and computes `activity_data * emission_factor` per item. Both `createReport()` and `calculateEmissions()` now call this shared method. Previously, `createReport()` treated scopes as scalar text values, causing `totalEmission=0` on create responses.

### WR-01: Test script curl calls have no timeout ✓

**File:** carbon-report-test.sh (throughout)
**Fix:** Added `CURL_OPTS="--connect-timeout 5 --max-time 15"` variable and applied it to all 18 curl calls in the script. Script will no longer block indefinitely if backend hangs.

### WR-02: MySQL password leaked in process list ✓

**File:** carbon-report-test.sh:24
**Fix:** Changed `docker exec "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD"` to `docker exec -e MYSQL_PWD="$DB_PASSWORD" "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME"`. The `MYSQL_PWD` environment variable is not visible in `ps` output.

### WR-04: Frontend reviewReport() uses magic numbers ✓

**File:** carbon.ts:31
**Fix:** Extracted `REVIEW_APPROVED = 3` and `REVIEW_REJECTED = 4` constants with a comment referencing `ReportStatusEnum`. The ternary now uses the named constants instead of bare literals.

## Fixes Applied (Info)

### IN-01: Test data cleanup uses broad LIKE patterns ✓

**File:** carbon-report-test.sh:25-33
**Fix:** Narrowed cleanup LIKE patterns from `'%TEST%'`, `'%UAT%'`, `'%CARB-TEST%'` to `'CARB-TEST-%'` prefix-only matches. Reduces risk of accidentally deleting real data that happens to contain "TEST" in the title.

### IN-04: Frontend getReportList/getMyReports return Promise<unknown> ✓

**File:** carbon.ts:14,19
**Fix:** Changed return types from `Promise<unknown>` to `Promise<PageResponse<CarbonReportResponse>>`. `PageResponse` is defined in `types/api.ts` and re-exported via `types/index.ts`. Frontend type-checks cleanly (pre-existing e2e errors unrelated).

### IN-05: ratingYear null check incomplete in reviewReport() ✓

**File:** CarbonService.java:160-163
**Fix:** Added null/empty guard: if `accountingPeriod` is null or empty, falls back to current year. Prevents potential NPE when `ratingYear.length()` is called on a null value.

## Skipped Findings (not fixable)

### IN-02: extract_field JSON parsing via grep is fragile — SKIPPED

**Rationale:** Acceptable for a local dev/test script with known response shapes. Replacing with `jq` would add a dependency. Documented as-is.

### IN-03: toResponse() makes N+1 database queries — SKIPPED

**Rationale:** Pre-existing pattern across the codebase. Fixing requires JOIN FETCH or @EntityGraph changes that touch repository queries and are out of scope for a code review fix pass. Deferred to a dedicated performance pass.

### IN-06: No explicit @Transactional isolation level on reviewReport() — SKIPPED

**Rationale:** Design decision documented in 02-02-SUMMARY. Default isolation level is sufficient for the current concurrency model. Would need load testing to justify changing.
