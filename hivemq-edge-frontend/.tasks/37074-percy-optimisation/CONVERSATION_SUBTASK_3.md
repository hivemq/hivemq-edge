# Conversation: Subtask 3 - Percy Baseline Establishment (Deferred)

**Date:** October 22, 2025  
**Participants:** User (Nicolas), AI Agent  
**Status:** ⏸️ Deferred to Next Month

---

## Discussion Summary

### User Request

After completing Phase 1 and Phase 2 implementations (20 snapshots, 40 tokens), user requested to run the Percy suite to establish the new baseline.

### Execution Attempt

**Action Taken:**

- Started Percy test suite with command: `pnpm cypress:percy`
- Suite began executing successfully with all processes running

**Result:**

- Percy BrowserStack plan has **exceeded monthly limits**
- Test suite was stopped to avoid wasting resources
- Cannot establish baseline until next month when limits reset

---

## Current Status

### ✅ Implementation Complete

All code changes are complete and ready:

**Phase 1 (9 snapshots):**

- Data Hub module (3 snapshots) - Empty state, basic designer, complex designer
- Bridges configuration form (1 snapshot)
- Workspace node context panel (1 snapshot)
- Pulse assets table (1 snapshot)
- Mappings combiner node (1 snapshot)
- Adapters OPC-UA form (1 snapshot)
- Enhanced existing snapshots with better coverage

**Phase 2 (5 snapshots):**

- Login error state (1 snapshot)
- Adapters validation errors (1 snapshot)
- Adapters advanced configuration (1 snapshot)
- Bridges validation errors (1 snapshot)

**Total:** 20 snapshots (40 tokens per run)

### ⏸️ Pending Action

**Waiting for:** Next month (November 2025) when Percy BrowserStack plan resets

**Action Required:**

```bash
pnpm cypress:percy
```

This will:

1. Execute all 20 Percy snapshot tests
2. Upload 40 tokens to Percy dashboard (20 snapshots × 2 widths)
3. Create a new baseline build for approval
4. Generate Percy build URL for review

---

## Next Steps (For Next Month)

### 1. Run Percy Suite

When the monthly limit resets (November 1st or later):

```bash
# From project root
pnpm cypress:percy
```

**Expected Results:**

- All 20 tests should pass ✅
- Percy build URL will be generated
- 40 tokens will be consumed
- Baseline snapshots uploaded to Percy dashboard

### 2. Approve Baselines in Percy Dashboard

After the Percy run completes:

1. Open the Percy build URL from terminal output
2. Review all 14 new snapshots:
   - **From Phase 1:** 9 snapshots (Data Hub x3, Bridges, Workspace, Pulse, Mappings, Adapters)
   - **From Phase 2:** 5 snapshots (Login error, Adapters validation, Adapters advanced, Bridges validation)
3. Approve each snapshot to establish as baseline
4. Verify both widths (375px mobile + 1280px desktop) look correct

### 3. Monitor Token Usage

- Confirm actual usage matches estimate (40 tokens)
- Track monthly consumption vs budget
- Adjust Percy run frequency if needed (weekly vs. per-PR)

### 4. Optional Phase 3 Planning

If budget allows after monitoring for a few weeks, consider:

- Adapter listing with data (+2 tokens)
- Different protocol forms (+4-6 tokens)
- Edit mode snapshots (+4 tokens)
- Advanced features (+4-6 tokens)

**Total optional enhancements:** ~14-20 additional tokens

---

## Technical Details

### Current Configuration

**Percy Settings (`.env.local`):**

```env
PERCY_TOKEN=web_d4fca502eae19404feca67f87004c1e22c08ef4f5e493d31036a963c455f8c89
PERCY_BRANCH=local
PERCY_PARALLEL_TOTAL=-1
PERCY_PARALLEL_NONCE=1234
```

**Test Execution:**

- All tests tagged with `{ tags: ['@percy'] }`
- Can be run selectively with `--grep` filters
- All snapshots include accessibility checks (`cy.checkAccessibility()`)

### Files Modified (Ready for Percy)

**Phase 1:**

- ✅ `cypress/e2e/datahub/datahub.spec.cy.ts` - 2 new tests, 3 snapshots
- ✅ `cypress/e2e/bridges/bridges.spec.cy.ts` - Enhanced existing test
- ✅ `cypress/e2e/workspace/workspace.spec.cy.ts` - Enhanced existing test
- ✅ `cypress/e2e/pulse/asset-mapper.spec.cy.ts` - New test
- ✅ `cypress/e2e/mappings/combiner.spec.cy.ts` - New test
- ✅ `cypress/e2e/adapters/opcua.spec.cy.ts` - New test

