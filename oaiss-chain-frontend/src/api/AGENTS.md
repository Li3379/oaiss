<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# api/ — Axios API Client Modules

Axios-based API client modules, one file per backend domain.

## Key Files (22 modules)

| # | File | Chinese | Backend Path | Description |
|---|------|---------|--------------|-------------|
| 1 | `search.ts` | 搜索 | `/api/v1/search` | Full-text search |
| 2 | `file.ts` | 文件 | `/api/v1/files` | File upload/download (MinIO) |
| 3 | `admin.ts` | 管理 | `/api/v1/admin` | Admin user/config management |
| 4 | `auction.ts` | 拍卖 | `/api/v1/auction` | Double auction orders |
| 5 | `auth.ts` | 认证 | `/api/v1/auth` | Login, register, token refresh |
| 6 | `blockchain.ts` | 区块链 | `/api/v1/blockchain` | Blockchain transactions |
| 7 | `captcha.ts` | 验证码 | `/api/v1/captcha` | CAPTCHA generation/verification |
| 8 | `carbon.ts` | 碳报告 | `/api/v1/carbon` | Carbon reports |
| 9 | `carbonCoin.ts` | 碳币 | `/api/v1/coin` | Carbon coin accounts |
| 10 | `carbonFormula.ts` | 碳核算公式 | `/api/v1/carbon-formula` | Carbon accounting formulas |
| 11 | `carbonNeutral.ts` | 碳中和 | `/api/v1/carbon-neutral` | Carbon neutral projects |
| 12 | `credit.ts` | 信用评分 | `/api/v1/credit` | Credit scores |
| 13 | `emission.ts` | 排放 | `/api/v1/emission` | Emission ratings |
| 14 | `enterprise.ts` | 企业 | `/api/v1/enterprise` | Enterprise data |
| 15 | `enterpriseInference.ts` | 企业推断 | `/api/v1/inference` | ML enterprise inference |
| 16 | `marketPrediction.ts` | 市场预测 | `/api/v1/prediction` | ML market prediction |
| 17 | `request.ts` | 请求封装 | — | Axios instance with interceptors (JWT injection, pageNum→page conversion, token refresh, Spring Page→transformed page) |
| 18 | `reviewer.ts` | 审核员 | `/api/v1/reviewer` | Report review |
| 19 | `signature.ts` | 数字签名 | `/api/v1/signature` | Digital signatures |
| 20 | `thirdParty.ts` | 第三方 | `/api/v1/third-party` | Third-party monitoring |
| 21 | `trade.ts` | 交易 | `/api/v1/trade` | P2P trade orders |
| 22 | `user.ts` | 用户 | `/api/v1/users` | User profile management |

## For AI Agents

- All API calls use the shared `request.ts` Axios instance which adds JWT auth headers automatically.
- `request.ts` handles critical cross-cutting concerns: JWT injection, `pageNum`/`pageSize` → `page`/`size` conversion, token refresh, and Spring Data `Page` → frontend `{ items, total, page, size, totalPages }` transformation.
- Add new endpoints to the relevant existing module, or create a new `.ts` file for new domains.
- Each module corresponds to a backend controller — see `controller/AGENTS.md` in the backend for the 1:1 mapping.
- ML modules (`enterpriseInference.ts`, `marketPrediction.ts`) call the ML-augmented endpoints (`/inference`, `/prediction`).
