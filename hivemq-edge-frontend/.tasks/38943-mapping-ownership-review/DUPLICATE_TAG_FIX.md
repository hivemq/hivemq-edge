# Duplicate Tag Name Support - Bug Fixes and Tests

**Task:** 38943-mapping-ownership-review
**Date:** 2026-02-10
**Status:** ✅ Complete - All tests passing

---

## Executive Summary

Fixed critical bugs preventing tags with the same name from different adapters from coexisting in the frontend. The Option H architecture was designed to support this via the `scope` field, but three deduplication bugs prevented it from working.

**SonarQube Analysis:** ✅ Clean - https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge&pullRequest=1386

---

## Critical Bugs Fixed

### Bug 1: CombinedEntitySelect Deduplication (CRITICAL)

**File:** `src/modules/Mappings/combiner/CombinedEntitySelect.tsx:87-93`

**Problem:**

```typescript
// ❌ OLD - Only checked value and type
const isAlreadyIn = acc.find((item) => item.value === current.value && item.type === current.type)
```

When two adapters had tags with the same name:

- Tag "temperature" from modbus-adapter: `{value: "temperature", type: "TAG", adapterId: "modbus-adapter"}`
- Tag "temperature" from opcua-adapter: `{value: "temperature", type: "TAG", adapterId: "opcua-adapter"}`

The second tag was filtered out, even though it had a different `adapterId`.

**Fix:**

```typescript
// ✅ NEW - Also check adapterId (scope)
const isAlreadyIn = acc.find(
  (item) => item.value === current.value && item.type === current.type && item.adapterId === current.adapterId
)
```

**Impact:** Tags with same name from different adapters now appear as separate options in the dropdown.

---

### Bug 2: React-Select Uniqueness (CRITICAL)

**File:** `src/modules/Mappings/combiner/CombinedEntitySelect.tsx:131-146`

**Problem:**

react-select uses the `value` field by default to determine option uniqueness. Even after fixing Bug 1, react-select was still deduplicating options with the same `value` but different `adapterId`.

**Fix:**

```typescript
<Select
  // ...other props
  // ✅ Make options unique by combining value + adapterId + type
  getOptionValue={(option) => `${option.value}@${option.adapterId || 'null'}@${option.type}`}
  onChange={(newValue) => {
    if (newValue) onChange(newValue)
  }}
/>
```

**Impact:** react-select now treats tags with same name but different adapters as distinct options.

---

### Bug 3: getFilteredDataReferences Deduplication (CRITICAL)

**File:** `src/modules/Mappings/utils/combining.utils.ts:114-121`

**Problem:**

```typescript
// ❌ OLD - Only checked id and type
const isAlreadyIn = acc.find((item) => item.id === current.id && item.type === current.type)
```

This affected `CombinedSchemaLoader`, which uses `getFilteredDataReferences` to determine which schemas to load.

**Fix:**

```typescript
// ✅ NEW - Also check scope
const isAlreadyIn = acc.find(
  (item) => item.id === current.id && item.type === current.type && item.scope === current.scope
)
```

**Impact:** CombinedSchemaLoader now loads schemas for tags with same name from different adapters.

---

## Test Results

### Test 1: CombinedEntitySelect - Duplicate Names

**Command:**

```bash
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx"
```

**Results:**

```
CombinedEntitySelect
  ✓ should render properly (286ms)
  ✓ should handle duplicate tag names from different adapters (184ms)
  ✓ should be accessible (208ms)

3 passing (2s)
```

**New test verifies:**

- ✅ 4 options appear in dropdown (not 3) - duplicate names not deduplicated
- ✅ Both "temperature" tags visible (one from modbus, one from opcua)
- ✅ Different descriptions distinguish them ("Modbus temperature sensor" vs "OPC-UA temperature sensor")
- ✅ onClick passes correct `adapterId` in data structure
- ✅ Data structure maintains separate entries with different scopes

**Test code:**

```typescript
it('should handle duplicate tag names from different adapters', () => {
  const contextWithDuplicates: CombinerContext = {
    entityQueries: [
      {
        entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
        query: mockTagQuery([
          { name: 'temperature', description: 'Modbus temperature sensor' },
          { name: 'pressure', description: 'Modbus pressure sensor' },
        ]),
      },
      {
        entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
        query: mockTagQuery([
          { name: 'temperature', description: 'OPC-UA temperature sensor' },
          { name: 'humidity', description: 'OPC-UA humidity sensor' },
        ]),
      },
    ],
  }

  cy.mountWithProviders(<CombinedEntitySelect formContext={contextWithDuplicates} onChange={onChange} />)

  cy.get('#combiner-entity-select').click()

  // CRITICAL: Should have 4 options, not 3!
  cy.get('#react-select-entity-listbox').find('[role="option"]').should('have.length', 4)

  // Both temperature tags present
  cy.get('@options').filter(':contains("temperature")').should('have.length', 2)
  cy.get('@options').filter(':contains("Modbus temperature")').should('have.length', 1)
  cy.get('@options').filter(':contains("OPC-UA temperature")').should('have.length', 1)
})
```

---

### Test 2: DataCombiningEditorField - Race Condition

**Command:**

```bash
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx"
```

**Results:**

```
DataCombiningEditorField
  ✓ should render properly (164ms)
  ✓ should handle queries not loaded initially (race condition fix) (48ms)
  ✓ should handle queries loaded (race condition fix) (70ms)
  ✓ should be accessible (195ms)

4 passing (2s)
2 pending
```

**Tests verify:**

- ✅ Component handles queries not loaded (`data: undefined`, `isLoading: true`)
- ✅ Component handles queries loaded (`data: {...}`, `isLoading: false`)
- ✅ The `queriesAreLoaded` dependency triggers reconstruction correctly

