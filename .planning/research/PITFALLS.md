# Domain Pitfalls: Manual Testing of OAISS CHAIN

**Domain:** Multi-role Spring Boot + Vue 3 carbon trading platform
**Researched:** 2026-05-08
**Confidence:** HIGH (based on codebase analysis + known CONCERNS.md)

---

## Critical Pitfalls

Mistakes that cause wasted hours or false bug reports.

---

### Pitfall 1: JWT Token Expiration During Long Test Sessions

**What goes wrong:** Access token expires after 1 hour (`jwt.expiration: 3600000`). Mid-test, all API calls return 401. Tester thinks the system is broken.

**Why it happens:** `JwtTokenProvider` issues tokens with `System.currentTimeMillis() + jwtExpiration`. The frontend auto-refresh logic in `request.ts` intercepts expired tokens and calls `/auth/refresh`, but this has edge cases.

**Warning signs:**
- Sudden "登录已过期，请重新登录" toast message
- All API calls return 401 in Network tab
- Page redirects to `/login` unexpectedly
- Forms submit but nothing happens (silent 401 redirect)

**Detection:** Open DevTools Console. If you see `Token 刷新失败，请重新登录`, the refresh token mechanism failed.

**Prevention:**
1. Re-login every 45 minutes during testing sessions
2. After switching roles (logout/login), verify token is fresh: DevTools > Application > sessionStorage > `access_token` should exist
3. Keep the DevTools Network tab open -- 401 responses are the earliest signal

**Workaround:** If token refresh fails, the system clears tokens and redirects to login. Simply re-login. Do NOT report this as a bug unless it happens within minutes of login.

**Known interaction:** The refresh token lives in `localStorage` (7-day TTL). If you clear browser storage between tests, the refresh token is lost and re-login is required.

---

### Pitfall 2: Browser Cache Interfering with UI State

**What goes wrong:** After backend data changes, the frontend shows stale data. Tester reports "data not updated" when the issue is cached Vue component state or browser storage.

**Why it happens:**
- Vue Router uses lazy-loaded components (`() => import(...)`) which Vite caches
- Pinia stores persist in memory across route navigations
- `sessionStorage` holds the access token; `localStorage` holds the refresh token
- Axios has no explicit cache headers, but browsers may cache GET responses

**Warning signs:**
- Data appears correct after hard refresh (Ctrl+Shift+R) but not after normal navigation
- A form shows values from a previous submission
- List data doesn't reflect recent create/update/delete operations
- Role shown in the UI doesn't match the logged-in user (stale Pinia store)

**Detection:** Compare the API response in Network tab with what the UI displays. If they differ, it is a caching issue.

**Prevention:**
1. Use Incognito/Private browsing mode for each role test session
2. Between role switches: DevTools > Application > Clear site data
3. After any data mutation, refresh the page before asserting the result
4. For list pages, always click the search/query button rather than relying on auto-loaded data

**Workaround:** Hard refresh (Ctrl+Shift+R) clears component cache. For persistent issues, clear Application storage entirely and re-login.

---

### Pitfall 3: Database State Pollution Between Test Cases

**What goes wrong:** Test A creates a carbon report. Test B expects zero pending reports. Test B fails. The tests are not independent.

**Why it happens:**
- No automatic cleanup between manual test cases
- Flyway seed data (`V2__seed_data.sql`) provides baseline data but is not reset between tests
- All test operations write to the same MySQL database
- No test-specific database isolation (unlike `BaseIntegrationTest` which uses Testcontainers with `deleteAll()`)

**Warning signs:**
- A test that worked yesterday fails today without code changes
- Counts or totals are higher than expected
- "Duplicate entry" errors on unique constraints
- A role sees data created by a different role's test

**Detection:** Before starting a test scenario, query the relevant table to understand current state. Use the admin panel or direct MySQL client.

**Prevention:**
1. **Test in a fixed order:** Login tests first, then CRUD, then workflows that depend on created data
2. **Use unique identifiers:** Append timestamps to test data names (e.g., "TestReport_20260508_1430")
3. **Document expected seed state:** Know what `V2__seed_data.sql` creates before testing
4. **Reset when stuck:** `docker-compose down -v && docker-compose up` gives a clean database with fresh seed data

