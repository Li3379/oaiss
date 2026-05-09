# Phase 2: Carbon Report Lifecycle - Discussion Log

**Date:** 2026-05-09
**Participants:** Claude (assistant), LiShuai (user)
**Status:** Complete — all gray areas resolved, CONTEXT.md written

## Gray Areas Discussed

### 1. Cascading Side Effects (CARB-08/09/10)

**Question:** How should the cascading side effects (credit score, emission rating, blockchain) be wired into the review approval flow?

**Options presented:**
- Synchronous sequential in same transaction
- Async event-driven (Spring Events)
- Dedicated domain event handler

**Decision (D-01/02/03/04):** Synchronous sequential execution inside existing @Transactional. CreditScoreService.addBonusPoints(+5) → EmissionRatingService.rateEnterprise() → BlockchainService.commitReportToChain() → set ON_CHAIN status. Simple, consistent, same transaction rollback on failure.

---

### 2. UNDER_REVIEW(2) Status & State Machine (CARB-05/11)

**Question:** UNDER_REVIEW(2) is unreachable — no "start review" endpoint exists. How to handle?

**Decision (D-05/06/07):**
- Keep UNDER_REVIEW(2) unreachable — reviewer reviews SUBMITTED(1) reports directly
- Add ON_CHAIN(5) transition after successful blockchain mock
- Test reviewer with SUBMITTED(1) status filter, not UNDER_REVIEW(2)
- Valid transitions: DRAFT→SUBMITTED→APPROVED→ON_CHAIN, REJECTED→SUBMITTED (resubmit)

---

### 3. Test Data & Authenticator Scope (CARB-12/13)

**Question:** What testing approach and data strategy?

**Decision (D-08/09/10):**
- API scripts + browser verify (reuse login-test.sh pattern)
- enterprise001 + enterprise002 for report testing; enterprise003 stays clean for Phase 3
- Real MinIO upload for attachments via FileController API

**Authenticator scope (D-11):** Read-only access only — view report list and details. No verify/certify operations (no endpoints exist).

---

### 4. Frontend-Backend Review Field Mapping

**Decision (D-12):** Fix field mismatch in `carbon.ts`. Frontend sends `{ approved: boolean, comment }` → backend expects `{ reviewResult: Integer, reviewComment }`. API layer must map `approved:true→reviewResult:3`, `approved:false→reviewResult:4`, `comment→reviewComment`.

---

## Files Produced

- `02-CONTEXT.md` — Full context document with 12 decisions, canonical refs, code insights

## Next Steps

Run `/gsd-plan-phase 2` to create detailed execution plans based on CONTEXT.md decisions.
