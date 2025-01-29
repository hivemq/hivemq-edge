import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockBehaviorPolicy } from './__handlers__'
import { useCreateBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useCreateBehaviorPolicy.ts'

describe('useCreateBehaviorPolicy', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useCreateBehaviorPolicy, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync(mockBehaviorPolicy)
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({})
  })
})
