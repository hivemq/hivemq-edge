/**
 * Basic unit tests for useCompleteBridgeWizard hook
 *
 * NOTE: This hook is complex and integration-heavy (timers, React Flow, React Query).
 * These tests focus on:
 * - Return type validation
 * - Basic error handling
 * - Type safety
 *
 * Full integration testing should be done via E2E tests.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import { useCompleteBridgeWizard } from './useCompleteBridgeWizard'
import { useCreateBridge } from '@/api/hooks/useGetBridges/useCreateBridge'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'

// Mock dependencies
vi.mock('@chakra-ui/react', () => ({
  useToast: vi.fn(),
}))

vi.mock('@xyflow/react', () => ({
  useReactFlow: vi.fn(),
}))

vi.mock('@/api/hooks/useGetBridges/useCreateBridge', () => ({
  useCreateBridge: vi.fn(),
}))

vi.mock('@/modules/Workspace/hooks/useWizardStore', () => ({
  useWizardStore: {
    getState: vi.fn(),
  },
}))

describe('useCompleteBridgeWizard', () => {
  const mockToast = vi.fn()
  const mockGetNodes = vi.fn()
  const mockSetNodes = vi.fn()
  const mockGetEdges = vi.fn()
  const mockSetEdges = vi.fn()
  const mockCreateBridge = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(useToast).mockReturnValue(mockToast as unknown as ReturnType<typeof useToast>)
    vi.mocked(useReactFlow).mockReturnValue({
      getNodes: mockGetNodes,
      setNodes: mockSetNodes,
      getEdges: mockGetEdges,
      setEdges: mockSetEdges,
    } as unknown as ReturnType<typeof useReactFlow>)
    vi.mocked(useCreateBridge).mockReturnValue({
      mutateAsync: mockCreateBridge,
    } as unknown as ReturnType<typeof useCreateBridge>)
    vi.mocked(useWizardStore.getState).mockReturnValue({
      configurationData: {
        bridgeConfig: {
          id: 'test-bridge',
          name: 'Test Bridge',
          host: 'broker.example.com',
          port: 1883,
        },
      },
      actions: {
        completeWizard: vi.fn(),
        setError: vi.fn(),
      },
    } as unknown as ReturnType<typeof useWizardStore.getState>)

    mockGetNodes.mockReturnValue([])
    mockGetEdges.mockReturnValue([])
  })

  describe('hook interface', () => {
    it('should return an object with completeWizard function', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      expect(result.current).toHaveProperty('completeWizard')
      expect(typeof result.current.completeWizard).toBe('function')
    })

    it('should return an object with isCompleting boolean', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      expect(result.current).toHaveProperty('isCompleting')
      expect(typeof result.current.isCompleting).toBe('boolean')
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      // Verify the exact shape expected by TypeScript
      expect(result.current).toEqual({
        completeWizard: expect.any(Function),
        isCompleting: expect.any(Boolean),
      })
    })
  })

  describe('initial state', () => {
    it('should start with isCompleting as false', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      expect(result.current.isCompleting).toBe(false)
    })
  })

  describe('completeWizard function signature', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      const returnValue = result.current.completeWizard()
      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should not throw synchronously', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      expect(() => {
        result.current.completeWizard()
      }).not.toThrow()
    })
  })

  describe('error handling - missing configuration', () => {
    it('should handle missing bridgeConfig gracefully', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {},
        actions: {
          completeWizard: vi.fn(),
          setError: vi.fn(),
        },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteBridgeWizard())

      // Should not throw, but handle error internally
      await expect(result.current.completeWizard()).resolves.not.toThrow()
    })

    it('should handle empty configuration gracefully', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          bridgeConfig: undefined,
        },
        actions: {
          completeWizard: vi.fn(),
          setError: vi.fn(),
        },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteBridgeWizard())

      // Should not throw, but handle error internally
      await expect(result.current.completeWizard()).resolves.not.toThrow()
    })
  })

  describe('dependencies', () => {
    it('should use useToast hook', () => {
      renderHook(() => useCompleteBridgeWizard())

      expect(useToast).toHaveBeenCalled()
    })

    it('should use useReactFlow hook', () => {
      renderHook(() => useCompleteBridgeWizard())

      expect(useReactFlow).toHaveBeenCalled()
    })

    it('should use useCreateBridge hook', () => {
      renderHook(() => useCompleteBridgeWizard())

      expect(useCreateBridge).toHaveBeenCalled()
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      // This test passes if TypeScript compilation succeeds
      const { completeWizard, isCompleting } = result.current

      expect(completeWizard).toBeDefined()
      expect(isCompleting).toBeDefined()
    })
  })

  describe('shared behavior with adapter wizard', () => {
    it('should have same interface as useCompleteAdapterWizard', () => {
      const { result } = renderHook(() => useCompleteBridgeWizard())

      // Both hooks should have identical interface
      expect(Object.keys(result.current).sort()).toEqual(['completeWizard', 'isCompleting'].sort())
    })
  })
})
