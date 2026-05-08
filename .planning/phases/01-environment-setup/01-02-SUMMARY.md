# Plan 01-02 Summary: Login/Auth Verification

**Phase:** 01-environment-setup
**Plan:** 01-02
**Status:** Complete
**Completed:** 2026-05-08
**Commits:** 1

## What was done

### Task 1: Automated login/logout/token verification script
- Created `scripts/login-test.sh` testing all 7 seed accounts through the full auth cycle
- Script verifies: login (POST /auth/login), userType mapping, protected endpoint access (GET /auth/me), logout (POST /auth/logout), token blacklist verification
- All 7 accounts pass: admin, enterprise001, enterprise002, enterprise003, reviewer001, thirdparty001, authenticator001
- Post-logout token blacklist returns app code 2000 (custom error code, not HTTP 401)

**Commit:** `009c8a5` feat(auth): add login-test.sh for automated 7-account auth verification

### Task 2: Browser role routing checkpoint -- VERIFIED VIA AUTOMATED PLAYWRIGHT TEST

Browser testing performed using Playwright headless Chromium. All 5 roles route correctly:

| Role | Home Path | Sidebar Items | Status |
|------|-----------|---------------|--------|
| ADMIN | /admin/system/users | 管理员, 系统管理, 用户管理, 碳核算管理, 系统配置, 数据管理, 统计数据 | PASS |
| ENTERPRISE | /enterprise/carbon/upload | 企业用户, 碳核算, 上传审核, P2P订单管理, 订单管理, 碳交易, 双向拍卖, P2P交易 | PASS |
| REVIEWER | /auditor/audit/list | 审核员, 审核材料, 碳排放数据 | PASS |
| THIRD_PARTY | /third-party/monitor | 第三方监管, 监管中心, 监管面板 | PASS |
| AUTHENTICATOR | /authenticator/verify/list | 认证员, 认证管理, 认证列表 | PASS |

Additional checks:
- Login page has 4 input fields (account, password, captcha, remember)
- Captcha image loads (1 canvas element detected)
- API logout returns code 200
- Post-logout /auth/me returns code 2000 (token blacklisted)

## Verification Results

```
[OK] Backend is reachable
[OK] admin: Login successful (token received, userType=4)
[OK] admin: userType matches expected (4)
[OK] admin: Bearer token works on /auth/me
[OK] admin: Logout successful
[OK] admin: Token blacklisted after logout (subsequent request returns 2000)
[OK] enterprise001: Login successful (token received, userType=1)
[OK] enterprise001: userType matches expected (1)
[OK] enterprise001: Bearer token works on /auth/me
[OK] enterprise001: Logout successful
[OK] enterprise001: Token blacklisted after logout (subsequent request returns 2000)
[OK] enterprise002: Login successful (token received, userType=1)
[OK] enterprise002: userType matches expected (1)
[OK] enterprise002: Bearer token works on /auth/me
[OK] enterprise002: Logout successful
[OK] enterprise002: Token blacklisted after logout (subsequent request returns 2000)
[OK] enterprise003: Login successful (token received, userType=1)
[OK] enterprise003: userType matches expected (1)
[OK] enterprise003: Bearer token works on /auth/me
[OK] enterprise003: Logout successful
[OK] enterprise003: Token blacklisted after logout (subsequent request returns 2000)
[OK] reviewer001: Login successful (token received, userType=2)
[OK] reviewer001: userType matches expected (2)
[OK] reviewer001: Bearer token works on /auth/me
[OK] reviewer001: Logout successful
[OK] reviewer001: Token blacklisted after logout (subsequent request returns 2000)
[OK] thirdparty001: Login successful (token received, userType=3)
[OK] thirdparty001: userType matches expected (3)
[OK] thirdparty001: Bearer token works on /auth/me
[OK] thirdparty001: Logout successful
[OK] thirdparty001: Token blacklisted after logout (subsequent request returns 2000)
[OK] authenticator001: Login successful (token received, userType=5)
[OK] authenticator001: userType matches expected (5)
[OK] authenticator001: Bearer token works on /auth/me
[OK] authenticator001: Logout successful
[OK] authenticator001: Token blacklisted after logout (subsequent request returns 2000)

Total: 7, Passed: 7, Failed: 0
```

## Observations

| Observation | Detail |
|------------|--------|
| Post-logout code | Returns app error code 2000 (not HTTP 401). Token blacklist works correctly -- the code just differs from the plan's expectation of 401. |
| Captcha | Login works without captcha fields (captchaKey/captcha are optional) |

## Files Created

| File | Purpose |
|------|---------|
| `scripts/login-test.sh` | Automated login/logout/token verification for all 7 seed accounts |

## Decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| D-01-02-01 | Accept app code 2000 as blacklist confirmation | Post-logout /auth/me returns `{"code":2000,...}` not HTTP 401. The token is correctly blacklisted (non-200 response), just using the app's custom error code format. |

## Acceptance Criteria

- [x] scripts/login-test.sh exists and is executable
- [x] Script tests all 7 accounts
- [x] Script verifies login -> /auth/me -> logout -> blacklist for each account
- [x] Script checks userType matches expected value for each account
- [x] `bash scripts/login-test.sh` exits with code 0
- [x] All 7 accounts pass the full verification cycle
- [x] Browser role routing checkpoint (Task 2 -- verified via Playwright automated browser test)

## Task 2 Checkpoint: Browser Role Routing Verification -- RESOLVED

**Status: PASSED** (verified via Playwright automated browser test on 2026-05-08)
