# Task 38943: Mapping Ownership Review

Complete analysis of ownership tracking gaps in the HiveMQ Edge frontend mapping system.

## 📄 Quick Navigation

### 🚀 Start Here

- **[EXECUTIVE_SUMMARY.md](./EXECUTIVE_SUMMARY.md)** - For stakeholders and decision makers (5 min read)
- **[DECISION_TREE.md](./DECISION_TREE.md)** - Interactive guide to choose the right solution ⭐
- **[INDEX.md](./INDEX.md)** - Complete document navigation and overview

### 🔍 Problem Analysis

- **[TASK_BRIEF.md](./TASK_BRIEF.md)** - Original requirements
- **[ANALYSIS.md](./ANALYSIS.md)** - What's broken and why
- **[SEPARATION_OF_CONCERNS.md](./SEPARATION_OF_CONCERNS.md)** - Why distributed logic is problematic ⭐

### 🏗️ Technical Details

- **[ARCHITECTURE_REVIEW.md](./ARCHITECTURE_REVIEW.md)** - Code structure and file locations
- **[UX_FLOW_ANALYSIS.md](./UX_FLOW_ANALYSIS.md)** - React lifecycle and user journey

### 💡 Solutions

- **[SOLUTION_OPTIONS.md](./SOLUTION_OPTIONS.md)** - 8 options with detailed comparison
- **[OPTION_H_CURRENT_IMPLEMENTATION.md](./OPTION_H_CURRENT_IMPLEMENTATION.md)** - Current implementation ⭐ **IMPLEMENTED**
- **[OPTION_F_SMART_RECONSTRUCTION.md](./OPTION_F_SMART_RECONSTRUCTION.md)** - Advanced reconstruction algorithm
- **[OPTION_G_FORM_LAYER_ENRICHMENT.md](./OPTION_G_FORM_LAYER_ENRICHMENT.md)** - Form layer transformation
- **[VISUAL_SUMMARY.md](./VISUAL_SUMMARY.md)** - Before/after visual comparison
- **[DECISION_TREE.md](./DECISION_TREE.md)** - Decision making guide ⭐
- **[MIGRATION_GUIDE_G_TO_A.md](./MIGRATION_GUIDE_G_TO_A.md)** - G→A migration steps

### 📚 Reference

- **[QUICK_REFERENCE.md](./QUICK_REFERENCE.md)** - Fast lookup guide

## 🎯 The Problem

```
sources.tags[] stores only strings: ["tag1", "tag3"]
                                      ↓
                         ❌ Which adapter owns each tag?
                                      ↓
               Cannot validate, detect conflicts, or ensure integrity
```

## ✅ Current Solution (Implemented)

**Option H: Frontend Context Storage with EntityQuery**

- **Status:** ✅ Implemented in branch `refactor/38943-mapping-ownership-review`
- **Effort:** 18 hours
- **Risk:** Low
- **Backend Changes:** Deprecation only (removed in future API version)
- **Backward Compatible:** Perfect (dual path support)
- **On-Premises Viable:** Yes ✅

**Key Features:**

- Eliminates index-based pairing (EntityQuery type)
- Full frontend type safety (DataIdentifierReference[])
- Per-mapping state isolation
- Migration path to Option A

---

## ⭐ Ideal Long-Term Solution

**Option A: Upgrade arrays from `string[]` to `DataIdentifierReference[]`**

- **Status:** ⏰ Blocked by on-premises customer migration cost
- **Effort:** 16 hours
- **Risk:** Low
- **Backend Changes:** API schema change
- **When Viable:** After on-premises constraint lifts
- **Migration from H:** Additional 12-16 hours

## 📊 Document Stats

| Document                           | Pages | Diagrams | Priority                       |
| ---------------------------------- | ----- | -------- | ------------------------------ |
| EXECUTIVE_SUMMARY.md               | 4     | 1        | 🔴 Essential (Stakeholders)    |
| DECISION_TREE.md                   | 8     | 5        | 🔴 Essential (Decision Making) |
| INDEX.md                           | 8     | 2        | 🔴 Essential (Navigation)      |
| ANALYSIS.md                        | 5     | 3        | 🔴 Essential (Technical)       |
| SEPARATION_OF_CONCERNS.md          | 9     | 12       | 🔴 Essential (Architecture)    |
| COMBINER_COMPONENTS_INVENTORY.md   | 22    | 2        | 🔴 Essential (Developers)      |
| COMPLETE_DATA_FLOW.md              | 16    | 10       | 🔴 Essential (Developers)      |
| OPTION_H_CURRENT_IMPLEMENTATION.md | 18    | 2        | 🔴 Essential (Current)         |
| SOLUTION_OPTIONS.md                | 18    | 5        | 🟡 Recommended                 |
| OPTION_F_SMART_RECONSTRUCTION.md   | 12    | 4        | 🟢 Reference                   |
| OPTION_G_FORM_LAYER_ENRICHMENT.md  | 11    | 6        | 🟢 Reference                   |
| MIGRATION_GUIDE_G_TO_A.md          | 14    | 2        | 🟢 Reference (when needed)     |
| ARCHITECTURE_REVIEW.md             | 8     | 1        | 🟢 Reference                   |
| UX_FLOW_ANALYSIS.md                | 7     | 7        | 🟢 Reference                   |
| VISUAL_SUMMARY.md                  | 6     | 7        | 🟡 Recommended                 |
| QUICK_REFERENCE.md                 | 5     | 0        | 🟢 Optional                    |

**Total:** 167 pages, 67 mermaid diagrams, 16 documents

