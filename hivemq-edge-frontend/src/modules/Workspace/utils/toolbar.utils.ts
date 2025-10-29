import type { Node } from '@xyflow/react'
import type { Adapter, Bridge, EntityReference, ProtocolAdapter } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'
import type { NodeCombinerType, NodePulseType } from '@/modules/Workspace/types'
import { arrayWithSameObjects } from './combiner.utils'

/**
 * Type representing nodes that can be combined (adapters, bridges, pulse agents)
 */
export type CombinerEligibleNode =
  | Node<Adapter, NodeTypes.ADAPTER_NODE>
  | Node<Bridge, NodeTypes.BRIDGE_NODE>
  | NodePulseType

/**
 * Checks if a node is eligible to be part of a combiner based on its type and capabilities
 * @param node - The node to check
 * @param adapterTypes - List of available adapter types with their capabilities
 * @returns true if the node can be part of a combiner
 */
export const isNodeCombinerCandidate = (node: Node, adapterTypes?: ProtocolAdapter[]): node is CombinerEligibleNode => {
  if (node.type === NodeTypes.ADAPTER_NODE) {
    const protocol = adapterTypes?.find((e) => e.id === (node.data as Adapter).type)
    return protocol?.capabilities?.includes('COMBINE') ?? false
  }

  return node.type === NodeTypes.BRIDGE_NODE || node.type === NodeTypes.PULSE_NODE
}

/**
 * Builds an array of EntityReference objects from selected nodes, including the edge broker
 * @param nodes - Array of eligible nodes to convert to entity references
 * @returns Array of EntityReference objects with proper types and IDs
 */
export const buildEntityReferencesFromNodes = (nodes: CombinerEligibleNode[]): EntityReference[] => {
  const references = nodes.map<EntityReference>((node) => {
    const getType = () => {
      if (node.type === NodeTypes.ADAPTER_NODE) return EntityType.ADAPTER
      if (node.type === NodeTypes.BRIDGE_NODE) return EntityType.BRIDGE
      return EntityType.PULSE_AGENT
    }

    return {
      type: getType(),
      id: node.data.id,
    }
  })

  // Always add the edge broker as the last reference
  references.push({ id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER })

  return references
}

/**
 * Finds an existing combiner node that has the exact same source references
 * @param allNodes - All nodes in the workspace
 * @param targetReferences - The entity references to check against
 * @returns The matching combiner node if found, undefined otherwise
 */
export const findExistingCombiner = (
  allNodes: Node[],
  targetReferences: EntityReference[]
): NodeCombinerType | undefined => {
  return allNodes.find((node) => {
    if (node.type === NodeTypes.COMBINER_NODE) {
      const combinerNode = node as NodeCombinerType
      const existingSources = combinerNode.data.sources.items
      const hasSameSources = arrayWithSameObjects<EntityReference>(targetReferences)(existingSources)
      return Boolean(hasSameSources)
    }
    return false
  }) as NodeCombinerType | undefined
}

/**
 * Filters a list of selected nodes to only those eligible for combiner creation
 * @param selectedNodes - All selected nodes
 * @param adapterTypes - List of available adapter types with their capabilities
 * @returns Array of eligible nodes or undefined if none are eligible
 */
export const filterCombinerCandidates = (
  selectedNodes: Node[],
  adapterTypes?: ProtocolAdapter[]
): CombinerEligibleNode[] | undefined => {
  const candidates = selectedNodes.filter((node): node is CombinerEligibleNode =>
    isNodeCombinerCandidate(node, adapterTypes)
  )
  return candidates.length > 0 ? candidates : undefined
}

/**
 * Checks if a combiner with the given nodes would be an asset mapper (contains a pulse node)
 * @param nodes - The nodes to check
 * @returns true if any node is a pulse node
 */
export const isAssetMapperCombiner = (nodes?: CombinerEligibleNode[]): boolean => {
  return Boolean(nodes?.some((node) => node.type === NodeTypes.PULSE_NODE))
}
