import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockAdapter } from './__handlers__'
import { useUpdateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useUpdateProtocolAdapter.ts'

describe('useUpdateProtocolAdapter', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useUpdateProtocolAdapter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({ adapterId: 'test', requestBody: mockAdapter })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
