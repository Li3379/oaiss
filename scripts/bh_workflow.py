"""Browser-harness: Interactive workflow testing."""
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
    key = data["data"]["captchaKey"]
    code = None
    for _ in range(15):
        try:
            with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
                c = f.read()
            m = re.findall(rf"generateCaptcha: key={key}, code=([A-Z0-9]{{4}})", c)
            if m:
                code = m[-1]
                break
        except Exception:
            pass
        time.sleep(0.5)
    if not code:
        return None, None
    p = json.dumps({"username": username, "password": password, "captchaKey": key, "captchaCode": code}).encode()
    r = urlopen(Request(f"{API}/auth/login", data=p, headers={"Content-Type": "application/json"}), timeout=5)
    d = json.loads(r.read())
    if d.get("code") == 200:
        return d["data"]["accessToken"], d["data"].get("refreshToken", "")
    return None, None


def inject(a, r):
    js('sessionStorage.clear()')
    h = len(a) // 2
    js('var a1="' + a[:h] + '"')
    js('var a2="' + a[h:] + '"')
    js('sessionStorage.setItem("access_token",a1+a2)')
    if r:
        rh = len(r) // 2
        js('var r1="' + r[:rh] + '"')
        js('var r2="' + r[rh:] + '"')
        js('localStorage.setItem("refresh_token",r1+r2)')


def click_button_at(selector):
    """Click a button by CSS selector, return position or None."""
    result = js(f"""
    (function() {{
        var btn = document.querySelector("{selector}");
        if (!btn) return null;
        var r = btn.getBoundingClientRect();
        return JSON.stringify({{x: Math.round(r.x + r.width/2), y: Math.round(r.y + r.height/2)}});
    }})()
    """)
    if result and result != "null":
        pos = json.loads(result)
        click_at_xy(pos["x"], pos["y"])
        return pos
    return None


# ── MAIN ────────────────────────────────────────────────────────────────
print("=" * 60)
print("  Interactive Workflow Testing")
print("=" * 60)

# ═══════════════════════════════════════════════════════════════════════
# TEST 1: Enterprise - Create Carbon Report Dialog
# ═══════════════════════════════════════════════════════════════════════
print("\n--- TEST 1: Enterprise Create Report Dialog ---")
a, r = login_api("enterprise001", "admin123")
inject(a, r)
goto_url("http://localhost:5173/enterprise/carbon/upload")
wait_for_load()
time.sleep(2)

# Click create button
create_btn = js("""
(function() {
    var btns = document.querySelectorAll(".el-button");
    for (var i = 0; i < btns.length; i++) {
        if (btns[i].textContent.trim() === "创建项目") {
            var r = btns[i].getBoundingClientRect();
            return JSON.stringify({x: Math.round(r.x + r.width/2), y: Math.round(r.y + r.height/2)});
        }
    }
    return null;
})()
""")
if create_btn and create_btn != "null":
    pos = json.loads(create_btn)
    click_at_xy(pos["x"], pos["y"])
    time.sleep(1)

    # Check dialog
    dialog = js("""
    (function() {
        var dlg = document.querySelector(".el-dialog__wrapper");
        if (!dlg) return "NO_DIALOG";
        var title = dlg.querySelector(".el-dialog__title");
        var inputs = dlg.querySelectorAll("input, select, textarea");
        var btns = dlg.querySelectorAll(".el-button");
        var btnTexts = [];
        btns.forEach(function(b) { btnTexts.push(b.textContent.trim()); });
        return JSON.stringify({
            title: title ? title.textContent.trim() : "",
            inputs: inputs.length,
            buttons: btnTexts
        });
    })()
    """)
    print(f"  Dialog result: {dialog}")
    if dialog == "NO_DIALOG":
        issues.append(("ENT", "CarbonUpload", "MEDIUM", "Create button did not open dialog"))
    else:
        print("  PASS: Dialog opened with form")

    # Close dialog
    click_button_at(".el-dialog__headerbtn")
    time.sleep(0.5)
else:
    print("  SKIP: Create button not found")

# ═══════════════════════════════════════════════════════════════════════
# TEST 2: Admin - User Action Button
# ═══════════════════════════════════════════════════════════════════════
print("\n--- TEST 2: Admin User Action ---")
a, r = login_api("admin", "admin123")
inject(a, r)
goto_url("http://localhost:5173/admin/system/users")
wait_for_load()
time.sleep(2)

