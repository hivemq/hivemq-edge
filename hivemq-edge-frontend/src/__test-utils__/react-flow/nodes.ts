import type { NodeProps } from '@xyflow/react'
import { Position } from '@xyflow/react'

import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockMqttListener } from '@/api/hooks/useGateway/__handlers__'
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'
import type {
  NodeAdapterType,
  NodeBridgeType,
  NodeCombinerType,
  NodeDeviceType,
  NodeEdgeType,
  NodeGroupType,
  NodeListenerType,
} from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

export const MOCK_DEFAULT_NODE = {
  selected: false,
  zIndex: 1000,
  isConnectable: true,
  positionAbsoluteX: 0,
  positionAbsoluteY: 0,
  dragging: false,
  selectable: true,
  draggable: true,
  deletable: true,
}

export const MOCK_NODE_ADAPTER: NodeProps<NodeAdapterType> = {
  id: 'idAdapter',
  type: NodeTypes.ADAPTER_NODE,
  sourcePosition: Position.Bottom,
  data: mockAdapter,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_BRIDGE: NodeProps<NodeBridgeType> = {
  id: 'idBridge',
  type: NodeTypes.BRIDGE_NODE,
  sourcePosition: Position.Bottom,
  data: mockBridge,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_EDGE: NodeProps<NodeEdgeType> = {
  id: 'idEdge',
  type: NodeTypes.EDGE_NODE,
  sourcePosition: Position.Bottom,
  data: { label: 'HiveMQ Edge' },
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_LISTENER: NodeProps<NodeListenerType> = {
  id: 'idListener',
  type: NodeTypes.LISTENER_NODE,
  sourcePosition: Position.Bottom,
  data: mockMqttListener,
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_GROUP: NodeProps<NodeGroupType> = {
  id: 'idGroup',
  type: NodeTypes.CLUSTER_NODE,
  sourcePosition: Position.Bottom,
  data: { childrenNodeIds: ['idAdapter', 'idBridge'], title: 'The group title', isOpen: true },
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_DEVICE: NodeProps<NodeDeviceType> = {
  id: 'idDevice',
  type: NodeTypes.DEVICE_NODE,
  data: { ...mockProtocolAdapter, sourceAdapterId: MOCK_ADAPTER_ID },
  ...MOCK_DEFAULT_NODE,
}

export const MOCK_NODE_COMBINER: NodeProps<NodeCombinerType> = {
  id: 'idCombiner',
  type: NodeTypes.COMBINER_NODE,
  data: mockCombiner,
  ...MOCK_DEFAULT_NODE,
}
