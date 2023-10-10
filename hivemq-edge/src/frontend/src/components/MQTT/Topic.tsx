import { FC, ReactNode } from 'react'
import { Tag, TagLabel, TagProps } from '@chakra-ui/react'

import TopicIcon from '@/components/Icons/TopicIcon.tsx'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

// TODO[NVL] Not sure adding ReactNode as possible children is a good move.
interface TopicProps extends TagProps {
  topic: ReactNode
}

const Topic: FC<TopicProps> = ({ topic, ...rest }) => {
  const expandedTopic = typeof topic === 'string' ? formatTopicString(topic) : topic
  return (
    <Tag data-testid={'topic-wrapper'} {...rest} letterSpacing={'-0.05rem'}>
      <TopicIcon boxSize="12px" mr={2} />
      {typeof topic === 'string' ? <TagLabel>{expandedTopic}</TagLabel> : topic}
    </Tag>
  )
}

export default Topic
