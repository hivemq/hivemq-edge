# Workspace State Management: Architecture Review & Refactoring Plan

**Date:** November 13, 2025  
**Status:** 📋 ANALYSIS & RECOMMENDATIONS

---

## Executive Summary

The workspace has **THREE competing sources of truth** for node/edge state:
1. **`useWorkspaceStore`** (Zustand) - Persisted to localStorage, user modifications
2. **`useGetFlowElements`** - Regenerated from backend API, initial positions
3. **`useReactFlow`** - React Flow's internal state, actual rendering

This creates **synchronization issues, data loss, and bugs**.

---

## Current Architecture Problems

### Problem 1: Multiple Sources of Truth

```typescript
// THREE different places nodes can be accessed:

// 1. useWorkspaceStore (persisted)
const { nodes } = useWorkspaceStore()

// 2. useGetFlowElements (from API)
const { nodes } = useGetFlowElements()

// 3. useReactFlow (React Flow internal)
const { getNodes } = useReactFlow()
```

**Issues:**
- ❌ No clear authority - which is "correct"?
- ❌ Data can get out of sync
- ❌ Mutations in one don't reflect in others
- ❌ Race conditions during updates

### Problem 2: Backend Data vs User Modifications Conflict

**Current Flow:**
```
1. Backend returns adapters/bridges (no position data)
2. useGetFlowElements calculates initial positions
3. setNodes([...]) updates React Flow
4. User drags node to new position
5. useWorkspaceStore.onNodesChange saves to localStorage
6. Backend data changes (new adapter added)
7. useGetFlowElements recalculates ALL nodes from scratch
8. User's position changes LOST ❌
```

**Example from code:**
```typescript
// useGetFlowElements.ts line 138
setNodes([nodeEdge, ...applyLayout(nodes, groups)])
// ↑ This REPLACES all nodes, losing user modifications!
```

### Problem 3: No Stable Node IDs

**Current ID generation:**
```typescript
// For adapters
id: `ADAPTER_NODE@${adapter.id}`

// For bridges  
id: `BRIDGE_NODE@${bridge.id}`

// For combiners
id: combiner.id // Uses backend UUID
```

**Issues:**
- ❌ Adapter IDs are user-defined strings, can change
- ❌ Bridge IDs can change on backend
- ❌ No guarantee of ID stability across sessions
- ❌ localStorage persistence breaks when IDs change
- ❌ Can't reliably match localStorage nodes to backend entities

### Problem 4: Position Data Not Persisted to Backend

**Current situation:**
```typescript
// Backend Adapter type:
{
  id: string
  type: string
  config: {...}
  // ❌ NO position field!
}

// localStorage WorkspaceStore:
{
  nodes: [{
    id: "ADAPTER_NODE@my-adapter",
    position: { x: 450, y: 200 } // ← Only here!
  }]
}
```

**Problems:**
- ❌ Position only in localStorage
- ❌ Lost when localStorage cleared
- ❌ Not synced across devices
- ❌ Backend changes can't preserve positions
- ❌ Team members see different layouts

### Problem 5: useGetFlowElements Regenerates Everything

**Current pattern:**
```typescript
useEffect(() => {
  const nodes: Node[] = []
  const edges: Edge[] = []
  
  // Recreate ALL nodes from scratch
  bridges?.forEach(bridge => {
    const { nodeBridge } = createBridgeNode(bridge, ...)
    nodes.push(nodeBridge)
  })
  
  // ❌ This REPLACES everything, losing user changes
  setNodes([nodeEdge, ...applyLayout(nodes, groups)])
}, [bridges, adapters, listeners, ...])
```

**Why this is bad:**
- Runs on EVERY backend data change
- Recalculates positions from scratch
- Attempts to preserve positions with `existingCombiner` check
- But only works for combiners, not adapters/bridges
- Fragile and error-prone

### Problem 6: Wizard Uses Both Stores

**Wizard flow:**
```typescript
// GhostNodeRenderer.tsx
const { getNodes, setNodes } = useReactFlow() // ← Direct React Flow

// WizardCombinerConfiguration.tsx  
const currentNodes = useWorkspaceStore.getState().nodes // ← Workspace store

// ReactFlowWrapper.tsx (node click handler)
const { protocolAdapters } = useProtocolAdaptersContext()
// Uses protocolAdapters in callback, but nodes from where?
```

**Confusion:**
- Which store has current positions?
- Which store should wizard update?
- How do changes propagate?

---

## Usage Analysis

### useWorkspaceStore Usage (20 locations)

**Legitimate uses:**
- ✅ `ReactFlowWrapper.tsx` - Main canvas, uses nodes/edges
- ✅ `CombinerMappingManager.tsx` - Updates combiner data
- ✅ `SearchEntities.tsx` - Searches nodes
- ✅ `FilterSelection.tsx` - Filters nodes

**Problematic uses:**
- ⚠️ `useGetFlowElements.ts` - Reads workspace store to preserve positions
  - Should be single source, not coordinate between stores

### useReactFlow Usage (20 locations)

**Legitimate uses:**
- ✅ Individual node components - `updateNodeData()` for local updates
- ✅ `ContextualToolbar.tsx` - `fitView()`, `getNodesBounds()`
- ✅ Wizard components - Ghost node manipulation

**Questionable uses:**
- ⚠️ Wizard directly manipulating React Flow state
  - Should go through workspace store?

### useGetFlowElements Usage

**Current role:**
- ❌ Acts as "backend → React Flow" synchronizer
- ❌ Regenerates all nodes on every backend change
- ❌ Attempts to preserve user positions (fragile)
- ❌ Becomes source of truth conflicts

---

## React Flow Best Practices (from official docs)

### Recommendation 1: Controlled vs Uncontrolled

**React Flow supports TWO patterns:**

