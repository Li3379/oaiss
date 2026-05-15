import json, time, subprocess

# Login as enterprise001
result = subprocess.run(
    ["curl", "-s", "-X", "POST", "http://localhost:8080/api/v1/auth/login",
     "-H", "Content-Type: application/json",
     "-d", '{"username":"enterprise001","password":"admin123"}'],
    capture_output=True, text=True
)
data = json.loads(result.stdout)
td = data["data"]
at = td["accessToken"]
rt = td["refreshToken"]
ei = td["expiresIn"]
print("Login:", td["username"])

# Open login page
new_tab("http://localhost:5173/login")
wait_for_load()
time.sleep(1)

# Inject tokens
js_code = (
    "localStorage.setItem('access_token', " + json.dumps(at) + ");"
    "localStorage.setItem('refresh_token', " + json.dumps(rt) + ");"
    "localStorage.setItem('remember_me', 'true');"
    "localStorage.setItem('token_expiry', String(Date.now() + " + str(ei) + " * 1000));"
    "return 'Tokens injected';"
)
result = js(js_code)
print("Inject:", result)

# Navigate to enterprise home
goto_url("http://localhost:5173/enterprise/carbon/upload")
time.sleep(3)
wait_for_load()
capture_screenshot("test-screenshots/tc-02-01-enterprise-upload")
print(page_info())
