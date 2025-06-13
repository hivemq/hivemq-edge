import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from '@xyflow/react'

import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { type BehaviorPolicyOnTransition, type DataPolicy, type PolicyOperation, Script } from '@/api/__generated__'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

import type { FunctionData, SchemaData, TransitionData, WorkspaceState } from '@datahub/types.ts'
import { BehaviorPolicyType } from '@datahub/types.ts'
import { DataHubNodeType, DataPolicyData, OperationData, SchemaType } from '@datahub/types.ts'
import {
  checkValidityPipeline,
  checkValidityTransformFunction,
  loadBehaviorPolicyPipelines,
  loadDataPolicyPipelines,
  loadPipeline,
  processOperations,
} from '@datahub/designer/operation/OperationNode.utils.ts'

const NODE_POLICY_ID = 'my-policy-id'
const NODE_OPERATION_ID = 'my-operation-id'
const NODE_FUNCTION_ID = 'node-function'
const TRANSFORM_FUNCTION_ID = 'DataHub.transform'
const NODE_DESERIALISER_ID = 'node-schema-deserializer'
const NODE_SERIALISER_ID = 'node-schema-serializer'

describe('checkValidityTransformFunction', () => {
  it('should return error if not configured', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: { id: NODE_OPERATION_ID, functionId: 'Javascript' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'The Operation is not properly defined. The following properties are missing: functionId, formData',
        id: 'node-id',
        title: 'OPERATION',
        type: 'datahub.notConfigured',
      })
    )
  })

  it('should return error if no function connected', async () => {
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
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_FUNCTION, MOCK_NODE_OPERATION],
      edges: [
        {
          source: MOCK_NODE_FUNCTION.id,
          sourceHandle: 'source',
          target: '2',
          targetHandle: 'function',
          id: '1',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No JS Function connected to Operation',
        id: 'node-id',
        title: 'OPERATION',
        type: 'datahub.notConnected',
      })
    )
  })

  const NODE_SCHEMA_ID = 'node-schema'

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
  const MOCK_NODE_SERIAL: Node<SchemaData> = {
    id: NODE_SCHEMA_ID,
    type: DataHubNodeType.SCHEMA,
    data: {
      name: NODE_SCHEMA_ID,
      type: SchemaType.JSON,
      version: 1,
      schemaSource: '{ t: 1}',
    },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return error if no serialiser connected', async () => {
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
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Schema connected to the "serialiser" handle of Operation',
        id: 'node-id',
        title: 'OPERATION',
        type: 'datahub.notConnected',
      })
    )
  })

  it('should return error if multiple serialiser connected', async () => {
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
    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: NODE_SCHEMA_ID,
      type: DataHubNodeType.SCHEMA,
      data: {
        name: NODE_SCHEMA_ID,
        type: SchemaType.JSON,
        version: 1,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [
        MOCK_NODE_SERIAL,
        { ...MOCK_NODE_SERIAL, id: 'duplicate-serial' },
        MOCK_NODE_FUNCTION,
        MOCK_NODE_OPERATION,
      ],
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
        {
          source: 'duplicate-serial',
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'serialiser',
          id: '4',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'Too many Schema connected to Operation',
        id: 'node-id',
        status: 404,
        title: 'OPERATION',
        type: 'datahub.cardinality',
      })
    )
  })

  it('should return error if no deserialiser connected', async () => {
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
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Schema connected to the "deserialiser" handle of Operation',
        id: 'node-id',
        title: 'OPERATION',
        type: 'datahub.notConnected',
      })
    )
  })

  it('should return error if multiple deserialiser connected', async () => {
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
    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: NODE_SCHEMA_ID,
      type: DataHubNodeType.SCHEMA,
      data: {
        name: NODE_SCHEMA_ID,
        type: SchemaType.JSON,
        version: 1,
        schemaSource: '{ t: 1}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [
        MOCK_NODE_SERIAL,
        { ...MOCK_NODE_SERIAL, id: 'duplicate-serial' },
        MOCK_NODE_FUNCTION,
        MOCK_NODE_OPERATION,
      ],
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
        {
          source: 'duplicate-serial',
          sourceHandle: 'source',
          target: MOCK_NODE_OPERATION.id,
          targetHandle: 'deserialiser',
          id: '3',
        },
      ],
    }

    const results = checkValidityTransformFunction(MOCK_NODE_OPERATION, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'Too many Schema connected to Operation',
        id: 'node-id',
        status: 404,
        title: 'OPERATION',
        type: 'datahub.cardinality',
      })
    )
  })

  it('should return the payload otherwise', async () => {
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
    const MOCK_NODE_SERIAL: Node<SchemaData> = {
      id: NODE_SCHEMA_ID,
      type: DataHubNodeType.SCHEMA,
      data: {
        name: NODE_SCHEMA_ID,
        type: SchemaType.JSON,
        version: 1,
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

    {
      const { node, data, error, resources } = results[0]
      expect(node).toStrictEqual(MOCK_NODE_OPERATION)
      expect(error).toBeUndefined()
      expect(resources).toBeUndefined()
      expect(data).toEqual(
        expect.objectContaining({
          arguments: {
            schemaId: NODE_SCHEMA_ID,
            schemaVersion: '1',
          },
          functionId: 'Serdes.deserialize',
          id: 'node-id-deserializer',
        })
      )
    }
    {
      const { node, data, error, resources } = results[2]
      expect(node).toStrictEqual(MOCK_NODE_OPERATION)
      expect(error).toBeUndefined()
      expect(resources).toHaveLength(2)
      expect(data).toEqual(
        expect.objectContaining({
          arguments: {
            schemaId: NODE_SCHEMA_ID,
            schemaVersion: '1',
          },
          functionId: OperationData.Function.SERDES_SERIALIZE,
          id: 'node-id-serializer',
        })
      )
    }
    {
      const { node, data, error, resources } = results[1]
      expect(node).toStrictEqual(MOCK_NODE_OPERATION)
      expect(error).toBeUndefined()
      expect(resources).toBeUndefined()
      expect(data).toEqual(
        expect.objectContaining({
          arguments: {},
          functionId: 'fn:the_function:1',
          id: NODE_FUNCTION_ID,
        })
      )
    }
  })
})

