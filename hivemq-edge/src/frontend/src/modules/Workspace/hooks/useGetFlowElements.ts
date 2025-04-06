import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { Edge, Node } from '@xyflow/react'
import { useEdgesState, useNodesState } from '@xyflow/react'
import { useTheme } from '@chakra-ui/react'

import type { ProtocolAdapter } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useGetListeners } from '@/api/hooks/useGateway/useGetListeners.ts'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListCombiners } from '@/api/hooks/useCombiners/useListCombiners'

import {
  createEdgeNode,
  createBridgeNode,
  createAdapterNode,
  createListenerNode,
  createCombinerNode,
} from '@/modules/Workspace/utils/nodes-utils.ts'
import { applyLayout } from '@/modules/Workspace/utils/layout-utils.ts'
import { useEdgeFlowContext } from './useEdgeFlowContext.ts'

const useGetFlowElements = () => {
  const { t } = useTranslation()
  const theme = useTheme()
  const { options, groups } = useEdgeFlowContext()
  const { data: adapterTypes, isLoading: isTypeLoading } = useGetAdapterTypes()
  const { data: bridges, isLoading: isBridgeLoading } = useListBridges()
  const { data: adapters, isLoading: isAdapterLoading } = useListProtocolAdapters()
  const { data: listenerList, isLoading: isListenerLoading } = useGetListeners()
  const { data: combinerList, isLoading: isCombinerLoading } = useListCombiners()
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  const { items: listeners } = listenerList || {}

  const isLoading = isAdapterLoading || isListenerLoading || isCombinerLoading || isBridgeLoading || isTypeLoading

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

    const nbCombiners = combinerList?.items?.length || 1
    const deltaPosition = Math.floor((nbCombiners - 1) / 2)

    combinerList?.items?.forEach((combiner, index) => {
      const sources =
        combiner.sources?.items
          ?.map((entity) => {
            return nodes.find((node) => node.data.id === entity.id)
          })
          // TODO[] Error message for missing references
          .filter((node) => node) || []

      const { nodeCombiner, edgeConnector, sourceConnectors } = createCombinerNode(
        combiner,
        (index - deltaPosition) / nbCombiners,
        sources as Node[],
        theme
      )

      nodes.push(nodeCombiner)
      edges.push(edgeConnector, ...sourceConnectors)
    })

    setNodes([nodeEdge, ...applyLayout(nodes, groups)])
    setEdges([...edges])
  }, [
    bridges,
    adapters,
    listeners,
    groups,
    setNodes,
    setEdges,
    t,
    options,
    theme,
    adapterTypes?.items,
    isLoading,
    combinerList,
  ])

  return { nodes, edges, onNodesChange, onEdgesChange, isLoading }
}

export default useGetFlowElements
