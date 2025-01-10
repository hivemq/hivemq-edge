import { type MouseEvent as ReactMouseEvent, useCallback, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import ReactFlow, { Background, getIncomers, getOutgoers, Node, NodePositionChange } from 'reactflow'
import { Box } from '@chakra-ui/react'

import 'reactflow/dist/style.css'
import './reactflow-chakra.fix.css'

import MiniMap from '@/components/react-flow/MiniMap.tsx'
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
} from '@/modules/Workspace/components/nodes'
import { gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'
import { proOptions } from '@/components/react-flow/react-flow.utils.ts'

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

  /**
   * Bug with the SHIFT+select
   * @see https://github.com/xyflow/xyflow/issues/4441
   */
  const onReactFlowNodeDrag = useCallback(
    (_event: ReactMouseEvent, _node: Node, draggedNodes: Node[]) => {
      const gluedDraggedNodes = draggedNodes.filter((node) =>
        Object.keys(gluedNodeDefinition).includes(node.type as NodeTypes)
      )
      for (const movedNode of gluedDraggedNodes) {
        const [type, spacing, handle] = gluedNodeDefinition[movedNode.type as NodeTypes]
        if (!type) continue

        const outgoers =
          handle === 'target' ? getOutgoers(movedNode, nodes, edges) : getIncomers(movedNode, nodes, edges)
        const gluedNode = outgoers.find((node) => node.type === type)
        if (!gluedNode) continue

        const positionChange: NodePositionChange = {
          id: gluedNode.id,
          type: 'position',
          position: { x: movedNode.position.x, y: movedNode.position.y + spacing },
        }

        onNodesChange([positionChange])
      }
    },
    [edges, nodes, onNodesChange]
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
      onNodeDrag={onReactFlowNodeDrag}
      fitView
      snapToGrid={true}
      nodesConnectable={false}
      deleteKeyCode={null}
      proOptions={proOptions}
    >
      <Box role="toolbar" aria-label={t('workspace.controls.aria-label')} aria-controls="edge-workspace-canvas">
        <SelectionListener />
        <StatusListener />
        <Background />
        <CanvasControls />
        <MiniMap
          zoomable
          pannable
          nodeClassName={(node) => node.type || ''}
          nodeComponent={(miniMapNode) => {
            if (miniMapNode.className === NodeTypes.EDGE_NODE)
              return <circle cx={miniMapNode.x} cy={miniMapNode.y} r="50" fill="#ffc000" />
            if (miniMapNode.className === NodeTypes.DEVICE_NODE || miniMapNode.className === NodeTypes.HOST_NODE)
              return null
            return (
              <rect
                x={miniMapNode.x}
                y={miniMapNode.y}
                width={miniMapNode.width}
                height={miniMapNode.height}
                fill="#e2e2e2"
              />
            )
          }}
        />
      </Box>
      <SuspenseOutlet />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
