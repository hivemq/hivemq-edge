import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import ReactFlow, { Background } from 'reactflow'
import { Box } from '@chakra-ui/react'

import 'reactflow/dist/style.css'
import './reactflow-chakra.fix.css'

import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'
import useGetFlowElements from '@/modules/Workspace/hooks/useGetFlowElements.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import StatusListener from '@/modules/Workspace/components/controls/StatusListener.tsx'
import CanvasControls from '@/modules/Workspace/components/controls/CanvasControls.tsx'
import SelectionListener from '@/modules/Workspace/components/controls/SelectionListener.tsx'
import MonitoringEdge from '@/modules/Workspace/components/edges/MonitoringEdge.tsx'
import {
  NodeAdapter,
  NodeBridge,
  NodeEdge,
  NodeGroup,
  NodeListener,
  NodeHost,
  NodeDevice,
  NodeClient,
} from '@/modules/Workspace/components/nodes'

const ReactFlowWrapper = () => {
  const { t } = useTranslation()
  const { nodes: newNodes, edges: newEdges } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.CLUSTER_NODE]: NodeGroup,
      [NodeTypes.EDGE_NODE]: NodeEdge,
      [NodeTypes.ADAPTER_NODE]: NodeAdapter,
      [NodeTypes.BRIDGE_NODE]: NodeBridge,
      [NodeTypes.CLIENT_NODE]: NodeClient,
      [NodeTypes.LISTENER_NODE]: NodeListener,
      [NodeTypes.HOST_NODE]: NodeHost,
      [NodeTypes.DEVICE_NODE]: NodeDevice,
    }),
    []
  )
  const { nodes, edges, onNodesChange, onEdgesChange, onAddNodes, onAddEdges } = useWorkspaceStore()

  useEffect(() => {
    if (newNodes.length) onAddNodes(newNodes.map((node) => ({ item: node, type: 'add' })))
    if (newEdges.length) onAddEdges(newEdges.map((edge) => ({ item: edge, type: 'add' })))
  }, [newEdges, newNodes, onAddEdges, onAddNodes])

  const edgeTypes = useMemo(
    () => ({
      [EdgeTypes.REPORT_EDGE]: MonitoringEdge,
    }),
    []
  )

  return (
    <ReactFlow
      id="edge-workspace-canvas"
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
      <Box role="toolbar" aria-label={t('workspace.controls.aria-label')} aria-controls="edge-workspace-canvas">
        <SelectionListener />
        <StatusListener />
        <Background />
        <CanvasControls />
      </Box>
      <SuspenseOutlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
