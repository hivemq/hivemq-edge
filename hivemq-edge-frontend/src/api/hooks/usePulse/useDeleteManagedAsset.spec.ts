import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useDeleteManagedAsset } from '@/api/hooks/usePulse/useDeleteManagedAsset.ts'
import { handlers as pulseAssetsHandlers, MOCK_PULSE_ASSET_MAPPED } from '@/api/hooks/usePulse/__handlers__'

describe('useDeleteManagedAsset', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should execute the mutation', async () => {
    server.use(...pulseAssetsHandlers)

    const { result } = renderHook(useDeleteManagedAsset, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    await act(async () => {
      await result.current.mutateAsync(MOCK_PULSE_ASSET_MAPPED.id)
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      deleted: '3b028f58-f949-4de1-9b8b-c1a35b1643a5',
    })
  })
})
