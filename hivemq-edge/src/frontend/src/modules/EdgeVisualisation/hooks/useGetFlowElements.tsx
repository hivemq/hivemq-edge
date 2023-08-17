import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Edge, Node, useEdgesState, useNodesState, XYPosition, MarkerType, Position } from 'reactflow'
import { useTheme, WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { Adapter, Bridge, ConnectionStatus } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'

import { IdStubs, NodeTypes } from '../types.ts'

const POS_SEPARATOR = 8
const POS_EDGE: XYPosition = { x: 300, y: 300 }
const POS_NODE_INC: XYPosition = { x: 150 + POS_SEPARATOR, y: 200 }

export const createEdgeNode = (label: string, positionStorage?: Record<string, XYPosition>) => {
  const nodeEdge: Node<unknown, NodeTypes.EDGE_NODE> = {
    id: IdStubs.EDGE_NODE,
    type: NodeTypes.EDGE_NODE,
    data: { label: label },
    draggable: false,
    position: positionStorage?.[IdStubs.EDGE_NODE] ?? POS_EDGE,
  }
  return nodeEdge
}

export const createBridgeNode = (
  bridge: Bridge,
  nbBridge: number,
  maxBridge: number,
  theme: WithCSSVar<Dict>,
  positionStorage?: Record<string, XYPosition>
) => {
  const idBridge = `${IdStubs.BRIDGE_NODE}#${bridge.id}`
  const isConnected = bridge.bridgeRuntimeInformation?.connectionStatus?.status === ConnectionStatus.status.CONNECTED

  const nodeBridge: Node<Bridge, NodeTypes.BRIDGE_NODE> = {
    id: idBridge,
    type: NodeTypes.BRIDGE_NODE,
    sourcePosition: Position.Top,
    // @ts-ignore To force a label on the default node
    data: { ...bridge, label: bridge.id },
    position: positionStorage?.[idBridge] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbBridge - (maxBridge - 1) / 2),
      y: POS_EDGE.y + POS_NODE_INC.y,
    },
    style: {
      backgroundColor: 'white',
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idBridge}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Bottom',
    source: idBridge,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: isConnected ? theme.colors.green[500] : theme.colors.yellow[500],
    },
    animated: isConnected,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: isConnected ? theme.colors.green[500] : theme.colors.yellow[500],
    },
    // label: bridge.host,
    // type: 'step',
  }

  // TODO[NVL] Add separate node for the host ?
  return { nodeBridge, edgeConnector }
}

export const createAdapterNode = (
  adapter: Adapter,
  nbAdapter: number,
  maxAdapter: number,
  theme: WithCSSVar<Dict>,
  positionStorage?: Record<string, XYPosition>
) => {
  const idAdapter = `${IdStubs.ADAPTER_NODE}#${adapter.id}`
  const isConnected = adapter.adapterRuntimeInformation?.connectionStatus?.status === ConnectionStatus.status.CONNECTED

  const nodeAdapter: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
    id: idAdapter,
    type: NodeTypes.ADAPTER_NODE,
    sourcePosition: Position.Bottom,
    // @ts-ignore To force a label on the default node
    data: { ...adapter, label: adapter.id },
    position: positionStorage?.[idAdapter] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbAdapter - (maxAdapter - 1) / 2),
      y: POS_EDGE.y - POS_NODE_INC.y,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idAdapter}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    source: idAdapter,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: isConnected ? theme.colors.green[500] : theme.colors.yellow[500],
    },
    animated: isConnected,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: isConnected ? theme.colors.green[500] : theme.colors.yellow[500],
    },
    // label: bridge.host,
    // type: 'step',
  }

  return { nodeAdapter, edgeConnector }
}

const useGetFlowElements = () => {
  const { t } = useTranslation()
  const { data: bridges } = useListBridges()
  const { data: adapters } = useListProtocolAdapters()
  const theme = useTheme()

  const [nodes, setNodes, onNodesChange] = useNodesState<Bridge | Adapter>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])

  useEffect(() => {
    if (!bridges) return
    if (!adapters) return

    const nodes: Node[] = []
    const edges: Edge[] = []

    const nodeEdge = createEdgeNode(t('branding.appName'))

    bridges.forEach((bridge, incBridgeNb) => {
      const { nodeBridge, edgeConnector } = createBridgeNode(bridge, incBridgeNb, bridges.length, theme)
      nodes.push(nodeBridge)
      edges.push(edgeConnector)
    })

    adapters.forEach((adapter, incAdapterNb) => {
      const { nodeAdapter, edgeConnector } = createAdapterNode(adapter, incAdapterNb, adapters.length, theme)
      nodes.push(nodeAdapter)
      edges.push(edgeConnector)
    })

    setNodes([nodeEdge, ...nodes])
    setEdges([...edges])
  }, [bridges, adapters, setNodes, setEdges])

  return { nodes, edges, onNodesChange, onEdgesChange }
}

export default useGetFlowElements
