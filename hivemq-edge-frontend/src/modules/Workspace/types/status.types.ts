import type { PulseStatus, Status } from '@/api/__generated__'

/**
 * Unified runtime status for all workspace nodes.
 * Represents the operational state of a node in the system.
 */
export enum RuntimeStatus {
  /** Node is running and connected */
  ACTIVE = 'ACTIVE',
  /** Node is stopped or disconnected */
  INACTIVE = 'INACTIVE',
  /** Node has errors or is in error state */
  ERROR = 'ERROR',
}

/**
 * Operational status represents the configuration completeness for data transformation.
 * This indicates whether a node is properly configured for its intended purpose.
 */
export enum OperationalStatus {
  /** Fully configured for data transformation */
  ACTIVE = 'ACTIVE',
  /** Partially configured (e.g., DRAFT mode) but not yet activated */
  INACTIVE = 'INACTIVE',
  /** Not configured for data transformation */
  ERROR = 'ERROR',
}

/**
 * Source type indicating how a node's status was determined
 */
export type StatusSource = 'ADAPTER' | 'BRIDGE' | 'PULSE' | 'DERIVED' | 'STATIC'

/**
 * Unified status model for all workspace nodes.
 * Combines runtime and operational status with metadata about the source.
 */
export interface NodeStatusModel {
  /** Current runtime state of the node */
  runtime: RuntimeStatus
  /** Current operational/configuration state of the node */
  operational: OperationalStatus
  /** How this status was determined */
  source: StatusSource
  /** Reference to the original status object if applicable */
  originalStatus?: Status | PulseStatus
  /** Optional timestamp of last status update */
  lastUpdated?: string
}

/**
 * Helper type for nodes with status model
 */
export interface WithStatusModel {
  statusModel?: NodeStatusModel
}

/**
 * Type guard to check if a node has a status model
 */
export function hasStatusModel(data: unknown): data is WithStatusModel {
  return typeof data === 'object' && data !== null && 'statusModel' in data
}

/**
 * Type guard to check if status is of type Status (adapter/bridge)
 */
export function isAdapterBridgeStatus(status: Status | PulseStatus | undefined): status is Status {
  if (!status) return false
  return 'connection' in status
}

/**
 * Type guard to check if status is of type PulseStatus
 */
export function isPulseStatus(status: Status | PulseStatus | undefined): status is PulseStatus {
  if (!status) return false
  return 'activation' in status
}
