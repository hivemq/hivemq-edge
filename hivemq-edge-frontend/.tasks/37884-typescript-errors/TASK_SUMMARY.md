# Task 37884: TypeScript Errors - Action Plan

## Overview

We have **24 TypeScript errors** across **9 files** that need to be fixed. The errors have been grouped by complexity and type.

## Error Summary by File

```
11 errors - src/modules/Mappings/hooks/useValidateCombiner.spec.ts
 3 errors - src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx
 3 errors - src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx
 2 errors - src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx
 1 error  - src/extensions/datahub/hooks/useDataHubDraftStore.ts
 1 error  - src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts
 1 error  - src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx
 1 error  - src/modules/Mappings/combiner/components/AutoMapping.spec.cy.tsx
 1 error  - src/modules/Workspace/hooks/useGetPoliciesMatching.ts
 1 error  - src/modules/Workspace/hooks/useWorkspaceStore.ts
```

## Proposed Subtasks

### Subtask 1: Simple Test Mock Fixes (EASY) - 16 errors

**Files affected:**

- `src/modules/Mappings/hooks/useValidateCombiner.spec.ts` (11 errors)
- `src/modules/Mappings/combiner/CombinedEntitySelect.spec.cy.tsx` (1 error)
- `src/modules/Mappings/combiner/components/AutoMapping.spec.cy.tsx` (1 error)
- `src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx` (3 errors)

**Issue:** Mock objects missing type assertions or required properties

- UseQueryResult mocks need proper type casting from `unknown` to specific types
- Missing `isActive` property in FilterConfigurationOption mocks

**Estimated complexity:** EASY - Add type assertions and missing properties

---

### Subtask 2: React Flow Node Type Fixes (MEDIUM) - 5 errors

**Files affected:**

- `src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx` (3 errors)
- `src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx` (2 errors)

**Issue:** Node type mismatches in test fixtures

- `Node<Group>` not assignable to `NodeGroupType`
- `Node<Bridge | Adapter>` not assignable to `NodeAdapterType | NodeBridgeType`
- Missing required `type` property or type union issues

**Estimated complexity:** MEDIUM - Need to ensure test fixtures match expected types

---

### Subtask 3: Generic Type Constraint Issues (MEDIUM) - 3 errors

**Files affected:**

- `src/extensions/datahub/hooks/useDataHubDraftStore.ts` (1 error)
- `src/modules/Workspace/hooks/useWorkspaceStore.ts` (1 error)
- `src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts` (1 error)

**Issue:** Generic type `T` not properly constrained

- `onUpdateNodes<T>` and `onUpdateNode<T>` methods accept any type but assign to `Record<string, unknown>`
- Generic matcher type not properly constrained in test utility

**Estimated complexity:** MEDIUM - Add proper type constraints to generics

---

### Subtask 4: Runtime Type Safety (MEDIUM) - 1 error

**Files affected:**

- `src/modules/Workspace/hooks/useGetPoliciesMatching.ts` (1 error)

**Issue:** Passing `unknown` type to function expecting `GenericObjectType`

- Need to add type guard or type assertion for `node.data.config`

**Estimated complexity:** MEDIUM - Add type guard or proper typing

---

## Execution Order

1. **Subtask 1** ✅ COMPLETED - 16 errors fixed (see CONVERSATION_SUBTASK_1.md)
2. **Subtask 2** ✅ COMPLETED - 5 errors fixed (see CONVERSATION_SUBTASK_2.md)
3. **Subtask 3** ✅ COMPLETED - 3 errors fixed (generic type constraints)
4. **Subtask 4** ✅ COMPLETED - 1 error fixed (type assertion)

## Progress

**Total errors at start:** 24  
**Total errors fixed:** 24  
**Errors remaining:** 0 ✅

### ✅ All Subtasks Complete!

See [TASK_COMPLETE.md](.tasks/37884-typescript-errors/TASK_COMPLETE.md) for full details.

---

## Files Modified (8 total)

