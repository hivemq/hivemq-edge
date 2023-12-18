import { Edge, Node, OnEdgesChange, OnNodesChange, NodeAddChange, EdgeAddChange, Rect } from 'reactflow'

export interface EdgeFlowOptions {
  showTopics: boolean
  showStatus: boolean
  showHosts: boolean
  showGateway: boolean
  showMonitoringOnEdge: boolean
}

export enum EdgeFlowLayout {
  HORIZONTAL = 'HORIZONTAL',
  CIRCLE_PACKING = 'CIRCLE_PACKING',
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
}

export enum EdgeTypes {
  REPORT_EDGE = 'REPORT_EDGE',
}

export enum IdStubs {
  EDGE_NODE = 'edge',
  BRIDGE_NODE = 'bridge',
  ADAPTER_NODE = 'adapter',
  HOST_NODE = 'host',
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
