import { MOCK_SCRIPT_ID, mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { getSchemaFamilies, getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { ResourceStatus, ResourceWorkingVersion } from '@datahub/types.ts'
import { describe, expect } from 'vitest'

import type { PolicySchema, SchemaList, Script } from '@/api/__generated__'
import { MOCK_SCHEMA_ID, mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { type ExpandableGroupedResource, getResourceInternalStatus, groupResourceItems } from './policy.utils'

interface GroupSchemaTest {
  data: SchemaList | undefined
  result: ExpandableGroupedResource<PolicySchema>[]
  prompt: {
    input: string
    output: string
  }
}

const CREATED_AT = '2021-10-13T11:51:24.234Z'
const resourceNameTestSuite: GroupSchemaTest[] = [
  {
    data: undefined,
    result: [],
    prompt: {
      input: 'undefined',
      output: '[]',
    },
  },
  {
    data: { items: [] },
    result: [],
    prompt: {
      input: 'empty list',
      output: '[]',
    },
  },
  {
    data: { items: [mockSchemaTempHumidity] },
    result: [mockSchemaTempHumidity],
    prompt: {
      input: 'single item',
      output: 'same item',
    },
  },
  {
    data: {
      items: [
        { ...mockSchemaTempHumidity, createdAt: CREATED_AT },
        { ...mockSchemaTempHumidity, version: 2 },
      ],
    },
    result: [
      {
        children: [
          {
            ...mockSchemaTempHumidity,
            createdAt: CREATED_AT,
          },
          {
            ...mockSchemaTempHumidity,
            version: 2,
          },
        ],
        ...mockSchemaTempHumidity,
        createdAt: CREATED_AT,
        version: 2,
      },
    ],
    prompt: {
      input: 'two versions',
      output: 'one item with 2 children',
    },
  },
  {
    data: {
      items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, id: 'new policy' }],
    },
    result: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, id: 'new policy' }],
    prompt: {
      input: 'two versions',
      output: 'one item with 2 children',
    },
  },
]

describe('groupResourceItems', () => {
  it.each<GroupSchemaTest>(resourceNameTestSuite)(
    'should return $prompt.output with $prompt.input',
    ({ data, result }) => {
      expect(groupResourceItems<SchemaList, PolicySchema>(data)).toStrictEqual(result)
    }
  )
})

describe('getResourceInternalStatus', () => {
  it('should work for Schemas', () => {
    expect(getResourceInternalStatus<PolicySchema>('my-schema', {}, getSchemaFamilies)).toStrictEqual({
      internalStatus: ResourceStatus.DRAFT,
      version: ResourceWorkingVersion.DRAFT,
    })

    expect(
      getResourceInternalStatus<PolicySchema>(MOCK_SCHEMA_ID, { items: [mockSchemaTempHumidity] }, getSchemaFamilies)
    ).toStrictEqual({
      internalStatus: ResourceStatus.LOADED,
      internalVersions: [1],
      version: 1,
    })
  })
  it('should work for Scripts', () => {
    expect(getResourceInternalStatus<Script>('my-script', {}, getScriptFamilies)).toStrictEqual({
      internalStatus: ResourceStatus.DRAFT,
      version: ResourceWorkingVersion.DRAFT,
    })

    expect(getResourceInternalStatus<Script>(MOCK_SCRIPT_ID, { items: [mockScript] }, getScriptFamilies)).toStrictEqual(
      {
        internalStatus: ResourceStatus.LOADED,
        internalVersions: [1],
        version: 1,
      }
    )
  })
})
