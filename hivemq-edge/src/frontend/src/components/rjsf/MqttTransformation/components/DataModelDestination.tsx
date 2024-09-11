import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'

import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ValidationStatus from '@/components/rjsf/MqttTransformation/components/mapping/ValidationStatus.tsx'
import { MappingValidation } from '@/modules/Subscriptions/types.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'

interface DataModelDestinationProps extends CardProps {
  topic: string | undefined
  validation: MappingValidation
}

const DataModelDestination: FC<DataModelDestinationProps> = ({ topic, validation, ...props }) => {
  const { t } = useTranslation('components')
  const { data, isLoading } = useGetSubscriptionSchemas(topic as string, topic ? 'destination' : undefined)

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.destination.header')}
        </Heading>
        <ValidationStatus validation={validation} />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {!isLoading && !data && <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />}
        {!isLoading && data && <JsonSchemaBrowser schema={data as JSONSchema7} />}
      </CardBody>
    </Card>
  )
}

export default DataModelDestination
