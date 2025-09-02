import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { useSelectCombinerFromMapping } from '@/api/hooks/useCombiners/useSelectCombinerFromMapping.ts'

import { handlerCombinerAssets, handlers } from './__handlers__'

describe('useSelectCombinerFromMapping', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should not be enabled without mappingId', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useSelectCombinerFromMapping(undefined), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
    })

    expect(result.current.isEnabled).toBeFalsy()
    expect(result.current.data).toBeUndefined()
  })

  it('should throw error if no combiner found', async () => {
    server.use(...handlers)

    const { result } = renderHook(() => useSelectCombinerFromMapping('ff02efff-7b4c-4f8c-8bf6-74d0756283fb'), {
      wrapper,
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeFalsy()
    })

    expect(result.current.isEnabled).toBeTruthy()
    expect(result.current.isError).toBeTruthy()
    expect(result.current.data).toBeUndefined()
    expect(result.current.error?.message).toStrictEqual('No matching Combiner found')
  })

  it('should return the matching combiner', async () => {
    server.use(...handlerCombinerAssets)

    const { result } = renderHook(() => useSelectCombinerFromMapping('ff02efff-7b4c-4f8c-8bf6-74d0756283fb'), {
      wrapper,
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.isError).toBeFalsy()
    expect(result.current.data).toStrictEqual(
      expect.objectContaining({
        id: 'e9af7f82-bec1-4d07-8c0f-e4591148af19',
        name: 'my-combiner-for-asset',
      })
    )
  })
})
