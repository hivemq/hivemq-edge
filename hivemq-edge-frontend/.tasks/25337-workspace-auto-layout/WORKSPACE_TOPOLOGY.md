# Workspace Topology Reference

**Task:** 25337-workspace-auto-layout  
**Document Type:** Technical Reference  
**Purpose:** Understanding workspace graph structure for layout algorithms

---

## Overview

This document describes the topology, node types, edge types, and connection patterns in the HiveMQ Edge workspace. This information is critical for designing appropriate layout algorithms.

---

## Node Types

### 1. Edge Node (EDGE_NODE)

**ID Pattern:** `edge-{instance}`  
**Role:** Central hub/gateway  
**Typical Count:** 1 (singleton)  
**Visual:** Large circular node  
**Connections:**

- Parent to: Adapters, Bridges, Listeners
- Connected to: External hosts (via bridges)

**Special Properties:**

- Typically the root node in hierarchical layouts
- Natural center point for radial layouts
- Should be prominent/central in most layouts

---

### 2. Adapter Node (ADAPTER_NODE)

**ID Pattern:** `adapter-{adapterId}`  
**Role:** Protocol adapter (MQTT, OPC-UA, Modbus, etc.)  
**Typical Count:** 1-20  
**Visual:** Rectangular node with protocol icon  
**Connections:**

- Child of: Edge node
- Parent to: Device nodes
- Publishes to: MQTT topics

**Data Structure:**

```typescript
interface Adapter {
  id: string
  protocolId: string
  type: string
  status?: 'CONNECTED' | 'DISCONNECTED' | 'ERROR'
  config: Record<string, unknown>
}
```

**Layout Considerations:**

- Group by protocol type
- Arrange in same layer (layer 1 from Edge)
- May have many children (devices)

---

### 3. Bridge Node (BRIDGE_NODE)

**ID Pattern:** `bridge-{bridgeId}`  
**Role:** MQTT bridge to remote broker  
**Typical Count:** 0-10  
**Visual:** Rectangular node with bridge icon  
**Connections:**

- Child of: Edge node
- Connected to: Remote host nodes
- Bidirectional: Local ↔ Remote topic forwarding

**Data Structure:**

```typescript
interface Bridge {
  id: string
  remoteHost: string
  remotePort: number
  status?: 'CONNECTED' | 'DISCONNECTED'
  localSubscriptions: string[]
  remoteSubscriptions: string[]
}
```

**Layout Considerations:**

- Same layer as adapters (layer 1)
- May extend to external hosts (layer 2+)
- Bidirectional edge styling

---

### 4. Listener Node (LISTENER_NODE)

**ID Pattern:** `listener-{listenerId}`  
**Role:** MQTT listener (inbound connections)  
**Typical Count:** 1-5  
**Visual:** Small rectangular node  
**Connections:**

- **Glued to Edge node** (position-locked!)
- No other connections

**Data Structure:**

```typescript
interface Listener {
  id: string
  port: number
  protocol: 'mqtt' | 'websocket'
  bindAddress: string
}
```

**⚠️ Layout Constraint:**

- **MUST stay at fixed offset from Edge node**
- Implemented via `gluedNodeDefinition` in `nodes-utils.ts`
- Example: Always 50px below and 50px right of Edge node
- **Cannot be moved by layout algorithms independently**

---

### 5. Device Node (DEVICE_NODE)

**ID Pattern:** `device-{adapterId}-{deviceId}`  
**Role:** Connected device/sensor  
**Typical Count:** 0-100+ per adapter  
**Visual:** Small rectangular node with device icon  
**Connections:**

- Child of: Adapter node
- Leaf node (no children)

**Data Structure:**

```typescript
interface DeviceMetadata {
  sourceAdapterId: string
  label: string
  // ... protocol-specific fields
}
```

**Layout Considerations:**

- Leaf nodes in hierarchy
- Group by parent adapter
- May be numerous (performance concern)
- Layer 2+ from Edge

---

### 6. Host Node (HOST_NODE)

**ID Pattern:** `host-{hostname}`  
**Role:** External/remote host  
**Typical Count:** 0-10  
**Visual:** Rectangular node with host icon  
**Connections:**

- Connected to: Bridge nodes
- Represents external systems

**Layout Considerations:**

- Typically rightmost/bottom in layout
- May be hidden in minimap
- External to main topology

---

### 7. Group/Cluster Node (CLUSTER_NODE)

**ID Pattern:** `group-{groupId}`  
**Role:** Logical grouping container  
**Typical Count:** 0-5  
**Visual:** Large rectangular area containing children  
**Connections:**

- Contains: Multiple child nodes
- May connect to: Other groups or nodes

**Data Structure:**

```typescript
interface Group {
  childrenNodeIds: string[]
  title: string
  isOpen: boolean
  colorScheme?: string
}
```

