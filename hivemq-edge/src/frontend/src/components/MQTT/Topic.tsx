import { FC } from 'react'
import { Tag, TagLabel, TagLeftIcon } from '@chakra-ui/react'
import { SiMqtt } from 'react-icons/si'

interface TopicProps {
  topic: string
}

const Topic: FC<TopicProps> = ({ topic }) => {
  return (
    <Tag>
      <TagLeftIcon boxSize="12px" as={SiMqtt} />
      <TagLabel>{topic}</TagLabel>
    </Tag>
  )
}

export default Topic
