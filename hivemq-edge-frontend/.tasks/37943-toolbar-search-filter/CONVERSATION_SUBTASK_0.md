# Conversation: Subtask 0 - Planning & Architecture

**Date:** October 31, 2025  
**Phase:** 1 - Planning  
**Participants:** AI Agent + User

---

## Session Overview

Initial planning session for task 37943-toolbar-search-filter. Created comprehensive documentation for combining the layout controls toolbar and search/filter toolbar into a unified collapsible component.

---

## User Request

> We are starting a new task, 37943, name "toolbar-search-filter".
>
> This is a follow-up to task 25337 (workspace-auto-layout), where we created a layout controls toolbar positioned at the top-left of the canvas. During that task, two improvements were mentioned for the layout toolbar:
>
> - Make the toolbar expandable to reduce footprint on the canvas
> - Make the toolbar responsive, possibly turning to icon-only with small media size
>
> There is an existing search & filter toolbar positioned on the top-right of the canvas.
> The goal is to combine both toolbars into the same collapsable toolbar.
> The proposed layout for the combination is a section-based model:
>
> - the search comes first (top of the toolbar content)
> - the layout comes second (bottom of the toolbar content)
>
> Propose a plan that will achieve this proposal in subtasks that we can monitor together
>
> Acceptance criteria:
>
> - make yourself familiar with our REPORTING and TESTING GUIDELINES
> - make yourself familiar with aria attributes related to collapsed and expanded widgets
> - Adjust the tests when the merging of the two toolbars is done
> - always ensure that the tests are ALL passing

---

## Context Gathered

### Existing Components Analyzed

1. **LayoutControlsPanel.tsx** (Top-Left)

   - Location: `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
   - Position: `top-left` of canvas
   - Components: LayoutSelector, ApplyLayoutButton, LayoutPresetsManager, Settings button
   - State: Always expanded, no collapse functionality
   - Feature flag: `config.features.WORKSPACE_AUTO_LAYOUT`
   - Tests: 7 tests including accessibility

2. **CanvasToolbar.tsx** (Top-Right)
   - Location: `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
   - Position: `top-right` of canvas
   - Components: SearchEntities, DrawerFilterToolbox, Expand/Collapse buttons
   - State: Collapsible with animated transitions
   - Animation: Uses `ANIMATION.TOOLBAR_ANIMATION_DURATION_MS` (400ms)
   - Tests: 1 test covering expand/collapse

### Guidelines Reviewed

1. **TESTING_GUIDELINES.md**

   - Mandatory accessibility test pattern: `it('should be accessible', () => {...})`
   - Must test both collapsed and expanded states
   - Use `cy.checkAccessibility()` with axe-core
   - Select components require `aria-label`

2. **DESIGN_GUIDELINES.md**

   - Button variant patterns (primary, outline, ghost)
   - Modal icons and colors
   - Consistent theming

3. **REPORTING_STRATEGY.md**
   - Two-tier documentation (permanent + ephemeral)
   - CONVERSATION_SUBTASK_N.md files
   - TASK_SUMMARY.md updates

### ARIA Research

Researched WAI-ARIA Disclosure Pattern for collapsible widgets:

- `aria-expanded="true|false"` on toggle button
- `aria-controls` pointing to content ID
- `role="region"` on major sections with `aria-label`
- Content element needs unique `id`

---

## Deliverables Created

### 1. TASK_BRIEF.md ✅

Comprehensive task brief including:

- Objective and context
- Current state analysis
- Proposed layout with ASCII diagram
- Acceptance criteria
- Technical requirements (ARIA attributes)
- Implementation notes
- Related files
- References

**Key Highlights:**

- Section-based model (Search first, Layout second)
- Single toolbar at top-left
- Collapsible with animations
- Proper ARIA attributes
- All tests must pass

---

### 2. TASK_SUMMARY.md ✅

Detailed task summary with:

- Progress tracking (3 phases)
- 7 subtasks breakdown
- Phase 1: Planning (Complete)
- Phase 2: Core Implementation (4 subtasks)
- Phase 3: Testing & Polish (3 subtasks)
- Integration steps
- Technical decisions
- Risk assessment
- Definition of done

**Phases:**

- **Phase 1:** Planning & Architecture ✅ COMPLETE
- **Phase 2:** Core Implementation (Subtasks 1-4)
- **Phase 3:** Testing & Polish (Subtasks 5-7)

---

### 3. ROADMAP.md ✅

Visual roadmap including:

- ASCII diagrams of before/after states
- Detailed subtask breakdown
- Work session recommendations
- Progress tracking checklists
- Critical success factors
- Risk mitigation table

**Recommended Sessions:**

- Session 1: Planning ✅ COMPLETE
- Session 2: Core Implementation (Subtasks 1-5)
- Session 3: Testing & Polish (Subtasks 6-8)

