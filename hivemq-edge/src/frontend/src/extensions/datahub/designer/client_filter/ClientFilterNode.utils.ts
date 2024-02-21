import { getIncomers, Node } from 'reactflow'

import { BehaviorPolicyData, DataHubNodeType, DryRunResults, WorkspaceState } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { isClientFilterNodeType } from '@datahub/utils/node.utils.ts'

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
