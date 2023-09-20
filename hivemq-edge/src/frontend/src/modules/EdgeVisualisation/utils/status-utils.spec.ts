import { expect } from 'vitest'
import { Node, NodeProps } from 'reactflow'

import { Adapter, Status } from '@/api/__generated__'

import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_LISTENER } from '@/__test-utils__/react-flow/nodes.ts'
import { updateNodeStatus } from '@/modules/EdgeVisualisation/utils/status-utils.ts'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

const disconnectedBridge: NodeProps = {
  ...MOCK_NODE_BRIDGE,
  data: {
    ...MOCK_NODE_BRIDGE.data,
    bridgeRuntimeInformation: {
      connectionStatus: {
        status: Status.connectionStatus.DISCONNECTED,
      },
    },
  },
}
const disconnectedAdapter: NodeProps<Adapter> = {
  ...MOCK_NODE_ADAPTER,
  data: {
    ...MOCK_NODE_ADAPTER.data,
    adapterRuntimeInformation: {
      connectionStatus: {
        status: Status.connectionStatus.DISCONNECTED,
      },
    },
  },
}

interface Suite {
  nodes: Node[]
  status: Status[]
  expected: Node[]
}
const validationSuite: Suite[] = [
  {
    nodes: [],
    status: [],
    expected: [],
  },
  {
    nodes: [],
    status: [
      { connectionStatus: Status.connectionStatus.DISCONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'one' },
      { connectionStatus: Status.connectionStatus.DISCONNECTED, type: NodeTypes.ADAPTER_NODE, id: 'two' },
    ],
    expected: [],
  },
  {
    // @ts-ignore
    nodes: [disconnectedBridge, disconnectedAdapter],
    status: [
      { connectionStatus: Status.connectionStatus.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'non-existing-bridge' },
    ],
    // @ts-ignore
    expected: [disconnectedBridge, disconnectedAdapter],
  },
  {
    // @ts-ignore
    nodes: [MOCK_NODE_LISTENER],
    status: [
      { connectionStatus: Status.connectionStatus.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: 'non-existing-bridge' },
    ],
    // @ts-ignore
    expected: [MOCK_NODE_LISTENER],
  },
  {
    // @ts-ignore
    nodes: [disconnectedBridge, disconnectedAdapter],
    status: [
      { connectionStatus: Status.connectionStatus.DISCONNECTED, type: NodeTypes.BRIDGE_NODE, id: mockBridgeId },
      { connectionStatus: Status.connectionStatus.DISCONNECTED, type: NodeTypes.ADAPTER_NODE, id: MOCK_ADAPTER_ID },
    ],
    // @ts-ignore
    expected: [disconnectedBridge, disconnectedAdapter],
  },
  {
    // @ts-ignore
    nodes: [disconnectedBridge, disconnectedAdapter],
    status: [
      { connectionStatus: Status.connectionStatus.CONNECTED, type: NodeTypes.BRIDGE_NODE, id: mockBridgeId },
      { connectionStatus: Status.connectionStatus.CONNECTED, type: NodeTypes.ADAPTER_NODE, id: MOCK_ADAPTER_ID },
    ],
    expected: expect.arrayContaining([
      expect.objectContaining({
        data: expect.objectContaining({
          bridgeRuntimeInformation: expect.objectContaining({
            connectionStatus: expect.objectContaining({ status: Status.connectionStatus.CONNECTED }),
          }),
        }),
      }),
    ]),
  },
]

describe('updateNodeStatus', () => {
  it.each<Suite>(validationSuite)('should work', ({ nodes, status, expected }) => {
    const updatedNodes = updateNodeStatus(nodes, status)
    expect(updatedNodes.length).toBe(nodes.length)
    expect(updatedNodes).toStrictEqual(expected)
  })
})
