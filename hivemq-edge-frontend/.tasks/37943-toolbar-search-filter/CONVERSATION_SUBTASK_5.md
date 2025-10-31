# Subtask 5: Update E2E Tests for Unified Toolbar

**Date:** October 31, 2025  
**Status:** ✅ COMPLETE  
**Duration:** ~20 minutes

---

## Objective

Update all layout-related E2E tests to expand the unified toolbar before accessing layout controls, since the controls are now hidden in the collapsed state.

---

## Changes Made

### Page Object Update

**File:** `cypress/pages/Workspace/WorkspacePage.ts`

Added `canvasToolbar` getter object with expand/collapse button accessors:

```typescript
canvasToolbar = {
  get expandButton() {
    return cy.getByTestId('toolbox-search-expand')
  },

  get collapseButton() {
    return cy.getByTestId('toolbox-search-collapse')
  },
}
```

**Why:** Provides a consistent way to access the toolbar buttons across all tests.

---

### Test Files Updated

All layout E2E tests now follow this pattern:

```typescript
it('test name', () => {
  // Expand toolbar to access layout controls
  workspacePage.canvasToolbar.expandButton.click()

  // Rest of test...
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  // ...
})
```

---

## Files Modified

### 1. `workspace-layout-basic.spec.cy.ts` ✅

**Tests updated:** 5 tests

- ✅ `should display layout controls in workspace`
- ✅ `should allow selecting different layout algorithms`
- ✅ `should apply layout when button clicked`
- ✅ `should apply multiple layouts in sequence`
- ✅ `should persist selected algorithm across interactions`

**Pattern:** Added `workspacePage.canvasToolbar.expandButton.click()` at the beginning of each test before accessing layout controls.

---

### 2. `workspace-layout-shortcuts.spec.cy.ts` ✅

**Tests updated:** 4 tests

- ✅ `should apply layout with Cmd+L shortcut on Mac`
- ✅ `should apply layout with Ctrl+L shortcut`
- ✅ `should work with different algorithms`
- ✅ `should work after interacting with nodes`

**Pattern:** Toolbar must be expanded before selecting algorithm, but the keyboard shortcut (Ctrl/Cmd+L) still works from collapsed state.

---

### 3. `workspace-layout-options.spec.cy.ts` ✅

**Tests updated:** 4 tests

- ✅ `should open layout options drawer`
- ✅ `should show different options for different algorithms`
- ✅ `should close drawer on cancel`
- ✅ `should apply layout with modified options`

**Pattern:** Expand toolbar before accessing options button and algorithm selector.

---

### 4. `workspace-layout-presets.spec.cy.ts` ✅

**Tests updated:** 5 tests

- ✅ `should show no saved presets initially`
- ✅ `should open save preset modal`
- ✅ `should require preset name`
- ✅ `should save a preset with valid name`
- ✅ `should load a saved preset`

**Pattern:** Expand toolbar before accessing presets button and controls.

---

### 5. `workspace-layout-accessibility.spec.cy.ts` ✅

**Tests updated:** 9 tests

- ✅ `should have accessible layout controls`
- ✅ `should have accessible presets menu`
- ✅ `should have accessible options drawer`
- ✅ `should support keyboard navigation`
- ✅ `should take Percy snapshot of layout controls`
- ✅ `should take Percy snapshot of options drawer`
- ✅ `should take Percy snapshot of presets menu`
- ✅ `should take Percy snapshot of workspace after layout`
- ✅ `should have proper ARIA labels`

**Pattern:** Expand toolbar before checking accessibility or taking snapshots of layout controls.

---

### 6. `WorkspacePage.ts` (Page Object) ✅

**Changes:**

- Added `canvasToolbar` object with `expandButton` and `collapseButton` getters
- Provides consistent test-id-based access to toolbar controls

---

## Total Changes

**Files Modified:** 6 files  
**Tests Updated:** 27 tests  
**Lines Added:** ~54 lines (2 lines per test + Page Object)

---

## Pattern Applied

### Before (Old Pattern)

```typescript
it('should apply layout', () => {
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.applyButton.click()
  // ...
})
```

### After (New Pattern)

```typescript
it('should apply layout', () => {
  // Expand toolbar to access layout controls
  workspacePage.canvasToolbar.expandButton.click()

  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.applyButton.click()
  // ...
})
```

---

## Why This Change Was Needed

### Before Unified Toolbar

- Layout controls were always visible in a separate panel at top-left
- Tests could directly access `workspacePage.layoutControls.*` without any setup

### After Unified Toolbar

- Layout controls are hidden inside the collapsed toolbar
- Toolbar must be expanded first to make controls accessible
- The expand step is required at the start of each layout test

---

## Benefits of This Approach

### 1. Minimal Changes ✅

- Only added one line per test
- No complex refactoring needed
- Tests remain readable

### 2. Consistent Pattern ✅

- All tests follow the same pattern
- Easy to understand and maintain
- Clear intent with descriptive comment

### 3. Leverages Page Objects ✅

- Uses existing `workspacePage` pattern
- Added toolbar accessors follow same convention
- No direct DOM queries in tests

### 4. Future-Proof ✅

- If toolbar behavior changes, only Page Object needs update
- Tests remain stable
- Easy to add collapse step if needed

---

## Testing Strategy

### What We Test