---

### 4. ARIA_REFERENCE.md ✅

Comprehensive ARIA guide with:

- Required attributes for each element
- Complete example implementation
- Testing strategies (manual + automated)
- Common mistakes to avoid
- WCAG success criteria mapping
- Resources and references

**Key Patterns:**

- Toggle button: `aria-expanded`, `aria-controls`, `aria-label`
- Content: `id`, `role="group"`
- Sections: `role="region"`, `aria-label`
- Panel: `role="complementary"`

---

### 5. Updated ACTIVE_TASKS.md ✅

Added task 37943 to the active tasks index with:

- Status: Active 🟡
- Phase breakdown
- Quick start links
- Summary of objective

---

## Subtasks Defined

### Phase 2: Core Implementation

1. **Subtask 1:** Create unified toolbar component
   - New file: `WorkspaceToolbar.tsx`
   - Collapse/expand state
   - ARIA attributes
   - Animations
2. **Subtask 2:** Integrate search/filter section
   - Import SearchEntities, DrawerFilterToolbox
   - Add as Section 1 with proper ARIA
3. **Subtask 3:** Integrate layout controls section
   - Import layout components
   - Add as Section 2 with proper ARIA
   - Respect feature flag
4. **Subtask 4:** Add visual separators

   - Divider between sections
   - Theme-aware styling

5. **Subtask 5:** Update ReactFlowWrapper integration
   - Remove old toolbar imports
   - Add new WorkspaceToolbar
   - Test in context

### Phase 3: Testing & Polish

6. **Subtask 6:** Create comprehensive component tests
   - 10+ test cases
   - Cover all functionality
   - Follow TESTING_GUIDELINES.md
7. **Subtask 7:** Accessibility validation
   - Mandatory accessibility test
   - Test both states
   - Validate all ARIA attributes
   - No axe violations
8. **Subtask 8:** Cleanup and documentation
   - Remove old component files
   - Remove old test files
   - Update documentation
   - Verify all tests pass

---

## Technical Decisions

### 1. Component Architecture

**Decision:** Create new unified component, don't modify existing ones

**Rationale:**

- Cleaner implementation
- Easier to test in isolation
- Can deprecate old components cleanly
- Lower risk during transition

---

### 2. Position Strategy

**Decision:** Use `position="top-left"` for unified toolbar

**Rationale:**

- Layout controls already at top-left
- Better visibility for primary tools
- Top-right free for other controls
- Matches common IDE patterns

---

### 3. Section Organization

**Decision:** Search/Filter first, Layout Controls second

**Rationale:**

- Search used more frequently
- Natural top-to-bottom reading order
- Search needs more vertical space
- Layout controls are "power user" features

---

### 4. Animation Strategy

**Decision:** Reuse existing `ANIMATION.TOOLBAR_ANIMATION_DURATION_MS` (400ms)

**Rationale:**

- Consistent with existing toolbar
- Already tested and proven
- Defined in theme utilities
- No need to reinvent

---

## Risk Assessment

### Low Risk ✅

- Both toolbars well-tested
- Components are modular
- Animation patterns established

### Medium Risk ⚠️

- ARIA attributes need careful implementation
- Tests need rewriting
- Integration may reveal layout conflicts

### Mitigation Strategies

1. Follow TESTING_GUIDELINES.md strictly
2. Test incrementally after each subtask
3. Keep old components until new one fully tested
4. Created comprehensive ARIA_REFERENCE.md

---

## Next Steps

### Immediate (Session 2)

1. Start Subtask 1: Create WorkspaceToolbar.tsx
2. Implement collapse/expand functionality
3. Add proper ARIA attributes
4. Test in isolation

### Short Term (Session 2-3)

1. Complete Subtasks 2-5 (Core Implementation)
2. Integrate both sections
3. Update ReactFlowWrapper
4. Verify functionality

### Final (Session 3)

1. Complete Subtasks 6-8 (Testing & Polish)
2. Write comprehensive tests
3. Validate accessibility
4. Remove old components
5. Update documentation

---

## Key Takeaways

1. **Well-Defined Scope:** Clear objective with measurable acceptance criteria
2. **Comprehensive Planning:** 4 documentation files covering all aspects
3. **Accessibility First:** ARIA requirements documented before coding
4. **Risk Mitigation:** Identified risks with mitigation strategies
5. **Incremental Approach:** 8 subtasks allowing progress monitoring
6. **Testing Focus:** Guidelines reviewed, patterns established

---

## User Feedback & Corrected Approach

### User Correction ✅

