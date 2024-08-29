import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { BiCheckboxChecked } from 'react-icons/bi'

import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { useGetSubscriptionSchemas } from '@/api/hooks/useTopicOntology/useGetSubscriptionSchemas.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

interface DataModelDestinationProps extends CardProps {
  topic: string
}

const DataModelDestination: FC<DataModelDestinationProps> = ({ topic, ...props }) => {
  const { t } = useTranslation('components')
  const { data, isLoading } = useGetSubscriptionSchemas(topic, 'activated_short')

  const isReady = Boolean(!isLoading && data)

  return (
    <Card {...props}>
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.destination.header')}
        </Heading>
        <IconButton
          size="xs"
          icon={<BiCheckboxChecked />}
          aria-label={t('rjsf.MqttTransformationField.destination.check.aria-label')}
        />
      </CardHeader>
      <CardBody>
        {!isReady && <LoaderSpinner />}
        {isReady && <JsonSchemaBrowser schema={data as JSONSchema7} />}
      </CardBody>
    </Card>
  )
}

export default DataModelDestination
