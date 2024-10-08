import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import useGetAdapterInfo from '@/modules/ProtocolAdapters/hooks/useGetAdapterInfo.ts'
import { handlers } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('useGetAdapterInfo', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should do it', async () => {
    const { result } = renderHook(() => useGetAdapterInfo('my-adapter'), { wrapper })

    expect(result.current.isLoading).toBeTruthy()
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })
    expect(result.current.isDiscoverable).toBeTruthy()
    expect(result.current.adapter).toStrictEqual(
      expect.objectContaining({
        id: 'my-adapter',
        status: {
          connection: 'CONNECTED',
          startedAt: '2023-08-21T11:51:24.234+01',
        },
        type: 'simulation',
      })
    )
    expect(result.current.protocol).toStrictEqual(
      expect.objectContaining({
        author: 'HiveMQ',
        description:
          'Without needing to configure real devices, simulate traffic from an edge device into HiveMQ Edge.',
        id: 'simulation',
        installed: true,
        logoUrl: 'http://localhost:8080/images/hivemq-icon.png',
        name: 'Simulated Edge Device',
        protocol: 'Simulation',
      })
    )
  })
})
