/**
 * Constraint Utilities for Workspace Layout
 *
 * This module provides utilities for extracting and managing layout constraints
 * from the workspace graph structure.
 */

import type { Node, Edge, XYPosition } from '@xyflow/react'
import type { LayoutConstraints, GluedNodeInfo } from '../../types/layout'
import type { NodeDeviceType } from '../../types'
import { NodeTypes } from '../../types'
import { gluedNodeDefinition } from '../nodes-utils'

/**
 * Extract layout constraints from current graph structure
 *
 * This function analyzes the nodes and edges to identify:
 * - Glued nodes (e.g., listeners that must stay attached to edge node)
 * - Fixed nodes (nodes that should not be moved)
 * - Group nodes and their children (for nested layouts)
 *
 * @param nodes - Array of workspace nodes
 * @param _edges - Array of workspace edges (unused but kept for API consistency)
 * @returns Layout constraints object
 *
 * @example
 * ```typescript
 * const constraints = extractLayoutConstraints(nodes, edges)
 * const result = await algorithm.apply(nodes, edges, options, constraints)
 * ```
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const extractLayoutConstraints = (nodes: Node[], _edges: Edge[]): LayoutConstraints => {
  const gluedNodes = new Map<string, GluedNodeInfo>()
  const fixedNodes = new Set<string>()
  const groupNodes = new Map<string, string[]>()

  // Find glued nodes (e.g., listeners glued to edge node, devices glued to adapters)
  // These nodes must maintain a fixed offset from their parent
  //
  // The gluedNodeDefinition is bidirectional, but we only want to identify CHILDREN,
  // not parents. A child is a node with a negative offset (positioned before/above parent)
  for (const node of nodes) {
    if (node.type && Object.keys(gluedNodeDefinition).includes(node.type)) {
      const [parentType, offset, handle] = gluedNodeDefinition[node.type as NodeTypes]

      // Only treat this as a glued child if the offset indicates it's positioned
      // relative to a parent (typically negative offset means child comes before parent)
      // Examples:
      // - DEVICE_NODE has positive offset (200) → glued to ADAPTER (child)
      // - ADAPTER_NODE has negative offset (-200) → NOT a glued child, it's the parent!
      // - LISTENER_NODE has negative offset → glued to EDGE (child)
      // - HOST_NODE has positive offset → glued to BRIDGE (child)
      const isGluedChild = offset > 0 // Positive offset = positioned after parent = is a child

      if (isGluedChild) {
        let parent: Node | undefined

        // Special handling for DEVICE nodes - they have sourceAdapterId that links to specific ADAPTER
        if (node.type === NodeTypes.DEVICE_NODE) {
          const { data } = node as NodeDeviceType
          const sourceAdapterId = data.sourceAdapterId
          parent = nodes.find((n) => n.type === parentType && n.data?.id === sourceAdapterId)
        }

        // Fallback: find first node of parent type (for other glued nodes like LISTENER)
        parent ??= nodes.find((n) => n.type === parentType)

        if (parent) {
          gluedNodes.set(node.id, {
            parentId: parent.id,
            // For now, use fixed offset; could be calculated from current positions
            offset: { x: offset, y: offset } as XYPosition,
            handle,
          })
        }
      }
    }
  }

  // Find group nodes and their children
  // Group children must be laid out within group boundaries
  for (const node of nodes) {
    if (node.type === NodeTypes.CLUSTER_NODE && node.data?.childrenNodeIds) {
      groupNodes.set(node.id, node.data.childrenNodeIds as string[])
    }
  }

  // Fixed nodes could be added here if needed
  // For example, always keep edge node at origin
  // fixedNodes.add(IdStubs.EDGE_NODE)

  return {
    gluedNodes,
    fixedNodes,
    groupNodes,
  }
}

/**
 * Check if a node is constrained (cannot be freely positioned)
 *
 * @param nodeId - ID of the node to check
 * @param constraints - Layout constraints object
 * @returns true if node is constrained (glued or fixed)
 *
 * @example
 * ```typescript
 * if (isNodeConstrained(node.id, constraints)) {
 *   // Skip this node in layout algorithm
 * }
 * ```
 */
export const isNodeConstrained = (nodeId: string, constraints: LayoutConstraints): boolean => {
  return constraints.gluedNodes.has(nodeId) || constraints.fixedNodes.has(nodeId)
}

/**
 * Get the parent node ID for a glued node
 *
 * @param nodeId - ID of the glued node
 * @param constraints - Layout constraints object
 * @returns Parent node ID or undefined if not glued
 */
export const getGluedParentId = (nodeId: string, constraints: LayoutConstraints): string | undefined => {
  return constraints.gluedNodes.get(nodeId)?.parentId
}

/**
 * Calculate position for a glued node based on its parent's position
 *
 * @param gluedNode - The glued node
 * @param parentNode - The parent node
 * @param gluedInfo - Glued node constraint information
 * @returns Updated node with calculated position
 */
export const calculateGluedPosition = (gluedNode: Node, parentNode: Node, gluedInfo: GluedNodeInfo): Node => {
  return {
    ...gluedNode,
    position: {
      x: parentNode.position.x + gluedInfo.offset.x,
      y: parentNode.position.y + gluedInfo.offset.y,
    },
  }
}

/**
 * Filter nodes to exclude constrained nodes for layout algorithms
 *
 * @param nodes - All nodes
 * @param constraints - Layout constraints
 * @returns Array of nodes that can be freely positioned by layout
 */
export const getLayoutableNodes = (nodes: Node[], constraints: LayoutConstraints): Node[] => {
  const constrainedIds = new Set<string>()
  for (const id of constraints.gluedNodes.keys()) {
    constrainedIds.add(id)
  }
  for (const id of constraints.fixedNodes) {
    constrainedIds.add(id)
  }

  return nodes.filter((node) => !constrainedIds.has(node.id))
}

/**
 * Apply glued node positions after layout has been computed
 *
 * This function takes layout results and updates glued nodes to maintain
 * their offset relationships with parent nodes.
 *
 * @param layoutedNodes - Nodes after layout algorithm
 * @param constraints - Layout constraints
 * @returns Nodes with glued positions recalculated
 */
export const applyGluedPositions = (layoutedNodes: Node[], constraints: LayoutConstraints): Node[] => {
  return layoutedNodes.map((node) => {
    const gluedInfo = constraints.gluedNodes.get(node.id)
    if (!gluedInfo) return node

    const parent = layoutedNodes.find((n) => n.id === gluedInfo.parentId)
    if (!parent || !parent.position) return node

    return calculateGluedPosition(node, parent, gluedInfo)
  })
}
