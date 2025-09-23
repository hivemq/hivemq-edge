import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, MOCK_ASSET_MAPPER } from './__handlers__'

import { useUpdateAssetMapper } from './useUpdateAssetMapper'

describe('useUpdateAssetMapper', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useUpdateAssetMapper(), { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({
        combinerId: 'my-id',
        requestBody: MOCK_ASSET_MAPPER,
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({
        id: 'my-id',
        updated: MOCK_ASSET_MAPPER,
      })
    })
  })
})
