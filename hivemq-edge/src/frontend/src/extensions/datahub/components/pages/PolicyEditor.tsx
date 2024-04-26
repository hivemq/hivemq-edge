import React, { FC, useCallback, useMemo, useRef, useState } from 'react'
import ReactFlow, {
  Connection,
  HandleType,
  Node,
  NodeAddChange,
  ReactFlowInstance,
  ReactFlowProvider,
  XYPosition,
} from 'reactflow'
import { Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Box } from '@chakra-ui/react'

import styles from './PolicyEditor.module.scss'

import { CustomNodeTypes } from '@datahub/designer/mappings.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { getConnectedNodeFrom, getNodeId, getNodePayload, isValidPolicyConnection } from '@datahub/utils/node.utils.ts'
import CanvasControls from '@datahub/components/controls/CanvasControls.tsx'
import Minimap from '@datahub/components/controls/Minimap.tsx'
import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'
import ToolboxSelectionListener from '@datahub/components/controls/ToolboxSelectionListener.tsx'
import { CopyPasteListener } from '@datahub/components/controls/CopyPasteListener.tsx'
import CopyPasteStatus from '@datahub/components/controls/CopyPasteStatus.tsx'

export type OnConnectStartParams = {
  nodeId: string | null
  handleId: string | null
  handleType: HandleType | null
}
interface OnConnectStartParamsNode extends OnConnectStartParams {
  type: string | undefined
}

const PolicyEditor: FC = () => {
  const { t } = useTranslation('datahub')
  const reactFlowWrapper = useRef(null)
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null)
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect, onAddNodes } = useDataHubDraftStore()
  const edgeConnectStart = useRef<OnConnectStartParamsNode | undefined>(undefined)

  const nodeTypes = useMemo(() => CustomNodeTypes, [])

  const checkValidity = useCallback(
    (connection: Connection) => isValidPolicyConnection(connection, nodes, edges),
    [edges, nodes]
  )

  const onDragOver = useCallback((event: React.DragEvent<HTMLElement> | undefined) => {
    if (event) {
      event.preventDefault()
      event.dataTransfer.dropEffect = 'move'
    }
  }, [])

  const onDrop = useCallback(
    (event: React.DragEvent<HTMLElement> | undefined) => {
      if (event && reactFlowInstance) {
        event.preventDefault()

        // check if the dropped element is valid
        const type = event.dataTransfer.getData('application/reactflow')
        if (typeof type === 'undefined' || !type) {
          return
        }

        const position: XYPosition = reactFlowInstance.screenToFlowPosition({
          x: event.clientX,
          y: event.clientY,
        })

        const newNode: Node = {
          id: getNodeId(type),
          type,
          position,
          data: getNodePayload(type),
        }
        onAddNodes([{ item: newNode, type: 'add' }])
      }
    },
    [onAddNodes, reactFlowInstance]
  )

  const onConnectStart = useCallback(
    (_: unknown, params: OnConnectStartParams) => {
      const nodeFound = nodes.find((e) => e.id === params.nodeId)
      edgeConnectStart.current = undefined
      if (nodeFound) {
        edgeConnectStart.current = { ...params, type: nodeFound.type }
      }
    },
    [nodes]
  )

  const onConnectEnd = useCallback(
    (event: MouseEvent | TouchEvent) => {
      const targetElement = event.target as Element
      const isTargetCanvas = targetElement.classList.contains('react-flow__pane')

      if (isTargetCanvas && edgeConnectStart.current && reactFlowInstance) {
        const { type, handleId, nodeId } = edgeConnectStart.current

        const droppedNode = getConnectedNodeFrom(type, handleId)
        if (droppedNode) {
          const id = getNodeId()
          const newNode: Node = {
            id,
            position: reactFlowInstance?.screenToFlowPosition({
              x: (event as MouseEvent).clientX || (event as TouchEvent).touches[0].clientX,
              y: (event as MouseEvent).clientY || (event as TouchEvent).touches[0].clientY,
            }),
            type: droppedNode.type,
            data: getNodePayload(droppedNode.type),
          }

          const edgeConnection: Connection = droppedNode.isSource
            ? {
                target: nodeId,
                targetHandle: handleId,
                source: id,
                sourceHandle: droppedNode.handle,
              }
            : {
                source: nodeId,
                sourceHandle: handleId,
                target: id,
                targetHandle: droppedNode.handle,
              }

          onAddNodes([{ item: newNode, type: 'add' } as NodeAddChange])
          onConnect(edgeConnection)
        }
      }
    },
    [onAddNodes, onConnect, reactFlowInstance]
  )

  return (
    <>
      <ReactFlowProvider>
        <ReactFlow
          ref={reactFlowWrapper}
          id="edge-workspace-canvas"
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          // onEdgeUpdate={onEdgeUpdate}
          onConnectStart={onConnectStart}
          onConnectEnd={onConnectEnd}
          onConnect={onConnect}
          connectionLineComponent={ConnectionLine}
          onInit={setReactFlowInstance}
          fitView
          snapToGrid
          snapGrid={[25, 25]}
          className={styles.dataHubFlow}
          onDragOver={onDragOver}
          onDrop={onDrop}
          isValidConnection={checkValidity}

          // onError={(id: string, message: string) => console.log('XXXXXX e', id, message)}
        >
          <Box role="toolbar" aria-label={t('workspace.aria-label') as string} aria-controls="edge-workspace-canvas">
            <ToolboxSelectionListener />
            <CopyPasteListener render={(copiedNodes) => <CopyPasteStatus nbCopied={copiedNodes.length} />} />
            <DesignerToolbox />
            <CanvasControls />
            <Minimap />
          </Box>
        </ReactFlow>
        <Outlet />
      </ReactFlowProvider>
    </>
  )
}

export default PolicyEditor
