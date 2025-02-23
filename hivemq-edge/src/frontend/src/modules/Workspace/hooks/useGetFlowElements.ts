import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { Edge, Node } from 'reactflow'
import { useEdgesState, useNodesState } from 'reactflow'
import { useTheme } from '@chakra-ui/react'

import type { Adapter, Bridge, ProtocolAdapter } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useGetListeners } from '@/api/hooks/useGateway/useGetListeners.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'

import { createEdgeNode, createBridgeNode, createAdapterNode, createListenerNode } from '../utils/nodes-utils.ts'
import { applyLayout } from '../utils/layout-utils.ts'
import { useEdgeFlowContext } from './useEdgeFlowContext.ts'

const useGetFlowElements = () => {
  const { t } = useTranslation()
  const theme = useTheme()
  const { options, groups } = useEdgeFlowContext()
  const { data: adapterTypes, isLoading: isTypeLoding } = useGetAdapterTypes()
  const { data: bridges, isLoading: isBridgeLoading } = useListBridges()
  const { data: adapters, isLoading: isAdapterLoading } = useListProtocolAdapters()
  const { data: listenerList, isLoading: isLisrtenerLoading } = useGetListeners()
  const [nodes, setNodes, onNodesChange] = useNodesState<Bridge | Adapter>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])

  const { items: listeners } = listenerList || {}

  const isLoading = isAdapterLoading || isLisrtenerLoading || isLisrtenerLoading || isBridgeLoading || isTypeLoding

  useEffect(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []

    const nodeEdge = createEdgeNode(t('branding.appName'))

    listeners?.forEach((listener, nb) => {
      const { nodeListener, edgeConnector } = createListenerNode(listener, nb)

      if (options.showGateway) {
        nodes.push(nodeListener)
        edges.push(edgeConnector)
      }
    })

    bridges?.forEach((bridge, incBridgeNb) => {
      const { nodeBridge, edgeConnector, nodeHost, hostConnector } = createBridgeNode(
        bridge,
        incBridgeNb,
        bridges.length,
        theme
      )
      nodes.push(nodeBridge)
      edges.push(edgeConnector)

      nodes.push(nodeHost)
      edges.push(hostConnector)
    })

    adapters?.forEach((adapter, incAdapterNb) => {
      const type = adapterTypes?.items?.find((e) => e.id === adapter.type)

      const { nodeAdapter, edgeConnector, nodeDevice, deviceConnector } = createAdapterNode(
        type as ProtocolAdapter,
        adapter,
        incAdapterNb,
        adapters.length,
        theme
      )
      nodes.push(nodeAdapter)
      edges.push(edgeConnector)

      nodes.push(nodeDevice)
      edges.push(deviceConnector)
    })

    setNodes([nodeEdge, ...applyLayout(nodes, groups)])
    setEdges([...edges])
  }, [bridges, adapters, listeners, groups, setNodes, setEdges, t, options, theme, adapterTypes?.items, isLoading])

  return { nodes, edges, onNodesChange, onEdgesChange, isLoading }
}

export default useGetFlowElements
