import { describe, expect } from 'vitest'
import { Edge, Node } from 'reactflow'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { DataPolicyValidator } from '@/api/__generated__'
import { MOCK_JSONSCHEMA_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { getNodeId, getNodePayload, isValidPolicyConnection } from '@datahub/utils/node.utils.ts'
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
