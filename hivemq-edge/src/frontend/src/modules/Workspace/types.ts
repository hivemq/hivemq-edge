import type { Edge, Node, OnEdgesChange, OnNodesChange, NodeAddChange, EdgeAddChange, Rect } from '@xyflow/react'
import type { Adapter, Bridge, Combiner, Listener, ProtocolAdapter } from '@/api/__generated__'

export type NodeAdapterType = Node<Adapter, NodeTypes.ADAPTER_NODE>
export type NodeDeviceType = Node<DeviceMetadata, NodeTypes.DEVICE_NODE>
export type NodeBridgeType = Node<Bridge, NodeTypes.BRIDGE_NODE>
export type NodeGroupType = Node<Group, NodeTypes.CLUSTER_NODE>
export type NodeCombinerType = Node<Combiner, NodeTypes.COMBINER_NODE>
export type NodeListenerType = Node<Listener, NodeTypes.LISTENER_NODE>
export type NodeEdgeType = Node<{ label: string }, NodeTypes.EDGE_NODE>
export type NodeHostType = Node<{ label: string }, NodeTypes.HOST_NODE>

export interface EdgeFlowOptions {
  showTopics: boolean
  showStatus: boolean
  showGateway: boolean
}

export enum EdgeFlowLayout {
  HORIZONTAL = 'HORIZONTAL',
}

export interface EdgeFlowGrouping {
  layout: EdgeFlowLayout
  keys: string[]
  showGroups: boolean
}

export enum NodeTypes {
  EDGE_NODE = 'EDGE_NODE',
  BRIDGE_NODE = 'BRIDGE_NODE',
  ADAPTER_NODE = 'ADAPTER_NODE',
  LISTENER_NODE = 'LISTENER_NODE',
  CLUSTER_NODE = 'CLUSTER_NODE',
  HOST_NODE = 'HOST_NODE',
  DEVICE_NODE = 'DEVICE_NODE',
  COMBINER_NODE = 'COMBINER_NODE',
}

export enum EdgeTypes {
  REPORT_EDGE = 'REPORT_EDGE',
}

export type EdgeStatus = {
  isConnected: boolean
  hasTopics: boolean
}

export enum IdStubs {
  EDGE_NODE = 'edge',
  BRIDGE_NODE = 'bridge',
  ADAPTER_NODE = 'adapter',
  HOST_NODE = 'host',
  DEVICE_NODE = 'device',
  GROUP_NODE = 'group',
  LISTENER_NODE = 'listener',
  CONNECTOR = 'connect',
}

export interface TopicFilter {
  topic: string
  frequency?: number
}

export type Group = {
  childrenNodeIds: string[]
  title: string
  isOpen: boolean
  colorScheme?: string
}

export interface WorkspaceState {
  nodes: Node[]
  edges: Edge[]
}

export interface WorkspaceAction {
  reset: () => void
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onAddNodes: (changes: NodeAddChange[]) => void
  onAddEdges: (changes: EdgeAddChange[]) => void
  onInsertGroupNode: (node: Node<Group, NodeTypes.CLUSTER_NODE>, edge: Edge, rect: Rect) => void
  onDeleteNode: (type: NodeTypes, adapterId: string) => void
  onToggleGroup: (node: Pick<Node<Group, NodeTypes.CLUSTER_NODE>, 'id' | 'data'>, show: boolean) => void
  onGroupSetData: (id: string, node: Pick<Group, 'title' | 'colorScheme'>) => void
  onUpdateNode: <T>(id: string, data: T) => void
}

export interface TopicTreeMetadata {
  label: string
  count: number
}

export type DeviceMetadata = ProtocolAdapter & {
  sourceAdapterId: string
}
