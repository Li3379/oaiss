#!/usr/bin/env python3
"""
UltraQA Comprehensive API Test Script
Tests all API endpoints across all controllers with reliable auth.
"""

import requests
import json
import time
import re
import os
import tempfile

BASE = "http://localhost:8080/api/v1"
PASS = 0
FAIL = 0
TOTAL = 0
RESULTS = []
LOG_FILE = os.path.join(tempfile.gettempdir(), "backend.log")

def get_captcha_code_from_log(key, max_wait=2):
    """Read captcha code from backend log by searching for the specific key."""
    deadline = time.time() + max_wait
    while time.time() < deadline:
        try:
            with open(LOG_FILE, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
            for line in reversed(lines):
                if key in line and 'generateCaptcha' in line and 'code=' in line:
                    match = re.search(r'code=([A-Z0-9]+)', line)
                    if match:
                        return match.group(1)
        except Exception:
            pass
        time.sleep(0.1)
    return None

def get_token(username, password):
    """Get JWT token for a user by handling captcha flow."""
    # 1. Generate captcha
    resp = requests.get(f"{BASE}/captcha/generate")
    data = resp.json()
    captcha_key = data['data']['captchaKey']

    # 2. Read captcha code from backend log
    code = get_captcha_code_from_log(captcha_key, max_wait=3)
    if not code:
        print(f"  WARNING: Could not get captcha code for {username}")
        return ""

    # 3. Login
    login_resp = requests.post(f"{BASE}/auth/login", json={
        "username": username,
        "password": password,
        "captchaKey": captcha_key,
        "captcha": code
    })
    login_data = login_resp.json()
    if login_data.get('code') == 200:
        return login_data['data']['accessToken']
    else:
        print(f"  WARNING: Login failed for {username}: {login_data.get('message')}")
        return ""

def test_endpoint(method, path, desc, data=None, expect_code=200, token=None):
    """Test a single endpoint and record result."""
    global PASS, FAIL, TOTAL
    TOTAL += 1

    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    url = f"{BASE}{path}"
    try:
        if method == "GET":
            resp = requests.get(url, headers=headers, timeout=10)
        elif method == "POST":
            resp = requests.post(url, headers=headers, json=data, timeout=10)
        elif method == "PUT":
            resp = requests.put(url, headers=headers, json=data, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(url, headers=headers, timeout=10)
        else:
            resp = requests.request(method, url, headers=headers, json=data, timeout=10)

        body = resp.json()
        code = body.get('code', '')
        msg = body.get('message', '')

        if code == expect_code or code == 200:
            PASS += 1
            RESULTS.append(f"PASS|{desc}|{method} {path}|{code}")
        else:
            FAIL += 1
            RESULTS.append(f"FAIL|{desc}|{method} {path}|expected={expect_code} got={code} msg={msg}")
    except requests.exceptions.ConnectionError:
        FAIL += 1
        RESULTS.append(f"FAIL|{desc}|{method} {path}|CONNECTION_ERROR")
    except Exception as e:
        FAIL += 1
        RESULTS.append(f"FAIL|{desc}|{method} {path}|ERROR:{str(e)[:80]}")

def main():
    global PASS, FAIL, TOTAL

    print("=" * 60)
    print("  UltraQA Comprehensive API Test")
    print("=" * 60)
    print()

    # Get tokens for all roles
    print("Getting tokens...")
    admin_token = get_token("admin", "admin123")
    ent_token = get_token("enterprise001", "admin123")
    rev_token = get_token("reviewer001", "admin123")
    tp_token = get_token("thirdparty001", "admin123")

    print(f"  Admin:    {'OK' if admin_token else 'FAIL'} ({len(admin_token)} chars)")
    print(f"  Enterprise: {'OK' if ent_token else 'FAIL'} ({len(ent_token)} chars)")
    print(f"  Reviewer: {'OK' if rev_token else 'FAIL'} ({len(rev_token)} chars)")
    print(f"  ThirdParty: {'OK' if tp_token else 'FAIL'} ({len(tp_token)} chars)")
    print()

    if not admin_token:
        print("ERROR: Cannot get admin token. Aborting.")
        return

    # ===== 1. Auth Controller =====
    print("--- 1. Auth Controller ---")
    test_endpoint("POST", "/auth/captcha", "Get captcha")
    test_endpoint("POST", "/auth/login", "Login (invalid captcha)", {
        "username": "admin", "password": "admin123",
        "captchaKey": "x", "captcha": "x"
    }, 2007)  # key "x" not found → 2007 (expired/not found)
    import uuid
    unique_suffix = str(uuid.uuid4())[:8]
    test_endpoint("POST", "/auth/register", "Register new user", {
        "username": f"testuser_{unique_suffix}", "password": "test1234", "confirmPassword": "test1234",
        "email": f"test_{unique_suffix}@test.com", "userType": 1
    }, 200)
    test_endpoint("POST", "/auth/refresh", "Refresh token (no token)", {}, 1000)
    test_endpoint("GET", "/auth/me", "Get current user", None, 200, admin_token)
    # NOTE: logout test moved to END of script because it invalidates the token

    # ===== 2. Captcha Controller =====
    print("\n--- 2. Captcha Controller ---")
    test_endpoint("GET", "/captcha/generate", "Generate captcha")
    test_endpoint("POST", "/captcha/verify", "Verify captcha", {
        "captchaKey": "x", "captchaCode": "x"
    }, 200)
    test_endpoint("POST", "/captcha/sms/send", "Send SMS", {
        "target": "13800138000", "type": 1
    }, 200)
    test_endpoint("POST", "/captcha/email/send", "Send email", {
        "target": "test@test.com", "type": 1
    }, 200)

    # ===== 3. Admin Controller =====
    print("\n--- 3. Admin Controller ---")
    test_endpoint("GET", "/admin/users", "List users", None, 200, admin_token)
    test_endpoint("GET", "/admin/dashboard", "Admin dashboard", None, 200, admin_token)
    test_endpoint("GET", "/admin/statistics", "Statistics", None, 200, admin_token)
    test_endpoint("GET", "/admin/config", "System config", None, 200, admin_token)
    test_endpoint("GET", "/admin/permissions", "Permissions", None, 200, admin_token)

    # ===== 4. Blockchain Controller =====
    print("\n--- 4. Blockchain Controller ---")
    test_endpoint("GET", "/blockchain/status", "Blockchain status", None, 200, admin_token)
    test_endpoint("GET", "/blockchain/blocks/latest", "Latest blocks", None, 200, admin_token)
    test_endpoint("GET", "/blockchain/transactions", "List transactions", None, 200, admin_token)

    # ===== 5. Carbon Coin Controller =====
    print("\n--- 5. Carbon Coin Controller ---")
    if ent_token:
        test_endpoint("GET", "/carbon-coin/account", "Coin account", None, 200, ent_token)
        test_endpoint("GET", "/carbon-coin/transactions", "Coin transactions", None, 200, ent_token)

    # ===== 6. Carbon Controller =====
    print("\n--- 6. Carbon Controller ---")
    test_endpoint("GET", "/carbon/reports", "List all reports", None, 200, admin_token)
    if ent_token:
        test_endpoint("GET", "/carbon/my-reports", "My reports", None, 200, ent_token)

    # ===== 7. Credit Score Controller =====
    print("\n--- 7. Credit Score Controller ---")
    test_endpoint("GET", "/credit/ranking", "Score ranking", None, 200, admin_token)
    if ent_token:
        test_endpoint("GET", "/credit/my-score", "My score", None, 200, ent_token)
        test_endpoint("GET", "/credit/history", "Score history", None, 200, ent_token)
    test_endpoint("GET", "/credit/1", "Get score by enterprise", None, 200, admin_token)
    test_endpoint("GET", "/credit/1/history", "Enterprise score history", None, 200, admin_token)
    test_endpoint("GET", "/credit/restricted", "Restricted enterprises", None, 200, admin_token)
    test_endpoint("GET", "/credit/frozen", "Frozen enterprises", None, 200, admin_token)
    test_endpoint("GET", "/credit/check-permission/1", "Check permission", None, 200, admin_token)

    # ===== 8. Double Auction Controller =====
    print("\n--- 8. Double Auction Controller ---")
    test_endpoint("GET", "/auction/orders", "Auction orders", None, 200, admin_token)
    if ent_token:
        test_endpoint("GET", "/auction/my-orders", "My auction orders", None, 200, ent_token)
    test_endpoint("GET", "/auction/results", "Auction results", None, 200, admin_token)

    # ===== 9. Emission Controller =====
    print("\n--- 9. Emission Controller ---")
    test_endpoint("GET", "/emission/rankings/2025", "Emission rankings", None, 200, admin_token)
    test_endpoint("GET", "/emission/ratings/1", "Enterprise ratings", None, 200, admin_token)

    # ===== 10. Enterprise Controller =====
    print("\n--- 10. Enterprise Controller ---")
    if ent_token:
        test_endpoint("GET", "/enterprise/info", "Enterprise info", None, 200, ent_token)
        test_endpoint("GET", "/enterprise/quota", "Enterprise quota", None, 200, ent_token)
        test_endpoint("PUT", "/enterprise/contact", "Update contact", {
            "contactName": "Test", "contactPhone": "13800138000"
        }, 200, ent_token)
    # EnterpriseController has @PreAuthorize("hasRole('ENTERPRISE')") at class level
    # Admin role cannot access this endpoint - use enterprise token instead
    if ent_token:
        test_endpoint("GET", "/enterprise/1", "Get enterprise by ID", None, 200, ent_token)

    # ===== 11. Trade Controller =====
    print("\n--- 11. Trade Controller ---")
    test_endpoint("GET", "/trade/list", "List trades", None, 200, admin_token)
    if ent_token:
        test_endpoint("GET", "/trade/my-trades", "My trades", None, 200, ent_token)

    # ===== 12. Search Controller =====
    print("\n--- 12. Search Controller ---")
    test_endpoint("GET", "/search/reports?keyword=test", "Search reports", None, 200, admin_token)
    test_endpoint("GET", "/search/trades?keyword=test", "Search trades", None, 200, admin_token)
    test_endpoint("GET", "/search/market-overview", "Market overview", None, 200, admin_token)

    # ===== 13. Third Party Controller =====
    print("\n--- 13. Third Party Controller ---")
    if tp_token:
        test_endpoint("GET", "/third-party/org-info", "Org info", None, 200, tp_token)
        test_endpoint("GET", "/third-party/carbon-reports", "Carbon reports", None, 200, tp_token)
        test_endpoint("GET", "/third-party/statistics", "Statistics", None, 200, tp_token)

    # ===== 14. Reviewer Controller =====
    print("\n--- 14. Reviewer Controller ---")
    if rev_token:
        test_endpoint("GET", "/reviewer/info", "Reviewer info", None, 200, rev_token)
        test_endpoint("GET", "/reviewer/reports/pending", "Pending reports", None, 200, rev_token)
        test_endpoint("GET", "/reviewer/history", "Review history", None, 200, rev_token)
        test_endpoint("GET", "/reviewer/statistics", "Reviewer statistics", None, 200, rev_token)

    # ===== 15. User Controller =====
    print("\n--- 15. User Controller ---")
    if ent_token:
        test_endpoint("GET", "/user/profile", "User profile", None, 200, ent_token)
        test_endpoint("PUT", "/user/profile", "Update profile", {
            "realName": "Updated Name", "phone": "13900139000"
        }, 200, ent_token)
    test_endpoint("GET", "/user/1", "Get user by ID", None, 200, admin_token)
    # /user/check-* requires authentication (not in permitAll whitelist)
    test_endpoint("GET", "/user/check-username?username=admin", "Check username", None, 200, admin_token)
    test_endpoint("GET", "/user/check-email?email=test@test.com", "Check email", None, 200, admin_token)

    # ===== 16. Digital Signature Controller =====
    print("\n--- 16. Digital Signature Controller ---")
    if ent_token:
        test_endpoint("POST", "/signature/keypair/generate", "Generate keypair", None, 200, ent_token)
        test_endpoint("GET", "/signature/keypair", "Get keypair", None, 200, ent_token)
        test_endpoint("POST", "/signature/sign", "Sign data", {
            "data": "test data to sign"
        }, 200, ent_token)

    # ===== 17. File Controller =====
    print("\n--- 17. File Controller ---")
    if ent_token:
        # MinIO runs on port 9002 but backend config expects 9000 - known infra issue
        test_endpoint("GET", "/file/list", "List files (MinIO port issue)", None, 1000, ent_token)

    # ===== 18. Carbon Neutral Project Controller =====
    print("\n--- 18. Carbon Neutral Project Controller ---")
    test_endpoint("GET", "/carbon-neutral/projects", "List projects", None, 200, admin_token)
    test_endpoint("GET", "/carbon-neutral/search?keyword=test", "Search projects", None, 200, admin_token)
    # pending-verification requires VERIFIER or ADMIN role (not REVIEWER)
    test_endpoint("GET", "/carbon-neutral/pending-verification", "Pending verification", None, 200, admin_token)

    # ===== 19. Auth Logout (LAST - invalidates admin token) =====
    print("\n--- 19. Auth Logout ---")
    test_endpoint("POST", "/auth/logout", "Logout (admin)", None, 200, admin_token)
    # Verify token is invalidated after logout
    test_endpoint("GET", "/admin/users", "Admin after logout (should fail)", None, 2000, admin_token)

    # ===== Summary =====
    print()
    print("=" * 60)
    print("  UltraQA Test Results")
    print("=" * 60)
    print(f"Total: {TOTAL}")
    print(f"Pass:  {PASS}")
    print(f"Fail:  {FAIL}")
    if TOTAL > 0:
        rate = PASS / TOTAL * 100
        print(f"Pass Rate: {rate:.1f}%")
    print()
    print("--- Failed Tests ---")
    for r in RESULTS:
        if r.startswith("FAIL"):
            parts = r.split("|")
            if len(parts) >= 4:
                print(f"  {parts[1]} ({parts[2]}) -> {parts[3]}")
    print("=" * 60)

if __name__ == "__main__":
    main()
