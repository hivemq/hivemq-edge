import { expect } from 'vitest'
import type { Node, Edge } from '@xyflow/react'

import {
  createGhostAdapter,
  createGhostBridge,
  createGhostCombiner,
  createGhostAssetMapper,
  createGhostGroup,
  isGhostNode,
  getGhostNodeIds,
  removeGhostNodes,
  removeGhostEdges,
  createGhostNodeForType,
  calculateGhostAdapterPosition,
  createGhostAdapterGroup,
  calculateGhostBridgePosition,
  createGhostBridgeGroup,
  isGhostEdge,
} from './ghostNodeFactory'
import { EntityType } from '../types'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'

describe('ghostNodeFactory', () => {
  const mockEdgeNode: Node = {
    id: 'EDGE_NODE',
    type: 'EDGE_NODE',
    position: { x: 100, y: 100 },
    data: {},
  }

  describe('createGhostAdapter', () => {
    it('should create a ghost adapter node', () => {
      const ghost = createGhostAdapter('test-id')

      expect(ghost).toBeDefined()
      expect(ghost.id).toBe('ghost-test-id')
      expect(ghost.type).toBe('ADAPTER_NODE')
      expect(ghost.data.isGhost).toBe(true)
      expect(ghost.selectable).toBe(false)
      expect(ghost.draggable).toBe(false)
    })

    it('should use provided label', () => {
      const ghost = createGhostAdapter('test-id', 'Custom Label')

      expect(ghost.data.label).toBe('Custom Label')
    })
  })

  // createGhostDevice - not exported, skipping test

  describe('createGhostBridge', () => {
    it('should create a ghost bridge node', () => {
      const ghost = createGhostBridge('test-id')

      expect(ghost).toBeDefined()
      expect(ghost.id).toBe('ghost-test-id')
      expect(ghost.type).toBe('BRIDGE_NODE')
      expect(ghost.data.isGhost).toBe(true)
    })
  })

  describe('createGhostCombiner', () => {
    it('should create a ghost combiner node', () => {
      const ghost = createGhostCombiner('test-id', mockEdgeNode)

      expect(ghost).toBeDefined()
      expect(ghost.id).toContain('ghost-combiner')
      expect(ghost.type).toBe('COMBINER_NODE')
      expect(ghost.data.isGhost).toBe(true)
      expect(ghost.selectable).toBe(true) // Combiner ghost is selectable
    })

    it('should position relative to edge node', () => {
      const ghost = createGhostCombiner('test-id', mockEdgeNode)

      expect(ghost.position.x).toBeGreaterThan(mockEdgeNode.position.x)
      expect(ghost.position.y).toBe(mockEdgeNode.position.y)
    })

    it('should have sources and mappings structure', () => {
      const ghost = createGhostCombiner('test-id', mockEdgeNode)

      expect(ghost.data).toBeDefined()
      // Type assertion needed for dynamic data structure
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data = ghost.data as any
      expect(data.sources).toBeDefined()
      expect(data.sources.items).toEqual([])
      expect(data.mappings).toBeDefined()
      expect(data.mappings.items).toEqual([])
    })

    it('should have UUID for ID validation', () => {
      const ghost = createGhostCombiner('test-id', mockEdgeNode)

      // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
      expect(ghost.data.id).toMatch(uuidRegex)
    })
  })

  describe('createGhostAssetMapper', () => {
    it('should create a ghost asset mapper node', () => {
      const ghost = createGhostAssetMapper('test-id', mockEdgeNode)

      expect(ghost).toBeDefined()
      expect(ghost.type).toBe('COMBINER_NODE') // Asset Mapper IS a Combiner
      expect(ghost.data.isGhost).toBe(true)
    })

    it('should reuse Combiner structure', () => {
      const combinerGhost = createGhostCombiner('test-id', mockEdgeNode, 'Test Combiner')
      const assetMapperGhost = createGhostAssetMapper('test-id', mockEdgeNode, 'Test Asset Mapper')

      // Should have same structure (both use createGhostCombiner)
      expect(assetMapperGhost.type).toBe(combinerGhost.type)
      expect(assetMapperGhost.data.sources).toBeDefined()
      expect(assetMapperGhost.data.mappings).toBeDefined()
    })
  })

  describe('createGhostGroup', () => {
    it('should create a ghost group node', () => {
      const ghost = createGhostGroup('test-id')

      expect(ghost.id).toBe('ghost-test-id')
      expect(ghost.type).toBe('CLUSTER_NODE')
      expect(ghost.data.isGhost).toBe(true)
      expect(ghost.data.label).toBe('New Group')
      expect(ghost.data.childrenNodeIds).toEqual([])
    })

    it('should use custom label', () => {
      const ghost = createGhostGroup('test-id', 'My Custom Group')

      expect(ghost.data.label).toBe('My Custom Group')
    })

    it('should have correct dimensions', () => {
      const ghost = createGhostGroup('test-id')

      expect(ghost.style?.width).toBe(300)
      expect(ghost.style?.height).toBe(200)
    })
  })

  describe('createGhostNodeForType', () => {
    it('should create ghost adapter for ADAPTER type', () => {
      const ghost = createGhostNodeForType(EntityType.ADAPTER, 'test-id')

      expect(ghost).not.toBeNull()
      expect(ghost?.type).toBe(NodeTypes.ADAPTER_NODE)
    })

    it('should create ghost bridge for BRIDGE type', () => {
      const ghost = createGhostNodeForType(EntityType.BRIDGE, 'test-id')

      expect(ghost).not.toBeNull()
      expect(ghost?.type).toBe(NodeTypes.BRIDGE_NODE)
    })

    it('should create ghost combiner for COMBINER type', () => {
      const ghost = createGhostNodeForType(EntityType.COMBINER, 'test-id')

      expect(ghost).not.toBeNull()
      expect(ghost?.type).toBe(NodeTypes.COMBINER_NODE)
    })

    it('should create ghost combiner for ASSET_MAPPER type', () => {
      const ghost = createGhostNodeForType(EntityType.ASSET_MAPPER, 'test-id')

      expect(ghost).not.toBeNull()
      expect(ghost?.type).toBe(NodeTypes.COMBINER_NODE)
    })

    it('should create ghost group for GROUP type', () => {
      const ghost = createGhostNodeForType(EntityType.GROUP, 'test-id')

      expect(ghost).not.toBeNull()
      expect(ghost?.type).toBe('CLUSTER_NODE')
    })

    it('should return null for unknown type', () => {
      const ghost = createGhostNodeForType('UNKNOWN_TYPE' as EntityType, 'test-id')

      expect(ghost).toBeNull()
    })
  })

  describe('calculateGhostAdapterPosition', () => {
    it('should calculate position for first adapter', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const { adapterPos, devicePos } = calculateGhostAdapterPosition(0, edgeNodePos)

      expect(adapterPos).toBeDefined()
      expect(devicePos).toBeDefined()
      expect(devicePos.y).toBeLessThan(adapterPos.y) // Device is above adapter
    })

    it('should calculate positions for multiple adapters', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const pos1 = calculateGhostAdapterPosition(0, edgeNodePos)
      const pos2 = calculateGhostAdapterPosition(1, edgeNodePos)
      const pos3 = calculateGhostAdapterPosition(2, edgeNodePos)

      // Positions should vary as adapters are added
      expect(pos1.adapterPos).toBeDefined()
      expect(pos2.adapterPos).toBeDefined()
      expect(pos3.adapterPos).toBeDefined()

      // Y positions should match (same row until MAX_ADAPTERS)
      expect(pos1.adapterPos.y).toBe(pos2.adapterPos.y)
    })

    it('should handle multiple rows when exceeding MAX_ADAPTERS', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const pos = calculateGhostAdapterPosition(10, edgeNodePos) // More than MAX_ADAPTERS (6)

      expect(pos.adapterPos.y).toBeLessThan(edgeNodePos.y) // Should be in a different row
    })
  })

  describe('createGhostAdapterGroup', () => {
    it('should create adapter with device node and edges', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostAdapterGroup('test-id', 0, edgeNode)

      expect(group.nodes).toHaveLength(2) // Adapter + Device
      expect(group.edges).toHaveLength(2) // Adapter->Edge + Device->Adapter
    })

    it('should use custom label', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostAdapterGroup('test-id', 0, edgeNode, 'Custom Adapter')

      const adapterNode = group.nodes.find((n) => n.type === NodeTypes.ADAPTER_NODE)
      expect(adapterNode?.data.label).toBe('Custom Adapter')
    })

    it('should create edges with correct source and target', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostAdapterGroup('test-id', 0, edgeNode)

      const edgeToEdge = group.edges.find((e) => e.target === IdStubs.EDGE_NODE)
      expect(edgeToEdge).toBeDefined()
      expect(edgeToEdge?.source).toBe('ghost-adapter-test-id')

      const edgeToDevice = group.edges.find((e) => e.source === 'ghost-adapter-test-id')
      console.log('XXXXXX edge, grou', group.edges)
      expect(edgeToDevice).toBeDefined()
      expect(edgeToDevice?.target).toBe(IdStubs.EDGE_NODE)
    })

    it('should set ghost edges to animated', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostAdapterGroup('test-id', 0, edgeNode)

      group.edges.forEach((edge) => {
        expect(edge.animated).toBe(true)
        expect(edge.data?.isGhost).toBe(true)
      })
    })
  })

  describe('calculateGhostBridgePosition', () => {
    it('should calculate position for first bridge', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const { bridgePos, hostPos } = calculateGhostBridgePosition(0, edgeNodePos)

      expect(bridgePos).toBeDefined()
      expect(hostPos).toBeDefined()
      expect(hostPos.y).toBeGreaterThan(bridgePos.y) // Host is below bridge
    })

    it('should calculate centered positions for multiple bridges', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const pos1 = calculateGhostBridgePosition(0, edgeNodePos)
      const pos2 = calculateGhostBridgePosition(1, edgeNodePos)

      expect(pos1.bridgePos.x).not.toBe(pos2.bridgePos.x)
    })

    it('should maintain 250px vertical spacing between bridge and host', () => {
      const edgeNodePos = { x: 500, y: 300 }
      const { bridgePos, hostPos } = calculateGhostBridgePosition(0, edgeNodePos)

      expect(hostPos.y - bridgePos.y).toBe(250)
    })
  })

  describe('createGhostBridgeGroup', () => {
    it('should create bridge with host node and edges', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostBridgeGroup('test-id', 0, edgeNode)

      expect(group.nodes).toHaveLength(2) // Bridge + Host
      expect(group.edges).toHaveLength(2) // Bridge->Edge + Bridge->Host
    })

    it('should use custom label', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostBridgeGroup('test-id', 0, edgeNode, 'Custom Bridge')

      const bridgeNode = group.nodes.find((n) => n.type === NodeTypes.BRIDGE_NODE)
      expect(bridgeNode?.data.label).toBe('Custom Bridge')
    })

    it('should create edges with correct source and target', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostBridgeGroup('test-id', 0, edgeNode)

      const edgeToEdge = group.edges.find((e) => e.target === IdStubs.EDGE_NODE)
      expect(edgeToEdge).toBeDefined()
      expect(edgeToEdge?.source).toBe('ghost-bridge-test-id')

      const edgeToHost = group.edges.find((e) => e.target === 'ghost-host-test-id')
      expect(edgeToHost).toBeDefined()
      expect(edgeToHost?.source).toBe('ghost-bridge-test-id')
    })

    it('should set correct handles for edges', () => {
      const edgeNode = { id: 'EDGE_NODE', position: { x: 500, y: 300 } } as Node
      const group = createGhostBridgeGroup('test-id', 0, edgeNode)

      const edgeToEdge = group.edges.find((e) => e.target === IdStubs.EDGE_NODE)
      expect(edgeToEdge).toBeDefined()
      expect(edgeToEdge?.targetHandle).toBe('Bottom')

      const edgeToHost = group.edges.find((e) => e.target === 'ghost-host-test-id')
      expect(edgeToHost).toBeDefined()
      expect(edgeToHost?.sourceHandle).toBe('Bottom')
    })
  })

  describe('isGhostNode', () => {
    it('should identify ghost nodes', () => {
      const ghostNode = createGhostAdapter('test-id')

      expect(isGhostNode(ghostNode)).toBe(true)
    })

    it('should identify real nodes', () => {
      const realNode = {
        id: 'real-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: false },
      }

      expect(isGhostNode(realNode)).toBe(false)
    })

    it('should handle nodes without data', () => {
      const node = {
        data: undefined,
      }

      expect(isGhostNode(node)).toBe(false)
    })

    it('should handle nodes without isGhost property', () => {
      const node = {
        id: 'node-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: {},
      }

      expect(isGhostNode(node)).toBe(false)
    })
  })

  describe('getGhostNodeIds', () => {
    it('should extract ghost node IDs', () => {
      const ghost1 = createGhostAdapter('test-1')
      const ghost2 = createGhostBridge('test-2')
      const realNode = {
        id: 'real-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: false },
      }

      const nodes = [ghost1, realNode, ghost2]
      const ghostIds = getGhostNodeIds(nodes)

      expect(ghostIds).toHaveLength(2)
      expect(ghostIds).toContain(ghost1.id)
      expect(ghostIds).toContain(ghost2.id)
      expect(ghostIds).not.toContain(realNode.id)
    })

    it('should return empty array for no ghost nodes', () => {
      const realNode = {
        id: 'real-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: false },
      }

      const ghostIds = getGhostNodeIds([realNode])

      expect(ghostIds).toEqual([])
    })
  })

  describe('removeGhostNodes', () => {
    it('should remove ghost nodes from list', () => {
      const ghost1 = createGhostAdapter('test-1')
      const ghost2 = createGhostBridge('test-2')
      const realNode = {
        id: 'real-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: false },
      }

      const nodes = [ghost1, realNode, ghost2]
      const filteredNodes = removeGhostNodes(nodes)

      expect(filteredNodes).toHaveLength(1)
      expect(filteredNodes[0]).toEqual(realNode)
    })

    it('should return all nodes if none are ghosts', () => {
      const realNode1 = {
        id: 'real-1',
        type: 'ADAPTER_NODE',
        position: { x: 0, y: 0 },
        data: { isGhost: false },
      }
      const realNode2 = {
        id: 'real-2',
        type: 'BRIDGE_NODE',
        position: { x: 100, y: 100 },
        data: { isGhost: false },
      }

      const nodes = [realNode1, realNode2]
      const filteredNodes = removeGhostNodes(nodes)

      expect(filteredNodes).toEqual(nodes)
    })
  })

  describe('removeGhostEdges', () => {
    it('should remove ghost edges from list', () => {
      const ghostEdge = {
        id: 'ghost-edge-1',
        source: 'ghost-1',
        target: 'EDGE_NODE',
        data: { isGhost: true },
      }
      const realEdge = {
        id: 'edge-1',
        source: 'node-1',
        target: 'node-2',
        data: { isGhost: false },
      }

      const edges = [ghostEdge, realEdge]
      const filteredEdges = removeGhostEdges(edges)

      expect(filteredEdges).toHaveLength(1)
      expect(filteredEdges[0]).toEqual(realEdge)
    })

    it('should handle edges without data', () => {
      const edge = {
        id: 'edge-1',
        source: 'node-1',
        target: 'node-2',
      }

      const filteredEdges = removeGhostEdges([edge])

      expect(filteredEdges).toHaveLength(1)
      expect(filteredEdges[0]).toEqual(edge)
    })
  })

  describe('ghost node styling', () => {
    it('should have ghost styling for non-selectable ghosts', () => {
      const ghost = createGhostAdapter('test-id')

      expect(ghost.style).toBeDefined()
      expect(ghost.style?.opacity).toBeLessThan(1)
    })

    it('should have selectable styling for combiner ghost', () => {
      const ghost = createGhostCombiner('test-id', mockEdgeNode)

      expect(ghost.selectable).toBe(true)
      expect(ghost.style).toBeDefined()
    })
  })

  describe('isGhostEdge', () => {
    it('should identify ghost edges', () => {
      const ghostEdge: Edge = {
        id: 'edge-1',
        source: 'ghost-1',
        target: 'node-2',
        data: { isGhost: true },
      }

      expect(isGhostEdge(ghostEdge)).toBe(true)
    })

    it('should identify real edges', () => {
      const realEdge: Edge = {
        id: 'edge-1',
        source: 'node-1',
        target: 'node-2',
      }

      expect(isGhostEdge(realEdge)).toBe(false)
    })

    it('should handle edges without data', () => {
      const edge: Edge = {
        id: 'edge-1',
        source: 'node-1',
        target: 'node-2',
      }

      expect(isGhostEdge(edge)).toBe(false)
    })
  })
})
