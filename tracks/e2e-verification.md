# E2E 验证报告

**日期**: 2026-05-02
**工具**: Playwright CLI (v0.1.9)
**环境**: Vite dev server (localhost:5173), 无后端服务
**方法**: 设置 mock JWT token + API route mocking 模拟已登录状态

## 验证结果总览

| 项目 | 页面路径 | 结果 | 说明 |
|------|----------|------|------|
| D1 | /login | ✅ 通过 | 登录页渲染正常，验证码图片元素存在 |
| D2 | /enterprise/carbon/upload | ✅ 通过 | 碳核算页面表格、搜索表单、分页完整 |
| D3 | /enterprise/trading/market | ✅ 通过 | 双向拍卖页面 Tab 切换、表格结构完整 |
| D4 | /enterprise/trading/p2p | ✅ 通过 | P2P 交易页面搜索表单和表格完整 |
| D5 | /admin/system/users | ✅ 通过 | 管理员用户管理页面角色菜单和表格完整 |

## D1: 登录页

- **URL**: http://localhost:5173/login
- **页面标题**: 碳资产监管后台
- **元素**:
  - 账号输入框 (placeholder: 请输入账号)
  - 密码输入框 (placeholder: 请输入密码)
  - 验证码输入框 + 验证码图片 (可点击刷新)
  - "记住账号" 复选框 (默认选中)
  - "登录" 按钮
- **截图**: `screenshots/d1-login-page.png`

## D2: 碳核算-上传审核

- **URL**: http://localhost:5173/enterprise/carbon/upload
- **面包屑**: 碳核算 / 上传审核
- **搜索表单**: 报告标题输入框、核算周期输入框、查询按钮、创建报告按钮
- **表格列**: 报告编号、报告标题、核算周期、总排放量(tCO2e)、状态、审核人、创建时间、操作
- **空数据状态**: "No Data" (正确)
- **分页**: Total 0, 10/page
- **侧边栏**: 企业用户菜单完整展开（碳核算、P2P订单管理、碳交易、本公司信息、信誉评分、碳币账户、区块链、碳中和、个人中心）
- **用户信息**: testuser / 企业用户
- **截图**: `screenshots/d2-carbon-upload.png`

## D3: 双向拍卖

- **URL**: http://localhost:5173/enterprise/trading/market
- **面包屑**: 碳交易 / 双向拍卖
- **搜索表单**: 关键字输入框 (placeholder: 请输入关键字（买卖方向/数量/价格/状态/创建时间）)、查询按钮、创建订单按钮
- **Tab 切换**: 全部挂单 (默认选中)、我的挂单、撮合结果
- **表格列**: 序号、买卖方向、数量（吨）、价格（元/吨）、状态、创建时间
- **空数据状态**: "No Data" (正确)
- **分页**: Total 0, 10/page
- **截图**: `screenshots/d3-auction-market.png`

## D4: P2P 交易

- **URL**: http://localhost:5173/enterprise/trading/p2p
- **面包屑**: 碳交易 / P2P交易
- **搜索表单**: 名称输入框、身份下拉框、碳交易订单号输入框、查询按钮、创建P2P交易按钮
- **表格列**: 序号、交易编号、买方名称、卖方名称、交易数量、单价、总金额、状态、创建时间、操作
- **空数据状态**: "No Data" (正确)
- **分页**: Total 0, 10/page
- **截图**: `screenshots/d4-p2p-trade.png`

## D5: 管理员-用户管理

- **URL**: http://localhost:5173/admin/system/users
- **面包屑**: 系统管理 / 用户管理
- **侧边栏**: 管理员菜单（系统管理 > 用户管理/碳核算管理/系统配置, 数据管理 > 统计数据）
- **搜索表单**: 用户类型下拉框、状态下拉框、查询按钮
- **表格列**: 用户名、邮箱、用户类型、状态、创建时间、操作
- **空数据状态**: "No Data" (正确)
- **分页**: Total 0, 10/page
- **用户信息**: admin / 管理员
- **截图**: `screenshots/d5-admin-users.png`

## 测试方法说明

### Mock JWT Token

使用 mock JWT token 模拟已登录状态：

```
Header: {"alg":"HS256"}
Payload: {"sub":"testuser","userId":1,"enterpriseId":1,"roles":["ENTERPRISE","ADMIN"],"userType":1,"exp":9999999999}
Signature: mock_signature
```

### API Route Mocking

使用 Playwright `page.route()` 拦截所有 `/api/v1/**` 请求，返回空数据响应：

```json
{"code": 200, "message": "ok", "data": {"content": [], "totalElements": 0}}
```

此方法验证页面渲染和路由正确性，不依赖后端服务。

### 已知限制

1. 无后端服务运行，API 调用返回 mock 数据
2. 实际数据交互（登录、提交表单、数据加载）需启动完整环境验证
3. 401 拦截器行为已确认正确（未 mock 时会清除 token 并重定向到登录页）

## Acceptance 更新

D1-D5 全部通过后，`tracks/phase-01-acceptance.md` 应更新为全部勾选。
