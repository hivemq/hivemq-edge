import type { FC } from 'react'
import type { NodeProps } from 'reactflow'
import { Position } from 'reactflow'
import { Text } from '@chakra-ui/react'

import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const BaseNode: FC<NodeProps> = (props) => {
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <Text>{data.label}</Text>
      </NodeWrapper>
      <CustomHandle type="target" position={Position.Left} />
      <CustomHandle type="source" position={Position.Right} />
    </>
  )
}
