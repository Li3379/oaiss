# Feature Landscape: OAISS CHAIN Business Flows

**Domain:** Carbon trading and blockchain platform
**Researched:** 2026-05-08
**Source:** 16 REST controllers, 19 services, 22 entities, 8 enums
**Confidence:** HIGH (derived directly from source code)

## Role Reference

| Role | Code | Home Path | Primary Domain |
|------|------|-----------|----------------|
| ENTERPRISE | 1 | `/enterprise/carbon/upload` | Carbon reports, trading, projects, credit |
| REVIEWER | 2 | `/auditor/audit/list` | Report review, credit deduction |
| THIRD_PARTY | 3 | `/third-party/monitor` | Monitoring, compliance |
| ADMIN | 4 | `/admin/system/users` | System management, matching, credit admin |
| AUTHENTICATOR | 5 | `/authenticator/verify/list` | Certification verification |

---

## Domain 1: Authentication & Authorization

**Controllers:** `AuthController` (`/auth`), `CaptchaController` (`/captcha`)
**Services:** `AuthService`, `CaptchaService`
**Tables:** `users`

### Flow 1.1: Login (ALL roles)

| Aspect | Detail |
|--------|--------|
| Initiator | Any role |
| Steps | 1. `GET /captcha/generate` -- returns captcha image (Base64) + captchaKey. 2. `POST /auth/login {username, password, captchaKey, captchaCode}` -- validates credentials + captcha. 3. Frontend stores accessToken + refreshToken in Pinia store. 4. `GET /auth/me` -- loads user details into context. |
| Data Created | JWT access token, JWT refresh token |
| Expected Outcome | `ApiResponse<LoginResponse>` with `{accessToken, refreshToken}`. User redirected to role-specific home path. |
| Edge Cases | Wrong password (401), wrong/expired captcha (error 1005), disabled account (status=0), expired token, concurrent logins from different devices |

### Flow 1.2: Registration (PUBLIC)

| Aspect | Detail |
|--------|--------|
| Initiator | Public (no auth) |
| Steps | 1. `GET /user/check-username?username=X` -- check availability. 2. `GET /user/check-email?email=X` -- check availability. 3. `POST /auth/register {username, password, email, userType, ...}` -- creates user. |
| Data Created | `users` row with specified `userType` (1=ENTERPRISE, 2=REVIEWER, 3=THIRD_PARTY, 4=ADMIN, 5=AUTHENTICATOR) |
| Expected Outcome | `ApiResponse<LoginResponse>` -- auto-login after registration |
| Edge Cases | Duplicate username (error 1001), duplicate email (error 1002), invalid userType, weak password |

### Flow 1.3: Token Refresh (ALL roles)

| Aspect | Detail |
|--------|--------|
| Initiator | Any authenticated user |
| Steps | 1. Axios interceptor detects 401 response. 2. `POST /auth/refresh` with `Refresh-Token` header. 3. Receives new token pair, retries original request. |
| Data Modified | New JWT pair issued |
| Expected Outcome | Seamless session extension |
| Edge Cases | Expired refresh token (401), revoked token after logout, refresh token reuse |

### Flow 1.4: Logout (ALL roles)

| Aspect | Detail |
|--------|--------|
| Initiator | Any authenticated user |
| Steps | 1. `POST /auth/logout` with `Authorization: Bearer` header. 2. Server invalidates token in Redis cache. |
| Data Modified | Token blacklisted in Redis |
| Expected Outcome | `ApiResponse<Void>` success. Frontend clears Pinia store, redirects to `/login`. |
| Edge Cases | Logout with already-expired token, logout without network |

### Flow 1.5: Password Change (ALL roles)

| Aspect | Detail |
|--------|--------|
| Initiator | Any authenticated user |
| Steps | 1. `PUT /auth/password` or `PUT /user/password` with `{oldPassword, newPassword}`. |
| Data Modified | `users.password_hash` |
| Expected Outcome | Password updated, old tokens invalidated |
| Edge Cases | Wrong old password (401), same as old password, weak new password |

### Flow 1.6: SMS/Email Captcha (PUBLIC)

| Aspect | Detail |
|--------|--------|
| Initiator | Public |
| Steps | 1. `POST /captcha/sms/send {phone}` or `POST /captcha/email/send {email}`. |
| Expected Outcome | Captcha sent (mock in dev environment) |
| Edge Cases | Rate limiting, expired captcha, wrong captcha |

---

## Domain 2: User Profile Management

**Controller:** `UserController` (`/user`)
**Service:** `UserService`
**Tables:** `users`, `enterprises`, `reviewers`, `authenticators`, `third_party_orgs`

### Flow 2.1: View Profile (ALL roles)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /user/profile` -- returns full profile including role-specific data. |
| Expected Outcome | `ApiResponse<UserInfoResponse>` with user info, enterprise info (if ENTERPRISE), etc. |
| Edge Cases | Profile for soft-deleted user, profile with missing enterprise record |