describe('checkValidityPipeline', () => {
  it('should return empty pipeline if no operations connected', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
    }

    const results = checkValidityPipeline(MOCK_NODE_DATA_POLICY, DataPolicyData.Handle.ON_SUCCESS, MOCK_STORE)
    expect(results).toHaveLength(0)
  })

  it('should return checked elements otherwise', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
    }

    const results = checkValidityPipeline(MOCK_NODE_DATA_POLICY, DataPolicyData.Handle.ON_SUCCESS, MOCK_STORE)
    expect(results).toHaveLength(0)
  })
})

describe('processOperations', () => {
  const MOCK_STORE: WorkspaceState = {
    nodes: [],
    edges: [],
  }

  it('should return error if not configured', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: NODE_OPERATION_ID,
        functionId: undefined,
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const results = processOperations(MOCK_STORE)([], MOCK_NODE_OPERATION)
    expect(results).toHaveLength(1)
    const { node, error, data, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)

    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual({
      detail: 'The Operation is not properly defined. The following properties are missing: functionId',
      id: 'node-id',
      status: 404,
      title: 'OPERATION',
      type: 'datahub.notConfigured',
    })
  })

  it('should process transformation function', async () => {
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

    const results = processOperations(MOCK_STORE)([], MOCK_NODE_OPERATION)
    expect(results).toHaveLength(1)
    const { node, error, data, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)

    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toStrictEqual({
      detail: 'No JS Function connected to Operation',
      id: 'node-id',
      status: 404,
      title: 'OPERATION',
      type: 'datahub.notConnected',
    })
  })

  it('should return the operation payload otherwise', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: NODE_OPERATION_ID,
        functionId: 'System.log',
        formData: {
          level: 'DEBUG',
          message: 'test the message',
        },
        metadata: {
          isTerminal: false,
        },
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const results = processOperations(MOCK_STORE)([], MOCK_NODE_OPERATION)
    expect(results).toHaveLength(1)
    const { node, error, data, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_OPERATION)

    expect(resources).toBeUndefined()
    expect(data).toEqual({
      arguments: {
        level: 'DEBUG',
        message: 'test the message',
      },
      functionId: 'System.log',
      id: NODE_OPERATION_ID,
    })
    expect(error).toBeUndefined()
  })
})

