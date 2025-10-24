# Task 33168 - Subtask 2: Duplicate Combiner Modal Implementation

**Date:** October 24, 2025  
**Phase:** Phase 2 - UX Refactoring  
**Status:** ✅ Complete

---

## Objective

Replace the toast notification for duplicate combiner detection with a comprehensive modal dialog that provides better user control and visual feedback.

---

## Design Decisions

### Modal Architecture

**Positioning & Animation**
- Modal positioned in top-left quadrant to leave space for canvas animation
- Uses `size="xl"` for adequate space without overwhelming the view
- **Clear overlay (no backdrop blur)** - Critical for showing the fitView animation to existing combiner
- Animated fitView with 150ms delay allows modal animation to start first
- Uses `ANIMATION.FIT_VIEW_DURATION_MS` constant for consistent timing

**Visual Hierarchy**
- Header: Warning icon + title + combiner name
- Body: Description + mappings list + prompt
- Footer: Cancel (ghost) + Create New (outline) + Use Existing (primary blue)

**Accessibility Features**
- Initial focus on primary action ("Use Existing" button)
- Keyboard navigation support (ESC to close, Enter to confirm)
- Proper ARIA labels and data-testid attributes throughout
- Close on ESC key press
- `closeOnOverlayClick={false}` prevents accidental dismissal

---

## Implementation

### New Files Created

1. **`DuplicateCombinerModal.tsx`** (162 lines)
   - Main modal component
   - Integrates with ReactFlow for canvas animation
   - Handles both combiner and asset mapper variants
   - Three action callbacks: onClose, onUseExisting, onCreateNew

2. **`CombinerMappingsList.tsx`** (70 lines)
   - Lightweight list component showing existing mappings
   - Displays source → destination with arrow icon
   - Shows mapping count badge
   - Shows instruction count per mapping
   - Scrollable for many mappings (maxH: 200px)
   - Empty state support

3. **`modals/index.ts`**
   - Barrel export for clean imports

### Modified Files

**`ContextualToolbar.tsx`**
- Added modal state management (duplicateCombiner, pendingEntityReferences, isDuplicateModalOpen)
- Refactored `onManageTransformationNode()` to open modal instead of toast
- Added `createCombinerWithReferences()` helper function
- Added three modal handlers:
  - `handleUseExistingCombiner()`: Navigate to existing combiner
  - `handleCreateNewCombiner()`: Create despite duplicate
  - `handleCloseDuplicateModal()`: Clean up state
- Removed dependency on `DEFAULT_TOAST_OPTION`
- Added modal JSX wrapped in Fragment

**`translation.json`**
- Added `workspace.modal.duplicateCombiner` section with:
  - `title.combiner` and `title.assetMapper`
  - `description.combiner` and `description.assetMapper`
  - `existingMappings`, `noMappings`, `noDestination`
  - `mappingsCount` (pluralized)
  - `instructionsCount` (pluralized)
  - `prompt`
  - `actions.useExisting` and `actions.createNew`

---

## Testing

### Cypress Component Tests

**DuplicateCombinerModal.spec.cy.tsx** (11 tests)
- ✅ Renders modal with combiner information
- ✅ Renders asset mapper variant correctly
- ✅ Calls onClose when cancel button clicked
- ✅ Calls onClose when X button clicked
- ✅ Calls onUseExisting when "Use Existing" clicked
- ✅ Calls onCreateNew when "Create New Anyway" clicked
- ✅ Displays mappings count badge
- ✅ Shows empty state when no mappings
- ✅ Focuses "Use Existing" button on open
- ✅ Supports ESC key navigation
- ✅ Does not render when isOpen is false

**CombinerMappingsList.spec.cy.tsx** (4 tests)
- ✅ Renders empty state when no mappings
- ✅ Renders single mapping correctly
- ✅ Renders multiple mappings correctly
- ✅ Displays instruction count when present

**Total: 15 tests, 100% passing**

---

## User Experience Improvements

### Before (Toast)
- Brief notification with generic message
- Automatic dismissal after timeout
- No user choice presented
- Automatic navigation to existing combiner
- Limited information about the duplicate