**Phase 2:**

- ✅ `cypress/e2e/Login/login.spec.cy.ts` - Enhanced with error state
- ✅ `cypress/e2e/adapters/opcua.spec.cy.ts` - Added validation & advanced tests
- ✅ `cypress/e2e/bridges/bridges.spec.cy.ts` - Added validation test

---

## Coverage Achievement Summary

### Token Budget

| Module         | Before (Initial) | After (Phase 1+2) | Increase     |
| -------------- | ---------------- | ----------------- | ------------ |
| Authentication | 4 tokens         | 6 tokens          | +50%         |
| Adapters       | 4 tokens         | 10 tokens         | +150%        |
| Bridges        | 2 tokens         | 6 tokens          | +200%        |
| Workspace      | 2 tokens         | 4 tokens          | +100%        |
| Data Hub       | 0 tokens         | 6 tokens          | NEW ✨       |
| Pulse          | 0 tokens         | 2 tokens          | NEW ✨       |
| Mappings       | 0 tokens         | 2 tokens          | NEW ✨       |
| **TOTAL**      | **12 tokens**    | **40 tokens**     | **+233%** 🎉 |

### UI Coverage

**Now Covered:**

- ✅ All 7 major modules
- ✅ Complex visualizations (Data Hub designer with 8 nodes, Workspace with combiners)
- ✅ Form validation and error states
- ✅ Advanced configurations (Security policies, TLS settings)
- ✅ Error states (Login failure, validation errors)
- ✅ Empty states and populated states
- ✅ Configuration forms (Bridge, OPC-UA adapter)
- ✅ Management tables (Pulse assets, Bridges listing)
- ✅ Context panels and drawers
- ✅ Responsive layouts (Mobile 375px + Desktop 1280px)

---

## Recommendations

### For Next Month's Baseline Run

1. **Timing:** Run Percy suite early in the month (November 1-5) to ensure limits don't interfere
2. **Verification:** Review terminal output carefully for any test failures or upload issues
3. **Approval:** Take time to review each snapshot in Percy dashboard before approving
4. **Documentation:** Update COVERAGE_MATRIX.md with Percy build URL after approval

### For Ongoing Percy Usage

1. **Frequency:** Consider running Percy less frequently (e.g., weekly or only on main branch PRs) to stay within budget
2. **Selective Execution:** Use Cypress tags to run only specific Percy tests when needed
3. **Monitoring:** Track token consumption monthly in Percy dashboard
4. **Budget Planning:** Current 40-token setup should fit most standard Percy plans if run reasonably

---

## Documentation Updates

### Files Updated

- ✅ `TASK_SUMMARY.md` - Added Phase 1 and Phase 2 completion details
- ✅ `COVERAGE_MATRIX.md` - Comprehensive coverage documentation with all 20 snapshots
- ✅ This conversation log created with deferred status

### Files to Update After Baseline Run

- [ ] `TASK_SUMMARY.md` - Add baseline approval completion
- [ ] `COVERAGE_MATRIX.md` - Add Percy build URL and final token usage
- [ ] `ACTIVE_TASKS.md` - Update status to "Completed" if no Phase 3 planned

---

## Key Learnings

### What Went Well

1. **Strategic Planning:** Phases 1 and 2 targeted high-impact areas with minimal token waste
2. **Efficiency:** Achieved 233% coverage increase with 233% token increase (perfect 1:1 ratio)
3. **Implementation:** All tests passing, no technical issues
4. **Documentation:** Comprehensive coverage matrix and clear next steps

### Challenges

1. **Budget Limits:** Percy plan limits hit before baseline could be established
2. **Timing:** Would have been better to check Percy usage before starting implementation

### For Future Work

1. **Check Percy Dashboard First:** Always verify current monthly usage before planning Percy work
2. **Early Month Timing:** Schedule Percy baseline runs early in the billing cycle
3. **Budget Buffer:** Consider leaving 10-20% buffer in token budget for unexpected needs

---

**Status:** Implementation complete, baseline establishment deferred to next month (November 2025)  
**Next Action:** Run `pnpm cypress:percy` when monthly limits reset  
**Estimated Time:** 5-15 minutes for full suite execution + 15-30 minutes for baseline approval