**Workaround:** If tests become polluted, restart the Docker stack with volume removal (`-v` flag) to get a clean database. This takes ~30 seconds but guarantees clean state.

**Critical note:** The project constraint says "use real backend API and database, no mock data." This means state pollution is inherent to the testing approach. Accept it and plan test order accordingly.

---

### Pitfall 4: Redis Cache Causing Stale Data in UI

**What goes wrong:** After changing system configuration (emission factors, permissions) through the admin panel, the system still uses old values. Tester reports "configuration change not working."

**Why it happens:**
- `CachePreloadService` loads emission factors and permissions into Redis on startup (`@EventListener(ApplicationReadyEvent.class)`)
- These values are hardcoded Java constants (`PERMISSIONS_CACHE`, emission factors) -- they are NOT read from the database (CONCERNS.md SEC-08)
- `getCacheStatistics()` uses `redisTemplate.keys("*")` which is O(N) and can cause latency (PERF-02)
- No cache invalidation endpoint exists for manual refresh

**Warning signs:**
- Admin changes a configuration value but the system behavior doesn't change
- Emission calculations use old factors after update
- Permission checks use stale role assignments
- Slow responses after accessing cache statistics page

**Detection:** Check Redis directly: `docker exec -it <redis-container> redis-cli GET <key>`. Compare with expected value.

**Prevention:**
1. After changing system configuration, restart the backend to reload cache
2. Know which values are cached vs. read from DB (emission factors = cached, user roles = DB)
3. Do NOT test emission factor changes expecting immediate effect -- they require a code change (SEC-08)

**Workaround:** Restart the backend container: `docker-compose restart backend`. Cache reloads on startup.

---

### Pitfall 5: Race Conditions in Trading Operations (False Failures)

**What goes wrong:** Two rapid trading operations produce inconsistent results -- double-matched orders, negative quotas, or duplicate transaction records. Tester reports a "data corruption bug."

**Why it happens:**
- `executeMatching()` is `synchronized` on the Spring singleton (CON-01)
- `@Transactional` on the synchronized method means the transaction commits AFTER the lock releases (CON-03)
- No `@Version` optimistic locking on any entity (CON-04)
- Quota updates in `updateEnterpriseQuota()` use separate `save()` calls without locking (CON-02)

**Warning signs:**
- Seller or buyer quota goes negative
- Same carbon credits appear in multiple matched orders
- Transaction amount doesn't match order quantity * price
- Matching results appear duplicated in the list

**Detection:** After placing orders, wait 2-3 seconds before triggering matching. Check the database directly for quota values.

**Prevention:**
1. **Test trading sequentially, never concurrently.** Place one order, wait for confirmation, then place the next.
2. **Never open two browser tabs placing orders simultaneously** for the same enterprise.
3. **Record initial quota values** before trading tests. Compare after each operation.
4. **Expect single-node behavior only.** The `synchronized` block works for one JVM instance. Do not test with multiple backend instances.

**Workaround:** If quotas become inconsistent, restart the backend (resets in-memory state) and re-query the database for actual quota values. Report as a known issue (CON-01/02/03), not a new bug.

---

### Pitfall 6: Role Switching Without Full Cleanup

**What goes wrong:** Tester logs out as ENTERPRISE, logs in as ADMIN, but the frontend still shows ENTERPRISE menu items or the backend returns ENTERPRISE-scoped data.

**Why it happens:**
- Access token is in `sessionStorage`, refresh token in `localStorage`, and a memory cache in `auth.ts`
- Pinia store (`useAppStore`) holds the current role in memory
- Vue Router guard checks `appStore.role` against `to.meta.roles`
- If `clearTokens()` is not fully executed during logout, stale tokens persist

**Warning signs:**
- Menu items from a previous role still visible
- 403 errors on endpoints that should be accessible for the new role
- "没有权限执行此操作" toast on operations that should work
- URL redirects to wrong role's home page

**Detection:** After login, check DevTools > Application > sessionStorage for `access_token`. Decode the JWT payload (base64) and verify the `roles` and `userType` fields match the expected role.

**Prevention:**
1. **Always use the logout button**, not browser back or manual URL navigation
2. **Between role switches:** Close all browser tabs, open a new Incognito window
3. **Verify role after login:** Check the menu items and home page URL match the expected role
4. **One role per Incognito window:** Never test two roles in the same browser session

