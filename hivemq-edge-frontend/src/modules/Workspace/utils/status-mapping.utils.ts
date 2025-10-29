import { PulseStatus, Status } from '@/api/__generated__'
import { OperationalStatus, RuntimeStatus, type NodeStatusModel } from '../types/status.types'

/**
 * Maps adapter/bridge Status to unified RuntimeStatus.
 * Considers both connection and runtime fields to determine the overall state.
 *
 * Logic:
 * - ERROR if runtime is STOPPED or connection is ERROR
 * - ACTIVE if runtime is STARTED and connection is CONNECTED or STATELESS
 * - INACTIVE for all other cases (DISCONNECTED, UNKNOWN, etc.)
 *
 * @param status - The Status object from adapter or bridge
 * @returns Unified RuntimeStatus
 */
export const mapAdapterStatusToRuntime = (status?: Status): RuntimeStatus => {
  if (!status) return RuntimeStatus.INACTIVE

  // Check for error states first
  if (status.runtime === Status.runtime.STOPPED || status.connection === Status.connection.ERROR) {
    return RuntimeStatus.ERROR
  }

  // Check for active state
  if (
    status.runtime === Status.runtime.STARTED &&
    (status.connection === Status.connection.CONNECTED || status.connection === Status.connection.STATELESS)
  ) {
    return RuntimeStatus.ACTIVE
  }

  // Default to inactive (disconnected, unknown, etc.)
  return RuntimeStatus.INACTIVE
}

/**
 * Maps PulseStatus to unified RuntimeStatus.
 * Considers both activation and runtime fields to determine the overall state.
 *
 * Logic:
 * - ERROR if activation or runtime is ERROR
 * - INACTIVE if activation is DEACTIVATED
 * - ACTIVE if activation is ACTIVATED and runtime is CONNECTED
 * - INACTIVE for all other cases
 *
 * @param status - The PulseStatus object
 * @returns Unified RuntimeStatus
 */
export const mapPulseStatusToRuntime = (status?: PulseStatus): RuntimeStatus => {
  if (!status) return RuntimeStatus.INACTIVE

  // Check for error states first
  if (status.activation === PulseStatus.activation.ERROR || status.runtime === PulseStatus.runtime.ERROR) {
    return RuntimeStatus.ERROR
  }

  // Check for deactivated state
  if (status.activation === PulseStatus.activation.DEACTIVATED) {
    return RuntimeStatus.INACTIVE
  }

  // Check for active state
  if (status.activation === PulseStatus.activation.ACTIVATED && status.runtime === PulseStatus.runtime.CONNECTED) {
    return RuntimeStatus.ACTIVE
  }

  // Default to inactive (disconnected, etc.)
  return RuntimeStatus.INACTIVE
}

/**
 * Creates a complete NodeStatusModel for an adapter or bridge.
 *
 * @param status - The original Status object
 * @param operational - The operational status (defaults to ACTIVE)
 * @returns Complete NodeStatusModel
 */
export const createAdapterStatusModel = (
  status?: Status,
  operational: OperationalStatus = OperationalStatus.ACTIVE
): NodeStatusModel => {
  return {
    runtime: mapAdapterStatusToRuntime(status),
    operational,
    source: status?.type === 'bridge' ? 'BRIDGE' : 'ADAPTER',
    originalStatus: status,
    lastUpdated: status?.lastActivity || new Date().toISOString(),
  }
}

/**
 * Creates a complete NodeStatusModel for a bridge.
 * This is an alias for createAdapterStatusModel since they use the same Status type.
 *
 * @param status - The original Status object
 * @param operational - The operational status (defaults to ACTIVE)
 * @returns Complete NodeStatusModel
 */
export const createBridgeStatusModel = createAdapterStatusModel

/**
 * Creates a complete NodeStatusModel for a pulse node.
 *
 * @param status - The original PulseStatus object
 * @param operational - The operational status (defaults to ACTIVE)
 * @returns Complete NodeStatusModel
 */
export const createPulseStatusModel = (
  status?: PulseStatus,
  operational: OperationalStatus = OperationalStatus.ACTIVE
): NodeStatusModel => {
  return {
    runtime: mapPulseStatusToRuntime(status),
    operational,
    source: 'PULSE',
    originalStatus: status,
    lastUpdated: new Date().toISOString(),
  }
}

/**
 * Creates a static status model for passive nodes.
 * This will be replaced with derived status in the propagation phase.
 *
 * @param runtime - The runtime status
 * @param operational - The operational status
 * @returns NodeStatusModel with STATIC source
 */
export const createStaticStatusModel = (
  runtime: RuntimeStatus = RuntimeStatus.INACTIVE,
  operational: OperationalStatus = OperationalStatus.INACTIVE
): NodeStatusModel => {
  return {
    runtime,
    operational,
    source: 'STATIC',
    lastUpdated: new Date().toISOString(),
  }
}
