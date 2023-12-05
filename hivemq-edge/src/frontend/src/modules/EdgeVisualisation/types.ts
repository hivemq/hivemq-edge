import { Edge, Node, OnConnect, OnEdgesChange, OnNodesChange } from 'reactflow'

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
}

export interface RFState {
  nodes: Node[]
  edges: Edge[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: OnConnect
}
