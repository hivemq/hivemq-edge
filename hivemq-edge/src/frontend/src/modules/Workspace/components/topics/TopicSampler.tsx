import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { usePostTopicSamples } from '@/api/hooks/useTopicOntology/useGetTopicSamples.tsx'
import { Button, Card, CardBody, CardFooter, CardHeader, Heading, HStack, Text } from '@chakra-ui/react'

interface TopicSamplerProps {
  topic: string
}

/**
 * @deprecated This is a mock, will need to be replaced
 */
const MOCK_ADAPTER = 'MOCK_ADAPTER'

const TopicSampler: FC<TopicSamplerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const topicSampler = usePostTopicSamples()

  return (
    <Card size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading size="md">{topic}</Heading>
      </CardHeader>
      <CardBody data-testid="sampler-warning">
        <Text>{t('workspace.topicWheel.topicSampler.title')} </Text>
        <Text>{t('workspace.topicWheel.topicSampler.description')} </Text>
      </CardBody>
      <CardFooter>
        <Button
          onClick={() => topicSampler.mutateAsync({ adapter: MOCK_ADAPTER, topic: topic })}
          isLoading={topicSampler.isPending}
          loadingText={t('workspace.topicWheel.topicSampler.sampler.loader')}
        >
          {t('workspace.topicWheel.topicSampler.sampler.cta')}
        </Button>
      </CardFooter>
    </Card>
  )
}

export default TopicSampler
