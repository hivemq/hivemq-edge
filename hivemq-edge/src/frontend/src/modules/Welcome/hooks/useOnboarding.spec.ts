import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'
import '@/config/i18n.config.ts'

import { useOnboarding } from './useOnboarding.tsx'

describe('useOnboarding()', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('should return the correct list of tasks', () => {
    const { result } = renderHook(() => useOnboarding())

    expect(result.current).toHaveLength(2)
    expect(result.current[0].sections).toHaveLength(1)
    expect(result.current[1].sections).toHaveLength(1)
    expect(result.current[0].sections).toEqual(
      expect.arrayContaining([expect.objectContaining({ to: '/protocol-adapters' })])
    )
    expect(result.current[1].sections).toEqual(
      expect.arrayContaining([expect.objectContaining({ to: '/mqtt-bridges' })])
    )
  })
})
