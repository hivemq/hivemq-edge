import { expect, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import { handlers } from '@/api/hooks/useFrontendServices/__handlers__'

import '@/config/i18n.config.ts'

import useGetNavItems from './useGetNavItems.tsx'

describe('useSpringClient', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should retrieve all the menu items', async () => {
    server.use(...handlers)
    const { result } = renderHook(useGetNavItems, { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBeTruthy()
    })

    expect(result.current.data.map((e) => e.title)).toStrictEqual(['HiveMQ Edge', 'External resources'])
    expect(result.current.data[0].items).toHaveLength(7)
    expect(result.current.data[1].items.map((e) => e.href)).toStrictEqual([
      'https://www.hivemq.com/articles/power-of-iot-data-management-in-smart-manufacturing/',
      'https://github.com/hivemq/hivemq-edge',
      'https://github.com/hivemq/hivemq-edge/wiki',
    ])
  })
})
