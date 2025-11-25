# Subtask 9: Critical Nested Group Fixes

**Task:** 38139-wizard-group  
**Subtask:** 9 - Critical Nested Group Fixes  
**Date:** November 21, 2025  
**Status:** 🔄 In Progress  
**Priority:** 🔴 HIGH (Blocker for production)  
**Estimated Effort:** 1 day

---

## Objectives

Fix critical issues with nested groups to make the feature production-ready:

1. ✅ Fix group deletion to handle nesting properly
2. ✅ Add nesting depth limit (max 3 levels)
3. ✅ Prevent nested collapse when parent collapsed
4. ✅ Add validation for circular references

---

## Implementation Plan

### Part 1: Fix Group Deletion (Smart Ungrouping)

**Problem:** Deleting nested groups places children at wrong positions

**Solution:** Move children to parent group (one level up) instead of root

**Files to Modify:**

- Find and update group deletion handler
- Likely in: `useWorkspaceStore.ts` or group utilities

**Implementation:**

```typescript
const deleteGroup = (groupId: string) => {
  const groupNode = nodes.find((n) => n.id === groupId)
  const children = nodes.filter((n) => n.parentId === groupId)
  const parentGroupId = groupNode?.parentId

  const updatedNodes = nodes
    .map((node) => {
      if (node.id === groupId) return null // Remove group

      if (node.parentId === groupId) {
        if (parentGroupId) {
          // NESTED: Move to parent group
          return {
            ...node,
            parentId: parentGroupId,
            position: {
              x: node.position.x + groupNode.position.x,
              y: node.position.y + groupNode.position.y,
            },
          }
        } else {
          // FLAT: Ungroup to root
          return {
            ...node,
            parentId: undefined,
            extent: undefined,
            position: {
              x: node.position.x + groupNode.position.x,
              y: node.position.y + groupNode.position.y,
            },
          }
        }
      }
      return node
    })
    .filter(Boolean)

  setNodes(updatedNodes)
}
```

---

### Part 2: Add Nesting Depth Limit

**Problem:** Unlimited nesting causes cognitive overload

**Solution:** Limit to MAX_NESTING_DEPTH = 3 levels

**Files to Modify:**

- `groupConstraints.ts` - Add depth calculation and validation
- `WizardSelectionRestrictions.tsx` - Apply validation during selection
- `useCompleteGroupWizard.ts` - Validate before creation

**Implementation:**

```typescript
// In groupConstraints.ts
export const MAX_NESTING_DEPTH = 3

export const getNodeNestingDepth = (nodeId: string, allNodes: Node[]): number => {
  const node = allNodes.find((n) => n.id === nodeId)
  if (!node) return 0

  let depth = 0
  let current = node

  while (current.parentId) {
    depth++
    current = allNodes.find((n) => n.id === current.parentId)
    if (!current) break
  }

  return depth
}

export const canAddToGroup = (
  nodeId: string,
  targetGroupId: string,
  allNodes: Node[]
): { allowed: boolean; reason?: string } => {
  const nodeDepth = getNodeNestingDepth(nodeId, allNodes)
  const targetDepth = getNodeNestingDepth(targetGroupId, allNodes)

  // For groups being added, also account for their children depth
  const node = allNodes.find((n) => n.id === nodeId)
  if (node?.type === NodeTypes.CLUSTER_NODE) {
    const maxChildDepth = getMaxChildDepth(node, allNodes)
    const totalDepth = targetDepth + 1 + maxChildDepth

    if (totalDepth > MAX_NESTING_DEPTH) {
      return {
        allowed: false,
        reason: `Maximum nesting depth (${MAX_NESTING_DEPTH}) would be exceeded`,
      }
    }
  } else {
    if (targetDepth + 1 > MAX_NESTING_DEPTH) {
      return {
        allowed: false,
        reason: `Maximum nesting depth (${MAX_NESTING_DEPTH}) reached`,
      }
    }
  }

  return { allowed: true }
}

const getMaxChildDepth = (groupNode: Node, allNodes: Node[]): number => {
  const childIds = (groupNode.data?.childrenNodeIds || []) as string[]
  if (childIds.length === 0) return 0

  let maxDepth = 0
  childIds.forEach((childId) => {
    const child = allNodes.find((n) => n.id === childId)
    if (child?.type === NodeTypes.CLUSTER_NODE) {
      const childDepth = 1 + getMaxChildDepth(child, allNodes)
      maxDepth = Math.max(maxDepth, childDepth)
    }
  })

  return maxDepth
}
```

