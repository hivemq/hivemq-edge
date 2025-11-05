# Visualization Comparison & Enhancement Roadmap

**Task:** 38018-domain-ontology-visualisation  
**Document Type:** Visual Design & Enhancement Strategy  
**Version:** 1.0  
**Date:** November 4, 2025

---

## Current Visualizations Overview

### 1. Sankey Diagram (ConceptFlow)

**Current State:** ✅ Implemented  
**Library:** Nivo Sankey  
**File:** `src/modules/DomainOntology/components/ConceptFlow.tsx`

**What it shows:**

```
[TAGS] ──────→ [TOPICS] ──────→ [TOPIC FILTERS]
         (Northbound)        (Southbound)

[TOPIC FILTERS] ←────── [TOPICS] ←────── [BRIDGES]
                  (Bridge Subscriptions)
```

**Strengths:**

- ✅ Intuitive flow representation
- ✅ Shows data volume (link width)
- ✅ Clear directionality
- ✅ Good for presentations

**Weaknesses:**

- ❌ Poor layout for dense graphs (overlapping flows)
- ❌ No interaction/drill-down
- ❌ Hard to trace specific paths
- ❌ No filtering options
- ❌ Can't navigate to entity config

**Enhancement Priority:** 🔥 HIGH

**Proposed Enhancements:**

1. **Node click** → Navigate to entity configuration page
2. **Link hover** → Tooltip with mapping details (QoS, format, etc.)
3. **Path highlighting** → Select node, highlight all connected paths
4. **Filter controls** → Filter by adapter type, entity type, status
5. **Layout improvements** → Better algorithm for dense graphs
6. **Mini-map** → Overview + focus area for large graphs

---

### 2. Chord Diagram (RelationMatrix)

**Current State:** ✅ Implemented  
**Library:** Nivo Chord  
**File:** `src/modules/DomainOntology/components/RelationMatrix.tsx`

**What it shows:**

```
Circular layout with arcs showing bidirectional relationships:
- Outer ring: All integration points (TAG, TOPIC, FILTER)
- Arcs: Transformation flows (thickness = connection strength)
- Color: Different weights for source→target vs target→source
```

**Strengths:**

- ✅ Compact representation of all relationships
- ✅ Shows connection density
- ✅ Good for pattern discovery
- ✅ Includes wildcard matching logic

**Weaknesses:**

- ❌ Hard to read for non-technical users
- ❌ No clear flow direction (despite weight difference)
- ❌ Overwhelming with >30 nodes
- ❌ No labels for small segments
- ❌ Can't isolate specific connections

**Enhancement Priority:** 🔶 MEDIUM

**Proposed Enhancements:**

1. **Legend** → Explain directionality (source vs target weights)
2. **Arc click** → Filter to show only related connections
3. **Search** → Highlight by name/pattern
4. **Tooltip improvements** → Show connection count, type
5. **Size threshold** → Hide or aggregate small segments

---

### 3. Sunburst Chart (ConceptWheel)

**Current State:** ✅ Implemented  
**Library:** Nivo Sunburst  
**File:** `src/modules/DomainOntology/components/ConceptWheel.tsx`

**What it shows:**

```
Hierarchical circle divided by MQTT topic levels:
- Center: Root
- Ring 1: Level 1 topics (e.g., "factory")
- Ring 2: Level 2 topics (e.g., "floor1")
- Ring 3: Level 3 topics (e.g., "temperature")
```

**Strengths:**

- ✅ Beautiful hierarchical visualization
- ✅ Shows topic namespace structure
- ✅ Interactive drill-down
- ✅ Includes MetadataExplorer on selection
- ✅ Good for understanding topic organization

**Weaknesses:**

- ❌ Only shows topics (not tags or filters)
- ❌ Doesn't show transformations/mappings
- ❌ Limited to hierarchical data
- ❌ Hard to compare branches
- ❌ No wildcard filter visualization

**Enhancement Priority:** 🔷 LOW

**Proposed Enhancements:**

1. **Multi-layer mode** → Show tags in inner ring, topics in outer
2. **Transformation indicators** → Visual markers for mapped topics
3. **Wildcard overlays** → Show which filters match which topics
4. **Compare mode** → Side-by-side comparison of subtrees

