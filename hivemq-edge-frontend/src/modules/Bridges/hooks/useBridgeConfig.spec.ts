import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'
import { BridgeProviderWrapper as wrapper } from '@/__test-utils__/hooks/BridgeProviderWrapper'

import { useBridgeConfig } from './useBridgeConfig'
import type { BridgeContextProps } from './BridgeContext'

describe('useBridgeConfig', () => {
  beforeEach(() => {})

  afterEach(() => {})

  it('should be used in the right context', () => {
    expect(() => {
      renderHook(() => useBridgeConfig())
    }).toThrow('useBridgeSetup must be used within a BridgeContext')
  })

  it('should be used in the right context', () => {
    const { result } = renderHook(() => useBridgeConfig(), { wrapper })
    expect(result.current).toEqual<BridgeContextProps>(
      expect.objectContaining({
        bridge: {
          cleanStart: true,
          host: '',
          id: '',
          keepAlive: 60,
          persist: true,
          port: 1883,
          sessionExpiry: 3600,
        },
      })
    )
  })
})
