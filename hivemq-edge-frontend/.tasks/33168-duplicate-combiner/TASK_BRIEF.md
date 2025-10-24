# Task 33168: Duplicate Combiner Detection & ContextualToolbar Refactoring

**Task ID:** 33168  
**Task Name:** duplicate-combiner  
**Status:** Active  
**Started:** October 23, 2025  
**Last Updated:** October 23, 2025

---

## Objective

This task has two main objectives:

1. **Improve User Experience**: Enhance the detection and handling of existing combiners when users attempt to create a new one with the same sources
2. **Refactor ContextualToolbar**: Simplify the component logic to enable better coordination between Vitest unit tests and Cypress component tests

---

## Context

The `ContextualToolbar` component (`src/modules/Workspace/components/nodes/ContextualToolbar.tsx`) currently handles the creation of combiners and asset mappers from selected nodes. When attempting to create a combiner that already exists (with the same source connections), the system detects this and shows a toast notification, then navigates to the existing combiner.

### Current Behavior

- Users can select multiple nodes and create a combiner
- System checks if a combiner with identical sources already exists
- If found, shows an info toast and navigates to the existing combiner
- The logic is complex and tightly coupled, making testing difficult

### Issues to Address

1. **UX Improvements Needed:**
   - Better visual feedback when duplicate is detected
   - Clearer messaging about why creation was prevented
   - Potentially offer choice to edit existing or create new with modifications

2. **Refactoring Needs:**
   - Extract complex logic into testable utility functions
   - Separate concerns (combiner detection, creation, navigation)
   - Enable easier unit testing with Vitest
   - Enable component testing with Cypress

---

## Technical Details

### Key Component

**File:** `src/modules/Workspace/components/nodes/ContextualToolbar.tsx`

**Key Functions:**
- `onManageTransformationNode()` - Handles combiner/asset mapper creation
- Duplicate detection using `arrayWithSameObjects` utility
- Toast notifications for various states
- Navigation to existing combiners

### Related Files

- `src/modules/Workspace/utils/combiner.utils.ts` - Contains `arrayWithSameObjects` utility
- `src/api/hooks/useCombiners.ts` - API hooks for combiner creation
- `src/api/hooks/useAssetMapper.ts` - API hooks for asset mapper creation

---

## Success Criteria

1. **UX Improvements:**
   - [ ] Enhanced visual feedback for duplicate detection
   - [ ] Improved toast messaging
   - [ ] Clear user guidance on next steps

2. **Refactoring:**
   - [ ] Extract logic into testable utility functions
   - [ ] Separate concerns (detection, creation, navigation)
   - [ ] Add comprehensive Vitest unit tests
   - [ ] Add Cypress component tests
   - [ ] Maintain existing functionality

3. **Testing:**
   - [ ] 100% coverage of new utility functions
   - [ ] Component tests covering all interaction paths
   - [ ] Integration tests for duplicate detection flow

---

## Approach

We will tackle this task in subtasks:

1. **Analysis & Planning:** Review current implementation and plan refactoring strategy
2. **Extract Logic:** Move complex logic to testable utility functions
3. **Improve UX:** Enhance duplicate detection feedback and messaging
4. **Add Tests:** Create comprehensive test coverage
5. **Integration:** Ensure everything works together smoothly

---

## Notes

- This refactoring should maintain backward compatibility
- All existing functionality must continue to work
- Focus on testability without over-engineering
- Consider future extensibility

---

**Related Tasks:**
- 37542-code-coverage (improving test coverage across the codebase)

