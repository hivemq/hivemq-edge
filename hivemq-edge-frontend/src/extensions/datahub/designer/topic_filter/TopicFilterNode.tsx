import type { FC } from 'react'
import type { NodeProps, Node } from '@xyflow/react'
import { Position } from '@xyflow/react'
import { VStack } from '@chakra-ui/react'

import { Topic } from '@/components/MQTT/EntityTag.tsx'

import type { TopicFilterData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'
import { getHandlePosition } from '@datahub/utils/theme.utils.ts'

export const TopicFilterNode: FC<NodeProps<Node<TopicFilterData>>> = (props) => {
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <VStack ml={6} alignItems="flex-end">
          {data.topics?.map((t) => <Topic tagTitle={t} key={t} />)}
        </VStack>
      </NodeWrapper>
      {data.topics?.map((_, index) => (
        <CustomHandle
          type="source"
          isConnectable={1}
          position={Position.Right}
          id={`topic-${index}`}
          key={`${id}-topic-${index}`}
          style={{
            top: getHandlePosition(index),
          }}
        />
      ))}
    </>
  )
}
