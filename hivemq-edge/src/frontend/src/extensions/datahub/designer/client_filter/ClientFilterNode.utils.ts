import { getIncomers, Node } from 'reactflow'

import { BehaviorPolicy } from '@/api/__generated__'
import { ClientFilterData, DataHubNodeType, DryRunResults, WorkspaceState } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValidityClients(
  dataPolicyNode: Node<BehaviorPolicy>,
  store: WorkspaceState
): DryRunResults<string> {
  const { nodes, edges } = store

  const clients = getIncomers(dataPolicyNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.CLIENT_FILTER
  ) as Node<ClientFilterData>[]

  if (!clients.length) {
    return {
      node: dataPolicyNode,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.CLIENT_FILTER, dataPolicyNode),
    }
  }

  // TODO[XXXX] Do we create multiple identical policies based on different topics
  if (clients.length > 1) {
    return {
      error: PolicyCheckErrors.cardinality(DataHubNodeType.CLIENT_FILTER, dataPolicyNode),
      node: clients[0],
    }
  }

  return {
    node: clients[0],
    // TODO[XXXX] Multiple clients?
    data: clients[0].data.clients[0],
  }
}
