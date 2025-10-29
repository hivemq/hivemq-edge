/**
 * Layout Types and Interfaces for Workspace Auto-Layout
 *
 * This module defines all types, interfaces, and enums related to
 * automatic graph layout in the workspace.
 */

import type { Node, Edge, XYPosition } from '@xyflow/react'

// ========== Enums ==========

/**
 * Available layout algorithm types
 */
export enum LayoutType {
  /** Dagre hierarchical layout, top-to-bottom */
  DAGRE_TB = 'DAGRE_TB',
  /** Dagre hierarchical layout, left-to-right */
  DAGRE_LR = 'DAGRE_LR',
  /** WebCola force-directed layout */
  COLA_FORCE = 'COLA_FORCE',
  /** Constrained layout with WebCola */
  COLA_CONSTRAINED = 'COLA_CONSTRAINED',
  /** Radial hub layout with EDGE at center */
  RADIAL_HUB = 'RADIAL_HUB',
  /** Manual positioning with saved layouts */
  MANUAL = 'MANUAL',
}

/**
 * Features supported by layout algorithms
 */
export enum LayoutFeature {
  /** Hierarchical tree structure */
  HIERARCHICAL = 'HIERARCHICAL',
  /** Directional flow (TB, LR, etc.) */
  DIRECTIONAL = 'DIRECTIONAL',
  /** Force-directed simulation */
  FORCE_DIRECTED = 'FORCE_DIRECTED',
  /** Radial/circular arrangement */
  RADIAL = 'RADIAL',
  /** Constraint-based positioning */
  CONSTRAINED = 'CONSTRAINED',
  /** Automatic clustering */
  CLUSTERING = 'CLUSTERING',
  /** Overlap removal */
  OVERLAP_REMOVAL = 'OVERLAP_REMOVAL',
}

/**
 * Layout application mode
 */
export enum LayoutMode {
  /** Manual trigger only */
  STATIC = 'STATIC',
  /** Auto-layout on graph changes */
  DYNAMIC = 'DYNAMIC',
}

// ========== Base Interfaces ==========

/**
 * Common options for all layout algorithms
 */
export interface LayoutOptions {
  /** Enable animated transitions */
  animate?: boolean
  /** Animation duration in milliseconds */
  animationDuration?: number
  /** Fit view after layout */
  fitView?: boolean
  /** Fit view options */
  fitViewOptions?: {
    /** Padding around the graph */
    padding?: number
    /** Include hidden nodes in fit calculation */
    includeHiddenNodes?: boolean
  }
}

/**
 * Options specific to Dagre layout algorithm
 */
export interface DagreOptions extends LayoutOptions {
  /** Rank direction (TB, LR, BT, RL) */
  rankdir: 'TB' | 'LR' | 'BT' | 'RL'
  /** Separation between ranks (layers) in pixels */
  ranksep: number
  /** Separation between nodes in same rank in pixels */
  nodesep: number
  /** Separation between edges in pixels */
  edgesep: number
  /** Ranking algorithm */
  ranker?: 'network-simplex' | 'tight-tree' | 'longest-path'
  /** Alignment of nodes */
  align?: 'UL' | 'UR' | 'DL' | 'DR'
}

/**
 * Options specific to WebCola force-directed layout
 */
export interface ColaForceOptions extends LayoutOptions {
  /** Target link distance in pixels */
  linkDistance: number
  /** Enable overlap avoidance */
  avoidOverlaps: boolean
  /** Handle disconnected components separately */
  handleDisconnected: boolean
  /** Convergence threshold for stopping */
  convergenceThreshold: number
  /** Maximum iterations */
  maxIterations: number
  /** ID of node to keep centered (e.g., Edge node) */
  centerNodeId?: string
  /** Alignment constraints */
  alignmentConstraints?: AlignmentConstraint[]
}

/**
 * Options specific to Radial Hub layout
 */
export interface RadialOptions extends LayoutOptions {
  /** Center X coordinate */
  centerX?: number
  /** Center Y coordinate */
  centerY?: number
  /** Distance between concentric layers in pixels */
  layerSpacing: number
  /** Starting angle in radians (default: -π/2 for top) */
  startAngle?: number
}

