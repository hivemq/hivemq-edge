import { FC } from 'react'
import { usePostTopicSamples } from '@/api/hooks/useTopicOntology/useGetTopicSamples.tsx'
import { Button, Card, CardBody, CardFooter, CardHeader, Heading, HStack, Text } from '@chakra-ui/react'

interface TopicSamplerProps {
  topic: string
}

const TopicSampler: FC<TopicSamplerProps> = ({ topic }) => {
  const topicSampler = usePostTopicSamples()
  return (
    <Card size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading size="md">{topic}</Heading>
      </CardHeader>
      <CardBody>
        <Text>
          The device is using a wildcard to subscribe to topics, they are therefore not known at configuration.
        </Text>
        <Text>
          You can trigger a short monitoring of the relevant MQTT traffic in order to extract a snapshot of the MQTT
          topics involved.
        </Text>
        <Text>They will be temporarily added to the Topic Wheel</Text>
      </CardBody>
      <CardFooter>
        <Button
          onClick={() => topicSampler.mutateAsync({ adapter: 'sss', topic: 'ss' })}
          isLoading={topicSampler.isPending}
          loadingText="Monitoring MQTT traffic"
        >
          Get Topic samples
        </Button>
      </CardFooter>
    </Card>
  )
}

export default TopicSampler
