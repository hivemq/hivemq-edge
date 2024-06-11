import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockBridgeId } from './__handlers__'
import { useGetBridge } from '@/api/hooks/useGetBridges/useGetBridge.ts'

describe('useGetBridge', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetBridge(mockBridgeId), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        cleanStart: true,
        clientId: 'my-client-id',
        host: 'my.h0st.org',
        id: 'bridge-id-01',
        status: {
          connection: 'CONNECTED',
        },
      })
    )
  })
})
