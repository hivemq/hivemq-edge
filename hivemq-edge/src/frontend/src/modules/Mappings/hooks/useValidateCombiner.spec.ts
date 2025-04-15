import { beforeEach, expect } from 'vitest'
import { v4 as uuidv4 } from 'uuid'
import { http, HttpResponse, type HttpHandler } from 'msw'
import { renderHook, waitFor } from '@testing-library/react'
import { createErrorHandler, toErrorList } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { MOCK_EMPTY_SCHEMA_URI, MOCK_SIMPLE_SCHEMA_URI } from '@/__test-utils__/rjsf/schema.mocks'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type {
  AdaptersList,
  Combiner,
  DataCombining,
  DomainTagList,
  EntityReference,
  JsonNode,
  ProtocolAdaptersList,
  TopicFilterList,
} from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import {
  deviceHandlers,
  handlers as failCapabilityHandlers,
  mockAdapter_OPCUA,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombinerId, mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'
import {
  handlers as topicFilterHandlers,
  MOCK_TOPIC_FILTER,
  MOCK_TOPIC_FILTER_SCHEMA_INVALID,
} from '@/api/hooks/useTopicFilters/__handlers__'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks'
import { useGetCombinedEntities } from '@/api/hooks/useDomainModel/useGetCombinedEntities'

import { useValidateCombiner } from './useValidateCombiner'

const capabilityHandlers = [
  http.get('*/protocol-adapters/types', () => {
    return HttpResponse.json<ProtocolAdaptersList>({ items: [mockProtocolAdapter_OPCUA] }, { status: 200 })
  }),

  http.get('*/protocol-adapters/adapters', () => {
    return HttpResponse.json<AdaptersList>({ items: [mockAdapter_OPCUA] }, { status: 200 })
  }),
]

describe('useValidateCombiner', () => {
  beforeEach(() => {
    server.use(...capabilityHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  const loadingEntities = async (
    sources: EntityReference[],
    topicHandlers?: HttpHandler[],
    tagHandlers?: HttpHandler[]
  ) => {
    server.use(...(tagHandlers || mappingHandlers), ...deviceHandlers, ...(topicHandlers || topicFilterHandlers))
    const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

    expect(result.current).toHaveLength(2)
    await waitFor(() => {
      expect(result.current[0].isSuccess).toBeTruthy()
      expect(result.current[1].isSuccess).toBeTruthy()
    })

    return result
  }

  const renderValidateHook = async (
    formData: Combiner | undefined,
    queries?: UseQueryResult<DomainTagList | TopicFilterList, Error>[],
    entities?: EntityReference[]
  ) => {
    const errors = createErrorHandler<Combiner>(formData || mockEmptyCombiner)
    const { result } = renderHook(() => useValidateCombiner(queries || [], entities || []), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
    })

    const formValidation = result.current?.(formData, errors)
    return toErrorList(formValidation)
  }

  describe('validateSourceCapability', () => {
    it('should fail to validate an empty payload', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook(undefined)
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The combiner payload must be defined',
        }),
      ])
    })

    it('should fail to validate an unknown adapter', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'dd',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: "The Edge broker must be connected to the combiner's sources",
        }),
        expect.objectContaining({
          message: 'This is not a valid reference to a Workspace entity',
        }),
      ])
    })

    it('should fail to validate an adapter without combine', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'my-adapter',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: "The Edge broker must be connected to the combiner's sources",
        }),
        expect.objectContaining({
          message: 'The adapter does not support data combining and cannot be used as a source',
        }),
      ])
    })

    it('should fail to validate an adapter without combine but validate the Edge', async () => {
      server.use(...failCapabilityHandlers)
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'the edge name',
              type: EntityType.EDGE_BROKER,
            },
            {
              id: 'my-adapter',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The adapter does not support data combining and cannot be used as a source',
        }),
      ])
    })

    it('should validate properly', async () => {
      const errors = await renderValidateHook({
        id: mockCombinerId,
        name: 'my-combiner',
        sources: {
          items: [
            {
              id: 'the edge name',
              type: EntityType.EDGE_BROKER,
            },
            {
              id: 'opcua-1',
              type: EntityType.ADAPTER,
            },
          ],
        },
        mappings: {
          items: [],
        },
      })
      expect(errors).toStrictEqual([])
    })
  })

  describe('validateDataSources', () => {
    const sources: EntityReference[] = [
      {
        id: 'the edge name',
        type: EntityType.EDGE_BROKER,
      },
      {
        id: 'opcua-1',
        type: EntityType.ADAPTER,
      },
    ]
    const getFormData = (mappings: DataCombining[]): Combiner => ({
      id: mockCombinerId,
      name: 'my-combiner',
      sources: {
        items: sources,
      },
      mappings: {
        items: mappings,
      },
    })

    it('should validate an empty list of mappings', async () => {
      const errors = await renderValidateHook(getFormData([]))
      expect(errors).toStrictEqual([])
    })

    it('should not validate an empty list of mappings', async () => {
      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: [],
              topicFilters: [],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {},
            instructions: [],
          },
        ])
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'At least one schema should be available',
        }),
      ])
    })

    it('should not validate a tag not belonging to a source', async () => {
      await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['test/tag1'],
              topicFilters: [],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {},
            instructions: [],
          },
        ]),
        [],
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'At least one schema should be available',
        }),
        expect.objectContaining({
          message: "The tag test/tag1 is not defined in any of the combiner's sources",
        }),
      ])
    })

    it('should not validate a topic filter not belonging to a source', async () => {
      await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: [],
              topicFilters: ['test/topic/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {},
            instructions: [],
          },
        ]),
        [],
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'At least one schema should be available',
        }),
        expect.objectContaining({
          message: "The topic filter test/topic/filter is not defined in any of the combiner's sources",
        }),
      ])
    })

    it('should validate tag and topic filter belonging to a source', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['opcua-1/log/event'],
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {},
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([])
    })
  })

  describe('validateDataSourceSchemas', () => {
    const sources: EntityReference[] = [
      {
        id: 'the edge name',
        type: EntityType.EDGE_BROKER,
      },
      {
        id: 'opcua-1',
        type: EntityType.ADAPTER,
      },
    ]
    const getFormData = (mappings: DataCombining[]): Combiner => ({
      id: mockCombinerId,
      name: 'my-combiner',
      sources: {
        items: sources,
      },
      mappings: {
        items: mappings,
      },
    })

    it('should not validate if there is not at least one schema displayed', async () => {
      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        [],
        sources
      )
      expect(errors).toStrictEqual([expect.objectContaining({ message: 'At least one schema should be available' })])
    })

    // TODO[NVL] These validation are wrong; need to refactor the data structure for clarity
    it.skip('should not validate when a topic filter schema is invalid', async () => {
      const result = await loadingEntities(sources, [
        http.get('**/management/topic-filters', () => {
          return HttpResponse.json<TopicFilterList>(
            { items: [{ ...MOCK_TOPIC_FILTER, schema: MOCK_TOPIC_FILTER_SCHEMA_INVALID }] },
            { status: 200 }
          )
        }),
      ])

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({ message: 'Not a valid JSONSchema: `properties` is missing' }),
      ])
    })

    // TODO[NVL] These validation are wrong; need to refactor the data structure for clarity
    it.skip('should not validate when a tag schema is invalid', async () => {
      const result = await loadingEntities(sources, undefined, [
        http.get<{ adapterId: string; tagName: string }>(
          '*/management/protocol-adapters/writing-schema/:adapterId/:tagName',
          ({ params }) => {
            const { tagName } = params

            return HttpResponse.json<JsonNode>({ description: 'fake schema', title: tagName }, { status: 200 })
          }
        ),
      ])

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['opcua-1/log/event'],
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({ message: 'Not a valid JSONSchema: `properties` is missing' }),
      ])
    })
  })

  describe('validateDestinationSchema', () => {
    const sources: EntityReference[] = [
      {
        id: 'the edge name',
        type: EntityType.EDGE_BROKER,
      },
      {
        id: 'opcua-1',
        type: EntityType.ADAPTER,
      },
    ]
    const getFormData = (mappings: DataCombining[]): Combiner => ({
      id: mockCombinerId,
      name: 'my-combiner',
      sources: {
        items: sources,
      },
      mappings: {
        items: mappings,
      },
    })

    it('should not validate when there is no schema', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['opcua-1/log/event'],
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: { topic: undefined, schema: undefined },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'Your topic is currently not assigned a schema',
        }),
      ])
    })

    it('should not validate when there is no properties in the schema', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['opcua-1/log/event'],
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_EMPTY_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: "Not a valid JSONSchema: `properties` doesn't contain any properties",
        }),
      ])
    })

    it('should  validate when there is a valid schema', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              tags: ['opcua-1/log/event'],
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([])
    })
  })

  describe('validateInstructions', () => {
    const sources: EntityReference[] = [
      {
        id: 'the edge name',
        type: EntityType.EDGE_BROKER,
      },
      {
        id: 'opcua-1',
        type: EntityType.ADAPTER,
      },
    ]
    const getFormData = (mappings: DataCombining[]): Combiner => ({
      id: mockCombinerId,
      name: 'my-combiner',
      sources: {
        items: sources,
      },
      mappings: {
        items: mappings,
      },
    })

    it.skip('should not validate when there is no instruction', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'At least one mapping instruction must be defined',
        }),
      ])
    })

    it('should not validate when the destination path is incorrect', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [
              {
                source: 'description',
                destination: 'wrong.destination',
                sourceRef: {
                  id: 'a/topic/+/filter',
                  type: DataIdentifierReference.type.TOPIC_FILTER,
                },
              },
            ],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The instruction is not a recognised source property',
        }),
      ])
    })

    it('should not validate when the source path is incorrect', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [
              {
                source: 'wrong.source',
                destination: 'value',
                sourceRef: {
                  id: 'a/topic/+/filter',
                  type: DataIdentifierReference.type.TOPIC_FILTER,
                },
              },
            ],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([
        expect.objectContaining({
          message: 'The instruction is not a recognised destination property',
        }),
      ])
    })

    it('should validate when the instructions are correct', async () => {
      const result = await loadingEntities(sources)

      const errors = await renderValidateHook(
        getFormData([
          {
            id: uuidv4(),
            sources: {
              topicFilters: ['a/topic/+/filter'],
              // @ts-ignore TODO[NVL] Needs to be nullable
              primary: {},
            },
            destination: {
              topic: 'test/ss',
              schema: MOCK_SIMPLE_SCHEMA_URI,
            },
            instructions: [
              {
                source: 'description',
                destination: 'value',
                sourceRef: {
                  id: 'a/topic/+/filter',
                  type: DataIdentifierReference.type.TOPIC_FILTER,
                },
              },
            ],
          },
        ]),
        result.current,
        sources
      )
      expect(errors).toStrictEqual([])
    })
  })
})
