import { describe, expect, it } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import { NodeTypes } from '@/modules/Workspace/types'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'
import {
  isActiveNode,
  getUpstreamActiveNodes,
  computePassiveNodeRuntimeStatus,
  computePassiveNodeStatus,
  getDownstreamNodes,
  getAffectedNodes,
} from './status-propagation.utils'

describe('status-propagation.utils', () => {
  describe('isActiveNode', () => {
    it('should return true for ADAPTER_NODE', () => {
      expect(isActiveNode(NodeTypes.ADAPTER_NODE)).toBe(true)
    })

    it('should return true for BRIDGE_NODE', () => {
      expect(isActiveNode(NodeTypes.BRIDGE_NODE)).toBe(true)
    })

    it('should return true for PULSE_NODE', () => {
      expect(isActiveNode(NodeTypes.PULSE_NODE)).toBe(true)
    })

    it('should return false for EDGE_NODE', () => {
      expect(isActiveNode(NodeTypes.EDGE_NODE)).toBe(false)
    })

    it('should return false for DEVICE_NODE', () => {
      expect(isActiveNode(NodeTypes.DEVICE_NODE)).toBe(false)
    })

    it('should return false for HOST_NODE', () => {
      expect(isActiveNode(NodeTypes.HOST_NODE)).toBe(false)
    })

    it('should return false for COMBINER_NODE', () => {
      expect(isActiveNode(NodeTypes.COMBINER_NODE)).toBe(false)
    })
  })

  describe('getUpstreamActiveNodes', () => {
    it('should return upstream active nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const upstream = getUpstreamActiveNodes('edge1', edges, nodes)

      expect(upstream).toHaveLength(1)
      expect(upstream[0].id).toBe('adapter1')
    })

    it('should filter out passive nodes from upstream', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'device1', type: NodeTypes.DEVICE_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'device1', target: 'edge1' },
      ]

      const upstream = getUpstreamActiveNodes('edge1', edges, nodes)

      expect(upstream).toHaveLength(1)
      expect(upstream[0].id).toBe('adapter1')
    })

    it('should return empty array when no upstream nodes', () => {
      const nodes: Node[] = [{ id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } }]
      const edges: Edge[] = []

      const upstream = getUpstreamActiveNodes('edge1', edges, nodes)

      expect(upstream).toHaveLength(0)
    })

    it('should return multiple upstream active nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'bridge1', type: NodeTypes.BRIDGE_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'bridge1', target: 'edge1' },
      ]

      const upstream = getUpstreamActiveNodes('edge1', edges, nodes)

      expect(upstream).toHaveLength(2)
      expect(upstream.map((n) => n.id)).toContain('adapter1')
      expect(upstream.map((n) => n.id)).toContain('bridge1')
    })
  })

  describe('computePassiveNodeRuntimeStatus', () => {
    it('should return INACTIVE when no upstream nodes', () => {
      const nodes: Node[] = [{ id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } }]
      const edges: Edge[] = []

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return ACTIVE when upstream node is ACTIVE', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel }, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.ACTIVE)
    })

    it('should return ERROR when upstream node is ERROR', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel }, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.ERROR)
    })

    it('should return ERROR when any upstream node is ERROR', () => {
      const activeStatus: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const errorStatus: NodeStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.ACTIVE,
        source: 'BRIDGE',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel: activeStatus }, position: { x: 0, y: 0 } },
        { id: 'bridge1', type: NodeTypes.BRIDGE_NODE, data: { statusModel: errorStatus }, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'bridge1', target: 'edge1' },
      ]

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.ERROR)
    })

    it('should return INACTIVE when all upstream nodes are INACTIVE', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.INACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel }, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.INACTIVE)
    })

    it('should return ACTIVE when at least one upstream is ACTIVE and none are ERROR', () => {
      const activeStatus: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const inactiveStatus: NodeStatusModel = {
        runtime: RuntimeStatus.INACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'BRIDGE',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel: activeStatus }, position: { x: 0, y: 0 } },
        {
          id: 'bridge1',
          type: NodeTypes.BRIDGE_NODE,
          data: { statusModel: inactiveStatus },
          position: { x: 0, y: 0 },
        },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'bridge1', target: 'edge1' },
      ]

      const status = computePassiveNodeRuntimeStatus('edge1', edges, nodes)

      expect(status).toBe(RuntimeStatus.ACTIVE)
    })
  })

  describe('computePassiveNodeStatus', () => {
    it('should create complete status model with DERIVED source', () => {
      const statusModel: NodeStatusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER',
      }
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: { statusModel }, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const status = computePassiveNodeStatus('edge1', edges, nodes, OperationalStatus.ACTIVE)

      expect(status.runtime).toBe(RuntimeStatus.ACTIVE)
      expect(status.operational).toBe(OperationalStatus.ACTIVE)
      expect(status.source).toBe('DERIVED')
      expect(status.lastUpdated).toBeDefined()
    })

    it('should use provided operational status', () => {
      const nodes: Node[] = [{ id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } }]
      const edges: Edge[] = []

      const status = computePassiveNodeStatus('edge1', edges, nodes, OperationalStatus.INACTIVE)

      expect(status.operational).toBe(OperationalStatus.INACTIVE)
    })
  })

  describe('getDownstreamNodes', () => {
    it('should return downstream nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const downstream = getDownstreamNodes('adapter1', edges, nodes)

      expect(downstream).toHaveLength(1)
      expect(downstream[0].id).toBe('edge1')
    })

    it('should return multiple downstream nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'combiner1', type: NodeTypes.COMBINER_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'adapter1', target: 'combiner1' },
      ]

      const downstream = getDownstreamNodes('adapter1', edges, nodes)

      expect(downstream).toHaveLength(2)
      expect(downstream.map((n) => n.id)).toContain('edge1')
      expect(downstream.map((n) => n.id)).toContain('combiner1')
    })

    it('should return empty array when no downstream nodes', () => {
      const nodes: Node[] = [{ id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } }]
      const edges: Edge[] = []

      const downstream = getDownstreamNodes('adapter1', edges, nodes)

      expect(downstream).toHaveLength(0)
    })
  })

  describe('getAffectedNodes', () => {
    it('should return directly affected passive nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'edge1' }]

      const affected = getAffectedNodes('adapter1', edges, nodes)

      expect(affected).toContain('edge1')
    })

    it('should not include active downstream nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'adapter2', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [{ id: 'e1', source: 'adapter1', target: 'adapter2' }]

      const affected = getAffectedNodes('adapter1', edges, nodes)

      expect(affected).not.toContain('adapter2')
      expect(affected).toHaveLength(0)
    })

    it('should recursively find affected nodes', () => {
      const nodes: Node[] = [
        { id: 'adapter1', type: NodeTypes.ADAPTER_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'device1', type: NodeTypes.DEVICE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'adapter1', target: 'edge1' },
        { id: 'e2', source: 'edge1', target: 'device1' },
      ]

      const affected = getAffectedNodes('adapter1', edges, nodes)

      expect(affected).toContain('edge1')
      expect(affected).toContain('device1')
    })

    it('should prevent circular traversal', () => {
      const nodes: Node[] = [
        { id: 'edge1', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
        { id: 'edge2', type: NodeTypes.EDGE_NODE, data: {}, position: { x: 0, y: 0 } },
      ]
      const edges: Edge[] = [
        { id: 'e1', source: 'edge1', target: 'edge2' },
        { id: 'e2', source: 'edge2', target: 'edge1' },
      ]

      // Should not cause infinite loop
      const affected = getAffectedNodes('edge1', edges, nodes)

      expect(affected).toBeDefined()
      expect(affected.length).toBeGreaterThan(0)
    })
  })
})
