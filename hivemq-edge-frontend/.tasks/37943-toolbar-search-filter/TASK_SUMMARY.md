# Task 37943: Toolbar Search Filter - Summary (REVISED)

**Status:** 🟡 Ready to Start  
**Started:** October 31, 2025  
**Task Directory:** `.tasks/37943-toolbar-search-filter/`

---

## Quick Overview

**USER CORRECTION:** Don't reinvent the wheel! The collapsible toolbar already exists in `CanvasToolbar.tsx`.

**Simple Plan:**

1. Add ARIA attributes to existing CanvasToolbar
2. Move toolbar to top-left & add layout controls section
3. Update tests

---

## Progress Tracking

### Phase 1: Planning & Architecture ✅ COMPLETE

- [x] Created TASK_BRIEF.md
- [x] Created TASK_SUMMARY.md
- [x] Reviewed existing implementation
- [x] Identified ARIA requirements
- [x] **User feedback: Corrected to leverage existing CanvasToolbar**

### Phase 2: Implementation 🟡 READY TO START

- [ ] Subtask 1: Add ARIA attributes to existing CanvasToolbar
- [ ] Subtask 2: Move toolbar to top-left & add layout controls
- [ ] Subtask 3: Consolidate and update tests

---

## REVISED Subtasks (3 Simple Steps)

**Key Principle:** Do not modify any passing tests until we have agreement after each subtask.

---

### Subtask 1: Add ARIA Attributes to Existing CanvasToolbar 🎯 NEXT

**Objective:** Enhance existing CanvasToolbar.tsx with proper accessibility attributes WITHOUT breaking anything.

**File to Modify:**

- `src/modules/Workspace/components/controls/CanvasToolbar.tsx`

**Changes:**

1. Add `aria-expanded={expanded ? "true" : "false"}` to expand button
2. Add `aria-expanded={expanded ? "true" : "false"}` to collapse button
3. Add `aria-controls="workspace-toolbar-content"` to both buttons
4. Add `id="workspace-toolbar-content"` to the HStack with SearchEntities
5. Optionally wrap content in `<Box role="region" aria-label="Search and filter">`

**DO NOT:**

- ❌ Change any existing behavior
- ❌ Modify any tests yet
- ❌ Change animations or styling
- ❌ Break any passing tests

**Acceptance Criteria:**

- ✅ All existing tests still pass: `pnpm test:component CanvasToolbar`
- ✅ ARIA attributes added correctly
- ✅ No console errors/warnings
- ✅ Expand/collapse works exactly as before
- ✅ **GET USER AGREEMENT before Subtask 2**

**Estimated Time:** 10-15 minutes

---

### Subtask 2: Move Toolbar to Top-Left & Add Layout Controls

**Objective:** Relocate toolbar and integrate layout controls as second section.

**Files to Modify:**

1. `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
2. `src/modules/Workspace/components/ReactFlowWrapper.tsx`

**Changes in CanvasToolbar.tsx:**

1. Change `position="top-right"` to `position="top-left"`
2. After search/filter section, add `<Divider my={2} />`
3. Add layout controls section (copy from LayoutControlsPanel.tsx):
   ```tsx
   {
     config.features.WORKSPACE_AUTO_LAYOUT && (
       <Box role="region" aria-label="Layout controls" p={2}>
         <HStack spacing={2}>
           <LayoutSelector />
           <ApplyLayoutButton />
           <LayoutPresetsManager />
           <IconButton /* settings */ />
         </HStack>
       </Box>
     )
   }
   ```
4. Import all necessary components/hooks from LayoutControlsPanel
5. Move LayoutOptionsDrawer logic (useDisclosure, drawer component)
6. Keep keyboard shortcut (Ctrl/Cmd+L)

**Changes in ReactFlowWrapper.tsx:**

1. Remove `import LayoutControlsPanel`
2. Remove `<LayoutControlsPanel />` usage
3. Keep `<CanvasToolbar />` (already there)

**DO NOT:**

- ❌ Modify search/filter behavior
- ❌ Change animation logic
- ❌ Modify any tests yet
- ❌ Remove LayoutControlsPanel.tsx file yet

**Acceptance Criteria:**

- ✅ Toolbar at top-left
- ✅ Search/filter works as before
- ✅ Layout section visible when expanded (if feature enabled)
- ✅ Visual divider between sections
- ✅ All controls functional
- ✅ Keyboard shortcut works
- ✅ Settings drawer opens
- ✅ No LayoutControlsPanel on canvas
- ✅ Manual testing confirms everything works
- ✅ **GET USER AGREEMENT before Subtask 3**

**Estimated Time:** 30-45 minutes

---

### Subtask 3: Consolidate and Update Tests

**Objective:** Update tests for combined toolbar and remove deprecated components.

**File to Modify:**

- `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`

**Files to Remove (After Tests Pass):**

- `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
- `src/modules/Workspace/components/controls/LayoutControlsPanel.spec.cy.tsx`

