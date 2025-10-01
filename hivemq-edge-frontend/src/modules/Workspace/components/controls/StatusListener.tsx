import { useEffect } from 'react'
import { useReactFlow } from '@xyflow/react'
import { useTheme } from '@chakra-ui/react'

import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.ts'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useGetPulseStatus } from '@/api/hooks/usePulse/useGetPulseStatus.ts'

import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import type { NodePulseType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'
import { NODE_PULSE_AGENT_DEFAULT_ID } from '@/modules/Workspace/utils/nodes-utils.ts'

import { updateEdgesStatus, updateNodeStatus, updatePulseStatus } from '../../utils/status-utils.ts'

/**
 * @deprecated StatusListener could be replaced by in-node processing and flow computing
 */
const StatusListener = () => {
  const { data: adapterConnections } = useGetAdaptersStatus()
  const { data: bridgeConnections } = useGetBridgesStatus()
  const { data: pulseConnections } = useGetPulseStatus()
  const { data: adapterTypes } = useGetAdapterTypes()
  const { setNodes, getNode, setEdges } = useReactFlow()
  const { onUpdateNode, edges, onEdgesChange, nodes } = useWorkspaceStore()

  const theme = useTheme()

  useEffect(() => {
    if (!pulseConnections) return
    const pulseNode = nodes.find((e) => e.type === NodeTypes.PULSE_NODE) as NodePulseType | undefined

    if (!pulseNode) return

    const changes = updatePulseStatus(pulseNode, pulseConnections, edges, theme)
    onUpdateNode<NodePulseType['data']>(NODE_PULSE_AGENT_DEFAULT_ID, changes.nodes)
    onEdgesChange(changes.edges)

    // TODO[NVL] Propagate changes down the graph

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pulseConnections])

  useEffect(() => {
    if (adapterConnections?.items || bridgeConnections?.items) {
      const updates = [...(adapterConnections?.items || []), ...(bridgeConnections?.items || [])]

      setNodes((currentNodes) => {
        return updateNodeStatus(currentNodes, updates)
      })

      const { items } = adapterTypes || {}
      if (items)
        setEdges((currentEdges) => {
          return updateEdgesStatus(items, currentEdges, updates, getNode, theme)
        })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adapterConnections?.items, adapterTypes, bridgeConnections?.items])

  return null
}

export default StatusListener
