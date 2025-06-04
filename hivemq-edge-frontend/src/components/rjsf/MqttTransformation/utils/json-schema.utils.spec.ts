import { expect } from 'vitest'
import type { JSONSchema7 } from 'json-schema'
import type { RJSFSchema } from '@rjsf/utils'
import {
  MOCK_MQTT_SCHEMA_METADATA,
  MOCK_MQTT_SCHEMA_PLAIN,
  MOCK_MQTT_SCHEMA_REFS,
} from '@/__test-utils__/rjsf/schema.mocks.ts'

import {
  type FlatJSONSchema7,
  getPropertyListFrom,
  payloadToSchema,
  reducerSchemaExamples,
} from './json-schema.utils.ts'

describe('getPropertyListFrom', () => {
  it('should return an empty list of properties', async () => {
    expect(getPropertyListFrom({})).toStrictEqual<FlatJSONSchema7[]>([])
  })

  it('should return error', async () => {
    expect(
      getPropertyListFrom({
        definitions: {
          address: {
            type: 'string',
          },
        },
        type: 'object',
        properties: {
          fake_prop: {
            title: 'Fake Property',
            $ref: '/schemas/address',
          },
        },
      })
    ).toStrictEqual<FlatJSONSchema7[]>([
      {
        description: "error: definition address doesn't exist",
        key: 'fake_prop',
        path: [],
        type: 'null',
      },
    ])
  })

  it('should handle definitions', async () => {
    expect(getPropertyListFrom(MOCK_MQTT_SCHEMA_REFS)).toStrictEqual<FlatJSONSchema7[]>(
      expect.arrayContaining([
        expect.objectContaining({
          description: undefined,
          key: 'billing_address',
          path: [],
          title: 'Billing address',
          type: 'object',
        }),
        expect.objectContaining({
          description: undefined,
          key: 'street_address',
          path: ['billing_address'],
          title: 'street_address',
          type: 'string',
        }),
        expect.objectContaining({
          description: undefined,
          key: 'children',
          path: ['tree'],
          title: 'children',
          type: 'array',
        }),
      ])
    )
  })

  it('should handle self-contained schema', async () => {
    expect(getPropertyListFrom(MOCK_MQTT_SCHEMA_PLAIN)).toStrictEqual<FlatJSONSchema7[]>(
      expect.arrayContaining([
        expect.objectContaining({
          description: undefined,
          examples: undefined,
          key: 'firstName',
          path: [],
          title: 'First name',
          type: 'string',
        }),
        expect.objectContaining({
          description: undefined,
          examples: undefined,
          key: 'telephone',
          path: [],
          title: 'Telephone',
          type: 'string',
        }),
      ])
    )
  })

  interface SchemaSuite {
    type: string
  }

  const nodeUpdateTests: SchemaSuite[] = [
    { type: 'string' },
    { type: 'number' },
    { type: 'integer' },
    { type: 'boolean' },
    { type: 'array' },
    { type: 'object' },
  ]

  it.each<SchemaSuite>(nodeUpdateTests)('should work for $type', ({ type }) => {
    const hhhh = getPropertyListFrom(MOCK_MQTT_SCHEMA_METADATA).find((e) => e.key === type)
    expect(hhhh).not.toBeUndefined()
    const { properties } = MOCK_MQTT_SCHEMA_METADATA
    const flat = { ...(properties?.[type] as Omit<JSONSchema7, 'required'>), key: type, path: [] } as FlatJSONSchema7

    expect(hhhh).toStrictEqual<FlatJSONSchema7>(expect.objectContaining(flat))
  })
})

describe('reducerSchemaExamples', () => {
  it('should return the plain schema', async () => {
    expect(reducerSchemaExamples(MOCK_MQTT_SCHEMA_PLAIN, {})).toStrictEqual<RJSFSchema>(MOCK_MQTT_SCHEMA_PLAIN)
  })

  it('should return the plain schema', async () => {
    expect(reducerSchemaExamples(MOCK_MQTT_SCHEMA_PLAIN, { fakeProp: 1 })).toStrictEqual<RJSFSchema>(
      MOCK_MQTT_SCHEMA_PLAIN
    )
  })

  it('should add examples for primitive types', async () => {
    expect(reducerSchemaExamples(MOCK_MQTT_SCHEMA_PLAIN, { age: 1 })).toStrictEqual<RJSFSchema>(
      expect.objectContaining({
        properties: expect.objectContaining({
          age: {
            examples: [1],
            title: 'Age',
            type: 'integer',
          },
        }),
      })
    )
  })

  it('should add examples for nested objects', async () => {
    expect(
      reducerSchemaExamples(MOCK_MQTT_SCHEMA_PLAIN, { nestedObject: { name: 'test2', age: 100 } })
    ).toStrictEqual<RJSFSchema>(
      expect.objectContaining({
        properties: expect.objectContaining({
          nestedObject: expect.objectContaining({
            properties: expect.objectContaining({
              age: {
                examples: [100],
                title: 'Age',
                type: 'integer',
              },
              name: {
                default: 'Default name',
                examples: ['test2'],
                type: 'string',
              },
            }),
          }),
        }),
      })
    )
  })
})

describe('payloadToSchema', () => {
  it('should return an empty list', async () => {
    expect(payloadToSchema(undefined)).toStrictEqual({})
  })

  it('should return an empty list', async () => {
    expect(payloadToSchema([])).toStrictEqual({})
  })

  it('should return an empty list', async () => {
    expect(payloadToSchema([{ topic: 'test/topic1', payload: { age: 1 } }])).toStrictEqual({
      'test/topic1': {
        properties: {
          age: {
            examples: [1],
            type: 'integer',
          },
        },
        required: ['age'],
        type: 'object',
      },
    })
  })
})

describe('getSchemaFromPropertyList', () => {
  it.skip('should return an empty list of properties', async () => {})
})