#### Pattern A: Controlled (Recommended for complex apps)
```typescript
const [nodes, setNodes] = useState(initialNodes)
const [edges, setEdges] = useState(initialEdges)

<ReactFlow 
  nodes={nodes}
  edges={edges}
  onNodesChange={(changes) => {
    setNodes(applyNodeChanges(changes, nodes))
  }}
/>
```

#### Pattern B: Uncontrolled (Simple use cases)
```typescript
<ReactFlow 
  defaultNodes={initialNodes}
  defaultEdges={initialEdges}
/>
// React Flow manages state internally
```

**Current implementation:**
- Uses Pattern A (controlled) ✅
- But state is in Zustand store (useWorkspaceStore) ✅
- Conflict: useGetFlowElements ALSO tries to control state ❌

### Recommendation 2: Single State Source

**From React Flow docs:**
> "Keep your nodes and edges state in ONE place. Don't duplicate state between React Flow internal state and external state."

**Current violation:**
- React Flow internal state (via useReactFlow)
- Zustand store (useWorkspaceStore)
- Hook state (useGetFlowElements local useState)
- All trying to be authoritative!

### Recommendation 3: Backend Sync Pattern

**React Flow recommended pattern:**
```typescript
// 1. Load from backend
const backendData = await fetchGraphData()

// 2. Merge with local state (preserve positions)
const mergedNodes = mergeNodes(backendData, localNodes)

// 3. Update state ONCE
setNodes(mergedNodes)

// 4. On user changes, update LOCAL state
onNodesChange((changes) => {
  setNodes(applyNodeChanges(changes, nodes))
  // Optionally debounce save to backend
})

// 5. Periodically or on demand, save to backend
const saveToBackend = debounce(() => {
  savePositions(nodes.map(n => ({ id: n.id, position: n.position })))
}, 1000)
```

**Current implementation:**
- ❌ Doesn't merge, REPLACES all nodes
- ❌ Doesn't save positions to backend
- ❌ Relies on localStorage (fragile)

---

## Proposed Refactoring: Single Source of Truth

### Option 1: Zustand Store as Single Source (RECOMMENDED)

**Architecture:**
```
┌─────────────────────────────────────────────────────┐
│                  Backend API                         │
│  (adapters, bridges, combiners - NO positions)      │
└────────────────┬────────────────────────────────────┘
                 │ fetch data
                 ▼
┌─────────────────────────────────────────────────────┐
│           useWorkspaceStore (Zustand)                │
│  - SINGLE source of truth for nodes/edges           │
│  - Persisted to localStorage                         │
│  - Syncs with backend data                          │
│  - Preserves user modifications (positions)          │
└────────────────┬────────────────────────────────────┘
                 │ nodes/edges
                 ▼
┌─────────────────────────────────────────────────────┐
│              ReactFlow Component                     │
│  - Renders nodes/edges from store                   │
│  - onNodesChange → store.onNodesChange              │
│  - Pure presentation layer                           │
└─────────────────────────────────────────────────────┘
```

**Key changes:**

#### 1. Enhanced Store with Backend Sync

```typescript
// useWorkspaceStore.ts

interface WorkspaceState {
  nodes: Node[]
  edges: Edge[]
  
  // Track backend entity metadata
  entityMetadata: Map<string, {
    id: string
    type: 'adapter' | 'bridge' | 'combiner'
    backendId: string  // Original backend ID
    lastSynced: number
  }>
}

interface WorkspaceAction {
  // Existing
  onNodesChange: (changes: NodeChange[]) => void
  
  // NEW: Sync with backend data
  syncFromBackend: (data: {
    adapters?: Adapter[]
    bridges?: Bridge[]
    combiners?: Combiner[]
  }) => void
  
  // NEW: Merge strategy
  mergeNodes: (newNodes: Node[]) => void
}
```

#### 2. Smart Merge Logic

```typescript
syncFromBackend: (data) => {
  const currentNodes = get().nodes
  const currentMetadata = get().entityMetadata
  
  // Build new nodes from backend data
  const backendNodes: Node[] = []
  
  data.adapters?.forEach(adapter => {
    const nodeId = `ADAPTER_NODE@${adapter.id}`
    const existingNode = currentNodes.find(n => n.id === nodeId)
    
    const newNode = createAdapterNode(adapter, theme)
    
    // PRESERVE user position if node exists
    if (existingNode) {
      newNode.position = existingNode.position
      newNode.selected = existingNode.selected
      // Preserve other user modifications
    }
    
    backendNodes.push(newNode)
  })
  
  // Remove nodes for deleted backend entities
  const backendIds = new Set(backendNodes.map(n => n.id))
  const nodesToKeep = currentNodes.filter(n => 
    n.data.isGhost || // Keep ghost nodes
    backendIds.has(n.id) // Keep if in backend
  )
  
  // Merge: preserved nodes + new nodes
  set({ nodes: [...backendNodes] })
}
```

#### 3. Remove useGetFlowElements Hook

**Replace with:**
```typescript
// useBackendSync.ts
const useBackendSync = () => {
  const { syncFromBackend } = useWorkspaceStore()
  
  const { data: adapters } = useListProtocolAdapters()
  const { data: bridges } = useListBridges()
  const { data: combiners } = useListCombiners()
  
  useEffect(() => {
    if (adapters || bridges || combiners) {
      syncFromBackend({ adapters, bridges, combiners })
    }
  }, [adapters, bridges, combiners, syncFromBackend])
}
```

**Usage:**
```typescript
// EdgeFlowPage.tsx or ReactFlowWrapper.tsx
const EdgeWorkspace = () => {
  useBackendSync() // Just call this
  
  const { nodes, edges, onNodesChange, onEdgesChange } = useWorkspaceStore()
  
  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
    />
  )
}
```

