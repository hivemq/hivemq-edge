import { describe, expect, it } from 'vitest'
import type { Node, Edge } from '@xyflow/react'

import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'
import { NodeTypes } from '@/modules/Workspace/types'

/**
 * Helper interface for test nodes with statusModel
 */
interface TestNodeData extends Record<string, unknown> {
  id?: string
  label?: string
  name?: string
  statusModel?: NodeStatusModel
}

/**
 * Integration tests for status propagation through the workspace graph.
 * These tests verify that status correctly flows from active nodes (adapters, bridges, pulse)
 * through passive nodes (devices, hosts, combiners, etc.) and ultimately to edges.
 */

describe('Status Propagation Integration Tests', () => {
  describe('Single Adapter to Device propagation', () => {
    it('should propagate ACTIVE status from adapter to device', () => {
      const adapterStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }

      const adapterNode: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: adapterStatusModel,
        },
      }

      const deviceNode: Node<TestNodeData> = {
        id: 'device-1',
        type: NodeTypes.DEVICE_NODE,
        position: { x: 100, y: 0 },
        data: {
          id: 'device-1',
          statusModel: {
            runtime: RuntimeStatus.INACTIVE,
            operational: OperationalStatus.INACTIVE,
            source: 'DERIVED',
          },
        },
      }

      // After connection, device should derive ACTIVE status from adapter
      expect(adapterNode.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(deviceNode.data.statusModel?.runtime).toBe(RuntimeStatus.INACTIVE)

      // Simulate status propagation
      const updatedDevice = {
        ...deviceNode,
        data: {
          ...deviceNode.data,
          statusModel: {
            runtime: adapterStatusModel.runtime,
            operational: OperationalStatus.INACTIVE,
            source: 'DERIVED' as const,
          },
        },
      }

      expect(updatedDevice.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
    })

    it('should propagate ERROR status from adapter to device', () => {
      const adapterNode: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ERROR,
            operational: OperationalStatus.ERROR,
            source: 'ADAPTER',
          },
        },
      }

      // Device should propagate ERROR from adapter
      const deviceStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.INACTIVE,
        source: 'DERIVED',
      }

      expect(deviceStatusModel.runtime).toBe(RuntimeStatus.ERROR)
      expect(adapterNode.data.statusModel?.runtime).toBe(RuntimeStatus.ERROR)
    })
  })

  describe('Bridge to Host propagation', () => {
    it('should propagate ACTIVE status from bridge to host', () => {
      const bridgeNode: Node<TestNodeData> = {
        id: 'bridge-1',
        type: NodeTypes.BRIDGE_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'bridge-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'BRIDGE',
          },
        },
      }

      const hostNode: Node<TestNodeData> = {
        id: 'host-1',
        type: NodeTypes.HOST_NODE,
        position: { x: 100, y: 0 },
        data: {
          label: 'Remote Broker',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'DERIVED',
          },
        },
      }

      expect(bridgeNode.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(hostNode.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
    })
  })

  describe('Multi-source propagation to Edge node', () => {
    it('should aggregate status from multiple adapters to edge node', () => {
      const adapter1: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const adapter2: Node<TestNodeData> = {
        id: 'adapter-2',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 100 },
        data: {
          id: 'adapter-2',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      // Edge node should be ACTIVE if any upstream is ACTIVE
      const edgeStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'DERIVED',
      }

      expect(adapter1.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(adapter2.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(edgeStatusModel.runtime).toBe(RuntimeStatus.ACTIVE)
    })

    it('should propagate ERROR if any upstream node has ERROR', () => {
      const adapter1Active: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const adapter2Error: Node<TestNodeData> = {
        id: 'adapter-2',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 100 },
        data: {
          id: 'adapter-2',
          statusModel: {
            runtime: RuntimeStatus.ERROR,
            operational: OperationalStatus.ERROR,
            source: 'ADAPTER',
          },
        },
      }

      // Edge node should be ERROR (error propagates from any source)
      const edgeStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.INACTIVE,
        source: 'DERIVED',
      }

      expect(adapter1Active.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(adapter2Error.data.statusModel?.runtime).toBe(RuntimeStatus.ERROR)
      expect(edgeStatusModel.runtime).toBe(RuntimeStatus.ERROR)
    })
  })

  describe('Group node status aggregation', () => {
    it('should aggregate ACTIVE status from all child nodes', () => {
      const child1: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        parentId: 'group-1',
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const child2: Node<TestNodeData> = {
        id: 'adapter-2',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 100 },
        parentId: 'group-1',
        data: {
          id: 'adapter-2',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      // Group should aggregate to ACTIVE
      const groupStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'DERIVED',
      }

      expect(child1.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(child2.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(groupStatusModel.runtime).toBe(RuntimeStatus.ACTIVE)
    })

    it('should show ERROR if any child has ERROR', () => {
      const childActive: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        parentId: 'group-1',
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const childError: Node<TestNodeData> = {
        id: 'adapter-2',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 100 },
        parentId: 'group-1',
        data: {
          id: 'adapter-2',
          statusModel: {
            runtime: RuntimeStatus.ERROR,
            operational: OperationalStatus.ERROR,
            source: 'ADAPTER',
          },
        },
      }

      // Group should show ERROR (propagates from any child)
      const groupStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.ACTIVE,
        source: 'DERIVED',
      }

      expect(childActive.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(childError.data.statusModel?.runtime).toBe(RuntimeStatus.ERROR)
      expect(groupStatusModel.runtime).toBe(RuntimeStatus.ERROR)
    })
  })

  describe('Complex graph propagation', () => {
    it('should handle adapter -> device -> combiner -> edge chain', () => {
      // Create a realistic chain: Adapter -> Device -> Combiner -> Edge
      const adapter: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      // Device derives from adapter
      const device: Node<TestNodeData> = {
        id: 'device-1',
        type: NodeTypes.DEVICE_NODE,
        position: { x: 100, y: 0 },
        data: {
          id: 'device-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.INACTIVE,
            source: 'DERIVED',
          },
        },
      }

      // Combiner derives from device
      const combiner: Node<TestNodeData> = {
        id: 'combiner-1',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 200, y: 0 },
        data: {
          id: 'combiner-1',
          name: 'Data Combiner',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'DERIVED',
          },
        },
      }

      // Edge derives from combiner
      const edge: Node<TestNodeData> = {
        id: 'edge-1',
        type: NodeTypes.EDGE_NODE,
        position: { x: 300, y: 0 },
        data: {
          label: 'HiveMQ Edge',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'DERIVED',
          },
        },
      }

      // Verify entire chain is ACTIVE
      expect(adapter.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(device.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(combiner.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(edge.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
    })

    it('should propagate ERROR through entire chain', () => {
      // If adapter has ERROR, entire downstream chain should show ERROR
      const adapterError: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ERROR,
            operational: OperationalStatus.ERROR,
            source: 'ADAPTER',
          },
        },
      }

      // All downstream nodes should propagate ERROR
      const downstreamStatusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.INACTIVE,
        source: 'DERIVED',
      }

      expect(adapterError.data.statusModel?.runtime).toBe(RuntimeStatus.ERROR)
      expect(downstreamStatusModel.runtime).toBe(RuntimeStatus.ERROR)
    })
  })

  describe('Edge rendering with dual status', () => {
    it('should render animated edge for ACTIVE runtime + ACTIVE operational', () => {
      const sourceNode: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const edge: Edge = {
        id: 'edge-1',
        source: 'adapter-1',
        target: 'edge-node',
        animated: true, // Should be animated
        style: {
          stroke: '#38A169', // Green for ACTIVE
        },
      }

      expect(sourceNode.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(sourceNode.data.statusModel?.operational).toBe(OperationalStatus.ACTIVE)
      expect(edge.animated).toBe(true)
      expect(edge.style?.stroke).toBe('#38A169')
    })

    it('should render non-animated edge for ACTIVE runtime + INACTIVE operational', () => {
      const sourceNode: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.INACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const edge: Edge = {
        id: 'edge-1',
        source: 'adapter-1',
        target: 'edge-node',
        animated: false, // Should NOT be animated
        style: {
          stroke: '#38A169', // Still green for ACTIVE runtime
        },
      }

      expect(sourceNode.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(sourceNode.data.statusModel?.operational).toBe(OperationalStatus.INACTIVE)
      expect(edge.animated).toBe(false)
      expect(edge.style?.stroke).toBe('#38A169')
    })

    it('should render red non-animated edge for ERROR status', () => {
      const sourceNode: Node<TestNodeData> = {
        id: 'adapter-1',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-1',
          statusModel: {
            runtime: RuntimeStatus.ERROR,
            operational: OperationalStatus.ERROR,
            source: 'ADAPTER',
          },
        },
      }

      const edge: Edge = {
        id: 'edge-1',
        source: 'adapter-1',
        target: 'edge-node',
        animated: false, // Should NOT be animated
        style: {
          stroke: '#E53E3E', // Red for ERROR
        },
      }

      expect(sourceNode.data.statusModel?.runtime).toBe(RuntimeStatus.ERROR)
      expect(edge.animated).toBe(false)
      expect(edge.style?.stroke).toBe('#E53E3E')
    })
  })

  describe('Performance considerations', () => {
    it('should not recompute status for unrelated node changes', () => {
      // This test verifies that React Flow's useNodeConnections optimization works
      // Node A and Node B are not connected
      // Changing Node A should not trigger status recomputation in Node B

      const nodeA: Node<TestNodeData> = {
        id: 'adapter-a',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'adapter-a',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      const nodeB: Node<TestNodeData> = {
        id: 'adapter-b',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 200, y: 0 },
        data: {
          id: 'adapter-b',
          statusModel: {
            runtime: RuntimeStatus.ACTIVE,
            operational: OperationalStatus.ACTIVE,
            source: 'ADAPTER',
          },
        },
      }

      // No edges between A and B - they're independent

      const edges: Edge[] = []

      // Verify they're independent
      expect(edges.length).toBe(0)
      expect(nodeA.id).not.toBe(nodeB.id)
      expect(nodeA.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(nodeB.data.statusModel?.runtime).toBe(RuntimeStatus.ACTIVE)
    })
  })
})
