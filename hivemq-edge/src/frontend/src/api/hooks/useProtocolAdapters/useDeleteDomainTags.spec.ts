import { expect } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { deviceHandlers } from './__handlers__'
import { useDeleteDomainTags } from '@/api/hooks/useProtocolAdapters/useDeleteDomainTags.ts'

describe('useDeleteDomainTags', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...deviceHandlers)

    const { result } = renderHook(useDeleteDomainTags, { wrapper })

    expect(result.current.isSuccess).toBeFalsy()
    act(() => {
      result.current.mutateAsync({ adapterId: 'my-adapter', tagId: encodeURIComponent('test/tag1') })
    })
    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({})
  })
})