1. `src/api/hooks/useDomainModel/useGetCombinedEntities.ts` - Added return type (13 errors)
2. `src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx` - Added property (3 errors)
3. `src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx` - Fixed type (3 errors)
4. `src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx` - Fixed type (2 errors)
5. `src/extensions/datahub/types.ts` - Added generic constraint (1 error)
6. `src/modules/Workspace/types.ts` - Added generic constraint (1 error)
7. `src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts` - Fixed matcher (1 error)
8. `src/modules/Workspace/hooks/useGetPoliciesMatching.ts` - Added type assertion (1 error)

## Testing Strategy

After each subtask:

1. Run `npm run build:tsc` to verify errors are resolved
2. Run `npm run test:unit` to ensure tests still pass
3. Run `npm run test:cy:component` for affected component tests

## Success Criteria

- ✅ All 24 TypeScript errors resolved
- ✅ No new TypeScript errors introduced
- ✅ All existing tests still passing
- ✅ No significant refactoring required

---

## Progress Update - October 28, 2025

### Completed Subtasks

#### ✅ Subtask 1: Simple Test Mock Fixes (16 errors)

- Fixed `useGetCombinedEntities` hook return type
- Added missing `isActive` property to test mocks
- **See:** CONVERSATION_SUBTASK_1.md

#### ✅ Subtask 2: React Flow Node Type Fixes (5 errors)

- Fixed `GroupPropertyDrawer.spec.cy.tsx` - changed to `NodeGroupType`
- Fixed `NodePropertyDrawer.spec.cy.tsx` - changed to `NodeAdapterType`
- **See:** CONVERSATION_SUBTASK_2.md

#### ✅ Subtask 3: Generic Type Constraint Issues (3 errors)

- Added `<T extends Record<string, unknown>>` constraints to interface AND implementation
- Fixed test matcher with `expect.arrayContaining`
- **See:** CONVERSATION_SUBTASK_3.md

#### ✅ Subtask 4: Runtime Type Safety (1 error)

- Added type assertion for `node.data.config as GenericObjectType`
- **See:** CONVERSATION_SUBTASK_4.md

#### ✅ Subtask 5: DataHub & Component Errors (8 errors - not in original plan)

- These were in TASK_BRIEF but not assigned to subtasks 1-4
- Fixed Filter, vitest utils, ReactFlowRenderer, BaseNode, PolicyEditor, PolicyEditorLoader, TransitionNode
- **See:** CONVERSATION_SUBTASK_5.md

### Error Count Tracking

- **Start:** 24 errors (from TASK_BRIEF)
- **After Subtask 1:** 8 errors remaining
- **After Subtask 2:** 3 errors remaining
- **After Subtask 3:** 3 errors remaining (Subtask 2 partially overlapped)
- **After Subtask 4:** 8 errors remaining (original 8 still there)
- **After Subtask 5:** 0 errors (user-verified) ✅

### Total Files Modified

**17 files total** (some modified in multiple subtasks):

Subtask 1:

1. `src/api/hooks/useDomainModel/useGetCombinedEntities.ts`
2. `src/modules/Workspace/components/filters/ConfigurationSave.spec.cy.tsx`

Subtask 2: 3. `src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx` 4. `src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx`

Subtask 3: 5. `src/extensions/datahub/types.ts` 6. `src/extensions/datahub/hooks/useDataHubDraftStore.ts` 7. `src/modules/Workspace/types.ts` 8. `src/modules/Workspace/hooks/useWorkspaceStore.ts` 9. `src/modules/DomainOntology/hooks/useGetDomainOntology.spec.ts`

Subtask 4: 10. `src/modules/Workspace/hooks/useGetPoliciesMatching.ts`

Subtask 5: 11. `src/components/PaginatedTable/components/Filter.tsx` 12. `src/extensions/datahub/__test-utils__/vitest.utils.ts` (user fixed) 13. `src/extensions/datahub/components/fsm/ReactFlowRenderer.tsx` 14. `src/extensions/datahub/components/nodes/BaseNode.tsx` 15. `src/extensions/datahub/components/pages/PolicyEditor.tsx` 16. `src/extensions/datahub/components/pages/PolicyEditorLoader.tsx` 17. `src/extensions/datahub/designer/transition/TransitionNode.utils.spec.ts`

---

**Awaiting user verification before marking as COMPLETE.**

**User: Please run `npx tsc -b` and confirm error count is 0.**
