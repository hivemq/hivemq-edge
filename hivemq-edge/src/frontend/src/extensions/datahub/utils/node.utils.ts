import { Edge, Node } from 'reactflow'
import { CSSProperties } from 'react'
import { MOCK_INITIAL_POLICY } from '../__test-utils__/react-flow.mocks.tsx'
import { MOCK_JSONSCHEMA_SCHEMA } from '../__test-utils__/schema-mocks.ts'

import {
  ClientFilterData,
  DataHubNodeType,
  OperationData,
  SchemaData,
  SchemaType,
  StrategyType,
  TopicFilterData,
  ValidatorData,
  ValidatorType,
} from '../types.ts'

export const styleSourceHandle: CSSProperties = {
  width: '12px',
  right: '-6px',
  borderRadius: 0,
  height: '12px',
}

export const initialFlow = () => {
  const nodes: Node[] = []
  const edges: Edge[] = []

  return { nodes, edges }
}

export const getNodeId = () => `node_${self.crypto.randomUUID()}`

export const getNodePayload = (type: string) => {
  if (type === DataHubNodeType.TOPIC_FILTER) {
    const payload: TopicFilterData = {
      topics: ['root/test1', 'root/test2'],
    }
    return payload
  }
  if (type === DataHubNodeType.CLIENT_FILTER) {
    const payload: ClientFilterData = {
      clients: ['client10', 'client20', 'client30'],
    }
    return payload
  }

  if (type === DataHubNodeType.VALIDATOR) {
    const payload: ValidatorData = {
      type: ValidatorType.SCHEMA,
      strategy: StrategyType.ALL_OF,
    }
    return payload
  }
  if (type === DataHubNodeType.OPERATION) {
    const payload: OperationData = {
      action: undefined,
    }
    return payload
  }
  if (type === DataHubNodeType.SCHEMA) {
    const payload: SchemaData = {
      type: SchemaType.JSON,
      version: '1',
      schemaSource: MOCK_JSONSCHEMA_SCHEMA,
    }
    return payload
  }
  return { label: `${type} node` }
}
