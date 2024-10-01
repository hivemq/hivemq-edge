import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'

import { useGetTagSchemas } from '@/api/hooks/useDomainModel/useGetTagSchemas.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ValidationStatus from '@/components/rjsf/MqttTransformation/components/mapping/ValidationStatus.tsx'
import { MappingValidation } from '@/modules/Subscriptions/types.ts'

interface DataModelDestinationProps extends CardProps {
  topic: string
  validation: MappingValidation
}

const DataModelDestination: FC<DataModelDestinationProps> = ({ topic, validation, ...props }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetTagSchemas([topic])

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
        {!isLoading && data && <JsonSchemaBrowser schema={data} />}
      </CardBody>
    </Card>
  )
}

export default DataModelDestination
