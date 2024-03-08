import { expect } from 'vitest'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, SchemaData, SchemaType } from '@datahub/types.ts'
import { checkValiditySchema } from '@datahub/designer/schema/SchemaNode.utils.ts'

describe('checkValiditySchema', () => {
  it('should return an error if not configured', async () => {
    const MOCK_NODE_SCHEMA: Node<SchemaData> = {
      id: 'node-id',
      type: DataHubNodeType.FUNCTION,
      data: {
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
      arguments: {},
      id: 'node-id',
      schemaDefinition: 'eyB0ZzogMX0=',
      type: 'JSON',
      version: 1,
    })
    expect(resources).toBeUndefined()
    expect(error).toBeUndefined()
  })
})
