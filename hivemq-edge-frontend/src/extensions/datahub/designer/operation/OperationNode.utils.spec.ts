import { expect } from 'vitest'
import type { Node } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { FunctionData, SchemaData, WorkspaceState } from '@datahub/types.ts'
import { DataHubNodeType, DataPolicyData, OperationData, SchemaType } from '@datahub/types.ts'
import {
  checkValidityPipeline,
  checkValidityTransformFunction,
  processOperations,
} from '@datahub/designer/operation/OperationNode.utils.ts'

describe('checkValidityTransformFunction', () => {
  it('should return error if not configured', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: { id: 'my-operation-id', functionId: 'Javascript' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
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
        id: 'my-operation-id',
        functionId: 'DataHub.transform',
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
      id: 'node-function',
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
      functions: [],
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

  it('should return error if no serialiser connected', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'my-operation-id',
        functionId: 'DataHub.transform',
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
      id: 'node-function',
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
      functions: [],
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

  it('should return error if no deserialiser connected', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'my-operation-id',
        functionId: 'DataHub.transform',
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
      id: 'node-function',
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
      functions: [],
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

  it('should return the payload otherwise', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'my-operation-id',
        functionId: 'DataHub.transform',
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
      id: 'node-function',
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
      functions: [],
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
            schemaId: 'node-schema',
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
            schemaId: 'node-schema',
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
          functionId: 'fn:the_function:latest',
          id: 'node-function',
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
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const results = checkValidityPipeline(MOCK_NODE_DATA_POLICY, DataPolicyData.Handle.ON_SUCCESS, MOCK_STORE)
    expect(results).toHaveLength(0)
  })

  it('should return checked elements otherwise', async () => {
    const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.DATA_POLICY,
      data: { id: 'my-policy-id' },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const results = checkValidityPipeline(MOCK_NODE_DATA_POLICY, DataPolicyData.Handle.ON_SUCCESS, MOCK_STORE)
    expect(results).toHaveLength(0)
  })
})

describe('processOperations', () => {
  const MOCK_STORE: WorkspaceState = {
    nodes: [],
    edges: [],
    functions: [],
  }

  it('should return error if not configured', async () => {
    const MOCK_NODE_OPERATION: Node<OperationData> = {
      id: 'node-id',
      type: DataHubNodeType.OPERATION,
      data: {
        id: 'my-operation-id',
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
        id: 'my-operation-id',
        functionId: 'DataHub.transform',
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
        id: 'my-operation-id',
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
      id: 'my-operation-id',
    })
    expect(error).toBeUndefined()
  })
})
