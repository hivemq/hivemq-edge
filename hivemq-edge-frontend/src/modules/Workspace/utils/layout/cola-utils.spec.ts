import { describe, it, expect } from 'vitest'
import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'

import type { LayoutConstraints } from '../../types/layout'
import {
  DEFAULT_NODE_WIDTH,
  DEFAULT_NODE_HEIGHT,
  filterLayoutableNodes,
  createNodeIndexMap,
  nodesToColaNodes,
  edgesToColaLinks,
  applyGluedNodePositions,
  colaNodestoNodes,
  createValidationResult,
} from './cola-utils'

describe('cola-utils', () => {
  describe('constants', () => {
    it('should have correct default dimensions', () => {
      expect(DEFAULT_NODE_WIDTH).toBe(245)
      expect(DEFAULT_NODE_HEIGHT).toBe(100)
    })
  })

  describe('filterLayoutableNodes', () => {
    it('should return all nodes when no constraints provided', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'adapter' },
      ]

      const result = filterLayoutableNodes(nodes)

      expect(result).toEqual(nodes)
      expect(result.length).toBe(2)
    })

    it('should return all nodes when constraints have no glued nodes', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'adapter' },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map(),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = filterLayoutableNodes(nodes, constraints)

      expect(result).toEqual(nodes)
      expect(result.length).toBe(2)
    })

    it('should filter out glued nodes', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'adapter' },
        { id: 'node-3', position: { x: 200, y: 200 }, data: {}, type: 'adapter' },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['node-2', { parentId: 'node-1', offset: { x: 10, y: 10 }, handle: 'source' }]]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = filterLayoutableNodes(nodes, constraints)

      expect(result.length).toBe(2)
      expect(result.find((n) => n.id === 'node-1')).toBeDefined()
      expect(result.find((n) => n.id === 'node-2')).toBeUndefined()
      expect(result.find((n) => n.id === 'node-3')).toBeDefined()
    })

    it('should filter out multiple glued nodes', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'adapter' },
        { id: 'node-3', position: { x: 200, y: 200 }, data: {}, type: 'adapter' },
        { id: 'node-4', position: { x: 300, y: 300 }, data: {}, type: 'adapter' },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([
          ['node-2', { parentId: 'node-1', offset: { x: 10, y: 10 }, handle: 'source' }],
          ['node-4', { parentId: 'node-3', offset: { x: 20, y: 20 }, handle: 'source' }],
        ]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = filterLayoutableNodes(nodes, constraints)

      expect(result.length).toBe(2)
      expect(result.map((n) => n.id)).toEqual(['node-1', 'node-3'])
    })
  })

  describe('createNodeIndexMap', () => {
    it('should create a map of node IDs to indices', () => {
      const nodes: Node[] = [
        { id: 'node-a', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-b', position: { x: 100, y: 100 }, data: {} },
        { id: 'node-c', position: { x: 200, y: 200 }, data: {} },
      ]

      const result = createNodeIndexMap(nodes)

      expect(result.size).toBe(3)
      expect(result.get('node-a')).toBe(0)
      expect(result.get('node-b')).toBe(1)
      expect(result.get('node-c')).toBe(2)
    })

    it('should handle empty array', () => {
      const result = createNodeIndexMap([])

      expect(result.size).toBe(0)
    })

    it('should handle single node', () => {
      const nodes: Node[] = [{ id: 'only-node', position: { x: 0, y: 0 }, data: {} }]

      const result = createNodeIndexMap(nodes)

      expect(result.size).toBe(1)
      expect(result.get('only-node')).toBe(0)
    })

    it('should maintain correct indices for many nodes', () => {
      const nodes: Node[] = Array.from({ length: 10 }, (_, i) => ({
        id: `node-${i}`,
        position: { x: i * 100, y: i * 100 },
        data: {},
      }))

      const result = createNodeIndexMap(nodes)

      expect(result.size).toBe(10)
      for (let i = 0; i < 10; i++) {
        expect(result.get(`node-${i}`)).toBe(i)
      }
    })
  })

  describe('nodesToColaNodes', () => {
    it('should convert nodes to WebCola format with default dimensions', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 100, y: 200 }, data: {} },
        { id: 'node-2', position: { x: 300, y: 400 }, data: {} },
      ]

      const result = nodesToColaNodes(nodes)

      expect(result.length).toBe(2)
      expect(result[0]).toEqual({ x: 100, y: 200, width: DEFAULT_NODE_WIDTH, height: DEFAULT_NODE_HEIGHT })
      expect(result[1]).toEqual({ x: 300, y: 400, width: DEFAULT_NODE_WIDTH, height: DEFAULT_NODE_HEIGHT })
    })

    it('should use node width and height when provided', () => {
      const nodes: Node[] = [{ id: 'node-1', position: { x: 100, y: 200 }, data: {}, width: 300, height: 150 }]

      const result = nodesToColaNodes(nodes)

      expect(result[0]).toEqual({ x: 100, y: 200, width: 300, height: 150 })
    })

    it('should use measured dimensions when available', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 100, y: 200 }, data: {}, measured: { width: 250, height: 120 } },
      ]

      const result = nodesToColaNodes(nodes)

      expect(result[0]).toEqual({ x: 100, y: 200, width: 250, height: 120 })
    })

    it('should prefer explicit width/height over measured', () => {
      const nodes: Node[] = [
        {
          id: 'node-1',
          position: { x: 100, y: 200 },
          data: {},
          width: 300,
          height: 150,
          measured: { width: 250, height: 120 },
        },
      ]

      const result = nodesToColaNodes(nodes)

      expect(result[0]).toEqual({ x: 100, y: 200, width: 300, height: 150 })
    })

    it('should handle empty array', () => {
      const result = nodesToColaNodes([])

      expect(result).toEqual([])
    })
  })

  describe('edgesToColaLinks', () => {
    it('should convert edges to WebCola links', () => {
      const edges: Edge[] = [
        { id: 'edge-1', source: 'node-a', target: 'node-b' },
        { id: 'edge-2', source: 'node-b', target: 'node-c' },
      ]

      const nodeIndexMap = new Map([
        ['node-a', 0],
        ['node-b', 1],
        ['node-c', 2],
      ])

      const result = edgesToColaLinks(edges, nodeIndexMap)

      expect(result.length).toBe(2)
      expect(result[0]).toEqual({ source: 0, target: 1 })
      expect(result[1]).toEqual({ source: 1, target: 2 })
    })

    it('should include link distance when provided', () => {
      const edges: Edge[] = [{ id: 'edge-1', source: 'node-a', target: 'node-b' }]

      const nodeIndexMap = new Map([
        ['node-a', 0],
        ['node-b', 1],
      ])

      const result = edgesToColaLinks(edges, nodeIndexMap, 350)

      expect(result[0]).toEqual({ source: 0, target: 1, length: 350 })
    })

    it('should filter out edges with missing source nodes', () => {
      const edges: Edge[] = [
        { id: 'edge-1', source: 'node-a', target: 'node-b' },
        { id: 'edge-2', source: 'node-missing', target: 'node-b' },
      ]

      const nodeIndexMap = new Map([
        ['node-a', 0],
        ['node-b', 1],
      ])

      const result = edgesToColaLinks(edges, nodeIndexMap)

      expect(result.length).toBe(1)
      expect(result[0]).toEqual({ source: 0, target: 1 })
    })

    it('should filter out edges with missing target nodes', () => {
      const edges: Edge[] = [
        { id: 'edge-1', source: 'node-a', target: 'node-b' },
        { id: 'edge-2', source: 'node-a', target: 'node-missing' },
      ]

      const nodeIndexMap = new Map([
        ['node-a', 0],
        ['node-b', 1],
      ])

      const result = edgesToColaLinks(edges, nodeIndexMap)

      expect(result.length).toBe(1)
      expect(result[0]).toEqual({ source: 0, target: 1 })
    })

    it('should handle empty edges array', () => {
      const nodeIndexMap = new Map([['node-a', 0]])

      const result = edgesToColaLinks([], nodeIndexMap)

      expect(result).toEqual([])
    })
  })

  describe('applyGluedNodePositions', () => {
    it('should return layouted nodes when no constraints provided', () => {
      const originalNodes: Node[] = []
      const layoutedNodes: Node[] = [
        { id: 'node-1', position: { x: 100, y: 200 }, data: {}, sourcePosition: Position.Right },
      ]

      const result = applyGluedNodePositions(originalNodes, layoutedNodes)

      expect(result).toEqual(layoutedNodes)
    })

    it('should return layouted nodes when constraints have no glued nodes', () => {
      const originalNodes: Node[] = []
      const layoutedNodes: Node[] = [
        { id: 'node-1', position: { x: 100, y: 200 }, data: {}, sourcePosition: Position.Right },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map(),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = applyGluedNodePositions(originalNodes, layoutedNodes, constraints)

      expect(result).toEqual(layoutedNodes)
    })

    it('should add glued nodes positioned relative to their parents', () => {
      const originalNodes: Node[] = [
        { id: 'parent', position: { x: 0, y: 0 }, data: {} },
        { id: 'glued', position: { x: 0, y: 0 }, data: {} },
      ]

      const layoutedNodes: Node[] = [
        {
          id: 'parent',
          position: { x: 100, y: 200 },
          data: {},
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['glued', { parentId: 'parent', offset: { x: 50, y: 75 }, handle: 'source' }]]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = applyGluedNodePositions(originalNodes, layoutedNodes, constraints)

      expect(result.length).toBe(2)
      expect(result[0].id).toBe('parent')
      expect(result[1].id).toBe('glued')
      expect(result[1].position).toEqual({ x: 150, y: 275 })
      expect(result[1].sourcePosition).toBe(Position.Right)
      expect(result[1].targetPosition).toBe(Position.Left)
    })

    it('should handle multiple glued nodes', () => {
      const originalNodes: Node[] = [
        { id: 'parent', position: { x: 0, y: 0 }, data: {} },
        { id: 'glued-1', position: { x: 0, y: 0 }, data: {} },
        { id: 'glued-2', position: { x: 0, y: 0 }, data: {} },
      ]

      const layoutedNodes: Node[] = [
        {
          id: 'parent',
          position: { x: 100, y: 200 },
          data: {},
          sourcePosition: Position.Bottom,
          targetPosition: Position.Top,
        },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([
          ['glued-1', { parentId: 'parent', offset: { x: 10, y: 20 }, handle: 'source' }],
          ['glued-2', { parentId: 'parent', offset: { x: 30, y: 40 }, handle: 'source' }],
        ]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = applyGluedNodePositions(originalNodes, layoutedNodes, constraints)

      expect(result.length).toBe(3)
      expect(result[1].id).toBe('glued-1')
      expect(result[1].position).toEqual({ x: 110, y: 220 })
      expect(result[2].id).toBe('glued-2')
      expect(result[2].position).toEqual({ x: 130, y: 240 })
    })

    it('should skip glued nodes when parent is not found', () => {
      const originalNodes: Node[] = [{ id: 'orphan-glued', position: { x: 0, y: 0 }, data: {} }]

      const layoutedNodes: Node[] = [{ id: 'other-node', position: { x: 100, y: 200 }, data: {} }]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([
          ['orphan-glued', { parentId: 'missing-parent', offset: { x: 10, y: 20 }, handle: 'source' }],
        ]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = applyGluedNodePositions(originalNodes, layoutedNodes, constraints)

      expect(result.length).toBe(1)
      expect(result[0].id).toBe('other-node')
    })
  })

  describe('colaNodestoNodes', () => {
    it('should convert WebCola nodes back to ReactFlow format', () => {
      const nodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-2', position: { x: 0, y: 0 }, data: {} },
      ]

      const colaNodes = [
        { x: 100, y: 200, width: 245, height: 100 },
        { x: 300, y: 400, width: 245, height: 100 },
      ]

      const result = colaNodestoNodes(nodes, colaNodes)

      expect(result.length).toBe(2)
      expect(result[0].id).toBe('node-1')
      expect(result[0].position).toEqual({ x: 100, y: 200 })
      expect(result[0].sourcePosition).toBe(Position.Right)
      expect(result[0].targetPosition).toBe(Position.Left)
      expect(result[1].id).toBe('node-2')
      expect(result[1].position).toEqual({ x: 300, y: 400 })
    })

    it('should use custom source and target positions', () => {
      const nodes: Node[] = [{ id: 'node-1', position: { x: 0, y: 0 }, data: {} }]

      const colaNodes = [{ x: 100, y: 200, width: 245, height: 100 }]

      const result = colaNodestoNodes(nodes, colaNodes, Position.Bottom, Position.Top)

      expect(result[0].sourcePosition).toBe(Position.Bottom)
      expect(result[0].targetPosition).toBe(Position.Top)
    })

    it('should preserve node properties', () => {
      const nodes: Node[] = [
        {
          id: 'node-1',
          position: { x: 0, y: 0 },
          data: { label: 'Test' },
          type: 'custom',
          width: 300,
        },
      ]

      const colaNodes = [{ x: 100, y: 200, width: 245, height: 100 }]

      const result = colaNodestoNodes(nodes, colaNodes)

      expect(result[0].id).toBe('node-1')
      expect(result[0].data).toEqual({ label: 'Test' })
      expect(result[0].type).toBe('custom')
      expect(result[0].width).toBe(300)
    })

    it('should handle empty arrays', () => {
      const result = colaNodestoNodes([], [])

      expect(result).toEqual([])
    })
  })

  describe('createValidationResult', () => {
    it('should return valid result when no errors', () => {
      const result = createValidationResult([], [])

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
      expect(result.warnings).toBeUndefined()
    })

    it('should return invalid result when errors present', () => {
      const result = createValidationResult(['Error 1', 'Error 2'], [])

      expect(result.valid).toBe(false)
      expect(result.errors).toEqual(['Error 1', 'Error 2'])
      expect(result.warnings).toBeUndefined()
    })

    it('should include warnings when present', () => {
      const result = createValidationResult([], ['Warning 1', 'Warning 2'])

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
      expect(result.warnings).toEqual(['Warning 1', 'Warning 2'])
    })

    it('should include both errors and warnings', () => {
      const result = createValidationResult(['Error 1'], ['Warning 1'])

      expect(result.valid).toBe(false)
      expect(result.errors).toEqual(['Error 1'])
      expect(result.warnings).toEqual(['Warning 1'])
    })

    it('should handle single error', () => {
      const result = createValidationResult(['Single error'], [])

      expect(result.valid).toBe(false)
      expect(result.errors).toEqual(['Single error'])
    })

    it('should handle single warning', () => {
      const result = createValidationResult([], ['Single warning'])

      expect(result.valid).toBe(true)
      expect(result.warnings).toEqual(['Single warning'])
    })
  })
})