#### 4. Position Persistence to Backend (Future)

**Backend API changes needed:**
```typescript
// New endpoint
POST /api/v1/workspace/layout
{
  "nodes": [
    { "id": "adapter-1", "position": { "x": 100, "y": 200 } },
    { "id": "bridge-1", "position": { "x": 300, "y": 200 } }
  ]
}

GET /api/v1/workspace/layout
// Returns saved positions
```

**Store integration:**
```typescript
// useWorkspaceStore.ts
saveLayoutToBackend: debounce(() => {
  const positions = get().nodes.map(n => ({
    id: n.data.id, // Backend ID
    position: n.position
  }))
  
  api.saveWorkspaceLayout(positions)
}, 2000)

// Call after position changes
onNodesChange: (changes) => {
  set({ nodes: applyNodeChanges(changes, get().nodes) })
  
  if (changes.some(c => c.type === 'position')) {
    get().saveLayoutToBackend()
  }
}
```

---

## Option 2: React Flow as Single Source

**Architecture:**
```
Backend API
     ↓
React Flow Internal State (via useReactFlow)
     ↓
External store syncs FROM React Flow (not TO it)
```

**Pros:**
- Follows React Flow's internal state pattern
- No risk of state conflicts
- Simpler mental model

**Cons:**
- ❌ Harder to persist to localStorage
- ❌ Harder to access state outside React Flow context
- ❌ Wizard would need refactoring
- ❌ Loses Zustand benefits (devtools, middleware)

**Verdict:** Not recommended for this app's complexity

---

## Option 3: Hybrid with Clear Boundaries (PRAGMATIC)

**Keep both stores but with STRICT rules:**

### Rules:
1. **useWorkspaceStore = Authority for nodes/edges**
   - Only place to call `setNodes()` / `setEdges()`
   - Syncs with backend via `syncFromBackend()`
   - Persisted to localStorage

2. **useReactFlow = Read-only queries + utilities**
   - Only use for: `fitView()`, `getNodesBounds()`, `screenToFlowPosition()`
   - NEVER use: `setNodes()`, `setEdges()`, `addNodes()`, `deleteElements()`
   - Exception: Wizard ghost nodes (temporary, not persisted)

3. **Backend data → Store only**
   - No direct backend-to-ReactFlow sync
   - Always go through `useWorkspaceStore.syncFromBackend()`

### Implementation:

#### Enforce boundaries:
```typescript
// useWorkspaceStore.ts
const useWorkspaceStore = create<WorkspaceState & WorkspaceAction>()(
  persist(
    (set, get) => ({
      // ... existing ...
      
      // NEW: Explicit sync method
      syncFromBackend: (data) => {
        const mergedNodes = mergeBackendData(
          get().nodes,
          data,
          get().theme
        )
        set({ nodes: mergedNodes })
      },
      
      // NEW: Helper to get node by backend ID
      getNodeByBackendId: (backendId: string, type: 'adapter' | 'bridge') => {
        return get().nodes.find(n => 
          n.data?.id === backendId && n.type === `${type.toUpperCase()}_NODE`
        )
      },
    }),
    {
      name: 'workspace-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        nodes: state.nodes,
        edges: state.edges,
        layoutConfig: state.layoutConfig,
      }),
    }
  )
)
```

#### Refactor useGetFlowElements:
```typescript
// RENAME to useBackendSync.ts
const useBackendSync = () => {
  const syncFromBackend = useWorkspaceStore(state => state.syncFromBackend)
  
  // All the API hooks
  const { data: adapters } = useListProtocolAdapters()
  const { data: bridges } = useListBridges()
  // ...
  
  useEffect(() => {
    syncFromBackend({
      adapters: adapters?.items,
      bridges: bridges?.items,
      combiners: combiners?.items,
      listeners: listeners?.items,
    })
  }, [adapters, bridges, combiners, listeners, syncFromBackend])
}
```

#### Update ReactFlowWrapper:
```typescript
// ReactFlowWrapper.tsx
const ReactFlowWrapper = () => {
  useBackendSync() // Sync backend data to store
  
  const { nodes, edges, onNodesChange, onEdgesChange } = useWorkspaceStore()
  
  // ❌ DON'T do this:
  // const { setNodes } = useReactFlow()
  
  // ✅ DO use this for utilities:
  const { fitView, screenToFlowPosition } = useReactFlow()
  
  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
    />
  )
}
```

---

## Smart Merge Strategy: Robust Entity Matching

### The Problem with Simple ID Matching

**Current naive approach:**
```typescript
const existingNode = currentNodes.find(n => n.id === nodeId)
if (existingNode) {
  newNode.position = existingNode.position // ❌ DANGEROUS!
}
```

**Critical Issues:**

#### Issue 1: ID Reuse After Deletion
```
Scenario:
1. User creates HTTP adapter "sensor-1"
   → Node ID: "ADAPTER_NODE@sensor-1"
   → User positions it at (500, 300)
2. User deletes "sensor-1"
3. User creates MQTT adapter "sensor-1" (same name!)
   → Node ID: "ADAPTER_NODE@sensor-1" (SAME!)
   → New adapter gets OLD position ❌
   → HTTP adapter position applied to MQTT adapter ❌
```

#### Issue 2: Type Changes Not Detected
```
Scenario:
1. Bridge "production" exists
   → Node ID: "BRIDGE_NODE@production"
2. Backend deleted bridge, created adapter with same ID
   → Node still has ID "ADAPTER_NODE@production"
   → Merge matches by ID, thinks it's the same entity ❌
   → Wrong data, wrong type, wrong behavior ❌
```

#### Issue 3: Configuration Changes Break Assumptions
```
Scenario:
1. Adapter references topic "sensor/temp"
   → Edges connected to specific topics
2. Backend updates adapter config to "sensor/humidity"
   → Edges still point to old topic ❌
   → Visual graph doesn't match reality ❌
```