describe('loadPipeline', () => {
  it('should return no elements if no pipeline', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const results = loadPipeline(MOCK_NODE_DATA_POLICY, [], null, [], [])
    expect(results).toHaveLength(0)
  })

  it('should return fully defined HiveMq.transform elements', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

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
        functionId: 'fn:script1:latest',
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
      null,
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
          source:
            'Ci8qKgogKgogKiBAcGFyYW0ge09iamVjdH0gcHVibGlzaAogKiBAcGFyYW0ge3N0cmluZ30gcHVibGlzaC50b3BpYyAgICBUaGUgTVFUVCB0b3BpYyB0aGF0IGlzIGN1cnJlbnRseSBzcGVjaWZpZWQgZm9yIHRoaXMgUFVCTElTSCBwYWNrZXQuCiAqIEBwYXJhbSB7T2JqZWN0fSBwdWJsaXNoLnBheWxvYWQgIEEgbGlzdCBvZiB0aGUgbmFtZSBhbmQgdmFsdWUgb2YgYWxsIHVzZXIgcHJvcGVydGllcyBvZiB0aGUgTVFUVCA1IFBVQkxJU0ggcGFja2V0LiBUaGlzIHNldHRpbmcgaGFzIG5vIGVmZmVjdCBvbiBNUVRUIDMgY2xpZW50cy4KICogQHBhcmFtIHtSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+W119IHB1Ymxpc2gudXNlclByb3BlcnRpZXMgVGhlIEpTT04gb2JqZWN0IHJlcHJlc2VudGF0aW9uIG9mIHRoZSBkZXNlcmlhbGl6ZWQgTVFUVCBwYXlsb2FkLgogKiBAcGFyYW0ge09iamVjdH0gY29udGV4dAogKiBAcGFyYW0ge1JlY29yZDxzdHJpbmcsIHN0cmluZz5bXX0gY29udGV4dC5hcmd1bWVudHMgIFRoZSBhcmd1bWVudHMgcHJvdmlkZWQgdG8gdGhlIHNjcmlwdC4gQ3VycmVudGx5LCBhcmd1bWVudHMgY2FuIG9ubHkgYmUgcHJvdmlkZWQgdmlhIGEgZGF0YSBwb2xpY3kuCiAqIEBwYXJhbSB7c3RyaW5nfSBjb250ZXh0LnBvbGljeUlkIFRoZSBwb2xpY3kgaWQgb2YgdGhlIHBvbGljeSBmcm9tIHdoaWNoIHRoZSB0cmFuc2Zvcm1hdGlvbiBmdW5jdGlvbiBpcyBjYWxsZWQuCiAqIEBwYXJhbSB7c3RyaW5nfSBjb250ZXh0LmNsaWVudElkIFRoZSBjbGllbnQgSWQgb2YgdGhlIGNsaWVudCBmcm9tIHdoaWNoIHRoZSBNUVRUIHB1Ymxpc2ggd2FzIHNlbnQuCiAqIEByZXR1cm5zIHtPYmplY3R9IFRoZSBwdWJsaXNoLW9iamVjdCBpcyBwYXNzZWQgYXMgYSBwYXJhbWV0ZXIgaW50byB0aGUgdHJhbnNmb3JtIGZ1bmN0aW9uLiBUaGUgc2FtZSBvYmplY3Qgb3IgYSBuZXcgb2JqZWN0IGlzIHJldHVybmVkIGFzIHRoZSB0cmFuc2Zvcm1lZCBvYmplY3QuCiAqLwpmdW5jdGlvbiB0cmFuc2Zvcm0ocHVibGlzaCwgY29udGV4dCkgewogIHJldHVybiBwdWJsaXNoCn0KCg==',
        },
      ]
    )

    // The transform node is created with a UI ID that starts with 'node_'
    const transformNode = (results[1] as Connection).target
    expect(transformNode).toMatch('OPERATION_')

    expect(results).toStrictEqual(
      expect.arrayContaining<NodeAddChange | Connection>([
        {
          item: expect.objectContaining<Partial<Node<SchemaData>>>({
            id: expect.stringContaining('SCHEMA_'),
            type: DataHubNodeType.SCHEMA,
          }),
          type: 'add',
        },
        expect.objectContaining<Connection>({
          source: expect.stringContaining('SCHEMA_'),
          sourceHandle: null,
          target: expect.stringContaining(transformNode),
          targetHandle: 'deserialiser',
        }),
        {
          item: expect.objectContaining<Partial<Node<SchemaData>>>({
            id: expect.stringContaining('SCHEMA_'),
            type: DataHubNodeType.SCHEMA,
          }),
          type: 'add',
        },
        expect.objectContaining<Connection>({
          source: expect.stringContaining('SCHEMA_'),
          sourceHandle: null,
          target: expect.stringContaining(transformNode),
          targetHandle: 'serialiser',
        }),
        {
          item: expect.objectContaining<Partial<Node<FunctionData>>>({
            id: 'script1',
            type: DataHubNodeType.FUNCTION,
          }),
          type: 'add',
        },
        expect.objectContaining<Connection>({
          source: 'script1',
          sourceHandle: null,
          target: expect.stringContaining(transformNode),
          targetHandle: 'function',
        }),
        {
          item: expect.objectContaining<Partial<Node<OperationData>>>({
            id: expect.stringContaining(transformNode),
            type: 'OPERATION',
            data: expect.objectContaining({
              functionId: TRANSFORM_FUNCTION_ID,
            }),
          }),
          type: 'add',
        },
        expect.objectContaining<Connection>({
          source: 'node-id',
          sourceHandle: null,
          target: expect.stringContaining(transformNode),
          targetHandle: null,
        }),
      ])
    )
  })
})

