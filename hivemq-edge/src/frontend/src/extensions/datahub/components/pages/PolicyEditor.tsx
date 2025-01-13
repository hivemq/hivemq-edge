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
import { useTranslation } from 'react-i18next'
import { Box } from '@chakra-ui/react'

import styles from './PolicyEditor.module.scss'

import { proOptions } from '@/components/react-flow/react-flow.utils.ts'
import MiniMap from '@/components/react-flow/MiniMap.tsx'
import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import CanvasControls from '@datahub/components/controls/CanvasControls.tsx'
import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'
import ToolboxSelectionListener from '@datahub/components/controls/ToolboxSelectionListener.tsx'
import { CopyPasteListener } from '@datahub/components/controls/CopyPasteListener.tsx'
import CopyPasteStatus from '@datahub/components/controls/CopyPasteStatus.tsx'
import DeleteListener from '@datahub/components/controls/DeleteListener.tsx'
import ConnectionLine from '@datahub/components/nodes/ConnectionLine.tsx'
import { CustomEdgeTypes, CustomNodeTypes } from '@datahub/config/nodes.config.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { getConnectedNodeFrom, getNodeId, getNodePayload, isValidPolicyConnection } from '@datahub/utils/node.utils.ts'
import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'

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
  const { status, nodes, edges, onNodesChange, onEdgesChange, onConnect, onAddNodes, isPolicyInDraft } =
    useDataHubDraftStore()
  const edgeConnectStart = useRef<OnConnectStartParamsNode | undefined>(undefined)
  const nodeTypes = useMemo(() => CustomNodeTypes, [])
  const edgeTypes = useMemo(() => CustomEdgeTypes, [])
  const isEditable = useMemo(() => status !== DesignerStatus.LOADED, [status])

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
      if (!isEditable) return
      const nodeFound = nodes.find((e) => e.id === params.nodeId)
      edgeConnectStart.current = undefined
      if (nodeFound) {
        edgeConnectStart.current = { ...params, type: nodeFound.type }
      }
    },
    [isEditable, nodes]
  )

  const onConnectEnd = useCallback(
    (event: MouseEvent | TouchEvent) => {
      const targetElement = event.target as Element
      const isTargetCanvas = targetElement.classList.contains('react-flow__pane')

      if (isTargetCanvas && edgeConnectStart.current && reactFlowInstance) {
        const { type, handleId, nodeId } = edgeConnectStart.current

        const droppedNode = getConnectedNodeFrom(type, handleId)
        if (!droppedNode) return

        if (
          droppedNode.type === DataHubNodeType.DATA_POLICY ||
          (droppedNode.type === DataHubNodeType.BEHAVIOR_POLICY && isPolicyInDraft())
        )
          return

        if (droppedNode) {
          const id = getNodeId()
          const newNode: Node = {
            id,
            position: reactFlowInstance.screenToFlowPosition({
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
    [isPolicyInDraft, onAddNodes, onConnect, reactFlowInstance]
  )

  const onConnectNodes = useCallback(
    (connection: Connection) => {
      edgeConnectStart.current = undefined
      onConnect(connection)
    },
    [onConnect]
  )

  return (
    <>
      <ReactFlowProvider>
        <ReactFlow
          ref={reactFlowWrapper}
          id="edge-datahub-canvas"
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          // onEdgeUpdate={onEdgeUpdate}
          onConnectStart={onConnectStart}
          onConnectEnd={onConnectEnd}
          onConnect={onConnectNodes}
          connectionRadius={35}
          connectionLineComponent={isEditable ? ConnectionLine : undefined}
          onInit={setReactFlowInstance}
          fitView
          snapToGrid
          snapGrid={[25, 25]}
          className={styles.dataHubFlow}
          onDragOver={onDragOver}
          onDrop={onDrop}
          isValidConnection={checkValidity}
          deleteKeyCode={[]}
          nodesConnectable={isEditable}
          proOptions={proOptions}
          role="region"
          aria-label={t('workspace.canvas.aria-label')}
          // nodesDraggable={isEditable}
          // elementsSelectable={isEditable}
          // onError={(id: string, message: string) => console.log('XXXXXX e', id, message)}
        >
          <Box role="toolbar" aria-label={t('workspace.toolbars.aria-label')} aria-controls="edge-datahub-canvas">
            <DeleteListener />
            <ToolboxSelectionListener />
            <DesignerToolbox />
            <CanvasControls />
            <CopyPasteListener render={(copiedNodes) => <CopyPasteStatus nbCopied={copiedNodes.length} />} />
            <MiniMap />
          </Box>
        </ReactFlow>
        <SuspenseOutlet />
      </ReactFlowProvider>
    </>
  )
}

export default PolicyEditor