---

### 4. Cluster View (AdapterCluster)

**Current State:** ✅ Implemented  
**Library:** D3 Force Simulation  
**File:** `src/modules/DomainOntology/components/AdapterCluster.tsx`

**What it shows:**

```
Force-directed network with:
- Nodes: Adapters (sized by tag count)
- Links: Northbound mappings to topics
- Groups: Colored by adapter type
```

**Strengths:**

- ✅ Shows adapter grouping naturally
- ✅ Physics-based layout reveals structure
- ✅ Node size = tag count (useful metric)
- ✅ Good for exploring relationships

**Weaknesses:**

- ❌ Unstable layout (changes on each render)
- ❌ Poor performance with many nodes (>50)
- ❌ No control over layout algorithm
- ❌ Hard to find specific adapters
- ❌ No pan/zoom controls

**Enhancement Priority:** 🔶 MEDIUM

**Proposed Enhancements:**

1. **Stable layout** → Save positions, allow manual repositioning
2. **Pan/zoom** → D3 zoom behavior
3. **Search** → Find and highlight adapter
4. **Layout algorithms** → Choice of force/hierarchical/circular
5. **Performance** → Virtualization or WebGL rendering

---

### 5. Edge Bundling (RelationEdgeBundling)

**Current State:** ⚠️ Experimental (feature flag)  
**Library:** D3 Hierarchical Edge Bundling  
**File:** `src/modules/DomainOntology/components/RelationEdgeBundling.tsx`

**What it shows:**

```
Hierarchical bundling with entity types as groups:
- ADAPTERS group
- BRIDGES group
- EDGE group
- Curved connections between entities
```

**Strengths:**

- ✅ Reduces visual clutter for dense graphs
- ✅ Shows entity type grouping
- ✅ Beautiful for presentations
- ✅ Good for identifying connection patterns

**Weaknesses:**

- ❌ Experimental feature flag required
- ❌ Hard to trace individual connections
- ❌ No interaction/filtering
- ❌ Confusing for non-experts
- ❌ Poor performance

**Enhancement Priority:** 🔷 LOW

**Proposed Enhancements:**

1. **Promote from experimental** → Full feature status
2. **Interaction** → Click to highlight bundle
3. **Filter** → Show/hide specific groups
4. **Performance** → Canvas rendering instead of SVG

---

## Proposed New Visualizations

### 6. Network Graph View (NEW)

**Status:** 📋 Planned  
**Library:** React Flow or Cytoscape.js  
**Priority:** 🔥 HIGH

**What it will show:**

```
Interactive network graph:
- Nodes: TAGs, TOPICs, TOPIC FILTERs
- Edges: Transformations (color-coded by type)
- Groups: Optional grouping by adapter/bridge
```

**Why it's needed:**

- Users are familiar with React Flow from workspace
- Better interaction model than current visualizations
- Scalable to large graphs with virtualization
- Multiple layout algorithms available
- Better for exploration and discovery

**Features:**

- ✅ Pan, zoom, drag nodes
- ✅ Click node → details panel
- ✅ Double-click → navigate to config
- ✅ Right-click → context menu
- ✅ Mini-map for large graphs
- ✅ Search with auto-focus
- ✅ Layout algorithms: Force-directed, Hierarchical, Circular
- ✅ Filter by entity type, status, adapter

**Implementation Estimate:** 2-3 weeks

---

### 7. Data Flow Tracer (NEW)

**Status:** 📋 Planned  
**Priority:** 🔥 HIGH

**What it will show:**

```
Interactive path tracing UI:
1. Select starting integration point
2. Choose direction (upstream/downstream/both)
3. Show highlighted path through transformations
4. Display transformation rules at each step
```

**Why it's needed:**

- Critical for debugging data flow issues
- Helps understand complex multi-hop transformations
- Useful for impact analysis ("what breaks if I remove X?")
- Educational for new users

**Features:**

- ✅ Start from any TAG, TOPIC, or TOPIC FILTER
- ✅ Show full upstream lineage (where data comes from)
- ✅ Show full downstream impact (where data goes to)
- ✅ Display transformation rules at each hop
- ✅ Multi-hop tracing support
- ✅ Export trace as documentation
- ✅ Visualize trace on any compatible visualization

