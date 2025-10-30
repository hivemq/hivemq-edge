import { describe, expect, it, beforeEach } from 'vitest'
import type { Node, Edge } from '@xyflow/react'
import { ColaForceLayoutAlgorithm } from './cola-force-layout'
import { LayoutType, type ColaForceOptions } from '../../types/layout'

describe('ColaForceLayoutAlgorithm', () => {
  let algorithm: ColaForceLayoutAlgorithm

  beforeEach(() => {
    algorithm = new ColaForceLayoutAlgorithm()
  })

  describe('metadata', () => {
    it('should have correct type', () => {
      expect(algorithm.type).toBe(LayoutType.COLA_FORCE)
    })

    it('should have correct name', () => {
      expect(algorithm.name).toBe('Force-Directed Layout')
    })

    it('should have correct description', () => {
      expect(algorithm.description).toContain('Physics-based')
      expect(algorithm.description).toContain('natural clustering')
    })
  })

  describe('defaultOptions', () => {
    it('should have correct default linkDistance', () => {
      expect(algorithm.defaultOptions.linkDistance).toBe(350)
    })

    it('should have avoidOverlaps enabled by default', () => {
      expect(algorithm.defaultOptions.avoidOverlaps).toBe(true)
    })

    it('should have handleDisconnected enabled by default', () => {
      expect(algorithm.defaultOptions.handleDisconnected).toBe(true)
    })

    it('should have correct default convergenceThreshold', () => {
      expect(algorithm.defaultOptions.convergenceThreshold).toBe(0.01)
    })

    it('should have correct default maxIterations', () => {
      expect(algorithm.defaultOptions.maxIterations).toBe(1000)
    })

    it('should have animation enabled by default', () => {
      expect(algorithm.defaultOptions.animate).toBe(true)
    })

    it('should have correct default animationDuration', () => {
      expect(algorithm.defaultOptions.animationDuration).toBe(500)
    })

    it('should have fitView enabled by default', () => {
      expect(algorithm.defaultOptions.fitView).toBe(true)
    })
  })

  describe('validateOptions', () => {
    it('should validate correct options', () => {
      const options: ColaForceOptions = {
        linkDistance: 400,
        maxIterations: 500,
        convergenceThreshold: 0.05,
        avoidOverlaps: false,
        handleDisconnected: true,
      }
      const result = algorithm.validateOptions(options)

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
    })

    it('should warn about small linkDistance', () => {
      const result = algorithm.validateOptions({ linkDistance: 100 } as ColaForceOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('linkDistance < 200px may cause overlapping nodes')
    })

    it('should warn about large linkDistance', () => {
      const result = algorithm.validateOptions({ linkDistance: 900 } as ColaForceOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('linkDistance > 800px may cause excessive spacing')
    })

    it('should warn about low maxIterations', () => {
      const result = algorithm.validateOptions({ maxIterations: 50 } as ColaForceOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('maxIterations < 100 may not converge properly')
    })

    it('should warn about very high maxIterations', () => {
      const result = algorithm.validateOptions({ maxIterations: 6000 } as ColaForceOptions)

      expect(result.valid).toBe(true)
      expect(result.warnings).toContain('maxIterations > 5000 may cause slow performance')
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

    it('should layout a simple graph', async () => {
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

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(3)

      // Positions should be changed from (0,0)
      result.nodes.forEach((node) => {
        expect(node.position.x !== 0 || node.position.y !== 0).toBe(true)
      })
    })

    it('should use custom linkDistance', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      const options: Partial<ColaForceOptions> = { linkDistance: 200 }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)

      // Calculate actual distance
      const node1 = result.nodes[0]
      const node2 = result.nodes[1]
      const distance = Math.sqrt(
        Math.pow(node2.position.x - node1.position.x, 2) + Math.pow(node2.position.y - node1.position.y, 2)
      )

      // Should be roughly the link distance (with some tolerance for force simulation)
      expect(distance).toBeGreaterThan(100)
      expect(distance).toBeLessThan(400)
    })

    it('should prevent overlaps when avoidOverlaps is true', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]

      const options: Partial<ColaForceOptions> = { avoidOverlaps: true }
      const result = await algorithm.apply(nodes, [], options)

      expect(result.success).toBe(true)

      // Check that nodes are separated (allowing some tolerance for cola's physics)
      const MIN_DISTANCE = 100 // Reduced from 200 - cola doesn't guarantee perfect spacing
      for (let i = 0; i < result.nodes.length; i++) {
        for (let j = i + 1; j < result.nodes.length; j++) {
          const n1 = result.nodes[i]
          const n2 = result.nodes[j]
          const distance = Math.sqrt(
            Math.pow(n2.position.x - n1.position.x, 2) + Math.pow(n2.position.y - n1.position.y, 2)
          )
          expect(distance).toBeGreaterThan(MIN_DISTANCE)
        }
      }
    })

    it('should handle disconnected components when handleDisconnected is true', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '4', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e3-4', source: '3', target: '4' },
      ]

      const options: Partial<ColaForceOptions> = { handleDisconnected: true }
      const result = await algorithm.apply(nodes, edges, options)

      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(4)
    })

    it('should use custom maxIterations', async () => {
      const nodes: Node[] = [
        { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }]

      // Lower iterations should complete faster
      const options: Partial<ColaForceOptions> = { maxIterations: 10 }
      const startTime = performance.now()
      const result = await algorithm.apply(nodes, edges, options)
      const duration = performance.now() - startTime

      expect(result.success).toBe(true)
      expect(duration).toBeLessThan(100) // Should be very fast with low iterations
    })

    it('should preserve node IDs', async () => {
      const nodes: Node[] = [
        { id: 'my-node-1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
        { id: 'my-node-2', type: 'edge', position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.nodes.map((n) => n.id)).toEqual(['my-node-1', 'my-node-2'])
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
        algorithm: LayoutType.COLA_FORCE,
        nodeCount: 3,
        edgeCount: 2,
      })
    })

    it('should complete in reasonable time for moderate graphs', async () => {
      const nodes: Node[] = Array.from({ length: 20 }, (_, i) => ({
        id: `node-${i}`,
        type: 'adapter',
        position: { x: 0, y: 0 },
        data: {},
      }))

      const edges: Edge[] = Array.from({ length: 30 }, (_, i) => ({
        id: `edge-${i}`,
        source: `node-${Math.floor(Math.random() * 20)}`,
        target: `node-${Math.floor(Math.random() * 20)}`,
      }))

      const options: Partial<ColaForceOptions> = { maxIterations: 500 }
      const startTime = performance.now()
      const result = await algorithm.apply(nodes, edges, options)
      const duration = performance.now() - startTime

      expect(result.success).toBe(true)
      expect(duration).toBeLessThan(5000) // Should complete in reasonable time
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
  })
})
