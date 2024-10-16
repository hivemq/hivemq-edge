import { Edge, Node, OnEdgesChange, OnNodesChange, NodeAddChange, EdgeAddChange, Rect } from 'reactflow'
import { ProtocolAdapter } from '@/api/__generated__'

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
  CLIENT_NODE = 'CLIENT_NODE',
  LISTENER_NODE = 'LISTENER_NODE',
  CLUSTER_NODE = 'CLUSTER_NODE',
  HOST_NODE = 'HOST_NODE',
  DEVICE_NODE = 'DEVICE_NODE',
}

export enum EdgeTypes {
  REPORT_EDGE = 'REPORT_EDGE',
}

export enum IdStubs {
  EDGE_NODE = 'edge',
  BRIDGE_NODE = 'bridge',
  ADAPTER_NODE = 'adapter',
  CLIENT_NODE = 'client',
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

export interface Group {
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
}

export interface TopicTreeMetadata {
  label: string
  count: number
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export type DeviceMetadata = ProtocolAdapter