**⚠️ Layout Constraint:**

- **Must layout children within its bounds**
- Requires nested/recursive layout
- Children cannot escape group boundary
- Group size adapts to children

---

### 8. Combiner Node (COMBINER_NODE)

**ID Pattern:** `combiner-{combinerId}`  
**Role:** Data combination/aggregation  
**Typical Count:** 0-5  
**Visual:** Rectangular node with combiner icon  
**Connections:**

- Can have multiple inputs
- Single output

---

### 9. Pulse Node (PULSE_NODE)

**ID Pattern:** `pulse-{pulseId}`  
**Role:** Health/heartbeat monitoring  
**Typical Count:** 0-5  
**Visual:** Small node with pulse icon  
**Connections:**

- Monitoring edges to monitored resources

**Data Structure:**

```typescript
interface NodePulseType {
  label: string
  id: string
  status?: PulseStatus
}
```

---

## Edge Types

### 1. Report Edge (REPORT_EDGE)

**Purpose:** Data flow/reporting connection  
**Visual:** Solid line with arrow  
**Direction:** Source → Target  
**Typical Use:**

- Adapter → Edge
- Device → Adapter
- Bridge ↔ Remote

**Data Structure:**

```typescript
interface Edge {
  id: string
  source: string
  target: string
  type: EdgeTypes.REPORT_EDGE
  data?: {
    topics?: string[]
    status?: EdgeStatus
  }
}
```

---

### 2. Dynamic Edge (DYNAMIC_EDGE)

**Purpose:** Dynamic/runtime connections  
**Visual:** Dashed or styled line  
**Direction:** Bidirectional possible  
**Typical Use:**

- Runtime-created connections
- Temporary links

---

## Topology Patterns

### Pattern 1: Simple Hub-and-Spoke

**Most Common**

```
              [Edge]
                |
        +--------------+
        |      |       |
    [Adapter] [Bridge] [Listener]
        |              (glued to Edge)
    +-------+
    |       |
 [Dev1]  [Dev2]
```

**Characteristics:**

- Single Edge node as hub
- 1-10 adapters/bridges as spokes
- 0-100 devices as leaves
- 1-3 listeners glued to Edge

**Best Layout:** Dagre vertical tree (TB)

---

### Pattern 2: Multi-Protocol Hierarchy

```
                    [Edge]
                      |
        +-------------+-------------+
        |             |             |
   [OPC-UA Adapter] [Modbus] [MQTT Bridge]
        |                            |
    +---+---+                    [Remote Host]
    |   |   |
  [M1][M2][M3]
```

**Characteristics:**

- Diverse protocol adapters
- Different device counts per adapter
- May include external hosts

**Best Layout:** Dagre horizontal tree (LR) or constraint-based

---

### Pattern 3: Clustered/Grouped

```
                    [Edge]
                      |
        +-------------+-------------+
        |                           |
   [Group: Factory 1]          [Group: Factory 2]
        |                           |
    +---------+               +---------+
    |         |               |         |
[Adapter1][Adapter2]     [Adapter3][Adapter4]
```

**Characteristics:**

- Logical grouping by location/function
- Nested layout required
- Groups may have different sizes

**Best Layout:** Hierarchical with grouping constraints

---

### Pattern 4: Complex Network

```
        [Host1]         [Host2]
           |              |
        [Bridge1]-----[Edge]-----[Bridge2]
                         |
           +-------------+-------------+
           |             |             |
       [Adapter1]    [Adapter2]    [Combiner]
           |             |             |
       [Devices...]  [Devices...]   [Output]
```

**Characteristics:**

- Multiple bridges and external connections
- Data combiners/aggregators
- More interconnected (not pure tree)

**Best Layout:** Force-directed or constrained flow

---

## Graph Characteristics

### Size Metrics

- **Small:** 5-20 nodes, 5-15 edges
- **Medium:** 20-100 nodes, 15-80 edges
- **Large:** 100-500 nodes, 80-400 edges
- **Very Large:** 500+ nodes, 400+ edges

### Density

- **Sparse:** Tree-like, ~(n-1) edges for n nodes
- **Typical:** Hub-spoke with some cross-links, ~n edges
- **Dense:** Many interconnections, >2n edges

### Depth (Tree Depth)

- **Level 0:** Edge node (1)
- **Level 1:** Adapters, Bridges (1-20)
- **Level 2:** Devices, Hosts (0-100)
- **Level 3+:** Rarely deeper than 3 levels

---

## Layout Constraints

### Hard Constraints (MUST respect)

