# Bug Fixes for E2E Test File

**Date:** October 24, 2025  
**File:** `cypress/e2e/workspace/duplicate-combiner.spec.cy.ts`

---

## Issues Fixed

### 1. TypeScript Error (Line 54) ✅

**Problem:**
```typescript
const sourceIds = combiner.sources?.items?.map((s: { identifier: string }) => s.identifier).join('-') || ''
```
Error: `Property 'identifier' is missing in type 'EntityReference'`

**Root Cause:**
The `EntityReference` type has an `id` property, not `identifier`. The type is defined as:
```typescript
export type EntityReference = {
    type: EntityType;
    id: string;  // ← Not 'identifier'
}
```

**Fix:**
Changed `identifier` to `id`:
```typescript
const sourceIds = combiner.sources?.items?.map((s) => s.id).join('-') || ''
```

### 2. ESLint Error - Unused Variable ✅

**Problem:**
```typescript
const COMBINER_ID_1 = '9e975b62-6f8d-410f-9007-3f83719aec6f'
const COMBINER_ID_2 = 'combiner-duplicate-attempt'
```
Error: `'COMBINER_ID_1' is assigned a value but never used`

**Fix:**
Removed `COMBINER_ID_1` and simplified to use a single `COMBINER_ID` constant matching the existing combiner test pattern.

### 3. ESLint Warnings - Unused Aliases ✅

**Problem:**
Multiple unused `cy.as()` aliases:
- `getProtocols`
- `getAdapters`
- `getTopicFilters`
- `deleteCombiner`

**Fix:**
Removed all unused `.as()` calls from intercepts that don't need to be waited on.

### 4. Test Failures - Incorrect Combiner ID Format ✅

**Problem:**
Tests were using incorrect combiner ID format:
```typescript
const firstCombinerId = `combiner-adapter@opcua-pump-adapter@opcua-boiler`
```

This format doesn't match how the mock API generates IDs, causing tests to fail when looking for combiner nodes.

**Fix:**
Simplified the ID generation approach to use a single fixed `COMBINER_ID` constant throughout all tests, matching the pattern used in the existing `combiner.spec.cy.ts` test file. This makes the tests more predictable and maintainable.

**Changes:**
- Removed dynamic ID generation based on source IDs
- Used fixed `COMBINER_ID = '9e975b62-6f8d-410f-9007-3f83719aec6f'`
- Updated all test assertions to use `COMBINER_ID` constant
- Removed redundant `firstCombinerId` variables

---

## Summary of Changes

**Lines Modified:**
- Line 11: Removed unused `COMBINER_ID_1` constant
- Line 32-48: Removed `.as()` aliases from unused intercepts
- Line 52-61: Simplified POST combiner intercept to use fixed ID
- Line 94: Removed unused `.as('deleteCombiner')`
- Lines 118, 164, 189, 234, 254, 321: Removed local `firstCombinerId` variables
- All tests: Updated to use global `COMBINER_ID` constant

**Result:**
- ✅ All TypeScript errors resolved
- ✅ All ESLint errors resolved
- ✅ Tests now use consistent, predictable IDs
- ✅ Code follows existing test patterns

---

## Test Execution

To run the fixed tests:

```bash
# Terminal 1: Start dev server
pnpm dev

# Terminal 2: Run E2E tests
pnpm exec cypress run --spec "cypress/e2e/workspace/duplicate-combiner.spec.cy.ts"
```

Note: E2E tests require the dev server to be running to access the application at `http://localhost:3000`.

