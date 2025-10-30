/**
 * Dagre Layout Algorithm Implementation
 *
 * Implements hierarchical tree layouts using the dagre library.
 * Supports both vertical (top-to-bottom) and horizontal (left-to-right) layouts.
 */

import dagre from '@dagrejs/dagre'
import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'
import debug from 'debug'

import type {
  LayoutAlgorithm,
  LayoutType,
  LayoutOptions,
  LayoutResult,
  LayoutConstraints,
  DagreOptions,
  LayoutFeature,
  ValidationResult,
} from '../../types/layout'

const log = debug('workspace:layout:dagre')

/**
 * Default node dimensions for dagre layout
 */
const DEFAULT_NODE_WIDTH = 172
const DEFAULT_NODE_HEIGHT = 36

/**
 * Dagre layout algorithm implementation
 *
 * Provides hierarchical tree layouts with configurable direction and spacing.
 */
export class DagreLayoutAlgorithm implements LayoutAlgorithm {
  readonly name: string
  readonly type: LayoutType
  readonly description: string
  readonly defaultOptions: DagreOptions

  constructor(direction: 'TB' | 'LR') {
    this.type = direction === 'TB' ? ('DAGRE_TB' as LayoutType) : ('DAGRE_LR' as LayoutType)
    this.name = direction === 'TB' ? 'Vertical Tree Layout' : 'Horizontal Tree Layout'
    this.description =
      direction === 'TB'
        ? 'Top-to-bottom hierarchical tree layout, ideal for hub-spoke topologies'
        : 'Left-to-right hierarchical tree layout, good for wide screens and data flows'

    this.defaultOptions = {
      rankdir: direction,
      ranksep: direction === 'TB' ? 150 : 200,
      nodesep: 80,
      edgesep: 20,
      ranker: 'network-simplex',
      animate: true,
      animationDuration: 300,
      fitView: true,
    }
  }

  /**
   * Apply dagre layout algorithm to nodes and edges
   */
  async apply(
    nodes: Node[],
    edges: Edge[],
    options: LayoutOptions,
    constraints?: LayoutConstraints
  ): Promise<LayoutResult> {
    const startTime = performance.now()
    const dagreOptions = { ...this.defaultOptions, ...options } as DagreOptions

    try {
      // Validate we have nodes to layout
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

      // Debug logging
      log('🎯 Dagre Layout Debug:')
      log('  Total nodes:', nodes.length)
      log(
        '  Node types:',
        nodes.map((n) => ({ id: n.id, type: n.type }))
      )
      log('  Constraints:', constraints)

      // 1. Create dagre graph
      const g = new dagre.graphlib.Graph()
      g.setDefaultEdgeLabel(() => ({}))
      g.setGraph({
        rankdir: dagreOptions.rankdir,
        ranksep: dagreOptions.ranksep,
        nodesep: dagreOptions.nodesep,
        edgesep: dagreOptions.edgesep,
        ranker: dagreOptions.ranker,
        align: dagreOptions.align,
      })

      // 2. Identify constrained nodes (glued or fixed)
      const constrainedNodeIds = new Set<string>()
      const gluedPairs = new Map<string, string>() // parent -> child mapping
      let gluedChildIds = new Set<string>() // Track glued children

      if (constraints) {
        // For glued nodes, we'll handle them specially in dagre
        // Instead of excluding them, we'll include both parent and child
        // but maintain their relative positions
        for (const [childId, gluedInfo] of constraints.gluedNodes.entries()) {
          gluedPairs.set(gluedInfo.parentId, childId)
        }

        gluedChildIds = new Set(gluedPairs.values())

        log('  Glued pairs (parent -> child):', Array.from(gluedPairs.entries()))
        log('  Glued children:', Array.from(gluedChildIds))

        for (const id of constraints.fixedNodes) {
          constrainedNodeIds.add(id)
        }
      }

      // 3. Add nodes to dagre
      // For glued pairs (ADAPTER+DEVICE), we include BOTH but adjust their sizes
      const layoutableNodes = nodes.filter((node) => !constrainedNodeIds.has(node.id))

      log(
        '  Layoutable nodes:',
        layoutableNodes.map((n) => ({ id: n.id, type: n.type }))
      )

      for (const node of layoutableNodes) {
        const width = node.width || node.measured?.width || DEFAULT_NODE_WIDTH
        const height = node.height || node.measured?.height || DEFAULT_NODE_HEIGHT

        // If this node has a glued child (e.g., ADAPTER has DEVICE)
        // We need to account for the child's space in the layout
        const gluedChildId = gluedPairs.get(node.id)
        if (gluedChildId) {
          const childNode = nodes.find((n) => n.id === gluedChildId)
          if (childNode) {
            const childWidth = childNode.width || childNode.measured?.width || DEFAULT_NODE_WIDTH
            const childHeight = childNode.height || childNode.measured?.height || DEFAULT_NODE_HEIGHT

            // Expand the node size to include child space
            // DEVICE is typically below ADAPTER (negative Y offset)
            const gluedInfo = constraints?.gluedNodes.get(gluedChildId)
            const offsetY = Math.abs(gluedInfo?.offset.y || 0)

            const compoundWidth = Math.max(width, childWidth)
            const compoundHeight = height + offsetY + childHeight

            log(`  📦 Compound node: ${node.id} (${node.type}) + ${gluedChildId} (${childNode.type})`)
            log(`     Size: ${width}x${height} + ${childWidth}x${childHeight} = ${compoundWidth}x${compoundHeight}`)

            g.setNode(node.id, {
              width: compoundWidth,
              height: compoundHeight,
            })
            continue
          }
        }

        // Don't add glued children separately - they're handled with their parent
        const isGluedChild = constraints?.gluedNodes.has(node.id)
        if (!isGluedChild) {
          g.setNode(node.id, { width, height })
        }
      }

      // 4. Add edges (skip edges involving glued children since they're part of parent)
      for (const edge of edges) {
        const isSourceGluedChild = gluedChildIds.has(edge.source)
        const isTargetGluedChild = gluedChildIds.has(edge.target)

        if (
          !constrainedNodeIds.has(edge.source) &&
          !constrainedNodeIds.has(edge.target) &&
          !isSourceGluedChild &&
          !isTargetGluedChild
        ) {
          g.setEdge(edge.source, edge.target)
        }
      }

      // 5. Run dagre layout
      dagre.layout(g)

      // 6. Extract positions and transform
      const isHorizontal = dagreOptions.rankdir === 'LR' || dagreOptions.rankdir === 'RL'
      const layoutedNodes = nodes.map((node) => {
        // Handle glued/constrained nodes separately
        if (constraints && constraints.gluedNodes.has(node.id)) {
          return this.positionGluedNode(node, nodes, constraints)
        }

        // Get position from dagre
        const nodeWithPosition = g.node(node.id)
        if (!nodeWithPosition) {
          // Node was excluded from layout, keep original position
          return node
        }

        const width = node.width || node.measured?.width || DEFAULT_NODE_WIDTH
        const height = node.height || node.measured?.height || DEFAULT_NODE_HEIGHT

        // Dagre positions are center-based, React Flow uses top-left
        return {
          ...node,
          position: {
            x: nodeWithPosition.x - width / 2,
            y: nodeWithPosition.y - height / 2,
          },
          // Set handle positions based on layout direction
          targetPosition: isHorizontal ? Position.Left : Position.Top,
          sourcePosition: isHorizontal ? Position.Right : Position.Bottom,
        }
      })

      const duration = performance.now() - startTime

      // Count actual nodes laid out (excluding glued children that are part of parents)
      const actualLayoutedCount = layoutableNodes.filter((n) => !gluedChildIds.has(n.id)).length

      return {
        nodes: layoutedNodes,
        duration,
        success: true,
        metadata: {
          algorithm: this.type,
          nodeCount: actualLayoutedCount,
          edgeCount: edges.length,
        },
      }
    } catch (error) {
      const duration = performance.now() - startTime
      log('Dagre layout error:', error)

      return {
        nodes,
        duration,
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error during dagre layout',
      }
    }
  }

