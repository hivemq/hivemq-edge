import { Connection, Node } from 'reactflow'
import { v4 as uuidv4 } from 'uuid'
import { MOCK_JSONSCHEMA_SCHEMA } from '../__test-utils__/schema.mocks.ts'

import {
  BehaviorPolicyData,
  ClientFilterData,
  DataHubNodeData,
  DataHubNodeType,
  DataPolicyData,
  FunctionData,
  OperationData,
  PolicyDryRunStatus,
  SchemaData,
  SchemaType,
  StrategyType,
  TopicFilterData,
  TransitionData,
  ValidatorData,
} from '../types.ts'
import { RiPassExpiredLine, RiPassPendingLine, RiPassValidLine } from 'react-icons/ri'
import { DataPolicyValidator } from '@/api/__generated__'

export const getNodeId = (stub = 'node') => `${stub}_${uuidv4()}`

export const getNodePayload = (type: string): DataHubNodeData => {
  if (type === DataHubNodeType.TOPIC_FILTER) {
    return {
      topics: ['root/test1', 'root/test2'],
    } as TopicFilterData
  }
  if (type === DataHubNodeType.CLIENT_FILTER) {
    return {
      clients: ['client10', 'client20', 'client30'],
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
      type: SchemaType.JSON,
      version: 1,
      schemaSource: MOCK_JSONSCHEMA_SCHEMA,
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

export const isValidPolicyConnection = (connection: Connection, nodes: Node[]) => {
  const source = nodes.find((node) => node.id === connection.source)
  const destination = nodes.find((node) => node.id === connection.target)

  if (!source) {
    return false
  }
  const { type } = source
  if (!type) {
    return false
  }
  const connectionValidators = validConnections[type]
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
