import { NodeTypes } from '@/modules/Workspace/types.ts'
import type { IconType } from 'react-icons'
import {
  LuDatabase,
  LuNetwork,
  LuMerge,
  LuMap,
  LuFolderTree,
  LuTag,
  LuFilter,
  LuArrowUp,
  LuArrowDown,
  LuCombine,
} from 'react-icons/lu'

import type { EntityType, IntegrationPointType, WizardType, WizardStepConfig } from '../types'
import { EntityType as EntityTypeEnum, IntegrationPointType as IntegrationPointTypeEnum } from '../types'

/**
 * Metadata for a wizard type
 */
export interface WizardMetadata {
  /** The wizard type */
  type: WizardType

  /** Category: entity creation or integration point addition */
  category: 'entity' | 'integration'

  /** Icon from react-icons */
  icon: IconType

  /** Whether this wizard requires node selection */
  requiresSelection: boolean

  /** Whether this wizard shows ghost nodes */
  requiresGhost: boolean

  /** Step configurations for this wizard */
  steps: WizardStepConfig[]
}

/**
 * Complete wizard metadata registry
 * Maps each wizard type to its configuration
 */
export const WIZARD_REGISTRY: Record<WizardType, WizardMetadata> = {
  // ====================================================================
  // ENTITY WIZARDS
  // ====================================================================

  [EntityTypeEnum.ADAPTER]: {
    type: EntityTypeEnum.ADAPTER,
    category: 'entity',
    icon: LuDatabase,
    requiresSelection: false,
    requiresGhost: true,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_ADAPTER_0', // "Review adapter preview"
        showsGhostNodes: true,
      },
      {
        index: 1,
        descriptionKey: 'step_ADAPTER_1', // "Select protocol type"
        requiresConfiguration: true,
      },
      {
        index: 2,
        descriptionKey: 'step_ADAPTER_2', // "Configure adapter settings"
        requiresConfiguration: true,
      },
    ],
  },

  [EntityTypeEnum.BRIDGE]: {
    type: EntityTypeEnum.BRIDGE,
    category: 'entity',
    icon: LuNetwork,
    requiresSelection: false,
    requiresGhost: true,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_BRIDGE_0', // "Review bridge preview"
        showsGhostNodes: true,
      },
      {
        index: 1,
        descriptionKey: 'step_BRIDGE_1', // "Configure bridge settings"
        requiresConfiguration: true,
      },
    ],
  },

  [EntityTypeEnum.COMBINER]: {
    type: EntityTypeEnum.COMBINER,
    category: 'entity',
    icon: LuMerge,
    requiresSelection: true,
    requiresGhost: true,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_COMBINER_0', // "Select data sources"
        requiresSelection: true,
        selectionConstraints: {
          minNodes: 2,
          allowedNodeTypes: [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE],
          // Only allow adapters with COMBINE capability
          // Note: customFilter will be enhanced by WizardSelectionRestrictions with protocol adapter data
          customFilter: (node) => {
            // Bridges are always allowed
            if (node.type === NodeTypes.BRIDGE_NODE) return true

            // For adapters, we need to check the protocol definition
            // This will be handled by WizardSelectionRestrictions which has access to protocol adapters
            return node.type === NodeTypes.ADAPTER_NODE
          },
          // Flag to indicate we need protocol adapter capabilities check
          requiresProtocolCapabilities: ['COMBINE'],
        },
      },
      {
        index: 1,
        descriptionKey: 'step_COMBINER_1', // "Configure combining logic"
        requiresConfiguration: true,
      },
    ],
  },

  [EntityTypeEnum.ASSET_MAPPER]: {
    type: EntityTypeEnum.ASSET_MAPPER,
    category: 'entity',
    icon: LuMap,
    requiresSelection: true,
    requiresGhost: true,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_ASSET_MAPPER_0', // "Select data sources" (Pulse Agent auto-included)
        requiresSelection: true,
        selectionConstraints: {
          minNodes: 3, // Minimum: 1 Pulse Agent (auto-selected) + 2 data sources
          allowedNodeTypes: [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE],
          // Same as Combiner: only allow adapters with COMBINE capability
          // Note: customFilter will be enhanced by WizardSelectionRestrictions with protocol adapter data
          customFilter: (node) => {
            // Bridges are always allowed
            if (node.type === NodeTypes.BRIDGE_NODE) return true

            // For adapters, we need to check the protocol definition
            // This will be handled by WizardSelectionRestrictions which has access to protocol adapters
            return node.type === NodeTypes.ADAPTER_NODE
          },
          // Flag to indicate we need protocol adapter capabilities check
          requiresProtocolCapabilities: ['COMBINE'],
        },
      },
      {
        index: 1,
        descriptionKey: 'step_ASSET_MAPPER_1', // "Configure asset mappings"
        requiresConfiguration: true,
      },
    ],
  },

  [EntityTypeEnum.GROUP]: {
    type: EntityTypeEnum.GROUP,
    category: 'entity',
    icon: LuFolderTree,
    requiresSelection: true,
    requiresGhost: true,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_GROUP_0', // "Select nodes to group"
        requiresSelection: true,
        selectionConstraints: {
          minNodes: 2,
          allowedNodeTypes: [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE, NodeTypes.CLUSTER_NODE],
          // Custom filter will be applied in WizardSelectionRestrictions
          // to check for nodes already in groups
          excludeNodesInGroups: true, // Flag for WizardSelectionRestrictions
        },
      },
      {
        index: 1,
        descriptionKey: 'step_GROUP_1', // "Configure group settings"
        requiresConfiguration: true,
      },
    ],
  },

  // ====================================================================
  // INTEGRATION POINT WIZARDS
  // ====================================================================

  [IntegrationPointTypeEnum.TAG]: {
    type: IntegrationPointTypeEnum.TAG,
    category: 'integration',
    icon: LuTag,
    requiresSelection: true,
    requiresGhost: false,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_TAG_0', // "Select device node"
        requiresSelection: true,
      },
      {
        index: 1,
        descriptionKey: 'step_TAG_1', // "Configure tags"
        requiresConfiguration: true,
      },
    ],
  },

  [IntegrationPointTypeEnum.TOPIC_FILTER]: {
    type: IntegrationPointTypeEnum.TOPIC_FILTER,
    category: 'integration',
    icon: LuFilter,
    requiresSelection: true,
    requiresGhost: false,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_TOPIC_FILTER_0', // "Select Edge Broker"
        requiresSelection: true,
      },
      {
        index: 1,
        descriptionKey: 'step_TOPIC_FILTER_1', // "Configure topic filters"
        requiresConfiguration: true,
      },
    ],
  },

  [IntegrationPointTypeEnum.DATA_MAPPING_NORTH]: {
    type: IntegrationPointTypeEnum.DATA_MAPPING_NORTH,
    category: 'integration',
    icon: LuArrowUp,
    requiresSelection: true,
    requiresGhost: false,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_DATA_MAPPING_NORTH_0', // "Select adapter"
        requiresSelection: true,
      },
      {
        index: 1,
        descriptionKey: 'step_DATA_MAPPING_NORTH_1', // "Configure northbound mappings"
        requiresConfiguration: true,
      },
    ],
  },

  [IntegrationPointTypeEnum.DATA_MAPPING_SOUTH]: {
    type: IntegrationPointTypeEnum.DATA_MAPPING_SOUTH,
    category: 'integration',
    icon: LuArrowDown,
    requiresSelection: true,
    requiresGhost: false,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_DATA_MAPPING_SOUTH_0', // "Select adapter"
        requiresSelection: true,
      },
      {
        index: 1,
        descriptionKey: 'step_DATA_MAPPING_SOUTH_1', // "Configure southbound mappings"
        requiresConfiguration: true,
      },
    ],
  },

  [IntegrationPointTypeEnum.DATA_COMBINING]: {
    type: IntegrationPointTypeEnum.DATA_COMBINING,
    category: 'integration',
    icon: LuCombine,
    requiresSelection: true,
    requiresGhost: false,
    steps: [
      {
        index: 0,
        descriptionKey: 'step_DATA_COMBINING_0', // "Select combiner"
        requiresSelection: true,
      },
      {
        index: 1,
        descriptionKey: 'step_DATA_COMBINING_1', // "Configure combining logic"
        requiresConfiguration: true,
      },
    ],
  },
}

