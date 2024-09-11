import { FC } from 'react'
import { NodeProps, Position } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, Text, VStack } from '@chakra-ui/react'

import { Topic } from '@/components/MQTT/EntityTag.tsx'

import { TopicFilterData } from '@datahub/types.ts'
import { CustomHandle, NodeWrapper } from '@datahub/components/nodes'

export const TopicFilterNode: FC<NodeProps<TopicFilterData>> = (props) => {
  const { t } = useTranslation('datahub')
  const { id, data, type } = props

  return (
    <>
      <NodeWrapper route={`node/${type}/${id}`} {...props}>
        <HStack>
          {/*<NodeIcon type={DataHubNodeType.TOPIC_FILTER} />*/}
          <VStack>
            <Text data-testid={`node-topicFilter-${id}`}> {t('workspace.nodes.type', { context: type })}</Text>
          </VStack>
        </HStack>
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
            top: `calc(var(--chakra-space-3) + 12px + ${index * 24}px + ${0.5 * index}rem)`,
          }}
        />
      ))}
    </>
  )
}