---

### Part 3: Prevent Nested Collapse

**Problem:** Collapsing nested groups when parent collapsed hides nodes completely

**Solution:** Prevent collapse if parent is collapsed

**Files to Modify:**

- Group collapse handler (likely in `useWorkspaceStore.ts`)
- Group UI component with collapse button

**Implementation:**

```typescript
export const canGroupCollapse = (
  groupId: string,
  allNodes: Node[]
): {
  allowed: boolean
  reason?: string
} => {
  const group = allNodes.find((n) => n.id === groupId)
  if (!group) return { allowed: false, reason: 'Group not found' }

  // Check if parent is collapsed
  if (group.parentId) {
    const parent = allNodes.find((n) => n.id === group.parentId)
    if (parent?.data?.isCollapsed) {
      return {
        allowed: false,
        reason: 'Cannot collapse nested group when parent is collapsed',
      }
    }
  }

  return { allowed: true }
}
```

---

### Part 4: Add Circular Reference Validation

**Problem:** Potential for circular parent-child relationships

**Solution:** Validate hierarchy on state updates

**Files to Modify:**

- `useWorkspaceStore.ts` - Add validation middleware
- Group creation/modification handlers

**Implementation:**

```typescript
export const validateGroupHierarchy = (
  nodes: Node[]
): {
  valid: boolean
  errors: string[]
} => {
  const errors: string[] = []

  // Check for circular references
  nodes.forEach((node) => {
    if (!node.parentId) return

    const ancestors = new Set<string>()
    let current = node
    let depth = 0

    while (current.parentId && depth < 10) {
      // Max 10 to prevent infinite loop
      if (ancestors.has(current.parentId)) {
        errors.push(`Circular reference detected: ${node.id}`)
        break
      }

      ancestors.add(current.parentId)
      current = nodes.find((n) => n.id === current.parentId)
      if (!current) {
        errors.push(`Orphaned node: ${node.id} has invalid parent`)
        break
      }

      depth++
    }
  })

  return {
    valid: errors.length === 0,
    errors,
  }
}
```

---

## Testing Strategy

### Unit Tests

1. Test smart ungrouping for nested groups
2. Test depth limit validation
3. Test collapse prevention
4. Test circular reference detection

### Manual Testing

1. Create nested groups (3 levels)
2. Delete middle group → verify children move to parent
3. Try to create 4th level → verify prevented with toast
4. Collapse outer group → try to collapse inner → verify prevented
5. Try to create circular reference → verify prevented

---

## Files to Create/Modify

### Files to Modify (4-5)

1. `utils/groupConstraints.ts` - Add depth validation functions
2. `hooks/useWorkspaceStore.ts` - Update group deletion + add validation
3. `WizardSelectionRestrictions.tsx` - Apply depth validation
4. `hooks/useCompleteGroupWizard.ts` - Add depth validation before creation
5. Group collapse handler (location TBD)

### Files to Create (1)

1. `utils/groupConstraints.spec.ts` - Add tests for new functions (if not exist, add to existing)

---

## Implementation Steps

1. ✅ Create Subtask 9 plan document
2. 🔄 Locate group deletion handler
3. 🔄 Implement smart ungrouping
4. 🔄 Add depth calculation functions
5. 🔄 Add depth limit validation
6. 🔄 Add collapse prevention
7. 🔄 Add circular reference validation
8. 🔄 Add unit tests
9. 🔄 Manual testing
10. 🔄 Update documentation

---

## Success Criteria

- ✅ Deleting nested groups maintains correct positions
- ✅ Cannot create groups deeper than 3 levels
- ✅ Cannot collapse nested group if parent collapsed
- ✅ Circular references prevented
- ✅ All unit tests passing
- ✅ Manual testing passes all scenarios

---

**Status:** Planning complete, ready to implement  
**Next:** Locate group deletion handler and begin implementation
