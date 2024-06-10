import { vi, expect, beforeEach } from 'vitest'

import { act, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers as frontendHandler } from '@/api/hooks/useFrontendServices/__handlers__'
import { handlers as gitHubHandler } from '@/api/hooks/useGitHub/__handlers__'

import { useGetManagedNotifications } from './useGetManagedNotifications.tsx'

import '@/config/i18n.config.ts'

describe('useGetManagedNotifications', () => {
  beforeEach(() => {
    server.use(...frontendHandler, ...gitHubHandler)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should show a list of unread notifications', async () => {
    const { result } = renderHook(useGetManagedNotifications, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.notifications).toHaveLength(2)
    expect(result.current.readNotifications).toHaveLength(0)
  })

  it('should handle reading a notification', async () => {
    const { result } = renderHook(useGetManagedNotifications, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.notifications).toHaveLength(2)
    expect(result.current.readNotifications).toHaveLength(0)

    // close the first notification
    act(() => {
      result.current.notifications[0].onCloseComplete?.()
    })

    expect(result.current.notifications).toHaveLength(1)
    expect(result.current.readNotifications).toHaveLength(1)
    expect(result.current.readNotifications).toContainEqual('Default Credentials Need Changing!')

    // close the first notification
    act(() => {
      result.current.notifications[0].onCloseComplete?.()
    })

    expect(result.current.notifications).toHaveLength(0)
    expect(result.current.readNotifications).toHaveLength(2)
    expect(result.current.readNotifications).toContainEqual('Default Credentials Need Changing!')
    expect(result.current.readNotifications).toContainEqual('2023.XXX')
  })
})
