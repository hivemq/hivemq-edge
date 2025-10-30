import { describe, it, expect, beforeEach } from 'vitest'

import useWorkspaceStore from './useWorkspaceStore'
import { LayoutType, LayoutMode, type LayoutPreset, type LayoutHistoryEntry } from '../types/layout'

describe('useWorkspaceStore - Layout Actions', () => {
  beforeEach(() => {
    // Reset store to initial state before each test
    const store = useWorkspaceStore.getState()
    store.reset()
  })

  describe('setLayoutAlgorithm', () => {
    it('should set layout algorithm', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutConfig.currentAlgorithm).toBe(LayoutType.MANUAL)

      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.DAGRE_TB)
    })

    it('should change algorithm multiple times', () => {
      const store = useWorkspaceStore.getState()

      store.setLayoutAlgorithm(LayoutType.DAGRE_LR)
      expect(useWorkspaceStore.getState().layoutConfig.currentAlgorithm).toBe(LayoutType.DAGRE_LR)

      store.setLayoutAlgorithm(LayoutType.RADIAL_HUB)
      expect(useWorkspaceStore.getState().layoutConfig.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)

      store.setLayoutAlgorithm(LayoutType.COLA_FORCE)
      expect(useWorkspaceStore.getState().layoutConfig.currentAlgorithm).toBe(LayoutType.COLA_FORCE)
    })

    it('should preserve other layoutConfig properties', () => {
      const store = useWorkspaceStore.getState()

      const originalOptions = store.layoutConfig.options
      const originalPresets = store.layoutConfig.presets

      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.options).toEqual(originalOptions)
      expect(updatedStore.layoutConfig.presets).toEqual(originalPresets)
    })
  })

  describe('setLayoutMode', () => {
    it('should set layout mode', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutConfig.mode).toBe(LayoutMode.STATIC)

      store.setLayoutMode(LayoutMode.DYNAMIC)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.mode).toBe(LayoutMode.DYNAMIC)
    })

    it('should toggle between static and dynamic modes', () => {
      const store = useWorkspaceStore.getState()

      store.setLayoutMode(LayoutMode.DYNAMIC)
      expect(useWorkspaceStore.getState().layoutConfig.mode).toBe(LayoutMode.DYNAMIC)

      store.setLayoutMode(LayoutMode.STATIC)
      expect(useWorkspaceStore.getState().layoutConfig.mode).toBe(LayoutMode.STATIC)
    })

    it('should preserve other layoutConfig properties', () => {
      const store = useWorkspaceStore.getState()

      const originalAlgorithm = store.layoutConfig.currentAlgorithm
      const originalOptions = store.layoutConfig.options

      store.setLayoutMode(LayoutMode.DYNAMIC)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(originalAlgorithm)
      expect(updatedStore.layoutConfig.options).toEqual(originalOptions)
    })
  })

  describe('setLayoutOptions', () => {
    it('should update layout options', () => {
      const store = useWorkspaceStore.getState()

      const newOptions = {
        animate: false,
        animationDuration: 500,
      }

      store.setLayoutOptions(newOptions)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.options.animate).toBe(false)
      expect(updatedStore.layoutConfig.options.animationDuration).toBe(500)
    })

    it('should merge options with existing options', () => {
      const store = useWorkspaceStore.getState()

      // Original has animate: true, animationDuration: 300, fitView: true
      expect(store.layoutConfig.options.animate).toBe(true)
      expect(store.layoutConfig.options.animationDuration).toBe(300)
      expect(store.layoutConfig.options.fitView).toBe(true)

      // Update only animationDuration
      store.setLayoutOptions({ animationDuration: 1000 })

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.options.animate).toBe(true) // preserved
      expect(updatedStore.layoutConfig.options.animationDuration).toBe(1000) // updated
      expect(updatedStore.layoutConfig.options.fitView).toBe(true) // preserved
    })

    it('should handle multiple option updates', () => {
      const store = useWorkspaceStore.getState()

      store.setLayoutOptions({ animate: false })
      expect(useWorkspaceStore.getState().layoutConfig.options.animate).toBe(false)

      store.setLayoutOptions({ fitView: false })
      expect(useWorkspaceStore.getState().layoutConfig.options.fitView).toBe(false)
      expect(useWorkspaceStore.getState().layoutConfig.options.animate).toBe(false) // still false
    })

    it('should preserve other layoutConfig properties', () => {
      const store = useWorkspaceStore.getState()

      const originalAlgorithm = store.layoutConfig.currentAlgorithm
      const originalMode = store.layoutConfig.mode

      store.setLayoutOptions({ animate: false })

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(originalAlgorithm)
      expect(updatedStore.layoutConfig.mode).toBe(originalMode)
    })
  })

  describe('toggleAutoLayout', () => {
    it('should toggle auto layout from false to true', () => {
      const store = useWorkspaceStore.getState()

      expect(store.isAutoLayoutEnabled).toBe(false)

      store.toggleAutoLayout()

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.isAutoLayoutEnabled).toBe(true)
    })

    it('should toggle auto layout from true to false', () => {
      const store = useWorkspaceStore.getState()

      // Enable first
      store.toggleAutoLayout()
      expect(useWorkspaceStore.getState().isAutoLayoutEnabled).toBe(true)

      // Disable
      store.toggleAutoLayout()
      expect(useWorkspaceStore.getState().isAutoLayoutEnabled).toBe(false)
    })

    it('should toggle multiple times correctly', () => {
      const store = useWorkspaceStore.getState()

      expect(store.isAutoLayoutEnabled).toBe(false)

      store.toggleAutoLayout()
      expect(useWorkspaceStore.getState().isAutoLayoutEnabled).toBe(true)

      store.toggleAutoLayout()
      expect(useWorkspaceStore.getState().isAutoLayoutEnabled).toBe(false)

      store.toggleAutoLayout()
      expect(useWorkspaceStore.getState().isAutoLayoutEnabled).toBe(true)
    })
  })

  describe('saveLayoutPreset', () => {
    it('should save a layout preset', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutConfig.presets.length).toBe(0)

      const preset: LayoutPreset = {
        id: 'preset-1',
        name: 'My Layout',
        algorithm: LayoutType.DAGRE_TB,
        options: { animate: true },
        positions: new Map([['node-1', { x: 100, y: 200 }]]),
        createdAt: new Date('2025-10-30'),
        updatedAt: new Date('2025-10-30'),
      }

      store.saveLayoutPreset(preset)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)
      expect(updatedStore.layoutConfig.presets[0]).toEqual(preset)
    })

    it('should save multiple presets', () => {
      const store = useWorkspaceStore.getState()

      const preset1: LayoutPreset = {
        id: 'preset-1',
        name: 'Layout 1',
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      const preset2: LayoutPreset = {
        id: 'preset-2',
        name: 'Layout 2',
        algorithm: LayoutType.RADIAL_HUB,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset1)
      store.saveLayoutPreset(preset2)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(2)
      expect(updatedStore.layoutConfig.presets[0].id).toBe('preset-1')
      expect(updatedStore.layoutConfig.presets[1].id).toBe('preset-2')
    })

    it('should append presets without removing existing ones', () => {
      const store = useWorkspaceStore.getState()

      const preset1: LayoutPreset = {
        id: 'preset-1',
        name: 'First',
        algorithm: LayoutType.MANUAL,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset1)
      expect(useWorkspaceStore.getState().layoutConfig.presets.length).toBe(1)

      const preset2: LayoutPreset = {
        id: 'preset-2',
        name: 'Second',
        algorithm: LayoutType.DAGRE_LR,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset2)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(2)
      expect(updatedStore.layoutConfig.presets.find((p) => p.id === 'preset-1')).toBeDefined()
      expect(updatedStore.layoutConfig.presets.find((p) => p.id === 'preset-2')).toBeDefined()
    })
  })

  describe('loadLayoutPreset', () => {
    it('should load a layout preset', () => {
      const store = useWorkspaceStore.getState()

      const preset: LayoutPreset = {
        id: 'preset-1',
        name: 'My Layout',
        algorithm: LayoutType.RADIAL_HUB,
        options: { animate: false, animationDuration: 1000 },
        positions: new Map([
          ['node-1', { x: 100, y: 200 }],
          ['node-2', { x: 300, y: 400 }],
        ]),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      // Save preset first
      store.saveLayoutPreset(preset)

      // Change current settings
      store.setLayoutAlgorithm(LayoutType.MANUAL)
      store.setLayoutOptions({ animate: true })

      // Load preset
      store.loadLayoutPreset('preset-1')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
      expect(updatedStore.layoutConfig.options.animate).toBe(false)
      expect(updatedStore.layoutConfig.options.animationDuration).toBe(1000)
    })

    it('should do nothing if preset id does not exist', () => {
      const store = useWorkspaceStore.getState()

      const originalAlgorithm = store.layoutConfig.currentAlgorithm
      const originalOptions = store.layoutConfig.options

      store.loadLayoutPreset('non-existent-id')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(originalAlgorithm)
      expect(updatedStore.layoutConfig.options).toEqual(originalOptions)
    })

    it('should load correct preset when multiple presets exist', () => {
      const store = useWorkspaceStore.getState()

      const preset1: LayoutPreset = {
        id: 'preset-1',
        name: 'Layout 1',
        algorithm: LayoutType.DAGRE_TB,
        options: { animate: true },
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      const preset2: LayoutPreset = {
        id: 'preset-2',
        name: 'Layout 2',
        algorithm: LayoutType.COLA_FORCE,
        options: { animate: false },
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset1)
      store.saveLayoutPreset(preset2)

      // Load preset 2
      store.loadLayoutPreset('preset-2')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.COLA_FORCE)
      expect(updatedStore.layoutConfig.options.animate).toBe(false)
    })
  })

  describe('deleteLayoutPreset', () => {
    it('should delete a layout preset', () => {
      const store = useWorkspaceStore.getState()

      const preset: LayoutPreset = {
        id: 'preset-1',
        name: 'To Delete',
        algorithm: LayoutType.MANUAL,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset)
      expect(useWorkspaceStore.getState().layoutConfig.presets.length).toBe(1)

      store.deleteLayoutPreset('preset-1')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(0)
    })

    it('should delete only the specified preset', () => {
      const store = useWorkspaceStore.getState()

      const preset1: LayoutPreset = {
        id: 'preset-1',
        name: 'Keep This',
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      const preset2: LayoutPreset = {
        id: 'preset-2',
        name: 'Delete This',
        algorithm: LayoutType.RADIAL_HUB,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset1)
      store.saveLayoutPreset(preset2)
      expect(useWorkspaceStore.getState().layoutConfig.presets.length).toBe(2)

      // Delete preset 2
      store.deleteLayoutPreset('preset-2')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)
      expect(updatedStore.layoutConfig.presets[0].id).toBe('preset-1')
    })

    it('should do nothing if preset id does not exist', () => {
      const store = useWorkspaceStore.getState()

      const preset: LayoutPreset = {
        id: 'preset-1',
        name: 'Keep This',
        algorithm: LayoutType.MANUAL,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset)
      expect(useWorkspaceStore.getState().layoutConfig.presets.length).toBe(1)

      // Try to delete non-existent preset
      store.deleteLayoutPreset('non-existent')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)
      expect(updatedStore.layoutConfig.presets[0].id).toBe('preset-1')
    })
  })

  describe('pushLayoutHistory', () => {
    it('should push a history entry', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutHistory.length).toBe(0)

      const entry: LayoutHistoryEntry = {
        id: 'history-1',
        timestamp: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: { animate: true },
        nodePositions: new Map([['node-1', { x: 100, y: 200 }]]),
      }

      store.pushLayoutHistory(entry)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(1)
      expect(updatedStore.layoutHistory[0]).toEqual(entry)
    })

    it('should push multiple history entries', () => {
      const store = useWorkspaceStore.getState()

      const entry1: LayoutHistoryEntry = {
        id: 'history-1',
        timestamp: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        nodePositions: new Map(),
      }

      const entry2: LayoutHistoryEntry = {
        id: 'history-2',
        timestamp: new Date(),
        algorithm: LayoutType.RADIAL_HUB,
        options: {},
        nodePositions: new Map(),
      }

      store.pushLayoutHistory(entry1)
      store.pushLayoutHistory(entry2)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(2)
      expect(updatedStore.layoutHistory[0].id).toBe('history-1')
      expect(updatedStore.layoutHistory[1].id).toBe('history-2')
    })

    it('should limit history to 20 entries', () => {
      const store = useWorkspaceStore.getState()

      // Add 25 entries
      for (let i = 0; i < 25; i++) {
        store.pushLayoutHistory({
          id: `history-${i}`,
          timestamp: new Date(),
          algorithm: LayoutType.MANUAL,
          options: {},
          nodePositions: new Map(),
        })
      }

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(20)

      // Should have removed the first 5 entries
      expect(updatedStore.layoutHistory[0].id).toBe('history-5')
      expect(updatedStore.layoutHistory[19].id).toBe('history-24')
    })

    it('should maintain order of history entries', () => {
      const store = useWorkspaceStore.getState()

      const entries = [
        { id: 'first', algorithm: LayoutType.DAGRE_TB },
        { id: 'second', algorithm: LayoutType.DAGRE_LR },
        { id: 'third', algorithm: LayoutType.RADIAL_HUB },
      ]

      entries.forEach((entry) => {
        store.pushLayoutHistory({
          id: entry.id,
          timestamp: new Date(),
          algorithm: entry.algorithm,
          options: {},
          nodePositions: new Map(),
        })
      })

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory[0].id).toBe('first')
      expect(updatedStore.layoutHistory[1].id).toBe('second')
      expect(updatedStore.layoutHistory[2].id).toBe('third')
    })
  })

  describe('clearLayoutHistory', () => {
    it('should clear all history entries', () => {
      const store = useWorkspaceStore.getState()

      // Add some history entries
      store.pushLayoutHistory({
        id: 'history-1',
        timestamp: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        nodePositions: new Map(),
      })
      store.pushLayoutHistory({
        id: 'history-2',
        timestamp: new Date(),
        algorithm: LayoutType.RADIAL_HUB,
        options: {},
        nodePositions: new Map(),
      })

      expect(useWorkspaceStore.getState().layoutHistory.length).toBe(2)

      // Clear history
      store.clearLayoutHistory()

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(0)
      expect(updatedStore.layoutHistory).toEqual([])
    })

    it('should work when history is already empty', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutHistory.length).toBe(0)

      // Clear empty history
      store.clearLayoutHistory()

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(0)
    })

    it('should not affect other store properties', () => {
      const store = useWorkspaceStore.getState()

      // Set some state
      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)
      store.setLayoutOptions({ animate: false })

      // Add history
      store.pushLayoutHistory({
        id: 'history-1',
        timestamp: new Date(),
        algorithm: LayoutType.MANUAL,
        options: {},
        nodePositions: new Map(),
      })

      const beforeClear = useWorkspaceStore.getState()
      const algorithmBeforeClear = beforeClear.layoutConfig.currentAlgorithm
      const optionsBeforeClear = beforeClear.layoutConfig.options

      // Clear history
      store.clearLayoutHistory()

      const afterClear = useWorkspaceStore.getState()
      expect(afterClear.layoutConfig.currentAlgorithm).toBe(algorithmBeforeClear)
      expect(afterClear.layoutConfig.options).toEqual(optionsBeforeClear)
    })
  })

  describe('integration scenarios', () => {
    it('should handle complete workflow: set algorithm, options, save preset, load preset', () => {
      const store = useWorkspaceStore.getState()

      // Step 1: Configure layout
      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)
      store.setLayoutOptions({ animate: true, animationDuration: 500 })

      // Step 2: Save as preset
      const preset: LayoutPreset = {
        id: 'workflow-preset',
        name: 'My Workflow',
        algorithm: useWorkspaceStore.getState().layoutConfig.currentAlgorithm,
        options: useWorkspaceStore.getState().layoutConfig.options,
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }
      store.saveLayoutPreset(preset)

      // Step 3: Change settings
      store.setLayoutAlgorithm(LayoutType.MANUAL)
      store.setLayoutOptions({ animate: false })

      // Step 4: Load preset back
      store.loadLayoutPreset('workflow-preset')

      const final = useWorkspaceStore.getState()
      expect(final.layoutConfig.currentAlgorithm).toBe(LayoutType.DAGRE_TB)
      expect(final.layoutConfig.options.animate).toBe(true)
      expect(final.layoutConfig.options.animationDuration).toBe(500)
    })

    it('should handle history tracking workflow', () => {
      const store = useWorkspaceStore.getState()

      // Make several layout changes, tracking each in history
      const algorithms = [LayoutType.DAGRE_TB, LayoutType.RADIAL_HUB, LayoutType.COLA_FORCE]

      algorithms.forEach((algorithm, index) => {
        store.setLayoutAlgorithm(algorithm)
        store.pushLayoutHistory({
          id: `change-${index}`,
          timestamp: new Date(),
          algorithm,
          options: useWorkspaceStore.getState().layoutConfig.options,
          nodePositions: new Map(),
        })
      })

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(3)
      expect(updatedStore.layoutHistory[0].algorithm).toBe(LayoutType.DAGRE_TB)
      expect(updatedStore.layoutHistory[1].algorithm).toBe(LayoutType.RADIAL_HUB)
      expect(updatedStore.layoutHistory[2].algorithm).toBe(LayoutType.COLA_FORCE)
    })
  })
})
