import { FC, ReactNode } from 'react'
import { Tag, TagLabel, TagProps } from '@chakra-ui/react'

import TopicIcon from '@/components/Icons/TopicIcon.tsx'

// TODO[NVL] Not sure adding ReactNode as possible children is a good move.
interface TopicProps extends TagProps {
  topic: string | ReactNode
}

const Topic: FC<TopicProps> = ({ topic, ...rest }) => {
  return (
    <Tag data-testid={'topic-wrapper'} {...rest}>
      <TopicIcon boxSize="12px" mr={2} />
      {typeof topic === 'string' ? <TagLabel>{topic}</TagLabel> : topic}
    </Tag>
  )
}

export default Topic
