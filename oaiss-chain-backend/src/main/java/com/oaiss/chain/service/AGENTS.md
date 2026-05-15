<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# com.oaiss.chain.service

Business logic layer -- the core of the application.

## Key Files (21 services)

- `AuthService.java` — Authentication, registration, token management
- `UserService.java` — User CRUD, profile management
- `EnterpriseService.java` — Enterprise data management
- `CarbonService.java` — Carbon report CRUD and calculations
- `CarbonPredictionService.java` — AI-based carbon prediction
- `CarbonCoinService.java` — Carbon coin account management and transfers
- `CarbonNeutralProjectService.java` — Carbon neutral project lifecycle
- `CreditScoreService.java` — Credit score calculation and events
- `EmissionRatingService.java` — Emission rating management
- `DoubleAuctionService.java` — Double auction matching engine
- `TradeService.java` — P2P carbon trading
- `BlockchainService.java` — Blockchain attestation (hash storage)
- `DigitalSignatureService.java` — RSA digital signature operations
- `MinioService.java` — File upload/download via MinIO
- `SearchService.java` — Full-text search
- `ReviewerService.java` — Review workflow
- `ThirdPartyService.java` — Third-party monitoring data
- `CaptchaService.java` — CAPTCHA generation
- `RedisLockService.java` — Distributed lock management
- `CachePreloadService.java` — Cache warming on startup
- `MetricsService.java` — Application metrics collection

## For AI Agents

Services are `@Service` beans with `@Transactional`. Call repositories for data, other services for cross-domain logic. Never access repositories directly from controllers.
