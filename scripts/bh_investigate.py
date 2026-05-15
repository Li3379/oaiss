"""Browser-harness: Investigate dialog and button click issues."""
import json
import os
import time
import tempfile
import re
from urllib.request import urlopen, Request

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")


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


# === INVESTIGATE: CarbonUpload Create Button ===
print("=== INVESTIGATE: CarbonUpload Create Button ===")
a, r = login_api("enterprise001", "admin123")
inject(a, r)
goto_url("http://localhost:5173/enterprise/carbon/upload")
wait_for_load()
time.sleep(2)

btn_pos = js("""
(function() {
    var btns = document.querySelectorAll(".el-button");
    for (var i = 0; i < btns.length; i++) {
        if (btns[i].textContent.includes("创建")) {
            var r = btns[i].getBoundingClientRect();
            return JSON.stringify({
                text: btns[i].textContent.trim(),
                x: Math.round(r.x + r.width/2),
                y: Math.round(r.y + r.height/2),
                w: Math.round(r.width),
                h: Math.round(r.height)
            });
        }
    }
    return "NOT_FOUND";
})()
""")
print(f"  Button: {btn_pos}")

if btn_pos != "NOT_FOUND":
    pos = json.loads(btn_pos)
    click_at_xy(pos["x"], pos["y"])
    time.sleep(1.5)

    # Check all dialog types
    state = js("""
    (function() {
        var dlg = document.querySelector(".el-dialog");
        var wrapper = document.querySelector(".el-dialog__wrapper");
        var overlay = document.querySelector(".el-overlay");
        var wrappers = document.querySelectorAll(".el-dialog__wrapper");
        var wInfo = [];
        wrappers.forEach(function(w) {
            wInfo.push({
                display: w.style.display,
                classes: w.className.substring(0,60)
            });
        });
        return JSON.stringify({
            hasDialog: !!dlg,
            hasWrapper: !!wrapper,
            wrapperCount: wrappers.length,
            wrappers: wInfo,
            hasOverlay: !!overlay,
            overlayVisible: overlay ? overlay.style.display : "none"
        });
    })()
    """)
    print(f"  After click: {state}")

    # Try opening via Vue API directly
    print("  Trying Vue component method directly...")
    vue_result = js("""
    (function() {
        var btn = document.querySelector(".el-button--success");
        if (btn) btn.click();
        return "clicked_via_dom";
    })()
    """)
    print(f"  DOM click: {vue_result}")
    time.sleep(1)

    state2 = js("""
    (function() {
        var wrappers = document.querySelectorAll(".el-dialog__wrapper");
        var wInfo = [];
        wrappers.forEach(function(w) {
            wInfo.push({display: w.style.display, classes: w.className.substring(0,60)});
        });
        var dlg = document.querySelector(".el-dialog");
        var dlgTitle = dlg ? dlg.querySelector(".el-dialog__title") : null;
        return JSON.stringify({
            wrappers: wInfo,
            hasDialog: !!dlg,
            title: dlgTitle ? dlgTitle.textContent.trim() : ""
        });
    })()
    """)
    print(f"  After DOM click: {state2}")

# === INVESTIGATE: Admin Disable Button ===
print("\n=== INVESTIGATE: Admin Disable Button ===")
a, r = login_api("admin", "admin123")
inject(a, r)
goto_url("http://localhost:5173/admin/system/users")
wait_for_load()
time.sleep(2)

disable_info = js("""
(function() {
    var btns = document.querySelectorAll(".el-table__body-wrapper .el-button");
    if (btns.length === 0) return "NO_BTNS";
    var btn = btns[0];
    var r = btn.getBoundingClientRect();
    var row = btn.closest("tr");
    var cells = row ? row.querySelectorAll("td") : [];
    var user = cells.length > 0 ? cells[0].textContent.trim() : "";
    return JSON.stringify({
        text: btn.textContent.trim(),
        x: Math.round(r.x + r.width/2),
        y: Math.round(r.y + r.height/2),
        user: user
    });
})()
""")
print(f"  Button: {disable_info}")

if disable_info != "NO_BTNS":
    pos = json.loads(disable_info)
    click_at_xy(pos["x"], pos["y"])
    time.sleep(1.5)

    after = js("""
    (function() {
        var mb = document.querySelector(".el-message-box");
        var overlay = document.querySelector(".el-overlay");
        var msg = document.querySelector(".el-message");
        return JSON.stringify({
            hasMessageBox: !!mb,
            msgText: mb ? mb.textContent.trim().substring(0, 100) : "",
            hasOverlay: !!overlay,
            hasMessage: !!msg,
            messageText: msg ? msg.textContent.trim() : ""
        });
    })()
    """)
    print(f"  After click: {after}")

    # Close messagebox if open
    js('try { document.querySelector(".el-message-box__btns .el-button--primary").click() } catch(e) {}')
    time.sleep(0.5)

print("\n--- DONE ---")