### Flow 2.2: Update Profile (ALL roles)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `PUT /user/profile` with `{name, phone, avatar, ...}`. |
| Data Modified | `users` table fields |
| Expected Outcome | Updated `UserInfoResponse` |
| Edge Cases | Invalid phone format, duplicate email, XSS in name field |

### Flow 2.3: View Other User (ALL roles)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /user/{userId}` -- returns public profile. |
| Expected Outcome | `ApiResponse<UserInfoResponse>` with public fields only |
| Edge Cases | Non-existent user (404), soft-deleted user |

---

## Domain 3: Carbon Emission Report Lifecycle

**Controller:** `CarbonController` (`/carbon`)
**Service:** `CarbonService`
**Tables:** `carbon_reports`, `operation_log`
**Status Flow:** `DRAFT(0) -> SUBMITTED(1) -> UNDER_REVIEW(2) -> APPROVED(3) / REJECTED(4) -> ON_CHAIN(5)`

### Flow 3.1: Create Report (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /file/upload` -- upload supporting documents to MinIO. 2. `POST /carbon/reports` with `{title, emissionData, period, fileUrls, ...}`. |
| Data Created | `carbon_reports` row with `status=DRAFT(0)` |
| Expected Outcome | `ApiResponse<CarbonReportResponse>` with draft report |
| Edge Cases | Empty required fields, file upload failure, report with future dates |

### Flow 3.2: Submit Report (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon/reports/{reportId}/submit` -- transitions DRAFT/REJECTED -> SUBMITTED. |
| Data Modified | `carbon_reports.status` = SUBMITTED(1) |
| Expected Outcome | Report now visible to REVIEWER in audit list |
| Edge Cases | Submit non-draft report (400), submit another user's report (403), submit report with missing attachments |

### Flow 3.3: Review Report (REVIEWER)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon/reports?status=1` -- view pending reports. 2. `GET /carbon/reports/{reportId}` -- view report detail. 3. `POST /carbon/review` with `{reportId, approved: true/false, comment}`. |
| Data Modified | `carbon_reports.status` = APPROVED(3) or REJECTED(4). Credit score updated via `CreditScoreService`. Emission rating calculated. Blockchain hash recorded via `BlockchainService`. |
| Expected Outcome | If approved: report moves to APPROVED, credit score may increase, emission rating generated. If rejected: report moves to REJECTED, enterprise can resubmit. |
| Edge Cases | Review already-reviewed report, review with empty comment, approve report with invalid data |

### Flow 3.4: View My Reports (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon/my-reports?status=X&page=1&size=10` -- paginated list of own reports. |
| Expected Outcome | `ApiResponse<Page<CarbonReportResponse>>` |
| Edge Cases | Empty list, filter by non-existent status |

### Flow 3.5: Delete Report (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `DELETE /carbon/reports/{reportId}` -- soft-delete draft report. |
| Data Modified | `carbon_reports.deleted = true` |
| Expected Outcome | Report removed from list |
| Edge Cases | Delete non-draft report (400), delete another user's report (403), delete non-existent report (404) |

### Flow 3.6: View All Reports (ADMIN, REVIEWER, AUTHENTICATOR, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon/reports?enterpriseId=X&status=Y&keyword=Z&page=1&size=10` |
| Expected Outcome | Filtered, paginated report list |
| Edge Cases | Filter by non-existent enterprise, keyword with special characters |

### Service Interactions

```
CarbonService.reviewReport()
  -> CreditScoreService.updateScore()     // credit adjustment on approval/rejection
  -> EmissionRatingService.rateEnterprise() // generate emission rating
  -> BlockchainService.recordHash()         // record on blockchain (mock mode)
  -> AuditLogAspect                         // record operation log
```

---

## Domain 4: Carbon Coin Management

**Controller:** `CarbonCoinController` (`/carbon-coin`)
**Service:** `CarbonCoinService`
**Tables:** `carbon_coin_accounts`, `carbon_coin_transactions`

### Flow 4.1: View Account Balance (ENTERPRISE, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon-coin/account` -- get current user's account. Admin can pass `?userId=X`. |
| Expected Outcome | `ApiResponse<CarbonCoinAccountResponse>` with balance, frozen amount |
| Edge Cases | Account not yet created (first login), admin querying non-existent user |

### Flow 4.2: Recharge (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-coin/recharge` with `{userId, amount, description}`. |
| Data Modified | `carbon_coin_accounts.balance` += amount. `carbon_coin_transactions` row created (type=RECHARGE). |
| Expected Outcome | Account balance increased |
| Edge Cases | Recharge to non-existent user, negative amount, concurrent recharge |

### Flow 4.3: Transfer (ENTERPRISE, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-coin/transfer` with `{toUserId, amount, description}`. |
| Data Modified | Sender `balance` -= amount, receiver `balance` += amount. Two `carbon_coin_transactions` rows (DEBIT + CREDIT). |
| Expected Outcome | Instant transfer, both accounts updated |
| Edge Cases | Insufficient balance, transfer to self, transfer to non-existent user, concurrent transfers causing negative balance |

