import type { Edge, Instance, Node } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import type { WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'

import type { Adapter, Bridge, ProtocolAdapter } from '@/api/__generated__'
import { Status } from '@/api/__generated__'
import type { EdgeStatus, Group } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'

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

  // if (status?.connection === Status.connection.ERROR) return theme.colors.status.error[500]
  // if (status?.connection === Status.connection.UNKNOWN) return theme.colors.status.error[500]
  return theme.colors.status.error[500]
}

export const updateNodeStatus = (currentNodes: Node[], updates: Status[]) => {
  return currentNodes.map((n): Node<Bridge> => {
    if (n.type === NodeTypes.BRIDGE_NODE) {
      const newData = { ...n.data } as Bridge
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n
      if (newStatus.connection === newData.status?.connection) return n

      n.data = {
        ...newData,
        status: {
          connection: newStatus.connection,
        },
      }
      return n
    }
    if (n.type === NodeTypes.ADAPTER_NODE) {
      const newData = { ...n.data } as Adapter
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n
      // if (newStatus.connection === newData.status?.connection) return n

      n.data = {
        ...newData,
        status: { ...newStatus },
      }
      return n
    }
    return n
  })
}

export type EdgeStyle<T> = Pick<Edge<T>, 'style' | 'animated' | 'markerEnd' | 'data'>

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

export const updateEdgesStatus = (
  adapterTypes: ProtocolAdapter[],
  currentEdges: Edge[],
  updates: Status[],
  getNode: Instance.GetNode<Partial<Bridge | Adapter>>,
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
