import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockBridgeId } from './__handlers__'
import { useGetBridgeConnectionStatus } from '@/api/hooks/useGetBridges/useGetBridgeConnectionStatus.ts'

describe('useGetBridgeConnectionStatus', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetBridgeConnectionStatus(mockBridgeId), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        items: [
          {
            connection: 'CONNECTED',
            id: 'first-bridge',
            type: 'bridge',
          },
        ],
      })
    )
  })
})