---

### Solution: Entity Fingerprinting

**Concept:** Create a unique fingerprint for each entity that captures its identity beyond just ID.

#### 1. Entity Fingerprint Structure

```typescript
interface EntityFingerprint {
  // Primary identity
  id: string
  type: 'adapter' | 'bridge' | 'combiner' | 'listener' | 'pulse'
  
  // Type-specific identity markers
  subType?: string // e.g., adapter protocol type: "http", "mqtt", "opcua"
  
  // Configuration hash (detect meaningful changes)
  configHash: string
  
  // Creation metadata
  createdAt?: number
  
  // Version tracking
  version?: number
  etag?: string // If backend supports ETags
}
```

#### 2. Generate Fingerprints

```typescript
// utils/entityFingerprinting.ts

import { hashObject } from '@/utils/crypto'

export const generateAdapterFingerprint = (adapter: Adapter): EntityFingerprint => {
  // Extract configuration that defines identity
  const identityConfig = {
    id: adapter.id,
    type: adapter.type, // "http", "mqtt", etc.
    // Don't include position, last_modified, etc.
  }
  
  return {
    id: adapter.id,
    type: 'adapter',
    subType: adapter.type,
    configHash: hashObject(identityConfig),
    version: adapter.version,
  }
}

export const generateBridgeFingerprint = (bridge: Bridge): EntityFingerprint => {
  const identityConfig = {
    id: bridge.id,
    type: 'bridge',
    host: bridge.host, // Core identity attribute
    port: bridge.port,
  }
  
  return {
    id: bridge.id,
    type: 'bridge',
    subType: 'bridge',
    configHash: hashObject(identityConfig),
  }
}

export const generateCombinerFingerprint = (combiner: Combiner): EntityFingerprint => {
  // Combiners have UUIDs, more stable
  return {
    id: combiner.id,
    type: 'combiner',
    subType: 'combiner',
    configHash: hashObject({ id: combiner.id }),
  }
}
```

#### 3. Store Fingerprints with Nodes

```typescript
// Enhanced node data structure
interface EnhancedNodeData {
  // Original entity data
  ...originalData
  
  // Add fingerprint
  _fingerprint?: EntityFingerprint
  
  // Track when last synced
  _lastSynced?: number
}
```

#### 4. Smart Match Algorithm

```typescript
// utils/smartMerge.ts

interface MatchResult {
  existingNode: Node | null
  confidence: 'high' | 'medium' | 'low' | 'none'
  reason: string
  warnings: string[]
}

export const findMatchingNode = (
  backendEntity: Adapter | Bridge | Combiner,
  backendFingerprint: EntityFingerprint,
  currentNodes: Node[]
): MatchResult => {
  const warnings: string[] = []
  
  // Step 1: Find candidate nodes by ID
  const nodeId = generateNodeId(backendEntity)
  const candidateById = currentNodes.find(n => n.id === nodeId)
  
  if (!candidateById) {
    return {
      existingNode: null,
      confidence: 'none',
      reason: 'No node with this ID exists',
      warnings: [],
    }
  }
  
  const existingFingerprint = candidateById.data?._fingerprint
  
  if (!existingFingerprint) {
    // Old node without fingerprint - assume it's correct but warn
    warnings.push('Node has no fingerprint - cannot verify identity')
    return {
      existingNode: candidateById,
      confidence: 'low',
      reason: 'ID match but no fingerprint for verification',
      warnings,
    }
  }
  
  // Step 2: Verify type matches
  if (existingFingerprint.type !== backendFingerprint.type) {
    // CRITICAL: Type mismatch! This is a different entity!
    warnings.push(
      `Type mismatch: existing is ${existingFingerprint.type}, backend is ${backendFingerprint.type}`
    )
    return {
      existingNode: null,
      confidence: 'none',
      reason: 'Type mismatch - different entity type',
      warnings,
    }
  }
  
  // Step 3: Verify subType matches (for adapters)
  if (existingFingerprint.subType !== backendFingerprint.subType) {
    warnings.push(
      `SubType mismatch: existing is ${existingFingerprint.subType}, backend is ${backendFingerprint.subType}`
    )
    return {
      existingNode: null,
      confidence: 'none',
      reason: 'SubType mismatch - different protocol/entity subtype',
      warnings,
    }
  }
  
  // Step 4: Check configuration hash
  if (existingFingerprint.configHash !== backendFingerprint.configHash) {
    // Configuration changed - this is OK, but note it
    warnings.push('Configuration hash changed - entity was modified')
    return {
      existingNode: candidateById,
      confidence: 'medium',
      reason: 'ID and type match, but configuration changed',
      warnings,
    }
  }
  
  // Step 5: Perfect match!
  return {
    existingNode: candidateById,
    confidence: 'high',
    reason: 'ID, type, subtype, and config all match',
    warnings: [],
  }
}
```

#### 5. Enhanced Merge Logic

