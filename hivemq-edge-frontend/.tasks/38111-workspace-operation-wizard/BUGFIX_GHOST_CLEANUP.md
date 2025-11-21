# Bug Fix: Ghost Nodes Not Removed on Cancel

**Date:** November 10, 2025  
**Issue:** Ghost nodes remained visible on canvas after canceling wizard  
**Status:** ✅ Fixed

---

## Problem

When clicking Cancel on the wizard:

1. Wizard state was reset (isActive → false)
2. Zustand store was cleared (ghostNodes/ghostEdges → [])
3. **But React Flow nodes/edges still contained ghost nodes** ❌

The ghost nodes stayed visible on the canvas even though the wizard was canceled.

---

## Root Cause

The cleanup logic had a race condition:

```typescript
// BEFORE - BUGGY CODE
if (!isActive || !entityType) {
  // Only clean up if store has ghost nodes
  if (ghostNodes.length > 0 || ghostEdges.length > 0) {
    const nodes = getNodes()
    const edges = getEdges()
    const realNodes = removeGhostNodes(nodes)
    const realEdges = removeGhostEdges(edges)
    setNodes(realNodes)
    setEdges(realEdges)
    clearGhostNodes()
  }
  return
}
```

**Problem:** The condition `if (ghostNodes.length > 0 || ghostEdges.length > 0)` checked the Zustand store, but:

- Sometimes `cancelWizard()` clears the store before the effect runs
- React Flow state (`getNodes()`, `getEdges()`) still had ghost nodes
- Cleanup was skipped because store was already empty

**Result:** Ghost nodes stayed in React Flow even though wizard was canceled.

---

## Solution

Always clean up React Flow state when wizard becomes inactive, regardless of Zustand store state:

```typescript
// AFTER - FIXED CODE
if (!isActive || !entityType) {
  // Always check React Flow state for ghost nodes
  const nodes = getNodes()
  const edges = getEdges()
  const realNodes = removeGhostNodes(nodes)
  const realEdges = removeGhostEdges(edges)

  // Only update if there are actually ghost nodes/edges to remove
  if (realNodes.length !== nodes.length || realEdges.length !== edges.length) {
    setNodes(realNodes)
    setEdges(realEdges)
  }

  // Clear store if not already empty
  if (ghostNodes.length > 0 || ghostEdges.length > 0) {
    clearGhostNodes()
  }
  return
}
```

**Key Changes:**

1. **Always** get nodes/edges from React Flow when wizard is inactive
2. **Always** filter out ghost nodes/edges
3. **Only update** if there were actually ghosts to remove (optimization)
4. **Then** clear Zustand store if needed

---

## How It Works Now

### When User Clicks Cancel:

```
1. User clicks Cancel button
   ↓
2. cancelWizard() action called
   ├─ Sets isActive = false
   ├─ Clears ghostNodes = []
   └─ Clears ghostEdges = []
   ↓
3. GhostNodeRenderer useEffect triggers (isActive changed)
   ↓
4. Effect sees isActive = false
   ↓
5. Gets current nodes/edges from React Flow
   ├─ nodes might still have ghost nodes
   └─ edges might still have ghost edges
   ↓
6. Filters out all ghost nodes/edges
   ├─ removeGhostNodes(nodes) → only real nodes
   └─ removeGhostEdges(edges) → only real edges
   ↓
7. Updates React Flow state
   ├─ setNodes(realNodes)
   └─ setEdges(realEdges)
   ↓
8. Ghost nodes removed! ✅
```

---

## Testing

### Manual Test:

1. ✅ Start wizard (ghost nodes appear)
2. ✅ Click Cancel
3. ✅ Ghost nodes disappear immediately
4. ✅ Only real nodes remain

### Edge Cases:

1. ✅ Cancel on step 0 (ghost preview) → cleaned up
2. ✅ Cancel on step 1 (protocol selection) → cleaned up
3. ✅ Cancel on step 2 (configuration) → cleaned up
4. ✅ Rapid cancel after start → cleaned up
5. ✅ Multiple wizard starts/cancels → no ghost accumulation

---

## Why This Fix Works

### Before (Race Condition):

```
cancelWizard() runs
  → Clears Zustand store
  → useEffect triggers
  → Checks: ghostNodes.length > 0? NO ❌
  → Skips cleanup
  → React Flow still has ghosts 👻
```

### After (Always Clean):

```
cancelWizard() runs
  → Clears Zustand store
  → useEffect triggers
  → Always checks React Flow state ✓
  → Removes ghosts from React Flow
  → Ghost nodes gone! ✅
```

The key insight: **Don't trust the Zustand store state to determine if cleanup is needed. Always check the actual React Flow state.**

---

## Files Modified

**File:** `GhostNodeRenderer.tsx`

**Lines Changed:** ~10 lines

**Change Type:** Bug fix (logic correction)

---

## Related Issues

This same pattern should be applied to other cleanup scenarios:

- ✅ Wizard cancellation (fixed)
- ✅ Wizard completion (already working - different code path)
- ✅ Component unmount (already working - cleanup effect)

---

**Status:** ✅ Bug fixed - Ghost nodes now properly removed on cancel!
