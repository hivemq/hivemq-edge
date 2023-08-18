export interface EdgeFlowOptions {
  showTopics: boolean
  showStatus: boolean
  showMetrics: boolean
  showHosts: boolean
}

export enum NodeTypes {
  EDGE_NODE = 'EDGE_NODE',
  BRIDGE_NODE = 'BRIDGE_NODE',
  ADAPTER_NODE = 'ADAPTER_NODE',
}

export enum IdStubs {
  EDGE_NODE = 'edge',
  BRIDGE_NODE = 'bridge',
  ADAPTER_NODE = 'adapter',
  HOST_NODE = 'host',
  HOST_GROUP = 'group',
  CONNECTOR = 'connect',
}

export interface TopicFilter {
  topic: string
  frequency?: number
}
