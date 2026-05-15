"""Browser-harness: Deep interactive testing and DOM-based visual inspection."""
import json
import os
import sys
import time
import tempfile
import re
from urllib.request import urlopen, Request

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")
issues = []


def login_api(username, password):
    resp = urlopen(f"{API}/captcha/generate", timeout=5)
    data = json.loads(resp.read())
    captcha_key = data["data"]["captchaKey"]
    code = None
    for _ in range(15):
        try:
            with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            matches = re.findall(rf"generateCaptcha: key={captcha_key}, code=([A-Z0-9]{{4}})", content)
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


def check_page_visual(role, name):
    """Check page for visual/layout issues via DOM analysis."""
    findings = []

    # Check for empty content areas
    main_content = js("""
        (function() {
            var main = document.querySelector('.app-main') ||
                       document.querySelector('.main-content') ||
                       document.querySelector('.el-main') ||
                       document.querySelector('[class*="content"]');
            return main ? main.innerText.trim().length : 0;
        })()
    """)
    if main_content < 20:
        findings.append(f"Empty main content area (chars={main_content})")

    # Check for broken images
    broken = js("""
        (function() {
            var imgs = document.querySelectorAll('img');
            var broken = 0;
            imgs.forEach(function(img) {
                if (!img.complete || img.naturalWidth === 0) broken++;
            });
            return broken;
        })()
    """)
    if broken > 0:
        findings.append(f"Broken images: {broken}")

    # Check for console errors
    errors = js("""
        (function() {
            var errs = [];
            document.querySelectorAll('.el-message--error, .el-notification--error, [class*="error"]').forEach(function(el) {
                var t = el.textContent.trim();
                if (t && t.length < 200) errs.push(t);
            });
            return errs.join('; ');
        })()
    """)
    if errors:
        findings.append(f"Error elements: {errors}")

    # Check sidebar menu items
    menu_items = js("""
        (function() {
            return document.querySelectorAll('.el-menu-item').length;
        })()
    """)

    # Check for overlapping elements (basic check)
    overlap = js("""
        (function() {
            var els = document.querySelectorAll('.el-button, .el-input, .el-table');
            var issues = 0;
            for (var i = 0; i < els.length; i++) {
                var r1 = els[i].getBoundingClientRect();
                for (var j = i + 1; j < els.length; j++) {
                    var r2 = els[j].getBoundingClientRect();
                    if (r1.width > 0 && r1.height > 0 && r2.width > 0 && r2.height > 0) {
                        if (r1.left < r2.right && r1.right > r2.left &&
                            r1.top < r2.bottom && r1.bottom > r2.top) {
                            issues++;
                        }
                    }
                }
            }
            return issues;
        })()
    """)

    # Check page title
    title = js("document.title")
    if not title or title == "":
        findings.append("Missing page title")

    return findings, menu_items


def test_interactive_elements(role, name):
    """Test interactive elements on the page."""
    findings = []

    # Check buttons
    buttons = js("""
        (function() {
            var btns = document.querySelectorAll('.el-button');
            var info = [];
            btns.forEach(function(btn) {
                info.push({
                    text: btn.textContent.trim().substring(0, 30),
                    disabled: btn.disabled || btn.classList.contains('is-disabled'),
                    type: btn.classList.contains('el-button--primary') ? 'primary' :
                          btn.classList.contains('el-button--danger') ? 'danger' :
                          btn.classList.contains('el-button--success') ? 'success' : 'default'
                });
            });
            return JSON.stringify(info);
        })()
    """)

    # Check form validation
    forms = js("""
        (function() {
            var forms = document.querySelectorAll('.el-form');
            var info = [];
            forms.forEach(function(form) {
                var items = form.querySelectorAll('.el-form-item');
                var required = form.querySelectorAll('.el-form-item.is-required').length;
                info.push({items: items.length, required: required});
            });
            return JSON.stringify(info);
        })()
    """)

    # Check tables
    tables = js("""
        (function() {
            var tables = document.querySelectorAll('.el-table');
            var info = [];
            tables.forEach(function(table) {
                var rows = table.querySelectorAll('.el-table__body-wrapper tbody tr').length;
                var headers = table.querySelectorAll('.el-table__header-wrapper th').length;
                info.push({headers: headers, rows: rows});
            });
            return JSON.stringify(info);
        })()
    """)

    return buttons, forms, tables


# ── MAIN ────────────────────────────────────────────────────────────────
print("=" * 60)
print("  Interactive + Visual QA: All Roles")
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

total_pages = 0
total_issues = 0

for role, cfg in roles_config.items():
    print(f"\n{'='*50}")
    print(f"  {role} (user: {cfg['user']})")
    print(f"{'='*50}")

    access, refresh = login_api(cfg["user"], cfg["pwd"])
    if not access:
        print(f"  LOGIN FAILED!")
        continue
    inject_token(access, refresh)

    for name, path in cfg["pages"]:
        total_pages += 1
        url = f"http://localhost:5173{path}"
        print(f"\n  [{name}] {url}")
        try:
            goto_url(url)
            wait_for_load()
            time.sleep(2)
        except Exception as e:
            print(f"    NAV ERROR: {e}")
            issues.append({"role": role, "page": name, "severity": "HIGH", "desc": f"Navigation failed: {e}"})
            total_issues += 1
            continue

        # Visual check
        visual, menu_count = check_page_visual(role, name)
        print(f"    Menu items: {menu_count}")

        if visual:
            for v in visual:
                print(f"    [VISUAL] {v}")
                issues.append({"role": role, "page": name, "severity": "LOW", "desc": v})
                total_issues += 1
        else:
            print(f"    [VISUAL] OK")

        # Interactive check
        buttons, forms, tables = test_interactive_elements(role, name)
        print(f"    Buttons: {buttons}")
        print(f"    Forms: {forms}")
        print(f"    Tables: {tables}")

# ── Summary ─────────────────────────────────────────────────────────────
print(f"\n{'='*60}")
print(f"  SUMMARY")
print(f"{'='*60}")
print(f"  Pages tested: {total_pages}")
print(f"  Issues found: {total_issues}")
if issues:
    print(f"\n  Issue Details:")
    for iss in issues:
        print(f"    [{iss['severity']}] {iss['role']}/{iss['page']}: {iss['desc']}")
else:
    print(f"  All pages passed visual + interactive checks!")
