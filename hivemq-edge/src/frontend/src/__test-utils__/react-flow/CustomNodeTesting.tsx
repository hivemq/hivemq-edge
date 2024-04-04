import ReactFlow, { Edge, EdgeTypes, Node, NodeTypes } from 'reactflow'
import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import 'reactflow/dist/style.css'

interface MockReactFlowProps {
  nodeTypes?: NodeTypes
  nodes: Node[]
  edges?: Edge[]
  edgeTypes?: EdgeTypes
}

export const CustomNodeTesting: FC<MockReactFlowProps> = ({ nodeTypes, nodes, edges, edgeTypes }) => {
  return (
    <Box w="90vw" h="90vh">
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
    </Box>
  )
}