/**
 * Options specific to WebCola constraint-based layout
 */
export interface ColaConstrainedOptions extends LayoutOptions {
  /** Flow direction (x=horizontal, y=vertical) */
  flowDirection: 'x' | 'y'
  /** Gap between layers in pixels */
  layerGap: number
  /** Gap between nodes in same layer in pixels */
  nodeGap: number
  /** Layer assignment constraints */
  layerConstraints?: LayerConstraint[]
  /** Alignment constraints */
  alignmentConstraints?: AlignmentConstraint[]
  /** Separation constraints */
  separationConstraints?: SeparationConstraint[]
}

// ========== Constraints ==========

/**
 * Layout constraints that must be respected
 */
export interface LayoutConstraints {
  /** Nodes that are glued to other nodes (e.g., listeners to edge) */
  gluedNodes: Map<string, GluedNodeInfo>
  /** Nodes with fixed positions */
  fixedNodes: Set<string>
  /** Group nodes and their children */
  groupNodes: Map<string, string[]>
}

/**
 * Information about a node glued to another node
 */
export interface GluedNodeInfo {
  /** ID of the parent node */
  parentId: string
  /** Offset from parent position */
  offset: XYPosition
  /** Handle type (source or target) */
  handle: 'source' | 'target'
}

/**
 * Constraint to assign nodes to specific layers
 */
export interface LayerConstraint {
  /** Layer number (0-based) */
  layer: number
  /** Node IDs in this layer */
  nodeIds: string[]
}

/**
 * Constraint to align nodes along an axis
 */
export interface AlignmentConstraint {
  /** Axis to align on */
  axis: 'x' | 'y'
  /** Node IDs to align */
  nodeIds: string[]
  /** Optional offset from alignment line */
  offset?: number
}

/**
 * Constraint to maintain separation between nodes
 */
export interface SeparationConstraint {
  /** Left/top node ID */
  left: string
  /** Right/bottom node ID */
  right: string
  /** Minimum gap in pixels */
  gap: number
  /** Axis for separation */
  axis: 'x' | 'y'
}

// ========== Results ==========

/**
 * Result of applying a layout algorithm
 */
export interface LayoutResult {
  /** Nodes with updated positions */
  nodes: Node[]
  /** Time taken to compute layout in milliseconds */
  duration: number
  /** Whether layout was successful */
  success: boolean
  /** Error message if layout failed */
  error?: string
  /** Additional metadata about the layout */
  metadata?: LayoutMetadata
}

/**
 * Metadata about a layout computation
 */
export interface LayoutMetadata {
  /** Algorithm that was used */
  algorithm: LayoutType
  /** Number of nodes that were laid out */
  nodeCount: number
  /** Number of edges in the graph */
  edgeCount: number
  /** Number of iterations (for iterative algorithms) */
  iterations?: number
  /** Final convergence value (for force-directed) */
  convergence?: number
}

// ========== Presets ==========

/**
 * A saved layout configuration or manual positioning
 */
export interface LayoutPreset {
  /** Unique identifier */
  id: string
  /** User-friendly name */
  name: string
  /** Optional description */
  description?: string
  /** Algorithm type */
  algorithm: LayoutType
  /** Algorithm options */
  options: LayoutOptions
  /** Saved node positions (for MANUAL type) */
  positions?: Map<string, XYPosition>
  /** Creation timestamp */
  createdAt: Date
  /** Last update timestamp */
  updatedAt: Date
}

// ========== History ==========

/**
 * A historical layout state for undo/redo
 */
export interface LayoutHistoryEntry {
  /** Unique identifier */
  id: string
  /** Timestamp */
  timestamp: Date
  /** Algorithm that was used */
  algorithm: LayoutType
  /** Options that were used */
  options: LayoutOptions
  /** Node positions at this point */
  nodePositions: Map<string, XYPosition>
}

// ========== Algorithm Interface ==========

