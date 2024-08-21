import { type FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Button, Card, CardBody, CardHeader, Heading, HStack, Text } from '@chakra-ui/react'
import { LuLoader } from 'react-icons/lu'

import { BrokerClientConfiguration, BrokerClientSubscription } from '@/api/types/api-broker-client.ts'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import { useListClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useListClientSubscriptions.ts'
import { useCreateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useCreateClientSubscriptions.ts'
import { useUpdateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useUpdateClientSubscriptions.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface MetadataExplorerProps {
  topic: string
}

const MetadataExplorer: FC<MetadataExplorerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const { data, isLoading } = useGetSubscriptionSchemas(topic, 'activated')
  const { data: allClients, isLoading: isClientLoading } = useListClientSubscriptions()
  const createClient = useCreateClientSubscriptions()
  const updateClient = useUpdateClientSubscriptions()

  const handleOnClick = () => {
    if (!allClients || allClients.length === 0) {
      /**
       * @deprecated This is a mock, replace with topic filter (https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/)
       */
      const id = topic.slice(4)
      const newClientFilter: BrokerClientConfiguration = {
        id: id,
        subscriptions: [{ destination: id, maxQoS: BrokerClientSubscription.maxQoS._0 }],
      }
      createClient.mutateAsync({ id: id, config: newClientFilter })
      return
    }

    const firstFilter = allClients?.[0]
    if (firstFilter) {
      const firstFilterConfig: BrokerClientConfiguration = firstFilter.config
      firstFilterConfig.subscriptions?.push({ destination: firstFilter.id, maxQoS: BrokerClientSubscription.maxQoS._0 })
      updateClient.mutateAsync({ id: firstFilter.id, config: firstFilterConfig })
    }
  }

  const isClientReady = useMemo(() => {
    /**
     * @deprecated This is a mock, replace with topic filter (https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/)
     */
    const isWildcard = topic.startsWith('/tmp/')
    if (!isWildcard) return false

    /**
     * @deprecated This is a mock, replace with topic filter (https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/)
     */
    const newTopic = topic.slice(4)
    const allClientTopics =
      allClients?.reduce<string[]>((acc, clientFilter) => {
        const subscriptions = clientFilter.config.subscriptions?.map((subscription) => subscription.destination) || []
        return Array.from(new Set([...acc, ...subscriptions]))
      }, []) || []

    return !allClientTopics.includes(newTopic)
  }, [allClients, topic])

  return (
    <Card size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading size="md">{topic}</Heading>
        <IconButton size="sm" icon={<LuLoader />} aria-label={t('workspace.topicWheel.metadata.sample')} isDisabled />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {!isLoading && data && (
          <>
            {isClientReady && (
              <HStack mb={3}>
                <Box flex={1}>
                  <Text>{t('workspace.topicWheel.topicFilter.title')}</Text>
                </Box>
                <Button size="sm" onClick={handleOnClick} isDisabled={isClientLoading}>
                  {t('workspace.topicWheel.topicFilter.filter.cta')}
                </Button>
              </HStack>
            )}
            <Box h="25vh" overflowY="scroll" tabIndex={0}>
              <JsonSchemaBrowser schema={data} />
            </Box>
          </>
        )}
      </CardBody>
    </Card>
  )
}

export default MetadataExplorer
