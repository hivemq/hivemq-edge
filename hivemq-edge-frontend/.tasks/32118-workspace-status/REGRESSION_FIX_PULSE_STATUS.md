# Regression Fix: PULSE Node Status

**Date:** October 26, 2025  
**Issue:** PULSE node showing as INACTIVE after per-edge status implementation  
**Status:** ✅ Fixed

---

## Problem

After implementing per-edge operational status, the PULSE node and its outbound edges were showing as INACTIVE, even when:

- Pulse API status was ACTIVATED and CONNECTED
- Connected asset mappers had valid mappings
- Previously working correctly before the changes

---

## Root Cause

The issue was a **conflict between two sources of statusModel updates**:

### 1. StatusListener (External Update)

```typescript
// Old code in StatusListener
useEffect(() => {
  const statusModel = createPulseStatusModel(pulseConnections, OperationalStatus.ACTIVE)
  const changes = updatePulseStatusWithModel(pulseNode, statusModel, edges, theme)
  onUpdateNode(NODE_PULSE_AGENT_DEFAULT_ID, changes.nodes)
}, [pulseConnections])
```

StatusListener was:

- Creating a statusModel with hardcoded `OperationalStatus.ACTIVE`
- Calling `updatePulseStatusWithModel` which updated edges
- Overwriting the node's statusModel

### 2. NodePulse (Internal Update)

```typescript
// Code in NodePulse component
const statusModel = useMemo(() => {
  const operational = computePulseNodeOperationalStatus(validCombiners, allAssets.items)
  return createPulseStatusModel(data.status, operational)
}, [data.status, connectedNodes, allAssets])

useEffect(() => {
  updateNodeData(id, { statusModel })
}, [id, statusModel, updateNodeData])
```

NodePulse was:

- Computing operational status based on connected asset mappers
- Creating statusModel from `data.status` (which might be undefined)
- Updating node data

### The Conflict

**Race condition scenario:**

1. StatusListener updates Pulse node with `statusModel` from API
2. NodePulse tries to compute statusModel using `data.status`
3. But `data.status` might be undefined or stale
4. Result: NodePulse creates statusModel with INACTIVE runtime
5. Edges see INACTIVE runtime → no animation

**Overwrite scenario:**

1. NodePulse computes correct statusModel
2. StatusListener updates and overwrites it with hardcoded operational=ACTIVE
3. NodePulse recomputes based on stale `data.status`
4. Result: incorrect status

---

## Solution

**Separate concerns:**

- **StatusListener** should only update the **API data** (`status` field)
- **NodePulse** should be the **sole owner** of `statusModel` computation

### Updated StatusListener

```typescript
useEffect(() => {
  if (!pulseConnections) return
  const pulseNode = nodes.find((e) => e.type === NodeTypes.PULSE_NODE) as NodePulseType | undefined
  if (!pulseNode) return

  // Update only the status field from API, not the statusModel
  // The statusModel will be computed by NodePulse based on asset mappers
  onUpdateNode<NodePulseType['data']>(NODE_PULSE_AGENT_DEFAULT_ID, {
    ...pulseNode.data,
    status: pulseConnections, // Update API status data
    // Don't touch statusModel - let NodePulse compute it
  })
}, [pulseConnections])
```

**Key changes:**

- ✅ Only updates `status` field (the raw API data)
- ✅ Doesn't compute or update `statusModel`
- ✅ Removes dependency on `updatePulseStatusWithModel`
- ✅ Removes dependency on `createPulseStatusModel`
- ✅ Let NodePulse be the single source of truth for statusModel

### How It Works Now

```
1. API polling → pulseConnections updates
   ↓
2. StatusListener updates node.data.status
   ↓
3. NodePulse detects data.status change
   ↓
4. NodePulse recomputes statusModel:
   - runtime = mapPulseStatusToRuntime(data.status) ← from API
   - operational = computePulseNodeOperationalStatus(...) ← from asset mappers
   ↓
5. NodePulse updates node.data.statusModel
   ↓
6. StatusListener (2nd useEffect) detects nodes change
   ↓
7. Edges re-render with correct statusModel ✅
```

---

## Why This is Better

### Before (Two Writers)

- ❌ StatusListener and NodePulse both update statusModel
- ❌ Race conditions possible
- ❌ Unclear which one wins
- ❌ Hard to debug

### After (Single Writer)

- ✅ Only NodePulse updates statusModel
- ✅ StatusListener only provides raw API data
- ✅ Clear ownership and responsibility
- ✅ No race conditions

---

## Pattern for Other Nodes

This establishes a clear pattern:

**For Active Nodes with API status:**

1. **StatusListener** updates raw API status field (`status` or `connections`)
2. **Node Component** is sole owner of `statusModel` computation
3. Node component watches `data.status` and recomputes `statusModel` when it changes

**Applied to:**

- ✅ PULSE_NODE - Fixed with this change
- ✅ ADAPTER_NODE - Already follows this pattern
- ✅ BRIDGE_NODE - Already follows this pattern

**For Passive Nodes:**

- Only the node component updates `statusModel`
- Derives from upstream connections
- No API status to update

---

## Files Modified

- ✅ `src/modules/Workspace/components/controls/StatusListener.tsx`
  - Simplified Pulse status update (removed statusModel computation)
  - Removed unused imports (`updatePulseStatusWithModel`, `createPulseStatusModel`, `OperationalStatus`)
  - Reduced complexity (~15 lines removed)

---

## Validation

### Before Fix

```javascript
// Pulse node in DevTools
pulseNode.data.statusModel
// { runtime: "INACTIVE", operational: "INACTIVE" } ❌

// Edges
edge.animated // false ❌
```

### After Fix

```javascript
// Pulse node in DevTools
pulseNode.data.statusModel
// { runtime: "ACTIVE", operational: "ACTIVE" } ✅

// Edges
edge.animated // true ✅
```

### Test Scenario

1. Pulse API returns ACTIVATED + CONNECTED
2. Pulse has connected asset mapper with valid mappings
3. **Expected:**
   - Pulse node shows as ACTIVE (green)
   - Edge to asset mapper is animated (green + flowing dots)
4. **Result:** ✅ Working as expected

---

## Lessons Learned

### 1. Single Responsibility

Each piece of code should have one clear responsibility:

- StatusListener: Provide raw API data
- Node components: Compute derived statusModel

### 2. Single Source of Truth

Don't have multiple places updating the same piece of state:

- Only NodePulse should update its statusModel
- StatusListener shouldn't interfere

### 3. Component Ownership

The component that renders the data should own the computation:

- NodePulse renders the node → NodePulse computes its status
- StatusListener just provides raw ingredients

### 4. Watch for Race Conditions

When multiple effects update the same data:

- Think about execution order
- Consider which one should win
- Better: eliminate the conflict entirely

---

## Related Work

This fix complements:

- ✅ Per-edge operational status implementation
- ✅ Edge update triggers on node changes
- ✅ Combiner operational status computation
- ✅ Pulse operational status based on asset mappers

**The complete status system now works correctly with proper separation of concerns!** 🎉

---

## Summary

Fixed PULSE node regression by establishing clear ownership:

- **StatusListener** = Provider of raw API data
- **NodePulse** = Owner of statusModel computation

No more conflicts, no more race conditions, clean architecture! ✅