```typescript
// useWorkspaceStore.ts

syncFromBackend: (data: BackendData) => {
  const currentNodes = get().nodes
  const theme = get().theme // Store theme for node creation
  
  const mergedNodes: Node[] = []
  const orphanedNodes: Node[] = [] // Nodes that no longer exist in backend
  const conflicts: Array<{node: Node, reason: string}> = []
  
  // Track which existing nodes we've matched
  const matchedNodeIds = new Set<string>()
  
  // Process adapters
  data.adapters?.forEach(adapter => {
    const fingerprint = generateAdapterFingerprint(adapter)
    const match = findMatchingNode(adapter, fingerprint, currentNodes)
    
    // Create new node from backend data
    const newNode = createAdapterNode(adapter, theme)
    newNode.data._fingerprint = fingerprint
    newNode.data._lastSynced = Date.now()
    
    if (match.confidence === 'high' || match.confidence === 'medium') {
      // PRESERVE user modifications
      const existingNode = match.existingNode!
      newNode.position = existingNode.position
      newNode.selected = existingNode.selected
      newNode.style = existingNode.style
      // Preserve any user-added metadata
      
      matchedNodeIds.add(existingNode.id)
      
      if (match.warnings.length > 0) {
        console.warn(`[Merge] Adapter ${adapter.id}:`, match.warnings)
      }
    } else if (match.confidence === 'low') {
      // Low confidence - preserve position but warn
      const existingNode = match.existingNode!
      newNode.position = existingNode.position
      matchedNodeIds.add(existingNode.id)
      
      console.warn(`[Merge] Low confidence match for adapter ${adapter.id}:`, match.reason)
    } else {
      // No match - this is a NEW node
      // Position will be calculated by layout algorithm
      console.log(`[Merge] New adapter detected: ${adapter.id}`)
    }
    
    mergedNodes.push(newNode)
  })
  
  // Process bridges (same pattern)
  data.bridges?.forEach(bridge => {
    const fingerprint = generateBridgeFingerprint(bridge)
    const match = findMatchingNode(bridge, fingerprint, currentNodes)
    
    const newNode = createBridgeNode(bridge, theme)
    newNode.data._fingerprint = fingerprint
    newNode.data._lastSynced = Date.now()
    
    if (match.confidence === 'high' || match.confidence === 'medium') {
      newNode.position = match.existingNode!.position
      matchedNodeIds.add(match.existingNode!.id)
    }
    
    mergedNodes.push(newNode)
  })
  
  // Process combiners (same pattern)
  data.combiners?.forEach(combiner => {
    const fingerprint = generateCombinerFingerprint(combiner)
    const match = findMatchingNode(combiner, fingerprint, currentNodes)
    
    const sources = getSources(combiner, mergedNodes)
    const newNode = createCombinerNode(combiner, sources, theme)
    newNode.data._fingerprint = fingerprint
    newNode.data._lastSynced = Date.now()
    
    if (match.confidence === 'high' || match.confidence === 'medium') {
      newNode.position = match.existingNode!.position
      matchedNodeIds.add(match.existingNode!.id)
    }
    
    mergedNodes.push(newNode)
  })
  
  // Identify orphaned nodes (deleted from backend)
  currentNodes.forEach(node => {
    if (!matchedNodeIds.has(node.id) && !node.data?.isGhost) {
      orphanedNodes.push(node)
    }
  })
  
  // Handle orphaned nodes
  if (orphanedNodes.length > 0) {
    console.warn(`[Merge] Found ${orphanedNodes.length} orphaned nodes (deleted from backend)`, 
      orphanedNodes.map(n => ({ id: n.id, type: n.type }))
    )
    
    // Option 1: Remove them (current backend is source of truth)
    // Option 2: Keep them temporarily with warning visual
    // Option 3: Ask user via dialog
    
    // For now: Remove them
    // Future: Add "orphaned" visual state and let user decide
  }
  
  // Add EDGE node (always present)
  const edgeNode = createEdgeNode(theme)
  mergedNodes.unshift(edgeNode)
  
  // Update store
  set({ 
    nodes: mergedNodes,
    // Track merge metadata
    lastMerge: {
      timestamp: Date.now(),
      newNodes: mergedNodes.filter(n => !matchedNodeIds.has(n.id)).length,
      updatedNodes: matchedNodeIds.size,
      orphanedNodes: orphanedNodes.length,
    }
  })
}
```

---

### Advanced Matching: Fuzzy Matching for ID Changes

**Problem:** Backend might change IDs (e.g., adapter renamed)

**Solution:** Secondary matching by attributes

```typescript
interface FuzzyMatchCandidate {
  node: Node
  score: number
  matchedAttributes: string[]
}

export const fuzzyMatchNode = (
  backendEntity: Adapter,
  backendFingerprint: EntityFingerprint,
  unmatchedNodes: Node[]
): FuzzyMatchCandidate | null => {
  const candidates: FuzzyMatchCandidate[] = []
  
  unmatchedNodes.forEach(node => {
    // Skip if wrong type
    if (node.type !== 'ADAPTER_NODE') return
    
    const fingerprint = node.data?._fingerprint
    if (!fingerprint) return
    
    let score = 0
    const matched: string[] = []
    
    // Same protocol type? +50 points
    if (fingerprint.subType === backendEntity.type) {
      score += 50
      matched.push('protocol-type')
    }
    
    // Similar config? +30 points
    if (fingerprint.configHash === backendFingerprint.configHash) {
      score += 30
      matched.push('config-hash')
    }
    
    // Same position in canvas? (spatial locality) +10 points
    const expectedPosition = calculateExpectedPosition(backendEntity)
    const distance = Math.hypot(
      node.position.x - expectedPosition.x,
      node.position.y - expectedPosition.y
    )
    if (distance < 100) {
      score += 10
      matched.push('position-nearby')
    }
    
    // Created around same time? +10 points
    if (fingerprint.createdAt) {
      const timeDiff = Math.abs(fingerprint.createdAt - (backendEntity.createdAt || 0))
      if (timeDiff < 60000) { // Within 1 minute
        score += 10
        matched.push('creation-time')
      }
    }
    
    if (score > 0) {
      candidates.push({ node, score, matchedAttributes: matched })
    }
  })
  
  // Return highest scoring candidate if above threshold
  candidates.sort((a, b) => b.score - a.score)
  
  if (candidates.length > 0 && candidates[0].score >= 50) {
    return candidates[0]
  }
  
  return null
}
```

---

### Migration: Adding Fingerprints to Existing Nodes

