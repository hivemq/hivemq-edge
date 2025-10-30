/**
 * Manual Layout Algorithm
 *
 * This is a no-op layout that keeps nodes exactly where they are.
 * Used as the default layout option when users want manual positioning.
 * Nodes remain in their current positions unless manually dragged.
 */

import type { Node, Edge } from '@xyflow/react'
import type {
  LayoutAlgorithm,
  LayoutType,
  LayoutOptions,
  LayoutResult,
  LayoutConstraints,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'

/**
 * Manual Layout Algorithm
 *
 * Returns nodes in their current positions without any automatic layout.
 * This is the default "no layout" option.
 */
export class ManualLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string = 'Manual'
  readonly type: LayoutType = 'MANUAL' as LayoutType
  readonly description: string = 'Manual positioning - nodes stay exactly where they are'
  readonly defaultOptions: LayoutOptions

  constructor() {
    this.defaultOptions = {
      animate: false, // No animation for manual layout
      animationDuration: 0,
      fitView: false, // Don't auto-fit view
    }
  }

  /**
   * Apply manual layout (no-op - returns nodes as-is)
   */
  async apply(
    nodes: Node[],
    _edges: Edge[],
    _options: LayoutOptions,
    _constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()

    // Simply return nodes in their current positions
    const layoutedNodes = nodes.map((node) => ({
      ...node,
      position: node.position, // Keep existing position
    }))

    const duration = performance.now() - startTime

    return {
      nodes: layoutedNodes,
      duration,
      success: true,
      metadata: {
        algorithm: this.type,
        nodeCount: nodes.length,
        edgeCount: _edges.length,
      },
    }
  }

  /**
   * Check if algorithm supports a feature
   */
  supports(_feature: LayoutFeature): boolean {
    // Manual layout doesn't support any automatic layout features
    return false
  }

  /**
   * Validate layout options
   */
  validateOptions(_options: LayoutOptions): ValidationResult {
    // Manual layout only supports common options, no validation needed
    return {
      valid: true,
      errors: [],
      warnings: [],
    }
  }
}
