import { beforeEach, describe, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper'
import { server } from '@/__test-utils__/msw/mockServer'

import type { DomainTag, EntityReference, TopicFilter } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'

import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { useGetCombinedDataSchemas } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers, MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

import type { AutoMatchAccumulator } from './combining.utils'
import {
  findBestMatch,
  getCombinedDataEntityReference,
  getSchemasFromReferences,
  getFilteredDataReferences,
  getAdapterIdForTag,
} from './combining.utils'

describe('getCombinedDataEntityReference', () => {
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
          scope: undefined,
          id: 'opcua-1/power/off',
          type: DataIdentifierReference.type.TAG,
        },
        {
          scope: undefined,
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
          scope: undefined,
          id: 'a/topic/+/filter',
          type: DataIdentifierReference.type.TOPIC_FILTER,
        },
      ],
    },
  ]

  it.each<TestEachSuite>(selectors)('should work for $test', ({ content, entities, results }) => {
    const updatedNodes = getCombinedDataEntityReference(content, entities)
    expect(updatedNodes).toStrictEqual(results)
  })
})

describe('getSchemasFromReferences', () => {
  interface TestEachSchemaSuite {
    test: string
    input: DataReference[]
    output: DataReference[]
  }

  const mockDataReferences: DataReference[] = [
    {
      id: 'my-tag',
      scope: 'string',
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
          scope: 'string',
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
        scope: 'string',
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

describe('getFilteredDataReferences', () => {
  beforeEach(() => {
    server.use(...mappingHandlers, ...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should work for undefined', async () => {
    expect(getFilteredDataReferences(undefined, undefined)).toStrictEqual([])
  })
})

describe('findBestMatch', () => {
  interface TestMatchSuite {
    test: string
    source: FlatJSONSchema7
    candidates: FlatJSONSchema7[]
    result: AutoMatchAccumulator | undefined
    min?: number | null
  }

  const mocProperty: FlatJSONSchema7 = {
    path: [],
    key: 'test_string1',
  }

  const tests: TestMatchSuite[] = [
    {
      test: 'same source',
      source: mocProperty,
      candidates: [mocProperty],
      result: { distance: 0, value: mocProperty },
    },
    {
      test: 'same source',
      source: mocProperty,
      candidates: [
        { path: [], key: 'aaaa' },
        { path: [], key: 'test_string2' },
      ],
      result: { distance: 1, value: { path: [], key: 'test_string2' } },
    },
    {
      test: 'same source',
      source: mocProperty,
      candidates: [
        { path: [], key: 'aaaa' },
        { path: [], key: 'bbbb' },
      ],
      result: undefined,
    },
  ]

  it.each<TestMatchSuite>(tests)('should work for $test', ({ source, candidates, result }) => {
    expect(findBestMatch(source, candidates)).toStrictEqual(result)
  })
})

describe('getAdapterIdForTag', () => {
  it('should return undefined when formContext is undefined', () => {
    expect(getAdapterIdForTag('tag1', undefined)).toBeUndefined()
  })

  it('should return undefined when formContext has no queries', () => {
    const context = { entities: [], queries: undefined }
    expect(getAdapterIdForTag('tag1', context as any)).toBeUndefined()
  })

  it('should return undefined when formContext has no entities', () => {
    const context = { entities: undefined, queries: [] }
    expect(getAdapterIdForTag('tag1', context as any)).toBeUndefined()
  })

  it('should return undefined when tag is not found', () => {
    const mockQuery = {
      data: {
        items: [
          { name: 'tag1' } as DomainTag,
          { name: 'tag2' } as DomainTag,
        ],
      },
    }
    const context = {
      entities: [{ id: 'adapter1', type: 'ADAPTER' }],
      queries: [mockQuery],
    }
    expect(getAdapterIdForTag('nonexistent', context as any)).toBeUndefined()
  })

  it('should return adapterId when tag is found', () => {
    const mockQuery = {
      data: {
        items: [
          { name: 'temperature' } as DomainTag,
          { name: 'pressure' } as DomainTag,
        ],
      },
    }
    const context = {
      entities: [{ id: 'opcua-adapter-1', type: EntityType.ADAPTER }],
      queries: [mockQuery],
    }
    expect(getAdapterIdForTag('temperature', context as any)).toBe('opcua-adapter-1')
  })

  it('should return correct adapterId when multiple adapters exist', () => {
    const mockQuery1 = {
      data: {
        items: [
          { name: 'tag1' } as DomainTag,
          { name: 'tag2' } as DomainTag,
        ],
      },
    }
    const mockQuery2 = {
      data: {
        items: [
          { name: 'temperature' } as DomainTag,
          { name: 'pressure' } as DomainTag,
        ],
      },
    }
    const context = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [mockQuery1, mockQuery2],
    }
    expect(getAdapterIdForTag('temperature', context as any)).toBe('adapter2')
  })

  it('should skip queries with empty items', () => {
    const mockQuery1 = {
      data: {
        items: [],
      },
    }
    const mockQuery2 = {
      data: {
        items: [{ name: 'tag1' } as DomainTag],
      },
    }
    const context = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [mockQuery1, mockQuery2],
    }
    expect(getAdapterIdForTag('tag1', context as any)).toBe('adapter2')
  })

  it('should skip queries with topic filters (not tags)', () => {
    const mockQuery1 = {
      data: {
        items: [{ topicFilter: 'filter1' } as TopicFilter],
      },
    }
    const mockQuery2 = {
      data: {
        items: [{ name: 'tag1' } as DomainTag],
      },
    }
    // Queries and entities are parallel arrays - each query corresponds to an entity at the same index
    const context = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [mockQuery1, mockQuery2],
    }
    // Should find tag1 in query[1] and return entities[1] (adapter2)
    expect(getAdapterIdForTag('tag1', context as any)).toBe('adapter2')
  })

  it('should return undefined when adapter entity is missing at query index', () => {
    const mockQuery = {
      data: {
        items: [{ name: 'tag1' } as DomainTag],
      },
    }
    const context = {
      entities: [], // No adapters
      queries: [mockQuery],
    }
    expect(getAdapterIdForTag('tag1', context as any)).toBeUndefined()
  })
})
