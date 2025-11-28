/**
 * Unit tests for useCompleteUtilities hook
 *
 * This hook is a refactored utility extracted from:
 * - useCompleteAdapterWizard
 * - useCompleteBridgeWizard
 * - useCompleteCombinerWizard
 *
 * It handles the common ghost node transition sequence:
 * 1. Fade out ghost nodes (opacity animation)
 * 2. Wait for animation and real nodes to appear
 * 3. Remove ghost nodes and edges
 * 4. Reset wizard state
 *
 * NOTE: This hook involves timers and React Flow state management.
 * These tests focus on:
 * - Return type validation
 * - State transitions
 * - Timing behavior
 * - Integration with React Flow and wizard store
 *
 * Full integration testing should be done via E2E tests.
 */

import { GHOST_SUCCESS_OPACITY_TRANSITION } from '@/modules/Workspace/components/wizard/utils/styles.ts'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useReactFlow } from '@xyflow/react'

import { useCompleteUtilities } from './useCompleteUtilities'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import type { Node, Edge } from '@xyflow/react'

// Mock dependencies
vi.mock('@xyflow/react', () => ({
  useReactFlow: vi.fn(),
}))

vi.mock('@/modules/Workspace/hooks/useWizardStore', () => ({
  useWizardStore: {
    getState: vi.fn(),
  },
}))

vi.mock('@/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts', () => ({
  removeGhostNodes: vi.fn((nodes) => nodes.filter((n: Node) => !n.data?.isGhost)),
  removeGhostEdges: vi.fn((edges) => edges.filter((e: Edge) => !e.id.startsWith('ghost-'))),
}))

