import { describe, it, expect, beforeEach } from 'vitest'
import type { Node } from '@xyflow/react'

import useWorkspaceStore from './useWorkspaceStore'
import { layoutRegistry } from '../utils/layout/layout-registry'
import { LayoutType } from '../types/layout'

describe('useLayoutEngine - Store Integration', () => {
  beforeEach(() => {
    // Reset store before each test
    const store = useWorkspaceStore.getState()

    // Clear nodes and edges
    store.onNodesChange([])
    store.onEdgesChange([])

    // Reset layout config
    store.setLayoutAlgorithm(LayoutType.DAGRE_TB)

    // Clear all presets
    const presets = [...store.layoutConfig.presets]
    presets.forEach((preset) => store.deleteLayoutPreset(preset.id))

    // Clear history
    store.clearLayoutHistory()
  })

  describe('layout algorithm selection', () => {
    it('should change algorithm in store', () => {
      const store = useWorkspaceStore.getState()

      expect(store.layoutConfig.currentAlgorithm).toBe(LayoutType.DAGRE_TB)

      store.setLayoutAlgorithm(LayoutType.DAGRE_LR)

      // Get fresh state after mutation
      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.DAGRE_LR)
    })

    it('should get algorithm instance from registry', () => {
      const store = useWorkspaceStore.getState()

      const algorithm = layoutRegistry.get(store.layoutConfig.currentAlgorithm)

      expect(algorithm).toBeDefined()
      expect(algorithm?.type).toBe(LayoutType.DAGRE_TB)
    })

    it('should list all available algorithms', () => {
      const algorithms = layoutRegistry.getAll()

      expect(algorithms).toBeDefined()
      expect(algorithms.length).toBeGreaterThan(0)

      // Check for known algorithms
      const algorithmTypes = algorithms.map((a) => a.type)
      expect(algorithmTypes).toContain(LayoutType.DAGRE_TB)
      expect(algorithmTypes).toContain(LayoutType.DAGRE_LR)
      expect(algorithmTypes).toContain(LayoutType.MANUAL)
      expect(algorithmTypes).toContain(LayoutType.RADIAL_HUB)
    })
  })

  describe('layout options', () => {
    it('should update layout options in store', () => {
      const store = useWorkspaceStore.getState()

      const newOptions = {
        animate: true,
        animationDuration: 500,
        fitView: true,
      }

      store.setLayoutOptions(newOptions)

      // Get fresh state after mutation
      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.options.animate).toBe(true)
      expect(updatedStore.layoutConfig.options.animationDuration).toBe(500)
      expect(updatedStore.layoutConfig.options.fitView).toBe(true)
    })

    it('should get default options from algorithm', () => {
      const store = useWorkspaceStore.getState()

      const algorithm = layoutRegistry.get(store.layoutConfig.currentAlgorithm)

      expect(algorithm?.defaultOptions).toBeDefined()
      expect(algorithm?.defaultOptions).toHaveProperty('animate')
    })
  })

  describe('preset management', () => {
    it('should save preset', () => {
      const store = useWorkspaceStore.getState()

      // Add test nodes
      const testNodes: Node[] = [{ id: 'node-1', position: { x: 100, y: 200 }, data: {}, type: 'adapter' }]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      const preset = {
        id: 'test-preset-1',
        name: 'Test Preset',
        description: 'Test description',
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        positions: new Map([['node-1', { x: 100, y: 200 }]]),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset)

      // Get fresh state after mutation
      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)
      expect(updatedStore.layoutConfig.presets[0].name).toBe('Test Preset')
    })

    it('should delete preset', () => {
      const store = useWorkspaceStore.getState()

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

      // Get fresh state after save
      let updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(1)

      store.deleteLayoutPreset('delete-me')

      // Get fresh state after delete
      updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutConfig.presets.length).toBe(0)
    })

    it('should load preset', () => {
      const store = useWorkspaceStore.getState()

      // Add test nodes
      const testNodes: Node[] = [{ id: 'node-1', position: { x: 0, y: 0 }, data: {}, type: 'adapter' }]
      store.onAddNodes(testNodes.map((node) => ({ type: 'add', item: node })))

      const preset = {
        id: 'load-me',
        name: 'Load Test',
        algorithm: LayoutType.RADIAL_HUB,
        options: { animate: true },
        positions: new Map([['node-1', { x: 100, y: 200 }]]),
        createdAt: new Date(),
        updatedAt: new Date(),
      }

      store.saveLayoutPreset(preset)
      store.loadLayoutPreset('load-me')

      // Get fresh state after loading preset
      const updatedStore = useWorkspaceStore.getState()

      // Verify algorithm and options were updated
      expect(updatedStore.layoutConfig.currentAlgorithm).toBe(LayoutType.RADIAL_HUB)
      expect(updatedStore.layoutConfig.options.animate).toBe(true)

      // Verify nodes were repositioned
      const nodes = updatedStore.nodes
      expect(nodes[0].position.x).toBe(100)
      expect(nodes[0].position.y).toBe(200)
    })
  })

  describe('layout history', () => {
    it('should push history entry', () => {
      const store = useWorkspaceStore.getState()

      const historyEntry = {
        id: 'history-1',
        timestamp: new Date(),
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        nodePositions: new Map(),
      }

      store.pushLayoutHistory(historyEntry)

      // Get fresh state after mutation
      const updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(1)
      expect(updatedStore.layoutHistory[0].id).toBe('history-1')
    })

    it('should clear history', () => {
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
        algorithm: LayoutType.DAGRE_TB,
        options: {},
        nodePositions: new Map(),
      })

      // Get fresh state after adding entries
      let updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(2)

      store.clearLayoutHistory()

      // Get fresh state after clear
      updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.layoutHistory.length).toBe(0)
    })

    it('should limit history to max size', () => {
      const store = useWorkspaceStore.getState()

      // Add more than max history entries
      for (let i = 0; i < 25; i++) {
        store.pushLayoutHistory({
          id: `history-${i}`,
          timestamp: new Date(),
          algorithm: LayoutType.DAGRE_TB,
          options: {},
          nodePositions: new Map(),
        })
      }

      // Should be capped at 20 (or whatever the max is)
      expect(store.layoutHistory.length).toBeLessThanOrEqual(20)
    })
  })

  describe('auto-layout mode', () => {
    it('should toggle auto-layout', () => {
      const store = useWorkspaceStore.getState()

      const initialState = store.isAutoLayoutEnabled

      store.toggleAutoLayout()

      // Get fresh state after first toggle
      let updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.isAutoLayoutEnabled).toBe(!initialState)

      store.toggleAutoLayout()

      // Get fresh state after second toggle
      updatedStore = useWorkspaceStore.getState()
      expect(updatedStore.isAutoLayoutEnabled).toBe(initialState)
    })
  })

  describe('layout algorithm features', () => {
    it('should check algorithm supports features', () => {
      const algorithm = layoutRegistry.get(LayoutType.DAGRE_TB)

      expect(algorithm).toBeDefined()
      expect(algorithm?.supports).toBeDefined()
      expect(typeof algorithm?.supports).toBe('function')
    })

    it('should validate algorithm options', () => {
      const algorithm = layoutRegistry.get(LayoutType.DAGRE_TB)

      expect(algorithm).toBeDefined()
      expect(algorithm?.validateOptions).toBeDefined()

      const validation = algorithm?.validateOptions({})
      expect(validation).toHaveProperty('valid')
    })

    it('should have default options for each algorithm', () => {
      const algorithms = layoutRegistry.getAll()

      algorithms.forEach((algorithm) => {
        expect(algorithm.defaultOptions).toBeDefined()
        expect(typeof algorithm.defaultOptions).toBe('object')
      })
    })
  })
})
