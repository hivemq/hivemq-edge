import { Node } from 'reactflow'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { Adapter, Bridge, Status } from '@/api/__generated__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

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
      if (newStatus.connection === newData.status?.connection) return n

      n.data = {
        ...newData,
        status: {
          connection: newStatus.connection,
        },
      }
      return n
    }
    return n
  })
}
