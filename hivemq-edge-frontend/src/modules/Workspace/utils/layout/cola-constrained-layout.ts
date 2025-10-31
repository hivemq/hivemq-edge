/**
 * WebCola Constraint-Based Layout Algorithm
 *
 * Uses WebCola with explicit layer constraints to create strict hierarchical layouts.
 * Nodes are arranged in layers with alignment and flow direction constraints.
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
  ColaConstrainedOptions,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'
import { NodeTypes } from '../../types'
import {
  filterLayoutableNodes,
  createNodeIndexMap,
  nodesToColaNodes,
  edgesToColaLinks,
  applyGluedNodePositions,
  colaNodestoNodes,
  createValidationResult,
} from './cola-utils'

const log = debug('workspace:layout:cola-constrained')

const DEFAULT_LAYER_GAP = 350 // Accounts for node height (~100px) + visual gap
const DEFAULT_NODE_GAP = 300 // Accounts for node width (~245px) + gap

const NODE_LAYER_MAP: Record<string, number> = {
  [NodeTypes.EDGE_NODE]: 0,
  [NodeTypes.LISTENER_NODE]: 0,
  [NodeTypes.COMBINER_NODE]: 1,
  [NodeTypes.PULSE_NODE]: 1,
  [NodeTypes.ADAPTER_NODE]: 2,
  [NodeTypes.BRIDGE_NODE]: 2,
  [NodeTypes.DEVICE_NODE]: 3,
  [NodeTypes.HOST_NODE]: 3,
}

export class ColaConstrainedLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string = 'Hierarchical Constraint Layout'
  readonly type: LayoutType = 'COLA_CONSTRAINED' as LayoutType
  readonly description: string = 'Strict hierarchical layout with layer-based constraints and alignment'
  readonly defaultOptions: ColaConstrainedOptions

  constructor() {
    this.defaultOptions = {
      flowDirection: 'y',
      layerGap: DEFAULT_LAYER_GAP,
      nodeGap: DEFAULT_NODE_GAP,
      animate: true,
      animationDuration: 300,
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
    const colaOptions = { ...this.defaultOptions, ...options } as ColaConstrainedOptions

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
      const colaLinks = edgesToColaLinks(edges, nodeIndexMap)

      const isVertical = colaOptions.flowDirection === 'y'
      const axis = isVertical ? 'y' : 'x'

      const layerGroups = new Map<number, number[]>()
      for (let i = 0; i < layoutableNodes.length; i++) {
        const node = layoutableNodes[i]
        const layer = NODE_LAYER_MAP[node.type || ''] ?? 2
        if (!layerGroups.has(layer)) layerGroups.set(layer, [])
        layerGroups.get(layer)!.push(i)
      }

      const layout = new cola.Layout()
        .nodes(colaNodes)
        .links(colaLinks)
        .avoidOverlaps(true)
        .handleDisconnected(true)
        .convergenceThreshold(0.01)
        .flowLayout(axis, colaOptions.layerGap)

      layout.start(100, 0, 10, 10)

      // Convert back to ReactFlow nodes with appropriate positions
      const sourcePosition = isVertical ? Position.Bottom : Position.Right
      const targetPosition = isVertical ? Position.Top : Position.Left
      const layoutedNodes = colaNodestoNodes(layoutableNodes, colaNodes, sourcePosition, targetPosition)

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
      log('WebCola constraint layout error:', error)
      return {
        nodes,
        duration,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error during WebCola constraint layout',
      }
    }
  }

  supports(feature: LayoutFeature): boolean {
    const supportedFeatures: LayoutFeature[] = [
      'HIERARCHICAL' as LayoutFeature,
      'CONSTRAINED' as LayoutFeature,
      'DIRECTIONAL' as LayoutFeature,
      'OVERLAP_REMOVAL' as LayoutFeature,
    ]
    return supportedFeatures.includes(feature)
  }

  validateOptions(options: LayoutOptions): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []
    const colaOpts = options as Partial<ColaConstrainedOptions>

    if (colaOpts.layerGap !== undefined) {
      if (colaOpts.layerGap < 200) warnings.push('layerGap < 200px may cause overlapping layers')
      if (colaOpts.layerGap > 800) warnings.push('layerGap > 800px may cause excessive spacing')
    }

    if (colaOpts.nodeGap !== undefined) {
      if (colaOpts.nodeGap < 200) warnings.push('nodeGap < 200px may cause overlapping nodes')
      if (colaOpts.nodeGap > 600) warnings.push('nodeGap > 600px may cause excessive spacing')
    }

    return createValidationResult(errors, warnings)
  }
}
