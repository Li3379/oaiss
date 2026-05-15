"""Browser test runner for OAISS CHAIN full-role testing."""
import json
import time
import sys

def api_login(username, password="admin123"):
    """Login via API and return token data."""
    resp = http_post("http://localhost:8080/api/v1/auth/login",
                     json={"username": username, "password": password})
    data = resp.json()
    if data.get("code") != 200:
        print(f"Login failed for {username}: {data}")
        return None
    return data["data"]

def inject_tokens(access_token, refresh_token, expires_in):
    """Inject JWT tokens into browser localStorage."""
    js_code = """
    localStorage.setItem('access_token', arguments[0]);
    localStorage.setItem('refresh_token', arguments[1]);
    localStorage.setItem('remember_me', 'true');
    localStorage.setItem('token_expiry', String(Date.now() + arguments[2] * 1000));
    return 'Tokens injected';
    """
    return js(js_code, access_token, refresh_token, expires_in)

def login_and_navigate(username, home_path, screenshot_name):
    """Full login flow: API login, inject tokens, navigate, screenshot."""
    print(f"\n=== Testing {username} ===")

    # API login
    token_data = api_login(username)
    if not token_data:
        return False

    print(f"  Login OK: {token_data['username']} (userId={token_data['userId']})")

    # Navigate to login page first
    new_tab("http://localhost:5173/login")
    wait_for_load()

    # Inject tokens
    result = inject_tokens(
        token_data["accessToken"],
        token_data["refreshToken"],
        token_data["expiresIn"]
    )
    print(f"  Token injection: {result}")

    # Navigate to home page
    goto_url(f"http://localhost:5173{home_path}")
    time.sleep(3)
    wait_for_load()

    # Screenshot
    capture_screenshot(f"test-screenshots/{screenshot_name}")
    info = page_info()
    print(f"  Page: {info}")
    return True

def test_page(path, screenshot_name):
    """Navigate to a page and capture screenshot."""
    goto_url(f"http://localhost:5173{path}")
    time.sleep(2)
    wait_for_load()
    capture_screenshot(f"test-screenshots/{screenshot_name}")
    print(f"  Captured: {screenshot_name}")

# ==================== MAIN ====================
if len(sys.argv) < 2:
    print("Usage: test_runner.py <command> [args]")
    print("Commands: admin, enterprise, reviewer, thirdparty")
    sys.exit(1)

command = sys.argv[1]

if command == "admin-login":
    login_and_navigate("admin", "/admin/system/users", "tc-01-01-admin-users")

elif command == "admin-page":
    path = sys.argv[2]
    name = sys.argv[3]
    test_page(path, name)

elif command == "enterprise-login":
    login_and_navigate("enterprise001", "/enterprise/carbon/upload", "tc-02-01-enterprise-upload")

elif command == "reviewer-login":
    login_and_navigate("reviewer001", "/auditor/audit/list", "tc-03-01-reviewer-audit")

elif command == "thirdparty-login":
    login_and_navigate("thirdparty001", "/third-party/monitor", "tc-05-01-thirdparty-monitor")

elif command == "custom-login":
    username = sys.argv[2]
    path = sys.argv[3]
    name = sys.argv[4]
    login_and_navigate(username, path, name)

else:
    print(f"Unknown command: {command}")
    sys.exit(1)
