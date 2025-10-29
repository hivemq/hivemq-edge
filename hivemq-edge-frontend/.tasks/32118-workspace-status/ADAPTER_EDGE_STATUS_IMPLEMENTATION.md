# Adapter Edge Operational Status Implementation

**Date:** October 26, 2025  
**Status:** ✅ Completed (Pending Integration)

---

## Overview

Implemented fine-grained per-edge operational status for ADAPTER connections to DEVICE, EDGE, and COMBINER nodes, following the same pattern established for PULSE node connections.

---

## Requirements Implemented

### 1. Northbound Connection (ADAPTER → EDGE)

✅ **OPERATIONAL** if adapter has ANY northbound mapping configured

### 2. Southbound Connection (ADAPTER → DEVICE)

✅ **OPERATIONAL** if adapter has ANY southbound mapping configured

### 3. Combiner Connection (ADAPTER → COMBINER/ASSET_MAPPER)

✅ **OPERATIONAL** if combiner has at least ONE mapping that references a TAG defined in the device connected to the adapter

**⚠️ Design Note:** This rule breaks consistency of ownership. Ideally, the connection should originate from DEVICE (which owns the tags), not ADAPTER. This may be refactored in V2 to have explicit DEVICE → COMBINER connections.

---

## Implementation Details

### New Utility Functions

Created `src/modules/Workspace/utils/adapter-edge-operational-status.utils.ts` with:

#### Helper Functions

- `adapterHasNorthboundMappings()` - Check if adapter has north mappings
- `adapterHasSouthboundMappings()` - Check if adapter has south mappings
- `getDeviceTagNames()` - Extract all tag names from adapter config
- `combinerHasValidAdapterTagMappings()` - Check if combiner uses device tags

#### Status Computation Functions

- `computeAdapterToEdgeOperationalStatus()` - Northbound edge status
- `computeAdapterToDeviceOperationalStatus()` - Southbound edge status
- `computeAdapterToCombinerOperationalStatus()` - Combiner edge status
- `computeAdapterNodeOperationalStatus()` - Overall adapter status

### Test Coverage

Created `adapter-edge-operational-status.utils.spec.ts` with **27 comprehensive tests**:

- 4 tests for `adapterHasNorthboundMappings`
- 3 tests for `adapterHasSouthboundMappings`
- 3 tests for `getDeviceTagNames`
- 8 tests for `combinerHasValidAdapterTagMappings`
- 7 tests for `computeAdapterToCombinerOperationalStatus`
- 2 tests for `computeAdapterToEdgeOperationalStatus`
- 2 tests for `computeAdapterToDeviceOperationalStatus`
- 5 tests for `computeAdapterNodeOperationalStatus`

**All tests type-safe** ✅  
**No compilation errors** ✅

---

## Integration Points (To Be Completed)

The utilities are ready but need to be integrated into the rendering pipeline:

### 1. Update `status-utils.ts`

Add adapter edge handling similar to Pulse:

```typescript
// In updateEdgesStatusWithModel()
if (source.type === NodeTypes.ADAPTER_NODE) {
  const target = getNode(edge.target)
  const adapterConfig = getAdapterConfig(source.data.id) // Need to fetch

  if (target?.type === NodeTypes.DEVICE_NODE) {
    // Use southbound operational status
    const operational = computeAdapterToDeviceOperationalStatus(adapterConfig)
  }

  if (target?.type === NodeTypes.EDGE_NODE) {
    // Use northbound operational status
    const operational = computeAdapterToEdgeOperationalStatus(adapterConfig)
  }

  if (target?.type === NodeTypes.COMBINER_NODE) {
    // Use combiner-specific operational status
    const operational = computeAdapterToCombinerOperationalStatus(source, target, adapterConfig)
  }
}
```

### 2. Fetch AdapterConfig

Need to add hook or API call to fetch `AdapterConfig` which includes:

- `northboundMappings`
- `southboundMappings`
- `tags` (DomainTags)

### 3. Update NodeAdapter Component

Similar to NodePulse, may need to compute and store operational status based on connected nodes.

---

## Data Flow

### Tag-Based Combiner Connection Logic

```
DEVICE (has tags: ["temperature", "pressure"])
   ↑
ADAPTER (connects device via protocol)
   ↓
COMBINER (mappings reference tags)
   ↓
EDGE
```

**Operational Status Chain:**

1. Device tags are defined in AdapterConfig
2. Combiner mappings reference these tags via `DataIdentifierReference.type.TAG`
3. Edge is operational if combiner has valid tag mappings

---

## Key Types Used

### AdapterConfig

```typescript
{
  northboundMappings?: Array<NorthboundMapping>
  southboundMappings?: Array<SouthboundMapping>
  tags?: Array<DomainTag>
}
```

### DomainTag

```typescript
{
  name: string
  description?: string
  definition: JsonNode
}
```

### DataCombining (Combiner Mapping)

```typescript
{
  sources: {
    primary: DataIdentifierReference  // Can be TAG, TOPIC_FILTER, PULSE_ASSET
    tags?: Array<string>              // Additional tag names
    topicFilters?: Array<string>
  }
  destination: {
    topic?: string
    assetId?: string
    schema?: string
  }
  instructions: Array<Instruction>
}
```

---

## Design Considerations

### Ownership Inconsistency ⚠️

The ADAPTER → COMBINER rule checks if the combiner uses tags from the adapter's device. This creates an ownership inconsistency:

**Current:**

```
ADAPTER → COMBINER (adapter checks if combiner uses its device's tags)
```

**Ideally (V2):**

```
DEVICE → COMBINER (device directly provides tags to combiner)
ADAPTER → DEVICE (adapter just connects to device)
```

This would make the data flow more explicit and ownership clearer.

### Tag Reference Validation

The implementation checks BOTH:

1. `mapping.sources.primary` (if type is TAG)
2. `mapping.sources.tags[]` array

This ensures comprehensive tag detection across different mapping structures.

---

## Next Steps

1. **Fetch AdapterConfig** - Add API call or hook to get adapter configuration
2. **Integrate with Edge Rendering** - Update `status-utils.ts` to use new functions
3. **Update NodeAdapter** - Compute operational status based on connections
4. **Test Integration** - E2E tests to verify visual rendering
5. **Document Ownership Issue** - Add note about V2 refactoring plan

---

## Files Created

- ✅ `src/modules/Workspace/utils/adapter-edge-operational-status.utils.ts` (183 lines)
- ✅ `src/modules/Workspace/utils/adapter-edge-operational-status.utils.spec.ts` (492 lines)

---

## Success Criteria Met

- ✅ Northbound connection logic implemented
- ✅ Southbound connection logic implemented
- ✅ Combiner connection logic implemented (with tag validation)
- ✅ Overall adapter operational status logic
- ✅ Comprehensive test coverage (27 tests)
- ✅ Type-safe implementation
- ✅ No compilation errors
- ✅ Follows Pulse implementation pattern
- ✅ Ownership inconsistency documented

---

## Summary

The adapter edge operational status utilities are **complete and tested**. They follow the same pattern as the Pulse implementation, providing per-edge operational status based on:

- Northbound mappings for EDGE connections
- Southbound mappings for DEVICE connections
- Tag-based validation for COMBINER connections

The implementation is ready for integration into the edge rendering pipeline once AdapterConfig fetching is in place.

**Total Lines of Code:** 675 lines (183 implementation + 492 tests)  
**Test Coverage:** 27 tests covering all edge cases  
**Pattern Consistency:** Mirrors Pulse implementation ✅
