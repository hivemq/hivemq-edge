import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect } from 'vitest'

import '@/config/i18n.config.ts'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { handlers as handlerGatewayConfiguration } from '@/api/hooks/useFrontendServices/__handlers__'

import { useOnboarding } from './useOnboarding.tsx'

describe('useOnboarding()', () => {
  beforeEach(() => {
    window.localStorage.clear()
    server.use(...handlerGatewayConfiguration)
  })

  afterEach(() => {
    server.resetHandlers()
  })

  it('should return the correct list of tasks', async () => {
    const { result } = renderHook(() => useOnboarding(), { wrapper })

    await waitFor(() => {
      const { data } = result.current
      expect(data?.[2].isLoading).toEqual(false)
    })

    const { data } = result.current
    expect(data?.[0].sections).toHaveLength(1)
    expect(data?.[1].sections).toHaveLength(1)
    expect(data?.[0].sections).toEqual(expect.arrayContaining([expect.objectContaining({ to: '/protocol-adapters' })]))
    expect(data?.[1].sections).toEqual(expect.arrayContaining([expect.objectContaining({ to: '/mqtt-bridges' })]))
    expect(data?.[2].sections).toEqual(
      expect.arrayContaining([expect.objectContaining({ to: 'https://hivemq.com/cloud' })])
    )
  })
})
