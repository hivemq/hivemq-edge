import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { type PayloadSampleList } from '@/api/__generated__'
import { useGetSamplesForTopic } from '@/api/hooks/useDomainModel/useGetSamplesForTopic.ts'

import { handlers } from './__handlers__'

describe('useGetSamplesForTopic', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetSamplesForTopic('test/topic1', true), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<PayloadSampleList>({
      items: [],
    })
  })

  it('should not load if not seeded', async () => {
    const { result } = renderHook(() => useGetSamplesForTopic('test/topic1'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isFetched).toBeFalsy()
    })
    expect(result.current.data).toBeUndefined()
    expect(result.current.error).toBeNull()
  })
})
