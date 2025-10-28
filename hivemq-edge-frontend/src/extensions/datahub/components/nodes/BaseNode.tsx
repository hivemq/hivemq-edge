import type { FC } from 'react'
import type { Node, NodeProps } from '@xyflow/react'
import { Position } from '@xyflow/react'
import { Text } from '@chakra-ui/react'

import type { DataHubNodeData } from '@datahub/types.ts'
import { NodeWrapper } from './NodeWrapper.tsx'
import { CustomHandle } from './CustomHandle.tsx'

export const BaseNode: FC<NodeProps<Node<DataHubNodeData & { label: string }>>> = (props) => {
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