**New Test Cases to Add:**

1. "should show layout section when expanded and feature enabled"
2. "should hide layout section when feature disabled"
3. "should show visual divider between sections"
4. "should show layout selector"
5. "should show apply layout button"
6. "should show presets manager"
7. "should show settings button"
8. "should open layout options drawer"

**Update Accessibility Test:**

- Verify both sections have proper ARIA
- Test collapsed and expanded states
- Verify all ARIA attributes

**After All Tests Pass:**

1. Remove LayoutControlsPanel.tsx
2. Remove LayoutControlsPanel.spec.cy.tsx
3. Verify no broken imports
4. Run full test suite

**Acceptance Criteria:**

- ✅ 8+ new tests added
- ✅ All tests passing
- ✅ Accessibility test covers both sections
- ✅ Old files removed
- ✅ No broken imports
- ✅ Full suite passes: `pnpm test:component`

**Estimated Time:** 30-45 minutes

---

## Work Session Plan

**Session 2 (Now):** Subtask 1 only

- Add ARIA attributes
- Verify tests pass
- Get user approval

**Session 3 (Next):** Subtask 2 only

- Move toolbar & add layout section
- Manual testing
- Get user approval

**Session 4 (Final):** Subtask 3 only

- Update tests
- Remove old components
- Final validation

---

## Technical Notes

### Existing CanvasToolbar Has:

- ✅ Collapse/expand functionality
- ✅ Smooth animations (400ms)
- ✅ Expand/collapse buttons
- ✅ SearchEntities component
- ✅ DrawerFilterToolbox component
- ✅ Tests passing

### We Need to Add:

- ARIA attributes for accessibility
- Layout controls section
- Visual separator (Divider)
- Updated tests

### Files Involved:

- **Modify:** CanvasToolbar.tsx, CanvasToolbar.spec.cy.tsx, ReactFlowWrapper.tsx
- **Remove (later):** LayoutControlsPanel.tsx, LayoutControlsPanel.spec.cy.tsx

---

## Definition of Done

- [ ] Subtask 1: ARIA attributes added, tests pass
- [ ] Subtask 2: Toolbar moved, layout integrated, manual testing confirms
- [ ] Subtask 3: Tests updated, old components removed, all tests pass
- [ ] User approval at each step
- [ ] No regressions
- [ ] Documentation updated

---

**Last Updated:** October 31, 2025  
**Next Action:** Start Subtask 1 - Add ARIA attributes

- `src/modules/Workspace/components/filters/DrawerFilterToolbox.tsx`

**Implementation Details:**

1. Import SearchEntities and DrawerFilterToolbox components
2. Add section with `role="region"` and `aria-label="Search and Filter"`
3. Maintain existing component behavior and styling
4. Ensure search functionality works in new location
5. Keep drawer interactions functional

**Acceptance Criteria:**

- ✅ Search input appears in Section 1
- ✅ Filter button appears in Section 1
- ✅ All search functionality works
- ✅ Filter drawer opens correctly
- ✅ Section has proper ARIA labels

**Estimated Complexity:** Low

---

#### Subtask 3: Integrate Layout Controls Section

