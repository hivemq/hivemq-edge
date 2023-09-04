import { useMemo } from 'react'
import ReactFlow, { Background } from 'reactflow'
import { Outlet } from 'react-router-dom'

import 'reactflow/dist/style.css'

import { EdgeTypes, NodeTypes } from '../types.ts'
import useGetFlowElements from '../hooks/useGetFlowElements.tsx'

import CanvasControls from './controls/CanvasControls.tsx'
import NodeEdge from './nodes/NodeEdge.tsx'
import NodeAdapter from './nodes/NodeAdapter.tsx'
import NodeBridge from './nodes/NodeBridge.tsx'
import NodeListener from './nodes/NodeListener.tsx'
import MonitoringEdge from './edges/MonitoringEdge.tsx'
import NodeGroup from '@/modules/EdgeVisualisation/components/nodes/NodeGroup.tsx'

const ReactFlowWrapper = () => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.EDGE_NODE]: NodeEdge,
      [NodeTypes.ADAPTER_NODE]: NodeAdapter,
      [NodeTypes.BRIDGE_NODE]: NodeBridge,
      [NodeTypes.LISTENER_NODE]: NodeListener,
    }),
    []
  )

  const edgeTypes = useMemo(
    () => ({
      [EdgeTypes.REPORT_EDGE]: MonitoringEdge,
    }),
    []
  )

  return (
    <ReactFlow
      deleteKeyCode={null}
      snapToGrid={true}
      nodes={nodes}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={onNodesChange}
      edges={edges}
      onEdgesChange={onEdgesChange}
    >
      <Background />
      <CanvasControls />
      <Outlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
