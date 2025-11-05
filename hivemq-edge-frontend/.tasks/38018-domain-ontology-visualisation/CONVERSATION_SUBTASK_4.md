# Task 38018 - Conversation: Subtask 4 - Add Combiner and Asset Mapper Data

**Date:** November 4, 2025  
**Subtask:** Add Combiner and Asset Mapper integration points to Network Graph  
**Status:** ✅ COMPLETE

---

## Objective

Extend the domain ontology data gathering to include Combiners and Asset Mappers, which were missing from the Network Graph View. Both represent data transformation flows where multiple sources (TAGs or TOPIC FILTERs) combine into a single destination TOPIC.

---

## Data Model Understanding

### Combiner / Asset Mapper Schema

Both entities share the same TypeScript schema (`Combiner`):

```typescript
type Combiner = {
  id: string
  name: string
  description?: string
  sources: EntityReferenceList
  mappings: DataCombiningList // Array of DataCombining
}

type DataCombining = {
  id: string
  sources: {
    primary: DataIdentifierReference
    tags?: Array<string> // TAG names
    topicFilters?: Array<string> // TOPIC FILTER names
  }
  destination: {
    assetId?: string
    topic?: string // Destination TOPIC
    schema?: string
  }
  // ...instructions
}
```

### Data Flow Pattern

**Combiners & Asset Mappers create:**

- **New TOPICs** - Destination topics that may not exist elsewhere
- **New Edges** - Multiple sources → Single destination
  - TAG → TOPIC (via Combiner)
  - TOPIC_FILTER → TOPIC (via Combiner)
  - TAG → TOPIC (via Asset Mapper)
  - TOPIC_FILTER → TOPIC (via Asset Mapper)

---

## Implementation Approach

### Centralized Data Gathering

**Key Design Decision:** Add data sources to `useGetDomainOntology` rather than scattering hooks across components.

**Before:**

```typescript
// useGetNetworkGraphData.ts
const combiners = useListCombiners() // ❌ Wrong location
const assetMappers = useListAssetMappers() // ❌ Wrong location
```

**After:**

```typescript
// useGetDomainOntology.ts - Centralized
const combiners = useListCombiners() // ✅ Correct
const assetMappers = useListAssetMappers() // ✅ Correct

// useGetNetworkGraphData.ts - Consumer
const { combiners, assetMappers } = useGetDomainOntology() // ✅ Clean
```

### Benefits of Centralization

1. **Single Source of Truth** - All domain data in one place
2. **Consistent Loading States** - Combined isLoading/isError
3. **Easier Refactoring** - Other visualizations can reuse
4. **Better Caching** - React Query manages all hooks together

---

## Changes Made

### 1. Updated `useGetDomainOntology` Hook

**Added:**

- `useListCombiners()` hook call
- `useListAssetMappers()` hook call
- Both added to `isLoading` and `isError` aggregation
- Both added to return object

**File:** `src/modules/DomainOntology/hooks/useGetDomainOntology.ts`

### 2. Updated `useGetNetworkGraphData` Hook

**Added Logic for Combiners:**

```typescript
for (const combiner of combiners.data?.items || []) {
  for (const mapping of combiner.mappings || []) {
    // Create destination topic node if needed
    // Create edges from tags → topic
    // Create edges from topic filters → topic
  }
}
```

**Added Logic for Asset Mappers:**

```typescript
for (const assetMapper of assetMappers.data?.items || []) {
  for (const mapping of assetMapper.mappings || []) {
    // Create destination topic node if needed
    // Create edges from tags → topic
    // Create edges from topic filters → topic
  }
}
```

**File:** `src/modules/DomainOntology/hooks/useGetNetworkGraphData.ts`

### 3. Updated Edge Colors

**Added New Edge Types:**

