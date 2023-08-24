import { FC } from 'react'
import { HStack, Tag, TagLabel, VStack } from '@chakra-ui/react'
import Topic from '@/components/MQTT/Topic.tsx'

import { TopicFilter } from '../../types.ts'

interface NodeTopicsProps {
  topics: TopicFilter[]
}

const MAX_TOPICS = 2

const TopicsContainer: FC<NodeTopicsProps> = ({ topics }) => {
  return (
    <HStack spacing="4" data-testid={'topics-container'}>
      <VStack alignItems={'flex-start'}>
        {topics.slice(0, MAX_TOPICS).map((e, i) => (
          <Topic key={`local-${i}`} topic={e.topic} />
        ))}
      </VStack>
      {topics.length > MAX_TOPICS && (
        <Tag size={'sm'} data-testid={'topics-show-more'}>
          <TagLabel>+{topics.length - MAX_TOPICS}</TagLabel>
        </Tag>
      )}
    </HStack>
  )
}

export default TopicsContainer
