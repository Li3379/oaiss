"""Browser-harness full role QA test script."""
import json
import sys
import time
import os

# ── helpers ──────────────────────────────────────────────────────────────
REPORT_FILE = os.path.join(os.path.dirname(__file__), "..", ".planning", "specs", "BROWSER-QA-REPORT.md")
issues = []
page_results = []


def report_page(role, page_name, url, status, details=""):
    page_results.append({
        "role": role, "page": page_name, "url": url,
        "status": status, "details": details,
    })
    icon = "OK" if status == "OK" else "FAIL"
    print(f"  [{icon}] {role}/{page_name}: {status} {details}")


def report_issue(role, page, severity, description):
    issues.append({
        "role": role, "page": page, "severity": severity,
        "description": description,
    })
    print(f"  [ISSUE-{severity}] {role}/{page}: {description}")


def nav_and_check(url, expected_title_keyword=None):
    """Navigate to URL and check if page loaded."""
    try:
        goto_url(url)
        wait_for_load()
        time.sleep(1.5)
        current = js("window.location.href")
        return current
    except Exception as e:
        return f"ERROR: {e}"


def get_page_summary():
    """Get a text summary of current page content."""
    try:
        text = js("document.body.innerText.substring(0, 600)")
        return text
    except:
        return ""


def get_element_count(selector):
    """Count elements matching selector."""
    try:
        return js(f'document.querySelectorAll("{selector}").length')
    except:
        return 0


def check_for_errors():
    """Check for visible error messages on page."""
    try:
        errs = js("""
            (function() {
                var msgs = [];
                document.querySelectorAll('.el-message--error, .el-notification__content, .el-form-item__error').forEach(function(el) {
                    var t = el.textContent.trim();
                    if (t) msgs.push(t);
                });
                return msgs.join('; ');
            })()
        """)
        return errs if errs else ""
    except:
        return ""


def get_table_rows():
    """Count table rows on current page."""
    try:
        return js('document.querySelectorAll(".el-table__body-wrapper tbody tr").length')
    except:
        return 0


def get_form_fields():
    """Count form fields on current page."""
    try:
        return js('document.querySelectorAll(".el-form-item").length')
    except:
        return 0


# ── run via browser-harness ─────────────────────────────────────────────
print("=" * 60)
print("  Browser QA: Full Role Page Testing")
print("=" * 60)

# ── ENTERPRISE ROLE ─────────────────────────────────────────────────────
role = "ENTERPRISE"
print(f"\n--- {role}: Page Navigation Tests ---")

pages = [
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
]

for name, path in pages:
    url = f"http://localhost:5173{path}"
    nav_and_check(url)
    time.sleep(1)

    errors = check_for_errors()
    tables = get_table_rows()
    forms = get_form_fields()
    body_text = get_page_summary()[:200]
    current_url = js("window.location.href")

    # Check if redirected to login (means auth failed)
    if "/login" in current_url:
        report_issue(role, name, "HIGH", "Redirected to login - auth token may have expired")
        report_page(role, name, url, "FAIL", "redirected to login")
    elif errors:
        report_issue(role, name, "MEDIUM", f"Error on page: {errors}")
        report_page(role, name, url, "FAIL", errors)
    else:
        details = f"tables={tables} forms={forms}"
        report_page(role, name, url, "OK", details)

# ── Print summary ───────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("  Summary")
print("=" * 60)
ok = sum(1 for r in page_results if r["status"] == "OK")
fail = sum(1 for r in page_results if r["status"] == "FAIL")
print(f"  Pages tested: {len(page_results)} | OK: {ok} | FAIL: {fail}")
print(f"  Issues found: {len(issues)}")

if issues:
    print("\n  Issues:")
    for iss in issues:
        print(f"    [{iss['severity']}] {iss['role']}/{iss['page']}: {iss['description']}")

# ── Write report ─────────────────────────────────────────────────────────
report_lines = [
    "---",
    "status: in-progress",
    f"created: {time.strftime('%Y-%m-%d')}",
    "author: browser-harness QA",
    "---",
    "",
    "# Browser QA Report",
    "",
    "## Page Navigation Tests",
    "",
    f"| # | Role | Page | URL | Status | Details |",
    f"|---|------|------|-----|--------|---------|",
]
for i, r in enumerate(page_results, 1):
    icon = "OK" if r["status"] == "OK" else "FAIL"
    report_lines.append(f"| {i} | {r['role']} | {r['page']} | `{r['url']}` | {icon} | {r['details']} |")

if issues:
    report_lines.append("")
    report_lines.append("## Issues Found")
    report_lines.append("")
    report_lines.append("| # | Role | Page | Severity | Description |")
    report_lines.append("|---|------|------|----------|-------------|")
    for i, iss in enumerate(issues, 1):
        report_lines.append(f"| {i} | {iss['role']} | {iss['page']} | {iss['severity']} | {iss['description']} |")

os.makedirs(os.path.dirname(REPORT_FILE), exist_ok=True)
with open(REPORT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(report_lines))
print(f"\n  Report written to {REPORT_FILE}")
