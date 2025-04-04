import type { Connection, Node, NodeAddChange, XYPosition } from '@xyflow/react'

import type { DataPolicy } from '@/api/__generated__'

import { getNodeId } from '@datahub/utils/node.utils.ts'
import type { DataPolicyData, TopicFilterData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

export const loadTopicFilter = (
  policy: DataPolicy,
  dataPolicyNode: Node<DataPolicyData>
): (NodeAddChange | Connection)[] => {
  const position: XYPosition = {
    x: dataPolicyNode.position.x + CANVAS_POSITION.Topic.x,
    y: dataPolicyNode.position.y + CANVAS_POSITION.Topic.y,
  }

  const topicNode: Node<TopicFilterData> = {
    id: getNodeId(),
    type: DataHubNodeType.TOPIC_FILTER,
    position,
    data: {
      // TODO[DATAHBUB] Edge-related information (adapters) are not serialised
      adapter: undefined,
      topics: [policy.matching.topicFilter],
    },
  }

  return [
    { item: topicNode, type: 'add' },
    { source: topicNode.id, target: dataPolicyNode.id, sourceHandle: null, targetHandle: null },
  ]
}
