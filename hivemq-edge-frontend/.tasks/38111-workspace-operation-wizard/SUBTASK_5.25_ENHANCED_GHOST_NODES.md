# SUBTASK_5.25: Enhanced Ghost Node System

**Date:** November 10, 2025  
**Status:** 🔄 In Progress  
**Priority:** High

---

## Overview

Enhance the ghost node system to support multi-node previews (ADAPTER + DEVICE with connections), use deterministic positioning from the existing algorithm, and provide better visual feedback for ghost state.

---

## Requirements

### 1. Multi-Node Ghost System

**Current State:**

- Only single ADAPTER node shown as ghost
- No DEVICE node
- No connections/edges

**Required:**

- **ADAPTER node** + **DEVICE node** as ghosts
- **Edge from ADAPTER to EDGE node**
- **Edge from ADAPTER to DEVICE node**
- Both nodes and edges in "ghost" state

### 2. Deterministic Positioning

**Current State:**

- Ghost nodes at hardcoded position `{ x: 200, y: 200 }`

**Required:**

- Use existing `createAdapterNode` positioning algorithm
- Calculate based on current adapter count
- Position exactly where real nodes would appear
- Smooth transition from ghost → real (no position jump)

**Algorithm (from `nodes-utils.ts`):**

```typescript
const posX = nbAdapter % MAX_ADAPTERS // Current adapter index mod 10
const posY = Math.floor(nbAdapter / MAX_ADAPTERS) + 1 // Row number
const deltaX = Math.floor((Math.min(MAX_ADAPTERS, maxAdapter) - 1) / 2) // Center offset

// ADAPTER position
x: POS_EDGE.x + POS_NODE_INC.x * (posX - deltaX)
y: POS_EDGE.y - POS_NODE_INC.y * posY * 1.5

// DEVICE position (glued)
x: ADAPTER.x
y: ADAPTER.y + GLUE_SEPARATOR // +200px below
```

### 3. Enhanced Visual State

**Current State:**

- `opacity: 0.6`
- `border: '2px dashed #4299E1'`
- `backgroundColor: '#EBF8FF'`

**Required:**

- **More visible selection state**
- **Animated/glowing effect** to draw attention
- **Different visual treatment** from regular nodes

**Design Options:**

#### Option A: Glowing Box Shadow (Recommended)

```typescript
style: {
  opacity: 0.7,  // Slightly more opaque
  border: '3px dashed #4299E1',
  backgroundColor: '#EBF8FF',
  boxShadow: '0 0 0 4px rgba(66, 153, 225, 0.4), 0 0 20px rgba(66, 153, 225, 0.6)',
  // Creates glowing blue halo effect
}
```

#### Option B: Pulsing Animation

```typescript
// Add CSS animation
@keyframes ghostPulse {
  0%, 100% {
    boxShadow: 0 0 0 0 rgba(66, 153, 225, 0.7);
  }
  50% {
    boxShadow: 0 0 0 10px rgba(66, 153, 225, 0);
  }
}

style: {
  opacity: 0.7,
  border: '3px dashed #4299E1',
  backgroundColor: '#EBF8FF',
  animation: 'ghostPulse 2s infinite',
}
```

#### Option C: Gradient Border (Modern)

```typescript
style: {
  opacity: 0.75,
  border: '3px solid transparent',
  backgroundImage: 'linear-gradient(white, white), linear-gradient(135deg, #4299E1, #63B3ED, #4299E1)',
  backgroundOrigin: 'border-box',
  backgroundClip: 'padding-box, border-box',
}
```

---

## Implementation Plan

### Phase 1: Enhanced Ghost Factory

**File:** `ghostNodeFactory.ts`

**Changes:**

1. Add `createGhostAdapterGroup` function

   - Returns: `{ adapterNode, deviceNode, edgeToEdge, edgeToDevice }`
   - Uses positioning algorithm from `nodes-utils.ts`

2. Add positioning calculation helper

   ```typescript
   export const calculateGhostAdapterPosition = (
     nbAdapters: number,  // Current adapter count
     edgeNode: Node        // Reference to EDGE node
   ): { adapterPos: XYPosition; devicePos: XYPosition }
   ```

3. Update visual styles with enhanced feedback

**New Type:**

```typescript
export interface GhostNodeGroup {
  nodes: GhostNode[]
  edges: Edge[]
}
```

### Phase 2: Enhanced Ghost Renderer

**File:** `GhostNodeRenderer.tsx`

**Changes:**

1. Import adapter count from API

   ```typescript
   const { data: adapters } = useListProtocolAdapters()
   const nbAdapters = adapters?.length || 0
   ```

2. Get EDGE node position

   ```typescript
   const edgeNode = getNodes().find((n) => n.id === IdStubs.EDGE_NODE)
   ```

3. Create multi-node ghost group

   ```typescript
   const ghostGroup = createGhostAdapterGroup('wizard-preview', nbAdapters, edgeNode)

   addGhostNodes(ghostGroup.nodes)
   // Also add edges to React Flow
   ```

