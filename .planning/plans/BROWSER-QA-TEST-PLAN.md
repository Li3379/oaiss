---
status: active
created: 2026-05-15
author: claude
source: Codebase Onboarding + Deep Research Synthesis
---

# Browser-QA Comprehensive Test Plan

## Research Findings Summary

### Agent 1: Backend Endpoints (18 Controllers, 64 Endpoints)
- All 64 endpoints previously tested via API with 100% pass rate
- SPEC-006 through SPEC-010 bugs already fixed and verified

### Agent 2: Frontend Routes (22 Page Routes, 4 Roles)
- ENTERPRISE: 11 pages (carbon, trading, credit, blockchain, profile, etc.)
- REVIEWER: 1 page (audit list)
- THIRD_PARTY: 1 page (monitor)
- ADMIN: 5 pages (users, config, statistics, verify, carbon)
- Previous browser-harness: 18/18 pages loaded correctly

### Agent 3: Test Coverage Gaps
- **Missing backend tests**: EnterpriseController, EnterpriseService, ReviewerService, BlockchainController
- **Missing frontend tests**: SystemConfig, SystemUsers, VerifyList, LanguageSwitcher, PageContainer
- **No E2E framework** in place - browser-harness fills this gap

### Agent 4: Service Layer Issues (CRITICAL FINDINGS)
1. **Report Status Flow**: ON_CHAIN transition without blockchain error handling
2. **Trade Operations**: No carbon coin balance check before P2P trades
3. **Missing Validation**: Empty username/password, unreasonable prices, negative values
4. **Generic Exceptions**: EnterpriseService throws RuntimeException
5. **No Distributed Locks**: Critical operations (report submit, trade confirm) unprotected
6. **Race Conditions**: Quota updates during concurrent trades
7. **Missing Business Rules**: No min/max trade limits, no price validation

---

## Test Plan: 4 Roles × Business Workflow Coverage

### Phase A: ENTERPRISE Role (11 Pages, Highest Priority)

| # | Workflow | Test Steps | What to Verify |
|---|----------|------------|----------------|
| E1 | Carbon Report CRUD | Create draft → Submit → Verify status | Form validation, status transitions |
| E2 | P2P Trading | Browse market → Create buy/sell order | Order creation, balance checks |
| E3 | Double Auction | View market → Place bid | Bid validation, market display |
| E4 | Credit Score | View score history → Check rating | Score display accuracy |
| E5 | Carbon Coin | View balance → Check transactions | Balance accuracy, transaction list |
| E6 | Blockchain Browser | View transactions → Verify data | Transaction display, block data |
| E7 | Carbon Neutral | View projects → Check status | Project list, status display |
| E8 | Emission Data | View ratings → Check history | Rating accuracy, history display |
| E9 | Company Dashboard | View all data widgets | Data accuracy vs API |
| E10 | User Profile | View → Edit → Save | Profile update persistence |
| E11 | Orders Management | View orders → Filter → Sort | Order list, filtering |

### Phase B: ADMIN Role (5 Pages)

| # | Workflow | Test Steps | What to Verify |
|---|----------|------------|----------------|
| A1 | User Management | List users → Disable → Enable → Role change | User CRUD, status toggle |
| A2 | System Config | List configs → Edit → Save | Config persistence |
| A3 | Statistics | View dashboard → Compare with API | Data accuracy |
| A4 | Verify List | List pending → Approve/Reject | Status transitions |
| A5 | Carbon Admin | View carbon reports → Filter | Report list accuracy |

### Phase C: REVIEWER Role (1 Page)

| # | Workflow | Test Steps | What to Verify |
|---|----------|------------|----------------|
| R1 | Audit List | List reports → Review → Approve/Reject | Review workflow, status update |

### Phase D: THIRD_PARTY Role (1 Page)

| # | Workflow | Test Steps | What to Verify |
|---|----------|------------|----------------|
| T1 | Monitor Dashboard | View monitoring data → Check alerts | Data accuracy, alert display |

### Phase E: Cross-Role & Edge Cases

| # | Test | What to Verify |
|---|------|----------------|
| X1 | Permission Isolation | Enterprise cannot access Admin endpoints |
| X2 | Token Expiry | Expired token shows proper error |
| X3 | Invalid Inputs | Form validation on all input fields |
| X4 | Empty States | Pages handle no-data gracefully |
| X5 | Navigation | All sidebar menu links work |
| X6 | Data Consistency | Page data matches API responses |

---

## Execution Strategy

1. **Browser-Harness**: DOM `.click()` for Element Plus interactions (NOT click_at_xy)
2. **Token Injection**: Split JWT in half for CDP JS eval
3. **Verification**: API calls alongside browser tests for data consistency
4. **Issue Tracking**: Each finding gets a SPEC-ID for fix tracking

## Success Criteria

| Metric | Target |
|--------|--------|
| Per-role coverage | 100% of pages tested |
| Overall pass rate | >90% |
| Business workflows | All primary flows tested |
| Edge cases | Permission isolation verified |
| Data accuracy | Page data matches API |
