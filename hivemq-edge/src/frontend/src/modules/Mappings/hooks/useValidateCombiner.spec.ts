import { beforeEach, expect } from 'vitest'
import { v4 as uuidv4 } from 'uuid'
import { http, HttpResponse } from 'msw'
import { renderHook, waitFor } from '@testing-library/react'
import { createErrorHandler, toErrorList } from '@rjsf/utils'
import type { UseQueryResult } from '@tanstack/react-query'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type {
  AdaptersList,
  Combiner,
  DataCombining,
  DomainTagList,
  EntityReference,
  ProtocolAdaptersList,
  TopicFilterList,
} from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import {
  deviceHandlers,
  handlers as failCapabilityHandlers,
  mockAdapter_OPCUA,
  mockProtocolAdapter_OPCUA,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockCombinerId, mockEmptyCombiner } from '@/api/hooks/useCombiners/__handlers__'
import { handlers as topicFilterHandlers } from '@/api/hooks/useTopicFilters/__handlers__'
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
      server.use(...topicFilterHandlers, ...deviceHandlers)
      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
      server.use(...topicFilterHandlers, ...deviceHandlers)
      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
      server.use(...topicFilterHandlers, ...deviceHandlers, ...mappingHandlers)

      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
      server.use(...topicFilterHandlers, ...deviceHandlers, ...mappingHandlers)

      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
          message: 'Your topic filter is currently not assigned a schema',
        }),
      ])
    })

    it('should not validate when there is no properties in the schema', async () => {
      server.use(...topicFilterHandlers, ...deviceHandlers, ...mappingHandlers)

      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
          message: "The destination schema doesn't have any property to be mapped into",
        }),
      ])
    })

    it('should  validate when there is a valid schema', async () => {
      server.use(...topicFilterHandlers, ...deviceHandlers, ...mappingHandlers)

      const { result } = renderHook(() => useGetCombinedEntities(sources), { wrapper })

      expect(result.current).toHaveLength(2)
      await waitFor(() => {
        expect(result.current[0].isSuccess).toBeTruthy()
        expect(result.current[1].isSuccess).toBeTruthy()
      })

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
})
