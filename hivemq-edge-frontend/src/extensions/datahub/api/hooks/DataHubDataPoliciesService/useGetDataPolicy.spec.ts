import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockDataPolicy } from './__handlers__'
import { useGetDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetDataPolicy.ts'

describe('useGetDataPolicy', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetDataPolicy(mockDataPolicy.id), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        createdAt: '2023-10-13T11:51:24.234Z',
        id: 'my-policy-id',
        matching: {
          topicFilter: 'root/topic/ref/1',
        },
      })
    )
  })
})
