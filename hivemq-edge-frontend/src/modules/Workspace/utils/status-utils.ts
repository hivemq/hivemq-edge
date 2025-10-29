import type { Edge, Node, EdgeChange, EdgeReplaceChange } from '@xyflow/react'
import { getConnectedEdges } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import type { WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'

import type { Adapter, Bridge, Combiner, ProtocolAdapter } from '@/api/__generated__'
import { EntityType, PulseStatus, Status } from '@/api/__generated__'
import type { EdgeStatus, Group, NodePulseType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'
import {
  RuntimeStatus,
  OperationalStatus,
  type NodeStatusModel,
  type WithStatusModel,
} from '@/modules/Workspace/types/status.types'

import { getBridgeTopics } from './topics-utils.ts'

/**
 * @param theme
 * @param status
 *
 * TODO[NVL] Unify the styling with ConnectionStatusBadge
 * @see ConnectionStatusBadge
 */
export const getThemeForStatus = (theme: Partial<WithCSSVar<Dict>>, status: Status | undefined) => {
  if (status?.runtime === Status.runtime.STOPPED) return theme.colors.status.error[500]

  if (status?.connection === Status.connection.CONNECTED) return theme.colors.status.connected[500]
  if (status?.connection === Status.connection.DISCONNECTED) return theme.colors.status.disconnected[500]
  if (status?.connection === Status.connection.STATELESS) return theme.colors.status.stateless[500]

  return theme.colors.status.error[500]
}

/**
 * TODO[NVL] Unify the styling with PulseStatusBadge
 * @see PulseStatusBadge
 */
export const getThemeForPulseStatus = (theme: Partial<WithCSSVar<Dict>>, status: PulseStatus | undefined) => {
  if (status?.activation === PulseStatus.activation.ERROR || status?.runtime === PulseStatus.runtime.ERROR)
    return theme.colors.status.error[500]

  if (status?.activation === PulseStatus.activation.DEACTIVATED) return theme.colors.status.disconnected[500]

  if (status?.runtime === PulseStatus.runtime.CONNECTED) return theme.colors.status.connected[500]
  if (status?.runtime === PulseStatus.runtime.DISCONNECTED) return theme.colors.status.disconnected[500]

  return theme.colors.status.error[500]
}
/**
 * Get theme color for unified RuntimeStatus.
 * Maps RuntimeStatus to appropriate theme colors.
 *
 * @param theme - Chakra UI theme object
 * @param status - RuntimeStatus from NodeStatusModel
 * @returns Theme color string for the status
 * @todo check for differentiation for default return
 */
export const getThemeForRuntimeStatus = (theme: Partial<WithCSSVar<Dict>>, status: RuntimeStatus) => {
  switch (status) {
    case RuntimeStatus.ACTIVE:
      return theme.colors.status.connected[500]
    case RuntimeStatus.ERROR:
      return theme.colors.status.error[500]
    case RuntimeStatus.INACTIVE:
      return theme.colors.status.disconnected[500]
    default:
      return theme.colors.status.disconnected[500]
  }
}

/**
 * Get theme color for NodeStatusModel (uses runtime status).
 *
 * @param theme - Chakra UI theme object
 * @param statusModel - Complete NodeStatusModel
 * @returns Theme color string for the status
 */
export const getThemeForStatusModel = (theme: Partial<WithCSSVar<Dict>>, statusModel?: NodeStatusModel) => {
  if (!statusModel) return theme.colors.status.disconnected[500]
  return getThemeForRuntimeStatus(theme, statusModel.runtime)
}

/**
 * Get status color string from NodeStatusModel.
 * This is a utility function that extracts just the color value for use in various contexts
 * (edges, node shadows, etc.)
 *
 * @param theme - Chakra UI theme object
 * @param statusModel - Complete NodeStatusModel (optional)
 * @returns Color string for the status (e.g., for use in box-shadow, borders, etc.)
 */
export const getStatusColor = (theme: Partial<WithCSSVar<Dict>>, statusModel?: NodeStatusModel): string => {
  if (!statusModel) {
    return theme.colors.status.disconnected[500]
  }
  return getThemeForRuntimeStatus(theme, statusModel.runtime)
}

export const updatePulseStatus = (
  pulseNode: NodePulseType,
  pulseConnections: PulseStatus,
  edges: Edge[],
  theme: Partial<WithCSSVar<Dict>>
) => {
  // use onNodesChange to be consistent
  const newNodeData = { ...pulseNode.data, status: pulseConnections }

  const edgeChanges: EdgeChange[] = []
  const targets = getConnectedEdges([pulseNode], edges)
  for (const edge of targets) {
    const themeForStatus = getThemeForPulseStatus(theme, pulseConnections)
    const newEdgeData = {
      ...edge,
      style: {
        strokeWidth: 1.5,
        stroke: themeForStatus,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: themeForStatus,
      },
    }

    const edgeReplaceChange: EdgeReplaceChange = { id: edge.id, type: 'replace', item: newEdgeData }
    edgeChanges.push(edgeReplaceChange)
  }

  return { nodes: newNodeData, edges: edgeChanges }
}

/**
 * Update pulse node edges using unified NodeStatusModel.
 * This function applies both runtime status (colors) and operational status (animations)
 * to edges connected to a pulse node.
 *
 * @param pulseNode - The pulse node to update
 * @param statusModel - Unified status model for the pulse node
 * @param edges - All edges in the workspace
 * @param theme - Chakra UI theme object for color resolution
 * @returns Updated node data and edge changes
 */
export const updatePulseStatusWithModel = (
  pulseNode: NodePulseType,
  statusModel: NodeStatusModel,
  edges: Edge[],
  theme: Partial<WithCSSVar<Dict>>
) => {
  // Update node data with status model
  const newNodeData = { ...pulseNode.data, statusModel }

  const edgeChanges: EdgeChange[] = []
  const targets = getConnectedEdges([pulseNode], edges)

  for (const edge of targets) {
    const edgeStyle = getEdgeStatusFromModel(statusModel, true, theme)
    const newEdgeData = {
      ...edge,
      ...edgeStyle,
    }

    const edgeReplaceChange: EdgeReplaceChange = { id: edge.id, type: 'replace', item: newEdgeData }
    edgeChanges.push(edgeReplaceChange)
  }

  return { nodes: newNodeData, edges: edgeChanges }
}

/**
 * @deprecated This is wrong, should be based on the useWorkspaceStore, not the React Flow store
 */
export const updateNodeStatus = (currentNodes: Node[], updates: Status[]) => {
  return currentNodes.map((n): Node<Bridge | Adapter> => {
    if (n.type === NodeTypes.BRIDGE_NODE) {
      const newData = { ...n.data } as Bridge
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n as Node<Bridge>
      if (newStatus.connection === newData.status?.connection) return n as Node<Bridge>

      n.data = {
        ...newData,
        status: {
          connection: newStatus.connection,
        },
      }
      return n as Node<Bridge>
    }
    if (n.type === NodeTypes.ADAPTER_NODE) {
      const newData = { ...n.data } as Adapter
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n as Node<Adapter>
      // if (newStatus.connection === newData.status?.connection) return n

      n.data = {
        ...newData,
        status: { ...newStatus },
      }
      return n as Node<Adapter>
    }
    return n as Node<Adapter>
  })
}

export type EdgeStyle<T extends Record<string, unknown>> = Pick<Edge<T>, 'style' | 'animated' | 'markerEnd' | 'data'>

export const getEdgeStatus = (
  isConnected: boolean,
  hasTopics: boolean,
  hasMarker: boolean,
  themeForStatus: string
): EdgeStyle<EdgeStatus> => {
  const edge: EdgeStyle<EdgeStatus> = {}
  edge.style = {
    strokeWidth: 1.5,
    stroke: themeForStatus,
  }
  edge.animated = isConnected && hasTopics

  edge.markerEnd = hasMarker
    ? {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: themeForStatus,
      }
    : undefined

  edge.data = {
    isConnected,
    hasTopics,
  }
  return edge
}

/**
 * Get edge styling based on unified NodeStatusModel.
 * This function applies both runtime status (via colors) and operational status (via animations).
 *
 * @param statusModel - The unified status model containing runtime and operational status
 * @param hasMarker - Whether the edge should have an arrow marker
 * @param theme - Chakra UI theme object for color resolution
 * @param forceAnimation - Optional flag to force animation regardless of operational status
 * @returns Edge style properties including color, animation, and marker
 */
export const getEdgeStatusFromModel = (
  statusModel: NodeStatusModel | undefined,
  hasMarker: boolean,
  theme: Partial<WithCSSVar<Dict>>,
  forceAnimation?: boolean
): EdgeStyle<EdgeStatus> => {
  const edge: EdgeStyle<EdgeStatus> = {}

  // Runtime status determines color - using extracted utility function
  const themeColor = getStatusColor(theme, statusModel)

  edge.style = {
    strokeWidth: 1.5,
    stroke: themeColor,
  }

  // Operational status determines animation
  // ACTIVE operational status = animated edge (data is flowing and configured)
  // INACTIVE or ERROR = no animation (not configured or error state)
  const isOperational = statusModel?.operational === OperationalStatus.ACTIVE
  const isRuntimeActive = statusModel?.runtime === RuntimeStatus.ACTIVE
  edge.animated = forceAnimation !== undefined ? forceAnimation : isOperational && isRuntimeActive

  edge.markerEnd = hasMarker
    ? {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: themeColor,
      }
    : undefined

  edge.data = {
    isConnected: statusModel?.runtime === RuntimeStatus.ACTIVE,
    hasTopics: statusModel?.operational === OperationalStatus.ACTIVE,
  }

  return edge
}

/**
 * @deprecated This is wrong, should be based on the useWorkspaceStore, not the React Flow store
 */
export const updateEdgesStatus = (
  adapterTypes: ProtocolAdapter[],
  currentEdges: Edge[],
  updates: Status[],
  getNode: (id: string) => Node | undefined,
  theme: Partial<WithCSSVar<Dict>>
): Edge[] => {
  const newEdges: Edge[] = []

  // NOTE (to test): This pattern only work because the groups have to be before the included nodes in the array but the
  // group's edges are after the node's edges
  currentEdges.forEach((edge) => {
    if (edge.id.startsWith('connect-edge-group')) {
      const group = getNode(edge.source)
      if (!group || group.type !== NodeTypes.CLUSTER_NODE) return edge

      const groupEdges = newEdges.filter((e) =>
        (group as Node<Group>).data.childrenNodeIds.includes(e.source)
      ) as Edge<EdgeStatus>[]
      const isConnected = groupEdges.every((e) => e.data?.isConnected)
      const hasTopics = groupEdges.every((e) => e.data?.hasTopics)
      // status is mocked from the metadata
      const status: Status = {
        runtime: isConnected ? Status.runtime.STARTED : Status.runtime.STOPPED,
        connection: isConnected ? Status.connection.CONNECTED : Status.connection.DISCONNECTED,
      }

      newEdges.push({ ...edge, ...getEdgeStatus(isConnected, hasTopics, true, getThemeForStatus(theme, status)) })
      return
    }

    const [a, b] = edge.source.split('@')
    const status = updates.find((e) => e.id === b && e.type === a)
    if (!status) {
      newEdges.push(edge)
      return
    }

    const source = getNode(edge.source)
    const target = getNode(edge.target)
    const isConnected =
      (status?.connection === Status.connection.CONNECTED || status?.connection === Status.connection.STATELESS) &&
      status?.runtime === Status.runtime.STARTED

    if (source && source.type === NodeTypes.ADAPTER_NODE) {
      const type = adapterTypes?.find((e) => e.id === (source.data as Adapter).type)
      if (target?.type === NodeTypes.DEVICE_NODE) {
        newEdges.push({
          ...edge,
          ...getEdgeStatus(isConnected, false, isBidirectional(type), getThemeForStatus(theme, status)),
        })
      } else {
        newEdges.push({
          ...edge,
          ...getEdgeStatus(isConnected, false, true, getThemeForStatus(theme, status)),
        })
      }

      return
    }

    if (source && source.type === NodeTypes.BRIDGE_NODE) {
      const { remote } = getBridgeTopics(source.data as Bridge)
      newEdges.push({ ...edge, ...getEdgeStatus(isConnected, !!remote.length, true, getThemeForStatus(theme, status)) })
      return
    }
    newEdges.push(edge)
  })

  return newEdges
}

/**
 * Update edge styles based on unified NodeStatusModel.
 * This function applies both runtime status (colors) and operational status (animations).
 *
 * Runtime Status (Colors):
 * - ACTIVE: Green
 * - INACTIVE: Yellow/Gray
 * - ERROR: Red
 *
 * Operational Status (Animation):
 * - ACTIVE: Animated edge (data is flowing and properly configured)
 * - INACTIVE: No animation (not configured or draft state)
 * - ERROR: No animation (configuration error)
 *
 * @param adapterTypes - Available protocol adapter types for checking bidirectional support
 * @param currentEdges - Current edges in the workspace
 * @param getNode - Function to retrieve a node by ID
 * @param theme - Chakra UI theme object for color resolution
 * @returns Updated edges with proper styling based on status models
 */
export const updateEdgesStatusWithModel = (
  adapterTypes: ProtocolAdapter[],
  currentEdges: Edge[],
  getNode: (id: string) => Node | undefined,
  theme: Partial<WithCSSVar<Dict>>
): Edge[] => {
  const newEdges: Edge[] = []

  currentEdges.forEach((edge) => {
    // Handle group/cluster node edges
    if (edge.id.startsWith('connect-edge-group')) {
      const group = getNode(edge.source)
      if (!group || group.type !== NodeTypes.CLUSTER_NODE) {
        newEdges.push(edge)
        return
      }

      // Aggregate status from child nodes
      const groupEdges = newEdges.filter((e) =>
        (group as Node<Group>).data.childrenNodeIds.includes(e.source)
      ) as Edge<EdgeStatus>[]

      const isConnected = groupEdges.every((e) => e.data?.isConnected)
      const hasTopics = groupEdges.every((e) => e.data?.hasTopics)

      // Create aggregated status model for the group
      const aggregatedStatusModel: NodeStatusModel = {
        runtime: isConnected ? RuntimeStatus.ACTIVE : RuntimeStatus.INACTIVE,
        operational: hasTopics ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE,
        source: 'DERIVED',
      }

      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(aggregatedStatusModel, true, theme),
      })
      return
    }

    // Get source node
    const source = getNode(edge.source)
    if (!source) {
      newEdges.push(edge)
      return
    }

    // Extract status model from source node
    const sourceData = source.data as WithStatusModel
    const statusModel = sourceData.statusModel

    // Handle adapter node edges
    if (source.type === NodeTypes.ADAPTER_NODE) {
      const type = adapterTypes?.find((e) => e.id === (source.data as Adapter).type)
      const target = getNode(edge.target)

      if (target?.type === NodeTypes.DEVICE_NODE) {
        // Device connections may be bidirectional
        newEdges.push({
          ...edge,
          ...getEdgeStatusFromModel(statusModel, isBidirectional(type), theme),
        })
        return
      }

      // For Adapter → Combiner connections, use the target's operational status
      // The combiner itself determines if it's operational (has valid mappings)
      if (target?.type === NodeTypes.COMBINER_NODE) {
        const targetData = target.data as WithStatusModel
        const targetStatusModel = targetData.statusModel

        if (targetStatusModel) {
          // Create edge-specific status model using:
          // - Runtime status from Adapter node (is the source active?)
          // - Operational status from target combiner (does it have mappings?)
          const edgeStatusModel: NodeStatusModel = {
            runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
            operational: targetStatusModel.operational, // Use target's operational status
            source: 'DERIVED' as const,
          }

          newEdges.push({
            ...edge,
            ...getEdgeStatusFromModel(edgeStatusModel, true, theme),
          })
          return
        }

        // Fallback: If combiner hasn't computed statusModel yet, check mappings directly
        const targetCombiner = target.data as Combiner
        const hasMapping = targetCombiner.mappings?.items && targetCombiner.mappings.items.length > 0
        const fallbackStatusModel: NodeStatusModel = {
          runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
          operational: hasMapping ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE,
          source: 'DERIVED' as const,
        }

        newEdges.push({
          ...edge,
          ...getEdgeStatusFromModel(fallbackStatusModel, true, theme),
        })
        return
      }

      // Other connections (to Edge node, etc.)
      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(statusModel, true, theme),
      })
      return
    }

    // Handle bridge node edges
    if (source.type === NodeTypes.BRIDGE_NODE) {
      const { remote } = getBridgeTopics(source.data as Bridge)
      const target = getNode(edge.target)

      // For bridges, operational status depends on having remote topics configured
      const bridgeStatusModel = statusModel
        ? {
            ...statusModel,
            operational: remote.length > 0 ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE,
          }
        : undefined

      // For Bridge → Combiner connections, use the target's operational status
      // The combiner itself determines if it's operational (has valid mappings)
      if (target?.type === NodeTypes.COMBINER_NODE) {
        const targetData = target.data as WithStatusModel
        const targetStatusModel = targetData.statusModel

        if (targetStatusModel) {
          // Create edge-specific status model using:
          // - Runtime status from Bridge node (is the source active?)
          // - Operational status from target combiner (does it have mappings?)
          const edgeStatusModel: NodeStatusModel = {
            runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
            operational: targetStatusModel.operational, // Use target's operational status
            source: 'DERIVED' as const,
          }

          newEdges.push({
            ...edge,
            ...getEdgeStatusFromModel(edgeStatusModel, true, theme),
          })
          return
        }

        // Fallback: If combiner hasn't computed statusModel yet, check mappings directly
        const targetCombiner = target.data as Combiner
        const hasMapping = targetCombiner.mappings?.items && targetCombiner.mappings.items.length > 0
        const fallbackStatusModel: NodeStatusModel = {
          runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
          operational: hasMapping ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE,
          source: 'DERIVED' as const,
        }

        newEdges.push({
          ...edge,
          ...getEdgeStatusFromModel(fallbackStatusModel, true, theme),
        })
        return
      }

      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(bridgeStatusModel, true, theme),
      })
      return
    }

    // Handle pulse node edges
    if (source.type === NodeTypes.PULSE_NODE) {
      const target = getNode(edge.target)

      // For Pulse → Asset Mapper connections, use the target's operational status
      // The asset mapper itself determines if it's operational (has valid mappings)
      if (target?.type === NodeTypes.COMBINER_NODE) {
        const targetData = target.data as WithStatusModel
        const targetStatusModel = targetData.statusModel

        const targetCombiner = target.data as Combiner
        const isAssetMapper = targetCombiner.sources.items.some((s) => s.type === EntityType.PULSE_AGENT)

        if (isAssetMapper && targetStatusModel) {
          // Create edge-specific status model using:
          // - Runtime status from Pulse node (is the source active?)
          // - Operational status from target asset mapper (does it have valid mappings?)
          const edgeStatusModel: NodeStatusModel = {
            runtime: statusModel?.runtime || RuntimeStatus.INACTIVE,
            operational: targetStatusModel.operational, // Use target's operational status
            source: 'DERIVED' as const,
          }

          newEdges.push({
            ...edge,
            ...getEdgeStatusFromModel(edgeStatusModel, true, theme),
          })
          return
        }
      }

      // For other Pulse edges (e.g., to Edge node), use Pulse node's overall status
      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(statusModel, true, theme),
      })
      return
    }

    // Handle combiner node outbound edges
    // Combiners have their own operational status based on whether they have mappings
    if (source.type === NodeTypes.COMBINER_NODE) {
      // Use the combiner's own statusModel which includes:
      // - Runtime: derived from upstream sources
      // - Operational: ACTIVE if has mappings, INACTIVE otherwise
      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(statusModel, true, theme),
      })
      return
    }

    // Handle passive node edges (Device, Host, etc.)
    // These derive their status from upstream nodes
    if (statusModel) {
      newEdges.push({
        ...edge,
        ...getEdgeStatusFromModel(statusModel, true, theme),
      })
      return
    }

    // Default: keep edge unchanged if no status model available
    newEdges.push(edge)
  })

  return newEdges
}
