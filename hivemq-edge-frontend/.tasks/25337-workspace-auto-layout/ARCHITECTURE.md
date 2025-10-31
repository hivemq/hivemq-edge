# Workspace Auto-Layout: Architecture & Implementation Guide

**Task:** 25337-workspace-auto-layout  
**Document Type:** Technical Architecture  
**Version:** 1.0  
**Date:** October 27, 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Current State Analysis](#current-state-analysis)
3. [Proposed Architecture](#proposed-architecture)
4. [Layout Algorithm Specifications](#layout-algorithm-specifications)
5. [Implementation Strategy](#implementation-strategy)
6. [Data Models](#data-models)
7. [API Design](#api-design)
8. [Performance Considerations](#performance-considerations)
9. [Testing Strategy](#testing-strategy)
10. [Migration Path](#migration-path)

---

## Overview

### Problem Statement

The HiveMQ Edge workspace currently uses a rudimentary static layout system that:

- Returns nodes unchanged (no actual layout algorithm)
- Offers only one layout option (HORIZONTAL)
- Provides no user control over layout preferences
- Cannot adapt to graph topology changes

### Solution

Implement a flexible, extensible layout system with:

- Multiple professional layout algorithms (dagre, WebCola)
- User-configurable layout options and presets
- Static (manual trigger) and dynamic (automatic) layout modes
- Persistent user preferences and saved layouts
- Feature-flagged experimental features

### Success Metrics

- ✅ Multiple working layout algorithms (≥3)
- ✅ User satisfaction with layout quality
- ✅ Performance <500ms for graphs with 200 nodes
- ✅ 80%+ test coverage
- ✅ Zero breaking changes to existing workspace functionality

---

## Current State Analysis

### Existing Layout System

**File:** `src/modules/Workspace/utils/layout-utils.ts`

```typescript
export const applyLayout = (nodes: Node[], groupOption: EdgeFlowGrouping): Node[] => {
  // TODO Implements better layouts for the workspace
  switch (groupOption.layout) {
    case EdgeFlowLayout.HORIZONTAL:
    default:
      return nodes
  }
}
```

**Issues:**

- No-op implementation
- No actual positioning logic
- Single layout type
- No configuration options

### Workspace Component Structure

```
src/modules/Workspace/
├── components/
│   ├── ReactFlowWrapper.tsx          # Main canvas
│   ├── controls/
│   │   ├── CanvasToolbar.tsx         # Toolbar (will add layout controls)
│   │   ├── CanvasControls.tsx        # Zoom/pan controls
│   │   ├── SelectionListener.tsx     # Selection state
│   │   └── StatusListener.tsx        # Status monitoring
│   ├── nodes/                        # Node components
│   │   ├── NodeAdapter.tsx
│   │   ├── NodeBridge.tsx
│   │   ├── NodeEdge.tsx
│   │   └── ...
│   └── edges/                        # Edge components
│       ├── MonitoringEdge.tsx
│       └── DynamicEdge.tsx
├── hooks/
│   ├── useWorkspaceStore.ts          # Zustand state management
│   ├── useGetFlowElements.ts         # Fetch graph data
│   └── useEdgeFlowContext.ts         # Flow context
├── utils/
│   ├── layout-utils.ts               # Current (empty) layout code
│   └── nodes-utils.ts                # Node positioning helpers
└── types.ts                          # Type definitions
```

### Node Topology Characteristics

**Node Types & Relationships:**

1. **Edge Node (Hub):**

   - Central node, typically singular
   - Connected to adapters, bridges, listeners
   - Natural root for hierarchical layouts

2. **Adapter Nodes:**

   - Connect to Edge node (parent)
   - Can have Device nodes as children
   - Form hierarchical trees

3. **Bridge Nodes:**

   - Connect Edge to remote MQTT brokers
   - Peer-level connections

4. **Listener Nodes:**

   - **Glued to Edge node** (position-locked)
   - Must stay at fixed offset from Edge
   - Special constraint handling required

5. **Device Nodes:**

   - Children of Adapter nodes
   - Leaf nodes in hierarchy

6. **Group Nodes (Clusters):**

   - Container nodes with child nodes
   - Require nested layout (children inside bounds)

7. **Host, Combiner, Pulse Nodes:**
   - Various specialized nodes
   - Standard layout rules apply

**Connection Patterns:**

- Hub-and-spoke (Edge ↔ Adapters/Bridges)
- Hierarchical (Adapter → Devices)
- Clusters (Group → Children)

---

## Proposed Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interface Layer                     │
│  ┌───────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ CanvasToolbar │  │ Layout Panel │  │  Apply Layout   │  │
│  │  (Controls)   │  │ (Config UI)  │  │    (Button)     │  │
│  └───────┬───────┘  └──────┬───────┘  └────────┬────────┘  │
└──────────┼──────────────────┼───────────────────┼───────────┘
           │                  │                   │
           ▼                  ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                    Layout Engine Layer                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │             useLayoutEngine Hook                    │   │
│  │  - Select algorithm                                 │   │
│  │  - Apply configuration                              │   │
│  │  - Handle constraints                               │   │
│  │  - Trigger layout calculation                       │   │
│  │  - Animate transitions                              │   │
│  └────────────────────────┬────────────────────────────┘   │
└───────────────────────────┼────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Layout Algorithm Layer                     │
│  ┌──────────────┐  ┌───────────────┐  ┌────────────────┐  │
│  │ Dagre Layout │  │ WebCola Force │  │  WebCola       │  │
│  │  (TB / LR)   │  │   Directed    │  │  Constrained   │  │
│  └──────┬───────┘  └───────┬───────┘  └────────┬───────┘  │
│         │                  │                    │          │
│         └──────────────────┴────────────────────┘          │
│                            │                                │
│                   ┌────────▼─────────┐                     │
│                   │ Layout Registry  │                     │
│                   │   (Factory)      │                     │
│                   └──────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      State Layer                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Zustand Workspace Store                    │  │
│  │  - nodes[]                                           │  │
│  │  - edges[]                                           │  │
│  │  - layoutConfig (algorithm, options, presets)       │  │
│  │  - isAutoLayoutEnabled                              │  │
│  │  - savedLayouts[]                                   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Persistence Layer                         │
│  ┌─────────────┐         ┌──────────────────────────────┐  │
│  │ LocalStorage│   or    │  Backend API (future)        │  │
│  │  (Presets)  │         │  (User preferences/layouts)  │  │
│  └─────────────┘         └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

#### 1. Layout Engine (`useLayoutEngine` hook)

**Responsibilities:**

- Orchestrate layout calculation and application
- Handle layout algorithm selection
- Apply user configuration options
- Manage layout constraints (glued nodes, groups)
- Trigger layout animations
- Handle auto-layout mode

**Interface:**

```typescript
const {
  applyLayout,
  currentAlgorithm,
  setAlgorithm,
  layoutOptions,
  setLayoutOptions,
  isAutoLayoutEnabled,
  toggleAutoLayout,
  saveCurrentLayout,
  loadLayout,
  availableAlgorithms,
} = useLayoutEngine()
```

#### 2. Layout Algorithm Interface

**Responsibilities:**

- Define common contract for all layout algorithms
- Provide metadata (name, type, supported features)
- Execute layout calculation
- Return positioned nodes

**Interface:**

```typescript
interface LayoutAlgorithm {
  name: string
  type: LayoutType
  description: string
  defaultOptions: LayoutOptions

  apply(nodes: Node[], edges: Edge[], options: LayoutOptions, constraints?: LayoutConstraints): Promise<LayoutResult>

  supports(feature: LayoutFeature): boolean
  validateOptions(options: LayoutOptions): ValidationResult
}
```

#### 3. Layout Registry

**Responsibilities:**

- Register available layout algorithms
- Provide algorithm discovery
- Factory pattern for algorithm instantiation

**Interface:**

```typescript
class LayoutRegistry {
  register(algorithm: LayoutAlgorithm): void
  get(type: LayoutType): LayoutAlgorithm | undefined
  getAll(): LayoutAlgorithm[]
  getByFeature(feature: LayoutFeature): LayoutAlgorithm[]
}
```

#### 4. Layout Configuration Panel (UI)

**Responsibilities:**

- Display available layout algorithms
- Show/edit configuration options
- Manage presets
- Trigger layout application

**UI Structure:**

```
┌─────────────────────────────┐
│  Layout Configuration       │
├─────────────────────────────┤
│  Algorithm: [Dropdown    ▼] │
│    • Vertical Tree          │
│    • Horizontal Tree        │
│    • Force Directed         │
│    • Constraint-Based       │
├─────────────────────────────┤
│  Options:                   │
│    Node Spacing: [100] px   │
│    Rank Sep:     [150] px   │
│    Direction:    [TB    ▼]  │
├─────────────────────────────┤
│  Mode:                      │
│    ☐ Auto-layout on change  │
│    ☑ Animate transitions    │
├─────────────────────────────┤
│  Presets: [My Layout 1  ▼]  │
│    💾 Save Current          │
│    📂 Load Preset           │
├─────────────────────────────┤
│  [Apply Layout] [Reset]     │
└─────────────────────────────┘
```

---

## Layout Algorithm Specifications

### 1. Dagre Vertical Tree (Primary)

**Type:** `DAGRE_TB`  
**Use Case:** Default hierarchical layout, top-to-bottom  
**Best For:** Clear hierarchy visualization, hub-spoke topologies

**Configuration Options:**

```typescript
interface DagreTBOptions extends LayoutOptions {
  rankdir: 'TB' // Top to bottom
  ranksep: number // Separation between ranks (default: 150)
  nodesep: number // Separation between nodes (default: 80)
  edgesep: number // Separation between edges (default: 20)
  ranker: 'network-simplex' | 'tight-tree' | 'longest-path'
  align: 'UL' | 'UR' | 'DL' | 'DR' | undefined
}
```

**Algorithm Steps:**

1. Build dagre graph from React Flow nodes/edges
2. Set graph layout direction (TB)
3. Add nodes with dimensions
4. Add edges
5. Run dagre.layout()
6. Transform dagre positions to React Flow format
7. Adjust for node anchor point (center → top-left)

**Constraint Handling:**

- **Glued nodes:** Apply layout, then reposition glued nodes based on parent offsets
- **Group nodes:** Layout children separately, then layout groups

**Example Output:**

```
         [Edge]
           │
    ┌──────┼──────┐
    │      │      │
[Adapter] [Bridge] [Listener]
    │
┌───┴───┐
│       │
[Dev1] [Dev2]
```

---

### 2. Dagre Horizontal Tree

**Type:** `DAGRE_LR`  
**Use Case:** Wide screens, horizontal data flows  
**Best For:** Pipeline/workflow visualization

**Configuration Options:**

```typescript
interface DagreLROptions extends LayoutOptions {
  rankdir: 'LR' // Left to right
  ranksep: number // Separation between ranks (default: 200)
  nodesep: number // Separation between nodes (default: 80)
  // ... same as TB
}
```

**Example Output:**

```
            ┌──[Adapter]──[Device1]
            │             └[Device2]
[Edge]──────┤
            │  [Bridge]
            │
            └──[Listener]
```

---

### 3. WebCola Force-Directed

**Type:** `COLA_FORCE`  
**Use Case:** Organic layouts, cluster discovery  
**Best For:** Complex graphs without clear hierarchy

**Configuration Options:**

```typescript
interface ColaForceOptions extends LayoutOptions {
  linkDistance: number // Target link length (default: 150)
  symmetricDiffLinkLengths: number
  jaccardLinkLengths: number
  avoidOverlaps: boolean // Prevent node overlap (default: true)
  handleDisconnected: boolean // Handle disconnected components (default: true)
  convergenceThreshold: number // Stop threshold (default: 0.01)
  maxIterations: number // Max iterations (default: 1000)

  // Constraints
  centerNode?: string // Keep specific node centered (Edge node)
  flowConstraints?: boolean // Add flow direction constraints
}
```

**Algorithm Steps:**

1. Initialize WebCola layout engine
2. Set link distance and overlap avoidance
3. Add nodes and links
4. Apply constraints (center Edge node, avoid overlaps)
5. Run layout simulation
6. Extract final positions

**Constraint Handling:**

- **Center constraint:** Fix Edge node at canvas center
- **Alignment constraints:** Align similar node types
- **Separation constraints:** Minimum distance between node types

---

### 4. WebCola Constraint-Based

**Type:** `COLA_CONSTRAINED`  
**Use Case:** Strict hierarchical layout with alignment  
**Best For:** Layered architectures, clear data flows

**Configuration Options:**

```typescript
interface ColaConstrainedOptions extends LayoutOptions {
  flowDirection: 'x' | 'y' // Horizontal or vertical flow
  layerGap: number // Gap between layers (default: 150)
  nodeGap: number // Gap between nodes in layer (default: 80)

  // Constraints
  layerConstraints: LayerConstraint[]
  alignmentConstraints: AlignmentConstraint[]
  separationConstraints: SeparationConstraint[]
}

interface LayerConstraint {
  layer: number
  nodeIds: string[]
}

interface AlignmentConstraint {
  axis: 'x' | 'y'
  nodeIds: string[]
  offset?: number
}

interface SeparationConstraint {
  left: string
  right: string
  gap: number
}
```

**Algorithm Steps:**

1. Classify nodes into layers (by distance from Edge node)
2. Create layer constraints (each layer at specific y-coordinate)
3. Create alignment constraints (nodes in same layer aligned)
4. Create separation constraints (minimum gaps)
5. Run WebCola with constraints
6. Extract final positions

**Example Layers:**

```
Layer 0: [Edge]
Layer 1: [Adapters, Bridges, Listeners]
Layer 2: [Devices]
```

---

### 5. Manual + Saved Positions

**Type:** `MANUAL`  
**Use Case:** User-controlled positioning with save/restore  
**Best For:** Custom layouts, presentations

**Features:**

- Save current node positions as named preset
- Load saved preset
- Export/import layout files
- Snap to grid
- Alignment guides (future)

---

## Implementation Strategy

### Phase 1: Foundation (Days 1-3)

**Goals:**

- ✅ Set up dependency infrastructure
- ✅ Create type definitions
- ✅ Implement feature flag system
- ✅ Set up state management

**Tasks:**

1. **Install Dependencies**

```bash
pnpm add @dagrejs/dagre webcola
pnpm add -D @types/dagre
```

2. **Create Feature Flag**

```typescript
// src/config/features.ts
export const FEATURES = {
  WORKSPACE_AUTO_LAYOUT: import.meta.env.VITE_FEATURE_AUTO_LAYOUT === 'true',
} as const

export type FeatureFlags = typeof FEATURES
```

3. **Define Layout Types**

```typescript
// src/modules/Workspace/types/layout.ts
export enum LayoutType {
  DAGRE_TB = 'DAGRE_TB',
  DAGRE_LR = 'DAGRE_LR',
  COLA_FORCE = 'COLA_FORCE',
  COLA_CONSTRAINED = 'COLA_CONSTRAINED',
  MANUAL = 'MANUAL',
}

export interface LayoutOptions {
  animate?: boolean
  duration?: number
  fitView?: boolean
}

export interface LayoutResult {
  nodes: Node[]
  duration: number
  success: boolean
  error?: string
}

export interface LayoutConstraints {
  gluedNodes: Map<string, { parentId: string; offset: Position }>
  fixedNodes: Set<string>
  groupNodes: Map<string, string[]> // groupId -> childIds
}

export interface LayoutPreset {
  id: string
  name: string
  algorithm: LayoutType
  options: LayoutOptions
  positions?: Map<string, Position> // For MANUAL type
  createdAt: Date
}
```

4. **Extend Workspace Store**

```typescript
// src/modules/Workspace/hooks/useWorkspaceStore.ts
interface WorkspaceState {
  // ...existing
  layoutConfig: {
    currentAlgorithm: LayoutType
    options: LayoutOptions
    presets: LayoutPreset[]
  }
  isAutoLayoutEnabled: boolean
  layoutHistory: LayoutHistoryEntry[]
}

interface WorkspaceAction {
  // ...existing
  setLayoutAlgorithm: (algorithm: LayoutType) => void
  setLayoutOptions: (options: Partial<LayoutOptions>) => void
  toggleAutoLayout: () => void
  saveLayoutPreset: (preset: LayoutPreset) => void
  loadLayoutPreset: (presetId: string) => void
  applyLayout: () => Promise<void>
}
```

---

### Phase 2: Dagre Implementation (Days 4-7)

**Goals:**

- ✅ Implement dagre wrapper
- ✅ Create TB and LR layouts
- ✅ Handle constraints
- ✅ Add unit tests

**Key Files to Create:**

1. **Dagre Layout Algorithm**

```typescript
// src/modules/Workspace/utils/layout/dagre-layout.ts
import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@xyflow/react'
import type { LayoutAlgorithm, LayoutOptions, LayoutResult } from '../../types/layout'

export class DagreLayoutAlgorithm implements LayoutAlgorithm {
  name = 'Dagre Hierarchical'
  type: LayoutType

  constructor(direction: 'TB' | 'LR') {
    this.type = direction === 'TB' ? LayoutType.DAGRE_TB : LayoutType.DAGRE_LR
  }

  async apply(
    nodes: Node[],
    edges: Edge[],
    options: DagreOptions,
    constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()

    try {
      // 1. Create dagre graph
      const g = new dagre.graphlib.Graph()
      g.setDefaultEdgeLabel(() => ({}))
      g.setGraph({
        rankdir: options.rankdir,
        ranksep: options.ranksep,
        nodesep: options.nodesep,
        edgesep: options.edgesep,
      })

      // 2. Add nodes (exclude glued nodes from layout)
      const gluedNodeIds = constraints?.gluedNodes ? new Set(Array.from(constraints.gluedNodes.keys())) : new Set()

      nodes.forEach((node) => {
        if (!gluedNodeIds.has(node.id)) {
          const width = node.width || 172
          const height = node.height || 36
          g.setNode(node.id, { width, height })
        }
      })

      // 3. Add edges (exclude edges to/from glued nodes)
      edges.forEach((edge) => {
        if (!gluedNodeIds.has(edge.source) && !gluedNodeIds.has(edge.target)) {
          g.setEdge(edge.source, edge.target)
        }
      })

      // 4. Run layout
      dagre.layout(g)

      // 5. Extract positions and transform
      const isHorizontal = options.rankdir === 'LR'
      const layoutedNodes = nodes.map((node) => {
        if (gluedNodeIds.has(node.id)) {
          // Handle glued nodes separately
          return this.positionGluedNode(node, nodes, constraints!)
        }

        const nodeWithPosition = g.node(node.id)
        if (!nodeWithPosition) return node

        const width = node.width || 172
        const height = node.height || 36

        return {
          ...node,
          position: {
            x: nodeWithPosition.x - width / 2,
            y: nodeWithPosition.y - height / 2,
          },
          targetPosition: isHorizontal ? 'left' : 'top',
          sourcePosition: isHorizontal ? 'right' : 'bottom',
        }
      })

      const duration = performance.now() - startTime

      return {
        nodes: layoutedNodes,
        duration,
        success: true,
      }
    } catch (error) {
      return {
        nodes,
        duration: performance.now() - startTime,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      }
    }
  }

  private positionGluedNode(node: Node, allNodes: Node[], constraints: LayoutConstraints): Node {
    const gluedInfo = constraints.gluedNodes.get(node.id)
    if (!gluedInfo) return node

    const parent = allNodes.find((n) => n.id === gluedInfo.parentId)
    if (!parent) return node

    return {
      ...node,
      position: {
        x: parent.position.x + gluedInfo.offset.x,
        y: parent.position.y + gluedInfo.offset.y,
      },
    }
  }

  supports(feature: LayoutFeature): boolean {
    return [LayoutFeature.HIERARCHICAL, LayoutFeature.DIRECTIONAL, LayoutFeature.CONSTRAINED].includes(feature)
  }
}
```

2. **Layout Registry**

```typescript
// src/modules/Workspace/utils/layout/layout-registry.ts
import type { LayoutAlgorithm } from '../../types/layout'
import { LayoutType } from '../../types/layout'
import { DagreLayoutAlgorithm } from './dagre-layout'

class LayoutRegistry {
  private algorithms = new Map<LayoutType, LayoutAlgorithm>()

  constructor() {
    this.registerDefaults()
  }

  private registerDefaults() {
    this.register(new DagreLayoutAlgorithm('TB'))
    this.register(new DagreLayoutAlgorithm('LR'))
    // WebCola algorithms added in Phase 3
  }

  register(algorithm: LayoutAlgorithm): void {
    this.algorithms.set(algorithm.type, algorithm)
  }

  get(type: LayoutType): LayoutAlgorithm | undefined {
    return this.algorithms.get(type)
  }

  getAll(): LayoutAlgorithm[] {
    return Array.from(this.algorithms.values())
  }
}

export const layoutRegistry = new LayoutRegistry()
```

3. **Layout Engine Hook**

```typescript
// src/modules/Workspace/hooks/useLayoutEngine.ts
import { useCallback, useMemo } from 'react'
import { useWorkspaceStore } from './useWorkspaceStore'
import { layoutRegistry } from '../utils/layout/layout-registry'
import { extractLayoutConstraints } from '../utils/layout/constraint-utils'
import type { LayoutType, LayoutOptions } from '../types/layout'

export const useLayoutEngine = () => {
  const {
    nodes,
    edges,
    onNodesChange,
    layoutConfig,
    setLayoutAlgorithm,
    setLayoutOptions,
    isAutoLayoutEnabled,
    toggleAutoLayout,
  } = useWorkspaceStore()

  const currentAlgorithm = useMemo(
    () => layoutRegistry.get(layoutConfig.currentAlgorithm),
    [layoutConfig.currentAlgorithm]
  )

  const applyLayout = useCallback(async () => {
    if (!currentAlgorithm) {
      console.warn('No layout algorithm selected')
      return
    }

    // Extract constraints from current node structure
    const constraints = extractLayoutConstraints(nodes, edges)

    // Apply layout algorithm
    const result = await currentAlgorithm.apply(nodes, edges, layoutConfig.options, constraints)

    if (result.success) {
      // Update nodes with new positions
      const changes = result.nodes.map((node) => ({
        id: node.id,
        type: 'position' as const,
        position: node.position,
        positionAbsolute: node.position,
      }))

      onNodesChange(changes)

      console.log(`Layout applied in ${result.duration.toFixed(2)}ms`)
    } else {
      console.error('Layout failed:', result.error)
    }
  }, [currentAlgorithm, nodes, edges, layoutConfig, onNodesChange])

  return {
    applyLayout,
    currentAlgorithm: layoutConfig.currentAlgorithm,
    setAlgorithm: setLayoutAlgorithm,
    layoutOptions: layoutConfig.options,
    setLayoutOptions,
    isAutoLayoutEnabled,
    toggleAutoLayout,
    availableAlgorithms: layoutRegistry.getAll(),
  }
}
```

---

### Phase 3: WebCola Implementation (Days 8-12)

Similar structure to Dagre, implement:

- `ColaForceLayoutAlgorithm`
- `ColaConstrainedLayoutAlgorithm`
- Register in layout registry

---

### Phase 4: UI Implementation (Days 13-16)

Create:

- `LayoutConfigPanel.tsx` - Main configuration drawer/panel
- `LayoutSelector.tsx` - Algorithm dropdown
- `LayoutOptionsEditor.tsx` - Dynamic options form
- `LayoutPresetManager.tsx` - Save/load presets
- Update `CanvasToolbar.tsx` - Add layout button

---

## Data Models

### Full Type Definitions

```typescript
// src/modules/Workspace/types/layout.ts

import type { Node, Edge, Position } from '@xyflow/react'

// ========== Enums ==========

export enum LayoutType {
  DAGRE_TB = 'DAGRE_TB',
  DAGRE_LR = 'DAGRE_LR',
  COLA_FORCE = 'COLA_FORCE',
  COLA_CONSTRAINED = 'COLA_CONSTRAINED',
  MANUAL = 'MANUAL',
}

export enum LayoutFeature {
  HIERARCHICAL = 'HIERARCHICAL',
  DIRECTIONAL = 'DIRECTIONAL',
  FORCE_DIRECTED = 'FORCE_DIRECTED',
  CONSTRAINED = 'CONSTRAINED',
  CLUSTERING = 'CLUSTERING',
  OVERLAP_REMOVAL = 'OVERLAP_REMOVAL',
}

export enum LayoutMode {
  STATIC = 'STATIC', // Manual trigger only
  DYNAMIC = 'DYNAMIC', // Auto-layout on changes
}

// ========== Base Interfaces ==========

export interface LayoutOptions {
  animate?: boolean
  animationDuration?: number
  fitView?: boolean
  fitViewOptions?: {
    padding?: number
    includeHiddenNodes?: boolean
  }
}

export interface DagreOptions extends LayoutOptions {
  rankdir: 'TB' | 'LR' | 'BT' | 'RL'
  ranksep: number
  nodesep: number
  edgesep: number
  ranker?: 'network-simplex' | 'tight-tree' | 'longest-path'
  align?: 'UL' | 'UR' | 'DL' | 'DR'
}

export interface ColaForceOptions extends LayoutOptions {
  linkDistance: number
  avoidOverlaps: boolean
  handleDisconnected: boolean
  convergenceThreshold: number
  maxIterations: number
  centerNodeId?: string
  flowConstraints?: boolean
}

export interface ColaConstrainedOptions extends LayoutOptions {
  flowDirection: 'x' | 'y'
  layerGap: number
  nodeGap: number
  layerConstraints?: LayerConstraint[]
  alignmentConstraints?: AlignmentConstraint[]
  separationConstraints?: SeparationConstraint[]
}

// ========== Constraints ==========

export interface LayoutConstraints {
  gluedNodes: Map<string, GluedNodeInfo>
  fixedNodes: Set<string>
  groupNodes: Map<string, string[]>
}

export interface GluedNodeInfo {
  parentId: string
  offset: Position
  handle: 'source' | 'target'
}

export interface LayerConstraint {
  layer: number
  nodeIds: string[]
}

export interface AlignmentConstraint {
  axis: 'x' | 'y'
  nodeIds: string[]
  offset?: number
}

export interface SeparationConstraint {
  left: string
  right: string
  gap: number
  axis: 'x' | 'y'
}

// ========== Results ==========

export interface LayoutResult {
  nodes: Node[]
  duration: number
  success: boolean
  error?: string
  metadata?: LayoutMetadata
}

export interface LayoutMetadata {
  algorithm: LayoutType
  nodeCount: number
  edgeCount: number
  iterations?: number
  convergence?: number
}

// ========== Presets ==========

export interface LayoutPreset {
  id: string
  name: string
  description?: string
  algorithm: LayoutType
  options: LayoutOptions
  positions?: Map<string, Position>
  createdAt: Date
  updatedAt: Date
}

// ========== History ==========

export interface LayoutHistoryEntry {
  id: string
  timestamp: Date
  algorithm: LayoutType
  options: LayoutOptions
  nodePositions: Map<string, Position>
}

// ========== Algorithm Interface ==========

export interface LayoutAlgorithm {
  readonly name: string
  readonly type: LayoutType
  readonly description: string
  readonly defaultOptions: LayoutOptions

  apply(nodes: Node[], edges: Edge[], options: LayoutOptions, constraints?: LayoutConstraints): Promise<LayoutResult>

  supports(feature: LayoutFeature): boolean
  validateOptions(options: LayoutOptions): ValidationResult
}

export interface ValidationResult {
  valid: boolean
  errors?: string[]
  warnings?: string[]
}

// ========== Configuration ==========

export interface LayoutConfiguration {
  currentAlgorithm: LayoutType
  mode: LayoutMode
  options: LayoutOptions
  presets: LayoutPreset[]
  history: LayoutHistoryEntry[]
  maxHistorySize: number
}

// ========== Events ==========

export interface LayoutEvent {
  type: 'apply' | 'preset-save' | 'preset-load' | 'undo' | 'redo'
  timestamp: Date
  data: unknown
}
```

---

## API Design

### Layout Engine API

```typescript
// Public API surface of useLayoutEngine hook

interface UseLayoutEngine {
  // Core layout operations
  applyLayout: () => Promise<LayoutResult>
  applyLayoutWithAlgorithm: (type: LayoutType, options?: LayoutOptions) => Promise<LayoutResult>

  // Algorithm selection
  currentAlgorithm: LayoutType
  setAlgorithm: (type: LayoutType) => void
  availableAlgorithms: LayoutAlgorithm[]

  // Options management
  layoutOptions: LayoutOptions
  setLayoutOptions: (options: Partial<LayoutOptions>) => void
  resetOptionsToDefault: () => void

  // Mode control
  layoutMode: LayoutMode
  isAutoLayoutEnabled: boolean
  toggleAutoLayout: () => void

  // Preset management
  presets: LayoutPreset[]
  saveCurrentLayout: (name: string, description?: string) => void
  loadPreset: (presetId: string) => Promise<void>
  deletePreset: (presetId: string) => void
  exportPreset: (presetId: string) => string // JSON
  importPreset: (json: string) => void

  // History management
  canUndo: boolean
  canRedo: boolean
  undo: () => void
  redo: () => void
  clearHistory: () => void

  // Status
  isLayouting: boolean
  lastLayoutResult?: LayoutResult
}
```

### Layout Algorithm Registry API

```typescript
interface LayoutRegistry {
  register(algorithm: LayoutAlgorithm): void
  unregister(type: LayoutType): void
  get(type: LayoutType): LayoutAlgorithm | undefined
  getAll(): LayoutAlgorithm[]
  getByFeature(feature: LayoutFeature): LayoutAlgorithm[]
  has(type: LayoutType): boolean
}
```

---

## Performance Considerations

### Optimization Strategies

1. **Web Workers for Large Graphs**

```typescript
// Use web worker for graphs > 50 nodes
if (nodes.length > 50) {
  const worker = new Worker(new URL('./layout-worker.ts', import.meta.url))
  worker.postMessage({ nodes, edges, options })
  const result = await new Promise((resolve) => {
    worker.onmessage = (e) => resolve(e.data)
  })
  return result
}
```

2. **Memoization**

```typescript
// Memoize layout results based on graph structure hash
const graphHash = hashGraph(nodes, edges)
const cached = layoutCache.get(graphHash)
if (cached) return cached
```

3. **Debouncing Auto-Layout**

```typescript
// Debounce auto-layout triggers
const debouncedAutoLayout = useMemo(() => debounce(applyLayout, 500), [applyLayout])
```

4. **Progressive Rendering**

```typescript
// Update nodes in batches for smoother animation
for (const batch of chunk(layoutedNodes, 20)) {
  onNodesChange(batch)
  await delay(16) // One frame
}
```

5. **Constraint Caching**

```typescript
// Cache extracted constraints
const constraints = useMemo(() => extractLayoutConstraints(nodes, edges), [nodes, edges])
```

### Performance Targets

- **Small graphs (<50 nodes):** <100ms
- **Medium graphs (50-200 nodes):** <500ms
- **Large graphs (>200 nodes):** <2000ms (with web worker)
- **Animation frame rate:** 60 FPS (16ms per frame)

---

## Testing Strategy

### Unit Tests

**Layout Algorithms:**

```typescript
describe('DagreLayoutAlgorithm', () => {
  it('should position nodes in hierarchical tree', async () => {
    const nodes = createTestNodes()
    const edges = createTestEdges()
    const algorithm = new DagreLayoutAlgorithm('TB')

    const result = await algorithm.apply(nodes, edges, defaultOptions)

    expect(result.success).toBe(true)
    expect(result.nodes).toHaveLength(nodes.length)
    expect(result.nodes[0].position).toBeDefined()
  })

  it('should respect glued node constraints', async () => {
    const nodes = createNodesWithGluedListener()
    const constraints = extractLayoutConstraints(nodes, [])
    const algorithm = new DagreLayoutAlgorithm('TB')

    const result = await algorithm.apply(nodes, [], defaultOptions, constraints)

    const edgeNode = result.nodes.find((n) => n.id === 'edge-node')
    const listenerNode = result.nodes.find((n) => n.id === 'listener-node')

    expect(listenerNode.position).toEqual({
      x: edgeNode.position.x + 50,
      y: edgeNode.position.y + 50,
    })
  })
})
```

**Layout Engine:**

```typescript
describe('useLayoutEngine', () => {
  it('should apply layout when triggered', async () => {
    const { result } = renderHook(() => useLayoutEngine(), {
      wrapper: WorkspaceProvider,
    })

    await act(async () => {
      await result.current.applyLayout()
    })

    expect(result.current.lastLayoutResult?.success).toBe(true)
  })
})
```

### Integration Tests

**UI Integration:**

```typescript
describe('LayoutConfigPanel', () => {
  it('should change algorithm and apply layout', async () => {
    render(<LayoutConfigPanel />, { wrapper: WorkspaceProvider })

    const algorithmSelect = screen.getByLabelText('Layout Algorithm')
    await userEvent.selectOptions(algorithmSelect, 'DAGRE_LR')

    const applyButton = screen.getByRole('button', { name: 'Apply Layout' })
    await userEvent.click(applyButton)

    // Verify layout was applied
    expect(screen.getByText(/Layout applied/i)).toBeInTheDocument()
  })
})
```

### E2E Tests (Cypress)

```typescript
describe('Workspace Layout', () => {
  beforeEach(() => {
    cy.visit('/workspace')
    cy.enableFeature('WORKSPACE_AUTO_LAYOUT')
  })

  it('should apply vertical tree layout', () => {
    cy.getByTestId('layout-config-button').click()
    cy.getByTestId('layout-algorithm-select').select('DAGRE_TB')
    cy.getByTestId('apply-layout-button').click()

    // Verify nodes are positioned vertically
    cy.getByTestId('node-edge').should('have.css', 'top', '100px')
    cy.getByTestId('node-adapter-1').should('have.css', 'top', '300px')
  })

  it('should persist layout preferences', () => {
    cy.getByTestId('layout-config-button').click()
    cy.getByTestId('layout-algorithm-select').select('DAGRE_LR')
    cy.getByTestId('apply-layout-button').click()

    cy.reload()

    cy.getByTestId('layout-algorithm-select').should('have.value', 'DAGRE_LR')
  })
})
```

### Performance Tests

```typescript
describe('Layout Performance', () => {
  it('should layout 100 nodes in <500ms', async () => {
    const nodes = generateNodes(100)
    const edges = generateEdges(nodes)
    const algorithm = new DagreLayoutAlgorithm('TB')

    const startTime = performance.now()
    const result = await algorithm.apply(nodes, edges, defaultOptions)
    const duration = performance.now() - startTime

    expect(duration).toBeLessThan(500)
    expect(result.success).toBe(true)
  })
})
```

---

## Migration Path

### Phase 1: Feature Flag Rollout

1. **Week 1:** Internal testing with feature flag OFF by default
2. **Week 2:** Enable for specific users/environments
3. **Week 3:** Beta release with opt-in
4. **Week 4:** General availability with UI toggle

### Phase 2: User Communication

- **Documentation:** Layout guide, algorithm comparison
- **Tutorials:** Video walkthrough, interactive demo
- **Migration guide:** How to switch from manual to auto-layout

### Phase 3: Deprecation of Old System

- **Timeline:** 2-3 months after GA
- **Warning messages:** "Old layout system will be removed in vX.Y"
- **Automated migration:** Convert old layout configs to new format

---

## Appendix: Example Configurations

### Default Configurations

```typescript
export const DEFAULT_LAYOUT_CONFIGS: Record<LayoutType, LayoutOptions> = {
  [LayoutType.DAGRE_TB]: {
    rankdir: 'TB',
    ranksep: 150,
    nodesep: 80,
    edgesep: 20,
    ranker: 'network-simplex',
    animate: true,
    animationDuration: 300,
    fitView: true,
  },
  [LayoutType.DAGRE_LR]: {
    rankdir: 'LR',
    ranksep: 200,
    nodesep: 80,
    edgesep: 20,
    ranker: 'network-simplex',
    animate: true,
    animationDuration: 300,
    fitView: true,
  },
  [LayoutType.COLA_FORCE]: {
    linkDistance: 150,
    avoidOverlaps: true,
    handleDisconnected: true,
    convergenceThreshold: 0.01,
    maxIterations: 1000,
    animate: true,
    animationDuration: 500,
    fitView: true,
  },
  [LayoutType.COLA_CONSTRAINED]: {
    flowDirection: 'y',
    layerGap: 150,
    nodeGap: 80,
    animate: true,
    animationDuration: 400,
    fitView: true,
  },
  [LayoutType.MANUAL]: {
    animate: false,
    fitView: false,
  },
}
```

---

**Document Version:** 1.0  
**Last Updated:** October 27, 2025  
**Next Review:** After Phase 1 completion