  /**
   * Position a glued node based on its parent's position
   */
  private positionGluedNode(node: Node, allNodes: Node[], constraints: LayoutConstraints): Node {
    const gluedInfo = constraints.gluedNodes.get(node.id)
    if (!gluedInfo) return node

    const parent = allNodes.find((n) => n.id === gluedInfo.parentId)
    if (!parent || !parent.position) return node

    return {
      ...node,
      position: {
        x: parent.position.x + gluedInfo.offset.x,
        y: parent.position.y + gluedInfo.offset.y,
      },
    }
  }

  /**
   * Check if this algorithm supports a specific feature
   */
  supports(feature: LayoutFeature): boolean {
    const supportedFeatures: LayoutFeature[] = [
      'HIERARCHICAL' as LayoutFeature,
      'DIRECTIONAL' as LayoutFeature,
      'CONSTRAINED' as LayoutFeature,
    ]
    return supportedFeatures.includes(feature)
  }

  /**
   * Validate dagre-specific options
   */
  validateOptions(options: LayoutOptions): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []
    const dagreOpts = options as Partial<DagreOptions>

    // Check rank separation
    if (dagreOpts.ranksep !== undefined) {
      if (dagreOpts.ranksep < 50) {
        warnings.push('ranksep < 50px may cause overlapping nodes')
      }
      if (dagreOpts.ranksep > 500) {
        warnings.push('ranksep > 500px may create excessive spacing')
      }
    }

    // Check node separation
    if (dagreOpts.nodesep !== undefined) {
      if (dagreOpts.nodesep < 20) {
        warnings.push('nodesep < 20px may cause overlapping nodes')
      }
      if (dagreOpts.nodesep > 300) {
        warnings.push('nodesep > 300px may create excessive spacing')
      }
    }

    // Check rank direction
    if (dagreOpts.rankdir && !['TB', 'LR', 'BT', 'RL'].includes(dagreOpts.rankdir)) {
      errors.push(`Invalid rankdir: ${dagreOpts.rankdir}. Must be TB, LR, BT, or RL`)
    }

    return {
      valid: errors.length === 0,
      errors: errors.length > 0 ? errors : undefined,
      warnings: warnings.length > 0 ? warnings : undefined,
    }
  }
}
