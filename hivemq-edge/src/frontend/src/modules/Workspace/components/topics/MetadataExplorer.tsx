import { type FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Button, Card, CardBody, CardHeader, Heading, HStack, Text } from '@chakra-ui/react'
import { LuLoader } from 'react-icons/lu'

import type { ClientFilter } from '@/api/__generated__'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import { useListClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useListClientSubscriptions.ts'
import { useCreateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useCreateClientSubscriptions.ts'
import { useUpdateClientSubscriptions } from '@/api/hooks/useClientSubscriptions/useUpdateClientSubscriptions.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { MOCK_CLIENT_STUB } from '@/api/hooks/useTopicOntology/__handlers__'

interface MetadataExplorerProps {
  topic: string
}

const MetadataExplorer: FC<MetadataExplorerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetSubscriptionSchemas(topic, 'source')
  const { data: allClients, isLoading: isClientLoading } = useListClientSubscriptions()
  const createClient = useCreateClientSubscriptions()
  const updateClient = useUpdateClientSubscriptions()

  const schema = useMemo(() => {
    return data?.[topic]
  }, [data, topic])

  const handleOnClick = () => {
    // TODO[25280] Refactor with new topic mapping structure, https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/
    const mockTopicId = topic.slice(MOCK_CLIENT_STUB.length + 1)
    if (!allClients || allClients.length === 0) {
      const newClientFilter: ClientFilter = {
        id: mockTopicId,
        topicFilters: [{ destination: mockTopicId }],
      }
      createClient.mutateAsync(newClientFilter)
      return
    }

    const [firstFilter] = allClients
    if (firstFilter) {
      firstFilter.topicFilters?.push({ destination: mockTopicId })
      updateClient.mutateAsync(firstFilter)
    }
  }

  const isClientReady = useMemo(() => {
    /**
     * @deprecated This is a mock, replace with topic filter (https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/)
     */
    const isWildcard = topic.startsWith(`/${MOCK_CLIENT_STUB}/`)
    if (!isWildcard) return false

    /**
     * @deprecated This is a mock, replace with topic filter (https://hivemq.kanbanize.com/ctrl_board/57/cards/25280/details/)
     */
    const newTopic = topic.slice(4)
    const allClientTopics =
      allClients?.reduce<string[]>((acc, clientFilter) => {
        const subscriptions = clientFilter.topicFilters?.map((subscription) => subscription.destination) || []
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
        {isError && error && <ErrorMessage message={error.message} />}
        {schema && (
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
              <JsonSchemaBrowser schema={schema} />
            </Box>
          </>
        )}
      </CardBody>
    </Card>
  )
}

export default MetadataExplorer
