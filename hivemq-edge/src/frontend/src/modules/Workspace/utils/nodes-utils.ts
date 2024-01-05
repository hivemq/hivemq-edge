import { Edge, MarkerType, Node, Position, XYPosition } from 'reactflow'
import { WithCSSVar } from '@chakra-ui/react'
import { Dict } from '@chakra-ui/utils'
import { GenericObjectType } from '@rjsf/utils'

import { Adapter, Bridge, Status, Listener, ProtocolAdapter } from '@/api/__generated__'

import { EdgeTypes, IdStubs, NodeTypes } from '../types.ts'
import { getBridgeTopics, discoverAdapterTopics } from '../utils/topics-utils.ts'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'

export const CONFIG_ADAPTER_WIDTH = 245

const POS_SEPARATOR = 8
const POS_EDGE: XYPosition = { x: 300, y: 200 }
const POS_NODE_INC: XYPosition = { x: 245 + POS_SEPARATOR, y: 300 }
const MAX_ADAPTERS = 10

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
  const idBridge = `${IdStubs.BRIDGE_NODE}@${bridge.id}`
  const isConnected =
    bridge.status?.connection === Status.connection.CONNECTED ||
    (bridge.status?.runtime === Status.runtime.STARTED && bridge.status?.connection === Status.connection.STATELESS)

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
    focusable: false,
    source: idBridge,
    type: EdgeTypes.REPORT_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, bridge.status),
    },
    animated: isConnected && !!remote.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: getThemeForStatus(theme, bridge.status),
    },
  }

  const idBridgeHost = `${IdStubs.HOST_NODE}@${bridge.id}`
  const nodeHost: Node = {
    id: idBridgeHost,
    type: NodeTypes.HOST_NODE,
    targetPosition: Position.Top,
    data: { label: bridge.host },
    position: positionStorage?.[idBridgeHost] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbBridge - (maxBridge - 1) / 2),
      y: POS_EDGE.y + POS_NODE_INC.y + 250,
    },
  }

  const hostConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.HOST_NODE}@${bridge.id}`,
    target: idBridgeHost,
    sourceHandle: 'Bottom',
    source: idBridge,
    // type: EdgeTypes.REPORT_EDGE,
    focusable: false,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, bridge.status),
    },
    animated: isConnected && !!local.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: getThemeForStatus(theme, bridge.status),
    },
  }

  return { nodeBridge, edgeConnector, nodeHost, hostConnector }
}

export const createListenerNode = (
  listener: Listener,
  nbListener: number,
  positionStorage?: Record<string, XYPosition>
) => {
  const idListener = `${IdStubs.LISTENER_NODE}@${listener.name}`

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
    focusable: false,
    // type: EdgeTypes.REPORT_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
    },
  }

  return { nodeListener, edgeConnector }
}

export const createAdapterNode = (
  type: ProtocolAdapter,
  adapter: Adapter,
  nbAdapter: number,
  maxAdapter: number,
  theme: Partial<WithCSSVar<Dict>>,
  positionStorage?: Record<string, XYPosition>
) => {
  const idAdapter = `${IdStubs.ADAPTER_NODE}@${adapter.id}`
  const isConnected =
    adapter.status?.connection === Status.connection.CONNECTED ||
    (adapter.status?.runtime === Status.runtime.STARTED && adapter.status?.connection === Status.connection.STATELESS)
  const topics = discoverAdapterTopics(type, adapter.config as GenericObjectType)

  const posX = nbAdapter % MAX_ADAPTERS
  const posY = Math.floor(nbAdapter / MAX_ADAPTERS) + 1
  const deltaX = Math.floor((Math.min(MAX_ADAPTERS, maxAdapter) - 1) / 2)

  const nodeAdapter: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
    id: idAdapter,
    type: NodeTypes.ADAPTER_NODE,
    sourcePosition: Position.Bottom,
    data: adapter,
    position: positionStorage?.[idAdapter] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (posX - deltaX),
      y: POS_EDGE.y - (POS_NODE_INC.y * posY) / 1.5,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idAdapter}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    source: idAdapter,
    focusable: false,
    type: EdgeTypes.REPORT_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, adapter.status),
    },
    animated: isConnected && !!topics.length,
    style: {
      strokeWidth: isConnected ? 1.5 : 0.5,
      stroke: getThemeForStatus(theme, adapter.status),
    },
  }

  return { nodeAdapter, edgeConnector }
}

export const getDefaultMetricsFor = (node: Node): string[] => {
  if (NodeTypes.ADAPTER_NODE === node.type) {
    const data = node.data as Adapter
    const suffix = 'com.hivemq.edge.protocol-adapters'
    const prefix = 'read.publish.success.count'
    return [`${suffix}.${data.type}.${data.id}.${prefix}`]
  }
  if (NodeTypes.BRIDGE_NODE === node.type) {
    const data = node.data as Bridge
    const suffix = 'com.hivemq.edge.bridge'
    const prefix = 'forward.publish.count'
    return [`${suffix}.${data.id}.${prefix}`]
  }
  return [] as string[]
}
