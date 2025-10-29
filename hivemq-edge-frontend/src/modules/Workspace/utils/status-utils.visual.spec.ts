import { describe, expect, it } from 'vitest'
import type { Edge, Node, EdgeMarker } from '@xyflow/react'

import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_PULSE,
  MOCK_NODE_DEVICE,
  MOCK_NODE_LISTENER,
  MOCK_NODE_GROUP,
} from '@/__test-utils__/react-flow/nodes.ts'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { EdgeTypes, NodeTypes, type NodePulseType } from '@/modules/Workspace/types.ts'
import { RuntimeStatus, OperationalStatus } from '@/modules/Workspace/types/status.types'

import {
  getThemeForRuntimeStatus,
  getThemeForStatusModel,
  getEdgeStatusFromModel,
  updateEdgesStatusWithModel,
  updatePulseStatusWithModel,
} from './status-utils.ts'

describe('Visual Rendering with Unified Status Model', () => {
  describe('getThemeForRuntimeStatus', () => {
    it('should return green color for ACTIVE runtime status', () => {
      const color = getThemeForRuntimeStatus(MOCK_THEME, RuntimeStatus.ACTIVE)
      expect(color).toBe(MOCK_THEME.colors.status.connected[500])
    })

    it('should return red color for ERROR runtime status', () => {
      const color = getThemeForRuntimeStatus(MOCK_THEME, RuntimeStatus.ERROR)
      expect(color).toBe(MOCK_THEME.colors.status.error[500])
    })

    it('should return yellow/gray color for INACTIVE runtime status', () => {
      const color = getThemeForRuntimeStatus(MOCK_THEME, RuntimeStatus.INACTIVE)
      expect(color).toBe(MOCK_THEME.colors.status.disconnected[500])
    })

    it('should return default color for unknown status', () => {
      const color = getThemeForRuntimeStatus(MOCK_THEME, 'UNKNOWN' as RuntimeStatus)
      expect(color).toBe(MOCK_THEME.colors.status.disconnected[500])
    })
  })

  describe('getThemeForStatusModel', () => {
    it('should return color based on runtime status', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER' as const,
      }

      const color = getThemeForStatusModel(MOCK_THEME, statusModel)
      expect(color).toBe(MOCK_THEME.colors.status.connected[500])
    })

    it('should return default color for undefined status model', () => {
      const color = getThemeForStatusModel(MOCK_THEME, undefined)
      expect(color).toBe(MOCK_THEME.colors.status.disconnected[500])
    })
  })

  describe('getEdgeStatusFromModel', () => {
    it('should apply green color and animation for ACTIVE runtime + ACTIVE operational', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME)

      expect(result.style?.stroke).toBe(MOCK_THEME.colors.status.connected[500])
      expect(result.style?.strokeWidth).toBe(1.5)
      expect(result.animated).toBe(true)
      expect(result.markerEnd).toBeDefined()
      expect((result.markerEnd as EdgeMarker).color).toBe(MOCK_THEME.colors.status.connected[500])
      expect(result.data?.isConnected).toBe(true)
      expect(result.data?.hasTopics).toBe(true)
    })

    it('should apply green color but no animation for ACTIVE runtime + INACTIVE operational', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.INACTIVE,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME)

      expect(result.style?.stroke).toBe(MOCK_THEME.colors.status.connected[500])
      expect(result.animated).toBe(false)
      expect(result.data?.isConnected).toBe(true)
      expect(result.data?.hasTopics).toBe(false)
    })

    it('should apply red color and no animation for ERROR runtime status', () => {
      const statusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.ERROR,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME)

      expect(result.style?.stroke).toBe(MOCK_THEME.colors.status.error[500])
      expect(result.animated).toBe(false)
      expect(result.markerEnd).toBeDefined()
      expect((result.markerEnd as EdgeMarker).color).toBe(MOCK_THEME.colors.status.error[500])
      expect(result.data?.isConnected).toBe(false)
    })

    it('should apply yellow color and no animation for INACTIVE runtime status', () => {
      const statusModel = {
        runtime: RuntimeStatus.INACTIVE,
        operational: OperationalStatus.INACTIVE,
        source: 'DERIVED' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME)

      expect(result.style?.stroke).toBe(MOCK_THEME.colors.status.disconnected[500])
      expect(result.animated).toBe(false)
      expect(result.data?.isConnected).toBe(false)
    })

    it('should not add marker when hasMarker is false', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, false, MOCK_THEME)

      expect(result.markerEnd).toBeUndefined()
    })

    it('should use forceAnimation parameter when provided', () => {
      const statusModel = {
        runtime: RuntimeStatus.INACTIVE,
        operational: OperationalStatus.INACTIVE,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME, true)

      expect(result.animated).toBe(true)
    })

    it('should not animate when forceAnimation is explicitly false', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER' as const,
      }

      const result = getEdgeStatusFromModel(statusModel, true, MOCK_THEME, false)

      expect(result.animated).toBe(false)
    })

    it('should handle undefined status model gracefully', () => {
      const result = getEdgeStatusFromModel(undefined, true, MOCK_THEME)

      expect(result.style?.stroke).toBe(MOCK_THEME.colors.status.disconnected[500])
      expect(result.animated).toBe(false)
      expect(result.data?.isConnected).toBe(false)
      expect(result.data?.hasTopics).toBe(false)
    })
  })

  describe('updateEdgesStatusWithModel', () => {
    const mockGetNodeWithStatus = (id: string): Node | undefined => {
      const nodes: Record<string, Node> = {
        'idAdapter@adapter-id': {
          ...MOCK_NODE_ADAPTER,
          id: 'idAdapter@adapter-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.ADAPTER_NODE,
          data: {
            ...MOCK_NODE_ADAPTER.data,
            statusModel: {
              runtime: RuntimeStatus.ACTIVE,
              operational: OperationalStatus.ACTIVE,
              source: 'ADAPTER' as const,
            },
          },
        },
        'idBridge@bridge-id': {
          ...MOCK_NODE_BRIDGE,
          id: 'idBridge@bridge-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.BRIDGE_NODE,
          data: {
            ...MOCK_NODE_BRIDGE.data,
            statusModel: {
              runtime: RuntimeStatus.ACTIVE,
              operational: OperationalStatus.ACTIVE,
              source: 'BRIDGE' as const,
            },
          },
        },
        'idPulse@pulse-id': {
          ...MOCK_NODE_PULSE,
          id: 'idPulse@pulse-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.PULSE_NODE,
          data: {
            ...MOCK_NODE_PULSE.data,
            statusModel: {
              runtime: RuntimeStatus.ACTIVE,
              operational: OperationalStatus.INACTIVE,
              source: 'PULSE' as const,
            },
          },
        },
        idListener: { ...MOCK_NODE_LISTENER, position: { x: 0, y: 0 } },
        idDevice: { ...MOCK_NODE_DEVICE, position: { x: 0, y: 0 } },
      }
      return nodes[id]
    }

    it('should update adapter edges with unified status model', () => {
      const edges: Edge[] = [
        {
          id: 'edge-adapter',
          source: 'idAdapter@adapter-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeWithStatus, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0]).toMatchObject({
        id: 'edge-adapter',
        animated: true,
        data: {
          isConnected: true,
          hasTopics: true,
        },
      })
      expect(result[0].style?.stroke).toBe(MOCK_THEME.colors.status.connected[500])
    })

    it('should update bridge edges with unified status model', () => {
      const edges: Edge[] = [
        {
          id: 'edge-bridge',
          source: 'idBridge@bridge-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeWithStatus, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0]).toMatchObject({
        id: 'edge-bridge',
        data: {
          isConnected: true,
        },
      })
      expect(result[0].style?.stroke).toBe(MOCK_THEME.colors.status.connected[500])
    })

    it('should update pulse edges with unified status model', () => {
      const edges: Edge[] = [
        {
          id: 'edge-pulse',
          source: 'idPulse@pulse-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeWithStatus, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0]).toMatchObject({
        id: 'edge-pulse',
        animated: false, // INACTIVE operational status
        data: {
          isConnected: true,
          hasTopics: false,
        },
      })
    })

    it('should handle adapter to device edges correctly', () => {
      const edges: Edge[] = [
        {
          id: 'edge-adapter-device',
          source: 'idAdapter@adapter-id',
          target: 'idDevice',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeWithStatus, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0].id).toBe('edge-adapter-device')
      // Should check bidirectional property
    })

    it('should handle group edges with aggregated status', () => {
      const mockGetNodeForGroup = (id: string): Node | undefined => {
        const nodes: Record<string, Node> = {
          idGroup: {
            ...MOCK_NODE_GROUP,
            position: { x: 0, y: 0 },
            type: NodeTypes.CLUSTER_NODE,
            data: {
              childrenNodeIds: ['idAdapter@adapter-id', 'idBridge@bridge-id'],
              title: 'Group',
              isOpen: true,
            },
          },
          'idAdapter@adapter-id': {
            ...MOCK_NODE_ADAPTER,
            id: 'idAdapter@adapter-id',
            position: { x: 0, y: 0 },
            type: NodeTypes.ADAPTER_NODE,
            data: {
              ...MOCK_NODE_ADAPTER.data,
              statusModel: {
                runtime: RuntimeStatus.ACTIVE,
                operational: OperationalStatus.ACTIVE,
                source: 'ADAPTER' as const,
              },
            },
          },
          idListener: { ...MOCK_NODE_LISTENER, position: { x: 0, y: 0 } },
        }
        return nodes[id]
      }

      const edges: Edge[] = [
        {
          id: 'edge-adapter',
          source: 'idAdapter@adapter-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
          data: { isConnected: true, hasTopics: true },
        },
        {
          id: 'connect-edge-group-1',
          source: 'idGroup',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeForGroup, MOCK_THEME)

      expect(result).toHaveLength(2)
      const groupEdge = result.find((e) => e.id === 'connect-edge-group-1')
      expect(groupEdge).toBeDefined()
      expect(groupEdge?.data?.isConnected).toBe(true)
      expect(groupEdge?.data?.hasTopics).toBe(true)
    })

    it('should handle group edge when source is not a cluster node', () => {
      const mockGetNodeNonGroup = (id: string): Node | undefined => {
        if (id === 'idAdapter@adapter-id') {
          return {
            ...MOCK_NODE_ADAPTER,
            id: 'idAdapter@adapter-id',
            position: { x: 0, y: 0 },
            type: NodeTypes.ADAPTER_NODE,
          }
        }
        return undefined
      }

      const edges: Edge[] = [
        {
          id: 'connect-edge-group-1',
          source: 'idAdapter@adapter-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([], edges, mockGetNodeNonGroup, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0].id).toBe('connect-edge-group-1')
    })

    it('should keep edge unchanged if no status model available', () => {
      const mockGetNodeNoStatus = (id: string): Node | undefined => {
        if (id === 'idDevice') {
          return { ...MOCK_NODE_DEVICE, position: { x: 0, y: 0 } }
        }
        return undefined
      }

      const edges: Edge[] = [
        {
          id: 'edge-device',
          source: 'idDevice',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeNoStatus, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0]).toEqual(edges[0])
    })

    it('should keep edge unchanged if source node not found', () => {
      const mockGetNodeNotFound = (): Node | undefined => {
        return undefined
      }

      const edges: Edge[] = [
        {
          id: 'edge-unknown',
          source: 'unknown-id',
          target: 'idListener',
          type: EdgeTypes.DYNAMIC_EDGE,
        },
      ]

      const result = updateEdgesStatusWithModel([mockProtocolAdapter], edges, mockGetNodeNotFound, MOCK_THEME)

      expect(result).toHaveLength(1)
      expect(result[0]).toEqual(edges[0])
    })
  })

  describe('updatePulseStatusWithModel', () => {
    // Create a proper pulse node with position
    const mockPulseNode: NodePulseType = {
      ...MOCK_NODE_PULSE,
      position: { x: 0, y: 0 },
    }

    it('should update pulse node and connected edges with unified status model', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'PULSE' as const,
      }

      const edges: Edge[] = [
        { id: 'edge-test1', source: 'other', target: 'something' },
        { id: 'edge-test2', source: mockPulseNode.id, target: 'something' },
        { id: 'edge-test3', source: mockPulseNode.id, target: 'another' },
      ]

      const result = updatePulseStatusWithModel(mockPulseNode, statusModel, edges, MOCK_THEME)

      expect(result.nodes).toMatchObject({
        ...mockPulseNode.data,
        statusModel,
      })

      expect(result.edges).toHaveLength(2)
      expect(result.edges[0]).toMatchObject({
        id: 'edge-test2',
        type: 'replace',
      })
      expect(result.edges[1]).toMatchObject({
        id: 'edge-test3',
        type: 'replace',
      })
    })

    it('should apply correct colors based on runtime status', () => {
      const errorStatusModel = {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.ERROR,
        source: 'PULSE' as const,
      }

      const edges: Edge[] = [{ id: 'edge-test', source: mockPulseNode.id, target: 'something' }]

      const result = updatePulseStatusWithModel(mockPulseNode, errorStatusModel, edges, MOCK_THEME)

      expect(result.edges[0]).toMatchObject({
        id: 'edge-test',
        type: 'replace',
        item: expect.objectContaining({
          style: expect.objectContaining({
            stroke: MOCK_THEME.colors.status.error[500],
          }),
          markerEnd: expect.objectContaining({
            color: MOCK_THEME.colors.status.error[500],
          }),
        }),
      })
    })

    it('should not animate edges when operational status is INACTIVE', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.INACTIVE,
        source: 'PULSE' as const,
      }

      const edges: Edge[] = [{ id: 'edge-test', source: mockPulseNode.id, target: 'something' }]

      const result = updatePulseStatusWithModel(mockPulseNode, statusModel, edges, MOCK_THEME)

      expect(result.edges[0]).toMatchObject({
        item: expect.objectContaining({
          animated: false,
        }),
      })
    })

    it('should animate edges when both runtime and operational are ACTIVE', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'PULSE' as const,
      }

      const edges: Edge[] = [{ id: 'edge-test', source: mockPulseNode.id, target: 'something' }]

      const result = updatePulseStatusWithModel(mockPulseNode, statusModel, edges, MOCK_THEME)

      expect(result.edges[0]).toMatchObject({
        item: expect.objectContaining({
          animated: true,
        }),
      })
    })

    it('should handle empty edges array', () => {
      const statusModel = {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'PULSE' as const,
      }

      const edges: Edge[] = []

      const result = updatePulseStatusWithModel(mockPulseNode, statusModel, edges, MOCK_THEME)

      expect(result.edges).toHaveLength(0)
      expect(result.nodes.statusModel).toEqual(statusModel)
    })
  })
})
