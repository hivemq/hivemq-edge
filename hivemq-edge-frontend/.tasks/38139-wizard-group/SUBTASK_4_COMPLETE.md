# Subtask 4 Complete: Ghost Group Renderer Integration

**Task:** 38139-wizard-group  
**Subtask:** 4 - Ghost Group Renderer Integration  
**Date:** November 21, 2025  
**Status:** ✅ Complete  
**Duration:** ~45 minutes

---

## What Was Implemented

### 1. Dynamic Ghost Group Rendering in GhostNodeRenderer

Added a dedicated useEffect hook for GROUP wizard that creates and updates ghost groups in real-time based on selection.

#### Key Features:

- ✅ **Reactive to Selection**: Triggers on every `selectedNodeIds` change
- ✅ **Dynamic Creation**: Creates ghost group when nodes selected
- ✅ **Dynamic Updates**: Updates boundary when selection changes
- ✅ **Dynamic Removal**: Removes ghost when last node deselected
- ✅ **Clean State Management**: Removes old ghosts before adding new ones
- ✅ **Smooth Animations**: Fits viewport on first selection
- ✅ **Proper Cleanup**: Clears ghost store when empty

#### Implementation Pattern:

```typescript
useEffect(() => {
  // Only for GROUP wizard
  if (!isActive || entityType !== EntityType.GROUP) return

  // Remove existing ghosts
  const nodesWithoutGhosts = removeGhostGroup(currentNodes)

  // Get selected nodes
  const selectedNodes = currentNodes.filter((n) => selectedNodeIds.includes(n.id) && !n.data?.isGhost)

  // Create ghost group for current selection
  const ghostGroup = createGhostGroupWithChildren(
    selectedNodes,
    currentNodes,
    currentEdges,
    getNodesBounds,
    getGroupBounds
  )

  if (ghostGroup === null) {
    // No selection - remove ghosts
    // ...cleanup logic
    return
  }

  // Add new ghost group
  addGhostNodes(ghostGroup.nodes)
  onAddNodes(ghostGroup.nodes.map((node) => ({ item: node, type: 'add' })))
}, [selectedNodeIds]) // KEY: Triggers on selection change
```

### 2. Integration with Existing Ghost System

Integrated GROUP wizard seamlessly with existing ADAPTER, BRIDGE, COMBINER, and ASSET_MAPPER ghost rendering.

#### Changes Made:

1. **Imports Added**:

   - `createGhostGroupWithChildren` from ghostNodeFactory
   - `removeGhostGroup` from ghostNodeFactory
   - `getGroupBounds` from group.utils

2. **Component-Level Hook**:

   - Added `getNodesBounds` from `useReactFlow()` at component level

3. **Initial Ghost Creation**:

   - GROUP wizard skips initial ghost creation (unlike other wizards)
   - Comment added explaining GROUP is handled dynamically

4. **Dedicated Effect Hook**:
   - New useEffect specifically for GROUP wizard
   - Runs after existing ghost creation effect
   - Independent from other wizard logic

---

## Files Modified (1)

1. `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx`
   - Added imports for GROUP functions (~3 lines)
   - Added `getNodesBounds` to useReactFlow hook (~1 line)
   - Updated initial ghost creation to skip GROUP (~3 lines)
   - Added dynamic GROUP ghost effect (~70 lines)

---

## Technical Implementation Details

### 1. Reactive Dependency Array

The useEffect hook depends on `selectedNodeIds`, making it reactive to selection changes:

```typescript
}, [
  isActive,
  entityType,
  selectedNodeIds, // ← KEY: Triggers on every selection change
  nodes,
  edges,
  getNodesBounds,
  // ...other deps
])
```

### 2. Ghost Lifecycle

**Selection Flow:**

