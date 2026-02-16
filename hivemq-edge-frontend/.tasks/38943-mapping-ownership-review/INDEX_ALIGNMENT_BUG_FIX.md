# Index Alignment Bug Fix - getDataReference

**Task:** 38943-mapping-ownership-review
**Date:** 2026-02-10
**Status:** ✅ Complete - All tests passing

---

## Executive Summary

Fixed a critical index alignment bug in `getDataReference` function that would cause tags to receive incorrect or undefined scopes when `entityQueries` contained mixed entity types (ADAPTER, BRIDGE, PULSE_AGENT, etc.).

**Root Cause:** The function created a filtered `dataSources` array (only ADAPTER/EDGE_BROKER entities) but used indices from the unfiltered `entityQueries` iteration, causing misalignment when non-ADAPTER entities were present.

**Solution:** Eliminated the filtered `dataSources` array and used `entityQuery.entity.id` directly for scope assignment.

---

## The Bug

### Problem Description

In `src/modules/Mappings/utils/combining.utils.ts`, the `getDataReference` function had this pattern:

```typescript
// ❌ OLD CODE - BUGGY
const dataSources = formContext.entityQueries
  .map((eq) => eq.entity)
  .filter((e) => e.type === EntityType.ADAPTER || e.type === EntityType.EDGE_BROKER)

return formContext.entityQueries.reduce<DataReference[]>((acc, entityQuery, currentIndex) => {
  // ... processing ...
  const tagDataReferences = (items as DomainTag[]).map<DataReference>((tag) => ({
    id: tag.name,
    type: DataIdentifierReference.type.TAG,
    scope: dataSources[currentIndex]?.id, // ❌ BUG: currentIndex from unfiltered array!
  }))
})
```

### Example Scenario Where It Fails

**Input:**

```typescript
entityQueries = [
  { entity: { id: 'modbus-adapter', type: 'ADAPTER' }, query: { data: [tag1] } }, // index 0
  { entity: { id: 'bridge-1', type: 'BRIDGE' }, query: { data: [tag2] } }, // index 1
  { entity: { id: 'opcua-adapter', type: 'ADAPTER' }, query: { data: [tag3] } }, // index 2
]
```

**After filtering:**

```typescript
dataSources = [
  { id: 'modbus-adapter', type: 'ADAPTER' }, // index 0
  { id: 'opcua-adapter', type: 'ADAPTER' }, // index 1 (NOT index 2!)
]
```

**Result:**

- When processing `entityQuery[0]` (modbus): `dataSources[0]` = ✅ 'modbus-adapter' (correct)
- When processing `entityQuery[1]` (bridge): `dataSources[1]` = ❌ 'opcua-adapter' (WRONG! Should be null)
- When processing `entityQuery[2]` (opcua): `dataSources[2]` = ❌ undefined (WRONG! Should be 'opcua-adapter')

**Impact:** Tags get assigned to the wrong adapter or undefined scope, breaking schema loading and ownership tracking.

---

## The Fix

### Solution

Removed the filtered `dataSources` array entirely and computed scope directly from the current `entityQuery`:

```typescript
// ✅ NEW CODE - FIXED
return formContext.entityQueries.reduce<DataReference[]>((acc, entityQuery) => {
  const { entity, query } = entityQuery
  const items = query.data?.items || []
  if (!items.length) return acc

  const firstItem = items[0]

  if ((firstItem as DomainTag).name) {
    // For tags, use entity.id as scope (only for ADAPTER/EDGE_BROKER types)
    const scope = entity.type === EntityType.ADAPTER || entity.type === EntityType.EDGE_BROKER ? entity.id : null
    const tagDataReferences = (items as DomainTag[]).map<DataReference>((tag) => ({
      id: tag.name,
      type: DataIdentifierReference.type.TAG,
      scope, // ✅ Direct from entityQuery.entity
    }))
    acc.push(...tagDataReferences)
  }
  // ... rest of implementation
})
```

### Key Changes

1. **Removed filtered array**: No more `dataSources` array creation
2. **Direct entity access**: `const { entity, query } = entityQuery` destructures the current item
3. **Conditional scope**: Computes scope inline based on entity type
4. **No index dependency**: No reliance on array indices for lookups

---

## Test Coverage

Added comprehensive test suite with 9 new tests for `getDataReference`:

### Test Cases

1. **Empty context** - Returns empty array
2. **Context without entityQueries** - Returns empty array
3. **ADAPTER entities with tags** - Scope = adapter ID
4. **EDGE_BROKER entities with tags** - Scope = broker ID
5. **BRIDGE entities with tags** - Scope = null
6. **Mixed entity types** ⭐ (Critical test) - Verifies no index misalignment
7. **Topic filters** - Scope always null
8. **Mixed tags and topic filters** - Handles both correctly
9. **Empty data** - Skips entities with no data

### Critical Test - Mixed Entity Types

