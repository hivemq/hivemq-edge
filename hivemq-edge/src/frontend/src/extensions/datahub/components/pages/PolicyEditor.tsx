import React, { FC, useCallback, useMemo, useRef, useState } from 'react'
import ReactFlow, {
  Background,
  BackgroundVariant,
  Connection,
  Node,
  ReactFlowInstance,
  ReactFlowProvider,
  XYPosition,
} from 'reactflow'
import { Outlet, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Box } from '@chakra-ui/react'

import styles from './PolicyEditor.module.scss'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { PolicyType } from '@datahub/types.ts'
import { CustomNodeTypes } from '@datahub/designer/mappings.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { getNodeId, getNodePayload, isValidPolicyConnection } from '@datahub/utils/node.utils.ts'
import CanvasControls from '@datahub/components/controls/CanvasControls.tsx'
import Minimap from '@datahub/components/controls/Minimap.tsx'
import DesignerToolbox from '@datahub/components/controls/DesignerToolbox.tsx'

const PolicyEditor: FC = () => {
  const { t } = useTranslation('datahub')
  const reactFlowWrapper = useRef(null)
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null)
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect, onAddNodes } = useDataHubDraftStore()
  const { policyType /*, policyId */ } = useParams()

  const nodeTypes = useMemo(() => CustomNodeTypes, [])

  const checkValidity = useCallback((connection: Connection) => isValidPolicyConnection(connection, nodes), [nodes])

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
          id: getNodeId(),
          type,
          position,
          data: getNodePayload(type),
        }
        onAddNodes([{ item: newNode, type: 'add' }])
      }
    },
    [onAddNodes, reactFlowInstance]
  )

  if (!policyType || !(policyType in PolicyType))
    return (
      <ErrorMessage
        type={t('error.notDefined.title') as string}
        message={t('error.notDefined.description') as string}
      />
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
          // onConnectStart={onConnectStart}
          // onConnectEnd={onConnectEnd}
          onConnect={onConnect}
          onInit={setReactFlowInstance}
          fitView
          snapToGrid
          snapGrid={[25, 25]}
          className={styles.dataHubFlow}
          // nodesConnectable
          onDragOver={onDragOver}
          onDrop={onDrop}
          isValidConnection={checkValidity}
          // onError={(id: string, message: string) => console.log('XXXXXX e', id, message)}
        >
          <Background
            id="1"
            gap={25}
            //color="var(--chakra-colors-gray-100)"
            variant={BackgroundVariant.Cross}
          />
          <Background
            id="2"
            gap={100}
            // color="var(--chakra-colors-gray-300)"
            variant={BackgroundVariant.Lines}
          />
          <Box role="toolbar" aria-label={t('workspace.aria-label') as string} aria-controls="edge-workspace-canvas">
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