> I disagree with your plan. You are missing the big picture: the toolbar functionalities already exist! It's been implemented for the search feature.
> Do not reinvent the wheel:
>
> - subtask 1: extend the existing toolbar to incorporate the aria attributes
> - subtask 2: move the search bar to the left and migrate the layout element into it
> - subtask 3: adjust tests (most of the component SHOULD be already tested)
>   My added criteria: do not change any existing (and passing test) until we have an agreement

**Key Insight:** CanvasToolbar.tsx already has:

- ✅ Collapsible functionality working
- ✅ Smooth animations
- ✅ Expand/collapse buttons
- ✅ Tests passing

**Corrected Strategy:** Extend existing toolbar, don't rebuild from scratch.

---

## REVISED Subtasks (Much Simpler!)

### Subtask 1: Add ARIA Attributes to Existing CanvasToolbar ✅

**Objective:** Enhance CanvasToolbar.tsx with proper accessibility attributes without breaking existing functionality.

**File:** `src/modules/Workspace/components/controls/CanvasToolbar.tsx`

**Changes:**

1. Add `aria-expanded` to toggle buttons (expand/collapse)
2. Add `aria-controls` pointing to content container
3. Add `id` to content container
4. Add `role="region"` with `aria-label` to content sections
5. Ensure Panel has proper `role="complementary"`

**DO NOT:**

- ❌ Change existing behavior
- ❌ Modify tests yet
- ❌ Change animations or styling
- ❌ Break any passing tests

**Success Criteria:**

- All existing tests still pass
- ARIA attributes added
- No functional changes

---

### Subtask 2: Move Toolbar to Top-Left & Add Layout Controls

**Objective:** Relocate toolbar from top-right to top-left and add layout controls as a second section.

**Files to Modify:**

- `src/modules/Workspace/components/controls/CanvasToolbar.tsx`

**Changes:**

1. Change Panel position from `"top-right"` to `"top-left"`
2. Add `<Divider>` after search/filter section
3. Add layout controls section:
   - Import layout components from LayoutControlsPanel.tsx
   - Add with `role="region"` and `aria-label="Layout controls"`
   - Respect `config.features.WORKSPACE_AUTO_LAYOUT` flag
4. Update `ReactFlowWrapper.tsx`:
   - Remove `<LayoutControlsPanel />` import and usage
   - Keep `<CanvasToolbar />` (already there)

**DO NOT:**

- ❌ Modify existing search/filter behavior
- ❌ Change animation logic
- ❌ Break any passing tests yet

**Success Criteria:**

- Toolbar at top-left
- Both sections visible when expanded
- Layout controls functional
- Existing search/filter still works
- LayoutControlsPanel removed from canvas

---

### Subtask 3: Consolidate and Update Tests

**Objective:** Update tests to reflect new combined toolbar, ensuring all functionality is covered.

**Files to Modify:**

- `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`

**Changes:**

1. Add tests for layout controls section:
   - Layout selector visible when expanded (if feature enabled)
   - Apply button functional
   - Presets manager visible
   - Settings button opens drawer
   - Section hidden when feature disabled
2. Update accessibility test:
   - Test both sections have proper ARIA
   - Test expanded/collapsed states
   - Validate all ARIA attributes
3. Add test for visual separator between sections

**Files to Remove (After Agreement):**

- `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
- `src/modules/Workspace/components/controls/LayoutControlsPanel.spec.cy.tsx`

**DO NOT:**

- ❌ Remove files until all tests pass
- ❌ Modify existing passing tests until functionality confirmed

**Success Criteria:**

- All tests passing
- New tests cover layout section
- Accessibility test updated
- Old component files can be safely removed

---

## Revised Implementation Order

1. **Session 2:** Subtask 1 only

   - Add ARIA attributes
   - Verify all existing tests still pass
   - Get user agreement before proceeding

2. **Session 3:** Subtask 2 only

   - Move toolbar & add layout section
   - Test manually
   - Get user agreement before touching tests

3. **Session 4:** Subtask 3 only
   - Update tests
   - Remove old components
   - Final validation

**Key Principle:** Stop after each subtask and get user agreement before proceeding.

---

## Resources Created

- `.tasks/37943-toolbar-search-filter/TASK_BRIEF.md`
- `.tasks/37943-toolbar-search-filter/TASK_SUMMARY.md`
- `.tasks/37943-toolbar-search-filter/ROADMAP.md`
- `.tasks/37943-toolbar-search-filter/ARIA_REFERENCE.md`
- `.tasks/37943-toolbar-search-filter/CONVERSATION_SUBTASK_0.md` (this file)
- Updated: `.tasks/ACTIVE_TASKS.md`

---

**Session Status:** ✅ Complete  
**Next Session:** Phase 2 - Core Implementation  
**Files Modified:** 1 (ACTIVE_TASKS.md)  
**Files Created:** 5 (Task documentation)

---

**End of Conversation Log**
