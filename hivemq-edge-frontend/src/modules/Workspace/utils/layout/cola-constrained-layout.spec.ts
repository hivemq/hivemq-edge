import { describe, expect, it, beforeEach } from 'vitest'
import type { Node, Edge } from '@xyflow/react'
import { ColaConstrainedLayoutAlgorithm } from './cola-constrained-layout'
import { LayoutType, type ColaConstrainedOptions } from '../../types/layout'

describe('ColaConstrainedLayoutAlgorithm', () => {
  let algorithm: ColaConstrainedLayoutAlgorithm

  beforeEach(() => {
    algorithm = new ColaConstrainedLayoutAlgorithm()
  })

  describe('metadata', () => {
    it('should have correct type', () => {
      expect(algorithm.type).toBe(LayoutType.COLA_CONSTRAINED)
    })

    it('should have correct name', () => {
      expect(algorithm.name).toBe('Hierarchical Constraint Layout')
    })

    it('should have correct description', () => {
      expect(algorithm.description).toContain('Strict hierarchical')
      expect(algorithm.description).toContain('constraints')
    })
  })

  describe('defaultOptions', () => {
    it('should have correct default flowDirection', () => {
      expect(algorithm.defaultOptions.flowDirection).toBe('y')
    })

    it('should have correct default layerGap', () => {
      expect(algorithm.defaultOptions.layerGap).toBe(350)
    })

    it('should have correct default nodeGap', () => {
      expect(algorithm.defaultOptions.nodeGap).toBe(300)
    })

    it('should have animation enabled by default', () => {
      expect(algorithm.defaultOptions.animate).toBe(true)
    })

    it('should have correct default animationDuration', () => {
      expect(algorithm.defaultOptions.animationDuration).toBe(300)
    })

    it('should have fitView enabled by default', () => {
      expect(algorithm.defaultOptions.fitView).toBe(true)
    })
  })

  describe('validateOptions', () => {
    it('should validate correct options', () => {
      const options: ColaConstrainedOptions = {
        flowDirection: 'y',
        layerGap: 400,
        nodeGap: 250,
      }
      const result = algorithm.validateOptions(options)

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
    })

    it('should accept flowDirection "y"', () => {
      const result = algorithm.validateOptions({ flowDirection: 'y' } as ColaConstrainedOptions)
      expect(result.valid).toBe(true)
    })

    it('should accept flowDirection "x"', () => {
      const result = algorithm.validateOptions({ flowDirection: 'x' } as ColaConstrainedOptions)
      expect(result.valid).toBe(true)
    })

    it('should warn about small layerGap', () => {
      const result = algorithm.validateOptions({ layerGap: 100 } as ColaConstrainedOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('layerGap < 200px may cause overlapping layers')
    })

    it('should warn about large layerGap', () => {
      const result = algorithm.validateOptions({ layerGap: 900 } as ColaConstrainedOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('layerGap > 800px may cause excessive spacing')
    })

    it('should warn about small nodeGap', () => {
      const result = algorithm.validateOptions({ nodeGap: 100 } as ColaConstrainedOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('nodeGap < 200px may cause overlapping nodes')
    })

    it('should warn about large nodeGap', () => {
      const result = algorithm.validateOptions({ nodeGap: 700 } as ColaConstrainedOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('nodeGap > 600px may cause excessive spacing')
    })

    it('should handle empty options', () => {
      const result = algorithm.validateOptions({})
      expect(result.valid).toBe(true)
    })
  })

  describe('apply', () => {
    it('should handle empty nodes array', async () => {
      const result = await algorithm.apply([], [], {})

      expect(result.success).toBe(true)
      expect(result.nodes).toEqual([])
      expect(result.metadata?.nodeCount).toBe(0)
    })

    it('should layout a simple hierarchical graph vertically', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'client', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e2-3', source: '2', target: '3' },
      ]

      const options: Partial<ColaConstrainedOptions> = { flowDirection: 'y' }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(3)

      // For vertical flow, nodes should be arranged top to bottom
      // Y coordinates should increase down the hierarchy
      const node1 = result.nodes.find((n) => n.id === '1')!
      const node2 = result.nodes.find((n) => n.id === '2')!
      const node3 = result.nodes.find((n) => n.id === '3')!

      expect(node1.position.y).toBeLessThan(node2.position.y)
      expect(node2.position.y).toBeLessThan(node3.position.y)
    })

    it('should layout a simple hierarchical graph horizontally', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'client', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e2-3', source: '2', target: '3' },
      ]

      const options: Partial<ColaConstrainedOptions> = { flowDirection: 'x' }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(3)

      // For horizontal flow, nodes should be arranged left to right
      // X coordinates should increase across the hierarchy
      const node1 = result.nodes.find((n) => n.id === '1')!
      const node2 = result.nodes.find((n) => n.id === '2')!
      const node3 = result.nodes.find((n) => n.id === '3')!

      expect(node1.position.x).toBeLessThan(node2.position.x)
      expect(node2.position.x).toBeLessThan(node3.position.x)
    })

    it('should use custom layerGap', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const options: Partial<ColaConstrainedOptions> = { flowDirection: 'y', layerGap: 500 }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)

      const node1 = result.nodes[0]
      const node2 = result.nodes[1]
      const verticalGap = Math.abs(node2.position.y - node1.position.y)

      // Gap should be roughly the layerGap (with some tolerance for WebCola)
      expect(verticalGap).toBeGreaterThan(400)
      expect(verticalGap).toBeLessThan(600)
    })

    it('should use custom nodeGap', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '4', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e3-2', source: '3', target: '2' },
        { id: 'e4-2', source: '4', target: '2' },
      ]

      const options: Partial<ColaConstrainedOptions> = { nodeGap: 400 }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(4)
    })

    it('should preserve node IDs', async () => {
      const nodes: Node[] = [
        { id: 'my-node-1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'my-node-2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: 'my-node-1', target: 'my-node-2' }]

      const result = await algorithm.apply(nodes, edges, {})

      expect(result.nodes.map((n) => n.id)).toContain('my-node-1')
      expect(result.nodes.map((n) => n.id)).toContain('my-node-2')
    })

    it('should return metadata with correct counts', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'client', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e2-3', source: '2', target: '3' },
      ]

      const result = await algorithm.apply(nodes, edges, {})

      expect(result.metadata).toEqual({
        algorithm: LayoutType.COLA_CONSTRAINED,
        nodeCount: 3,
        edgeCount: 2,
      })
    })

    it('should complete in reasonable time', async () => {
      const nodes: Node[] = Array.from({ length: 30 }, (_, i) => ({
        id: `node-${i}`,
        type: 'adapter',
        position: { x: 0, y: 0 },
        data: {},
      }))

      const edges: Edge[] = Array.from({ length: 40 }, (_, i) => ({
        id: `edge-${i}`,
        source: `node-${Math.floor(Math.random() * 30)}`,
        target: `node-${Math.floor(Math.random() * 30)}`,
      }))

      const startTime = performance.now()
      const result = await algorithm.apply(nodes, edges, {})
      const duration = performance.now() - startTime

      expect(result.success).toBe(true)
      expect(duration).toBeLessThan(5000) // Should complete reasonably fast
    })

    it('should handle nodes without edges', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(2)
    })

    it('should set handle positions', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const result = await algorithm.apply(nodes, edges, {})

      result.nodes.forEach((node) => {
        expect(node.sourcePosition).toBeDefined()
        expect(node.targetPosition).toBeDefined()
      })
    })

    it('should handle complex branching hierarchies', async () => {
      const nodes: Node[] = [
        { id: 'root', type: 'edge', position: { x: 0, y: 0 }, data: {} },
        { id: 'child1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'child2', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'child3', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'grandchild1', type: 'device', position: { x: 0, y: 0 }, data: {} },
        { id: 'grandchild2', type: 'device', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e-root-c1', source: 'root', target: 'child1' },
        { id: 'e-root-c2', source: 'root', target: 'child2' },
        { id: 'e-root-c3', source: 'root', target: 'child3' },
        { id: 'e-c1-gc1', source: 'child1', target: 'grandchild1' },
        { id: 'e-c2-gc2', source: 'child2', target: 'grandchild2' },
      ]

      const options: Partial<ColaConstrainedOptions> = { flowDirection: 'y' }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(6)

      // Just verify all nodes have different positions (layout applied)
      const positions = result.nodes.map((n) => `${n.position.x},${n.position.y}`)
      const uniquePositions = new Set(positions)
      expect(uniquePositions.size).toBeGreaterThan(1) // Nodes should not all be at same position
    })
  })
})
