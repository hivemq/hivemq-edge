import type { Node } from '@xyflow/react'
import type { Combiner, ManagedAsset } from '@/api/__generated__'
import { AssetMapping, EntityType } from '@/api/__generated__'
import type { NodeCombinerType } from '../types'
import { NodeTypes } from '../types'
import { OperationalStatus } from '../types/status.types'

/**
 * Checks if a combiner (asset mapper) has any mappings that reference valid (mapped) Pulse assets.
 *
 * An asset mapper is operationally active for a specific Pulse connection if:
 * - It has at least one mapping configured
 * - At least one mapping references a valid asset from the connected Pulse agent
 * - The referenced asset is in MAPPED status (not UNMAPPED)
 *
 * @param combiner - The combiner node data
 * @param pulseAssets - All available Pulse managed assets
 * @returns true if the combiner has valid asset mappings
 */
export const combinerHasValidPulseAssetMappings = (combiner: Combiner, pulseAssets: ManagedAsset[]): boolean => {
  if (!combiner.mappings?.items || combiner.mappings.items.length === 0) {
    return false
  }

  // Check if combiner is connected to a Pulse agent
  const hasPulseSource = combiner.sources.items.some((source) => source.type === EntityType.PULSE_AGENT)
  if (!hasPulseSource) {
    return false
  }

  // Get all mapped asset IDs
  const mappedAssetIds = new Set(
    pulseAssets.filter((asset) => asset.mapping.status === AssetMapping.status.STREAMING).map((asset) => asset.id)
  )

  // Check if at least one mapping references a valid mapped asset
  return combiner.mappings.items.some((mapping) => {
    const assetId = mapping.destination.assetId
    return assetId && mappedAssetIds.has(assetId)
  })
}

/**
 * Computes the operational status for an edge connecting a Pulse agent to an asset mapper.
 *
 * The edge is operationally ACTIVE if:
 * - The target combiner (asset mapper) has at least one mapping
 * - At least one mapping references a valid (MAPPED) asset from the Pulse agent
 *
 * Otherwise, it's INACTIVE (not fully configured).
 *
 * @param sourcePulseNode - The source Pulse agent node
 * @param targetCombinerNode - The target combiner/asset mapper node
 * @param pulseAssets - All available Pulse managed assets
 * @returns Operational status for this specific edge
 */
export const computePulseToAssetMapperOperationalStatus = (
  sourcePulseNode: Node,
  targetCombinerNode: Node,
  pulseAssets: ManagedAsset[]
): OperationalStatus => {
  // Validate node types
  if (sourcePulseNode.type !== NodeTypes.PULSE_NODE) {
    return OperationalStatus.ERROR
  }

  if (targetCombinerNode.type !== NodeTypes.COMBINER_NODE) {
    return OperationalStatus.ERROR
  }

  const combinerData = (targetCombinerNode as NodeCombinerType).data

  // Check if this combiner is an asset mapper (has Pulse agent as source)
  const isAssetMapper = combinerData.sources.items.some((source) => source.type === EntityType.PULSE_AGENT)
  if (!isAssetMapper) {
    return OperationalStatus.ERROR
  }

  // Check if the combiner has valid asset mappings
  const hasValidMappings = combinerHasValidPulseAssetMappings(combinerData, pulseAssets)

  return hasValidMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
}

/**
 * Computes the operational status for a Pulse node based on its outbound connections.
 *
 * A Pulse node is operationally ACTIVE if it has at least one outbound connection
 * to an asset mapper that references valid mapped assets.
 *
 * @param connectedCombiners - All combiner nodes connected to this Pulse node
 * @param pulseAssets - All available Pulse managed assets
 * @returns Operational status for the Pulse node
 */
export const computePulseNodeOperationalStatus = (
  connectedCombiners: NodeCombinerType[],
  pulseAssets: ManagedAsset[]
): OperationalStatus => {
  if (connectedCombiners.length === 0) {
    return OperationalStatus.INACTIVE
  }

  // Check if at least one connected combiner has valid asset mappings
  const hasAnyValidConnection = connectedCombiners.some((combiner) =>
    combinerHasValidPulseAssetMappings(combiner.data, pulseAssets)
  )

  return hasAnyValidConnection ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
}
