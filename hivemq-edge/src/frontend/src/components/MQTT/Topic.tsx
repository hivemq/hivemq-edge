import { FC } from 'react'
import { Tag, TagLabel } from '@chakra-ui/react'

import TopicIcon from '@/components/Icons/TopicIcon.tsx'

interface TopicProps {
  topic: string
}

const Topic: FC<TopicProps> = ({ topic }) => {
  return (
    <Tag data-testid={'topic-wrapper'}>
      <TopicIcon boxSize="12px" mr={2} />
      <TagLabel>{topic}</TagLabel>
    </Tag>
  )
}

export default Topic