**UI Mockup:**

```
┌──────────────────────────────────────────┐
│ 🔍 Trace Data Flow                       │
├──────────────────────────────────────────┤
│ Start from: [modbus-adapter/temp    ▼]  │
│ Direction:  ● Downstream  ○ Upstream    │
├──────────────────────────────────────────┤
│ TRACE RESULTS (4 hops):                  │
│                                          │
│ 1. [TAG] modbus-adapter/device-1/temp    │
│    ↓ Northbound Mapping                  │
│      • Topic: factory/floor1/temp        │
│      • QoS: 1, Format: JSON              │
│                                          │
│ 2. [TOPIC] factory/floor1/temp           │
│    ↓ Bridge Subscription                 │
│      • Remote: hivemq/edge/factory       │
│                                          │
│ 3. [TOPIC] hivemq/edge/factory           │
│    ↓ Topic Filter Match                  │
│      • Filter: hivemq/edge/#             │
│                                          │
│ 4. [FILTER] hivemq/edge/#                │
│                                          │
│ [Export] [Visualize] [Copy]             │
└──────────────────────────────────────────┘
```

**Implementation Estimate:** 1-2 weeks

---

## Cross-Cutting Enhancements

### Unified Filter Panel

**Priority:** 🔥 HIGH  
**Effort:** High (3-4 days)

**Purpose:** Consistent filtering across ALL visualizations

**Features:**

- Filter by adapter type (Modbus, OPC-UA, MQTT, etc.)
- Filter by entity status (active, inactive, error)
- Search by name/pattern (with regex support)
- Filter by transformation type (north, south, bridge, combiner)
- Save/load filter presets
- Apply filters to all tabs (shared state)

**UI Design:**

```
┌─────────────────────────────────────────────┐
│ 🔍 Filter Integration Points                │
├─────────────────────────────────────────────┤
│ Search: [modbus-*                      ] 🔍 │
│                                             │
│ Adapter Types:                              │
│  ☑ Modbus   ☑ OPC-UA   ☐ MQTT   ☐ HTTP    │
│                                             │
│ Entity Status:                              │
│  ☑ Active   ☑ Inactive   ☐ Error          │
│                                             │
│ Show:                                       │
│  ☑ TAGs   ☑ TOPICs   ☑ TOPIC FILTERs      │
│                                             │
│ Transformation Types:                       │
│  ☑ Northbound   ☑ Southbound               │
│  ☐ Bridge       ☐ Combiner                 │
│                                             │
│ [Apply] [Reset] [Save Preset ▼]           │
│                                             │
│ Saved Presets:                              │
│  • Production Adapters                      │
│  • Error States Only                        │
│  • Bidirectional Flows                      │
└─────────────────────────────────────────────┘
```

---

### Performance Optimization

**Priority:** 🔥 HIGH  
**Effort:** Medium (2-3 days)

**Target Metrics:**

| Scenario                     | Current | Target |
| ---------------------------- | ------- | ------ |
| Initial load (50 entities)   | ~1s     | <500ms |
| Render update (100 entities) | ~500ms  | <300ms |
| Interaction response         | ~200ms  | <100ms |
| Memory usage (200 entities)  | ~150MB  | <100MB |

**Optimizations:**

1. **Memoization**

   - Cache wildcard matching results
   - Memoize expensive data transformations
   - Use React.memo for visualization components

2. **Virtualization**

   - Render only visible nodes in large graphs
   - Implement level-of-detail rendering
   - Progressive loading for large datasets

3. **Web Workers**

   - Offload layout calculations
   - Background data processing
   - Async wildcard matching

4. **Render Optimization**
   - Debounce rapid updates
   - Batch state updates
   - Use Canvas for >100 nodes instead of SVG

---

### Export & Documentation

**Priority:** 🔶 MEDIUM  
**Effort:** Low (1-2 days)

**Features:**

- Export visualization as PNG/SVG
- Export data as JSON/CSV
- Generate topology documentation (Markdown/PDF)
- Copy visualization to clipboard
- Share view with deep link

---

