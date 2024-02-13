import { getIncomers, Node } from 'reactflow'

import { Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeData, DataHubNodeType, DataPolicyData, DryRunResults, PolicyDryRunStatus } from '@datahub/types.ts'
import { getSubFlow } from '@datahub/utils/flow.utils.ts'
import { checkValidityFilter, checkValidityPipeline } from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'

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
