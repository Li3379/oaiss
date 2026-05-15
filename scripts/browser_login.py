"""Browser-harness login helper - login via API and inject auth into browser."""
import json, re, tempfile, os, time, sys
from urllib.request import urlopen, Request

API = "http://localhost:8080/api/v1"
log_path = os.path.join(tempfile.gettempdir(), "backend.log")


def login(username, password):
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
        return None, "Cannot get captcha"

    payload = json.dumps({
        "username": username,
        "password": password,
        "captchaKey": captcha_key,
        "captchaCode": code,
    }).encode()
    req = Request(f"{API}/auth/login", data=payload, headers={"Content-Type": "application/json"})
    resp = urlopen(req, timeout=5)
    data = json.loads(resp.read())

    if data.get("code") == 200:
        return data["data"], None
    return None, data.get("message", "unknown")


if __name__ == "__main__":
    username = sys.argv[1] if len(sys.argv) > 1 else "enterprise001"
    password = sys.argv[2] if len(sys.argv) > 2 else "admin123"

    tokens, err = login(username, password)
    if err:
        print(f"LOGIN_FAILED: {err}")
        sys.exit(1)

    access = tokens["accessToken"]
    refresh = tokens.get("refreshToken", "")

    # Get user profile
    req = Request(f"{API}/user/profile", headers={"Authorization": f"Bearer {access}"})
    resp = urlopen(req, timeout=5)
    profile = json.loads(resp.read())
    user_data = profile.get("data", {})
    role = user_data.get("role", "UNKNOWN")

    print(f"LOGIN_OK: username={username} role={role}")
    print(f"TOKEN_START:{access}:TOKEN_END")
    print(f"REFRESH_START:{refresh}:REFRESH_END")
