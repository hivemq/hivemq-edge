import { getIncomers, Node } from 'reactflow'
import { DataHubNodeType, DataPolicyData, DryRunResults, TopicFilterData, WorkspaceState } from '@datahub/types.ts'

export function checkValidityFilter(
  dataPolicyNode: Node<DataPolicyData>,
  store: WorkspaceState
): DryRunResults<string> {
  const { nodes, edges } = store

  const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.TOPIC_FILTER
  ) as Node<TopicFilterData>[]

  if (!incomers.length) {
    return {
      node: dataPolicyNode,
      error: {
        title: dataPolicyNode.type as string,
        status: 404,
        detail: 'No topic filter connected to the data policy',
        type: 'datahub.notConnected',
        id: dataPolicyNode.id,
      },
    }
  }

  // TODO[XXXX] Do we create multiple identical policies based on different topics
  if (incomers.length > 1) {
    return {
      node: incomers[0],
      error: {
        status: 404,
        title: incomers[0].type as string,
        detail: 'Too many topic filters connected to the data policy',
        type: 'datahub.cardinality',
        id: incomers[0].id,
      },
    }
  }

  return {
    node: incomers[0],
    // TODO[XXXX] Multiple topics?
    data: incomers[0].data.topics[0],
  }
}
