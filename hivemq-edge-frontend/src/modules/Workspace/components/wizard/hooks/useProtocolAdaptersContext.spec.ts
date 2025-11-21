/**
 * Unit tests for useProtocolAdaptersContext hook
 *
 * This is a simple context hook wrapper that provides access to ProtocolAdaptersContext.
 * Tests focus on:
 * - Return type validation
 * - Context value access
 * - Type safety
 *
 * NOTE: This is a minimal context hook - tests are intentionally simple.
 * Full integration testing is covered by E2E tests.
 */

import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { useProtocolAdaptersContext } from './useProtocolAdaptersContext'

describe('useProtocolAdaptersContext', () => {
  describe('hook interface', () => {
    it('should return context value object', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      expect(result.current).toBeDefined()
      expect(typeof result.current).toBe('object')
    })

    it('should have protocolAdapters property', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      expect(result.current).toHaveProperty('protocolAdapters')
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      expect(result.current).toHaveProperty('protocolAdapters')
      expect(Object.keys(result.current)).toEqual(['protocolAdapters'])
    })
  })

  describe('default behavior', () => {
    it('should return default context value with undefined protocolAdapters', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      expect(result.current.protocolAdapters).toBeUndefined()
    })

    it('should not throw when called without provider', () => {
      expect(() => {
        renderHook(() => useProtocolAdaptersContext())
      }).not.toThrow()
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      // This test passes if TypeScript compilation succeeds
      const { protocolAdapters } = result.current

      // protocolAdapters can be undefined (default value)
      expect(protocolAdapters === undefined || Array.isArray(protocolAdapters)).toBe(true)
    })

    it('should allow undefined protocolAdapters', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      // TypeScript should allow undefined
      const adapters = result.current.protocolAdapters
      if (adapters) {
        expect(Array.isArray(adapters)).toBe(true)
      } else {
        expect(adapters).toBeUndefined()
      }
    })

    it('should return same reference on multiple renders', () => {
      const { result, rerender } = renderHook(() => useProtocolAdaptersContext())

      const firstResult = result.current

      rerender()

      expect(result.current).toBe(firstResult)
    })
  })

  describe('hook dependencies', () => {
    it('should use React useContext internally', () => {
      // This test verifies the hook doesn't throw
      // useContext is called internally
      const { result } = renderHook(() => useProtocolAdaptersContext())

      expect(result.current).toBeDefined()
    })

    it('should access ProtocolAdaptersContext', () => {
      const { result } = renderHook(() => useProtocolAdaptersContext())

      // Default value from context should be returned
      expect(result.current).toEqual({
        protocolAdapters: undefined,
      })
    })
  })
})
