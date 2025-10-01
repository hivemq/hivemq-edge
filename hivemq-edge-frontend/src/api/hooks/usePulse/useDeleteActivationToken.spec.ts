import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useDeleteActivationToken } from '@/api/hooks/usePulse/useDeleteActivationToken.ts'
import { handlers } from './__handlers__'

describe('useDeleteActivationToken', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should execute the mutation', async () => {
    server.use(...handlers)

    const { result } = renderHook(useDeleteActivationToken, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    await act(async () => {
      await result.current.mutateAsync()
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual('Token deleted')
  })
})
