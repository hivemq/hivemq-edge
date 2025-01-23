import type { Connection, Edge, HandleProps, Node } from 'reactflow'
import { getConnectedEdges, getIncomers, getOutgoers } from 'reactflow'
import { v4 as uuidv4 } from 'uuid'
import type { TFunction } from 'i18next'
import validator from '@rjsf/validator-ajv8'
import { RiPassExpiredLine, RiPassPendingLine, RiPassValidLine } from 'react-icons/ri'

import i18n from '@/config/i18n.config.ts'

import { MOCK_JSONSCHEMA_SCHEMA } from '../__test-utils__/schema.mocks.ts'
import type {
  ClientFilterData,
  DataHubNodeData,
  DryRunResults,
  FunctionData,
  SchemaData,
  TopicFilterData,
  ValidatorData,
  ValidDropConnection,
} from '../types.ts'
import {
  BehaviorPolicyData,
  DataHubNodeType,
  DataPolicyData,
  DesignerStatus,
  OperationData,
  PolicyDryRunStatus,
  ResourceStatus,
  ResourceWorkingVersion,
  SchemaType,
  StrategyType,
  TransitionData,
} from '../types.ts'
import { DataPolicyValidator } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { CustomNodeJSONSchema } from '@datahub/config/schemas.config.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export const getNodeId = (stub = 'node') => `${stub}_${uuidv4()}`

export const getNodePayload = (type: string, getDraftName?: (type: DataHubNodeType) => string): DataHubNodeData => {
  if (type === DataHubNodeType.TOPIC_FILTER) {
    return {
      topics: ['topic/example/1'],
    } as TopicFilterData
  }
  if (type === DataHubNodeType.CLIENT_FILTER) {
    return {
      clients: ['client/example/1'],
    } as ClientFilterData
  }

  if (type === DataHubNodeType.VALIDATOR) {
    return {
      type: DataPolicyValidator.type.SCHEMA,
      strategy: StrategyType.ALL_OF,
      schemas: [{ version: '1', schemaId: 'first mock schema' }],
    } as ValidatorData
  }
  if (type === DataHubNodeType.OPERATION) {
    return {
      functionId: undefined,
    } as OperationData
  }
  if (type === DataHubNodeType.SCHEMA) {
    return {
      name: getDraftName?.(DataHubNodeType.SCHEMA),
      type: SchemaType.JSON,
      schemaSource: MOCK_JSONSCHEMA_SCHEMA,
      internalStatus: ResourceStatus.DRAFT,
      version: ResourceWorkingVersion.DRAFT,
    } as SchemaData
  }
  return {}
}

type ConnectionValidity = Record<string, (DataHubNodeType | [DataHubNodeType, string])[]>

export const validConnections: ConnectionValidity = {
  [DataHubNodeType.TOPIC_FILTER]: [[DataHubNodeType.DATA_POLICY, DataPolicyData.Handle.TOPIC_FILTER]],
  [DataHubNodeType.VALIDATOR]: [[DataHubNodeType.DATA_POLICY, DataPolicyData.Handle.VALIDATION]],
  [DataHubNodeType.DATA_POLICY]: [DataHubNodeType.OPERATION],
  [DataHubNodeType.OPERATION]: [DataHubNodeType.OPERATION],
  [DataHubNodeType.SCHEMA]: [
    DataHubNodeType.VALIDATOR,
    [DataHubNodeType.OPERATION, OperationData.Handle.SCHEMA],
    [DataHubNodeType.OPERATION, OperationData.Handle.SERIALISER],
    [DataHubNodeType.OPERATION, OperationData.Handle.DESERIALISER],
  ],
  [DataHubNodeType.CLIENT_FILTER]: [[DataHubNodeType.BEHAVIOR_POLICY, BehaviorPolicyData.Handle.CLIENT_FILTER]],
  [DataHubNodeType.BEHAVIOR_POLICY]: [DataHubNodeType.TRANSITION],
  [DataHubNodeType.TRANSITION]: [DataHubNodeType.OPERATION],
  [DataHubNodeType.FUNCTION]: [[DataHubNodeType.OPERATION, OperationData.Handle.FUNCTION]],
}

export const isValidPolicyConnection = (connection: Connection, nodes: Node[], edges: Edge[]) => {
  const source = nodes.find((node) => node.id === connection.source)
  const destination = nodes.find((node) => node.id === connection.target)

  const hasCycle = (node: Node, visited = new Set()) => {
    if (visited.has(node.id)) return false

    visited.add(node.id)

    for (const outgoer of getOutgoers(node, nodes, edges)) {
      if (outgoer.id === connection.source) return true
      if (hasCycle(outgoer, visited)) return true
    }
    return false
  }

  if (!source || !destination) {
    return false
  }

  if (!source.type) {
    // node that are not Data Hub types are illegal
    return false
  }

  if (destination.id === source.id) {
    // self-connection are illegal
    return false
  }

  if (hasCycle(destination)) {
    // cycle are illegal
    return false
  }

  const connectionValidators = validConnections[source.type]
  if (!connectionValidators) {
    return false
  }
  return connectionValidators.some((elt) => {
    if (Array.isArray(elt)) {
      return destination?.type === elt[0] && connection.targetHandle === elt[1]
    }

    return destination?.type === elt
  })
}

