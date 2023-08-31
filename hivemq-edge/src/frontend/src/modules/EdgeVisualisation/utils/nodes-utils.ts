import { Edge, MarkerType, Node, Position, XYPosition } from 'reactflow'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'

import { Adapter, Bridge, ConnectionStatus, Listener } from '@/api/__generated__'

import { IdStubs, NodeTypes } from '../types.ts'
import { getAdapterTopics, getBridgeTopics } from '../utils/topics-utils.ts'

const POS_SEPARATOR = 8
const POS_EDGE: XYPosition = { x: 300, y: 200 }
const POS_NODE_INC: XYPosition = { x: 200 + POS_SEPARATOR, y: 200 }

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
  theme: Partial<WithCSSVar<Dict>>,
  positionStorage?: Record<string, XYPosition>
) => {
  const idBridge = `${IdStubs.BRIDGE_NODE}#${bridge.id}`
  const isConnected = bridge.bridgeRuntimeInformation?.connectionStatus?.status === ConnectionStatus.status.CONNECTED
  const { local, remote } = getBridgeTopics(bridge)

  const nodeBridge: Node<Bridge, NodeTypes.BRIDGE_NODE> = {
    id: idBridge,
    type: NodeTypes.BRIDGE_NODE,
    sourcePosition: Position.Top,
    data: bridge,
    position: positionStorage?.[idBridge] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbBridge - (maxBridge - 1) / 2),
      y: POS_EDGE.y + POS_NODE_INC.y,
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
      color: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
    animated: isConnected && !!remote.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
  }

  const idBridgeHost = `${IdStubs.HOST_NODE}#${bridge.id}`
  const nodeHost: Node = {
    id: idBridgeHost,
    type: 'output',
    targetPosition: Position.Top,
    data: { label: bridge.host },
    position: positionStorage?.[idBridgeHost] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbBridge - (maxBridge - 1) / 2),
      y: POS_EDGE.y + POS_NODE_INC.y + 250,
    },
  }

  const hostConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.HOST_NODE}#${bridge.id}`,
    target: idBridgeHost,
    sourceHandle: 'Bottom',
    source: idBridge,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
    animated: isConnected && !!local.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
  }

  return { nodeBridge, edgeConnector, nodeHost, hostConnector }
}

export const createListenerNode = (
  listener: Listener,
  nbListener: number,
  positionStorage?: Record<string, XYPosition>
) => {
  const idListener = `${IdStubs.LISTENER_NODE}#${listener.name}`

  const nodeListener: Node<Listener, NodeTypes.LISTENER_NODE> = {
    id: idListener,
    type: NodeTypes.LISTENER_NODE,
    targetPosition: Position.Left,
    data: listener,
    position: positionStorage?.[idListener] ?? {
      x: POS_EDGE.x - POS_NODE_INC.x,
      y: POS_EDGE.y + 80 * nbListener,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idListener}`,
    source: IdStubs.EDGE_NODE,
    targetHandle: 'Listeners',
    target: idListener,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
    },
  }

  return { nodeListener, edgeConnector }
}

export const createAdapterNode = (
  adapter: Adapter,
  nbAdapter: number,
  maxAdapter: number,
  theme: Partial<WithCSSVar<Dict>>,
  positionStorage?: Record<string, XYPosition>
) => {
  const idAdapter = `${IdStubs.ADAPTER_NODE}#${adapter.id}`
  const isConnected = adapter.adapterRuntimeInformation?.connectionStatus?.status === ConnectionStatus.status.CONNECTED
  const topics = getAdapterTopics(adapter)

  const nodeAdapter: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
    id: idAdapter,
    type: NodeTypes.ADAPTER_NODE,
    sourcePosition: Position.Bottom,
    data: adapter,
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
      color: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
    animated: isConnected && !!topics.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: isConnected ? theme.colors.status.connected[500] : theme.colors.status.disconnected[500],
    },
  }

  return { nodeAdapter, edgeConnector }
}
