import { vi, expect, beforeEach } from 'vitest'
import { delay, http, HttpResponse } from 'msw'

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

// The hook beside the capabilities query, so a test can wait until the read-only state is known.
const renderManagedNotifications = async () => {
  const hook = renderHook(() => ({ managed: useGetManagedNotifications(), capabilities: useGetCapabilities() }), {
    wrapper,
  })
  await waitFor(() => {
    expect(hook.result.current.managed.isSuccess).toBeTruthy()
    expect(hook.result.current.capabilities.isSuccess).toBeTruthy()
  })
  return hook
}

describe('useGetManagedNotifications', () => {
  beforeEach(() => {
    server.use(...frontendHandler, ...gitHubHandler)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should show a list of unread notifications', async () => {
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))
    const { result } = await renderManagedNotifications()

    expect(result.current.managed.notifications).toHaveLength(3)
    expect(result.current.managed.readNotifications).toHaveLength(0)
  })

  it('should explain why the config cannot be updated', async () => {
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))
    const { result } = await renderManagedNotifications()

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
    server.use(...handlerCapabilities(MOCK_CAPABILITIES))
    const { result } = await renderManagedNotifications()

    expect(result.current.managed.notifications).toHaveLength(3)
    expect(result.current.managed.readNotifications).toHaveLength(0)

    // close the first notification
    act(() => {
      result.current.managed.notifications[0].onCloseComplete?.()
    })

    expect(result.current.managed.notifications).toHaveLength(2)
    expect(result.current.managed.readNotifications).toHaveLength(1)
    expect(result.current.managed.readNotifications).toContainEqual('Default Credentials Need Changing!')

    // close the first notification
    act(() => {
      result.current.managed.notifications[0].onCloseComplete?.()
    })

    expect(result.current.managed.notifications).toHaveLength(2)
    expect(result.current.managed.readNotifications).toHaveLength(2)
    expect(result.current.managed.readNotifications).toContainEqual('Default Credentials Need Changing!')
    expect(result.current.managed.readNotifications).toContainEqual('config-writeable')
  })

  it('should handle config-writeable', async () => {
    server.use(...handlerCapabilities({ items: [MOCK_CAPABILITY_WRITEABLE_CONFIG] }))

    const { result } = await renderManagedNotifications()

    expect(result.current.managed.notifications).toHaveLength(2)

    // close the first notification
    act(() => {
      result.current.managed.notifications[0].onCloseComplete?.()
    })

    expect(result.current.managed.notifications).toHaveLength(1)
    expect(result.current.managed.readNotifications).toHaveLength(1)
    expect(result.current.managed.readNotifications).toContainEqual('Default Credentials Need Changing!')

    // close the first notification
    act(() => {
      result.current.managed.notifications[0].onCloseComplete?.()
    })

    expect(result.current.managed.notifications).toHaveLength(0)
    expect(result.current.managed.readNotifications).toHaveLength(2)
    expect(result.current.managed.readNotifications).toContainEqual('Default Credentials Need Changing!')
    expect(result.current.managed.readNotifications).toContainEqual('2023.XXX')
  })

  it('should not warn while the capabilities are loading', () => {
    server.use(
      http.get('**/frontend/capabilities', async () => {
        await delay('infinite')
        return HttpResponse.json({ items: [] })
      })
    )
    const { result } = renderHook(useGetManagedNotifications, { wrapper })

    expect(result.current.notifications.map(({ id }) => id)).not.toContain('config-writeable')
  })

  it('should not warn when the capabilities cannot be loaded', async () => {
    server.use(http.get('**/frontend/capabilities', () => HttpResponse.json({}, { status: 500 })))
    const { result } = renderHook(
      () => ({ managed: useGetManagedNotifications(), capabilities: useGetCapabilities() }),
      { wrapper }
    )

    await waitFor(() => {
      expect(result.current.managed.isSuccess).toBeTruthy()
      expect(result.current.capabilities.isError).toBeTruthy()
    })
    expect(result.current.managed.notifications.map(({ id }) => id)).not.toContain('config-writeable')
  })
})
