# Conversation: Task 25337 - Workspace Auto-Layout - Subtask 1

**Date:** October 27, 2025  
**Subtask:** Initial Planning and Architecture Design  
**Status:** In Progress

---

## Conversation Summary

Starting work on implementing automatic and dynamic layout capabilities for the HiveMQ Edge workspace. The workspace currently has a rudimentary static layout system that needs to be replaced with professional graph layout algorithms.

### Current State Analysis

**Existing Code Review:**

1. **Layout System:**

   - Current implementation: `src/modules/Workspace/utils/layout-utils.ts`
   - Very basic - just returns nodes unchanged for HORIZONTAL layout
   - No actual layout algorithm implemented

2. **Workspace Architecture:**

   - Main canvas: `src/modules/Workspace/components/ReactFlowWrapper.tsx`
   - Uses React Flow (v11+) with @xyflow/react
   - Toolbar: `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
   - Store: Zustand-based workspace store for nodes/edges state

3. **Node Types in Workspace:**

   - EDGE_NODE: Central HiveMQ Edge gateway
   - ADAPTER_NODE: Protocol adapters
   - BRIDGE_NODE: MQTT bridges
   - LISTENER_NODE: MQTT listeners
   - HOST_NODE: External hosts/devices
   - DEVICE_NODE: Connected devices
   - COMBINER_NODE: Data combiners
   - PULSE_NODE: Health monitoring
   - CLUSTER_NODE: Grouping/clustering
   - ASSETS_NODE: Asset nodes

4. **Edge Types:**

   - REPORT_EDGE: Monitoring edges
   - DYNAMIC_EDGE: Dynamic connections

5. **Current Layout Options:**
   - Only EdgeFlowLayout.HORIZONTAL exists in types
   - EdgeFlowGrouping interface supports layout switching
   - EdgeFlowOptions for display toggles (topics, status, gateway)

### Workspace Topology Characteristics

Based on code analysis:

- **Hub-and-spoke pattern**: Edge node is typically central
- **Hierarchical relationships**: Adapters → Devices, Bridges → Remote
- **Bidirectional flows**: Data flows in/out
- **Grouping support**: Cluster nodes can contain children
- **Glued nodes**: Some nodes are position-locked to others (e.g., listeners to edge)

---

## Proposed Development Plan

### Phase 1: Foundation & Infrastructure (Subtask 1)

**Goal:** Set up the groundwork for layout algorithms

**Tasks:**

1. ✅ Analyze current codebase and workspace topology
2. ⏳ Install required dependencies (dagre, webcola)
3. ⏳ Create feature flag system for experimental layouts
4. ⏳ Define layout algorithm interface/types
5. ⏳ Create layout configuration state management
6. ⏳ Document layout algorithm requirements

**Deliverables:**

- Updated types with new layout enums
- Feature flag infrastructure
- Layout algorithm interface
- Configuration schema for layout options

---

### Phase 2: Dagre Tree Layouts (Subtask 2)

**Goal:** Implement basic hierarchical layouts with dagre

**Tasks:**

1. Implement dagre integration wrapper
2. Create vertical tree layout (TB - top-to-bottom)
3. Create horizontal tree layout (LR - left-to-right)
4. Handle node sizing dynamically
5. Respect glued node constraints
6. Add layout animation/transitions
7. Unit tests for dagre layouts

**Deliverables:**

- Working dagre layout algorithms
- Smooth layout transitions
- Tests covering edge cases

---

### Phase 3: WebCola Advanced Layouts (Subtask 3)

**Goal:** Implement constraint-based and force-directed layouts

**Tasks:**

1. Integrate WebCola library
2. Implement force-directed layout
3. Implement constraint-based layout
4. Add layout constraints (alignment, separation, grouping)
5. Optimize performance for large graphs
6. Unit tests for WebCola layouts

**Deliverables:**

- Force-directed layout
- Constraint-based layout with customizable constraints
- Performance optimizations

---

### Phase 4: UI Controls & Configuration (Subtask 4)

**Goal:** Add user controls for layout selection and configuration

**Tasks:**

1. Design layout configuration panel UI (Chakra UI)
2. Add layout selector dropdown/menu
3. Implement layout options panel (spacing, direction, etc.)
4. Add "Apply Layout" button
5. Add "Auto-layout on change" toggle
6. Create layout preset system
7. Add "Save current positions" feature
8. Persist user preferences (localStorage or backend)

**Deliverables:**

- Layout configuration widget in toolbar
- Working UI controls
- Preset management system
- User preference persistence

---

### Phase 5: Dynamic Layout Mode (Subtask 5)

**Goal:** Support automatic layout on graph changes

**Tasks:**

1. Detect node/edge additions/removals
2. Implement incremental layout updates
3. Add debouncing/throttling for performance
4. Handle layout conflicts with manual positioning
5. Add smooth transitions for dynamic changes

**Deliverables:**

- Automatic layout on graph changes
- Smooth, performant transitions
- Conflict resolution between auto and manual layout

---

### Phase 6: Testing & Documentation (Subtask 6)

**Goal:** Comprehensive testing and user documentation

**Tasks:**

1. Unit tests for all layout algorithms
2. Integration tests for UI controls
3. E2E tests for layout workflows
4. Performance benchmarks
5. User documentation
6. Code documentation
7. Migration guide

**Deliverables:**

- > 80% test coverage for new code
- Complete documentation
- Performance benchmarks

---

## Acceptance Criteria

### Must Have (P0)

- [ ] Dagre vertical tree layout (TB) working correctly
- [ ] Dagre horizontal tree layout (LR) working correctly
- [ ] Layout selection UI in toolbar
- [ ] "Apply Layout" manual trigger
- [ ] Respects glued node constraints (listeners, etc.)
- [ ] Smooth transitions when applying layout
- [ ] Feature flag to enable/disable experimental layouts
- [ ] "Save current positions" functionality
- [ ] Positions persist across sessions

### Should Have (P1)

- [ ] WebCola force-directed layout
- [ ] WebCola constraint-based layout
- [ ] Auto-layout on graph changes (toggle-able)
- [ ] Layout presets (saved configurations)
- [ ] Layout configuration panel with options (spacing, padding, etc.)
- [ ] Undo/redo for layout changes
- [ ] Performance optimization for large graphs (>100 nodes)

### Nice to Have (P2)

- [ ] Additional dagre layouts (radial, circular)
- [ ] Custom constraint builder UI
- [ ] Layout preview before applying
- [ ] Animation speed control
- [ ] "Fit to view" after layout
- [ ] Layout comparison tool
- [ ] Export/import layout configurations

---

## Recommended Layout Algorithms

Based on workspace topology analysis:

### 1. **Hierarchical Tree (Dagre TB/LR)** - PRIMARY

**Use case:** Default layout for hub-and-spoke topology
**Why:** Edge node as root, adapters/bridges as children, devices as leaves
**Configuration:**

- Rank direction: TB (top-bottom) or LR (left-right)
- Rank separation: 100-150px
- Node separation: 50-80px
- Edge routing: Smooth/polyline

### 2. **Force-Directed (WebCola)** - EXPERIMENTAL

**Use case:** Organic, balanced distribution of nodes
**Why:** Good for discovering clusters and relationships
**Configuration:**

- Link distance: 100-200px
- Charge/repulsion: Adjustable
- Gravity: Center or multi-center
- Constraints: Keep edge node central

### 3. **Layered/Hierarchical (WebCola)** - SECONDARY

**Use case:** Strict hierarchy with alignment
**Why:** Clear data flow visualization (sources → edge → sinks)
**Configuration:**

- Flow direction: Horizontal or vertical
- Layer separation: 150px
- Node alignment: Center/top/bottom per layer
- Constraints: Group related nodes

### 4. **Radial/Circular (Custom or ELK)** - FUTURE

**Use case:** Emphasize central edge node
**Why:** Natural for hub-spoke, shows all connections equally
**Configuration:**

- Radius: Based on node count
- Angular separation: Even distribution
- Concentric rings: By node type/distance

### 5. **Manual with Smart Guides** - ALWAYS AVAILABLE

**Use case:** User wants full control
**Why:** Allow manual positioning but save/restore layouts
**Configuration:**

- Snap to grid
- Alignment guides
- Distribution tools

---

## Technical Decisions

### Feature Flag System

**Decision:** Use environment variable + runtime toggle

```typescript
// config/features.ts
export const FEATURES = {
  WORKSPACE_AUTO_LAYOUT: import.meta.env.VITE_FEATURE_AUTO_LAYOUT === 'true' || false,
}
```

### Layout State Management

**Decision:** Extend existing Zustand workspace store

```typescript
interface WorkspaceState {
  // ...existing
  layoutConfig: LayoutConfiguration
  layoutHistory: LayoutHistoryEntry[]
  isAutoLayoutEnabled: boolean
}
```

### Layout Algorithm Interface

**Decision:** Strategy pattern for swappable algorithms

```typescript
interface LayoutAlgorithm {
  name: string
  type: LayoutType
  apply(nodes: Node[], edges: Edge[], options: LayoutOptions): Promise<Node[]>
  supports(topology: GraphTopology): boolean
}
```

### Performance Strategy

**Decision:**

- Web Worker for heavy layouts (>50 nodes)
- Debounce auto-layout (500ms)
- Memoize layout calculations
- Progressive rendering for large graphs

---

## Next Steps

1. Install dependencies (dagre, webcola)
2. Create feature flag configuration
3. Define layout types and interfaces
4. Set up layout algorithm registry
5. Implement first dagre layout (vertical tree)
6. Create basic UI control for layout selection

---

## Questions & Notes

**Q: Should layout respect existing manual positions?**
A: Yes - need "lock node" feature or "reset to layout" option

**Q: How to handle grouped nodes (CLUSTER_NODE)?**
A: Layout children within group bounds, treat group as single node for outer layout

**Q: Performance target?**
A: <100ms for <50 nodes, <500ms for <200 nodes, use web worker for larger graphs

**Q: Should we support custom layout algorithms via plugins?**
A: Future consideration - P2/P3 feature

---

## References

- React Flow Layout Examples: https://reactflow.dev/examples/layout
- Dagre Documentation: https://github.com/dagrejs/dagre
- WebCola Documentation: https://ialab.it.monash.edu/webcola/
- ELK.js (future): https://github.com/kieler/elkjs

---

**End of Subtask 1 Planning**
