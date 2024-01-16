import { FC, useMemo, useRef, useState } from 'react'
import ReactFlow, { Background, ReactFlowInstance, ReactFlowProvider } from 'reactflow'
import { Outlet, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { DataHubNodeType, PolicyType } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import CanvasControls from '../controls/CanvasControls.tsx'
import Minimap from '../controls/Minimap.tsx'
import { BaseNode } from '../nodes/BaseNode.tsx'
import { Box } from '@chakra-ui/react'

const PolicyEditor: FC = () => {
  const { t } = useTranslation('datahub')
  const reactFlowWrapper = useRef(null)
  const [, /*reactFlowInstance */ setReactFlowInstance] = useState<ReactFlowInstance | null>(null)
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect } = useDataHubDraftStore()
  const { policyType /*, policyId */ } = useParams()

  const nodeTypes = useMemo(
    () => ({
      [DataHubNodeType.TOPIC_FILTER]: BaseNode,
      [DataHubNodeType.CLIENT_FILTER]: BaseNode,
    }),
    []
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
          id={'edge-workspace-canvas'}
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
          // nodesConnectable
          // onDrop={onDrop}
          // onDragOver={onDragOver}
          // isValidConnection={isValidConnection}
        >
          <Background />
          <Box
            role={'toolbar'}
            aria-label={t('workspace.controls.aria-label') as string}
            aria-controls={'edge-workspace-canvas'}
          >
            <CanvasControls />
            <Minimap />
          </Box>
        </ReactFlow>
      </ReactFlowProvider>
      <Outlet />
    </>
  )
}

export default PolicyEditor
