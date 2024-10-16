import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { server } from '@/__test-utils__/msw/mockServer.ts'
import { useGetClientTopicSamples } from '@/api/hooks/useClientSubscriptions/useGetClientTopicSamples.ts'
import type { ClientTopicList } from '@/api/__generated__'
import { handlers } from './__handlers__'

describe('useGetClientTopicSamples', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)
    const { result } = renderHook(() => useGetClientTopicSamples(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    await waitFor(() => {
      result.current.refetch()
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<ClientTopicList>({
      items: [],
    })
  })
})