### Flow 4.4: View Transaction History (ENTERPRISE, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon-coin/transactions?userId=X&type=Y&page=1&size=10` |
| Expected Outcome | Paginated transaction list |
| Edge Cases | Empty history, filter by invalid type |

---

## Domain 5: Double Auction Trading

**Controller:** `DoubleAuctionController` (`/auction`)
**Service:** `DoubleAuctionService`
**Tables:** `auction_orders`, `matching_results`, `transactions`, `carbon_coin_accounts`
**Order Status Flow:** `PENDING(0) -> PARTIALLY_MATCHED(1) / FULLY_MATCHED(2) / CANCELLED(3)`

### Flow 5.1: Place Buy Order (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /auction/buy` with `{price, quantity, ...}`. |
| Data Created | `auction_orders` row with `direction=BUY, status=PENDING(0)` |
| Expected Outcome | `ApiResponse<AuctionOrderResponse>` |
| Edge Cases | Price <= 0, quantity <= 0, insufficient carbon coin balance for buy order |

### Flow 5.2: Place Sell Order (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /auction/sell` with `{price, quantity, ...}`. |
| Data Created | `auction_orders` row with `direction=SELL, status=PENDING(0)` |
| Expected Outcome | `ApiResponse<AuctionOrderResponse>` |
| Edge Cases | Insufficient carbon quota balance, price/quantity validation |

### Flow 5.3: Execute Matching (ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /auction/match` -- triggers matching algorithm. |
| Data Modified | Matched orders: `status` -> FULLY_MATCHED(2) or PARTIALLY_MATCHED(1). `matching_results` rows created. `transactions` rows created. `carbon_coin_accounts` updated (buyer debited, seller credited). Blockchain hash recorded. |
| Expected Outcome | `ApiResponse<List<MatchingResultResponse>>` with all matches |
| Edge Cases | No matching orders exist, partial match (order quantity > counterparty quantity), price priority ordering, time priority for same price |

### Flow 5.4: View Orders (ADMIN, REVIEWER, THIRD_PARTY, ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /auction/orders?direction=1&status=0&page=1&size=20` -- all orders. 2. `GET /auction/my-orders?direction=X&status=Y` -- own orders only. |
| Expected Outcome | Paginated order list |
| Edge Cases | Filter combinations, empty results |

### Flow 5.5: View Matching Results (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /auction/results?page=1&size=20` |
| Expected Outcome | Paginated matching results for current user |
| Edge Cases | No results yet |

### Known Issues (from PROJECT.md)

- CON-01: `DoubleAuctionService` uses `synchronized` -- not suitable for distributed deployment
- CON-04: No `@Version` optimistic locking -- concurrent order matching may cause inconsistency

---

## Domain 6: P2P Trading

**Controller:** `TradeController` (`/trade`)
**Service:** `TradeService`
**Tables:** `transactions`, `carbon_coin_accounts`
**Trade Status Flow:** `PENDING(0) -> PROCESSING(1) -> COMPLETED(2) / CANCELLED(3) / FAILED(4)`

### Flow 6.1: Create P2P Trade (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /trade/p2p` with `{buyerId, quantity, price, ...}`. |
| Data Created | `transactions` row with `type=P2P, status=PENDING(0)` |
| Expected Outcome | `ApiResponse<TradeResponse>` |
| Edge Cases | Trade with self, insufficient quota, invalid buyer ID, buyer is not ENTERPRISE |

### Flow 6.2: Create Auction Order via TradeController (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /trade/auction` with `{quantity, price, ...}`. |
| Data Created | `transactions` row with `type=AUCTION, status=PENDING(0)` |
| Expected Outcome | `ApiResponse<TradeResponse>` |
| Edge Cases | Insufficient carbon quota, concurrent orders |

### Flow 6.3: Confirm Trade (ENTERPRISE, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /trade/{tradeId}/confirm` -- buyer or seller confirms. |
| Data Modified | `transactions.status` = COMPLETED(2). `carbon_coin_accounts` updated. Blockchain hash recorded. |
| Expected Outcome | Trade finalized, funds and quota transferred |
| Edge Cases | Confirm already-completed trade (400), confirm cancelled trade, confirm by non-party |

### Flow 6.4: Cancel Trade (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /trade/{tradeId}/cancel` -- only initiator can cancel. |
| Data Modified | `transactions.status` = CANCELLED(3) |
| Expected Outcome | Trade cancelled, any frozen funds released |
| Edge Cases | Cancel completed trade (400), cancel by non-initiator (403), cancel already-cancelled trade |

### Flow 6.5: View Trade Details (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /trade/{tradeId}` |
| Expected Outcome | `ApiResponse<TradeResponse>` with full trade info |
| Edge Cases | Non-existent trade (404), access by non-party (data isolation) |

### Flow 6.6: List Trades (ADMIN, REVIEWER, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /trade/list?sellerId=X&buyerId=Y&tradeType=1&status=1&page=1&size=10` |
| Expected Outcome | Filtered, paginated trade list |
| Edge Cases | Filter by non-existent user, conflicting filters |

