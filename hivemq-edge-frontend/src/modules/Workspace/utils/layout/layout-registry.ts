/**
 * Layout Algorithm Registry
 *
 * Central registry for all available layout algorithms.
 * Uses factory pattern to provide algorithm discovery and instantiation.
 */

import type { LayoutAlgorithm, LayoutType, LayoutFeature } from '../../types/layout'
import debug from 'debug'

import { ManualLayoutAlgorithm } from './manual-layout'
import { DagreLayoutAlgorithm } from './dagre-layout'
import { RadialHubLayoutAlgorithm } from './radial-hub-layout'
import { ColaForceLayoutAlgorithm } from './cola-force-layout'
import { ColaConstrainedLayoutAlgorithm } from './cola-constrained-layout'

const log = debug('workspace:layout:registry')

/**
 * Registry for layout algorithms
 *
 * Manages registration and discovery of layout algorithms.
 * Initialized with default algorithms (dagre TB/LR).
 */
class LayoutRegistry {
  private algorithms = new Map<LayoutType, LayoutAlgorithm>()

  constructor() {
    this.registerDefaults()
  }

  /**
   * Register default layout algorithms
   */
  private registerDefaults() {
    // Register manual layout (default - no automatic layout)
    this.register(new ManualLayoutAlgorithm())

    // Register dagre algorithms
    this.register(new DagreLayoutAlgorithm('TB'))
    this.register(new DagreLayoutAlgorithm('LR'))

    // Register radial hub algorithm
    this.register(new RadialHubLayoutAlgorithm())

    // Register WebCola algorithms
    this.register(new ColaForceLayoutAlgorithm())
    this.register(new ColaConstrainedLayoutAlgorithm())
  }

  /**
   * Register a new layout algorithm
   *
   * @param algorithm - Layout algorithm to register
   * @throws Error if algorithm type is already registered
   */
  register(algorithm: LayoutAlgorithm): void {
    if (this.algorithms.has(algorithm.type)) {
      log(`Layout algorithm ${algorithm.type} is already registered, overwriting`)
    }
    this.algorithms.set(algorithm.type, algorithm)
  }

  /**
   * Unregister a layout algorithm
   *
   * @param type - Type of algorithm to unregister
   */
  unregister(type: LayoutType): void {
    this.algorithms.delete(type)
  }

  /**
   * Get a layout algorithm by type
   *
   * @param type - Layout algorithm type
   * @returns Layout algorithm instance or undefined if not found
   */
  get(type: LayoutType): LayoutAlgorithm | undefined {
    return this.algorithms.get(type)
  }

  /**
   * Get all registered layout algorithms
   *
   * @returns Array of all registered algorithms
   */
  getAll(): LayoutAlgorithm[] {
    return Array.from(this.algorithms.values())
  }

  /**
   * Get all algorithms that support a specific feature
   *
   * @param feature - Feature to filter by
   * @returns Array of algorithms supporting the feature
   */
  getByFeature(feature: LayoutFeature): LayoutAlgorithm[] {
    return this.getAll().filter((algo) => algo.supports(feature))
  }

  /**
   * Check if an algorithm type is registered
   *
   * @param type - Layout algorithm type
   * @returns true if registered
   */
  has(type: LayoutType): boolean {
    return this.algorithms.has(type)
  }

  /**
   * Get number of registered algorithms
   *
   * @returns Count of registered algorithms
   */
  get count(): number {
    return this.algorithms.size
  }
}

/**
 * Singleton instance of the layout registry
 */
export const layoutRegistry = new LayoutRegistry()

/**
 * Named export for testing purposes
 */
export { LayoutRegistry }
