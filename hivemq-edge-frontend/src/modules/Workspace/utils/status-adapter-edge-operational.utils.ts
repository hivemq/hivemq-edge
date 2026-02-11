import type { AdapterConfig, Combiner } from '@/api/__generated__'
import { DataIdentifierReference, EntityType } from '@/api/__generated__'
import type { Node } from '@xyflow/react'
import { NodeTypes, type NodeCombinerType } from '../types'
import { OperationalStatus } from '../types/status.types'

/**
 * Checks if an adapter has northbound mappings configured.
 *
 * Northbound mappings control the flow from adapter/device to the Edge broker.
 *
 * @param adapterConfig - The adapter configuration
 * @returns true if adapter has at least one northbound mapping
 */
export const adapterHasNorthboundMappings = (adapterConfig?: AdapterConfig): boolean => {
  return (adapterConfig?.northboundMappings?.length ?? 0) > 0
}

/**
 * Checks if an adapter has southbound mappings configured.
 *
 * Southbound mappings control the flow from Edge broker to adapter/device.
 *
 * @param adapterConfig - The adapter configuration
 * @returns true if adapter has at least one southbound mapping
 */
export const adapterHasSouthboundMappings = (adapterConfig?: AdapterConfig): boolean => {
  return (adapterConfig?.southboundMappings?.length ?? 0) > 0
}

/**
 * Gets all tag names defined in a device (from its adapter).
 *
 * @param adapterConfig - The adapter configuration
 * @returns Set of tag names
 */
export const getDeviceTagNames = (adapterConfig?: AdapterConfig): Set<string> => {
  if (!adapterConfig?.tags) return new Set()
  return new Set(adapterConfig.tags.map((tag) => tag.name))
}

/**
 * Checks if a combiner has mappings that reference tags from a specific device.
 *
 * For a combiner to be operational with respect to a specific adapter/device connection,
 * it must have at least one mapping that uses a TAG from that device.
 *
 * @param combiner - The combiner node data
 * @param deviceTags - Set of tag names available in the device
 * @param adapterId - the id of the owning adapter
 * @returns true if combiner has mappings using at least one device tag
 */
export const combinerHasValidAdapterTagMappings = (
  combiner: Combiner,
  deviceTags: Set<string>,
  adapterId: string
): boolean => {
  if (!combiner.mappings?.items || combiner.mappings.items.length === 0) {
    return false
  }

  // Check if combiner is connected to an adapter
  const hasAdapterSource = combiner.sources.items.some((source) => source.type === EntityType.ADAPTER)
  if (!hasAdapterSource) {
    return false
  }

  // Check if at least one mapping uses a TAG from the device
  return combiner.mappings.items.some((mapping) => {
    // Check primary source - must match both tag name AND scope
    if (
      mapping.sources.primary.type === DataIdentifierReference.type.TAG &&
      mapping.sources.primary.scope === adapterId &&
      deviceTags.has(mapping.sources.primary.id)
    ) {
      return true
    }

    // Check additional tags array if present
    // NOTE: sources.tags is string[] without scope info, so we can only check tag name
    // This is a limitation until Stage 2 adds full DataIdentifierReference[] for tags
    if (mapping.sources.tags) {
      return mapping.sources.tags.some((tagName) => deviceTags.has(tagName))
    }

    return false
  })
}

/**
 * Computes the operational status for an edge connecting an adapter to a combiner.
 *
 * ⚠️ **DESIGN NOTE - Ownership Inconsistency:**
 * This rule breaks the consistency of ownership. Ideally, the connection should originate
 * from the DEVICE itself (which owns the tags), not from the ADAPTER. This may be refactored
 * in a future version to have explicit DEVICE → COMBINER connections.
 *
 * The edge is operationally ACTIVE if:
 * - The target combiner has at least one mapping
 * - At least one mapping references a TAG from the device connected to this adapter
 *
 * @param sourceAdapterNode - The source adapter node
 * @param targetCombinerNode - The target combiner node
 * @param adapterConfig - The adapter configuration (with tags)
 * @returns Operational status for this specific edge
 */
export const computeAdapterToCombinerOperationalStatus = (
  sourceAdapterNode: Node,
  targetCombinerNode: Node,
  adapterConfig?: AdapterConfig
): OperationalStatus => {
  // Validate node types
  if (sourceAdapterNode.type !== NodeTypes.ADAPTER_NODE) {
    return OperationalStatus.ERROR
  }

  if (targetCombinerNode.type !== NodeTypes.COMBINER_NODE) {
    return OperationalStatus.ERROR
  }

  const combinerData = (targetCombinerNode as NodeCombinerType).data

  // Check if this combiner is connected to an adapter
  const hasAdapterSource = combinerData.sources.items.some((source) => source.type === EntityType.ADAPTER)
  if (!hasAdapterSource) {
    return OperationalStatus.ERROR
  }

  // Get device tags
  const deviceTags = getDeviceTagNames(adapterConfig)
  if (deviceTags.size === 0) {
    return OperationalStatus.INACTIVE // No tags defined in device
  }

  // Check if the combiner has valid tag mappings
  const hasValidMappings = combinerHasValidAdapterTagMappings(combinerData, deviceTags, sourceAdapterNode.id)

  return hasValidMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
}

/**
 * Computes the operational status for an edge connecting an adapter to the Edge broker.
 *
 * The edge is operationally ACTIVE if the adapter has at least one northbound mapping.
 *
 * @param adapterConfig - The adapter configuration
 * @returns Operational status for the adapter → Edge edge
 */
export const computeAdapterToEdgeOperationalStatus = (adapterConfig?: AdapterConfig): OperationalStatus => {
  return adapterHasNorthboundMappings(adapterConfig) ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
}

/**
 * Computes the operational status for an edge connecting an adapter to a device.
 *
 * The edge is operationally ACTIVE if the adapter has at least one southbound mapping.
 *
 * @param adapterConfig - The adapter configuration
 * @returns Operational status for the adapter → Device edge
 */
export const computeAdapterToDeviceOperationalStatus = (adapterConfig?: AdapterConfig): OperationalStatus => {
  return adapterHasSouthboundMappings(adapterConfig) ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
}

/**
 * Computes the overall operational status for an adapter node.
 *
 * An adapter is operationally ACTIVE if it has the mappings required for its type:
 * - Unidirectional adapters: Need northbound mappings only
 * - Bidirectional adapters: Need BOTH northbound AND southbound mappings
 *
 * @param adapterConfig - The adapter configuration
 * @param isBidirectional - Whether the adapter supports bidirectional communication
 * @returns Operational status for the adapter node
 */
export const computeAdapterNodeOperationalStatus = (
  adapterConfig?: AdapterConfig,
  isBidirectional: boolean = false
): OperationalStatus => {
  const hasNorth = adapterHasNorthboundMappings(adapterConfig)
  const hasSouth = adapterHasSouthboundMappings(adapterConfig)

  if (isBidirectional) {
    // Bidirectional adapters need BOTH north and south mappings
    return hasNorth && hasSouth ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
  } else {
    // Unidirectional adapters only need northbound mappings
    return hasNorth ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
  }
}
