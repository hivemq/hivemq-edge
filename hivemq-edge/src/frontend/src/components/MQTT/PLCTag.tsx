import { FC, ReactNode } from 'react'
import { Tag, TagLabel, TagProps } from '@chakra-ui/react'

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { PLCTagIcon } from '@/components/Icons/TopicIcon.tsx'

// TODO[NVL] Not sure adding ReactNode as possible children is a good move.
interface TopicProps extends TagProps {
  topic: ReactNode
}

const PLCTag: FC<TopicProps> = ({ topic, ...rest }) => {
  const expandedTopic = typeof topic === 'string' ? formatTopicString(topic) : topic
  return (
    <Tag data-testid="topic-wrapper" {...rest} letterSpacing="-0.05rem" colorScheme="blue">
      <PLCTagIcon boxSize="12px" mr={2} />
      {typeof topic === 'string' ? <TagLabel>{expandedTopic}</TagLabel> : topic}
    </Tag>
  )
}

export default PLCTag