```
No selection
  ↓ User clicks node 1
Ghost created with 1 node (+ auto-included DEVICE)
  ↓ User clicks node 2
Ghost updated with 2 nodes (+ 2 auto-included DEVICES)
  ↓ User deselects node 1
Ghost updated with 1 node (+ 1 auto-included DEVICE)
  ↓ User deselects node 2
Ghost removed (null returned from factory)
```

### 3. State Management

**Wizard Store Updates:**

- `addGhostNodes(ghostGroup.nodes)` - Updates wizard store
- `clearGhostNodes()` - Clears wizard store when empty

**Workspace Store Updates:**

- `onNodesChange([...])` - Removes old ghost nodes
- `onAddNodes([...])` - Adds new ghost nodes

### 4. Cleanup Strategy

**Old Ghost Removal:**

```typescript
// Remove any existing ghost group nodes
const nodesWithoutGhosts = removeGhostGroup(currentNodes)

// Only update if there were ghosts to remove
if (nodesWithoutGhosts.length !== currentNodes.length) {
  const ghostNodeIds = currentNodes.filter((n) => !nodesWithoutGhosts.find((rn) => rn.id === n.id)).map((n) => n.id)

  onNodesChange(ghostNodeIds.map((id) => ({ id, type: 'remove' })))
}
```

This ensures:

- Old ghosts removed before new ones added
- No duplicate ghosts
- No orphaned ghost nodes

### 5. Viewport Management

**Auto-Fit on First Selection:**

```typescript
// Optional: Fit view to show ghost group (only on first selection)
if (selectedNodes.length === 1) {
  setTimeout(() => {
    fitView({
      nodes: ghostGroup.nodes,
      duration: 500,
      padding: 0.2,
    })
  }, 100)
}
```

This provides smooth UX by:

- Showing ghost group immediately when first node selected
- Not refitting viewport on subsequent selections (avoids jarring movements)
- Using animation for smooth transition

---

## Behavior Comparison

### Other Wizards (ADAPTER, BRIDGE, COMBINER, ASSET_MAPPER)

**Pattern**: Static ghost created on wizard start

```
Wizard starts
  ↓
Ghost created immediately
  ↓
Ghost stays until wizard completion
```

### GROUP Wizard

**Pattern**: Dynamic ghost created/updated during selection

```
Wizard starts
  ↓
No ghost (waiting for selection)
  ↓
User selects node 1
  ↓
Ghost appears with node 1
  ↓
User selects node 2
  ↓
Ghost updates to include both nodes
  ↓
User deselects node 1
  ↓
Ghost updates to show only node 2
  ↓
User deselects node 2
  ↓
Ghost disappears
```

---

## Debug Logging

Added debug statements for troubleshooting:

```typescript
debugLog('[GROUP] Selection changed:', selectedNodeIds.length, 'nodes selected')
debugLog('[GROUP] No selection - removing ghost group')
debugLog('[GROUP] Creating ghost group with', ghostGroup.nodes.length, 'nodes')
```

Enable debug logging:

```bash
localStorage.setItem('debug', 'workspace:wizard:ghostnode')
```

---

## Integration with Subtask 3

The ghost group factory (`createGhostGroupWithChildren`) integrates perfectly:

**Factory Returns:**

- `null` for empty selection → Ghost removed
- `GhostNodeGroup` for 1+ nodes → Ghost created/updated

**Renderer Handles:**

- Null return → Cleanup logic
- GhostNodeGroup return → Add/update logic

This separation of concerns makes the code:

- Easy to test (factory is pure function)
- Easy to maintain (renderer handles React state)
- Easy to extend (new ghost types can use same pattern)

---

## User Experience Flow

### Step 0: Selection Phase

1. **User starts GROUP wizard**

   - CreateEntityButton dropdown → Click "Group"
   - Wizard starts, no ghost visible yet

2. **User clicks first node** (e.g., Adapter 1)

   - Ghost group appears instantly
   - Contains Adapter 1 + Device 1 (auto-included)
   - Viewport fits to show ghost group
   - Selection panel shows "1 node selected (1 auto-included)"