### Flow 6.7: List My Trades (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /trade/my-trades?tradeType=X&status=Y&page=1&size=10` |
| Expected Outcome | Paginated list of trades where user is buyer or seller |
| Edge Cases | Empty list, multiple status filters |

---

## Domain 7: Carbon Neutral Project Management

**Controller:** `CarbonNeutralProjectController` (`/carbon-neutral`)
**Service:** `CarbonNeutralProjectService`
**Tables:** `carbon_neutral_projects`
**Project Lifecycle:** `PREPARING(0) -> PENDING_REVIEW(1) -> REVIEW_APPROVED(2) -> IMPLEMENTING(3) -> COMPLETED(4) / TERMINATED(5) / REVIEW_REJECTED(6)`
**Additional states:** PENDING_VERIFICATION, VERIFIED, PENDING_CERTIFICATION, CERTIFIED

### Flow 7.1: Create Project (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral` with `{projectName, projectType, description, estimatedReduction, ...}`. |
| Data Created | `carbon_neutral_projects` row with `status=PREPARING(0)` |
| Project Types | 1=Carbon Sink, 2=CCUS, 3=Renewable Energy, 4=Energy Saving, 5=Other |
| Expected Outcome | `ApiResponse<CarbonNeutralProjectResponse>` |
| Edge Cases | Duplicate project name, missing required fields |

### Flow 7.2: Update Project (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `PUT /carbon-neutral/{id}` with updated fields. |
| Data Modified | Project fields updated |
| Expected Outcome | Updated project response |
| Edge Cases | Update non-draft project (400), update another user's project (403) |

### Flow 7.3: Submit for Review (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/submit`. |
| Data Modified | `status` = PENDING_REVIEW(1) |
| Expected Outcome | Project now visible to ADMIN/REVIEWER |
| Edge Cases | Submit non-draft project, submit incomplete project |

### Flow 7.4: Review Project (ADMIN, REVIEWER)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/review` with `{approved: true/false, comment}`. |
| Data Modified | `status` = REVIEW_APPROVED(2) or REVIEW_REJECTED(6) |
| Expected Outcome | If approved: ready for implementation. If rejected: enterprise can update and resubmit. |
| Edge Cases | Review non-pending project, review with empty comment |

### Flow 7.5: Start Implementation (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/start`. |
| Data Modified | `status` = IMPLEMENTING(3) |
| Expected Outcome | Project enters active implementation phase |
| Edge Cases | Start non-approved project |

### Flow 7.6: Apply for Verification (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/submit-verification` with `{verifierId}`. |
| Data Modified | `status` = PENDING_VERIFICATION. Assigned to verifier. |
| Expected Outcome | Project appears in verifier's pending list |
| Edge Cases | Apply before implementation, invalid verifier ID |

### Flow 7.7: Verify Project (AUTHENTICATOR/VERIFIER, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon-neutral/pending-verification` -- view assigned projects. 2. `POST /carbon-neutral/verify` with `{projectId, approved, verificationReport, verifiedReduction}`. |
| Data Modified | Verification status updated. If approved: carbon credits issued. |
| Expected Outcome | Project verified, credits potentially issued |
| Edge Cases | Verify non-pending project, verify by non-assigned verifier |

### Flow 7.8: Apply for Certification (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/apply-certification` with `{certOrg}`. |
| Data Modified | `status` = PENDING_CERTIFICATION |
| Expected Outcome | Project submitted to certification body |
| Edge Cases | Apply before verification |

### Flow 7.9: Complete Certification (AUTHENTICATOR/CERTIFIER, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/certify` with `{certNo}`. |
| Data Modified | Certification number assigned, `status` updated |
| Expected Outcome | Project fully certified |
| Edge Cases | Certify non-pending project |

### Flow 7.10: Use Carbon Credits (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/use-credits` with `{amount}`. |
| Data Modified | Available credits reduced |
| Expected Outcome | Credits consumed |
| Edge Cases | Use more than available (400), use on non-completed project |

### Flow 7.11: Update Monitoring Data (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `PUT /carbon-neutral/{id}/monitoring` with `{monitoringData}`. |
| Data Modified | Monitoring data field updated |
| Expected Outcome | Monitoring record appended |
| Edge Cases | Update non-implementing project |

### Flow 7.12: Terminate Project (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /carbon-neutral/{id}/terminate` with `{reason}`. |
| Data Modified | `status` = TERMINATED(5) |
| Expected Outcome | Project terminated |
| Edge Cases | Terminate completed project, terminate without reason |

### Flow 7.13: Search Projects (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /carbon-neutral/search?projectType=1&status=3&keyword=林业&page=1&size=20`. 2. `GET /carbon-neutral/my?status=X` -- own projects. |
| Expected Outcome | Filtered, paginated project list |
| Edge Cases | Empty results, invalid project type |

---

## Domain 8: Credit Scoring

