"""
Browser QA: Full-Stack Integration Test via API + Frontend
Tests all role pages and API endpoints for end-to-end integration issues
"""
import json, time, tempfile, os, re, sys, requests

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")
results = []
issues = []


def get_captcha_and_login(username, password):
    resp = requests.get(f"{API}/captcha/generate", timeout=5)
    data = resp.json()
    captcha_key = data["data"]["captchaKey"]

    code = None
    for _ in range(15):
        try:
            with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            # Match: generateCaptcha: key=CAP_xxx, code=XXXX
            matches = re.findall(rf"generateCaptcha: key={captcha_key}, code=([A-Z0-9]{{4}})", content)
            if matches:
                code = matches[-1]
                break
        except Exception:
            pass
        time.sleep(0.5)

    if not code:
        return None, "Cannot get captcha code"

    resp = requests.post(f"{API}/auth/login", json={
        "username": username,
        "password": password,
        "captchaKey": captcha_key,
        "captchaCode": code
    }, timeout=5)

    data = resp.json()
    if data.get("code") == 200:
        return data["data"]["accessToken"], None
    else:
        return None, data.get("message", "unknown")


def api_test(name, method, path, token=None, data=None, expected_code=200):
    """Test an API endpoint and record results"""
    url = f"{API}{path}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    try:
        if method == "GET":
            resp = requests.get(url, headers=headers, timeout=10)
        elif method == "POST":
            resp = requests.post(url, json=data, headers=headers, timeout=10)
        elif method == "PUT":
            resp = requests.put(url, json=data, headers=headers, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(url, headers=headers, timeout=10)
        else:
            return {"name": name, "status": "SKIP", "error": f"Unknown method {method}"}

        body = resp.json()
        ok = body.get("code") == expected_code

        result = {
            "name": name,
            "method": method,
            "path": path,
            "status": "OK" if ok else "FAIL",
            "http_code": resp.status_code,
            "api_code": body.get("code"),
            "message": body.get("message", ""),
        }

        if not ok:
            result["expected"] = expected_code
            result["actual_body"] = json.dumps(body, ensure_ascii=False)[:200]
            issues.append({
                "name": name,
                "path": path,
                "expected": expected_code,
                "actual": body.get("code"),
                "message": body.get("message", ""),
            })

        return result
    except Exception as e:
        result = {"name": name, "status": "ERROR", "error": str(e)}
        issues.append({"name": name, "path": path, "error": str(e)})
        return result


# ===== START TESTING =====
print("=" * 60)
print("  Browser QA: Full-Stack Integration Test")
print("=" * 60)

# Step 1: Login all roles
print("\n--- Step 1: Login All Roles ---")
tokens = {}
for role, user, pwd in [
    ("admin", "admin", "admin123"),
    ("enterprise", "enterprise001", "admin123"),
    ("reviewer", "reviewer001", "admin123"),
    ("thirdparty", "thirdparty001", "admin123"),
]:
    token, err = get_captcha_and_login(user, pwd)
    if token:
        tokens[role] = token
        print(f"  {role}: OK")
    else:
        print(f"  {role}: FAILED - {err}")

# Step 2: Test Frontend pages load correctly (via API data check)
print("\n--- Step 2: Enterprise Pages Data Check ---")
ent = tokens.get("enterprise")

# Carbon Upload page data
r = api_test("ent_carbon_list", "GET", "/carbon/my-reports", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Carbon reports - code={r.get('api_code')}")

# Company Dashboard data
r = api_test("ent_company_info", "GET", "/enterprise/info", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Enterprise info - code={r.get('api_code')}")

# Credit Score page data
r = api_test("ent_credit_history", "GET", "/credit/history", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Credit history - code={r.get('api_code')}")

# Carbon Coin account data
r = api_test("ent_carbon_coin_account", "GET", "/carbon-coin/account", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Carbon coin account - code={r.get('api_code')}")

# Blockchain browser data
r = api_test("ent_blockchain_list", "GET", "/blockchain/transactions", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Blockchain transactions - code={r.get('api_code')}")

# Carbon Neutral Projects
r = api_test("ent_carbon_neutral", "GET", "/carbon-neutral/projects", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Carbon neutral projects - code={r.get('api_code')}")

# Emission data (ratings for enterprise)
r = api_test("ent_emission_ratings", "GET", "/emission/ratings/1", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Emission ratings - code={r.get('api_code')}")

# Trading market (double auction orders)
r = api_test("ent_trading_market", "GET", "/auction/orders", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Trading market - code={r.get('api_code')}")

# P2P trading - enterprise uses my-trades (list is admin/reviewer/thirdparty only)
r = api_test("ent_trading_p2p", "GET", "/trade/my-trades", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: My P2P trades - code={r.get('api_code')}")

# My trades
r = api_test("ent_orders", "GET", "/trade/my-trades", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: My trades - code={r.get('api_code')}")

# User profile
r = api_test("ent_profile", "GET", "/user/profile", ent)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: User profile - code={r.get('api_code')}")

# Step 3: Reviewer pages
print("\n--- Step 3: Reviewer Pages Data Check ---")
rev = tokens.get("reviewer")

# Audit list - pending reviews
r = api_test("rev_pending_reviews", "GET", "/reviewer/reports/pending", rev)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Pending reviews - code={r.get('api_code')}")

# Review history
r = api_test("rev_review_history", "GET", "/reviewer/history", rev)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Review history - code={r.get('api_code')}")

# Step 4: ThirdParty pages
print("\n--- Step 4: ThirdParty Pages Data Check ---")
tp = tokens.get("thirdparty")

# Monitor data (carbon reports)
r = api_test("tp_monitor_data", "GET", "/third-party/carbon-reports", tp)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Third-party carbon reports - code={r.get('api_code')}")

# Step 5: Admin pages
print("\n--- Step 5: Admin Pages Data Check ---")
adm = tokens.get("admin")

# User management
r = api_test("adm_users_list", "GET", "/admin/users", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin users list - code={r.get('api_code')}")

# Dashboard (replaces carbon reports)
r = api_test("adm_dashboard", "GET", "/admin/dashboard", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin dashboard - code={r.get('api_code')}")

# System config
r = api_test("adm_config_list", "GET", "/admin/config", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin config - code={r.get('api_code')}")

# Statistics
r = api_test("adm_statistics", "GET", "/admin/statistics", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin statistics - code={r.get('api_code')}")

# Permissions list (replaces verify list)
r = api_test("adm_permissions", "GET", "/admin/permissions", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin permissions - code={r.get('api_code')}")

# Step 6: Cross-role permission tests
print("\n--- Step 6: Cross-Role Permission Tests ---")

# Enterprise trying to access admin
r = api_test("perm_ent_to_admin", "GET", "/admin/users", ent, expected_code=2004)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Ent->Admin blocked (expected 2004)")

# Reviewer trying to access admin
r = api_test("perm_rev_to_admin", "GET", "/admin/users", rev, expected_code=2004)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Rev->Admin blocked (expected 2004)")

# Admin accessing enterprise by ID (SPEC-009 fix verification)
r = api_test("perm_adm_to_ent", "GET", "/enterprise/1", adm)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin->Enterprise by ID - code={r.get('api_code')}")

# Admin accessing /enterprise/info (should be blocked - enterprise only)
r = api_test("perm_adm_ent_info", "GET", "/enterprise/info", adm, expected_code=2004)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Admin->Enterprise info blocked (expected 2004)")

# Reviewer accessing pending-verification (SPEC-008 fix verification)
r = api_test("perm_rev_pending_verify", "GET", "/carbon-neutral/pending-verification", rev)
results.append(r)
print(f"  {'OK' if r['status']=='OK' else 'FAIL'}: Rev->Pending verification - code={r.get('api_code')}")

# Step 7: Refresh token error code test (SPEC-010 fix)
print("\n--- Step 7: SPEC-010 Verification ---")
# Test with no Refresh-Token header (should return 2002 not 1000)
r = requests.post(f"{API}/auth/refresh", timeout=5)
body = r.json()
spec010_ok = body.get("code") == 2002  # token invalid, not system error
results.append({
    "name": "spec010_refresh_no_token",
    "status": "OK" if spec010_ok else "FAIL",
    "api_code": body.get("code"),
    "message": body.get("message", ""),
})
print(f"  {'OK' if spec010_ok else 'FAIL'}: Refresh no header - code={body.get('code')} msg={body.get('message', '')}")

# ===== SUMMARY =====
print("\n" + "=" * 60)
print("  Test Summary")
print("=" * 60)
ok_count = sum(1 for r in results if r["status"] == "OK")
fail_count = sum(1 for r in results if r["status"] == "FAIL")
err_count = sum(1 for r in results if r["status"] == "ERROR")
total = len(results)
print(f"  Total: {total} | OK: {ok_count} | FAIL: {fail_count} | ERROR: {err_count}")
print(f"  Pass Rate: {ok_count/total*100:.1f}%")

if issues:
    print(f"\n--- Issues Found ({len(issues)}) ---")
    for iss in issues:
        if "error" in iss:
            print(f"  [ERROR] {iss['name']}: {iss['error']}")
        else:
            print(f"  [FAIL] {iss['name']}: {iss['path']} expected={iss.get('expected')} actual={iss.get('actual')} msg={iss.get('message', '')}")
else:
    print("\n  No issues found!")

print("\n--- All Results ---")
for r in results:
    icon = "OK" if r["status"] == "OK" else "FAIL"
    code = r.get("api_code", r.get("error", ""))
    print(f"  [{icon}] {r['name']}: api_code={code}")
