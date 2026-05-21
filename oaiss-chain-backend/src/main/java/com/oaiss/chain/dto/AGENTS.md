<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-19 | Updated: 2026-05-19 -->

# com.oaiss.chain.dto

Data Transfer Objects for API request/response payloads. 45 DTOs organized by feature domain.

## Key Files (45 DTOs)

### Infrastructure (4)

| File | Type | Description |
|------|------|-------------|
| `ApiResponse.java` | Response | Generic response envelope `ApiResponse<T>` with `code`, `message`, `data`, `meta` (includes `requestId`, `timestamp`). `@JsonInclude(NON_NULL)`. |
| `PageRequest.java` | Request | Pagination params: `pageNum` (1-based), `pageSize` (1-100), `sortBy`, `sortOrder`, `keyword`, `startTime`/`endTime`. Validation: `@Min(1)`, `@Max(100)`. |
| `PageResponse.java` | Response | Generic pagination result `PageResponse<T>` with `list`, `total`, `pageNum`, `pageSize`, `pages`, `hasPrevious`, `hasNext`. Factory method `from(Page<T>, Function)`. |
| `SearchResponse.java` | Response | Container for search results: inner classes `CarbonReportSummary`, `TradeSummary`, `MarketOverview`. |

### Auth (5)

| File | Type | Description |
|------|------|-------------|
| `LoginRequest.java` | Request | `username` (3-50), `password` (6-100), `captcha`, `captchaKey`. `@NotBlank` + `@Size` validation. |
| `LoginResponse.java` | Response | `accessToken`, `refreshToken`, `tokenType` (Bearer), `expiresIn`, `userId`, `username`, `userType`, `realName`. |
| `RegisterRequest.java` | Request | `username`, `password`, `confirmPassword`, `phone`, `email`, `realName`, `userType`, `creditCode`, `enterpriseName` (for enterprise users). |
| `CaptchaSendRequest.java` | Request | `target` (phone/email, `@NotBlank`), `type` (default 1). |
| `CaptchaResponse.java` | Response | `captchaKey`, `captchaImage` (Base64), `expiresIn` (default 300s). |

### User Profile (3)

| File | Type | Description |
|------|------|-------------|
| `UserInfoResponse.java` | Response | `userId`, `username`, `realName`, `phone`, `email`, `avatar`, `company`, `address`, `userType`, `userTypeDesc`, `status`, `lastLoginAt`, `lastLoginIp`, `createdAt`. |
| `UserProfileUpdateRequest.java` | Request | `realName`, `phone`, `email`, `avatar`, `company`, `address`. All optional with `@Size` validation. |
| `PasswordChangeRequest.java` | Request | `oldPassword`, `newPassword` (6-20), `confirmPassword`. All `@NotBlank`. |

### Carbon Report (2)

| File | Type | Description |
|------|------|-------------|
| `CarbonReportRequest.java` | Request | `accountingPeriod`, `title` (no `<>` chars), `reportType`, `emissionData` (JSON), `calculationMethod`, `attachments`. `@NotBlank`/`@Pattern` validation. |
| `CarbonReportResponse.java` | Response | `id`, `reportNo`, `enterpriseId`, `enterpriseName`, `accountingPeriod`, `title`, `reportType`, `emissionData`, `totalEmission`, `scope1/2/3Emission`, `calculationMethod`, `status`, `statusText`, `reviewerId`, `reviewerName`, `reviewComment`, `reviewedAt`, `signatureData`, `blockchainTxHash`, `onChainAt`, `attachments`, `createdAt`, `updatedAt`. |

### Trade / Auction (5)

| File | Type | Description |
|------|------|-------------|
| `TradeRequest.java` | Request | `tradeType` (1=auction, 2=P2P), `buyerId`, `sellerId`, `quantity` (`@Positive`), `unitPrice` (`@Positive`), `reportId`, `remark`. |
| `TradeResponse.java` | Response | `id`, `tradeNo`, `tradeType`, `tradeTypeText`, `sellerId`, `sellerName`, `buyerId`, `buyerName`, `quantity`, `unitPrice`, `totalAmount`, `reportId`, `status`, `statusText`, `remark`, `blockchainTxHash`, `completedAt`, `createdAt`. |
| `AuctionOrderRequest.java` | Request | `direction` (1=buy, 2=sell), `quantity`, `price`. |
| `AuctionOrderResponse.java` | Response | `id`, `orderNo`, `userId`, `direction`, `directionText`, `quantity`, `price`, `matchedQuantity`, `remainingQuantity`, `status`, `statusText`, `settlementPrice`, `matchedAt`, `createdAt`. |
| `MatchingResultResponse.java` | Response | `id`, `matchNo`, `buyOrderId`, `sellOrderId`, `buyerId`, `sellerId`, `buyerName`, `sellerName`, `matchedQuantity`, `settlementPrice`, `totalAmount`, `status`, `statusText`, `transactionId`, `settledAt`, `createdAt`. |

