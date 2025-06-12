import type { RJSFSchema } from '@rjsf/utils'
import { expect, describe, it, vi, beforeEach } from 'vitest'

import type { FunctionSpecs } from '@/api/__generated__'
import { getNodeValidationSchema } from '@datahub/config/schemas.config.ts'
import { DataHubNodeType } from '@datahub/types.ts'

import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/designer/transition/TransitionData.ts'
import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/designer/topic_filter/TopicFilterData.ts'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'

interface NodeValidationSchemaSuite {
  type: string
  expected?: RJSFSchema | undefined
  functions?: FunctionSpecs[]
  description: string
}

describe('getNodeValidationSchema', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const testCases: NodeValidationSchemaSuite[] = [
    {
      type: DataHubNodeType.VALIDATOR,
      expected: MOCK_VALIDATOR_SCHEMA.schema,
      description: 'should return validator schema',
    },
    {
      type: DataHubNodeType.TRANSITION,
      expected: MOCK_TRANSITION_SCHEMA.schema,
      description: 'should return transition schema',
    },
    {
      type: DataHubNodeType.TOPIC_FILTER,
      expected: MOCK_TOPIC_FILTER_SCHEMA.schema,
      description: 'should return topic filter schema',
    },
    {
      type: DataHubNodeType.FUNCTION,
      expected: MOCK_FUNCTION_SCHEMA.schema,
      description: 'should return function schema',
    },
    {
      type: DataHubNodeType.SCHEMA,
      expected: MOCK_SCHEMA_SCHEMA.schema,
      description: 'should return schema schema',
    },
    {
      type: DataHubNodeType.DATA_POLICY,
      expected: MOCK_DATA_POLICY_SCHEMA.schema,
      description: 'should return data policy schema',
    },
    {
      type: DataHubNodeType.CLIENT_FILTER,
      expected: MOCK_CLIENT_FILTER_SCHEMA.schema,
      description: 'should return client filter schema',
    },
    {
      type: DataHubNodeType.BEHAVIOR_POLICY,
      expected: MOCK_BEHAVIOR_POLICY_SCHEMA.schema,
      description: 'should return behavior policy schema',
    },
    {
      type: DataHubNodeType.OPERATION,
      description: 'should return operation schema',
    },
    {
      type: 'UNKNOWN_TYPE',
      expected: undefined,
      description: 'should return undefined for unknown type',
    },
  ]

  it.each(testCases)('$description', ({ type, expected, functions }) => {
    const result = getNodeValidationSchema(type, functions)

    if (type === DataHubNodeType.OPERATION) {
      // expect(getOperationSchema).toHaveBeenCalledWith(functions)
      expect(result).toStrictEqual(
        expect.objectContaining({
          $ref: '#/definitions/functionId',
          definitions: expect.objectContaining({
            functionId: expect.objectContaining({
              properties: expect.objectContaining({
                functionId: expect.objectContaining({ title: 'Function' }),
                id: expect.objectContaining({ title: 'id', type: 'string' }),
              }),
            }),
          }),
        })
      )
    } else {
      expect(result).toStrictEqual(expected)
    }
  })
})
