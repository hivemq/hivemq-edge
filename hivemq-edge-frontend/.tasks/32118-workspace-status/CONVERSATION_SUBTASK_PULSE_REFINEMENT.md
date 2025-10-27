# Subtask: Fine-Tuning Pulse Agent Operational Status

**Task:** 32118-workspace-status  
**Date:** October 26, 2025  
**Status:** ✅ Completed

## Overview

Implemented fine-grained operational status logic for PULSE_AGENT nodes and their connections to ASSET_MAPPER (combiner) nodes. The operational status is now based on whether asset mappers have valid mappings that reference mapped Pulse assets, rather than just checking if the Pulse agent has mapped assets.

## Problem Statement

The initial implementation (from Phase 4) determined a Pulse node's operational status based solely on whether it had mapped assets:

```typescript
// OLD LOGIC - Too simple
const hasMappedAssets = assetStats.mapped > 0
const operational = hasMappedAssets ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
```

This didn't account for the actual data flow topology:

- Pulse agents connect to Asset Mapper nodes (a special type of combiner)
- Asset mappers have mappings that reference specific assets via `assetId`
- A Pulse agent should only be operational if at least one connected asset mapper has mappings referencing valid (MAPPED) assets

## Solution Design

### 1. New Utility Functions

Created `src/modules/Workspace/utils/edge-operational-status.utils.ts` with three key functions:

#### `combinerHasValidPulseAssetMappings()`

Checks if a combiner (asset mapper) has valid mappings:

- Must have at least one mapping configured
- Must be connected to a Pulse agent (has `EntityType.PULSE_AGENT` in sources)
- At least one mapping must reference a valid asset ID
- The referenced asset must be in `MAPPED` status (not `UNMAPPED`)

#### `computePulseToAssetMapperOperationalStatus()`

Computes operational status for a specific edge (Pulse → Asset Mapper):

- Returns `ERROR` if node types don't match or combiner isn't an asset mapper
- Returns `INACTIVE` if asset mapper has no valid mappings
- Returns `ACTIVE` if asset mapper has at least one valid mapping

#### `computePulseNodeOperationalStatus()`

Computes operational status for the entire Pulse node:

- Takes all connected combiner nodes
- Returns `INACTIVE` if no connections exist
- Returns `ACTIVE` if at least one connected combiner has valid mappings
- Returns `INACTIVE` if no combiner has valid mappings

### 2. Updated NodePulse Component

Modified `src/modules/Workspace/components/nodes/NodePulse.tsx`:

```typescript
// Get outbound connections to asset mappers using React Flow's efficient hooks
const connections = useNodeConnections({ id, type: 'source' })
const connectedNodes = useNodesData<NodeCombinerType>(connections.map((connection) => connection.target))

// Compute operational status based on connected asset mappers
const statusModel = useMemo(() => {
  const validCombiners = (connectedNodes || []).filter(
    (node): node is NodeCombinerType => node !== null && node !== undefined
  )

  const operational = allAssets?.items
    ? computePulseNodeOperationalStatus(validCombiners, allAssets.items)
    : OperationalStatus.INACTIVE

  return createPulseStatusModel(data.status, operational)
}, [data.status, connectedNodes, allAssets])
```

Key improvements:

- Uses React Flow's `useNodeConnections()` for efficient connection tracking
- Uses React Flow's `useNodesData()` to get connected node data
- Automatically re-computes when connections or assets change
- Properly filters null/undefined nodes

## Implementation Details

### Type Definitions

Used existing types from the codebase:

- `NodeCombinerType` - typed combiner nodes from React Flow
- `ManagedAsset` - Pulse asset with mapping status from API
- `Combiner` - combiner data structure with sources and mappings
- `DataCombining` - mapping definition with source and destination

### Data Flow

1. **Pulse Agent** loads all managed assets via `useListManagedAssets()`
2. **React Flow hooks** track outbound connections to combiners
3. **Status computation** checks each connected combiner for valid mappings
4. **Status update** triggers React Flow node data update
5. **Visual rendering** shows status via colors and animations

### Edge Cases Handled

- ✅ No connected combiners → `INACTIVE`
- ✅ Connected combiner with no mappings → `INACTIVE`
- ✅ Connected combiner referencing unmapped asset → `INACTIVE`
- ✅ Connected combiner referencing non-existent asset → `INACTIVE`
- ✅ Multiple combiners, at least one valid → `ACTIVE`
- ✅ Combiner not an asset mapper (no Pulse source) → `ERROR`
- ✅ Null/undefined nodes in connection list → filtered out

