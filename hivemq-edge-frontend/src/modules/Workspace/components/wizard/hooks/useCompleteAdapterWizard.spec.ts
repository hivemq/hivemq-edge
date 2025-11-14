/**
 * Basic unit tests for useCompleteAdapterWizard hook
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
import { act, renderHook } from '@testing-library/react'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import { useCompleteAdapterWizard } from './useCompleteAdapterWizard'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'

// Mock dependencies
vi.mock('@chakra-ui/react', () => ({
  useToast: vi.fn(),
}))

vi.mock('@xyflow/react', () => ({
  useReactFlow: vi.fn(),
}))

vi.mock('@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter', () => ({
  useCreateProtocolAdapter: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/modules/Workspace/hooks/useWizardStore', () => ({
  useWizardStore: {
    getState: vi.fn(),
  },
}))

describe('useCompleteAdapterWizard', () => {
  const mockToast = vi.fn()
  const mockGetNodes = vi.fn()
  const mockSetNodes = vi.fn()
  const mockGetEdges = vi.fn()
  const mockSetEdges = vi.fn()
  const mockCreateAdapter = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(useToast).mockReturnValue(mockToast as unknown as ReturnType<typeof useToast>)
    vi.mocked(useReactFlow).mockReturnValue({
      getNodes: mockGetNodes,
      setNodes: mockSetNodes,
      getEdges: mockGetEdges,
      setEdges: mockSetEdges,
    } as unknown as ReturnType<typeof useReactFlow>)
    vi.mocked(useCreateProtocolAdapter).mockReturnValue({
      mutateAsync: mockCreateAdapter,
    } as unknown as ReturnType<typeof useCreateProtocolAdapter>)
    vi.mocked(useWizardStore.getState).mockReturnValue({
      configurationData: {
        protocolId: 'simulation',
        adapterConfig: {
          id: 'test-adapter',
          name: 'Test Adapter',
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
      const { result } = renderHook(() => useCompleteAdapterWizard())

      expect(result.current).toHaveProperty('completeWizard')
      expect(typeof result.current.completeWizard).toBe('function')
    })

    it('should return an object with isCompleting boolean', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      expect(result.current).toHaveProperty('isCompleting')
      expect(typeof result.current.isCompleting).toBe('boolean')
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      // Verify the exact shape expected by TypeScript
      expect(result.current).toEqual({
        completeWizard: expect.any(Function),
        isCompleting: expect.any(Boolean),
      })
    })
  })

  describe('initial state', () => {
    it('should start with isCompleting as false', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      expect(result.current.isCompleting).toBe(false)
    })
  })

  describe('completeWizard function signature', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      act(() => {
        const returnValue = result.current.completeWizard()
        expect(returnValue).toBeInstanceOf(Promise)
      })
    })

    it('should not throw synchronously', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      act(() => {
        expect(() => {
          result.current.completeWizard()
        }).not.toThrow()
      })
    })
  })

  describe('error handling - missing configuration', () => {
    it('should handle missing protocolId gracefully', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          adapterConfig: { id: 'test' },
        },
        actions: {
          completeWizard: vi.fn(),
          setError: vi.fn(),
        },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteAdapterWizard())

      await act(async () => {
        // Should not throw, but handle error internally
        await expect(result.current.completeWizard()).resolves.not.toThrow()
      })
    })

    it('should handle missing adapterConfig gracefully', async () => {
      vi.mocked(useWizardStore.getState).mockReturnValue({
        configurationData: {
          protocolId: 'simulation',
        },
        actions: {
          completeWizard: vi.fn(),
          setError: vi.fn(),
        },
      } as unknown as ReturnType<typeof useWizardStore.getState>)

      const { result } = renderHook(() => useCompleteAdapterWizard())

      await act(async () => {
        // Should not throw, but handle error internally
        await expect(result.current.completeWizard()).resolves.not.toThrow()
      })
    })
  })

  describe('dependencies', () => {
    it('should use useToast hook', () => {
      renderHook(() => useCompleteAdapterWizard())

      expect(useToast).toHaveBeenCalled()
    })

    it('should use useReactFlow hook', () => {
      renderHook(() => useCompleteAdapterWizard())

      expect(useReactFlow).toHaveBeenCalled()
    })

    it('should use useCreateProtocolAdapter hook', () => {
      renderHook(() => useCompleteAdapterWizard())

      expect(useCreateProtocolAdapter).toHaveBeenCalled()
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useCompleteAdapterWizard())

      // This test passes if TypeScript compilation succeeds
      const { completeWizard, isCompleting } = result.current

      expect(completeWizard).toBeDefined()
      expect(isCompleting).toBeDefined()
    })
  })
})
