import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers, MOCK_DEVICE_TAGS } from './__handlers__'
import { useUpdateAllDomainTags } from '@/api/hooks/useProtocolAdapters/useUpdateAllDomainTags.ts'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'

describe('useUpdateAllDomainTags', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...deviceHandlers)

    const { result } = renderHook(useUpdateAllDomainTags, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({
        adapterId: 'my-adapter',
        requestBody: { items: MOCK_DEVICE_TAGS('my-adapter', MockAdapterType.MODBUS) },
      })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
      expect(result.current.data).toStrictEqual({})
    })
  })
})
