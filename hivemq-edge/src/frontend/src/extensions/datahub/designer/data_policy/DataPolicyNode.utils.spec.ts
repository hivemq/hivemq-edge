import { expect } from 'vitest'
import type { Node, NodeAddChange } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { DataPolicyData, TopicFilterData, WorkspaceState } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { checkValidityFilter, loadDataPolicy } from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'
import type { DataPolicy } from '@/api/__generated__'

describe('checkValidityFilter', () => {
  it('should return error if no topic filter connected', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityFilter(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Topic Filter connected to Data Policy',
        id: 'node-id',
        title: 'DATA_POLICY',
        type: 'datahub.notConnected',
      })
    )
  })

  it('should return error if too many filters connected', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_NODE_TOPICS: Node<TopicFilterData> = {
      id: 'node-topic1',
      type: DataHubNodeType.TOPIC_FILTER,
      data: {
        topics: ['topic 1'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_NODE_TOPICS_2: Node<TopicFilterData> = {
      id: 'node-topic2',
      type: DataHubNodeType.TOPIC_FILTER,
      data: {
        topics: ['topic 2'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_TOPICS_2, MOCK_NODE_TOPICS, MOCK_NODE_DATA_POLICY],
      edges: [
        {
          source: MOCK_NODE_TOPICS.id,
          sourceHandle: 'topic 1-0',
          target: MOCK_NODE_DATA_POLICY.id,
          targetHandle: 'topicFilter',
          id: '1',
        },

        {
          source: MOCK_NODE_TOPICS_2.id,
          sourceHandle: 'topic 2-0',
          target: MOCK_NODE_DATA_POLICY.id,
          targetHandle: 'topicFilter',
          id: '2',
        },
      ],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityFilter(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_TOPICS_2)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'Too many Topic Filter connected to Data Policy',
        id: 'node-id',
        title: 'DATA_POLICY',
        type: 'datahub.cardinality',
      })
    )
  })

  it('should return the payload otherwise', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_NODE_TOPICS: Node<TopicFilterData> = {
      id: 'node-topic1',
      type: DataHubNodeType.TOPIC_FILTER,
      data: {
        topics: ['topic 1'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_TOPICS, MOCK_NODE_DATA_POLICY],
      edges: [
        {
          source: MOCK_NODE_TOPICS.id,
          sourceHandle: 'topic 1-0',
          target: MOCK_NODE_DATA_POLICY.id,
          targetHandle: 'topicFilter',
          id: '1',
        },
      ],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityFilter(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_TOPICS)
    expect(resources).toBeUndefined()
    expect(data).toEqual('topic 1')
    expect(error).toBeUndefined()
  })
})

describe('loadDataPolicy', () => {
  const dataPolicy: DataPolicy = {
    id: 'string',
    matching: { topicFilter: '*.*' },
  }

  it('should return nodes', () => {
    expect(loadDataPolicy(dataPolicy)).toStrictEqual<NodeAddChange>({
      item: expect.objectContaining<Node<DataPolicyData>>({
        id: expect.stringContaining('node_'),
        type: DataHubNodeType.DATA_POLICY,
        data: { id: 'string' },
        position: { x: 0, y: 0 },
      }),
      type: 'add',
    })
  })
})
