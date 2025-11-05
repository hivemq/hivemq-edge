# Task 38018: Domain Ontology Visualisation

**Status:** 🚀 Active (Discovery Phase)  
**Started:** November 4, 2025  
**Priority:** High

---

## Quick Start

This task focuses on **enhancing existing domain ontology visualizations** that show integration point connectivity and transformation flows in HiveMQ Edge.

**Location in UI:** Edge node → Select → "Open side panel" → DomainOntologyManager

**Key Files:**

- `src/modules/DomainOntology/DomainOntologyManager.tsx` - Main component
- `src/modules/DomainOntology/hooks/useGetDomainOntology.ts` - Data aggregation hook

---

## Documentation Index

### 📋 Planning & Requirements

- **[TASK_BRIEF.md](./TASK_BRIEF.md)** - Task overview, objectives, and success criteria
- **[TASK_SUMMARY.md](./TASK_SUMMARY.md)** - Progress tracking and key findings

### 📚 Technical Documentation

- **[DOMAIN_MODEL.md](./DOMAIN_MODEL.md)** - Comprehensive domain model, transformation flows, and architecture analysis
- **[VISUALIZATION_ROADMAP.md](./VISUALIZATION_ROADMAP.md)** - Visual comparison of current/proposed visualizations with enhancement roadmap

### 💬 Conversation History

- **[CONVERSATION_SUBTASK_1.md](./CONVERSATION_SUBTASK_1.md)** - Initial discovery and context gathering
- **[CONVERSATION_SUBTASK_2.md](./CONVERSATION_SUBTASK_2.md)** - Network Graph View implementation (Phase 1 complete)
- **[CONVERSATION_SUBTASK_3.md](./CONVERSATION_SUBTASK_3.md)** - Details Panel design and implementation (Complete)
- **[CONVERSATION_SUBTASK_4.md](./CONVERSATION_SUBTASK_4.md)** - Add Combiner and Asset Mapper data (Complete)
- **[CONVERSATION_SUBTASK_5.md](./CONVERSATION_SUBTASK_5.md)** - Data Flow Tracer implementation (Phase 1 complete) ✅

### 🔄 Session Logs (Ephemeral)

Detailed session logs for bug fixes and iterations are in `.tasks-log/38018_*.md` (not committed to git)

---

## Current State

### Existing Visualizations (Proof of Concept)

1. **Sankey Diagram** - Data flow from TAG → TOPIC → FILTER
2. **Chord Matrix** - Adjacency matrix with directionality
3. **Sunburst Chart** - Hierarchical topic exploration
4. **Cluster View** - Force-directed adapter grouping
5. **Network Graph** - Force-directed topology with data flow (NEW - Phase 1 complete) ✅
6. **Edge Bundling** - Hierarchical connection patterns (experimental)

### Integration Points

- **TAG**: Device data points (e.g., `modbus-adapter/device-1/temperature`)
- **TOPIC**: MQTT topics (e.g., `factory/floor1/temperature`)
- **TOPIC FILTER**: Edge subscriptions (e.g., `factory/+/data/#`)

### Transformation Flows

- **Northbound Mapping**: TAG → TOPIC (device to broker)
- **Southbound Mapping**: TOPIC FILTER → TAG (broker to device)
- **Combiner**: Multiple sources → Single output
- **Bridge Subscriptions**: TOPIC ↔ TOPIC (local ↔ remote)

---

## Key Findings

### ✅ Strengths

- Modular architecture with dedicated hooks
- Centralized data fetching
- Multiple visualization perspectives
- Integration with workspace UI

### ❌ Weaknesses

- Limited interactivity (mostly static)
- No filtering or search
- Poor performance for large datasets
- No drill-down to configurations
- No export functionality

---

## Enhancement Priorities

### 🔥 High Priority (Phase 1-2)

1. **Enhanced Sankey Interactions**

   - Node click → navigate to config
   - Link hover → mapping details
   - Path highlighting
   - Filter controls

2. **Unified Filter Panel**

   - Filter by adapter type, status
   - Search by name/pattern
   - Shared state across tabs

3. **Performance Optimization**

   - Memoization
   - Virtualization for >100 nodes
   - Profile and optimize

4. **Network Graph View** (NEW)

   - React Flow implementation
   - Advanced interactions
   - Multiple layouts

5. **Data Flow Tracer** (NEW)
   - Interactive path tracing
   - Show transformation rules
   - Export traces

### 🔶 Medium Priority (Phase 3)

- Interactive chord improvements
- Metadata panel integration
- Export functionality

