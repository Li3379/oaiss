<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.controller

REST API controllers handling HTTP requests and routing them to service-layer business logic. All controllers live under the `/api/v1` base path configured in `SecurityConfig` and return responses wrapped in `ApiResponse<T>`. Each controller is annotated with `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, and a Swagger `@Tag` for OpenAPI documentation grouping.

## Key Files (20 controllers)

| File | Base Path | Swagger Tag | Role Guard | Description |
|------|-----------|-------------|------------|-------------|
| `AuthController.java` | `/auth` | 01. 认证管理 | Public (no guard) | Login, register, refresh token, captcha integration |
| `UserController.java` | `/user` | 02. 用户中心 | Per-endpoint | User profile management, password changes |
| `CarbonController.java` | `/carbon` | 03. 碳核算管理 | ENTERPRISE / REVIEWER / multi-role | Carbon report CRUD, power grid & generation emission formulas |
| `TradeController.java` | `/trade` | 04. 碳交易管理 | ENTERPRISE / ADMIN / multi-role | P2P carbon trading, trade confirmation, trade history |
| `CreditScoreController.java` | `/credit` | 05. 信誉评分管理 | ENTERPRISE / ADMIN / REVIEWER | Credit score queries, adjustments, grade assessment |
| `CaptchaController.java` | `/captcha` | 06. 验证码管理 | Public | CAPTCHA image generation and validation |
| `FileController.java` | `/file` | 07. 文件管理 | Per-endpoint | MinIO file upload, download, delete, presigned URLs |
| `DigitalSignatureController.java` | `/signature` | 08. 数字签名 | ENTERPRISE / REVIEWER / THIRD_PARTY | RSA key pair management, report signing/verification |
| `BlockchainController.java` | `/blockchain` | 09. 区块链管理 | ADMIN / ENTERPRISE / THIRD_PARTY | Blockchain status, block queries, on-chain transaction records |
| `ReviewerController.java` | `/reviewer` | 10. 审核员管理 | REVIEWER | Report review, carbon-neutral project review, review history |
| `CarbonNeutralProjectController.java` | `/carbon-neutral` | 11. 碳中和项目管理 | Multi-role | Carbon offset projects: create, review, certify, track |
| `DoubleAuctionController.java` | `/auction` | 12. 双向拍卖管理 | ENTERPRISE / ADMIN | Double auction: buy/sell orders, matching, results |
| `EmissionController.java` | `/emission` | 13. 碳排放评级管理 | ENTERPRISE / ADMIN | Emission ratings, industry rankings, AI prediction |
| `SearchController.java` | `/search` | 14. 搜索查询管理 | Per-endpoint | Full-text search for reports, trades, market statistics |
| `ThirdPartyController.java` | `/third-party` | 15. 第三方监管管理 | THIRD_PARTY | Regulatory data queries, report auditing, supervision stats |
| `AdminController.java` | `/admin` | 16. 管理后台 | ADMIN | User management, system config, data stats, admission certs, reviewer qualifications |
| `EnterpriseController.java` | `/enterprise` | 17. 企业用户管理 | ENTERPRISE | Enterprise info, quota management, admission |
| `CarbonCoinController.java` | `/carbon-coin` | 18. 碳币交易管理 | ENTERPRISE / ADMIN | Carbon coin accounts, recharge, transfer, transaction history |
| `MarketPredictionController.java` | `/ai/market` | 19. AI市场预测 | ENTERPRISE / ADMIN | Market trend prediction, supply/demand forecast |
| `EnterpriseInferenceController.java` | `/predict/enterprise` | 20. AI企业推断 | Multi-role | Enterprise emission trend inference, compliance risk, anomaly detection |

## For AI Agents

### Working Instructions

1. **Adding a new endpoint**: Follow the established pattern -- `@RestController` + `@RequestMapping` + `@RequiredArgsConstructor` + `@Tag`. Each method gets `@Operation` for Swagger docs, `@ApiResponses` for status codes, `@PreAuthorize` for role guard, `@Valid` on request DTOs, and returns `ApiResponse<T>`.
2. **Authentication**: Inject `@AuthenticationPrincipal JwtUserDetails` to get the current user. Never trust raw request parameters for identity.
3. **Pagination**: Accept `pageNum`/`pageSize` as request params; convert to Spring Data `PageRequest.of(page, size)` inside the controller. Return `Page<T>` from the service, wrap in `ApiResponse<Page<T>>`.
4. **Error handling**: Throw `BusinessException(ErrorCode, ErrorMessage)` from services -- the global exception handler converts it to `ApiResponse`. Do not catch and wrap in controllers.
5. **AOP annotations**: Apply cross-cutting concerns at the method level:
   - `@AuditLog(module, action)` for operation logging
   - `@RateLimit(key, limit, period)` for request throttling (used on ML/AI endpoints)
   - `@DataIsolation` for tenant data isolation (used on signature endpoints)
   - `@DistributedLock` for concurrency control
   - `@RequirePermission` for fine-grained permission checks
6. **Controller-to-service mapping**: Each controller delegates to one primary service. A few controllers inject additional services or repositories (e.g., `CarbonController` uses `CarbonService` + `PowerGridFormulaService` + `PowerGenerationFormulaService`; `AdminController` injects `UserRepository` and `AccountPermissionListRepository` directly).

### Testing Requirements

- Unit test every endpoint with mocked service layer (`@WebMvcTest` + `@MockBean`).
- Verify `@PreAuthorize` role restrictions: each role should be tested for both access and denial.
- Test `@Valid` input validation: send invalid DTOs and assert 400 responses.
- Test paginated endpoints: verify `pageNum`/`pageSize` conversion and response structure.
- Integration tests (`*IntegrationTest.java`) should cover the full filter chain: JWT validation -> role check -> controller -> service.

### Common Patterns

```java
// Standard controller method pattern
@PostMapping("/reports")
@Operation(summary = "...", description = "...", security = @SecurityRequirement(name = "Bearer Authentication"))
@PreAuthorize("hasRole('ENTERPRISE')")
public ApiResponse<CarbonReportResponse> createReport(
        @Valid @RequestBody CarbonReportRequest request,
        @AuthenticationPrincipal JwtUserDetails userDetails) {
    return ApiResponse.success(carbonService.createReport(request, userDetails.getId()));
}

