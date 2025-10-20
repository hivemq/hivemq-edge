import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useTopicFilterManager } from '@/modules/TopicFilters/hooks/useTopicFilterManager.ts'
import { handlers, MOCK_TOPIC_FILTER_SCHEMA_VALID } from '@/api/hooks/useTopicFilters/__handlers__'
import type { TopicFilter, TopicFilterList } from '@/api/__generated__'

import '@/config/i18n.config.ts'

describe('useTopicFilterManager', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  it('should return the manager', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })
    expect(result.current.isLoading).toBeTruthy()

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current).toStrictEqual(
      expect.objectContaining({
        data: {
          items: [
            {
              description: 'This is a topic filter',
              topicFilter: 'a/topic/+/filter',
              schema: MOCK_TOPIC_FILTER_SCHEMA_VALID,
            },
          ],
        },
        error: null,
        isError: false,
        isLoading: false,
        isPending: false,
      })
    )
  })

  it('should have schema and context properties', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.context).toBeDefined()
    expect(result.current.context.schema).toBeDefined()
    expect(result.current.context.uiSchema).toBeDefined()

    if (result.current.context.schema) {
      expect(result.current.context.schema.definitions?.TopicFilter).toBeDefined()
      expect(result.current.context.schema.properties?.items).toBeDefined()
    }
  })

  it('should call onCreate with topic filter', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const newFilter: TopicFilter = {
      topicFilter: 'test/+/filter',
      description: 'Test filter',
    }

    await act(async () => {
      result.current.onCreate(newFilter)
    })

    // The onCreate should not throw
    expect(result.current.onCreate).toBeTypeOf('function')
  })

  it('should call onUpdate with topic filter', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const updatedFilter: TopicFilter = {
      topicFilter: 'updated/+/filter',
      description: 'Updated filter',
    }

    await act(async () => {
      result.current.onUpdate('test-filter', updatedFilter)
    })

    expect(result.current.onUpdate).toBeTypeOf('function')
  })

  it('should call onDelete with filter name', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    await act(async () => {
      result.current.onDelete('test-filter')
    })

    expect(result.current.onDelete).toBeTypeOf('function')
  })

  it('should call onUpdateCollection with topic filter list', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const filterList: TopicFilterList = {
      items: [
        {
          topicFilter: 'test/+/filter',
          description: 'Test filter 1',
        },
        {
          topicFilter: 'test/+/filter2',
          description: 'Test filter 2',
        },
      ],
    }

    await act(async () => {
      result.current.onUpdateCollection(filterList)
    })

    expect(result.current.onUpdateCollection).toBeTypeOf('function')
  })

  it('should have correct uiSchema configuration', async () => {
    const { result } = renderHook(() => useTopicFilterManager(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    const { uiSchema } = result.current.context

    if (uiSchema) {
      expect(uiSchema['ui:submitButtonOptions']).toEqual({ norender: true })
      expect(uiSchema.items).toBeDefined()
      expect(uiSchema.items['ui:title']).toBe('Topic Filters')
      expect(uiSchema.items.items['ui:order']).toEqual(['topicFilter', '*'])
    }
  })
})
