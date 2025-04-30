import { expect } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import { Position } from '@xyflow/react'

import { MOCK_LOCAL_STORAGE, MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_EDGE,
  MOCK_NODE_LISTENER,
} from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import type { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { mockMqttListener } from '@/api/hooks/useGateway/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import type { DeviceMetadata } from '../types.ts'
import { IdStubs, NodeTypes } from '../types.ts'
import {
  createAdapterNode,
  createBridgeNode,
  createCombinerNode,
  createEdgeNode,
  createListenerNode,
  getDefaultMetricsFor,
  getGluedPosition,
  LAYOUT_GLUE_TYPE,
} from './nodes-utils.ts'

describe('createEdgeNode', () => {
  it('should create a default Edge node', async () => {
    expect(createEdgeNode('test')).toStrictEqual<Node<{ label: string }, NodeTypes.EDGE_NODE>>({
      data: {
        label: 'test',
      },
      id: IdStubs.EDGE_NODE,
      position: {
        x: 300,
        y: 200,
      },
      type: NodeTypes.EDGE_NODE,
    })
  })

  it('should create an Edge node with stored location', async () => {
    expect(createEdgeNode('test', MOCK_LOCAL_STORAGE)).toStrictEqual<Node<{ label: string }, NodeTypes.EDGE_NODE>>(
      expect.objectContaining({
        position: {
          x: 1,
          y: 1,
        },
      })
    )
  })
})

describe('createBridgeNode', () => {
  it('should create a default Bridge node', async () => {
    const actual = createBridgeNode(mockBridge, 1, 2, MOCK_THEME)
    const expected: {
      nodeBridge: Node<Bridge, NodeTypes.BRIDGE_NODE>
      edgeConnector: Edge
      nodeHost: Node
      hostConnector: Edge
    } = {
      nodeBridge: expect.objectContaining({
        id: 'bridge@bridge-id-01',
        sourcePosition: Position.Top,
        type: NodeTypes.BRIDGE_NODE,
      }),

      nodeHost: expect.objectContaining({
        id: 'host@bridge-id-01',
        targetPosition: Position.Top,
        type: NodeTypes.HOST_NODE,
      }),
      hostConnector: expect.objectContaining({
        id: 'connect-host@bridge-id-01',
        markerEnd: expect.objectContaining({}),
        source: 'bridge@bridge-id-01',
        target: 'host@bridge-id-01',
        sourceHandle: 'Bottom',
      }),
      edgeConnector: expect.objectContaining({
        id: 'connect-edge-bridge@bridge-id-01',
        markerEnd: expect.objectContaining({}),
        source: 'bridge@bridge-id-01',
        target: 'edge',
        targetHandle: 'Bottom',
      }),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })

  it('should create a Bridge node with stored location', async () => {
    const actual = createBridgeNode(mockBridge, 1, 2, MOCK_THEME, MOCK_LOCAL_STORAGE)
    const expected: {
      nodeBridge: Node<Bridge, NodeTypes.BRIDGE_NODE>
      edgeConnector: Edge
      nodeHost: Node
      hostConnector: Edge
    } = {
      nodeBridge: expect.objectContaining({
        id: 'bridge@bridge-id-01',
        position: {
          x: 462.5,
          y: 600,
        },
      }),

      nodeHost: expect.objectContaining({
        id: 'host@bridge-id-01',
        position: {
          x: 462.5,
          y: 850,
        },
      }),
      hostConnector: expect.objectContaining({}),
      edgeConnector: expect.objectContaining({}),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })
})

describe('createListenerNode', () => {
  it('should create a default Listener node', async () => {
    const actual = createListenerNode(mockMqttListener, 1)
    const expected: {
      nodeListener: Node<Bridge, NodeTypes.LISTENER_NODE>
      edgeConnector: Edge
    } = {
      nodeListener: expect.objectContaining({
        id: 'listener@tcp-listener-1883',
        targetPosition: Position.Left,
        type: NodeTypes.LISTENER_NODE,
      }),
      edgeConnector: expect.objectContaining({}),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })

  it('should create a Bridge node with stored location', async () => {
    const actual = createListenerNode(mockMqttListener, 1, MOCK_LOCAL_STORAGE)
    const expected: {
      nodeListener: Node<Bridge, NodeTypes.LISTENER_NODE>
      edgeConnector: Edge
    } = {
      nodeListener: expect.objectContaining({
        id: 'listener@tcp-listener-1883',
        position: {
          x: -25,
          y: 280,
        },
      }),
      edgeConnector: expect.objectContaining({}),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })
})

describe('createAdapterNode', () => {
  it('should create a default Adapter node', async () => {
    const actual = createAdapterNode(mockProtocolAdapter, mockAdapter, 1, 2, MOCK_THEME)
    const expected: {
      nodeAdapter: Node<Bridge, NodeTypes.ADAPTER_NODE>
      nodeDevice: Node<DeviceMetadata, NodeTypes.DEVICE_NODE>
      edgeConnector: Edge
      deviceConnector: Edge
    } = {
      nodeAdapter: expect.objectContaining({
        id: `adapter@${MOCK_ADAPTER_ID}`,
        sourcePosition: Position.Bottom,
        type: NodeTypes.ADAPTER_NODE,
      }),
      edgeConnector: expect.objectContaining({
        id: `connect-edge-adapter@${MOCK_ADAPTER_ID}`,
        source: `adapter@${MOCK_ADAPTER_ID}`,
        animated: true,
        target: 'edge',
      }),
      nodeDevice: expect.objectContaining({
        id: 'device@adapter@my-adapter',
        targetPosition: Position.Top,
        type: NodeTypes.DEVICE_NODE,
      }),
      deviceConnector: expect.objectContaining({
        id: 'connect-device@adapter@my-adapter',
        source: 'adapter@my-adapter',
        animated: true,
        target: 'device@adapter@my-adapter',
      }),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })

  it('should create an Adapter node with stored location', async () => {
    const actual = createAdapterNode(mockProtocolAdapter, mockAdapter, 1, 2, MOCK_THEME, MOCK_LOCAL_STORAGE)
    const expected: {
      nodeAdapter: Node<Bridge, NodeTypes.ADAPTER_NODE>
      edgeConnector: Edge
    } = {
      nodeAdapter: expect.objectContaining({
        id: `adapter@${MOCK_ADAPTER_ID}`,
        position: {
          x: 625,
          y: -400,
        },
      }),
      edgeConnector: expect.objectContaining({}),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })
})

describe('getDefaultMetricsFor', () => {
  it('should return the default metrics', async () => {
    expect(getDefaultMetricsFor({ ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } })).toStrictEqual([])
    expect(getDefaultMetricsFor({ ...MOCK_NODE_BRIDGE, position: { x: 0, y: 0 } })).toStrictEqual([
      'com.hivemq.edge.bridge.bridge-id-01.forward.publish.count',
    ])
    expect(getDefaultMetricsFor({ ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } })).toStrictEqual([
      'com.hivemq.edge.protocol-adapters.simulation.my-adapter.read.publish.success.count',
    ])
  })
})

describe('createCombinerNode', () => {
  it('should create a default combiner node', async () => {
    const actual = createCombinerNode(mockCombiner, 0, [], MOCK_THEME)

    const mockId = '6991ff43-9105-445f-bce3-976720df40a3'
    expect(actual).toStrictEqual({
      nodeCombiner: expect.objectContaining({
        id: mockId,
        type: NodeTypes.COMBINER_NODE,
        data: expect.objectContaining({
          id: mockId,
          name: 'my-combiner',
          sources: {
            items: [
              {
                id: 'my-adapter',
                type: 'ADAPTER',
              },
              {
                id: 'my-other-adapter',
                type: 'ADAPTER',
              },
            ],
          },
        }),
      }),
      edgeConnector: expect.objectContaining({
        id: `connect-edge-${mockId}`,
        source: mockId,
        target: 'edge',
      }),
      sourceConnectors: [],
    })
  })

  it('should create links to sources', async () => {
    const actual = createCombinerNode(mockCombiner, 0, [{ ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } }], MOCK_THEME)

    const mockId = '6991ff43-9105-445f-bce3-976720df40a3'
    expect(actual).toStrictEqual({
      nodeCombiner: expect.objectContaining({
        id: mockId,
      }),
      edgeConnector: expect.objectContaining({
        id: `connect-edge-${mockId}`,
        source: mockId,
        target: 'edge',
      }),
      sourceConnectors: [
        expect.objectContaining({
          id: `connect-idEdge-${mockId}`,
          source: 'idEdge',
          target: '6991ff43-9105-445f-bce3-976720df40a3',
        }),
      ],
    })
  })
})

describe('getGluedPosition', () => {
  it('should handle unsupported node', async () => {
    const source: Node = { ...MOCK_NODE_LISTENER, position: { x: 0, y: 0 } }
    expect(getGluedPosition(source)).toStrictEqual({
      x: 0,
      y: 0,
    })
  })

  it('should handle nodes in a group', async () => {
    const source: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 }, parentId: 'my-group' }
    expect(getGluedPosition(source)).toStrictEqual({
      x: 0,
      y: -200,
    })
  })

  it('should handle top half-space', async () => {
    const source: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
    const centroid: Node = { ...MOCK_NODE_EDGE, position: { x: 50, y: 50 } }
    expect(getGluedPosition(source, centroid)).toStrictEqual({
      x: 0,
      y: -200,
    })
  })

  it('should handle bottom half-space', async () => {
    const source: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
    const centroid: Node = { ...MOCK_NODE_EDGE, position: { x: 50, y: -50 } }
    expect(getGluedPosition(source, centroid)).toStrictEqual({
      x: 0,
      y: 200,
    })
  })

  it('should handle radial position', async () => {
    const source: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
    const centroid: Node = { ...MOCK_NODE_EDGE, position: { x: 0, y: 50 } }
    expect(getGluedPosition(source, centroid, LAYOUT_GLUE_TYPE.RADIAL)).toStrictEqual({
      x: 0,
      y: -400,
    })
  })

  it('should handle default position', async () => {
    const source: Node = { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } }
    expect(getGluedPosition(source)).toStrictEqual({
      x: 0,
      y: -200,
    })
  })
})