**Workaround:** If role state is stuck, clear all browser storage (DevTools > Application > Clear site data), close the tab, and start fresh.

---

### Pitfall 7: CORS Errors Misdiagnosed as Backend Bugs

**What goes wrong:** API calls fail with CORS errors. Tester reports "backend is down" or "API not working."

**Why it happens:**
- CORS is configured in `SecurityConfig` with `allowedOrigins` from `app.cors.allowed-origins` (default: `http://localhost:5173`)
- If the frontend runs on a different port (e.g., 5174), all preflight requests fail
- CORS errors in the browser look like network failures, not permission errors
- SEC-04: If production config is missing, the localhost default leaks

**Warning signs:**
- Console error: "Access to XMLHttpRequest blocked by CORS policy"
- Network tab shows OPTIONS requests returning 403 or no response
- GET requests work but POST/PUT/DELETE fail (prefflight required for non-simple requests)
- Error appears only when frontend port changes

**Detection:** Check Network tab for OPTIONS requests. If they fail with 403, it is a CORS issue, not a backend bug.

**Prevention:**
1. Always run frontend on port 5173 (the configured default)
2. Use `npm run dev` from `oaiss-chain-frontend/` which defaults to 5173
3. If you must change the port, update `app.cors.allowed-origins` in `application.yml`

**Workaround:** Verify `app.cors.allowed-origins` includes your frontend URL. Restart backend after config change.

---

### Pitfall 8: @PreAuthorize Role Name Mismatches

**What goes wrong:** A role gets 403 on an endpoint that the router allows. Tester reports "RBAC is broken."

**Why it happens:**
- Frontend routes use `ROLE.ENTERPRISE`, `ROLE.REVIEWER`, etc. from `config/menu.ts`
- Backend `@PreAuthorize` uses `hasRole('ENTERPRISE')`, `hasRole('REVIEWER')`, etc.
- Some endpoints use `hasAnyRole()` with unexpected combinations
- Spring Security strips the `ROLE_` prefix, so `hasRole('ADMIN')` matches authority `ROLE_ADMIN`

**Warning signs:**
- 403 response on an endpoint the route guard should have blocked
- A role can access an endpoint via direct URL but not via menu
- Inconsistent behavior between frontend route guard and backend authorization

**Detection:** Compare `meta.roles` in the Vue Router definition with `@PreAuthorize` on the corresponding controller method. They should match.

**Prevention:**
1. Test each role by logging in and clicking through the menu -- do not use direct URLs
2. Verify that each role's home page loads without errors
3. Test negative cases: try accessing another role's URL directly and expect redirect to home

**Workaround:** If a role gets 403 on a legitimate endpoint, check the `@PreAuthorize` annotation. It may use a different role name or `hasAnyRole()` combination than expected.

---

### Pitfall 9: Pagination Off-by-One and Empty Page Issues

**What goes wrong:** Pagination shows wrong page numbers, "next" button doesn't work on the last page, or empty pages appear.

**Why it happens:**
- Frontend sends `pageNum`/`pageSize` which the Axios interceptor transforms to `page`/`size` (request.ts lines 44-53)
- Spring Data returns 0-based page numbers (`number` field)
- Frontend transforms `SpringPage` to `{ items, total, page, size, totalPages }` (request.ts lines 109-118)
- If `totalPages` calculation is off, the last page may be inaccessible

**Warning signs:**
- Clicking "next" on the last page shows empty results
- Page numbers start at 0 instead of 1
- Total count doesn't match the number of items shown
- Sorting by price returns unexpected order (ascending vs. descending)

**Detection:** Compare the API response's `totalElements` and `totalPages` with what the UI shows. Check the `page` parameter in the request URL.

**Prevention:**
1. Test with exactly 1 item (single page), exactly `pageSize` items (boundary), and `pageSize + 1` items (two pages)
2. Verify the last page has the correct number of items
3. Test sorting on the double auction page -- buy orders use descending price, sell orders use ascending

**Workaround:** If pagination is broken, try changing the page size or navigating to page 1. Report the specific boundary condition.

---

### Pitfall 10: i18n Validation Messages Not Displaying Correctly

