import { expect } from 'vitest'
import type { Edge, EdgeChange, Node, NodeProps } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import type * as CSS from 'csstype'
import type { ResponsiveValue, ThemeTypings } from '@chakra-ui/react'

import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_LISTENER,
  MOCK_NODE_PULSE,
  MOCK_NODE_DEVICE,
  MOCK_NODE_GROUP,
} from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'

import type { Adapter, Bridge, Combiner } from '@/api/__generated__'
import { PulseStatus } from '@/api/__generated__'
import { Status } from '@/api/__generated__'
import { mockBridgeId, mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_PULSE_STATUS_ERROR } from '@/api/hooks/usePulse/__handlers__'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombiner, mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'

import type { EdgeStyle } from './status-utils.ts'
import { updatePulseStatus } from './status-utils.ts'
import { getThemeForPulseStatus } from './status-utils.ts'
import {
  getEdgeStatus,
  getThemeForStatus,
  updateEdgesStatus,
  updateNodeStatus,
  createNewStatusEdgeForCombiner,
} from './status-utils.ts'
import { type EdgeStatus, EdgeTypes, type NodePulseType, NodeTypes } from '../types.ts'
import { RuntimeStatus, OperationalStatus, type NodeStatusModel } from '../types/status.types.ts'

const disconnectedBridge: NodeProps<Node<Bridge>> = {
  ...MOCK_NODE_BRIDGE,
  data: {
    ...MOCK_NODE_BRIDGE.data,
    status: {
      connection: Status.connection.DISCONNECTED,
    },
  },
}
const disconnectedAdapter: NodeProps<Node<Adapter>> = {
  ...MOCK_NODE_ADAPTER,
  data: {
    ...MOCK_NODE_ADAPTER.data,
    status: {
      connection: Status.connection.DISCONNECTED,
    },
  },
}

interface NodeSuite {
  nodes: Node[]
  status: Status[]
  expected: Node[]
}

const nodeUpdateTests: NodeSuite[] = [
  {
    nodes: [],
    status: [],
    expected: [],
  },
  {
    nodes: [],
    status: [
      { connection: Status.connection.DISCONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'one' },
      { connection: Status.connection.DISCONNECTED, type: NodeTypes.ADAPTER_NODE, id: 'two' },
    ],
    expected: [],
  },
  {
    // @ts-ignore
    nodes: [{ ...disconnectedBridge }, disconnectedAdapter],
    status: [{ connection: Status.connection.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'non-existing-bridge' }],
    // @ts-ignore
    expected: [disconnectedBridge, disconnectedAdapter],
  },
  {
    // @ts-ignore
    nodes: [MOCK_NODE_LISTENER],
    status: [{ connection: Status.connection.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'non-existing-bridge' }],
    // @ts-ignore
    expected: [MOCK_NODE_LISTENER],
  },
  {
    // @ts-ignore
    nodes: [{ ...disconnectedBridge }, disconnectedAdapter],
    status: [
      { connection: Status.connection.DISCONNECTED, type: NodeTypes.BRIDGE_NODE, id: mockBridgeId },
      { connection: Status.connection.DISCONNECTED, type: NodeTypes.ADAPTER_NODE, id: MOCK_ADAPTER_ID },
    ],
    // @ts-ignore
    expected: [disconnectedBridge, disconnectedAdapter],
  },
  {
    // @ts-ignore
    nodes: [disconnectedBridge, disconnectedAdapter],
    status: [
      { connection: Status.connection.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: mockBridgeId },
      { connection: Status.connection.CONNECTED, type: NodeTypes.ADAPTER_NODE, id: MOCK_ADAPTER_ID },
    ],
    expected: expect.arrayContaining([
      expect.objectContaining({
        data: expect.objectContaining({
          status: expect.objectContaining({
            connection: Status.connection.CONNECTED,
          }),
        }),
      }),
    ]),
  },
]

describe('updateNodeStatus', () => {
  it.each<NodeSuite>(nodeUpdateTests)('should work', ({ nodes, status, expected }) => {
    const updatedNodes = updateNodeStatus(nodes, status)
    expect(updatedNodes.length).toBe(nodes.length)
    expect(updatedNodes).toStrictEqual(expected)
  })
})

type Token<CSSType, ThemeKey = unknown> = ThemeKey extends keyof ThemeTypings
  ? ResponsiveValue<CSSType | ThemeTypings[ThemeKey]>
  : ResponsiveValue<CSSType>

interface StatusSuite {
  status?: Status
  expected: Token<CSS.Property.Color, 'colors'>
}

