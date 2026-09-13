import type { FC, PropsWithChildren } from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useIsPanelOpen, useReloadWorkspace } from './useReloadWorkspace.ts'

const wrapperFor = (queryClient: QueryClient, initialPath = '/workspace'): FC<PropsWithChildren> => {
  const Wrapper: FC<PropsWithChildren> = ({ children }) => (
    <MemoryRouter initialEntries={[initialPath]}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MemoryRouter>
  )
  return Wrapper
}

/**
 * Seed the cache with the six queries the graph is built from, so a reload has something to refetch.
 * `failing` names the keys whose fetcher should reject.
 */
const seedWorkspaceQueries = async (queryClient: QueryClient, failing: string[] = []) => {
  const keys = [
    QUERY_KEYS.ADAPTERS,
    QUERY_KEYS.PROTOCOLS,
    QUERY_KEYS.BRIDGES,
    QUERY_KEYS.LISTENERS,
    QUERY_KEYS.COMBINER,
    QUERY_KEYS.ASSET_MAPPER,
  ]

  await Promise.all(
    keys.map((key) =>
      queryClient
        .prefetchQuery({
          queryKey: [key],
          queryFn: () => (failing.includes(key) ? Promise.reject(new Error(`${key} is down`)) : Promise.resolve([])),
        })
        .catch(() => undefined)
    )
  )
}

describe('useIsPanelOpen', () => {
  it('should report no panel on the bare workspace route', () => {
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useIsPanelOpen(), { wrapper: wrapperFor(queryClient, '/workspace') })

    expect(result.current).toBe(false)
  })

  it('should report a panel for an adapter route', () => {
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useIsPanelOpen(), {
      wrapper: wrapperFor(queryClient, '/workspace/adapter/opcua/my-adapter'),
    })

    expect(result.current).toBe(true)
  })

  it('should report a panel for a combiner route', () => {
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useIsPanelOpen(), {
      wrapper: wrapperFor(queryClient, '/workspace/combiner/combiner-1'),
    })

    expect(result.current).toBe(true)
  })
})

describe('useReloadWorkspace', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
      // Silence the expected rejections of the failure cases.
      logger: undefined,
    } as ConstructorParameters<typeof QueryClient>[0])
  })

  afterEach(() => {
    vi.useRealTimers()
    queryClient.clear()
  })

  it('should reconcile once every query has been refetched', async () => {
    await seedWorkspaceQueries(queryClient)
    const reconcile = vi.fn()

    const { result } = renderHook(() => useReloadWorkspace(), { wrapper: wrapperFor(queryClient) })

    await result.current.reload(reconcile)

    expect(reconcile).toHaveBeenCalledTimes(1)
    expect(result.current.phase).toBe('IDLE')
  })

  it('should not reconcile anything when one query keeps failing', async () => {
    // The decisive safety property: a partial result must change nothing. Reconciling here would read
    // the failed query as "the server has no combiners" and delete every combiner from the workspace.
    await seedWorkspaceQueries(queryClient, [QUERY_KEYS.COMBINER])
    const reconcile = vi.fn()

    const { result } = renderHook(() => useReloadWorkspace(), { wrapper: wrapperFor(queryClient) })

    await expect(result.current.reload(reconcile)).rejects.toThrow()
    expect(reconcile).not.toHaveBeenCalled()
  })

  it('should return to idle after a failed reload, so status polling resumes', async () => {
    await seedWorkspaceQueries(queryClient, [QUERY_KEYS.BRIDGES])

    const { result } = renderHook(() => useReloadWorkspace(), { wrapper: wrapperFor(queryClient) })

    await expect(result.current.reload(vi.fn())).rejects.toThrow()

    await waitFor(() => {
      expect(result.current.phase).toBe('IDLE')
      expect(result.current.isReloading).toBe(false)
    })
  })

  it('should retry a flaky query and succeed', async () => {
    // Fails once, then recovers — the reload should ride it out rather than give up.
    let attempts = 0
    await queryClient
      .prefetchQuery({
        queryKey: [QUERY_KEYS.ADAPTERS],
        queryFn: () => {
          attempts += 1
          return attempts <= 1 ? Promise.reject(new Error('flaky')) : Promise.resolve([])
        },
      })
      .catch(() => undefined)

    await seedWorkspaceQueries(queryClient)
    const reconcile = vi.fn()

    const { result } = renderHook(() => useReloadWorkspace(), { wrapper: wrapperFor(queryClient) })
    await result.current.reload(reconcile)

    expect(reconcile).toHaveBeenCalledTimes(1)
  })
})