**What goes wrong:** Form validation messages appear in Chinese when the UI is set to English, or vice versa. Tester reports "i18n is broken."

**Why it happens:**
- Frontend i18n migration is complete (vue-i18n 11), but backend error messages are still hardcoded in Chinese (QUAL-03, QUAL-08)
- `ErrorMessage.java` contains Chinese strings returned directly to the client
- Some services throw `BusinessException` with inline Chinese messages (e.g., "企业信誉分不存在")
- Element Plus validation rules may use hardcoded strings

**Warning signs:**
- Toast/error message in Chinese while the rest of the UI is in English
- Validation error message doesn't match the selected language
- Backend error response `message` field is always in Chinese regardless of frontend locale

**Detection:** Switch the UI language, then trigger a validation error. Check if the error message language matches.

**Prevention:**
1. Accept that backend error messages are in Chinese -- this is a known issue (QUAL-03)
2. Test i18n only on frontend-generated messages (form labels, menu items, page titles)
3. Do not report backend Chinese error messages as i18n bugs

**Workaround:** This is expected behavior. Document it as a known limitation for future i18n work.

---

### Pitfall 11: File Upload Failures Due to MinIO Issues

**What goes wrong:** File upload hangs, returns a generic error, or the file appears uploaded but cannot be downloaded.

**Why it happens:**
- `MinioService` has 14 broad `catch (Exception e)` blocks (QUAL-02) that mask root causes
- No MinIO health indicator exists (INFRA-03) -- storage outages are silent
- Upload timeout is 15 seconds (`REQUEST_TIMEOUT_MS` in request.ts) which may be insufficient for large files
- MinIO container may not be fully ready when the backend starts

**Warning signs:**
- Upload button spins indefinitely
- Generic "请求失败" error toast
- File appears in the list but download returns 404
- Upload works for small files but fails for larger ones

**Detection:** Check MinIO console directly (default: `http://localhost:9001`). Verify the file exists in the expected bucket.

**Prevention:**
1. Verify MinIO is running before testing uploads: `docker ps | grep minio`
2. Use small test files (< 100KB) for initial upload tests
3. Check the MinIO console to confirm files are stored correctly
4. Test download immediately after upload to verify the full cycle

**Workaround:** If uploads fail, restart MinIO: `docker-compose restart minio`. Wait 10 seconds for it to be ready. If the issue persists, check MinIO logs: `docker-compose logs minio`.

---

### Pitfall 12: Flyway Migration State Confusion

**What goes wrong:** Tester manually modifies the database schema, then Flyway fails on restart with "migration checksum mismatch" or "schema has been modified."

**Why it happens:**
- Flyway tracks applied migrations in `flyway_schema_history` table
- Versioned migrations (V1, V2) are run-once and checksummed
- Manual DDL changes (ALTER TABLE, CREATE TABLE) are not tracked by Flyway
- If the `rsa_key_pairs` table was added outside Flyway (INFRA-01), it creates schema drift

**Warning signs:**
- Backend fails to start with Flyway validation error
- "Schema `oaiss_chain` contains a failed migration" error
- Tables exist that are not in any migration script

**Detection:** Check `flyway_schema_history` table for applied migrations. Compare with `db/migration/` directory.

**Prevention:**
1. **Never modify the database schema manually.** Use Flyway migrations for all DDL changes.
2. If you need to reset, use `docker-compose down -v` to destroy volumes and start fresh
3. Do not add tables or columns outside of migration scripts

**Workaround:** If Flyway is stuck, reset the database: `docker-compose down -v && docker-compose up`. This destroys all data and re-runs migrations from scratch.

---

## Moderate Pitfalls

---

### Pitfall 13: Swagger Accessible in All Environments (SEC-03)

**What goes wrong:** Tester discovers Swagger UI is accessible and reports it as a security bug. This is already known (SEC-03) and is out of scope for this testing phase.

**Warning signs:** Navigating to `/swagger-ui.html` shows the API documentation.

**Prevention:** SEC-03 is tracked. Do not report it as a new finding. Focus on functional testing.

---

### Pitfall 14: Hardcoded Emission Factors Cannot Be Changed at Runtime (SEC-08)

**What goes wrong:** Tester tries to change emission factors through the admin panel, expecting it to affect calculations. The change has no effect.