describe('getThemeForStatus', () => {
  it.each<StatusSuite>([
    { status: undefined, expected: '#E53E3E' },
    { status: { runtime: Status.runtime.STOPPED }, expected: '#E53E3E' },
    { status: { connection: Status.connection.CONNECTED }, expected: '#38A169' },
    { status: { connection: Status.connection.DISCONNECTED }, expected: '#718096' },
    { status: { connection: Status.connection.ERROR }, expected: '#E53E3E' },
    { status: { connection: Status.connection.UNKNOWN }, expected: '#E53E3E' },
    { status: { connection: Status.connection.STATELESS }, expected: '#38A169' },
  ])('should return $expected for $status', ({ status, expected }) => {
    const color = getThemeForStatus(MOCK_THEME, status)
    expect(color).toBe(expected)
  })
})

interface PulseStatusSuite {
  status?: PulseStatus
  expected: Token<CSS.Property.Color, 'colors'>
}

describe('getThemeForPulseStatus', () => {
  it.each<PulseStatusSuite>([
    { status: undefined, expected: '#E53E3E' },
    {
      status: { activation: PulseStatus.activation.ERROR, runtime: PulseStatus.runtime.CONNECTED },
      expected: '#E53E3E',
    },
    {
      status: { activation: PulseStatus.activation.ACTIVATED, runtime: PulseStatus.runtime.ERROR },
      expected: '#E53E3E',
    },
    {
      status: { activation: PulseStatus.activation.DEACTIVATED, runtime: PulseStatus.runtime.CONNECTED },
      expected: '#718096',
    },
    {
      status: { activation: PulseStatus.activation.ACTIVATED, runtime: PulseStatus.runtime.CONNECTED },
      expected: '#38A169',
    },
    {
      status: { activation: PulseStatus.activation.ACTIVATED, runtime: PulseStatus.runtime.DISCONNECTED },
      expected: '#718096',
    },

    // { status: { runtime: Status.runtime.STOPPED }, expected: '#E53E3E' },
    // { status: { connection: Status.connection.CONNECTED }, expected: '#38A169' },
    // { status: { connection: Status.connection.DISCONNECTED }, expected: '#718096' },
    // { status: { connection: Status.connection.ERROR }, expected: '#E53E3E' },
    // { status: { connection: Status.connection.UNKNOWN }, expected: '#E53E3E' },
    // { status: { connection: Status.connection.STATELESS }, expected: '#38A169' },
  ])('should return $expected for $status', ({ status, expected }) => {
    const color = getThemeForPulseStatus(MOCK_THEME, status)
    expect(color).toBe(expected)
  })
})

interface EdgeSuite {
  edges: Edge[]
  updates: Status[]
  expected: Node[]
}

const edgeUpdateTests: EdgeSuite[] = [
  {
    edges: [],
    updates: [],
    expected: [],
  },
]

describe('updateEdgesStatus', () => {
  it.each<EdgeSuite>(edgeUpdateTests)('should work', ({ edges, updates, expected }) => {
    const updatedNodes = updateEdgesStatus(
      [],
      edges,
      updates,
      () => ({
        ...MOCK_NODE_ADAPTER,
        position: { x: 0, y: 0 },
      }),
      MOCK_THEME
    )
    expect(updatedNodes.length).toBe(edges.length)
    expect(updatedNodes).toStrictEqual(expected)
  })
})

interface StatusStyleSuite {
  isConnected: boolean
  hasTopics: boolean
  expected: EdgeStyle<EdgeStatus>
}

