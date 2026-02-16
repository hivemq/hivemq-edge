# Quick Reference Guide

## 🎯 Start Here

1. **New to this task?** → Read [INDEX.md](./INDEX.md)
2. **Want to understand the problem?** → Read [ANALYSIS.md](./ANALYSIS.md)
3. **Need to see the fix?** → Read [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md)
4. **Ready to implement?** → Read [SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md)

## 📋 Document Overview

| Document                                           | Purpose                 | Read Time | Priority       |
| -------------------------------------------------- | ----------------------- | --------- | -------------- |
| [INDEX.md](./INDEX.md)                             | Navigation hub          | 5 min     | 🔴 Essential   |
| [TASK_BRIEF.md](./TASK_BRIEF.md)                   | Original requirements   | 2 min     | 🔴 Essential   |
| [ANALYSIS.md](./ANALYSIS.md)                       | Problem analysis        | 15 min    | 🔴 Essential   |
| [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md)           | Before/after comparison | 10 min    | 🟡 Recommended |
| [SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md)       | 5 solution options      | 20 min    | 🟡 Recommended |
| [ARCHITECTURE_REVIEW.md](./ARCHITECTURE_REVIEW.md) | Technical deep dive     | 25 min    | 🟢 Reference   |
| [UX_FLOW_ANALYSIS.md](./UX_FLOW_ANALYSIS.md)       | React lifecycle         | 20 min    | 🟢 Reference   |
| [QUICK_REFERENCE.md](./QUICK_REFERENCE.md)         | This document           | 3 min     | 🟢 Optional    |

## 🔍 Find Information Fast

### Problem Understanding

