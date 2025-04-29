import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetListeners } from '@/api/hooks/useGateway/useGetListeners.ts'

describe('useGetListeners', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetListeners, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        description: 'MQTT TCP Listener',
        hostName: '127.0.0.1',
        name: 'tcp-listener-1883',
        port: 1883,
        protocol: 'mqtt',
        transport: 'TCP',
      }),
    ])
  })
})
