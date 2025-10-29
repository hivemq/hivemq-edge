/**
 * WebCola Force-Directed Layout Algorithm
 *
 * Uses WebCola's force-directed simulation to create organic, naturally-clustered layouts.
 * Nodes are positioned using physics-based forces with overlap removal.
 */

import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'
import * as cola from 'webcola'
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

const DEFAULT_LINK_DISTANCE = 350 // Accounts for node width (~245px) + gap
const DEFAULT_MAX_ITERATIONS = 1000
const DEFAULT_NODE_WIDTH = 245
const DEFAULT_NODE_HEIGHT = 100

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

      const gluedNodeIds = new Set<string>()
      if (constraints) {
        for (const id of constraints.gluedNodes.keys()) {
          gluedNodeIds.add(id)
        }
      }

      const layoutableNodes = nodes.filter((node) => !gluedNodeIds.has(node.id))
      const nodeIndexMap = new Map<string, number>()

      const colaNodes = layoutableNodes.map((node, index) => {
        nodeIndexMap.set(node.id, index)
        const width = node.width || node.measured?.width || DEFAULT_NODE_WIDTH
        const height = node.height || node.measured?.height || DEFAULT_NODE_HEIGHT
        return { x: node.position.x, y: node.position.y, width, height }
      })

      const colaLinks = edges
        .filter((edge) => nodeIndexMap.has(edge.source) && nodeIndexMap.has(edge.target))
        .map((edge) => ({
          source: nodeIndexMap.get(edge.source)!,
          target: nodeIndexMap.get(edge.target)!,
          length: colaOptions.linkDistance,
        }))

      const layout = new cola.Layout()
        .nodes(colaNodes)
        .links(colaLinks)
        .linkDistance(colaOptions.linkDistance)
        .convergenceThreshold(colaOptions.convergenceThreshold)
        .avoidOverlaps(colaOptions.avoidOverlaps)
        .handleDisconnected(colaOptions.handleDisconnected)

      layout.start(colaOptions.maxIterations, 0, 0, 0)

      const layoutedNodes: Node[] = layoutableNodes.map((node, index) => {
        const colaNode = colaNodes[index]
        return {
          ...node,
          position: { x: colaNode.x, y: colaNode.y },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        }
      })

      if (constraints) {
        for (const node of nodes) {
          if (constraints.gluedNodes.has(node.id)) {
            const gluedInfo = constraints.gluedNodes.get(node.id)!
            const parent = layoutedNodes.find((n) => n.id === gluedInfo.parentId)
            if (parent) {
              layoutedNodes.push({
                ...node,
                position: {
                  x: parent.position.x + gluedInfo.offset.x,
                  y: parent.position.y + gluedInfo.offset.y,
                },
                sourcePosition: parent.sourcePosition,
                targetPosition: parent.targetPosition,
              })
            }
          }
        }
      }

      const duration = performance.now() - startTime
      return {
        nodes: layoutedNodes,
        duration,
        success: true,
        metadata: { algorithm: this.type, nodeCount: layoutableNodes.length, edgeCount: edges.length },
      }
    } catch (error) {
      const duration = performance.now() - startTime
      console.error('WebCola force layout error:', error)
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

    return {
      valid: errors.length === 0,
      errors: errors.length > 0 ? errors : undefined,
      warnings: warnings.length > 0 ? warnings : undefined,
    }
  }
}
