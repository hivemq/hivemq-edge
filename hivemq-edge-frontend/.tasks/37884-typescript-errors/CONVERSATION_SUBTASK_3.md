# Subtask 3: Generic Type Constraint Issues - COMPLETE ✅

## Objective

Fix 3 errors where generic type `T` was not properly constrained to `Record<string, unknown>`.

## Errors to Fix

1. `src/extensions/datahub/hooks/useDataHubDraftStore.ts` (line 82)
2. `src/modules/Workspace/hooks/useWorkspaceStore.ts` (line 97)
3. `src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts` (line 22)

## Work Log

### Fix 1: useDataHubDraftStore.ts

**Error:** Type 'T' is not assignable to type 'Record<string, unknown>'
**Root cause:** `onUpdateNodes<T>` generic had no constraint

**Solution:** Added constraint to both interface definition AND implementation

```typescript
// In src/extensions/datahub/types.ts
onUpdateNodes: <T extends Record<string, unknown>>(item: string, data: T) => void

// In src/extensions/datahub/hooks/useDataHubDraftStore.ts
onUpdateNodes: <T extends Record<string, unknown>>(item: string, data: T) => {
  // implementation
}
```

### Fix 2: useWorkspaceStore.ts

**Error:** Type 'T' is not assignable to type 'Record<string, unknown>'
**Root cause:** `onUpdateNode<T>` generic had no constraint

**Solution:** Added constraint to both interface definition AND implementation

```typescript
// In src/modules/Workspace/types.ts
onUpdateNode: <T extends Record<string, unknown>>(id: string, data: T) => void

// In src/modules/Workspace/hooks/useWorkspaceStore.ts
onUpdateNode: <T extends Record<string, unknown>>(id: string, data: T) => {
  // implementation
}
```

### Fix 3: useGetDomainOntology.spec.ts

**Error:** Argument of type 'T' is not assignable to parameter of type 'DeeplyAllowMatchers<T>'
**Root cause:** Generic constraint incompatible with vitest's expect.objectContaining

**Solution:**

1. Added constraint to function: `<T extends Record<string, unknown>>`
2. Changed from fixed array to `expect.arrayContaining`
3. Added type assertion `as never` for vitest compatibility

```typescript
const successListOf = <T extends Record<string, unknown>>(payload: T) =>
  expect.objectContaining({
    data: expect.objectContaining({
      items: expect.arrayContaining([expect.objectContaining(payload as never)]),
    }),
    error: null,
    isSuccess: true,
    isError: false,
  })
```

**Note:** User corrected that the helper MUST validate structure `{ data: { items: [...payload] } }` - changed to use `expect.arrayContaining` to properly check array contents.

## Files Modified

1. `src/extensions/datahub/types.ts` - Added generic constraint to interface
2. `src/extensions/datahub/hooks/useDataHubDraftStore.ts` - Added generic constraint to implementation
3. `src/modules/Workspace/types.ts` - Added generic constraint to interface
4. `src/modules/Workspace/hooks/useWorkspaceStore.ts` - Added generic constraint to implementation
5. `src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts` - Fixed test helper with constraint and type assertion

## Key Learning

When adding generic constraints:

- Must update BOTH the interface/type definition AND the implementation
- The constraint prevents TypeScript from allowing unconstrained types to be assigned to structured types

---

**Status:** COMPLETED ✅  
**Errors fixed:** 3  
**Next:** Subtask 4
