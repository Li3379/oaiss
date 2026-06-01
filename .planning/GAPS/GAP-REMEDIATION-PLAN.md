# Gap Remediation Plan: OAISS CHAIN

> **Project:** OAISS CHAIN
> **Current Milestone:** v2.1 (archived)
> **Plan Created:** 2026-06-01
> **Estimated Effort:** 2-3 sessions

---

## Executive Summary

This plan addresses all identified gaps from the project audit. Gaps are organized by priority and dependency, with clear success criteria for each.

| Category | Count | Priority | Est. Effort |
|----------|-------|----------|-------------|
| Missing SUMMARY.md (Phase 05) | 6 files | HIGH | 1 session |
| Missing SUMMARY.md (Phase 14) | 1 file | HIGH | 30 min |
| Missing SUMMARY.md (Phase 09) | 3 files | MEDIUM | 45 min |
| Stale VERIFICATION.md (Phase 12) | 1 file | MEDIUM | 30 min |
| Uncommitted changes | 13 files | LOW | 30 min |
| Backlog items | 3 items | LOW | N/A (future) |

---

## Phase 1: Immediate Cleanup (Session 1)

### 1.1 Commit Uncommitted Changes

**Files:** 13 modified files in working tree
**Action:** Review diff, stage, commit with conventional commit message
**Success Criteria:** Working tree clean (`git status` shows no modifications)

```bash
# Review changes
git diff --stat

# Stage and commit
git add oaiss-chain-frontend/src/views/enterprise/
git add oaiss-chain-frontend/tests/e2e/fixtures/
git add oaiss-chain-frontend/tests/e2e/smoke/
git add scripts/launch-backend.ps1
git commit -m "chore: commit pending frontend changes from v2.1"
```

---

## Phase 2: Document Debt Resolution (Session 1-2)

### 2.1 Phase 05 — Supporting Domains (6 missing SUMMARYs)

**Context:** Phase 05 has 7 plans but only 1 SUMMARY.md. This is the largest documentation gap.

**Approach:**
1. Read each PLAN.md to understand what was planned
2. Check git history for commits matching the plan timeframe
3. Determine if work was executed but not documented, or never executed
4. Write SUMMARY.md for executed work; mark unexecuted plans as "deferred"

**Files to create:**
- [ ] `.planning/phases/05-supporting-domains/05-01-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-02-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-03-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-04-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-05-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-06-SUMMARY.md`
- [ ] `.planning/phases/05-supporting-domains/05-07-SUMMARY.md`

**Template for each SUMMARY:**
```yaml
---
phase: 05
plan: 0N
subsystem: <name>
tags: [<relevant tags>]
dependency_graph:
  requires: []
  provides: []
  affects: []
tech_stack:
  added: []
  patterns: []
key_files:
  created: []
  modified: []
decisions: []
metrics:
  duration: <duration>
  tasks: <count>
  files: <count>
  completed_date: <date>
---

# Phase 05 Plan 0N: <Name> Summary

<Brief description of what was accomplished>

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | <task> | <hash> | <files> |

## Verification

- <verification details>

## Decisions Made

- <decisions>

## Deviations from Plan

### Auto-fixed Issues

None.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- <checklist items>
```

### 2.2 Phase 14 — Performance & Code Quality (1 missing SUMMARY)

**Context:** Phase 14 has 2 plans (14-01, 14-02). 14-02 has SUMMARY, 14-01 does not.

**Action:**
- [ ] Read `14-01-PLAN.md`
- [ ] Check git history for 14-01 execution
- [ ] Write `14-01-SUMMARY.md`

### 2.3 Phase 09 — Blockchain Real Integration (3 missing SUMMARYs)

**Context:** Phase 09 has 3 plans but 0 SUMMARYs. ROADMAP marks it complete.

**Action:**
- [ ] Verify actual execution status (check git history, code changes)
- [ ] If executed: write 3 SUMMARY.md files
- [ ] If partially executed: write SUMMARYs for completed work, mark remainder as deferred
- [ ] If not executed: update ROADMAP.md status to reflect reality

---

## Phase 3: Verification Update (Session 2)

### 3.1 Phase 12 — E2E Testing Verification

**Context:** Phase 12 VERIFICATION.md has `status: gaps_found` with issues that were resolved in v2.1.

**Issues to resolve:**
1. REQ-06 (Carbon Formulas) — **FIXED in v2.1 Phase 17** (blockchain-formula-flow.spec.ts)
2. REQ-03 (Emission Prediction) — **FIXED in v2.1 Phase 17** (ai-prediction-flow.spec.ts)
3. d9/d10 orphaned specs — **FIXED in v2.1 Phase 16** (migrated to v1.1/)
4. Page objects — **FIXED in v2.1 Phase 16** (BlockchainExplorerPage.ts created)
5. Acceptance report factual error — **NEEDS CORRECTION**

**Action:**
- [ ] Update `12-VERIFICATION.md` with v2.1 fixes
- [ ] Add cross-reference to v2.1 Phase 16/17
- [ ] Update status from `gaps_found` to `resolved_in_v2.1`

---

## Phase 4: Backlog Triage (Session 2-3)

### 4.1 Backlog Items Review

| Item | Current Status | Recommendation |
|------|---------------|----------------|
| E2E smoke route 失败修复 | backlog LOW | Close as "won't fix" or move to v3.0 |
| Prophet Windows CmdStan | backlog LOW | Close as "environment issue, not code" |
| 拍卖订单分页化 (PERF-04) | backlog LOW | Evaluate if still needed; close or move to v3.0 |

**Action:**
- [ ] Review each item with user
- [ ] Update PROJECT.md Deferred Items
- [ ] Close or reassign

---

## Success Criteria

### Phase 1 Complete When:
- [ ] `git status` shows clean working tree
- [ ] All 13 files committed with meaningful message

### Phase 2 Complete When:
- [ ] Phase 05: 7/7 SUMMARY.md files exist
- [ ] Phase 14: 2/2 SUMMARY.md files exist
- [ ] Phase 09: Status verified and SUMMARYs written (or status corrected)

### Phase 3 Complete When:
- [ ] Phase 12 VERIFICATION.md updated with v2.1 fixes
- [ ] Status changed to reflect resolution

### Phase 4 Complete When:
- [ ] All 3 backlog items triaged (closed or assigned)
- [ ] PROJECT.md Deferred Items updated

---

## Rollback Plan

If any SUMMARY cannot be reconstructed:
1. Mark the plan as "execution status unknown" in the SUMMARY
2. Create a placeholder SUMMARY with minimal frontmatter
3. Add to PROJECT.md as "documentation debt"
4. Do NOT block other phases

---

## Timeline

| Phase | Estimated Time | Dependencies |
|-------|---------------|-------------|
| 1.1 Commit changes | 30 min | None |
| 2.1 Phase 05 SUMMARYs | 60-90 min | Phase 1.1 |
| 2.2 Phase 14 SUMMARY | 30 min | Phase 1.1 |
| 2.3 Phase 09 SUMMARYs | 45 min | Phase 1.1 |
| 3.1 Phase 12 Update | 30 min | Phase 2.x |
| 4.1 Backlog triage | 30 min | Phase 3.1 |
| **Total** | **~4 hours** | Sequential |

---

*Plan created: 2026-06-01*
*Next step: Execute Phase 1.1 (Commit uncommitted changes)*