1. **Glued Nodes:**

   - Listener nodes MUST stay at fixed offset from Edge
   - Implemented in `getGluedPosition()` and `gluedNodeDefinition`
   - Example offsets:
     ```typescript
     const gluedNodeDefinition = {
       [NodeTypes.LISTENER_NODE]: [NodeTypes.EDGE_NODE, 50, 'target'],
       // Type: EDGE_NODE, Offset: 50px, Handle: target
     }
     ```

2. **Group Boundaries:**

   - Child nodes MUST stay within group bounds
   - Group size MUST accommodate all children
   - Children cannot be positioned outside parent

3. **Node Overlap:**
   - Nodes SHOULD NOT overlap (unless intentional)
   - Minimum spacing: ~50px between nodes

### Soft Constraints (SHOULD respect)

1. **Edge Direction:**

   - Data flows logically (source → target)
   - Prefer top-to-bottom or left-to-right for hierarchies

2. **Visual Grouping:**

   - Similar node types should be near each other
   - Same protocol adapters grouped

3. **Edge Crossings:**

   - Minimize edge crossings for readability
   - Orthogonal edges preferred for clean look

4. **Symmetry:**
   - Balanced layout when possible
   - Central hub node centered

---

## React Flow Integration Notes

### Node Structure

```typescript
interface Node {
  id: string
  type: NodeTypes
  position: { x: number; y: number }
  data: NodeData
  width?: number
  height?: number
  selected?: boolean
  draggable?: boolean
}
```

### Position Coordinate System

- **Origin:** Top-left corner of canvas (0, 0)
- **Units:** Pixels
- **Node anchor:** Top-left corner of node
- **Dagre anchor:** Center of node (must convert!)

### Node Dimensions

Typical sizes (approximate):

- Edge node: 100x100 (circular, diameter)
- Adapter node: 172x36
- Device node: 150x30
- Listener node: 120x30
- Group node: Variable (based on children)

### Handle Positions

```typescript
// For directed edges
node.sourcePosition = 'top' | 'right' | 'bottom' | 'left'
node.targetPosition = 'top' | 'right' | 'bottom' | 'left'

// Example for vertical tree:
sourcePosition: 'bottom'
targetPosition: 'top'
```

---

## Data Flow Patterns

### Inbound Data Flow

```
[External Device]
    → [Adapter]
    → [Edge]
    → MQTT Topic
```

### Outbound Data Flow

```
MQTT Topic
    → [Edge]
    → [Bridge]
    → [Remote Host]
```

### Bidirectional Bridge Flow

```
[Local Topics]
    ↔ [Bridge]
    ↔ [Remote Broker]
```

---

## Special Considerations for Layout Algorithms

### Dagre (Hierarchical)

- ✅ Perfect for hub-spoke
- ✅ Handles tree structure well
- ⚠️ Must handle glued nodes separately
- ⚠️ Need to convert center-anchor to top-left-anchor
- ❌ Not great for cyclic graphs

### WebCola Force-Directed

- ✅ Good for complex interconnections
- ✅ Natural clustering emerges
- ✅ Handles cycles well
- ⚠️ Need center constraint for Edge node
- ⚠️ May require many iterations
- ❌ Can be slow for large graphs

### WebCola Constraint-Based

- ✅ Strict hierarchical layers
- ✅ Precise control over alignment
- ✅ Good for data flow visualization
- ⚠️ Requires layer classification algorithm
- ⚠️ Complex constraint setup
- ❌ Less flexible than force-directed

---

## Example Graph Structures for Testing

### Test Case 1: Minimal (5 nodes)

```typescript
nodes: [
  { id: 'edge', type: NodeTypes.EDGE_NODE },
  { id: 'adapter-1', type: NodeTypes.ADAPTER_NODE },
  { id: 'listener-1', type: NodeTypes.LISTENER_NODE }, // glued!
  { id: 'device-1', type: NodeTypes.DEVICE_NODE },
  { id: 'device-2', type: NodeTypes.DEVICE_NODE },
]
edges: [
  { source: 'adapter-1', target: 'edge' },
  { source: 'device-1', target: 'adapter-1' },
  { source: 'device-2', target: 'adapter-1' },
]
```

### Test Case 2: Medium (20 nodes)

- 1 Edge
- 3 Adapters
- 2 Bridges
- 1 Listener (glued)
- 10 Devices
- 2 Hosts
- 1 Group containing 2 adapters

### Test Case 3: Large (100 nodes)

- 1 Edge
- 10 Adapters
- 5 Bridges
- 2 Listeners (glued)
- 75 Devices
- 5 Hosts
- 2 Groups

---

## References

- `src/modules/Workspace/types.ts` - Node/Edge type definitions
- `src/modules/Workspace/utils/nodes-utils.ts` - Glued node logic
- `src/modules/Workspace/hooks/useGetFlowElements.ts` - Graph data fetching

---

**Document Version:** 1.0  
**Last Updated:** October 27, 2025