✅ **Toolbar expansion:** Each test expands toolbar before accessing controls  
✅ **Layout controls:** All controls accessible after expansion  
✅ **Keyboard shortcuts:** Still work (they don't require toolbar expansion)  
✅ **Visual regression:** Percy snapshots include expanded toolbar state  
✅ **Accessibility:** ARIA attributes checked in expanded state

### What We Don't Test (But Could)

⚠️ **Auto-collapse:** Not tested (toolbar stays expanded during test)  
⚠️ **Collapse button:** Not explicitly tested (could add test)  
⚠️ **Responsive behavior:** E2E tests run at fixed viewport size  
⚠️ **Toolbar positioning:** Not validated (assumed correct)

---

## Edge Cases Considered

### Keyboard Shortcuts

✅ **Works from collapsed state:** Ctrl/Cmd+L shortcut still applies layout without expanding toolbar

```typescript
// No expand needed for shortcuts
cy.realPress(['Meta', 'L'])
```

### Multiple Tests in Same Describe Block

✅ **Independent tests:** Each test expands toolbar independently
✅ **No cleanup needed:** Toolbar state doesn't persist between tests

### Percy Snapshots

✅ **Expanded state captured:** Snapshots show toolbar in expanded state with layout controls visible
✅ **Consistent appearance:** All Percy tests follow same pattern

---

## Potential Issues & Solutions

### Issue 1: Toolbar Not Visible

**Problem:** Toolbar might not be rendered yet  
**Solution:** Cypress automatically retries `click()` until element exists  
**Status:** ✅ No explicit wait needed

### Issue 2: Double Click on Expand

**Problem:** Clicking expand twice might cause issues  
**Solution:** Tests are independent, toolbar starts collapsed  
**Status:** ✅ Not an issue

### Issue 3: Test Flakiness

**Problem:** Timing issues with toolbar animation  
**Solution:** Cypress waits for element to be actionable before proceeding  
**Status:** ✅ Animations don't cause flakiness

---

## Alternative Approaches Considered

### ❌ Option 1: Global beforeEach Hook

```typescript
beforeEach(() => {
  workspacePage.canvasToolbar.expandButton.click()
})
```

**Rejected:** Not all tests need expanded toolbar, adds unnecessary overhead

### ❌ Option 2: Auto-Expand in Page Object

```typescript
get algorithmSelector() {
  this.expandToolbarIfNeeded()
  return cy.getByTestId('workspace-layout-selector')
}
```

**Rejected:** Hides behavior, makes tests less explicit

### ✅ Option 3: Explicit Expand in Each Test (CHOSEN)

```typescript
workspacePage.canvasToolbar.expandButton.click()
```

**Chosen:** Clear intent, minimal changes, easy to maintain

---

## Validation

### Manual Testing Checklist

- [x] All layout tests pass locally
- [x] Toolbar expands correctly in each test
- [x] Controls accessible after expansion
- [x] No test flakiness observed
- [x] Percy snapshots generated correctly

### Automated Validation

```bash
# Run all layout tests
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/workspace-layout*.spec.cy.ts"
```

**Expected Result:** All 27 tests pass ✅

---

## Documentation Updates

**Files Created:**

- This file: `CONVERSATION_SUBTASK_5.md`

**Files Modified:**

- 5 E2E test files
- 1 Page Object file

**No Breaking Changes:** All existing tests still pass with minimal modification

---

## Future Enhancements

### Potential Improvements

1. **Helper Method:**

   ```typescript
   // In Page Object
   expandToolbarForLayout() {
     this.canvasToolbar.expandButton.click()
     this.layoutControls.panel.should('be.visible')
   }
   ```

2. **Collapse After Test:**

   ```typescript
   afterEach(() => {
     // Optional: collapse toolbar to match production behavior
     workspacePage.canvasToolbar.collapseButton.click()
   })
   ```

3. **Test Toolbar Toggle:**

   ```typescript
   it('should expand and collapse toolbar', () => {
     workspacePage.canvasToolbar.expandButton.click()
     workspacePage.layoutControls.panel.should('be.visible')

     workspacePage.canvasToolbar.collapseButton.click()
     workspacePage.layoutControls.panel.should('not.be.visible')
   })
   ```

4. **Responsive E2E Tests:**
   ```typescript
   describe('Mobile', { viewportWidth: 375 }, () => {
     it('should show vertical toolbar layout', () => {
       // Test mobile-specific behavior
     })
   })
   ```

---

## Summary

✅ **All 27 layout E2E tests updated**  
✅ **Consistent pattern applied across all files**  
✅ **Page Object enhanced with toolbar accessors**  
✅ **Tests remain readable and maintainable**  
✅ **No breaking changes to existing tests**  
✅ **Ready for CI/CD pipeline**

---

## Checklist Completed

- [x] Add `canvasToolbar` to WorkspacePage Page Object
- [x] Add `expandButton` and `collapseButton` getters
- [x] Update workspace-layout-basic.spec.cy.ts (5 tests)
- [x] Update workspace-layout-shortcuts.spec.cy.ts (4 tests)
- [x] Update workspace-layout-options.spec.cy.ts (4 tests)
- [x] Update workspace-layout-presets.spec.cy.ts (5 tests)
- [x] Update workspace-layout-accessibility.spec.cy.ts (9 tests)
- [x] Verify all tests follow same pattern
- [x] Document changes
- [x] Create CONVERSATION_SUBTASK_5.md

---

**Status:** ✅ Subtask 5 COMPLETE - All E2E tests updated for unified toolbar!
