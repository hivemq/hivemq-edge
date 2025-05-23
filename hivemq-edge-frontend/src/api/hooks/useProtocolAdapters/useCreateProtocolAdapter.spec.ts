import { beforeEach, expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers, mockAdapter } from './__handlers__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter.ts'

describe('useCreateProtocolAdapter', () => {
  beforeEach(() => {
    server.use(...handlers)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    const { result } = renderHook(useCreateProtocolAdapter, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()

    act(() => {
      result.current.mutateAsync({ adapterType: 'sdss', requestBody: mockAdapter })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
