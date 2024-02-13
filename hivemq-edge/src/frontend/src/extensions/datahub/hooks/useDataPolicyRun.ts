import { getIncomers, Node } from 'reactflow'

import { Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeData, DataHubNodeType, DataPolicyData, DryRunResults, PolicyDryRunStatus } from '@datahub/types.ts'
import {
  checkValidityFilter,
  checkValidityPipeline,
  getSubFlow,
} from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'

const mockDelay = (ms = 100) => new Promise((resolve) => setTimeout(resolve, ms))

export const useDataPolicyRun = () => {
  const store = useDataHubDraftStore()
  const { nodes, edges, onUpdateNodes } = store

  const updateNodeStatus = async (results: DryRunResults<unknown>) => {
    onUpdateNodes<DataHubNodeData>(results.node.id, {
      ...results.node.data,
      dryRunStatus: results.error ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS,
    })
    await mockDelay()
  }

  return {
    checkPolicyAsync: (dataPolicyNode: Node<DataPolicyData>) => {
      const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(
        (node) => node.type === DataHubNodeType.TOPIC_FILTER
      )
      const allNodes = getSubFlow(dataPolicyNode, [dataPolicyNode, ...incomers], store) as Node<DataHubNodeData>[]
      const reducedStore = { ...store, nodes: allNodes }

      const filter = checkValidityFilter(dataPolicyNode, {
        ...store,
        nodes: allNodes,
      })

      const onSuccess = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_SUCCESS, reducedStore)
      const onError = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_ERROR, reducedStore)

      const successResources = onSuccess.reduce((acc, oper) => {
        if (oper.resources) {
          acc.push(...oper.resources)
        }
        return acc
      }, [] as DryRunResults<Script>[])
      const errorResources = onError.reduce((acc, oper) => {
        if (oper.resources) {
          acc.push(...oper.resources)
        }
        return acc
      }, [] as DryRunResults<Script>[])
      const allResources = [...successResources, ...errorResources]

      const runPolicyChecks = async () => {
        for (const node of allNodes) {
          onUpdateNodes<DataHubNodeData>(node.id, {
            ...node.data,
            dryRunStatus: PolicyDryRunStatus.RUNNING,
          })
          await mockDelay()
        }

        const processedNodes = [filter, ...onSuccess, ...onError, ...allResources]
        for (const result of processedNodes) {
          await updateNodeStatus(result)
        }
        return processedNodes
      }

      return runPolicyChecks()
    },
  }
}

// const useDataPolicyRunXXXX = (dataPolicyNode: NodeProps<DataPolicyData>) => {
//   const store = useDataHubDraftStore()
//   const { nodes } = store
//
//   const selectedNode = nodes.find((node) => node.id === dataPolicyNode.id)
//   if (!selectedNode)
//     return [
//       {
//         status: 404,
//         title: 'The data policy could not be identified',
//         type: 'datahub.notFound',
//       } as ProblemDetails,
//     ]
//
//   const filter = checkValidityFilter(selectedNode, store)
//   if (!filter.data) return filter.error
//
//   const onSuccess = checkValidityPipeline(selectedNode, DataPolicyData.Handle.ON_SUCCESS, store)
//   const successOperations = onSuccess.filter((e) => e.data && e.data?.functionId).map((e) => e.data as PolicyOperation)
//   const onError = checkValidityPipeline(selectedNode, DataPolicyData.Handle.ON_ERROR, store)
//   const errorOperations = onError.filter((e) => e.data && e.data?.functionId).map((e) => e.data as PolicyOperation)
//
//   const ff: DataPolicy = {
//     id: dataPolicyNode.id,
//     createdAt: DateTime.now().toISO() || undefined,
//     matching: { topicFilter: filter.data },
//     onSuccess: successOperations.length ? { pipeline: successOperations } : undefined,
//     onFailure: errorOperations.length ? { pipeline: errorOperations } : undefined,
//   }
//
//   return ff
// }