**Problem:** Existing nodes in localStorage have no fingerprints

**Solution:** Gradual migration with fallback

```typescript
// useWorkspaceStore.ts initialization

const migrateNodesWithFingerprints = (
  nodes: Node[],
  backendData: BackendData
): Node[] => {
  return nodes.map(node => {
    // Skip if already has fingerprint
    if (node.data?._fingerprint) {
      return node
    }
    
    // Try to generate fingerprint from current data
    let fingerprint: EntityFingerprint | undefined
    
    if (node.type === 'ADAPTER_NODE') {
      // Find matching adapter in backend
      const adapter = backendData.adapters?.find(a => 
        `ADAPTER_NODE@${a.id}` === node.id
      )
      if (adapter) {
        fingerprint = generateAdapterFingerprint(adapter)
      }
    } else if (node.type === 'BRIDGE_NODE') {
      const bridge = backendData.bridges?.find(b => 
        `BRIDGE_NODE@${b.id}` === node.id
      )
      if (bridge) {
        fingerprint = generateBridgeFingerprint(bridge)
      }
    } else if (node.type === 'COMBINER_NODE') {
      const combiner = backendData.combiners?.find(c => 
        c.id === node.id
      )
      if (combiner) {
        fingerprint = generateCombinerFingerprint(combiner)
      }
    }
    
    if (fingerprint) {
      return {
        ...node,
        data: {
          ...node.data,
          _fingerprint: fingerprint,
          _lastSynced: Date.now(),
        },
      }
    }
    
    // Can't determine fingerprint - mark for manual review
    console.warn(`[Migration] Cannot generate fingerprint for node ${node.id}`)
    return node
  })
}
```

---

### Conflict Resolution Strategies

#### Strategy 1: Always Trust Backend (Current)
```typescript
// If type mismatch, backend wins
if (existingFingerprint.type !== backendFingerprint.type) {
  return { existingNode: null } // Don't preserve, create new
}
```

**Pros:** Simple, backend is source of truth  
**Cons:** Loses user positions when entity replaced

#### Strategy 2: User Confirmation
```typescript
// If type mismatch, ask user
if (existingFingerprint.type !== backendFingerprint.type) {
  showConflictDialog({
    title: 'Entity Type Changed',
    message: `The entity "${nodeId}" changed from ${existingFingerprint.type} to ${backendFingerprint.type}`,
    options: [
      'Keep old position for new entity',
      'Use default position for new entity',
      'Manually review'
    ]
  })
}
```

**Pros:** User control  
**Cons:** Annoying for many changes

#### Strategy 3: Heuristic-Based
```typescript
// If types are "similar", preserve position
const compatibleTypes = {
  'adapter': ['adapter'],
  'bridge': ['bridge'],
  'combiner': ['combiner', 'asset_mapper'], // These are similar
}

if (areCompatibleTypes(existing.type, backend.type)) {
  // Preserve position with warning marker
  newNode.position = existingNode.position
  newNode.data._warning = 'Entity type changed'
}
```

**Pros:** Smart, reduces friction  
**Cons:** Complexity, might guess wrong

#### Strategy 4: Spatial Clustering (RECOMMENDED)
```typescript
// Group nearby nodes, preserve positions within groups
// If entity type changes but others nearby didn't, likely intentional replacement

const nearbyNodes = getNearbyNodes(existingNode, currentNodes, 200)
const nearbyTypesChanged = nearbyNodes.filter(n => 
  hasTypeChanged(n, backendData)
)

if (nearbyTypesChanged.length === 0) {
  // Only this node changed - likely replacement
  // Use default position for new type
} else {
  // Multiple nearby nodes changed - likely mass operation
  // Preserve positions for consistency
  newNode.position = existingNode.position
}
```

**Pros:** Intelligent, considers context  
**Cons:** Most complex

---

### Testing Strategy for Smart Merge

```typescript
describe('Smart Merge', () => {
  describe('Entity Fingerprinting', () => {
    it('generates stable fingerprints for same entity', () => {
      const adapter1 = { id: 'test', type: 'http', config: {...} }
      const adapter2 = { id: 'test', type: 'http', config: {...} }
      
      expect(generateAdapterFingerprint(adapter1))
        .toEqual(generateAdapterFingerprint(adapter2))
    })
    
    it('generates different fingerprints for different types', () => {
      const adapter = { id: 'test', type: 'http' }
      const bridge = { id: 'test', type: 'bridge' }
      
      expect(generateAdapterFingerprint(adapter).configHash)
        .not.toEqual(generateBridgeFingerprint(bridge).configHash)
    })
  })
  
  describe('Matching Algorithm', () => {
    it('detects type mismatch', () => {
      const backend = { id: 'test', type: 'mqtt' }
      const existing = createNode({ id: 'test', type: 'http' })
      
      const match = findMatchingNode(backend, existing)
      
      expect(match.confidence).toBe('none')
      expect(match.reason).toContain('Type mismatch')
    })
    
    it('handles ID reuse correctly', () => {
      // Scenario: HTTP adapter deleted, MQTT adapter created with same ID
      const oldNode = createAdapterNode({ id: 'sensor', type: 'http' })
      const newBackend = { id: 'sensor', type: 'mqtt' }
      
      const match = findMatchingNode(newBackend, [oldNode])
      
      // Should NOT match - different subtype
      expect(match.existingNode).toBeNull()
    })
    
    it('preserves position when config changes but identity same', () => {
      const existing = createAdapterNode({ 
        id: 'test', 
        type: 'http',
        config: { url: 'old' }
      })
      existing.position = { x: 999, y: 999 }
      
      const backend = { 
        id: 'test', 
        type: 'http',
        config: { url: 'new' } // Config changed
      }
      
      const result = syncFromBackend({ adapters: [backend] })
      
      expect(result.nodes[0].position).toEqual({ x: 999, y: 999 })
      expect(result.nodes[0].data.config.url).toBe('new')
    })
  })
  
  describe('Orphaned Node Detection', () => {
    it('identifies nodes deleted from backend', () => {
      const existingNodes = [
        createAdapterNode({ id: 'keep' }),
        createAdapterNode({ id: 'delete' }),
      ]
      
      const backendData = {
        adapters: [{ id: 'keep' }] // 'delete' missing
      }
      
      const result = syncFromBackend(backendData, existingNodes)
      
      expect(result.orphanedNodes).toHaveLength(1)
      expect(result.orphanedNodes[0].id).toContain('delete')
    })
  })
})
```

