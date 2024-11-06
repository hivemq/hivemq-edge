import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, CardBody, CardFooter, CardHeader, Heading, HStack, Text } from '@chakra-ui/react'
import { useGetClientTopicSamples } from '@/api/hooks/useClientSubscriptions/useGetClientTopicSamples.ts'
import { MOCK_SAMPLE_REFETCH_TRIGGER } from '@/api/hooks/useClientSubscriptions/__handlers__'

interface TopicSamplerProps {
  topic: string
}

const TopicSampler: FC<TopicSamplerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const { refetch, isLoading, isFetching } = useGetClientTopicSamples(MOCK_SAMPLE_REFETCH_TRIGGER)

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
          onClick={() => refetch()}
          isLoading={isLoading || isFetching}
          loadingText={t('workspace.topicWheel.topicSampler.sampler.loader')}
        >
          {t('workspace.topicWheel.topicSampler.sampler.cta')}
        </Button>
      </CardFooter>
    </Card>
  )
}

export default TopicSampler
