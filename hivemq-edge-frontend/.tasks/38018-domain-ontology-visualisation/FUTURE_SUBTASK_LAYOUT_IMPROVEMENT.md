# Task 38018 - Future Subtask: Replace Custom Layout with WebCola/Dagre

**Priority:** Medium  
**Depends On:** Subtask 2, 3, 4 complete  
**Estimated Effort:** 1-2 days

---

## Objective

Replace the custom force-directed layout algorithm in NetworkGraphView with proven layout engines (WebCola or Dagre) that are already used in the Workspace module.

---

## Current Situation

### What We Have

**Custom Algorithm** (`NetworkGraphView.tsx` lines 33-224):

- Simple physics simulation
- Repulsion + directional flow forces
- 200 iterations
- Works well for simple graphs (< 15 nodes)

**Current Graph Complexity:**

```
12 nodes (3 TAGs, 4 TOPICs, 4 FILTERs, 1 combiner result)
Multiple edge types (NORTHBOUND, SOUTHBOUND, BRIDGE, COMBINER, ASSET_MAPPER)
```

### Problems with Custom Algorithm

1. **Limited Scalability** - Struggles with 20+ nodes
2. **Poor Clustering** - No semantic grouping (by type, connectivity)
3. **Overlap Issues** - Nodes can overlap in dense graphs
4. **No Hierarchy Support** - Can't show clear data flow direction
5. **Maintenance Burden** - Custom physics is hard to tune

---

## Available Solutions in Codebase

### WebCola (Force-Directed)

**File:** `/src/modules/Workspace/utils/layout/cola-force-layout.ts`

**Advantages:**

- Physics-based natural clustering
- Automatic overlap removal
- Handles disconnected components
- Configurable link distance
- Convergence detection

**Best For:**

- Graphs with mixed connectivity
- When showing organic relationships
- Non-hierarchical structures

**Example Usage:**

```typescript
const layout = new cola.Layout()
  .nodes(colaNodes)
  .links(colaLinks)
  .linkDistance(350)
  .avoidOverlaps(true)
  .handleDisconnected(true)
  .start(1000)
```

### Dagre (Hierarchical)

**File:** `/src/modules/Workspace/utils/layout/dagre-layout.ts`

**Advantages:**

- Clear hierarchical flow (top → bottom, left → right)
- Perfect for data pipeline visualization
- Minimal edge crossings
- Consistent node spacing
- Fast for directed acyclic graphs

**Best For:**

- TAG → TOPIC → FILTER flows
- Transformation pipeline visualization
- When direction matters

**Example Usage:**

```typescript
const g = new dagre.graphlib.Graph()
g.setGraph({ rankdir: 'TB', nodesep: 100, ranksep: 150 })
g.setDefaultEdgeLabel(() => ({}))
// Add nodes and edges
dagre.layout(g)
```

---

## Recommended Approach

### Phase 1: Extract Layout Interface

Create shared layout utilities:

**File:** `src/modules/DomainOntology/utils/network-graph-layout.ts`

```typescript
export type NetworkGraphLayoutType = 'FORCE' | 'HIERARCHICAL' | 'RADIAL'

export interface LayoutOptions {
  linkDistance?: number
  nodeSpacing?: number
  direction?: 'TB' | 'LR' | 'BT' | 'RL'
}

export interface LayoutAlgorithm {
  apply(nodes: NetworkGraphNode[], edges: NetworkGraphEdge[], options?: LayoutOptions): NetworkGraphNode[]
}
```

### Phase 2: Implement WebCola Adapter

Reuse Workspace's WebCola implementation:

```typescript
import { ColaForceLayoutAlgorithm } from '@/modules/Workspace/utils/layout/cola-force-layout'

export class NetworkGraphColaLayout implements LayoutAlgorithm {
  apply(nodes, edges, options) {
    // Convert NetworkGraphNode → ReactFlow Node
    // Apply cola-force-layout
    // Convert back → NetworkGraphNode
  }
}
```

### Phase 3: Implement Dagre Adapter

Reuse Workspace's Dagre implementation:

```typescript
import { DagreLayoutAlgorithm } from '@/modules/Workspace/utils/layout/dagre-layout'

export class NetworkGraphHierarchicalLayout implements LayoutAlgorithm {
  apply(nodes, edges, options) {
    // Convert to dagre format
    // Apply hierarchical layout
    // Convert back
  }
}
```

