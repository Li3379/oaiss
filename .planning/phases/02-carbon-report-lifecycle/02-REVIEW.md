---
status: issues_found
phase: 02
phase_name: carbon-report-lifecycle
depth: standard
files_reviewed: 3
findings:
  critical: 0
  warning: 4
  info: 6
  total: 10
---

# Code Review: Phase 02 — Carbon Report Lifecycle

**Reviewed:** 2026-05-15
**Depth:** standard
**Files:** CarbonService.java, carbon.ts, carbon-report-test.sh

## Findings

### CR-01: createReport() emission data parsing treats arrays as scalars

**Severity:** critical (downgraded to warning — mitigated by submit flow)
**File:** CarbonService.java:59-72
**Category:** bug

`createReport()` parses `scope1`/`scope2`/`scope3` as scalar text values (`node.get("scope1").asText()`), but the actual emission data structure has these as arrays of objects (e.g., `[{"name":"gas","activity_data":1000,"emission_factor":2.0}]`). Calling `asText()` on an array node returns `""`, which `new BigDecimal("")` fails on. The catch block silently swallows the exception, so `totalEmission` is always 0 in the create response.

The `calculateEmissions()` method (called on submit) correctly iterates array items and computes `activity_data * emission_factor`, so the data is corrected before persistence on submit. However, the create response returned to the frontend shows `totalEmission=0`, and DRAFT reports that are never submitted permanently have zero totals.

```java
// BUG: treats array as scalar
scope1 = node.has("scope1") ? new BigDecimal(node.get("scope1").asText()) : BigDecimal.ZERO;
```

**Recommendation:** Use the same array-iteration logic as `calculateEmissions()`, or extract the calculation into a shared private method and call it from both `createReport()` and `submitReport()`.

---

### WR-01: Test script curl calls have no timeout

**Severity:** warning
**File:** carbon-report-test.sh (throughout)
**Category:** quality

All `curl` calls lack `--connect-timeout` and `--max-time` flags. If the backend hangs, the script blocks indefinitely. This was flagged as IN-03 in Phase 1 review and remains unfixed.

**Recommendation:** Add `--connect-timeout 5 --max-time 15` to all curl calls, or define a `CURL_OPTS` variable and use it consistently.

---

### WR-02: Test script leaks MySQL password in process list

**Severity:** warning
**File:** carbon-report-test.sh:24
**Category:** security

`docker exec "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" -p"$DB_PASSWORD"` passes the password on the command line, making it visible in `ps` output. Same pattern as Phase 1 WR-03.

**Recommendation:** Use `docker exec -i "$MYSQL_CONTAINER" mysql -u"$DB_USERNAME" --password="$DB_PASSWORD"` (with `=` form) or pipe credentials via `MYSQL_PWD` environment variable.

---

### WR-03: reviewReport() no reviewer role validation in service layer

**Severity:** warning (downgraded to info — mitigated by controller @PreAuthorize)
**File:** CarbonService.java:134-183
**Category:** defense-in-depth

`reviewReport()` validates report status but does not verify the caller has the REVIEWER role. The controller has `@PreAuthorize("hasRole('REVIEWER')")`, so this is mitigated at the API layer. However, if the service is called from another service or a future endpoint without the annotation, the role check would be bypassed.

**Recommendation:** Consider adding a role check in the service for defense-in-depth, or document that this service method requires the caller to enforce role-based access.

---

### WR-04: Frontend reviewReport() uses magic numbers

**Severity:** warning
**File:** carbon.ts:31
**Category:** quality

`reviewResult: data.approved ? 3 : 4` uses hardcoded magic numbers instead of referencing `ReportStatusEnum` values. If the enum values change, this mapping silently breaks.

**Recommendation:** Import and use the status enum constants, or define a local constant mapping `approved → APPROVED_CODE, rejected → REJECTED_CODE`.

---

### IN-01: Test data cleanup uses broad LIKE patterns

**Severity:** info
**File:** carbon-report-test.sh:25-27
**Category:** quality

The cleanup function deletes records matching `LIKE '%TEST%'` or `LIKE '%UAT%'`. If real reports accidentally contain "TEST" in their title, they would be deleted. Low risk for a dev-only script.

**Recommendation:** Use more specific prefixes (e.g., `LIKE 'CARB-TEST-%'`) to narrow the match scope.

---

### IN-02: extract_field JSON parsing via grep is fragile

**Severity:** info
**File:** carbon-report-test.sh:49-52
**Category:** quality

The `extract_field` helper uses `grep -o` to parse JSON. This breaks on nested objects, arrays, or values containing the field name. Acceptable for a local dev script with known response shapes, as noted in Phase 1 review.

---

### IN-03: toResponse() makes N+1 database queries

**Severity:** info
**File:** CarbonService.java:297-342
**Category:** performance

`toResponse()` calls `enterpriseRepository.findById()` and `userRepository.findById()` for each report. When returning a page of N reports, this generates 2N additional queries. Pre-existing pattern, not introduced in Phase 2.

**Recommendation:** Consider JOIN FETCH or `@EntityGraph` to load enterprise/user names in the initial query, or batch-fetch names for the page.

---

### IN-04: Frontend getReportList/getMyReports return Promise<unknown>

**Severity:** info
**File:** carbon.ts:14,19
**Category:** type-safety

`getReportList()` and `getMyReports()` return `Promise<unknown>`, losing type information. Callers must cast or assert types manually.

**Recommendation:** Define a return type (e.g., `Promise<PageResponse<CarbonReportResponse>>`) for better type safety.

---

### IN-05: ratingYear null check incomplete in reviewReport()

**Severity:** info
**File:** CarbonService.java:160-163
**Category:** robustness

The code checks `ratingYear.length() > 4` but does not guard against `ratingYear` being null before the length check. If `accountingPeriod` is null, `ratingYear.length()` throws NPE. The `accountingPeriod` field is required by the request DTO, so this is unlikely in practice.

**Recommendation:** Add a null check: `if (ratingYear != null && ratingYear.length() > 4)` (already present), but also handle the null case gracefully for `rateEnterprise()`.

---

### IN-06: No explicit @Transactional isolation level on reviewReport()

**Severity:** info
**File:** CarbonService.java:133
**Category:** quality

`reviewReport()` runs three cascading side effects (credit score, emission rating, blockchain) inside a single `@Transactional`. If any side effect fails, all roll back. This is correct by design (D-03), but the default isolation level may allow phantom reads if concurrent reviews target the same report. Pre-existing design decision.

---

## Observations (non-findings)

| Observation | Detail |
|------------|--------|
| V3__test_seed_data.sql missing | Referenced in Phase 1 SUMMARY but not on disk. Does not affect Phase 2. |
| Test script not idempotent | Re-running fails at CARB-06 due to emission_rating uniqueness constraint. By design per 02-03-SUMMARY. |
| calculateEmissions() correctly handles arrays | The submit path's emission calculation iterates scope arrays properly — only createReport() has the scalar-vs-array bug. |
