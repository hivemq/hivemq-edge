import { describe, expect } from 'vitest'
import type { Edge, Node } from '@xyflow/react'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { DataPolicyValidator } from '@/api/__generated__'
import { MOCK_JSONSCHEMA_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import type { ConnectableHandleProps } from '@datahub/utils/node.utils.ts'
import {
  canDeleteEdge,
  canDeleteNode,
  getAllParents,
  getConnectedNodeFrom,
  getNodeId,
  getNodePayload,
  isNodeHandleConnectable,
  isValidPolicyConnection,
  reduceIdsFrom,
  renderResourceName,
} from '@datahub/utils/node.utils.ts'
import type { ValidDropConnection } from '@datahub/types.ts'
import {
  BehaviorPolicyData,
  DataHubNodeType,
  DataPolicyData,
  DesignerStatus,
  OperationData,
  ResourceWorkingVersion,
  SchemaType,
  StrategyType,
  TransitionData,
} from '@datahub/types.ts'
import i18n from '@/config/i18n.config.ts'

const NODE_OPERATION_ID = 'node-operation'

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
        name: undefined,
        internalStatus: 'DRAFT',
        type: SchemaType.JSON,
        version: ResourceWorkingVersion.DRAFT,
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
      id: NODE_OPERATION_ID,
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        { source: 'node-id', target: NODE_OPERATION_ID, sourceHandle: null, targetHandle: null },
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
      id: NODE_OPERATION_ID,
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        {
          source: 'node-schema',
          target: NODE_OPERATION_ID,
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
          target: NODE_OPERATION_ID,
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
      id: NODE_OPERATION_ID,
      type: DataHubNodeType.OPERATION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    expect(
      isValidPolicyConnection(
        {
          source: NODE_OPERATION_ID,
          target: NODE_OPERATION_ID,
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
      id: NODE_OPERATION_ID,
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
      { id: '1', source: NODE_OPERATION_ID, target: 'node-operation-1', sourceHandle: null, targetHandle: null },
      { id: '1', source: 'node-operation-1', target: 'node-operation-2', sourceHandle: null, targetHandle: null },
    ]

    expect(
      isValidPolicyConnection(
        {
          source: 'node-operation-1',
          target: NODE_OPERATION_ID,
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
          target: NODE_OPERATION_ID,
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

describe('canDeleteEdge', () => {
  it('should prevent illegal deletions', async () => {
    const allNodes: Node[] = [
      {
        id: 'target-dataPolicy',
        type: DataHubNodeType.DATA_POLICY,
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: 'source-TopicFilter',
        type: DataHubNodeType.TOPIC_FILTER,
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: 'node-operation1',
        data: { id: 'second-id' },
        type: DataHubNodeType.OPERATION,
        position: { x: 0, y: 0 },
      },
      {
        id: 'node-operation2',
        data: { id: 'third-id' },
        type: DataHubNodeType.OPERATION,
        position: { x: 0, y: 0 },
      },
    ]
    const edge: Edge[] = [
      {
        id: '1',
        source: 'source-TopicFilter',
        target: 'target-dataPolicy',
        sourceHandle: null,
        targetHandle: null,
      },
      {
        id: '2',
        source: 'node-operation1',
        target: 'node-operation2',
        sourceHandle: null,
        targetHandle: null,
      },
    ]

    expect(canDeleteEdge(edge[0], allNodes, DesignerStatus.DRAFT)).toStrictEqual({ delete: true })
    expect(canDeleteEdge(edge[0])).toStrictEqual({ delete: true })
    expect(canDeleteEdge(edge[0], allNodes, DesignerStatus.MODIFIED)).toStrictEqual({
      delete: false,
      error: 'Topic filter cannot be modified once published',
    })
    expect(canDeleteEdge(edge[1])).toStrictEqual({ delete: true })
  })
})

describe('canDeleteNode', () => {
  it('should prevent illegal deletions', async () => {
    const allNodes: Node[] = [
      {
        id: 'target-dataPolicy',
        type: DataHubNodeType.DATA_POLICY,
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: 'source-TopicFilter',
        type: DataHubNodeType.TOPIC_FILTER,
        data: {},
        position: { x: 0, y: 0 },
      },
      {
        id: 'node-behaviorPolicy',
        data: {},
        type: DataHubNodeType.BEHAVIOR_POLICY,
        position: { x: 0, y: 0 },
      },
    ]

    expect(canDeleteNode(allNodes[1], DesignerStatus.DRAFT)).toStrictEqual({ delete: true })
    expect(canDeleteNode(allNodes[1], DesignerStatus.MODIFIED)).toStrictEqual({
      delete: false,
      error: 'Topic filter cannot be modified once published',
    })
    expect(canDeleteNode(allNodes[0], DesignerStatus.MODIFIED)).toStrictEqual({
      delete: false,
      error: 'Policy node cannot be deleted once published',
    })
    expect(canDeleteNode(allNodes[2], DesignerStatus.MODIFIED)).toStrictEqual({
      delete: false,
      error: 'Policy node cannot be deleted once published',
    })
  })
})

interface ConnectionTest {
  node?: string
  handle?: string | null
  result: Partial<ValidDropConnection> | undefined
}

const connectionTestSuite: ConnectionTest[] = [
  {
    node: DataHubNodeType.TOPIC_FILTER,
    result: {
      type: DataHubNodeType.DATA_POLICY,
    },
  },
  {
    node: DataHubNodeType.CLIENT_FILTER,
    result: {
      type: DataHubNodeType.BEHAVIOR_POLICY,
    },
  },
  {
    node: DataHubNodeType.DATA_POLICY,
    handle: DataPolicyData.Handle.TOPIC_FILTER,
    result: {
      type: DataHubNodeType.TOPIC_FILTER,
    },
  },
  {
    node: DataHubNodeType.DATA_POLICY,
    handle: DataPolicyData.Handle.VALIDATION,
    result: {
      type: DataHubNodeType.VALIDATOR,
    },
  },
  {
    node: DataHubNodeType.DATA_POLICY,
    handle: DataPolicyData.Handle.ON_SUCCESS,
    result: {
      type: DataHubNodeType.OPERATION,
    },
  },
  {
    node: DataHubNodeType.DATA_POLICY,
    handle: DataPolicyData.Handle.ON_ERROR,
    result: {
      type: DataHubNodeType.OPERATION,
    },
  },

  {
    node: DataHubNodeType.VALIDATOR,
    handle: 'source',
    result: {
      type: DataHubNodeType.DATA_POLICY,
    },
  },
  {
    node: DataHubNodeType.VALIDATOR,
    handle: 'target',
    result: {
      type: DataHubNodeType.SCHEMA,
    },
  },
  {
    node: DataHubNodeType.SCHEMA,
    result: undefined,
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.OUTPUT,
    result: {
      type: DataHubNodeType.OPERATION,
      handle: OperationData.Handle.INPUT,
    },
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.INPUT,
    result: {
      type: DataHubNodeType.OPERATION,
      handle: OperationData.Handle.OUTPUT,
    },
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.SCHEMA,
    result: {
      type: DataHubNodeType.SCHEMA,
    },
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.SERIALISER,
    result: {
      type: DataHubNodeType.SCHEMA,
    },
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.DESERIALISER,
    result: {
      type: DataHubNodeType.SCHEMA,
    },
  },
  {
    node: DataHubNodeType.OPERATION,
    handle: OperationData.Handle.FUNCTION,
    result: {
      type: DataHubNodeType.FUNCTION,
    },
  },
  {
    node: DataHubNodeType.FUNCTION,
    result: undefined,
  },
  {
    node: DataHubNodeType.BEHAVIOR_POLICY,
    handle: BehaviorPolicyData.Handle.CLIENT_FILTER,
    result: {
      type: DataHubNodeType.CLIENT_FILTER,
    },
  },
  {
    node: DataHubNodeType.BEHAVIOR_POLICY,
    handle: BehaviorPolicyData.Handle.TRANSITIONS,
    result: {
      type: DataHubNodeType.TRANSITION,
    },
  },
  {
    node: DataHubNodeType.TRANSITION,
    handle: TransitionData.Handle.BEHAVIOR_POLICY,
    result: {
      type: DataHubNodeType.BEHAVIOR_POLICY,
    },
  },
  {
    node: DataHubNodeType.TRANSITION,
    handle: TransitionData.Handle.OPERATION,
    result: {
      type: DataHubNodeType.OPERATION,
    },
  },
  {
    node: DataHubNodeType.INTERNAL,
    result: undefined,
  },
]

describe('getConnectedNodeFrom', () => {
  it.each<ConnectionTest>(connectionTestSuite)(
    'should return a $result.type with $node + $handle',
    ({ node, handle, result }) => {
      expect(getConnectedNodeFrom(node, handle)).toStrictEqual(result ? expect.objectContaining(result) : undefined)
    }
  )
})

interface ResourceNameTest {
  name?: string
  version?: number
  result: string
}

const resourceNameTestSuite: ResourceNameTest[] = [
  {
    name: 'test',
    version: 1,
    result: 'test:1',
  },
  {
    version: 1,
    result: '< not set >',
  },
  {
    name: 'test',
    result: '< not set >',
  },
  {
    name: 'test',
    version: ResourceWorkingVersion.DRAFT,
    result: 'test:DRAFT',
  },
  {
    name: 'test',
    version: ResourceWorkingVersion.MODIFIED,
    result: 'test:MODIFIED',
  },
]

describe('renderResourceName', () => {
  it.each<ResourceNameTest>(resourceNameTestSuite)(
    'should return $result with $name and $version',
    ({ name, version, result }) => {
      expect(renderResourceName(name, version, i18n.t)).toStrictEqual(result)
    }
  )
})
