import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { RxReload } from 'react-icons/rx'

import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface DataModelSourcesProps extends CardProps {
  topics: string[]
}

const DataModelSources: FC<DataModelSourcesProps> = ({ topics, ...props }) => {
  const { t } = useTranslation('components')
  const { data, isLoading } = useGetSubscriptionSchemas(topics, topics ? 'source' : undefined)

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.sources.header')}
        </Heading>
        <IconButton
          size="sm"
          icon={<RxReload />}
          aria-label={t('rjsf.MqttTransformationField.sources.samples.aria-label')}
          isDisabled
        />
      </CardHeader>

      <CardBody maxH="55vh" overflowY="scroll">
        {isLoading && <LoaderSpinner />}
        {!isLoading && !data && <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />}
        {!isLoading && data && <JsonSchemaBrowser schema={data as JSONSchema7} isDraggable />}
      </CardBody>
    </Card>
  )
}

export default DataModelSources