**Controller:** `CreditScoreController` (`/credit`)
**Service:** `CreditScoreService`
**Tables:** `credit_scores`, `credit_events`
**Credit Levels:** EXCELLENT(80-100), GOOD(60-79), WARNING(40-59), DANGER(20-39), FROZEN(0-19)

### Flow 8.1: View My Score (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /credit/my-score` |
| Expected Outcome | `ApiResponse<CreditScoreResponse>` with score, level, status |
| Edge Cases | Score not yet initialized (new enterprise) |

### Flow 8.2: View Credit History (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /credit/history?eventType=X&page=1&size=20` |
| Expected Outcome | Paginated list of credit events (bonuses, deductions, etc.) |
| Edge Cases | Empty history, filter by invalid event type |

### Flow 8.3: View Other Enterprise Score (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /credit/{enterpriseId}`. 2. `GET /credit/{enterpriseId}/history?eventType=X` |
| Expected Outcome | Enterprise credit info and history |
| Edge Cases | Non-existent enterprise (404) |

### Flow 8.4: Deduct Points (ADMIN, REVIEWER)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /credit/deduct` with `{enterpriseId, eventType, description, relatedReportId}`. |
| Data Modified | `credit_scores.score` decreased. `credit_events` row created. Level may change (e.g., GOOD -> WARNING). |
| Expected Outcome | Score reduced, event recorded |
| Edge Cases | Deduct from non-existent enterprise, deduct causing level downgrade to FROZEN (trading restriction), double deduction |

### Flow 8.5: Add Bonus Points (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /credit/bonus?enterpriseId=X&points=10&description=...` |
| Data Modified | `credit_scores.score` increased. `credit_events` row created. |
| Expected Outcome | Score increased |
| Edge Cases | Add to already-max score, add to frozen enterprise |

### Flow 8.6: Evaluate Level (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /credit/evaluate/{enterpriseId}` |
| Data Modified | `credit_scores.level` recalculated from current score |
| Expected Outcome | Level updated to match current score |
| Edge Cases | Evaluate non-existent enterprise |

### Flow 8.7: View Restricted/Frozen Enterprises (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /credit/restricted` -- enterprises with WARNING level. 2. `GET /credit/frozen` -- enterprises with FROZEN level. |
| Expected Outcome | List of enterprises with restricted trading |
| Edge Cases | Empty lists |

### Flow 8.8: Check Trade Permission (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /credit/check-permission/{enterpriseId}` |
| Expected Outcome | `Boolean` -- true if enterprise can trade, false if restricted/frozen |
| Edge Cases | Non-existent enterprise |

### Service Interactions

```
CreditScoreService.deductPoints()
  -> Check if score drops below WARNING threshold -> restrict trading
  -> Check if score drops below FROZEN threshold -> freeze account
  -> Create CreditEvent record

CreditScoreService.evaluateLevel()
  -> fromScore(score) -> EXCELLENT / GOOD / WARNING / DANGER / FROZEN
```

---

## Domain 9: Digital Signatures

**Controller:** `DigitalSignatureController` (`/signature`)
**Service:** `DigitalSignatureService`
**Tables:** `rsa_key_pairs`
**Algorithm:** SHA256withRSA

### Flow 9.1: Generate Key Pair (ENTERPRISE, REVIEWER, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /signature/keypair/generate`. |
| Data Created | `rsa_key_pairs` row with public key + encrypted private key. Old key pair auto-revoked. |
| Key Validity | 2 years |
| Expected Outcome | `ApiResponse<RsaKeyPairResponse>` with public key info (private key NOT returned) |
| Edge Cases | Generate when active key exists (old one revoked), key generation failure (error 5001) |

### Flow 9.2: View Key Pair (ENTERPRISE, REVIEWER, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /signature/keypair` |
| Expected Outcome | Key pair info (public key, status, expiry) -- private key excluded |
| Edge Cases | No key pair exists (error 5002) |

### Flow 9.3: Revoke Key Pair (ENTERPRISE, REVIEWER, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `DELETE /signature/keypair` |
| Data Modified | Key pair marked as revoked |
| Expected Outcome | Key pair invalidated, signing no longer possible |
| Edge Cases | Revoke already-revoked key |

### Flow 9.4: Sign Report (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /signature/sign` with report data (JSON string). |
| Data Modified | Signature generated using private key |
| Expected Outcome | `ApiResponse<SignatureResult>` with signature data |
| Edge Cases | No key pair (5002), expired key (5003), signing failure (5004), data isolation check |

### Flow 9.5: Verify Signature (REVIEWER, THIRD_PARTY, ADMIN)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /signature/verify` with `{reportData, signatureData, signerId}`. |
| Expected Outcome | `ApiResponse<SignatureResult>` with `{valid: true/false}` |
| Edge Cases | Signer key not found (5002), tampered data, expired signer key |

### Flow 9.6: Encrypt for Reviewer (ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /signature/encrypt?reviewerId=X` with plaintext data. |
| Expected Outcome | `ApiResponse<String>` with encrypted data (Base64) |
| Edge Cases | Reviewer has no key pair (5002), encryption failure (5007) |

