import type { Node, Edge } from '@xyflow/react'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'

/**
 * Feature flag: Allow nested groups vs. flatten groups
 *
 * When true: Groups can be selected and added to other groups (nested structure)
 * When false: Selecting a group automatically expands it and selects its children (flat structure)
 *
 * This can be made configurable via environment variable or user preference
 */
export const ALLOW_NESTED_GROUPS = true // Change to false to flatten groups

/**
 * Check if a node is already in a group
 *
 * @param node - Node to check
 * @returns true if node has a parentId (is in a group)
 */
export const isNodeInGroup = (node: Node): boolean => {
  return !!node.parentId
}

/**
 * Get the parent group node that contains a given node
 *
 * @param node - Node to find parent for
 * @param allNodes - All workspace nodes
 * @returns Parent group node or null if not in a group
 */
export const getNodeParentGroup = (node: Node, allNodes: Node[]): Node | null => {
  if (!node.parentId) return null
  return allNodes.find((n) => n.id === node.parentId) || null
}

/**
 * Check if a node can be selected for grouping
 *
 * This function enforces the following rules:
 * - Ghost nodes cannot be grouped
 * - EDGE node cannot be grouped
 * - DEVICE and HOST nodes cannot be directly selected (they are auto-included)
 * - Nodes already in a group cannot be selected for another group
 * - Only ADAPTER, BRIDGE, and CLUSTER (group) nodes can be selected
 *
 * @param node - Node to check
 * @param _allNodes - All workspace nodes (reserved for future use)
 * @returns true if node can be selected for grouping
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const canNodeBeGrouped = (node: Node, _allNodes: Node[]): boolean => {
  // Ghost nodes cannot be grouped
  if (node.data?.isGhost) {
    return false
  }

  // EDGE node cannot be grouped
  if (node.id === IdStubs.EDGE_NODE) {
    return false
  }

  // DEVICE and HOST nodes cannot be directly selected
  // (they are automatically included with their adapters/bridges)
  if (node.type === NodeTypes.DEVICE_NODE || node.type === NodeTypes.HOST_NODE) {
    return false
  }

  // Nodes already in a group cannot be selected for another group
  if (isNodeInGroup(node)) {
    return false
  }

  // Only allow these node types to be grouped
  const allowedTypes: string[] = [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE, NodeTypes.CLUSTER_NODE]

  return allowedTypes.includes(node.type || '')
}

/**
 * Get all child nodes from a group (recursively)
 *
 * Used when ALLOW_NESTED_GROUPS is false to flatten group selection
 *
 * @param groupNode - Group node to expand
 * @param allNodes - All workspace nodes
 * @returns Array of all child nodes (recursively expanded)
 */
export const getGroupChildren = (groupNode: Node, allNodes: Node[]): Node[] => {
  if (groupNode.type !== NodeTypes.CLUSTER_NODE) {
    return []
  }

  const childIds = (groupNode.data?.childrenNodeIds || []) as string[]
  const children: Node[] = []

  childIds.forEach((childId) => {
    const childNode = allNodes.find((n) => n.id === childId)
    if (!childNode) return

    children.push(childNode)

    // Recursively get children of nested groups
    if (childNode.type === NodeTypes.CLUSTER_NODE) {
      const nestedChildren = getGroupChildren(childNode, allNodes)
      children.push(...nestedChildren)
    }
  })

  return children
}

/**
 * Get DEVICE/HOST nodes that should be auto-included with selected nodes
 *
 * When adapters are selected, their connected DEVICE nodes are auto-included.
 * When bridges are selected, their connected HOST nodes are auto-included.
 *
 * @param selectedNodes - Nodes that have been manually selected
 * @param allNodes - All workspace nodes
 * @param allEdges - All workspace edges
 * @returns Array of nodes that should be auto-included (DEVICE/HOST nodes)
 */
export const getAutoIncludedNodes = (selectedNodes: Node[], allNodes: Node[], allEdges: Edge[]): Node[] => {
  const autoIncluded: Node[] = []

  selectedNodes.forEach((node) => {
    // For adapters, find connected DEVICE node
    if (node.type === NodeTypes.ADAPTER_NODE) {
      // Look for edge connecting adapter to device
      // Edge can be in either direction: adapter → device or device → adapter
      const deviceEdge = allEdges.find(
        (e) =>
          (e.source === node.id && e.target.startsWith(IdStubs.DEVICE_NODE)) ||
          (e.target === node.id && e.source.startsWith(IdStubs.DEVICE_NODE))
      )

      if (deviceEdge) {
        const deviceId = deviceEdge.source.startsWith(IdStubs.DEVICE_NODE) ? deviceEdge.source : deviceEdge.target
        const deviceNode = allNodes.find((n) => n.id === deviceId)

        // Only add if not already in the auto-included list
        if (deviceNode && !autoIncluded.some((n) => n.id === deviceNode.id)) {
          autoIncluded.push(deviceNode)
        }
      }
    }

    // For bridges, find connected HOST node
    if (node.type === NodeTypes.BRIDGE_NODE) {
      // Look for edge connecting bridge to host
      // Edge can be in either direction: bridge → host or host → bridge
      const hostEdge = allEdges.find(
        (e) =>
          (e.source === node.id && e.target.startsWith(IdStubs.HOST_NODE)) ||
          (e.target === node.id && e.source.startsWith(IdStubs.HOST_NODE))
      )

      if (hostEdge) {
        const hostId = hostEdge.source.startsWith(IdStubs.HOST_NODE) ? hostEdge.source : hostEdge.target
        const hostNode = allNodes.find((n) => n.id === hostId)

        // Only add if not already in the auto-included list
        if (hostNode && !autoIncluded.some((n) => n.id === hostNode.id)) {
          autoIncluded.push(hostNode)
        }
      }
    }

    // For groups (CLUSTER_NODE), do NOT recursively traverse
    // Groups already contain their children, so there's nothing to auto-include
    // The group node itself is selected, and all its children come with it automatically
    // by virtue of the parent-child relationship in React Flow
    if (node.type === NodeTypes.CLUSTER_NODE) {
      // No auto-inclusion needed for groups - they already contain their children
      // This prevents duplicate nodes and incorrect position calculations
    }
  })

  return autoIncluded
}

