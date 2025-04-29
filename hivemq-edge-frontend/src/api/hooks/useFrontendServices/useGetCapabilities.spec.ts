import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetCapabilities } from '@/api/hooks/useFrontendServices/useGetCapabilities.ts'

describe('useGetCapabilities', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetCapabilities, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        displayName: 'Persistent Data for MQTT traffic',
        id: 'mqtt-persistence',
      }),

      expect.objectContaining({
        displayName: 'Data Hub for HiveMQ Edge',
        id: 'data-hub',
      }),
    ])
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetCapabilities, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.items).toStrictEqual([
      expect.objectContaining({
        displayName: 'Persistent Data for MQTT traffic',
        id: 'mqtt-persistence',
      }),

      expect.objectContaining({
        displayName: 'Data Hub for HiveMQ Edge',
        id: 'data-hub',
      }),
    ])
  })
})