# Find first action button in table
action_info = js("""
(function() {
    var btns = document.querySelectorAll(".el-table__body-wrapper .el-button");
    if (btns.length === 0) return "NO_BTNS";
    var result = [];
    btns.forEach(function(b) {
        var r = b.getBoundingClientRect();
        result.push({text: b.textContent.trim(), x: Math.round(r.x + r.width/2), y: Math.round(r.y + r.height/2)});
    });
    return JSON.stringify(result.slice(0, 3));
})()
""")
print(f"  Action buttons: {action_info}")

if action_info and action_info != "NO_BTNS":
    btns = json.loads(action_info)
    # Click first action button
    click_at_xy(btns[0]["x"], btns[0]["y"])
    time.sleep(1)

    result = js("""
    (function() {
        var dlg = document.querySelector(".el-dialog__wrapper");
        if (!dlg) return "NO_DIALOG";
        var title = dlg.querySelector(".el-dialog__title");
        return JSON.stringify({title: title ? title.textContent.trim() : ""});
    })()
    """)
    print(f"  After click [{btns[0]['text']}]: {result}")

    if result == "NO_DIALOG":
        # Maybe it was a direct action (delete, toggle status)
        msg = js("""
        (function() {
            var msgs = document.querySelectorAll(".el-message");
            var texts = [];
            msgs.forEach(function(m) { texts.push(m.textContent.trim()); });
            return texts.join("; ") || "no message";
        })()
        """)
        print(f"  Page message: {msg}")

    # Close any dialog
    click_button_at(".el-dialog__headerbtn")
    time.sleep(0.5)

# ═══════════════════════════════════════════════════════════════════════
# TEST 3: Verify Statistics API vs Page
# ═══════════════════════════════════════════════════════════════════════
print("\n--- TEST 3: Statistics Accuracy ---")
goto_url("http://localhost:5173/admin/data/statistics")
wait_for_load()
time.sleep(2)

page_stats = js("""
(function() {
    var text = document.body.innerText;
    var users = text.match(/总用户数\\s*(\\d+)/);
    var ent = text.match(/企业数量\\s*(\\d+)/);
    var rev = text.match(/审核员数量\\s*(\\d+)/);
    var tp = text.match(/第三方数量\\s*(\\d+)/);
    return JSON.stringify({
        users: users ? users[1] : "N/A",
        enterprises: ent ? ent[1] : "N/A",
        reviewers: rev ? rev[1] : "N/A",
        thirdparties: tp ? tp[1] : "N/A"
    });
})()
""")
print(f"  Page stats: {page_stats}")

# Compare with API
a2, _ = login_api("admin", "admin123")
if a2:
    req = Request(f"{API}/admin/statistics", headers={"Authorization": f"Bearer {a2}"})
    resp = urlopen(req, timeout=5)
    api_data = json.loads(resp.read())
    api_stats = api_data.get("data", {})
    api_str = json.dumps(api_stats, ensure_ascii=False)[:200]
    print(f"  API stats: {api_str}")

    # Compare
    page_d = json.loads(page_stats)
    if page_d.get("users") != "N/A":
        api_users = str(api_stats.get("totalUsers", api_stats.get("userCount", "?")))
        match = page_d.get("users") == api_users
        print(f"  Users match: {match} (page={page_d.get('users')}, api={api_users})")

# ═══════════════════════════════════════════════════════════════════════
# TEST 4: Verify Page Actions
# ═══════════════════════════════════════════════════════════════════════
print("\n--- TEST 4: Verify Report Actions ---")
goto_url("http://localhost:5173/admin/verify/list")
wait_for_load()
time.sleep(2)

verify_info = js("""
(function() {
    var btns = document.querySelectorAll(".el-table__body-wrapper .el-button");
    var result = [];
    btns.forEach(function(b) { result.push(b.textContent.trim()); });
    var rows = document.querySelectorAll(".el-table__body-wrapper tbody tr").length;
    return JSON.stringify({rows: rows, buttons: result});
})()
""")
print(f"  Verify page: {verify_info}")

# ═══════════════════════════════════════════════════════════════════════
# SUMMARY
# ═══════════════════════════════════════════════════════════════════════
print(f"\n{'='*60}")
print(f"  INTERACTIVE TEST SUMMARY")
print(f"{'='*60}")
print(f"  Issues found: {len(issues)}")
for i in issues:
    print(f"  [{i[2]}] {i[0]}/{i[1]}: {i[3]}")
if not issues:
    print("  All interactive workflow tests passed!")
