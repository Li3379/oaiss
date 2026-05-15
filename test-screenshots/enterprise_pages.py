import time

pages = [
    ("/enterprise/orders/manage", "tc-02-02-enterprise-orders"),
    ("/enterprise/trading/market", "tc-02-03-enterprise-market"),
    ("/enterprise/trading/p2p", "tc-02-04-enterprise-p2p"),
    ("/enterprise/company/dashboard", "tc-02-05-enterprise-dashboard"),
    ("/enterprise/credit/score", "tc-02-06-enterprise-credit"),
    ("/enterprise/carbon-coin/account", "tc-02-07-enterprise-coin"),
    ("/enterprise/blockchain/browser", "tc-02-08-enterprise-blockchain"),
    ("/enterprise/carbon-neutral/projects", "tc-02-09-enterprise-neutral"),
    ("/enterprise/emission/data", "tc-02-10-enterprise-emission"),
    ("/enterprise/user/profile", "tc-02-11-enterprise-profile"),
]

for path, name in pages:
    print(f"\n=== Navigating to {path} ===")
    goto_url("http://localhost:5173" + path)
    time.sleep(3)
    wait_for_load()
    capture_screenshot("test-screenshots/" + name)
    info = page_info()
    print(f"  Page: {info}")