---

### Performance Considerations

#### Optimization 1: Index Nodes by Fingerprint Hash

```typescript
// Build index for O(1) lookup
const nodeIndex = new Map<string, Node>()
currentNodes.forEach(node => {
  if (node.data?._fingerprint) {
    const key = `${node.data._fingerprint.type}:${node.data._fingerprint.configHash}`
    nodeIndex.set(key, node)
  }
})

// Fast lookup
const key = `${fingerprint.type}:${fingerprint.configHash}`
const match = nodeIndex.get(key)
```

#### Optimization 2: Batch Fingerprint Generation

```typescript
// Generate all fingerprints at once
const adapterFingerprints = adapters.map(generateAdapterFingerprint)
// Process in parallel if needed
```

#### Optimization 3: Debounce Backend Sync

```typescript
// Don't sync on every tiny backend change
const debouncedSync = debounce(syncFromBackend, 500)

useEffect(() => {
  debouncedSync({ adapters, bridges, combiners })
}, [adapters, bridges, combiners])
```

---

### Monitoring and Debugging

```typescript
// Add merge statistics to store
interface MergeStats {
  timestamp: number
  newNodes: number
  updatedNodes: number
  orphanedNodes: number
  conflicts: Array<{
    nodeId: string
    reason: string
    resolution: 'replaced' | 'preserved' | 'manual'
  }>
  duration: number
}

// Track in store
interface WorkspaceState {
  // ...existing
  mergeHistory: MergeStats[]
}

// Expose debug method
window.__debugWorkspace = () => {
  const store = useWorkspaceStore.getState()
  console.table(store.mergeHistory)
  console.log('Orphaned nodes:', store.nodes.filter(n => n.data?._warning))
}
```

---

## Refactoring Plan: Step-by-Step

### Phase 1: Add Smart Merge (No Breaking Changes)

**Goal:** Stop losing user positions

**Tasks:**
1. ✅ Add `syncFromBackend()` method to useWorkspaceStore
2. ✅ Implement smart merge logic that preserves positions
3. ✅ Update useGetFlowElements to use new sync method
4. ✅ Test that positions are preserved across backend updates

**Estimated effort:** 4-6 hours

**Files to modify:**
- `useWorkspaceStore.ts` - Add syncFromBackend
- `useGetFlowElements.ts` - Use syncFromBackend instead of setNodes
- Add tests

### Phase 2: Cleanup Direct React Flow Access

**Goal:** Enforce boundaries

**Tasks:**
1. ✅ Audit all `useReactFlow` usage
2. ✅ Replace `setNodes/setEdges` calls with store methods
3. ✅ Keep only utility methods (`fitView`, `screenToFlowPosition`)
4. ✅ Document allowed vs forbidden patterns

**Estimated effort:** 3-4 hours

**Files to modify:**
- Review all 20 useReactFlow usages
- Likely no changes needed (most are utilities already)
- Add ESLint rule to prevent `setNodes` outside store?

### Phase 3: Stable Node IDs

**Goal:** Reliable localStorage persistence

**Tasks:**
1. ✅ Add `entityMetadata` map to store
2. ✅ Track backend ID → node ID mapping
3. ✅ Handle backend ID changes gracefully
4. ✅ Add migration for existing localStorage data

**Estimated effort:** 4-5 hours

**Files to modify:**
- `useWorkspaceStore.ts` - Add metadata tracking
- `nodes-utils.ts` - Update node creation functions

### Phase 4: Backend Position Persistence (Future)

**Goal:** Sync positions across devices

**Tasks:**
1. ⏳ Backend API: Add `/workspace/layout` endpoints
2. ⏳ Store: Add `saveLayoutToBackend()` with debounce
3. ⏳ Store: Load positions on init
4. ⏳ Merge backend positions with local positions

**Estimated effort:** 8-10 hours (requires backend changes)

**Dependencies:**
- Backend team to implement endpoints
- Decision on data model (per-user? per-workspace?)

---

## Immediate Wins (Quick Fixes)

### Fix 1: Preserve Positions for ALL Entities

**Current:** Only combiners preserve positions
**Fix:** Apply to adapters, bridges too

```typescript
// In syncFromBackend
data.adapters?.forEach(adapter => {
  const nodeId = `ADAPTER_NODE@${adapter.id}`
  const existingNode = currentNodes.find(n => n.id === nodeId)
  
  const newNode = createAdapterNode(adapter, theme)
  
  if (existingNode) {
    newNode.position = existingNode.position // ← Add this
  }
  
  nodes.push(newNode)
})
```

### Fix 2: Reduce useGetFlowElements Re-runs

**Current:** Runs on every backend data change
**Fix:** Memoize better, only sync on actual changes

```typescript
const adapterId = useMemo(() => 
  adapters?.items?.map(a => a.id).join(','),
  [adapters]
)

useEffect(() => {
  syncFromBackend({ adapters: adapters?.items })
}, [adapterId]) // Only when IDs change, not object reference
```

### Fix 3: Clear Documentation