## 🎓 Reading Paths

### For Stakeholders/PMs (20 min)

1. EXECUTIVE_SUMMARY.md - Business case and recommendation
2. DECISION_TREE.md - Which option to choose
3. VISUAL_SUMMARY.md - Before/after comparison
4. INDEX.md - Quick Summary section

### For Technical Leads (60 min)

1. INDEX.md - Overview
2. ANALYSIS.md - Problem understanding
3. SEPARATION_OF_CONCERNS.md - Architectural implications ⭐
4. SOLUTION_OPTIONS.md - All 7 options comparison
5. DECISION_TREE.md - Decision making process
6. EXECUTIVE_SUMMARY.md - Final recommendation

### For Developers (150 min)

1. TASK_BRIEF.md - Requirements
2. COMBINER_COMPONENTS_INVENTORY.md - Complete component reference ⭐
3. COMPLETE_DATA_FLOW.md - Data transformation pipeline ⭐
4. UX_FLOW_ANALYSIS.md - How it works today
5. ARCHITECTURE_REVIEW.md - Code structure
6. SEPARATION_OF_CONCERNS.md - Design issues ⭐
7. SOLUTION_OPTIONS.md - All 7 options implementation details
8. OPTION_F_SMART_RECONSTRUCTION.md - Advanced reconstruction (if choosing F)
9. OPTION_G_FORM_LAYER_ENRICHMENT.md - Form layer strategy (if choosing G)
10. VISUAL_SUMMARY.md - Proposed changes
11. MIGRATION_GUIDE_G_TO_A.md - If implementing G→A transition

## 🔑 Key Findings

### What's Broken

- ❌ `sources.tags[]` - String array, no ownership
- ❌ `sources.topicFilters[]` - String array, no ownership
- ❌ Information loss at `CombinedEntitySelect.handleOnChange`
- ❌ Ownership logic scattered across 5+ layers
- ❌ Index-based pairing is fragile

### What Works

- ✅ `sources.primary` - Has DataIdentifierReference
- ✅ `instructions[].sourceRef` - Has scope field
- ✅ Auto-instruction generation - Preserves ownership
- ✅ Type system exists - Just not used consistently

### The Gap

**Inconsistent application of ownership pattern:** Instructions do it right, arrays don't.

## 📈 Recommendations

### ✅ **IMPLEMENTED: Option H** (Current Reality)

**Frontend Context Storage with EntityQuery**

**Status:** ✅ Implemented and working

**Why chosen:**

- ✅ On-premises customer viable (no API schema change)
- ✅ Eliminates index-based pairing fragility (EntityQuery)
- ✅ Full frontend type safety (DataIdentifierReference[])
- ✅ Perfect backward compatibility
- ✅ Clear migration path to Option A
- ✅ 18 hours effort (completed)

**Trade-offs accepted:**

- ⚠️ Reconstruction overhead (~50-100ms on load)
- ⚠️ Not API source of truth (frontend state)

---

### ⭐ **IDEAL: Option A** (Future Target)

**Upgrade Arrays to DataIdentifierReference[]**

**Status:** ⏰ Blocked by on-premises customer migration cost

**Why ideal:**

- ✅ Cleanest architecture (API source of truth)
- ✅ No reconstruction overhead (20-40x faster)
- ✅ Simpler code (no dual paths)
- ✅ 16 hours effort
- ❌ Requires API schema change (blocking)

**When viable:** After all customers on cloud/SaaS or coordinated migration

**Migration:** H → A (12-16 additional hours when constraint lifts)

---

### 📋 Decision Framework

See **[DECISION_TREE.md](./DECISION_TREE.md)** for detailed decision flowchart

### Other Options Analyzed

8 total options analyzed:

- **Option H:** ✅ Implemented (8.7/10)
- **Option A:** ⭐ Ideal but blocked (8.25/10 with constraint)
- Option C: Maintains duplication (7.65/10)
- Option G: Inferior to H - retains pairing (6.65/10)
- Option D, B, E, F: Lower scores (5.35-5.75/10)

## 📋 Implementation Checklist

### Week 1: Core Changes

- [ ] Regenerate TypeScript models
- [ ] Update DataCombining type
- [ ] Add migration utility
- [ ] Update CombinedEntitySelect

### Week 2: Components

- [ ] Update DataCombiningEditorField
- [ ] Update validation hooks
- [ ] Update display components
- [ ] Add migration tests

### Week 3: Testing

- [ ] Test with old data format
- [ ] Test with new data format
- [ ] Integration tests
- [ ] Documentation

## 🔗 Related Work

- **Task 38936** - Added scope to sources.primary and instructions[].sourceRef
- **Commit c53bb0594** - OpenAPI schema updated with scope field
- **Backend Schema** - Already has scope, frontend models need update

## 📞 Next Actions

1. **Review** EXECUTIVE_SUMMARY.md
2. **Discuss** SEPARATION_OF_CONCERNS.md findings
3. **Decide** on Option A (or alternative)
4. **Schedule** 3-week implementation
5. **Assign** developer(s)

## 💬 Questions?

Refer to:

- **Problem understanding** → ANALYSIS.md
- **Code locations** → ARCHITECTURE_REVIEW.md
- **Solution details** → SOLUTION_OPTIONS.md
- **Visual explanation** → VISUAL_SUMMARY.md
- **Business case** → EXECUTIVE_SUMMARY.md

---

**Created:** 2026-02-10
**Status:** Analysis complete, ready for decision
**Estimated Effort:** 16 hours (Option A)
**Risk Level:** Low
**Backend Changes:** None required
