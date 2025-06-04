import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useGetProtocolAdapter.ts'

describe('useGetProtocolAdapter', () => {
  beforeEach(() => {
    server.use(...handlers)
  })
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(() => useGetProtocolAdapter('test'), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        id: 'test',
        status: {
          connection: 'CONNECTED',
          startedAt: '2023-08-21T11:51:24.234+01',
        },
        type: 'simulation',
        config: expect.objectContaining({
          simulationToMqtt: expect.objectContaining({
            pollingIntervalMillis: 10000,
            simulationToMqttMappings: [
              {
                mqttTopic: 'root/topic/ref/1',
                qos: 0,
              },
              {
                mqttTopic: 'root/topic/ref/2',
                qos: 0,
              },
            ],
          }),
        }),
      })
    )
  })
})
