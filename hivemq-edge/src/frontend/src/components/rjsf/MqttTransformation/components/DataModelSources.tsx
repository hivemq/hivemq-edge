import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { JSONSchema7 } from 'json-schema'
import { Card, CardBody, CardHeader, CardProps, Heading, HStack } from '@chakra-ui/react'
import { RxReload } from 'react-icons/rx'

import { MOCK_PROTOCOL_MODBUS } from '@/__test-utils__/adapters/modbus.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface DataModelSourcesProps extends CardProps {
  id?: string
}

const DataModelSources: FC<DataModelSourcesProps> = ({ id, ...props }) => {
  const { t } = useTranslation('components')
  return (
    <Card {...props}>
      <CardHeader as={HStack} justifyContent="space-between">
        <Heading as="h3" size="sm">
          {t('rjsf.MqttTransformationField.sources.header')}
        </Heading>
        <IconButton
          size="xs"
          icon={<RxReload />}
          aria-label={t('rjsf.MqttTransformationField.sources.samples.aria-label')}
        />
      </CardHeader>

      <CardBody>
        <JsonSchemaBrowser schema={MOCK_PROTOCOL_MODBUS.configSchema as JSONSchema7} />
      </CardBody>
    </Card>
  )
}

export default DataModelSources
