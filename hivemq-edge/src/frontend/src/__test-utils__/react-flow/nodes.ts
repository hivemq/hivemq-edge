import { NodeProps, Position } from 'reactflow'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { Listener } from '@/api/__generated__'
import { mockMqttListener } from '@/api/hooks/useGateway/__handlers__'
import { Group, NodeTypes } from '@/modules/Workspace/types.ts'

export const MOCK_DEFAULT_NODE = {
  selected: false,
  zIndex: 1000,
  isConnectable: true,
  xPos: 0,
  yPos: 0,
  dragging: false,
}

export const MOCK_NODE_ADAPTER: NodeProps = {
  id: 'idAdapter',
  type: NodeTypes.ADAPTER_NODE,
  sourcePosition: Position.Bottom,
  data: mockAdapter,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_BRIDGE: NodeProps = {
  id: 'idBridge',
  type: NodeTypes.BRIDGE_NODE,
  sourcePosition: Position.Bottom,
  data: mockBridge,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_EDGE: NodeProps = {
  id: 'idEdge',
  type: NodeTypes.EDGE_NODE,
  sourcePosition: Position.Bottom,
  data: { label: 'HiveMQ Edge' },
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_LISTENER: NodeProps<Listener> = {
  id: 'idListener',
  type: NodeTypes.LISTENER_NODE,
  sourcePosition: Position.Bottom,
  data: mockMqttListener,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_GROUP: NodeProps<Group> = {
  id: 'idGroup',
  type: NodeTypes.CLUSTER_NODE,
  sourcePosition: Position.Bottom,
  data: { childrenNodeIds: ['idAdapter', 'idBridge'], title: 'The group title', isOpen: true },
  ...MOCK_DEFAULT_NODE,
}
