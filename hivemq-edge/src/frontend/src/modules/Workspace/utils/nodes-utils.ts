import type { Edge, Node, XYPosition } from '@xyflow/react'
import { MarkerType, Position } from '@xyflow/react'
import type { WithCSSVar } from '@chakra-ui/react'
import type { Dict } from '@chakra-ui/utils'
import type { GenericObjectType } from '@rjsf/utils'

import type { Adapter, Bridge, Combiner, Listener, ProtocolAdapter } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

import type { DeviceMetadata, NodeEdgeType } from '../types.ts'
import { EdgeTypes, IdStubs, NodeTypes } from '../types.ts'
import { getBridgeTopics, discoverAdapterTopics } from './topics-utils'
import { getThemeForStatus } from '@/modules/Workspace/utils/status-utils.ts'

export const CONFIG_ADAPTER_WIDTH = 245

const POS_SEPARATOR = 80
const POS_EDGE: XYPosition = { x: 300, y: 200 }
const POS_NODE_INC: XYPosition = { x: CONFIG_ADAPTER_WIDTH + POS_SEPARATOR, y: 400 }
const MAX_ADAPTERS = 10

export const gluedNodeDefinition: Record<string, [NodeTypes, number, 'target' | 'source']> = {
  [NodeTypes.BRIDGE_NODE]: [NodeTypes.HOST_NODE, 200, 'target'],
  [NodeTypes.ADAPTER_NODE]: [NodeTypes.DEVICE_NODE, -175, 'target'],
  [NodeTypes.HOST_NODE]: [NodeTypes.BRIDGE_NODE, -200, 'source'],
  [NodeTypes.DEVICE_NODE]: [NodeTypes.ADAPTER_NODE, 175, 'source'],
}

export const createEdgeNode = (label: string, positionStorage?: Record<string, XYPosition>) => {
  const nodeEdge: NodeEdgeType = {
    id: IdStubs.EDGE_NODE,
    type: NodeTypes.EDGE_NODE,
    data: { label: label },
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
    type: EdgeTypes.DYNAMIC_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, bridge.status),
    },
    animated: isConnected && !!remote.length,
    style: {
      strokeWidth: 1.5,
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
    type: EdgeTypes.DYNAMIC_EDGE,
    focusable: false,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, bridge.status),
    },
    animated: isConnected && !!local.length,
    style: {
      strokeWidth: 1.5,
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
    type: EdgeTypes.DYNAMIC_EDGE,
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
      y: POS_EDGE.y - POS_NODE_INC.y * posY * 1.5,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idAdapter}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    source: idAdapter,
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, adapter.status),
    },
    animated: isConnected && !!topics.length,
    style: {
      strokeWidth: 1.5,
      stroke: getThemeForStatus(theme, adapter.status),
    },
  }

  // let nodeDevice: Node<DeviceMetadata, NodeTypes.DEVICE_NODE> | undefined = undefined
  // let deviceConnector: Edge | undefined = undefined

  const idBAdapterDevice = `${IdStubs.DEVICE_NODE}@${idAdapter}`
  const nodeDevice: Node<DeviceMetadata, NodeTypes.DEVICE_NODE> = {
    id: idBAdapterDevice,
    type: NodeTypes.DEVICE_NODE,
    targetPosition: Position.Top,
    data: { ...type, sourceAdapterId: adapter.id },
    position: positionStorage?.[idBAdapterDevice] ?? {
      x: nodeAdapter.position.x,
      y: nodeAdapter.position.y + gluedNodeDefinition[NodeTypes.ADAPTER_NODE][1],
    },
  }

  const deviceConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.DEVICE_NODE}@${idAdapter}`,
    target: idBAdapterDevice,
    sourceHandle: 'Top',
    source: idAdapter,
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: getThemeForStatus(theme, adapter.status),
    },
    animated: isConnected && !!topics.length,
    style: {
      strokeWidth: 1.5,
      stroke: getThemeForStatus(theme, adapter.status),
    },
  }

  return { nodeAdapter, edgeConnector, nodeDevice, deviceConnector }
}

export const createCombinerNode = (
  combiner: Combiner,
  index: number,
  sources: Node[],
  theme: Partial<WithCSSVar<Dict>>
) => {
  const nodeCombiner: Node<Combiner, NodeTypes.COMBINER_NODE> = {
    id: combiner.id,
    type: NodeTypes.COMBINER_NODE,
    sourcePosition: Position.Bottom,
    data: combiner,
    position: {
      x: POS_EDGE.x + POS_NODE_INC.x * index * 2,
      y: POS_EDGE.y - POS_NODE_INC.y * 0.75,
    },
  }

  const sourceConnectors = sources.map<Edge>((source) => {
    const edgeConnector: Edge = {
      id: `${IdStubs.CONNECTOR}-${source.id}-${combiner.id}`,
      target: combiner.id,
      targetHandle: 'Top',
      source: source.id,
      focusable: false,
      type: EdgeTypes.DYNAMIC_EDGE,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: theme.colors.brand[500],
      },
      animated: false,
      style: {
        strokeWidth: 1.5,
        stroke: theme.colors.brand[500],
      },
    }
    return edgeConnector
  })

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${combiner.id}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top',
    source: combiner.id,
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: theme.colors.brand[500],
    },
    animated: false,
    style: {
      strokeWidth: 1.5,
      stroke: theme.colors.brand[500],
    },
  }

  return { nodeCombiner, edgeConnector, sourceConnectors }
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
