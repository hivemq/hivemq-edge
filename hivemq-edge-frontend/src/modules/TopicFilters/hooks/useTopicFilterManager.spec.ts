import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useTopicFilterManager } from '@/modules/TopicFilters/hooks/useTopicFilterManager.ts'
import { handlers, MOCK_TOPIC_FILTER_SCHEMA_VALID } from '@/api/hooks/useTopicFilters/__handlers__'

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
})
