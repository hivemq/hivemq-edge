# Quick Start Development Guide

**Task:** 25337-workspace-auto-layout  
**For:** Developers starting implementation  
**Last Updated:** October 27, 2025

---

## Getting Started

### Prerequisites

1. **Read these documents first:**

   - `TASK_BRIEF.md` - Understand the goal
   - `WORKSPACE_TOPOLOGY.md` - Understand the graph structure
   - `ARCHITECTURE.md` - Understand the technical design

2. **Environment setup:**

   - Node.js 18+
   - pnpm installed
   - HiveMQ Edge Frontend repository cloned
   - Development server running: `pnpm dev`

3. **Feature flag enabled:**
   - Create/update `.env.local`:
     ```bash
     VITE_FEATURE_AUTO_LAYOUT=true
     ```

---

## Development Workflow

### Phase 1: Foundation (Start Here!)

#### Step 1: Install Dependencies (5 min)

```bash
# From project root
pnpm add @dagrejs/dagre webcola
pnpm add -D @types/dagre
```

**Verify installation:**

```bash
# Check package.json
grep -A 2 "dagrejs/dagre" package.json
grep -A 2 "webcola" package.json
```

---

#### Step 2: Create Feature Flag System (10 min)

**File:** `src/config/features.ts`

```typescript
/**
 * Feature flags for experimental features
 */
export const FEATURES = {
  /**
   * Enable workspace auto-layout algorithms
   * @experimental Phase: Beta testing
   */
  WORKSPACE_AUTO_LAYOUT: import.meta.env.VITE_FEATURE_AUTO_LAYOUT === 'true',
} as const

export type FeatureFlags = typeof FEATURES

/**
 * Check if a feature is enabled
 */
export const isFeatureEnabled = (feature: keyof FeatureFlags): boolean => {
  return FEATURES[feature] ?? false
}
```

**Test it:**

```typescript
// In any component
import { FEATURES } from '@/config/features'

console.log('Auto-layout enabled:', FEATURES.WORKSPACE_AUTO_LAYOUT)
```

---

#### Step 3: Define Layout Types (20 min)

**File:** `src/modules/Workspace/types/layout.ts`

Create this file with all type definitions from `ARCHITECTURE.md` section "Data Models".

Key types to include:

- `LayoutType` enum
- `LayoutFeature` enum
- `LayoutOptions` interface and variants (Dagre, Cola)
- `LayoutConstraints` interface
- `LayoutResult` interface
- `LayoutAlgorithm` interface
- `LayoutPreset` interface

**Tip:** Copy directly from ARCHITECTURE.md, don't reinvent!

---

#### Step 4: Update Workspace Types (15 min)

**File:** `src/modules/Workspace/types.ts`

Add import and update existing `EdgeFlowLayout` enum:

```typescript
// Add to imports
import type { LayoutType, LayoutPreset, LayoutOptions } from './types/layout'

// Replace existing EdgeFlowLayout
export enum EdgeFlowLayout {
  HORIZONTAL = 'HORIZONTAL',
  VERTICAL = 'VERTICAL',
  DAGRE_TB = 'DAGRE_TB',
  DAGRE_LR = 'DAGRE_LR',
  COLA_FORCE = 'COLA_FORCE',
  COLA_CONSTRAINED = 'COLA_CONSTRAINED',
  MANUAL = 'MANUAL',
}

// Update EdgeFlowGrouping interface
export interface EdgeFlowGrouping {
  layout: EdgeFlowLayout | LayoutType
  keys: string[]
  showGroups: boolean
}
```

---

#### Step 5: Extend Workspace Store (30 min)

**File:** `src/modules/Workspace/hooks/useWorkspaceStore.ts`

Add to state interface:

