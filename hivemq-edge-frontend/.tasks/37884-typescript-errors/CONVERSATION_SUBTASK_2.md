# Subtask 2: React Flow Node Type Fixes - IN PROGRESS

## Files Being Fixed

### 1. `src/modules/Workspace/components/drawers/GroupPropertyDrawer.spec.cy.tsx`

**Errors:** 3 (lines 51, 83, 121)
**Issue:** `Node<Group>` not assignable to `NodeGroupType` - missing proper type assertion
**Solution:** Changed `mockNode` type from `Node<Group>` to `NodeGroupType`

```typescript
// Before
const mockNode: Node<Group> = { ... }

// After
const mockNode: NodeGroupType = { ... }
```

### 2. `src/modules/Workspace/components/drawers/NodePropertyDrawer.spec.cy.tsx`

**Errors:** 2 (lines 31, 66)
**Issue:** `Node<Bridge | Adapter>` not assignable to `NodeAdapterType | NodeBridgeType`
**Solution:** Changed `mockNode` type from `Node<Bridge | Adapter>` to `NodeAdapterType` and fixed data reference

```typescript
// Before
const mockNode: Node<Bridge | Adapter> = {
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER,
}

// After
const mockNode: NodeAdapterType = {
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER.data,
}
```

## Status

- ✅ GroupPropertyDrawer fixes applied
- ✅ NodePropertyDrawer fixes applied
- 🔄 Need to verify fixes with tsc -b
- 🔄 Continue to Subtask 3

## Remaining Errors (from original list)

- DataHub PolicyEditorLoader errors (5)
- TransitionNode utils spec error (1)
- useDataHubDraftStore error (1)
- useGetDomainOntology spec error (1)
- useGetPoliciesMatching error (1)
- useWorkspaceStore error (1)