/**
 * Get metadata for a specific wizard type
 */
export const getWizardMetadata = (type: WizardType): WizardMetadata => {
  return WIZARD_REGISTRY[type]
}

/**
 * Get the icon for a wizard type
 */
export const getWizardIcon = (type: WizardType): IconType => {
  return WIZARD_REGISTRY[type].icon
}

/**
 * Get the category for a wizard type
 */
export const getWizardCategory = (type: WizardType): 'entity' | 'integration' => {
  return WIZARD_REGISTRY[type].category
}

/**
 * Get the number of steps for a wizard type
 */
export const getWizardStepCount = (type: WizardType): number => {
  return WIZARD_REGISTRY[type].steps.length
}

/**
 * Check if a wizard type requires node selection
 */
export const requiresSelection = (type: WizardType): boolean => {
  return WIZARD_REGISTRY[type].requiresSelection
}

/**
 * Check if a wizard type shows ghost nodes
 */
export const requiresGhost = (type: WizardType): boolean => {
  return WIZARD_REGISTRY[type].requiresGhost
}

/**
 * Get all entity wizard types
 */
export const getEntityWizardTypes = (): EntityType[] => {
  return Object.values(EntityTypeEnum)
}

/**
 * Get all integration point wizard types
 */
export const getIntegrationWizardTypes = (): IntegrationPointType[] => {
  return Object.values(IntegrationPointTypeEnum)
}

/**
 * Get step configuration for a specific wizard and step index
 */
export const getWizardStep = (type: WizardType, stepIndex: number): WizardStepConfig | undefined => {
  const metadata = WIZARD_REGISTRY[type]
  return metadata.steps[stepIndex]
}

/**
 * Get the translation key for a wizard step description
 * Returns the full i18n key with context
 */
export const getStepDescriptionKey = (type: WizardType, stepIndex: number): string => {
  const step = getWizardStep(type, stepIndex)
  return step?.descriptionKey || ''
}
