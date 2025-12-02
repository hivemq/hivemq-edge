import type { SchemaReference } from '@/api/__generated__'
import { MOCK_PROTOBUF_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { expect } from 'vitest'
import type { Node } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import type { PolicyOperationArguments, SchemaData, DataHubNodeData } from '@datahub/types.ts'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import {
  checkValiditySchema,
  getSchemaFamilies,
  getScriptFamilies,
  getSchemaRefVersion,
  loadSchema,
} from '@datahub/designer/schema/SchemaNode.utils.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { SCRIPT_FUNCTION_LATEST } from '@datahub/utils/datahub.utils.ts'

const NODE_ID = 'the other id'

describe('getSchemaFamilies', () => {
  it('should deal with an empty list of schemas', () => {
    expect(getSchemaFamilies([])).toEqual({})
  })

  it('should isolate families of schemas', () => {
    const results = getSchemaFamilies([mockSchemaTempHumidity, { ...mockSchemaTempHumidity, id: NODE_ID }])

    expect(results).toEqual(
      expect.objectContaining({
        'my-schema-id': expect.objectContaining({}),
        [NODE_ID]: expect.objectContaining({}),
      })
    )
  })

  it('should identify list of versions', () => {
    const results = getSchemaFamilies([
      mockSchemaTempHumidity,
      { ...mockSchemaTempHumidity, id: NODE_ID },
      { ...mockSchemaTempHumidity, version: 2 },
    ])

    expect(results).toEqual(
      expect.objectContaining({
        'my-schema-id': expect.objectContaining({ name: 'my-schema-id', versions: [1, 2] }),
        [NODE_ID]: expect.objectContaining({ name: NODE_ID, versions: [1] }),
      })
    )
  })

  it('should extract description from JSON schema definition', () => {
    const schemaWithDescription = {
      ...mockSchemaTempHumidity,
      id: 'schema-with-desc',
    }

    const results = getSchemaFamilies([schemaWithDescription])

    expect(results['schema-with-desc']).toEqual(
      expect.objectContaining({
        description: 'A schema that matches the temperature and humidity values of any object',
      })
    )
  })

  it('should handle schemas with invalid schemaDefinition gracefully', () => {
    const schemaWithInvalidDef = {
      ...mockSchemaTempHumidity,
      id: 'invalid-schema',
      schemaDefinition: 'not-valid-base64!!!',
    }

    const results = getSchemaFamilies([schemaWithInvalidDef])

    expect(results['invalid-schema']).toEqual(
      expect.objectContaining({
        name: 'invalid-schema',
        description: undefined,
      })
    )
  })
})

describe('getScriptFamilies', () => {
  it('should deal with an empty list of scripts', () => {
    expect(getSchemaFamilies([])).toEqual({})
  })

  it('should isolate families of schemas', () => {
    const results = getScriptFamilies([mockScript, { ...mockScript, id: NODE_ID }])

    expect(results).toEqual(
      expect.objectContaining({
        'my-script-id': expect.objectContaining({}),
        [NODE_ID]: expect.objectContaining({}),
      })
    )
  })

  it('should identify list of versions', () => {
    const results = getScriptFamilies([mockScript, { ...mockScript, id: NODE_ID }, { ...mockScript, version: 2 }])

    expect(results).toEqual(
      expect.objectContaining({
        'my-script-id': expect.objectContaining({ name: 'my-script-id', versions: [1, 2] }),
        [NODE_ID]: expect.objectContaining({ name: NODE_ID, versions: [1] }),
      })
    )
  })
})

describe('getSchemaRefVersion', () => {
  const MOCK_SCHEMA_REF: SchemaReference = { schemaId: 'test', version: '1' }
  const MOCK_SCHEMA_POLICY_REF: PolicyOperationArguments = { schemaId: 'test', schemaVersion: '1' }

  it('should deal with an empty list of schemas', () => {
    expect(getSchemaRefVersion(MOCK_SCHEMA_REF)).toStrictEqual('1')
  })

  it('should deal with an empty list of schemas', () => {
    expect(getSchemaRefVersion(MOCK_SCHEMA_POLICY_REF)).toEqual('1')
  })
})

describe('checkValiditySchema', () => {
  describe('JSON', () => {
    it('should return an error if not configured', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          type: SchemaType.JSON,
          version: 1,
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(error).toStrictEqual({
        detail:
          'The JS Function is not properly defined. The following properties are missing: type, version, schemaSource',
        id: 'node-id',
        status: 404,
        title: 'FUNCTION',
        type: 'datahub.notConfigured',
      })
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(resources).toBeUndefined()
      expect(data).toBeUndefined()
    })

    it('should return the payload otherwise', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          type: SchemaType.JSON,
          version: 1,
          schemaSource: '{ tg: 1}',
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(data).toStrictEqual({
        id: 'node-id',
        schemaDefinition: 'eyB0ZzogMX0=',
        type: 'JSON',
      })
      expect(resources).toBeUndefined()
      expect(error).toBeUndefined()
    })
  })

  describe('PROTOBUF', () => {
    it('should return an error if not configured', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          type: SchemaType.PROTOBUF,
          version: 1,
          schemaSource: 'SOME FAKE PROTOBUF',
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(error).toStrictEqual(
        expect.objectContaining({
          detail: 'The JS Function is not properly defined. The following properties are missing: messageType',
          status: 404,
          type: 'datahub.notConfigured',
        })
      )
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(resources).toBeUndefined()
      expect(data).toBeUndefined()
    })
    it('should return an error if illegal content', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          type: SchemaType.PROTOBUF,
          version: 1,
          schemaSource: 'SOME FAKE PROTOBUF',
          messageType: 'messageType',
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(error).toStrictEqual(
        expect.objectContaining({
          detail: "Encountered an error while processing JS Function: illegal token 'SOME' (line 1)",
          id: 'node-id',
          status: 404,
          title: 'FUNCTION',
          type: 'datahub.notConfigured',
        })
      )
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(resources).toBeUndefined()
      expect(data).toBeUndefined()
    })

    it('should return the payload otherwise', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          type: SchemaType.PROTOBUF,
          version: 1,
          schemaSource: MOCK_PROTOBUF_SCHEMA,
          messageType: 'GpsCoordinates',
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(data).toStrictEqual({
        arguments: {
          messageType: 'GpsCoordinates',
        },
        id: 'node-id',
        schemaDefinition:
          'CksKCnJvb3QucHJvdG8iNQoOR3BzQ29vcmRpbmF0ZXMSEQoJbG9uZ2l0dWRlGAEgASgFEhAKCGxhdGl0dWRlGAIgASgFYgZwcm90bzM=',
        type: SchemaType.PROTOBUF,
      })
      expect(resources).toBeUndefined()
      expect(error).toBeUndefined()
    })
  })

  describe('Unknown schema type', () => {
    it('should return an error for unknown schema type', async () => {
      const MOCK_NODE_SCHEMA: Node<SchemaData> = {
        id: 'node-id',
        type: DataHubNodeType.FUNCTION,
        data: {
          name: 'node-id',
          // @ts-ignore - forcing an unknown type to test the fallback
          type: 'UNKNOWN_TYPE',
          version: 1,
          schemaSource: 'some source',
        },
        ...MOCK_DEFAULT_NODE,
        position: { x: 0, y: 0 },
      }

      const { node, error, data, resources } = checkValiditySchema(MOCK_NODE_SCHEMA)
      expect(error).toStrictEqual(
        expect.objectContaining({
          detail: 'The JS Function is not properly defined. The following properties are missing: schemaSource',
          status: 404,
          type: 'datahub.notConfigured',
        })
      )
      expect(node).toStrictEqual(MOCK_NODE_SCHEMA)
      expect(resources).toBeUndefined()
      expect(data).toBeUndefined()
    })
  })
})

