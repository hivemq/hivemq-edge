import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useCreateTopicFilter } from '@/api/hooks/useTopicFilters/useCreateTopicFilter.ts'
import { handlers, MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

describe('useCreateTopicFilter', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useCreateTopicFilter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()

    act(() => {
      result.current.mutateAsync({ requestBody: MOCK_TOPIC_FILTER })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
