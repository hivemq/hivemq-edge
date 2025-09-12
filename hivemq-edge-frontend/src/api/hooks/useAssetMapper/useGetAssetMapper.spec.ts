import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, MOCK_ASSET_MAPPER } from './__handlers__'

import type { Combiner } from '@/api/__generated__'
import { useGetAssetMapper } from './useGetAssetMapper'

describe('useGetAssetMapper', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useGetAssetMapper('combinerId'), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<Combiner>({ ...MOCK_ASSET_MAPPER, id: 'combinerId' })
  })
})