## Test Coverage

Created `edge-operational-status.utils.spec.ts` with **15 comprehensive tests**:

### `combinerHasValidPulseAssetMappings` (7 tests)

- ✅ Returns false if combiner has no mappings
- ✅ Returns false if combiner has no Pulse source
- ✅ Returns true if combiner has valid mapped asset reference
- ✅ Returns false if mapping references unmapped asset
- ✅ Returns false if mapping references non-existent asset
- ✅ Returns false if mapping has no assetId
- ✅ Returns true if at least one mapping is valid (mixed scenario)

### `computePulseToAssetMapperOperationalStatus` (5 tests)

- ✅ Returns ERROR if source is not a Pulse node
- ✅ Returns ERROR if target is not a Combiner node
- ✅ Returns ERROR if combiner is not an asset mapper
- ✅ Returns INACTIVE if asset mapper has no valid mappings
- ✅ Returns ACTIVE if asset mapper has valid mapped asset reference

### `computePulseNodeOperationalStatus` (3 tests)

- ✅ Returns INACTIVE if Pulse node has no connected combiners
- ✅ Returns INACTIVE if no connected combiner has valid mappings
- ✅ Returns ACTIVE if at least one combiner has valid mappings

**All 15 tests passing** ✅

## Performance Considerations

### React Flow Optimization

- Uses `useNodeConnections()` - subscribes only to connection changes for this node
- Uses `useNodesData()` - efficient bulk data fetching with React Flow's internal store
- Memoized computation with `useMemo()` - only recalculates when dependencies change

### Dependency Array

```typescript
useMemo(() => { ... }, [data.status, connectedNodes, allAssets])
```

- `data.status` - PulseStatus from API
- `connectedNodes` - React Flow's optimized node data array
- `allAssets` - managed assets from API hook

Only re-renders when these values actually change, not on every React Flow update.

## Future Enhancements

### Per-Edge Operational Status

The `computePulseToAssetMapperOperationalStatus()` function is already implemented for future use when we need per-edge status visualization.

Possible use cases:

- Show which specific edge (connection) is operational vs. inactive
- Color individual edges differently based on their mapping status
- Add tooltips showing why an edge is inactive

### Edge Data Extension

Could extend the edge data structure to include operational status:

```typescript
export type EdgeWithStatus = Edge<{
  operationalStatus: OperationalStatus
  lastChecked: string
}>
```

### Status Caching

For large graphs with many Pulse agents and asset mappers, could implement:

- Debounced status computation
- Status result caching per combiner
- Incremental updates when only one mapping changes

## Files Changed

### Created

- ✅ `src/modules/Workspace/utils/edge-operational-status.utils.ts` - Core logic (108 lines)
- ✅ `src/modules/Workspace/utils/edge-operational-status.utils.spec.ts` - Tests (425 lines)

### Modified

- ✅ `src/modules/Workspace/components/nodes/NodePulse.tsx` - Updated status computation

## Integration Notes

### NodeCombiner Integration

The `NodeCombiner.tsx` component already computes its operational status based on whether it has mappings:

```typescript
const operational = hasMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
```

This works well with the Pulse logic:

- Pulse checks if combiner has valid asset mappings
- Combiner independently shows if it has any mappings configured
- Both statuses are compatible and complementary

### StatusModel Propagation

The status flows through the graph:

1. **Pulse Node** computes its operational status
2. **React Flow** stores `statusModel` in node data
3. **StatusListener** updates edge colors and animations
4. **Visual rendering** shows the complete status picture

## Success Criteria Met

- ✅ Fine-grained operational status for Pulse → Asset Mapper connections
- ✅ Status based on valid asset references in mappings
- ✅ Efficient React Flow integration with minimal re-renders
- ✅ Comprehensive test coverage (15 tests)
- ✅ Type-safe implementation
- ✅ Backward compatible with existing status system
- ✅ No compilation errors
- ✅ All tests passing

## Conclusion

This subtask successfully refined the Pulse Agent operational status logic to account for the actual data flow topology through asset mappers. The implementation is efficient, well-tested, and ready for future enhancements like per-edge status visualization.

The key insight: **A Pulse agent is only operational if its assets are actually being used in downstream mappings**, not just if they exist and are mapped.