## Implementation Roadmap

### Phase 1: Quick Wins (Week 1-2)

**Goal:** Improve existing visualizations with minimal effort

- [ ] Add Sankey interactions (click, hover, path highlight)
- [ ] Optimize chord matrix rendering
- [ ] Add export functionality (PNG, JSON)
- [ ] Performance profiling and initial optimizations

**Expected Impact:** 40% improvement in user experience

---

### Phase 2: Unified Experience (Week 2-3)

**Goal:** Consistent UX across all visualizations

- [ ] Implement unified filter panel
- [ ] Add shared state management between tabs
- [ ] Performance optimization (memoization, virtualization)
- [ ] Interactive chord matrix improvements

**Expected Impact:** 60% improvement in usability

---

### Phase 3: New Capabilities (Week 3-5)

**Goal:** Add new visualization types and features

- [ ] Build network graph view (React Flow)
- [ ] Implement data flow tracer
- [ ] Add metadata panel integration
- [ ] Promote edge bundling from experimental

**Expected Impact:** 2x more use cases supported

---

### Phase 4: Polish & Scale (Week 5-6)

**Goal:** Production-ready at scale

- [ ] Performance testing with large datasets (>200 entities)
- [ ] E2E test coverage
- [ ] Accessibility compliance (WCAG 2.1 AA)
- [ ] User documentation
- [ ] A/B testing with users

**Expected Impact:** Production-ready, scales to enterprise

---

## Success Metrics

### User Experience

- ✅ 80% of users can find specific integration point in <30s
- ✅ 90% of users understand data flow after viewing visualization
- ✅ 50% reduction in support tickets about "where does my data go?"

### Performance

- ✅ <500ms initial load for typical workspace (50 entities)
- ✅ <2s initial load for large workspace (200 entities)
- ✅ <100ms interaction response time
- ✅ Smooth 60fps animations

### Functionality

- ✅ All visualizations interactive (not static)
- ✅ Consistent filtering across all views
- ✅ Export functionality for all visualizations
- ✅ Path tracing for any integration point

### Quality

- ✅ 100% E2E test coverage for new features
- ✅ WCAG 2.1 AA compliance
- ✅ Zero critical bugs in production
- ✅ Documentation complete

---

## Risk Assessment

### Technical Risks

| Risk                                      | Impact | Likelihood | Mitigation                          |
| ----------------------------------------- | ------ | ---------- | ----------------------------------- |
| Performance degradation with large graphs | High   | Medium     | Incremental testing, virtualization |
| Browser compatibility issues              | Medium | Low        | Progressive enhancement, polyfills  |
| Library version conflicts                 | Low    | Low        | Lock versions, test upgrades        |

### User Experience Risks

| Risk                        | Impact | Likelihood | Mitigation                                     |
| --------------------------- | ------ | ---------- | ---------------------------------------------- |
| Overwhelming UI complexity  | High   | Medium     | Progressive disclosure, user testing           |
| Steep learning curve        | Medium | Medium     | Contextual help, tooltips, documentation       |
| Breaking existing workflows | High   | Low        | Preserve existing features, graceful migration |

---

## Appendix: Visual Examples

### Sankey Enhancement Mockup

```
Before: Static flow diagram
After:  Interactive with highlights

[TAG: temp] ──→ [TOPIC: factory/temp] ──→ [FILTER: factory/#]
    ↑ Click             ↑ Hover shows:           ↑ Click
    │ Shows details     │ • QoS: 1               │ Navigate
    │                   │ • Format: JSON          │ to config
    │                   │ • Adapter: modbus       │
    └─────────── Highlights path ─────────────────┘
```

### Network Graph Layout Options

```
Force-Directed:          Hierarchical:           Circular:
    ┌─○─┐                    ○                   ╭─○─╮
  ○─○   ○─○                 ╱│╲                 ○     ○
    │   │                  ○ ○ ○               │       │
  ○─○   ○─○                 │ │                ○       ○
    └─○─┘                  ○ ○ ○               │       │
                                                 ○     ○
                                                  ╰─○─╯
```

---

**Document Version:** 1.0  
**Last Updated:** November 4, 2025  
**Next Review:** After Phase 1 completion
