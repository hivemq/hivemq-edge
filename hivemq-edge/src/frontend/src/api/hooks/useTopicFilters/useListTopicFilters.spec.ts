import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { TopicFilterList } from '@/api/__generated__'
import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import { handlers } from '@/api/hooks/useTopicFilters/__handlers__'

describe('useListTopicFilters', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data ', async () => {
    const { result } = renderHook(() => useListTopicFilters(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<TopicFilterList>({
      items: [
        {
          description: 'This is a topic filter',
          topicFilter: 'a/topic/+/filter',
        },
      ],
    })
  })
})