```typescript
it('should correctly handle mixed entity types without index misalignment', () => {
  const context: CombinerContext = {
    entityQueries: [
      {
        entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
        query: mockTagQuery([{ name: 'modbus-temp' }]),
      },
      {
        entity: { id: 'bridge-1', type: EntityType.BRIDGE },
        query: mockTagQuery([{ name: 'bridge-tag' }]),
      },
      {
        entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
        query: mockTagQuery([{ name: 'opcua-temp' }]),
      },
    ],
  }

  const result = getDataReference(context)

  expect(result[0]).toEqual({
    id: 'modbus-temp',
    type: DataIdentifierReference.type.TAG,
    scope: 'modbus-adapter', // ✅ Correct
  })
  expect(result[1]).toEqual({
    id: 'bridge-tag',
    type: DataIdentifierReference.type.TAG,
    scope: null, // ✅ Bridge gets null
  })
  expect(result[2]).toEqual({
    id: 'opcua-temp',
    type: DataIdentifierReference.type.TAG,
    scope: 'opcua-adapter', // ✅ Correct (would be undefined with old code!)
  })
})
```

**This test would have FAILED with the old code** - `opcua-temp` would get `undefined` scope.

---

## Test Results

### Unit Tests

```bash
pnpm vitest run src/modules/Mappings/utils/combining.utils.spec.ts
```

**Result:** ✅ 61 tests passed (52 existing + 9 new)

```
✓ src/modules/Mappings/utils/combining.utils.spec.ts (61 tests) 144ms

Test Files  1 passed (1)
     Tests  61 passed (61)
```

### Component Tests

```bash
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx"
```

**Result:** ✅ 3 tests passed

```
CombinedEntitySelect
  ✓ should render properly (311ms)
  ✓ should handle duplicate tag names from different adapters (187ms)
  ✓ should be accessible (219ms)
```

### TypeScript Compilation

```bash
npx tsc -b --force
```

**Result:** ✅ No errors

---

## Files Changed

| File                      | Lines Changed | Type | Impact                      |
| ------------------------- | ------------- | ---- | --------------------------- |
| `combining.utils.ts`      | -7, +10       | Fix  | Fixed index alignment bug   |
| `combining.utils.spec.ts` | +234          | Test | Added 9 comprehensive tests |

**Total:** 2 files, ~240 lines added/changed

---

## Backward Compatibility

✅ **No breaking changes**

- The function signature remains unchanged
- Old code paths (backward compatibility) are untouched
- All existing tests continue to pass
- Only the `entityQueries` path was modified

---

## Related Issues

### Similar Patterns in Codebase

The `getCombinedDataEntityReference` function (lines 74-105) has a similar pattern but is part of the backward compatibility layer:

```typescript
// This function also uses index-based lookup
const dataSources = entities.filter((e) => e.type === EntityType.ADAPTER || e.type === EntityType.EDGE_BROKER)
return content.reduce<DataReference[]>((acc, cur, currentIndex) => {
  // ...
  scope: dataSources?.[currentIndex]?.id,
})
```

**Status:** Not fixed - this is in the deprecated backward compatibility path. The new `entityQueries` structure (fixed in this PR) is the recommended path forward.

**Recommendation:** Document that this function assumes `content` and `entities` arrays are aligned (i.e., no filtering should happen before calling this function).

---

## Integration with Option H Architecture

This fix is critical for Option H architecture to work correctly:

### Before (Broken)

```typescript
// User has: modbus-adapter, bridge-1, opcua-adapter
// Bridge in the middle causes index misalignment
entityQueries = [modbus, bridge, opcua]
dataSources = [modbus, opcua] // Filtered

// ❌ Result:
// modbus tags → scope: 'modbus-adapter' ✅
// bridge tags → scope: 'opcua-adapter' ❌ WRONG!
// opcua tags  → scope: undefined ❌ WRONG!
```

### After (Fixed)

```typescript
// User has: modbus-adapter, bridge-1, opcua-adapter
entityQueries = [modbus, bridge, opcua]

// ✅ Result:
// modbus tags → scope: 'modbus-adapter' ✅
// bridge tags → scope: null ✅ (bridges don't own tags)
// opcua tags  → scope: 'opcua-adapter' ✅
```

---

## Edge Cases Handled

1. **Empty entityQueries** - Returns empty array
2. **Entity with no data** - Skips gracefully
3. **BRIDGE entities** - Assigns null scope (correct behavior)
4. **PULSE_AGENT entities** - Assigns null scope (correct behavior)
5. **EDGE_BROKER entities** - Assigns broker ID as scope (correct behavior)
6. **Mixed content types** - Handles tags and topic filters in same list
7. **Multiple adapters** - Each gets its own scope correctly

---

## Verification Checklist

- [x] TypeScript compilation clean
- [x] All existing unit tests pass (52 tests)
- [x] New unit tests added (9 tests)
- [x] All component tests pass (3 tests)
- [x] Critical test for mixed entity types passes
- [x] No breaking changes to API
- [x] Backward compatibility maintained
- [x] Documentation updated

---

## Related Documents

- **Test Coverage Analysis:** `.tasks/38943-mapping-ownership-review/TEST_COVERAGE_ANALYSIS.md`
- **Duplicate Tag Fix:** `.tasks/38943-mapping-ownership-review/DUPLICATE_TAG_FIX.md`
- **Option H Implementation:** `.tasks/38943-mapping-ownership-review/OPTION_H_CURRENT_IMPLEMENTATION.md`
- **Pull Request:** `.tasks/38943-mapping-ownership-review/PULL_REQUEST.md`

---

**Generated:** 2026-02-10
**Status:** ✅ Complete - All tests passing
**Priority:** Critical - Prevents data corruption and ownership misalignment