### Flow 9.7: Decrypt Data (REVIEWER, ENTERPRISE)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /signature/decrypt` with Base64 encrypted data. |
| Expected Outcome | `ApiResponse<String>` with decrypted plaintext |
| Edge Cases | Wrong key (5008), corrupted data, no key pair |

---

## Domain 10: File Operations

**Controller:** `FileController` (`/file`)
**Service:** `MinioService`
**Storage:** MinIO object storage

### Flow 10.1: Upload Single File (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /file/upload` (multipart/form-data) with `file` + optional `folder`. |
| Data Created | File stored in MinIO, metadata recorded |
| Max Size | 100MB per file |
| Expected Outcome | `ApiResponse<UploadResult>` with objectName, url, size |
| Edge Cases | Empty file, unsupported format, size > 100MB (413), folder with special characters |

### Flow 10.2: Batch Upload (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /file/upload/batch` (multipart/form-data) with `files[]` + optional `folder`. |
| Max Files | 10 per batch |
| Expected Outcome | `ApiResponse<List<UploadResult>>` |
| Edge Cases | More than 10 files, mix of valid/invalid files |

### Flow 10.3: Download File (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /file/download?objectName=reports/2024/report.pdf` |
| Expected Outcome | File stream with correct Content-Type and Content-Disposition headers |
| Edge Cases | Non-existent file (404), special characters in filename, concurrent downloads |

### Flow 10.4: Delete File (Admin or file owner)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `DELETE /file?objectName=...` or `DELETE /file/batch` with object names list. |
| Permission | Admin (userType=4) can delete any file. Regular users can only delete their own files. |
| Expected Outcome | File removed from MinIO |
| Edge Cases | Delete non-existent file, delete by non-owner (403), delete referenced file |

### Flow 10.5: Get File Info / Check Existence (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /file/info?objectName=...` -- metadata. 2. `GET /file/exists?objectName=...` -- boolean. |
| Expected Outcome | File metadata or existence check |
| Edge Cases | Non-existent file |

### Flow 10.6: Presigned URLs (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /file/presigned-url?objectName=...` -- download URL (1hr expiry). 2. `GET /file/presigned-upload-url?objectName=...` -- upload URL. |
| Expected Outcome | Temporary URL for direct MinIO access |
| Edge Cases | Expired URL, non-existent file |

### Flow 10.7: List Files (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /file/list?prefix=reports/2024/&page=1&size=20` |
| Expected Outcome | `ApiResponse<FileListResult>` with file list |
| Edge Cases | Empty folder, non-existent prefix |

---

## Domain 11: Emission Ratings & AI Prediction

**Controller:** `EmissionController` (`/emission`)
**Services:** `EmissionRatingService`, `CarbonPredictionService`
**Tables:** `emission_ratings`

### Flow 11.1: View Enterprise Rating History (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /emission/ratings/{enterpriseId}` |
| Expected Outcome | `ApiResponse<List<EmissionRating>>` with historical ratings |
| Edge Cases | Enterprise with no ratings, non-existent enterprise |

### Flow 11.2: Generate Rating (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /emission/ratings` with `{enterpriseId, year, totalEmission, revenue, ratedBy}`. |
| Data Created | `emission_ratings` row with calculated level |
| Expected Outcome | `ApiResponse<EmissionRating>` with rating result |
| Edge Cases | Duplicate rating for same year, zero revenue (division by zero), negative emission |

### Flow 11.3: Industry Rankings (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /emission/rankings/{year}` |
| Expected Outcome | List of enterprises ranked by emission intensity |
| Edge Cases | Year with no data, future year |

### Flow 11.4: AI Carbon Prediction (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `POST /emission/predict` with `{enterpriseId, historicalData, predictionYears}`. |
| Expected Outcome | `ApiResponse<CarbonPredictionResponse>` with predicted values |
| Edge Cases | Insufficient historical data, prediction for distant future |

---

## Domain 12: Blockchain Explorer

**Controller:** `BlockchainController` (`/blockchain`)
**Service:** `BlockchainService`
**Mode:** Mock (no real blockchain integration)

### Flow 12.1: Check Blockchain Status (ADMIN, AUTHENTICATOR)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /blockchain/status` |
| Expected Outcome | Network info (mock): network name, channel count, node status |
| Edge Cases | Mock always returns success |

### Flow 12.2: Query Block (ADMIN, AUTHENTICATOR, THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /blockchain/block/{blockNumber}` |
| Expected Outcome | Block details (mock data) |
| Edge Cases | Non-existent block number |

### Flow 12.3: Query Transaction (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /blockchain/transaction/{txHash}` |
| Expected Outcome | Transaction details |
| Edge Cases | Non-existent hash |

### Flow 12.4: List Transactions (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /blockchain/transactions?page=1&size=20` |
| Expected Outcome | Paginated transaction list |
| Edge Cases | Empty list |

### Flow 12.5: Latest Blocks (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /blockchain/blocks/latest` |
| Expected Outcome | List of recent blocks |
| Edge Cases | No blocks yet |