- **What's broken?** → [ANALYSIS.md - Problem Statement](./ANALYSIS.md#problem-statement)
- **Why is it broken?** → [ANALYSIS.md - Root Causes](./ANALYSIS.md#root-causes)
- **Where does it break?** → [UX_FLOW_ANALYSIS.md - Phase 2](./UX_FLOW_ANALYSIS.md#phase-2-user-interaction--selection)
- **How severe is it?** → [ANALYSIS.md - Key Findings Summary](./ANALYSIS.md#key-findings-summary)

### Solution Information

- **Recommended fix** → [SOLUTION_OPTIONS.md - Option A](./SOLUTION_OPTIONS.md#option-a-upgrade-arrays-to-dataidentifierreference)
- **Why this fix?** → [SOLUTION_OPTIONS.md - Recommendations](./SOLUTION_OPTIONS.md#recommendations)
- **How to implement?** → [SOLUTION_OPTIONS.md - Implementation Roadmap](./SOLUTION_OPTIONS.md#implementation-roadmap-option-a)
- **Visual comparison** → [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md)

### Technical Details

- **Type definitions** → [ARCHITECTURE_REVIEW.md - Type System](./ARCHITECTURE_REVIEW.md#type-system-overview)
- **Component hierarchy** → [ARCHITECTURE_REVIEW.md - Component Architecture](./ARCHITECTURE_REVIEW.md#component-architecture)
- **File locations** → [ARCHITECTURE_REVIEW.md - File Structure](./ARCHITECTURE_REVIEW.md#file-structure--responsibilities)
- **React lifecycle** → [UX_FLOW_ANALYSIS.md - Detailed React Lifecycle](./UX_FLOW_ANALYSIS.md#detailed-react-lifecycle)

## 🗂️ Critical Files

### Files with Issues (Need Changes)

| File                         | Path                                     | Lines   | Issue                        | Severity |
| ---------------------------- | ---------------------------------------- | ------- | ---------------------------- | -------- |
| DataCombining.ts             | `src/api/__generated__/models/`          | 1-47    | tags/topicFilters as strings | 🔴       |
| CombinedEntitySelect.tsx     | `src/modules/Mappings/components/forms/` | 45-82   | Information loss             | 🔴       |
| DataCombiningEditorField.tsx | `src/modules/Mappings/components/forms/` | 96-136  | Stores strings               | 🔴       |
| combining.utils.ts           | `src/modules/Mappings/utils/`            | 57      | Wrong index                  | 🔴       |
| useValidateCombiner.ts       | `src/modules/Mappings/hooks/`            | 149-175 | Can't validate ownership     | 🟡       |

### Files Working Correctly (Reference Examples)

| File                        | Path                                     | Lines     | Feature                   | Status |
| --------------------------- | ---------------------------------------- | --------- | ------------------------- | ------ |
| useDomainModel.ts           | `src/modules/Mappings/hooks/`            | 8-11      | Has adapterId             | ✅     |
| DestinationSchemaLoader.tsx | `src/modules/Mappings/components/forms/` | 87-103    | Auto-generates with scope | ✅     |
| MappingInstruction.tsx      | `src/modules/Mappings/components/forms/` | 49, 85-92 | Uses sourceRef            | ✅     |

## 🎨 Visual Aids

### Key Diagrams

| Diagram             | Location                                                                          | Shows                         |
| ------------------- | --------------------------------------------------------------------------------- | ----------------------------- |
| Information Loss    | [ANALYSIS.md](./ANALYSIS.md#2-information-loss-in-ui-flow)                        | Where adapter ID is lost      |
| Data Flow           | [ANALYSIS.md](./ANALYSIS.md#current-state-flow)                                   | How data flows through system |
| Component Hierarchy | [ARCHITECTURE_REVIEW.md](./ARCHITECTURE_REVIEW.md#component-hierarchy)            | Component relationships       |
| React Lifecycle     | [UX_FLOW_ANALYSIS.md](./UX_FLOW_ANALYSIS.md#phase-1-initialization--data-loading) | User journey phases           |
| Before/After        | [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md#component-changes)                        | Proposed fix comparison       |
| Migration Strategy  | [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md#migration-strategy)                       | Backward compatibility        |

### Mermaid Diagram Index

All diagrams use mermaid syntax and cover:

- Sequence diagrams (data flow between components)
- Flowcharts (logic flow and decisions)
- Class diagrams (type relationships)
- Journey maps (user experience)
- Graph diagrams (dependencies and pairing)

## 🚀 Implementation Checklist

Based on **Option A** (recommended solution):

### Week 1: Type & Core Changes

- [ ] Regenerate TypeScript models from OpenAPI
- [ ] Update `DataCombining` type definition
- [ ] Add `migrateSources` utility function
- [ ] Update `CombinedEntitySelect.handleOnChange`

### Week 2: Component & Validation Updates

- [ ] Update `DataCombiningEditorField.handleSourcesUpdate`
- [ ] Update `useValidateCombiner.validateTags`
- [ ] Update `DataCombiningTableField` display
- [ ] Add tests for migration logic

### Week 3: Testing & Documentation

- [ ] Test with old data format (strings)
- [ ] Test with new data format (DataIdentifierReference)
- [ ] Integration testing
- [ ] Update documentation
- [ ] Code review and merge

## 📊 Decision Matrix

Quick comparison of solution options:

| Option                    | Effort | Clean?              | Backward Compatible? | Recommended?   |
| ------------------------- | ------ | ------------------- | -------------------- | -------------- |
| A: Upgrade Arrays         | 16h    | ✅ Yes              | ⚠️ With migration    | ⭐ **YES**     |
| B: Remove Arrays          | 37h    | ⚠️ Reconstruction   | ❌ Breaks            | ❌ No          |
| C: Display-Only           | 15h    | ⚠️ Duplication      | ✅ Yes               | ⚠️ Alternative |
| D: Parallel Arrays        | 6-13h  | ❌ More duplication | ✅ Yes               | ⚠️ Alternative |
| E: Runtime Reconstruction | 14h    | ❌ Workaround       | ✅ Yes               | ❌ No          |

## 🔑 Key Concepts

### DataIdentifierReference

A type that identifies a data source with full ownership context:

```typescript
{
  id: string // e.g., "tag1" or "adapter1"
  type: string // e.g., "tag", "adapter", "metric"
  scope: string // e.g., "adapter1" (ownership)
}
```

### DomainModel

Frontend extension that adds `adapterId`:

```typescript
{
  ...DataIdentifierReference,
  adapterId: string;  // Explicit adapter ownership
  node?: T;           // Optional additional data
}
```

### Information Loss

The critical point where `CombinedEntitySelect.handleOnChange` extracts only the `id` field from `DomainModel`, discarding `adapterId`.

### Index-Based Pairing

Fragile association between query results and entity arrays based on array position rather than explicit relationships.

## 🎓 Learning Path

### For Developers New to Codebase

1. Read [TASK_BRIEF.md](./TASK_BRIEF.md) - Understand what needs fixing
2. Read [UX_FLOW_ANALYSIS.md](./UX_FLOW_ANALYSIS.md) - See how it works today
3. Read [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md) - Understand the fix
4. Review code files with issues (use file:line references)

### For Technical Leads/Architects

1. Read [INDEX.md](./INDEX.md) - Overview
2. Read [ANALYSIS.md](./ANALYSIS.md) - Problem assessment
3. Read [SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md) - Compare options
4. Review [ARCHITECTURE_REVIEW.md](./ARCHITECTURE_REVIEW.md) if needed

### For Project Managers/Stakeholders

1. Read [INDEX.md - Quick Summary](./INDEX.md#quick-summary) - Executive summary
2. Read [VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md) - Visual comparison
3. Review [SOLUTION_OPTIONS.md - Comparison Matrix](./SOLUTION_OPTIONS.md#comparison-matrix)
4. Check [Implementation Impact](./VISUAL_SUMMARY.md#implementation-impact)

## 💡 Common Questions

### Q: Why is this important?

**A:** Without ownership tracking, we can't validate which adapter a tag belongs to, leading to:

- Ambiguous validation errors
- Potential cross-adapter conflicts
- Fragile mapping configurations

### Q: Can we fix this without breaking existing mappings?

**A:** Yes, with migration logic that converts old string arrays to new format, setting `scope: 'unknown'` for unmigrated data.

### Q: Why not remove the arrays entirely?

**A:** Cannot migrate existing data without destination path information. See [SOLUTION_OPTIONS.md - Option B](./SOLUTION_OPTIONS.md#option-b-remove-arrays-use-only-instructions).

### Q: How long will this take?

**A:** Recommended solution (Option A): **16 hours** over 3 weeks.

### Q: Will this require backend changes?

**A:** No, this is **frontend-only**. Backend already has scope field in schema.

### Q: What's the risk level?

**A:** Low-Medium. Type safety helps prevent errors, and migration handles old data gracefully.

## 📞 Next Steps

1. **Review analysis** - All stakeholders read [INDEX.md](./INDEX.md) and [ANALYSIS.md](./ANALYSIS.md)
2. **Choose solution** - Decide between Option A (recommended) or Option C (alternative)
3. **Plan implementation** - Follow roadmap in [SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md)
4. **Create tickets** - Break down work into implementation tasks
5. **Begin work** - Start with Week 1 tasks

## 📚 Related Resources

- **Task 38936:** Previous work that added scope to `sources.primary`
- **Commit c53bb0594:** OpenAPI schema update with scope field
- **OpenAPI Schema:** Backend source of truth for type definitions

---

**Created:** 2026-02-10
**Task:** 38943-mapping-ownership-review
**Status:** Analysis complete, ready for implementation decision
