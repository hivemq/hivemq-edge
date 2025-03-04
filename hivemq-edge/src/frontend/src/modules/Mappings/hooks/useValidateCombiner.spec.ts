import { beforeEach, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { mappingHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { useValidateCombiner } from './useValidateCombiner'

describe('useValidateCombiner', () => {
  beforeEach(() => {
    server.use(...mappingHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should do it', async () => {
    const { result } = renderHook(() => useValidateCombiner([], []), { wrapper })
    await waitFor(() => {
      expect(result.current).not.toBeUndefined()
    })
  })
})
