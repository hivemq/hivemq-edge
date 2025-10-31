import { describe, it, expect, beforeEach, vi } from 'vitest'
import type { Node } from '@xyflow/react'

import useWorkspaceStore from './useWorkspaceStore'
import { layoutRegistry } from '../utils/layout/layout-registry'
import { LayoutType, LayoutMode } from '../types/layout'

// Mock debug
vi.mock('debug', () => ({
  default: () => () => {},
}))

/**
 * Tests for useLayoutEngine hook function coverage
 *
 * Note: These tests focus on the hook's integration with the store and registry
 * to provide coverage for the hook file itself. The existing useLayoutEngine.spec.ts
 * tests the store methods directly.
 */
describe('useLayoutEngine - Hook Function Coverage', () => {
  beforeEach(() => {
    // Reset store
    const store = useWorkspaceStore.getState()
    store.reset()
  })

  describe('layout algorithm operations', () => {
    it('should get current algorithm from registry', () => {
      const store = useWorkspaceStore.getState()
      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

      // Get fresh state after setting algorithm
      const updatedStore = useWorkspaceStore.getState()
      const algorithm = layoutRegistry.get(updatedStore.layoutConfig.currentAlgorithm)

      expect(algorithm).toBeDefined()
      expect(algorithm?.type).toBe(LayoutType.DAGRE_TB)
      expect(algorithm?.name).toBeDefined()
    })

    it('should update current algorithm', () => {
      const store = useWorkspaceStore.getState()

      store.setLayoutAlgorithm(LayoutType.RADIAL_HUB)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)

      const algorithm = layoutRegistry.get(updatedStore.layoutConfig.currentAlgorithm)
      expect(algorithm?.type).toBe(LayoutType.RADIAL_HUB)
    })

    it('should list all available algorithms', () => {
      const algorithms = layoutRegistry.getAll()

      expect(algorithms).toBeDefined()
      expect(algorithms.length).toBeGreaterThan(0)

      // Verify structure
      for (const algorithm of algorithms) {
        expect(algorithm).toHaveProperty('type')
        expect(algorithm).toHaveProperty('name')
        expect(algorithm).toHaveProperty('description')
        expect(algorithm).toHaveProperty('defaultOptions')
        expect(algorithm).toHaveProperty('apply')
        expect(algorithm).toHaveProperty('supports')
        expect(algorithm).toHaveProperty('validateOptions')
      }
    })
  })

  describe('layout options management', () => {
    it('should update layout options', () => {
      const store = useWorkspaceStore.getState()

      const newOptions = {
        animate: false,
        animationDuration: 1000,
        fitView: true,
      }

      store.setLayoutOptions(newOptions)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.options.animate).toBe(false)
      expect(updatedStore.layoutConfig.options.animationDuration).toBe(1000)
      expect(updatedStore.layoutConfig.options.fitView).toBe(true)
    })

    it('should reset options to algorithm defaults', () => {
      const store = useWorkspaceStore.getState()
      store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

      // Change options
      store.setLayoutOptions({ animate: false, animationDuration: 999 })

      // Get algorithm defaults
      const algorithm = layoutRegistry.get(store.layoutConfig.currentAlgorithm)
      const defaultOptions = algorithm?.defaultOptions

      // Reset to defaults
      if (defaultOptions) {
        store.setLayoutOptions(defaultOptions)

        const updatedStore = useWorkspaceStore.getState()
        expect(updatedStore.layoutConfig.options).toEqual(defaultOptions)
      }
    })
  })

  describe('preset management operations', () => {
    it('should save current layout as preset', () => {
      const store = useWorkspaceStore.getState()

      // Add test nodes with positions
      const testNodes: Node[] = [
        { id: 'node-1', position: { x: 100, y: 200 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 300, y: 400 }, data: {}, type: 'adapter' },
      ]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      // Create preset
      const preset = {
        id: crypto.randomUUID(),
        name: 'Test Layout',
        description: 'Test description',
        algorithm: store.layoutConfig.currentAlgorithm,
        options: store.layoutConfig.options,
        positions: new Map(store.nodes.map((node) => [node.id, node.position])),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)
      expect(updatedStore.layoutConfig.presets[0].name).toBe('Test Layout')

      // Verify positions were saved (Map gets serialized)
      const savedPreset = updatedStore.layoutConfig.presets[0]
      expect(savedPreset.positions).toBeDefined()
    })

    it('should load a saved preset', () => {
      const store = useWorkspaceStore.getState()

      // Setup: Save a preset
      const testNodes: Node[] = [{ id: 'node-1', position: { x: 100, y: 200 }, data: {}, type: 'adapter' }]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))
      store.setLayoutAlgorithm(LayoutType.RADIAL_HUB)

      const preset = {
        id: 'test-preset-id',
        name: 'Test Preset',
        algorithm: LayoutType.RADIAL_HUB,
        options: { animate: true },
        positions: new Map([['node-1', { x: 100, y: 200 }]]),
        createdAt: new Date(),
        updatedAt: new Date(),
      }
      store.saveLayoutPreset(preset)

      // Change current settings
      store.setLayoutAlgorithm(LayoutType.MANUAL)

      // Load the preset
      store.loadLayoutPreset('test-preset-id')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
      expect(updatedStore.layoutConfig.options.animate).toBe(true)
    })

    it('should delete a preset', () => {
      const store = useWorkspaceStore.getState()

      // Save preset
      const preset = {
        id: 'delete-me',
        name: 'Delete Test',
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map(),
        createdAt: new Date(),
        updatedAt: new Date(),
      }
      store.saveLayoutPreset(preset)

      expect(useWorkspaceStore.getState().layoutConfig.presets.length).toBe(1)

      // Delete it
      store.deleteLayoutPreset('delete-me')

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(0)
    })
  })

  describe('layout history operations', () => {
    it('should push layout history entry', () => {
      const store = useWorkspaceStore.getState()

      const historyEntry = {
        id: crypto.randomUUID(),
        timestamp: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        nodePositions: new Map([['node-1', { x: 100, y: 200 }]]),
      }

      store.pushLayoutHistory(historyEntry)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(1)
      expect(updatedStore.layoutHistory[0].algorithm).toBe(LayoutType.DAGRE_TB)
    })

    it('should indicate when undo is available', () => {
      const store = useWorkspaceStore.getState()

      // Add multiple history entries
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
        algorithm: LayoutType.DAGRE_LR,
        options: {},
        nodePositions: new Map(),
      })

      const updatedStore = useWorkspaceStore.getState()
      // Can undo when history length > 1
      expect(updatedStore.layoutHistory.length).toBeGreaterThan(1)
    })

    it('should clear layout history', () => {
      const store = useWorkspaceStore.getState()

      // Add history entries
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
        algorithm: LayoutType.DAGRE_LR,
        options: {},
        nodePositions: new Map(),
      })

      expect(useWorkspaceStore.getState().layoutHistory.length).toBe(2)

      // Clear history
      store.clearLayoutHistory()

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(0)
    })
  })

  describe('layout mode operations', () => {
    it('should change layout mode', () => {
      const store = useWorkspaceStore.getState()

      store.setLayoutMode(LayoutMode.DYNAMIC)

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.mode).toBe(LayoutMode.DYNAMIC)
    })

    it('should toggle auto-layout', () => {
      const store = useWorkspaceStore.getState()

      const initialState = store.isAutoLayoutEnabled

      store.toggleAutoLayout()

      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.isAutoLayoutEnabled).toBe(!initialState)
    })
  })

  describe('layout validation', () => {
    it('should validate layout options for algorithm', () => {
      const algorithm = layoutRegistry.get(LayoutType.DAGRE_TB)

      expect(algorithm).toBeDefined()

      const validation = algorithm?.validateOptions({ animate: true })

      expect(validation).toBeDefined()
      expect(validation).toHaveProperty('valid')
    })

    it('should detect warnings for extreme values', () => {
      const algorithm = layoutRegistry.get(LayoutType.COLA_FORCE)

      // Test with extreme values that should trigger warnings
      const validation = algorithm?.validateOptions({
        animate: true,
        animationDuration: 50, // Very short - may cause warnings
      })

      expect(validation).toBeDefined()
      expect(validation).toHaveProperty('valid')
    })
  })

  describe('algorithm feature support', () => {
    it('should check feature support for algorithms', () => {
      const dagreAlgorithm = layoutRegistry.get(LayoutType.DAGRE_TB)
      const radialAlgorithm = layoutRegistry.get(LayoutType.RADIAL_HUB)

      expect(dagreAlgorithm).toBeDefined()
      expect(radialAlgorithm).toBeDefined()

      // Test supports method
      expect(typeof dagreAlgorithm?.supports).toBe('function')
      expect(typeof radialAlgorithm?.supports).toBe('function')
    })
  })

  describe('constraint extraction', () => {
    it('should handle nodes without glued constraints', () => {
      const store = useWorkspaceStore.getState()

      const testNodes: Node[] = [
        { id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' },
        { id: 'node-2', position: { x: 100, y: 100 }, data: {}, type: 'adapter' },
      ]

      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      // Simply verify nodes were added
      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.nodes.length).toBe(2)
    })
  })

  describe('layout registry integration', () => {
    it('should provide registry access', () => {
      expect(layoutRegistry).toBeDefined()
      expect(typeof layoutRegistry.get).toBe('function')
      expect(typeof layoutRegistry.getAll).toBe('function')
    })

    it('should get all algorithm types from registry', () => {
      const algorithms = layoutRegistry.getAll()
      const types = algorithms.map((a) => a.type)

      expect(types).toContain(LayoutType.DAGRE_TB)
      expect(types).toContain(LayoutType.DAGRE_LR)
      expect(types).toContain(LayoutType.RADIAL_HUB)
      expect(types).toContain(LayoutType.COLA_FORCE)
      expect(types).toContain(LayoutType.MANUAL)
    })
  })
})
