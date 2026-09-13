import type { FC, PropsWithChildren } from 'react'
import { describe, expect, it } from 'vitest'
import { renderHook } from '@testing-library/react'
import { MemoryRouter } from 'react-router'

import { useIsPanelOpen } from './useReloadWorkspace.ts'

/**
 * Only the route-derived panel detection is unit-tested here; it is pure routing and needs nothing
 * else. The reload itself needs an authenticated API client, so it is covered end-to-end in
 * `ReloadWorkspaceButton.spec.cy.tsx` against intercepted endpoints — including the decisive case
 * where a list fails and the workspace must be left untouched.
 */
const routerAt = (initialPath: string): FC<PropsWithChildren> => {
  const Wrapper: FC<PropsWithChildren> = ({ children }) => (
    <MemoryRouter initialEntries={[initialPath]}>{children}</MemoryRouter>
  )
  return Wrapper
}

describe('useIsPanelOpen', () => {
  it('should report no panel on the bare workspace route', () => {
    const { result } = renderHook(() => useIsPanelOpen(), { wrapper: routerAt('/workspace') })

    expect(result.current).toBe(false)
  })

  it('should report no panel on a trailing-slash workspace route', () => {
    const { result } = renderHook(() => useIsPanelOpen(), { wrapper: routerAt('/workspace/') })

    expect(result.current).toBe(false)
  })

  it('should report a panel for an adapter route', () => {
    const { result } = renderHook(() => useIsPanelOpen(), {
      wrapper: routerAt('/workspace/adapter/opcua/my-adapter'),
    })

    expect(result.current).toBe(true)
  })

  it('should report a panel for a combiner route', () => {
    const { result } = renderHook(() => useIsPanelOpen(), { wrapper: routerAt('/workspace/combiner/combiner-1') })

    expect(result.current).toBe(true)
  })

  it('should report a panel for a nested mapping route', () => {
    const { result } = renderHook(() => useIsPanelOpen(), {
      wrapper: routerAt('/workspace/adapter/opcua/my-adapter/northbound'),
    })

    expect(result.current).toBe(true)
  })

  it('should report no panel elsewhere in the app', () => {
    const { result } = renderHook(() => useIsPanelOpen(), { wrapper: routerAt('/protocol-adapters/catalog') })

    expect(result.current).toBe(false)
  })
})
