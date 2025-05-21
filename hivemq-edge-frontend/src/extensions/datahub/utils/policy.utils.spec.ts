import { describe, expect } from 'vitest'

import type { PolicySchema, SchemaList } from '@/api/__generated__'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { type ExpandableGroupedResource, groupResourceItems } from './policy.utils'

interface GroupSchemaTest {
  data: SchemaList | undefined
  result: ExpandableGroupedResource<PolicySchema>[]
  prompt: {
    input: string
    output: string
  }
}

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
        { ...mockSchemaTempHumidity, createdAt: '2021-10-13T11:51:24.234Z' },
        { ...mockSchemaTempHumidity, version: 2 },
      ],
    },
    result: [
      {
        children: [
          {
            ...mockSchemaTempHumidity,
            createdAt: '2021-10-13T11:51:24.234Z',
          },
          {
            ...mockSchemaTempHumidity,
            version: 2,
          },
        ],
        ...mockSchemaTempHumidity,
        createdAt: '2021-10-13T11:51:24.234Z',
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