// Paginated list endpoint pattern
@GetMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
public ApiResponse<Page<CarbonReportResponse>> listReports(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
    Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by("createdAt").descending());
    return ApiResponse.success(carbonService.listReports(pageable));
}
```

## Dependencies

### Internal (service layer)

| Controller | Primary Service | Additional Dependencies |
|------------|----------------|------------------------|
| AuthController | `AuthService` | -- |
| UserController | `UserService` | -- |
| CarbonController | `CarbonService` | `PowerGridFormulaService`, `PowerGenerationFormulaService` |
| TradeController | `TradeService` | -- |
| CreditScoreController | `CreditScoreService` | -- |
| CaptchaController | `CaptchaService` | -- |
| FileController | `MinioService` | -- |
| DigitalSignatureController | `DigitalSignatureService` | -- |
| BlockchainController | `BlockchainServicePort` | -- |
| ReviewerController | `ReviewerService` | `ReviewerQualificationService`, `ReviewerRepository` |
| CarbonNeutralProjectController | `CarbonNeutralProjectService` | -- |
| DoubleAuctionController | `DoubleAuctionService` | -- |
| EmissionController | `EmissionRatingService` | `CarbonPredictionService`, `EnterpriseRepository` |
| SearchController | `SearchService` | -- |
| ThirdPartyController | `ThirdPartyService` | -- |
| AdminController | -- | `UserRepository`, `AccountPermissionListRepository`, `EnterpriseAdmissionService`, `ReviewerQualificationService` |
| EnterpriseController | `EnterpriseService` | `EnterpriseAdmissionService` |
| CarbonCoinController | `CarbonCoinService` | -- |
| MarketPredictionController | `MarketPredictionService` | -- |
| EnterpriseInferenceController | `EnterpriseInferenceService` | -- |

### External (framework)

- **Spring Web**: `@RestController`, `@RequestMapping`, `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`, `@RequestParam`, `@PathVariable`, `@RequestBody`
- **Spring Security**: `@PreAuthorize` (role-based access), `@AuthenticationPrincipal` (current user)
- **Jakarta Validation**: `@Valid` on request DTOs
- **SpringDoc OpenAPI**: `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement`, `@Schema`
- **Project Lombok**: `@RequiredArgsConstructor` (constructor injection via final fields)
- **Spring Data**: `PageRequest`, `Pageable`, `Sort`, `Page<T>`
