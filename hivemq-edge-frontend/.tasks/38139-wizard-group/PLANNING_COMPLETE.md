# Task 38139: Wizard Group - Planning Complete

**Task ID:** 38139-wizard-group  
**Created:** November 21, 2025  
**Status:** ✅ Planning Complete - Ready for Implementation  
**Dependencies:** Task 38111 (Workspace Operation Wizard)

---

## What Was Created

### Core Planning Documents

1. **TASK_BRIEF.md** (4,500 words)

   - Requirements and constraints
   - Group-specific selection rules
   - Ghost node challenge explanation
   - React Flow group requirements
   - Acceptance criteria (6 major areas)
   - Reference implementation from existing code

2. **TASK_PLAN.md** (10,000+ words)

   - 8 detailed subtasks with implementation steps
   - 4-phase breakdown (Foundation, Ghost System, Configuration, Testing)
   - Code examples for all key functions
   - Risk management section
   - Timeline (2-3 weeks)
   - Success criteria and metrics

3. **QUICK_REFERENCE.md** (3,000 words)

   - Quick start guide for developers
   - Critical constraints summary
   - Architecture overview with diagrams
   - Key functions to implement
   - Common pitfalls and solutions
   - Testing strategy

4. **ACTIVE_TASKS.md** (Updated)
   - Added task 38139 to active tasks index
   - Links to all planning documents
   - Next action clearly stated

---

## Task Overview

### Objective

Implement the Group wizard as the final entity wizard in the Workspace Operation Wizard system, allowing users to create groups by selecting 2+ nodes through an interactive selection interface.

### Key Challenges

1. **Ghost Node Approach**: Groups are different from other entities

   - **Problem**: Groups contain other nodes (parent-child relationship)
   - **Solution**: Ghost group appears in Step 1 (preview), not Step 0 (selection)
   - **Reason**: Prevents showing same nodes twice

2. **Selection Constraints**: Complex rules for what can be grouped

   - Can select: ADAPTER, BRIDGE, CLUSTER nodes
   - Cannot select: Nodes already in groups
   - Cannot select: DEVICE, HOST (auto-included instead)

3. **React Flow Requirements**: Strict ordering for parent-child groups
   - Group node MUST be added before children in nodes array
   - Children must have `parentId` set to group ID
   - Positions must be relative to group origin

### Three-Step Workflow

```
Step 0: SELECTION
├── User selects 2+ nodes (adapters, bridges, groups)
├── DEVICE/HOST nodes shown as "auto-included"
└── Next when minimum nodes selected

Step 1: PREVIEW
├── Ghost group appears with selected nodes as children
├── Auto-included DEVICE/HOST visible in group
└── User reviews structure

Step 2: CONFIGURATION
├── Form: Group title (required)
├── Form: Color scheme selection
└── Complete creates real group
```

---

## Implementation Plan Breakdown

### Phase 1: Foundation & Constraints (3 days)

**Subtask 1: Group Selection Constraints** (1-2 days)

- Create `groupConstraints.ts` helper utilities
- Functions: `canNodeBeGrouped`, `getAutoIncludedNodes`, `isNodeInGroup`
- Update `WizardSelectionRestrictions` with group logic
- Tests for all constraint scenarios

**Subtask 2: Auto-Inclusion UI** (1 day)

- Create `AutoIncludedNodesList` component
- Show DEVICE/HOST that will be auto-included
- Visual distinction (blue background, plus icons)
- Update selection count display

### Phase 2: Ghost Group System (4 days)

**Subtask 3: Ghost Group Factory** (2 days)

- Create `createGhostGroup` function in `ghostNodeFactory.ts`
- Handle parent-child relationships
- Calculate bounds including auto-included nodes
- Ensure group node first in array (React Flow requirement)
- Tests for ghost group creation

**Subtask 4: Ghost Group Renderer** (2 days)

- Update `GhostNodeRenderer` for Step 1 behavior
- Hide real selected nodes when ghost appears
- Show ghost children with parentId set
- Handle cleanup on cancel/complete
- Tests for rendering logic

### Phase 3: Configuration & Completion (3 days)

**Subtask 5: Configuration Form** (1.5 days)

- Create `WizardGroupConfiguration` component
- Title input (required)
- Color scheme selection (5 options)
- Integrate into `WizardConfigurationPanel`
- Component tests

**Subtask 6: Wizard Completion** (2 days)

- Create `groupWizardCompletion.ts` utility
- Convert ghost group to real group
- Use existing `createGroup` utility
- Apply user configuration
- Update nodes with parentId relationships
- Tests for completion logic

### Phase 4: Testing & Polish (2-3 days)

**Subtask 7: E2E Testing** (1.5 days)

- Create `wizard-create-group.spec.cy.ts`
- Test complete workflow
- Test edge cases (cancel, already grouped)
- Accessibility tests

