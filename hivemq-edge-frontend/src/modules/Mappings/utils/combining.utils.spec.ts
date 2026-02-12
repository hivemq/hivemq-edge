import type { CombinerContext } from '@/modules/Mappings/types.ts'
import type { UseQueryResult } from '@tanstack/react-query'
import { beforeEach, describe, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper'
import { server } from '@/__test-utils__/msw/mockServer'

import type { DomainTag, DomainTagList, EntityReference, TopicFilter, TopicFilterList } from '@/api/__generated__'
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
  reconstructSelectedSources,
  getDataReference,
} from './combining.utils'
import type { DataCombining, Instruction } from '@/api/__generated__'

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
          scope: null,
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

describe('getDataReference', () => {
  const mockTagQuery = (tags: DomainTag[]): Partial<UseQueryResult<DomainTagList, Error>> => ({
    data: { items: tags },
    isLoading: false,
    isSuccess: true,
    isError: false,
  })

  const mockTopicFilterQuery = (
    topicFilters: TopicFilter[]
  ): Partial<UseQueryResult<TopicFilterList, Error>> => ({
    data: { items: topicFilters },
    isLoading: false,
    isSuccess: true,
    isError: false,
  })

  it('should return empty array for undefined context', () => {
    const result = getDataReference(undefined)
    expect(result).toEqual([])
  })

  it('should return empty array for context without entityQueries', () => {
    const context: CombinerContext = {}
    const result = getDataReference(context)
    expect(result).toEqual([])
  })

  it('should handle ADAPTER entities with tags', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([
            { name: 'temperature', description: 'Temperature sensor' } as DomainTag,
            { name: 'pressure', description: 'Pressure sensor' } as DomainTag,
          ]) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'temperature',
      type: DataIdentifierReference.type.TAG,
      scope: 'modbus-adapter',
    })
    expect(result[1]).toEqual({
      id: 'pressure',
      type: DataIdentifierReference.type.TAG,
      scope: 'modbus-adapter',
    })
  })

  it('should handle EDGE_BROKER entities with tags', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'edge-broker-1', type: EntityType.EDGE_BROKER },
          query: mockTagQuery([{ name: 'broker-tag', description: 'Broker tag' } as DomainTag]) as UseQueryResult<
            DomainTagList,
            Error
          >,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'broker-tag',
      type: DataIdentifierReference.type.TAG,
      scope: 'edge-broker-1',
    })
  })

  it('should handle BRIDGE entities with null scope for tags', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'bridge-1', type: EntityType.BRIDGE },
          query: mockTagQuery([{ name: 'bridge-tag', description: 'Bridge tag' } as DomainTag]) as UseQueryResult<
            DomainTagList,
            Error
          >,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'bridge-tag',
      type: DataIdentifierReference.type.TAG,
      scope: null,
    })
  })

  it('should correctly handle mixed entity types without index misalignment', () => {
    // This is the critical test for the bug fix
    // When entityQueries contains ADAPTER, BRIDGE, ADAPTER, the old code would misalign indices
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([{ name: 'modbus-temp', description: 'Modbus temperature' } as DomainTag]) as UseQueryResult<
            DomainTagList,
            Error
          >,
        },
        {
          entity: { id: 'bridge-1', type: EntityType.BRIDGE },
          query: mockTagQuery([{ name: 'bridge-tag', description: 'Bridge tag' } as DomainTag]) as UseQueryResult<
            DomainTagList,
            Error
          >,
        },
        {
          entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([{ name: 'opcua-temp', description: 'OPC-UA temperature' } as DomainTag]) as UseQueryResult<
            DomainTagList,
            Error
          >,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(3)

    // Verify each tag has the correct scope from its entity
    expect(result[0]).toEqual({
      id: 'modbus-temp',
      type: DataIdentifierReference.type.TAG,
      scope: 'modbus-adapter', // ✅ Should be modbus-adapter, not undefined
    })
    expect(result[1]).toEqual({
      id: 'bridge-tag',
      type: DataIdentifierReference.type.TAG,
      scope: null, // ✅ Bridge entities get null scope
    })
    expect(result[2]).toEqual({
      id: 'opcua-temp',
      type: DataIdentifierReference.type.TAG,
      scope: 'opcua-adapter', // ✅ Should be opcua-adapter, NOT undefined or modbus-adapter!
    })
  })

  it('should handle topic filters with null scope', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'adapter-1', type: EntityType.ADAPTER },
          query: mockTopicFilterQuery([
            { topicFilter: 'topic/filter/1' } as TopicFilter,
            { topicFilter: 'topic/filter/2' } as TopicFilter,
          ]) as UseQueryResult<TopicFilterList, Error>,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      id: 'topic/filter/1',
      type: DataIdentifierReference.type.TOPIC_FILTER,
      scope: null,
    })
    expect(result[1]).toEqual({
      id: 'topic/filter/2',
      type: DataIdentifierReference.type.TOPIC_FILTER,
      scope: null,
    })
  })

  it('should handle mixed tags and topic filters from different entity types', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'modbus-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([{ name: 'modbus-tag' } as DomainTag]) as UseQueryResult<DomainTagList, Error>,
        },
        {
          entity: { id: 'bridge-1', type: EntityType.BRIDGE },
          query: mockTopicFilterQuery([{ topicFilter: 'bridge/topic' } as TopicFilter]) as UseQueryResult<
            TopicFilterList,
            Error
          >,
        },
        {
          entity: { id: 'opcua-adapter', type: EntityType.ADAPTER },
          query: mockTagQuery([{ name: 'opcua-tag' } as DomainTag]) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(3)
    expect(result[0]).toEqual({
      id: 'modbus-tag',
      type: DataIdentifierReference.type.TAG,
      scope: 'modbus-adapter',
    })
    expect(result[1]).toEqual({
      id: 'bridge/topic',
      type: DataIdentifierReference.type.TOPIC_FILTER,
      scope: null,
    })
    expect(result[2]).toEqual({
      id: 'opcua-tag',
      type: DataIdentifierReference.type.TAG,
      scope: 'opcua-adapter',
    })
  })

  it('should skip entity queries with empty data', () => {
    const context: CombinerContext = {
      entityQueries: [
        {
          entity: { id: 'adapter-1', type: EntityType.ADAPTER },
          query: mockTagQuery([]) as UseQueryResult<DomainTagList, Error>,
        },
        {
          entity: { id: 'adapter-2', type: EntityType.ADAPTER },
          query: mockTagQuery([{ name: 'tag-1' } as DomainTag]) as UseQueryResult<DomainTagList, Error>,
        },
      ],
    }

    const result = getDataReference(context)

    expect(result).toHaveLength(1)
    expect(result[0]).toEqual({
      id: 'tag-1',
      type: DataIdentifierReference.type.TAG,
      scope: 'adapter-2',
    })
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
      scope: null,
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
  // Shared mock data
  const mockTagQuery = (tags: string[]): Partial<UseQueryResult<DomainTagList, Error>> => ({
    data: {
      items: tags.map((name) => ({ name }) as DomainTag),
    },
  })

  const mockTopicFilterQuery = (filters: string[]): Partial<UseQueryResult<TopicFilterList, Error>> => ({
    data: {
      items: filters.map((topicFilter) => ({ topicFilter }) as TopicFilter),
    },
  })

  const mockEmptyQuery = (): Partial<UseQueryResult<DomainTagList, Error>> => ({
    data: {
      items: [],
    },
  })

  it('should return undefined when formContext is undefined', () => {
    expect(getAdapterIdForTag('tag1', undefined)).toBeUndefined()
  })

  it('should return undefined when formContext has no queries', () => {
    const context: CombinerContext = { entities: [], queries: undefined }
    expect(getAdapterIdForTag('tag1', context)).toBeUndefined()
  })

  it('should return undefined when formContext has no entities', () => {
    const context: CombinerContext = { entities: undefined, queries: [] }
    expect(getAdapterIdForTag('tag1', context)).toBeUndefined()
  })

  it('should return undefined when tag is not found', () => {
    const context: CombinerContext = {
      entities: [{ id: 'adapter1', type: EntityType.ADAPTER }],
      queries: [mockTagQuery(['tag1', 'tag2']) as UseQueryResult<DomainTagList | TopicFilterList, Error>],
    }
    expect(getAdapterIdForTag('nonexistent', context)).toBeUndefined()
  })

  it('should return adapterId when tag is found', () => {
    const context: CombinerContext = {
      entities: [{ id: 'opcua-adapter-1', type: EntityType.ADAPTER }],
      queries: [mockTagQuery(['temperature', 'pressure']) as UseQueryResult<DomainTagList | TopicFilterList, Error>],
    }
    expect(getAdapterIdForTag('temperature', context)).toBe('opcua-adapter-1')
  })

  it('should return correct adapterId when multiple adapters exist', () => {
    const context: CombinerContext = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [
        mockTagQuery(['tag1', 'tag2']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
        mockTagQuery(['temperature', 'pressure']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
      ],
    }
    expect(getAdapterIdForTag('temperature', context)).toBe('adapter2')
  })

  it('should skip queries with empty items', () => {
    const context: CombinerContext = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [
        mockEmptyQuery() as UseQueryResult<DomainTagList | TopicFilterList, Error>,
        mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
      ],
    }
    expect(getAdapterIdForTag('tag1', context)).toBe('adapter2')
  })

  it('should skip queries with topic filters (not tags)', () => {
    // Queries and entities are parallel arrays - each query corresponds to an entity at the same index
    const context: CombinerContext = {
      entities: [
        { id: 'adapter1', type: EntityType.ADAPTER },
        { id: 'adapter2', type: EntityType.ADAPTER },
      ],
      queries: [
        mockTopicFilterQuery(['filter1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
        mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
      ],
    }
    // Should find tag1 in query[1] and return entities[1] (adapter2)
    expect(getAdapterIdForTag('tag1', context)).toBe('adapter2')
  })

  it('should return undefined when adapter entity is missing at query index', () => {
    const context: CombinerContext = {
      entities: [], // No adapters
      queries: [mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>],
    }
    expect(getAdapterIdForTag('tag1', context)).toBeUndefined()
  })

  describe('with entityQueries (new structure)', () => {
    it('should return adapterId from entityQueries', () => {
      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'modbus-adapter-1', type: EntityType.ADAPTER },
            query: mockTagQuery(['temperature', 'pressure']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }
      expect(getAdapterIdForTag('temperature', context)).toBe('modbus-adapter-1')
    })

    it('should skip non-adapter entities in entityQueries', () => {
      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'bridge-1', type: EntityType.BRIDGE },
            query: mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
          {
            entity: { id: 'adapter1', type: EntityType.ADAPTER },
            query: mockTagQuery(['temperature']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }
      expect(getAdapterIdForTag('temperature', context)).toBe('adapter1')
    })

    it('should return undefined when entityQueries array is empty', () => {
      const context: CombinerContext = {
        entityQueries: [],
      }
      expect(getAdapterIdForTag('tag1', context)).toBeUndefined()
    })
  })
})

describe('reconstructSelectedSources', () => {
  // Helper to create mock tag query
  const mockTagQuery = (tags: string[]): Partial<UseQueryResult<DomainTagList, Error>> => ({
    data: {
      items: tags.map((name) => ({ name }) as DomainTag),
    },
  })

  // Default primary for tests that don't specifically test primary matching
  const mockPrimary: DataIdentifierReference = {
    id: 'default-primary',
    type: DataIdentifierReference.type.TAG,
    scope: 'default-adapter',
  }

  describe('edge cases - no data', () => {
    it('should return empty arrays when formData is undefined', () => {
      const result = reconstructSelectedSources(undefined, undefined)
      expect(result).toEqual({ tags: [], topicFilters: [] })
    })

    it('should return empty arrays when formData has no sources', () => {
      const formData: Partial<DataCombining> = {}
      const result = reconstructSelectedSources(formData as DataCombining, undefined)
      expect(result).toEqual({ tags: [], topicFilters: [] })
    })

    it('should return empty arrays when sources has no tags or topicFilters', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
        },
      }
      const result = reconstructSelectedSources(formData as DataCombining, undefined)
      expect(result).toEqual({ tags: [], topicFilters: [] })
    })

    it('should return empty arrays when tags and topicFilters are empty arrays', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: [],
          topicFilters: [],
        },
      }
      const result = reconstructSelectedSources(formData as DataCombining, undefined)
      expect(result).toEqual({ tags: [], topicFilters: [] })
    })
  })

  describe('tags reconstruction - Strategy 1: Primary source', () => {
    it('should use primary source scope when tag matches primary', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['temperature'],
          primary: {
            id: 'temperature',
            type: DataIdentifierReference.type.TAG,
            scope: 'modbus-adapter-1',
          },
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0]).toEqual({
        id: 'temperature',
        type: DataIdentifierReference.type.TAG,
        scope: 'modbus-adapter-1',
      })
    })

    it('should NOT use primary when tag does not match primary id', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['pressure'],
          primary: {
            id: 'temperature',
            type: DataIdentifierReference.type.TAG,
            scope: 'modbus-adapter-1',
          },
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].id).toBe('pressure')
      expect(result.tags[0].scope).toBeNull() // Fallback to null
    })

    it('should NOT use primary when type is wrong (TOPIC_FILTER instead of TAG)', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['temperature'],
          primary: {
            id: 'temperature',
            type: DataIdentifierReference.type.TOPIC_FILTER, // Wrong type!
            scope: 'modbus-adapter-1',
          },
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull() // Should fallback
    })

    it('should NOT use primary when primary has no scope', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['temperature'],
          primary: {
            id: 'temperature',
            type: DataIdentifierReference.type.TAG,
            scope: null,
          },
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull()
    })
  })

  describe('tags reconstruction - Strategy 2: Instructions', () => {
    it('should find scope from instructions when tag is in sourceRef', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature', 'pressure'],
        },
        instructions: [
          {
            sourceRef: {
              id: 'temperature',
              type: DataIdentifierReference.type.TAG,
              scope: 'modbus-adapter-1',
            },
            destination: 'dest1',
            source: 'sourceField',
          },
          {
            sourceRef: {
              id: 'pressure',
              type: DataIdentifierReference.type.TAG,
              scope: 'opcua-adapter-2',
            },
            destination: 'dest2',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(2)
      expect(result.tags[0]).toEqual({
        id: 'temperature',
        type: DataIdentifierReference.type.TAG,
        scope: 'modbus-adapter-1',
      })
      expect(result.tags[1]).toEqual({
        id: 'pressure',
        type: DataIdentifierReference.type.TAG,
        scope: 'opcua-adapter-2',
      })
    })

    it('should skip instructions with wrong type (TOPIC_FILTER instead of TAG)', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature'],
        },
        instructions: [
          {
            sourceRef: {
              id: 'temperature',
              type: DataIdentifierReference.type.TOPIC_FILTER, // Wrong type
              scope: 'adapter1',
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull() // Should fallback
    })

    it('should skip instructions with no scope', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature'],
        },
        instructions: [
          {
            sourceRef: {
              id: 'temperature',
              type: DataIdentifierReference.type.TAG,
              scope: null,
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull()
    })

    it('should handle missing instructions array', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature'],
        },
        instructions: undefined,
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull() // Fallback
    })
  })

  describe('tags reconstruction - Strategy 3: Context lookup', () => {
    it('should find scope via context lookup with entityQueries', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature'],
        },
      }

      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'modbus-adapter-1', type: EntityType.ADAPTER },
            query: mockTagQuery(['temperature', 'pressure']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0]).toEqual({
        id: 'temperature',
        type: DataIdentifierReference.type.TAG,
        scope: 'modbus-adapter-1',
      })
    })

    it('should find scope via context lookup with legacy queries/entities', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['pressure'],
        },
      }

      const context: CombinerContext = {
        entities: [{ id: 'opcua-adapter-1', type: EntityType.ADAPTER }],
        queries: [mockTagQuery(['temperature', 'pressure']) as UseQueryResult<DomainTagList | TopicFilterList, Error>],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0]).toEqual({
        id: 'pressure',
        type: DataIdentifierReference.type.TAG,
        scope: 'opcua-adapter-1',
      })
    })

    it('should return null scope when tag not found in context', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['nonexistent-tag'],
        },
      }

      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'adapter1', type: EntityType.ADAPTER },
            query: mockTagQuery(['temperature']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0]).toEqual({
        id: 'nonexistent-tag',
        type: DataIdentifierReference.type.TAG,
        scope: null,
      })
    })

    it('should handle empty entityQueries array', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['temperature'],
        },
      }

      const context: CombinerContext = {
        entityQueries: [],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBeNull()
    })
  })

  describe('tags reconstruction - Mixed strategies', () => {
    it('should use different strategies for different tags', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['tag1', 'tag2', 'tag3'],
          primary: {
            id: 'tag1',
            type: DataIdentifierReference.type.TAG,
            scope: 'adapter-primary',
          },
        },
        instructions: [
          {
            sourceRef: {
              id: 'tag2',
              type: DataIdentifierReference.type.TAG,
              scope: 'adapter-instruction',
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'adapter-context', type: EntityType.ADAPTER },
            query: mockTagQuery(['tag3']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(3)
      expect(result.tags[0].scope).toBe('adapter-primary') // Strategy 1
      expect(result.tags[1].scope).toBe('adapter-instruction') // Strategy 2
      expect(result.tags[2].scope).toBe('adapter-context') // Strategy 3
    })

    it('should handle strategy fallback chain correctly', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['temperature'],
          primary: {
            id: 'pressure', // Different tag
            type: DataIdentifierReference.type.TAG,
            scope: 'wrong-adapter',
          },
        },
        instructions: [
          {
            sourceRef: {
              id: 'humidity', // Different tag
              type: DataIdentifierReference.type.TAG,
              scope: 'wrong-adapter',
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'correct-adapter', type: EntityType.ADAPTER },
            query: mockTagQuery(['temperature']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      // Should skip Strategy 1 (wrong id), skip Strategy 2 (wrong id), use Strategy 3
      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBe('correct-adapter')
    })
  })

  describe('topic filters reconstruction', () => {
    it('should return empty array when no topic filters', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          topicFilters: undefined,
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)
      expect(result.topicFilters).toEqual([])
    })

    it('should use primary when topic filter matches primary', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          topicFilters: ['a/topic/+/filter'],
          primary: {
            id: 'a/topic/+/filter',
            type: DataIdentifierReference.type.TOPIC_FILTER,
            scope: null,
          },
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.topicFilters).toHaveLength(1)
      expect(result.topicFilters[0]).toEqual({
        id: 'a/topic/+/filter',
        type: DataIdentifierReference.type.TOPIC_FILTER,
        scope: null,
      })
    })

    it('should find topic filter from instructions', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          topicFilters: ['filter1', 'filter2'],
        },
        instructions: [
          {
            sourceRef: {
              id: 'filter1',
              type: DataIdentifierReference.type.TOPIC_FILTER,
              scope: null,
            },
            destination: 'dest1',
            source: 'sourceField',
          },
          {
            sourceRef: {
              id: 'filter2',
              type: DataIdentifierReference.type.TOPIC_FILTER,
              scope: null,
            },
            destination: 'dest2',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.topicFilters).toHaveLength(2)
      expect(result.topicFilters[0].id).toBe('filter1')
      expect(result.topicFilters[1].id).toBe('filter2')
    })

    it('should default to null scope when not found in primary or instructions', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          topicFilters: ['filter1'],
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.topicFilters).toHaveLength(1)
      expect(result.topicFilters[0]).toEqual({
        id: 'filter1',
        type: DataIdentifierReference.type.TOPIC_FILTER,
        scope: null,
      })
    })

    it('should handle multiple topic filters with different sources', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          topicFilters: ['filter1', 'filter2', 'filter3'],
          primary: {
            id: 'filter1',
            type: DataIdentifierReference.type.TOPIC_FILTER,
            scope: null,
          },
        },
        instructions: [
          {
            sourceRef: {
              id: 'filter2',
              type: DataIdentifierReference.type.TOPIC_FILTER,
              scope: null,
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.topicFilters).toHaveLength(3)
      // filter1 from primary, filter2 from instruction, filter3 default
      expect(result.topicFilters.every((tf) => tf.scope === null)).toBe(true)
    })
  })

  describe('combined tags and topic filters', () => {
    it('should reconstruct both tags and topic filters correctly', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['temperature'],
          topicFilters: ['mqtt/sensor/+'],
          primary: {
            id: 'temperature',
            type: DataIdentifierReference.type.TAG,
            scope: 'modbus-1',
          },
        },
        instructions: [
          {
            sourceRef: {
              id: 'mqtt/sensor/+',
              type: DataIdentifierReference.type.TOPIC_FILTER,
              scope: null,
            },
            destination: 'dest1',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0].scope).toBe('modbus-1')
      expect(result.topicFilters).toHaveLength(1)
      expect(result.topicFilters[0].scope).toBeNull()
    })

    it('should handle empty tags with non-empty topic filters', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: [],
          topicFilters: ['filter1'],
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toEqual([])
      expect(result.topicFilters).toHaveLength(1)
    })

    it('should handle non-empty tags with empty topic filters', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['tag1'],
          topicFilters: [],
        },
      }

      const result = reconstructSelectedSources(formData as DataCombining, undefined)

      expect(result.tags).toHaveLength(1)
      expect(result.topicFilters).toEqual([])
    })
  })

  describe('complex real-world scenarios', () => {
    it('should handle mapping with multiple tags from different adapters', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          tags: ['modbus-temp', 'opcua-pressure', 'mqtt-humidity'],
          primary: {
            id: 'modbus-temp',
            type: DataIdentifierReference.type.TAG,
            scope: 'modbus-adapter-1',
          },
        },
        instructions: [
          {
            sourceRef: {
              id: 'modbus-temp',
              type: DataIdentifierReference.type.TAG,
              scope: 'modbus-adapter-1',
            },
            destination: 'temperature',
            source: 'sourceField',
          },
          {
            sourceRef: {
              id: 'opcua-pressure',
              type: DataIdentifierReference.type.TAG,
              scope: 'opcua-adapter-1',
            },
            destination: 'pressure',
            source: 'sourceField',
          },
        ] as Instruction[],
      }

      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'mqtt-adapter-1', type: EntityType.ADAPTER },
            query: mockTagQuery(['mqtt-humidity']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(3)
      expect(result.tags[0].scope).toBe('modbus-adapter-1') // From primary
      expect(result.tags[1].scope).toBe('opcua-adapter-1') // From instruction
      expect(result.tags[2].scope).toBe('mqtt-adapter-1') // From context
    })

    it('should handle orphaned tags (no scope found anywhere)', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['orphan-tag'],
        },
        instructions: [],
      }

      const context: CombinerContext = {
        entityQueries: [],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      expect(result.tags[0]).toEqual({
        id: 'orphan-tag',
        type: DataIdentifierReference.type.TAG,
        scope: null, // Should gracefully default to null
      })
    })

    it('should handle migration scenario with both old and new context structures', () => {
      const formData: Partial<DataCombining> = {
        sources: {
          primary: mockPrimary,
          tags: ['tag1'],
        },
      }

      // During migration, both structures might exist
      const context: CombinerContext = {
        entityQueries: [
          {
            entity: { id: 'new-adapter', type: EntityType.ADAPTER },
            query: mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>,
          },
        ],
        // Legacy structure also present
        entities: [{ id: 'old-adapter', type: EntityType.ADAPTER }],
        queries: [mockTagQuery(['tag1']) as UseQueryResult<DomainTagList | TopicFilterList, Error>],
      }

      const result = reconstructSelectedSources(formData as DataCombining, context)

      expect(result.tags).toHaveLength(1)
      // Should prefer new entityQueries structure
      expect(result.tags[0].scope).toBe('new-adapter')
    })
  })
})
