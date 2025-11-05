# Domain Ontology: Data Model & Transformation Flows

**Task:** 38018-domain-ontology-visualisation  
**Document Type:** Technical Architecture & Data Model  
**Version:** 1.0  
**Date:** November 4, 2025

---

## Table of Contents

1. [Domain Model Overview](#domain-model-overview)
2. [Integration Points](#integration-points)
3. [Transformation Flows](#transformation-flows)
4. [Data Aggregation Layer](#data-aggregation-layer)
5. [Visualization Layer](#visualization-layer)
6. [Current Implementation Analysis](#current-implementation-analysis)
7. [Recommendations](#recommendations)

---

## Domain Model Overview

### Conceptual Model

HiveMQ Edge implements a **domain-driven data integration architecture** where:

1. **Devices** expose **TAGs** (data points)
2. **Adapters** transform TAGs into **TOPICs** (northbound mapping)
3. **Edge** subscribes to **TOPIC FILTERs**
4. **Bridges** map TOPICs between local and remote brokers
5. **Combiners** merge multiple sources into single outputs
6. **Southbound mappings** allow reverse flow (TOPIC FILTER → TAG)

### Graph Structure

```
┌──────────────────────────────────────────────────────────────┐
│                    WORKSPACE GRAPH                           │
│                                                              │
│  DEVICES ──→ ADAPTERS ──→ TOPICS ──→ EDGE ──→ LISTENERS    │
│     ↑           ↓           ↑         ↑                      │
│     │        TAGS ────→ COMBINERS ────┘                      │
│     │           ↑                                            │
│     └──────────┐│                                            │
│                ││                                            │
│    TOPIC FILTERS (Southbound)                               │
│                                                              │
│  BRIDGES ──→ HOSTS                                          │
│     ↓                                                        │
│  TOPICS (local/remote)                                      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Integration Points

### 1. TAG (Device Data Point)

**Definition:** Named data point exposed by a device through an adapter

**API Type:** `DomainTag`

**Schema:**

```typescript
interface DomainTag {
  name: string // Unique tag identifier (e.g., "sensor-1/temperature")
  adapterId: string // Parent adapter ID
  definedTags?: DefinedTag // Schema definition (data type, units)
}
```

**Properties:**

- Hierarchical naming: `{adapterId}/{tagName}` or `{adapterId}/{deviceId}/{tagName}`
- Can be used in northbound mappings (TAG → TOPIC)
- Can be target of southbound mappings (TOPIC FILTER → TAG)
- May have schema/metadata (data type, units, range)

**Example:**

```json
{
  "name": "modbus-adapter/device-1/temperature",
  "adapterId": "modbus-adapter",
  "definedTags": {
    "type": "float",
    "unit": "°C"
  }
}
```

---

### 2. TOPIC (MQTT Topic)

**Definition:** MQTT topic where data is published

**Usage Contexts:**

1. **Northbound target**: Result of TAG transformation
2. **Bridge endpoint**: Local/remote broker topics
3. **Combiner output**: Merged data destination

**Schema:**

```typescript
interface Topic {
  topic: string // MQTT topic string (e.g., "factory/floor1/temp")
}
```

**Properties:**

- Follows MQTT topic structure (`level1/level2/level3`)
- Can contain wildcards in filters (`+`, `#`)
- Target of northbound mappings
- Source for bridge subscriptions

**Example:**

```json
{
  "topic": "enterprise/manufacturing/sensor-data/temperature"
}
```

---

### 3. TOPIC FILTER (Edge Subscription)

**Definition:** MQTT topic subscription on the Edge broker

**API Type:** `TopicFilter`

**Schema:**

```typescript
interface TopicFilter {
  topicFilter: string // MQTT filter with wildcards
  description?: string
  strategy?: string
  maxQueuedMessages?: number
}
```

**Properties:**

- Supports MQTT wildcards (`+` single level, `#` multi-level)
- Can be source for southbound mappings (TOPIC FILTER → TAG)
- Edge operational status depends on having topic filters configured
- Can match multiple topics

**Example:**

```json
{
  "topicFilter": "enterprise/+/sensor-data/#",
  "description": "All sensor data from any department"
}
```

---

## Transformation Flows

### 1. Northbound Mapping (TAG → TOPIC)

**Purpose:** Transform device data points into MQTT topics for publication

**API Type:** `NorthboundMapping`

**Schema:**

```typescript
interface NorthboundMapping {
  tagName: string // Source TAG
  topic: string // Destination TOPIC
  maxQoS?: number
  messageFormat?: string // JSON, XML, etc.
}
```

**Flow:**

```
DEVICE TAG ──[Adapter Protocol]──→ ADAPTER ──[Northbound Mapping]──→ TOPIC ──→ EDGE BROKER
```

**Example:**

```json
{
  "tagName": "modbus-adapter/device-1/temperature",
  "topic": "factory/floor1/temperature",
  "maxQoS": 1,
  "messageFormat": "JSON"
}
```

**Visualization Considerations:**

- Most common flow in typical deployments
- One-to-many relationships possible (one TAG → multiple TOPICs)
- Should show transformation rules/format conversions

---

### 2. Southbound Mapping (TOPIC FILTER → TAG)

**Purpose:** Write back from MQTT topics to device data points

**API Type:** `SouthboundMapping`

**Schema:**

```typescript
interface SouthboundMapping {
  topicFilter: string // Source TOPIC FILTER
  tagName: string // Destination TAG
  maxQoS?: number
}
```

**Flow:**

```
TOPIC FILTER ──[Match]──→ MQTT MESSAGE ──[Southbound Mapping]──→ TAG ──[Adapter Protocol]──→ DEVICE
```

**Example:**

```json
{
  "topicFilter": "commands/device-1/+",
  "tagName": "modbus-adapter/device-1/setpoint",
  "maxQoS": 1
}
```

**Visualization Considerations:**

- Reverse direction of northbound
- Bidirectional adapters need both north + south
- Topic filter wildcards add complexity (one filter → many tags)
- Should visually distinguish from northbound (color, arrow style)

---

### 3. Combiner (Multi-source → Single output)

**Purpose:** Merge data from multiple sources (TAGs, TOPICs) into single output

**API Type:** `Combiner`

**Schema:**

```typescript
interface Combiner {
  id: string
  sources: EntityReferenceList // List of adapter/bridge/pulse IDs
  mappings: DataCombiningList // Transformation rules
}

interface DataCombining {
  source: string // Source TAG or TOPIC
  destination: string // Output TOPIC
  transformScript?: string
}
```

**Flow:**

```
TAG_1 ──┐
TAG_2 ──┤──[Combiner]──→ TOPIC_OUT
TOPIC ──┘
```

**Example:**

```json
{
  "id": "temp-humidity-combiner",
  "sources": ["modbus-adapter", "opcua-adapter"],
  "mappings": [
    {
      "source": "modbus-adapter/device-1/temperature",
      "destination": "combined/environmental-data"
    },
    {
      "source": "opcua-adapter/device-2/humidity",
      "destination": "combined/environmental-data"
    }
  ]
}
```

**Visualization Considerations:**

- Fan-in pattern (multiple sources → one output)
- Should show which sources contribute to each output
- Complex dependency tracking needed
- May have transformation logic

---

### 4. Bridge Subscriptions (TOPIC ↔ TOPIC)

**Purpose:** Bidirectional MQTT bridge between local and remote brokers

**API Type:** `Bridge`

**Schema:**

```typescript
interface Bridge {
  id: string
  host: string
  localSubscriptions: Subscription[]
  remoteSubscriptions: Subscription[]
}

interface Subscription {
  filters: string[] // Topic filters
  destination: string // Target topic
  maxQoS?: number
}
```

**Flow:**

```
LOCAL TOPIC ──[Bridge]──→ REMOTE BROKER
              ←─────────
REMOTE TOPIC ──[Bridge]──→ LOCAL BROKER
```

**Example:**

```json
{
  "id": "enterprise-bridge",
  "host": "mqtt://enterprise-broker:1883",
  "localSubscriptions": [
    {
      "filters": ["factory/+/data"],
      "destination": "hivemq/edge/factory-data"
    }
  ],
  "remoteSubscriptions": [
    {
      "filters": ["commands/#"],
      "destination": "hivemq/commands"
    }
  ]
}
```

**Visualization Considerations:**

- Bidirectional flows
- Multiple filters → single destination
- Should show which broker is local vs. remote
- Connection status important

---

### 5. Asset Mapper (Special Combiner)

**Purpose:** Pulse-specific combiner that references managed assets

**Schema:**

```typescript
interface AssetMapper extends Combiner {
  sources: [{ type: 'PULSE_AGENT'; id: string }]
  mappings: [
    {
      assetId: string // Reference to Pulse asset
      destination: string
    },
  ]
}
```

**Flow:**

```
PULSE ASSETS ──[Asset Mapper]──→ TOPICS ──→ EDGE
```

**Visualization Considerations:**

- Distinct from regular combiners
- Assets may be MAPPED or UNMAPPED
- Operational status requires valid asset references

---

## Data Aggregation Layer

### `useGetDomainOntology` Hook

**Purpose:** Central hook that aggregates all integration points and transformations

**File:** `src/modules/DomainOntology/hooks/useGetDomainOntology.ts`

**Return Schema:**

```typescript
interface DomainOntology {
  // Integration Points
  tags: UseQueryResult<DomainTagList>
  topicFilters: UseQueryResult<TopicFilterList>

  // Transformations
  northMappings: UseQueryResult<NorthboundMappingList>
  southMappings: UseQueryResult<SouthboundMappingList>
  bridgeSubscriptions: BridgeSubscription

  // Status
  isLoading: boolean
  isError: boolean
}

interface BridgeSubscription {
  topics: string[] // All bridge topics (deduplicated)
  topicFilters: string[] // All bridge filters (deduplicated)
  mappings: string[][] // [filter, destination] pairs
}
```

**Data Sources:**

- `useListDomainNorthboundMappings()` - GET `/api/v1/domain/mappings/northbound`
- `useListDomainSouthboundMappings()` - GET `/api/v1/domain/mappings/southbound`
- `useListDomainTags()` - GET `/api/v1/domain/tags`
- `useListTopicFilters()` - GET `/api/v1/topic-filters`
- `useListBridges()` - GET `/api/v1/mqtt/bridges`

**Key Features:**

1. **Unified loading state**: Returns true if ANY source is loading
2. **Unified error state**: Returns true if ANY source has error
3. **Bridge transformation**: Flattens bridge subscriptions into simple arrays
4. **Memoization**: Recalculates only when dependencies change

---

## Visualization Layer

### Current Implementations

#### 1. Sankey Diagram (ConceptFlow)

**File:** `src/modules/DomainOntology/hooks/useGetSankeyData.ts`

**Data Transformation:**

```typescript
// Nodes: All unique integration points
nodes = [...tags, ...topics, ...topicFilters]

// Links: All transformation flows
links = [
  ...northMappings.map((m) => ({ source: m.tagName, target: m.topic })),
  ...southMappings.map((m) => ({ source: m.topicFilter, target: m.tagName })),
  ...bridgeMappings.map((m) => ({ source: m[0], target: m[1] })),
]
```

**Strengths:**

- ✅ Intuitive flow representation
- ✅ Shows data volume (link width)
- ✅ Clear directionality

**Weaknesses:**

- ❌ Poor layout for dense graphs
- ❌ No interaction/drill-down
- ❌ Hard to trace specific paths
- ❌ No filtering options

**Library:** Nivo Sankey (`@nivo/sankey`)

---

#### 2. Chord Diagram (RelationMatrix)

**File:** `src/modules/DomainOntology/hooks/useGetChordMatrixData.ts`

**Data Transformation:**

```typescript
// Build adjacency matrix
matrix[source][target] = 1  // Source → Target
matrix[target][source] = 3  // (Different value for directionality)

// Also includes wildcard matching
for (topic, filter in combinations) {
  if (mqttTopicMatch(filter, topic)) {
    matrix[topic][filter] = weight
  }
}
```

**Strengths:**

- ✅ Compact representation of all relationships
- ✅ Shows connection density
- ✅ Good for pattern discovery
- ✅ Includes wildcard matching logic

**Weaknesses:**

- ❌ Hard to read for non-technical users
- ❌ No clear flow direction (despite weight difference)
- ❌ Overwhelm with >30 nodes
- ❌ No labels for small segments

**Library:** Nivo Chord (`@nivo/chord`)

---

#### 3. Sunburst Chart (ConceptWheel)

**File:** `src/modules/DomainOntology/hooks/useGetSunburstData.ts`

**Data Transformation:**

```typescript
// Hierarchical structure based on MQTT topic levels
tree = {
  name: 'root',
  children: [
    {
      name: 'level1',
      children: [{ name: 'level2', value: count }],
    },
  ],
}
```

**Strengths:**

- ✅ Beautiful hierarchical visualization
- ✅ Shows topic namespace structure
- ✅ Interactive drill-down
- ✅ Includes metadata explorer on selection

**Weaknesses:**

- ❌ Only shows topics, not tags or filters
- ❌ Doesn't show transformations/mappings
- ❌ Limited to hierarchical data
- ❌ Hard to compare branches

**Library:** Nivo Sunburst (`@nivo/sunburst`)

---

#### 4. Cluster View (AdapterCluster)

**File:** `src/modules/DomainOntology/hooks/useGetClusterData.ts`

**Data Transformation:**

```typescript
// D3 force-directed clustering
nodes = adapters.map((a) => ({
  id: a.id,
  group: a.type,
  value: a.tags.length,
}))

links = northMappings.map((m) => ({
  source: m.adapterId,
  target: m.topic,
}))
```

**Strengths:**

- ✅ Shows adapter grouping naturally
- ✅ Physics-based layout reveals structure
- ✅ Node size = tag count (useful metric)

**Weaknesses:**

- ❌ Unstable layout (changes on each render)
- ❌ Poor performance with many nodes
- ❌ No control over layout algorithm
- ❌ Hard to find specific adapters

**Library:** D3 force simulation

---

#### 5. Edge Bundling (Experimental)

**File:** `src/modules/DomainOntology/components/RelationEdgeBundling.tsx`

**Data Transformation:**

```typescript
// Hierarchical edge bundling based on entity types
hierarchy = {
  name: 'root',
  children: [
    { name: 'ADAPTERS', children: [...adapters] },
    { name: 'BRIDGES', children: [...bridges] },
    { name: 'EDGE', children: [edge] },
  ],
}
```

**Strengths:**

- ✅ Reduces visual clutter for dense graphs
- ✅ Shows entity type grouping
- ✅ Beautiful for presentations

**Weaknesses:**

- ❌ Experimental feature flag required
- ❌ Hard to trace individual connections
- ❌ No interaction/filtering
- ❌ Confusing for non-experts

**Library:** D3 hierarchical edge bundling

---

## Current Implementation Analysis

### Architecture Assessment

**Component Structure:**

```
DomainOntologyManager (Tab container)
├── AdapterCluster
├── ConceptWheel
│   └── MetadataExplorer (on selection)
├── RelationMatrix
├── ConceptFlow
└── RelationEdgeBundling (if feature flag)
```

**Hook Dependencies:**

```
useGetDomainOntology (data source)
├── useGetSankeyData
├── useGetChordMatrixData
├── useGetSunburstData
├── useGetClusterData
└── useGetTreeData
```

**Strengths:**

1. ✅ **Centralized data fetching**: `useGetDomainOntology` avoids redundant API calls
2. ✅ **Modular hook architecture**: Each visualization has dedicated data transformer
3. ✅ **Lazy loading**: Tabs load visualizations on demand
4. ✅ **Error handling**: Consistent loading/error states
5. ✅ **Accessible via workspace**: Integrated into Edge node workflow

**Weaknesses:**

1. ❌ **No cross-visualization state**: Selections don't persist across tabs
2. ❌ **Limited interactivity**: Most visualizations are static displays
3. ❌ **No filtering/search**: Can't filter by adapter, tag pattern, etc.
4. ❌ **Performance issues**: No virtualization for large datasets
5. ❌ **No export**: Can't export visualizations or data
6. ❌ **Inconsistent UX**: Each viz has different interaction patterns
7. ❌ **No drill-down paths**: Can't navigate from viz to entity config

---

## Recommendations

### Immediate Improvements (Short-term)

#### 1. Enhanced Sankey Diagram

**Priority:** High  
**Effort:** Medium

**Improvements:**

- Add node click → navigate to entity configuration
- Implement link hover → show mapping details tooltip
- Add filtering controls (by adapter, bridge, or entity type)
- Improve layout algorithm for dense graphs
- Add "highlight path" feature (select node → highlight all connected)

**Implementation:**

```typescript
// Enhanced Sankey with interactions
<SankeyChart
  data={sankeyData}
  onNodeClick={(node) => navigateToEntity(node.id)}
  onLinkHover={(link) => showMappingDetails(link)}
  filters={{ adapterId, entityType }}
  highlightPath={selectedNodeId}
/>
```

---

#### 2. Interactive Chord Matrix

**Priority:** Medium  
**Effort:** Low

**Improvements:**

- Add legend explaining directionality (source vs. target weights)
- Implement arc click → filter to show only related connections
- Add search/highlight by name
- Show tooltip with connection count

---

#### 3. Performance Optimization

**Priority:** High  
**Effort:** Medium

**Improvements:**

- Implement virtualization for >100 nodes
- Add pagination/lazy loading for visualizations
- Memoize expensive calculations (especially wildcard matching)
- Add loading skeletons instead of spinners
- Profile and optimize re-renders

**Example:**

```typescript
// Memoize expensive wildcard matching
const wildcardMatches = useMemo(() => {
  return computeWildcardMatches(topics, filters)
}, [topics, filters])
```

---

### Strategic Enhancements (Medium-term)

#### 4. Unified Filter Panel

**Priority:** High  
**Effort:** High

**Purpose:** Consistent filtering across all visualizations

**Features:**

- Filter by adapter type (Modbus, OPC-UA, MQTT, etc.)
- Filter by entity status (active, inactive, error)
- Search by name/pattern
- Filter by transformation type (north, south, bridge, combiner)
- Save/load filter presets

**UI Mockup:**

```
┌─────────────────────────────────────┐
│ 🔍 Search: [modbus-*          ]    │
│                                     │
│ Adapter Types:                      │
│  ☑ Modbus  ☑ OPC-UA  ☐ MQTT       │
│                                     │
│ Entity Status:                      │
│  ☑ Active  ☑ Inactive  ☐ Error    │
│                                     │
│ Transformation Types:               │
│  ☑ Northbound  ☑ Southbound        │
│  ☐ Bridge  ☐ Combiner              │
│                                     │
│ [Apply Filters] [Reset] [Save]     │
└─────────────────────────────────────┘
```

---

#### 5. Network Graph View (NEW)

**Priority:** Medium  
**Effort:** High

**Purpose:** Force-directed network graph with advanced interactions

**Library:** React Flow (already in workspace) or Cytoscape.js

**Features:**

- Nodes = Integration points (TAG, TOPIC, FILTER)
- Edges = Transformations (with type indicator)
- Color coding by entity type
- Size by connection count
- Layout algorithms: Force-directed, Hierarchical, Circular
- Advanced interactions:
  - Pan/zoom
  - Drag to reposition
  - Click → show details panel
  - Double-click → navigate to config
  - Right-click → context menu (filter, hide, trace path)
- Mini-map for large graphs
- Search with auto-focus

**Benefits:**

- Familiar to users (similar to workspace canvas)
- Better for exploration and discovery
- Can show bidirectional flows clearly
- Scales better than Sankey for dense graphs

---

#### 6. Data Flow Tracer (NEW)

**Priority:** High  
**Effort:** Medium

**Purpose:** Interactive path tracing through transformation pipeline

**Features:**

- Select any integration point
- Highlight all upstream sources (where data comes from)
- Highlight all downstream destinations (where data goes to)
- Show transformation rules at each step
- Support multi-hop tracing (TAG → TOPIC → FILTER → TAG)
- Export trace as documentation

**UI:**

```
┌────────────────────────────────────────────────┐
│ Trace Data Flow                                │
├────────────────────────────────────────────────┤
│ Start from: [modbus-adapter/device-1/temp  ▼] │
│                                                │
│ Direction:  ● Downstream  ○ Upstream  ○ Both  │
├────────────────────────────────────────────────┤
│ PATH TRACE RESULTS:                            │
│                                                │
│ [TAG] modbus-adapter/device-1/temp             │
│   ↓ Northbound Mapping                         │
│       • Topic: factory/floor1/temperature      │
│       • QoS: 1, Format: JSON                   │
│ [TOPIC] factory/floor1/temperature             │
│   ↓ Bridge Subscription (enterprise-bridge)    │
│       • Remote: hivemq/edge/factory-data       │
│ [TOPIC] hivemq/edge/factory-data               │
│   ↓ Topic Filter: hivemq/edge/#                │
│ [FILTER] hivemq/edge/#                         │
│   ↓ Southbound Mapping                         │
│       • Tag: opcua-adapter/actuator-1/command  │
│ [TAG] opcua-adapter/actuator-1/command         │
│                                                │
│ [Export Trace] [Visualize]                    │
└────────────────────────────────────────────────┘
```

---

#### 7. Metadata Panel Integration

**Priority:** Low  
**Effort:** Medium

**Purpose:** Show entity metadata alongside visualizations

**Features:**

- Persistent side panel (collapsible)
- Shows details of selected integration point
- Quick actions (edit, delete, test)
- Historical metrics (message count, error rate)
- Related entities

---

### Advanced Features (Long-term)

#### 8. Time-Series View

**Purpose:** Show how topology changes over time  
**Features:** Timeline scrubber, playback, comparison mode

#### 9. Health Heatmap

**Purpose:** Overlay operational status on visualizations  
**Features:** Color-coded by health, filter by status, alert zones

#### 10. AI-Powered Insights

**Purpose:** Automated pattern detection and recommendations  
**Features:** Bottleneck detection, unused mappings, optimization suggestions

#### 11. Collaborative Annotations

**Purpose:** Team annotations on visualizations  
**Features:** Comments, tags, share views

#### 12. Export & Documentation

**Purpose:** Generate documentation from topology  
**Features:** PDF/PNG export, auto-generated docs, data lineage reports

---

## Performance Benchmarks

### Target Metrics

| Metric                   | Small (<50) | Medium (50-200) | Large (>200) |
| ------------------------ | ----------- | --------------- | ------------ |
| **Initial Load**         | <500ms      | <1s             | <2s          |
| **Render Update**        | <100ms      | <300ms          | <500ms       |
| **Interaction Response** | <50ms       | <100ms          | <200ms       |
| **Memory Usage**         | <50MB       | <100MB          | <200MB       |

### Optimization Strategies

1. **Virtualization**: Only render visible nodes/links
2. **Memoization**: Cache expensive calculations
3. **Web Workers**: Offload layout algorithms
4. **Incremental Rendering**: Render in batches
5. **Lazy Loading**: Load data on-demand
6. **Debouncing**: Delay updates during rapid changes
7. **Level of Detail**: Simplify distant nodes

---

## Data Science Insights to Surface

### 1. Connection Density Metrics

- **Hub Detection**: Which integration points have most connections?
- **Isolated Entities**: Which entities have no connections (configuration gaps)?
- **Clustering Coefficient**: How tightly are entities grouped?

### 2. Transformation Complexity

- **Mapping Depth**: Average hops from TAG to final destination
- **Fan-out**: How many destinations per source?
- **Fan-in**: How many sources per destination?
- **Bidirectional Flows**: Which entities have both north+south?

### 3. Bottleneck Detection

- **Single Points of Failure**: Critical paths with no redundancy
- **Overloaded Topics**: Topics with many subscribers
- **Underutilized Adapters**: Adapters with few mappings

### 4. Pattern Recognition

- **Common Transformation Patterns**: Most frequent mapping structures
- **Naming Conventions**: Topic/tag naming patterns
- **Entity Templates**: Reusable configuration patterns

### 5. Impact Analysis

- **"What if" Simulations**: What breaks if I remove entity X?
- **Dependency Graphs**: Full upstream/downstream dependencies
- **Refactoring Suggestions**: Simplification opportunities

---

## Next Steps

### Phase 1: Analysis & Documentation (CURRENT)

- [x] Document domain model
- [x] Analyze existing visualizations
- [ ] Performance benchmarking
- [ ] User research (if possible)

### Phase 2: Quick Wins (Week 1-2)

- [ ] Add interactions to Sankey (click, hover, filter)
- [ ] Optimize chord matrix rendering
- [ ] Implement unified filter panel
- [ ] Add export functionality

### Phase 3: New Visualizations (Week 3-4)

- [ ] Build network graph view
- [ ] Implement data flow tracer
- [ ] Add metadata panel integration

### Phase 4: Advanced Features (Week 5+)

- [ ] Performance optimization with virtualization
- [ ] Time-series/historical view
- [ ] Health status overlay
- [ ] AI-powered insights

---

**Document Version:** 1.0  
**Last Updated:** November 4, 2025  
**Next Review:** After Phase 2 completion
