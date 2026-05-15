<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.controller

REST API controllers handling HTTP requests.

## Key Files (18 controllers)

- `AuthController.java` — `/api/v1/auth` (login, register, refresh)
- `UserController.java` — `/api/v1/users` (profile management)
- `AdminController.java` — `/api/v1/admin` (user management, system config)
- `CarbonController.java` — `/api/v1/carbon` (carbon reports)
- `CarbonCoinController.java` — `/api/v1/coin` (carbon coin accounts)
- `CarbonNeutralProjectController.java` — `/api/v1/carbon-neutral` (projects)
- `CreditScoreController.java` — `/api/v1/credit` (credit scores)
- `EmissionController.java` — `/api/v1/emission` (emission ratings)
- `DoubleAuctionController.java` — `/api/v1/auction` (double auction)
- `TradeController.java` — `/api/v1/trade` (P2P trading)
- `BlockchainController.java` — `/api/v1/blockchain` (blockchain attestation)
- `DigitalSignatureController.java` — `/api/v1/signature` (digital signatures)
- `FileController.java` — `/api/v1/files` (MinIO upload/download)
- `SearchController.java` — `/api/v1/search` (full-text search)
- `ReviewerController.java` — `/api/v1/reviewer` (report review)
- `CaptchaController.java` — `/api/v1/captcha` (CAPTCHA)
- `ThirdPartyController.java` — `/api/v1/third-party` (monitoring)
- `EnterpriseController.java` — `/api/v1/enterprise` (enterprise data)

## For AI Agents

Controllers use `@Valid` + DTOs for input, return `ApiResponse<T>`. Use `@PreAuthorize` for role-based access. Follow existing patterns for new endpoints.