describe('loadDataPolicyPipelines', () => {
  it('should return no elements if no pipeline', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: NODE_POLICY_ID },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const dataPolicy: DataPolicy = {
      id: 'string',
      matching: { topicFilter: '*.*' },
    }

    const results = loadDataPolicyPipelines(dataPolicy, [], [], MOCK_NODE_DATA_POLICY)
    expect(results).toHaveLength(0)
  })
})

describe('loadBehaviorPolicyPipelines', () => {
  const MOCK_TRANSITION_NODE: Node<TransitionData> = {
    id: 'node-id',
    type: DataHubNodeType.TRANSITION,
    data: { model: BehaviorPolicyType.MQTT_EVENT },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should throw an error if no transition', async () => {
    const pol: BehaviorPolicyOnTransition = {
      fromState: 'state1',
      toState: 'state2',
    }

    expect(() => loadBehaviorPolicyPipelines(pol, MOCK_TRANSITION_NODE, [], [])).toThrow(
      'there is no transition pipeline for the operation'
    )
  })

  it('should return an empty list of nodes if no pipeline', async () => {
    const pol: BehaviorPolicyOnTransition = {
      fromState: 'state1',
      toState: 'state2',
      'Mqtt.OnInboundConnect': { pipeline: [] },
    }

    expect(loadBehaviorPolicyPipelines(pol, MOCK_TRANSITION_NODE, [], [])).toStrictEqual([])
  })
})
