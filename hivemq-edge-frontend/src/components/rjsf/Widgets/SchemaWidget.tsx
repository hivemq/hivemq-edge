import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { Box, ButtonGroup, FormControl, FormLabel, HStack, Icon, Input } from '@chakra-ui/react'
import { LuDownload } from 'react-icons/lu'

import type { ManagedAsset } from '@/api/__generated__'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import { downloadJSON } from '@/utils/download.utils.ts'

const SchemaWidget: FC<WidgetProps<ManagedAsset['schema']>> = ({ id, value, required, disabled, readonly, label }) => {
  const { t } = useTranslation('components')
  const schemaHandler = validateSchemaFromDataURI(value as ManagedAsset['schema'], SelectEntityType.TOPIC)
  const isInvalid = Boolean(schemaHandler.status === 'error')

  const handleSchemaDownload = () => {
    if (isInvalid || !schemaHandler.schema) return

    downloadJSON<JSONSchema7>(schemaHandler.schema.title || 'topic-untitled', schemaHandler.schema)
  }

  return (
    <FormControl alignItems="center" isRequired={required} isDisabled={disabled || readonly} isInvalid={isInvalid}>
      <HStack justifyContent="space-between" alignItems="flex-end">
        <FormLabel htmlFor={id}>{label}</FormLabel>
        <ButtonGroup isAttached size="sm" variant="outline" mb={2}>
          <IconButton
            icon={<Icon as={LuDownload} />}
            data-testid="schema-download"
            onClick={handleSchemaDownload}
            isDisabled={isInvalid || !schemaHandler.schema}
            aria-label={t('rjsf.SchemaWidget.download')}
          />
        </ButtonGroup>
      </HStack>
      {!schemaHandler.schema && (
        <Input
          id={id}
          name={id}
          isDisabled
          defaultValue={
            isInvalid ? t('rjsf.SchemaWidget.error.noValidSchema') : t('rjsf.MqttTransformationField.unset')
          }
        />
      )}

      {schemaHandler.schema && (
        <Box borderWidth={1} p={2} borderRadius="var(--chakra-radii-md)">
          <JsonSchemaBrowser schema={schemaHandler.schema} />
        </Box>
      )}
    </FormControl>
  )
}

export default SchemaWidget