### Phase 4: Add Layout Switcher UI

Add controls to NetworkGraphView:

```tsx
<Controls>
  <ControlButton onClick={() => setLayout('FORCE')}>Force Layout</ControlButton>
  <ControlButton onClick={() => setLayout('HIERARCHICAL')}>Hierarchical</ControlButton>
  <ControlButton onClick={() => setLayout('RADIAL')}>Radial</ControlButton>
</Controls>
```

---

## Implementation Plan

### Step 1: Create Layout Abstraction (2 hours)

- [ ] Create `network-graph-layout.ts` interface
- [ ] Create layout registry pattern
- [ ] Add layout type selection state

### Step 2: WebCola Integration (3 hours)

- [ ] Create WebCola adapter for NetworkGraph types
- [ ] Test with current 12-node graph
- [ ] Compare with custom algorithm
- [ ] Tune parameters (link distance, overlap removal)

### Step 3: Dagre Integration (3 hours)

- [ ] Create Dagre adapter
- [ ] Implement hierarchical flow (TAG → TOPIC → FILTER)
- [ ] Test with directional transformations
- [ ] Handle cycles (bridge subscriptions)

### Step 4: UI Controls (2 hours)

- [ ] Add layout switcher buttons
- [ ] Persist layout preference (localStorage)
- [ ] Add "Re-layout" button
- [ ] Smooth transitions between layouts

### Step 5: Testing & Polish (2 hours)

- [ ] Test with large graphs (50+ nodes)
- [ ] Performance benchmarking
- [ ] Edge case handling (disconnected components, cycles)
- [ ] Documentation

**Total Effort:** ~12 hours (1.5 days)

---

## Benefits

### Immediate

- ✅ Better scalability (handles 100+ nodes)
- ✅ Professional-quality layouts
- ✅ Automatic overlap removal
- ✅ Less maintenance burden

### Long-term

- ✅ Multiple layout options for users
- ✅ Reusable across other visualizations
- ✅ Easier to add new layout types
- ✅ Consistent with Workspace patterns

---

## Migration Strategy

### Backward Compatible

Keep custom algorithm as fallback:

```typescript
const layoutAlgorithms = {
  FORCE: new NetworkGraphColaLayout(),
  HIERARCHICAL: new NetworkGraphDagreLayout(),
  SIMPLE: new SimpleForceLayout(), // Current custom algorithm
}

const layout = layoutAlgorithms[userPreference] || layoutAlgorithms.SIMPLE
```

### Gradual Rollout

1. **Phase 1:** Add WebCola as optional (beta flag)
2. **Phase 2:** Make WebCola default, custom as fallback
3. **Phase 3:** Remove custom algorithm after validation

---

## Alternative: ELK (Elk Layered)

**If WebCola/Dagre insufficient:**

Eclipse Layout Kernel (ELK) offers advanced algorithms:

- Layered (hierarchical with layer optimization)
- Force (similar to WebCola but more features)
- Stress (stress-minimization)
- Radial (hub-spoke patterns)

**Trade-off:** Larger bundle size (~200kb), but very powerful

---

## References

### Existing Implementations

- `src/modules/Workspace/utils/layout/cola-force-layout.ts`
- `src/modules/Workspace/utils/layout/dagre-layout.ts`
- `src/modules/Workspace/utils/layout/cola-utils.ts`
- `src/modules/Workspace/utils/layout/layout-registry.ts`

### Documentation

- [WebCola Documentation](https://ialab.it.monash.edu/webcola/)
- [Dagre Wiki](https://github.com/dagrejs/dagre/wiki)
- [React Flow Layout Docs](https://reactflow.dev/learn/layouting)
- Task 25337 - Workspace Auto Layout implementation

---

## Success Criteria

- [ ] Graph scales to 50+ nodes without performance issues
- [ ] No node overlaps in force-directed mode
- [ ] Clear hierarchy in hierarchical mode
- [ ] User can switch layouts seamlessly
- [ ] Layout calculation < 500ms for 50 nodes
- [ ] Tests pass for all layout types

---

**Status:** 📋 PLANNED  
**Priority:** Medium  
**Recommendation:** Implement after basic functionality is complete and validated
