import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Text } from '@chakra-ui/react'

import NodeWrapper from '../parts/NodeWrapper.tsx'

const NodeHost: FC<NodeProps> = ({ selected, data }) => {
  const { label } = data
  return (
    <>
      <NodeWrapper isSelected={selected} wordBreak={'break-word'} maxW={200} textAlign={'center'} p={3}>
        <Text>{label}</Text>
      </NodeWrapper>
      <Handle type="target" position={Position.Top} isConnectable={false} />
    </>
  )
}

export default NodeHost
