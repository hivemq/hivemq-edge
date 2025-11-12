/**
 * Unit tests for phantomNodeFactory
 *
 * Tests phantom node creation for wizard mode, which provides temporary data structures
 * before actual API entity creation.
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import type { Node } from '@xyflow/react'

import { createPhantomCombinerNode } from './phantomNodeFactory'
import { EntityType } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'

// Mock the workspace store
vi.mock('@/modules/Workspace/hooks/useWorkspaceStore', () => ({
  default: {
    getState: vi.fn(),
  },
}))

describe('phantomNodeFactory', () => {
  const mockAdapterNode: Node = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 100, y: 100 },
    data: { id: 'adapter-1', name: 'Test Adapter' },
  }

  const mockBridgeNode: Node = {
    id: 'bridge-1',
    type: NodeTypes.BRIDGE_NODE,
    position: { x: 200, y: 200 },
    data: { id: 'bridge-1', name: 'Test Bridge' },
  }

  const mockEdgeNode: Node = {
    id: 'edge',
    type: NodeTypes.EDGE_NODE,
    position: { x: 500, y: 300 },
    data: { id: 'edge', name: 'Edge Node' },
  }

  const mockPulseNode: Node = {
    id: 'pulse-1',
    type: NodeTypes.PULSE_NODE,
    position: { x: 300, y: 300 },
    data: { id: 'pulse-1', name: 'Pulse Agent' },
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('createPhantomCombinerNode', () => {
    it('should create a phantom combiner node with selected node IDs', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode, mockBridgeNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const selectedNodeIds = ['adapter-1', 'bridge-1']
      const phantomNode = createPhantomCombinerNode(selectedNodeIds)

      expect(phantomNode).toBeDefined()
      expect(phantomNode.id).toBe('phantom-combiner-wizard')
      expect(phantomNode.type).toBe(NodeTypes.COMBINER_NODE)
    })

    it('should use default name "New Combiner" when no name provided', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.data.name).toBe('New Combiner')
    })

    it('should use custom name when provided', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const customName = 'My Custom Combiner'
      const phantomNode = createPhantomCombinerNode(['adapter-1'], customName)

      expect(phantomNode.data.name).toBe(customName)
    })

    it('should create source entities from selected node IDs', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode, mockBridgeNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const selectedNodeIds = ['adapter-1', 'bridge-1']
      const phantomNode = createPhantomCombinerNode(selectedNodeIds)

      expect(phantomNode.data.sources).toBeDefined()
      expect(phantomNode.data.sources.items).toHaveLength(2)
      expect(phantomNode.data.sources.items[0]).toEqual({
        id: 'adapter-1',
        type: EntityType.ADAPTER,
      })
      expect(phantomNode.data.sources.items[1]).toEqual({
        id: 'bridge-1',
        type: EntityType.BRIDGE,
      })
    })

    it('should initialize with empty mappings', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.data.mappings).toBeDefined()
      expect(phantomNode.data.mappings.items).toEqual([])
    })

    it('should initialize with empty description', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.data.description).toBe('')
    })

    it('should set position to 0,0 as it is not rendered', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.position).toEqual({ x: 0, y: 0 })
    })

    it('should set selected and dragging to false', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.selected).toBe(false)
      expect(phantomNode.dragging).toBe(false)
    })

    it('should handle multiple adapters', () => {
      const mockAdapterNode2: Node = {
        id: 'adapter-2',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 150, y: 150 },
        data: { id: 'adapter-2', name: 'Test Adapter 2' },
      }

      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode, mockAdapterNode2],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const selectedNodeIds = ['adapter-1', 'adapter-2']
      const phantomNode = createPhantomCombinerNode(selectedNodeIds)

      expect(phantomNode.data.sources.items).toHaveLength(2)
      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.ADAPTER)
      expect(phantomNode.data.sources.items[1].type).toBe(EntityType.ADAPTER)
    })

    it('should handle mixed node types', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode, mockBridgeNode, mockPulseNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const selectedNodeIds = ['adapter-1', 'bridge-1', 'pulse-1']
      const phantomNode = createPhantomCombinerNode(selectedNodeIds)

      expect(phantomNode.data.sources.items).toHaveLength(3)
      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.ADAPTER)
      expect(phantomNode.data.sources.items[1].type).toBe(EntityType.BRIDGE)
      expect(phantomNode.data.sources.items[2].type).toBe(EntityType.PULSE_AGENT)
    })

    it('should handle empty selected node IDs', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [],
      } as unknown as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode([])

      expect(phantomNode.data.sources.items).toEqual([])
    })

    it('should handle node ID not found in workspace', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const selectedNodeIds = ['non-existent-id']
      const phantomNode = createPhantomCombinerNode(selectedNodeIds)

      // Should default to EDGE_BROKER when node not found
      expect(phantomNode.data.sources.items).toHaveLength(1)
      expect(phantomNode.data.sources.items[0]).toEqual({
        id: 'non-existent-id',
        type: EntityType.EDGE_BROKER,
      })
    })

    it('should have correct combiner data structure', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'], 'Test Combiner')

      expect(phantomNode.data).toEqual({
        id: 'phantom-combiner-wizard',
        name: 'Test Combiner',
        description: '',
        sources: {
          items: [
            {
              id: 'adapter-1',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
    })
  })

  describe('inferEntityTypeFromNode (via createPhantomCombinerNode)', () => {
    it('should infer ADAPTER type from ADAPTER_NODE', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.ADAPTER)
    })

    it('should infer BRIDGE type from BRIDGE_NODE', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockBridgeNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['bridge-1'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.BRIDGE)
    })

    it('should infer EDGE_BROKER type from EDGE_NODE', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockEdgeNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['edge'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.EDGE_BROKER)
    })

    it('should infer PULSE_AGENT type from PULSE_NODE', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockPulseNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['pulse-1'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.PULSE_AGENT)
    })

    it('should default to EDGE_BROKER for unknown node type', () => {
      const unknownNode: Node = {
        id: 'unknown-1',
        type: 'UNKNOWN_TYPE',
        position: { x: 0, y: 0 },
        data: {},
      }

      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [unknownNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['unknown-1'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.EDGE_BROKER)
    })

    it('should default to EDGE_BROKER for undefined node', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [],
      } as unknown as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['non-existent'])

      expect(phantomNode.data.sources.items[0].type).toBe(EntityType.EDGE_BROKER)
    })
  })

  describe('phantom node usage for wizard', () => {
    it('should create phantom node suitable for CombinerMappingManager', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode, mockBridgeNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode = createPhantomCombinerNode(['adapter-1', 'bridge-1'], 'Wizard Combiner')

      // Verify it has all required properties for CombinerMappingManager
      expect(phantomNode.data.id).toBeDefined()
      expect(phantomNode.data.name).toBeDefined()
      expect(phantomNode.data.description).toBeDefined()
      expect(phantomNode.data.sources).toBeDefined()
      expect(phantomNode.data.sources.items).toBeDefined()
      expect(phantomNode.data.mappings).toBeDefined()
      expect(phantomNode.data.mappings.items).toBeDefined()
    })

    it('should create consistent phantom ID for wizard mode', () => {
      vi.mocked(useWorkspaceStore.getState).mockReturnValue({
        nodes: [mockAdapterNode],
      } as ReturnType<typeof useWorkspaceStore.getState>)

      const phantomNode1 = createPhantomCombinerNode(['adapter-1'])
      const phantomNode2 = createPhantomCombinerNode(['adapter-1'])

      // Both should have the same phantom ID
      expect(phantomNode1.id).toBe('phantom-combiner-wizard')
      expect(phantomNode2.id).toBe('phantom-combiner-wizard')
      expect(phantomNode1.data.id).toBe('phantom-combiner-wizard')
      expect(phantomNode2.data.id).toBe('phantom-combiner-wizard')
    })
  })
})
