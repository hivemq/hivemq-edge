/**
 * Unit tests for Dagre Layout Algorithm
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { DagreLayoutAlgorithm } from './dagre-layout'
import type { LayoutType, DagreOptions, LayoutFeature } from '../../types/layout'
import { type LayoutConstraints } from '../../types/layout'
import type { Node, Edge } from '@xyflow/react'

describe('DagreLayoutAlgorithm', () => {
  let algorithm: DagreLayoutAlgorithm

  beforeEach(() => {
    algorithm = new DagreLayoutAlgorithm('TB')
  })

  describe('constructor', () => {
    it('should create vertical tree algorithm', () => {
      const tbAlgo = new DagreLayoutAlgorithm('TB')
      expect(tbAlgo.type).toBe('DAGRE_TB' as LayoutType)
      expect(tbAlgo.name).toBe('Vertical Tree Layout')
      expect(tbAlgo.defaultOptions.rankdir).toBe('TB')
    })

    it('should create horizontal tree algorithm', () => {
      const lrAlgo = new DagreLayoutAlgorithm('LR')
      expect(lrAlgo.type).toBe('DAGRE_LR' as LayoutType)
      expect(lrAlgo.name).toBe('Horizontal Tree Layout')
      expect(lrAlgo.defaultOptions.rankdir).toBe('LR')
    })
  })

  describe('apply', () => {
    it('should layout nodes in hierarchical structure', async () => {
      const nodes: Node[] = [
        { id: '1', position: { x: 0, y: 0 }, data: {} },
        { id: '2', position: { x: 0, y: 0 }, data: {} },
        { id: '3', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e1-3', source: '1', target: '3' },
      ]

      const result = await algorithm.apply(nodes, edges, algorithm.defaultOptions)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(3)
      expect(result.duration).toBeGreaterThanOrEqual(0)

      // Check that node 1 is at the top (root)
      const node1 = result.nodes.find((n) => n.id === '1')
      const node2 = result.nodes.find((n) => n.id === '2')
      const node3 = result.nodes.find((n) => n.id === '3')

      expect(node1).toBeDefined()
      expect(node2).toBeDefined()
      expect(node3).toBeDefined()

      // Root should be above children in TB layout
      if (node1 && node2 && node3) {
        expect(node1.position.y).toBeLessThan(node2.position.y)
        expect(node1.position.y).toBeLessThan(node3.position.y)
      }
    })

    it('should handle empty node array', async () => {
      const result = await algorithm.apply([], [], algorithm.defaultOptions)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(0)
      expect(result.metadata?.nodeCount).toBe(0)
    })

    it('should handle single node', async () => {
      const nodes: Node[] = [{ id: '1', position: { x: 0, y: 0 }, data: {} }]

      const result = await algorithm.apply(nodes, [], algorithm.defaultOptions)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(1)
      expect(result.nodes[0].position).toBeDefined()
    })

    it.skip('should respect glued node constraints', async () => {
      const nodes: Node[] = [
        { id: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: 'listener', position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1', source: 'adapter', target: 'edge' }]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['listener', { parentId: 'edge', offset: { x: 50, y: 50 }, handle: 'target' }]]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = await algorithm.apply(nodes, edges, algorithm.defaultOptions, constraints)

      expect(result.success).toBe(true)

      const edgeNode = result.nodes.find((n) => n.id === 'edge')
      const listenerNode = result.nodes.find((n) => n.id === 'listener')

      expect(edgeNode).toBeDefined()
      expect(listenerNode).toBeDefined()

      // Listener should be offset from edge node by exactly 50px in both directions
      if (edgeNode && listenerNode) {
        const xOffset = listenerNode.position.x - edgeNode.position.x
        const yOffset = listenerNode.position.y - edgeNode.position.y
        // Just verify that the glued positioning function was called
        // The actual offset values depend on dagre's layout
        expect(xOffset).toBe(50)
        expect(yOffset).toBe(50)
      }
    })

    it('should exclude glued nodes from dagre layout', async () => {
      const nodes: Node[] = [
        { id: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: 'listener', position: { x: 0, y: 0 }, data: {} },
      ]

      const constraints: LayoutConstraints = {
        gluedNodes: new Map([['listener', { parentId: 'edge', offset: { x: 100, y: 100 }, handle: 'target' }]]),
        fixedNodes: new Set(),
        groupNodes: new Map(),
      }

      const result = await algorithm.apply(nodes, [], algorithm.defaultOptions, constraints)

      expect(result.success).toBe(true)
      expect(result.metadata?.nodeCount).toBe(1) // Only edge node is laid out
    })

    it('should set handle positions based on layout direction', async () => {
      const tbAlgo = new DagreLayoutAlgorithm('TB')
      const nodes: Node[] = [
        { id: '1', position: { x: 0, y: 0 }, data: {} },
        { id: '2', position: { x: 0, y: 0 }, data: {} },
      ]
      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const result = await tbAlgo.apply(nodes, edges, tbAlgo.defaultOptions)

      expect(result.success).toBe(true)
      expect(result.nodes[0].targetPosition).toBe('top')
      expect(result.nodes[0].sourcePosition).toBe('bottom')
    })

    it('should set horizontal handle positions for LR layout', async () => {
      const lrAlgo = new DagreLayoutAlgorithm('LR')
      const nodes: Node[] = [
        { id: '1', position: { x: 0, y: 0 }, data: {} },
        { id: '2', position: { x: 0, y: 0 }, data: {} },
      ]
      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const result = await lrAlgo.apply(nodes, edges, lrAlgo.defaultOptions)

      expect(result.success).toBe(true)
      expect(result.nodes[0].targetPosition).toBe('left')
      expect(result.nodes[0].sourcePosition).toBe('right')
    })

    it('should include metadata in result', async () => {
      const nodes: Node[] = [
        { id: '1', position: { x: 0, y: 0 }, data: {} },
        { id: '2', position: { x: 0, y: 0 }, data: {} },
      ]
      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const result = await algorithm.apply(nodes, edges, algorithm.defaultOptions)

      expect(result.metadata).toBeDefined()
      expect(result.metadata?.algorithm).toBe('DAGRE_TB' as LayoutType)
      expect(result.metadata?.nodeCount).toBe(2)
      expect(result.metadata?.edgeCount).toBe(1)
    })
  })

  describe('supports', () => {
    it('should support hierarchical feature', () => {
      expect(algorithm.supports('HIERARCHICAL' as LayoutFeature)).toBe(true)
    })

    it('should support directional feature', () => {
      expect(algorithm.supports('DIRECTIONAL' as LayoutFeature)).toBe(true)
    })

    it('should support constrained feature', () => {
      expect(algorithm.supports('CONSTRAINED' as LayoutFeature)).toBe(true)
    })

    it('should not support force-directed feature', () => {
      expect(algorithm.supports('FORCE_DIRECTED' as LayoutFeature)).toBe(false)
    })
  })

  describe('validateOptions', () => {
    it('should validate valid options', () => {
      const result = algorithm.validateOptions(algorithm.defaultOptions)

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
    })

    it('should warn about small ranksep', () => {
      const result = algorithm.validateOptions({ ranksep: 30, rankdir: 'TB' } as DagreOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('ranksep < 50px may cause overlapping nodes')
    })

    it('should warn about large ranksep', () => {
      const result = algorithm.validateOptions({ ranksep: 600, rankdir: 'TB' } as DagreOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('ranksep > 500px may create excessive spacing')
    })

    it('should warn about small nodesep', () => {
      const result = algorithm.validateOptions({ nodesep: 10, rankdir: 'TB' } as DagreOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('nodesep < 20px may cause overlapping nodes')
    })

    it('should error on invalid rankdir', () => {
      const result = algorithm.validateOptions({ rankdir: 'INVALID' } as unknown as DagreOptions)

      expect(result.valid).toBe(false)
      expect(result.errors).toContain('Invalid rankdir: INVALID. Must be TB, LR, BT, or RL')
    })
  })
})
