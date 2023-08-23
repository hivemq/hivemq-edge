import { FC } from 'react'
import { Tag, TagLabel, TagLeftIcon } from '@chakra-ui/react'
import { SiMqtt } from 'react-icons/si'
import { useTranslation } from 'react-i18next'

interface TopicProps {
  topic: string
}

const Topic: FC<TopicProps> = ({ topic }) => {
  const { t } = useTranslation()

  return (
    <Tag data-testid={'topic-wrapper'}>
      <TagLeftIcon boxSize="12px" as={SiMqtt} aria-label={t('hivemq.topic') as string} />
      <TagLabel>{topic}</TagLabel>
    </Tag>
  )
}

export default Topic