4. Handle both nodes and edges

### Phase 3: Visual Enhancement

**Approach:** Add enhanced styles to ghost factory

**Implementation:**

```typescript
export const GHOST_STYLE_ENHANCED = {
  opacity: 0.75,
  border: '3px dashed #4299E1',
  backgroundColor: '#EBF8FF',
  boxShadow: '0 0 0 4px rgba(66, 153, 225, 0.4), 0 0 20px rgba(66, 153, 225, 0.6)',
  pointerEvents: 'none' as const,
  transition: 'all 0.3s ease',
}

export const GHOST_EDGE_STYLE = {
  stroke: '#4299E1',
  strokeWidth: 2,
  strokeDasharray: '5,5',
  opacity: 0.6,
  animated: true, // Animated dashed line
}
```

---

## Technical Details

### Positioning Algorithm

**Based on existing `createAdapterNode` logic:**

```typescript
const POS_EDGE: XYPosition = { x: 300, y: 200 } // EDGE node position
const POS_NODE_INC: XYPosition = { x: 325, y: 400 } // Spacing between adapters
const MAX_ADAPTERS = 10 // Max adapters per row
const GLUE_SEPARATOR = 200 // Distance between ADAPTER and DEVICE

export const calculateGhostAdapterPosition = (
  nbAdapters: number,
  edgeNodePos: XYPosition
): { adapterPos: XYPosition; devicePos: XYPosition } => {
  const posX = nbAdapters % MAX_ADAPTERS
  const posY = Math.floor(nbAdapters / MAX_ADAPTERS) + 1
  const deltaX = Math.floor((Math.min(MAX_ADAPTERS, nbAdapters + 1) - 1) / 2)

  const adapterPos = {
    x: edgeNodePos.x + POS_NODE_INC.x * (posX - deltaX),
    y: edgeNodePos.y - POS_NODE_INC.y * posY * 1.5,
  }

  const devicePos = {
    x: adapterPos.x,
    y: adapterPos.y + GLUE_SEPARATOR,
  }

  return { adapterPos, devicePos }
}
```

### Multi-Node Ghost Creation

```typescript
export const createGhostAdapterGroup = (
  id: string,
  nbAdapters: number,
  edgeNode: Node,
  label: string = 'New Adapter'
): GhostNodeGroup => {
  const { adapterPos, devicePos } = calculateGhostAdapterPosition(nbAdapters, edgeNode.position)

  // Create ADAPTER ghost node
  const adapterNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-adapter-${id}`,
    type: 'ADAPTER_NODE',
    position: adapterPos,
    sourcePosition: Position.Bottom,
    data: {
      isGhost: true,
      label,
      status: { connection: 'STATELESS', runtime: 'STOPPED' },
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create DEVICE ghost node
  const deviceNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-device-${id}`,
    type: 'DEVICE_NODE',
    position: devicePos,
    targetPosition: Position.Top,
    data: {
      isGhost: true,
      label: `${label} Device`,
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create edges
  const edgeToEdge: Edge = {
    id: `ghost-edge-adapter-to-edge-${id}`,
    source: `ghost-adapter-${id}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    type: EdgeTypes.DYNAMIC_EDGE,
    focusable: false,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: '#4299E1',
    },
  }

  const edgeToDevice: Edge = {
    id: `ghost-edge-adapter-to-device-${id}`,
    source: `ghost-adapter-${id}`,
    target: `ghost-device-${id}`,
    sourceHandle: 'Top',
    type: EdgeTypes.DYNAMIC_EDGE,
    focusable: false,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: '#4299E1',
    },
  }

  return {
    nodes: [adapterNode, deviceNode],
    edges: [edgeToEdge, edgeToDevice],
  }
}
```

---

## Wizard Store Updates

**Add ghost edges support:**

```typescript
interface WizardState {
  // ...existing
  ghostNodes: GhostNode[]
  ghostEdges: Edge[] // ← NEW
}

