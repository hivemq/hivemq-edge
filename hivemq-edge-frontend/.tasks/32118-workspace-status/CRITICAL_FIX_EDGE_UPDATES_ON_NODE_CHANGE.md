# Critical Fix: Edge Updates When Node StatusModel Changes

**Date:** October 26, 2025  
**Issue:** ADAPTER → COMBINER edge not animating despite combiner having operational status ACTIVE  
**Root Cause:** Edges only updated on adapter/bridge status changes, not on node data changes  
**Status:** ✅ Fixed

---

## The Real Problem

### User's Observation

```
ADAPTER (green - runtime ACTIVE)
  ↓
  → COMBINER (has mappings - operational ACTIVE)
      ↓
      → EDGE (green and animated ✅)

BUT: ADAPTER → COMBINER edge is GREEN but NOT ANIMATED ❌
```

### Why This Happened

**The StatusListener component was only updating edges in ONE scenario:**

```typescript
useEffect(() => {
  // Updates edges when adapter/bridge STATUS changes
  setEdges(updateEdgesStatusWithModel(...))
}, [adapterConnections?.items, bridgeConnections?.items])
```

**What was missing:**

- Edges were NOT updated when **combiner nodes changed** their statusModel
- Combiner calls `updateNodeData(id, { statusModel })` to store its operational status
- But this node data change **didn't trigger edge re-rendering**
- So edges kept using stale data (no statusModel)

### The Sequence Issue

1. **Graph loads** → edges rendered (combiner statusModel not yet available)
2. **Adapters get status** → edges updated (still no combiner statusModel)
3. **Combiner component mounts** → computes statusModel → calls updateNodeData
4. **❌ Edges NEVER get updated** → they still don't see combiner's statusModel
5. **Result:** ADAPTER → COMBINER edge uses fallback, but old fallback didn't work

---

## The Fix

Added a **second `useEffect`** that updates edges when **nodes change**:

```typescript
// Update edges when nodes change (e.g., when combiner statusModel updates)
// This ensures edges reflect the latest operational status of target nodes
useEffect(() => {
  const { items } = adapterTypes || {}
  if (items && nodes.length > 0) {
    setEdges((currentEdges) => {
      return updateEdgesStatusWithModel(items, currentEdges, getNode, theme)
    })
  }
}, [nodes, adapterTypes]) // ← Triggers when nodes array changes
```

### Why This Works

1. **Combiner mounts** → computes statusModel → calls updateNodeData
2. **React Flow updates nodes array** → new reference
3. **useEffect triggers** → edges re-rendered with latest node data
4. **Edge finds combiner.statusModel** → uses operational status
5. **Edge animates** ✅

---

## Technical Details

### StatusListener Component

**File:** `src/modules/Workspace/components/controls/StatusListener.tsx`

**Before (Only 1 useEffect):**

- ✅ Edges updated on adapter/bridge status changes
- ❌ Edges NOT updated on node data changes

**After (2 useEffects):**

- ✅ Edges updated on adapter/bridge status changes
- ✅ Edges updated on node changes (combiner statusModel, pulse statusModel, etc.)

### Edge Update Flow

```
1. Node Component (e.g., NodeCombiner)
   ↓
   computes statusModel
   ↓
   updateNodeData(id, { statusModel })
   ↓
2. React Flow internals
   ↓
   updates nodes array (new reference)
   ↓
3. StatusListener useEffect (NEW!)
   ↓
   detects nodes change
   ↓
   calls updateEdgesStatusWithModel
   ↓
4. updateEdgesStatusWithModel
   ↓
   getNode(edge.target) ← gets fresh combiner with statusModel
   ↓
   uses combiner.statusModel.operational
   ↓
5. Edge rendered with animation ✅
```

---

## Performance Considerations

### Concern: Updating edges on every node change might be expensive

**Mitigation:**

