/**
 * WebCola Force-Directed Layout Algorithm
 *
 * Uses WebCola's force-directed simulation to create organic, naturally-clustered layouts.
 * Nodes are positioned using physics-based forces with overlap removal.
 */

import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'
import * as cola from 'webcola'
import debug from 'debug'

import type {
  LayoutAlgorithm,
  LayoutType,
  LayoutOptions,
  LayoutResult,
  LayoutConstraints,
  ColaForceOptions,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'
import {
  filterLayoutableNodes,
  createNodeIndexMap,
  nodesToColaNodes,
  edgesToColaLinks,
  applyGluedNodePositions,
  colaNodestoNodes,
  createValidationResult,
} from './cola-utils'

const log = debug('workspace:layout:cola-force')

const DEFAULT_LINK_DISTANCE = 350 // Accounts for node width (~245px) + gap
const DEFAULT_MAX_ITERATIONS = 1000

/**
 * WebCola Force-Directed Layout Algorithm
 *
 * Creates organic layouts using physics-based force simulation.
 * Nodes naturally cluster based on connectivity.
 */
export class ColaForceLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string = 'Force-Directed Layout'
  readonly type: LayoutType = 'COLA_FORCE' as LayoutType
  readonly description: string = 'Physics-based layout with natural clustering and overlap removal'
  readonly defaultOptions: ColaForceOptions

  constructor() {
    this.defaultOptions = {
      linkDistance: DEFAULT_LINK_DISTANCE,
      avoidOverlaps: true,
      handleDisconnected: true,
      convergenceThreshold: 0.01,
      maxIterations: DEFAULT_MAX_ITERATIONS,
      animate: true,
      animationDuration: 500,
      fitView: true,
    }
  }

  async apply(
    nodes: Node[],
    edges: Edge[],
    options: LayoutOptions,
    constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()
    const colaOptions = { ...this.defaultOptions, ...options } as ColaForceOptions

    try {
      if (nodes.length === 0) {
        return {
          nodes: [],
          duration: 0,
          success: true,
          metadata: { algorithm: this.type, nodeCount: 0, edgeCount: 0 },
        }
      }

      // Use shared utilities
      const layoutableNodes = filterLayoutableNodes(nodes, constraints)
      const nodeIndexMap = createNodeIndexMap(layoutableNodes)
      const colaNodes = nodesToColaNodes(layoutableNodes)
      const colaLinks = edgesToColaLinks(edges, nodeIndexMap, colaOptions.linkDistance)

      const layout = new cola.Layout()
        .nodes(colaNodes)
        .links(colaLinks)
        .linkDistance(colaOptions.linkDistance)
        .convergenceThreshold(colaOptions.convergenceThreshold)
        .avoidOverlaps(colaOptions.avoidOverlaps)
        .handleDisconnected(colaOptions.handleDisconnected)

      layout.start(colaOptions.maxIterations, 0, 0, 0)

      // Convert back to ReactFlow nodes
      const layoutedNodes = colaNodestoNodes(layoutableNodes, colaNodes, Position.Right, Position.Left)

      // Apply glued node positioning
      const finalNodes = applyGluedNodePositions(nodes, layoutedNodes, constraints)

      const duration = performance.now() - startTime
      return {
        nodes: finalNodes,
        duration,
        success: true,
        metadata: { algorithm: this.type, nodeCount: layoutableNodes.length, edgeCount: edges.length },
      }
    } catch (error) {
      const duration = performance.now() - startTime
      log('WebCola force layout error:', error)
      return {
        nodes,
        duration,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error during WebCola force layout',
      }
    }
  }

  supports(feature: LayoutFeature): boolean {
    const supportedFeatures: LayoutFeature[] = [
      'FORCE_DIRECTED' as LayoutFeature,
      'CONSTRAINED' as LayoutFeature,
      'OVERLAP_REMOVAL' as LayoutFeature,
    ]
    return supportedFeatures.includes(feature)
  }

  validateOptions(options: LayoutOptions): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []
    const colaOpts = options as Partial<ColaForceOptions>

    if (colaOpts.linkDistance !== undefined) {
      if (colaOpts.linkDistance < 200) warnings.push('linkDistance < 200px may cause overlapping nodes')
      if (colaOpts.linkDistance > 800) warnings.push('linkDistance > 800px may cause excessive spacing')
    }

    if (colaOpts.maxIterations !== undefined) {
      if (colaOpts.maxIterations < 100) warnings.push('maxIterations < 100 may not converge properly')
      if (colaOpts.maxIterations > 5000) warnings.push('maxIterations > 5000 may cause slow performance')
    }

    return createValidationResult(errors, warnings)
  }
}
