/**
 * Workspace Wizard Types
 *
 * Core type definitions for the workspace wizard system.
 * These types support creating entities and integration points directly in the workspace.
 */

import type { Node, Edge } from '@xyflow/react'

/**
 * Entity types that can be created in the workspace
 */
export enum EntityType {
  ADAPTER = 'ADAPTER',
  BRIDGE = 'BRIDGE',
  COMBINER = 'COMBINER',
  ASSET_MAPPER = 'ASSET_MAPPER',
  GROUP = 'GROUP',
}

/**
 * Integration point types that can be added to existing entities
 */
export enum IntegrationPointType {
  TAG = 'TAG',
  TOPIC_FILTER = 'TOPIC_FILTER',
  DATA_MAPPING_NORTH = 'DATA_MAPPING_NORTH',
  DATA_MAPPING_SOUTH = 'DATA_MAPPING_SOUTH',
  DATA_COMBINING = 'DATA_COMBINING',
}

/**
 * Combined type for all wizard types
 */
export type WizardType = EntityType | IntegrationPointType

/**
 * Wizard step configuration
 */
export interface WizardStepConfig {
  /** Step index */
  index: number
  /** Translation key for step description */
  descriptionKey: string
  /** Whether this step requires node selection */
  requiresSelection?: boolean
  /** Selection constraints for this step */
  selectionConstraints?: SelectionConstraints
  /** Whether this step requires configuration */
  requiresConfiguration?: boolean
  /** Whether this step shows ghost nodes */
  showsGhostNodes?: boolean
}

/**
 * Constraints for node selection in interactive steps
 */
export interface SelectionConstraints {
  /** Minimum number of nodes that must be selected */
  minNodes?: number
  /** Maximum number of nodes that can be selected */
  maxNodes?: number
  /** Allowed node types for selection */
  allowedNodeTypes?: string[]
  /** Node IDs that must be included in selection */
  requiredNodeIds?: string[]
  /** Whether to exclude nodes that are already in groups */
  excludeGrouped?: boolean
  /** Whether to exclude nodes that are already in groups (GROUP wizard specific) */
  excludeNodesInGroups?: boolean
  /** Custom filter function for advanced filtering (e.g., adapter capabilities) */
  customFilter?: (node: Node) => boolean
  /** Required protocol adapter capabilities (e.g., ['COMBINE']) - only for ADAPTER_NODE types */
  requiresProtocolCapabilities?: Array<'READ' | 'DISCOVER' | 'WRITE' | 'COMBINE'>
  /** Protocol adapter types data (for capability checking) - injected by WizardSelectionRestrictions */
  _protocolAdapters?: Array<{ id?: string; capabilities?: Array<'READ' | 'DISCOVER' | 'WRITE' | 'COMBINE'> }>
}

/**
 * Ghost node data for visual preview
 */
export interface GhostNode extends Omit<Node, 'id'> {
  /** Temporary ID for the ghost node */
  id: string
  /** Whether this is a ghost node */
  data: {
    isGhost: true
    label: string
    [key: string]: unknown
  }
}

/**
 * Ghost edge data for connection preview
 */
export interface GhostEdge extends Omit<Edge, 'id'> {
  /** Temporary ID for the ghost edge */
  id: string
  /** Whether this is a ghost edge */
  data?: {
    isGhost: true
    [key: string]: unknown
  }
}

/**
 * Wizard context passed to configuration forms
 */
export interface WizardContext {
  /** Callback when configuration is complete */
  onComplete: (data: Record<string, unknown>) => void
  /** Callback when user cancels */
  onCancel: () => void
  /** ID of the ghost node being configured (if applicable) */
  ghostNodeId?: string
  /** Indicates this form is in wizard mode */
  mode: 'wizard'
}

/**
 * Wizard state interface
 */
export interface WizardState {
  /** Whether the wizard is currently active */
  isActive: boolean

  /** The type of entity or integration point being created */
  entityType: WizardType | null

  /** Current step index (0-based) */
  currentStep: number

  /** Total number of steps in the current wizard */
  totalSteps: number

  /** IDs of nodes selected during interactive selection steps */
  selectedNodeIds: string[]

  /** Constraints for node selection (if applicable) */
  selectionConstraints: SelectionConstraints | null

  /** Ghost nodes shown on canvas as preview */
  ghostNodes: GhostNode[]

  /** Ghost edges connecting ghost nodes */
  ghostEdges: GhostEdge[]

  /** Configuration data accumulated through wizard steps */
  configurationData: Record<string, unknown>

  /** Whether the current configuration is valid */
  isConfigurationValid: boolean

  /** Whether the side panel is currently open */
  isSidePanelOpen: boolean

  /** Current error message, if any */
  errorMessage: string | null
}

/**
 * Wizard actions interface
 */
export interface WizardActions {
  /** Start a new wizard for the given type */
  startWizard: (type: WizardType) => void

  /** Cancel the current wizard and clean up */
  cancelWizard: () => void

  /** Move to the next step */
  nextStep: () => void

  /** Move to the previous step */
  previousStep: () => void

  /** Complete the wizard and create the entity/integration point */
  completeWizard: () => Promise<void>

  /** Select a node during interactive selection */
  selectNode: (nodeId: string) => void

  /** Deselect a node during interactive selection */
  deselectNode: (nodeId: string) => void

  /** Clear all selected nodes */
  clearSelection: () => void

  /** Update configuration data */
  updateConfiguration: (data: Partial<Record<string, unknown>>) => void

  /** Validate the current configuration */
  validateConfiguration: () => boolean

  /** Add ghost nodes to the preview */
  addGhostNodes: (nodes: GhostNode[]) => void

  /** Add ghost edges to the preview */
  addGhostEdges: (edges: GhostEdge[]) => void

  /** Remove all ghost nodes and edges */
  clearGhostNodes: () => void

  /** Set an error message */
  setError: (message: string | null) => void

  /** Clear the current error */
  clearError: () => void

  /** Open the side panel */
  openSidePanel: () => void

  /** Close the side panel */
  closeSidePanel: () => void
}

/**
 * Complete wizard store interface
 */
export interface WizardStore extends WizardState {
  actions: WizardActions
}