3. **User clicks second node** (e.g., Adapter 2)

   - Ghost group expands smoothly
   - Now contains both adapters + their devices
   - Selection panel shows "2 nodes selected (2 auto-included)"

4. **User clicks "Next"**
   - Moves to Step 1 (Preview)
   - Ghost group persists (no change)

### Step 1: Preview Phase

- Ghost group remains visible
- User can click "Back" to modify selection
- Ghost updates if selection changes

---

## Testing Status

✅ **TypeScript**: No errors  
✅ **ESLint**: No errors (with intentional disable for dep array)  
✅ **React Hooks**: All hooks properly called at component level  
✅ **Infinite Loop Fix**: nodes/edges removed from dep array to prevent re-render loop  
✅ **Manual Testing**: Tested and working  
⏭️ **Component Tests**: Will add in Subtask 7  
⏭️ **E2E Tests**: Will add in Subtask 7

---

## Bugfix Applied

### Infinite Loop Prevention

**Issue**: Initial implementation caused "Maximum update depth exceeded" error

**Root Cause**: Including `nodes` and `edges` in the useEffect dependency array caused infinite loop:

1. Effect runs and adds ghost nodes
2. `nodes` array changes (ghost nodes added)
3. Effect triggers again because `nodes` changed
4. Repeat infinitely

**Solution**: Removed `nodes` and `edges` from dependency array

- They're accessed via closure (fine for reading current values)
- Only `selectedNodeIds` should trigger ghost updates
- Added ESLint disable with explanation comment

**Code Change**:

```typescript
// eslint-disable-next-line react-hooks/exhaustive-deps
}, [
  isActive,
  entityType,
  selectedNodeIds, // Only this should trigger updates
  // nodes and edges intentionally omitted
  // ...other deps
])
```

---

## Known Limitations

### 1. Performance with Large Selections

**Impact**: With 10+ nodes selected, ghost group might update slowly

**Mitigation**:

- `getNodesBounds` is optimized (React Flow internal)
- `removeGhostGroup` uses simple filter
- Ghost node count is bounded by selection size

**Future Optimization**:

- Could debounce selection changes
- Could memoize ghost group calculation

### 2. Viewport Auto-Fit

**Current**: Only fits on first selection

**Rationale**: Prevents jarring viewport movements on every selection

**Alternative**: Could add user preference to enable/disable auto-fit

---

## Next Steps

**Ready for Subtask 5**: Group Configuration Form

**What Works Now:**

- ✅ GROUP wizard appears in dropdown
- ✅ Selection mode works
- ✅ Selection constraints enforced
- ✅ Auto-inclusion displayed
- ✅ **Ghost group appears dynamically** ← NEW!
- ✅ **Ghost updates in real-time** ← NEW!
- ✅ Toast for already-grouped nodes

**What's Next (Subtask 5):**

1. Create `WizardGroupConfiguration.tsx` component
2. Form for group title (required)
3. Form for color scheme selection
4. Integrate into `WizardConfigurationPanel`
5. Add i18n keys (already added in Subtask 1!)
6. Write component tests

**Estimated Time**: 1.5 days

---

## Code Quality

✅ **Documented**: Clear comments explaining GROUP-specific logic  
✅ **Type Safe**: All types properly defined  
✅ **React Compliant**: Hooks called at component level  
✅ **Maintainable**: Separated concerns (factory vs renderer)  
✅ **Debuggable**: Debug logging added  
✅ **Performant**: Minimal unnecessary re-renders

---

**Completed By**: AI Agent  
**Reviewed By**: Pending  
**Ready for Subtask 5**: ✅ Yes  
**Ready for User Testing**: ✅ Yes

---

_Excellent progress! The ghost group now appears and updates dynamically as you select nodes. Try it in the UI - you'll see the magic happen in real-time!_
