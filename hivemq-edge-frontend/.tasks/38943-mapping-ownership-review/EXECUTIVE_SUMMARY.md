# Executive Summary

## Task Overview

**Task ID:** 38943-mapping-ownership-review
**Type:** Technical debt / Architecture improvement
**Priority:** High
**Estimated Effort:** 16 hours (3 weeks)

## Problem Statement

Task 38936 partially fixed ownership tracking in the mapping system by adding scope to `sources.primary` and `instructions[].sourceRef`. However, **critical gaps remain** in `sources.tags[]` and `sources.topicFilters[]` arrays, which store only string values without adapter ownership information.

### The Core Issue

```
User selects "tag1" from Adapter A and "tag3" from Adapter B
    ↓
System stores: ["tag1", "tag3"]  ❌ Which adapter owns which tag?
    ↓
Cannot validate ownership, detect conflicts, or ensure data integrity
```

## Impact

| Area                | Current Impact                                        | Severity  |
| ------------------- | ----------------------------------------------------- | --------- |
| **Data Integrity**  | Cannot verify tag belongs to correct adapter          | 🔴 High   |
| **Validation**      | False positives if multiple adapters have same tag ID | 🔴 High   |
| **User Experience** | Ambiguous error messages                              | 🟡 Medium |
| **Maintainability** | Ownership logic scattered across 5+ components        | 🔴 High   |
| **Architecture**    | Inconsistent patterns (primary has scope, tags don't) | 🟡 Medium |

## Root Cause

**Information loss in UI component:** `CombinedEntitySelect.handleOnChange` receives full ownership context but extracts only the tag ID, discarding the adapter information.

```typescript
// Current (broken)
const handleOnChange = (value) => {
  onChange?.(value.map((v) => v.id)) // ❌ Loses adapterId
}
```

## Analysis Highlights

### What We Discovered

1. **Scattered Logic:** Ownership handling distributed across Query, Display, Storage, Validation, and Instruction layers
2. **Dual Representation:** Same data stored in two formats (strings vs. references) creating sync risk
3. **Index-Based Pairing:** Fragile association between queries and entities based on array position
4. **Incomplete Validation:** Cannot verify tags belong to correct adapter
5. **Type System Gap:** `DataIdentifierReference` exists but not used consistently

### Key Finding

**The instruction layer already does this correctly.** `instructions[].sourceRef` stores full ownership with scope. We need to apply the same pattern to `sources.tags[]` and `sources.topicFilters[]`.

## Recommended Solution: Option A

**Upgrade arrays from `string[]` to `DataIdentifierReference[]`**

### What Changes

```typescript
// Before
sources: {
  tags: ['tag1', 'tag3'] // ❌ No ownership
}

// After
sources: {
  tags: [
    { id: 'tag1', type: 'tag', scope: 'adapter1' }, // ✅ Full ownership
    { id: 'tag3', type: 'tag', scope: 'adapter2' },
  ]
}
```

### Why This Solution

| Criterion               | Assessment                                                                   |
| ----------------------- | ---------------------------------------------------------------------------- |
| **Clean Architecture**  | ✅ Consistent with existing `sources.primary` and `instructions[].sourceRef` |
| **Type Safety**         | ✅ Compiler enforces ownership preservation                                  |
| **Effort**              | ✅ 16 hours (proportionate to benefit)                                       |
| **Backend Changes**     | ✅ None required (frontend-only)                                             |
| **Backward Compatible** | ✅ Migration handles old data (sets scope to 'unknown')                      |
| **Maintainability**     | ✅ Single domain model throughout all layers                                 |

## Alternative Solutions Considered

| Option                        | Effort | Clean? | Viable? | Notes                                          |
| ----------------------------- | ------ | ------ | ------- | ---------------------------------------------- |
| **B: Remove arrays**          | 37h    | ⚠️     | ❌      | Cannot migrate existing data                   |
| **C: Display-only arrays**    | 15h    | ⚠️     | ⚠️      | Maintains duplication, sync complexity         |
| **D: Parallel arrays**        | 6-13h  | ❌     | ⚠️      | Increases technical debt                       |
| **E: Runtime reconstruction** | 14h    | ❌     | ❌      | Workaround, violates clean solution constraint |

**Option A is the only solution that meets all constraints.**

## Implementation Plan

### Timeline: 3 Weeks

**Week 1: Type & Core Changes (6 hours)**

- Regenerate TypeScript models from OpenAPI schema
- Update `DataCombining` type definition
- Add `migrateSources` utility for backward compatibility
- Update `CombinedEntitySelect.handleOnChange` to preserve scope

**Week 2: Component & Validation Updates (6 hours)**

- Update `DataCombiningEditorField.handleSourcesUpdate`
- Update `useValidateCombiner.validateTags` with ownership validation
- Update `DataCombiningTableField` to display adapter info
- Add comprehensive tests for migration logic

**Week 3: Testing & Documentation (4 hours)**

- Integration testing with old and new data formats
- Regression testing
- Update component documentation
- Code review and merge

### Files to Change

| Component                      | Change                  | Complexity |
| ------------------------------ | ----------------------- | ---------- |
| `DataCombining.ts`             | Update type definition  | Low        |
| `CombinedEntitySelect.tsx`     | Preserve full reference | Low        |
| `DataCombiningEditorField.tsx` | Accept new type         | Low        |
| `useValidateCombiner.ts`       | Validate with ownership | Medium     |
| `DataCombiningTableField.tsx`  | Display adapter info    | Low        |
| `migrateSources.ts` (new)      | Handle old format       | Medium     |

**Total:** 6 files, mostly low complexity changes

## Risk Assessment

| Risk                           | Likelihood | Impact | Mitigation                                  |
| ------------------------------ | ---------- | ------ | ------------------------------------------- |
| Migration issues for old data  | Medium     | Low    | Accept 'unknown' scope, document limitation |
| Type errors during development | Low        | Low    | TypeScript catches at compile time          |
| Display/UI regressions         | Low        | Medium | Comprehensive testing with both formats     |
| Performance impact             | Very Low   | Low    | No additional queries, simple type change   |

**Overall Risk Level:** 🟢 **Low**

## Success Criteria

- ✅ All tags and topicFilters have adapter ownership
- ✅ Validation verifies correct adapter for each tag
- ✅ Type system enforces ownership preservation
- ✅ Existing mappings load without errors
- ✅ No backend/API changes required
- ✅ Code follows consistent pattern with other references

## Business Value

### Short-Term Benefits

- 🎯 **Data Integrity:** Guaranteed ownership tracking prevents configuration errors
- 🎯 **Better Validation:** Accurate error messages help users fix issues faster
- 🎯 **Type Safety:** Compiler prevents ownership-related bugs

### Long-Term Benefits

- 🎯 **Maintainability:** Single pattern reduces code complexity
- 🎯 **Extensibility:** Clean architecture makes future enhancements easier
- 🎯 **Technical Debt Reduction:** Fixes architectural inconsistency

### Avoided Costs

- ❌ Debugging ambiguous ownership issues
- ❌ Supporting multiple inconsistent patterns
- ❌ Migrating away from fragile index-based pairing later (more expensive)

## Cost-Benefit Analysis

| Category                | Value                                                 |
| ----------------------- | ----------------------------------------------------- |
| **Development Cost**    | 16 hours (~2 days)                                    |
| **Testing Cost**        | Included in 16 hours                                  |
| **Maintenance Savings** | ~8 hours/year (reduced debugging, clearer code)       |
| **Risk Reduction**      | High (prevents data integrity issues)                 |
| **ROI**                 | Payback in ~2 years, architectural benefits immediate |

**Recommendation:** ✅ **Proceed with implementation**

## Alternatives to "Do Nothing"

If we don't fix this:

1. **Ownership ambiguity persists** - Cannot verify correct adapter
2. **Validation remains incomplete** - False positives possible
3. **Technical debt grows** - More code depends on broken pattern
4. **Future fixes more expensive** - More components to update later
5. **Architecture inconsistency** - Primary has scope, tags don't

**Cost of inaction:** Higher long-term maintenance burden and risk

## Next Steps

### Immediate Actions

1. **Review this analysis** with technical leads and stakeholders
2. **Approve Option A approach** (or select alternative if concerns exist)
3. **Schedule 3-week implementation window**
4. **Assign developer(s)** to execute implementation plan

### Decision Points

- [ ] **Approve solution approach** (Option A recommended)
- [ ] **Accept migration strategy** (old data gets 'unknown' scope)
- [ ] **Confirm timeline** (3 weeks acceptable?)
- [ ] **Allocate resources** (1 developer, 16 hours)

### Supporting Documents

For detailed technical analysis, see:

- **[ANALYSIS.md](./ANALYSIS.md)** - Complete problem analysis
- **[SEPARATION_OF_CONCERNS.md](./SEPARATION_OF_CONCERNS.md)** - Architectural implications
- **[SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md)** - All 5 options compared
- **[VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md)** - Before/after comparison
- **[INDEX.md](./INDEX.md)** - Document navigation

## Recommendation

**✅ APPROVE Option A: Upgrade Arrays to DataIdentifierReference[]**

This solution:

- ✅ Meets all constraints (no backend changes, backward compatible, proportionate effort, clean)
- ✅ Fixes root cause (information loss in UI layer)
- ✅ Establishes consistent architecture (same pattern as instructions)
- ✅ Has acceptable risk profile (low risk, 16 hours effort)
- ✅ Provides long-term maintainability benefits

---

**Prepared by:** Automated Analysis
**Date:** 2026-02-10
**Task:** 38943-mapping-ownership-review
**Status:** Analysis complete, awaiting approval