describe('useCompleteUtilities', () => {
  const mockGetNodes = vi.fn()
  const mockSetNodes = vi.fn()
  const mockGetEdges = vi.fn()
  const mockSetEdges = vi.fn()
  const mockCancelWizard = vi.fn()

  // Mock nodes for testing
  const mockGhostNode: Node = {
    id: 'ghost-adapter-1',
    type: 'adapter',
    position: { x: 100, y: 100 },
    data: { isGhost: true, label: 'Ghost Adapter' },
  }

  const mockRealNode: Node = {
    id: 'adapter-1',
    type: 'adapter',
    position: { x: 100, y: 100 },
    data: { label: 'Real Adapter' },
  }

  const mockGhostEdge: Edge = {
    id: 'ghost-edge-1',
    source: 'ghost-adapter-1',
    target: 'edge-node',
  }

  const mockRealEdge: Edge = {
    id: 'edge-1',
    source: 'adapter-1',
    target: 'edge-node',
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()

    vi.mocked(useReactFlow).mockReturnValue({
      getNodes: mockGetNodes,
      setNodes: mockSetNodes,
      getEdges: mockGetEdges,
      setEdges: mockSetEdges,
    } as unknown as ReturnType<typeof useReactFlow>)

    vi.mocked(useWizardStore.getState).mockReturnValue({
      actions: {
        cancelWizard: mockCancelWizard,
      },
    } as unknown as ReturnType<typeof useWizardStore.getState>)

    mockGetNodes.mockReturnValue([mockGhostNode, mockRealNode])
    mockGetEdges.mockReturnValue([mockGhostEdge, mockRealEdge])
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('hook interface', () => {
    it('should return an object with handleTransitionSequence function', () => {
      const { result } = renderHook(() => useCompleteUtilities())

      expect(result.current).toHaveProperty('handleTransitionSequence')
      expect(typeof result.current.handleTransitionSequence).toBe('function')
    })

    it('should have correct return type shape', () => {
      const { result } = renderHook(() => useCompleteUtilities())

      expect(result.current).toEqual({
        handleTransitionSequence: expect.any(Function),
      })
    })
  })

  describe('handleTransitionSequence', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const returnValue = result.current.handleTransitionSequence()
      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should not throw synchronously', () => {
      const { result } = renderHook(() => useCompleteUtilities())

      expect(() => {
        result.current.handleTransitionSequence()
      }).not.toThrow()
    })
  })

  describe('ghost node fade animation', () => {
    it('should fade ghost nodes to 30% opacity', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      result.current.handleTransitionSequence()

      // setNodes should be called synchronously with fade animation
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      const fadedNodes = mockSetNodes.mock.calls[0][0]
      const fadedGhostNode = fadedNodes.find((n: Node) => n.data?.isGhost)

      expect(fadedGhostNode).toBeDefined()
      expect(fadedGhostNode.style).toEqual({
        opacity: 0.3,
        transition: GHOST_SUCCESS_OPACITY_TRANSITION,
      })
    })

    it('should preserve non-ghost nodes during fade', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      result.current.handleTransitionSequence()

      // setNodes should be called synchronously
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      const fadedNodes = mockSetNodes.mock.calls[0][0]
      const realNodeAfterFade = fadedNodes.find((n: Node) => !n.data?.isGhost)

      expect(realNodeAfterFade).toEqual(mockRealNode)
    })

    it('should apply fade transition to all ghost nodes', async () => {
      const multipleGhostNodes = [
        { ...mockGhostNode, id: 'ghost-1' },
        { ...mockGhostNode, id: 'ghost-2' },
        { ...mockGhostNode, id: 'ghost-3' },
        mockRealNode,
      ]
      mockGetNodes.mockReturnValue(multipleGhostNodes)

      const { result } = renderHook(() => useCompleteUtilities())

      result.current.handleTransitionSequence()

      // setNodes should be called synchronously
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      const fadedNodes = mockSetNodes.mock.calls[0][0]
      const fadedGhostNodes = fadedNodes.filter((n: Node) => n.data?.isGhost)

      expect(fadedGhostNodes).toHaveLength(3)
      fadedGhostNodes.forEach((node: Node) => {
        expect(node.style?.opacity).toBe(0.3)
        expect(node.style?.transition).toBe('opacity 0.5s ease-out')
      })
    })
  })

  describe('timing behavior', () => {
    it('should wait 600ms before removing ghost nodes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // After initial fade, should have called setNodes once (synchronously)
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      // Before 600ms, should not remove ghosts yet
      vi.advanceTimersByTime(500)
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      // After 600ms, should remove ghosts
      vi.advanceTimersByTime(100)

      await transitionPromise

      expect(mockSetNodes).toHaveBeenCalledTimes(2)
    })

    it('should complete the full sequence within expected time', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Fast-forward through the entire sequence
      vi.advanceTimersByTime(600)

      await expect(transitionPromise).resolves.toBeUndefined()
    })
  })

  describe('ghost node removal', () => {
    it('should remove ghost nodes after fade completes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Fade happens synchronously
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      // Advance timer to trigger removal
      vi.advanceTimersByTime(600)

      await transitionPromise

      // Should have called setNodes twice: once for fade, once for removal
      expect(mockSetNodes).toHaveBeenCalledTimes(2)

      // Second call should have nodes without ghosts
      const finalNodes = mockSetNodes.mock.calls[1][0]
      expect(finalNodes.find((n: Node) => n.data?.isGhost)).toBeUndefined()
    })

    it('should remove ghost edges after fade completes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Advance timer to trigger removal
      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(mockSetEdges).toHaveBeenCalledTimes(1)

      // Should have edges without ghost edges
      const finalEdges = mockSetEdges.mock.calls[0][0]
      expect(finalEdges.find((e: Edge) => e.id.startsWith('ghost-'))).toBeUndefined()
    })

    it('should call removeGhostNodes utility', async () => {
      const { removeGhostNodes } = await import('@/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts')
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(removeGhostNodes).toHaveBeenCalledWith(expect.any(Array))
    })

    it('should call removeGhostEdges utility', async () => {
      const { removeGhostEdges } = await import('@/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts')
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(removeGhostEdges).toHaveBeenCalledWith(expect.any(Array))
    })
  })

  describe('wizard state reset', () => {
    it('should call cancelWizard after sequence completes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(mockCancelWizard).toHaveBeenCalledTimes(1)
    })

    it('should reset wizard state after ghost removal', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Before timer completes, wizard should not be cancelled
      vi.advanceTimersByTime(500)
      expect(mockCancelWizard).not.toHaveBeenCalled()

      // After timer completes, wizard should be cancelled
      vi.advanceTimersByTime(100)
      await transitionPromise

      expect(mockCancelWizard).toHaveBeenCalled()
    })
  })

  describe('React Flow integration', () => {
    it('should use getNodes from React Flow', () => {
      renderHook(() => useCompleteUtilities())

      expect(useReactFlow).toHaveBeenCalled()
    })

    it('should call getNodes to retrieve current nodes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Should call getNodes immediately
      expect(mockGetNodes).toHaveBeenCalledTimes(1)

      // Clean up
      vi.advanceTimersByTime(600)
      await transitionPromise
    })

    it('should call setNodes with modified nodes', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Should call setNodes synchronously with modified nodes
      expect(mockSetNodes).toHaveBeenCalledWith(expect.any(Array))

      // Clean up
      vi.advanceTimersByTime(600)
      await transitionPromise
    })

    it('should call getEdges to retrieve current edges', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(mockGetEdges).toHaveBeenCalled()
    })

    it('should call setEdges with modified edges', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(mockSetEdges).toHaveBeenCalledWith(expect.any(Array))
    })
  })

  describe('edge cases', () => {
    it('should handle empty node array', async () => {
      mockGetNodes.mockReturnValue([])
      mockGetEdges.mockReturnValue([])

      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await expect(transitionPromise).resolves.toBeUndefined()
      expect(mockCancelWizard).toHaveBeenCalled()
    })

    it('should handle nodes without ghost data', async () => {
      mockGetNodes.mockReturnValue([mockRealNode])

      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Should be called synchronously
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      const fadedNodes = mockSetNodes.mock.calls[0][0]
      expect(fadedNodes[0]).toEqual(mockRealNode)

      // Clean up
      vi.advanceTimersByTime(600)
      await transitionPromise
    })

    it('should handle nodes with existing styles', async () => {
      const nodeWithStyle: Node = {
        ...mockGhostNode,
        style: {
          backgroundColor: 'blue',
          borderColor: 'red',
        },
      }
      mockGetNodes.mockReturnValue([nodeWithStyle])

      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      // Should be called synchronously
      expect(mockSetNodes).toHaveBeenCalledTimes(1)

      const fadedNodes = mockSetNodes.mock.calls[0][0]
      expect(fadedNodes[0].style).toEqual({
        backgroundColor: 'blue',
        borderColor: 'red',
        opacity: 0.3,
        transition: 'opacity 0.5s ease-out',
      })

      // Clean up
      vi.advanceTimersByTime(600)
      await transitionPromise
    })

    it('should handle multiple sequential calls', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const promise1 = result.current.handleTransitionSequence()
      vi.advanceTimersByTime(600)
      await promise1

      vi.clearAllMocks()

      const promise2 = result.current.handleTransitionSequence()
      vi.advanceTimersByTime(600)
      await promise2

      expect(mockCancelWizard).toHaveBeenCalledTimes(1)
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useCompleteUtilities())

      // This test passes if TypeScript compilation succeeds
      const { handleTransitionSequence } = result.current

      expect(handleTransitionSequence).toBeDefined()
    })

    it('should accept void return type from handleTransitionSequence', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const returnValue = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await expect(returnValue).resolves.toBeUndefined()
    })
  })

  describe('dependencies', () => {
    it('should use useReactFlow hook', () => {
      renderHook(() => useCompleteUtilities())

      expect(useReactFlow).toHaveBeenCalled()
    })

    it('should access wizard store', async () => {
      const { result } = renderHook(() => useCompleteUtilities())

      const transitionPromise = result.current.handleTransitionSequence()

      vi.advanceTimersByTime(600)

      await transitionPromise

      expect(useWizardStore.getState).toHaveBeenCalled()
    })
  })
})