interface WizardActions {
  // ...existing
  addGhostEdges: (edges: Edge[]) => void
  clearGhostEdges: () => void
}
```

---

## GhostNodeRenderer Updates

**Handle both nodes and edges:**

```typescript
const WizardConfigurationPanel: FC = () => {
  const { ghostNodes, ghostEdges } = useWizardGhosts()
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()
  const { data: adapters } = useListProtocolAdapters()

  useEffect(() => {
    if (!isActive || currentStep !== 0) {
      // Cleanup
      if (ghostNodes.length > 0 || ghostEdges.length > 0) {
        const realNodes = getNodes().filter(n => !n.data?.isGhost)
        const realEdges = getEdges().filter(e => !e.id.startsWith('ghost-'))
        setNodes(realNodes)
        setEdges(realEdges)
        clearGhostNodes()
        clearGhostEdges()
      }
      return
    }

    // Create ghost group if needed
    if (ghostNodes.length === 0) {
      const edgeNode = getNodes().find(n => n.id === IdStubs.EDGE_NODE)
      const nbAdapters = adapters?.length || 0

      if (edgeNode) {
        const ghostGroup = createGhostAdapterGroup(
          'wizard-preview',
          nbAdapters,
          edgeNode
        )

        addGhostNodes(ghostGroup.nodes)
        addGhostEdges(ghostGroup.edges)

        setNodes([...getNodes(), ...ghostGroup.nodes])
        setEdges([...getEdges(), ...ghostGroup.edges])
      }
    }
  }, [isActive, currentStep, ...])
}
```

---

## Smooth Transition (Ghost → Real)

**In Subtask 7 (Wizard Completion):**

```typescript
const completeWizard = async () => {
  const { configurationData } = useWizardConfiguration()

  // 1. Create real adapter via API
  const newAdapter = await createAdapter(configurationData.adapterConfig)

  // 2. Calculate position (same algorithm as ghost)
  const nbAdapters = adapters.length // Before adding new one
  const edgeNode = getNodes().find((n) => n.id === IdStubs.EDGE_NODE)
  const { adapterPos, devicePos } = calculateGhostAdapterPosition(nbAdapters, edgeNode.position)

  // 3. Create real nodes at SAME position as ghost
  const { nodeAdapter, nodeDevice, edgeConnector, deviceConnector } = createAdapterNode(
    type,
    newAdapter,
    nbAdapters,
    nbAdapters + 1,
    theme
  )

  // Nodes will be at exact same position - NO JUMP!

  // 4. Remove ghost nodes/edges
  const realNodes = getNodes().filter((n) => !n.data?.isGhost)
  const realEdges = getEdges().filter((e) => !e.id.startsWith('ghost-'))

  // 5. Add real nodes/edges
  setNodes([...realNodes, nodeAdapter, nodeDevice])
  setEdges([...realEdges, edgeConnector, deviceConnector])

  // 6. Optional: Fade-in animation for real nodes
  // Could add transition effect here
}
```

---

## Visual Design Comparison

### Current Ghost (Single Node)

```
┌─────────────────────────┐
│  📦 New Adapter        │  opacity: 0.6
│  (Preview)             │  2px dashed border
│  [Stopped]             │  Light blue bg
└─────────────────────────┘
```

### Enhanced Ghost (Multi-Node with Glow)

```
        ☁️ EDGE

         ↑ (animated dashed line)

┌═════════════════════════┐  ← Glowing box-shadow
║  📦 New Adapter        ║  opacity: 0.75
║  (Preview)             ║  3px dashed border
║  [Stopped]             ║  Glowing halo effect
└═════════════════════════┘

         ↓ (animated dashed line)

┌═════════════════════════┐
║  🖥️  New Adapter Device║  opacity: 0.75
║  (Preview)             ║  Matching glow
└═════════════════════════┘
```

---

## Files to Modify

### 1. ghostNodeFactory.ts

- Add `GhostNodeGroup` type
- Add `GHOST_STYLE_ENHANCED`
- Add `GHOST_EDGE_STYLE`
- Add `calculateGhostAdapterPosition()`
- Add `createGhostAdapterGroup()`
- Export positioning constants from nodes-utils

### 2. types.ts (wizard)

- Add `ghostEdges: Edge[]` to WizardState
- Add edge actions to WizardActions

### 3. useWizardStore.ts

- Add `ghostEdges` state
- Add `addGhostEdges` action
- Add `clearGhostEdges` action
- Update `cancelWizard` to clear edges too

### 4. GhostNodeRenderer.tsx

- Import `useListProtocolAdapters`
- Get current adapter count
- Find EDGE node
- Create multi-node ghost group
- Handle both nodes and edges
- Cleanup both nodes and edges

---

## Testing Checklist

- [ ] Ghost ADAPTER node appears at correct position
- [ ] Ghost DEVICE node appears below ADAPTER (+200px)
- [ ] Edge from ADAPTER to EDGE node visible
- [ ] Edge from ADAPTER to DEVICE visible
- [ ] Edges are animated (dashed line moving)
- [ ] Enhanced visual style (glow effect)
- [ ] Position matches where real adapter would be
- [ ] Multiple existing adapters: ghost appears in next slot
- [ ] Ghost → real transition has no position jump
- [ ] Both nodes and edges cleaned up on cancel
- [ ] Both nodes and edges cleaned up on step change

---

## Benefits

### ✅ Complete Preview

- See both ADAPTER and DEVICE nodes
- See connection topology
- Better understanding of what will be created

### ✅ Accurate Positioning

- Ghost appears exactly where real node will be
- No position jump during transition
- Respects existing adapter layout

### ✅ Better Visual Feedback

- Glowing effect draws attention
- Clear "preview" state
- More professional appearance

### ✅ Smooth UX

- Seamless ghost → real transition
- No jarring repositioning
- Polished experience

---

**Status:** Ready for implementation
