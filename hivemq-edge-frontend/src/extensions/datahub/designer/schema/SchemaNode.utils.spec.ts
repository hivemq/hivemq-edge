import { MOCK_PROTOBUF_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { expect } from 'vitest'
import type { Node } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import type { SchemaData } from '@datahub/types.ts'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import { checkValiditySchema, getSchemaFamilies, getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'

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
})
