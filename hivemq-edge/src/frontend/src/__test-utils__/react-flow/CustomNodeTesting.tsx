import { FC } from 'react'
import { useLocation } from 'react-router-dom'
import ReactFlow, { Edge, EdgeTypes, Node, NodeTypes } from 'reactflow'
import { Code, VStack } from '@chakra-ui/react'

import 'reactflow/dist/style.css'

interface MockReactFlowProps {
  nodeTypes?: NodeTypes
  nodes: Node[]
  edges?: Edge[]
  edgeTypes?: EdgeTypes
}

export const CustomNodeTesting: FC<MockReactFlowProps> = ({ nodeTypes, nodes, edges, edgeTypes }) => {
  const { pathname } = useLocation()

  return (
    <VStack w="90vw" h="90vh" gap={3}>
      <Code data-testid="test-navigate-pathname">{pathname}</Code>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        // fitView
        nodesDraggable={false}
        zoomOnScroll={false}
        panOnDrag={false}
        autoPanOnConnect={false}
        nodesConnectable={false}
      />
    </VStack>
  )
}
