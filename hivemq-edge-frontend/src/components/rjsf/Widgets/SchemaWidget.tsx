import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { Box, FormControl, FormLabel, Text } from '@chakra-ui/react'

import type { ManagedAsset } from '@/api/__generated__'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

const SchemaWidget: FC<WidgetProps<ManagedAsset['schema']>> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  label,
  rawErrors,
}) => {
  const { t } = useTranslation('components')
  const schemaHandler = validateSchemaFromDataURI(value as ManagedAsset['schema'], SelectEntityType.TOPIC)
  const isInvalid = Boolean(rawErrors?.length)

  return (
    <FormControl alignItems="center" isRequired={required} isDisabled={disabled || readonly} isInvalid={isInvalid}>
      <FormLabel htmlFor={id} mb="0">
        {label}
      </FormLabel>
      <Box
        borderWidth={1}
        p={2}
        borderRadius="var(--chakra-radii-md)"
        sx={
          isInvalid
            ? {
                borderColor: 'var(--chakra-colors-status-error-500)',
                boxShadow: '0 0 0 1px var(--chakra-colors-status-error-500);',
              }
            : undefined
        }
      >
        {!schemaHandler.schema && <Text>{t('rjsf.MqttTransformationField.unset')}</Text>}
        {schemaHandler.schema && <JsonSchemaBrowser schema={schemaHandler.schema} />}
      </Box>
    </FormControl>
  )
}

export default SchemaWidget
