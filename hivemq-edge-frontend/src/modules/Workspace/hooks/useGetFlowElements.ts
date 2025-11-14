import { useListAssetMappers } from '@/api/hooks/useAssetMapper'
import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { Edge, Node } from '@xyflow/react'
import { useEdgesState, useNodesState } from '@xyflow/react'
import { useTheme } from '@chakra-ui/react'

import type { Combiner, ProtocolAdapter } from '@/api/__generated__'
import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
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
  createPulseNode,
} from '@/modules/Workspace/utils/nodes-utils.ts'
import { applyLayout } from '@/modules/Workspace/utils/layout-utils.ts'
import { useEdgeFlowContext } from './useEdgeFlowContext.ts'
import useWorkspaceStore from './useWorkspaceStore'

const useGetFlowElements = () => {
  const { t } = useTranslation()
  const theme = useTheme()
  const { options, groups } = useEdgeFlowContext()
  const { data: adapterTypes, isLoading: isTypeLoading } = useGetAdapterTypes()
  const { data: bridges, isLoading: isBridgeLoading } = useListBridges()
  const { data: adapters, isLoading: isAdapterLoading } = useListProtocolAdapters()
  const { data: listenerList, isLoading: isListenerLoading } = useGetListeners()
  const { data: combinerList, isLoading: isCombinerLoading } = useListCombiners()
  const { data: assetMapperList, isLoading: isAssetMapperLoading } = useListAssetMappers()
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const { data: hasPulse } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  const { items: listeners } = listenerList || {}

  const isLoading =
    isAdapterLoading ||
    isListenerLoading ||
    isCombinerLoading ||
    isBridgeLoading ||
    isTypeLoading ||
    isAssetMapperLoading

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

    if (hasPulse) {
      const { nodePulse, pulseConnector } = createPulseNode(theme)

      nodes.push(nodePulse)
      edges.push(pulseConnector)
    }

    const generateDataTransformationNodes = (combiner: Combiner) => {
      // Get current nodes from workspace store to use actual positions (after user drags)
      const currentNodes = useWorkspaceStore.getState().nodes

      // Find source nodes using current positions from workspace store
      const sources =
        combiner.sources?.items
          ?.map((entity) => {
            // First check workspace store for current position
            const currentNode = currentNodes.find((node: Node) => node.data?.id === entity.id)
            if (currentNode) return currentNode

            // Fallback to newly created nodes if not in store yet
            return nodes.find((node) => node.data.id === entity.id)
          })
          // TODO[xxxxxx] Error message for missing references
          .filter((node) => node) || []

      // Check if combiner already exists in workspace store (preserve manual position)
      const existingCombiner = currentNodes.find((node: Node) => node.id === combiner.id)

      const { nodeCombiner, edgeConnector, sourceConnectors } = createCombinerNode(combiner, sources as Node[], theme)

      // If combiner exists, preserve its current position (user might have moved it)
      if (existingCombiner) {
        nodeCombiner.position = existingCombiner.position
      }

      nodes.push(nodeCombiner)
      edges.push(edgeConnector, ...sourceConnectors)
    }

    combinerList?.items?.forEach(generateDataTransformationNodes)
    assetMapperList?.items?.forEach(generateDataTransformationNodes)

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
    assetMapperList?.items,
    isLoading,
    combinerList,
    hasPulse,
  ])

  return { nodes, edges, onNodesChange, onEdgesChange, isLoading }
}

export default useGetFlowElements