### Carbon Coin (3)

| File | Type | Description |
|------|------|-------------|
| `CarbonCoinAccountResponse.java` | Response | `id`, `userId`, `balance`, `totalRecharged`, `totalSpent`, `status`. |
| `CarbonCoinRechargeRequest.java` | Request | `amount` (`@NotNull`, `@DecimalMin("0.01")`), `paymentMethod` (1=bank, 2=online), `remark`. |
| `CarbonCoinTransferRequest.java` | Request | `counterpartId` (`@NotNull`), `amount` (`@NotNull`, `@DecimalMin("0.01")`), `remark`. |

### Credit Score (3)

| File | Type | Description |
|------|------|-------------|
| `CreditScoreResponse.java` | Response | `id`, `enterpriseId`, `enterpriseName`, `score`, `level`, `tradeRestricted`, `accountFrozen`, `lastEvaluatedAt`, `createdAt`. |
| `CreditEventResponse.java` | Response | `id`, `enterpriseId`, `eventType`, `eventTypeName`, `eventDescription`, `pointsChanged`, `scoreBefore`, `scoreAfter`, `relatedReportId`, `relatedTradeId`, `triggeredBy`, `triggeredByName`, `triggeredAt`. |
| `CreditDeductionRequest.java` | Request | `enterpriseId` (`@NotNull`), `eventType` (1-4, `@NotNull`), `description` (`@Size(max=500)`), `relatedReportId`. |

### Carbon Neutral Project (3)

| File | Type | Description |
|------|------|-------------|
| `CarbonNeutralProjectRequest.java` | Request | `projectName` (`@NotBlank`, max 200), `projectType` (`@NotNull`, 1-5), `description`, `location`, `expectedReduction`, `investmentAmount`, `startDate`, `endDate`, `methodology`, `applicationData`, `attachments`. |
| `CarbonNeutralProjectResponse.java` | Response | Full project details: `id`, `projectNo`, `projectName`, `projectType/Name`, `ownerId/Name`, `description`, `location`, `expectedReduction`, `actualReduction`, `investmentAmount`, `startDate/endDate`, `status/statusText`, `certStatus/certStatusText`, `certOrg`, `certDate`, `certNo`, `methodology`, `accountingPeriod`, `issuedCredits`, `usedCredits`, `availableCredits`, `reviewerId/Name`, `reviewedAt`, `monitoringData`, `verifierId/Name`, `verificationStatus`, + more. |
| `ProjectVerificationRequest.java` | Request | `projectId` (`@NotNull`), `verifiedReduction` (`@NotNull`, `@Positive`), `verificationReport`, `monitoringData`, `remark`. |

### Emission Rating (1)

| File | Type | Description |
|------|------|-------------|
| `EmissionRatingRequest.java` | Request | `enterpriseId` (`@NotNull`), `year` (`@NotBlank`), `totalEmission` (`@NotNull`), `revenue`, `ratedBy`. |

### Digital Signature (3)

| File | Type | Description |
|------|------|-------------|
| `RsaKeyPairResponse.java` | Response | `id`, `userId`, `publicKey`, `createdAt`, `expiresAt`, `keyStatus`, `keyStatusText`. **Never exposes privateKey.** |
| `SignatureResult.java` | Response | `signature` (Base64), `algorithm` (e.g. SHA256withRSA), `timestamp`, `valid` (for verify result), `signerId`. |
| `SignatureVerifyRequest.java` | Request | `reportId` (`@NotNull`), `signatureData` (`@NotBlank`), `reportData` (`@NotBlank`), `signerId` (optional). |

### Review (1)

| File | Type | Description |
|------|------|-------------|
| `ReviewRequest.java` | Request | `reportId` (`@NotNull`), `reviewResult` (3=approve, 4=reject, `@NotNull`), `reviewComment`. |

### ML / Prediction (8)

