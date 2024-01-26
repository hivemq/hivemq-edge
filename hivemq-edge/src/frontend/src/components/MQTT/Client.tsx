import { Icon, Tag, TagLabel, TagProps } from '@chakra-ui/react'
import { FC, ReactNode } from 'react'
import { AiOutlineCloudServer } from 'react-icons/ai'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

// TODO[NVL] Not sure adding ReactNode as possible children is a good move.
interface TopicProps extends TagProps {
  client: ReactNode
}

const Client: FC<TopicProps> = ({ client, ...rest }) => {
  const expandedTopic = typeof client === 'string' ? formatTopicString(client) : client
  return (
    <Tag data-testid="client-wrapper" {...rest} letterSpacing="-0.05rem">
      <Icon as={AiOutlineCloudServer} boxSize="18px" mr={2} />
      {typeof client === 'string' ? <TagLabel>{expandedTopic}</TagLabel> : client}
    </Tag>
  )
}

export default Client
