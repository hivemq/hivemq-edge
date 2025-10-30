/**
 * WebCola Layout Utilities
 *
 * Shared utility functions for WebCola-based layout algorithms
 * to eliminate code duplication.
 */

import type { Node, Edge } from '@xyflow/react'
import { Position } from '@xyflow/react'
import type { LayoutConstraints } from '../../types/layout'

export const DEFAULT_NODE_WIDTH = 245
export const DEFAULT_NODE_HEIGHT = 100

/**
 * Filters out glued nodes from the layout process
 *
 * @param nodes - All nodes
 * @param constraints - Layout constraints containing glued nodes
 * @returns Nodes that should be laid out (non-glued nodes)
 */
export function filterLayoutableNodes(nodes: Node[], constraints?: LayoutConstraints): Node[] {
  if (!constraints) {
    return nodes
  }

  const gluedNodeIds = new Set<string>()
  for (const id of constraints.gluedNodes.keys()) {
    gluedNodeIds.add(id)
  }

  return nodes.filter((node) => !gluedNodeIds.has(node.id))
}

/**
 * Creates a map of node IDs to their indices in the layout array
 *
 * @param nodes - Nodes to create index map for
 * @returns Map of node ID to array index
 */
export function createNodeIndexMap(nodes: Node[]): Map<string, number> {
  const nodeIndexMap = new Map<string, number>()
  for (const [index, node] of nodes.entries()) {
    nodeIndexMap.set(node.id, index)
  }
  return nodeIndexMap
}

/**
 * Converts nodes to WebCola node format with dimensions
 *
 * @param nodes - ReactFlow nodes
 * @returns Array of WebCola nodes with position and dimensions
 */
export function nodesToColaNodes(nodes: Node[]): Array<{ x: number; y: number; width: number; height: number }> {
  return nodes.map((node) => {
    const width = node.width || node.measured?.width || DEFAULT_NODE_WIDTH
    const height = node.height || node.measured?.height || DEFAULT_NODE_HEIGHT
    return {
      x: node.position.x,
      y: node.position.y,
      width,
      height,
    }
  })
}

/**
 * Creates WebCola links from edges, filtering out edges with missing nodes
 *
 * @param edges - ReactFlow edges
 * @param nodeIndexMap - Map of node IDs to indices
 * @param linkDistance - Optional distance between connected nodes
 * @returns Array of WebCola links
 */
export function edgesToColaLinks(
  edges: Edge[],
  nodeIndexMap: Map<string, number>,
  linkDistance?: number
): Array<{ source: number; target: number; length?: number }> {
  return edges
    .filter((edge) => nodeIndexMap.has(edge.source) && nodeIndexMap.has(edge.target))
    .map((edge) => {
      const link: { source: number; target: number; length?: number } = {
        source: nodeIndexMap.get(edge.source)!,
        target: nodeIndexMap.get(edge.target)!,
      }
      if (linkDistance !== undefined) {
        link.length = linkDistance
      }
      return link
    })
}

/**
 * Applies glued node positioning based on their parent nodes
 *
 * @param originalNodes - All original nodes (including glued ones)
 * @param layoutedNodes - Nodes after layout (non-glued)
 * @param constraints - Layout constraints containing glued node information
 * @returns Complete array of nodes with glued nodes positioned relative to parents
 */
export function applyGluedNodePositions(
  originalNodes: Node[],
  layoutedNodes: Node[],
  constraints?: LayoutConstraints
): Node[] {
  if (!constraints) {
    return layoutedNodes
  }

  const result = [...layoutedNodes]

  for (const node of originalNodes) {
    if (constraints.gluedNodes.has(node.id)) {
      const gluedInfo = constraints.gluedNodes.get(node.id)!
      const parent = layoutedNodes.find((n) => n.id === gluedInfo.parentId)
      if (parent) {
        result.push({
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

  return result
}

/**
 * Converts WebCola nodes back to ReactFlow nodes with updated positions
 *
 * @param nodes - Original ReactFlow nodes
 * @param colaNodes - WebCola nodes with calculated positions
 * @param sourcePosition - Position for outgoing edges
 * @param targetPosition - Position for incoming edges
 * @returns Updated ReactFlow nodes
 */
export function colaNodestoNodes(
  nodes: Node[],
  colaNodes: Array<{ x: number; y: number; width: number; height: number }>,
  sourcePosition: Position = Position.Right,
  targetPosition: Position = Position.Left
): Node[] {
  return nodes.map((node, index) => {
    const colaNode = colaNodes[index]
    return {
      ...node,
      position: { x: colaNode.x, y: colaNode.y },
      sourcePosition,
      targetPosition,
    }
  })
}

/**
 * Creates a validation result from errors and warnings arrays
 *
 * @param errors - Array of error messages
 * @param warnings - Array of warning messages
 * @returns Validation result object
 */
export function createValidationResult(
  errors: string[],
  warnings: string[]
): {
  valid: boolean
  errors?: string[]
  warnings?: string[]
} {
  return {
    valid: errors.length === 0,
    errors: errors.length > 0 ? errors : undefined,
    warnings: warnings.length > 0 ? warnings : undefined,
  }
}
