# Fix: Per-Edge Operational Status for Pulse → Asset Mapper Connections

**Date:** October 26, 2025  
**Issue:** Pulse node edges to asset mappers were all using the same operational status  
**Status:** ✅ Fixed

## Problem Description

When a PULSE node was connected to multiple ASSET MAPPERs, ALL edges showed the same operational status (animated or not), even if some asset mappers had valid mappings and others didn't.

### Example Scenario (Before Fix)

```
PULSE Agent (operational: ACTIVE - has some mappers with valid mappings)
  ├─> Asset Mapper 1 (NO mappings) ❌
  │     Edge was ANIMATED ❌ WRONG!
  │
  └─> Asset Mapper 2 (HAS valid mappings) ✅
        Edge was ANIMATED ✅ Correct
```

**Problem:** Both edges were animated because they used the Pulse node's overall operational status.

## Root Cause

In `status-utils.ts`, the `updateEdgesStatusWithModel()` function was applying the source Pulse node's status to ALL outbound edges:

```typescript
// OLD CODE - WRONG
if (source.type === NodeTypes.PULSE_NODE) {
  newEdges.push({
    ...edge,
    ...getEdgeStatusFromModel(statusModel, true, theme), // Same status for ALL edges
  })
  return
}
```

## Solution

Modified the Pulse edge handling to check the **target** node's operational status when the target is an asset mapper:

```typescript
// NEW CODE - CORRECT
if (source.type === NodeTypes.PULSE_NODE) {
  const target = getNode(edge.target)

  // For Pulse → Asset Mapper connections, use the target's operational status
  if (target?.type === NodeTypes.COMBINER_NODE) {
    const targetData = target.data as WithStatusModel
    const targetStatusModel = targetData.statusModel

    const targetCombiner = target.data as Combiner
    const isAssetMapper = targetCombiner.sources.items.some((s) => s.type === EntityType.PULSE_AGENT)

    if (isAssetMapper && targetStatusModel) {
      // Create edge-specific status model using:
      // - Runtime status from Pulse node (is the source active?)
      // - Operational status from target asset mapper (does it have valid mappings?)
      const edgeStatusModel: NodeStatusModel = {
        runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
        operational: targetStatusModel.operational, // ← Uses TARGET's status!
        source: 'DERIVED' as const,
      }

      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(edgeStatusModel, true, theme),
      })
      return
    }
  }

  // For other Pulse edges, use Pulse node's overall status
  newEdges.push({
    ...edge,
    ...getEdgeStatusFromModel(statusModel, true, theme),
  })
  return
}
```

### Key Changes

1. **Detect Asset Mapper targets** - Check if target is a combiner with Pulse source
2. **Use target's operational status** - Asset mapper's own status (has mappings or not)
3. **Keep source's runtime status** - Pulse node's runtime (is it active/connected)
4. **Create per-edge status model** - Each edge gets its own combined status

## How It Works Now

### Edge Status Composition

For Pulse → Asset Mapper edges:

- **Runtime Status** ← from Pulse node (is source active?)
- **Operational Status** ← from Asset Mapper (does it have valid mappings?)

### Visual Result

```
PULSE Agent (runtime: ACTIVE, operational: varies)
  ├─> Asset Mapper 1 (operational: INACTIVE - no mappings)
  │     Edge: Color=GREEN (runtime ACTIVE), Animation=OFF (operational INACTIVE) ✅
  │
  └─> Asset Mapper 2 (operational: ACTIVE - has valid mappings)
        Edge: Color=GREEN (runtime ACTIVE), Animation=ON (operational ACTIVE) ✅
```

## Status Flow

### 1. Pulse Node Computation (NodePulse.tsx)

```typescript
// Pulse node checks ALL connected asset mappers
const operational = computePulseNodeOperationalStatus(validCombiners, allAssets.items)
// Returns ACTIVE if ANY mapper has valid mappings
```

### 2. Asset Mapper Computation (NodeCombiner.tsx)

```typescript
// Each asset mapper checks its OWN mappings
const hasMappings = data.mappings.items.length > 0
const operational = hasMappings ? ACTIVE : INACTIVE
```

### 3. Edge Rendering (status-utils.ts)

```typescript
// For Pulse → Asset Mapper edge:
const edgeStatus = {
  runtime: pulseNode.statusModel.runtime, // From source
  operational: assetMapper.statusModel.operational, // From target ← KEY CHANGE!
}
```

## Files Changed

### Modified

- ✅ `src/modules/Workspace/utils/status-utils.ts` - Fixed edge rendering logic
  - Added `Combiner` and `EntityType` imports
  - Updated Pulse edge handling (lines ~470-500)
  - Per-edge status computation for asset mappers

### Created

- ✅ `src/modules/Workspace/utils/pulse-edge-status.spec.ts` - Tests for the fix (2 comprehensive tests)

## Test Coverage

Created 2 new tests:

### Test 1: Asset Mapper Operational Status

```typescript
it('should use asset mapper operational status for edge animation', () => {
  // Pulse ACTIVE, Mapper1 has NO mappings, Mapper2 HAS mappings
  // Edge1 should NOT animate (mapper1 operational INACTIVE)
  // Edge2 should animate (mapper2 operational ACTIVE)
})
```

### Test 2: Non-Asset-Mapper Combiners

```typescript
it('should handle non-asset-mapper combiners normally', () => {
  // Regular combiner (not asset mapper) should use Pulse overall status
})
```

## Benefits

1. **Accurate Visual Feedback** - Users can see which specific connections are operational
2. **Per-Edge Granularity** - Each edge reflects its target's configuration status
3. **Consistent Logic** - Asset mapper status computation remains in NodeCombiner
4. **No Breaking Changes** - Existing operational status computation unchanged
5. **Extensible** - Pattern can be applied to other node type connections

## Edge Cases Handled

- ✅ Asset mapper with no mappings → edge not animated
- ✅ Asset mapper with mappings referencing unmapped assets → handled by NodeCombiner
- ✅ Multiple asset mappers with mixed states → each edge independent
- ✅ Non-asset-mapper combiners → use Pulse overall status
- ✅ Pulse → Edge node connections → use Pulse overall status

## Validation

### Manual Testing Steps

1. Create a Pulse agent with mapped assets
2. Create Asset Mapper 1 connected to Pulse (no mappings)
3. Create Asset Mapper 2 connected to Pulse (with valid mappings)
4. Observe:
   - Edge to Mapper 1 should be GREEN but NOT ANIMATED
   - Edge to Mapper 2 should be GREEN and ANIMATED

### Expected Behavior

- **Color** (runtime status) reflects Pulse agent connectivity
- **Animation** (operational status) reflects individual asset mapper configuration

## Related Work

This fix complements the earlier work:

- ✅ Phase 4: Operational status detection for all node types
- ✅ Phase 5: Dual-status visual rendering (color + animation)
- ✅ Pulse Refinement: Per-node operational status based on connected mappers
- ✅ **This Fix**: Per-edge operational status for precise visual feedback

## Conclusion

The fix ensures that **each Pulse → Asset Mapper edge has its own operational status** based on whether that specific asset mapper has valid mappings, not whether the Pulse agent overall is operational. This provides accurate, per-connection visual feedback to users.

**Key Insight:** Operational status should reflect the **specific data transformation path**, not just the overall node state.
