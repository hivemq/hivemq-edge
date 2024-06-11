import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'

import { handlers } from './__handlers__'
import { useDeleteProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useDeleteProtocolAdapter.ts'

describe('useDeleteProtocolAdapter', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useDeleteProtocolAdapter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync(MOCK_ADAPTER_ID)
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({})
  })
})
