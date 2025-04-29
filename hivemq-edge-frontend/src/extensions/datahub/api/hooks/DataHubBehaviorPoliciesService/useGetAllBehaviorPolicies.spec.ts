import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAllBehaviorPolicies } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useGetAllBehaviorPolicies.ts'

describe('useGetAllBehaviorPolicies', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetAllBehaviorPolicies({}), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      items: [
        expect.objectContaining({
          behavior: {
            id: 'fgf',
          },
          createdAt: '2023-10-13T11:51:24.234Z',
          id: 'my-behavior-policy-id',
          matching: {
            clientIdRegex: 'client-mock-1',
          },
        }),
      ],
    })
  })
})