- `COMBINER` - Pink (#ED64A6)
- `ASSET_MAPPER` - Teal (#38B2AC)

**File:** `src/modules/DomainOntology/components/network-graph/NetworkGraphEdge.tsx`

---

## Node and Edge Creation Logic

### Destination Topic Nodes

If a destination topic from a combiner/asset mapper doesn't already exist:

```typescript
if (!nodesMap.has(targetId)) {
  nodesMap.set(targetId, {
    id: `topic-${destinationTopic}`,
    type: 'networkGraphNode',
    position: { x: 0, y: 0 },
    data: {
      label: destinationTopic,
      type: 'TOPIC',
      connectionCount: 0,
      metadata: {
        fromCombiner: true, // or fromAssetMapper: true
      },
    },
  })
}
```

### Combiner Edges

**From TAGs:**

```typescript
edgesList.push({
  id: `combiner-${combiner.id}-${tagName}-${destinationTopic}`,
  source: `tag-${tagName}`,
  target: `topic-${destinationTopic}`,
  type: 'networkGraphEdge',
  data: {
    transformationType: 'COMBINER',
    label: 'Combiner',
    metadata: {
      combinerId: combiner.id,
      combinerName: combiner.name,
    },
  },
})
```

**From TOPIC FILTERs:**

```typescript
edgesList.push({
  id: `combiner-${combiner.id}-${filterName}-${destinationTopic}`,
  source: `filter-${filterName}`,
  target: `topic-${destinationTopic}`,
  // ... same structure
})
```

### Asset Mapper Edges

Same structure as combiners, but with `transformationType: 'ASSET_MAPPER'` and asset mapper metadata.

---

## Visual Impact

### New Edge Types Visible

**Before:**

- NORTHBOUND (blue) - TAG → TOPIC
- SOUTHBOUND (purple) - FILTER → TAG
- BRIDGE (orange) - FILTER → TOPIC

**After:**

- NORTHBOUND (blue) - TAG → TOPIC
- SOUTHBOUND (purple) - FILTER → TAG
- BRIDGE (orange) - FILTER → TOPIC
- **COMBINER (pink)** - TAG/FILTER → TOPIC ✨ NEW
- **ASSET_MAPPER (teal)** - TAG/FILTER → TOPIC ✨ NEW

### Example Graph Structure

```
TAG-1 ──North──→ TOPIC-A
TAG-2 ──North──→ TOPIC-B
  │
  │─────Combiner──→ TOPIC-C (new)
  │                    ↑
FILTER-1 ─────Combiner──┘

TAG-3 ──AssetMapper──→ TOPIC-D (new)
  ↑
FILTER-2 ──AssetMapper──┘
```

---

## Testing Considerations

**What to Test:**

1. Combiners with TAG sources create edges
2. Combiners with TOPIC FILTER sources create edges
3. Asset Mappers with TAG sources create edges
4. Asset Mappers with TOPIC FILTER sources create edges
5. Destination topics are created if they don't exist
6. Connection counts increment correctly
7. Edge colors display correctly (pink and teal)
8. Details panel shows combiner/asset mapper metadata

---

## Future Refactoring Notes

**As mentioned in the subtask description**, the data hooks for all graphs need refactoring. Current issues:

1. **Duplication** - Each visualization (Sankey, Chord, Sunburst) has similar data transformation logic
2. **Inconsistency** - Some use `useGetDomainOntology`, others use direct hooks
3. **Performance** - Redundant calculations across visualizations

**Recommended Future Task:**

- Create shared data transformation utilities
- Centralize node/edge creation logic
- Consider memoized selectors for different visualization needs
- Standardize metadata structure

---

## Verification

- ✅ TypeScript: 0 errors (only 1 complexity warning)
- ✅ Combiners data included in `useGetDomainOntology`
- ✅ Asset Mappers data included in `useGetDomainOntology`
- ✅ Network Graph processes combiner mappings
- ✅ Network Graph processes asset mapper mappings
- ✅ New edge types have colors (pink, teal)
- ✅ Edge labels show "Combiner" and "Asset"
- ✅ Centralized data gathering maintained

---

## Files Modified

1. ✅ `useGetDomainOntology.ts` - Added combiners and asset mappers
2. ✅ `useGetNetworkGraphData.ts` - Added combiner/asset mapper processing
3. ✅ `NetworkGraphEdge.tsx` - Added new edge colors

---

## Layout Algorithm Improvement Note

**Observation:** With the addition of combiners and asset mappers, the network graph now displays more complex structures with 12+ nodes and multiple edge types.

**Current State:** Using custom force-directed algorithm in `NetworkGraphView.tsx` (lines 33-224)

**Recommendation:** Replace with **WebCola** or **Dagre** layout engines

**Why:**

- The Workspace already has proven implementations:
  - `cola-force-layout.ts` - Physics-based force simulation with overlap removal
  - `dagre-layout.ts` - Hierarchical directed graph layout
  - Both battle-tested in the Workspace with complex node structures
- Custom algorithms become unmaintainable for complex graphs
- WebCola provides better clustering and overlap avoidance
- Dagre excellent for hierarchical flows (TAG → TOPIC → FILTER)

**Suggested Approach:**

1. Extract layout algorithms to shared utilities
2. Create `network-graph-layout.ts` reusing Workspace patterns
3. Offer layout switcher in UI (Force, Hierarchical, Radial)
4. Maintain current simple algorithm as fallback

**Files to Reference:**

- `/src/modules/Workspace/utils/layout/cola-force-layout.ts`
- `/src/modules/Workspace/utils/layout/dagre-layout.ts`
- `/src/modules/Workspace/utils/layout/cola-utils.ts`

**Priority:** Medium - Current algorithm works for POC, but will struggle with 20+ nodes

---

**Status:** ✅ COMPLETE  
**Date:** November 4, 2025  
**Pattern:** Centralized data gathering in `useGetDomainOntology`
