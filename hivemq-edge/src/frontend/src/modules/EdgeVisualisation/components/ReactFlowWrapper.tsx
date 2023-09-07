import { useEffect, useMemo } from 'react'
import ReactFlow, { Background, ReactFlowState, useStore } from 'reactflow'
import { Outlet, useLocation } from 'react-router-dom'

import 'reactflow/dist/style.css'

import { EdgeTypes, NodeTypes } from '../types.ts'
import useGetFlowElements from '../hooks/useGetFlowElements.tsx'

import CanvasControls from './controls/CanvasControls.tsx'
import { NodeListener, NodeAdapter, NodeGroup, NodeBridge, NodeEdge } from './nodes/'
import MonitoringEdge from './edges/MonitoringEdge.tsx'

const addSelectedNodesState = (state: ReactFlowState) => (nodeIds: string[]) => state.addSelectedNodes(nodeIds)

const SelectionListener = () => {
  const { state } = useLocation()
  const addSelectedNodes = useStore(addSelectedNodesState)

  useEffect(() => {
    const { selectedAdapter } = state || {}
    const { adapterId, type } = selectedAdapter || {}
    if (!adapterId || !type) return

    addSelectedNodes([`adapter@${adapterId}`])
  }, [addSelectedNodes, state])

  return null
}

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
      <Background />
      <CanvasControls />
      <Outlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
