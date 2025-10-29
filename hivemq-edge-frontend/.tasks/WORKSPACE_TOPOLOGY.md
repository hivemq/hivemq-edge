# HiveMQ Edge Workspace Topology Reference

**Last Updated:** October 26, 2025  
**Purpose:** Comprehensive reference for workspace structure, node types, connections, and status propagation

---

## Table of Contents

1. [Overview](#overview)
2. [Node Types](#node-types)
3. [Edge Connections](#edge-connections)
4. [Status System](#status-system)
5. [Data Flow](#data-flow)
6. [Implementation Reference](#implementation-reference)

---

## Overview

The HiveMQ Edge Workspace is a **graph-based representation** of the Edge broker topology, visualizing data transformation flows between different entities.

### Graph Theory Context

- **Nodes** = Entities (adapters, bridges, devices, etc.)
- **Edges** = Connections (data transformation flows)
- **Groups** = Nested graphs (logical grouping)
- **Constraint** = Directed Acyclic Graph (DAG)

### Key Principles

1. **Connections represent configuration**, not necessarily active data flow
2. **Active nodes** have their own runtime status (adapters, bridges, pulse)
3. **Passive nodes** derive status from upstream connections
4. **Status propagates** through the graph topology

---

## Node Types

### Active Nodes (Have Own Runtime Status)

#### 1. ADAPTER_NODE

**Purpose:** Software communicating with devices via specific protocols

**File:** [`src/modules/Workspace/components/nodes/NodeAdapter.tsx`](../../src/modules/Workspace/components/nodes/NodeAdapter.tsx)

**Data Type:**

```typescript
NodeAdapterType = Node<Adapter & { statusModel?: NodeStatusModel }, NodeTypes.ADAPTER_NODE>
```

**Status Sources:**

- `Status` (connection + runtime from API)
- Operational status based on north/south mappings (bidirectional adapters need both)

**Outbound Connections:**

- → `DEVICE_NODE` (device communication)
- → `EDGE_NODE` (northbound data flow)
- → `COMBINER_NODE` (data combining)
- → `ASSET_MAPPER_NODE` (pulse asset mapping)

**Key Properties:**

- Has adapter type (defines protocol)
- Has tags (data points from device)
- Has mappings (north/south configuration)

---

#### 2. BRIDGE_NODE

**Purpose:** Bidirectional MQTT bridge to enterprise brokers

**File:** [`src/modules/Workspace/components/nodes/NodeBridge.tsx`](../../src/modules/Workspace/components/nodes/NodeBridge.tsx)

**Data Type:**

```typescript
NodeBridgeType = Node<Bridge & { statusModel?: NodeStatusModel }, NodeTypes.BRIDGE_NODE>
```

**Status Sources:**

- `Status` (connection + runtime from API)
- Operational status based on remote topic filters

**Outbound Connections:**

- → `HOST_NODE` (remote broker)
- → `EDGE_NODE` (local broker)
- → `COMBINER_NODE` (data combining)
- → `ASSET_MAPPER_NODE` (pulse asset mapping)

**Key Properties:**

- Has remote topics (upstream subscriptions)
- Has local topics (local subscriptions)
- Bidirectional communication

---

#### 3. PULSE_NODE

**Purpose:** Distributed execution engine integrating with HiveMQ Pulse

**File:** [`src/modules/Workspace/components/nodes/NodePulse.tsx`](../../src/modules/Workspace/components/nodes/NodePulse.tsx)

**Data Type:**

```typescript
NodePulseType = Node<
  {
    label: string
    id: string
    status?: PulseStatus
    statusModel?: NodeStatusModel
  },
  NodeTypes.PULSE_NODE
>
```

**Status Sources:**

- `PulseStatus` (activation + runtime from API)
- Operational status based on connected asset mappers with valid mappings

**Outbound Connections:**

- → `EDGE_NODE` (data publishing)
- → `ASSET_MAPPER_NODE` (asset-based data transformation)

**Key Properties:**

- Has managed assets (MAPPED or UNMAPPED)
- Connects specifically to asset mappers
- Overall operational if ANY connected mapper has valid mappings

**Status Logic:**

```typescript
// File: src/modules/Workspace/utils/edge-operational-status.utils.ts
computePulseNodeOperationalStatus(validCombiners, allAssets.items)
// Returns ACTIVE if at least one combiner has mappings referencing valid assets
```

---

### Passive Nodes (Derive Status from Upstream)

#### 4. DEVICE_NODE

**Purpose:** Physical/digital hardware connected via adapter

**File:** [`src/modules/Workspace/components/nodes/NodeDevice.tsx`](../../src/modules/Workspace/components/nodes/NodeDevice.tsx)

**Data Type:**

```typescript
NodeDeviceType = Node<DeviceMetadata & { statusModel?: NodeStatusModel }, NodeTypes.DEVICE_NODE>
```

**Status Derivation:**

- Runtime: Inherited from parent adapter
- Operational: ACTIVE if device has tags configured

**Inbound Connections:**

- ← `ADAPTER_NODE`

**Key Properties:**

- Associated with specific adapter (sourceAdapterId)
- Has device tags (data points)

---

#### 5. HOST_NODE

**Purpose:** Physical/digital hardware that bridge connects to

**File:** [`src/modules/Workspace/components/nodes/NodeHost.tsx`](../../src/modules/Workspace/components/nodes/NodeHost.tsx)

**Data Type:**

```typescript
NodeHostType = Node<{ label: string; statusModel?: NodeStatusModel }, NodeTypes.HOST_NODE>
```

**Status Derivation:**

- Runtime: Inherited from parent bridge
- Operational: Always ACTIVE (bridge itself has topic filters)

**Inbound Connections:**

- ← `BRIDGE_NODE`

---

#### 6. COMBINER_NODE

**Purpose:** Combines data from multiple sources into single output

**File:** [`src/modules/Workspace/components/nodes/NodeCombiner.tsx`](../../src/modules/Workspace/components/nodes/NodeCombiner.tsx)

**Data Type:**

```typescript
NodeCombinerType = Node<Combiner & { statusModel?: NodeStatusModel }, NodeTypes.COMBINER_NODE>
```

**API Type:** [`src/api/__generated__/models/Combiner.ts`](../../src/api/__generated__/models/Combiner.ts)

**Status Derivation:**

- Runtime: ERROR if any upstream ERROR, else ACTIVE if any upstream ACTIVE
- Operational: ACTIVE if has mappings configured, else INACTIVE

**Inbound Connections:**

- ← `ADAPTER_NODE` (tag sources)
- ← `BRIDGE_NODE` (topic filter sources)
- ← `PULSE_NODE` (when acting as asset mapper)

**Outbound Connections:**

- → `EDGE_NODE`

**Key Properties:**

```typescript
{
  sources: EntityReferenceList // List of adapters/bridges/pulse
  mappings: DataCombiningList // Transformation rules
}
```

**Special Case: Asset Mapper**

- Has `EntityType.PULSE_AGENT` in sources
- Mappings reference asset IDs from Pulse
- Destination uses `assetId` field

---

#### 7. EDGE_NODE

**Purpose:** Central hub - gateway to MQTT broker

**File:** [`src/modules/Workspace/components/nodes/NodeEdge.tsx`](../../src/modules/Workspace/components/nodes/NodeEdge.tsx)

**Data Type:**

```typescript
NodeEdgeType = Node<{ label: string; statusModel?: NodeStatusModel }, NodeTypes.EDGE_NODE>
```

**Status Derivation:**

- Runtime: Aggregates all upstream nodes (ERROR > ACTIVE > INACTIVE)
- Operational: ACTIVE if has topic filters configured

**Inbound Connections:**

- ← `ADAPTER_NODE`
- ← `BRIDGE_NODE`
- ← `PULSE_NODE`
- ← `COMBINER_NODE`

**Outbound Connections:**

- → `LISTENER_NODE`

---

#### 8. LISTENER_NODE

**Purpose:** MQTT listeners on the broker

**File:** [`src/modules/Workspace/components/nodes/NodeListener.tsx`](../../src/modules/Workspace/components/nodes/NodeListener.tsx)

**Data Type:**

```typescript
NodeListenerType = Node<Listener & { statusModel?: NodeStatusModel }, NodeTypes.LISTENER_NODE>
```

**Status Derivation:**

- Runtime: Inherited from Edge node
- Operational: Always ACTIVE

**Inbound Connections:**

- ← `EDGE_NODE`

---

#### 9. CLUSTER_NODE (Group)

**Purpose:** Logical grouping of nodes for management

**File:** [`src/modules/Workspace/components/nodes/NodeGroup.tsx`](../../src/modules/Workspace/components/nodes/NodeGroup.tsx)

**Data Type:**

```typescript
NodeGroupType = Node<Group & { statusModel?: NodeStatusModel }, NodeTypes.CLUSTER_NODE>
```

**Status Derivation:**

- Runtime: Aggregates child nodes (ERROR > ACTIVE > INACTIVE)
- Operational: Aggregates child nodes (ERROR > ACTIVE > INACTIVE)

**Key Properties:**

```typescript
{
  childrenNodeIds: string[]  // IDs of grouped nodes
  title: string
  isOpen: boolean
  colorScheme?: string
}
```

**Aggregation Logic:** [`src/modules/Workspace/utils/group-status.spec.ts`](../../src/modules/Workspace/utils/group-status.spec.ts)

---

#### 10. ASSETS_NODE

**Purpose:** Represents Pulse assets collection

**File:** [`src/modules/Workspace/components/nodes/NodeAssets.tsx`](../../src/modules/Workspace/components/nodes/NodeAssets.tsx)

**Data Type:**

```typescript
NodeAssetsType = Node<{ label: string; id: string; statusModel?: NodeStatusModel }, NodeTypes.ASSETS_NODE>
```

**Status Derivation:**

- Runtime: Inherited from upstream sources
- Operational: ACTIVE if has mapped assets

---

## Edge Connections

### Current Topology (V1)

```
┌─────────────┐
│  ADAPTER    │──→ DEVICE
└─────────────┘
      │
      ├──→ EDGE ──→ LISTENER
      │
      ├──→ COMBINER ──→ EDGE
      │
      └──→ ASSET_MAPPER ──→ EDGE

┌─────────────┐
│   BRIDGE    │──→ HOST
└─────────────┘
      │
      ├──→ EDGE
      │
      ├──→ COMBINER ──→ EDGE
      │
      └──→ ASSET_MAPPER ──→ EDGE

┌─────────────┐
│   PULSE     │──→ EDGE
└─────────────┘
      │
      └──→ ASSET_MAPPER ──→ EDGE
```

### Connection Matrix

| Source   | →   | Target       | Purpose                | Status Propagation                      |
| -------- | --- | ------------ | ---------------------- | --------------------------------------- |
| ADAPTER  | →   | DEVICE       | Protocol communication | Adapter → Device (runtime)              |
| ADAPTER  | →   | EDGE         | Northbound data        | Adapter → Edge (runtime + operational)  |
| ADAPTER  | →   | COMBINER     | Data combining         | Adapter → Combiner (runtime)            |
| ADAPTER  | →   | ASSET_MAPPER | Pulse integration      | Adapter → Mapper (runtime)              |
| BRIDGE   | →   | HOST         | Remote broker          | Bridge → Host (runtime)                 |
| BRIDGE   | →   | EDGE         | Local broker           | Bridge → Edge (runtime + operational)   |
| BRIDGE   | →   | COMBINER     | Data combining         | Bridge → Combiner (runtime)             |
| BRIDGE   | →   | ASSET_MAPPER | Pulse integration      | Bridge → Mapper (runtime)               |
| PULSE    | →   | EDGE         | Asset publishing       | Pulse → Edge (runtime + operational)    |
| PULSE    | →   | ASSET_MAPPER | Asset transformation   | **Per-edge operational status**         |
| COMBINER | →   | EDGE         | Combined output        | Combiner → Edge (runtime + operational) |
| EDGE     | →   | LISTENER     | Local consumers        | Edge → Listener (runtime)               |

---

## Status System

### Dual-Status Model

**File:** [`src/modules/Workspace/types/status.types.ts`](../../src/modules/Workspace/types/status.types.ts)

#### Runtime Status

Represents the **operational state** of nodes (are they running and connected?)

```typescript
enum RuntimeStatus {
  ACTIVE = 'ACTIVE', // Running and connected
  INACTIVE = 'INACTIVE', // Stopped or disconnected
  ERROR = 'ERROR', // Has errors or error state
}
```

#### Operational Status

Represents the **configuration completeness** for data transformation

```typescript
enum OperationalStatus {
  ACTIVE = 'ACTIVE', // Fully configured for data transformation
  INACTIVE = 'INACTIVE', // Partially configured (DRAFT) or not yet activated
  ERROR = 'ERROR', // Not configured for data transformation
}
```

#### Unified Status Model

```typescript
interface NodeStatusModel {
  runtime: RuntimeStatus
  operational: OperationalStatus
  source: StatusSource // 'ADAPTER' | 'BRIDGE' | 'PULSE' | 'DERIVED' | 'STATIC'
  originalStatus?: Status | PulseStatus
  lastUpdated?: string
}
```

---

### Status Mapping

**File:** [`src/modules/Workspace/utils/status-mapping.utils.ts`](../../src/modules/Workspace/utils/status-mapping.utils.ts)

#### Adapter/Bridge Status → Runtime Status

```typescript
mapAdapterStatusToRuntime(status?: Status): RuntimeStatus {
  // ERROR if runtime STOPPED or connection ERROR
  // ACTIVE if runtime STARTED and connection CONNECTED/STATELESS
  // INACTIVE otherwise (DISCONNECTED, UNKNOWN)
}
```

#### Pulse Status → Runtime Status

```typescript
mapPulseStatusToRuntime(status?: PulseStatus): RuntimeStatus {
  // ERROR if activation ERROR or runtime ERROR
  // INACTIVE if activation DEACTIVATED
  // ACTIVE if activation ACTIVATED and runtime CONNECTED
  // INACTIVE otherwise
}
```

---

### Status Propagation

**File:** [`src/modules/Workspace/utils/status-propagation.utils.ts`](../../src/modules/Workspace/utils/status-propagation.utils.ts)

#### Active vs Passive Nodes

```typescript
isActiveNode(nodeType: NodeTypes): boolean {
  return nodeType === NodeTypes.ADAPTER_NODE
      || nodeType === NodeTypes.BRIDGE_NODE
      || nodeType === NodeTypes.PULSE_NODE
}
```

#### Passive Node Status Computation

```typescript
computePassiveNodeRuntimeStatus(
  nodeId: string,
  edges: Edge[],
  nodes: Node[]
): RuntimeStatus {
  // 1. Get upstream active nodes
  // 2. ERROR if any upstream is ERROR
  // 3. ACTIVE if at least one upstream is ACTIVE (and no ERROR)
  // 4. INACTIVE otherwise
}
```

#### Status Priority Rules

For aggregated status (e.g., Edge node, Group nodes):

```
ERROR > ACTIVE > INACTIVE
```

---

### Operational Status Detection

**File:** [`src/modules/Workspace/utils/operational-status.utils.ts`](../../src/modules/Workspace/utils/operational-status.utils.ts)

#### Adapter Operational Status

```typescript
// Unidirectional adapters: ACTIVE if has northbound mappings
// Bidirectional adapters: ACTIVE if has BOTH north and south mappings
const hasNorth = adapter.mappings.north.length > 0
const hasSouth = adapter.mappings.south.length > 0
const operational = isBidirectional(type) ? hasNorth && hasSouth : hasNorth
```

#### Bridge Operational Status

```typescript
// ACTIVE if has remote topics configured
const hasRemoteTopics = bridge.remoteTopics.length > 0
const operational = hasRemoteTopics ? ACTIVE : INACTIVE
```

#### Pulse Operational Status

**File:** [`src/modules/Workspace/utils/edge-operational-status.utils.ts`](../../src/modules/Workspace/utils/edge-operational-status.utils.ts)

```typescript
// ACTIVE if at least one connected asset mapper has valid mappings
computePulseNodeOperationalStatus(
  connectedCombiners: NodeCombinerType[],
  pulseAssets: ManagedAsset[]
): OperationalStatus {
  // Check if any combiner has mappings referencing MAPPED assets
  return hasAnyValidConnection ? ACTIVE : INACTIVE
}
```

#### Combiner Operational Status

```typescript
// ACTIVE if has mappings configured
const hasMappings = combiner.mappings.items.length > 0
const operational = hasMappings ? ACTIVE : INACTIVE
```

---

### Per-Edge Operational Status Rules

**Implementation:** [`src/modules/Workspace/utils/status-utils.ts`](../../src/modules/Workspace/utils/status-utils.ts) - `updateEdgesStatusWithModel()`

Per-edge operational status ensures that each edge accurately reflects whether that **specific data transformation path** is configured and operational.

#### Rule 1: ADAPTER → COMBINER

**File:** `status-utils.ts` (lines ~447-476)

**Status Composition:**

```typescript
{
  runtime: adapter.statusModel.runtime,      // From source (is adapter active?)
  operational: combiner.statusModel.operational // From target (does combiner have mappings?)
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if target combiner has at least one mapping configured
- Edge is **NOT OPERATIONAL (INACTIVE)** if target combiner has no mappings

**Animation:** Edge animates only if BOTH adapter is ACTIVE AND combiner has mappings

---

#### Rule 2: BRIDGE → COMBINER

**File:** `status-utils.ts` (lines ~510-551)

**Status Composition:**

```typescript
{
  runtime: bridge.statusModel.runtime,       // From source (is bridge active?)
  operational: combiner.statusModel.operational // From target (does combiner have mappings?)
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if target combiner has at least one mapping configured
- Edge is **NOT OPERATIONAL (INACTIVE)** if target combiner has no mappings

**Animation:** Edge animates only if BOTH bridge is ACTIVE AND combiner has mappings

---

#### Rule 3: PULSE → ASSET_MAPPER (Combiner with Pulse Source)

**File:** `status-utils.ts` (lines ~554-592)

**Status Composition:**

```typescript
{
  runtime: pulse.statusModel.runtime,           // From source (is Pulse active?)
  operational: assetMapper.statusModel.operational // From target (does mapper have valid asset mappings?)
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if asset mapper has at least one mapping referencing a MAPPED Pulse asset
- Edge is **NOT OPERATIONAL (INACTIVE)** if asset mapper has no mappings or only references UNMAPPED assets

**Validation:** Uses `combinerHasValidPulseAssetMappings()` from `edge-operational-status.utils.ts`

**Animation:** Edge animates only if BOTH Pulse is ACTIVE AND mapper has valid asset mappings

---

#### Rule 4: COMBINER → EDGE (Outbound)

**File:** `status-utils.ts` (lines ~595-606)

**Status Composition:**

```typescript
{
  runtime: combiner.statusModel.runtime,        // Derived from upstream sources
  operational: combiner.statusModel.operational // Has mappings?
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if combiner has at least one mapping configured
- Edge is **NOT OPERATIONAL (INACTIVE)** if combiner has no mappings

**Animation:** Edge animates only if BOTH combiner runtime is ACTIVE AND has mappings

---

#### Rule 5: ADAPTER → DEVICE

**File:** `status-utils.ts` (lines ~439-445)

**Status Composition:**

```typescript
{
  runtime: adapter.statusModel.runtime,     // Is adapter active?
  operational: adapter.statusModel.operational // Adapter overall operational status
}
```

**Operational Logic:**

- Uses adapter's overall operational status
- For bidirectional adapters: ACTIVE if has BOTH north and south mappings
- For unidirectional adapters: ACTIVE if has northbound mappings

**Animation:** Edge animates if adapter is fully operational (has required mappings)

---

#### Rule 6: ADAPTER → EDGE

**File:** `status-utils.ts` (lines ~477-481)

**Status Composition:**

```typescript
{
  runtime: adapter.statusModel.runtime,     // Is adapter active?
  operational: adapter.statusModel.operational // Has northbound mappings?
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if adapter has at least one northbound mapping
- Edge is **NOT OPERATIONAL (INACTIVE)** if adapter has no northbound mappings

**Animation:** Edge animates if adapter is ACTIVE and has northbound mappings

---

#### Rule 7: BRIDGE → EDGE

**File:** `status-utils.ts` (lines ~552)

**Status Composition:**

```typescript
{
  runtime: bridge.statusModel.runtime,      // Is bridge active?
  operational: hasRemoteTopics ? ACTIVE : INACTIVE // Has remote topic filters?
}
```

**Operational Logic:**

- Edge is **OPERATIONAL (ACTIVE)** if bridge has remote topic filters configured
- Edge is **NOT OPERATIONAL (INACTIVE)** if bridge has no remote topics

**Animation:** Edge animates if bridge is ACTIVE and has remote topics

---

#### Rule 8: PULSE → EDGE

**File:** `status-utils.ts` (lines ~593)

**Status Composition:**

```typescript
{
  runtime: pulse.statusModel.runtime,       // Is Pulse active?
  operational: pulse.statusModel.operational // Has connected mappers with valid mappings?
}
```

**Operational Logic:**

- Uses Pulse node's overall operational status
- Pulse is operational if at least one connected asset mapper has valid asset mappings

**Animation:** Edge animates if Pulse is ACTIVE and operational

---

### Consistency Principle

**Key Rule:** For edges connecting to COMBINER nodes, the **target combiner's operational status** is used for the edge's operational status, ensuring:

1. **Visual Consistency** - All edges connected to a combiner show the same animation state
2. **Configuration Clarity** - Animation reflects whether the combiner itself is configured
3. **Data Flow Accuracy** - Only animated if the target is ready to transform data

**Exception:** Edges NOT connecting to combiners use the source node's operational status.

---

### Edge Update Triggers

**File:** [`src/modules/Workspace/components/controls/StatusListener.tsx`](../../src/modules/Workspace/components/controls/StatusListener.tsx)

Edges are re-rendered in TWO scenarios:

#### Trigger 1: API Status Changes

```typescript
useEffect(() => {
  // Updates edges when adapter/bridge status changes from API
  setEdges(updateEdgesStatusWithModel(...))
}, [adapterConnections?.items, bridgeConnections?.items])
```

#### Trigger 2: Node Data Changes

```typescript
useEffect(() => {
  // Updates edges when nodes change (e.g., combiner statusModel updates)
  setEdges(updateEdgesStatusWithModel(...))
}, [nodes, adapterTypes])
```

**Why Both?**

- Trigger 1 handles external status changes (API polling)
- Trigger 2 handles internal status changes (combiner computing its statusModel)
- Together, they ensure edges always have fresh node data for accurate rendering

---

### Fallback Logic

For robustness against timing issues, edges have fallback logic:

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  if (targetStatusModel) {
    // Use combiner's computed statusModel (preferred)
    operational = targetStatusModel.operational
  } else {
    // FALLBACK: Check combiner.mappings.items directly
    const hasMapping = targetCombiner.mappings?.items?.length > 0
    operational = hasMapping ? ACTIVE : INACTIVE
  }
}
```

This ensures edges work correctly even if the combiner's statusModel isn't available yet.

---

### Animation Requirements

For an edge to be **ANIMATED**, BOTH conditions must be true:

```typescript
edge.animated = isOperational && isRuntimeActive

where: isOperational = statusModel.operational === OperationalStatus.ACTIVE
isRuntimeActive = statusModel.runtime === RuntimeStatus.ACTIVE
```

**Visual Meaning:**

- **Color** (from runtime status) = Is the source node active/connected?
- **Animation** (from operational status) = Is the data transformation configured?

---

## Data Flow

### Configuration Flow (What Edges Represent)

```
┌─────────────┐
│  Physical   │  1. Device generates data
│   Device    │
└─────────────┘
      │
      ▼ (protocol)
┌─────────────┐
│   ADAPTER   │  2. Adapter reads data via protocol
│   (tags)    │
└─────────────┘
      │
      ├──→ EDGE         3a. Direct to broker (northbound mapping)
      │
      └──→ COMBINER     3b. Combine with other sources
             │
             └──→ EDGE  4. Combined output to broker
```

### Status Propagation Flow

```
┌─────────────┐
│   ADAPTER   │  Status from API (Status.connection + Status.runtime)
│  (ACTIVE)   │
└─────────────┘
      │
      ├──→ DEVICE        Inherits runtime status (ACTIVE)
      │   (ACTIVE)
      │
      └──→ COMBINER      Inherits runtime status (ACTIVE)
          (ACTIVE)       Has own operational status (has mappings?)
             │
             └──→ EDGE   Aggregates upstream (ACTIVE if any active)
                 (ACTIVE)
```

---

## Visual Rendering

### Colors (Runtime Status)

**File:** [`src/modules/Workspace/utils/status-utils.ts`](../../src/modules/Workspace/utils/status-utils.ts)

```typescript
getThemeForRuntimeStatus(status: RuntimeStatus, theme): string {
  ACTIVE   → theme.colors.status.connected[500]   // Green
  ERROR    → theme.colors.status.error[500]       // Red
  INACTIVE → theme.colors.status.disconnected[500] // Yellow/Gray
}
```

**Applied to:**

- Node borders
- Edge stroke color
- Edge marker (arrow) color

### Animations (Operational Status)

**File:** [`src/modules/Workspace/utils/status-utils.ts`](../../src/modules/Workspace/utils/status-utils.ts)

```typescript
getEdgeStatusFromModel(statusModel: NodeStatusModel): EdgeProps {
  edge.animated = statusModel.operational === ACTIVE
               && statusModel.runtime === ACTIVE
  // Animated only if BOTH operational AND runtime are ACTIVE
}
```

**Result:**

- ACTIVE operational + ACTIVE runtime → Animated edge (flowing dots)
- INACTIVE operational → No animation (not configured)
- ERROR → No animation (error state)

---

## Implementation Reference

### Core Files

#### Type Definitions

- [`src/modules/Workspace/types.ts`](../../src/modules/Workspace/types.ts) - All workspace types
- [`src/modules/Workspace/types/status.types.ts`](../../src/modules/Workspace/types/status.types.ts) - Status model types

#### Status Utilities

- [`src/modules/Workspace/utils/status-mapping.utils.ts`](../../src/modules/Workspace/utils/status-mapping.utils.ts) - API status → Unified status
- [`src/modules/Workspace/utils/status-propagation.utils.ts`](../../src/modules/Workspace/utils/status-propagation.utils.ts) - Passive node status computation
- [`src/modules/Workspace/utils/operational-status.utils.ts`](../../src/modules/Workspace/utils/operational-status.utils.ts) - Operational status detection
- [`src/modules/Workspace/utils/edge-operational-status.utils.ts`](../../src/modules/Workspace/utils/edge-operational-status.utils.ts) - Pulse-specific edge status
- [`src/modules/Workspace/utils/status-utils.ts`](../../src/modules/Workspace/utils/status-utils.ts) - Edge rendering with dual status

#### Node Components

- [`src/modules/Workspace/components/nodes/NodeAdapter.tsx`](../../src/modules/Workspace/components/nodes/NodeAdapter.tsx)
- [`src/modules/Workspace/components/nodes/NodeBridge.tsx`](../../src/modules/Workspace/components/nodes/NodeBridge.tsx)
- [`src/modules/Workspace/components/nodes/NodePulse.tsx`](../../src/modules/Workspace/components/nodes/NodePulse.tsx)
- [`src/modules/Workspace/components/nodes/NodeCombiner.tsx`](../../src/modules/Workspace/components/nodes/NodeCombiner.tsx)
- [`src/modules/Workspace/components/nodes/NodeDevice.tsx`](../../src/modules/Workspace/components/nodes/NodeDevice.tsx)
- [`src/modules/Workspace/components/nodes/NodeHost.tsx`](../../src/modules/Workspace/components/nodes/NodeHost.tsx)
- [`src/modules/Workspace/components/nodes/NodeEdge.tsx`](../../src/modules/Workspace/components/nodes/NodeEdge.tsx)
- [`src/modules/Workspace/components/nodes/NodeGroup.tsx`](../../src/modules/Workspace/components/nodes/NodeGroup.tsx)
- [`src/modules/Workspace/components/nodes/NodeListener.tsx`](../../src/modules/Workspace/components/nodes/NodeListener.tsx)
- [`src/modules/Workspace/components/nodes/NodeAssets.tsx`](../../src/modules/Workspace/components/nodes/NodeAssets.tsx)

#### API Types

- [`src/api/__generated__/models/Combiner.ts`](../../src/api/__generated__/models/Combiner.ts)
- [`src/api/__generated__/models/DataCombining.ts`](../../src/api/__generated__/models/DataCombining.ts)
- [`src/api/__generated__/models/DataCombiningList.ts`](../../src/api/__generated__/models/DataCombiningList.ts)

#### Test Files

- [`src/modules/Workspace/utils/edge-operational-status.utils.spec.ts`](../../src/modules/Workspace/utils/edge-operational-status.utils.spec.ts) - Pulse edge status tests
- [`src/modules/Workspace/utils/status-propagation.integration.spec.ts`](../../src/modules/Workspace/utils/status-propagation.integration.spec.ts) - Integration tests
- [`src/modules/Workspace/utils/group-status.spec.ts`](../../src/modules/Workspace/utils/group-status.spec.ts) - Group aggregation tests

---

## Quick Reference

### Status Computation Checklist

When adding/modifying node status:

1. **Identify node type**: Active or Passive?
2. **For Active nodes**: Map API status to unified RuntimeStatus
3. **For Passive nodes**: Compute from upstream connections
4. **Operational status**: Check configuration (mappings, topics, etc.)
5. **Store in node data**: Use `statusModel` property
6. **Update on change**: Use `useEffect` to sync with React Flow
7. **Test edge cases**: No connections, mixed statuses, errors

### Edge Rendering Checklist

When adding/modifying edge rendering:

1. **Get source node**: Check its `statusModel`
2. **Get target node**: Check its `statusModel` if needed
3. **Compute runtime**: Color based on source's runtime status
4. **Compute operational**: Animation based on configuration status
5. **Special cases**: Handle per-edge status (e.g., Pulse → Asset Mapper)
6. **Apply theme**: Use `getThemeForRuntimeStatus()`
7. **Set animation**: Use `getEdgeStatusFromModel()`

---

## Future Evolution (V2)

### Planned Node Types

- `NORTHBOUND_MAPPER_NODE` - Explicit north-bound data transformation
- `SOUTHBOUND_MAPPER_NODE` - Explicit south-bound data transformation

### Planned Connections

```
ADAPTER → NORTHBOUND_MAPPER → EDGE
ADAPTER → SOUTHBOUND_MAPPER → EDGE
```

### Design Considerations

- North/south mappers will make data flow more explicit
- Current adapter mappings may migrate to these explicit mapper nodes
- Operational status will be clearer (mapper has config vs adapter has config)
- Per-edge status model is already prepared for this evolution

---

## Glossary

| Term                     | Definition                                                              |
| ------------------------ | ----------------------------------------------------------------------- |
| **Active Node**          | Node with own runtime status (Adapter, Bridge, Pulse)                   |
| **Passive Node**         | Node deriving status from upstream (Device, Host, Combiner, Edge)       |
| **Runtime Status**       | Is the node running and connected? (ACTIVE/INACTIVE/ERROR)              |
| **Operational Status**   | Is the node configured for data transformation? (ACTIVE/INACTIVE/ERROR) |
| **Status Propagation**   | Computing passive node status from upstream active nodes                |
| **Per-Edge Status**      | Edge status computed independently for each connection                  |
| **Asset Mapper**         | Combiner with Pulse Agent as source, using asset IDs                    |
| **Unified Status Model** | Common status structure for all node types                              |
| **Dual-Status Model**    | Runtime + Operational status for complete picture                       |

---

**End of Document**

For questions or updates, see task documentation in `.tasks/32118-workspace-status/`
