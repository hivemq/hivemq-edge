import { describe, expect } from 'vitest'
import { Edge, Node } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { DataPolicyValidator } from '@/api/__generated__'
import { MOCK_JSONSCHEMA_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import {
  ConnectableHandleProps,
  getAllParents,
  getNodeId,
  getNodePayload,
  isNodeHandleConnectable,
  isValidPolicyConnection,
  reduceIdsFrom,
} from '@datahub/utils/node.utils.ts'
import { DataHubNodeType, DataPolicyData, OperationData, SchemaType, StrategyType } from '@datahub/types.ts'

describe('getNodeId', () => {
  it('should return the initial state of the store', async () => {
    expect(getNodeId()).toContain('node_')
  })
})

interface NodePayloadSuite {
  type: string
  expected: unknown
}

describe('getNodePayload', () => {
  test.each<NodePayloadSuite>([
    {
      type: 'undefined',
      expected: {},
    },
    {
      type: DataHubNodeType.TOPIC_FILTER,
      expected: {
        topics: ['topic/example/1'],
      },
    },
    {
      type: DataHubNodeType.CLIENT_FILTER,
      expected: {
        clients: ['client/example/1'],
      },
    },
    {
      type: DataHubNodeType.VALIDATOR,
      expected: {
        schemas: [
          {
            schemaId: 'first mock schema',
            version: '1',
          },
        ],
        type: DataPolicyValidator.type.SCHEMA,
        strategy: StrategyType.ALL_OF,
      },
    },
    {
      type: DataHubNodeType.OPERATION,
      expected: {
        functionId: undefined,
      },
    },
    {
      type: DataHubNodeType.SCHEMA,
      expected: {
        internalStatus: 'DRAFT',
        type: SchemaType.JSON,
        version: 'DRAFT',
        schemaSource: MOCK_JSONSCHEMA_SCHEMA,
      },
    },
  ])('should returns the payload for $type ', ({ type, expected }) => {
    expect(getNodePayload(type)).toStrictEqual(expected)
  })
})

describe('isValidPolicyConnection', () => {
  it('should not be a valid connection with an unknown source', async () => {
    expect(
      isValidPolicyConnection({ source: 'source', target: 'target', sourceHandle: null, targetHandle: null }, [], [])
    ).toBeFalsy()
  })

  it('should not be a valid connection with an unknown source', async () => {
    const MOCK_NODE_DATA_POLICY: Node = {
      id: 'node-id',
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        { source: 'node-id', target: 'target', sourceHandle: null, targetHandle: null },
        [MOCK_NODE_DATA_POLICY],
        []
      )
    ).toBeFalsy()
  })

  it('should not be a valid connection with an uncontrolled connectivity', async () => {
    const MOCK_NODE_DATA_POLICY: Node = {
      id: 'node-id',
      type: 'Test',
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        { source: 'node-id', target: 'target', sourceHandle: null, targetHandle: null },
        [MOCK_NODE_DATA_POLICY],
        []
      )
    ).toBeFalsy()
  })

  it('should be a valid connection with a controlled connectivity', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_OPERATION: Node = {
      id: 'node-operation',
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        { source: 'node-id', target: 'node-operation', sourceHandle: null, targetHandle: null },
        [MOCK_NODE_DATA_POLICY, MOCK_NODE_OPERATION],
        []
      )
    ).toBeTruthy()
  })

  it('should be a valid connection with a controlled handle connectivity', async () => {
    const MOCK_NODE_DATA_POLICY: Node = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_OPERATION: Node = {
      id: 'node-operation',
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        {
          source: 'node-schema',
          target: 'node-operation',
          sourceHandle: null,
          targetHandle: OperationData.Handle.SCHEMA,
        },
        [MOCK_NODE_DATA_POLICY, MOCK_NODE_OPERATION],
        []
      )
    ).toBeTruthy()

    expect(
      isValidPolicyConnection(
        {
          source: 'node-schema',
          target: 'node-operation',
          sourceHandle: null,
          targetHandle: DataPolicyData.Handle.ON_SUCCESS,
        },
        [MOCK_NODE_DATA_POLICY, MOCK_NODE_OPERATION],
        []
      )
    ).toBeFalsy()
  })

  it('should not be a valid connection if self-referencing', async () => {
    const MOCK_NODE_OPERATION: Node = {
      id: 'node-operation',
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        {
          source: 'node-operation',
          target: 'node-operation',
          sourceHandle: null,
          targetHandle: null,
        },
        [MOCK_NODE_OPERATION],
        []
      )
    ).toBeFalsy()
  })

  it('should not be a valid connection if creating a cycle', async () => {
    const MOCK_NODE_OPERATION: Node = {
      id: 'node-operation',
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const nodes: Node[] = [
      MOCK_NODE_OPERATION,
      { ...MOCK_NODE_OPERATION, id: 'node-operation-1' },
      { ...MOCK_NODE_OPERATION, id: 'node-operation-2' },
    ]
    const edges: Edge[] = [
      { id: '1', source: 'node-operation', target: 'node-operation-1', sourceHandle: null, targetHandle: null },
      { id: '1', source: 'node-operation-1', target: 'node-operation-2', sourceHandle: null, targetHandle: null },
    ]

    expect(
      isValidPolicyConnection(
        {
          source: 'node-operation-1',
          target: 'node-operation',
          sourceHandle: null,
          targetHandle: null,
        },
        nodes,
        edges
      )
    ).toBeFalsy()
    expect(
      isValidPolicyConnection(
        {
          source: 'node-operation-2',
          target: 'node-operation',
          sourceHandle: null,
          targetHandle: null,
        },
        nodes,
        edges
      )
    ).toBeFalsy()
  })
})