| File | Type | Description |
|------|------|-------------|
| `CarbonPredictionRequest.java` | Request | `enterpriseId` (`@NotNull`), `predictMonths` (1-12, default 6). |
| `CarbonPredictionResponse.java` | Response | `enterpriseId`, `confidence` (0-1), `message`, `predictions` (List of `PredictionPoint`: `period`, `predictedEmission`), `generatedAt`. |
| `EmissionForecastRequest.java` | Request | `enterpriseId` (`@NotNull`), `dates` (historical, `@NotEmpty`), `emissions` (historical, `@NotEmpty`), `sector` (default "power_generation"), `horizonDays` (default 180). `@JsonNaming(SnakeCaseStrategy)`. |
| `EmissionForecastResponse.java` | Response | `enterpriseId`, `forecastDates`, `forecastEmissions`, `lowerBound`, `upperBound`, `trend`, `confidence`, `modelVersion`. |
| `MarketForecastRequest.java` | Request | `dates` (`@NotEmpty`), `prices` (`@NotEmpty`), `volumes` (`@NotEmpty`), `horizonDays` (default 30). `@JsonNaming(SnakeCaseStrategy)`. |
| `MarketForecastResponse.java` | Response | `forecastDates`, `forecastPrices`, `lowerBound`, `upperBound`, `trend`, `modelVersion`. |
| `EnterpriseInferenceRequest.java` | Request | `enterpriseId` (`@NotNull`), `reportCount`, `totalEmissions`, `creditScore` (0-100), `emissionRating`, `transactionVolume`, `complianceFlags`, `avgEmissionPerReport`, `daysSinceLastReport`. `@JsonNaming(SnakeCaseStrategy)`. |
| `EnterpriseInferenceResponse.java` | Response | `enterpriseId`, `complianceStatus`, `confidence`, `anomalyScore`, `isAnomaly`, `riskFactors` (List), `modelVersion`. |

### Carbon Calculation (4)

| File | Type | Description |
|------|------|-------------|
| `PowerGenerationCalculationRequest.java` | Request | 25-parameter GB/T 32150-2015 formula. 5 fuel types x 4 params each (consumption, NCV, CC, OF) + desulfurization 3 params + metadata 2 params. All `@DecimalMin`/`@DecimalMax` validated. |
| `PowerGenerationCalculationResponse.java` | Response | `totalEmission`, `combustionEmission`, `desulfurizationEmission`, `fuelDetails` (List of `FuelEmissionDetail`), `reportingYear`, `enterpriseName`, `formulaReference`, `calculatedAt`. |
| `PowerGridCalculationRequest.java` | Request | 9-parameter grid emission: `transmissionVolume` (`@NotNull`), `lineLossRate` (0-1), `gridEmissionFactor` (`@NotNull`), `generationVolume`, `importedElectricity`, `exportedElectricity`, `importEmissionFactor`, `reportingYear` (`@NotNull`), `enterpriseName` (`@NotBlank`). |
| `PowerGridCalculationResponse.java` | Response | `totalEmission`, `transmissionLossEmission`, `importedEmission`, `transmissionLoss`, `formulaReference`, `reportingYear`, `enterpriseName`, `calculatedAt`. |

## For AI Agents

- **Use Lombok `@Data`/`@Builder`** on DTOs. Some use `@Getter`/`@Setter` instead of `@Data` (e.g., `PasswordChangeRequest`, `UserInfoResponse`, `UserProfileUpdateRequest`).
- **Keep DTOs flat** -- no nested entity references. Use `Long` IDs for references; resolve names in the service layer (e.g., `sellerId` + `sellerName` in `TradeResponse`).
- **Request DTOs**: Always add Jakarta validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@DecimalMin`, `@DecimalMax`, `@Pattern`). These are enforced by `@Valid` on controller parameters.
- **Response DTOs**: Include human-readable text fields alongside integer codes (e.g., `status` + `statusText`, `direction` + `directionText`, `tradeType` + `tradeTypeText`).
- **`ApiResponse<T>`** is the universal envelope -- all controller methods return `ApiResponse<SomeResponse>`.
- **`PageResponse<T>`** wraps paginated lists -- controllers return `ApiResponse<PageResponse<SomeResponse>>`.
- **ML DTOs** use `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` for Python-compatible snake_case serialization.
- **Sensitive fields**: `RsaKeyPairResponse` intentionally omits `privateKey`. `LoginRequest.password` is never echoed.
- **`SearchResponse`** uses static inner classes (`CarbonReportSummary`, `TradeSummary`, `MarketOverview`) rather than top-level DTOs.
- **`CarbonPredictionResponse`** uses a static inner class `PredictionPoint` for prediction data points.
- **`PowerGenerationCalculationResponse`** uses a static inner class `FuelEmissionDetail` for per-fuel breakdown.
- When adding a new entity, create matching Request and Response DTOs. Request for input validation, Response for API output.
