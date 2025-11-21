import { describe, it, expect } from 'vitest'
import type { Node } from '@xyflow/react'
import {
  extractLayoutConstraints,
  isNodeConstrained,
  getGluedParentId,
  calculateGluedPosition,
  getLayoutableNodes,
  applyGluedPositions,
} from './constraint-utils'
import type { LayoutConstraints, GluedNodeInfo } from '@/modules/Workspace/types/layout'
import { NodeTypes } from '@/modules/Workspace/types'

describe('constraint-utils', () => {
  describe('extractLayoutConstraints', () => {
    describe('glued node detection', () => {
      it('should detect DEVICE_NODE as glued child of ADAPTER_NODE', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: { sourceAdapterId: 'adapter-1' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.size).toBe(1)
        expect(result.gluedNodes.has('device-1')).toBe(true)
        expect(result.gluedNodes.get('device-1')?.parentId).toBe('adapter-1')
      })

      it('should detect HOST_NODE as glued child of BRIDGE_NODE', () => {
        const nodes: Node[] = [
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'bridge-1' },
          },
          {
            id: 'host-1',
            type: NodeTypes.HOST_NODE,
            position: { x: 200, y: 0 },
            data: {},
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.size).toBe(1)
        expect(result.gluedNodes.has('host-1')).toBe(true)
        expect(result.gluedNodes.get('host-1')?.parentId).toBe('bridge-1')
      })

      it('should NOT detect ADAPTER_NODE as glued child (it is the parent)', () => {
        const nodes: Node[] = [
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: {},
          },
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        // ADAPTER has negative offset (-200), so it's NOT a glued child
        expect(result.gluedNodes.has('adapter-1')).toBe(false)
      })

      it('should NOT detect BRIDGE_NODE as glued child (it is the parent)', () => {
        const nodes: Node[] = [
          {
            id: 'host-1',
            type: NodeTypes.HOST_NODE,
            position: { x: 200, y: 0 },
            data: {},
          },
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'bridge-1' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        // BRIDGE has negative offset, so it's NOT a glued child
        expect(result.gluedNodes.has('bridge-1')).toBe(false)
      })

      it('should match DEVICE to specific ADAPTER via sourceAdapterId', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'adapter-2',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 100 },
            data: { id: 'adapter-2' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 100 },
            data: { sourceAdapterId: 'adapter-2' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.get('device-1')?.parentId).toBe('adapter-2')
      })

      it('should use fallback parent search when sourceAdapterId is missing', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: {}, // No sourceAdapterId
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        // Should fall back to first ADAPTER found
        expect(result.gluedNodes.get('device-1')?.parentId).toBe('adapter-1')
      })

      it('should NOT create glued node if parent is not found', () => {
        const nodes: Node[] = [
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: { sourceAdapterId: 'non-existent-adapter' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        // No parent found, so device should not be in gluedNodes
        expect(result.gluedNodes.has('device-1')).toBe(false)
      })

      it('should set correct offset and handle for glued nodes', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: { sourceAdapterId: 'adapter-1' },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        const gluedInfo = result.gluedNodes.get('device-1')
        expect(gluedInfo?.offset.x).toBe(200) // GLUE_SEPARATOR value
        expect(gluedInfo?.offset.y).toBe(200)
        expect(gluedInfo?.handle).toBe('source')
      })

      it('should handle multiple glued nodes', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: { sourceAdapterId: 'adapter-1' },
          },
          {
            id: 'bridge-1',
            type: NodeTypes.BRIDGE_NODE,
            position: { x: 0, y: 300 },
            data: { id: 'bridge-1' },
          },
          {
            id: 'host-1',
            type: NodeTypes.HOST_NODE,
            position: { x: 200, y: 300 },
            data: {},
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.size).toBe(2)
        expect(result.gluedNodes.has('device-1')).toBe(true)
        expect(result.gluedNodes.has('host-1')).toBe(true)
      })

      it('should skip nodes without type', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'no-type-node',
            // No type property
            position: { x: 200, y: 0 },
            data: {},
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.has('no-type-node')).toBe(false)
      })

      it('should skip nodes with unknown type not in gluedNodeDefinition', () => {
        const nodes: Node[] = [
          {
            id: 'unknown-node',
            type: 'UNKNOWN_TYPE' as NodeTypes,
            position: { x: 0, y: 0 },
            data: {},
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.gluedNodes.size).toBe(0)
      })
    })

    describe('group node detection', () => {
      it('should detect CLUSTER_NODE with children', () => {
        const nodes: Node[] = [
          {
            id: 'cluster-1',
            type: NodeTypes.CLUSTER_NODE,
            position: { x: 0, y: 0 },
            data: { childrenNodeIds: ['node-1', 'node-2'] },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.groupNodes.size).toBe(1)
        expect(result.groupNodes.get('cluster-1')).toEqual(['node-1', 'node-2'])
      })

      it('should skip CLUSTER_NODE without childrenNodeIds', () => {
        const nodes: Node[] = [
          {
            id: 'cluster-1',
            type: NodeTypes.CLUSTER_NODE,
            position: { x: 0, y: 0 },
            data: {},
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result.groupNodes.size).toBe(0)
      })
    })

    describe('return structure', () => {
      it('should return empty constraints for empty nodes', () => {
        const result = extractLayoutConstraints([], [])

        expect(result.gluedNodes.size).toBe(0)
        expect(result.fixedNodes.size).toBe(0)
        expect(result.groupNodes.size).toBe(0)
      })

      it('should return all constraint types', () => {
        const nodes: Node[] = [
          {
            id: 'adapter-1',
            type: NodeTypes.ADAPTER_NODE,
            position: { x: 0, y: 0 },
            data: { id: 'adapter-1' },
          },
          {
            id: 'device-1',
            type: NodeTypes.DEVICE_NODE,
            position: { x: 200, y: 0 },
            data: { sourceAdapterId: 'adapter-1' },
          },
          {
            id: 'cluster-1',
            type: NodeTypes.CLUSTER_NODE,
            position: { x: 0, y: 300 },
            data: { childrenNodeIds: ['node-x'] },
          },
        ]

        const result = extractLayoutConstraints(nodes, [])

        expect(result).toHaveProperty('gluedNodes')
        expect(result).toHaveProperty('fixedNodes')
        expect(result).toHaveProperty('groupNodes')
        expect(result.gluedNodes).toBeInstanceOf(Map)
        expect(result.fixedNodes).toBeInstanceOf(Set)
        expect(result.groupNodes).toBeInstanceOf(Map)
      })
    })
  })

  describe('isNodeConstrained', () => {
    it('should return true for glued nodes', () => {
      const gluedInfo: GluedNodeInfo = { parentId: 'parent-1', offset: { x: 0, y: 0 }, handle: 'source' }
      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['node-1', gluedInfo]]),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      expect(isNodeConstrained('node-1', constraints)).toBe(true)
    })

    it('should return true for fixed nodes', () => {
      const constraints: LayoutConstraints = {
        gluedNodes: new Map(),
        fixedNodes: new Set(['node-1']),
        groupNodes: new Map(),
      }

      expect(isNodeConstrained('node-1', constraints)).toBe(true)
    })
  })

  describe('getGluedParentId', () => {
    it('should return parent ID for glued nodes', () => {
      const gluedInfo: GluedNodeInfo = { parentId: 'parent-1', offset: { x: 10, y: 20 }, handle: 'source' }
      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['child-1', gluedInfo]]),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      expect(getGluedParentId('child-1', constraints)).toBe('parent-1')
    })

    it('should return undefined for non-glued nodes', () => {
      const constraints: LayoutConstraints = {
        gluedNodes: new Map(),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      expect(getGluedParentId('node-1', constraints)).toBeUndefined()
    })
  })

  describe('calculateGluedPosition', () => {
    it('should calculate position with offset', () => {
      const gluedNode: Node = {
        id: 'child-1',
        position: { x: 0, y: 0 },
        data: {},
      }

      const parentNode: Node = {
        id: 'parent-1',
        position: { x: 100, y: 200 },
        data: {},
      }

      const gluedInfo: GluedNodeInfo = {
        parentId: 'parent-1',
        offset: { x: 15, y: 25 },
        handle: 'source',
      }

      const result = calculateGluedPosition(gluedNode, parentNode, gluedInfo)

      expect(result.position).toEqual({ x: 115, y: 225 })
      expect(result.id).toBe('child-1')
    })
  })

  describe('getLayoutableNodes', () => {
    it('should filter out glued nodes', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {} },
        { id: 'node-3', position: { x: 200, y: 200 }, data: {} },
      ]

      const gluedInfo: GluedNodeInfo = { parentId: 'node-1', offset: { x: 0, y: 0 }, handle: 'source' }
      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['node-2', gluedInfo]]),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      const result = getLayoutableNodes(nodes, constraints)

      expect(result).toHaveLength(2)
      expect(result.map((n) => n.id)).toEqual(['node-1', 'node-3'])
    })

    it('should filter out fixed nodes', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {} },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map(),
        fixedNodes: new Set(['node-1']),
        groupNodes: new Map(),
      }

      const result = getLayoutableNodes(nodes, constraints)

      expect(result).toHaveLength(1)
      expect(result[0].id).toBe('node-2')
    })
  })

  describe('applyGluedPositions', () => {
    it('should update glued node positions based on parent', () => {
      const layoutedNodes: Node[] = [
        { id: 'parent-1', position: { x: 200, y: 300 }, data: {} },
        { id: 'child-1', position: { x: 0, y: 0 }, data: {} },
      ]

      const gluedInfo: GluedNodeInfo = { parentId: 'parent-1', offset: { x: 10, y: 20 }, handle: 'source' }
      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['child-1', gluedInfo]]),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      const result = applyGluedPositions(layoutedNodes, constraints)

      expect(result[0].position).toEqual({ x: 200, y: 300 }) // Parent unchanged
      expect(result[1].position).toEqual({ x: 210, y: 320 }) // Child updated with offset
    })

    it('should handle missing parent gracefully', () => {
      const layoutedNodes: Node[] = [{ id: 'child-1', position: { x: 0, y: 0 }, data: {} }]

      const gluedInfo: GluedNodeInfo = { parentId: 'missing-parent', offset: { x: 10, y: 20 }, handle: 'source' }
      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['child-1', gluedInfo]]),
        fixedNodes: new Set<string>(),
        groupNodes: new Map(),
      }

      const result = applyGluedPositions(layoutedNodes, constraints)

      // Should return node unchanged when parent is missing
      expect(result[0].position).toEqual({ x: 0, y: 0 })
    })
  })
})