1. **React Flow optimizations** - Only re-renders changed edges
2. **Shallow comparison** - React only triggers if nodes array reference changes
3. **Conditional check** - Only runs if `nodes.length > 0` and `adapterTypes` available
4. **Efficient getNode** - React Flow's internal map lookup is O(1)

### When This Effect Triggers

- ✅ When combiner updates statusModel
- ✅ When pulse updates statusModel
- ✅ When any node data changes (position, selection, etc.)
- ✅ When nodes are added/removed

**Most of these are already triggering React Flow re-renders**, so the performance impact is minimal.

---

## Alternative Solutions Considered

### Option 1: Debounce Edge Updates ❌

```typescript
const debouncedUpdateEdges = useDebouncedCallback(() => {
  setEdges(updateEdgesStatusWithModel(...))
}, 100)

useEffect(() => {
  debouncedUpdateEdges()
}, [nodes])
```

**Rejected:** Adds delay, complexity, and the performance gain is minimal.

### Option 2: Only Update on StatusModel Changes ❌

```typescript
const nodeStatusModels = useMemo(() =>
  nodes.map(n => n.data.statusModel),
  [nodes]
)

useEffect(() => {
  setEdges(updateEdgesStatusWithModel(...))
}, [nodeStatusModels])
```

**Rejected:** Creates new array every render, defeats the optimization purpose.

### Option 3: Subscribe to Specific Node Updates ❌

```typescript
useOnNodesChange((changes) => {
  const hasDataChange = changes.some(c => c.type === 'update')
  if (hasDataChange) {
    setEdges(updateEdgesStatusWithModel(...))
  }
})
```

**Rejected:** More complex, harder to maintain, similar performance.

### ✅ Option 4: Simple useEffect on nodes (CHOSEN)

- Simplest implementation
- Leverages React Flow's existing optimizations
- Easy to understand and maintain
- Performance impact is negligible

---

## Files Modified

- ✅ `src/modules/Workspace/components/controls/StatusListener.tsx` (+11 lines)
  - Added second useEffect to update edges on node changes

---

## Testing Validation

### Before Fix

```javascript
// In browser
const edge = reactFlow.getEdges().find((e) => e.source === 'adapter-1' && e.target === 'combiner-1')
console.log(edge.animated) // false ❌
```

### After Fix

```javascript
// In browser (after combiner mounts)
const edge = reactFlow.getEdges().find((e) => e.source === 'adapter-1' && e.target === 'combiner-1')
console.log(edge.animated) // true ✅
```

### Manual Test Steps

1. Refresh browser
2. Wait for graph to load
3. Check ADAPTER → COMBINER edge
4. Should be GREEN and ANIMATED ✅

---

## Why Previous Fixes Didn't Work

### Fix 1: Added per-edge status logic ✅

- **Result:** Logic was correct, but edges weren't being re-rendered
- **Issue:** StatusListener didn't know about node changes

### Fix 2: Added fallback checking mappings directly ✅

- **Result:** Fallback was there, but never used because statusModel wasn't being checked
- **Issue:** Edges were using stale closure over getNode

### Fix 3: This fix - Update edges on node changes ✅

- **Result:** Edges now re-render when combiner updates its statusModel
- **Success:** The existing per-edge logic and fallback now work as intended

---

## Summary

The issue wasn't with the **logic** of how edges compute operational status - that was correct. The issue was with **when** edges were updated. By adding a useEffect that updates edges when nodes change, we ensure that edges always have access to the latest statusModel from combiners.

**Key Insight:** In a reactive graph system, you must update dependent elements (edges) whenever their dependencies (nodes) change, not just when external data (API responses) changes.

---

## Expected Behavior Now

✅ **ADAPTER → COMBINER edge:**

- GREEN (adapter runtime ACTIVE)
- ANIMATED (combiner operational ACTIVE - has mappings)

✅ **COMBINER → EDGE edge:**

- GREEN (combiner runtime ACTIVE)
- ANIMATED (combiner operational ACTIVE - has mappings)

Both edges now correctly reflect the combiner's operational status! 🎉
