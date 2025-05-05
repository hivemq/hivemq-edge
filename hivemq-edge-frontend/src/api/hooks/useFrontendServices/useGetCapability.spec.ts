import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { Capability } from '@/api/__generated__'

describe('useGetCapability', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load a capability', async () => {
    const { result } = renderHook(() => useGetCapability(Capability.id.DATA_HUB), { wrapper })

    await waitFor(() => {
      expect(result.current).toBeTruthy()
    })
  })
})
