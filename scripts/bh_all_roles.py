"""Browser-harness: Test all role pages with token injection."""
import json
import os
import sys
import time
from urllib.request import urlopen, Request
import re
import tempfile

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")

issues = []
page_results = []


def login_api(username, password):
    resp = urlopen(f"{API}/captcha/generate", timeout=5)
    data = json.loads(resp.read())
    captcha_key = data["data"]["captchaKey"]
    code = None
    for _ in range(15):
        try:
            with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            matches = re.findall(
                rf"generateCaptcha: key={captcha_key}, code=([A-Z0-9]{{4}})",
                content,
            )
            if matches:
                code = matches[-1]
                break
        except Exception:
            pass
        time.sleep(0.5)
    if not code:
        return None, None
    payload = json.dumps({
        "username": username, "password": password,
        "captchaKey": captcha_key, "captchaCode": code,
    }).encode()
    req = Request(f"{API}/auth/login", data=payload, headers={"Content-Type": "application/json"})
    resp = urlopen(req, timeout=5)
    data = json.loads(resp.read())
    if data.get("code") == 200:
        return data["data"]["accessToken"], data["data"].get("refreshToken", "")
    return None, None


def inject_token(access, refresh):
    # Inject in smaller chunks to avoid CDP timeout
    js('sessionStorage.clear()')
    js('localStorage.removeItem("access_token")')

    # Store token in parts
    half = len(access) // 2
    js(f'var t1 = "{access[:half]}"')
    js(f'var t2 = "{access[half:]}"')
    js('sessionStorage.setItem("access_token", t1 + t2)')

    if refresh:
        rh = len(refresh) // 2
        js(f'var r1 = "{refresh[:rh]}"')
        js(f'var r2 = "{refresh[rh:]}"')
        js('localStorage.setItem("refresh_token", r1 + r2)')


def test_pages(role, pages):
    results = []
    print(f"\n--- {role} Page Tests ---")
    for name, path in pages:
        url = f"http://localhost:5173{path}"
        try:
            goto_url(url)
            wait_for_load()
            time.sleep(1.5)
            current = js("window.location.href")
            tables = js('document.querySelectorAll(".el-table__body-wrapper tbody tr").length')
            forms = js('document.querySelectorAll(".el-form-item").length')
            body = js("document.body.innerText.substring(0, 150)")

            if "/login" in str(current):
                print(f"  FAIL {role}/{name}: redirected to login")
                results.append(("FAIL", name, "redirected to login"))
                issues.append({"role": role, "page": name, "severity": "HIGH", "desc": "Redirected to login"})
            else:
                print(f"  OK   {role}/{name}: tables={tables} forms={forms}")
                results.append(("OK", name, f"tables={tables} forms={forms}"))
        except Exception as e:
            print(f"  ERROR {role}/{name}: {e}")
            results.append(("ERROR", name, str(e)))
            issues.append({"role": role, "page": name, "severity": "MEDIUM", "desc": str(e)})
    return results


# ── MAIN ────────────────────────────────────────────────────────────────
print("=" * 60)
print("  Browser QA: All Roles Page Test")
print("=" * 60)

roles_config = {
    "ENTERPRISE": {
        "user": "enterprise001", "pwd": "admin123",
        "pages": [
            ("Carbon Upload", "/enterprise/carbon/upload"),
            ("P2P Orders", "/enterprise/orders/manage"),
            ("Trading Market", "/enterprise/trading/market"),
            ("P2P Trading", "/enterprise/trading/p2p"),
            ("Company Dashboard", "/enterprise/company/dashboard"),
            ("Credit Score", "/enterprise/credit/score"),
            ("Carbon Coin", "/enterprise/carbon-coin/account"),
            ("Blockchain", "/enterprise/blockchain/browser"),
            ("Carbon Neutral", "/enterprise/carbon-neutral/projects"),
            ("Emission Data", "/enterprise/emission/data"),
            ("User Profile", "/enterprise/user/profile"),
        ],
    },
    "REVIEWER": {
        "user": "reviewer001", "pwd": "admin123",
        "pages": [
            ("Audit List", "/auditor/audit/list"),
        ],
    },
    "THIRD_PARTY": {
        "user": "thirdparty001", "pwd": "admin123",
        "pages": [
            ("Monitor", "/third-party/monitor"),
        ],
    },
    "ADMIN": {
        "user": "admin", "pwd": "admin123",
        "pages": [
            ("Users", "/admin/system/users"),
            ("Carbon Mgmt", "/admin/system/carbon"),
            ("Config", "/admin/system/config"),
            ("Statistics", "/admin/data/statistics"),
            ("Verify List", "/admin/verify/list"),
        ],
    },
}

all_results = []
for role, cfg in roles_config.items():
    print(f"\n  Logging in as {cfg['user']}...")
    access, refresh = login_api(cfg["user"], cfg["pwd"])
    if not access:
        print(f"  LOGIN FAILED for {role}")
        for name, _ in cfg["pages"]:
            all_results.append(("FAIL", name, "login failed"))
            issues.append({"role": role, "page": name, "severity": "CRITICAL", "desc": "Login failed"})
        continue

    inject_token(access, refresh)
    results = test_pages(role, cfg["pages"])
    all_results.extend(results)

# ── Summary ─────────────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("  FINAL SUMMARY")
print("=" * 60)
ok = sum(1 for r in all_results if r[0] == "OK")
fail = sum(1 for r in all_results if r[0] == "FAIL")
err = sum(1 for r in all_results if r[0] == "ERROR")
total = len(all_results)
print(f"  Total: {total} | OK: {ok} | FAIL: {fail} | ERROR: {err}")
print(f"  Pass Rate: {ok/total*100:.1f}%")

if issues:
    print(f"\n  Issues ({len(issues)}):")
    for iss in issues:
        print(f"    [{iss['severity']}] {iss['role']}/{iss['page']}: {iss['desc']}")
else:
    print("\n  No issues found!")