---

### Test 3: Reconstruction Logic Unit Tests

**Command:**

```bash
pnpm vitest run src/modules/Mappings/utils/combining.utils.spec.ts
```

**Results:**

```
✓ src/modules/Mappings/utils/combining.utils.spec.ts (52 tests) 141ms

Test Files  1 passed (1)
     Tests  52 passed (52)
```

**Coverage:**

- ✅ 40 reconstruction tests (all 3 strategies)
- ✅ Edge cases (undefined, empty arrays)
- ✅ Real-world migration scenarios
- ✅ getFilteredDataReferences deduplication logic

---

### Test 4: TypeScript Validation

**Command:**

```bash
npx tsc -b
```

**Result:** ✅ No errors

---

## Known Limitation: UX Differentiation

### Current Behavior

Tags with the same name from different adapters:

- ✅ **Data structure:** Stored correctly with different `scope`
- ✅ **Dropdown:** Appear as separate options
- ✅ **Schemas:** Load separate schemas for each adapter
- ✅ **Selection:** Both can be selected simultaneously
- ⚠️ **Visual appearance:** Look identical in selected chips

**Example:**

When both "temperature" tags are selected, the multi-value display shows:

```
[temperature] [temperature]
```

Users cannot visually distinguish which adapter each belongs to.

### Why This Is Acceptable for Now

1. **Backend constraint:** Backend currently blocks duplicate names anyway
2. **Architecture ready:** Data structure fully supports duplicates
3. **Next task:** Will add visual differentiation (e.g., "temperature (modbus-adapter)")
4. **No data loss:** Internal state maintains correct scope information

### Recommended UX Improvement (Next Task)

**Option A: Show adapter in label**

```
[temperature (modbus-adapter)] [temperature (opcua-adapter)]
```

**Option B: Show adapter as badge**

```
[temperature 🔵modbus] [temperature 🟢opcua]
```

**Option C: Custom MultiValueContainer component**

```typescript
components={{
  MultiValueContainer: ({ children, data, ...props }) => (
    <>
      <PLCTag tagTitle={children} mr={3} />
      {data.adapterId && (
        <Badge ml={1} colorScheme="blue" fontSize="xs">
          {data.adapterId}
        </Badge>
      )}
    </>
  ),
}}
```

---

## Files Changed

| File                                   | Lines Changed | Type | Impact                                     |
| -------------------------------------- | ------------- | ---- | ------------------------------------------ |
| `CombinedEntitySelect.tsx`             | +3, -1        | Fix  | Deduplication now includes adapterId       |
| `CombinedEntitySelect.tsx`             | +2            | Fix  | react-select getOptionValue for uniqueness |
| `combining.utils.ts`                   | +3, -1        | Fix  | getFilteredDataReferences includes scope   |
| `CombinedEntitySelect.spec.cy.tsx`     | +57           | Test | New test for duplicate names               |
| `DataCombiningEditorField.spec.cy.tsx` | +74           | Test | Race condition tests                       |

**Total:** 5 files, ~140 lines added, 3 critical bugs fixed

---

## Verification Checklist

- [x] TypeScript compilation clean (`npx tsc -b`)
- [x] ESLint clean
- [x] Unit tests passing (52 tests)
- [x] Component tests passing (CombinedEntitySelect: 3 tests)
- [x] Component tests passing (DataCombiningEditorField: 4 tests)
- [x] SonarQube analysis clean
- [x] Manual verification: duplicate names appear in dropdown
- [x] Manual verification: both can be selected
- [x] Manual verification: schemas load for both

---

## Integration with Option H Architecture

These fixes complete the Option H implementation for duplicate tag support:

### Before (Broken)

```typescript
// Two adapters with "temperature" tag
entityQueries: [
  { entity: { id: 'modbus' }, query: { data: [{ name: 'temperature' }] } },
  { entity: { id: 'opcua' }, query: { data: [{ name: 'temperature' }] } },
]

// ❌ Result: Only 1 option in dropdown (second deduplicated)
// ❌ Schema loader: Only loads one schema
```

### After (Fixed)

```typescript
// Two adapters with "temperature" tag
entityQueries: [
  { entity: { id: 'modbus' }, query: { data: [{ name: 'temperature' }] } },
  { entity: { id: 'opcua' }, query: { data: [{ name: 'temperature' }] } },
]

// ✅ Result: 2 options in dropdown (both visible)
// ✅ Schema loader: Loads both schemas
// ✅ Data structure: [{id: 'temperature', scope: 'modbus'}, {id: 'temperature', scope: 'opcua'}]
```

---

## Backend Constraint

**Current limitation:** Backend still blocks duplicate tag names across adapters for on-premises customers.

**Why this fix matters:**

1. **Architecture ready:** When backend constraint lifts, frontend already supports it
2. **Testing:** Can test duplicate name handling even though backend blocks it
3. **Migration path:** Clear path to full duplicate support
4. **No regression:** Fix doesn't break existing single-adapter scenarios

---

## Related Documents

- **Option H Implementation:** `.tasks/38943-mapping-ownership-review/OPTION_H_CURRENT_IMPLEMENTATION.md`
- **Race Condition Fix:** `.tasks/38943-mapping-ownership-review/RACE_CONDITION_FIX.md`
- **Pull Request:** `.tasks/38943-mapping-ownership-review/PULL_REQUEST.md`
- **Decision Tree:** `.tasks/38943-mapping-ownership-review/DECISION_TREE.md`

---

**Generated:** 2026-02-10
**Status:** ✅ Complete - All tests passing
**Next Action:** Add visual differentiation for duplicate tag names in UI (separate task)
