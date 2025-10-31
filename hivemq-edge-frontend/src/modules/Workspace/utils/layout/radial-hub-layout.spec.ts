import { describe, expect, it, beforeEach } from 'vitest'
import type { Node, Edge } from '@xyflow/react'
import { RadialHubLayoutAlgorithm } from './radial-hub-layout'
import { LayoutType, type RadialOptions } from '../../types/layout'
import { NodeTypes } from '../../types'

describe('RadialHubLayoutAlgorithm', () => {
  let algorithm: RadialHubLayoutAlgorithm

  beforeEach(() => {
    algorithm = new RadialHubLayoutAlgorithm()
  })

  describe('metadata', () => {
    it('should have correct type', () => {
      expect(algorithm.type).toBe(LayoutType.RADIAL_HUB)
    })

    it('should have correct name', () => {
      expect(algorithm.name).toBe('Radial Hub Layout')
    })

    it('should have correct description', () => {
      expect(algorithm.description).toContain('Radial layout')
      expect(algorithm.description).toContain('EDGE at center')
    })
  })

  describe('defaultOptions', () => {
    it('should have correct default centerX', () => {
      expect(algorithm.defaultOptions.centerX).toBe(400)
    })

    it('should have correct default centerY', () => {
      expect(algorithm.defaultOptions.centerY).toBe(300)
    })

    it('should have correct default layerSpacing', () => {
      expect(algorithm.defaultOptions.layerSpacing).toBe(500)
    })

    it('should have correct default startAngle (top/12 oclock)', () => {
      expect(algorithm.defaultOptions.startAngle).toBe(-Math.PI / 2)
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
      const options: RadialOptions = {
        centerX: 500,
        centerY: 400,
        layerSpacing: 400,
        startAngle: 0,
      }
      const result = algorithm.validateOptions(options)

      expect(result.valid).toBe(true)
      expect(result.errors).toBeUndefined()
    })

    it('should allow negative centerX', () => {
      const result = algorithm.validateOptions({ centerX: -100 } as RadialOptions)
      expect(result.valid).toBe(true)
    })

    it('should allow negative centerY', () => {
      const result = algorithm.validateOptions({ centerY: -200 } as RadialOptions)
      expect(result.valid).toBe(true)
    })

    it('should allow any startAngle value', () => {
      const result = algorithm.validateOptions({ startAngle: Math.PI } as RadialOptions)
      expect(result.valid).toBe(true)
    })

    it('should warn about very small layerSpacing', () => {
      const result = algorithm.validateOptions({ layerSpacing: 50 } as RadialOptions)

      expect(result.valid).toBe(true) // Valid but with warnings
      expect(result.warnings).toContain('layerSpacing < 100px may cause overlapping nodes')
    })

    it('should warn about very large layerSpacing', () => {
      const result = algorithm.validateOptions({ layerSpacing: 1000 } as RadialOptions)

      expect(result.valid).toBe(true) // Valid but with warnings
      expect(result.warnings).toContain('layerSpacing > 800px may create excessive spacing')
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

    it('should place EDGE node at center', async () => {
      const nodes: Node[] = [{ id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} }]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.success).toBe(true)
      expect(result.nodes[0].position.x).toBe(400) // Default centerX
      expect(result.nodes[0].position.y).toBe(300) // Default centerY
    })

    it('should use custom center coordinates', async () => {
      const nodes: Node[] = [{ id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} }]

      const options: Partial<RadialOptions> = { centerX: 600, centerY: 500 }
      const result = await algorithm.apply(nodes, [], options)

      expect(result.nodes[0].position.x).toBe(600)
      expect(result.nodes[0].position.y).toBe(500)
    })

    it('should arrange adapters in circle around EDGE', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter2', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.success).toBe(true)

      // EDGE should be at center
      const edgeNode = result.nodes.find((n) => n.id === 'edge')!
      expect(edgeNode.position.x).toBe(400)
      expect(edgeNode.position.y).toBe(300)

      // Adapters should be at distance layerSpacing * 2 (layer 2)
      const adapter1 = result.nodes.find((n) => n.id === 'adapter1')!
      const adapter2 = result.nodes.find((n) => n.id === 'adapter2')!

      const distance1 = Math.sqrt(Math.pow(adapter1.position.x - 400, 2) + Math.pow(adapter1.position.y - 300, 2))
      const distance2 = Math.sqrt(Math.pow(adapter2.position.x - 400, 2) + Math.pow(adapter2.position.y - 300, 2))

      // Should be at layer 2: layerSpacing * 2 = 1000
      expect(distance1).toBeCloseTo(1000, 0)
      expect(distance2).toBeCloseTo(1000, 0)
    })

    it('should arrange nodes in different layers by type', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'combiner', type: NodeTypes.COMBINER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'device', type: NodeTypes.DEVICE_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      const centerX = 400
      const centerY = 300

      // Calculate distances from center
      const combinerDist = Math.sqrt(
        Math.pow(result.nodes[1].position.x - centerX, 2) + Math.pow(result.nodes[1].position.y - centerY, 2)
      )
      const adapterDist = Math.sqrt(
        Math.pow(result.nodes[2].position.x - centerX, 2) + Math.pow(result.nodes[2].position.y - centerY, 2)
      )
      const deviceDist = Math.sqrt(
        Math.pow(result.nodes[3].position.x - centerX, 2) + Math.pow(result.nodes[3].position.y - centerY, 2)
      )

      // Combiner should be closer than adapter
      expect(combinerDist).toBeLessThan(adapterDist)
      // Adapter should be closer than device
      expect(adapterDist).toBeLessThan(deviceDist)
    })

    it('should distribute nodes evenly around circle', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter2', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter3', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter4', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      // Get adapter nodes
      const adapters = result.nodes.filter((n) => n.type === NodeTypes.ADAPTER_NODE)

      // Calculate angles
      const angles = adapters.map((node) => {
        const dx = node.position.x - 400
        const dy = node.position.y - 300
        return Math.atan2(dy, dx)
      })

      // Sort angles
      angles.sort((a, b) => a - b)

      // Check angular spacing (should be roughly 2π/4 = π/2 radians apart)
      const expectedSpacing = (2 * Math.PI) / 4
      for (let i = 0; i < angles.length - 1; i++) {
        const spacing = angles[i + 1] - angles[i]
        expect(spacing).toBeCloseTo(expectedSpacing, 1)
      }
    })

    it('should use custom layerSpacing', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const options: Partial<RadialOptions> = { layerSpacing: 300 }
      const result = await algorithm.apply(nodes, [], options)

      const adapter = result.nodes.find((n) => n.id === 'adapter')!
      const distance = Math.sqrt(Math.pow(adapter.position.x - 400, 2) + Math.pow(adapter.position.y - 300, 2))

      // Adapter is layer 2: layerSpacing * 2 = 600
      expect(distance).toBeCloseTo(600, 0)
    })

    it('should use custom startAngle', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      // Start at right (3 o'clock)
      const options: Partial<RadialOptions> = { startAngle: 0 }
      const result = await algorithm.apply(nodes, [], options)

      const adapter = result.nodes.find((n) => n.id === 'adapter')!

      // Should be positioned away from center (radial layout)
      const distance = Math.sqrt(Math.pow(adapter.position.x - 400, 2) + Math.pow(adapter.position.y - 300, 2))
      expect(distance).toBeGreaterThan(900) // Layer 2 distance
    })

    it('should return metadata with correct counts', async () => {
      const nodes: Node[] = [
        { id: '1', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: '2', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: '3', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const edges: Edge[] = [
        { id: 'e1-2', source: '1', target: '2' },
        { id: 'e2-3', source: '2', target: '3' },
      ]

      const result = await algorithm.apply(nodes, edges, {})

      expect(result.metadata).toEqual({
        algorithm: LayoutType.RADIAL_HUB,
        nodeCount: 3,
        edgeCount: 2,
      })
    })

    it('should complete in reasonable time', async () => {
      const nodes: Node[] = Array.from({ length: 50 }, (_, i) => ({
        id: `node-${i}`,
        type: i === 0 ? NodeTypes.EDGE_NODE : NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {},
      }))

      const startTime = performance.now()
      const result = await algorithm.apply(nodes, [], {})
      const duration = performance.now() - startTime

      expect(result.success).toBe(true)
      expect(duration).toBeLessThan(100) // Should be fast
    })

    it('should handle nodes without EDGE node', async () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter2', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      // Should still work, just no central node
      expect(result.success).toBe(true)
      expect(result.nodes).toHaveLength(2)
    })

    it('should preserve node IDs', async () => {
      const nodes: Node[] = [
        { id: 'my-edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'my-adapter', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      expect(result.nodes.map((n) => n.id)).toEqual(['my-edge', 'my-adapter'])
    })

    it('should set handle positions for radial layout', async () => {
      const nodes: Node[] = [
        { id: 'edge', type: NodeTypes.EDGE_NODE, position: { x: 0, y: 0 }, data: {} },
        { id: 'adapter', type: NodeTypes.ADAPTER_NODE, position: { x: 0, y: 0 }, data: {} },
      ]

      const result = await algorithm.apply(nodes, [], {})

      // Nodes should have sourcePosition and targetPosition set
      result.nodes.forEach((node) => {
        expect(node.sourcePosition).toBeDefined()
        expect(node.targetPosition).toBeDefined()
      })
    })
  })
})
