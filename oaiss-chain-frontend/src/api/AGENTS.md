<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# api/ — Axios API Client Modules

Axios-based API client modules, one file per backend domain.

## Key Files (17 modules)

- `request.ts` — Axios instance with interceptors (auth headers, error handling, refresh token)
- `auth.ts` — Login, register, token refresh
- `admin.ts` — Admin user/config management
- `auction.ts` — Double auction orders
- `blockchain.ts` — Blockchain transactions
- `captcha.ts` — CAPTCHA generation/verification
- `carbon.ts` — Carbon reports
- `carbonCoin.ts` — Carbon coin accounts
- `carbonNeutral.ts` — Carbon neutral projects
- `credit.ts` — Credit scores
- `emission.ts` — Emission ratings
- `file.ts` — File upload/download (MinIO)
- `search.ts` — Full-text search
- `signature.ts` — Digital signatures
- `thirdParty.ts` — Third-party monitoring
- `trade.ts` — P2P trade orders
- `user.ts` — User profile management

## For AI Agents

All API calls use the shared `request.ts` Axios instance which adds JWT auth headers automatically. Add new endpoints to the relevant existing module, or create a new `.ts` file for new domains.
