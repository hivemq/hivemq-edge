/**
 * Ghost Node Factory Tests
 *
 * Tests for ghost node factory functions.
 * Following pragmatic testing strategy: only accessibility test is unskipped.
 */

import { describe, it, expect } from 'vitest'

import {
  createGhostAdapter,
  createGhostBridge,
  createGhostCombiner,
  createGhostAssetMapper,
  createGhostGroup,
  createGhostNodeForType,
  isGhostNode,
  getGhostNodeIds,
  removeGhostNodes,
  GHOST_STYLE,
  GHOST_STYLE_ENHANCED,
  GHOST_EDGE_STYLE,
  createGhostAdapterGroup,
  calculateGhostAdapterPosition,
  isGhostEdge,
  removeGhostEdges,
} from './ghostNodeFactory'
import { EntityType } from '../types'

describe('ghostNodeFactory', () => {
  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    // Ghost nodes should have proper accessibility properties
    const ghostNode = createGhostAdapter('test')

    // Should not be interactive (accessibility feature)
    expect(ghostNode.draggable).toBe(false)
    expect(ghostNode.selectable).toBe(false)
    expect(ghostNode.connectable).toBe(false)

    // Should have isGhost flag for screen readers to identify
    expect(ghostNode.data.isGhost).toBe(true)

    // Should have pointer-events: none for accessibility
    expect(ghostNode.style?.pointerEvents).toBe('none')
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  describe.skip('createGhostAdapter', () => {
    it('should create a ghost adapter node', () => {
      const node = createGhostAdapter('test-id')

      expect(node.id).toBe('ghost-test-id')
      expect(node.type).toBe('ADAPTER_NODE')
      expect(node.data.isGhost).toBe(true)
      expect(node.data.label).toBe('New Adapter')
    })

    it('should accept custom label', () => {
      const node = createGhostAdapter('test-id', 'My Custom Adapter')

      expect(node.data.label).toBe('My Custom Adapter')
    })

    it('should have ghost styling', () => {
      const node = createGhostAdapter('test-id')

      expect(node.style).toEqual(GHOST_STYLE)
    })

    it('should not be interactive', () => {
      const node = createGhostAdapter('test-id')

      expect(node.draggable).toBe(false)
      expect(node.selectable).toBe(false)
      expect(node.connectable).toBe(false)
    })
  })

  describe.skip('createGhostBridge', () => {
    it('should create a ghost bridge node', () => {
      const node = createGhostBridge('test-id')

      expect(node.id).toBe('ghost-test-id')
      expect(node.type).toBe('BRIDGE_NODE')
      expect(node.data.isGhost).toBe(true)
    })
  })

  describe.skip('createGhostCombiner', () => {
    it('should create a ghost combiner node', () => {
      const node = createGhostCombiner('test-id')

      expect(node.id).toBe('ghost-test-id')
      expect(node.type).toBe('COMBINER_NODE')
      expect(node.data.isGhost).toBe(true)
    })
  })

  describe.skip('createGhostAssetMapper', () => {
    it('should create a ghost asset mapper node', () => {
      const node = createGhostAssetMapper('test-id')

      expect(node.id).toBe('ghost-test-id')
      expect(node.type).toBe('PULSE_NODE')
      expect(node.data.isGhost).toBe(true)
    })
  })

  describe.skip('createGhostGroup', () => {
    it('should create a ghost group node', () => {
      const node = createGhostGroup('test-id')

      expect(node.id).toBe('ghost-test-id')
      expect(node.type).toBe('CLUSTER_NODE')
      expect(node.data.isGhost).toBe(true)
    })

    it('should have larger dimensions', () => {
      const node = createGhostGroup('test-id')

      expect(node.style?.width).toBe(300)
      expect(node.style?.height).toBe(200)
    })
  })

  describe.skip('createGhostNodeForType', () => {
    it('should create adapter for ADAPTER type', () => {
      const node = createGhostNodeForType(EntityType.ADAPTER)

      expect(node).not.toBeNull()
      expect(node?.type).toBe('ADAPTER_NODE')
    })

    it('should create bridge for BRIDGE type', () => {
      const node = createGhostNodeForType(EntityType.BRIDGE)

      expect(node).not.toBeNull()
      expect(node?.type).toBe('BRIDGE_NODE')
    })

    it('should create combiner for COMBINER type', () => {
      const node = createGhostNodeForType(EntityType.COMBINER)

      expect(node).not.toBeNull()
      expect(node?.type).toBe('COMBINER_NODE')
    })

    it('should create asset mapper for ASSET_MAPPER type', () => {
      const node = createGhostNodeForType(EntityType.ASSET_MAPPER)

      expect(node).not.toBeNull()
      expect(node?.type).toBe('PULSE_NODE')
    })

    it('should create group for GROUP type', () => {
      const node = createGhostNodeForType(EntityType.GROUP)

      expect(node).not.toBeNull()
      expect(node?.type).toBe('CLUSTER_NODE')
    })

    it('should use custom id', () => {
      const node = createGhostNodeForType(EntityType.ADAPTER, 'custom-id')

      expect(node?.id).toBe('ghost-custom-id')
    })
  })

  describe.skip('isGhostNode', () => {
    it('should return true for ghost nodes', () => {
      const ghostNode = createGhostAdapter('test')

      expect(isGhostNode(ghostNode)).toBe(true)
    })

    it('should return false for regular nodes', () => {
      const regularNode = {
        id: 'regular',
        data: { isGhost: false },
      }

      expect(isGhostNode(regularNode)).toBe(false)
    })

    it('should return false for nodes without isGhost flag', () => {
      const node = {
        id: 'regular',
        data: {},
      }

      expect(isGhostNode(node)).toBe(false)
    })

    it('should return false for nodes without data', () => {
      const node = {
        id: 'regular',
      }

      expect(isGhostNode(node)).toBe(false)
    })
  })

  describe.skip('getGhostNodeIds', () => {
    it('should return IDs of ghost nodes', () => {
      const nodes = [
        createGhostAdapter('1'),
        { id: 'regular-1', data: {} },
        createGhostBridge('2'),
        { id: 'regular-2', data: {} },
      ]

      const ghostIds = getGhostNodeIds(nodes)

      expect(ghostIds).toEqual(['ghost-1', 'ghost-2'])
    })

    it('should return empty array if no ghost nodes', () => {
      const nodes = [
        { id: 'regular-1', data: {} },
        { id: 'regular-2', data: {} },
      ]

      const ghostIds = getGhostNodeIds(nodes)

      expect(ghostIds).toEqual([])
    })
  })

  describe.skip('removeGhostNodes', () => {
    it('should remove ghost nodes from array', () => {
      const nodes = [
        createGhostAdapter('1'),
        { id: 'regular-1', data: {} },
        createGhostBridge('2'),
        { id: 'regular-2', data: {} },
      ]

      const realNodes = removeGhostNodes(nodes)

      expect(realNodes).toHaveLength(2)
      expect(realNodes[0].id).toBe('regular-1')
      expect(realNodes[1].id).toBe('regular-2')
    })

    it('should return all nodes if no ghost nodes', () => {
      const nodes = [
        { id: 'regular-1', data: {} },
        { id: 'regular-2', data: {} },
      ]

      const result = removeGhostNodes(nodes)

      expect(result).toEqual(nodes)
    })

    it('should return empty array if all are ghost nodes', () => {
      const nodes = [createGhostAdapter('1'), createGhostBridge('2')]

      const result = removeGhostNodes(nodes)

      expect(result).toEqual([])
    })
  })

  describe.skip('GHOST_STYLE', () => {
    it('should have semi-transparent opacity', () => {
      expect(GHOST_STYLE.opacity).toBe(0.6)
    })

    it('should have dashed border', () => {
      expect(GHOST_STYLE.border).toContain('dashed')
    })

    it('should have pointer-events none', () => {
      expect(GHOST_STYLE.pointerEvents).toBe('none')
    })

    it('should have light blue color scheme', () => {
      expect(GHOST_STYLE.border).toContain('#4299E1')
      expect(GHOST_STYLE.backgroundColor).toBe('#EBF8FF')
    })
  })

  describe.skip('createGhostAdapterGroup', () => {
    const mockEdgeNode = {
      id: 'EDGE_NODE',
      position: { x: 300, y: 200 },
      type: 'EDGE_NODE',
      data: {},
    }

    it('should create adapter and device nodes with edges', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)

      expect(group.nodes).toHaveLength(2)
      expect(group.edges).toHaveLength(2)
    })

    it('should create adapter node with correct properties', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)
      const adapterNode = group.nodes[0]

      expect(adapterNode.id).toBe('ghost-adapter-test')
      expect(adapterNode.type).toBe('ADAPTER_NODE')
      expect(adapterNode.data.isGhost).toBe(true)
      expect(adapterNode.style).toEqual(GHOST_STYLE_ENHANCED)
    })

    it('should create device node with correct properties', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)
      const deviceNode = group.nodes[1]

      expect(deviceNode.id).toBe('ghost-device-test')
      expect(deviceNode.type).toBe('DEVICE_NODE')
      expect(deviceNode.data.isGhost).toBe(true)
      expect(deviceNode.style).toEqual(GHOST_STYLE_ENHANCED)
    })

    it('should create edge from adapter to edge node', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)
      const edge = group.edges[0]

      expect(edge.id).toBe('ghost-edge-adapter-to-edge-test')
      expect(edge.source).toBe('ghost-adapter-test')
      expect(edge.target).toBe('EDGE_NODE')
      expect(edge.animated).toBe(true)
    })

    it('should create edge from device to adapter', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)
      const edge = group.edges[1]

      expect(edge.id).toBe('ghost-edge-device-to-adapter-test')
      expect(edge.source).toBe('ghost-device-test')
      expect(edge.target).toBe('ghost-adapter-test')
      expect(edge.animated).toBe(true)
    })

    it('should position device above adapter', () => {
      const group = createGhostAdapterGroup('test', 0, mockEdgeNode)
      const adapterNode = group.nodes[0]
      const deviceNode = group.nodes[1]

      expect(deviceNode.position.y).toBeLessThan(adapterNode.position.y)
      expect(deviceNode.position.x).toBe(adapterNode.position.x)
    })
  })

  describe.skip('calculateGhostAdapterPosition', () => {
    const edgeNodePos = { x: 300, y: 200 }

    it('should calculate position for first adapter', () => {
      const { adapterPos, devicePos } = calculateGhostAdapterPosition(0, edgeNodePos)

      expect(adapterPos.x).toBeDefined()
      expect(adapterPos.y).toBeDefined()
      expect(devicePos.x).toBe(adapterPos.x)
      expect(devicePos.y).toBeLessThan(adapterPos.y)
    })

    it('should offset position for multiple adapters', () => {
      const pos0 = calculateGhostAdapterPosition(0, edgeNodePos)
      const pos1 = calculateGhostAdapterPosition(1, edgeNodePos)

      expect(pos1.adapterPos.x).not.toBe(pos0.adapterPos.x)
    })

    it('should handle more than 10 adapters (second row)', () => {
      const pos10 = calculateGhostAdapterPosition(10, edgeNodePos)
      const pos0 = calculateGhostAdapterPosition(0, edgeNodePos)

      expect(pos10.adapterPos.y).not.toBe(pos0.adapterPos.y)
    })

    it('should maintain GLUE_SEPARATOR distance between adapter and device', () => {
      const { adapterPos, devicePos } = calculateGhostAdapterPosition(0, edgeNodePos)
      const GLUE_SEPARATOR = 200

      expect(adapterPos.y - devicePos.y).toBe(GLUE_SEPARATOR)
    })
  })

  describe.skip('isGhostEdge', () => {
    it('should identify ghost edge by id prefix', () => {
      const edge = { id: 'ghost-edge-test', source: 'a', target: 'b' }

      expect(isGhostEdge(edge)).toBe(true)
    })

    it('should identify ghost edge by data flag', () => {
      const edge = { id: 'regular-edge', source: 'a', target: 'b', data: { isGhost: true } }

      expect(isGhostEdge(edge)).toBe(true)
    })

    it('should return false for regular edges', () => {
      const edge = { id: 'regular-edge', source: 'a', target: 'b' }

      expect(isGhostEdge(edge)).toBe(false)
    })
  })

  describe.skip('removeGhostEdges', () => {
    it('should remove ghost edges from array', () => {
      const edges = [
        { id: 'ghost-edge-1', source: 'a', target: 'b' },
        { id: 'regular-edge-1', source: 'c', target: 'd' },
        { id: 'ghost-edge-2', source: 'e', target: 'f', data: { isGhost: true } },
      ]

      const realEdges = removeGhostEdges(edges)

      expect(realEdges).toHaveLength(1)
      expect(realEdges[0].id).toBe('regular-edge-1')
    })
  })

  describe.skip('GHOST_STYLE_ENHANCED', () => {
    it('should have higher opacity than basic style', () => {
      expect(GHOST_STYLE_ENHANCED.opacity).toBeGreaterThan(GHOST_STYLE.opacity)
    })

    it('should have glowing box shadow', () => {
      expect(GHOST_STYLE_ENHANCED.boxShadow).toContain('rgba(66, 153, 225')
    })

    it('should have thicker dashed border', () => {
      expect(GHOST_STYLE_ENHANCED.border).toContain('3px')
    })

    it('should have transition for smooth animations', () => {
      expect(GHOST_STYLE_ENHANCED.transition).toBeDefined()
    })
  })

  describe.skip('GHOST_EDGE_STYLE', () => {
    it('should have blue stroke color', () => {
      expect(GHOST_EDGE_STYLE.stroke).toBe('#4299E1')
    })

    it('should have dashed line pattern', () => {
      expect(GHOST_EDGE_STYLE.strokeDasharray).toBe('5,5')
    })

    it('should have semi-transparent opacity', () => {
      expect(GHOST_EDGE_STYLE.opacity).toBe(0.6)
    })
  })
})