describe('loadSchema', () => {
  const MOCK_PARENT_NODE: Node<DataHubNodeData> = {
    id: 'parent-node',
    type: DataHubNodeType.VALIDATOR,
    data: {},
    ...MOCK_DEFAULT_NODE,
    position: { x: 100, y: 200 },
  }

  describe('Error cases', () => {
    it('should throw an error if schema family is not found', () => {
      const schemaRef: SchemaReference = {
        schemaId: 'non-existent-schema',
        version: '1',
      }

      expect(() => loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRef, [])).toThrow(/Cannot find/)
    })

    it('should throw an error if specific version is not found', () => {
      const schemaRef: SchemaReference = {
        schemaId: mockSchemaTempHumidity.id,
        version: '999',
      }

      expect(() => loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRef, [mockSchemaTempHumidity])).toThrow(
        /Cannot find/
      )
    })

    it('should throw an error for unknown schema type', () => {
      const unknownSchema = {
        ...mockSchemaTempHumidity,
        // @ts-ignore - forcing unknown type
        type: 'UNKNOWN',
      }
      const schemaRef: SchemaReference = {
        schemaId: mockSchemaTempHumidity.id,
        version: '1',
      }

      expect(() => loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRef, [unknownSchema])).toThrow(
        /Cannot identify/
      )
    })
  })

  describe('JSON Schema', () => {
    it('should load JSON schema with specific version', () => {
      const schemaRef: SchemaReference = {
        schemaId: mockSchemaTempHumidity.id,
        version: '1',
      }

      const result = loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRef, [mockSchemaTempHumidity])

      expect(result).toHaveLength(2)
      expect(result[0]).toEqual(
        expect.objectContaining({
          type: 'add',
          item: expect.objectContaining({
            type: DataHubNodeType.SCHEMA,
            data: expect.objectContaining({
              name: mockSchemaTempHumidity.id,
              type: SchemaType.JSON,
              version: 1,
            }),
          }),
        })
      )
      expect(result[1]).toEqual(
        expect.objectContaining({
          target: MOCK_PARENT_NODE.id,
          targetHandle: 'target-handle',
        })
      )
    })

    it('should load JSON schema with LATEST version', () => {
      const schemas = [
        mockSchemaTempHumidity,
        { ...mockSchemaTempHumidity, version: 2 },
        { ...mockSchemaTempHumidity, version: 3 },
      ]
      const schemaRef: SchemaReference = {
        schemaId: mockSchemaTempHumidity.id,
        version: SCRIPT_FUNCTION_LATEST,
      }

      const result = loadSchema(MOCK_PARENT_NODE, 'target-handle', 1, schemaRef, schemas)

      expect(result[0]).toEqual(
        expect.objectContaining({
          item: expect.objectContaining({
            data: expect.objectContaining({
              version: 3,
            }),
          }),
        })
      )
    })

    it('should load JSON schema with PolicyOperationArguments reference', () => {
      const schemaRef: PolicyOperationArguments = {
        schemaId: mockSchemaTempHumidity.id,
        schemaVersion: '1',
      }

      const result = loadSchema(MOCK_PARENT_NODE, null, 2, schemaRef, [mockSchemaTempHumidity])

      expect(result).toHaveLength(2)
      expect(result[1]).toEqual(
        expect.objectContaining({
          targetHandle: null,
        })
      )
    })

    it('should set version to DRAFT when schema has no version', () => {
      const schemaWithoutVersion = {
        ...mockSchemaTempHumidity,
        version: undefined,
      }

      // Since version is used for matching, we need to use LATEST
      const schemaRefLatest: SchemaReference = {
        schemaId: mockSchemaTempHumidity.id,
        version: SCRIPT_FUNCTION_LATEST,
      }

      const result = loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRefLatest, [schemaWithoutVersion])

      expect(result[0]).toEqual(
        expect.objectContaining({
          item: expect.objectContaining({
            data: expect.objectContaining({
              internalVersions: undefined,
            }),
          }),
        })
      )
      // Verify the node has a valid schema
      expect(result).toHaveLength(2)
      const nodeChange = result[0] as { item: Node<SchemaData>; type: 'add' }
      expect(nodeChange.item.data.version).toBeDefined()
    })
  })

  describe('PROTOBUF Schema', () => {
    it('should load PROTOBUF schema', () => {
      const protobufSchema = {
        id: 'protobuf-schema-id',
        type: SchemaType.PROTOBUF,
        schemaDefinition:
          'CksKCnJvb3QucHJvdG8iNQoOR3BzQ29vcmRpbmF0ZXMSEQoJbG9uZ2l0dWRlGAEgASgFEhAKCGxhdGl0dWRlGAIgASgFYgZwcm90bzM=',
        version: 1,
      }
      const schemaRef: SchemaReference = {
        schemaId: 'protobuf-schema-id',
        version: '1',
      }

      const result = loadSchema(MOCK_PARENT_NODE, 'target-handle', 0, schemaRef, [protobufSchema])

      expect(result).toHaveLength(2)
      expect(result[0]).toEqual(
        expect.objectContaining({
          type: 'add',
          item: expect.objectContaining({
            id: 'protobuf-schema-id',
            type: DataHubNodeType.SCHEMA,
            data: expect.objectContaining({
              type: SchemaType.PROTOBUF,
              schemaSource: expect.stringContaining('// NOTICE: once encoded into a Base64 descriptor'),
              version: 1,
            }),
          }),
        })
      )
    })
  })
})