```typescript
import type { LayoutType, LayoutPreset, LayoutOptions } from '../types/layout'

// Add to WorkspaceState interface
export interface WorkspaceState {
  // ...existing properties

  // Layout configuration
  layoutConfig: {
    currentAlgorithm: LayoutType
    options: LayoutOptions
    presets: LayoutPreset[]
  }
  isAutoLayoutEnabled: boolean
  layoutHistory: LayoutHistoryEntry[]
}

// Add to WorkspaceAction interface
export interface WorkspaceAction {
  // ...existing methods

  // Layout actions
  setLayoutAlgorithm: (algorithm: LayoutType) => void
  setLayoutOptions: (options: Partial<LayoutOptions>) => void
  toggleAutoLayout: () => void
  saveLayoutPreset: (preset: LayoutPreset) => void
  loadLayoutPreset: (presetId: string) => void
  pushLayoutHistory: (entry: LayoutHistoryEntry) => void
}
```

Add implementation in the store:

```typescript
export const useWorkspaceStore = create<WorkspaceState & WorkspaceAction>()((set, get) => ({
  // ...existing state

  // Initial layout config
  layoutConfig: {
    currentAlgorithm: LayoutType.DAGRE_TB,
    options: {
      animate: true,
      animationDuration: 300,
      fitView: true,
    },
    presets: [],
  },
  isAutoLayoutEnabled: false,
  layoutHistory: [],

  // ...existing actions

  // Layout actions
  setLayoutAlgorithm: (algorithm) => {
    set((state) => ({
      layoutConfig: {
        ...state.layoutConfig,
        currentAlgorithm: algorithm,
      },
    }))
  },

  setLayoutOptions: (options) => {
    set((state) => ({
      layoutConfig: {
        ...state.layoutConfig,
        options: {
          ...state.layoutConfig.options,
          ...options,
        },
      },
    }))
  },

  toggleAutoLayout: () => {
    set((state) => ({
      isAutoLayoutEnabled: !state.isAutoLayoutEnabled,
    }))
  },

  saveLayoutPreset: (preset) => {
    set((state) => ({
      layoutConfig: {
        ...state.layoutConfig,
        presets: [...state.layoutConfig.presets, preset],
      },
    }))
  },

  loadLayoutPreset: (presetId) => {
    const state = get()
    const preset = state.layoutConfig.presets.find((p) => p.id === presetId)
    if (preset) {
      set({
        layoutConfig: {
          ...state.layoutConfig,
          currentAlgorithm: preset.algorithm,
          options: preset.options,
        },
      })
    }
  },

  pushLayoutHistory: (entry) => {
    set((state) => {
      const newHistory = [...state.layoutHistory, entry]
      // Keep only last 20 entries
      if (newHistory.length > 20) {
        newHistory.shift()
      }
      return { layoutHistory: newHistory }
    })
  },
}))
```

**Add persistence (localStorage):**

```typescript
import { persist } from 'zustand/middleware'

export const useWorkspaceStore = create<WorkspaceState & WorkspaceAction>()(
  persist(
    (set, get) => ({
      // ... store implementation
    }),
    {
      name: 'workspace-store',
      partialize: (state) => ({
        layoutConfig: state.layoutConfig,
        isAutoLayoutEnabled: state.isAutoLayoutEnabled,
      }),
    }
  )
)
```

---

### Phase 2: Dagre Implementation

#### Step 6: Create Constraint Utilities (20 min)

**File:** `src/modules/Workspace/utils/layout/constraint-utils.ts`

```typescript
import type { Node, Edge } from '@xyflow/react'
import type { LayoutConstraints, GluedNodeInfo } from '../../types/layout'
import { NodeTypes } from '../../types'
import { gluedNodeDefinition } from '../nodes-utils'

/**
 * Extract layout constraints from current graph structure
 */
export const extractLayoutConstraints = (nodes: Node[], edges: Edge[]): LayoutConstraints => {
  const gluedNodes = new Map<string, GluedNodeInfo>()
  const fixedNodes = new Set<string>()
  const groupNodes = new Map<string, string[]>()

  // Find glued nodes (e.g., listeners glued to edge)
  nodes.forEach((node) => {
    if (node.type && Object.keys(gluedNodeDefinition).includes(node.type)) {
      const [parentType, offset, handle] = gluedNodeDefinition[node.type as NodeTypes]
      const parent = nodes.find((n) => n.type === parentType)

      if (parent) {
        gluedNodes.set(node.id, {
          parentId: parent.id,
          offset: { x: offset, y: offset },
          handle: handle as 'source' | 'target',
        })
      }
    }
  })

  // Find group nodes and their children
  nodes.forEach((node) => {
    if (node.type === NodeTypes.CLUSTER_NODE && node.data?.childrenNodeIds) {
      groupNodes.set(node.id, node.data.childrenNodeIds)
    }
  })

  return {
    gluedNodes,
    fixedNodes,
    groupNodes,
  }
}

/**
 * Check if a node is constrained (cannot be freely positioned)
 */
export const isNodeConstrained = (nodeId: string, constraints: LayoutConstraints): boolean => {
  return constraints.gluedNodes.has(nodeId) || constraints.fixedNodes.has(nodeId)
}
```

