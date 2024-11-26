import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'

describe('useGetCapability', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should not load an non-existent capability', async () => {
    const { result } = renderHook(() => useGetCapability('wrong-capability'), { wrapper })

    await waitFor(() => {
      expect(result.current).toBeFalsy()
    })
  })

  it('should load a capability', async () => {
    const { result } = renderHook(() => useGetCapability('data-hub'), { wrapper })

    await waitFor(() => {
      expect(result.current).toBeTruthy()
    })
  })
})
