import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useStartSamplingForTopic } from '@/api/hooks/useDomainModel/useStartSamplingForTopic.ts'

import { handlers } from './__handlers__'

describe('useStartSamplingForTopic', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useStartSamplingForTopic(), { wrapper })

    act(() => {
      result.current.mutate('test/data')
    })
    await waitFor(() => {
      expect(result.current.isPending).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      topic: 'dGVzdC9kYXRh',
    })
  })
})