**Objective:** Move layout controls from `LayoutControlsPanel.tsx` into the new unified toolbar as Section 2.

**Files to Modify:**

- `src/modules/Workspace/components/controls/WorkspaceToolbar.tsx`

**Files to Reference:**

- `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx` (existing implementation)
- `src/modules/Workspace/components/layout/LayoutSelector.tsx`
- `src/modules/Workspace/components/layout/ApplyLayoutButton.tsx`
- `src/modules/Workspace/components/layout/LayoutPresetsManager.tsx`
- `src/modules/Workspace/components/layout/LayoutOptionsDrawer.tsx`

**Implementation Details:**

1. Import layout control components
2. Add section with `role="region"` and `aria-label="Layout Controls"`
3. Respect `config.features.WORKSPACE_AUTO_LAYOUT` feature flag
4. Maintain keyboard shortcut (Ctrl/Cmd+L)
5. Keep settings drawer functional

**Acceptance Criteria:**

- ✅ Layout selector appears in Section 2
- ✅ Apply button appears in Section 2
- ✅ Presets manager appears in Section 2
- ✅ Settings button appears in Section 2
- ✅ Feature flag controls section visibility
- ✅ Keyboard shortcut still works
- ✅ Settings drawer opens correctly
- ✅ Section has proper ARIA labels

**Estimated Complexity:** Medium

---

#### Subtask 4: Add Visual Separators

**Objective:** Add visual separation between the two sections for better UX.

**Files to Modify:**

- `src/modules/Workspace/components/controls/WorkspaceToolbar.tsx`

**Implementation Details:**

1. Add `<Divider>` component between sections
2. Style with appropriate spacing/margin
3. Ensure divider respects theme (light/dark mode)
4. Consider using Chakra UI's `Divider` component

**Acceptance Criteria:**

- ✅ Clear visual separation between sections
- ✅ Divider respects theme
- ✅ Appropriate spacing/padding
- ✅ Does not break layout

**Estimated Complexity:** Low

---

### Phase 3: Testing & Polish

#### Subtask 5: Update and Consolidate Tests

**Objective:** Create comprehensive component tests for the new unified toolbar and deprecate old tests.

**Files to Create:**

- `src/modules/Workspace/components/controls/WorkspaceToolbar.spec.cy.tsx`

**Files to Deprecate/Remove:**

- `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`
- `src/modules/Workspace/components/controls/LayoutControlsPanel.spec.cy.tsx`

**Test Coverage Required:**

1. Toolbar renders collapsed by default
2. Expand button shows content
3. Collapse button hides content
4. Search section renders with all components
5. Layout section renders with all components (when feature enabled)
6. Layout section hidden when feature disabled
7. Visual separator present
8. Keyboard shortcut still functional
9. Drawers open correctly (filter + layout options)

**Acceptance Criteria:**

- ✅ At least 10 test cases covering all functionality
- ✅ All tests passing
- ✅ Old test files removed
- ✅ Test follows TESTING_GUIDELINES.md patterns

**Estimated Complexity:** Medium

---

#### Subtask 6: Accessibility Validation

**Objective:** Ensure the unified toolbar meets all accessibility requirements.

**Files to Modify:**

- `src/modules/Workspace/components/controls/WorkspaceToolbar.spec.cy.tsx`

**Accessibility Test Coverage:**

1. Collapsed state is accessible
2. Expanded state is accessible
3. Toggle button has `aria-expanded`
4. Toggle button has `aria-controls`
5. Content has matching `id`
6. Sections have `role="region"` with `aria-label`
7. Keyboard navigation works (Tab, Enter, Escape)
8. Screen reader announces state changes

**Implementation Details:**

1. Add mandatory accessibility test: `it('should be accessible', () => {...})`
2. Test both collapsed and expanded states
3. Verify ARIA attributes are present and correct
4. Test keyboard interactions
5. Use `cy.checkAccessibility()` with axe-core

**Acceptance Criteria:**

