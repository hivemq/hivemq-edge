import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { handlers, MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { useUpdateAllTopicFilter } from '@/api/hooks/useTopicFilters/useUpdateAllTopicFilters.ts'

describe('useUpdateAllTopicFilter', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useUpdateAllTopicFilter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({
        requestBody: { items: [MOCK_TOPIC_FILTER] },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
