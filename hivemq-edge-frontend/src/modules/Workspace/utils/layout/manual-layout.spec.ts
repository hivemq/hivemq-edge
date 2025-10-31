import { describe, expect, it } from 'vitest'
import type { Node, Edge } from '@xyflow/react'
import { ManualLayoutAlgorithm } from './manual-layout'
import { LayoutType } from '../../types/layout'

describe('ManualLayoutAlgorithm', () => {
  const algorithm = new ManualLayoutAlgorithm()

  describe('metadata', () => {
    it('should have correct type', () => {
      expect(algorithm.type).toBe(LayoutType.MANUAL)
    })

    it('should have correct name', () => {
      expect(algorithm.name).toBe('Manual')
    })

    it('should have correct description', () => {
      expect(algorithm.description).toBe('Manual positioning - nodes stay exactly where they are')
    })
  })

  describe('defaultOptions', () => {
    it('should have correct default options', () => {
      expect(algorithm.defaultOptions).toEqual({
        animate: false,
        animationDuration: 0,
        fitView: false,
      })
    })
  })

  describe('validateOptions', () => {
    it('should always return valid for any options', () => {
      const result1 = algorithm.validateOptions({})
      const result2 = algorithm.validateOptions({ anything: 'goes' } as never)
      const result3 = algorithm.validateOptions({ foo: 123, bar: true } as never)

      expect(result1.valid).toBe(true)
      expect(result2.valid).toBe(true)
      expect(result3.valid).toBe(true)
    })

    it('should handle undefined options', () => {
      const result = algorithm.validateOptions(undefined as unknown as Record<string, unknown>)
      expect(result.valid).toBe(true)
    })

    it('should handle null options', () => {
      const result = algorithm.validateOptions(null as unknown as Record<string, unknown>)
      expect(result.valid).toBe(true)
    })
  })

  describe('apply', () => {
    it('should return nodes with unchanged positions', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 100, y: 200 }, data: {} },
        { id: '2', type: 'edge', position: { x: 300, y: 400 }, data: {} },
        { id: '3', type: 'client', position: { x: 500, y: 600 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e2-3', source: '2', target: '3' },
      ]

      const result = await algorithm.apply(nodes, edges, {})

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(3)
      expect(result.nodes[0].position).toEqual({ x: 100, y: 200 })
      expect(result.nodes[1].position).toEqual({ x: 300, y: 400 })
      expect(result.nodes[2].position).toEqual({ x: 500, y: 600 })
    })

    it('should preserve node IDs', async () => {
      const nodes: Node[] = [
        { id: 'node-1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-2', type: 'edge', position: { x: 100, y: 100 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.nodes.map((n) => n.id)).toEqual(['node-1', 'node-2'])
    })

    it('should handle empty node array', async () => {
      const result = await algorithm.apply([], [], {})

      expect(result.success).toBe(true)
      expect(result.nodes).toEqual([])
      expect(result.metadata?.nodeCount).toBe(0)
    })

    it('should handle nodes without edges', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 50, y: 50 }, data: {} },
        { id: '2', type: 'edge', position: { x: 150, y: 150 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(2)
    })

    it('should complete very quickly', async () => {
      const nodes: Node[] = Array.from({ length: 100 }, (_, i) => ({
        id: `node-${i}`,
        type: 'adapter',
        position: { x: i * 10, y: i * 10 },
        data: {},
      }))

      const startTime = performance.now()
      const result = await algorithm.apply(nodes, [], {})
      const duration = performance.now() - startTime

      expect(result.success).toBe(true)
      expect(duration).toBeLessThan(10) // Should be nearly instant
    })

    it('should ignore any options passed', async () => {
      const nodes: Node[] = [{ id: '1', type: 'adapter', position: { x: 10, y: 20 }, data: {} }]

      const result1 = await algorithm.apply(nodes, [], {})
      const result2 = await algorithm.apply(nodes, [], { ranksep: 500, animate: true } as never)

      expect(result1.nodes[0].position).toEqual(result2.nodes[0].position)
    })

    it('should ignore constraints', async () => {
      const nodes: Node[] = [{ id: '1', type: 'adapter', position: { x: 10, y: 20 }, data: {} }]

      const constraints = {
        fixedNodes: new Set(['1']),
        gluedNodes: new Map(),
        groupNodes: new Map(),
      }

      const result = await algorithm.apply(nodes, [], {}, constraints)

      // Should ignore constraint and keep original position
      expect(result.nodes[0].position).toEqual({ x: 10, y: 20 })
    })

    it('should return metadata with node count', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 100, y: 100 }, data: {} },
        { id: '3', type: 'client', position: { x: 200, y: 200 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.metadata?.algorithm).toBe(LayoutType.MANUAL)
      expect(result.metadata?.nodeCount).toBe(3)
      expect(result.metadata?.edgeCount).toBe(0)
    })

    it('should return metadata with edge count', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 100, y: 100 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const result = await algorithm.apply(nodes, edges, {})

      expect(result.metadata?.edgeCount).toBe(1)
    })

    it('should have minimal duration', async () => {
      const nodes: Node[] = [{ id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} }]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.duration).toBeGreaterThanOrEqual(0)
      expect(result.duration).toBeLessThan(5)
    })

    it('should preserve all node properties', async () => {
      const nodes: Node[] = [
        {
          id: 'complex-node',
          type: 'adapter',
          position: { x: 123, y: 456 },
          data: { name: 'Test Adapter', status: 'connected' },
          style: { backgroundColor: 'red' },
          className: 'my-class',
        },
      ]

      const result = await algorithm.apply(nodes, [], {})

      const node = result.nodes[0]
      expect(node.id).toBe('complex-node')
      expect(node.type).toBe('adapter')
      expect(node.position).toEqual({ x: 123, y: 456 })
      expect(node.data).toEqual({ name: 'Test Adapter', status: 'connected' })
      expect(node.style).toEqual({ backgroundColor: 'red' })
      expect(node.className).toBe('my-class')
    })
  })
})
