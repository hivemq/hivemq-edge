import type { Edge, Node } from '@xyflow/react'
import { NodeTypes } from '@/modules/Workspace/types'
import type { OperationalStatus } from '@/modules/Workspace/types/status.types'
import { RuntimeStatus, type NodeStatusModel } from '@/modules/Workspace/types/status.types'

/**
 * Determines if a node is an active node (has its own runtime status)
 */
export function isActiveNode(nodeType: NodeTypes): boolean {
  return nodeType === NodeTypes.ADAPTER_NODE || nodeType === NodeTypes.BRIDGE_NODE || nodeType === NodeTypes.PULSE_NODE
}

/**
 * Gets all upstream nodes that are connected to the target node via incoming edges.
 * Only returns nodes that are active (have their own runtime status).
 *
 * @param nodeId - The ID of the node to find upstream nodes for
 * @param edges - All edges in the graph
 * @param nodes - All nodes in the graph
 * @returns Array of upstream active nodes
 */
export function getUpstreamActiveNodes(nodeId: string, edges: Edge[], nodes: Node[]): Node[] {
  // Find all edges that point to this node (incoming edges)
  const incomingEdges = edges.filter((edge) => edge.target === nodeId)

  // Get the source nodes of those edges
  const upstreamNodeIds = incomingEdges.map((edge) => edge.source)

  // Filter to only active nodes and nodes that exist
  const upstreamNodes = upstreamNodeIds
    .map((id) => nodes.find((node) => node.id === id))
    .filter((node): node is Node => node !== undefined && isActiveNode(node.type as NodeTypes))

  return upstreamNodes
}

/**
 * Computes the runtime status for a passive node based on its upstream active nodes.
 *
 * Logic:
 * - ERROR if any upstream active node is in ERROR state
 * - ACTIVE if at least one upstream active node is ACTIVE and none are in ERROR
 * - INACTIVE if no upstream nodes are ACTIVE and none are in ERROR (all INACTIVE or no upstream nodes)
 *
 * @param nodeId - The ID of the passive node
 * @param edges - All edges in the graph
 * @param nodes - All nodes in the graph
 * @returns The computed RuntimeStatus for the passive node
 */
export function computePassiveNodeRuntimeStatus(nodeId: string, edges: Edge[], nodes: Node[]): RuntimeStatus {
  const upstreamNodes = getUpstreamActiveNodes(nodeId, edges, nodes)

  // If no upstream nodes, default to INACTIVE
  if (upstreamNodes.length === 0) {
    return RuntimeStatus.INACTIVE
  }

  // Check each upstream node's status
  let hasActiveUpstream = false
  let hasErrorUpstream = false

  for (const node of upstreamNodes) {
    const statusModel = (node.data as { statusModel?: NodeStatusModel }).statusModel
    if (!statusModel) continue

    if (statusModel.runtime === RuntimeStatus.ERROR) {
      hasErrorUpstream = true
    } else if (statusModel.runtime === RuntimeStatus.ACTIVE) {
      hasActiveUpstream = true
    }
  }

  // ERROR propagates first
  if (hasErrorUpstream) {
    return RuntimeStatus.ERROR
  }

  // Then ACTIVE if at least one is active
  if (hasActiveUpstream) {
    return RuntimeStatus.ACTIVE
  }

  // Default to INACTIVE (all upstream are inactive or no status)
  return RuntimeStatus.INACTIVE
}

/**
 * Computes status model for a passive node.
 * Derives runtime status from upstream nodes and uses provided operational status.
 *
 * @param nodeId - The ID of the passive node
 * @param edges - All edges in the graph
 * @param nodes - All nodes in the graph
 * @param operationalStatus - The operational status for this node
 * @returns Complete NodeStatusModel for the passive node
 */
export function computePassiveNodeStatus(
  nodeId: string,
  edges: Edge[],
  nodes: Node[],
  operationalStatus: OperationalStatus
): NodeStatusModel {
  const runtime = computePassiveNodeRuntimeStatus(nodeId, edges, nodes)

  return {
    runtime,
    operational: operationalStatus,
    source: 'DERIVED',
    lastUpdated: new Date().toISOString(),
  }
}

/**
 * Gets all downstream nodes that are connected from the source node via outgoing edges.
 *
 * @param nodeId - The ID of the node to find downstream nodes for
 * @param edges - All edges in the graph
 * @param nodes - All nodes in the graph
 * @returns Array of downstream nodes
 */
export function getDownstreamNodes(nodeId: string, edges: Edge[], nodes: Node[]): Node[] {
  // Find all edges that originate from this node (outgoing edges)
  const outgoingEdges = edges.filter((edge) => edge.source === nodeId)

  // Get the target nodes of those edges
  const downstreamNodeIds = outgoingEdges.map((edge) => edge.target)

  // Return the nodes that exist
  const downstreamNodes = downstreamNodeIds
    .map((id) => nodes.find((node) => node.id === id))
    .filter((node): node is Node => node !== undefined)

  return downstreamNodes
}

/**
 * Recursively collects all nodes affected by a status change (downstream propagation).
 * This includes direct downstream nodes and their downstream nodes.
 *
 * @param nodeId - The ID of the node whose change triggered propagation
 * @param edges - All edges in the graph
 * @param nodes - All nodes in the graph
 * @param visited - Set of already visited node IDs to prevent circular traversal
 * @returns Array of node IDs that need status recomputation
 */
export function getAffectedNodes(nodeId: string, edges: Edge[], nodes: Node[], visited = new Set<string>()): string[] {
  if (visited.has(nodeId)) {
    return []
  }

  visited.add(nodeId)
  const affected: string[] = []

  const downstreamNodes = getDownstreamNodes(nodeId, edges, nodes)

  for (const node of downstreamNodes) {
    // Only passive nodes need recomputation from propagation
    if (!isActiveNode(node.type as NodeTypes)) {
      affected.push(node.id)
      // Recursively get affected nodes downstream
      affected.push(...getAffectedNodes(node.id, edges, nodes, visited))
    }
  }

  return affected
}

export const computeNodeRuntimeStatus = (
  operational: OperationalStatus,
  connectedNodes: Pick<Node, 'id' | 'type' | 'data'>[]
) => {
  // Derive runtime status from connected upstream nodes
  if (!connectedNodes || connectedNodes.length === 0) {
    return {
      runtime: RuntimeStatus.INACTIVE,
      operational,
      source: 'DERIVED' as const,
    }
  }

  let hasErrorUpstream = false
  let hasActiveUpstream = false

  for (const node of connectedNodes) {
    if (!node) continue
    const upstreamStatusModel = (node.data as { statusModel?: NodeStatusModel }).statusModel
    if (!upstreamStatusModel) continue

    if (upstreamStatusModel.runtime === RuntimeStatus.ERROR) {
      hasErrorUpstream = true
    } else if (upstreamStatusModel.runtime === RuntimeStatus.ACTIVE) {
      hasActiveUpstream = true
    }
  }

  // ERROR propagates first
  const runtime = hasErrorUpstream
    ? RuntimeStatus.ERROR
    : hasActiveUpstream
      ? RuntimeStatus.ACTIVE
      : RuntimeStatus.INACTIVE

  return {
    runtime,
    operational,
    source: 'DERIVED' as const,
  }
}