/**
 * Maximum allowed nesting depth for groups
 * This prevents cognitive overload and performance issues
 */
export const MAX_NESTING_DEPTH = 3

/**
 * Get the nesting depth of a node
 *
 * @param nodeId - ID of the node
 * @param allNodes - All workspace nodes
 * @returns Depth level (0 = root, 1 = in one group, 2 = in nested group, etc.)
 */
export const getNodeNestingDepth = (nodeId: string, allNodes: Node[]): number => {
  const node = allNodes.find((n) => n.id === nodeId)
  if (!node) return 0

  let depth = 0
  let current: Node | undefined = node

  while (current?.parentId) {
    depth++
    current = allNodes.find((n) => n.id === current?.parentId)
    if (!current) break

    // Safety check to prevent infinite loops
    if (depth > 10) {
      console.error('Circular reference detected in group hierarchy')
      break
    }
  }

  return depth
}

/**
 * Get the maximum depth of children within a group (recursively)
 * Used to check if adding a group would exceed max depth
 *
 * @param groupNode - Group node to check
 * @param allNodes - All workspace nodes
 * @returns Maximum depth of children (0 if no children, 1+ if has nested groups)
 */
export const getMaxChildDepth = (groupNode: Node, allNodes: Node[]): number => {
  if (groupNode.type !== NodeTypes.CLUSTER_NODE) return 0

  const childIds = (groupNode.data?.childrenNodeIds || []) as string[]
  if (childIds.length === 0) return 0

  let maxDepth = 0
  childIds.forEach((childId) => {
    const child = allNodes.find((n) => n.id === childId)
    if (child?.type === NodeTypes.CLUSTER_NODE) {
      const childDepth = 1 + getMaxChildDepth(child, allNodes)
      maxDepth = Math.max(maxDepth, childDepth)
    }
  })

  return maxDepth
}

/**
 * Check if a node can be added to a group without exceeding max nesting depth
 *
 * @param nodeId - ID of the node to add
 * @param targetGroupId - ID of the target group
 * @param allNodes - All workspace nodes
 * @returns Object with allowed flag and optional reason
 */
export const canAddToGroup = (
  nodeId: string,
  targetGroupId: string,
  allNodes: Node[]
): { allowed: boolean; reason?: string } => {
  const targetDepth = getNodeNestingDepth(targetGroupId, allNodes)
  const node = allNodes.find((n) => n.id === nodeId)

  if (!node) {
    return { allowed: false, reason: 'Node not found' }
  }

  // For groups being added, account for their internal depth
  if (node.type === NodeTypes.CLUSTER_NODE) {
    const maxChildDepth = getMaxChildDepth(node, allNodes)
    const totalDepth = targetDepth + 1 + maxChildDepth

    if (totalDepth > MAX_NESTING_DEPTH) {
      return {
        allowed: false,
        reason: `Maximum nesting depth (${MAX_NESTING_DEPTH} levels) would be exceeded`,
      }
    }
  } else {
    // For regular nodes, just check target depth
    if (targetDepth + 1 > MAX_NESTING_DEPTH) {
      return {
        allowed: false,
        reason: `Maximum nesting depth (${MAX_NESTING_DEPTH} levels) reached`,
      }
    }
  }

  return { allowed: true }
}

/**
 * Check if a group can be collapsed
 * Prevents collapsing nested groups when parent is already collapsed
 *
 * @param groupId - ID of the group to collapse
 * @param allNodes - All workspace nodes
 * @returns Object with allowed flag and optional reason
 */
export const canGroupCollapse = (groupId: string, allNodes: Node[]): { allowed: boolean; reason?: string } => {
  const group = allNodes.find((n) => n.id === groupId)

  if (!group) {
    return { allowed: false, reason: 'Group not found' }
  }

  // Check if parent is collapsed
  if (group.parentId) {
    const parent = allNodes.find((n) => n.id === group.parentId)
    if (parent?.data?.isCollapsed) {
      return {
        allowed: false,
        reason: 'Cannot collapse nested group when parent is collapsed',
      }
    }
  }

  return { allowed: true }
}

/**
 * Validate group hierarchy for circular references and orphaned nodes
 *
 * @param nodes - All workspace nodes
 * @returns Object with validation result and error messages
 */
export const validateGroupHierarchy = (nodes: Node[]): { valid: boolean; errors: string[] } => {
  const errors: string[] = []

  nodes.forEach((node) => {
    if (!node.parentId) return

    const ancestors = new Set<string>()
    let current: Node | undefined = node
    let depth = 0

    while (current?.parentId && depth < 10) {
      // Check for circular reference
      if (ancestors.has(current.parentId)) {
        errors.push(`Circular reference detected: ${node.id} → ${current.parentId}`)
        break
      }

      ancestors.add(current.parentId)
      current = nodes.find((n) => n.id === current?.parentId)

      if (!current) {
        errors.push(`Orphaned node: ${node.id} has invalid parent ${node.parentId}`)
        break
      }

      depth++
    }

    if (depth >= 10) {
      errors.push(`Excessive nesting depth detected for node: ${node.id}`)
    }
  })

  return {
    valid: errors.length === 0,
    errors,
  }
}
