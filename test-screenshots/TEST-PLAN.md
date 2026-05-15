# OAISS CHAIN Full-Role Browser Test Plan

## Test Accounts (password: admin123)

| Username | Role | Home Route |
|----------|------|-----------|
| admin | ADMIN | /admin/system/users |
| enterprise001 | ENTERPRISE | /enterprise/carbon/upload |
| reviewer001 | REVIEWER | /auditor/audit/list |
| thirdparty001 | THIRD_PARTY | /third-party/monitor |

## Test Approach
- Login via API (captcha is optional), inject JWT into browser localStorage
- Navigate to each page, capture screenshot, verify visual correctness
- Test interactive elements (buttons, forms, tables)
- Record issues and fix inline

---

## TC-01: ADMIN Role Tests

### TC-01-01: Login as admin → /admin/system/users
- Verify sidebar menu items: 用户管理, 碳资产管理, 系统配置, 数据统计
- Verify user list table loads with data
- Test pagination controls

### TC-01-02: /admin/system/carbon (碳资产管理)
- Verify carbon data management page loads
- Check for data table, filters, search

### TC-01-03: /admin/system/config (系统配置)
- Verify system configuration page loads
- Check config form elements

### TC-01-04: /admin/data/statistics (数据统计)
- Verify statistics dashboard loads
- Check ECharts charts render

---

## TC-02: ENTERPRISE Role Tests

### TC-02-01: Login as enterprise001 → /enterprise/carbon/upload
- Verify sidebar menu: 碳核算, P2P订单管理, 碳交易, 企业信息, 信用评分, 碳币账户, 区块链, 碳中和, 个人中心
- Verify carbon upload page loads

### TC-02-02: /enterprise/orders/manage (订单管理)
- Verify order list page

### TC-02-03: /enterprise/trading/market (双拍市场)
- Verify trading market page

### TC-02-04: /enterprise/trading/p2p (P2P交易)
- Verify P2P trading page

### TC-02-05: /enterprise/company/dashboard (数据可视化)
- Verify company dashboard with charts

### TC-02-06: /enterprise/credit/score (信用评分)
- Verify credit score page

### TC-02-07: /enterprise/carbon-coin/account (碳币账户)
- Verify carbon coin account page

### TC-02-08: /enterprise/blockchain/browser (区块链浏览器)
- Verify blockchain browser page

### TC-02-09: /enterprise/carbon-neutral/projects (碳中和项目)
- Verify carbon neutral projects page

### TC-02-10: /enterprise/emission/data (排放数据)
- Verify emission data page

### TC-02-11: /enterprise/user/profile (个人中心)
- Verify user profile page

---

## TC-03: REVIEWER Role Tests

### TC-03-01: Login as reviewer001 → /auditor/audit/list
- Verify audit list page loads with table
- Check audit workflow buttons

---

## TC-04: THIRD_PARTY Role Tests

### TC-05-01: Login as thirdparty001 → /third-party/monitor
- Verify monitoring dashboard loads
- Check monitoring charts/data

---

## TC-05: Cross-Cutting Tests

### TC-06-01: Logout flow
- Click logout, verify redirect to login page

### TC-06-02: Unauthorized access
- Try accessing admin page as enterprise user

### TC-06-03: i18n toggle
- Switch language, verify page updates

---

## Issues Found
(To be filled during testing)