- ✅ Accessibility test passes in collapsed state
- ✅ Accessibility test passes in expanded state
- ✅ All ARIA attributes validated
- ✅ No axe violations
- ✅ Keyboard navigation functional

**Estimated Complexity:** Medium

---

#### Subtask 7: E2E Testing (Optional Enhancement)

**Objective:** Add end-to-end tests covering the unified toolbar in the full workspace context.

**Files to Create/Modify:**

- `cypress/e2e/workspace/toolbar.cy.ts` (if needed)
- Update existing workspace E2E tests if they reference old toolbars

**Test Scenarios:**

1. Toolbar appears on workspace load
2. Search filters nodes correctly
3. Layout algorithm can be applied
4. Preset management works
5. Settings persist across sessions

**Acceptance Criteria:**

- ✅ E2E tests cover main workflows
- ✅ Tests pass in CI/CD
- ✅ No regressions in existing workspace tests

**Estimated Complexity:** High (Optional)

---

## Integration Steps

### Update ReactFlowWrapper.tsx

**File:** `src/modules/Workspace/components/ReactFlowWrapper.tsx`

**Changes:**

1. Remove import of `CanvasToolbar` (top-right)
2. Remove import of `LayoutControlsPanel` (top-left)
3. Add import of new `WorkspaceToolbar`
4. Replace both toolbar components with single `<WorkspaceToolbar />`

**Before:**

```tsx
import CanvasToolbar from './controls/CanvasToolbar.tsx'
import LayoutControlsPanel from './controls/LayoutControlsPanel.tsx'

// ...
<CanvasToolbar />
<LayoutControlsPanel />
```

**After:**

```tsx
import WorkspaceToolbar from './controls/WorkspaceToolbar.tsx'

// ...
;<WorkspaceToolbar />
```

---

## Technical Decisions

### Component Architecture

**Decision:** Create a new unified component rather than modifying existing ones.

**Rationale:**

- Cleaner implementation
- Easier to test in isolation
- Can deprecate old components cleanly
- Reduces risk of breaking existing functionality during transition

### Position Strategy

**Decision:** Use `position="top-left"` for the unified toolbar.

**Rationale:**

- Layout controls were already at top-left
- Left side provides better visibility for primary tools
- Top-right can be reserved for future controls (notifications, user menu, etc.)
- Consistent with common IDE/design tool patterns

### Section Organization

**Decision:** Search/Filter at top, Layout Controls at bottom.

**Rationale:**

- Search is more frequently used than layout
- Natural reading order (top to bottom)
- Search results need more vertical space
- Layout controls are "power user" features

### Animation Strategy

**Decision:** Reuse existing animation constants from CanvasToolbar.

**Rationale:**

- Consistent animation timing across app
- Already tested and working
- Constants defined in theme utilities
- `ANIMATION.TOOLBAR_ANIMATION_DURATION_MS` = 400ms

---

## Risk Assessment

### Low Risk

- ✅ Both existing toolbars are well-tested
- ✅ Components are already modular
- ✅ Animation patterns already established

### Medium Risk

- ⚠️ ARIA attributes need careful implementation
- ⚠️ Tests need to be rewritten
- ⚠️ Integration may reveal layout conflicts

### Mitigation Strategies

1. Follow TESTING_GUIDELINES.md strictly
2. Test incrementally after each subtask
3. Keep old components until new one is fully tested
4. Use feature flag if needed for gradual rollout

---

## Definition of Done

- [ ] New WorkspaceToolbar component created
- [ ] Both sections integrated and functional
- [ ] Old components removed
- [ ] ReactFlowWrapper updated
- [ ] All tests passing (component + accessibility)
- [ ] No accessibility violations
- [ ] Visual separators in place
- [ ] ARIA attributes correct
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Merged to main branch

---

## Notes

- Consider adding responsive behavior (media queries) in future enhancement
- Could add keyboard shortcut to toggle toolbar (e.g., Ctrl+T)
- May want to persist collapsed/expanded state in localStorage
- Icon-only mode for very small viewports could be future iteration

---

**Last Updated:** October 31, 2025
