import { useMemo } from 'react'
import ReactFlow, { Background } from 'reactflow'
import { Outlet } from 'react-router-dom'

import 'reactflow/dist/style.css'

import { EdgeTypes, NodeTypes } from '../types.ts'
import useGetFlowElements from '../hooks/useGetFlowElements.tsx'

import StatusListener from './controls/StatusListener.tsx'
import CanvasControls from './controls/CanvasControls.tsx'
import SelectionListener from './controls/SelectionListener.tsx'
import { NodeListener, NodeAdapter, NodeGroup, NodeBridge, NodeEdge } from './nodes/'
import MonitoringEdge from './edges/MonitoringEdge.tsx'

const ReactFlowWrapper = () => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.CLUSTER_NODE]: NodeGroup,
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
      fitView
    >
      <SelectionListener />
      <StatusListener />
      <Background />
      <CanvasControls />
      <Outlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
