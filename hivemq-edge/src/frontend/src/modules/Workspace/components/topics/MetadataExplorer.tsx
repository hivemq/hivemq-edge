import { type FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Card, CardBody, CardHeader, Heading, HStack } from '@chakra-ui/react'
import { LuLoader } from 'react-icons/lu'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface MetadataExplorerProps {
  topic: string
}

const MetadataExplorer: FC<MetadataExplorerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const { data, isLoading } = useGetSubscriptionSchemas(topic, 'activated')

  return (
    <Card size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading size="md">{topic}</Heading>
        <IconButton size="sm" icon={<LuLoader />} aria-label={t('workspace.topicWheel.metadata.sample')} />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {!isLoading && data && (
          <Box h={200} overflowY="scroll" tabIndex={0}>
            <JsonSchemaBrowser schema={data} />
          </Box>
        )}
      </CardBody>
    </Card>
  )
}

export default MetadataExplorer