/* istanbul ignore next -- @preserve */
export const getDryRunStatusIcon = (state?: PolicyDryRunStatus) => {
  switch (state) {
    case PolicyDryRunStatus.IDLE: {
      return RiPassPendingLine
    }
    case PolicyDryRunStatus.SUCCESS: {
      return RiPassValidLine
    }
    case PolicyDryRunStatus.FAILURE: {
      return RiPassExpiredLine
    }
    default: {
      return RiPassPendingLine
    }
  }
}

export const isTopicFilterNodeType = (node: Node): node is Node<TopicFilterData> =>
  node.type === DataHubNodeType.TOPIC_FILTER

export const isClientFilterNodeType = (node: Node): node is Node<ClientFilterData> =>
  node.type === DataHubNodeType.CLIENT_FILTER

export const isSchemaNodeType = (node: Node): node is Node<SchemaData> => node.type === DataHubNodeType.SCHEMA

export const isFunctionNodeType = (node: Node): node is Node<FunctionData> => node.type === DataHubNodeType.FUNCTION

export const isValidatorNodeType = (node: Node): node is Node<ValidatorData> => node.type === DataHubNodeType.VALIDATOR

export const isTransitionNodeType = (node: Node): node is Node<TransitionData> =>
  node.type === DataHubNodeType.TRANSITION

export const isDataPolicyNodeType = (node: Node): node is Node<DataPolicyData> =>
  node.type === DataHubNodeType.DATA_POLICY

export const isBehaviorPolicyNodeType = (node: Node): node is Node<BehaviorPolicyData> =>
  node.type === DataHubNodeType.BEHAVIOR_POLICY

