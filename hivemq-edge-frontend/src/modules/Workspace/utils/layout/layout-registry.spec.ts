/**
 * Unit tests for Layout Registry
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { LayoutRegistry, layoutRegistry } from './layout-registry'
import { DagreLayoutAlgorithm } from './dagre-layout'
import type { LayoutType, LayoutFeature } from '../../types/layout'

describe('LayoutRegistry', () => {
  let registry: LayoutRegistry

  beforeEach(() => {
    registry = new LayoutRegistry()
  })

  describe('constructor', () => {
    it('should initialize with default algorithms', () => {
      expect(registry.count).toBe(6) // MANUAL, TB, LR, RADIAL_HUB, COLA_FORCE, COLA_CONSTRAINED
      expect(registry.has('MANUAL' as LayoutType)).toBe(true)
      expect(registry.has('DAGRE_TB' as LayoutType)).toBe(true)
      expect(registry.has('DAGRE_LR' as LayoutType)).toBe(true)
      expect(registry.has('RADIAL_HUB' as LayoutType)).toBe(true)
      expect(registry.has('COLA_FORCE' as LayoutType)).toBe(true)
      expect(registry.has('COLA_CONSTRAINED' as LayoutType)).toBe(true)
    })
  })

  describe('register', () => {
    it('should register a new algorithm', () => {
      const customAlgo = new DagreLayoutAlgorithm('TB')
      registry.register(customAlgo)

      expect(registry.has(customAlgo.type)).toBe(true)
      expect(registry.get(customAlgo.type)).toBe(customAlgo)
    })

    it('should overwrite existing algorithm with warning', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn')
      const algo1 = new DagreLayoutAlgorithm('TB')
      const algo2 = new DagreLayoutAlgorithm('TB')

      registry.register(algo1)
      registry.register(algo2)

      expect(consoleWarnSpy).toHaveBeenCalled()
      expect(registry.get(algo1.type)).toBe(algo2)
    })
  })

  describe('unregister', () => {
    it('should remove an algorithm', () => {
      const algo = new DagreLayoutAlgorithm('TB')
      registry.register(algo)

      expect(registry.has(algo.type)).toBe(true)

      registry.unregister(algo.type)

      expect(registry.has(algo.type)).toBe(false)
    })
  })

  describe('get', () => {
    it('should return algorithm by type', () => {
      const algo = registry.get('DAGRE_TB' as LayoutType)

      expect(algo).toBeDefined()
      expect(algo?.type).toBe('DAGRE_TB' as LayoutType)
    })

    it('should return undefined for non-existent type', () => {
      const algo = registry.get('NON_EXISTENT' as LayoutType)

      expect(algo).toBeUndefined()
    })
  })

  describe('getAll', () => {
    it('should return all registered algorithms', () => {
      const algorithms = registry.getAll()

      expect(algorithms).toHaveLength(6) // MANUAL, TB, LR, RADIAL_HUB, COLA_FORCE, COLA_CONSTRAINED
      expect(algorithms[0]).toBeDefined()
      expect(algorithms[1]).toBeDefined()
      expect(algorithms[2]).toBeDefined()
      expect(algorithms[3]).toBeDefined()
      expect(algorithms[4]).toBeDefined()
      expect(algorithms[5]).toBeDefined()
    })
  })

  describe('getByFeature', () => {
    it('should return algorithms supporting hierarchical layout', () => {
      const algorithms = registry.getByFeature('HIERARCHICAL' as LayoutFeature)

      expect(algorithms.length).toBeGreaterThan(0)
      algorithms.forEach((algo) => {
        expect(algo.supports('HIERARCHICAL' as LayoutFeature)).toBe(true)
      })
    })

    it('should return force-directed algorithms', () => {
      const algorithms = registry.getByFeature('FORCE_DIRECTED' as LayoutFeature)

      // ColaForceLayoutAlgorithm should support this feature
      expect(algorithms.length).toBeGreaterThan(0)
    })
  })

  describe('has', () => {
    it('should return true for registered algorithm', () => {
      expect(registry.has('DAGRE_TB' as LayoutType)).toBe(true)
    })

    it('should return false for non-registered algorithm', () => {
      expect(registry.has('NON_EXISTENT' as LayoutType)).toBe(false)
    })
  })

  describe('count', () => {
    it('should return number of registered algorithms', () => {
      expect(registry.count).toBe(6) // MANUAL, TB, LR, RADIAL_HUB, COLA_FORCE, COLA_CONSTRAINED
    })

    it('should update after registration', () => {
      const initialCount = registry.count
      const newAlgo = new DagreLayoutAlgorithm('TB')
      registry.register(newAlgo)

      expect(registry.count).toBe(initialCount) // Same count since TB already exists
    })
  })

  describe('singleton instance', () => {
    it('should export a singleton instance', () => {
      expect(layoutRegistry).toBeInstanceOf(LayoutRegistry)
      expect(layoutRegistry.count).toBeGreaterThan(0)
    })

    it('should be the same instance across imports', () => {
      const algo1 = layoutRegistry.get('DAGRE_TB' as LayoutType)
      const algo2 = layoutRegistry.get('DAGRE_TB' as LayoutType)

      expect(algo1).toBe(algo2)
    })
  })
})
