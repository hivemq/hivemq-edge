# Fix Summary: Per-Edge Operational Status for Pulse → Asset Mapper

**Date:** October 26, 2025  
**Status:** ✅ Complete

---

## Problem

When a PULSE node was connected to multiple ASSET MAPPERs, all edges showed the same operational status (animated or not), even if only some asset mappers had valid mappings.

**User's Report:**

> "If a PULSE node is connected to an ASSET MAPPER without any mapping, the node should not be Operationally ACTIVE and the link between them neither."

---

## What Was Wrong

The edge rendering code in `updateEdgesStatusWithModel()` was applying the Pulse node's **overall** operational status to ALL outbound edges:

```typescript
// BEFORE - All edges get same status
if (source.type === NodeTypes.PULSE_NODE) {
  newEdges.push({
    ...edge,
    ...getEdgeStatusFromModel(statusModel, true, theme), // ← Same for ALL
  })
}
```

---

## The Fix

Modified the Pulse edge handling to use **per-edge** operational status based on the target asset mapper's configuration:

```typescript
// AFTER - Each edge gets target's operational status
if (source.type === NodeTypes.PULSE_NODE) {
  const target = getNode(edge.target)

  if (target?.type === NodeTypes.COMBINER_NODE) {
    const isAssetMapper = /* check if has Pulse source */

    if (isAssetMapper && targetStatusModel) {
      const edgeStatusModel = {
        runtime: pulseNode.statusModel.runtime,        // From source
        operational: assetMapper.statusModel.operational // From target ← FIX!
      }
      // Use edge-specific status
    }
  }

  // Other edges use Pulse overall status
}
```

---

## How It Works Now

### Status Composition for Pulse → Asset Mapper Edges

| Component              | Source       | Meaning                                        |
| ---------------------- | ------------ | ---------------------------------------------- |
| **Runtime Status**     | Pulse Node   | Is the Pulse agent active/connected?           |
| **Operational Status** | Asset Mapper | Does this specific mapper have valid mappings? |

### Visual Result

```
PULSE Agent (runtime: ACTIVE)
  │
  ├─> Asset Mapper 1 (no mappings)
  │     Edge: GREEN (runtime) + NO ANIMATION (operational INACTIVE) ✅
  │
  └─> Asset Mapper 2 (has valid mappings)
        Edge: GREEN (runtime) + ANIMATED (operational ACTIVE) ✅
```

---

## Files Changed

### Modified (1 file)

- `src/modules/Workspace/utils/status-utils.ts`
  - Added `Combiner` and `EntityType` imports
  - Updated Pulse edge handling (~35 new lines)
  - Per-edge operational status from target asset mapper

### Created (2 files)

- `src/modules/Workspace/utils/pulse-edge-status.spec.ts` (new tests)
- `.tasks/32118-workspace-status/FIX_PER_EDGE_STATUS.md` (documentation)

---

## Test Coverage

### Existing Tests (Still Passing ✅)

- 15 tests in `edge-operational-status.utils.spec.ts`
- All workspace utils tests passing
- No regressions

### New Tests (2 tests)

- Test 1: Edge animation based on target asset mapper status
- Test 2: Non-asset-mapper combiners use Pulse overall status

---

## Validation Checklist

✅ **Compilation** - No errors, only pre-existing warnings  
✅ **Logic** - Per-edge status correctly computed  
✅ **Tests** - Existing tests pass, new tests added  
✅ **Documentation** - Comprehensive docs created

---

## What This Means for Users

### Before Fix ❌

User couldn't tell which specific Pulse → Asset Mapper connection was operational. All edges looked the same.

### After Fix ✅

User can now visually identify:

- Which asset mappers have valid mappings (animated edges)
- Which asset mappers need configuration (non-animated edges)
- Runtime status still shows if Pulse is connected (edge color)

---

## Key Insight

**Operational status should be per-connection, not per-node**, when the configuration is defined at the connection level (e.g., asset mapper mappings).

The Pulse node can be "overall operational" (has some valid mappers) while individual edges can be "not operational" (specific mapper has no mappings).

---

## Integration Points

This fix integrates with:

1. **NodePulse** - Computes overall operational status
2. **NodeCombiner** - Computes individual mapper operational status
3. **updateEdgesStatusWithModel** - Renders per-edge visual feedback
4. **getEdgeStatusFromModel** - Applies dual-status styling (color + animation)

---

## Summary

✅ Fixed per-edge operational status for Pulse → Asset Mapper connections  
✅ Each edge now reflects its target's configuration state  
✅ Visual feedback is accurate and granular  
✅ No breaking changes to existing functionality  
✅ Well-tested and documented

**Result:** Users can now clearly see which Pulse → Asset Mapper connections are operational and which need configuration.
