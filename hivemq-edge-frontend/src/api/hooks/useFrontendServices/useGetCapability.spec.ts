import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlerCapabilities, handlers, MOCK_CAPABILITIES } from './__handlers__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { Capability } from '@/api/__generated__'

describe('useGetCapability', () => {
  beforeEach(() => {
    server.use(...handlers, ...handlerCapabilities(MOCK_CAPABILITIES))
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load a capability', async () => {
    const { result } = renderHook(() => useGetCapability(Capability.id.DATA_HUB), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data?.id).toStrictEqual(Capability.id.DATA_HUB)
  })

  it("should fail when capability doesn't exist", async () => {
    const { result } = renderHook(() => useGetCapability(Capability.id.CONTROL_PLANE_CONNECTIVITY), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toBeUndefined()
  })
})
