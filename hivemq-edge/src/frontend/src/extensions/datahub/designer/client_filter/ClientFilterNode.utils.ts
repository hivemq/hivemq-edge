import { getIncomers, Node, NodeAddChange, XYPosition } from 'reactflow'

import { getNodeId, isClientFilterNodeType } from '@datahub/utils/node.utils.ts'
import { BehaviorPolicy } from '@/api/__generated__'
import i18n from '@/config/i18n.config.ts'

import {
  BehaviorPolicyData,
  ClientFilterData,
  DataHubNodeType,
  DryRunResults,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValidityClients(
  dataPolicyNode: Node<BehaviorPolicyData>,
  store: WorkspaceState
): DryRunResults<string> {
  const { nodes, edges } = store

  const clients = getIncomers(dataPolicyNode, nodes, edges).filter(isClientFilterNodeType)

  if (!clients.length) {
    return {
      node: dataPolicyNode,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.CLIENT_FILTER, dataPolicyNode),
    }
  }

  // TODO[19240] Do we create multiple identical policies based on different topics
  if (clients.length > 1) {
    return {
      error: PolicyCheckErrors.cardinality(DataHubNodeType.CLIENT_FILTER, dataPolicyNode),
      node: clients[0],
    }
  }

  return {
    node: clients[0],
    // TODO[19240] Multiple clients?
    data: clients[0].data.clients[0],
  }
}

export const loadClientFilter = (behaviorPolicy: BehaviorPolicy, store: WorkspaceState & WorkspaceAction) => {
  const { onNodesChange, onConnect } = store
  const BehaviorPolicyNode = store.nodes.find((n) => n.id === behaviorPolicy.id)
  if (!BehaviorPolicyNode)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.BEHAVIOR_POLICY }) as string
    )
  const position: XYPosition = {
    x: BehaviorPolicyNode.position.x - 300,
    y: BehaviorPolicyNode.position.y,
  }

  const topicNode: Node<ClientFilterData> = {
    id: getNodeId(),
    type: DataHubNodeType.CLIENT_FILTER,
    position,
    data: {
      clients: [behaviorPolicy.matching.clientIdRegex],
    },
  }

  onNodesChange([{ item: topicNode, type: 'add' } as NodeAddChange])
  onConnect({ source: topicNode.id, target: BehaviorPolicyNode.id, sourceHandle: null, targetHandle: null })
}
