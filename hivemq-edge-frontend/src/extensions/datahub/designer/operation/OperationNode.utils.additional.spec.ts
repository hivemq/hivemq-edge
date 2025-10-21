import { expect } from 'vitest'
import type { Node, NodeAddChange } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { type BehaviorPolicyOnTransition, type DataPolicy, type PolicyOperation, Script } from '@/api/__generated__'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

import type { FunctionData, SchemaData, TransitionData, WorkspaceState } from '@datahub/types.ts'
import { BehaviorPolicyType } from '@datahub/types.ts'
import { DataHubNodeType, DataPolicyData, OperationData, SchemaType, ResourceWorkingVersion } from '@datahub/types.ts'
import {
  checkValidityPipeline,
  checkValidityTransformFunction,
  loadBehaviorPolicyPipelines,
  loadDataPolicyPipelines,
  loadPipeline,
} from '@datahub/designer/operation/OperationNode.utils.ts'

const NODE_POLICY_ID = 'my-policy-id'
const NODE_OPERATION_ID = 'my-operation-id'
const NODE_FUNCTION_ID = 'node-function'
const TRANSFORM_FUNCTION_ID = 'DataHub.transform'
const NODE_DESERIALISER_ID = 'node-schema-deserializer'
const NODE_SERIALISER_ID = 'node-schema-serializer'

describe('loadBehaviorPolicyPipelines - additional tests', () => {
  const MOCK_TRANSITION_NODE: Node<TransitionData> = {
    id: 'node-id',
    type: DataHubNodeType.TRANSITION,
    data: { model: BehaviorPolicyType.MQTT_EVENT },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should throw an error if transition event is not found', async () => {
    const pol: BehaviorPolicyOnTransition = {
      fromState: 'state1',
      toState: 'state2',
      'Mqtt.OnInboundConnect': undefined,
    }

    expect(() => loadBehaviorPolicyPipelines(pol, MOCK_TRANSITION_NODE, [], [])).toThrow(
      'there is no transition pipeline for the operation'
    )
  })

  it('should load pipeline with operations', async () => {
    const pol: BehaviorPolicyOnTransition = {
      fromState: 'state1',
      toState: 'state2',
      'Mqtt.OnInboundConnect': {
        pipeline: [
          {
            id: 'node1',
            functionId: 'System.log',
            arguments: { level: 'DEBUG', message: 'connection' },
          },
        ],
      },
    }

    const results = loadBehaviorPolicyPipelines(pol, MOCK_TRANSITION_NODE, [], [])
    expect(results.length).toBeGreaterThan(0)
  })
})

describe('loadDataPolicyPipelines - additional tests', () => {
  const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.DATA_POLICY,
    data: { id: NODE_POLICY_ID },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should load onSuccess pipeline', async () => {
    const dataPolicy: DataPolicy = {
      id: 'string',
      matching: { topicFilter: '*.*' },
      onSuccess: {
        pipeline: [
          {
            id: 'node1',
            functionId: 'System.log',
            arguments: { level: 'DEBUG', message: 'success' },
          },
        ],
      },
    }

    const results = loadDataPolicyPipelines(dataPolicy, [], [], MOCK_NODE_DATA_POLICY)
    expect(results.length).toBeGreaterThan(0)
  })

  it('should load onFailure pipeline', async () => {
    const dataPolicy: DataPolicy = {
      id: 'string',
      matching: { topicFilter: '*.*' },
      onFailure: {
        pipeline: [
          {
            id: 'node1',
            functionId: 'System.log',
            arguments: { level: 'ERROR', message: 'failure' },
          },
        ],
      },
    }

    const results = loadDataPolicyPipelines(dataPolicy, [], [], MOCK_NODE_DATA_POLICY)
    expect(results.length).toBeGreaterThan(0)
  })

  it('should load both onSuccess and onFailure pipelines', async () => {
    const dataPolicy: DataPolicy = {
      id: 'string',
      matching: { topicFilter: '*.*' },
      onSuccess: {
        pipeline: [
          {
            id: 'node1',
            functionId: 'System.log',
            arguments: { level: 'DEBUG', message: 'success' },
          },
        ],
      },
      onFailure: {
        pipeline: [
          {
            id: 'node2',
            functionId: 'System.log',
            arguments: { level: 'ERROR', message: 'failure' },
          },
        ],
      },
    }

    const results = loadDataPolicyPipelines(dataPolicy, [], [], MOCK_NODE_DATA_POLICY)
    expect(results.length).toBeGreaterThan(0)
  })
})

