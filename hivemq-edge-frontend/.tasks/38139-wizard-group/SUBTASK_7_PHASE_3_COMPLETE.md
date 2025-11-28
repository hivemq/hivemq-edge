# E2E Test Implementation Status

**File:** `cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts`  
**Total Tests:** 16  
**Date:** November 24, 2025

---

## Test Implementation Status

### ✅ Ready to Run (Core Tests - 10 tests)

These tests can run with current mock setup:

#### Suite 1: Accessibility (2 tests)

- ✅ **Test 1.1**: Selection panel accessibility - **PASSING**
- ⏸️ **Test 1.2**: Configuration form accessibility (skipped - add later)

#### Suite 2: Critical Path (2 tests)

- 🔄 **Test 2.1**: Create group with 2 adapters (ACTIVE - currently running)
- ⏸️ **Test 2.2**: Create group with mixed types (ready to run)

#### Suite 3: Selection Constraints (3 tests)

- ⏸️ **Test 3.1**: Auto-include DEVICE/HOST nodes (ready to run)
- ⚠️ **Test 3.2**: Prevent selecting already-grouped nodes (needs mock data)
- ⏸️ **Test 3.3**: Deselection and panel updates (ready to run)

#### Suite 4: Ghost Preview (3 tests)

- ⏸️ **Test 4.1**: Ghost appears on first selection (ready to run)
- ⏸️ **Test 4.2**: Ghost expands with selections (ready to run)
- ⏸️ **Test 4.3**: Ghost shrinks on deselection (ready to run)

#### Suite 6: Configuration (3 tests)

- ⏸️ **Test 6.1**: Custom color scheme (ready to run)
- ⏸️ **Test 6.2**: Form validation (ready to run)
- ⏸️ **Test 6.3**: Back button navigation (ready to run)

#### Suite 7: Visual Regression (1 test)

- ⏸️ **Test 7.1**: Complete visual flow (ready to run)

---

### ⚠️ Requires Special Mock Setup (2 tests)

These tests need nested group hierarchy mocks:

#### Suite 5: Nested Groups (2 tests)

- ⚠️ **Test 5.1**: Maximum nesting depth (needs 3-level hierarchy mock)
- ⚠️ **Test 5.2**: Valid nested creation (needs 2-level hierarchy mock)

**Mock Requirements:**

```typescript
// Example mock structure needed:
const mockGroupLevel1 = {
  id: 'group-level-1',
  type: 'GROUP',
  children: ['adapter-1'],
}

const mockGroupLevel2 = {
  id: 'group-level-2',
  parentId: 'group-level-1',
  type: 'GROUP',
  children: ['adapter-2'],
}

const mockGroupLevel3 = {
  id: 'group-level-3',
  parentId: 'group-level-2',
  type: 'GROUP',
  children: ['adapter-3'],
}
```

---

## Testing Plan

### Phase 1: Core Functionality ✅ IN PROGRESS

1. ✅ Test 1.1 (accessibility) - PASSED
2. 🔄 Test 2.1 (create group) - RUNNING NOW
3. Test 2.2 (mixed types)
4. Test 3.3 (deselection)

### Phase 2: Ghost Preview

5. Test 4.1 (ghost appears)
6. Test 4.2 (ghost expands)
7. Test 4.3 (ghost shrinks)

### Phase 3: Selection & Configuration

8. Test 3.1 (auto-include)
9. Test 6.1 (color scheme)
10. Test 6.2 (validation)
11. Test 6.3 (back button)

### Phase 4: Visual Regression

12. Test 7.1 (Percy snapshots)

### Phase 5: Nested Groups (Requires Mock Setup)

13. Test 5.1 (depth limit)
14. Test 5.2 (valid nesting)
15. Test 3.2 (already-grouped prevention)

---

## Running Tests Individually

Use `.skip` to skip all tests except the one you're testing:

```bash
# Current approach: Test 2.1 is active, others are skipped
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
```

**To test a different one:**

1. Add `.skip` to Test 2.1: `it.skip('should create...', () => {`
2. Remove `.skip` from target test: `it('should expand ghost...', () => {`
3. Run again

---

## Known Issues / Adjustments Needed

### Selector Adjustments

May need to adjust based on actual implementation:

1. **Ghost group selector**: `[data-testid^="rf__node-ghost-group"]`
   - Verify actual testid in GhostNodeRenderer
2. **Auto-included section**: `wizard-auto-included-section`
   - Check AutoIncludedNodesList component for actual testid
3. **Color scheme selector**: `color-scheme-selector`
   - Check WizardGroupForm for actual implementation (buttons vs dropdown)
4. **Remove button in selection panel**
   - May need specific selector for "X" button

### Form Validation

Test 6.2 assumes form validation prevents submission. Verify:

- Does the form show inline validation errors?
- Does submit button become disabled?
- What's the validation message?

---

## Page Object Enhancements Made

✅ Added to `WizardPage.ts`:

- `wizardMenu.selectOption('GROUP')` - Added GROUP to type union
- `groupConfig` section with:
  - `panel`, `configForm`, `titleInput`
  - `colorSchemeSelector`, `submitButton`, `backButton`
  - `setTitle(title)`, `selectColorScheme(color)` helpers

---

## Next Steps

1. **Run Test 2.1** and fix any issues
2. **Apply learned patterns** to remaining tests
3. **Test incrementally** through Phases 1-4
4. **Setup nested group mocks** for Phase 5
5. **Document results** in completion report

---

## Completion Criteria

- ✅ All 14 ready-to-run tests passing
- ⏸️ 2 nested group tests documented (requires mock setup decision)
- ✅ Percy snapshots captured (Test 7.1)
- ✅ Cypress screenshots for docs
- ✅ No accessibility violations
- ✅ All HTML snapshots saved for AI debugging

---

**Current Status:** Phase 1 - Test 2.1 in progress  
**Next Test:** Test 2.2 (after 2.1 passes)
