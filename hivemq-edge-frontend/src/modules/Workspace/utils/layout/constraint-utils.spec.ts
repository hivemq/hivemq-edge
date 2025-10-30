import { describe, it, expect } from 'vitest'
import type { Node } from '@xyflow/react'
import {
  isNodeConstrained,
  getGluedParentId,
  calculateGluedPosition,
  getLayoutableNodes,
  applyGluedPositions,
} from './constraint-utils'
import type { LayoutConstraints, GluedNodeInfo } from '@/modules/Workspace/types/layout'

describe('constraint-utils', () => {
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