### After (Modal)
- Persistent dialog requiring explicit action
- Shows combiner name prominently
- Lists all existing mappings with details
- Three clear action choices
- Visual feedback with animated canvas focus
- Better accessibility support

---

## Technical Highlights

1. **ReactFlow Integration**: Modal uses `fitView()` to animate camera to existing combiner node
2. **Modal Positioning**: Custom container props position modal off-center for better canvas visibility
3. **Type Safety**: Full TypeScript support with proper prop types
4. **i18n Support**: All text externalized to translation files with pluralization
5. **Reusable Components**: CombinerMappingsList can be reused elsewhere
6. **Test Coverage**: Comprehensive Cypress tests for all user interactions

---

## Data-TestId References

For E2E testing, the following test IDs are available:

**Modal**
- `duplicate-combiner-modal`
- `modal-title`
- `modal-combiner-name`
- `modal-description`
- `modal-mappings-label`
- `modal-prompt`
- `modal-close-button`
- `modal-button-cancel`
- `modal-button-create-new`
- `modal-button-use-existing`

**Mappings List**
- `mappings-list`
- `mappings-list-empty`
- `mappings-count-badge`
- `mapping-item-{id}`
- `mapping-source-{id}`
- `mapping-destination-{id}`
- `mapping-instructions-{id}`

---

## Next Steps

### Future Enhancements (Optional)
- Add "Don't show again for this session" checkbox
- Show visual diff between selected nodes and existing combiner sources
- Add "Edit Existing" button to open combiner editor directly
- Show creation date/last modified for existing combiner
- Add analytics to track user choices (use existing vs create new)

### Phase 3 Preparation
- E2E test integration in critical path
- Visual regression testing with Percy
- Performance monitoring for modal animation
- User feedback collection

---

## Files Summary

**Created:**
- `src/modules/Workspace/components/modals/DuplicateCombinerModal.tsx`
- `src/modules/Workspace/components/modals/CombinerMappingsList.tsx`
- `src/modules/Workspace/components/modals/index.ts`
- `src/modules/Workspace/components/modals/DuplicateCombinerModal.spec.cy.tsx`
- `src/modules/Workspace/components/modals/CombinerMappingsList.spec.cy.tsx`

**Modified:**
- `src/modules/Workspace/components/nodes/ContextualToolbar.tsx`
- `src/locales/en/translation.json`

**Tests Added:** 15 Cypress component tests

---

## Design Review Summary

As a senior designer perspective, the implementation includes these key improvements:

1. **Progressive Disclosure**: Information is layered appropriately - title first, then details, then actions
2. **Visual Hierarchy**: Icon, spacing, and typography guide the user's eye through the content
3. **Action Clarity**: Button styling clearly indicates recommended action (primary blue for "Use Existing")
4. **Context Preservation**: Canvas remains visible under modal, maintaining spatial awareness
5. **Feedback Loop**: Animated focus to existing node provides immediate visual confirmation
6. **Escape Hatches**: Multiple ways to dismiss (Cancel, X, ESC key) respect user agency
7. **Accessibility First**: Focus management, keyboard navigation, and ARIA labels included from start
8. **Content Flexibility**: Handles empty state, single item, and many items gracefully

---

## Guidelines Established

This task resulted in the creation of two critical guideline documents:

1. **Design Guidelines** ([.tasks/DESIGN_GUIDELINES.md](../.tasks/DESIGN_GUIDELINES.md))
   - Button variant rules: `variant="primary"` instead of `colorScheme`
   - Button hierarchy patterns in modals and forms
   - Consistency and theming requirements

2. **Testing Guidelines** ([.tasks/TESTING_GUIDELINES.md](../.tasks/TESTING_GUIDELINES.md))
   - MANDATORY: Every component test must include `"should be accessible"` test
   - Must use `cy.injectAxe()` and `cy.checkAccessibility()`
   - Optional screenshot capture for PR documentation

---

**Status:** Ready for integration into E2E critical path testing

