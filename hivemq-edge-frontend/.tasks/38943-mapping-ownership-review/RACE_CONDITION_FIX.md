# Race Condition Fix: Query Data Availability

**Issue:** Reconstruction Strategy 3 (context lookup) fails when queries haven't loaded yet
**File:** `src/modules/Mappings/combiner/DataCombiningEditorField.tsx`
**Reported by:** Code review
**Date:** 2026-02-11

---

## Problem Description

### Current Behavior (Race Condition)

```typescript
useEffect(() => {
  if (!formData) {
    setSelectedSources({ tags: [], topicFilters: [] })
    return
  }

  const reconstructed = reconstructSelectedSources(formData, formContext)
  setSelectedSources(reconstructed)
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [formData?.id]) // ❌ Only runs when mapping ID changes
```

**Race condition timeline:**

1. **T=0ms**: Component mounts, `formData.id = "mapping-1"`
2. **T=1ms**: useEffect runs, calls `reconstructSelectedSources()`
3. **T=1ms**: Strategy 1 (primary) - checks, no match
4. **T=1ms**: Strategy 2 (instructions) - checks, no match
5. **T=1ms**: Strategy 3 (context) - tries to look up in `formContext.entityQueries[].query.data`
6. **T=1ms**: ⚠️ **`query.data` is `undefined`** (queries still loading)
7. **T=1ms**: Returns `scope: null` (fallback)
8. **T=100ms**: Queries resolve, `query.data` becomes available
9. **T=100ms**: `formContext` updates (via `dataUpdatedAt` dependency in parent)
10. **T=100ms**: ❌ **useEffect doesn't re-run** (formData.id unchanged)
11. **Result**: Scope remains `null` forever despite query data being available

### Why It Happens

- **Initial load**: Queries are async and resolve after first render
- **useEffect dependency**: Only watches `formData?.id`, ignores `formContext`
- **Intentional ignore**: `formContext` is ignored to prevent cascade re-renders on every query update
- **Side effect**: Also prevents reconstruction when queries first become available

### Impact

**Severity:** 🟡 Medium

- Tags get `scope: null` instead of correct adapter ID
- Only affects mappings where:
  - Primary doesn't match the tag
  - Instructions don't contain the tag
  - Only Strategy 3 (context lookup) would work
- User can still see/edit mappings, but validation may fail

**Affected scenarios:**

- Loading existing mappings where ownership must be inferred from context
- Tags not in instructions but available in loaded queries
- Rare but possible edge case

---

## Solution

### Approach: Smart Query Availability Detection (Functional setState)

Add a second condition to trigger reconstruction: when queries transition from "not loaded" to "loaded"

```typescript
import { useEffect, useMemo, useState } from 'react'

const [selectedSources, setSelectedSources] = useState<SelectedSources | undefined>(undefined)

// Detect if queries are currently loaded
const queriesAreLoaded = useMemo(() => {
  if (!formContext?.entityQueries?.length) return false

  // Check if at least one query has data (indicating queries have resolved)
  return formContext.entityQueries.some((eq) => eq.query.data !== undefined)
}, [formContext?.entityQueries])

useEffect(() => {
  if (!formData) {
    setSelectedSources({ tags: [], topicFilters: [] })
    return
  }

  // Use functional update form (React best practice)
  // Reconstruct when:
  // 1. Mapping ID changed (editing different mapping), OR
  // 2. Queries became available (transitioned from not-loaded to loaded)
  setSelectedSources(() => reconstructSelectedSources(formData, formContext))

  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [formData?.id, queriesAreLoaded])
// ^ Now depends on both mapping ID and query availability
```

### Why This Works

**Prevents cascade:**

- Only re-runs when `queriesAreLoaded` **changes** (boolean flip)
- NOT on every query data update (dataUpdatedAt changes)
- `queriesAreLoaded` flips from `false` → `true` once, then stays `true`

**Fixes race condition:**

- Detects transition from "queries not loaded" to "queries loaded"
- Triggers reconstruction when data becomes available
- Uses functional setState (idiomatic React pattern, no ref needed)

**Maintains isolation:**

- Still only reconstructs when switching mappings (formData.id changes)
- Doesn't cascade on every formContext update
- Per-mapping state remains isolated

---

## Alternative Solutions Considered

### Alternative 1: Add dataUpdatedAt to dependencies

```typescript
}, [formData?.id, ...formContext?.entityQueries?.map(eq => eq.query.dataUpdatedAt) || []])
```

**Problem:** ❌ Cascades on every query update

- Queries refetch periodically
- Would reconstruct constantly
- Defeats purpose of per-mapping state isolation

### Alternative 2: Reconstruct on every formContext change

```typescript
}, [formData?.id, formContext])
```

**Problem:** ❌ Massive cascade

- formContext updates on every query poll
- Would reconstruct dozens of times
- Performance impact

### Alternative 3: Retry logic in reconstructSelectedSources

```typescript
export const reconstructSelectedSources = (
  formData?: DataCombining,
  formContext?: CombinerContext,
  retryOnEmpty?: boolean
): { tags: DataIdentifierReference[]; topicFilters: DataIdentifierReference[] } => {
  // ... reconstruction logic ...

  // If all scopes are null and queries exist but aren't loaded, mark for retry
  if (retryOnEmpty && allScopesNull && queriesExistButNotLoaded) {
    // Schedule retry somehow?
  }
}
```