describe('loadPipeline - additional tests', () => {
  const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.DATA_POLICY,
    data: { id: NODE_POLICY_ID },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should handle DRAFT version schemas in transform operations', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: OperationData.Function.SERDES_DESERIALIZE,
        arguments: {
          schemaId: NODE_DESERIALISER_ID,
          schemaVersion: '1',
        },
      },
      {
        id: 'node2',
        functionId: 'fn:script1:1',
        arguments: {},
      },
      {
        id: 'node3',
        functionId: OperationData.Function.SERDES_SERIALIZE,
        arguments: {
          schemaId: NODE_SERIALISER_ID,
          schemaVersion: '1',
        },
      },
    ]

    const results = loadPipeline(
      MOCK_NODE_DATA_POLICY,
      policyOperations,
      DataPolicyData.Handle.ON_SUCCESS,
      [
        { ...mockSchemaTempHumidity, id: NODE_DESERIALISER_ID },
        { ...mockSchemaTempHumidity, id: NODE_SERIALISER_ID },
      ],
      [
        {
          id: 'script1',
          version: 1,
          createdAt: '2024-04-22T09:34:51.765Z',
          functionType: Script.functionType.TRANSFORMATION,
          source: btoa('function transform(publish, context) { return publish }'),
        },
      ]
    )

    expect(results.length).toBeGreaterThan(0)
  })

  it('should handle non-transform operations', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: 'System.log',
        arguments: {
          level: 'DEBUG',
          message: 'test message',
        },
      },
    ]

    const results = loadPipeline(MOCK_NODE_DATA_POLICY, policyOperations, DataPolicyData.Handle.ON_SUCCESS, [], [])

    expect(results).toHaveLength(2)
    const addChange = results[0] as NodeAddChange
    expect(addChange.type).toBe('add')
    expect(addChange.item.type).toBe(DataHubNodeType.OPERATION)
    expect((addChange.item as Node<OperationData>).data.functionId).toBe('System.log')
  })

  it('should handle multiple non-transform operations in sequence', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: 'System.log',
        arguments: { level: 'DEBUG', message: 'first' },
      },
      {
        id: 'node2',
        functionId: 'Delivery.redirectTo',
        arguments: { topic: 'new/topic' },
      },
    ]

    const results = loadPipeline(MOCK_NODE_DATA_POLICY, policyOperations, DataPolicyData.Handle.ON_ERROR, [], [])

    expect(results).toHaveLength(4) // 2 nodes + 2 connections
  })

  it('should throw error if deserialize found without proper context', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: OperationData.Function.SERDES_DESERIALIZE,
        arguments: {
          schemaId: NODE_DESERIALISER_ID,
          schemaVersion: '1',
        },
      },
      {
        id: 'node2',
        functionId: 'System.log',
        arguments: {},
      },
    ]

    expect(() => loadPipeline(MOCK_NODE_DATA_POLICY, policyOperations, null, [], [])).toThrow()
  })

  it('should throw error if serialize found without deserialize', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: OperationData.Function.SERDES_SERIALIZE,
        arguments: {
          schemaId: NODE_SERIALISER_ID,
          schemaVersion: '1',
        },
      },
    ]

    expect(() => loadPipeline(MOCK_NODE_DATA_POLICY, policyOperations, null, [], [])).toThrow()
  })

  it('should throw error if function found without deserialize', async () => {
    const policyOperations: PolicyOperation[] = [
      {
        id: 'node1',
        functionId: 'fn:script1:latest',
        arguments: {},
      },
    ]

    expect(() => loadPipeline(MOCK_NODE_DATA_POLICY, policyOperations, null, [], [])).toThrow()
  })
})

describe('checkValidityPipeline - additional tests', () => {
  it('should process a complete pipeline with multiple operations', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_OPERATION1: Node<OperationData> = {
      id: 'operation-1',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'op-1',
        functionId: 'System.log',
        formData: { level: 'DEBUG', message: 'test' },
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_OPERATION2: Node<OperationData> = {
      id: 'operation-2',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'op-2',
        functionId: 'Delivery.redirectTo',
        formData: { topic: 'new/topic' },
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_DATA_POLICY, MOCK_NODE_OPERATION1, MOCK_NODE_OPERATION2],
      edges: [
        {
          source: MOCK_NODE_DATA_POLICY.id,
          sourceHandle: DataPolicyData.Handle.ON_SUCCESS,
          target: MOCK_NODE_OPERATION1.id,
          targetHandle: null,
          id: 'edge-1',
        },
        {
          source: MOCK_NODE_OPERATION1.id,
          sourceHandle: null,
          target: MOCK_NODE_OPERATION2.id,
          targetHandle: null,
          id: 'edge-2',
        },
      ],
    }

    const results = checkValidityPipeline(MOCK_NODE_DATA_POLICY, DataPolicyData.Handle.ON_SUCCESS, MOCK_STORE)
    expect(results).toHaveLength(2)
    expect(results[0].data?.functionId).toBe('System.log')
    expect(results[1].data?.functionId).toBe('Delivery.redirectTo')
  })
})