**Subtask 8: Documentation** (1 day)

- Task summary
- User documentation
- Technical design doc
- Final polish and code review

---

## Key Technical Decisions

### 1. Ghost Group Timing

**Decision**: Ghost group appears in Step 1 (preview), not Step 0 (selection)

**Rationale**:

- Prevents node duplication on screen
- Matches user expectation (select first, then see result)
- Consistent with other multi-step wizards

**Implementation**: `GhostNodeRenderer` has step-aware logic

### 2. Auto-Inclusion Strategy

**Decision**: DEVICE/HOST nodes auto-included but not directly selectable

**Rationale**:

- Improves UX (users don't need to understand device relationships)
- Prevents incomplete groups (adapter without device)
- Matches existing group creation behavior

**Implementation**: `getAutoIncludedNodes` helper function

### 3. Reuse Existing Group Logic

**Decision**: Reuse `createGroup` from `group.utils.ts`

**Rationale**:

- Minimizes code changes (task requirement)
- Maintains consistency with existing group creation
- Reduces testing surface area
- Proven implementation

**Implementation**: `completeGroupWizard` wraps `createGroup`

### 4. Parent-Child Ordering

**Decision**: Enforce group-first ordering in completion utility

**Rationale**:

- React Flow requirement (groups must precede children)
- Prevents layout bugs
- Centralized ordering logic

**Implementation**: `completeGroupWizard` returns `[groupNode, ...children]`

---

## Files to Create (6 new files)

1. `src/modules/Workspace/components/wizard/utils/groupConstraints.ts`

   - Selection constraint helpers
   - Auto-inclusion logic

2. `src/modules/Workspace/components/wizard/utils/groupConstraints.spec.ts`

   - Unit tests for constraints

3. `src/modules/Workspace/components/wizard/AutoIncludedNodesList.tsx`

   - UI component for auto-included nodes

4. `src/modules/Workspace/components/wizard/AutoIncludedNodesList.spec.cy.tsx`

   - Component tests

5. `src/modules/Workspace/components/wizard/WizardGroupConfiguration.tsx`

   - Group configuration form

6. `src/modules/Workspace/components/wizard/WizardGroupConfiguration.spec.cy.tsx`

   - Component tests

7. `src/modules/Workspace/components/wizard/utils/groupWizardCompletion.ts`

   - Completion utility

8. `src/modules/Workspace/components/wizard/utils/groupWizardCompletion.spec.ts`

   - Completion tests

9. `cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts`
   - E2E tests

---

## Files to Modify (8 existing files)

1. `src/modules/Workspace/components/wizard/WizardSelectionRestrictions.tsx`

   - Add group-specific constraint checks
   - Handle "already in group" validation

2. `src/modules/Workspace/components/wizard/WizardSelectionPanel.tsx`

   - Integrate AutoIncludedNodesList
   - Update selection count display

3. `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts`

   - Add `createGhostGroup` function
   - Add `removeGhostGroup` function

4. `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.spec.ts`

   - Add tests for ghost group functions

5. `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx`

   - Add Step 1 ghost group rendering logic
   - Handle node hiding/showing

6. `src/modules/Workspace/components/wizard/GhostNodeRenderer.spec.cy.tsx`

   - Add tests for ghost group rendering

7. `src/modules/Workspace/components/wizard/WizardConfigurationPanel.tsx`

   - Add route for GROUP entity configuration

8. `src/locales/en/translation.json`
   - Add group wizard i18n keys

---

## Success Metrics

| Metric             | Target              |
| ------------------ | ------------------- |
| **Duration**       | 2-3 weeks (13 days) |
| **New Files**      | 9                   |
| **Modified Files** | 8                   |
| **New Functions**  | ~10                 |
| **New Components** | 2                   |
| **New Tests**      | 30+                 |
| **Lines of Code**  | ~1,500              |
| **Test Coverage**  | 80%+                |

---

## Risk Mitigation

### High Risk: React Flow Node Ordering

**Mitigation**:

- Enforce ordering in `completeGroupWizard`
- Unit test verifies group-first ordering
- Code comments warn future developers

### High Risk: Ghost Node Duplication

**Mitigation**:

- Clear logic in `GhostNodeRenderer` for each step
- Hide real nodes when ghost group appears
- Tests verify correct node counts

### Medium Risk: Auto-Inclusion Edge Cases

**Mitigation**:

- Defensive programming in `getAutoIncludedNodes`
- Handle missing device/host gracefully
- Tests cover missing connections

---

## Next Steps

### Immediate (Start Implementation)

1. **Create branch**: `feature/38139-wizard-group`
2. **Begin Subtask 1**: Group Selection Constraints
3. **First file**: `src/modules/Workspace/components/wizard/utils/groupConstraints.ts`

### Week 1 Goals

- [ ] Complete Subtask 1 (Selection Constraints)
- [ ] Complete Subtask 2 (Auto-Inclusion UI)
- [ ] Begin Subtask 3 (Ghost Group Factory)

### Week 2 Goals

- [ ] Complete Subtask 3 (Ghost Group Factory)
- [ ] Complete Subtask 4 (Ghost Group Renderer)
- [ ] Complete Subtask 5 (Configuration Form)

### Week 3 Goals

- [ ] Complete Subtask 6 (Wizard Completion)
- [ ] Complete Subtask 7 (E2E Testing)
- [ ] Complete Subtask 8 (Documentation)
- [ ] Code review and merge

---

## Guidelines to Follow

All implementation must follow these established guidelines:

- ✅ **DATAHUB_ARCHITECTURE.md** - Workspace state management patterns
- ✅ **TESTING_GUIDELINES.md** - Accessibility tests mandatory (one unskipped per component)
- ✅ **I18N_GUIDELINES.md** - Plain string keys with context feature
- ✅ **DESIGN_GUIDELINES.md** - UI component patterns
- ✅ **WORKSPACE_TOPOLOGY.md** - Node types and relationships

---

## Resources

### Reference Code (Study These)

1. **Existing Group Creation**:

   - `src/modules/Workspace/utils/group.utils.ts` - `createGroup`, `getGroupBounds`
   - `src/modules/Workspace/components/nodes/ContextualToolbar.tsx` - `onCreateGroup`

2. **Similar Wizard (Combiner)**:

   - Both use interactive selection
   - Study selection constraint patterns
   - Reuse selection panel approach

3. **Ghost Node Patterns**:
   - `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts`
   - `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx`

### Task 38111 Reference

This task builds on the complete wizard infrastructure from task 38111:

- Wizard state management (Zustand store)
- Metadata registry
- Trigger button
- Progress bar
- Selection system
- Configuration panel integration

**All infrastructure is ready** - this task focuses only on group-specific logic.

---

## Questions to Resolve During Implementation

### Question 1: Nested Group Selection

**Question**: If user selects a group node, should its children be auto-included?

**Options**:

- A) Yes - flatten the group and include all children
- B) No - treat group as single unit

