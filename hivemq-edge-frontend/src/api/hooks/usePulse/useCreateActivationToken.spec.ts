import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, MOCK_PULSE_ACTIVATION_TOKEN } from './__handlers__'

import { useCreateActivationToken } from '@/api/hooks/usePulse/useCreateActivationToken.ts'

describe('useCreateActivationToken', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useCreateActivationToken(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync(MOCK_PULSE_ACTIVATION_TOKEN)
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        created: MOCK_PULSE_ACTIVATION_TOKEN.token,
      })
    })
  })
})
