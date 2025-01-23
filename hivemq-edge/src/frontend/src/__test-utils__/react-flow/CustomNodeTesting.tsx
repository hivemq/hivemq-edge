import type { FC } from 'react'
import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import type { Edge, EdgeTypes, Node, NodeTypes } from 'reactflow'
import ReactFlow from 'reactflow'
import { Code, VStack } from '@chakra-ui/react'

import 'reactflow/dist/style.css'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/FlowContext.tsx'

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
        />
      </VStack>
    </EdgeFlowProvider>
  )
}