**Why it happens:** `CachePreloadService` hardcodes emission factors as Java constants. They are not read from the database.

**Prevention:** Accept that emission factors are code constants. Do not test "change emission factor" as a runtime feature.

---

### Pitfall 15: Error Messages Leak Internal State (SEC-07)

**What goes wrong:** Tester notices error messages contain entity IDs, field names, or internal details. Reports as a security issue.

**Why it happens:** `ErrorMessage.java` and various services include entity IDs in error messages (e.g., "企业信誉分不存在: " + enterpriseId).

**Prevention:** This is a known issue (SEC-07). Document it but do not block functional testing.

---

### Pitfall 16: Inconsistent Error Code Usage (QUAL-08)

**What goes wrong:** Some error responses use numeric codes (4003), others use string codes (`TOKEN_INVALID`). Tester cannot predict error response format.

**Prevention:** Accept that error codes are inconsistent. Focus on the HTTP status code (401, 403, 400, 500) rather than the response body code.

---

### Pitfall 17: Broad Exception Catching in MinIO (QUAL-02)

**What goes wrong:** File operation failures return generic error messages. Tester cannot determine if the issue is network, permission, or file-not-found.

**Prevention:** When file operations fail, check MinIO logs directly: `docker-compose logs minio`. The generic error message is a known limitation.

---

## Minor Pitfalls

---

### Pitfall 18: Hardcoded Chinese Strings in Service Layer (QUAL-03)

**What goes wrong:** Backend returns Chinese strings like "买入"/"卖出" in API responses even when the frontend locale is English.

**Prevention:** Accept this as known behavior. The i18n migration covered the frontend; backend strings are a separate concern.

---

### Pitfall 19: No @Version on Any Entity (CON-04)

**What goes wrong:** If two operations modify the same entity record rapidly, the second write silently overwrites the first. No conflict detection.

**Prevention:** Test operations sequentially. Do not expect optimistic locking conflict errors.

---

### Pitfall 20: N+1 Queries in Matching Results (PERF-01)

**What goes wrong:** The matching results page loads slowly with many results. Each result triggers two additional queries.

**Prevention:** Use small page sizes (10 or fewer) when viewing matching results. Do not test performance -- focus on correctness.

---

## Known CONCERNS.md Issues That Cause False Test Failures

This section maps each CONCERNS.md issue to how it manifests during manual testing and whether it should be reported as a new bug.

| CONCERNS ID | Issue | False Failure Scenario | Report as New Bug? |
|-------------|-------|----------------------|-------------------|
| SEC-01 | RSA keys in DB | Digital signature works but keys are in plaintext | No -- known, out of scope |
| SEC-02 | CSRF disabled | POST requests succeed without CSRF token | No -- by design for JWT |
| SEC-03 | Swagger public | API docs accessible in test environment | No -- known, to be fixed |
| SEC-04 | CORS default localhost | CORS works only on port 5173 | No -- known, to be fixed |
| SEC-07 | Error messages leak state | Error responses contain entity IDs | No -- known |
| SEC-08 | Hardcoded emission factors | Cannot change factors at runtime | No -- known |
| CON-01 | synchronized matching | Sequential trading works, concurrent does not | No -- known, out of scope |
| CON-02 | Non-atomic quota updates | Quota mismatch after rapid trades | No -- known, out of scope |
| CON-03 | @Transactional on synchronized | Stale reads between lock release and commit | No -- known, out of scope |
| CON-04 | No @Version | Silent overwrites on concurrent edits | No -- known, out of scope |
| CON-05 | No readOnly=true | Unnecessary write locks on reads | No -- known, not user-visible |
| PERF-01 | N+1 queries | Slow matching results page | No -- known, out of scope |
| PERF-02 | Redis KEYS command | Slow cache statistics page | No -- known, out of scope |
| PERF-03 | Sync cache preload | Slow backend startup | No -- known, out of scope |
| PERF-04 | No pagination on matching | Memory pressure with many orders | No -- known, out of scope |
| QUAL-01 | Swallowed exceptions | User type shows "未知" silently | Possibly -- depends on frequency |
| QUAL-02 | Generic catch blocks | Unhelpful error messages on file ops | No -- known |
| QUAL-03 | Hardcoded Chinese | Chinese strings in English locale | No -- known |
| QUAL-05 | No input validation on orders | Negative price/quantity accepted | **Yes** -- this is a real bug to test and report |
| INFRA-01 | Missing migration for rsa_key_pairs | Schema drift if table added manually | Verify during testing |
| INFRA-03 | No MinIO health check | Silent storage failures | No -- known |
| INFRA-04 | cookies.txt in repo | N/A for manual testing | No -- not test-related |

