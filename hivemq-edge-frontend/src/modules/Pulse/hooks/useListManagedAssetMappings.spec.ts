import { handlerCapabilities, MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { http, HttpResponse } from 'msw'
import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { ApiError } from '@/api/__generated__'
import { handlers as combinerHandlers, handlerCombinerAssets } from '@/api/hooks/useCombiners/__handlers__'
import { handlers as pulseHandlers } from '@/api/hooks/usePulse/__handlers__'

import type { ManagedAssetExtended } from '@/modules/Pulse/types.ts'
import { useCombinedAssetsAndCombiners } from '@/modules/Pulse/hooks/useListManagedAssetMappings.ts'

describe('useCombinedAssetsAndCombiners', () => {
  beforeEach(() => {
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should handle errors', async () => {
    server.use(
      http.get('*/management/combiners', () => {
        return HttpResponse.json({ error: 'combiners' }, { status: 500 })
      }),
      http.get('**/management/pulse/managed-assets', () => {
        return HttpResponse.json({ error: 'managed-assets' }, { status: 500 })
      })
    )

    const { result } = renderHook(() => useCombinedAssetsAndCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isError).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<ManagedAssetExtended[] | undefined>(undefined)
    const { status, statusText, body } = result.current.error as ApiError
    expect(status).toStrictEqual(500)
    expect(statusText).toStrictEqual('Internal Server Error')
    expect(body).toStrictEqual({ error: 'managed-assets' })
  })

  it('should handle errors', async () => {
    server.use(
      http.get('*/management/combiners', () => {
        return HttpResponse.json({ error: 'combiners' }, { status: 500 })
      }),
      ...pulseHandlers
    )

    const { result } = renderHook(() => useCombinedAssetsAndCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isError).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<ManagedAssetExtended[] | undefined>(undefined)
    const { status, statusText, body } = result.current.error as ApiError
    expect(status).toStrictEqual(500)
    expect(statusText).toStrictEqual('Internal Server Error')
    expect(body).toStrictEqual({ error: 'combiners' })
  })

  it('should handle errors', async () => {
    server.use(
      ...combinerHandlers,
      http.get('**/management/pulse/managed-assets', () => {
        return HttpResponse.json({ error: 'managed-assets' }, { status: 500 })
      })
    )

    const { result } = renderHook(() => useCombinedAssetsAndCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isError).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<ManagedAssetExtended[] | undefined>(undefined)
    const { status, statusText, body } = result.current.error as ApiError
    expect(status).toStrictEqual(500)
    expect(statusText).toStrictEqual('Internal Server Error')
    expect(body).toStrictEqual({ error: 'managed-assets' })
  })

  it('should load the data', async () => {
    server.use(...handlerCombinerAssets, ...pulseHandlers)

    const { result } = renderHook(() => useCombinedAssetsAndCombiners(), { wrapper })

    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data).toStrictEqual<ManagedAssetExtended[]>(
      expect.arrayContaining([
        {
          asset: expect.objectContaining({
            id: '3b028f58-f949-4de1-9b8b-c1a35b1643a4',
            name: 'Test asset',
          }),
          mapping: undefined,
        },
        {
          asset: expect.objectContaining({
            id: '3b028f58-f949-4de1-9b8b-c1a35b1643a5',
            name: 'Test mapped asset',
            mapping: {
              mappingId: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
              status: 'STREAMING',
            },
          }),
          mapping: expect.objectContaining({
            id: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
            destination: {
              topic: 'my/first/topic',
            },
          }),
        },
        {
          asset: expect.objectContaining({
            id: '3b028f58-f949-4de1-9b8b-c1a35b1643a9',
            name: 'Test other asset',
            mapping: {
              mappingId: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
              status: 'REQUIRES_REMAPPING',
            },
          }),
          mapping: expect.objectContaining({
            id: 'ff02efff-7b4c-4f8c-8bf6-74d0756283fb',
            destination: {
              topic: 'my/first/topic',
            },
          }),
        },
        {
          asset: expect.objectContaining({
            id: '3b028f58-f949-4de1-9b8b-c1a35b1643a7',
            name: 'Almost the same asset',
          }),
          mapping: undefined,
        },
      ])
    )
  })
})
