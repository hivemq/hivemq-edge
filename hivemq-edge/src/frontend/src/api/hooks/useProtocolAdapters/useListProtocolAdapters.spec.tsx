import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

describe('useListProtocolAdapters', () => {
  beforeEach(() => {
    server.use(...handlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useListProtocolAdapters(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual([
      expect.objectContaining({
        id: 'my-adapter',
        status: {
          connection: 'CONNECTED',
          startedAt: '2023-08-21T11:51:24.234+01',
        },
        type: 'simulation',
      }),
    ])
  })
})
