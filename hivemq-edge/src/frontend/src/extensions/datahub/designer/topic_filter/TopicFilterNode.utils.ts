import { Node, NodeAddChange, XYPosition } from 'reactflow'

import { DataPolicy } from '@/api/__generated__'
import i18n from '@/config/i18n.config.ts'

import { getNodeId } from '@datahub/utils/node.utils.ts'
import { DataHubNodeType, TopicFilterData, WorkspaceAction, WorkspaceState } from '@datahub/types.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

export const loadTopicFilter = (policy: DataPolicy, store: WorkspaceState & WorkspaceAction) => {
  const { onNodesChange, onConnect } = store
  const dataNode = store.nodes.find((node) => node.id === policy.id)
  if (!dataNode)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.DATA_POLICY }) as string
    )

  const position: XYPosition = {
    x: dataNode.position.x + CANVAS_POSITION.Topic.x,
    y: dataNode.position.y + CANVAS_POSITION.Topic.y,
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

  onNodesChange([{ item: topicNode, type: 'add' } as NodeAddChange])
  onConnect({ source: topicNode.id, target: dataNode.id, sourceHandle: null, targetHandle: null })
}