describe('getAllParents', () => {
  it('should be', async () => {
    const node: Node = {
      id: '3',
      data: {},
      position: { x: 0, y: 0 },
    }

    const branch: Node[] = [
      {
        id: '1',
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: '2',
        data: {},
        position: { x: 0, y: 0 },
      },
      node,
    ]

    const nodes: Node[] = [
      ...branch,
      {
        id: '4',
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: '5',
        data: {},
        position: { x: 0, y: 0 },
      },
    ]

    const edges: Edge[] = [
      { id: '1', source: '1', target: '2', sourceHandle: null, targetHandle: null },
      { id: '2', source: '2', target: '3', sourceHandle: null, targetHandle: null },
      { id: '3', source: '3', target: '5', sourceHandle: null, targetHandle: null },
    ]

    const expected = [...branch].reverse()
    expect(Array.from(getAllParents(node, nodes, edges))).toEqual(expected)
  })
})

describe('reduceIdsFrom', () => {
  it('should be', async () => {
    const allNodes: Node[] = [
      {
        id: 'node-id',
        data: { id: 'first-id' },
        type: DataHubNodeType.DATA_POLICY,
        position: { x: 0, y: 0 },
      },
      {
        id: 'node-id',
        data: {},
        type: DataHubNodeType.DATA_POLICY,
        position: { x: 0, y: 0 },
      },
      {
        id: 'node-id',
        data: { id: 'second-id' },
        type: DataHubNodeType.OPERATION,
        position: { x: 0, y: 0 },
      },
      {
        id: 'excluded-node-id',
        data: { id: 'third-id' },
        type: DataHubNodeType.DATA_POLICY,
        position: { x: 0, y: 0 },
      },
    ]

    expect(allNodes.reduce(reduceIdsFrom<DataPolicyData>(DataHubNodeType.DATA_POLICY), [])).toEqual([
      'first-id',
      'third-id',
    ])
    expect(allNodes.reduce(reduceIdsFrom(DataHubNodeType.TRANSITION), [])).toEqual([])
    expect(allNodes.reduce(reduceIdsFrom<DataPolicyData>(DataHubNodeType.DATA_POLICY, 'excluded-node-id'), [])).toEqual(
      ['first-id']
    )
  })
})

describe('isNodeHandleConnectable', () => {
  const nodes: Node[] = [
    {
      id: '1',
      data: {},
      position: { x: 0, y: 0 },
    },
    {
      id: '2',
      data: {},
      position: { x: 0, y: 0 },
    },
    {
      id: '3',
      data: {},
      position: { x: 0, y: 0 },
    },
    {
      id: '3',
      data: {},
      position: { x: 0, y: 0 },
    },
  ]
  const edges: Edge[] = [
    { id: '1', source: '1', target: '3', sourceHandle: 'source1', targetHandle: 'target3' },
    { id: '2', source: '2', target: '3', sourceHandle: 'source2', targetHandle: 'target3' },
    { id: '3', source: '3', target: '4', sourceHandle: 'source3', targetHandle: 'target4' },
  ]

  it('should detect connectivity', async () => {
    const handle: ConnectableHandleProps = { id: 'source', type: 'source', isConnectable: false }
    expect(isNodeHandleConnectable(handle, nodes[0], edges)).toBeFalsy()
    expect(isNodeHandleConnectable({ ...handle, isConnectable: undefined }, nodes[0], edges)).toBeFalsy()
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 1 }, nodes[0], edges)).toBeTruthy()
  })

  it('should detect connectivity', async () => {
    const handle: ConnectableHandleProps = { id: 'target1', type: 'target', isConnectable: false }
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 1 }, nodes[0], edges)).toBeTruthy()
  })

  it('should detect connectivity', async () => {
    const handle: ConnectableHandleProps = { id: 'source1', type: 'source', isConnectable: false }
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 1 }, nodes[0], edges)).toBeFalsy()
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 2 }, nodes[0], edges)).toBeTruthy()
  })

  it('should detect connectivity', async () => {
    const handle: ConnectableHandleProps = { id: 'target3', type: 'target', isConnectable: false }
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 1 }, nodes[2], edges)).toBeFalsy()
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 2 }, nodes[2], edges)).toBeFalsy()
    expect(isNodeHandleConnectable({ ...handle, isConnectable: 3 }, nodes[2], edges)).toBeTruthy()
  })
})
