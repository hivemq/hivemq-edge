import { useEffect } from 'react'
import { useReactFlow } from 'reactflow'
import { useTheme } from '@chakra-ui/react'

import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.tsx'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.tsx'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'

import { updateEdgesStatus, updateNodeStatus } from '../../utils/status-utils.ts'

const StatusListener = () => {
  const { data: adapterConnections } = useGetAdaptersStatus()
  const { data: bridgeConnections } = useGetBridgesStatus()
  const { data: adapterTypes } = useGetAdapterTypes()
  const { setNodes, getNode, setEdges } = useReactFlow()
  const theme = useTheme()

  useEffect(() => {
    const { items } = adapterTypes || {}
    if (adapterConnections?.items || bridgeConnections?.items) {
      const updates = [...(adapterConnections?.items || []), ...(bridgeConnections?.items || [])]

      setNodes((currentNodes) => {
        return updateNodeStatus(currentNodes, updates)
      })

      if (items)
        setEdges((currentEdges) => {
          return updateEdgesStatus(items, currentEdges, updates, getNode, theme)
        })
    }
  }, [adapterConnections, adapterTypes, bridgeConnections, getNode, setEdges, setNodes, theme])

  return null
}

export default StatusListener
