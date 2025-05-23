import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAllDataPolicies } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetAllDataPolicies.ts'

describe('useGetAllDataPolicies', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetAllDataPolicies(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      items: [
        expect.objectContaining({
          createdAt: '2023-10-13T11:51:24.234Z',
          id: 'my-policy-id',
          matching: {
            topicFilter: 'root/topic/ref/1',
          },
        }),
      ],
    })
  })
})
