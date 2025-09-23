import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect } from 'vitest'

import '@/config/i18n.config.ts'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import {
  handlerCapabilities,
  handlers as handlerGatewayConfiguration,
  MOCK_CAPABILITIES,
} from '@/api/hooks/useFrontendServices/__handlers__'

import { useOnboarding } from './useOnboarding.tsx'

describe('useOnboarding()', () => {
  beforeEach(() => {
    window.localStorage.clear()
    server.use(...handlerGatewayConfiguration, ...handlerCapabilities(MOCK_CAPABILITIES))
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should return the correct list of tasks', async () => {
    const { result } = renderHook(() => useOnboarding(), { wrapper })

    await waitFor(() => {
      const data = result.current
      expect(data?.[2].isLoading).toEqual(false)
    })

    const [adapter, bridge, cloud, pulse] = result.current
    expect(adapter.sections).toHaveLength(1)
    expect(bridge.sections).toHaveLength(1)
    expect(adapter.sections).toEqual(expect.arrayContaining([expect.objectContaining({ to: '/protocol-adapters' })]))
    expect(bridge.sections).toEqual(expect.arrayContaining([expect.objectContaining({ to: '/mqtt-bridges' })]))
    expect(cloud.sections).toEqual(
      expect.arrayContaining([expect.objectContaining({ to: 'https://hivemq.com/cloud' })])
    )

    expect(pulse).toStrictEqual(expect.objectContaining({ header: 'Connect to HiveMQ Pulse' }))
  })

  describe('Pulse', () => {
    it('should return the activation task', async () => {
      server.use(...handlerCapabilities({ items: [] }))

      const { result } = renderHook(() => useOnboarding(), { wrapper })

      await waitFor(() => {
        const data = result.current
        expect(data?.[2].isLoading).toEqual(false)
      })

      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [_adapter, _bridge, _cloud, pulse] = result.current

      expect(pulse).toStrictEqual(expect.objectContaining({ header: 'Connect to HiveMQ Pulse' }))
      expect(pulse.sections).toStrictEqual([
        expect.objectContaining({
          label: 'Pulse Activation',
          title: 'To access the features of HiveMQ Edge Pulse, you need to activate it first.',
        }),
      ])
    })

    it('should return the Pulse todo list', async () => {
      server.use(...handlerCapabilities(MOCK_CAPABILITIES))

      const { result } = renderHook(() => useOnboarding(), { wrapper })

      await waitFor(() => {
        const data = result.current
        expect(data?.[2].isLoading).toEqual(false)
      })

      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [_adapter, _bridge, _cloud, pulse] = result.current

      expect(pulse).toStrictEqual(expect.objectContaining({ header: 'Connect to HiveMQ Pulse' }))
      expect(pulse.sections).toStrictEqual([
        expect.objectContaining({
          label: 'Pulse Activation',
          title: 'Pulse is currently active.',
        }),
        expect.objectContaining({
          label: 'Manage Pulse Assets',
          title: 'Use HiveMQ Edge Pulse to manage and publish assets to your HiveMQ Edge',
        }),
        expect.objectContaining({
          title: 'Stay up-to-date with your asset mappings',
        }),
      ])
    })
  })
})
