import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockBehaviorPolicy } from './__handlers__'
import { useUpdateBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useUpdateBehaviorPolicy.ts'

describe('useUpdateBehaviorPolicy', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useUpdateBehaviorPolicy, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({ policyId: mockBehaviorPolicy.id, requestBody: mockBehaviorPolicy })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({})
  })
})
