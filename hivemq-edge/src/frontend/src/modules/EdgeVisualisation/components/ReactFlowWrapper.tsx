import { useEffect, useMemo } from 'react'
import ReactFlow, { Background } from 'reactflow'
import { Outlet } from 'react-router-dom'

import 'reactflow/dist/style.css'

import { EdgeTypes, NodeTypes } from '../types.ts'
import useGetFlowElements from '../hooks/useGetFlowElements.tsx'
import useWorkspaceStore from '../utils/store.ts'

import StatusListener from './controls/StatusListener.tsx'
import CanvasControls from './controls/CanvasControls.tsx'
import SelectionListener from './controls/SelectionListener.tsx'
import GroupNodesControl from './controls/GroupNodesControl.tsx'
import MonitoringEdge from './edges/MonitoringEdge.tsx'
import { NodeAdapter, NodeBridge, NodeEdge, NodeGroup, NodeListener } from './nodes'

const ReactFlowWrapper = () => {
  const { nodes: newNodes, edges: newEdges } = useGetFlowElements()
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
  const { nodes, edges, onNodesChange, onEdgesChange, onAddNodes, onAddEdges } = useWorkspaceStore()

  useEffect(() => {
    if (newNodes.length) onAddNodes(newNodes.map((e) => ({ item: e, type: 'add' })))
    if (newEdges.length) onAddEdges(newEdges.map((e) => ({ item: e, type: 'add' })))
  }, [newEdges, newNodes, onAddEdges, onAddNodes])

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
      <GroupNodesControl />
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
