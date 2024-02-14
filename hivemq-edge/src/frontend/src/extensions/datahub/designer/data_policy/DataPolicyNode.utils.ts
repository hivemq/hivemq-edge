import { getIncomers, getOutgoers, Node } from 'reactflow'
import { PolicyOperation } from '@/api/__generated__'

import { DataHubNodeType, DataPolicyData, DryRunResults, TopicFilterData, WorkspaceState } from '@datahub/types.ts'
import { checkValidityTransformFunction } from '@datahub/designer/operation/OperationNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function getSubFlow(source: Node, acc: Node[], store: WorkspaceState, only = false) {
  const allIds = Array.from(new Set(acc.map((e) => e.id)))
  const { nodes, edges } = store

  // This is wrong: should check it's from the right handles
  const incomers = getIncomers(source, nodes, edges).filter(
    (node) =>
      !allIds.includes(node.id) &&
      node.type !== DataHubNodeType.DATA_POLICY &&
      node.type !== DataHubNodeType.BEHAVIOR_POLICY
  )
  const outgoers = getOutgoers(source, nodes, edges).filter(
    (node) =>
      !allIds.includes(node.id) &&
      node.type !== DataHubNodeType.DATA_POLICY &&
      node.type !== DataHubNodeType.BEHAVIOR_POLICY
  )

  acc.push(...incomers, ...outgoers)

  if (outgoers.length) {
    outgoers.forEach((node) => {
      const subFlow = getSubFlow(node, acc, store)
      acc = Array.from(new Set([...acc, ...subFlow]))
    })
  }

  if (incomers.length && !only) {
    incomers.forEach((node) => {
      const subFlow = getSubFlow(node, acc, store)
      acc = Array.from(new Set([...acc, ...subFlow]))
    })
  }

  return acc
}

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
      error: PolicyCheckErrors.notConnected(DataHubNodeType.TOPIC_FILTER, dataPolicyNode),
    }
  }

  // TODO[XXXX] Do we create multiple identical policies based on different topics
  if (incomers.length > 1) {
    return {
      node: incomers[0],
      error: PolicyCheckErrors.cardinality(DataHubNodeType.TOPIC_FILTER, dataPolicyNode),
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
        error: PolicyCheckErrors.notConfigured(node, 'functionId'),
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
