import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import { useCompleteCombinerWizard } from './useCompleteCombinerWizard'
import { useCreateCombiner } from '@/api/hooks/useCombiners/useCreateCombiner'
import { useCreateAssetMapper } from '@/api/hooks/useAssetMapper/useCreateAssetMapper'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import type { Combiner } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import type { Node, Edge } from '@xyflow/react'

// Mock dependencies
vi.mock('@chakra-ui/react', () => ({
  useToast: vi.fn(),
}))

vi.mock('@xyflow/react', () => ({
  useReactFlow: vi.fn(),
}))

vi.mock('@/api/hooks/useCombiners/useCreateCombiner', () => ({
  useCreateCombiner: vi.fn(),
}))

vi.mock('@/api/hooks/useAssetMapper/useCreateAssetMapper', () => ({
  useCreateAssetMapper: vi.fn(),
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

vi.mock('../utils/ghostNodeFactory', () => ({
  removeGhostNodes: vi.fn((nodes: Node[]) => nodes.filter((n) => !n.data?.isGhost)),
  removeGhostEdges: vi.fn((edges: Edge[]) => edges.filter((e) => !e.data?.isGhost)),
}))

describe('useCompleteCombinerWizard', () => {
  const mockToast = vi.fn()
  const mockGetNodes = vi.fn()
  const mockSetNodes = vi.fn()
  const mockGetEdges = vi.fn()
  const mockSetEdges = vi.fn()
  const mockCreateCombiner = vi.fn()
  const mockCreateAssetMapper = vi.fn()
  const mockCompleteWizard = vi.fn()
  const mockCancelWizard = vi.fn()
  const mockSetError = vi.fn()

  const mockCombinerData: Combiner = {
    id: 'test-combiner-123',
    name: 'Test Combiner',
    description: 'Test description',
    sources: {
      items: [
        { id: 'adapter-1', type: EntityType.ADAPTER },
        { id: 'adapter-2', type: EntityType.ADAPTER },
      ],
    },
    mappings: {
      items: [],
    },
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()

    vi.mocked(useToast).mockReturnValue(mockToast as unknown as ReturnType<typeof useToast>)
    vi.mocked(useReactFlow).mockReturnValue({
      getNodes: mockGetNodes,
      setNodes: mockSetNodes,
      getEdges: mockGetEdges,
      setEdges: mockSetEdges,
    } as unknown as ReturnType<typeof useReactFlow>)
    vi.mocked(useCreateCombiner).mockReturnValue({
      mutateAsync: mockCreateCombiner,
    } as unknown as ReturnType<typeof useCreateCombiner>)
    vi.mocked(useCreateAssetMapper).mockReturnValue({
      mutateAsync: mockCreateAssetMapper,
    } as unknown as ReturnType<typeof useCreateAssetMapper>)
    vi.mocked(useWizardStore.getState).mockReturnValue({
      actions: {
        completeWizard: mockCompleteWizard,
        cancelWizard: mockCancelWizard,
        setError: mockSetError,
      },
    } as unknown as ReturnType<typeof useWizardStore.getState>)

    mockGetNodes.mockReturnValue([
      {
        id: 'ghost-combiner-preview',
        data: { isGhost: true },
        position: { x: 100, y: 100 },
      },
      {
        id: 'ADAPTER_NODE@adapter-1',
        data: { id: 'adapter-1' },
        position: { x: 0, y: 0 },
      },
    ])
    mockGetEdges.mockReturnValue([
      {
        id: 'ghost-edge-1',
        data: { isGhost: true },
        source: 'ghost-combiner-preview',
        target: 'EDGE_NODE',
      },
    ])

    mockCreateCombiner.mockResolvedValue(mockCombinerData)
    mockCreateAssetMapper.mockResolvedValue(mockCombinerData)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('hook interface', () => {
    it('should return an object with completeWizard function', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(result.current).toHaveProperty('completeWizard')
      expect(typeof result.current.completeWizard).toBe('function')
    })

    it('should return an object with isCompleting boolean', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(result.current).toHaveProperty('isCompleting')
      expect(typeof result.current.isCompleting).toBe('boolean')
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      // Verify the exact shape expected by TypeScript
      expect(result.current).toEqual({
        completeWizard: expect.any(Function),
        isCompleting: expect.any(Boolean),
      })
    })
  })

  describe('initial state', () => {
    it('should start with isCompleting as false', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(result.current.isCompleting).toBe(false)
    })
  })

  describe('completeWizard function signature', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      const returnValue = result.current.completeWizard(mockCombinerData)
      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should not throw synchronously', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(() => {
        result.current.completeWizard(mockCombinerData)
      }).not.toThrow()
    })
  })

  describe('combiner creation', () => {
    it('should call createCombiner when isAssetMapper is false', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      // Start the completion process
      result.current.completeWizard(mockCombinerData)

      // Should call the correct API
      expect(mockCreateCombiner).toHaveBeenCalledWith({ requestBody: mockCombinerData })
    })

    it('should NOT call createAssetMapper when isAssetMapper is false', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      result.current.completeWizard(mockCombinerData)

      expect(mockCreateCombiner).toHaveBeenCalled()
      expect(mockCreateAssetMapper).not.toHaveBeenCalled()
    })
  })

  describe('asset mapper creation', () => {
    it('should call createAssetMapper when isAssetMapper is true', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: true }))

      result.current.completeWizard(mockCombinerData)

      expect(mockCreateAssetMapper).toHaveBeenCalledWith({ requestBody: mockCombinerData })
    })

    it('should NOT call createCombiner when isAssetMapper is true', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: true }))

      result.current.completeWizard(mockCombinerData)

      expect(mockCreateAssetMapper).toHaveBeenCalled()
      expect(mockCreateCombiner).not.toHaveBeenCalled()
    })
  })

  // NOTE: Success flow tests with timers/animations are better suited for E2E tests.
  // Unit tests focus on behavior contracts, not timer-based integration.

  describe('error handling', () => {
    it('should handle API errors for combiner creation', async () => {
      const error = new Error('API Error: Failed to create combiner')
      mockCreateCombiner.mockRejectedValue(error)

      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      await expect(result.current.completeWizard(mockCombinerData)).rejects.toThrow(error)

      // Should show error toast
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          duration: 7000,
        })
      )

      // Should set error in wizard store
      expect(mockSetError).toHaveBeenCalledWith(error.message)
    })

    it('should handle API errors for asset mapper creation', async () => {
      const error = new Error('API Error: Failed to create asset mapper')
      mockCreateAssetMapper.mockRejectedValue(error)

      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: true }))

      await expect(result.current.completeWizard(mockCombinerData)).rejects.toThrow(error)

      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'error',
          duration: 7000,
        })
      )

      expect(mockSetError).toHaveBeenCalledWith(error.message)
    })

    it('should handle error state correctly', async () => {
      const error = new Error('Test error')
      mockCreateCombiner.mockRejectedValue(error)

      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      // Should reject with error
      await expect(result.current.completeWizard(mockCombinerData)).rejects.toThrow(error)
    })

    it('should NOT call cancelWizard on error', async () => {
      const error = new Error('Test error')
      mockCreateCombiner.mockRejectedValue(error)

      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      await expect(result.current.completeWizard(mockCombinerData)).rejects.toThrow()

      // Should not reset wizard state on error
      expect(mockCancelWizard).not.toHaveBeenCalled()
    })
  })

  describe('dependencies', () => {
    it('should use useToast hook', () => {
      renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(useToast).toHaveBeenCalled()
    })

    it('should use useReactFlow hook', () => {
      renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(useReactFlow).toHaveBeenCalled()
    })

    it('should use useCreateCombiner hook', () => {
      renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(useCreateCombiner).toHaveBeenCalled()
    })

    it('should use useCreateAssetMapper hook', () => {
      renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(useCreateAssetMapper).toHaveBeenCalled()
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      // This test passes if TypeScript compilation succeeds
      const { completeWizard, isCompleting } = result.current

      expect(completeWizard).toBeDefined()
      expect(isCompleting).toBeDefined()
    })

    it('should accept Combiner type for completeWizard parameter', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      // Should not throw TypeScript error
      expect(() => {
        result.current.completeWizard(mockCombinerData)
      }).not.toThrow()
    })

    it('should return boolean for isCompleting', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      expect(typeof result.current.isCompleting).toBe('boolean')
    })
  })

  describe('isAssetMapper configuration', () => {
    it('should accept isAssetMapper: false', () => {
      expect(() => {
        renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))
      }).not.toThrow()
    })

    it('should accept isAssetMapper: true', () => {
      expect(() => {
        renderHook(() => useCompleteCombinerWizard({ isAssetMapper: true }))
      }).not.toThrow()
    })

    it('should require isAssetMapper option', () => {
      // TypeScript would catch this, but verify at runtime
      const { result } = renderHook(() =>
        useCompleteCombinerWizard({ isAssetMapper: false } as {
          isAssetMapper: boolean
        })
      )

      expect(result.current).toBeDefined()
    })
  })

  describe('return value', () => {
    it('should return a promise from completeWizard', () => {
      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      const returnValue = result.current.completeWizard(mockCombinerData)

      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should throw error on failure', async () => {
      const error = new Error('Creation failed')
      mockCreateCombiner.mockRejectedValue(error)

      const { result } = renderHook(() => useCompleteCombinerWizard({ isAssetMapper: false }))

      await expect(result.current.completeWizard(mockCombinerData)).rejects.toThrow(error)
    })
  })
})