---

#### Step 7: Create Dagre Layout Algorithm (45 min)

**File:** `src/modules/Workspace/utils/layout/dagre-layout.ts`

```typescript
import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@xyflow/react'
import type {
  LayoutAlgorithm,
  LayoutType,
  LayoutOptions,
  LayoutResult,
  LayoutConstraints,
  DagreOptions,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'
import { isNodeConstrained } from './constraint-utils'

export class DagreLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string
  readonly type: LayoutType
  readonly description: string
  readonly defaultOptions: DagreOptions

  constructor(direction: 'TB' | 'LR') {
    this.type = direction === 'TB' ? ('DAGRE_TB' as LayoutType) : ('DAGRE_LR' as LayoutType)
    this.name = direction === 'TB' ? 'Vertical Tree' : 'Horizontal Tree'
    this.description =
      direction === 'TB' ? 'Top-to-bottom hierarchical tree layout' : 'Left-to-right hierarchical tree layout'
    this.defaultOptions = {
      rankdir: direction,
      ranksep: direction === 'TB' ? 150 : 200,
      nodesep: 80,
      edgesep: 20,
      ranker: 'network-simplex',
      animate: true,
      animationDuration: 300,
      fitView: true,
    }
  }

  async apply(
    nodes: Node[],
    edges: Edge[],
    options: LayoutOptions,
    constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()
    const dagreOptions = { ...this.defaultOptions, ...options } as DagreOptions

    try {
      // 1. Create dagre graph
      const g = new dagre.graphlib.Graph()
      g.setDefaultEdgeLabel(() => ({}))
      g.setGraph({
        rankdir: dagreOptions.rankdir,
        ranksep: dagreOptions.ranksep,
        nodesep: dagreOptions.nodesep,
        edgesep: dagreOptions.edgesep,
        ranker: dagreOptions.ranker,
        align: dagreOptions.align,
      })

      // 2. Identify constrained nodes
      const constrainedNodeIds = new Set<string>()
      if (constraints) {
        constraints.gluedNodes.forEach((_, id) => constrainedNodeIds.add(id))
        constraints.fixedNodes.forEach((id) => constrainedNodeIds.add(id))
      }

      // 3. Add nodes to dagre (exclude constrained nodes)
      const layoutableNodes = nodes.filter((node) => !constrainedNodeIds.has(node.id))
      layoutableNodes.forEach((node) => {
        const width = node.width || 172
        const height = node.height || 36
        g.setNode(node.id, { width, height })
      })

      // 4. Add edges (exclude edges to/from constrained nodes for now)
      edges.forEach((edge) => {
        if (!constrainedNodeIds.has(edge.source) && !constrainedNodeIds.has(edge.target)) {
          g.setEdge(edge.source, edge.target)
        }
      })

      // 5. Run dagre layout
      dagre.layout(g)

      // 6. Extract positions and transform
      const isHorizontal = dagreOptions.rankdir === 'LR' || dagreOptions.rankdir === 'RL'
      const layoutedNodes = nodes.map((node) => {
        // Handle glued/constrained nodes
        if (constraints && constraints.gluedNodes.has(node.id)) {
          return this.positionGluedNode(node, nodes, constraints)
        }

        // Get position from dagre
        const nodeWithPosition = g.node(node.id)
        if (!nodeWithPosition) return node // Node was excluded from layout

        const width = node.width || 172
        const height = node.height || 36

        // Dagre positions are center-based, React Flow uses top-left
        return {
          ...node,
          position: {
            x: nodeWithPosition.x - width / 2,
            y: nodeWithPosition.y - height / 2,
          },
          // Set handle positions based on layout direction
          targetPosition: isHorizontal ? ('left' as const) : ('top' as const),
          sourcePosition: isHorizontal ? ('right' as const) : ('bottom' as const),
        }
      })

      const duration = performance.now() - startTime

      return {
        nodes: layoutedNodes,
        duration,
        success: true,
        metadata: {
          algorithm: this.type,
          nodeCount: layoutableNodes.length,
          edgeCount: edges.length,
        },
      }
    } catch (error) {
      const duration = performance.now() - startTime
      return {
        nodes,
        duration,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error during layout',
      }
    }
  }

  private positionGluedNode(node: Node, allNodes: Node[], constraints: LayoutConstraints): Node {
    const gluedInfo = constraints.gluedNodes.get(node.id)
    if (!gluedInfo) return node

    const parent = allNodes.find((n) => n.id === gluedInfo.parentId)
    if (!parent || !parent.position) return node

    return {
      ...node,
      position: {
        x: parent.position.x + gluedInfo.offset.x,
        y: parent.position.y + gluedInfo.offset.y,
      },
    }
  }

  supports(feature: LayoutFeature): boolean {
    const supportedFeatures: LayoutFeature[] = [
      'HIERARCHICAL' as LayoutFeature,
      'DIRECTIONAL' as LayoutFeature,
      'CONSTRAINED' as LayoutFeature,
    ]
    return supportedFeatures.includes(feature)
  }

  validateOptions(options: LayoutOptions): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []
    const dagreOpts = options as Partial<DagreOptions>

    if (dagreOpts.ranksep && dagreOpts.ranksep < 50) {
      warnings.push('ranksep < 50 may cause overlapping nodes')
    }
    if (dagreOpts.nodesep && dagreOpts.nodesep < 20) {
      warnings.push('nodesep < 20 may cause overlapping nodes')
    }

    return {
      valid: errors.length === 0,
      errors: errors.length > 0 ? errors : undefined,
      warnings: warnings.length > 0 ? warnings : undefined,
    }
  }
}
```

