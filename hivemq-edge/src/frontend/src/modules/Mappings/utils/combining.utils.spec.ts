import { beforeEach, describe, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper'
import { server } from '@/__test-utils__/msw/mockServer'

import type { DomainTag, EntityReference, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'

import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers, MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { getCombinedDataEntityReference, getSchemasFromReferences } from './combining.utils'

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

interface TestEachSchemaSuite {
  test: string
  input: DataReference[]
  output: DataReference[]
}

const mockDataReferences: DataReference[] = [
  {
    id: 'my-tag',
    adapterId: 'string',
    type: DataIdentifierReference.type.TAG,
  },
  {
    id: 'a/topic/+/filter',
    type: DataIdentifierReference.type.TOPIC_FILTER,
  },
]

const schemaSuite: TestEachSchemaSuite[] = [
  {
    test: 'no data',
    input: [],
    output: [],
  },
  {
    test: 'references',
    input: mockDataReferences,
    output: [
      expect.objectContaining({
        adapterId: 'string',
        schema: expect.objectContaining({
          message: 'Your tag is currently assigned a valid schema',
          schema: expect.objectContaining({
            description: 'A simple form example.',
          }),
        }),
      }),
      expect.objectContaining({
        id: 'a/topic/+/filter',
        schema: expect.objectContaining({
          message: 'Your topic filter is currently assigned a valid schema',
          schema: expect.objectContaining({
            title: 'This is a simple schema',
          }),
        }),
      }),
    ],
  },
]

describe('getSchemasFromReferences', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it.each<TestEachSchemaSuite>(schemaSuite)('should work for $test', async ({ input, output }) => {
    const { result } = renderHook(() => useGetCombinedDataSchemas(mockDataReferences), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
      expect(result.current.every((query) => query.isLoading)).toBeFalsy()
    })

    expect(getSchemasFromReferences(input, result.current)).toStrictEqual(output)
  })

  it('should work for the wrong schema', async () => {
    expect(
      getSchemasFromReferences(mockDataReferences, [
        // @ts-ignore
        { data: undefined, error: new Error('error loading my-tag') },
        // @ts-ignore
        { data: undefined, error: new Error('a/topic/+/filter') },
      ])
    ).toStrictEqual([
      expect.objectContaining({
        adapterId: 'string',
        id: 'my-tag',
        schema: expect.objectContaining({
          message: 'Your tag is currently not assigned a schema',
          status: 'warning',
        }),
      }),
      expect.objectContaining({
        id: 'a/topic/+/filter',
        schema: expect.objectContaining({
          message: 'Your topic filter is currently not assigned a schema',
          status: 'warning',
        }),
      }),
    ])
  })
})
