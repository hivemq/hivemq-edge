import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'

import { getWrapperEdgeProvider } from '@/__test-utils__/hooks/WrapperEdgeProvider.tsx'

describe('useAuth', () => {
  beforeEach(() => {})

  it('should be used in the right context', () => {
    expect(() => {
      renderHook(() => useAuth())
    }).toThrow('useAuth must be used within a AuthProvider')
  })

  it('should return the canvas options', () => {
    const { result } = renderHook(() => useAuth(), { wrapper: getWrapperEdgeProvider() })
    expect(result.current).toEqual({
      credentials: null,
      isAuthenticated: false,
      isLoading: false,
      login: expect.any(Function),
      logout: expect.any(Function),
    })
  })
})
