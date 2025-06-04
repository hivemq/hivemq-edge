import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'

import { handlers } from './__handlers__'
import { type JsonNode } from '@/api/__generated__'

describe('useSamplingForTopic', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const mockTopic = 'topic/test'
    const { result } = renderHook(() => useSamplingForTopic(mockTopic), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<JsonNode>(
      expect.objectContaining({
        title: encodeURIComponent(mockTopic),
        type: 'object',
      })
    )

    // there is no samples so examples are the same as original
    expect(result.current.schema).toStrictEqual(result.current.data)

    // // TODO[NVL] Not the way to test the refetch - find better
    // expect(result.current.isLoading).toBeFalsy()
    // act(() => {
    //   result.current.refetch()
    // })
  })
})
