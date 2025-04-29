import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useDeleteTopicFilter } from '@/api/hooks/useTopicFilters/useDeleteTopicFilter.ts'
import { handlers } from '@/api/hooks/useTopicFilters/__handlers__'

describe('useDeleteTopicFilter', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useDeleteTopicFilter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({ name: 'test/tag1' })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({ name: encodeURIComponent('test/tag1') })
  })
})