---

## Domain 13: Search

**Controller:** `SearchController` (`/search`)
**Service:** `SearchService`

### Flow 13.1: Search Carbon Reports (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /search/reports?keyword=年度&enterpriseId=1&status=2&page=1&size=20` |
| Expected Outcome | `ApiResponse<Page<CarbonReportSummary>>` |
| Edge Cases | Empty keyword, special characters, no results |

### Flow 13.2: Search Trades (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /search/trades?sellerId=1&buyerId=2&tradeType=1&status=1&page=1&size=20` |
| Expected Outcome | `ApiResponse<Page<TradeSummary>>` |
| Edge Cases | Conflicting filters, no results |

### Flow 13.3: Market Overview (ALL authenticated)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /search/market-overview` |
| Expected Outcome | `ApiResponse<MarketOverview>` with total volume, average price, active enterprises |
| Edge Cases | No trades yet (zero values) |

---

## Domain 14: Admin Functions

**Controller:** `AdminController` (`/admin`)
**Service:** Direct repository access
**Tables:** `users`

### Flow 14.1: List Users (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /admin/users?userType=1&status=1&page=1&size=10` |
| Expected Outcome | `ApiResponse<Page<User>>` with filtered user list |
| Edge Cases | Filter by non-existent type, empty results |

### Flow 14.2: Enable/Disable User (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `PUT /admin/users/{userId}/status?status=0` (disable) or `?status=1` (enable). |
| Data Modified | `users.status` field |
| Expected Outcome | User status changed. Disabled users cannot login. |
| Edge Cases | Disable self, disable non-existent user (404), disable already-disabled user |

### Flow 14.3: Dashboard Data (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /admin/dashboard` |
| Expected Outcome | `{totalUsers, activeUsers}` |
| Edge Cases | System with zero users |

### Flow 14.4: System Statistics (ADMIN only)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /admin/statistics` |
| Expected Outcome | `{totalUsers, enterpriseCount, reviewerCount, thirdPartyCount}` |
| Edge Cases | Counts may not include soft-deleted users depending on query |

---

## Domain 15: Third-Party Monitoring

**Controller:** `ThirdPartyController` (`/third-party`)
**Service:** `ThirdPartyService`
**Tables:** `third_party_orgs`, `carbon_reports`
**Access:** ALL endpoints require `THIRD_PARTY` role (class-level `@PreAuthorize`)

### Flow 15.1: View Organization Info (THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /third-party/org-info` |
| Expected Outcome | `ApiResponse<ThirdPartyOrg>` with org details |
| Edge Cases | Org record missing for user |

### Flow 15.2: Query Carbon Reports (THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /third-party/carbon-reports?enterpriseId=X&status=Y&keyword=Z&page=1&size=10` |
| Expected Outcome | `ApiResponse<Page<CarbonReport>>` -- regulatory view of all reports |
| Edge Cases | Filter combinations, access to reports from all enterprises |

### Flow 15.3: View Statistics (THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `GET /third-party/statistics` |
| Expected Outcome | `ApiResponse<Map>` with regulatory statistics (report count, approval rate, etc.) |
| Edge Cases | No data in jurisdiction |

### Flow 15.4: Update Contact Info (THIRD_PARTY)

| Aspect | Detail |
|--------|--------|
| Steps | 1. `PUT /third-party/contact?contactPerson=张三&contactPhone=13800138000` |
| Data Modified | `third_party_orgs` contact fields |
| Expected Outcome | Contact updated |
| Edge Cases | Invalid phone format |

---

## Cross-Cutting: AOP Concerns

All flows are intercepted by these aspects. Testing should verify they fire correctly.

| Annotation | What to Verify |
|------------|----------------|
| `@AuditLog` | Operation logged to `operation_log` table with correct user, action, timestamp |
| `@RateLimit` | Requests beyond limit return 429 Too Many Requests |
| `@RequirePermission` | Unauthorized access returns 403 |
| `@DataIsolation` | Enterprise A cannot see Enterprise B's data |
| `@DistributedLock` | Concurrent operations on same resource are serialized |

---

## Critical Path Flows (Must Work)

These flows are prerequisites for the system to be useful. If any fail, the platform is non-functional.

| Priority | Flow | Roles | Reason |
|----------|------|-------|--------|
| P0 | 1.1 Login | ALL | Cannot use system without login |
| P0 | 1.3 Token Refresh | ALL | Sessions expire without refresh |
| P0 | 3.1 Create Report | ENTERPRISE | Core business function |
| P0 | 3.2 Submit Report | ENTERPRISE | Triggers review workflow |
| P0 | 3.3 Review Report | REVIEWER | Core approval workflow |
| P0 | 4.1 View Balance | ENTERPRISE | Prerequisite for trading |
| P0 | 5.1 Place Buy Order | ENTERPRISE | Core trading function |
| P0 | 5.2 Place Sell Order | ENTERPRISE | Core trading function |
| P0 | 5.3 Execute Matching | ADMIN | Trades cannot settle without matching |
| P0 | 6.1 Create P2P Trade | ENTERPRISE | Alternative trading path |
| P0 | 6.3 Confirm Trade | ENTERPRISE | Trade settlement |
| P0 | 14.2 Enable/Disable User | ADMIN | Account management |

