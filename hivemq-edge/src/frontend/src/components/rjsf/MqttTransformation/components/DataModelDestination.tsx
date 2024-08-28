import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { BiCheckboxChecked } from 'react-icons/bi'

import MOCK_SCHEMA from '@datahub/api/__generated__/schemas/TransitionData.json'
import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface DataModelDestinationProps extends CardProps {
  id?: string
}

const DataModelDestination: FC<DataModelDestinationProps> = ({ id, ...props }) => {
  const { t } = useTranslation('components')
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
        <JsonSchemaBrowser schema={MOCK_SCHEMA as JSONSchema7} />
      </CardBody>
    </Card>
  )
}

export default DataModelDestination