---

#### Step 8: Create Layout Registry (15 min)

**File:** `src/modules/Workspace/utils/layout/layout-registry.ts`

```typescript
import type { LayoutAlgorithm, LayoutType, LayoutFeature } from '../../types/layout'
import { DagreLayoutAlgorithm } from './dagre-layout'

/**
 * Registry for layout algorithms
 */
class LayoutRegistry {
  private algorithms = new Map<LayoutType, LayoutAlgorithm>()

  constructor() {
    this.registerDefaults()
  }

  private registerDefaults() {
    // Register dagre algorithms
    this.register(new DagreLayoutAlgorithm('TB'))
    this.register(new DagreLayoutAlgorithm('LR'))

    // WebCola algorithms will be added in Phase 3
  }

  register(algorithm: LayoutAlgorithm): void {
    this.algorithms.set(algorithm.type, algorithm)
  }

  unregister(type: LayoutType): void {
    this.algorithms.delete(type)
  }

  get(type: LayoutType): LayoutAlgorithm | undefined {
    return this.algorithms.get(type)
  }

  getAll(): LayoutAlgorithm[] {
    return Array.from(this.algorithms.values())
  }

  getByFeature(feature: LayoutFeature): LayoutAlgorithm[] {
    return this.getAll().filter((algo) => algo.supports(feature))
  }

  has(type: LayoutType): boolean {
    return this.algorithms.has(type)
  }
}

// Export singleton instance
export const layoutRegistry = new LayoutRegistry()
```

---

#### Step 9: Create Layout Engine Hook (30 min)

**File:** `src/modules/Workspace/hooks/useLayoutEngine.ts`

