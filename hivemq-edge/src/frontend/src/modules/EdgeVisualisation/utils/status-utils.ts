import { Node } from 'reactflow'
import { Adapter, Bridge, ConnectionStatus } from '@/api/__generated__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

export const updateNodeStatus = (currentNodes: Node[], updates: ConnectionStatus[]) => {
  return currentNodes.map((n) => {
    if (n.type === NodeTypes.BRIDGE_NODE) {
      const newData = { ...n.data } as Bridge
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n
      if (newStatus.status === newData.bridgeRuntimeInformation?.connectionStatus?.status) return n

      n.data = {
        ...newData,
        bridgeRuntimeInformation: {
          connectionStatus: newStatus,
        },
      }
      return n
    }
    if (n.type === NodeTypes.ADAPTER_NODE) {
      const newData = { ...n.data } as Adapter
      const newStatus = updates.find((s) => s.id === newData.id)
      if (!newStatus) return n
      if (newStatus.status === newData.adapterRuntimeInformation?.connectionStatus?.status) return n

      n.data = {
        ...newData,
        adapterRuntimeInformation: {
          connectionStatus: newStatus,
        },
      }
      return n
    }
    return n
  })
}