**Recommendation**: B (treat as single unit for simplicity)

### Question 2: Group Color Preview

**Question**: Should Step 1 ghost group show the selected color?

**Options**:

- A) Yes - update ghost styling based on selection
- B) No - always show blue ghost, color applies on completion

**Recommendation**: B (simpler implementation)

### Question 3: Maximum Group Size

**Question**: Should there be a maximum number of nodes in a group?

**Options**:

- A) Yes - set limit (e.g., 10 nodes)
- B) No - unlimited

**Recommendation**: B initially, add limit if performance issues arise

---

## Communication

### To Resume Work

In any new conversation, simply say:

> "Continue work on task 38139" or "We're working on task 38139-wizard-group"

The AI agent will automatically load:

- TASK_BRIEF.md
- TASK_PLAN.md
- QUICK_REFERENCE.md
- Latest CONVERSATION files

### To Start a Subtask

> "Let's start Subtask 1: Group Selection Constraints"

Or reference by number:

> "Begin subtask 3"

---

## Summary

**Planning Status**: ✅ Complete

**Documentation Created**:

- ✅ TASK_BRIEF.md - Requirements and constraints
- ✅ TASK_PLAN.md - 8 subtask implementation plan
- ✅ QUICK_REFERENCE.md - Developer quick start
- ✅ PLANNING_COMPLETE.md - This document

**Ready to Start**: Yes - all planning complete, subtasks clearly defined

**Estimated Timeline**: 2-3 weeks (13 working days)

**Next Action**: Create feature branch and begin Subtask 1

---

## Planning Update (Same Day)

**UX Improvement Applied**: Dynamic Ghost Group Formation

After initial planning, a significant UX improvement was identified and incorporated:

**Original Plan**: Ghost group appears only in Step 1 (preview)  
**Improved Plan**: Ghost group appears dynamically in Step 0 and updates in real-time

**Benefits**:

- Immediate visual feedback as users select nodes
- Users see the group forming as they click
- Better understanding of what will be created
- Reduces cognitive load (no surprise preview)

**Implementation Impact**:

- No change to timeline (actually simpler)
- Cleaner reactive approach using wizard store
- See [DYNAMIC_GHOST_APPROACH.md](./DYNAMIC_GHOST_APPROACH.md) for details

---

**Planning Completed:** November 21, 2025  
**Planning Duration:** 2 hours (+ 30 min UX refinement)  
**Ready for Implementation:** ✅ Yes  
**Approved for Development:** Pending review

---

_The foundation from task 38111 makes this implementation straightforward. The clear plan with 8 focused subtasks ensures steady progress with measurable checkpoints. The dynamic ghost approach provides superior UX with similar implementation complexity._