**Key takeaway:** Only QUAL-05 (no input validation on auction orders) is a new bug worth testing and reporting. All other CONCERNS.md issues are known and should not be re-reported.

---

## Phase-Specific Warnings

| Test Phase (TEST-XX) | Likely Pitfall | Mitigation |
|----------------------|---------------|------------|
| TEST-01 (Login all roles) | Pitfall 6 (role switching cleanup) | Use separate Incognito windows per role |
| TEST-02 (Carbon report CRUD) | Pitfall 3 (DB state pollution) | Use unique report names with timestamps |
| TEST-03 (Auditor approval) | Pitfall 3 (depends on TEST-02 data) | Run TEST-02 first, note created report IDs |
| TEST-04 (Authenticator) | Pitfall 3 (depends on TEST-03 approved reports) | Run TEST-03 first |
| TEST-05 (Carbon coin) | Pitfall 1 (token expiration during long session) | Re-login before starting |
| TEST-06 (Double auction) | Pitfall 5 (race conditions), Pitfall 9 (pagination) | Sequential orders only, test pagination boundaries |
| TEST-07 (P2P trade) | Pitfall 5 (race conditions) | Sequential operations, one trade at a time |
| TEST-08 (Carbon neutral) | Pitfall 3 (state pollution) | Unique project names |
| TEST-09 (Credit score) | Pitfall 14 (hardcoded factors) | Read-only testing, do not expect runtime changes |
| TEST-10 (Admin) | Pitfall 4 (Redis cache stale) | Restart backend after config changes |
| TEST-11 (Third party) | Pitfall 8 (role access) | Verify menu items match expected role |
| TEST-12 (Digital signature) | Pitfall 11 (MinIO for key storage) | Verify MinIO is running |
| TEST-13 (File upload) | Pitfall 11 (MinIO issues) | Use small files, check MinIO console |
| TEST-14 (Blockchain) | None significant | Mock mode, straightforward |
| TEST-15 (Bug fixes) | Pitfall 12 (Flyway) | Do not modify schema manually |
| TEST-16 (Security fixes) | Pitfall 13, Pitfall 15 | Known issues, not new bugs |

---

## Pre-Test Checklist

Before starting any test session:

- [ ] Docker containers are running: `docker ps` (MySQL, Redis, MinIO, backend, frontend)
- [ ] Frontend is on port 5173: `http://localhost:5173`
- [ ] Backend is on port 8080: `http://localhost:8080/actuator/health`
- [ ] MinIO console accessible: `http://localhost:9001`
- [ ] Browser is in Incognito/Private mode
- [ ] DevTools is open (Network + Console tabs)
- [ ] Database is clean or state is documented
- [ ] Test order is planned (dependencies noted)

---

## Sources

- `.planning/codebase/CONCERNS.md` -- Known issues and tech debt (HIGH confidence)
- `.planning/PROJECT.md` -- Project context and test requirements (HIGH confidence)
- `.planning/codebase/TESTING.md` -- Existing test patterns (HIGH confidence)
- `SecurityConfig.java` -- CSRF disabled, CORS config, Swagger public (HIGH confidence, direct code)
- `JwtTokenProvider.java` -- Token expiration: 1hr access, 7-day refresh (HIGH confidence, direct code)
- `auth.ts` -- Token storage: sessionStorage + localStorage (HIGH confidence, direct code)
- `request.ts` -- Auto-refresh logic, pagination transform (HIGH confidence, direct code)
- `DoubleAuctionService.java` -- synchronized + @Transactional race condition (HIGH confidence, direct code)
- `router/index.ts` -- Role-based route guards (HIGH confidence, direct code)
- `CachePreloadService.java` -- Hardcoded values, KEYS command (HIGH confidence, direct code)
