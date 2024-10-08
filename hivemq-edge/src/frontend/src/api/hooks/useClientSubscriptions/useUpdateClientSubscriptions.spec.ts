import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useUpdateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useUpdateClientSubscriptions.ts'
import { handlers, mockClientSubscription } from './__handlers__'

describe('useUpdateClientSubscriptions', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useUpdateClientSubscriptions(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync(mockClientSubscription)
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({})
  })
})
