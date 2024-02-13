import { getIncomers, Node } from 'reactflow'

import { Schema, Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeData, DataHubNodeType, DataPolicyData, DryRunResults, PolicyDryRunStatus } from '@datahub/types.ts'
import {
  checkValidityFilter,
  checkValidityPipeline,
  getSubFlow,
} from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'
import { checkValidityPolicyValidators } from '@datahub/designer/validator/ValidatorNode.utils.ts'

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

      const filter = checkValidityFilter(dataPolicyNode, reducedStore)

      const validators = checkValidityPolicyValidators(dataPolicyNode, reducedStore)

      const onSuccessPipeline = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_SUCCESS, reducedStore)
      const onErrorPipeline = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_ERROR, reducedStore)

      const successResources = onSuccessPipeline.reduce((acc, oper) => {
        if (oper.resources) {
          acc.push(...oper.resources)
        }
        return acc
      }, [] as DryRunResults<Script>[])
      const errorResources = onErrorPipeline.reduce((acc, oper) => {
        if (oper.resources) {
          acc.push(...oper.resources)
        }
        return acc
      }, [] as DryRunResults<Script>[])
      const valid = validators.reduce((acc, vv) => {
        if (vv.resources) {
          acc.push(...vv.resources)
        }
        return acc
      }, [] as DryRunResults<Schema>[])

      const allResources = [...successResources, ...errorResources, ...valid]

      const runPolicyChecks = async () => {
        for (const node of allNodes) {
          onUpdateNodes<DataHubNodeData>(node.id, {
            ...node.data,
            dryRunStatus: PolicyDryRunStatus.RUNNING,
          })
          await mockDelay()
        }
        await mockDelay(1000)

        const processedNodes = [filter, ...validators, ...onSuccessPipeline, ...onErrorPipeline, ...allResources]
        for (const result of processedNodes) {
          await updateNodeStatus(result)
        }
        return processedNodes
      }

      return runPolicyChecks()
    },
  }
}
