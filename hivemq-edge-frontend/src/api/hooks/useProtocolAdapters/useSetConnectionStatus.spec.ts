import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useSetConnectionStatus } from '@/api/hooks/useProtocolAdapters/useSetConnectionStatus.ts'
import { StatusTransitionCommand } from '@/api/__generated__'
import { handlers } from './__handlers__'

describe('useSetConnectionStatus', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useSetConnectionStatus(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutate({
        adapterId: 'my-adapter-id',
        requestBody: { command: StatusTransitionCommand.command.START },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        callbackTimeoutMillis: 2000,
        identifier: 'my-adapter-id',
        status: 'PENDING',
        type: 'adapter',
      })
    })
  })
})
