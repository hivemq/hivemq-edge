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

const log = debug('workspace:layout:cola-constrained')

const DEFAULT_LAYER_GAP = 350 // Accounts for node height (~100px) + visual gap
const DEFAULT_NODE_GAP = 300 // Accounts for node width (~245px) + gap
const DEFAULT_NODE_WIDTH = 245
const DEFAULT_NODE_HEIGHT = 100

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
        }))

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

      const layoutedNodes: Node[] = layoutableNodes.map((node, index) => {
        const colaNode = colaNodes[index]
        const sourcePosition = isVertical ? Position.Bottom : Position.Right
        const targetPosition = isVertical ? Position.Top : Position.Left

        return {
          ...node,
          position: { x: colaNode.x, y: colaNode.y },
          sourcePosition,
          targetPosition,
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

    return {
      valid: errors.length === 0,
      errors: errors.length > 0 ? errors : undefined,
      warnings: warnings.length > 0 ? warnings : undefined,
    }
  }
}
