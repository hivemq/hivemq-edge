import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import { useTranslation } from 'react-i18next'
import type { CardProps } from '@chakra-ui/react'
import { Card, CardBody, CardHeader, Heading, HStack } from '@chakra-ui/react'

import { useGetWritingSchema } from '@/api/hooks/useProtocolAdapters/useGetWritingSchema.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ValidationStatus from '@/components/rjsf/MqttTransformation/components/mapping/ValidationStatus.tsx'
import type { MappingValidation } from '@/modules/Mappings/types.ts'

interface DataModelDestinationProps extends CardProps {
  topic: string
  adapterType: string
  adapterId: string
  validation: MappingValidation
}

const DataModelDestination: FC<DataModelDestinationProps> = ({ topic, adapterId, validation, ...props }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetWritingSchema(adapterId, topic)

  return (
    <Card {...props} size="sm">
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('components:rjsf.MqttTransformationField.destination.header')}
        </Heading>
        <ValidationStatus validation={validation} />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}
        {isError && error && <ErrorMessage message={error.message} />}
        {!isLoading && data && <JsonSchemaBrowser schema={data.configSchema as JSONSchema7} />}
      </CardBody>
    </Card>
  )
}

export default DataModelDestination
