import { getIncomers, Node } from 'reactflow'

import { Schema, Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import {
  BehaviorPolicyData,
  DataHubNodeData,
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  PolicyDryRunStatus,
} from '@datahub/types.ts'
import {
  checkValidityDataPolicy,
  checkValidityFilter,
  getSubFlow,
} from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'
import { checkValidityPolicyValidators } from '@datahub/designer/validator/ValidatorNode.utils.ts'
import { checkValidityClients } from '@datahub/designer/client_filter/ClientFilterNode.utils.ts'
import {
  checkValidityBehaviorPolicy,
  checkValidityModel,
} from '@datahub/designer/behavior_policy/BehaviorPolicyNode.utils.ts'
import { checkValidityTransitions } from '@datahub/designer/transition/TransitionNode.utils.ts'
import { checkValidityPipeline } from '@datahub/designer/operation/OperationNode.utils.ts'
import { isClientFilterNodeType, isTopicFilterNodeType } from '@datahub/utils/node.utils.ts'

/* istanbul ignore next -- @preserve */
const mockDelay = (ms = 100) => new Promise((resolve) => setTimeout(resolve, ms))

export const onlyNonNullResources = (acc: DryRunResults<unknown, never>[], oper: DryRunResults<unknown, never>) => {
  if (oper.resources) {
    acc.push(...oper.resources)
  }
  return acc
}

export const onlyUniqueResources = (acc: DryRunResults<unknown, never>[], item: DryRunResults<unknown, never>) => {
  const existingResourceIds = acc.map((e) => e.node.id)
  if (!existingResourceIds.includes(item.node.id)) {
    acc.push(item)
  }
  return acc
}

export const usePolicyDryRun = () => {
  const store = useDataHubDraftStore()
  const { nodes, edges, onUpdateNodes } = store

  /* istanbul ignore next -- @preserve */
  const updateNodeStatus = async (results: DryRunResults<unknown>) => {
    onUpdateNodes<DataHubNodeData>(results.node.id, {
      ...results.node.data,
      dryRunStatus: results.error ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS,
    })
    await mockDelay(500)
  }

  /* istanbul ignore next -- @preserve */
  const runPolicyChecks = async (
    allNodes: Node<DataHubNodeData>[],
    processedNodes: DryRunResults<unknown, never>[]
  ) => {
    for (const node of allNodes) {
      onUpdateNodes<DataHubNodeData>(node.id, {
        ...node.data,
        dryRunStatus: PolicyDryRunStatus.RUNNING,
      })
      await mockDelay(100)
    }

    for (const result of processedNodes) {
      await updateNodeStatus(result)
    }
    return processedNodes
  }

  const checkDataPolicyAsync = (dataPolicyNode: Node<DataPolicyData>) => {
    const incomers = getIncomers(dataPolicyNode, nodes, edges).filter(isTopicFilterNodeType)
    const allNodes = getSubFlow(dataPolicyNode, [dataPolicyNode, ...incomers], store) as Node<DataHubNodeData>[]
    const reducedStore = { ...store, nodes: allNodes }

    const filter = checkValidityFilter(dataPolicyNode, reducedStore)
    const validators = checkValidityPolicyValidators(dataPolicyNode, reducedStore)
    const onSuccessPipeline = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_SUCCESS, reducedStore)
    const onErrorPipeline = checkValidityPipeline(dataPolicyNode, DataPolicyData.Handle.ON_ERROR, reducedStore)

    const successResources = onSuccessPipeline.reduce(onlyNonNullResources, [] as DryRunResults<Script>[])
    const errorResources = onErrorPipeline.reduce(onlyNonNullResources, [] as DryRunResults<Script>[])
    const schemaResources = validators.reduce(onlyNonNullResources, [] as DryRunResults<Schema>[])
    const allResources = [...successResources, ...errorResources, ...schemaResources].reduce(onlyUniqueResources, [])

    const processedNodes = [filter, ...validators, ...onSuccessPipeline, ...onErrorPipeline, ...allResources]
    const hasError = processedNodes.some((e) => !!e.error)

    if (!hasError) {
      const dataPolicy = checkValidityDataPolicy(
        dataPolicyNode,
        filter,
        validators,
        onSuccessPipeline,
        onErrorPipeline,
        allResources
      )
      processedNodes.push(dataPolicy)
    }

    return runPolicyChecks(allNodes, processedNodes)
  }

  const checkBehaviorPolicyAsync = (behaviourPolicyNode: Node<BehaviorPolicyData>) => {
    /* istanbul ignore next -- @preserve */
    const incomers = getIncomers(behaviourPolicyNode, nodes, edges).filter(isClientFilterNodeType)
    const allNodes = getSubFlow(
      behaviourPolicyNode,
      [behaviourPolicyNode, ...incomers],
      store
    ) as Node<DataHubNodeData>[]
    const reducedStore = { ...store, nodes: allNodes }

    const clients = checkValidityClients(behaviourPolicyNode, reducedStore)
    const model = checkValidityModel(behaviourPolicyNode)
    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(behaviourPolicyNode, reducedStore)

    const pipelineResources = pipelines?.reduce(onlyNonNullResources, [] as DryRunResults<Schema>[])

    // TODO[19240] This is wrong. Only if no errors
    const behaviorPolicy = checkValidityBehaviorPolicy(behaviourPolicyNode, clients, model, behaviorPolicyTransitions)

    // TODO[19240] Remove
    /* istanbul ignore next -- @preserve */
    console.log('[DatHub] Payloads', {
      behaviorPolicy: behaviorPolicy.data,
      resources: behaviorPolicy.resources?.map((e) => e.data),
    })

    return runPolicyChecks(allNodes, [
      clients,
      model,
      ...behaviorPolicyTransitions,
      ...(pipelines || []),
      ...(pipelineResources || []),
      behaviorPolicy,
    ])
  }

  return {
    checkPolicyAsync: (policy: Node<BehaviorPolicyData> | Node<DataPolicyData>) => {
      if (policy.type === DataHubNodeType.DATA_POLICY) return checkDataPolicyAsync(policy as Node<DataPolicyData>)
      if (policy.type === DataHubNodeType.BEHAVIOR_POLICY)
        return checkBehaviorPolicyAsync(policy as Node<BehaviorPolicyData>)
      return Promise.reject(new Error(`Policy Type not supported : ${policy.type}`))
    },
  }
}