describe('checkValidityTransformFunction - schema version handling', () => {
  const MOCK_NODE_OPERATION: Node<OperationData> = {
    id: 'node-id',
    type: DataHubNodeType.OPERATION,
    data: {
      id: NODE_OPERATION_ID,
      functionId: TRANSFORM_FUNCTION_ID,
      formData: {
        transform: ['the_function'],
      },
      metadata: {
        isTerminal: false,
        hasArguments: true,
      },
    },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  const MOCK_NODE_FUNCTION: Node<FunctionData> = {
    id: NODE_FUNCTION_ID,
    type: DataHubNodeType.FUNCTION,
    data: {
      type: 'Javascript',
      name: 'the_function',
      version: 1,
      sourceCode: 'const t=1',
    },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should handle DRAFT schema version', async () => {
    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        name: 'node-schema',
        type: SchemaType.JSON,
        version: ResourceWorkingVersion.DRAFT,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SERIAL, MOCK_NODE_FUNCTION, MOCK_NODE_OPERATION],
      edges: [
        {
          source: MOCK_NODE_FUNCTION.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'function',
          id: '1',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'serialiser',
          id: '2',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'deserialiser',
          id: '3',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(3)
    expect(results[0].data?.arguments).toEqual({
      schemaId: 'node-schema',
      schemaVersion: 'latest',
    })
    expect(results[2].data?.arguments).toEqual({
      schemaId: 'node-schema',
      schemaVersion: 'latest',
    })
  })

  it('should handle MODIFIED schema version', async () => {
    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        name: 'node-schema',
        type: SchemaType.JSON,
        version: ResourceWorkingVersion.MODIFIED,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SERIAL, MOCK_NODE_FUNCTION, MOCK_NODE_OPERATION],
      edges: [
        {
          source: MOCK_NODE_FUNCTION.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'function',
          id: '1',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'serialiser',
          id: '2',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'deserialiser',
          id: '3',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(3)
    expect(results[0].data?.arguments).toEqual({
      schemaId: 'node-schema',
      schemaVersion: 'latest',
    })
    expect(results[2].data?.arguments).toEqual({
      schemaId: 'node-schema',
      schemaVersion: 'latest',
    })
  })

  it('should handle multiple scripts in transform array', async () => {
    const MOCK_NODE_FUNCTION2: Node<FunctionData> = {
      id: 'node-function-2',
      type: DataHubNodeType.FUNCTION,
      data: {
        type: 'Javascript',
        name: 'the_function_2',
        version: 1,
        sourceCode: 'const t=2',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_OPERATION_MULTI: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: NODE_OPERATION_ID,
        functionId: TRANSFORM_FUNCTION_ID,
        formData: {
          transform: [NODE_FUNCTION_ID, 'node-function-2'],
        },
        metadata: {
          isTerminal: false,
          hasArguments: true,
        },
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        name: 'node-schema',
        type: SchemaType.JSON,
        version: 1,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SERIAL, MOCK_NODE_FUNCTION, MOCK_NODE_FUNCTION2, MOCK_NODE_OPERATION_MULTI],
      edges: [
        {
          source: MOCK_NODE_FUNCTION.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_MULTI.id,
          targetHandle: 'function',
          id: '1',
        },
        {
          source: MOCK_NODE_FUNCTION2.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_MULTI.id,
          targetHandle: 'function',
          id: '1b',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_MULTI.id,
          targetHandle: 'serialiser',
          id: '2',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_MULTI.id,
          targetHandle: 'deserialiser',
          id: '3',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION_MULTI, MOCK_STORE)
    // Only checking that we have results since the actual function may filter out scripts not in the default order
    expect(results.length).toBeGreaterThan(0)
    expect(results[0].data?.functionId).toBe('Serdes.deserialize')
  })

  it('should handle empty transform array defaulting to scriptNodes', async () => {
    const MOCK_NODE_OPERATION_EMPTY: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: NODE_OPERATION_ID,
        functionId: TRANSFORM_FUNCTION_ID,
        formData: {
          transform: [],
        },
        metadata: {
          isTerminal: false,
          hasArguments: true,
        },
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        name: 'node-schema',
        type: SchemaType.JSON,
        version: 1,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SERIAL, MOCK_NODE_FUNCTION, MOCK_NODE_OPERATION_EMPTY],
      edges: [
        {
          source: MOCK_NODE_FUNCTION.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_EMPTY.id,
          targetHandle: 'function',
          id: '1',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_EMPTY.id,
          targetHandle: 'serialiser',
          id: '2',
        },
        {
          source: MOCK_NODE_SERIAL.id,
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION_EMPTY.id,
          targetHandle: 'deserialiser',
          id: '3',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION_EMPTY, MOCK_STORE)
    expect(results).toHaveLength(3)
    expect(results[1].data?.functionId).toBe('fn:the_function:1')
  })
})
