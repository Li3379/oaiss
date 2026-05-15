import json, time, subprocess, sys

# This script navigates to a specific page and takes a screenshot
# Usage: pass path and screenshot name via sys.argv set before exec
page_path = sys.argv[1] if len(sys.argv) > 1 else "/admin/system/carbon"
screenshot_name = sys.argv[2] if len(sys.argv) > 2 else "temp-screenshot"

goto_url("http://localhost:5173" + page_path)
time.sleep(3)
wait_for_load()
capture_screenshot("test-screenshots/" + screenshot_name)
print(page_info())
