import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useListClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useListClientSubscriptions.ts'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { handlers } from './__handlers__'

describe('useListProtocolAdapters', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)
    const { result } = renderHook(() => useListClientSubscriptions(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual([
      {
        id: 'my-first-client',
        topicFilters: [
          {
            destination: 'test/topic/1',
          },
        ],
      },
    ])
  })
})