// TODO[NVL] Would a map object->Object make this process easier and more performant?
export const getConnectedNodeFrom = (node?: string, handle?: string | null): ValidDropConnection | undefined => {
  if (node === DataHubNodeType.TOPIC_FILTER) {
    return {
      type: DataHubNodeType.DATA_POLICY,
      handle: DataPolicyData.Handle.TOPIC_FILTER,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.CLIENT_FILTER) {
    return {
      type: DataHubNodeType.BEHAVIOR_POLICY,
      handle: BehaviorPolicyData.Handle.CLIENT_FILTER,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.DATA_POLICY && handle === DataPolicyData.Handle.TOPIC_FILTER) {
    return {
      type: DataHubNodeType.TOPIC_FILTER,
      handle: null,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.DATA_POLICY && handle === DataPolicyData.Handle.VALIDATION) {
    return {
      type: DataHubNodeType.VALIDATOR,
      handle: null,
      isSource: true,
    }
  }
  if (
    node === DataHubNodeType.DATA_POLICY &&
    (handle === DataPolicyData.Handle.ON_SUCCESS || handle === DataPolicyData.Handle.ON_ERROR)
  ) {
    return {
      type: DataHubNodeType.OPERATION,
      handle: null,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.VALIDATOR && handle === 'source') {
    return {
      type: DataHubNodeType.DATA_POLICY,
      handle: DataPolicyData.Handle.VALIDATION,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.VALIDATOR && handle === 'target') {
    return {
      type: DataHubNodeType.SCHEMA,
      handle: null,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.SCHEMA) {
    // There are two possibilities: DataHubNodeType.VALIDATOR and DataHubNodeType.OPERATION (transform)
    // We need some form of feedback
    return undefined
  }
  if (node === DataHubNodeType.OPERATION && handle === OperationData.Handle.OUTPUT) {
    return {
      type: DataHubNodeType.OPERATION,
      handle: OperationData.Handle.INPUT,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.OPERATION && handle === OperationData.Handle.INPUT) {
    return {
      type: DataHubNodeType.OPERATION,
      handle: OperationData.Handle.OUTPUT,
      isSource: true,
    }
  }
  if (
    node === DataHubNodeType.OPERATION &&
    (handle === OperationData.Handle.SCHEMA ||
      handle === OperationData.Handle.SERIALISER ||
      handle === OperationData.Handle.DESERIALISER)
  ) {
    return {
      type: DataHubNodeType.SCHEMA,
      handle: null,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.OPERATION && handle === OperationData.Handle.FUNCTION) {
    return {
      type: DataHubNodeType.FUNCTION,
      handle: null,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.FUNCTION) {
    // This is mapping to a specific data content of the OPERATION node. Not supported yet
    return undefined
  }
  if (node === DataHubNodeType.BEHAVIOR_POLICY && handle === BehaviorPolicyData.Handle.CLIENT_FILTER) {
    return {
      type: DataHubNodeType.CLIENT_FILTER,
      handle: null,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.BEHAVIOR_POLICY && handle === BehaviorPolicyData.Handle.TRANSITIONS) {
    return {
      type: DataHubNodeType.TRANSITION,
      handle: null,
      isSource: false,
    }
  }
  if (node === DataHubNodeType.TRANSITION && handle === TransitionData.Handle.BEHAVIOR_POLICY) {
    return {
      type: DataHubNodeType.BEHAVIOR_POLICY,
      handle: BehaviorPolicyData.Handle.TRANSITIONS,
      isSource: true,
    }
  }
  if (node === DataHubNodeType.TRANSITION && handle === TransitionData.Handle.OPERATION) {
    return {
      type: DataHubNodeType.OPERATION,
      handle: null,
      isSource: false,
    }
  }
  return undefined
}

export const canDeleteNode = (node: Node, status?: DesignerStatus): { delete: boolean; error?: string | null } => {
  if (status === DesignerStatus.DRAFT) {
    return { delete: true }
  }
  if (isTopicFilterNodeType(node)) {
    return { delete: false, error: i18n.t('datahub:workspace.guards.delete.topicFilter') }
  }
  if (isBehaviorPolicyNodeType(node) || isDataPolicyNodeType(node)) {
    return { delete: false, error: i18n.t('datahub:workspace.guards.delete.policyNode') }
  }
  return { delete: true }
}

export const canDeleteEdge = (
  edge: Edge,
  nodes?: Node[],
  status?: DesignerStatus
): { delete: boolean; error?: string | null } => {
  if (status === DesignerStatus.DRAFT) return { delete: true }

  if (nodes) {
    const source = nodes.find((node) => node.id === edge.source)
    const target = nodes.find((node) => node.id === edge.target)
    if (source && target && isDataPolicyNodeType(target) && isTopicFilterNodeType(source))
      return { delete: false, error: i18n.t('datahub:workspace.guards.delete.topicFilter') }
  }
  return { delete: true }
}

export const getAllParents = (node: Node, nodes: Node[], edges: Edge[], visited = new Set<Node>()): Set<Node> => {
  if (visited.has(node)) return visited
  visited.add(node)
  for (const incomer of getIncomers(node, nodes, edges)) {
    getAllParents(incomer, nodes, edges, visited)
  }
  return visited
}

export const reduceIdsFrom =
  <T>(type: DataHubNodeType, exclude?: string) =>
  (acc: string[], node: Node) => {
    if (node.type === type) {
      const { id, data } = node satisfies Node<T>
      if (data.id && id !== exclude) {
        acc.push(data.id)
      }
    }
    return acc
  }

export interface ConnectableHandleProps extends Pick<HandleProps, 'id' | 'type'> {
  isConnectable?: boolean | number
}

export const isNodeHandleConnectable = (handle: ConnectableHandleProps, node: Node, edges: Edge[]) => {
  if (typeof handle.isConnectable === 'number') {
    const connectedEdges = getConnectedEdges([node], edges)
    const toHandle = connectedEdges.filter((edge) => {
      return handle.type === 'source'
        ? edge.source === node.id && edge.sourceHandle === handle.id
        : edge.target === node.id && edge.targetHandle === handle.id
    })
    return toHandle.length < handle.isConnectable
  }
  return handle.isConnectable
}

export const renderResourceName = (
  name: string | undefined,
  version: ResourceStatus.DRAFT | number | ResourceStatus.MODIFIED | undefined,
  t: TFunction
) => {
  if (!name || !version) return t('error.noSet.select', { ns: 'datahub' })

  const isDraft = enumFromStringValue(ResourceStatus, version.toString())
  const formatedVersion = !isDraft ? version : t('workspace.nodes.status', { context: version, ns: 'datahub' })

  return `${name}:${formatedVersion}`
}

export const validateNode = (newNode: Node) => {
  if (!newNode.type) return { isValid: false }
  const schema = CustomNodeJSONSchema[newNode.type]
  const validate = validator.ajv.compile(schema)
  const isValid = validate(newNode.data)
  return {
    isValid,
    errors: validate.errors,
  }
}

export const checkValidityConfigurations = (allNodes: Node[]) => {
  const allConfigurations: DryRunResults<unknown>[] = []
  for (const node of allNodes) {
    const nodeConfiguration = validateNode(node)
    if (!nodeConfiguration.isValid && nodeConfiguration?.errors?.length) {
      for (const error of nodeConfiguration.errors) {
        allConfigurations.push({
          node: node,
          error: PolicyCheckErrors.notValidated(node, error.message as string),
        })
      }
    }
  }
  return allConfigurations
}