```typescript
import { useCallback, useMemo } from 'react'
import { useWorkspaceStore } from './useWorkspaceStore'
import { layoutRegistry } from '../utils/layout/layout-registry'
import { extractLayoutConstraints } from '../utils/layout/constraint-utils'
import type { LayoutType, LayoutOptions, LayoutResult } from '../types/layout'

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
    saveLayoutPreset,
    loadLayoutPreset,
    pushLayoutHistory,
  } = useWorkspaceStore()

  const currentAlgorithm = useMemo(
    () => layoutRegistry.get(layoutConfig.currentAlgorithm),
    [layoutConfig.currentAlgorithm]
  )

  const applyLayout = useCallback(async (): Promise<LayoutResult | null> => {
    if (!currentAlgorithm) {
      console.warn('No layout algorithm selected')
      return null
    }

    // Extract constraints from current node structure
    const constraints = extractLayoutConstraints(nodes, edges)

    console.log(`Applying ${currentAlgorithm.name} layout to ${nodes.length} nodes...`)

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

      // Save to history
      pushLayoutHistory({
        id: crypto.randomUUID(),
        timestamp: new Date(),
        algorithm: layoutConfig.currentAlgorithm,
        options: layoutConfig.options,
        nodePositions: new Map(result.nodes.map((n) => [n.id, n.position])),
      })

      console.log(`✓ Layout applied in ${result.duration.toFixed(2)}ms`)
    } else {
      console.error('✗ Layout failed:', result.error)
    }

    return result
  }, [currentAlgorithm, nodes, edges, layoutConfig, onNodesChange, pushLayoutHistory])

  return {
    // Core operations
    applyLayout,

    // Algorithm selection
    currentAlgorithm: layoutConfig.currentAlgorithm,
    setAlgorithm: setLayoutAlgorithm,
    availableAlgorithms: layoutRegistry.getAll(),

    // Options
    layoutOptions: layoutConfig.options,
    setLayoutOptions,

    // Auto-layout
    isAutoLayoutEnabled,
    toggleAutoLayout,

    // Presets
    presets: layoutConfig.presets,
    savePreset: saveLayoutPreset,
    loadPreset: loadLayoutPreset,
  }
}
```

---

#### Step 10: Test Basic Layout (15 min)

Create a test file to verify the implementation works:

**File:** `src/modules/Workspace/hooks/useLayoutEngine.spec.ts`

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useLayoutEngine } from './useLayoutEngine'
import { NodeTypes } from '../types'
import type { Node, Edge } from '@xyflow/react'

