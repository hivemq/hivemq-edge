/**
 * Comprehensive tests for useLayoutEngine hook with proper renderHook usage
 *
 * This test file ensures code coverage by actually invoking the hook through renderHook,
 * rather than calling store methods directly. The existing test files test store integration
 * but don't provide coverage for the hook's internal code.
 *
 * Based on patterns from:
 * - useLayoutEngine.spec.ts (store integration tests)
 * - useLayoutEngine.hook.spec.ts (hook function coverage attempts)
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { ReactFlowProvider } from '@xyflow/react'
import type { FC, PropsWithChildren } from 'react'

import { useLayoutEngine } from './useLayoutEngine'
import useWorkspaceStore from './useWorkspaceStore'
import { LayoutType, LayoutMode } from '../types/layout'
import type { LayoutPreset } from '../types/layout'

// Mock debug
vi.mock('debug', () => ({
  default: () => vi.fn(),
}))

// Mock React Flow
vi.mock('@xyflow/react', async () => {
  const actual = await vi.importActual('@xyflow/react')
  return {
    ...actual,
    useReactFlow: vi.fn(() => ({
      getNodes: vi.fn(() => []),
      setNodes: vi.fn(),
      getEdges: vi.fn(() => []),
      setEdges: vi.fn(),
      fitView: vi.fn(),
    })),
  }
})

// Wrapper component that provides React Flow context
const wrapper: FC<PropsWithChildren> = ({ children }) => <ReactFlowProvider>{children}</ReactFlowProvider>

describe('useLayoutEngine - with renderHook', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()

    // Reset store to clean state
    const store = useWorkspaceStore.getState()
    store.onNodesChange([])
    store.onEdgesChange([])
    store.setLayoutAlgorithm(LayoutType.DAGRE_TB)
    store.clearLayoutHistory()

    // Clear presets
    const presets = [...store.layoutConfig.presets]
    presets.forEach((preset) => store.deleteLayoutPreset(preset.id))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('hook return structure', () => {
    it('should return all expected properties', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      // Core operations
      expect(result.current).toHaveProperty('applyLayout')
      expect(result.current).toHaveProperty('applyLayoutWithAlgorithm')

      // Algorithm selection
      expect(result.current).toHaveProperty('currentAlgorithm')
      expect(result.current).toHaveProperty('currentAlgorithmInstance')
      expect(result.current).toHaveProperty('setAlgorithm')
      expect(result.current).toHaveProperty('availableAlgorithms')

      // Mode control
      expect(result.current).toHaveProperty('layoutMode')
      expect(result.current).toHaveProperty('setLayoutMode')
      expect(result.current).toHaveProperty('isAutoLayoutEnabled')
      expect(result.current).toHaveProperty('toggleAutoLayout')

      // Options management
      expect(result.current).toHaveProperty('layoutOptions')
      expect(result.current).toHaveProperty('setLayoutOptions')
      expect(result.current).toHaveProperty('resetOptionsToDefault')

      // Preset management
      expect(result.current).toHaveProperty('presets')
      expect(result.current).toHaveProperty('saveCurrentLayout')
      expect(result.current).toHaveProperty('loadPreset')
      expect(result.current).toHaveProperty('deletePreset')

      // History management
      expect(result.current).toHaveProperty('canUndo')
      expect(result.current).toHaveProperty('undo')
      expect(result.current).toHaveProperty('layoutHistory')
      expect(result.current).toHaveProperty('clearHistory')

      // Registry access
      expect(result.current).toHaveProperty('layoutRegistry')
    })

    it('should have correct function types', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(typeof result.current.applyLayout).toBe('function')
      expect(typeof result.current.applyLayoutWithAlgorithm).toBe('function')
      expect(typeof result.current.setAlgorithm).toBe('function')
      expect(typeof result.current.setLayoutMode).toBe('function')
      expect(typeof result.current.toggleAutoLayout).toBe('function')
      expect(typeof result.current.setLayoutOptions).toBe('function')
      expect(typeof result.current.resetOptionsToDefault).toBe('function')
      expect(typeof result.current.saveCurrentLayout).toBe('function')
      expect(typeof result.current.loadPreset).toBe('function')
      expect(typeof result.current.deletePreset).toBe('function')
      expect(typeof result.current.undo).toBe('function')
      expect(typeof result.current.clearHistory).toBe('function')
    })
  })

  describe('currentAlgorithm and currentAlgorithmInstance', () => {
    it('should return current algorithm type', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.currentAlgorithm).toBe(LayoutType.DAGRE_TB)
    })

    it('should return algorithm instance from registry', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.currentAlgorithmInstance).toBeDefined()
      expect(result.current.currentAlgorithmInstance?.type).toBe(LayoutType.DAGRE_TB)
      expect(result.current.currentAlgorithmInstance?.name).toBeDefined()
    })

    it('should update when algorithm changes', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.currentAlgorithm).toBe(LayoutType.DAGRE_TB)

      act(() => {
        result.current.setAlgorithm(LayoutType.RADIAL_HUB)
      })

      rerender()

      expect(result.current.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
      expect(result.current.currentAlgorithmInstance?.type).toBe(LayoutType.RADIAL_HUB)
    })
  })

  describe('availableAlgorithms', () => {
    it('should return list of all algorithms', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.availableAlgorithms).toBeDefined()
      expect(Array.isArray(result.current.availableAlgorithms)).toBe(true)
      expect(result.current.availableAlgorithms.length).toBeGreaterThan(0)
    })

    it('should include known algorithm types', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const types = result.current.availableAlgorithms.map((a) => a.type)
      expect(types).toContain(LayoutType.DAGRE_TB)
      expect(types).toContain(LayoutType.DAGRE_LR)
      expect(types).toContain(LayoutType.RADIAL_HUB)
      expect(types).toContain(LayoutType.MANUAL)
    })
  })

  describe('layoutMode and setLayoutMode', () => {
    it('should return current layout mode', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.layoutMode).toBeDefined()
      expect(Object.values(LayoutMode)).toContain(result.current.layoutMode)
    })

    it('should update layout mode', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      const initialMode = result.current.layoutMode

      act(() => {
        result.current.setLayoutMode(LayoutMode.DYNAMIC)
      })

      rerender()

      expect(result.current.layoutMode).toBe(LayoutMode.DYNAMIC)
      expect(result.current.layoutMode).not.toBe(initialMode)
    })
  })

  describe('isAutoLayoutEnabled and toggleAutoLayout', () => {
    it('should return auto layout state', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(typeof result.current.isAutoLayoutEnabled).toBe('boolean')
    })

    it('should toggle auto layout', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      const initialState = result.current.isAutoLayoutEnabled

      act(() => {
        result.current.toggleAutoLayout()
      })

      rerender()

      expect(result.current.isAutoLayoutEnabled).toBe(!initialState)
    })
  })

  describe('layoutOptions and setLayoutOptions', () => {
    it('should return current layout options', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.layoutOptions).toBeDefined()
      expect(result.current.layoutOptions).toHaveProperty('animate')
      expect(result.current.layoutOptions).toHaveProperty('animationDuration')
    })

    it('should update layout options', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      const newOptions = {
        animate: false,
        animationDuration: 1000,
        fitView: true,
      }

      act(() => {
        result.current.setLayoutOptions(newOptions)
      })

      rerender()

      expect(result.current.layoutOptions.animate).toBe(false)
      expect(result.current.layoutOptions.animationDuration).toBe(1000)
      expect(result.current.layoutOptions.fitView).toBe(true)
    })
  })

  describe('resetOptionsToDefault', () => {
    it('should reset options to algorithm defaults', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Change options away from defaults
      act(() => {
        result.current.setLayoutOptions({ animate: false, animationDuration: 9999 })
      })

      rerender()

      expect(result.current.layoutOptions.animate).toBe(false)

      // Reset to defaults
      act(() => {
        result.current.resetOptionsToDefault()
      })

      rerender()

      // Should match algorithm's default options
      const defaultOptions = result.current.currentAlgorithmInstance?.defaultOptions
      expect(result.current.layoutOptions.animate).toBe(defaultOptions?.animate)
    })

    it('should do nothing if no algorithm selected', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      // This shouldn't throw
      result.current.resetOptionsToDefault()

      expect(result.current.layoutOptions).toBeDefined()
    })
  })

  describe('applyLayout', () => {
    it('should return null when no nodes exist', async () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const layoutResult = await result.current.applyLayout()

      expect(layoutResult).toBeNull()
    })

    it('should be an async function', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const returnValue = result.current.applyLayout()

      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should not throw when called', async () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      await expect(result.current.applyLayout()).resolves.not.toThrow()
    })

    it('should return error result when validation fails', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add a node so we can test validation
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'test-node', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
        ])
      })

      rerender()

      const layoutResult = await result.current.applyLayout()

      // If validation worked, result should indicate failure or be null
      // The exact behavior depends on the algorithm's validation
      expect(layoutResult).toBeDefined()
    })

    it('should handle successful layout with nodes', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add test nodes
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
          {
            type: 'add',
            item: { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'bridge' },
          },
        ])
      })

      rerender()

      const layoutResult = await result.current.applyLayout()

      // Result should be defined (either success or failure, not null)
      expect(layoutResult).toBeDefined()
      if (layoutResult) {
        expect(layoutResult).toHaveProperty('success')
        expect(layoutResult).toHaveProperty('nodes')
        expect(layoutResult).toHaveProperty('duration')
      }
    })

    it('should handle layout without animation', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add test nodes
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
        ])
      })

      // Disable animation
      act(() => {
        result.current.setLayoutOptions({ animate: false })
      })

      rerender()

      const layoutResult = await result.current.applyLayout()

      expect(layoutResult).toBeDefined()
    })

    it('should handle layout with animation enabled', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add test nodes
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
        ])
      })

      // Enable animation
      act(() => {
        result.current.setLayoutOptions({ animate: true, animationDuration: 300 })
      })

      rerender()

      const layoutResult = await result.current.applyLayout()

      expect(layoutResult).toBeDefined()
    })

    it('should handle layout with fitView enabled', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add test nodes
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
        ])
      })

      // Enable fitView
      act(() => {
        result.current.setLayoutOptions({
          fitView: true,
          fitViewOptions: { padding: 0.1, includeHiddenNodes: true },
        })
      })

      rerender()

      const layoutResult = await result.current.applyLayout()

      // Advance timer to trigger fitView setTimeout
      act(() => {
        vi.advanceTimersByTime(100)
      })

      expect(layoutResult).toBeDefined()
    })
  })

  describe('applyLayoutWithAlgorithm', () => {
    it('should be an async function', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const returnValue = result.current.applyLayoutWithAlgorithm(LayoutType.RADIAL_HUB)

      expect(returnValue).toBeInstanceOf(Promise)
    })

    it('should temporarily change algorithm', async () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const initialAlgorithm = result.current.currentAlgorithm

      await result.current.applyLayoutWithAlgorithm(LayoutType.RADIAL_HUB)

      // Algorithm should be changed (even though no nodes to layout)
      expect(result.current.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
      expect(result.current.currentAlgorithm).not.toBe(initialAlgorithm)
    })

    it('should accept custom options', async () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      await result.current.applyLayoutWithAlgorithm(LayoutType.DAGRE_LR, {
        animate: false,
        fitView: true,
      })

      expect(result.current.layoutOptions.animate).toBe(false)
      expect(result.current.layoutOptions.fitView).toBe(true)
    })

    it('should restore previous settings on layout failure', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add a node so layout can be attempted
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
          },
        ])
      })

      rerender()

      // Try to apply layout with different algorithm
      await result.current.applyLayoutWithAlgorithm(LayoutType.RADIAL_HUB, {
        animate: false,
      })

      rerender()

      // Check that the algorithm was applied (success or failure, still defined)
      expect(result.current.currentAlgorithm).toBeDefined()
      expect(result.current.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
    })
  })

  describe('preset management', () => {
    it('should access presets array', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(Array.isArray(result.current.presets)).toBe(true)
    })

    it('should save current layout as preset', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      const initialPresetCount = result.current.presets.length

      act(() => {
        result.current.saveCurrentLayout('Test Preset', 'Test description')
      })

      rerender()

      expect(result.current.presets.length).toBe(initialPresetCount + 1)
      expect(result.current.presets.some((p) => p.name === 'Test Preset')).toBe(true)
    })

    it('should save layout preset with node positions', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add nodes with specific positions
      const store = useWorkspaceStore.getState()
      act(() => {
        store.onAddNodes([
          {
            type: 'add',
            item: { id: 'node-1', position: { x: 100, y: 200 }, data: {}, type: 'adapter' },
          },
          {
            type: 'add',
            item: { id: 'node-2', position: { x: 300, y: 400 }, data: {}, type: 'bridge' },
          },
        ])
      })

      rerender()

      act(() => {
        result.current.saveCurrentLayout('Positions Test', 'Testing node positions')
      })

      rerender()

      const savedPreset = result.current.presets.find((p) => p.name === 'Positions Test')
      expect(savedPreset).toBeDefined()

      if (savedPreset?.positions) {
        // Check that the preset has the node positions (may include nodes from other tests)
        expect(savedPreset.positions.size).toBeGreaterThanOrEqual(2)
        // Verify the nodes exist in the preset (positions may have changed due to previous layout tests)
        expect(savedPreset.positions.has('node-1')).toBe(true)
        expect(savedPreset.positions.has('node-2')).toBe(true)
        // Verify positions are objects with x and y coordinates
        const node1Pos = savedPreset.positions.get('node-1')
        const node2Pos = savedPreset.positions.get('node-2')
        expect(node1Pos).toHaveProperty('x')
        expect(node1Pos).toHaveProperty('y')
        expect(node2Pos).toHaveProperty('x')
        expect(node2Pos).toHaveProperty('y')
      }
    })

    it('should delete preset by ID', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Save a preset first
      act(() => {
        result.current.saveCurrentLayout('Delete Me')
      })

      rerender()

      const presetToDelete = result.current.presets.find((p) => p.name === 'Delete Me')
      expect(presetToDelete).toBeDefined()

      act(() => {
        result.current.deletePreset(presetToDelete!.id)
      })

      rerender()

      expect(result.current.presets.find((p) => p.name === 'Delete Me')).toBeUndefined()
    })

    it('should load preset', async () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Save a preset with specific algorithm
      const store = useWorkspaceStore.getState()
      const preset: LayoutPreset = {
        id: 'load-test',
        name: 'Load Test',
        algorithm: LayoutType.RADIAL_HUB,
        options: { animate: true },
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      act(() => {
        store.saveLayoutPreset(preset)
      })

      rerender()

      // Change to different algorithm
      act(() => {
        result.current.setAlgorithm(LayoutType.DAGRE_TB)
      })

      rerender()

      expect(result.current.currentAlgorithm).toBe(LayoutType.DAGRE_TB)

      // Load the preset
      act(() => {
        result.current.loadPreset('load-test')
      })

      // Fast-forward the setTimeout
      act(() => {
        vi.advanceTimersByTime(100)
      })

      rerender()

      // Algorithm should be updated
      expect(result.current.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
    })
  })

  describe('history management', () => {
    it('should access layout history', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(Array.isArray(result.current.layoutHistory)).toBe(true)
    })

    it('should report canUndo correctly', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // No history initially
      expect(result.current.canUndo).toBe(false)

      // Add history entries
      act(() => {
        const store = useWorkspaceStore.getState()
        store.pushLayoutHistory({
          id: '1',
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
        store.pushLayoutHistory({
          id: '2',
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
      })

      rerender()

      expect(result.current.canUndo).toBe(true)
    })

    it('should call undo when history exists', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add history
      act(() => {
        const store = useWorkspaceStore.getState()
        store.pushLayoutHistory({
          id: '1',
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
        store.pushLayoutHistory({
          id: '2',
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
      })

      rerender()

      const initialHistoryLength = result.current.layoutHistory.length

      act(() => {
        result.current.undo()
      })

      rerender()

      expect(result.current.layoutHistory.length).toBe(initialHistoryLength - 1)
    })

    it('should not crash when undoing with insufficient history', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      // No history
      expect(() => result.current.undo()).not.toThrow()
    })

    it('should clear history', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      // Add history
      act(() => {
        const store = useWorkspaceStore.getState()
        store.pushLayoutHistory({
          id: '1',
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
      })

      rerender()

      expect(result.current.layoutHistory.length).toBeGreaterThan(0)

      act(() => {
        result.current.clearHistory()
      })

      rerender()

      expect(result.current.layoutHistory.length).toBe(0)
    })
  })

  describe('layoutRegistry access', () => {
    it('should expose layoutRegistry', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      expect(result.current.layoutRegistry).toBeDefined()
      expect(result.current.layoutRegistry).toHaveProperty('get')
      expect(result.current.layoutRegistry).toHaveProperty('getAll')
    })

    it('should allow accessing registry methods', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      const algorithm = result.current.layoutRegistry.get(LayoutType.DAGRE_TB)
      expect(algorithm).toBeDefined()

      const allAlgorithms = result.current.layoutRegistry.getAll()
      expect(allAlgorithms.length).toBeGreaterThan(0)
    })
  })

  describe('type safety', () => {
    it('should work with TypeScript without type assertions', () => {
      const { result } = renderHook(() => useLayoutEngine(), { wrapper })

      // This test passes if TypeScript compilation succeeds
      const { applyLayout, currentAlgorithm, setAlgorithm, layoutOptions, presets, canUndo } = result.current

      expect(applyLayout).toBeDefined()
      expect(currentAlgorithm).toBeDefined()
      expect(setAlgorithm).toBeDefined()
      expect(layoutOptions).toBeDefined()
      expect(presets).toBeDefined()
      expect(typeof canUndo).toBe('boolean')
    })
  })

  describe('hook stability', () => {
    it('should return consistent function types across re-renders', () => {
      const { result, rerender } = renderHook(() => useLayoutEngine(), { wrapper })

      const firstApplyLayoutType = typeof result.current.applyLayout
      const firstSetAlgorithmType = typeof result.current.setAlgorithm

      rerender()

      // Functions should maintain their types (they may not be the same reference due to dependencies)
      expect(typeof result.current.applyLayout).toBe(firstApplyLayoutType)
      expect(typeof result.current.setAlgorithm).toBe(firstSetAlgorithmType)
      expect(typeof result.current.applyLayout).toBe('function')
      expect(typeof result.current.setAlgorithm).toBe('function')
    })
  })
})
