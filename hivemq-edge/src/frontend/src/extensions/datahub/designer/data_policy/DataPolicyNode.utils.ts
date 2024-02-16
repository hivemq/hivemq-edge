import { getIncomers, getOutgoers, Node } from 'reactflow'

import { DataPolicy, DataPolicyValidator, PolicyOperation } from '@/api/__generated__'
import { DataHubNodeType, DataPolicyData, DryRunResults, TopicFilterData, WorkspaceState } from '@datahub/types.ts'
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

  // TODO[19240] Do we create multiple identical policies based on different topics
  if (incomers.length > 1) {
    return {
      node: incomers[0],
      error: PolicyCheckErrors.cardinality(DataHubNodeType.TOPIC_FILTER, dataPolicyNode),
    }
  }

  return {
    node: incomers[0],
    // TODO[19240] Multiple topics?
    data: incomers[0].data.topics[0],
  }
}

export const checkValidityDataPolicy = (
  dataPolicyNode: Node<DataPolicyData>,
  filter: DryRunResults<string>,
  validators: DryRunResults<DataPolicyValidator>[],
  onSuccessPipeline: DryRunResults<PolicyOperation>[],
  onErrorPipeline: DryRunResults<PolicyOperation>[],
  allResources: DryRunResults<unknown>[]
): DryRunResults<DataPolicy, unknown> => {
  return {
    node: dataPolicyNode,
    data: {
      // TODO[19240] Id is not handled (like in many nodes); use UUID default?
      id: dataPolicyNode.id,
      matching: { topicFilter: filter.data as string },
      validation: validators.length
        ? {
            validators: validators.map((validatorResults) => ({
              ...(validatorResults.data as DataPolicyValidator),
            })),
          }
        : undefined,
      onFailure: onErrorPipeline.length
        ? {
            pipeline: onErrorPipeline.map((policyOperationResults) => ({
              ...(policyOperationResults.data as PolicyOperation),
            })),
          }
        : undefined,
      onSuccess: onSuccessPipeline.length
        ? {
            pipeline: onSuccessPipeline.map((policyOperationResults) => ({
              ...(policyOperationResults.data as PolicyOperation),
            })),
          }
        : undefined,
    },
    resources: allResources,
  }
}