**Add to README:**
```markdown
## State Management Rules

### ✅ DO:
- Use `useWorkspaceStore()` to access/modify nodes and edges
- Use `useReactFlow()` for utilities: fitView, getNodesBounds, screenToFlowPosition
- Let wizard use React Flow for ghost nodes (temporary state)

### ❌ DON'T:
- Never call setNodes/setEdges from useReactFlow outside the store
- Don't duplicate state between store and local component state
- Don't directly modify nodes array - use onNodesChange

### Pattern:
```typescript
// ✅ GOOD
const { nodes, onNodesChange } = useWorkspaceStore()

// ❌ BAD  
const { getNodes, setNodes } = useReactFlow()
const nodes = getNodes()
setNodes([...nodes, newNode])
```
```

---

## Migration Strategy

### Breaking Changes: None in Phase 1-3!

The refactoring can be done **incrementally** without breaking existing functionality:

1. **Add new methods** to useWorkspaceStore (backwards compatible)
2. **Gradually migrate** useGetFlowElements to use new methods
3. **Keep old behavior** until fully migrated
4. **Remove old code** in final step

### Testing Strategy

#### Unit Tests:
```typescript
describe('useWorkspaceStore.syncFromBackend', () => {
  it('preserves user positions when backend data changes', () => {
    // Arrange: User moved node
    const { result } = renderHook(() => useWorkspaceStore())
    act(() => {
      result.current.onNodesChange([{
        type: 'position',
        id: 'ADAPTER_NODE@test',
        position: { x: 999, y: 999 }
      }])
    })
    
    // Act: Backend sends updated data (position not in backend)
    act(() => {
      result.current.syncFromBackend({
        adapters: [{ id: 'test', type: 'http', ... }]
      })
    })
    
    // Assert: Position preserved
    expect(result.current.nodes[0].position).toEqual({ x: 999, y: 999 })
  })
})
```

#### Integration Tests:
- Test full flow: backend update → position preserved
- Test localStorage persistence
- Test wizard interaction with main canvas

---

## Alternative: Zustand + React Flow Official Pattern

**React Flow team recommends:**

Use Zustand for external state, but **don't duplicate** React Flow's internal state.

```typescript
// Instead of storing nodes in Zustand:
interface WorkspaceState {
  nodes: Node[] // ❌ Duplication!
}

// Do this:
interface WorkspaceState {
  selectedIds: string[] // ✅ Just your app logic
  filters: FilterState
  layoutMode: LayoutMode
}

// Let React Flow manage nodes/edges
const [nodes, setNodes] = useNodesState(initialNodes)
const [edges, setEdges] = useEdgesState(initialEdges)
```

**For this app, would require:**
- Move nodes/edges OUT of useWorkspaceStore
- Store only app-specific state (filters, layout config)
- Manage nodes/edges in ReactFlowWrapper with hooks
- Implement custom persistence hook for localStorage

**Pros:**
- ✅ Follows React Flow best practices exactly
- ✅ No state duplication
- ✅ Simpler mental model

**Cons:**
- ❌ Large refactoring (weeks of work)
- ❌ Need custom localStorage persistence
- ❌ Lose Zustand devtools for nodes
- ❌ Risk of breaking things

**Verdict:** Good for greenfield, too risky for refactoring

---

## Recommendations Summary

### Immediate (This Week):
1. ✅ **Implement smart merge** in useWorkspaceStore
2. ✅ **Preserve all positions** (adapters, bridges, combiners)
3. ✅ **Add documentation** on state management rules
4. ✅ **Audit useReactFlow usage** - ensure proper patterns

### Short Term (Next Sprint):
1. ✅ **Rename useGetFlowElements** → useBackendSync
2. ✅ **Add entity metadata tracking** for stable IDs
3. ✅ **Add unit tests** for merge logic
4. ✅ **Improve memoization** to reduce re-renders

### Long Term (Next Quarter):
1. ⏳ **Backend position persistence** (requires API changes)
2. ⏳ **Consider full refactor** to React Flow pattern (if pain continues)
3. ⏳ **Evaluate moving to uncontrolled mode** (if complexity reduces)

### Avoid:
- ❌ Adding more state sources
- ❌ Direct React Flow state mutations outside store
- ❌ Storing positions in multiple places

---

## Decision Framework

Choose **Option 1 (Zustand as single source)** if:
- ✅ Want minimal changes to existing code
- ✅ Need localStorage persistence NOW
- ✅ Zustand devtools are valuable
- ✅ Team comfortable with current architecture

Choose **Option 3 (Hybrid with boundaries)** if:
- ✅ Want best of both worlds
- ✅ Willing to enforce patterns via code review
- ✅ Need flexibility for wizard ghost nodes
- ✅ Incremental migration preferred

Choose **Alternative (React Flow pattern)** if:
- ⏳ Starting fresh / major rewrite acceptable
- ⏳ Want to follow React Flow best practices exactly
- ⏳ Can invest weeks in refactoring
- ⏳ Backend position persistence coming soon

---

## My Recommendation: **Hybrid (Option 3) + Phase 1-3 Immediately**

**Why:**
1. **Pragmatic** - Works with current architecture
2. **Safe** - No breaking changes
3. **Incremental** - Can migrate piece by piece
4. **Clear** - Documented rules prevent future mistakes
5. **Future-proof** - Sets up for backend persistence

**Priority order:**
1. Implement smart merge (Phase 1) - **CRITICAL**
2. Document state rules - **HIGH**  
3. Add position preservation for all entities - **HIGH**
4. Clean up React Flow usage - **MEDIUM**
5. Add metadata tracking - **MEDIUM**
6. Backend persistence - **FUTURE**

**Estimated total effort for critical fixes:** 1-2 days

---

**Status:** 📋 ANALYSIS COMPLETE - Ready for implementation decisions

