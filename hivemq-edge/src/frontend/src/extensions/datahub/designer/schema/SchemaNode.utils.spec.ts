import { expect } from 'vitest'
import type { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import type { SchemaData } from '@datahub/types.ts'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import { checkValiditySchema, getSchemaFamilies, getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'

describe('getSchemaFamilies', () => {
  it('should deal with an empty list of schemas', () => {
    expect(getSchemaFamilies([])).toEqual({})
  })

  it('should isolate families of schemas', () => {
    const results = getSchemaFamilies([mockSchemaTempHumidity, { ...mockSchemaTempHumidity, id: 'the other id' }])

    expect(results).toEqual(
      expect.objectContaining({
        'my-schema-id': expect.objectContaining({}),
        'the other id': expect.objectContaining({}),
      })
    )
  })

  it('should identify list of versions', () => {
    const results = getSchemaFamilies([
      mockSchemaTempHumidity,
      { ...mockSchemaTempHumidity, id: 'the other id' },
      { ...mockSchemaTempHumidity, version: 2 },
    ])

    expect(results).toEqual(
      expect.objectContaining({
        'my-schema-id': expect.objectContaining({ name: 'my-schema-id', versions: [1, 2] }),
        'the other id': expect.objectContaining({ name: 'the other id', versions: [1] }),
      })
    )
  })
})

describe('getScriptFamilies', () => {
  it('should deal with an empty list of scripts', () => {
    expect(getSchemaFamilies([])).toEqual({})
  })

  it('should isolate families of schemas', () => {
    const results = getScriptFamilies([mockScript, { ...mockScript, id: 'the other id' }])

    expect(results).toEqual(
      expect.objectContaining({
        'my-script-id': expect.objectContaining({}),
        'the other id': expect.objectContaining({}),
      })
    )
  })

  it('should identify list of versions', () => {
    const results = getScriptFamilies([
      mockScript,
      { ...mockScript, id: 'the other id' },
      { ...mockScript, version: 2 },
    ])

    expect(results).toEqual(
      expect.objectContaining({
        'my-script-id': expect.objectContaining({ name: 'my-script-id', versions: [1, 2] }),
        'the other id': expect.objectContaining({ name: 'the other id', versions: [1] }),
      })
    )
  })
})

describe('checkValiditySchema', () => {
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

  it('should return the paylaod otherwise', async () => {
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
