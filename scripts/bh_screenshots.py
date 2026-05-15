"""Browser-harness: Capture screenshots of all role pages for visual inspection."""
import json
import os
import sys
import time
import tempfile
import re
from urllib.request import urlopen, Request

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "..", "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


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
    js('sessionStorage.clear()')
    half = len(access) // 2
    js(f'var t1 = "{access[:half]}"')
    js(f'var t2 = "{access[half:]}"')
    js('sessionStorage.setItem("access_token", t1 + t2)')
    if refresh:
        rh = len(refresh) // 2
        js(f'var r1 = "{refresh[:rh]}"')
        js(f'var r2 = "{refresh[rh:]}"')
        js('localStorage.setItem("refresh_token", r1 + r2)')


def take_screenshots(role, access, refresh, pages):
    inject_token(access, refresh)
    print(f"\n--- {role} Screenshots ---")
    for name, path in pages:
        url = f"http://localhost:5173{path}"
        try:
            goto_url(url)
            wait_for_load()
            time.sleep(2)
            filename = f"{role}_{name.replace(' ', '_').replace('/', '_')}.png"
            filepath = os.path.join(SCREENSHOT_DIR, filename)
            capture_screenshot(filepath)
            print(f"  OK: {filename}")

            # Also get page text for content analysis
            body = js("document.body.innerText.substring(0, 300)")
            print(f"  Text: {body[:100]}...")
        except Exception as e:
            print(f"  FAIL: {name} - {e}")


# ── MAIN ────────────────────────────────────────────────────────────────
print("=" * 60)
print("  Screenshot Capture: All Roles")
print("=" * 60)

roles_config = {
    "ENTERPRISE": {
        "user": "enterprise001", "pwd": "admin123",
        "pages": [
            ("Carbon_Upload", "/enterprise/carbon/upload"),
            ("Company_Dashboard", "/enterprise/company/dashboard"),
            ("Trading_Market", "/enterprise/trading/market"),
            ("Credit_Score", "/enterprise/credit/score"),
            ("Blockchain", "/enterprise/blockchain/browser"),
            ("User_Profile", "/enterprise/user/profile"),
        ],
    },
    "REVIEWER": {
        "user": "reviewer001", "pwd": "admin123",
        "pages": [("Audit_List", "/auditor/audit/list")],
    },
    "THIRD_PARTY": {
        "user": "thirdparty001", "pwd": "admin123",
        "pages": [("Monitor", "/third-party/monitor")],
    },
    "ADMIN": {
        "user": "admin", "pwd": "admin123",
        "pages": [
            ("Users", "/admin/system/users"),
            ("Config", "/admin/system/config"),
            ("Statistics", "/admin/data/statistics"),
            ("Verify_List", "/admin/verify/list"),
        ],
    },
}

for role, cfg in roles_config.items():
    print(f"\n  Logging in as {cfg['user']}...")
    access, refresh = login_api(cfg["user"], cfg["pwd"])
    if not access:
        print(f"  LOGIN FAILED for {role}")
        continue
    take_screenshots(role, access, refresh, cfg["pages"])

print(f"\n  Screenshots saved to: {SCREENSHOT_DIR}")
print("  DONE")
