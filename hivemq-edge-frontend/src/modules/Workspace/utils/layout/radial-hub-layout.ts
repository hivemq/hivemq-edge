/**
 * Radial Hub Layout Algorithm
 *
 * Creates a radial (circular) layout with EDGE at the center and nodes arranged
 * in concentric circles based on their type/distance from center.
 *
 * Layout structure:
 * - Layer 0 (Center): EDGE node
 * - Layer 1: COMBINER, PULSE nodes (close to center)
 * - Layer 2: ADAPTER, BRIDGE nodes (middle layer)
 * - Layer 3: DEVICE, HOST nodes (outer layer)
 */

import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'
import type {
  LayoutAlgorithm,
  LayoutType,
  LayoutOptions,
  LayoutResult,
  LayoutConstraints,
  RadialOptions,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'
import { NodeTypes } from '../../types'

/**
 * Default radial layout configuration
 */
const DEFAULT_CENTER_X = 400
const DEFAULT_CENTER_Y = 300
const DEFAULT_LAYER_SPACING = 500 // Accounts for node width (~245px) + gap

/**
 * Node type to layer mapping
 * Lower layer number = closer to center
 */
const NODE_LAYER_MAP: Record<string, number> = {
  [NodeTypes.EDGE_NODE]: 0, // Center
  [NodeTypes.COMBINER_NODE]: 1, // Inner ring
  [NodeTypes.PULSE_NODE]: 1, // Inner ring
  [NodeTypes.ADAPTER_NODE]: 2, // Middle ring
  [NodeTypes.BRIDGE_NODE]: 2, // Middle ring
  [NodeTypes.DEVICE_NODE]: 3, // Outer ring
  [NodeTypes.HOST_NODE]: 3, // Outer ring
  [NodeTypes.LISTENER_NODE]: 0, // With EDGE at center
}

/**
 * Radial Hub Layout Algorithm
 *
 * Arranges nodes in concentric circles around a central EDGE node.
 */
export class RadialHubLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string = 'Radial Hub Layout'
  readonly type: LayoutType = 'RADIAL_HUB' as LayoutType
  readonly description: string = 'Radial layout with EDGE at center, arranging nodes in concentric circles by type'
  readonly defaultOptions: RadialOptions

  constructor() {
    this.defaultOptions = {
      centerX: DEFAULT_CENTER_X,
      centerY: DEFAULT_CENTER_Y,
      layerSpacing: DEFAULT_LAYER_SPACING, // 500px to account for node widths
      startAngle: -Math.PI / 2, // Start at top (12 o'clock)
      animate: true,
      animationDuration: 300,
      fitView: true,
    }
  }

  /**
   * Apply radial hub layout algorithm
   */
  async apply(
    nodes: Node[],
    edges: Edge[],
    options: LayoutOptions,
    constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()
    const radialOptions = { ...this.defaultOptions, ...options } as RadialOptions

    try {
      if (nodes.length === 0) {
        return {
          nodes: [],
          duration: 0,
          success: true,
          metadata: {
            algorithm: this.type,
            nodeCount: 0,
            edgeCount: 0,
          },
        }
      }

      // 1. Identify constrained nodes (glued nodes will be positioned after their parents)
      const gluedNodeIds = new Set<string>()
      if (constraints) {
        for (const id of constraints.gluedNodes.keys()) {
          gluedNodeIds.add(id)
        }
      }

      // 2. Group nodes by layer (excluding glued nodes - they follow their parents)
      const layers = new Map<number, Node[]>()
      const layoutableNodes = nodes.filter((node) => !gluedNodeIds.has(node.id))

      for (const node of layoutableNodes) {
        const layer = NODE_LAYER_MAP[node.type || ''] ?? 2 // Default to middle layer
        if (!layers.has(layer)) {
          layers.set(layer, [])
        }
        layers.get(layer)!.push(node)
      }

      // 3. Calculate positions for each layer
      const layoutedNodes: Node[] = []
      const centerX = radialOptions.centerX || DEFAULT_CENTER_X
      const centerY = radialOptions.centerY || DEFAULT_CENTER_Y
      const layerSpacing = radialOptions.layerSpacing || DEFAULT_LAYER_SPACING
      const startAngle = radialOptions.startAngle || -Math.PI / 2

      // Sort layers by layer number
      const sortedLayers = Array.from(layers.entries()).sort((a, b) => a[0] - b[0])

      for (const [layerNum, layerNodes] of sortedLayers) {
        if (layerNum === 0) {
          // Center layer (EDGE + LISTENER)
          // Position EDGE at exact center
          const edgeNode = layerNodes.find((n) => n.type === NodeTypes.EDGE_NODE)
          if (edgeNode) {
            layoutedNodes.push({
              ...edgeNode,
              position: { x: centerX, y: centerY },
              sourcePosition: Position.Bottom,
              targetPosition: Position.Top,
            })
          }
          // Listeners stay glued to EDGE (handled later)
        } else {
          // Outer layers - arrange in circle
          const radius = layerNum * layerSpacing
          const nodeCount = layerNodes.length
          const angleStep = (2 * Math.PI) / nodeCount

          layerNodes.forEach((node, index) => {
            const angle = startAngle + angleStep * index
            const x = centerX + radius * Math.cos(angle)
            const y = centerY + radius * Math.sin(angle)

            // Calculate which direction the node should face (toward center)
            const sourcePosition = this.getSourcePositionForAngle(angle)
            const targetPosition = this.getTargetPositionForAngle(angle)

            layoutedNodes.push({
              ...node,
              position: { x, y },
              sourcePosition,
              targetPosition,
            })
          })
        }
      }

      // 4. Position glued nodes relative to their parents
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
        metadata: {
          algorithm: this.type,
          nodeCount: layoutableNodes.length,
          edgeCount: edges.length,
        },
      }
    } catch (error) {
      const duration = performance.now() - startTime
      console.error('Radial hub layout error:', error)

      return {
        nodes,
        duration,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error during radial hub layout',
      }
    }
  }

  /**
   * Calculate source position based on angle (away from center)
   */
  private getSourcePositionForAngle(angle: number): Position {
    // Normalize angle to 0-2π
    const normalized = ((angle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI)

    // Divide circle into 4 quadrants
    if (normalized < Math.PI / 4 || normalized >= (7 * Math.PI) / 4) {
      return Position.Right // East
    } else if (normalized < (3 * Math.PI) / 4) {
      return Position.Bottom // South
    } else if (normalized < (5 * Math.PI) / 4) {
      return Position.Left // West
    } else {
      return Position.Top // North
    }
  }

  /**
   * Calculate target position based on angle (toward center)
   */
  private getTargetPositionForAngle(angle: number): Position {
    // Target is opposite of source (toward center)
    const source = this.getSourcePositionForAngle(angle)
    switch (source) {
      case Position.Right:
        return Position.Left
      case Position.Left:
        return Position.Right
      case Position.Top:
        return Position.Bottom
      case Position.Bottom:
        return Position.Top
      default:
        return Position.Left
    }
  }

  /**
   * Check if this algorithm supports a specific feature
   */
  supports(feature: LayoutFeature): boolean {
    const supportedFeatures: LayoutFeature[] = ['RADIAL' as LayoutFeature, 'CONSTRAINED' as LayoutFeature]
    return supportedFeatures.includes(feature)
  }

  /**
   * Validate radial-specific options
   */
  validateOptions(options: LayoutOptions): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []
    const radialOpts = options as Partial<RadialOptions>

    // Check layer spacing
    if (radialOpts.layerSpacing !== undefined) {
      if (radialOpts.layerSpacing < 100) {
        warnings.push('layerSpacing < 100px may cause overlapping nodes')
      }
      if (radialOpts.layerSpacing > 800) {
        warnings.push('layerSpacing > 800px may create excessive spacing')
      }
    }

    return {
      valid: errors.length === 0,
      errors: errors.length > 0 ? errors : undefined,
      warnings: warnings.length > 0 ? warnings : undefined,
    }
  }
}
