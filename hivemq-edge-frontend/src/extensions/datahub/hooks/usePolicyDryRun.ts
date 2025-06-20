import type { Node } from '@xyflow/react'
import { getIncomers } from '@xyflow/react'
import debug from 'debug'

import type { PolicySchema, Script } from '@/api/__generated__'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import type { BehaviorPolicyData, DataHubNodeData, DryRunResults } from '@datahub/types.ts'
import { DataHubNodeType, DataPolicyData, PolicyDryRunStatus } from '@datahub/types.ts'
import {
  checkValidityConfigurations,
  isClientFilterNodeType,
  isTopicFilterNodeType,
} from '@datahub/utils/node.utils.ts'
import { DRYRUN_VALIDATION_DELAY } from '@datahub/utils/datahub.utils.ts'
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
import { useFilteredFunctionsFetcher } from '@datahub/hooks/useFilteredFunctionsFetcher.tsx'

const datahubLog = debug('DataHub:usePolicyDryRun')

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
  const { getFilteredFunctions } = useFilteredFunctionsFetcher()

  const updateNodeStatus = async (results: DryRunResults<unknown>) => {
    const currentNode = nodes.find((node) => node.id === results.node.id)
    if (!currentNode) {
      datahubLog(`Node with ID ${results.node.id} not found in the current nodes list`)
      return
    }

    const getStatus = (): PolicyDryRunStatus => {
      if (results.error) return PolicyDryRunStatus.FAILURE
      // If current node was already marked as failure, keep it
      if (currentNode?.data.dryRunStatus === PolicyDryRunStatus.FAILURE) return PolicyDryRunStatus.FAILURE
      return PolicyDryRunStatus.SUCCESS
    }
    onUpdateNodes<DataHubNodeData>(currentNode.id, {
      ...currentNode.data,
      dryRunStatus: getStatus(),
    })
    await mockDelay(DRYRUN_VALIDATION_DELAY)
  }

  const runPolicyChecks = async (
    allNodes: Node<DataHubNodeData>[],
    processedNodes: DryRunResults<unknown, never>[]
  ) => {
    for (const node of allNodes) {
      onUpdateNodes<DataHubNodeData>(node.id, {
        ...node.data,
        dryRunStatus: PolicyDryRunStatus.RUNNING,
      })
      await mockDelay(DRYRUN_VALIDATION_DELAY)
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
    const schemaResources = validators.reduce(onlyNonNullResources, [] as DryRunResults<PolicySchema>[])
    const allResources = [...successResources, ...errorResources, ...schemaResources].reduce(onlyUniqueResources, [])

    const allConfigurations = checkValidityConfigurations(allNodes, getFilteredFunctions(DataHubNodeType.DATA_POLICY))

    const processedNodes = [
      ...allConfigurations,
      filter,
      ...validators,
      ...onSuccessPipeline,
      ...onErrorPipeline,
      ...allResources,
    ]
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

    const pipelineResources = pipelines?.reduce(onlyNonNullResources, [] as DryRunResults<PolicySchema>[])

    // TODO[29953] This is not enough, potential BehaviorPolicyTransitionEvent needs to be passed
    const allConfigurations = checkValidityConfigurations(
      allNodes,
      getFilteredFunctions(DataHubNodeType.BEHAVIOR_POLICY)
    )

    const processedNodes = [
      ...allConfigurations,
      clients,
      model,
      ...behaviorPolicyTransitions,
      ...(pipelines || []),
      ...(pipelineResources || []),
    ]

    const hasError = processedNodes.some((e) => !!e.error)
    if (!hasError) {
      const behaviorPolicy = checkValidityBehaviorPolicy(behaviourPolicyNode, clients, model, behaviorPolicyTransitions)
      processedNodes.push(behaviorPolicy)
    }

    return runPolicyChecks(allNodes, processedNodes)
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
