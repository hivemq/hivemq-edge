import type { FC } from 'react'
import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import type { Edge, EdgeTypes, Node, NodeTypes } from '@xyflow/react'
import { ReactFlow } from '@xyflow/react'
import { Code, VStack } from '@chakra-ui/react'

import '@xyflow/react/dist/style.css'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider.tsx'
import { proOptions } from '../../components/react-flow/react-flow.utils'

interface MockReactFlowProps {
  nodeTypes?: NodeTypes
  nodes: Node[]
  edges?: Edge[]
  edgeTypes?: EdgeTypes
}

export const CustomNodeTesting: FC<MockReactFlowProps> = ({ nodeTypes, nodes, edges, edgeTypes }) => {
  const { pathname } = useLocation()
  const { reset, onAddNodes, onAddEdges } = useWorkspaceStore()

  useEffect(() => {
    reset()
    onAddNodes(
      nodes.map((node) => ({
        item: node,
        type: 'add',
      }))
    )
    if (edges)
      onAddEdges(
        edges.map((edge) => ({
          item: edge,
          type: 'add',
        }))
      )

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <EdgeFlowProvider>
      <VStack w="90vw" h="90vh" gap={3}>
        <Code data-testid="test-navigate-pathname">{pathname}</Code>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          // fitView
          // nodesDraggable={false}
          zoomOnScroll={false}
          panOnDrag={false}
          autoPanOnConnect={false}
          nodesConnectable={false}
          proOptions={proOptions}
        />
      </VStack>
    </EdgeFlowProvider>
  )
}
