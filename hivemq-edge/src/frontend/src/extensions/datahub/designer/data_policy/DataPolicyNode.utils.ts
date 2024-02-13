import { getIncomers, Node } from 'reactflow'
import { DataHubNodeType, DataPolicyData, DryRunResults, TopicFilterData, WorkspaceState } from '@datahub/types.ts'
import { PolicyOperation } from '@/api/__generated__'
import { checkValidityTransformFunction } from '@datahub/designer/operation/OperationNode.utils.ts'

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

export function checkValidityPipeline(
  dataPolicyNode: Node<DataPolicyData>,
  handle: DataPolicyData.Handle,
  store: WorkspaceState
): DryRunResults<PolicyOperation>[] {
  const { nodes, edges } = store

  const getNextNode = (node: Node | undefined): Node | undefined => {
    if (node) {
      const outEdge = edges.find((edge) => edge.source === node.id)
      if (outEdge) {
        const nextNode = nodes.find((node) => node.id === outEdge.target)
        if (nextNode) return nextNode
      }
    }
    return undefined
  }

  const [outboundEdge] = edges.filter((edge) => edge.source === dataPolicyNode.id && edge.sourceHandle === handle)
  if (!outboundEdge) {
    return []
  }

  const pipeline: Node[] = []
  let nextNode = nodes.find((node) => node.id === outboundEdge.target)
  while (nextNode) {
    pipeline.push(nextNode)
    nextNode = getNextNode(nextNode)
  }

  return pipeline.map((node) => {
    if (!node.data.functionId) {
      return {
        node: node,
        error: {
          status: 404,
          title: node.type as string,
          detail: 'The event has not been defined',
          type: 'datahub.notDefined',
          id: node.id,
        },
      }
    }

    if (node.data.functionId === 'DataHub.transform') {
      return checkValidityTransformFunction(node, store)
    }

    // TODO[NVL] Serialisers need to be dealt with

    const operation: PolicyOperation = {
      functionId: node.data.functionId,
      arguments: node.data.formData,
      id: node.id,
    }
    return { node: node, data: operation }
  })
}
