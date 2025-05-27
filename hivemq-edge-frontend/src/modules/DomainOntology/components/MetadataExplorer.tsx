import { type FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Card, CardBody, CardHeader, Heading, HStack } from '@chakra-ui/react'
import { LuLoader } from 'react-icons/lu'

import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface MetadataExplorerProps {
  topic: string
}

const MetadataExplorer: FC<MetadataExplorerProps> = ({ topic }) => {
  const { t } = useTranslation()
  const { schema, isLoading, isError, error, refetch } = useSamplingForTopic(topic)

  return (
    <Card size="sm" w="100%">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading size="md">{topic}</Heading>
        <IconButton
          size="sm"
          icon={<LuLoader />}
          aria-label={t('workspace.topicWheel.metadata.sample')}
          onClick={() => refetch()}
          isDisabled={isLoading}
        />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {isError && error && <ErrorMessage message={error.message} />}
        {/*{!(isSchemaLoading || isSampleLoading) && !isError && !schema && (*/}
        {/*  <ErrorMessage message={t('domainMapping.error.noSampleForTopic', { topicFilter: topic })} />*/}
        {/*)}*/}
        {!isLoading && schema && (
          <Box h="25vh" overflowY="scroll" tabIndex={0}>
            <JsonSchemaBrowser schema={schema} hasExamples />
          </Box>
        )}
      </CardBody>
    </Card>
  )
}

export default MetadataExplorer