describe('getEdgeStatus', () => {
  const color = MOCK_THEME.colors.status.connected[500]
  const edge: EdgeStyle<EdgeStatus> = {}

  edge.data = {
    hasTopics: true,
    isConnected: true,
  }
  edge.style = {
    strokeWidth: 1.5,
    stroke: color,
  }
  edge.animated = true
  edge.markerEnd = {
    type: MarkerType.ArrowClosed,
    width: 20,
    height: 20,
    color: color,
  }
  it.each<StatusStyleSuite>([
    {
      isConnected: true,
      hasTopics: true,
      expected: {
        ...edge,
        animated: true,
        style: {
          ...edge.style,
          strokeWidth: 1.5,
        },
      },
    },
    {
      isConnected: false,
      hasTopics: true,
      expected: {
        ...edge,
        data: {
          hasTopics: true,
          isConnected: false,
        },
        animated: false,
      },
    },
    {
      isConnected: true,
      hasTopics: false,
      expected: {
        ...edge,
        data: {
          hasTopics: false,
          isConnected: true,
        },
        animated: false,
        style: {
          ...edge.style,
          strokeWidth: 1.5,
        },
      },
    },
    {
      isConnected: false,
      hasTopics: false,
      expected: {
        ...edge,
        data: {
          hasTopics: false,
          isConnected: false,
        },
        animated: false,
      },
    },
  ])('should return the correct style for $isConnected and $hasTopics', ({ isConnected, hasTopics, expected }) => {
    const edgeStyle = getEdgeStatus(isConnected, hasTopics, true, color)
    expect(edgeStyle).toStrictEqual(expected)
  })

  it('should return correct style when hasMarker is false', () => {
    const color = MOCK_THEME.colors.status.connected[500]
    const edgeStyle = getEdgeStatus(true, true, false, color)
    expect(edgeStyle).toStrictEqual({
      data: {
        hasTopics: true,
        isConnected: true,
      },
      animated: true,
      style: {
        strokeWidth: 1.5,
        stroke: color,
      },
      markerEnd: undefined,
    })
  })
})

