import { expect } from 'vitest'
import { Edge, Node, Position } from 'reactflow'

import { MOCK_LOCAL_STORAGE, MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockMqttListener } from '@/api/hooks/useGateway/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import { IdStubs, NodeTypes } from '../types.ts'
import { createAdapterNode, createBridgeNode, createEdgeNode, createListenerNode } from './nodes-utils.ts'

describe('createEdgeNode', () => {
  it('should create a default Edge node', async () => {
    expect(createEdgeNode('test')).toStrictEqual<Node<{ label: string }, NodeTypes.EDGE_NODE>>({
      data: {
        label: 'test',
      },
      draggable: false,
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
          x: 426.5,
          y: 500,
        },
      }),

      nodeHost: expect.objectContaining({
        id: 'host@bridge-id-01',
        position: {
          x: 426.5,
          y: 750,
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
          x: 47,
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
      edgeConnector: Edge
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
          x: 553,
          y: 0,
        },
      }),
      edgeConnector: expect.objectContaining({}),
    }

    expect(actual).toStrictEqual(expect.objectContaining(expected))
  })
})
