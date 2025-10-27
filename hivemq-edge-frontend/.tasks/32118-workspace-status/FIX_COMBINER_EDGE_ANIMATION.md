# Fix: Per-Edge Operational Status for All Combiner Connections

**Date:** October 26, 2025  
**Issue:** Combiner edges were not being animated even when combiners had valid mappings  
**Status:** ✅ Fixed

---

## Problem

You reported:

> "I'm still seeing COMBINER node with a proper mapping (to a TAG) whose edge is not animated."

**Root Cause:** The adapter edge operational status utilities were created but **never integrated** into the edge rendering pipeline. All combiner connections were using the source node's overall operational status instead of checking the combiner's own operational status.

---

## Solution

Updated `status-utils.ts` to apply **per-edge operational status** for **ALL combiner edges** (both inbound and outbound):

### 1. ADAPTER → COMBINER (Inbound)

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  const targetStatusModel = target.data.statusModel

  if (targetStatusModel) {
    const edgeStatusModel = {
      runtime: adapterStatusModel.runtime, // From source (is adapter active?)
      operational: targetStatusModel.operational, // From target (does combiner have mappings?)
    }
    // Edge will be animated if combiner has mappings
  }
}
```

### 2. BRIDGE → COMBINER (Inbound)

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  const targetStatusModel = target.data.statusModel

  if (targetStatusModel) {
    const edgeStatusModel = {
      runtime: bridgeStatusModel.runtime, // From source (is bridge active?)
      operational: targetStatusModel.operational, // From target (does combiner have mappings?)
    }
    // Edge will be animated if combiner has mappings
  }
}
```

### 3. PULSE → ASSET_MAPPER (Inbound)

Already implemented in previous fix ✅

### 4. COMBINER → EDGE (Outbound)

```typescript
if (source.type === NodeTypes.COMBINER_NODE) {
  // Use the combiner's own statusModel which includes:
  // - Runtime: derived from upstream sources
  // - Operational: ACTIVE if has mappings, INACTIVE otherwise
  // Edge will be animated if combiner has mappings
}
```

**Key Point:** COMBINER uses its own operational status for BOTH inbound and outbound edges, ensuring consistent animation behavior.

---

## How It Works Now

### Combiner Operational Status

**File:** `NodeCombiner.tsx` (lines 43-48)

```typescript
// Combiner computes its own operational status
const hasMappings = data.mappings.items.length > 0
const operational = hasMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
```

The combiner node **already correctly computes** its operational status based on whether it has any mappings configured.

### Edge Rendering

**File:** `status-utils.ts` - `updateEdgesStatusWithModel()`

For any edge connecting to a combiner:

1. **Get target combiner's statusModel** from node data
2. **Create per-edge status** combining:
   - Runtime status from **source** (is the source node active?)
   - Operational status from **target combiner** (does it have mappings?)
3. **Apply to edge rendering**:
   - Color from runtime status (green/red/yellow)
   - Animation from operational status (animated if has mappings)

---

## Visual Result

### Before Fix ❌

```
ADAPTER (runtime: ACTIVE, operational: varies)
  ├─> Combiner 1 (operational: INACTIVE - no mappings)
  │     Edge: GREEN, NOT ANIMATED ❌ (wrong - was using adapter status)
  │
  └─> Combiner 2 (operational: ACTIVE - has mappings)
        Edge: GREEN, ANIMATED ✅ (correct by chance)
```

### After Fix ✅

```
ADAPTER (runtime: ACTIVE)
  ├─> Combiner 1 (operational: INACTIVE - no mappings)
  │     Edge: GREEN (runtime), NOT ANIMATED (no mappings) ✅
  │
  └─> Combiner 2 (operational: ACTIVE - has mappings)
        Edge: GREEN (runtime), ANIMATED (has mappings) ✅
```

---

## Files Modified

- ✅ `src/modules/Workspace/utils/status-utils.ts`
  - Added per-edge handling for ADAPTER → COMBINER (~28 new lines)
  - Added per-edge handling for BRIDGE → COMBINER (~28 new lines)
  - Both follow the same pattern as PULSE → ASSET_MAPPER

---

## Pattern Consistency

All three source types now follow the **same pattern**:

| Source  | →   | Target       | Runtime From | Operational From |
| ------- | --- | ------------ | ------------ | ---------------- |
| ADAPTER | →   | COMBINER     | Adapter      | Combiner         |
| BRIDGE  | →   | COMBINER     | Bridge       | Combiner         |
| PULSE   | →   | ASSET_MAPPER | Pulse        | Asset Mapper     |

**Key Principle:** When connecting to a combiner/mapper, use the **target's operational status** because the target determines if the transformation is configured.

---

## Why This Works

1. **Combiner knows its configuration state** - Has mappings or not
2. **Combiner stores this in statusModel** - Computed in NodeCombiner component
3. **Edge rendering reads combiner's statusModel** - Gets accurate operational status
4. **Animation reflects configuration** - Animated only if combiner has mappings

**The combiner's operational status is the source of truth for whether that specific data transformation path is configured.**

---

## Validation

### Manual Testing Steps

1. Create an adapter with tags
2. Create Combiner 1 connected to adapter (no mappings)
3. Create Combiner 2 connected to adapter (with TAG mapping)
4. Observe:
   - Edge to Combiner 1: GREEN but NOT ANIMATED ✅
   - Edge to Combiner 2: GREEN and ANIMATED ✅

### Expected Behavior

- **Color** (runtime) shows if source is active
- **Animation** (operational) shows if target combiner is configured

---

## Integration Points

This fix works seamlessly with:

- ✅ NodeCombiner's operational status computation
- ✅ Pulse → Asset Mapper per-edge status (previous fix)
- ✅ Dual-status visual rendering (Phase 5)
- ✅ Status propagation system
- ✅ React Flow node data storage

---

## Summary

The issue was **not** in the combiner's operational status computation (that was correct), but in the **edge rendering not using it**.

By applying the same per-edge pattern we used for Pulse connections, all combiner edges now correctly show:

- **Animation ON** when combiner has mappings (operational ACTIVE)
- **Animation OFF** when combiner has no mappings (operational INACTIVE)

**The fix is minimal, consistent, and leverages the existing combiner status computation.** ✅

---

## Key Insight

**Per-edge operational status should check the TARGET node** when the target determines whether the data transformation is configured (like combiners and mappers). The source node's operational status is irrelevant for that specific edge - what matters is whether the **destination** is ready to receive and transform data.
