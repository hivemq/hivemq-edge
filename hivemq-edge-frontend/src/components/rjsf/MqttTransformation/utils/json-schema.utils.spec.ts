import { expect } from 'vitest'
import type { JSONSchema7 } from 'json-schema'
import type { RJSFSchema } from '@rjsf/utils'
import {
  MOCK_MQTT_SCHEMA_METADATA,
  MOCK_MQTT_SCHEMA_PLAIN,
  MOCK_MQTT_SCHEMA_READONLY,
  MOCK_MQTT_SCHEMA_REFS,
} from '@/__test-utils__/rjsf/schema.mocks.ts'

import {
  type FlatJSONSchema7,
  getPropertyListFrom,
  getSchemaFromPropertyList,
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

  it('should extract readOnly property for root-level properties', () => {
    const properties = getPropertyListFrom(MOCK_MQTT_SCHEMA_READONLY)

    const idProperty = properties.find((p) => p.key === 'id')
    expect(idProperty).toBeDefined()
    expect(idProperty?.readOnly).toBe(true)

    const timestampProperty = properties.find((p) => p.key === 'timestamp')
    expect(timestampProperty).toBeDefined()
    expect(timestampProperty?.readOnly).toBe(true)

    const nameProperty = properties.find((p) => p.key === 'name')
    expect(nameProperty).toBeDefined()
    expect(nameProperty?.readOnly).toBeUndefined()
  })

  it('should extract readOnly property for nested properties', () => {
    const properties = getPropertyListFrom(MOCK_MQTT_SCHEMA_READONLY)

    const versionProperty = properties.find((p) => p.key === 'version' && p.path.includes('config'))
    expect(versionProperty).toBeDefined()
    expect(versionProperty?.readOnly).toBe(true)

    const settingProperty = properties.find((p) => p.key === 'setting' && p.path.includes('config'))
    expect(settingProperty).toBeDefined()
    expect(settingProperty?.readOnly).toBeUndefined()
  })

  it('should extract required property for root-level properties', () => {
    const properties = getPropertyListFrom(MOCK_MQTT_SCHEMA_PLAIN)

    const firstNameProperty = properties.find((p) => p.key === 'firstName')
    expect(firstNameProperty).toBeDefined()
    expect(firstNameProperty?.required).toBe(true)

    const lastNameProperty = properties.find((p) => p.key === 'lastName')
    expect(lastNameProperty).toBeDefined()
    expect(lastNameProperty?.required).toBe(true)

    const ageProperty = properties.find((p) => p.key === 'age')
    expect(ageProperty).toBeDefined()
    expect(ageProperty?.required).toBeUndefined()
  })

  it('should extract required property for nested properties via $ref', () => {
    const properties = getPropertyListFrom(MOCK_MQTT_SCHEMA_REFS)

    // The address definition has required: ['street_address', 'city', 'state']
    const streetAddressProperty = properties.find(
      (p) => p.key === 'street_address' && p.path.includes('billing_address')
    )
    expect(streetAddressProperty).toBeDefined()
    expect(streetAddressProperty?.required).toBe(true)

    const cityProperty = properties.find((p) => p.key === 'city' && p.path.includes('billing_address'))
    expect(cityProperty).toBeDefined()
    expect(cityProperty?.required).toBe(true)
  })

  it('should not mark properties as required when not in required array', () => {
    const properties = getPropertyListFrom(MOCK_MQTT_SCHEMA_REFS)

    // The node definition has no required array, so name should not be required
    const nameProperty = properties.find((p) => p.key === 'name' && p.path.includes('tree'))
    expect(nameProperty).toBeDefined()
    expect(nameProperty?.required).toBeUndefined()
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
  it('should return the root object', async () => {
    expect(getSchemaFromPropertyList([])).toStrictEqual<RJSFSchema>({
      description: 'This is generated from the Edge data combiner',
      properties: {},
      title: 'OPCUA/MQTT combined schema',
      type: 'object',
    })
  })

  it('should handle root level properties with different types', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'stringProp',
        path: [],
        type: 'string',
        title: 'String Property',
      },
      {
        key: 'numberProp',
        path: [],
        type: 'number',
        title: 'Number Property',
      },
      {
        key: 'booleanProp',
        path: [],
        type: 'boolean',
        title: 'Boolean Property',
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    expect(result.properties).toEqual({
      stringProp: {
        type: 'string',
        title: 'String Property',
      },
      numberProp: {
        type: 'number',
        title: 'Number Property',
      },
      booleanProp: {
        type: 'boolean',
        title: 'Boolean Property',
      },
    })
  })

  it('should handle object properties with nested properties', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'parent',
        path: [],
        type: 'object',
        title: 'Parent Object',
      },
      {
        key: 'child',
        path: ['parent'],
        type: 'string',
        title: 'Child Property',
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    expect(result.properties).toEqual({
      parent: {
        type: 'object',
        title: 'Parent Object',
        properties: {
          child: {
            type: 'string',
            title: 'Child Property',
          },
        },
      },
    })
  })

  it('should handle array properties with arrayType', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'tags',
        path: [],
        type: 'array',
        arrayType: 'string',
        title: 'Tags Array',
      },
      {
        key: 'scores',
        path: [],
        type: 'array',
        arrayType: 'number',
        title: 'Scores Array',
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    expect(result.properties).toEqual({
      tags: {
        type: 'array',
        title: 'Tags Array',
        items: { type: 'string' },
      },
      scores: {
        type: 'array',
        title: 'Scores Array',
        items: { type: 'number' },
      },
    })
  })

  it('should handle nested array properties', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'container',
        path: [],
        type: 'object',
        title: 'Container',
      },
      {
        key: 'items',
        path: ['container'],
        type: 'array',
        arrayType: 'string',
        title: 'Items',
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    expect(result.properties).toEqual({
      container: {
        type: 'object',
        title: 'Container',
        properties: {
          items: {
            type: 'array',
            title: 'Items',
            items: { type: 'string' },
          },
        },
      },
    })
  })

  it('should handle properties with additional metadata', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'email',
        path: [],
        type: 'string',
        title: 'Email Address',
        description: 'User email address',
        examples: ['user@example.com'],
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    expect(result.properties).toEqual({
      email: {
        type: 'string',
        title: 'Email Address',
        description: 'User email address',
        examples: ['user@example.com'],
      },
    })
  })

  it('should filter out internal properties like path, key, arrayType, origin', async () => {
    const properties: FlatJSONSchema7[] = [
      {
        key: 'testProp',
        path: [],
        type: 'string',
        title: 'Test Property',
        arrayType: 'object',
        origin: 'some-origin',
      },
    ]

    const result = getSchemaFromPropertyList(properties)
    const testProp = result.properties?.['testProp'] as RJSFSchema

    expect(testProp).toBeDefined()
    expect(testProp).not.toHaveProperty('path')
    expect(testProp).not.toHaveProperty('key')
    expect(testProp).not.toHaveProperty('arrayType')
    expect(testProp).not.toHaveProperty('origin')
  })
})
