import { useEffect } from 'react'
import { useReactFlow } from 'reactflow'

import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.tsx'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.tsx'

import { updateNodeStatus } from '../../utils/status-utils.ts'

const StatusListener = () => {
  const { data: adapterConnections } = useGetAdaptersStatus()
  const { data: bridgeConnections } = useGetBridgesStatus()
  const { setNodes } = useReactFlow()

  useEffect(() => {
    if (adapterConnections?.items && bridgeConnections?.items) {
      const updates = [...adapterConnections.items, ...bridgeConnections.items]
      setNodes((currentNodes) => updateNodeStatus(currentNodes, updates))
    }
  }, [adapterConnections, bridgeConnections, setNodes])

  return null
}

export default StatusListener
