import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { VStack } from '@chakra-ui/react'

import { Topic } from '@/components/MQTT/EntityTag.tsx'

import { TopicFilterData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'

export const TopicFilterNode: FC<NodeProps<TopicFilterData>> = (props) => {
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <VStack ml={6} alignItems="flex-end">
          {data.topics?.map((t) => (
            <Topic tagTitle={t} key={t} />
          ))}
        </VStack>
      </NodeWrapper>
      {data.topics?.map((t, index) => (
        <CustomHandle
          type="source"
          isConnectable={1}
          position={Position.Right}
          id={`${t}-${index}`}
          key={`${id}-${t}-${index}`}
          style={{
            top: `calc(var(--chakra-space-3) + 12px + 48px + ${index * 24}px + ${0.5 * index}rem)`,
          }}
        />
      ))}
    </>
  )
}
