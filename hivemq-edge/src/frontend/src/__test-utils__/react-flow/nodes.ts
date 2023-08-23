import { NodeProps, Position } from 'reactflow'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

const DEFAULT_NODE = {
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
  ...DEFAULT_NODE,
}

export const MOCK_NODE_BRIDGE: NodeProps = {
  id: 'idBridge',
  type: NodeTypes.BRIDGE_NODE,
  sourcePosition: Position.Bottom,
  data: mockBridge,
  ...DEFAULT_NODE,
}