## Secondary Flows (Important)

| Priority | Flow | Roles | Reason |
|----------|------|-------|--------|
| P1 | 2.1 View Profile | ALL | User identity verification |
| P1 | 4.3 Transfer | ENTERPRISE | Carbon coin movement |
| P1 | 7.1-7.12 Project Lifecycle | ENTERPRISE, ADMIN, AUTHENTICATOR | Full project workflow |
| P1 | 8.1-8.8 Credit Scoring | ENTERPRISE, ADMIN, REVIEWER | Trading restriction logic |
| P1 | 9.1-9.7 Digital Signatures | ENTERPRISE, REVIEWER | Report integrity |
| P1 | 10.1 File Upload | ALL | Document management |
| P1 | 11.1-11.4 Emission Ratings | ALL | Carbon assessment |
| P1 | 15.1-15.4 Third-Party Monitoring | THIRD_PARTY | Regulatory compliance |
| P1 | 13.3 Market Overview | ALL | Market transparency |

## Edge Cases (Boundary Conditions)

### Cross-Role Access Control

| Test | Expected |
|------|----------|
| ENTERPRISE tries `POST /carbon/review` | 403 Forbidden |
| REVIEWER tries `POST /auction/buy` | 403 Forbidden |
| THIRD_PARTY tries `POST /credit/deduct` | 403 Forbidden |
| ADMIN tries `POST /trade/p2p` | 403 Forbidden (ADMIN not in allowed roles) |
| Unauthenticated tries `GET /carbon/reports` | 401 Unauthorized |
| ENTERPRISE tries `GET /carbon/reports` (all reports) | 403 (only `GET /carbon/my-reports` allowed) |

### Data Isolation

| Test | Expected |
|------|----------|
| Enterprise A views Enterprise B's draft report | 404 or filtered out |
| Enterprise A signs report with Enterprise B's data | Blocked by @DataIsolation |
| Enterprise A transfers to non-existent user | Error response |

### State Machine Violations

| Test | Expected |
|------|----------|
| Submit already-submitted report | 400 "状态不允许提交" |
| Review draft report (not submitted) | 400 "不在待审核状态" |
| Delete submitted report | 400 "状态不允许删除" |
| Start implementation before review approval | 400 |
| Confirm already-completed trade | 400 |
| Cancel completed trade | 400 |
| Use carbon credits before verification | 400 |

### Financial Integrity

| Test | Expected |
|------|----------|
| Transfer more than balance | Error, no money created |
| Two concurrent transfers from same account | Balance never goes negative |
| Matching with price = 0 | Validation error |
| Matching with quantity = 0 | Validation error |
| Buy order with insufficient carbon coins | Error at order placement |
| Sell order with insufficient carbon quota | Error at order placement |

### Concurrent Operations

| Test | Expected |
|------|----------|
| Two reviewers approve same report simultaneously | Only one succeeds |
| Two enterprises place competing buy orders | Both recorded correctly |
| Admin executes matching while orders being placed | No lost orders |
| Token refresh during active request | Seamless retry |

---

## Controller Coverage Matrix

| Controller | Flows | Roles Tested |
|------------|-------|--------------|
| AuthController | 1.1-1.5 | ALL (5 roles) |
| CaptchaController | 1.6 | PUBLIC |
| UserController | 2.1-2.3 | ALL |
| CarbonController | 3.1-3.6 | ENTERPRISE, REVIEWER, ADMIN, AUTHENTICATOR, THIRD_PARTY |
| CarbonCoinController | 4.1-4.4 | ENTERPRISE, ADMIN |
| DoubleAuctionController | 5.1-5.5 | ENTERPRISE, ADMIN, REVIEWER, THIRD_PARTY |
| TradeController | 6.1-6.7 | ENTERPRISE, ADMIN, REVIEWER, THIRD_PARTY |
| CarbonNeutralProjectController | 7.1-7.13 | ENTERPRISE, ADMIN, REVIEWER, AUTHENTICATOR |
| CreditScoreController | 8.1-8.8 | ENTERPRISE, ADMIN, REVIEWER |
| DigitalSignatureController | 9.1-9.7 | ENTERPRISE, REVIEWER, THIRD_PARTY |
| FileController | 10.1-10.7 | ALL |
| EmissionController | 11.1-11.4 | ALL |
| BlockchainController | 12.1-12.5 | ADMIN, AUTHENTICATOR, THIRD_PARTY, ENTERPRISE |
| SearchController | 13.1-13.3 | ALL |
| AdminController | 14.1-14.4 | ADMIN |
| ThirdPartyController | 15.1-15.4 | THIRD_PARTY |

**All 16 controllers covered. All 5 roles covered.**
