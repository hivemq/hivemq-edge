import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useCreateManagedAsset } from '@/api/hooks/usePulse/useCreateManagedAsset.ts'
import { handlers as pulseAssetsHandlers, MOCK_PULSE_ASSET_MAPPED } from '@/api/hooks/usePulse/__handlers__'

describe('useCreateManagedAsset', () => {
  beforeEach(() => {
    server.use(...pulseAssetsHandlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useCreateManagedAsset, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    await act(async () => {
      await result.current.mutateAsync({ requestBody: MOCK_PULSE_ASSET_MAPPED })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual({
      created: '3b028f58-f949-4de1-9b8b-c1a35b1643a5',
    })
  })
})
