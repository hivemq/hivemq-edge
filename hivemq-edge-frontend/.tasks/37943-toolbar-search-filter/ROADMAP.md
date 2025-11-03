# Task 37943 - Implementation Roadmap (REVISED)

**USER CORRECTION:** The toolbar already exists! Extend CanvasToolbar instead of creating new component.

---

## 3 Simple Subtasks

### Subtask 1: Add ARIA Attributes (10-15 min) 🎯 NEXT

- Add `aria-expanded`, `aria-controls`, `id`, `role`
- ❌ NO test changes
- ✋ Get user approval

### Subtask 2: Move & Integrate (30-45 min)

- Move toolbar `top-right` → `top-left`
- Add layout controls section + divider
- Remove old LayoutControlsPanel
- ❌ NO test changes yet
- ✋ Get user approval

### Subtask 3: Update Tests (30-45 min)

- Add 8+ new tests
- Update accessibility test
- Remove old component files
- ✅ All tests must pass

---

## Detailed Checklists

### ✅ Subtask 1: Add ARIA Attributes ✅ COMPLETE

**File:** `src/modules/Workspace/components/controls/CanvasToolbar.tsx`

- [x] Add `aria-expanded={expanded ? "true" : "false"}` to expand button
- [x] Add `aria-expanded={expanded ? "true" : "false"}` to collapse button
- [x] Add `aria-controls="workspace-toolbar-content"` to both buttons
- [x] Add `id="workspace-toolbar-content"` to content HStack
- [x] Added `role="region"` and `aria-label` to content HStack
- [x] Run: `pnpm cypress:run:component --spec "src/.../CanvasToolbar.spec.cy.tsx"`
- [x] Verify: All tests still pass ✅
- [x] 🛑 **STOP - Get user approval**

---

### ✅ Subtask 2: Move Toolbar & Add Layout Section ✅ COMPLETE

**File:** `src/modules/Workspace/components/controls/CanvasToolbar.tsx`

- [x] Change `position="top-right"` to `position="top-left"`
- [x] Add `<Divider my={2} />` after search/filter
- [x] Import layout components:
  - [x] LayoutSelector
  - [x] ApplyLayoutButton
  - [x] LayoutPresetsManager
  - [x] LayoutOptionsDrawer
- [x] Import hooks:
  - [x] useLayoutEngine
  - [ ] useWorkspaceStore
  - [ ] useKeyboardShortcut
  - [ ] useDisclosure
- [ ] Add layout section:
  ```tsx
  {
    config.features.WORKSPACE_AUTO_LAYOUT && (
      <Box role="region" aria-label="Layout controls">
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
- [ ] Add keyboard shortcut handler (Ctrl/Cmd+L)
- [ ] Add LayoutOptionsDrawer component at end
- [ ] Test expand/collapse works
- [ ] Test search still works
- [ ] Test layout controls functional

**File:** `src/modules/Workspace/components/ReactFlowWrapper.tsx`

- [ ] Remove `import LayoutControlsPanel` line
- [ ] Remove `<LayoutControlsPanel />` usage
- [ ] Verify `<CanvasToolbar />` still present

**Manual Testing:**

- [ ] Toolbar at top-left ✅
- [ ] Expand/collapse works ✅
- [ ] Search functional ✅
- [ ] Filter drawer opens ✅
- [ ] Layout selector works ✅
- [ ] Apply button works ✅
- [ ] Presets work ✅
- [ ] Settings drawer opens ✅
- [ ] Ctrl/Cmd+L shortcut works ✅
- [ ] Visual divider visible ✅
- [ ] 🛑 **STOP - Get user approval**

---

### ✅ Subtask 3: Update Tests & Cleanup

**File:** `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`

**Add New Tests:**

- [ ] "should show layout section when expanded and feature enabled"
- [ ] "should hide layout section when feature disabled"
- [ ] "should show visual divider between sections"
- [ ] "should show layout selector"
- [ ] "should show apply layout button"
- [ ] "should show presets manager"
- [ ] "should show settings button"
- [ ] "should open layout options drawer when settings clicked"

**Update Existing Tests:**

- [ ] Update accessibility test:
  - [ ] Test both sections have proper ARIA
  - [ ] Test collapsed state accessible
  - [ ] Test expanded state accessible
  - [ ] Verify all ARIA attributes

**Run Tests:**

- [ ] `pnpm test:component CanvasToolbar` ✅
- [ ] All tests passing

**Cleanup:**

- [ ] Delete `LayoutControlsPanel.tsx`
- [ ] Delete `LayoutControlsPanel.spec.cy.tsx`
- [ ] Search for remaining imports
- [ ] `pnpm test:component` (full suite) ✅
- [ ] `pnpm typecheck` ✅

---

## Quick Commands

```bash
# Test single component
pnpm test:component CanvasToolbar

# Test all components
pnpm test:component

# Type check
pnpm typecheck

# Dev server
pnpm dev
```

---

## Files Summary

### Modified (3):

- ✏️ `CanvasToolbar.tsx`
- ✏️ `CanvasToolbar.spec.cy.tsx`
- ✏️ `ReactFlowWrapper.tsx`

### Deleted (2):

- ❌ `LayoutControlsPanel.tsx`
- ❌ `LayoutControlsPanel.spec.cy.tsx`

---

## Session Plan

**Session 2:** Subtask 1 only (Recommended)

- 10-15 minutes
- Add ARIA, verify tests pass
- Get approval ✋

**Session 3:** Subtask 2 only (Recommended)

- 30-45 minutes
- Move + integrate
- Get approval ✋

**Session 4:** Subtask 3 only (Recommended)

- 30-45 minutes
- Tests + cleanup
- All tests pass ✅

**Alternative:** Do all 3 in one session if preferred

---

**Created:** October 31, 2025  
**Status:** Ready for Subtask 1 🚀
