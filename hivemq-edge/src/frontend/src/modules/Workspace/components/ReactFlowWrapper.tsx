import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import ReactFlow, { Background } from 'reactflow'
import { Outlet } from 'react-router-dom'
import { Box } from '@chakra-ui/react'

import 'reactflow/dist/style.css'
import './reactflow-chakra.fix.css'

import { EdgeTypes, NodeTypes } from '../types.ts'
import useGetFlowElements from '../hooks/useGetFlowElements.tsx'
import useWorkspaceStore from '../hooks/useWorkspaceStore.ts'

import StatusListener from './controls/StatusListener.tsx'
import CanvasControls from './controls/CanvasControls.tsx'
import SelectionListener from './controls/SelectionListener.tsx'
import GroupNodesControl from './controls/GroupNodesControl.tsx'
import MonitoringEdge from './edges/MonitoringEdge.tsx'
import { NodeAdapter, NodeBridge, NodeEdge, NodeGroup, NodeListener, NodeHost } from './nodes'

const ReactFlowWrapper = () => {
  const { t } = useTranslation()
  const { nodes: newNodes, edges: newEdges } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.CLUSTER_NODE]: NodeGroup,
      [NodeTypes.EDGE_NODE]: NodeEdge,
      [NodeTypes.ADAPTER_NODE]: NodeAdapter,
      [NodeTypes.BRIDGE_NODE]: NodeBridge,
      [NodeTypes.LISTENER_NODE]: NodeListener,
      [NodeTypes.HOST_NODE]: NodeHost,
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
      id={'edge-workspace-canvas'}
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      fitView
      snapToGrid={true}
      nodesConnectable={false}
      deleteKeyCode={null}
    >
      <Box
        role={'toolbar'}
        aria-label={t('workspace.controls.aria-label') as string}
        aria-controls={'edge-workspace-canvas'}
      >
        <GroupNodesControl />
        <SelectionListener />
        <StatusListener />
        <Background />
        <CanvasControls />
      </Box>
      <Outlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
