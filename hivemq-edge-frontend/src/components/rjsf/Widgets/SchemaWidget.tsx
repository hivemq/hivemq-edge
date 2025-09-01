import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { Box, FormControl, FormLabel, Text } from '@chakra-ui/react'

import { SelectEntityType } from '@/components/MQTT/types.ts'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

const SchemaWidget: React.FC<WidgetProps> = ({ id, value, required, disabled, readonly, label }) => {
  const { t } = useTranslation('components')
  const schemaHandler = validateSchemaFromDataURI(value, SelectEntityType.TOPIC)

  return (
    <FormControl alignItems="center" isRequired={required} isDisabled={disabled || readonly} isInvalid={true}>
      <FormLabel htmlFor={id} mb="0">
        {label}
      </FormLabel>
      <Box borderWidth={1} p={2}>
        {!schemaHandler.schema && <Text>{t('unset')}</Text>}
        {schemaHandler.schema && <JsonSchemaBrowser schema={schemaHandler.schema} />}
      </Box>
    </FormControl>
  )
}

export default SchemaWidget
