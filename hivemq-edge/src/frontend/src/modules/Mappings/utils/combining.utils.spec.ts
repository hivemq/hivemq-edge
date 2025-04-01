import { describe, expect } from 'vitest'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { DomainTag, EntityReference, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { getCombinedDataEntityReference } from './combining.utils'

interface TestEachSuite {
  test: string
  content: (DomainTag[] | TopicFilter[])[]
  entities: EntityReference[]
  results: DataReference[]
}

const selectors: TestEachSuite[] = [
  { test: 'an empty context', content: [], entities: [], results: [] },
  { test: 'an empty list of tags', content: [[]], entities: [], results: [] },
  {
    test: 'tags without their sources',
    content: [MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)],
    entities: [],
    results: [
      {
        adapterId: undefined,
        id: 'opcua-1/power/off',
        type: DataIdentifierReference.type.TAG,
      },
      {
        adapterId: undefined,
        id: 'opcua-1/log/event',
        type: DataIdentifierReference.type.TAG,
      },
    ],
  },
  {
    test: 'an empty list of tags',
    content: [[MOCK_TOPIC_FILTER]],
    entities: [],
    results: [
      {
        adapterId: undefined,
        id: 'a/topic/+/filter',
        type: DataIdentifierReference.type.TOPIC_FILTER,
      },
    ],
  },
]

describe('getCombinedDataEntityReference', () => {
  it.each<TestEachSuite>(selectors)('should work for $test', ({ content, entities, results }) => {
    const updatedNodes = getCombinedDataEntityReference(content, entities)
    expect(updatedNodes).toStrictEqual(results)
  })
})

describe('getFilteredDataReferences', () => {
  it('should return a warning if not assigned', () => {})
})
