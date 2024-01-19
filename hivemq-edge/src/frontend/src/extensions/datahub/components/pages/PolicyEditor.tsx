import React, { FC, useCallback, useMemo, useRef, useState } from 'react'
import ReactFlow, { Node, ReactFlowInstance, ReactFlowProvider, XYPosition } from 'reactflow'
import { Outlet, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Box } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { DataHubNodeType, PolicyType } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import CanvasControls from '../controls/CanvasControls.tsx'
import { Toolbox } from '../controls/Toolbox.tsx'
import Minimap from '../controls/Minimap.tsx'
import { BaseNode } from '../nodes/BaseNode.tsx'
import {
  TopicFilterNode,
  ClientFilterNode,
  DataPolicyNode,
  ValidatorNode,
  SchemaNode,
  OperationNode,
  BehaviorPolicyNode,
  TransitionNode,
} from '../../components/nodes/'
import { getNodeId, getNodePayload } from '@/extensions/datahub/utils/node.utils.ts'

const PolicyEditor: FC = () => {
  const { t } = useTranslation('datahub')
  const reactFlowWrapper = useRef(null)
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null)
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect, onAddNodes } = useDataHubDraftStore()
  const { policyType /*, policyId */ } = useParams()

  const nodeTypes = useMemo(
    () => ({
      baseNode: BaseNode,
      [DataHubNodeType.TOPIC_FILTER]: TopicFilterNode,
      [DataHubNodeType.CLIENT_FILTER]: ClientFilterNode,
      [DataHubNodeType.DATA_POLICY]: DataPolicyNode,
      [DataHubNodeType.VALIDATOR]: ValidatorNode,
      [DataHubNodeType.SCHEMA]: SchemaNode,
      [DataHubNodeType.OPERATION]: OperationNode,
      [DataHubNodeType.BEHAVIOR_POLICY]: BehaviorPolicyNode,
      [DataHubNodeType.TRANSITION]: TransitionNode,
    }),
    []
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
          onDragOver={onDragOver}
          onDrop={onDrop}
          // isValidConnection={isValidConnection}
        >
          <Box
            role={'toolbar'}
            aria-label={t('workspace.aria-label') as string}
            aria-controls={'edge-workspace-canvas'}
          >
            <Toolbox />
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