// Mock workspace store
vi.mock('./useWorkspaceStore', () => ({
  useWorkspaceStore: () => ({
    nodes: [
      { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
      { id: 'adapter-1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
    ] as Node[],
    edges: [{ id: 'e1', source: 'adapter-1', target: 'edge' }] as Edge[],
    onNodesChange: vi.fn(),
    layoutConfig: {
      currentAlgorithm: 'DAGRE_TB',
      options: { animate: false },
      presets: [],
    },
    setLayoutAlgorithm: vi.fn(),
    setLayoutOptions: vi.fn(),
    isAutoLayoutEnabled: false,
    toggleAutoLayout: vi.fn(),
    saveLayoutPreset: vi.fn(),
    loadLayoutPreset: vi.fn(),
    pushLayoutHistory: vi.fn(),
  }),
}))

describe('useLayoutEngine', () => {
  it('should provide layout engine interface', () => {
    const { result } = renderHook(() => useLayoutEngine())

    expect(result.current.applyLayout).toBeDefined()
    expect(result.current.currentAlgorithm).toBe('DAGRE_TB')
    expect(result.current.availableAlgorithms).toHaveLength(2) // TB and LR
  })

  it('should apply layout when triggered', async () => {
    const { result } = renderHook(() => useLayoutEngine())

    let layoutResult
    await act(async () => {
      layoutResult = await result.current.applyLayout()
    })

    expect(layoutResult).toBeDefined()
    expect(layoutResult?.success).toBe(true)
  })
})
```

Run the test:

```bash
pnpm test useLayoutEngine
```

---

### Testing Your Implementation

#### Manual Test in Browser

1. **Start dev server:**

   ```bash
   pnpm dev
   ```

2. **Open browser console**

3. **Test layout engine in console:**

   ```javascript
   // Access the workspace store
   const store = useWorkspaceStore.getState()
   console.log('Current algorithm:', store.layoutConfig.currentAlgorithm)

   // Check available algorithms
   import { layoutRegistry } from './src/modules/Workspace/utils/layout/layout-registry'
   console.log('Available:', layoutRegistry.getAll())
   ```

4. **Create a temporary "Apply Layout" button:**

   Add to `ReactFlowWrapper.tsx` temporarily:

   ```typescript
   import { useLayoutEngine } from '../hooks/useLayoutEngine'
   import { FEATURES } from '@/config/features'

   // Inside component:
   const { applyLayout } = useLayoutEngine()

   // In JSX:
   {FEATURES.WORKSPACE_AUTO_LAYOUT && (
     <Button
       position="absolute"
       top="10px"
       left="50%"
       onClick={() => applyLayout()}
       colorScheme="blue"
     >
       Apply Layout (Test)
     </Button>
   )}
   ```

5. **Click the button and watch nodes rearrange!**

---

## Next Steps

After completing Phase 1 foundation and Phase 2 dagre implementation:

1. **Phase 3:** Implement WebCola layouts
2. **Phase 4:** Build UI controls and configuration panel
3. **Phase 5:** Add dynamic/auto-layout mode
4. **Phase 6:** Write comprehensive tests

---

## Troubleshooting

### Common Issues

**Issue:** "Cannot find module '@dagrejs/dagre'"
**Fix:** Run `pnpm install` and restart dev server

**Issue:** "Feature flag not working"
**Fix:** Check `.env.local` exists and has `VITE_FEATURE_AUTO_LAYOUT=true`, restart dev server

**Issue:** "Nodes not moving after layout"
**Fix:** Check `onNodesChange` is being called with correct position changes. Add `console.log` in `applyLayout`.

**Issue:** "Glued nodes moving incorrectly"
**Fix:** Verify `gluedNodeDefinition` in `nodes-utils.ts` and `positionGluedNode` logic.

---

## Code Style & Conventions

1. **TypeScript:** Strict mode, all types defined
2. **Naming:** camelCase for functions, PascalCase for classes/components
3. **Comments:** JSDoc for public APIs
4. **Formatting:** Prettier (run `pnpm lint:prettier:write`)
5. **Linting:** ESLint (run `pnpm lint:eslint:fix`)
6. **Tests:** Vitest for unit tests, Cypress for E2E

---

## Useful Commands

```bash
# Development
pnpm dev                    # Start dev server
pnpm build                  # Build for production
pnpm preview                # Preview production build

# Testing
pnpm test                   # Run unit tests
pnpm test:coverage          # Run with coverage
pnpm cypress:open:component # Open Cypress component testing

# Linting
pnpm lint:all               # Run all linters
pnpm lint:eslint:fix        # Fix ESLint issues
pnpm lint:prettier:write    # Format with Prettier

# Type checking
pnpm build:tsc              # Type check only
```

---

## Resources

- **React Flow Docs:** https://reactflow.dev/
- **Dagre Wiki:** https://github.com/dagrejs/dagre/wiki
- **WebCola Examples:** https://ialab.it.monash.edu/webcola/examples.html
- **Zustand Docs:** https://zustand-demo.pmnd.rs/
- **Chakra UI:** https://chakra-ui.com/

---

## Getting Help

1. **Check existing code:**

   - Look at similar features (e.g., filter toolbar implementation)
   - Review test files for examples

2. **Documentation:**

   - Read ARCHITECTURE.md for detailed design
   - Check WORKSPACE_TOPOLOGY.md for graph structure

3. **Ask questions:**
   - Add questions to CONVERSATION_SUBTASK_N.md
   - Mark decisions in documentation

---

**Ready to Start?** Begin with Phase 1, Step 1! 🚀
