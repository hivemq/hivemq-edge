import { getIncomers, Node } from 'reactflow'

import { BehaviorPolicy, Schema, Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DataHubNodeData, DataHubNodeType, DataPolicyData, DryRunResults, PolicyDryRunStatus } from '@datahub/types.ts'
import {
  checkValidityFilter,
  checkValidityPipeline,
  getSubFlow,
} from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'
import { checkValidityPolicyValidators } from '@datahub/designer/validator/ValidatorNode.utils.ts'
import { checkValidityClients } from '@datahub/designer/client_filter/ClientFilterNode.utils.ts'

const mockDelay = (ms = 100) => new Promise((resolve) => setTimeout(resolve, ms))

export const usePolicyDryRun = () => {
  const store = useDataHubDraftStore()
  const { nodes, edges, onUpdateNodes } = store

  const updateNodeStatus = async (results: DryRunResults<unknown>) => {
    onUpdateNodes<DataHubNodeData>(results.node.id, {
      ...results.node.data,
      dryRunStatus: results.error ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS,
    })
    await mockDelay()
  }

  const checkDataPolicyAsync = (dataPolicyNode: Node<DataPolicyData>) => {
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
  }

  const checkBehaviorPolicyAsync = (behaviourPolicyNode: Node<BehaviorPolicy>) => {
    const incomers = getIncomers(behaviourPolicyNode, nodes, edges).filter(
      (node) => node.type === DataHubNodeType.CLIENT_FILTER
    )
    const allNodes = getSubFlow(
      behaviourPolicyNode,
      [behaviourPolicyNode, ...incomers],
      store
    ) as Node<DataHubNodeData>[]
    const reducedStore = { ...store, nodes: allNodes }

    const clients = checkValidityClients(behaviourPolicyNode, reducedStore)

    const runPolicyChecks = async () => {
      for (const node of allNodes) {
        onUpdateNodes<DataHubNodeData>(node.id, {
          ...node.data,
          dryRunStatus: PolicyDryRunStatus.RUNNING,
        })
        await mockDelay()
      }
      await mockDelay(1000)

      const processedNodes = [clients]
      for (const result of processedNodes) {
        await updateNodeStatus(result)
      }
      return processedNodes
    }

    return runPolicyChecks()
  }

  return {
    checkPolicyAsync: (policy: Node<BehaviorPolicy> | Node<DataPolicyData>) => {
      if (policy.type === DataHubNodeType.DATA_POLICY) return checkDataPolicyAsync(policy as Node<DataPolicyData>)
      if (policy.type === DataHubNodeType.BEHAVIOR_POLICY)
        return checkBehaviorPolicyAsync(policy as Node<BehaviorPolicy>)
      return Promise.reject(new Error(`Policy Type not supported : ${policy.type}`))
    },
  }
}
