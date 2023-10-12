import { Node } from 'reactflow'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { Adapter, Bridge, Status } from '@/api/__generated__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

export const getThemeForStatus = (theme: Partial<WithCSSVar<Dict>>, status: Status | undefined) => {
  const isConnected = status?.connection === Status.connection.CONNECTED
  return isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500]
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
