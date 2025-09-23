import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { PulseStatus } from '@/api/__generated__'
import { handlers as pulseAssetsHandlers } from '@/api/hooks/usePulse/__handlers__'
import { handlerCapabilities, MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { useGetPulseStatus } from '@/api/hooks/usePulse/useGetPulseStatus.ts'

describe('useGetPulseStatus', () => {
  beforeEach(() => {
    server.use(...pulseAssetsHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data ', async () => {
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))
    const { result } = renderHook(() => useGetPulseStatus(), { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isEnabled).toBeTruthy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual<PulseStatus>({
      activation: PulseStatus.activation.ACTIVATED,
      runtime: PulseStatus.runtime.CONNECTED,
    })
  })

  it('should not load data when disabled', async () => {
    server.use(...handlerCapabilities({ items: [] }))
    const { result } = renderHook(() => useGetPulseStatus(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.isEnabled).toBeFalsy()

    expect(result.current.data).toBeUndefined()
  })
})
