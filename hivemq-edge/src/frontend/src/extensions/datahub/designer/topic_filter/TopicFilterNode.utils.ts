import { DataPolicy } from '@/api/__generated__'
import { DataHubNodeType, TopicFilterData, WorkspaceAction, WorkspaceState } from '@datahub/types.ts'
import { Node, NodeAddChange, XYPosition } from 'reactflow'
import { getNodeId } from '@datahub/utils/node.utils.ts'

export const loadTopicFilter = (policy: DataPolicy, store: WorkspaceState & WorkspaceAction) => {
  const { onNodesChange, onConnect } = store
  const dataNode = store.nodes.find((n) => n.id === policy.id)
  if (!dataNode) throw new Error('cannot find the data policy node')

  const position: XYPosition = {
    x: dataNode.position.x - 300,
    y: dataNode.position.y,
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
