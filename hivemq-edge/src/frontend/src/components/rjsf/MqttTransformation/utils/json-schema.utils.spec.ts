import { expect } from 'vitest'
import {
  type FlatJSONSchema7,
  getPropertyListFrom,
} from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { MOCK_MQTT_SCHEMA_PLAIN, MOCK_MQTT_SCHEMA_REFS } from '@/api/hooks/useTopicOntology/__handlers__'

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
        {
          description: undefined,
          key: 'billing_address',
          path: [],
          title: 'Billing address',
          type: 'object',
        },
        {
          description: undefined,
          key: 'street_address',
          path: ['billing_address'],
          title: 'street_address',
          type: 'string',
        },
        {
          description: undefined,
          key: 'children',
          path: ['tree'],
          title: 'children',
          type: 'array',
        },
      ])
    )
  })

  it('should handle self-contained schema', async () => {
    expect(getPropertyListFrom(MOCK_MQTT_SCHEMA_PLAIN)).toStrictEqual<FlatJSONSchema7[]>(
      expect.arrayContaining([
        {
          description: undefined,
          key: 'firstName',
          path: [],
          title: 'First name',
          type: 'string',
        },
        {
          description: undefined,
          key: 'telephone',
          path: [],
          title: 'Telephone',
          type: 'string',
        },
        {
          description: undefined,
          key: 'minItemsList',
          path: [],
          title: 'A list with a minimal number of items',
          type: 'array',
        },
      ])
    )
  })
})
