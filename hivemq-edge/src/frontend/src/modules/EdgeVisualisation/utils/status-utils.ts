import { Edge, Instance, MarkerType, Node } from 'reactflow'
import { GenericObjectType } from '@rjsf/utils'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { Adapter, Bridge, ProtocolAdapter, Status } from '@/api/__generated__'

import { NodeTypes } from '../types.ts'
import { discoverAdapterTopics, getBridgeTopics } from './topics-utils.ts'

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

export type EdgeStyle = Pick<Edge, 'style' | 'animated' | 'markerEnd'>

export const getEdgeStatus = (isConnected: boolean, hasTopics: boolean, themeForStatus: string): EdgeStyle => {
  const edge: EdgeStyle = {}
  edge.style = {
    strokeWidth: isConnected ? 1.5 : 0.5,
    stroke: themeForStatus,
  }
  edge.animated = isConnected && hasTopics
  edge.markerEnd = {
    type: MarkerType.ArrowClosed,
    width: 20,
    height: 20,
    color: themeForStatus,
  }
  return edge
}

export const updateEdgesStatus = (
  adapterTypes: ProtocolAdapter[],
  currentEdges: Edge[],
  updates: Status[],
  getNode: Instance.GetNode<Partial<Bridge | Adapter>>,
  theme: Partial<WithCSSVar<Dict>>,
): Edge[] => {
  return currentEdges.map((edge) => {
    const [a, b] = edge.source.split('@')
    const status = updates.find((e) => e.id === b && e.type === a)
    if (!status) return edge

    const source = getNode(edge.source)
    const isConnected =
      status?.connection === Status.connection.CONNECTED ||
      (status?.runtime === Status.runtime.STARTED && status?.connection === Status.connection.STATELESS)

    if (source && source.type === NodeTypes.ADAPTER_NODE) {
      const type = adapterTypes?.find((e) => e.id === (source.data as Adapter).type)
      const topics = type ? discoverAdapterTopics(type, (source.data as Adapter).config as GenericObjectType) : []

      return {
        ...edge,
        ...getEdgeStatus(isConnected, !!topics.length, getThemeForStatus(theme, status)),
      }
    }

    if (source && source.type === NodeTypes.BRIDGE_NODE) {
      const { remote } = getBridgeTopics(source.data as Bridge)
      return {
        ...edge,
        ...getEdgeStatus(isConnected, !!remote.length, getThemeForStatus(theme, status)),
      }
    }
    return edge
  })
}