### 🔷 Low Priority (Phase 4)

- Sunburst enhancements
- Edge bundling promotion
- Time-series view

---

## Implementation Roadmap

```
Phase 1: Quick Wins (Week 1-2)
├─ Sankey interactions
├─ Performance profiling
└─ Export functionality

Phase 2: Unified Experience (Week 2-3)
├─ Unified filter panel
├─ Shared state management
└─ Optimization (memoization)

Phase 3: New Capabilities (Week 3-5)
├─ Network graph view
├─ Data flow tracer
└─ Metadata integration

Phase 4: Polish & Scale (Week 5-6)
├─ Performance at scale
├─ E2E tests
└─ Accessibility compliance
```

---

## Data Science Insights

The visualizations should surface:

1. **Connection Density**: Hub detection, isolated entities
2. **Transformation Complexity**: Mapping depth, fan-out/fan-in
3. **Bottleneck Detection**: Single points of failure
4. **Pattern Recognition**: Common mapping patterns
5. **Impact Analysis**: Dependency graphs, "what-if" scenarios

---

## Related Tasks

- **25337-workspace-auto-layout** - Workspace entities and auto-layout implementation
- **37542-code-coverage** - Testing guidelines and patterns

---

## Context Documents

- `.tasks/WORKSPACE_TOPOLOGY.md` - Workspace graph structure reference
- `.tasks/25337-workspace-auto-layout/ARCHITECTURE.md` - Auto-layout architecture
- `.tasks/DESIGN_GUIDELINES.md` - UI component patterns
- `.tasks/TESTING_GUIDELINES.md` - Testing requirements

---

## Next Steps

### Immediate

- [x] Task structure created
- [x] Domain model documented
- [x] Current implementations analyzed
- [x] Recommendations defined
- [ ] Performance benchmarking
- [ ] User feedback gathering

### Phase 2

- [ ] Sankey enhancement implementation
- [ ] Unified filter panel
- [ ] Performance optimization
- [ ] E2E tests

---

## Open Questions

1. **Performance**: What's the typical workspace size in production?
2. **Use Cases**: What are the most common visualization use cases?
3. **Priority**: Which enhancements provide the most user value?
4. **Export**: What formats are most important (PNG, PDF, JSON)?
5. **Standalone**: Should visualizations be accessible outside Edge panel?

---

## Progress Tracking

### Discovery Phase ✅

- [x] Analyze existing visualizations
- [x] Document domain model
- [x] Identify strengths/weaknesses
- [x] Create enhancement roadmap

### Implementation Phase 🚧

- [x] **Subtask 2: Network Graph View (Phase 1)** ✅
  - [x] Data transformation hook
  - [x] Force-directed layout algorithm
  - [x] Custom nodes and edges
  - [x] React Flow integration
  - [x] POC tests created
- [x] **Subtask 3: Details Panel** ✅
  - [x] Bottom slide-in panel design
  - [x] Node details display
  - [x] Connection information
  - [x] Card component pattern
- [x] **Subtask 4: Combiner & Asset Mapper Data** ✅
  - [x] Added to useGetDomainOntology
  - [x] Network Graph processing
  - [x] New edge types (COMBINER, ASSET_MAPPER)
- [x] **Subtask 5: Data Flow Tracer (Phase 1)** ✅
  - [x] Core tracing logic (BFS algorithm)
  - [x] Graph building from domain ontology
  - [x] Interactive UI with chakra-react-select
  - [x] Results visualization
  - [x] Design guidelines updated
  - [ ] Phase 2: Network Graph integration
  - [ ] Phase 2: Export functionality
  - [ ] Phase 2: Multi-path support
- [ ] Network Graph View (Phase 2) - Layout improvements (WebCola/Dagre)
- [ ] Performance benchmarking
- [ ] Sankey interactions
- [ ] Unified filter panel

### Testing Phase ⏸️

- [x] POC test structure (component + E2E)
- [ ] Test execution and verification
- [ ] Full E2E test coverage
- [ ] Accessibility compliance
- [ ] Performance regression tests
- [ ] User acceptance testing

---

## Success Criteria

- [ ] All visualizations have interactive features
- [ ] Consistent filtering across all views
- [ ] <500ms load time for 50 entities
- [ ] <2s load time for 200 entities
- [ ] Export functionality implemented
- [ ] Path tracing works for any integration point
- [ ] 100% E2E test coverage
- [ ] WCAG 2.1 AA compliance

---

**Last Updated:** November 4, 2025  
**Next Review:** After performance benchmarking

For questions or to continue work on this task, reference: **Task 38018**
