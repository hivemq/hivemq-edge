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

  it('should handle definitions', async () => {
    expect(getPropertyListFrom(MOCK_MQTT_SCHEMA_REFS)).toStrictEqual<FlatJSONSchema7[]>(
      expect.arrayContaining([
        {
          description: undefined,
          path: [],
          title: 'Billing address',
          type: 'object',
        },
        {
          path: ['Billing address'],
          title: 'street_address',
          type: 'string',
        },
        {
          path: ['Recursive references'],
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
          path: [],
          title: 'First name',
          type: 'string',
        },
        {
          description: undefined,
          path: [],
          title: 'Telephone',
          type: 'string',
        },
      ])
    )
  })
})