**Problem:** ❌ Complex state management

- Needs external retry mechanism
- Hard to coordinate with React lifecycle
- Overcomplicated solution

### Alternative 4: Use separate effect for query loading

```typescript
// First effect: reconstruct on mapping change
useEffect(() => {
  // ...
}, [formData?.id])

// Second effect: re-reconstruct if queries weren't ready
useEffect(() => {
  if (queriesJustBecameAvailable) {
    const reconstructed = reconstructSelectedSources(formData, formContext)
    setSelectedSources(reconstructed)
  }
}, [queriesAreLoaded])
```

**Problem:** ⚠️ Duplicated logic

- Two effects doing similar things
- Harder to reason about
- But could work

---

## Recommended Solution

**Use the smart detection approach** (shown in Solution section above)

**Pros:**

- ✅ Fixes race condition
- ✅ Prevents cascade
- ✅ Single effect (easier to maintain)
- ✅ Uses ref to avoid re-render loops
- ✅ Clear intent with comments

**Cons:**

- ⚠️ Slightly more complex than current code
- ⚠️ Adds ~10 lines

---

## Testing Strategy

### Unit Tests

Add test cases to `DataCombiningEditorField.spec.cy.tsx`:

```typescript
describe('DataCombiningEditorField - Query Loading Race Condition', () => {
  it('should reconstruct with null scopes when queries not loaded', () => {
    // Render with formContext where query.data is undefined
    const formContext: CombinerContext = {
      entityQueries: [{
        entity: { id: 'adapter1', type: EntityType.ADAPTER },
        query: { data: undefined, isLoading: true } // Simulates loading state
      }]
    }

    // Mount component
    cy.mountWithProviders(
      <DataCombiningEditorField
        formData={mappingData}
        formContext={formContext}
      />
    )

    // Verify scopes are null initially
    // ... assertions
  })

  it('should re-reconstruct when queries become available', () => {
    // Mount with queries not loaded
    const { rerender } = render(...)

    // Verify initial state has null scopes
    // ... assertions

    // Update formContext with loaded queries
    rerender(
      <DataCombiningEditorField
        formData={mappingData}
        formContext={formContextWithLoadedQueries}
      />
    )

    // Verify scopes are now populated from query data
    // ... assertions
  })

  it('should not cascade reconstruct on every query update', () => {
    const reconstructSpy = cy.spy()

    // Mount and wait for queries to load
    // ... setup

    // Update query data multiple times (simulating refetch)
    // ... trigger updates

    // Verify reconstruct was only called once (initial + when loaded)
    // NOT on every update
    expect(reconstructSpy).to.have.callCount(2) // Once on mount, once when loaded
  })
})
```

### Integration Test

Add to `DataCombiningEditorDrawer.backward-compat.spec.cy.tsx`:

```typescript
it('should handle async query loading for old mappings', () => {
  // Create mapping with tags that require context lookup (no instructions)
  const oldMapping = {
    id: 'mapping-1',
    sources: {
      primary: { id: 'tag1', type: 'TAG' },
      tags: ['tag1', 'tag2'], // tag2 needs context lookup
      topicFilters: [],
    },
    instructions: [
      // Only tag1 in instructions, tag2 missing
      { sourceRef: { id: 'tag1', type: 'TAG', scope: 'adapter1' }, destination: 'dest1' },
    ],
  }

  // Open mapping editor
  // Verify tag2 initially has null scope (queries not loaded)
  // Wait for queries to load
  // Verify tag2 now has correct scope from context
})
```

### Manual Test

1. Create a mapping with multiple tags
2. Save the mapping without all tags in instructions (edge case)
3. Close and reopen the mapping editor
4. Open browser DevTools → Network tab → Slow 3G throttling
5. Observe tag scopes populate after queries load (not immediately)

---

## Estimated Effort

- **Implementation**: 30 minutes (add smart detection logic)
- **Testing**: 1 hour (add unit tests + integration test)
- **Review**: 15 minutes
- **Total**: ~2 hours

---

## Priority

**🟡 Medium Priority**

**Reasoning:**

- Not a showstopper (mappings still work)
- Only affects specific edge case (Strategy 3 fallback needed)
- Can be addressed in next sprint
- Should fix before GA release

**When to fix:**

- Before Option H → A migration (ensures clean baseline)
- Include in next mapping-related PR
- Or create small follow-up PR

---

## Implementation Checklist

- [ ] Add `queriesWereLoadedRef` ref to track previous state
- [ ] Add `queriesAreLoaded` computed value
- [ ] Update useEffect dependencies to include `queriesAreLoaded`
- [ ] Add conditional logic for query transition detection
- [ ] Update ESLint disable comment to explain both conditions
- [ ] Add unit tests for race condition
- [ ] Add integration test for async loading
- [ ] Update PR documentation with fix
- [ ] Manual test with network throttling

---

## Documentation Updates

Update these files:

1. **PRE_REVIEW_REPORT_SKILL.md** - Add to "Suggestions" section
2. **PULL_REQUEST.md** - Mention in "Known Limitations" (if not fixed)
3. **OPTION_H_CURRENT_IMPLEMENTATION.md** - Document race condition and fix

---

**Generated:** 2026-02-11
**Reported by:** Code review
**Status:** Documented, not yet implemented
**Next Action:** Implement fix in DataCombiningEditorField.tsx