describe('updateEdgesStatus', () => {
  const mockGetNode = (id: string): Node | undefined => {
    const nodes: Record<string, Node> = {
      'idAdapter@adapter-id': {
        ...MOCK_NODE_ADAPTER,
        id: 'idAdapter@adapter-id',
        position: { x: 0, y: 0 },
        type: NodeTypes.ADAPTER_NODE,
      },
      'idBridge@bridge-id': {
        ...MOCK_NODE_BRIDGE,
        id: 'idBridge@bridge-id',
        position: { x: 0, y: 0 },
        type: NodeTypes.BRIDGE_NODE,
      },
      idListener: { ...MOCK_NODE_LISTENER, position: { x: 0, y: 0 } },
      idDevice: { ...MOCK_NODE_DEVICE, position: { x: 0, y: 0 }, type: NodeTypes.DEVICE_NODE },
      idGroup: { ...MOCK_NODE_GROUP, position: { x: 0, y: 0 } },
    }
    return nodes[id]
  }

  it('should return empty array for empty edges', () => {
    const result = updateEdgesStatus([], [], [], mockGetNode, MOCK_THEME)
    expect(result).toEqual([])
  })

  it('should handle edges without status updates', () => {
    const edges: Edge[] = [
      {
        id: 'edge-1',
        source: 'unknown@unknown-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const result = updateEdgesStatus([], edges, [], mockGetNode, MOCK_THEME)
    expect(result).toEqual(edges)
  })

  it('should update adapter node edges to device node', () => {
    const edges: Edge[] = [
      {
        id: 'edge-adapter-device',
        source: 'idAdapter@adapter-id',
        target: 'idDevice',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'adapter-id',
        type: 'idAdapter',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    ]
    const result = updateEdgesStatus([mockProtocolAdapter], edges, updates, mockGetNode, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      id: 'edge-adapter-device',
      data: {
        isConnected: true,
        hasTopics: false,
      },
      animated: false,
    })
  })

  it('should update adapter node edges to non-device node', () => {
    const edges: Edge[] = [
      {
        id: 'edge-adapter-listener',
        source: 'idAdapter@adapter-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'adapter-id',
        type: 'idAdapter',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    ]
    const result = updateEdgesStatus([mockProtocolAdapter], edges, updates, mockGetNode, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      id: 'edge-adapter-listener',
      data: {
        isConnected: true,
        hasTopics: false,
      },
      markerEnd: expect.objectContaining({
        type: MarkerType.ArrowClosed,
      }),
    })
  })

  it('should update bridge node edges with topics', () => {
    const bridgeWithTopics = {
      ...mockBridge,
      remoteSubscriptions: [{ filters: ['test/topic'], destination: 'dest', maxQoS: 0 }],
    }
    const mockGetNodeWithBridge = (id: string): Node | undefined => {
      if (id === 'idBridge@bridge-id') {
        return {
          ...MOCK_NODE_BRIDGE,
          id: 'idBridge@bridge-id',
          position: { x: 0, y: 0 },
          data: bridgeWithTopics,
          type: NodeTypes.BRIDGE_NODE,
        }
      }
      return mockGetNode(id)
    }

    const edges: Edge[] = [
      {
        id: 'edge-bridge-listener',
        source: 'idBridge@bridge-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'bridge-id',
        type: 'idBridge',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    ]
    const result = updateEdgesStatus([], edges, updates, mockGetNodeWithBridge, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      id: 'edge-bridge-listener',
      data: {
        isConnected: true,
        hasTopics: true,
      },
      animated: true,
    })
  })

  it('should update bridge node edges without topics', () => {
    const mockGetNodeWithBridge = (id: string): Node | undefined => {
      if (id === 'idBridge@bridge-id') {
        return {
          ...MOCK_NODE_BRIDGE,
          id: 'idBridge@bridge-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.BRIDGE_NODE,
        }
      }
      return mockGetNode(id)
    }

    const edges: Edge[] = [
      {
        id: 'edge-bridge-listener',
        source: 'idBridge@bridge-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'bridge-id',
        type: 'idBridge',
        connection: Status.connection.DISCONNECTED,
        runtime: Status.runtime.STOPPED,
      },
    ]
    const result = updateEdgesStatus([], edges, updates, mockGetNodeWithBridge, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      id: 'edge-bridge-listener',
      data: {
        isConnected: false,
        hasTopics: true,
      },
      animated: false,
    })
  })

  it('should handle group edges correctly', () => {
    const mockGetNodeForGroup = (id: string): Node | undefined => {
      const nodes: Record<string, Node> = {
        idGroup: {
          ...MOCK_NODE_GROUP,
          position: { x: 0, y: 0 },
          type: NodeTypes.CLUSTER_NODE,
          data: { childrenNodeIds: ['idAdapter@adapter-id', 'idBridge@bridge-id'], title: 'Group', isOpen: true },
        },
        'idAdapter@adapter-id': {
          ...MOCK_NODE_ADAPTER,
          id: 'idAdapter@adapter-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.ADAPTER_NODE,
        },
        'idBridge@bridge-id': {
          ...MOCK_NODE_BRIDGE,
          id: 'idBridge@bridge-id',
          position: { x: 0, y: 0 },
          type: NodeTypes.BRIDGE_NODE,
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
        data: { isConnected: true, hasTopics: false },
      },
      {
        id: 'edge-bridge',
        source: 'idBridge@bridge-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
        data: { isConnected: true, hasTopics: false },
      },
      {
        id: 'connect-edge-group-1',
        source: 'idGroup',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]

    const updates: Status[] = [
      {
        id: 'adapter-id',
        type: 'idAdapter',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
      {
        id: 'bridge-id',
        type: 'idBridge',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    ]

    const result = updateEdgesStatus([mockProtocolAdapter], edges, updates, mockGetNodeForGroup, MOCK_THEME)

    expect(result).toHaveLength(3)
    const groupEdge = result.find((e) => e.id === 'connect-edge-group-1')
    expect(groupEdge).toMatchObject({
      id: 'connect-edge-group-1',
      data: {
        isConnected: true,
        hasTopics: false,
      },
      animated: false,
    })
  })

  it('should handle group edges with non-group node', () => {
    const mockGetNodeNonGroup = (id: string): Node | undefined => {
      if (id === 'idAdapter@adapter-id') {
        return { ...MOCK_NODE_ADAPTER, id: 'idAdapter@adapter-id', position: { x: 0, y: 0 } }
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

    const result = updateEdgesStatus([], edges, [], mockGetNodeNonGroup, MOCK_THEME)
    // When the source is not a CLUSTER_NODE, the edge is not added to newEdges
    expect(result).toEqual([])
  })

  it('should handle stateless connection status', () => {
    const edges: Edge[] = [
      {
        id: 'edge-adapter',
        source: 'idAdapter@adapter-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'adapter-id',
        type: 'idAdapter',
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STARTED,
      },
    ]
    const result = updateEdgesStatus([mockProtocolAdapter], edges, updates, mockGetNode, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0].data?.isConnected).toBe(true)
  })

  it('should handle disconnected adapter nodes', () => {
    const edges: Edge[] = [
      {
        id: 'edge-adapter',
        source: 'idAdapter@adapter-id',
        target: 'idListener',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'adapter-id',
        type: 'idAdapter',
        connection: Status.connection.DISCONNECTED,
        runtime: Status.runtime.STOPPED,
      },
    ]
    const result = updateEdgesStatus([mockProtocolAdapter], edges, updates, mockGetNode, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0].data?.isConnected).toBe(false)
  })

  it('should push edge as-is when source is not adapter or bridge', () => {
    const edges: Edge[] = [
      {
        id: 'edge-other',
        source: 'idListener@listener-id',
        target: 'idAdapter@adapter-id',
        type: EdgeTypes.DYNAMIC_EDGE,
      },
    ]
    const updates: Status[] = [
      {
        id: 'listener-id',
        type: 'idListener',
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    ]
    const result = updateEdgesStatus([], edges, updates, mockGetNode, MOCK_THEME)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual(edges[0])
  })
})

describe('updatePulseStatus', () => {
  const expectedNode: NodePulseType['data'] = {
    id: 'idPulse',
    label: 'my pulse client',
    status: {
      activation: PulseStatus.activation.ACTIVATED,
      message: {
        title: 'Cannot connect to Pulse',
      },
      runtime: PulseStatus.runtime.ERROR,
    },
  }

  it('should update the Pulse node', async () => {
    expect(
      updatePulseStatus({ ...MOCK_NODE_PULSE, position: { x: 0, y: 0 } }, MOCK_PULSE_STATUS_ERROR, [], MOCK_THEME)
    ).toStrictEqual<{ nodes: NodePulseType['data']; edges: Partial<Edge>[] }>({
      edges: [],
      nodes: expectedNode,
    })
  })

  it('should update the connectors', async () => {
    const MOCK_EDGE: Edge[] = [
      {
        id: 'edge-test1',
        source: 'idAdapter',
        target: 'idAdapter2',
        type: EdgeTypes.REPORT_EDGE,
      },
      {
        id: 'edge-test2',
        source: 'idPulse',
        target: 'idAdapter3',
        type: EdgeTypes.REPORT_EDGE,
      },
      {
        id: 'edge-test3',
        source: 'idPulse',
        target: 'idAdapter4',
        type: EdgeTypes.REPORT_EDGE,
      },
    ]

    expect(
      updatePulseStatus(
        { ...MOCK_NODE_PULSE, position: { x: 0, y: 0 } },
        MOCK_PULSE_STATUS_ERROR,
        MOCK_EDGE,
        MOCK_THEME
      )
    ).toStrictEqual<{ nodes: NodePulseType['data']; edges: EdgeChange[] }>({
      nodes: expectedNode,
      edges: [
        {
          id: 'edge-test2',
          type: 'replace',
          item: expect.objectContaining({
            id: 'edge-test2',
            markerEnd: {
              color: '#E53E3E',
              height: 20,
              type: 'arrowclosed',
              width: 20,
            },
          }),
        },
        {
          id: 'edge-test3',
          type: 'replace',
          item: expect.objectContaining({
            id: 'edge-test3',
            markerEnd: {
              color: '#E53E3E',
              height: 20,
              type: 'arrowclosed',
              width: 20,
            },
          }),
        },
      ],
    })
  })
})

describe('createNewStatusEdgeForCombiner', () => {
  const mockEdge: Edge = {
    id: 'edge-test',
    source: 'source-node',
    target: 'combiner-node',
    type: EdgeTypes.DYNAMIC_EDGE,
  }

  const activeStatusModel: NodeStatusModel = {
    runtime: RuntimeStatus.ACTIVE,
    operational: OperationalStatus.ACTIVE,
    source: 'ADAPTER',
  }

  const inactiveStatusModel: NodeStatusModel = {
    runtime: RuntimeStatus.INACTIVE,
    operational: OperationalStatus.INACTIVE,
    source: 'ADAPTER',
  }

  it('should create edge with ACTIVE runtime and ACTIVE operational status when source is active and combiner has mappings', () => {
    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-test',
      animated: true,
      style: {
        strokeWidth: 1.5,
        stroke: MOCK_THEME.colors.status.connected[500], // Green for ACTIVE
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: MOCK_THEME.colors.status.connected[500],
      },
      data: {
        isConnected: true,
        hasTopics: true,
      },
    })
  })

  it('should create edge with ACTIVE runtime but INACTIVE operational status when source is active but combiner has no mappings', () => {
    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.INACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-test',
      animated: false, // No animation when operational is INACTIVE
      style: {
        strokeWidth: 1.5,
        stroke: MOCK_THEME.colors.status.connected[500], // Still green for ACTIVE runtime
      },
      data: {
        isConnected: true,
        hasTopics: false, // No topics when operational is INACTIVE
      },
    })
  })

  it('should create edge with INACTIVE runtime when source is inactive', () => {
    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(mockEdge, inactiveStatusModel, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-test',
      animated: false,
      style: {
        strokeWidth: 1.5,
        stroke: MOCK_THEME.colors.status.disconnected[500], // Gray for INACTIVE
      },
      data: {
        isConnected: false,
        hasTopics: true, // Combiner operational status
      },
    })
  })

  it('should create edge with ERROR runtime status', () => {
    const errorStatusModel: NodeStatusModel = {
      runtime: RuntimeStatus.ERROR,
      operational: OperationalStatus.ACTIVE,
      source: 'ADAPTER',
    }

    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(mockEdge, errorStatusModel, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-test',
      animated: false,
      style: {
        strokeWidth: 1.5,
        stroke: MOCK_THEME.colors.status.error[500], // Red for ERROR
      },
      data: {
        isConnected: false, // ERROR runtime means not connected
        hasTopics: true,
      },
    })
  })

  it('should use undefined source status model and fallback to INACTIVE runtime', () => {
    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(mockEdge, undefined, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-test',
      animated: false,
      style: {
        strokeWidth: 1.5,
        stroke: MOCK_THEME.colors.status.disconnected[500], // Gray for INACTIVE (fallback)
      },
      data: {
        isConnected: false,
        hasTopics: true,
      },
    })
  })

  describe('fallback path when combiner statusModel is not available', () => {
    it('should check mappings directly when statusModel is undefined - with mappings', () => {
      const combinerNode: Node = {
        id: 'combiner-node',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          ...mockCombiner,
          statusModel: undefined, // No statusModel
        },
      }

      const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

      expect(result).toMatchObject({
        id: 'edge-test',
        animated: true, // Has mappings, so operational is ACTIVE
        style: {
          strokeWidth: 1.5,
          stroke: MOCK_THEME.colors.status.connected[500],
        },
        data: {
          isConnected: true,
          hasTopics: true, // Has mappings
        },
      })
    })

    it('should check mappings directly when statusModel is undefined - without mappings', () => {
      const combinerNode: Node = {
        id: 'combiner-node',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          ...mockEmptyCombiner,
          statusModel: undefined, // No statusModel
        },
      }

      const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

      expect(result).toMatchObject({
        id: 'edge-test',
        animated: false, // No mappings, so operational is INACTIVE
        style: {
          strokeWidth: 1.5,
          stroke: MOCK_THEME.colors.status.connected[500],
        },
        data: {
          isConnected: true,
          hasTopics: false, // No mappings
        },
      })
    })

    it('should handle combiner with empty mappings items array', () => {
      const combinerNode: Node = {
        id: 'combiner-node',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          ...mockCombiner,
          mappings: { items: [] }, // Empty items
          statusModel: undefined,
        },
      }

      const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

      expect(result).toMatchObject({
        id: 'edge-test',
        animated: false,
        data: {
          isConnected: true,
          hasTopics: false, // Empty mappings
        },
      })
    })

    it('should handle combiner with null/undefined mappings', () => {
      const combinerNode: Node = {
        id: 'combiner-node',
        type: NodeTypes.COMBINER_NODE,
        position: { x: 0, y: 0 },
        data: {
          id: 'test-combiner',
          name: 'test',
          sources: { items: [] },
          mappings: undefined, // No mappings at all
          statusModel: undefined,
        } as unknown as Combiner,
      }

      const result = createNewStatusEdgeForCombiner(mockEdge, activeStatusModel, combinerNode, MOCK_THEME)

      expect(result).toMatchObject({
        id: 'edge-test',
        animated: false,
        data: {
          isConnected: true,
          hasTopics: false, // No mappings
        },
      })
    })
  })

  it('should preserve all edge properties when creating new edge', () => {
    const edgeWithExtraProps: Edge = {
      id: 'edge-with-props',
      source: 'source-node',
      target: 'combiner-node',
      type: EdgeTypes.DYNAMIC_EDGE,
      label: 'Test Edge',
      sourceHandle: 'handle-1',
      targetHandle: 'handle-2',
    }

    const combinerNode: Node = {
      id: 'combiner-node',
      type: NodeTypes.COMBINER_NODE,
      position: { x: 0, y: 0 },
      data: {
        ...mockCombiner,
        statusModel: {
          runtime: RuntimeStatus.ACTIVE,
          operational: OperationalStatus.ACTIVE,
          source: 'DERIVED',
        },
      },
    }

    const result = createNewStatusEdgeForCombiner(edgeWithExtraProps, activeStatusModel, combinerNode, MOCK_THEME)

    expect(result).toMatchObject({
      id: 'edge-with-props',
      source: 'source-node',
      target: 'combiner-node',
      type: EdgeTypes.DYNAMIC_EDGE,
      label: 'Test Edge',
      sourceHandle: 'handle-1',
      targetHandle: 'handle-2',
    })
  })
})