/**
 * Interface that all layout algorithms must implement
 */
export interface LayoutAlgorithm {
  /** Human-readable name */
  readonly name: string
  /** Algorithm type identifier */
  readonly type: LayoutType
  /** Brief description */
  readonly description: string
  /** Default options for this algorithm */
  readonly defaultOptions: LayoutOptions

  /**
   * Apply the layout algorithm to nodes and edges
   *
   * @param nodes - Array of nodes to layout
   * @param edges - Array of edges between nodes
   * @param options - Algorithm-specific options
   * @param constraints - Optional layout constraints
   * @returns Promise resolving to layout result
   */
  apply(nodes: Node[], edges: Edge[], options: LayoutOptions, constraints?: LayoutConstraints): Promise<LayoutResult>

  /**
   * Check if algorithm supports a specific feature
   *
   * @param feature - Feature to check
   * @returns true if feature is supported
   */
  supports(feature: LayoutFeature): boolean

  /**
   * Validate options before applying layout
   *
   * @param options - Options to validate
   * @returns Validation result with errors/warnings
   */
  validateOptions(options: LayoutOptions): ValidationResult
}

/**
 * Result of validating layout options
 */
export interface ValidationResult {
  /** Whether options are valid */
  valid: boolean
  /** Validation errors (prevent layout) */
  errors?: string[]
  /** Validation warnings (allow layout but inform user) */
  warnings?: string[]
}

// ========== Configuration ==========

/**
 * Complete layout configuration state
 */
export interface LayoutConfiguration {
  /** Currently selected algorithm */
  currentAlgorithm: LayoutType
  /** Current layout mode (static or dynamic) */
  mode: LayoutMode
  /** Current algorithm options */
  options: LayoutOptions
  /** Saved layout presets */
  presets: LayoutPreset[]
  /** Layout history for undo/redo */
  history: LayoutHistoryEntry[]
  /** Maximum number of history entries to keep */
  maxHistorySize: number
}

// ========== Events ==========

/**
 * Layout-related events
 */
export interface LayoutEvent {
  /** Event type */
  type: 'apply' | 'preset-save' | 'preset-load' | 'undo' | 'redo'
  /** Event timestamp */
  timestamp: Date
  /** Event-specific data */
  data: unknown
}

// ========== Default Configurations ==========

/**
 * Default configuration for each layout type
 */
export const DEFAULT_LAYOUT_OPTIONS: Record<LayoutType, LayoutOptions> = {
  [LayoutType.DAGRE_TB]: {
    rankdir: 'TB',
    ranksep: 150,
    nodesep: 80,
    edgesep: 20,
    ranker: 'network-simplex',
    animate: true,
    animationDuration: 300,
    fitView: true,
  } as DagreOptions,

  [LayoutType.DAGRE_LR]: {
    rankdir: 'LR',
    ranksep: 200,
    nodesep: 80,
    edgesep: 20,
    ranker: 'network-simplex',
    animate: true,
    animationDuration: 300,
    fitView: true,
  } as DagreOptions,

  [LayoutType.COLA_FORCE]: {
    linkDistance: 350, // Accounts for node width (~245px) + gap
    avoidOverlaps: true,
    handleDisconnected: true,
    convergenceThreshold: 0.01,
    maxIterations: 1000,
    animate: true,
    animationDuration: 500,
    fitView: true,
  } as ColaForceOptions,

  [LayoutType.COLA_CONSTRAINED]: {
    flowDirection: 'y',
    layerGap: 350, // Accounts for node height + gap
    nodeGap: 300, // Accounts for node width (~245px) + gap
    animate: true,
    animationDuration: 300,
    fitView: true,
  } as ColaConstrainedOptions,

  [LayoutType.RADIAL_HUB]: {
    centerX: 400,
    centerY: 300,
    layerSpacing: 500,
    startAngle: -Math.PI / 2,
    animate: true,
    animationDuration: 300,
    fitView: true,
  } as RadialOptions,

  [LayoutType.MANUAL]: {
    animate: false,
    fitView: false,
  },
}
