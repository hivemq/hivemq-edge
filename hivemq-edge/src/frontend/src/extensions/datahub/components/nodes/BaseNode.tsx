import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'

import { NodeWrapper } from './NodeWrapper.tsx'
import { Text } from '@chakra-ui/react'

export const BaseNode: FC<NodeProps> = (props) => {
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <Text>{data.label}</Text>
      </NodeWrapper>
      <Handle type="target" position={Position.Left} />
      <Handle type="source" position={Position.Right} />
    </>
  )
}
