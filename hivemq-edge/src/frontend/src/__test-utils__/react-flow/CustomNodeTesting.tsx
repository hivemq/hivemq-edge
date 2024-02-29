import ReactFlow, { Node, NodeTypes } from 'reactflow'
import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import 'reactflow/dist/style.css'

interface MockReactFlowProps {
  nodeTypes: NodeTypes
  nodes: Node[]
}

export const CustomNodeTesting: FC<MockReactFlowProps> = ({ nodeTypes, nodes }) => {
  return (
    <Box w={'90vw'} h={'90vh'}>
      <ReactFlow
        nodes={nodes}
        nodeTypes={nodeTypes}
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
