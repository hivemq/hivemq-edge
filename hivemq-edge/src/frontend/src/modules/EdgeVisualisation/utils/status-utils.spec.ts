import { expect } from 'vitest'
import { Node, NodeProps } from 'reactflow'
import * as CSS from 'csstype'
import { ResponsiveValue, ThemeTypings } from '@chakra-ui/react'

import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_LISTENER } from '@/__test-utils__/react-flow/nodes.ts'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { MOCK_THEME } from '@/__test-utils__/react-flow/utils.ts'

import { Adapter, Bridge, Status } from '@/api/__generated__'
import { mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

import { getThemeForStatus, updateNodeStatus } from '@/modules/EdgeVisualisation/utils/status-utils.ts'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

const disconnectedBridge: NodeProps<Bridge> = {
  ...MOCK_NODE_BRIDGE,
  data: {
    ...MOCK_NODE_BRIDGE.data,
    status: {
      connection: Status.connection.DISCONNECTED,
    },
  },
}
const disconnectedAdapter: NodeProps<Adapter> = {
  ...MOCK_NODE_ADAPTER,
  data: {
    ...MOCK_NODE_ADAPTER.data,
    status: {
      connection: Status.connection.DISCONNECTED,
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
  it.each<Suite>(validationSuite)('should work', ({ nodes, status, expected }) => {
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
