import { vi, expect, beforeEach } from 'vitest'

import { act, render, renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import {
  handlerCapabilities,
  handlers as frontendHandler,
  MOCK_CAPABILITIES,
  MOCK_CAPABILITY_WRITEABLE_CONFIG,
} from '@/api/hooks/useFrontendServices/__handlers__'
import { useGetCapabilities } from '@/api/hooks/useFrontendServices/useGetCapabilities.ts'
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

    expect(result.current.notifications).toHaveLength(3)
    expect(result.current.readNotifications).toHaveLength(0)
  })

  it('should explain why the config cannot be updated', async () => {
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))

    const { result } = renderHook(
      () => ({ managed: useGetManagedNotifications(), capabilities: useGetCapabilities() }),
      { wrapper }
    )

    await waitFor(() => {
      expect(result.current.managed.isSuccess).toBeTruthy()
      expect(result.current.capabilities.isSuccess).toBeTruthy()
    })
    expect(result.current.capabilities.data?.items?.map(({ id }) => id)).not.toContain('config-writeable')

    const notification = result.current.managed.notifications.find(({ id }) => id === 'config-writeable')
    expect(notification).toBeDefined()

    const { container: title } = render(notification?.title, { wrapper })
    const { container: description } = render(notification?.description, { wrapper })

    expect(title.textContent?.trim()).toBe('Config cannot be updated in this Edge')
    expect(description.textContent?.trim()).toBe(
      'This Edge was started with config updates turned off, the default with the HiveMQ Edge Helm chart. To change the config, update the Helm values, or the config file this Edge was started with.'
    )
  })

  it('should handle reading a notification', async () => {
    const { result } = renderHook(useGetManagedNotifications, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.notifications).toHaveLength(3)
    expect(result.current.readNotifications).toHaveLength(0)

    // close the first notification
    act(() => {
      result.current.notifications[0].onCloseComplete?.()
    })

    expect(result.current.notifications).toHaveLength(2)
    expect(result.current.readNotifications).toHaveLength(1)
    expect(result.current.readNotifications).toContainEqual('Default Credentials Need Changing!')

    // close the first notification
    act(() => {
      result.current.notifications[0].onCloseComplete?.()
    })

    expect(result.current.notifications).toHaveLength(2)
    expect(result.current.readNotifications).toHaveLength(2)
    expect(result.current.readNotifications).toContainEqual('Default Credentials Need Changing!')
    expect(result.current.readNotifications).toContainEqual('config-writeable')
  })

  it('should handle config-writeable', async () => {
    server.use(...handlerCapabilities({ items: [MOCK_CAPABILITY_WRITEABLE_CONFIG] }))

    const { result } = renderHook(useGetManagedNotifications, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.notifications).toHaveLength(2)

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
