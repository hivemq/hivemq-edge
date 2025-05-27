import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from '@xyflow/react'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { type DataPolicy } from '@/api/__generated__'
import type { DataPolicyData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { loadTopicFilter } from '@datahub/designer/topic_filter/TopicFilterNode.utils.ts'

describe('loadTopicFilter', () => {
  const dataPolicy: DataPolicy = {
    id: 'string',
    matching: { topicFilter: '*.*' },
  }

  const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.DATA_POLICY,
    data: { id: 'my-policy-id' },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }
  it('should return nodes', () => {
    expect(loadTopicFilter(dataPolicy, MOCK_NODE_DATA_POLICY)).toStrictEqual<(NodeAddChange | Connection)[]>([
      expect.objectContaining<NodeAddChange>({
        item: {
          data: {
            adapter: undefined,
            topics: ['*.*'],
          },
          id: expect.stringContaining('node_'),
          position: {
            x: -320,
            y: 0,
          },
          type: DataHubNodeType.TOPIC_FILTER,
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: expect.stringContaining('node_'),
        target: 'node-id',
      }),
    ])
  })
})
